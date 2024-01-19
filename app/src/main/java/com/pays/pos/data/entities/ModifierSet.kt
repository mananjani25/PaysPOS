package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "ModifierSet")
class ModifierSet : Parcelable {
    @SerializedName("created_at")
    var createdAt: String = ""

    @PrimaryKey
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("item_ids")
    var itemIds: List<Int> = emptyList()

    @SerializedName("modifiers")
    var modifiers: List<Modifier> = emptyList()

    @SerializedName("name")
    var name: String = ""

    @SerializedName("updated_at")
    var updatedAt: String = ""

    @SerializedName("location_id")
    var locationId: Int = 0

    var isChecked: Boolean = false

    @SerializedName("min_required")
    var min_required: Int = 0

    @SerializedName("max_allowed")
    var max_allowed: Int = 0

    @SerializedName("sort")
    var sort: Int = 0

    @SerializedName("is_deleted")
    var isDeleted: Boolean = false


    override fun toString(): String {
        return name
    }
}