package com.pays.pos.data.model

import java.io.Serializable


data class CustomerDetailModel(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String,
    val address1: String,
    val address2: String,
    val companyName: String,
    val birthDate: String
):Serializable
