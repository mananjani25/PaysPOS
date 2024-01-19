package com.pays.pos.data.model.requestModel


import com.pays.pos.data.entities.Modifier
import com.google.gson.annotations.SerializedName

class CreateModifierRequest {
    @SerializedName("modifier_set")
    var modifierSet: ModifierSet = ModifierSet()
}

class ModifierSet {

    var id: Int? = null

    @SerializedName("item_ids")
    var itemIds: List<Int> = emptyList()

    @SerializedName("location_id")
    var locationId: Int = 0

    @SerializedName("modifiers_attributes")
    var modifiersAttributes: List<Modifier> = emptyList()

    @SerializedName("name")
    var name: String = ""
}
