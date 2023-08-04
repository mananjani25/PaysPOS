package com.android.pos.data.model.requestModel.giftCard.response

import com.google.gson.annotations.SerializedName

data class GiftCard(
    val id: Int,
    val amount: String,
    val customer_id: Int,
    val location_id: Int,
    val name: String,
    val password: String,
    val gift_card_type: String,
    val customer: Customer,
    val employee: Employee,
    val payments: List<Payment>,
    val order_type_name: String,
){
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

    data class Employee(

        @SerializedName("id") var id: Int,
        @SerializedName("name") var name: String,
        @SerializedName("email") var email: String,
        @SerializedName("phone_number") var phoneNumber: String,
        @SerializedName("location_id") var locationId: Int,
        @SerializedName("passcode") var passcode: String,
        @SerializedName("is_active") var isActive: Boolean,
        @SerializedName("created_at") var createdAt: String,
        @SerializedName("updated_at") var updatedAt: String,
        @SerializedName("loggedin_terminal_id") var loggedinTerminalId: Int,
        @SerializedName("is_clocked_in") var isClockedIn: Boolean,
        @SerializedName("first_name") var firstName: String,
        @SerializedName("last_name") var lastName: String,
        @SerializedName("team_role_id") var teamRoleId: Int,
        @SerializedName("hourly_wages") var hourlyWages: Double

    )
}