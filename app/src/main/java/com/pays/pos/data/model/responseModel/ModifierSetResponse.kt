package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.ModifierSet
import com.google.gson.annotations.SerializedName

data class ModifierSetResponse(
    @SerializedName("data")
    val `data`: List<ModifierSet>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)