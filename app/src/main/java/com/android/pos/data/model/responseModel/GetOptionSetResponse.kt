package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class GetOptionSetResponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("display_name")
        val displayName: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("option_type")
        val optionType: String,
        @SerializedName("options")
        val options: List<Option>,
        @SerializedName("sort")
        val sort: Int
    ) {
        data class Option(
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("sort")
            val sort: Int
        )
    }
}