package com.pays.pos.data.model.requestModel


import com.pays.pos.data.entities.Option
import com.google.gson.annotations.SerializedName

data class CreateOptionRequestModel(
    @SerializedName("display_name")
    var displayName: String = "",
    @SerializedName("location_id")
    var locationId: Int = -1,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("option_type")
    var optionType: String = "",
    @SerializedName("options_attributes")
    var optionsAttributes: List<Option>? = null,
    @SerializedName("sort")
    var sort: Int = -1
)