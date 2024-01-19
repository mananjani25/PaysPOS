package com.pays.pos.data.model

data class DashboardItemModel(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val flag:Boolean=false
)