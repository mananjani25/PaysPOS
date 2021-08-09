package com.android.pos.data.model.responseModel


import com.android.pos.data.entities.ModifierSet
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