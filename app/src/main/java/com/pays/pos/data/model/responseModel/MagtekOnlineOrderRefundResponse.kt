package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class MagtekOnlineOrderRefundResponse(
    @SerializedName("AdditionalResponseData") val additionalResponseData: String,
    @SerializedName("CustomerTransactionID") val customerTransactionID: String,
    @SerializedName("MPPGv4WSFault") val mPPGv4WSFault: String,
    @SerializedName("MagTranID") val magTranID: String,
    @SerializedName("TransactionOutput") val transactionOutput: TransactionOutput,
    @SerializedName("TransactionUTCTimestamp") val transactionUTCTimestamp: String
) {
    data class TransactionOutput(
        @SerializedName("AVSResult") val aVSResult: String,
        @SerializedName("AuthCode") val authCode: String,
        @SerializedName("AuthorizedAmount") val authorizedAmount: Int,
        @SerializedName("CVVResult") val cVVResult: String,
        @SerializedName("IsTransactionApproved") val isTransactionApproved: Boolean,
        @SerializedName("IssuerAuthenticationData") val issuerAuthenticationData: String,
        @SerializedName("IssuerScriptTemplate1") val issuerScriptTemplate1: String,
        @SerializedName("IssuerScriptTemplate2") val issuerScriptTemplate2: String,
        @SerializedName("Token") val token: String,
        @SerializedName("TransactionID") val transactionID: String,
        @SerializedName("TransactionMessage") val transactionMessage: String,
        @SerializedName("TransactionOutputDetails") val transactionOutputDetails: List<TransactionOutputDetail>,
        @SerializedName("TransactionStatus") val transactionStatus: String
    ) {
        data class TransactionOutputDetail(
            @SerializedName("key") val key: String,
            @SerializedName("value") val value: String
        )
    }
}