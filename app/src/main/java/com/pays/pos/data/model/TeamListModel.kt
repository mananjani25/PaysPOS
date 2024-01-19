package com.pays.pos.data.model

data class TeamListModel(
    val id: Int,
    val title: String,
    val name: String,
    val email: String,
    val isHeader: Boolean = false
)