package com.pays.pos.ui.fragments.magtek


import com.google.gson.annotations.SerializedName

data class TsysCaptureRequest(
    @SerializedName("Authentication") val authentication: Authentication,
    @SerializedName("CustomerTransactionID") val customerTransactionID: String,
    @SerializedName("TransactionInput") val transactionInput: TransactionInput
) {
    data class Authentication(
        @SerializedName("CustomerCode") val customerCode: String,
        @SerializedName("Password") val password: String,
        @SerializedName("Username") val username: String
    )

    data class TransactionInput(
        @SerializedName("Amount") val amount: String,
        @SerializedName("ProcessorName") val processorName: String,
        @SerializedName("ReferenceTransactionID") val referenceTransactionID: String,
        @SerializedName("TransactionInputDetails") val transactionInputDetails: List<TransactionInputDetail>,
        @SerializedName("TransactionType") val transactionType: Int
    ) {
        data class TransactionInputDetail(
            @SerializedName("key") val key: String,
            @SerializedName("value") val value: String
        )
    }
}
