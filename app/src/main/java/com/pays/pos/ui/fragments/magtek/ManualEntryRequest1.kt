package com.pays.pos.ui.fragments.magtek


import com.google.gson.annotations.SerializedName

data class ManualEntryRequestRapid(
    @SerializedName("Authentication") val authentication: ProcessCardSwipeRequest.Authentication,
    @SerializedName("CustomerTransactionID") val customerTransactionID: String? = null,
    @SerializedName("ManualEntryInput") val manualEntryInput: ManualEntryInput,
    @SerializedName("TransactionInput") val transactionInput: ProcessCardSwipeRequest.TransactionInputRapid
) {

    data class ManualEntryInput(
        @SerializedName("CVV") val cVV: String,
        @SerializedName("ExpirationDate") val expirationDate: String,
        @SerializedName("PAN") val pAN: String
    )
}
