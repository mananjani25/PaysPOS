package com.android.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Modifier : Parcelable {


    @SerializedName("created_at")
    val createdAt: String = ""

    @SerializedName("id")
    val id: Int? = null

    @SerializedName("modifier_set_id")
    val modifierSetId: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("price")
    var price: Double = 0.00

    @SerializedName("updated_at")
    val updatedAt: String = ""

    @SerializedName("sort")
    val sort: Int = 0

    @SerializedName("_destroy")
    var _destroy: Boolean = false

    var isChecked: Boolean = false
}