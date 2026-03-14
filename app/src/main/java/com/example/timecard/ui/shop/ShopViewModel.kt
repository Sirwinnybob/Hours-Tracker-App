package com.example.timecard.ui.shop

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.ShopItem
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.Alert
import com.google.gson.Gson
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class EmployeeRecipient(val folderName: String, val displayName: String?)

class ShopViewModel : ViewModel() {

    var items by mutableStateOf<List<ShopItem>>(emptyList())
        private set

    var recipients by mutableStateOf<List<EmployeeRecipient>>(emptyList())
        private set

    /** Loaded image bytes keyed by item ID. */
    var itemImages by mutableStateOf<Map<String, ByteArray>>(emptyMap())
        private set

    /** How many employees own each item (for limited-quantity tracking). */
    var soldCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Special items the current user has not yet been notified about. */
    var newSpecialItems by mutableStateOf<List<ShopItem>>(emptyList())
        private set

    private var profileViewModel: ProfileViewModel? = null
    private var repository: FileRepository? = null

    val userCoins: Int
        get() = profileViewModel?.profile?.coins ?: 0

    fun initialize(repo: FileRepository?, profileVM: ProfileViewModel) {
        this.repository = repo
        this.profileViewModel = profileVM
        loadCatalog()
    }

    /** Re-reads the catalog from disk every time the shop opens. */
    fun reloadCatalog() = loadCatalog()

    private fun loadCatalog() {
        val repo = repository ?: return
        val currentEmployee = profileViewModel?.employeeName ?: ""

        viewModelScope.launch {
            val loadedItems = withContext(Dispatchers.IO) {
                repo.loadShopCatalog()
            }
            items = loadedItems

            // Load item images and count stock + recipients in one employee-folder pass
            val (loadedImages, counts, loadedRecipients) = withContext(Dispatchers.IO) {
                val images = mutableMapOf<String, ByteArray>()
                val stockCounts = mutableMapOf<String, Int>()
                val list = mutableListOf<EmployeeRecipient>()
                val gson = Gson()

                // Load images for items that have imageFile set
                for (item in loadedItems) {
                    val path = item.imageFile ?: continue
                    try {
                        val bytes = repo.loadGlobalBinaryFile(path)
                        if (bytes != null) images[item.id] = bytes
                    } catch (e: Exception) {
                        Log.w("ShopVM", "Failed to load image for ${item.id}: $path", e)
                    }
                }

                // Iterate employee folders: build recipients list + count ownership
                val limitedItemIds = loadedItems.filter { it.quantity != null }.map { it.id }.toSet()
                try {
                    for (folder in repo.listEmployeeFolders()) {
                        val profileJson = repo.loadGenericJSON(folder, "profile.json", useCache = true)
                        var dName: String? = null
                        if (profileJson != null) {
                            try {
                                val prof = gson.fromJson(profileJson, PlayerProfile::class.java)
                                dName = prof.displayName
                                // Count ownership of limited items
                                for (id in limitedItemIds) {
                                    if (prof.inventory.contains(id)) {
                                        stockCounts[id] = (stockCounts[id] ?: 0) + 1
                                    }
                                }
                            } catch (e: Exception) { /* malformed profile */ }
                        }
                        if (!folder.equals(currentEmployee, ignoreCase = true)) {
                            list.add(EmployeeRecipient(folderName = folder, displayName = dName))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ShopVM", "Error loading employee folders", e)
                }

                Triple(images, stockCounts, list)
            }

            itemImages = loadedImages
            soldCounts = counts
            recipients = loadedRecipients.sortedBy { it.displayName?.lowercase() ?: it.folderName.lowercase() }

            // Determine which special items are new (not yet seen by this user)
            val seen = profileViewModel?.profile?.seenSpecialItems ?: emptyList()
            newSpecialItems = items.filter { it.isSpecial && !seen.contains(it.id) }
        }
    }

    fun markSpecialItemsSeen() {
        val ids = newSpecialItems.map { it.id }
        if (ids.isNotEmpty()) {
            profileViewModel?.markSpecialItemsSeen(ids)
            newSpecialItems = emptyList()
        }
    }

    fun purchaseItem(id: String) {
        val item = items.find { it.id == id } ?: return
        profileViewModel?.processPurchase(item.id, item.price, item.title)
    }

    fun sendNote(recipientFolder: String, message: String, cost: Int) {
        val vm = profileViewModel ?: return
        val repo = repository ?: return
        val myName = vm.employeeName
        val myDisplayName = vm.profile.displayName ?: myName
        val gson = Gson()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (vm.spendCoins(cost, "consumable_send_note", "Send a Note")) {
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
                if (vm.spendCoins(cost, "consumable_send_anonymous_note", "Send Anonymous Note")) {
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
