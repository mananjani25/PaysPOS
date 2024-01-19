package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ServiceChargeUpdate(
    @SerializedName("data") val `data`: Data,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String
) {
    data class Data(
        @SerializedName("service_charge_enable") val serviceChargeEnable: Boolean,
        @SerializedName("id") val id: Int,
        @SerializedName("logo") val logo: Logo,
        @SerializedName("online_ordering_qr_code") val onlineOrderingQrCode: OnlineOrderingQrCode,
        @SerializedName("business_name") val businessName: String,
        @SerializedName("phone_number") val phoneNumber: String,
        @SerializedName("customer_contact_email") val customerContactEmail: String,
        @SerializedName("business_website") val businessWebsite: String,
        @SerializedName("user_id") val userId: Int,
        @SerializedName("enable_tax") val enableTax: Boolean,
        @SerializedName("locale") val locale: Any,
        @SerializedName("currency") val currency: Any,
        @SerializedName("time_zone") val timeZone: String,
        @SerializedName("subdomain") val subdomain: String,
        @SerializedName("report_start_time") val reportStartTime: String,
        @SerializedName("report_end_time") val reportEndTime: String,
        @SerializedName("terms_and_conditions") val termsAndConditions: Any,
        @SerializedName("latitude") val latitude: Int,
        @SerializedName("longitude") val longitude: Int,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("updated_at") val updatedAt: String,
        @SerializedName("location_category_id") val locationCategoryId: Int,
        @SerializedName("daily_report_sending_time") val dailyReportSendingTime: String,
        @SerializedName("phone_number_2") val phoneNumber2: String,
        @SerializedName("magtek_epx_id") val magtekEpxId: String,
        @SerializedName("magtek_epx_key") val magtekEpxKey: String,
        @SerializedName("is_printer_queue_enable") val isPrinterQueueEnable: Boolean,
        @SerializedName("sign_in_with_otp") val signInWithOtp: Boolean,
        @SerializedName("otp_phone_number") val otpPhoneNumber: Any,
        @SerializedName("is_include_tax") val isIncludeTax: Boolean,
        @SerializedName("enable_dine_in_service_charge") val enableDineInServiceCharge: Boolean,
        @SerializedName("phone_number_1_country") val phoneNumber1Country: Any,
        @SerializedName("phone_number_2_country") val phoneNumber2Country: Any,
        @SerializedName("show_item_with_images") val showItemWithImages: Boolean,
        @SerializedName("merchant_profile_id") val merchantProfileId: Int
    ) {
        data class Logo(
            @SerializedName("url") val url: String,
            @SerializedName("thumb") val thumb: Thumb
        ) {
            data class Thumb(
                @SerializedName("url") val url: String
            )
        }

        data class OnlineOrderingQrCode(
            @SerializedName("url") val url: String,
            @SerializedName("thumb") val thumb: Thumb
        ) {
            data class Thumb(
                @SerializedName("url") val url: String
            )
        }
    }
}