package com.pays.pos.ui.fragments.magtek


import com.google.gson.annotations.SerializedName

class PaymentResponse : ArrayList<PaymentResponse.PaymentResponseItem>() {
    data class PaymentResponseItem(
        @SerializedName("AdditionalResponseData")
        val additionalResponseData: Any,
        @SerializedName("CardSwipeOutput")
        val cardSwipeOutput: CardSwipeOutput?,
        @SerializedName("CustomerTransactionID")
        val customerTransactionID: String?,
        @SerializedName("DataOutput")
        val dataOutput: DataOutput?,
        @SerializedName("MPPGv4WSFault")
        val mPPGv4WSFault: MPPGv4WSFault?,
        @SerializedName("MagTranID")
        val magTranID: String,
        @SerializedName("TransactionOutput")
        val transactionOutput: TransactionOutput?,
        @SerializedName("TransactionUTCTimestamp")
        val transactionUTCTimestamp: String
    ) {
        data class CardSwipeOutput(
            @SerializedName("AdditionalOutputData")
            val additionalOutputData: List<AdditionalOutputData>?,
            @SerializedName("CardID")
            val cardID: String,
            @SerializedName("IsReplay")
            val isReplay: Boolean,
            @SerializedName("MagnePrintScore")
            val magnePrintScore: Double,
            @SerializedName("PANLast4")
            val pANLast4: String
        ) {
            data class AdditionalOutputData(
                @SerializedName("key")
                val key: String,
                @SerializedName("value")
                val value: String
            )
        }

        data class DataOutput(
            @SerializedName("AdditionalOutputData")
            val additionalOutputData: List<AdditionalOutputData>?,
            val CardID: String,
            val IsReplay: Boolean,
            val PANLast4: String
        ) {
            data class AdditionalOutputData(
                val key: String,
                val value: String
            )
        }

        data class TransactionOutput(
            @SerializedName("AVSResult")
            val aVSResult: Any,
            @SerializedName("AuthCode")
            val authCode: String,
            @SerializedName("AuthorizedAmount")
            val authorizedAmount: Double,
            @SerializedName("CVVResult")
            val cVVResult: Any,
            @SerializedName("IsTransactionApproved")
            val isTransactionApproved: Boolean,
            @SerializedName("IssuerAuthenticationData")
            val issuerAuthenticationData: Any,
            @SerializedName("IssuerScriptTemplate1")
            val issuerScriptTemplate1: Any,
            @SerializedName("IssuerScriptTemplate2")
            val issuerScriptTemplate2: Any,
            @SerializedName("Token")
            val token: String,
            @SerializedName("TransactionID")
            val transactionID: String,
            @SerializedName("TransactionMessage")
            val transactionMessage: String,
            @SerializedName("TransactionOutputDetails")
            val transactionOutputDetails: List<TransactionOutputDetail>,
            @SerializedName("TransactionStatus")
            val transactionStatus: String
        ) {
            data class TransactionOutputDetail(
                @SerializedName("key")
                val key: String,
                @SerializedName("value")
                val value: String
            )
        }

        data class MPPGv4WSFault(
            @SerializedName("AdditionalFaultData")
            val additionalFaultData: Any,
            @SerializedName("CustomerTransactionID")
            val customerTransactionID: Any,
            @SerializedName("FaultCode")
            val faultCode: String,
            @SerializedName("FaultReason")
            val faultReason: String,
            @SerializedName("MagTranID")
            val magTranID: String,
            @SerializedName("TransactionUTCTimestamp")
            val transactionUTCTimestamp: String
        )
    }
}