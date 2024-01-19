package com.pays.pos.data.model.responseModel.category

import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.model.responseModel.item.Item
import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("sort")
    val sort: Int = -1,
    @SerializedName("thumb_image_url")
    val thumbImgUrl: String? = "",
    @SerializedName("original_image_url")
    val originalImgUrl: String? = "",
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("items")
    val items: List<Item>,
    @SerializedName("item_ids")
    val itemIds: List<Int>,
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
)
