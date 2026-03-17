package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class LimitedPurchaseClaim(
    @SerializedName("claimId")       val claimId: String = "",
    @SerializedName("itemId")        val itemId: String = "",
    @SerializedName("itemTitle")     val itemTitle: String = "",
    @SerializedName("price")         val price: Int = 0,
    @SerializedName("employeeName")  val employeeName: String = "",
    @SerializedName("displayName")   val displayName: String? = null,
    @SerializedName("claimedAt")     val claimedAt: String = "",
    @SerializedName("deviceId")      val deviceId: String = "",
    @SerializedName("approved")      val approved: Boolean? = null,
    @SerializedName("resolvedAt")    val resolvedAt: String? = null
)
