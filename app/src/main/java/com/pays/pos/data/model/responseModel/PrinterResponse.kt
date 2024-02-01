package com.pays.pos.data.model.responseModel

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


data class PrinterResponse(
    @SerializedName("data") var data: Data,
    @SerializedName("type") var type: String,
    @SerializedName("status") var status: Int,
    @SerializedName("message") var message: String
) {


    data class Data(
        @SerializedName("customer_receipt_printers") var customerReceiptPrinters: List<CustomerReceiptPrinters>? = null,
        @SerializedName("kitchen_receipt_printers") var kitchenReceiptPrinters: List<KitchenReceiptPrinters>? = null

    ) {

        @Keep
@Entity(tableName = "TbCustomerPrint",indices = [
            Index( value = ["macAddress","ipAddress"], unique = true)
        ])
        data class CustomerReceiptPrinters(
            @PrimaryKey
            @SerializedName("id") var id: Int,
            @SerializedName("name") var name: String,
            @SerializedName("modal_name") var modalName: String,
            @SerializedName("mac_address") var macAddress: String,
            @SerializedName("status") var status: Boolean,
            @SerializedName("is_cash_drawer_open") var isCashDrawerOpen: Boolean,
            @SerializedName("location_id") var locationId: Int,
            @SerializedName("created_at") var createdAt: String,
            @SerializedName("updated_at") var updatedAt: String,
            @SerializedName("ip_address") var ipAddress: String? = null,
            @SerializedName("printer_type") var printer_type: String? = null,
            @SerializedName("unpaid_receipt_auto_printing") var unpaidReceiptAutoPrinting: Boolean,
            @SerializedName("is_report_print_enable") var isReportPrintEnable: Boolean,
            @SerializedName("is_automatic_two_customer_receipt") var isAutomaticTwoCustomerReceipt: Boolean,
            @SerializedName("receipt_print_type") var receiptPrintType: String,
           /* @SerializedName("kitchen_status") var kitchenStatus: Boolean,
            @SerializedName("customer_status") var customerStatus: Boolean,*/

            @SerializedName("printer_categories") var printerCategories: List<PrinterCategories>,
            @SerializedName("terminal_ids") var terminalIds: List<Int>,
            @SerializedName("unpaid_receipt_auto_print_terminal_ids") var unpaidReceiptAutoPrintTerminalIds: String,

            @SerializedName("order_types") var orderTypes: List<OrderTypes>,
            @SerializedName("is_deleted")
            val isDeleted: Boolean

        )


        @Keep
@Entity(tableName = "TbKitchenPrint")
        data class KitchenReceiptPrinters(
            @PrimaryKey
            @SerializedName("id") var id: Int,
            @SerializedName("name") var name: String,
            @SerializedName("modal_name") var modalName: String,
            @SerializedName("mac_address") var macAddress: String,
            @SerializedName("status") var status: Boolean,
            @SerializedName("is_cash_drawer_open") var isCashDrawerOpen: Boolean,
            @SerializedName("printer_type") var printer_type: String? = null,
            @SerializedName("location_id") var locationId: Int,
            @SerializedName("created_at") var createdAt: String,
            @SerializedName("updated_at") var updatedAt: String,
            @SerializedName("ip_address") var ipAddress: String? = null,
            @SerializedName("unpaid_receipt_auto_printing") var unpaidReceiptAutoPrinting: Boolean,
            @SerializedName("is_report_print_enable") var isReportPrintEnable: Boolean,
            @SerializedName("is_automatic_two_customer_receipt") var isAutomaticTwoCustomerReceipt: Boolean,
            @SerializedName("receipt_print_type") var receiptPrintType: String,
            @SerializedName("printer_categories") var printerCategories: List<PrinterCategories>,
            @SerializedName("terminal_ids") var terminalIds: List<Int>,
           /* @SerializedName("kitchen_status") var kitchenStatus: Boolean,
            @SerializedName("customer_status") var customerStatus: Boolean,*/
            @SerializedName("unpaid_receipt_auto_print_terminal_ids") var unpaidReceiptAutoPrintTerminalIds: String,
            @SerializedName("order_types") var orderTypes: List<OrderTypes>,
            @SerializedName("is_deleted")
            val isDeleted: Boolean

        )


        data class OrderTypes(
            @SerializedName("order_type_id") var orderTypeId: Int,
            @SerializedName("order_type_name") var orderTypeName: String,
            @SerializedName("order_type") var orderType: String,
            @SerializedName("printer_settings") var printerSettings: List<PrinterSettings>

        )


        data class PrinterSettings(

            @SerializedName("id") var id: Int? = null,
            @SerializedName("order_type_id") var orderTypeId: Int,
            @SerializedName("print_type") var printType: String,
            @SerializedName("manual_printing") var manualPrinting: Boolean,
            @SerializedName("auto_printing") var autoPrinting: Boolean,
            @SerializedName("printer_id") var printerId: Int,
            @SerializedName("created_at") var createdAt: String,
            @SerializedName("updated_at") var updatedAt: String,
            @SerializedName("_destroy") var isDestroy: Boolean = false

        )


        data class PrinterCategories(

            @SerializedName("id") var id: Int,
            @SerializedName("name") var name: String,
            @SerializedName("sort") var sort: Int,
            @SerializedName("location_id") var locationId: Int,
            @SerializedName("created_at") var createdAt: String,
            @SerializedName("updated_at") var updatedAt: String,
            @SerializedName("active") var categoryActive: Boolean,
            @SerializedName("printer_enable") var printerEnable: Boolean

        )
    }


}