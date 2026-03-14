package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class ShopItem(
    @SerializedName("id")          val id: String = "",
    @SerializedName("title")       val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("price")       val price: Int = 0,
    @SerializedName("category")    val category: String = "",
    @SerializedName("icon")        val icon: String = "🛍️",
    @SerializedName("inShop")      val inShop: Boolean = true,
    /** Special/featured item — shown in the Featured section and triggers the new-item banner. */
    @SerializedName("isSpecial")   val isSpecial: Boolean = false,
    /** Limited quantity remaining (null = unlimited). */
    @SerializedName("quantity")    val quantity: Int? = null,
    /** Relative path to an image file under the sync root (e.g. "shop_images/reward_nib.png"). */
    @SerializedName("imageFile")   val imageFile: String? = null
)
