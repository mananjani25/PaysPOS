package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class GetKitchenReceiptSettingsResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("fonts")
        val fonts: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("show_category")
        val showCategory: Boolean,
        @SerializedName("show_customer_address")
        val showCustomerAddress: Boolean,
        @SerializedName("show_customer_name")
        val showCustomerName: Boolean,
        @SerializedName("show_customer_phone")
        val showCustomerPhone: Boolean,
        @SerializedName("show_items_in_group")
        val showItemsInGroup: Boolean,
        @SerializedName("show_order_note")
        val showOrderNote: Boolean,
        @SerializedName("show_order_type")
        val showOrderType: Boolean,
        @SerializedName("show_team_member")
        val showTeamMember: Boolean,
        @SerializedName("updated_at")
        val updatedAt: String
    )
}