package com.pays.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("employee_data")
    val employeeData: List<Employee>?,
    @SerializedName("terminals")
    val terminals: List<Terminal>?,
    @SerializedName("sales_summary")
    val salesSummary: List<KeyValue>?,
    @SerializedName("refund_detail")
    val refundDetails: List<KeyValue>?,
    @SerializedName("pending_payments")
    val pendingPayments: List<KeyValue>?,
    @SerializedName("tax_details")
    val taxDetails: List<KeyValue>?,
    @SerializedName("discount_details")
    val discountDetails: List<KeyValue>?,
    @SerializedName("sales_and_tax_summary")
    val salesTaxSummary: List<KeyValue>?,
    @SerializedName("cash_events_summary")
    val cashEventSummary: List<KeyValue>?,
    @SerializedName("total_external_payments")
    val totalExternalPayments: List<KeyValue>?,
    @SerializedName("total_payments")
    val totalPayments: List<KeyValue>?,
    @SerializedName("cash_payments")
    val cashPayments: List<KeyValue>?,
    @SerializedName("payment_details")
    val paymentDetails: ArrayList<ArrayList<KeyValue>>?, // check
    @SerializedName("employee_report")
    val employeeReports: ArrayList<ArrayList<KeyValueWithString>>?,
    @SerializedName("other_details")
    val otherDetails: ArrayList<ArrayList<KeyValueWithString>>?,
    @SerializedName("service_charge_details")
    val serviceChargeDetails: ArrayList<ArrayList<KeyValue>>?,
    @SerializedName("tips_details")
    val tipsDetails: ArrayList<ArrayList<KeyValue>>?,// check
)
