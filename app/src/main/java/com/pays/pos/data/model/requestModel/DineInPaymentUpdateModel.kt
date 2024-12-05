package com.pays.pos.data.model.requestModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class DineInPaymentUpdateModel(
    @SerializedName("id")
    var id: Int? = null
):Parcelable{
}
