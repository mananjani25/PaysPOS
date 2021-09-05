package com.android.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Option : Parcelable {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("option_set_id")
    var optionSetId: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("sort")
    var sort: Int = 0

    @SerializedName("_destroy")
    var _destroy: Boolean = false

}