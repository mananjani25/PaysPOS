package com.pays.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class UpdateKitchenReceiptRequestModel(
    @SerializedName("fonts")
    var font: String = "",
    @SerializedName("show_category")
    var show_category: Boolean = true,
    @SerializedName("show_items_in_group")
    var show_items_in_group: Boolean = true,
    @SerializedName("show_team_member")
    var show_team_member: Boolean = true,
    @SerializedName("show_order_note")
    var show_order_note: Boolean = true,
    @SerializedName("show_order_type")
    var show_order_type: Boolean = true,
    @SerializedName("show_customer_name")
    var show_customer_name: Boolean = true,
    @SerializedName("show_customer_phone")
    var show_customer_phone: Boolean = true,
    @SerializedName("show_customer_address")
    var show_customer_address: Boolean = true
)
