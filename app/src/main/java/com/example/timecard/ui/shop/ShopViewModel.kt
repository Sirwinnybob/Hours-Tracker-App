package com.example.timecard.ui.shop

import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.Employee
import com.example.timecard.data.model.LimitedPurchaseClaim
import com.example.timecard.data.model.ShopItem
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.Alert
import com.google.gson.Gson
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class EmployeeRecipient(val folderName: String, val displayName: String?)

class ShopViewModel : ViewModel() {

    var items by mutableStateOf<List<ShopItem>>(emptyList())
        private set

    /** Special items the current user hasn't seen yet — drives the timesheet banner. */
    var newSpecialItems by mutableStateOf<List<ShopItem>>(emptyList())
        private set

    /** Image bytes for items that have an imageFile set: itemId → PNG/JPG bytes. */
    var itemImages by mutableStateOf<Map<String, ByteArray>>(emptyMap())
        private set

    /** Accent key being previewed (null = no active preview). Drives live theme override. */
    var previewAccentKey: String? by mutableStateOf(null)
        private set

    /** Item ID whose trial is currently active. */
    var previewItemId: String? by mutableStateOf(null)
        private set

    /** Wall-clock ms when the current preview expires (for countdown UI). */
    var previewExpiresAtMs: Long? by mutableStateOf(null)
        private set

    var recipients by mutableStateOf<List<EmployeeRecipient>>(emptyList())
        private set

    /** Pending limited purchase claims: itemId → claim. */
    var pendingLimitedClaims by mutableStateOf<Map<String, LimitedPurchaseClaim>>(emptyMap())
        private set

    /** Message shown when a limited claim is approved or denied. */
    var limitedClaimResult by mutableStateOf<String?>(null)
        private set

    private var profileViewModel: ProfileViewModel? = null
    private var repository: FileRepository? = null
    private var claimPollJob: Job? = null

    val userCoins: Int
        get() = profileViewModel?.profile?.coins ?: 0

    fun initialize(repo: FileRepository?, profileVM: ProfileViewModel) {
        this.repository = repo
        this.profileViewModel = profileVM
        loadCatalog(markSeenOnLoad = false)
    }

    /**
     * Reloads the catalog from disk (no cache) and marks all special items as seen.
     * Called when the user opens the shop modal — they are now seeing the featured items.
     */
    fun reloadAndMarkSeen() {
        loadCatalog(markSeenOnLoad = true)
    }

    /**
     * Starts a 30-second live theme preview for the given item.
     * Marks the trial as used immediately (one-time only, persisted to profile.json).
     * After 30s the preview is automatically cleared and the user's real theme restores.
     */
    fun tryTheme(itemId: String, accentKey: String) {
        profileViewModel?.markThemeTried(itemId)
        val expiresAt = System.currentTimeMillis() + 30_000L
        previewAccentKey   = accentKey
        previewItemId      = itemId
        previewExpiresAtMs = expiresAt
        viewModelScope.launch {
            delay(30_000L)
            previewAccentKey   = null
            previewItemId      = null
            previewExpiresAtMs = null
        }
    }

