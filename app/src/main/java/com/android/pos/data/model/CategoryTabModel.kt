package com.android.pos.data.model

data class CategoryTabModel(
    val id: Int,
    val title: String,
    var isSelected: Boolean,
    val position: Int
)