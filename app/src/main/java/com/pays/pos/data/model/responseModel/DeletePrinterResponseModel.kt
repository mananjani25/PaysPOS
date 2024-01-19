package com.pays.pos.data.model.responseModel

import com.pays.pos.data.model.requestModel.CreatePrinterRequestModel
import com.google.gson.annotations.SerializedName

data class DeletePrinterResponseModel(
    @SerializedName("data") var data: Data,
    @SerializedName("type") var type: String,
    @SerializedName("status") var status: Int,
    @SerializedName("message") var message: String
) {

    data class Data(

        @SerializedName("id") var id: Int,
        @SerializedName("name") var name: String,
        @SerializedName("modal_name") var modalName: String,
        @SerializedName("mac_address") var macAddress: String,
        @SerializedName("status") var status: Boolean,
        @SerializedName("is_cash_drawer_open") var isCashDrawerOpen: Boolean,
        @SerializedName("location_id") var locationId: Int,
        @SerializedName("created_at") var createdAt: String,
        @SerializedName("updated_at") var updatedAt: String,
        @SerializedName("ip_address") var ipAddress: String,
        @SerializedName("printer_type") var printerType: String,
        @SerializedName("unpaid_receipt_auto_printing") var unpaidReceiptAutoPrinting: Boolean,
        @SerializedName("is_report_print_enable") var isReportPrintEnable: Boolean,
        @SerializedName("is_automatic_two_customer_receipt") var isAutomaticTwoCustomerReceipt: Boolean,
        @SerializedName("receipt_print_type") var receiptPrintType: String,
        @SerializedName("printer_categories") var printerCategories: List<PrinterResponse.Data.PrinterCategories>,
        @SerializedName("terminal_ids") var terminalIds: List<Int>,
        @SerializedName("unpaid_receipt_auto_print_terminal_ids") var unpaidReceiptAutoPrintTerminalIds: String,
        @SerializedName("order_types") var orderTypes: List<OrderTypes>

    )

    data class OrderTypes(

        @SerializedName("order_type_id") var orderTypeId: Int,
        @SerializedName("order_type_name") var orderTypeName: String,
        @SerializedName("order_type") var orderType: String,
        @SerializedName("printer_settings") var printerSettings: List<CreatePrinterRequestModel.PrinterSettingsAttributes>

    )

}
