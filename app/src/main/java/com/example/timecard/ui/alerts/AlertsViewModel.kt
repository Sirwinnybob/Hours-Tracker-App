package com.example.timecard.ui.alerts

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.Acknowledgement
import com.example.timecard.data.model.AcksFile
import com.example.timecard.data.model.Alert
import com.example.timecard.data.model.AlertsFile
import com.example.timecard.data.repository.FileRepository
import com.google.gson.Gson
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertsViewModel : ViewModel() {

    var showAlertModal by mutableStateOf(false)
        private set
    var alertQueue by mutableStateOf<List<Alert>>(emptyList())
        private set
    var alertQueueIndex by mutableIntStateOf(0)
        private set
    var alertResponse by mutableStateOf("")

    private var permissionsGrantedToShow = false

    // Past alerts
    var allAlerts by mutableStateOf<List<MergedAlert>>(emptyList())
        private set

    private var employeeName = ""
    private var repository: FileRepository? = null
    private val gson = Gson()

    data class MergedAlert(
        val id: String,
        val message: String,
        val sentAt: String,
        val sentBy: String?,
        val acknowledged: Boolean,
        val acknowledgedAt: String?,
        val response: String?
    )

    val currentAlert: Alert?
        get() = alertQueue.getOrNull(alertQueueIndex)

    fun initialize(name: String, repo: FileRepository?) {
        employeeName = name
        repository = repo
        refreshAlerts()
    }

    var onAcknowledged: (() -> Unit)? = null

    fun acknowledgeCurrentAlert() {
        val current = currentAlert ?: return
        val repo = repository ?: return
        
        val responseText = alertResponse.trim().ifEmpty { null }
        
        // Read our profile to get display name
        val myProfileRaw = repo.loadGenericJSON(employeeName, "profile.json", useCache = true)
        var myDisplayName = employeeName
        if (myProfileRaw != null) {
            try {
                val prof = gson.fromJson(myProfileRaw, com.example.timecard.data.model.PlayerProfile::class.java)
                if (prof.displayName != null) myDisplayName = prof.displayName
            } catch(e: Exception) {}
        }

        val ackRecord = Acknowledgement(
            id = current.id,
            acknowledgedAt = Instant.now().toString(),
            response = responseText,
            responderName = myDisplayName,
            originalMessage = current.message
        )

        viewModelScope.launch(Dispatchers.IO) {
            // A. Save locally (Seen Flag)
            saveAcknowledgementLocally(ackRecord)
            
            // B. If it's a note with a sender, save reply in SENDER'S folder
            val senderFold = current.senderFolder
            if (senderFold != null && responseText != null) {
                saveAcknowledgementRemote(senderFold, ackRecord)
            }

            withContext(Dispatchers.Main) {
                onAcknowledged?.invoke()
                alertResponse = ""
                // Refresh local state immediately
                refreshAlerts()
                
                alertQueueIndex++
                if (alertQueueIndex >= alertQueue.size) {
                    showAlertModal = false
                    alertQueue = emptyList()
                    alertQueueIndex = 0
                }
            }
        }
    }

    fun closeAlertModal() {
        showAlertModal = false
        alertQueue = emptyList()
        alertQueueIndex = 0
    }

    fun loadPastAlerts() {
        val repo = repository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawAlerts = repo.loadGenericJSON(employeeName, "alerts.json")
                val serverDataArr = if (rawAlerts != null) try { gson.fromJson(rawAlerts, AlertsFile::class.java).alerts } catch(e: Exception) { emptyList() } else emptyList()

                // Legacy notes.json (backward compat)
                val rawLegacyNotes = repo.loadGenericJSON(employeeName, "notes.json")
                val legacyNotes = if (rawLegacyNotes != null) try { gson.fromJson(rawLegacyNotes, AlertsFile::class.java).alerts } catch(e: Exception) { emptyList() } else emptyList()

                // Per-file notes from notes/ directory
                val perFileNotes = repo.listFilesInDir(employeeName, "notes").mapNotNull { filename ->
                    try {
                        val raw = repo.loadFromDir(employeeName, "notes", filename) ?: return@mapNotNull null
                        gson.fromJson(raw, Alert::class.java)
                    } catch (e: Exception) { null }
                }

                val incoming = serverDataArr + legacyNotes + perFileNotes

                val rawAcks = repo.loadGenericJSON(employeeName, "acknowledgements.json")
                val localAcks = if (rawAcks != null) try { gson.fromJson(rawAcks, AcksFile::class.java).acknowledgements } catch(e: Exception) { emptyList() } else emptyList()

                // Remote ack replies written by others into remote_acks/
                val remoteAcks = repo.listFilesInDir(employeeName, "remote_acks").mapNotNull { filename ->
                    try {
                        val raw = repo.loadFromDir(employeeName, "remote_acks", filename) ?: return@mapNotNull null
                        gson.fromJson(raw, Acknowledgement::class.java)
                    } catch (e: Exception) { null }
                }

                val allAcks = (localAcks + remoteAcks).distinctBy { it.id }
                val ackMap = allAcks.associateBy { it.id }
                val incomingIds = incoming.map { it.id }.toSet()
                val localAckIds = localAcks.map { it.id }.toSet()

                val merged = incoming.map { alert ->
                    val ack = ackMap[alert.id]
                    MergedAlert(
                        id = alert.id,
                        message = alert.message,
                        sentAt = alert.sentAt,
                        sentBy = alert.sentBy,
                        acknowledged = ack != null,
                        acknowledgedAt = ack?.acknowledgedAt,
                        response = ack?.response
                    )
                }.toMutableList()

                // Add replies received (remote acks not in incoming = replies to notes I sent)
                remoteAcks.filter { it.id !in incomingIds }.forEach { ack ->
                    merged.add(MergedAlert(
                        id = ack.id,
                        message = ack.originalMessage ?: "Note sent by you",
                        sentAt = ack.acknowledgedAt,
                        sentBy = ack.responderName ?: "System",
                        acknowledged = true,
                        acknowledgedAt = ack.acknowledgedAt,
                        response = ack.response
                    ))
                }

                // Add persistence for deleted alerts: If we have a local ACK for an alert that's gone from 'incoming'
                // and it wasn't a reply to us (already handled above), keep it in history.
                val handledIds = merged.map { it.id }.toSet()
                localAcks.filter { it.id !in handledIds }.forEach { ack ->
                    merged.add(MergedAlert(
                        id = ack.id,
                        message = ack.originalMessage ?: "(Original message unavailable)",
                        sentAt = ack.acknowledgedAt, // Fallback
                        sentBy = null,
                        acknowledged = true,
                        acknowledgedAt = ack.acknowledgedAt,
                        response = ack.response
                    ))
                }

                withContext(Dispatchers.Main) {
                    allAlerts = merged.distinctBy { it.id }.sortedByDescending { it.sentAt }
                }
            } catch (e: Exception) {
                Log.e("AlertsVM", "Past alerts error", e)
            }
        }
    }

    fun logout() {
        showAlertModal = false
        permissionsGrantedToShow = false
        alertQueue = emptyList()
        alertQueueIndex = 0
        alertResponse = ""
        allAlerts = emptyList()
        employeeName = ""
        repository = null
    }

    fun refreshAlerts() {
        val repo = repository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("AlertsVM", "Refreshing alerts for $employeeName...")

                // 1. Load alerts.json (server-written only, tablet never writes this)
                val rawAlerts = repo.loadGenericJSON(employeeName, "alerts.json", useCache = false)
                val serverAlerts = if (rawAlerts != null) {
                    try { gson.fromJson(rawAlerts, AlertsFile::class.java).alerts } catch(e: Exception) { emptyList() }
                } else emptyList()

                // 2a. Legacy notes.json (backward compat — read-only going forward)
                val rawLegacyNotes = repo.loadGenericJSON(employeeName, "notes.json", useCache = false)
                val legacyNotes = if (rawLegacyNotes != null) {
                    try { gson.fromJson(rawLegacyNotes, AlertsFile::class.java).alerts } catch(e: Exception) { emptyList() }
                } else emptyList()

                // 2b. Per-file notes from notes/ directory (each note is its own UUID-named file)
                val perFileNotes = repo.listFilesInDir(employeeName, "notes").mapNotNull { filename ->
                    try {
                        val raw = repo.loadFromDir(employeeName, "notes", filename) ?: return@mapNotNull null
                        gson.fromJson(raw, Alert::class.java)
                    } catch (e: Exception) { null }
                }

                val peerNotes = legacyNotes + perFileNotes

                // 3. Load own acknowledgements.json (written only by this tablet)
                val rawAcks = repo.loadGenericJSON(employeeName, "acknowledgements.json", useCache = false)
                val localAcks = if (rawAcks != null) {
                    try { gson.fromJson(rawAcks, AcksFile::class.java).acknowledgements } catch(e: Exception) { emptyList() }
                } else emptyList()

                // 4. Load remote_acks/ — replies written by others into my folder
                val remoteAcks = repo.listFilesInDir(employeeName, "remote_acks").mapNotNull { filename ->
                    try {
                        val raw = repo.loadFromDir(employeeName, "remote_acks", filename) ?: return@mapNotNull null
                        gson.fromJson(raw, Acknowledgement::class.java)
                    } catch (e: Exception) { null }
                }

                // Merge acks (local acks + remote acks, dedup by id)
                val allAcks = (localAcks + remoteAcks).distinctBy { it.id }
                val ackIds = allAcks.map { it.id }.toSet()
                val localAckIds = localAcks.map { it.id }.toSet()

                // 5. Combine incoming items (server alerts + peer notes)
                val incoming = serverAlerts + peerNotes
                val incomingIds = incoming.map { it.id }.toSet()

                // 6. Replies from remote_acks not in incoming — these are replies to notes I sent.
                // IMPORTANT: Filter by localAckIds to avoid showing ones we already handled.
                val repliesAsAlerts = remoteAcks
                    .filter { it.response != null && it.id !in incomingIds && it.id !in localAckIds }
                    .map { ack ->
                        Alert(
                            id = ack.id,
                            message = "✉️ **${ack.responderName ?: "Someone"}** replied to your note:\n\n> ${ack.response}",
                            sentAt = ack.acknowledgedAt,
                            sentBy = ack.responderName ?: "System",
                            senderFolder = null
                        )
                    }

                // 7. Filter out already acknowledged incoming items
                val unackedIncoming = incoming.filter { it.id !in ackIds }

                // 8. Combine unacked incoming + new replies
                val fullQueue = unackedIncoming + repliesAsAlerts

                Log.d("AlertsVM", "Check complete. Incoming: ${incoming.size}, Local Acks: ${localAcks.size}, Remote Acks: ${remoteAcks.size}, New Replies: ${repliesAsAlerts.size}, Final Queue: ${fullQueue.size}")

                withContext(Dispatchers.Main) {
                    alertQueue = fullQueue
                    if (fullQueue.isEmpty()) {
                        showAlertModal = false
                        alertQueueIndex = 0
                    } else {
                        // Reset index if it's invalid for the new queue
                        if (alertQueueIndex >= fullQueue.size) {
                            alertQueueIndex = 0
                        }
                        if (permissionsGrantedToShow) {
                            showAlertModal = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlertsVM", "Alert check error", e)
            }
        }
    }
    
    fun allowAlertsToDisplay() {
        permissionsGrantedToShow = true
        refreshAlerts()
    }

    private fun saveAcknowledgementLocally(ack: Acknowledgement) {
        val repo = repository ?: return
        try {
            // profile.json.lock (held by ProfileViewModel) is the session lock —
            // only one tablet is active per employee at a time, so no separate lock needed here.
            val raw = repo.loadGenericJSON(employeeName, "acknowledgements.json", useCache = false)
            val data = if (raw != null) {
                try { gson.fromJson(raw, AcksFile::class.java) } catch(e: Exception) { AcksFile() }
            } else AcksFile()

            val existingIndex = data.acknowledgements.indexOfFirst { it.id == ack.id }
            val updatedAcks = data.acknowledgements.toMutableList()
            if (existingIndex >= 0) {
                updatedAcks[existingIndex] = ack
            } else {
                updatedAcks.add(ack)
            }

            repo.saveGenericJSON(employeeName, "acknowledgements.json", gson.toJson(AcksFile(updatedAcks)))
        } catch (e: Exception) {
            Log.e("AlertsVM", "Local ack save error", e)
        }
    }

    private fun saveAcknowledgementRemote(senderFolder: String, ack: Acknowledgement) {
        val repo = repository ?: return
        try {
            // Write to [senderFolder]/remote_acks/[noteId].json — unique file per note, no conflicts.
            // Only one person can reply to a given note from a given sender, so no two writers share this file.
            val result = repo.saveInDir(senderFolder, "remote_acks", "${ack.id}.json", gson.toJson(ack))
            Log.d("AlertsVM", "Reply saved to $senderFolder/remote_acks/${ack.id}.json: $result")
        } catch (e: Exception) {
            Log.e("AlertsVM", "Remote reply save error", e)
        }
    }
}
