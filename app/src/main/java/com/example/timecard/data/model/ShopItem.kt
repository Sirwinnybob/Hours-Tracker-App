package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class ShopItem(
    @SerializedName("id")          val id: String = "",
    @SerializedName("title")       val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("price")       val price: Int = 0,
    @SerializedName("category")    val category: String = "",
    @SerializedName("icon")        val icon: String = "🛍️",
    @SerializedName("inShop")      val inShop: Boolean? = null,  // null = shown (Gson sets false for missing boolean fields)
    @SerializedName("imageFile")   val imageFile: String? = null,
    @SerializedName("isSpecial")   val isSpecial: Boolean = false,
    @SerializedName("quantity")    val quantity: Int? = null
)
