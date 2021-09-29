package com.android.pos.data.model.responseModel


import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.android.pos.R
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.VariationsAttribute
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
        val modifierSets: List<ModifierSet> = emptyList(),
        @SerializedName("option_sets")
        val optionSets: List<OptionSet> = emptyList()

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
                @SerializedName("category_name")
                val categoryName: String?,
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
                val thumpImgUrl: String?,
                @SerializedName("active")
                val active: Boolean,
                var taxes: List<TaxData>? = null,
                @SerializedName("modifier_set_ids")
                val modifierIds: List<Int>,
                @SerializedName("option_sets")
                val optionSetIds: List<Int>,
                @SerializedName("modifier_sets")
                val modifierSets: List<ModifierSet> = emptyList(),
                @SerializedName("variations")
                val variations: List<VariationsAttribute> = emptyList(),
                @SerializedName("selected_option_sets")
                val optionSets: List<OptionSet> = emptyList()


            )
        }

    }

    object companion {
        @BindingAdapter("profileImage", "thumbHolder")
        @JvmStatic
        fun loadImage(view: ImageView, imageUrl: String?, thumbNail: String?) {
            if (imageUrl.isNullOrBlank() || imageUrl.trim() == "" || imageUrl.trim() == "null" || imageUrl.isNullOrEmpty()
            ) {
                view.setImageDrawable(view.context.resources.getDrawable(android.R.drawable.screen_background_dark_transparent))

            } else {
                Glide.with(view.context)
                    .load(imageUrl).centerCrop()
                    .thumbnail(Glide.with(view.context).load(thumbNail))
                    .into(view)
            }
        }
    }
}