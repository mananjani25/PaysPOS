package com.pays.pos.data.model.responseModel


import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.pays.pos.R
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.data.model.responseModel.category.Category
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
        val optionSets: List<OptionSet> = emptyList(),
        @SerializedName("time_stamp")
        val timeStamp: String
    )

    object companion {
        @BindingAdapter("profileImage", "thumbHolder")
        @JvmStatic
        fun loadImage(view: ImageView, imageUrl: String?, thumbNail: String?) {
            if (imageUrl.isNullOrBlank() || imageUrl.trim() == "" || imageUrl.trim() == "null" || imageUrl.isNullOrEmpty()
            ) {
                view.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_item_placeholder))

            } else {
                Glide.with(view.context)
                    .load(imageUrl).centerCrop()
                    .thumbnail(Glide.with(view.context).load(thumbNail))
                    .into(view)
            }
        }
    }
}