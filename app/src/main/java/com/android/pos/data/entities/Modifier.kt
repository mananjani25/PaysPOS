package com.android.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Modifier : Parcelable {


    @SerializedName("id")
    val id: Int? = null

    @SerializedName("modifier_set_id")
    var modifierSetId: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("price")
    var price: Double = 0.00

    @SerializedName("sort")
    val sort: Int = 0

    @SerializedName("_destroy")
    var _destroy: Boolean = false

    var isChecked: Boolean = false

    @SerializedName("itemQuantity")
    var itemQuantity: Int = 0
}