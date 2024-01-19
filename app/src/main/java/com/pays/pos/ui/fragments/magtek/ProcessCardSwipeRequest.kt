package com.pays.pos.ui.fragments.magtek


import com.pays.pos.data.model.responseModel.report.KeyValue
import com.google.gson.annotations.SerializedName

data class ProcessCardSwipeRequest(
    @SerializedName("Authentication")
    val authentication: Authentication,
    @SerializedName("CardSwipeInput")
    val cardSwipeInput: CardSwipeInput,
    @SerializedName("TransactionInput")
    val transactionInput: TransactionInput,
    @SerializedName("CustomerTransactionID")
    val customerTransactionID: String,

    ) {
    data class Authentication(
        @SerializedName("CustomerCode")
        val customerCode: String,
        @SerializedName("Password")
        val password: String,
        @SerializedName("Username")
        val username: String
    )

    data class CardSwipeInput(
        @SerializedName("EncryptedCardSwipe")
        val encryptedCardSwipe: EncryptedCardSwipe
    ) {
        data class EncryptedCardSwipe(
            @SerializedName("KSN")
            val kSN: String,
            @SerializedName("MagnePrint")
            val magnePrint: String,
            @SerializedName("MagnePrintStatus")
            val magnePrintStatus: String,
            @SerializedName("Track2")
            val track2: String
        )
    }

    data class TransactionInput(
        @SerializedName("Amount")
        val amount: String? = null,
        @SerializedName("ProcessorName")
        val processorName: String,
        @SerializedName("TransactionType")
        val transactionType: Int,
        @SerializedName("TransactionInputDetails")
        val transactionInputDetails: List<KeyValue>? = null,
        @SerializedName("ReferenceAuthCode")
        val referenceAuthCode: String? = null,
        @SerializedName("ReferenceTransactionID")
        val referenceTransactionID: String? = null,

        ) {
        data class KeyValue(
            @SerializedName("key")
            val key: String,
            @SerializedName("value")
            val value: String
        )
    }

    data class TransactionInputRapid(
        @SerializedName("Amount")
        val amount: Int,
        @SerializedName("ProcessorName")
        val processorName: String,
        @SerializedName("TransactionType")
        val transactionType: Int,
        @SerializedName("TransactionInputDetails")
        val transactionInputDetails: List<KeyValue>? = null,
        @SerializedName("ReferenceAuthCode")
        val referenceAuthCode: String? = null,
        @SerializedName("ReferenceTransactionID")
        val referenceTransactionID: String? = null,

        ) {
        data class KeyValue(
            @SerializedName("key")
            val key: String,
            @SerializedName("value")
            val value: String
        )
    }
}