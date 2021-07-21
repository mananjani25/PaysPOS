package com.android.pos.data.model.responseModel


import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.android.pos.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.annotations.SerializedName


data class VenueDataResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("categories")
        val categories: List<Category>
    ) {
        data class Category(
            @SerializedName("id")
            val id: Int,
            @SerializedName("items")
            val items: List<Item>,
            @SerializedName("name")
            val name: String,
            @SerializedName("sort")
            val sort: Int
        ) {
            data class Item(
                @SerializedName("cost")
                val cost: Int,
                @SerializedName("id")
                val id: Int,
                @SerializedName("kitchen_name")
                val kitchenName: String,
                @SerializedName("name")
                val name: String,
                @SerializedName("price")
                val price: Double,
                @SerializedName("price_type")
                val priceType: String,
                @SerializedName("product_code")
                val productCode: String,
                @SerializedName("quantity")
                val quantity: Int,
                @SerializedName("sku")
                val sku: String,
                @SerializedName("original_image_url")
                val imgUrl: String?,
                @SerializedName("thumb_image_url")
                val thumpImgUrl: String?


            )
        }
    }

    object companion {
        @BindingAdapter("profileImage")
        @JvmStatic
        fun loadImage(view: ImageView, imageUrl: String?) {
            if (imageUrl.isNullOrBlank() || imageUrl.trim() == "" || imageUrl.isNullOrEmpty()
            ) {
                return

            } else {
                Glide.with(view.context)
                    .load(imageUrl).centerCrop()
                    .placeholder(android.R.drawable.screen_background_dark_transparent)
                    .into(view)
            }
        }
    }
}