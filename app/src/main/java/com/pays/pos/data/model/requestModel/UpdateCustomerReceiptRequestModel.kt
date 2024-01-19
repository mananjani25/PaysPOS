package com.pays.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class UpdateCustomerReceiptRequestModel(
    @SerializedName("fonts")
    var fonts: String = "",
    @SerializedName("show_order_id_top")
    var showOrderIdTop: Boolean = false,
    @SerializedName("show_modifiers")
    var showModifiers: Boolean = false,
    @SerializedName("show_order_note")

    var showOrderNote: Boolean = false,

    @SerializedName("show_split_amount")
    var showSplitAmount: Boolean = false,
    @SerializedName("show_rolled_over")

    var showRolledOver: Boolean = false,
    @SerializedName("rolled_over_number")

    val rolledOverNumber: Int? = null,
    @SerializedName("show_order_type")

    var showOrderType: Boolean = false,
    @SerializedName("show_team")

    var showTeam: Boolean = false,
    @SerializedName("show_order_time")
    var showOrderTime: Boolean = false,
    @SerializedName("show_print_time")

    var showPrintTime: Boolean = false,
    @SerializedName("show_refund_amount")
    var showRefundAmount: Boolean = false,
    @SerializedName("show_venue_logo")

    var showVenueLogo: Boolean = false,
    @SerializedName("show_venue_phone")

    val showVenuePhone: Boolean = false,
    @SerializedName("show_venue_address")

    var showVenueAddress: Boolean = false,
    @SerializedName("show_website_address")
    var showWebsiteAddress: Boolean = false,
    @SerializedName("show_customer_name")
    var showCustomerName: Boolean = false,
    @SerializedName("show_customer_phone")
    var showCustomerPhone: Boolean = false,
    @SerializedName("show_customer_address")
    var showCustomerAddress: Boolean = false,
    @SerializedName("show_tip_suggestion")
    var showTipSuggestion: Boolean = false,
    @SerializedName("show_tip_line_for_cash")
    var showTipLineForCash: Boolean = false,
    @SerializedName("show_qr_code")
    var showQrCode: Boolean = false,
    @SerializedName("show_custom_note")

    var showCustomNote: Boolean = false,
    @SerializedName("custom_note")
    val customNote: String = "",

    @SerializedName("show_cd_and_sc_customer_receipt")
    var showCdAndScCustomerReceipt:Boolean = false

    )