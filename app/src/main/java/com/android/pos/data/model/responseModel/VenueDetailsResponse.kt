package com.android.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.pos.data.entities.*
import com.android.pos.data.model.ShiftRportConfiguration
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class VenueDetailsResponse(
    @SerializedName("data")
    val settingData: SettingData,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {

    data class SettingData(
        @SerializedName("setting_data") val `data`: Data, @SerializedName("time_stamp")
        val timeStamp: String
    )

    data class Data(
        @SerializedName("business_name")
        val businessName: String,
        @SerializedName("business_website")
        val businessWebsite: String,
        @SerializedName("cancel_order_reasons")
        val cancelOrderReasons: List<CancelOrderReason>,
        @SerializedName("customer_contact_email")
        val customerContactEmail: String?,
        @SerializedName("daily_report_sending_time")
        val dailyReportSendingTime: String,
        @SerializedName("dynamic_payment_records")
        val dynamicPaymentRecords: List<Any>,
        @SerializedName("end_time_for_report_summary")
        val endTimeForReportSummary: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("logo")
        val logo: Logo,
        @SerializedName("notes")
        val notes: List<NoteResponse.Data>,
        @SerializedName("phone_number")
        val phoneNumber: String,
        @SerializedName("phone_number_1_country")
        val phone_number_1_country: String?,
        @SerializedName("phone_number_2_country")
        val phone_number_2_country: String?,
        @SerializedName("phone_number_2")
        val phoneNumber2: String?,
        @SerializedName("business_time_zone")
        val business_time_zone: String?,
        @SerializedName("address")
        val address: String,
        @SerializedName("start_time_for_report_summary")
        val startTimeForReportSummary: String,
        @SerializedName("taxes")
        val taxes: List<TaxData>,
        @SerializedName("discounts")
        val discounts: List<TbDiscount>,
        @SerializedName("service_charges")
        val service_charges: List<TbServiceCharge>,
        @SerializedName("loyalty_programs")
        val loyaltyPrograms: List<LoyaltyProgramsModel>,
        @SerializedName("cash_discounts")
        val cash_discounts: List<CashDiscountModel>,
        @SerializedName("terminals")
        val terminals: List<Terminal>,
        @SerializedName("printers")
        val printers: Printer,
        @SerializedName("business_address")
        val business_address: BusinessAddress,
        @SerializedName("tip_settings")
        val tip_settings: List<GetTipReponse.Data>,
        @SerializedName("wastage_reasons")
        val wastageReasons: List<WastageReason>,
        @SerializedName("time_zone")
        val timeZone: String,
        @SerializedName("user_id")
        val userId: Int,
        @SerializedName("is_printer_queue_enable")
        val isPrinterQueueEnable: Boolean,
        @SerializedName("customer_receipt")
        val customerReceipt: GetCustomerReceiptSettingsResponse.Data? = null,
        @SerializedName("kitchen_receipt")
        val kitchenReceipt: GetKitchenReceiptSettingsResponse.Data? = null,
        @SerializedName("team_roles")
        val teamRoles: List<TeamRole>,
        @SerializedName("employee")
        val employee: List<Employee>,
        @SerializedName("phone_country")
        val phoneCountrylist: List<TbCountryList>,
        @SerializedName("order_types")
        val orderTypes: List<TbOrderType>,
        @SerializedName("magensa_settings")
        val magensaSettings: List<MagensaSettings>,
        @SerializedName("service_charge_enable")
        val service_charge_enable: Boolean,
        @SerializedName("enable_dine_in_service_charge")
        val enable_dine_in_service_charge: Boolean,
        @SerializedName("lock_screen_after_each_transaction")
        val lock_screen_after_each_transaction: Boolean,
        @SerializedName("show_table_name")
        val show_table_name: Boolean,
        @SerializedName("only_show_price_greater_than_zero")
        val only_show_price_greater_than_zero: Boolean,
        @SerializedName("order_number_starting_from_one")
        val order_number_starting_from_one: Boolean,
        @SerializedName("report_start_time")
        val report_start_time: String,
        @SerializedName("report_end_time")
        val report_end_time: String,
        @SerializedName("shift_report_configuration")
        val shift_report_configuration: ShiftRportConfiguration?,
        @SerializedName("time_zone_options")
        val time_zone_options: List<TbTimeZones>,
        @SerializedName("customer_sign_required_on_cd")
        val customer_sign_required_on_cd: Boolean,
        @SerializedName("show_cash_credit_price_on_customer_display")
        val show_cash_credit_price_on_customer_display: Boolean
    ) {
        data class Printer(
            @SerializedName("customer_receipt_printers")
            val customerPrinterList: List<PrinterResponse.Data.CustomerReceiptPrinters>,
            @SerializedName("kitchen_receipt_printers")
            val kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters>
        )

        @Entity(tableName = "TbCancelOrderReason")
        data class CancelOrderReason(
            @SerializedName("created_at")
            val createdAt: String,
            @PrimaryKey
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("reason")
            val reason: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("is_deleted")
            val isDeleted: Boolean
        )

        @Parcelize
        @Entity(tableName = "TbWastageReason")
        data class WastageReason (
            @PrimaryKey
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("name")
            val name: String,
            @SerializedName("sort")
            val sort: Int,
            @SerializedName("location_id")
            val locationID: Int,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("deleted_at")
            val deletedAt: String? = null
        ) : Parcelable

        data class MagensaSettings(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("location_id")
            val location_id: Int,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("processor_name")
            val processor_name: String,
            @SerializedName("customer_name")
            val customer_name: String,
            @SerializedName("customer_code")
            val customer_code: String,
            @SerializedName("user_name")
            val user_name: String,
            @SerializedName("password")
            val password: String,
            @SerializedName("mcc_code")
            val mcc_code: String,

            )

        data class Logo(
            @SerializedName("name")
            val name: String,
            @SerializedName("url")
            val logoUrl: String,
            @SerializedName("thumb")
            val thumb: Thumb,
            @SerializedName("record")
            val record: Record

        ) {
            data class Thumb(
                @SerializedName("url")
                val thumbUrl: String
            )

            data class Record(
                @SerializedName("business_name")
                val businessName: String,
                @SerializedName("business_website")
                val businessWebsite: Any,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("currency")
                val currency: Any,
                @SerializedName("customer_contact_email")
                val customerContactEmail: String,
                @SerializedName("daily_report_sending_time")
                val dailyReportSendingTime: String,
                @SerializedName("enable_tax")
                val enableTax: Boolean,
                @SerializedName("end_time_for_report_summary")
                val endTimeForReportSummary: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("latitude")
                val latitude: Int,
                @SerializedName("locale")
                val locale: Any,
                @SerializedName("location_category_id")
                val locationCategoryId: Any,
                @SerializedName("longitude")
                val longitude: Int,
                @SerializedName("phone_number")
                val phoneNumber: String,
                @SerializedName("phone_number_2")
                val phoneNumber2: Any,
                @SerializedName("report_end_time")
                val reportEndTime: String,
                @SerializedName("report_start_time")
                val reportStartTime: String,
                @SerializedName("service_charge_enable")
                val serviceChargeEnable: Boolean,
                @SerializedName("start_time_for_report_summary")
                val startTimeForReportSummary: String,
                @SerializedName("subdomain")
                val subdomain: String,
                @SerializedName("terms_and_conditions")
                val termsAndConditions: Any,
                @SerializedName("time_zone")
                val timeZone: String,
                @SerializedName("updated_at")
                val updatedAt: String,
                @SerializedName("user_id")
                val userId: Int
            )
        }

        @Entity(tableName = "TbTerminals")
        data class Terminal(
            @SerializedName("created_at")
            val createdAt: String,
            @PrimaryKey
            @SerializedName("id")
            val id: Int,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("master_terminal")
            val masterTerminal: Boolean,
            @SerializedName("name")
            val name: String,
            @SerializedName("uniq_id")
            val uniqId: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("enabled_for_receiving_web_order")
            val enabled_for_receiving_web_order: Boolean? = null,
            @SerializedName("is_deleted")
            var isDeleted: Boolean = false
        )
    }
}