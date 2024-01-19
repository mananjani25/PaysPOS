package com.pays.pos.data.model

data class UpdateCategoryModel(
    val `data`: UpdateCategoryData,
    val message: String,
    val status: Int,
    val type: String
)

data class UpdateCategoryData(
    val active: Boolean,
    val id: Int,
    val item_ids: List<Any>,
    val items: List<Any>,
    val location_id: Int,
    val name: String,
    val original_image_url: Any,
    val sort: Int,
    val thumb_image_url: Any
)