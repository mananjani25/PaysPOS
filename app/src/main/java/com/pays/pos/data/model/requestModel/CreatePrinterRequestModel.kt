package com.pays.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class CreatePrinterRequestModel(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("terminal_id")var terminalId:Int?=null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("mac_address") var macAddress: String? = null,
    @SerializedName("modal_name") var modalName: String? = null,
    @SerializedName("terminal_ids") var terminalIds: List<Int>? = null,
    @SerializedName("status") var status: Boolean = true,
    @SerializedName("ip_address") var ip_address: String? = null,
    @SerializedName("is_cash_drawer_open") var isCashDrawerOpen: Boolean = true,
    @SerializedName("is_report_print_enable") var isReportPrintEnable: Boolean = false,
    @SerializedName("is_automatic_two_customer_receipt") var isAutomaticTwoCustomerReceipt: Boolean = false,
    @SerializedName("location_id") var locationId: Int? = null,
    @SerializedName("unpaid_receipt_auto_printing") var unpaidReceiptAutoPrinting: Boolean = true,
    @SerializedName("unpaid_receipt_auto_print_terminal_ids") var unpaidReceiptAutoPrintTerminalIds: List<Int>? = null,
    @SerializedName("category_ids") var categoryIds: List<Int>? = null,
    @SerializedName("receipt_print_type") var receiptPrintType: String? = null,
    @SerializedName("printer_type") var printer_type: String? = null,
    @SerializedName("printer_settings_attributes") var printerSettingsAttributes: List<PrinterSettingsAttributes>? = null,
    @SerializedName("printer_brand")var printerBrand:String? = null,
    @SerializedName("port_no")var portNo:Int?=null
   // @SerializedName("category_ids")  var listCatIds:List<Int>?=null

) {
    data class PrinterSettingsAttributes(

        @SerializedName("id") var id: Int? = null,
        @SerializedName("order_type_id") var orderTypeId: Int? = null,
        @SerializedName("print_type") var printType: String? = null,
        @SerializedName("manual_printing") var manualPrinting: Boolean = false,
        @SerializedName("auto_printing") var autoPrinting: Boolean = true,
        @SerializedName("_destroy") var isDestroy: Boolean = false

        )
}
