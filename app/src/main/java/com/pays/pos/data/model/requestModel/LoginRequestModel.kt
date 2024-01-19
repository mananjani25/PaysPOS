package com.pays.pos.data.model.requestModel

import androidx.room.Entity
import androidx.room.PrimaryKey

data class LoginRequestModel(
    var emailAddress: String? = "",
    var password: String? = "",
)