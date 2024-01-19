package com.pays.pos.ui.fragments.magtek


import com.google.gson.annotations.SerializedName

data class ProcessTokenRequest(
    @SerializedName("AdditionalRequestData")
    val additionalRequestData: List<ProcessCardSwipeRequest.TransactionInput.KeyValue>? = null,
    @SerializedName("Authentication")
    val authentication: ProcessCardSwipeRequest.Authentication,
    @SerializedName("CustomerTransactionID")
    val customerTransactionID: String,
    @SerializedName("Token")
    val token: String? = null,
    @SerializedName("TransactionInput")
    val transactionInput: ProcessCardSwipeRequest.TransactionInput
)
