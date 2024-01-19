package com.pays.pos.data.model.responseModel

import com.pays.pos.data.model.GetPaymentOrderDetailsResponse
import com.google.gson.annotations.SerializedName

data class CustomerAssignedResponse(
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
        @SerializedName("order")
        val `order`: Order,
    ) {

        data class Order(
            @SerializedName("id") val id: Int,
            @SerializedName("creation_time_on_terminal") val creation_time_on_terminal: String,
            @SerializedName("offline_id") val offline_id: String,
            @SerializedName("employee_id") val employee_id: Int,
            @SerializedName("terminal_id") val terminal_id: Int,
            @SerializedName("location_id") val location_id: Int,
            @SerializedName("date") val date: String,
            @SerializedName("index_of_date") val index_of_date: String,
            @SerializedName("note") val note: String,
            @SerializedName("order_type_id") val order_type_id: Int,
            @SerializedName("customer_id") val customer_id: String,
            @SerializedName("open_order_type_id") val open_order_type_id: String,
            @SerializedName("integer") val integer: String,
            @SerializedName("service_charge_enabled") val service_charge_enabled: Boolean,
            @SerializedName("tax_enabled") val tax_enabled: Boolean,
            @SerializedName("cash_discount_or_surcharge") val cash_discount_or_surcharge: Double,
            @SerializedName("sub_total") val sub_total: Double,
            @SerializedName("total_discount") val total_discount: Double,
            @SerializedName("discount_type_id") val discount_type_id: String,
            @SerializedName("payment_status") val payment_status: String,
            @SerializedName("total_tips") val total_tips: Double,
            @SerializedName("total_tax_amount") val total_tax_amount: Double,
            @SerializedName("future_delivery_date") val future_delivery_date: String,
            @SerializedName("is_edited") val is_edited: Boolean,
            @SerializedName("edited_order_timestamp") val edited_order_timestamp: String,
            @SerializedName("edit_order_count") val edit_order_count: String,
            @SerializedName("future_delivery_time") val future_delivery_time: String,
            @SerializedName("total_service_charges") val total_service_charges: Double,
            @SerializedName("open_order_type") val open_order_type: String,
            @SerializedName("delivery_type") val delivery_type: String,
            @SerializedName("order_split_type") val order_split_type: String,
            @SerializedName("delivery_employee_id") val delivery_employee_id: String,
            @SerializedName("discount_id") val discount_id: String,
            @SerializedName("created_at") val created_at: String,
            @SerializedName("updated_at") val updated_at: String,
            @SerializedName("employee") val employee: String,
            @SerializedName("customer") val customer: Customer,
            @SerializedName("order_type") val order_type: String,
            @SerializedName("venue_website") val venue_website: String,
            @SerializedName("digital_receipt_url") val digital_receipt_url: String,
            @SerializedName("total_amount") val total_amount: Double
        ) {

            data class Customer(

                @SerializedName("id") var id: Int,
                @SerializedName("first_name") var firstName: String,
                @SerializedName("last_name") var lastName: String,
                @SerializedName("company") var company: String,
                @SerializedName("location_id") var locationId: Int,
                @SerializedName("created_at") var createdAt: String,
                @SerializedName("enroll_to_loyalty") var enroll_to_loyalty: Boolean,
                @SerializedName("final_reward") var final_reward: String,
                @SerializedName("updated_at") var updatedAt: String,
                @SerializedName("birth_date") var birthDate: String,
                @SerializedName("email") var email: String,
                @SerializedName("phones") var phones: List<Phones>,
                @SerializedName("addresses") var addresses: List<Addresses>

            ) {
                data class Phones(
                    @SerializedName("id") var id: Int,
                    @SerializedName("phone_number") var phoneNumber: String
                )

                data class Addresses(

                    @SerializedName("id") var id: Int,
                    @SerializedName("address1") var address1: String,
                    @SerializedName("address2") var address2: String,
                    @SerializedName("city") var city: String,
                    @SerializedName("state") var state: String,
                    @SerializedName("country") var country: String,
                    @SerializedName("postcode") var postcode: String,
                    @SerializedName("type_of_address") var typeOfAddress: String,
                    @SerializedName("latitude") var latitude: String,
                    @SerializedName("longitude") var longitude: String,
                    @SerializedName("full_address") var fullAddress: String,
                    @SerializedName("street") var street: String

                )
            }
        }

    }
}