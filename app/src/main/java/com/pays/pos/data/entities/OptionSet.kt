package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "OptionSet")
class OptionSet : Parcelable {
    @SerializedName("display_name")
    var displayName: String? = ""

    @PrimaryKey
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("location_id")
    var locationId: Int = 0

    @SerializedName("name")
    var name: String = ""

    @SerializedName("option_type")
    var optionType: String? = ""

    @SerializedName("options")
    var options: List<Option> = emptyList()

    @SerializedName("sort")
    var sort: Int = 0

    @SerializedName("is_deleted")
    var isDeleted: Boolean = false

}