package com.example.timecard.ui.profile

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.domain.ChallengeEngine
import com.example.timecard.domain.applyGrantChanges
import com.example.timecard.domain.GamificationEngine
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val activityEventsMutex = Mutex()

    var profile by mutableStateOf(PlayerProfile())
        private set

    /** Employee id whose profile.json has been loaded into [profile]; null until then / after logout. */
    var loadedProfileFor by mutableStateOf<String?>(null)
        private set

    /**
     * True when profile.json exists but couldn't be read or parsed. Nothing is saved while this is
     * set: writing the blank fallback would wipe the employee's coins, inventory and badges.
     */
    var profileLoadFailed by mutableStateOf(false)
        private set

    /** Completes once this login's profile has loaded; rewards wait on it (cancelled on logout). */
    private var profileReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** Badge images loaded from server: badge ID → raw PNG/JPG bytes. */
    var badgeImages by mutableStateOf<Map<String, ByteArray>>(emptyMap())
        private set

    /** Custom avatar image bytes (when avatar == "custom"). */
    var avatarImage by mutableStateOf<ByteArray?>(null)
        private set

    // UI events — consumed by TimecardApp overlays
    var pendingBadges by mutableStateOf<List<String>>(emptyList())
        private set
    var newRecordMessage by mutableStateOf<String?>(null)
        private set
    var pendingConfetti by mutableStateOf(false)
        private set
    var recentCoinsEarned by mutableStateOf<Int?>(null)
        private set
    var recentStreakBonus by mutableStateOf<Int>(0)
        private set
    var recentStreakMultiplier by mutableStateOf<Double>(1.0)
        private set
    /** True while the one-time historical backfill is running. */
    var isBackfilling by mutableStateOf(false)
        private set

    /** True when the logged-in employee has an active streak but hasn't logged hours today. */
    var streakAtRisk by mutableStateOf(false)
        private set

    val deviceId: String = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: java.util.UUID.randomUUID().toString()

    var isLockedByAnotherUser by mutableStateOf(false)
        private set
    var triggerAutoLogout by mutableStateOf(false)
        private set
    var lastInteractionTimeMillis by mutableStateOf(System.currentTimeMillis())
        private set

    /** False when launched from KKC: no idle logout while the app is in front. */
    @Volatile var idleLogoutEnabled = true

    private var lockRenewJob: kotlinx.coroutines.Job? = null

    /** PIN — the stable identity key used for every per-employee file path. */
    var employeeName = ""
        private set
    /** Human-readable name (or shop-set displayName) — for records/UI text only, never a path key. */
    var employeeDisplayName = ""
        private set
    private var repository: FileRepository? = null
    private val gson = Gson()

    fun initialize(id: String, displayName: String, repo: FileRepository?) {
        employeeDisplayName = displayName
        if (employeeName == id && repo == repository) return
        employeeName = id
        repository = repo
        profileReady.cancel()
        profileReady = kotlinx.coroutines.CompletableDeferred()
        profileLoadFailed = false
        viewModelScope.launch {
            loadBadgesConfig(repo)
            loadProfile()
        }
    }

    fun interact() {
        lastInteractionTimeMillis = System.currentTimeMillis()
    }

    fun logout() {
        lockRenewJob?.cancel()
        if (employeeName.isNotEmpty()) {
            val repo = repository
            val name = employeeName
            viewModelScope.launch(Dispatchers.IO) { repo?.releaseLock(name, "profile", deviceId) }
        }
        profileReady.cancel()
        profileReady = kotlinx.coroutines.CompletableDeferred()
        profileLoadFailed = false
        profile = PlayerProfile()
        loadedProfileFor = null
        employeeName = ""
        repository = null
        pendingBadges = emptyList()
        newRecordMessage = null
        pendingConfetti = false
        badgeImages = emptyMap()
        avatarImage = null
        triggerAutoLogout = false
        isLockedByAnotherUser = false
    }

    fun resetAutoLogout() { triggerAutoLogout = false }
    fun resetLockError() { isLockedByAnotherUser = false }
    fun dismissStreakWarning() { streakAtRisk = false }

    fun dismissNextBadge() {
        if (pendingBadges.isNotEmpty()) pendingBadges = pendingBadges.drop(1)
    }

    fun dismissRecord() { newRecordMessage = null }
    fun dismissConfetti() { pendingConfetti = false }
    fun dismissCoinsEarned() {
        recentCoinsEarned = null
        recentStreakBonus = 0
        recentStreakMultiplier = 1.0
    }

    fun setDisplayName(name: String) {
        val trimmed = name.trim()
        profile = profile.copy(displayName = trimmed.ifBlank { null })
        saveProfile()
        if (employeeName.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                com.example.timecard.data.network.HoursTrackerApi.setEmployeeDisplayName(employeeName, trimmed)
            }
        }
    }

    fun processPurchase(itemId: String, itemTitle: String = "", price: Int): Boolean {
        if (profile.coins >= price && !profile.inventory.contains(itemId)) {
            val record = com.example.timecard.data.model.PurchaseRecord(
                itemId = itemId,
                itemTitle = itemTitle,
                price = price,
                purchasedAt = java.time.Instant.now().toString()
            )
            profile = profile.copy(
                coins = profile.coins - price,
                inventory = profile.inventory + itemId,
                purchaseHistory = profile.purchaseHistory + record
            )
            saveProfile()
            return true
        }
        return false
    }

    /** Record that the user consumed their one-time 30s trial for a theme item. */
    fun markThemeTried(itemId: String) {
        val current = profile.triedThemes.toMutableList()
        if (current.contains(itemId)) return
        current.add(itemId)
        profile = profile.copy(triedThemes = current)
        saveProfile()
    }

    /** Mark special shop items as seen so the banner won't re-trigger for them. */
    fun markSpecialItemsSeen(ids: List<String>) {
        val current = profile.seenSpecialItems.toMutableList()
        val added = ids.filter { !current.contains(it) }
        if (added.isEmpty()) return
        current.addAll(added)
        profile = profile.copy(seenSpecialItems = current)
        saveProfile()
    }

    fun spendCoins(price: Int): Boolean {
        if (profile.coins >= price) {
            profile = profile.copy(coins = profile.coins - price)
            saveProfile()
            return true
        }
        return false
    }


    fun setAccentColor(key: String?) {
        profile = profile.copy(accentColor = key)
        saveProfile()
    }

    fun setAvatar(emoji: String?) {
        profile = profile.copy(avatar = emoji)
        if (emoji != "custom") avatarImage = null
        saveProfile()
    }

    fun onAlertAcknowledged() {
        viewModelScope.launch {
            // Wait for this login's profile: rewarding the blank pre-load profile would save it over the real one.
            try { profileReady.await() } catch (_: kotlinx.coroutines.CancellationException) { return@launch }
            val newStats = profile.runningStats.copy(
                alertsAcknowledged = profile.runningStats.alertsAcknowledged + 1
            )
            val updated = profile.copy(runningStats = newStats)
            applyCoins(updated, 5)
        }
    }

    /**
     * Called whenever a timecard is successfully saved.
     * Computes XP, streaks, badges, and records, then saves profile.json.
     */
    fun onTimecardSaved(
        weekData: TimecardData,
        allAvailableDates: List<String>
    ) {
        viewModelScope.launch {
            try {
                profileReady.await() // never compute rewards on the blank pre-load profile
                processTimecardSave(weekData, allAvailableDates)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error processing save", e)
            }
        }
    }

    private suspend fun processTimecardSave(
        weekData: TimecardData,
        allAvailableDates: List<String>
    ) {
        val current = profile
        val cal = Calendar.getInstance()
        val isMonday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val isBefore930 = hour < 9 || (hour == 9 && minute < 30)

        // Load month weeks for best-month record calculation
        val currentMonth = weekData.weekStarting.substring(0, 7)
        val repoForLoads = repository
        val loadName = employeeName
        val (monthWeeks, recentWeeks) = withContext(Dispatchers.IO) {
            fun load(date: String): TimecardData? = repoForLoads?.loadFile(loadName, date)?.let {
                try { gson.fromJson(it, TimecardData::class.java) } catch (_: Exception) { null }
            }
            val month = mutableListOf(weekData)
            allAvailableDates.filter { it.startsWith(currentMonth) && it != weekData.weekStarting }.forEach { d -> load(d)?.let(month::add) }
            // Recent weeks for streak and badge calculation
            val recent = mutableListOf(weekData)
            allAvailableDates.filter { it != weekData.weekStarting }.take(7).forEach { d -> load(d)?.let(recent::add) }
            month to recent
        }

        // Delegate all gamification logic to the engine
        val result = GamificationEngine.processTimecardSave(
            current = current,
            employeeName = employeeDisplayName,
            weekData = weekData,
            monthWeeks = monthWeeks,
            recentWeeks = recentWeeks,
            isMonday = isMonday,
            isBefore930 = isBefore930
        )

        // Apply result and trigger UI events
        profile = result.profile
        if (result.pendingBadges.isNotEmpty()) pendingBadges = result.pendingBadges
        result.newRecordMessage?.let { newRecordMessage = it }
        if (result.pendingConfetti) pendingConfetti = true
        if (result.coinsGainedThisSave > 0) {
            recentCoinsEarned = result.coinsGainedThisSave
            recentStreakBonus = result.streakBonusCoins
            recentStreakMultiplier = result.appliedStreakMultiplier
        }

        // Write activity events for this save
        if (result.newEvents.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val repo = repository ?: return@launch
                activityEventsMutex.withLock {
                    try {
                        val existing = repo.loadEmployeeActivityEvents(employeeName)
                        val merged = (result.newEvents + existing).take(50)
                        repo.saveEmployeeActivityEvents(employeeName, merged)
                    } catch (_: Exception) {}
                }
            }
        }

        // Detect weekly challenge completions and award coins
        try {
            val repo = repository
            if (repo != null) {
                val challenges = withContext(Dispatchers.IO) { repo.loadChallenges() }
                if (challenges.isNotEmpty()) {
                    val completed = ChallengeEngine.detectCompletions(challenges, weekData, profile)
                    if (completed.isNotEmpty()) {
                        val now = java.time.Instant.now().toString()
                        val newLog = profile.challengeLog.toMutableMap()
                        var bonusCoins = 0
                        for (ch in completed) {
                            newLog["${ch.id}_${weekData.weekStarting}"] = now
                            bonusCoins += ch.reward
                        }
                        val newCoins = profile.coins + bonusCoins
                        val newAllTime = profile.allTimeCoinsEarned + bonusCoins
                        profile = profile.copy(
                            challengeLog = newLog,
                            coins = newCoins,
                            allTimeCoinsEarned = newAllTime
                        )
                        if (bonusCoins > 0 && recentCoinsEarned == null) {
                            recentCoinsEarned = bonusCoins
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        saveProfile()
    }

    private fun applyCoins(updated: PlayerProfile, coinsGain: Int) {
        val newCoins = updated.coins + coinsGain
        val newAllTimeCoins = if (coinsGain > 0) updated.allTimeCoinsEarned + coinsGain else updated.allTimeCoinsEarned
        profile = updated.copy(coins = newCoins, allTimeCoinsEarned = newAllTimeCoins)
        saveProfile()
    }

    private suspend fun loadCustomAvatar() {
        val repo = repository ?: return
        val bytes = withContext(Dispatchers.IO) {
            repo.loadEmployeeBinaryFile(employeeName, ".avatar*")
        }
        avatarImage = bytes
    }

    fun saveCustomAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            try {
                val repo = repository
                val name = employeeName
                withContext(Dispatchers.IO) { repo?.saveEmployeeBinaryFile(name, ".avatar.jpg", bytes) }
                avatarImage = bytes
                setAvatar("custom")
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Error saving custom avatar", e)
            }
        }
    }

    fun clearCustomAvatar() {
        avatarImage = null
        setAvatar(null)
    }

    fun saveBadgeCustomImage(badgeId: String, bytes: ByteArray) {
        viewModelScope.launch {
            try {
                val repo = repository
                val name = employeeName
                withContext(Dispatchers.IO) { repo?.saveEmployeeBinaryFile(name, ".badge_${badgeId}.png", bytes) }
                badgeImages = badgeImages + (badgeId to bytes)
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Error saving badge image for $badgeId", e)
            }
        }
    }

    fun clearBadgeCustomImage(badgeId: String) {
        viewModelScope.launch {
            try {
                // Save empty file to signal removal
                val repo = repository
                val name = employeeName
                withContext(Dispatchers.IO) { repo?.saveEmployeeBinaryFile(name, ".badge_${badgeId}.png", ByteArray(0)) }
                badgeImages = badgeImages - badgeId
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Error clearing badge image for $badgeId", e)
            }
        }
    }

    private suspend fun loadBadgesConfig(repo: FileRepository?) {
        if (repo == null) return
        try {
            val json = withContext(Dispatchers.IO) { repo.loadGlobalFile("badges_config.json", useCache = false) } ?: return
            val wrapper = gson.fromJson(json, CustomBadgeConfig::class.java) ?: return
            val defs = wrapper.badges.map { cfg ->
                BadgeDefinition(
                    id = cfg.id,
                    name = cfg.name,
                    description = cfg.description,
                    emoji = cfg.emoji,
                    flavorText = cfg.flavorText,
                    coinReward = cfg.coinReward,
                    repeatable = cfg.repeatable,
                    trigger = cfg.trigger,
                    threshold = cfg.threshold,
                    imagePath = cfg.imagePath
                )
            }
            if (defs.isNotEmpty()) {
                BadgeEngine.loadBadgesConfig(defs)
                // Load badge images for any badge that has an imagePath
                val images = mutableMapOf<String, ByteArray>()
                defs.forEach { def ->
                    val path = def.imagePath ?: return@forEach
                    val bytes = withContext(Dispatchers.IO) { repo.loadGlobalBinaryFile(path) }
                    if (bytes != null) images[def.id] = bytes
                }
                if (images.isNotEmpty()) badgeImages = images
            }
        } catch (e: Exception) {
            Log.d("ProfileVM", "No badges config (normal): ${e.message}")
        }
    }

    private suspend fun loadProfile() {
        if (employeeName.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                repository?.releaseLock(employeeName, "profile", deviceId)
            }
            lockRenewJob?.cancel()
        }

        val lockAcquired = withContext(Dispatchers.IO) {
            repository?.acquireLock(employeeName, "profile", deviceId) ?: true
        }
        isLockedByAnotherUser = !lockAcquired

        if (isLockedByAnotherUser) {
            return // User cannot access the profile, locked out
        }

        lockRenewJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L) // Wait 1 minute
                val inactiveMs = System.currentTimeMillis() - lastInteractionTimeMillis
                if (idleLogoutEnabled && inactiveMs > 300_000L) { // 5 minutes
                    // TimecardApp observes the flag and performs the logout (which resets this VM).
                    // Calling logout() here would clear the flag before the UI could see it.
                    triggerAutoLogout = true
                    break
                } else {
                    withContext(Dispatchers.IO) {
                        repository?.renewLock(employeeName, "profile", deviceId)
                    }
                }
            }
        }

        run {
            val loaded = loadProfileFileWithRetry() ?: run {
                // profile.json is there but unreadable: keep it untouched (saves stay blocked).
                profileLoadFailed = true
                Log.e("ProfileVM", "profile.json for $employeeName could not be read; not overwriting it")
                return
            }
            profileLoadFailed = false

            profile = loaded
            loadedProfileFor = employeeName

            // Check if today's streak is at risk (active streak but no hours logged yet today)
            streakAtRisk = false
            if (loaded.streaks.currentDaily > 0) {
                try {
                    val today = java.time.LocalDate.now()
                    val dayOfWeek = today.dayOfWeek
                    val weekStart = today.minusDays((dayOfWeek.value - 1).toLong())
                    val dayKey = when (dayOfWeek) {
                        java.time.DayOfWeek.MONDAY    -> "mon"
                        java.time.DayOfWeek.TUESDAY   -> "tue"
                        java.time.DayOfWeek.WEDNESDAY -> "wed"
                        java.time.DayOfWeek.THURSDAY  -> "thu"
                        java.time.DayOfWeek.FRIDAY    -> "fri"
                        java.time.DayOfWeek.SATURDAY  -> "sat"
                        else -> null  // Sunday — no work expected
                    }
                    if (dayKey != null) {
                        val weekJson = withContext(Dispatchers.IO) {
                            repository?.loadFile(employeeName, weekStart.toString())
                        }
                        val weekData = weekJson?.let {
                            try { gson.fromJson(it, TimecardData::class.java) } catch (_: Exception) { null }
                        }
                        val todayHours = weekData?.rows
                            ?.sumOf { row -> row.getHours(dayKey).toDoubleOrNull() ?: 0.0 } ?: 0.0
                        streakAtRisk = todayHours == 0.0
                    }
                } catch (_: Exception) {}
            }

            // Adopt any dashboard-uploaded avatar staged as avatar_pending.jpg.
            // The backend writes avatar_pending.jpg instead of .avatar.jpg directly,
            // so that only the tablet (sole writer of its own folder) finalizes the binary.
            val repo = repository
            val pending = withContext(Dispatchers.IO) {
                repo?.loadEmployeeBinaryFile(employeeName, "avatar_pending.jpg")
            }
            if (pending != null && pending.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    repo?.saveEmployeeBinaryFile(employeeName, ".avatar.jpg", pending)
                    // Consume the pending file so it isn't re-adopted on next login
                    repo?.saveEmployeeBinaryFile(employeeName, "avatar_pending.jpg", ByteArray(0))
                }
                avatarImage = pending
                if (profile.avatar != "custom") {
                    profile = profile.copy(avatar = "custom")
                    saveProfile()
                }
            } else if (profile.avatar == "custom") {
                loadCustomAvatar()
            } else {
                // File-presence fallback: detect custom avatar even when profile.avatar
                // is stale or null (e.g. after a backend-only upload on a previous session).
                val bytes = withContext(Dispatchers.IO) {
                    repo?.loadEmployeeBinaryFile(employeeName, ".avatar*")
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    avatarImage = bytes
                    profile = profile.copy(avatar = "custom")
                    saveProfile()
                }
            }

            // Load any per-employee custom badge images (overrides config imagePath images)
            val customBadgeImages = mutableMapOf<String, ByteArray>()
            withContext(Dispatchers.IO) {
                BadgeEngine.ALL_BADGES.forEach { def ->
                    val bytes = repo?.loadEmployeeBinaryFile(employeeName, ".badge_${def.id}.png")
                    if (bytes != null && bytes.isNotEmpty()) {
                        customBadgeImages[def.id] = bytes
                    }
                }
            }
            if (customBadgeImages.isNotEmpty()) {
                badgeImages = badgeImages + customBadgeImages
            }

            // Run one-time historical backfill if it hasn't been done yet.
            // Must happen BEFORE applyGrantedBadges so grants aren't overwritten.
            if (!loaded.runningStats.backfillComplete) {
                runBackfill()
            }

            applyGrantedBadges()
            profileReady.complete(Unit)
        }
    }

    /**
     * Reads and parses profile.json. A missing file gives a fresh profile (new employee); a file that
     * exists but can't be read or parsed is retried a few times and then gives null, so the caller
     * never saves a blank profile over real data.
     */
    private suspend fun loadProfileFileWithRetry(attempts: Int = 4): PlayerProfile? {
        val repo = repository
        val name = employeeName
        repeat(attempts) { attempt ->
            val json = withContext(Dispatchers.IO) { repo?.loadGenericJSON(name, "profile.json", useCache = false) }
            if (json != null) {
                parseProfileJson(json)?.let { return it }
            } else {
                when (withContext(Dispatchers.IO) { repo?.employeeFileExists(name, "profile.json") }) {
                    false -> return PlayerProfile()
                    else -> Unit // there but unreadable, or storage couldn't be checked: retry
                }
            }
            if (attempt < attempts - 1) kotlinx.coroutines.delay(1500L * (attempt + 1))
            if (employeeName != name) return null // logged out or switched meanwhile
        }
        return null
    }

    /** profile.json to PlayerProfile (migrating the old badge array format); null when it doesn't parse. */
    internal fun parseProfileJson(json: String): PlayerProfile? = try {
        val jsonObj = gson.fromJson(json, JsonObject::class.java)
        val badgesElem = jsonObj?.get("badges")
        if (jsonObj != null && badgesElem != null && badgesElem.isJsonArray) {
            val oldList = gson.fromJson(badgesElem, Array<String>::class.java) ?: emptyArray()
            val newMap = JsonObject()
            oldList.forEach { newMap.addProperty(it, 1) }
            jsonObj.add("badges", newMap)
        }
        if (jsonObj == null) null else gson.fromJson(jsonObj, PlayerProfile::class.java)
    } catch (e: Exception) {
        Log.e("ProfileVM", "profile.json did not parse", e)
        null
    }

    /**
     * One-time scan of ALL historical timecard data for this employee.
     * Awards XP at the 85% timeliness tier (next-day rate) for all past work,
     * computes all cumulative stats, badges, streaks, and records from scratch.
     * No popup notifications are fired — this runs silently in the background.
     * Sets backfillComplete = true so it never runs again.
     */
    private suspend fun runBackfill() {
        val repo = repository ?: return
        isBackfilling = true
        try {
            val datesFor = employeeName
            val allDates = withContext(Dispatchers.IO) { repo.getAvailableDates(datesFor) }
            if (allDates.isEmpty()) {
                profile = profile.copy(
                    runningStats = profile.runningStats.copy(backfillComplete = true)
                )
                saveProfile()
                return
            }

            // Load all timecards, oldest first for chronological processing
            val allWeeks = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                allDates.sorted().map { date ->
                    async {
                        val json = repo.loadFile(employeeName, date)
                        if (json != null) {
                            try { gson.fromJson(json, TimecardData::class.java) }
                            catch (_: Exception) { null }
                        } else null
                    }
                }.awaitAll().filterNotNull().toMutableList()
            }

            profile = GamificationEngine.runBackfill(profile, allWeeks)
            Log.i("ProfileVM", "Backfill complete: ${profile.coins} coins, ${profile.badges.size} badges, ${allWeeks.size} weeks processed")
            saveProfile()
        } catch (e: Exception) {
            Log.e("ProfileVM", "Backfill failed", e)
            // Mark complete anyway to prevent infinite retry loops
            profile = profile.copy(
                runningStats = profile.runningStats.copy(backfillComplete = true)
            )
            saveProfile()
        } finally {
            isBackfilling = false
        }
    }

    /**
     * Reads [employeeName]/granted_badges.json from the server and diffs against
     * the previously tracked `grantedBadges`. It automatically adds XP for new grants
     * and revokes XP / the badge entirely if an admin removes it.
     */
    private suspend fun applyGrantedBadges() {
        val repo = repository ?: return
        try {
            val json = withContext(Dispatchers.IO) {
                repo.loadGenericJSON(employeeName, "granted_badges.json", useCache = false)
            } ?: return
            val config = gson.fromJson(json, GrantedBadgeConfig::class.java) ?: return
            
            val serverGrantedIds = config.badges.map { it.id }
            val serverGrantedMap = config.badges.associateBy { it.id } // Rich lookup by ID
            val knownGranted = profile.grantedBadges

            // "Truly new" = server has it, we haven't tracked it, AND it's not already
            // in profile.badges. Badges already in profile.badges with an empty grantedBadges
            // are legacy entries from before this tracking field existed — they need their
            // tracking updated but must NOT be double-awarded XP.
            val newlyGranted = serverGrantedIds.filter { id ->
                !knownGranted.contains(id) && (profile.badges[id] ?: 0) == 0
            }
            val revoked = knownGranted.filter { b -> !serverGrantedIds.contains(b) }

            // If the only "change" is untracked legacy badges, just update the list and return.
            if (newlyGranted.isEmpty() && revoked.isEmpty()) {
                val untracked = serverGrantedIds.filter { !knownGranted.contains(it) }
                if (untracked.isNotEmpty()) {
                    profile = profile.copy(grantedBadges = serverGrantedIds)
                    saveProfile()
                }
                return
            }

            val result = applyGrantChanges(profile.badges, profile.grantedBadgeCoins, newlyGranted, revoked) { id ->
                val coinsFromFile = serverGrantedMap[id]?.coinReward ?: 0
                if (coinsFromFile > 0) coinsFromFile else (BadgeEngine.getDefinition(id)?.coinReward ?: 0)
            }
            val updatedBadges = result.badges
            val netCoinDelta = result.coinDelta

            val newCoins = (profile.coins + netCoinDelta).coerceAtLeast(0)
            val newAllTimeCoins = if (netCoinDelta > 0) profile.allTimeCoinsEarned + netCoinDelta else profile.allTimeCoinsEarned
            profile = profile.copy(
                badges = updatedBadges,
                grantedBadges = serverGrantedIds, // Update our known state
                grantedBadgeCoins = result.grantCoins,
                coins = newCoins,
                allTimeCoinsEarned = newAllTimeCoins
            )
            saveProfile()

            // Write activity events for newly granted badges
            if (newlyGranted.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    activityEventsMutex.withLock {
                        try {
                            val now = java.time.Instant.now().toString()
                            val newEvents = newlyGranted.mapNotNull { id ->
                                val entry = serverGrantedMap[id] ?: return@mapNotNull null
                                com.example.timecard.data.model.ActivityEvent(
                                    type = "badge_granted",
                                    employeeName = employeeDisplayName,
                                    displayName = profile.displayName ?: employeeDisplayName,
                                    detail = entry.name,
                                    detailIcon = entry.emoji,
                                    timestamp = now
                                )
                            }
                            if (newEvents.isNotEmpty()) {
                                val existing = repo.loadEmployeeActivityEvents(employeeName)
                                val merged = (newEvents + existing).take(50)
                                repo.saveEmployeeActivityEvents(employeeName, merged)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("ProfileVM", "No granted badges file (normal): ${e.message}")
        }
    }

    fun saveProfile() {
        // Only ever write the profile we actually loaded for this employee (never the blank
        // pre-load or failed-load fallback, which would wipe coins, inventory and badges).
        if (employeeName.isEmpty() || loadedProfileFor != employeeName || profileLoadFailed) {
            Log.w("ProfileVM", "Skipped profile save: profile not loaded for '$employeeName'")
            return
        }
        val repo = repository
        val name = employeeName
        viewModelScope.launch {
            try {
                val json = gson.toJson(profile)
                withContext(Dispatchers.IO) {
                    repo?.saveGenericJSON(name, "profile.json", json)
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Save failed", e)
            }
        }
    }

    // --- Admin config shapes ---
    private data class GrantedBadgeEntry(
        val id: String = "",
        val name: String = "",
        val emoji: String = "🏅",
        val description: String = "",
        val coinReward: Int = 100
    )
    private data class GrantedBadgeConfig(val badges: List<GrantedBadgeEntry> = emptyList())
    private data class CustomBadgeConfig(val badges: List<CustomBadgeDef> = emptyList())
    private data class CustomBadgeDef(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val emoji: String = "🏅",
        val flavorText: String = "",
        val coinReward: Int = 100,
        val repeatable: Boolean = false,
        val trigger: String = "",
        val threshold: Double = 0.0,
        val imagePath: String? = null
    )
}
