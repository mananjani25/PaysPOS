package com.pays.pos.data.entities

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ItemModifierSets", primaryKeys = ["itemId", "modifierSetId"])
class ItemModifierSets {

    @SerializedName("itemId")
    var itemId: Int = 0

    @SerializedName("modifier_set_id")
    var modifierSetId: Int = 0

    @SerializedName("min_required")
    var minRequired: Int = 0

    @SerializedName("max_allowed")
    var maxAllowed: Int = 0

    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
}