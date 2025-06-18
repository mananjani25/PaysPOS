package com.pays.pos.data.model.requestModel

import androidx.room.Entity
import com.google.gson.annotations.SerializedName


@Entity
data class CreateCustomerRequestModel(
    @SerializedName("customer")
    var data: Customer? = Customer(),
) {
    data class Customer(
        @SerializedName("first_name")
        var first_name: String = "",
        @SerializedName("last_name")
        var last_name: String = "",
        @SerializedName("company")
        var company: String = "",
        @SerializedName("birth_day")
        var birth_day: String? = "",
        @SerializedName("birth_month")
        var birth_month: String = "",
        @SerializedName("birthday_year")
        var birthday_year: String = "",
        @SerializedName("email")
        var email: String? = "",
        @SerializedName("enroll_to_loyalty")
        var enroll_to_loyalty: Boolean? = true,
        @SerializedName("same_as_billing_address")
        var same_as_billing_address: Boolean? = true,
        @SerializedName("final_reward")
        var final_reward: Int? = 0,
        @SerializedName("phones_attributes")
        var phones_attributes: ArrayList<Phone>? = arrayListOf(),
        @SerializedName("addresses_attributes")
        var addresses_attributes: ArrayList<Addresses>? = arrayListOf(),
        @SerializedName("is_tokenized")
        var isTokenized: Boolean = false,
        @SerializedName("card_token")
        var cardToken: String = "",
    ) {
        data class Phone(
            @SerializedName("id")
            var id: Int? = null,
            @SerializedName("phone_number")
            var phone_number: String = "",
            @SerializedName("_destroy")
            var _destroy: String = "false"
        )

        data class Addresses(
            @SerializedName("id")
            var id: Int? = null,
            @SerializedName("address1")
            var address1: String = "",
            @SerializedName("address2")
            var address2: String = "",
            @SerializedName("city")
            var city: String = "",
            @SerializedName("state")
            var state: String = "",
            @SerializedName("country")
            var country: String? = "",
            @SerializedName("postcode")
            var postcode: String = "",
            @SerializedName("type_of_address")
            var type_of_address: String = "Shipping",
            @SerializedName("latitude")
            var latitude: Double = 0.0,
            @SerializedName("longitude")
            var longitude: Double = 0.0,
            @SerializedName("_destroy")
            var _destroy: String = "false"
        )


    }
}
