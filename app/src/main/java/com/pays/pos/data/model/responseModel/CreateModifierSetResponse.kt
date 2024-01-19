package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.ModifierSet
import com.google.gson.annotations.SerializedName

data class CreateModifierSetResponse(
    @SerializedName("data")
    val `data`: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("modifier_set")
        val modifierSet: ModifierSet
    )

}