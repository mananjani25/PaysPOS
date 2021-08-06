package com.android.pos.data.model.responseModel


import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.TaxData
import com.bumptech.glide.Glide
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
        val categories: List<Category>,
        @SerializedName("modifier_sets")
        val modifierSets: List<ModifierSet> = emptyList()

    ) {
        data class Category(
            @SerializedName("id")
            val id: Int,
            @SerializedName("items")
            val items: List<Item>,
            @SerializedName("name")
            val name: String,
            @SerializedName("sort")
            val sort: Int,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("active")
            val active: Boolean,
            @SerializedName("item_ids")
            val itemIds: List<Int>,

            ) {
            data class Item(
                @SerializedName("cost")
                val cost: Double,
                @SerializedName("id")
                val id: Int,
                @SerializedName("sort")
                val sort: Int,
                @SerializedName("category_id")
                val categoryId: Int,
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
                @SerializedName("thumb_image_url")
                val thumbNail: String?,
                @SerializedName("original_image_url")
                val imgUrl: String?,
                @SerializedName("thumb_image_url")
                val thumpImgUrl: String?,
                @SerializedName("active")
                val active: Boolean,
                var taxes: List<TaxData>? = null


            )
        }

    }

    object companion {
        @BindingAdapter("profileImage","thumbHolder")
        @JvmStatic
        fun loadImage(view: ImageView, imageUrl: String?, thumbNail: String?) {
            if (imageUrl.isNullOrBlank() || imageUrl.trim() == "" || imageUrl.trim() == "null" || imageUrl.isNullOrEmpty()
            ) {
                return

            } else {
                Glide.with(view.context)
                    .load(imageUrl).centerCrop()
                    .thumbnail(Glide.with(view.context).load(thumbNail))
                    .into(view)
            }
        }
    }
}