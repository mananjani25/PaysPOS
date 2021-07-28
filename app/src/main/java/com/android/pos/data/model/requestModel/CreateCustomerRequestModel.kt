package com.android.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class CreateCustomerRequestModel(
    @SerializedName("customer")
    var `data`: Customer,
) {
    data class Customer(
        @SerializedName("first_name")
        var first_name: String = "",
        @SerializedName("last_name")
        var last_name: String = "",
        @SerializedName("company")
        var company: String = "",
        @SerializedName("birth_day")
        var birth_day: String = "",
        @SerializedName("birth_month")
        var birth_month: String = "",
        @SerializedName("birthday_year")
        var birthday_year: String = "",
        @SerializedName("email")
        var email: String
    ) {
        data class Phone(
            @SerializedName("id")
            var id: Int = 0,
            @SerializedName("phone_number")
            var phone_number: String = "",
            @SerializedName("_destroy")
            var _destroy: String = "true"

        )
        data class Addresses(
            @SerializedName("id")
            var id:Int = 0,
            @SerializedName("address1")
            var address1:String = "",
            @SerializedName("address2")
            var address2:String ="",
            @SerializedName("city")
            var city:String = "",
            @SerializedName("state")
            var state:String = "",
            @SerializedName("country")
            var country:String = "",
            @SerializedName("postcode")
            var postcode:String ="",
            @SerializedName("type_of_address")
            var type_of_address:String="Shipping",
            @SerializedName("latitude")
            var latitude:Double = 0.0,
            @SerializedName("longitude")
            var longitude:Double = 0.0
        )

    }
}
