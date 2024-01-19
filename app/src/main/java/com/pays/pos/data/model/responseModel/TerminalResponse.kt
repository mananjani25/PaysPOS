package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class TerminalResponse(
    @SerializedName("data")
    val terminalData: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
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
    )
}