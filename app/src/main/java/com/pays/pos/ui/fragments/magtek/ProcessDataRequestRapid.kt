package com.pays.pos.ui.fragments.magtek


import com.google.gson.annotations.SerializedName

data class ProcessDataRequestRapid(
    @SerializedName("Authentication")
    val authentication: ProcessCardSwipeRequest.Authentication,
    @SerializedName("CustomerTransactionID")
    val customerTransactionID: String,
    @SerializedName("DataInput")
    val dataInput: ProcessDataRequest.DataInput,
    @SerializedName("TransactionInput")
    val transactionInput: ProcessCardSwipeRequest.TransactionInputRapid
) {


    data class DataInput(
        @SerializedName("Data")
        val `data`: String,
        @SerializedName("DataFormatType")
        val dataFormatType: Int,
        @SerializedName("EncryptionInfo")
        val encryptionInfo: EncryptionInfo,
        @SerializedName("IsEncrypted")
        val isEncrypted: Boolean,
        @SerializedName("PaymentMode")
        val paymentMode: Int
    ) {
        data class EncryptionInfo(
            @SerializedName("EncryptionType")
            val encryptionType: String,
            @SerializedName("NumberOfPaddedBytes")
            val numberOfPaddedBytes: String
        )
    }

}