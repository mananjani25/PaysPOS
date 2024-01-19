package com.pays.pos.data.model

data class CustomerModel(
    val id: Int,
    val titleName: String,
    val name: String,
    val number: String,
    val email: String,
    var isSelected: Boolean
)
