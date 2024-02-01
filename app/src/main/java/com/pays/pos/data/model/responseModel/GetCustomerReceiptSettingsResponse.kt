package com.pays.pos.data.model.responseModel

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class GetCustomerReceiptSettingsResponse(
    @SerializedName("data")
    val `data`: Data?=null,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    @Keep
@Entity(tableName = "TbCustomerSettings")
    data class Data(
        @PrimaryKey
        @SerializedName("id")
        val id: Int? = null,
        @SerializedName("fonts")
        val fonts: String = "",
        @SerializedName("show_order_id_top")
        val showOrderIdTop: Boolean = false,
        @SerializedName("show_modifiers")
        val showModifiers: Boolean = false,
        @SerializedName("show_order_note")

        val showOrderNote: Boolean = false,

        @SerializedName("show_split_amount")
        val showSplitAmount: Boolean = false,
        @SerializedName("show_rolled_over")

        val showRolledOver: Boolean = false,
        @SerializedName("rolled_over_number")

        val rolledOverNumber: Int? = null,
        @SerializedName("show_order_type")

        val showOrderType: Boolean = false,
        @SerializedName("show_team")

        val showTeam: Boolean = false,
        @SerializedName("show_order_time")
        val showOrderTime: Boolean = false,
        @SerializedName("show_print_time")

        val showPrintTime: Boolean = false,
        @SerializedName("show_refund_amount")
        val showRefundAmount: Boolean = false,
        @SerializedName("show_venue_logo")

        val showVenueLogo: Boolean = false,
        @SerializedName("show_venue_phone")

        val showVenuePhone: Boolean = false,
        @SerializedName("show_venue_address")

        val showVenueAddress: Boolean = false,
        @SerializedName("show_website_address")
        val showWebsiteAddress: Boolean = false,
        @SerializedName("show_customer_name")
        val showCustomerName: Boolean = false,
        @SerializedName("show_customer_phone")
        val showCustomerPhone: Boolean = false,
        @SerializedName("show_customer_address")
        val showCustomerAddress: Boolean = false,
        @SerializedName("show_tip_suggestion")
        val showTipSuggestion: Boolean = false,
        @SerializedName("show_tip_line_for_cash")
        val showTipLineForCash: Boolean = false,
        @SerializedName("show_qr_code")
        val showQrCode: Boolean = false,
        @SerializedName("show_custom_note")

        val showCustomNote: Boolean = false,
        @SerializedName("custom_note")

        val customNote: String = "",
        @SerializedName("show_cd_and_sc_customer_receipt")
        val showCashDisSurCharg:Boolean = false,

        @SerializedName("location_id")

        val locationId: Int? = null,
        @SerializedName("created_at")

        val createdAt: String = "",
        @SerializedName("updated_at")

        val updatedAt: String = "",

        )
}