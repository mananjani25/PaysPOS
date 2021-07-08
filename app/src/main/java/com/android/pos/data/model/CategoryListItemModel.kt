package com.android.pos.data.model

data class CategoryListItemModel(
    val id: Int,
    val title: String,
    val type: String,
    val imageUrl: String,
    val flag:Boolean =false
)