    private fun loadCatalog(markSeenOnLoad: Boolean) {
        val repo = repository ?: return
        val currentEmployee = profileViewModel?.employeeName ?: ""

        val isWinston = currentEmployee.equals("Winston Ferguson", ignoreCase = true)

        viewModelScope.launch {
            val loadedItems = withContext(Dispatchers.IO) {
                if (isWinston) repo.loadFullShopCatalog() else repo.loadShopCatalog()
            }

            // Sort: special (featured) items first, then alphabetical within each group
            items = loadedItems.sortedWith(
                compareByDescending<ShopItem> { it.isSpecial }
                    .thenBy { it.category }
                    .thenBy { it.title }
            )

            // Compute which special items the user hasn't seen yet
            val seenIds = profileViewModel?.profile?.seenSpecialItems ?: emptyList()
            newSpecialItems = items.filter { it.isSpecial && !seenIds.contains(it.id) }

            if (markSeenOnLoad) {
                val specialIds = items.filter { it.isSpecial }.map { it.id }
                if (specialIds.isNotEmpty()) {
                    profileViewModel?.markSpecialItemsSeen(specialIds)
                    newSpecialItems = emptyList()
                }
            }

            // Load images for items that have an imageFile path set
            val loadedImages = withContext(Dispatchers.IO) {
                val map = mutableMapOf<String, ByteArray>()
                for (item in items) {
                    val imagePath = item.imageFile ?: continue
                    try {
                        val bytes = repo.loadGlobalBinaryFile(imagePath)
                        if (bytes != null) map[item.id] = bytes
                    } catch (e: Exception) {
                        Log.e("ShopVM", "Failed to load image for ${item.id} at $imagePath", e)
                    }
                }
                map
            }
            itemImages = loadedImages

            val loadedRecipients = withContext(Dispatchers.IO) {
                val list = mutableListOf<EmployeeRecipient>()
                val gson = Gson()
                try {
                    // Load valid employee folders from employees.json, filtering out excluded entries
                    val employeeJson = repo.loadEmployeeList()
                    val validFolders: Set<String> = if (employeeJson != null) {
                        try {
                            val type = object : com.google.gson.reflect.TypeToken<List<Employee>>() {}.type
                            val employees: List<Employee> = gson.fromJson(employeeJson, type)
                            employees.filter { !it.excluded || it.name.equals("Winston Ferguson", ignoreCase = true) }.map { it.name }.toSet()
                        } catch (e: Exception) { emptySet() }
                    } else emptySet()

                    for (folder in repo.listEmployeeFolders()) {
                        if (folder.equals(currentEmployee, ignoreCase = true)) continue
                        if (validFolders.isNotEmpty() && !validFolders.any { it.equals(folder, ignoreCase = true) }) continue

                        val profileJson = repo.loadGenericJSON(folder, "profile.json", useCache = true)
                        var dName: String? = null
                        if (profileJson != null) {
                            try {
                                val prof = gson.fromJson(profileJson, PlayerProfile::class.java)
                                dName = prof.displayName
                            } catch (e: Exception) {}
                        }
                        list.add(EmployeeRecipient(folderName = folder, displayName = dName))
                    }
                } catch(e: Exception) {}
                list.sortedBy { it.displayName?.lowercase() ?: it.folderName.lowercase() }
            }
            recipients = loadedRecipients
        }
    }

    fun purchaseItem(id: String) {
        val item = items.find { it.id == id } ?: return
        profileViewModel?.processPurchase(item.id, item.title, item.price)
    }

