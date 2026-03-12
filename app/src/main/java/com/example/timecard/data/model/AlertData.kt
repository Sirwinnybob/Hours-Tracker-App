package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class Alert(
    @SerializedName("id") val id: String,
    @SerializedName("message") val message: String,
    @SerializedName("sentAt") val sentAt: String,
    @SerializedName("sentBy") val sentBy: String? = null,
    @SerializedName("senderFolder") val senderFolder: String? = null,
    @SerializedName("isAnonymous") val isAnonymous: Boolean = false,
    @SerializedName("senderDisplayName") val senderDisplayName: String? = null
)

data class AlertsFile(
    @SerializedName("alerts") val alerts: List<Alert> = emptyList()
)

data class Acknowledgement(
    @SerializedName("id") val id: String,
    @SerializedName("acknowledgedAt") val acknowledgedAt: String,
    @SerializedName("response") val response: String? = null,
    @SerializedName("responderName") val responderName: String? = null,
    @SerializedName("originalMessage") val originalMessage: String? = null
)

data class AcksFile(
    @SerializedName("acknowledgements") val acknowledgements: List<Acknowledgement> = emptyList()
)

