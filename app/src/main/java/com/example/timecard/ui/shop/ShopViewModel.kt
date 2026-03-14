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

    private var profileViewModel: ProfileViewModel? = null
    private var repository: FileRepository? = null

    val userCoins: Int
        get() = profileViewModel?.profile?.coins ?: 0

    fun initialize(repo: FileRepository?, profileVM: ProfileViewModel) {
        this.repository = repo
        this.profileViewModel = profileVM
        loadCatalog()
    }

    private fun loadCatalog() {
        val repo = repository ?: return
        val currentEmployee = profileViewModel?.employeeName ?: ""
        
        viewModelScope.launch {
            val loadedItems = withContext(Dispatchers.IO) {
                repo.loadShopCatalog()
            }
            items = loadedItems

            val loadedRecipients = withContext(Dispatchers.IO) {
                val list = mutableListOf<EmployeeRecipient>()
                val gson = Gson()
                try {
                    for (folder in repo.listEmployeeFolders()) {
                        if (folder.equals(currentEmployee, ignoreCase = true)) continue
                        
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
        profileViewModel?.processPurchase(item.id, item.price)
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
                    // Write to [recipient]/notes/[uuid].json — unique file per note, no conflicts
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
                    // Write to [recipient]/notes/[uuid].json — unique file per note, no conflicts
                    val result = repo.saveInDir(recipientFolder, "notes", "$noteId.json", gson.toJson(newNote))
                    Log.d("ShopVM", "Anonymous note saved to $recipientFolder/notes/$noteId.json: $result")
                }
            } catch (e: Exception) {
                Log.e("ShopVM", "Error sending anonymous note", e)
            }
        }
    }
}