    /**
     * Submits a limited purchase claim without deducting coins.
     * Writes a claim JSON to limited_purchases/ and sets file read-only.
     * Coins are only deducted after admin approval.
     */
    fun purchaseLimitedItem(id: String) {
        val item = items.find { it.id == id } ?: return
        val vm = profileViewModel ?: return
        val repo = repository ?: return
        if (vm.profile.coins < item.price) return
        if (vm.profile.inventory.contains(item.id)) return
        if (pendingLimitedClaims.containsKey(item.id)) return

        val claim = LimitedPurchaseClaim(
            claimId = UUID.randomUUID().toString(),
            itemId = item.id,
            itemTitle = item.title,
            price = item.price,
            employeeName = vm.employeeName,
            displayName = vm.profile.displayName,
            claimedAt = Instant.now().toString(),
            deviceId = "${Build.MANUFACTURER} ${Build.MODEL}"
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "${claim.claimId}.json"
                val json = Gson().toJson(claim)
                val result = repo.saveGlobalDir("limited_purchases", filename, json)
                if (result == "SUCCESS") {
                    Log.d("ShopVM", "Limited claim written: limited_purchases/$filename")
                    withContext(Dispatchers.Main) {
                        pendingLimitedClaims = pendingLimitedClaims + (item.id to claim)
                    }
                    // Start polling if not already running
                    startClaimPolling()
                } else {
                    Log.e("ShopVM", "Failed to write limited claim: $result")
                }
            } catch (e: Exception) {
                Log.e("ShopVM", "Error writing limited purchase claim", e)
            }
        }
    }

    fun dismissLimitedClaimResult() { limitedClaimResult = null }

    /** Start polling for claim approvals/denials every 10 seconds. */
    private fun startClaimPolling() {
        if (claimPollJob?.isActive == true) return
        claimPollJob = viewModelScope.launch {
            while (pendingLimitedClaims.isNotEmpty()) {
                delay(10_000L)
                checkPendingClaims()
            }
            claimPollJob = null
        }
    }

    private suspend fun checkPendingClaims() {
        val repo = repository ?: return
        val vm = profileViewModel ?: return
        val current = pendingLimitedClaims.toMap()
        if (current.isEmpty()) return

        val filenames = current.values.map { "${it.claimId}.json" }
        val loadedFiles = withContext(Dispatchers.IO) {
            repo.loadGlobalDirFiles("limited_purchases", filenames)
        }

        for ((itemId, claim) in current) {
            try {
                val json = loadedFiles["${claim.claimId}.json"] ?: continue

                val updated = Gson().fromJson(json, LimitedPurchaseClaim::class.java)
                when (updated.approved) {
                    true -> {
                        // Approved! Deduct coins and add to inventory
                        vm.processPurchase(claim.itemId, claim.itemTitle, claim.price)
                        pendingLimitedClaims = pendingLimitedClaims - itemId
                        limitedClaimResult = "✅ Purchase approved: ${claim.itemTitle}"
                        Log.d("ShopVM", "Limited claim approved: ${claim.claimId}")
                    }
                    false -> {
                        // Denied — no coins lost
                        pendingLimitedClaims = pendingLimitedClaims - itemId
                        limitedClaimResult = "❌ Purchase denied: ${claim.itemTitle}"
                        Log.d("ShopVM", "Limited claim denied: ${claim.claimId}")
                    }
                    null -> { /* Still pending, no action */ }
                }
            } catch (e: Exception) {
                Log.e("ShopVM", "Error checking claim ${claim.claimId}", e)
            }
        }
    }

    fun sendNote(recipientFolder: String, message: String, cost: Int) {
        val vm = profileViewModel ?: return
        val repo = repository ?: return
        val myName = vm.employeeName
        val myDisplayName = vm.profile.displayName ?: myName
        val gson = Gson()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (vm.spendCoins(cost)) {
                    val noteId = UUID.randomUUID().toString()
                    val newNote = Alert(
                        id = noteId,
                        message = message,
                        sentAt = Instant.now().toString(),
                        sentBy = myName,
                        senderDisplayName = myDisplayName,
                        senderFolder = myName,
                        isAnonymous = false
                    )
                    val result = repo.saveInDir(recipientFolder, "notes", "$noteId.json", gson.toJson(newNote))
                    Log.d("ShopVM", "Note saved to $recipientFolder/notes/$noteId.json: $result")
                }
            } catch (e: Exception) {
                Log.e("ShopVM", "Error sending note", e)
            }
        }
    }

    fun sendAnonymousNote(recipientFolder: String, message: String, cost: Int) {
        val vm = profileViewModel ?: return
        val repo = repository ?: return
        val myName = vm.employeeName
        val gson = Gson()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (vm.spendCoins(cost)) {
                    val noteId = UUID.randomUUID().toString()
                    val newNote = Alert(
                        id = noteId,
                        message = message,
                        sentAt = Instant.now().toString(),
                        sentBy = "Anonymous",
                        senderDisplayName = "Anonymous",
                        senderFolder = myName,
                        isAnonymous = true
                    )
                    val result = repo.saveInDir(recipientFolder, "notes", "$noteId.json", gson.toJson(newNote))
                    Log.d("ShopVM", "Anonymous note saved to $recipientFolder/notes/$noteId.json: $result")
                }
            } catch (e: Exception) {
                Log.e("ShopVM", "Error sending anonymous note", e)
            }
        }
    }
}
