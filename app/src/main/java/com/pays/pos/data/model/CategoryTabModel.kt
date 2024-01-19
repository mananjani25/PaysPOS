package com.pays.pos.data.model

data class CategoryTabModel(
    val id: Int,
    val title: String,
    var isSelected: Boolean = false,
    val position: Int,
    var type:String =""
)