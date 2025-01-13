package com.pays.pos.data.model.valor

import com.google.gson.annotations.SerializedName

data class ValorTransactionsList(
    val nameValuePairs: NameValuePairs,
)

data class NameValuePairs(
    val status: String,
    val statusMsg: String,
    val batchSummary: BatchSummary,
    val batchSummaryDetails: BatchSummaryDetails,
    val epiInfo: EpiInfo,
    val meta: Meta,
)

data class BatchSummary(
    val values: List<Value>,
)

data class Value(
    val nameValuePairs: NameValuePairs2,
)

data class NameValuePairs2(
    @SerializedName("dashboard_netamount")
    val dashboardNetamount: Long,
    @SerializedName("dashboard_batchcount")
    val dashboardBatchcount: Long,
)

data class BatchSummaryDetails(
    val values: List<Value2>,
)

data class Value2(
    val nameValuePairs: NameValuePairs3,
)

data class NameValuePairs3(
    @SerializedName("tip_adjusted_count")
    val tipAdjustedCount: Long,
    @SerializedName("txn_id")
    val txnId: Long,
    @SerializedName("epi_id")
    val epiId: String,
    @SerializedName("message_type")
    val messageType: String,
    @SerializedName("processing_code")
    val processingCode: String,
    @SerializedName("txn_type")
    val txnType: String,
    @SerializedName("txn_type_code")
    val txnTypeCode: Long,
    val amount: Long,
    @SerializedName("tip_amount")
    val tipAmount: Long,
    @SerializedName("cashback_amount")
    val cashbackAmount: Long,
    @SerializedName("custom_fee_amount")
    val customFeeAmount: Long,
    @SerializedName("isnew_txn")
    val isnewTxn: Long,
    @SerializedName("surcharge_fee_amount")
    val surchargeFeeAmount: Long,
    @SerializedName("merchant_fee_amount")
    val merchantFeeAmount: Long,
    @SerializedName("tax_amount")
    val taxAmount: Long,
    @SerializedName("tip_fee_amount")
    val tipFeeAmount: Long,
    @SerializedName("tax_fee_amount")
    val taxFeeAmount: Long,
    @SerializedName("city_tax_amount")
    val cityTaxAmount: Long,
    @SerializedName("state_tax_amount")
    val stateTaxAmount: Long,
    @SerializedName("original_amount")
    val originalAmount: Long,
    @SerializedName("tran_no")
    val tranNo: Long,
    @SerializedName("stan_no")
    val stanNo: String,
    @SerializedName("invoice_no")
    val invoiceNo: String,
    @SerializedName("batch_no")
    val batchNo: String,
    @SerializedName("pos_entry_mode")
    val posEntryMode: String,
    @SerializedName("masked_card_no")
    val maskedCardNo: String,
    @SerializedName("card_scheme")
    val cardScheme: String,
    @SerializedName("request_date")
    val requestDate: String,
    @SerializedName("request_time")
    val requestTime: String,
    @SerializedName("store_time_zone")
    val storeTimeZone: String,
    val mid: String,
    val tid: String,
    val rrn: String,
    @SerializedName("approval_code")
    val approvalCode: String,
    @SerializedName("card_menu_selection")
    val cardMenuSelection: String,
    @SerializedName("response_code")
    val responseCode: String,
    @SerializedName("switch_response_code")
    val switchResponseCode: String,
    @SerializedName("settled_at")
    val settledAt: String,
    @SerializedName("is_voided")
    val isVoided: Long,
    @SerializedName("is_auth_completed")
    val isAuthCompleted: Long,
    @SerializedName("created_at")
    val createdAt: String,
    val txamount: Long,
    @SerializedName("surcharge_label")
    val surchargeLabel: String,
)

data class EpiInfo(
    val nameValuePairs: NameValuePairs4,
)

data class NameValuePairs4(
    @SerializedName("tip_adjust")
    val tipAdjust: Boolean,
    @SerializedName("max_tip")
    val maxTip: String,
)

data class Meta(
    val nameValuePairs: NameValuePairs5,
)

data class NameValuePairs5(
    @SerializedName("number_of_records")
    val numberOfRecords: Long,
    @SerializedName("total_pages")
    val totalPages: Long,
    @SerializedName("total_results")
    val totalResults: Long,
    @SerializedName("page_number")
    val pageNumber: Long,
    @SerializedName("page_size")
    val pageSize: Long,
)
