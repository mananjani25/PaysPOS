package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName


data class CashEventDetailsResponse (

  @SerializedName("data"    ) var data    : ArrayList<Data> = arrayListOf(),
  @SerializedName("type"    ) var type    : String?         = null,
  @SerializedName("status"  ) var status  : Int?            = null,
  @SerializedName("message" ) var message : String?         = null

){

  data class Data (

    @SerializedName("id"              ) var id            : Int?    = null,
    @SerializedName("payment_id"      ) var paymentId     : Int?    = null,
    @SerializedName("order_id"        ) var orderId       : Int?    = null,
    @SerializedName("amount"          ) var amount        : Double? = null,
    @SerializedName("total_tips"      ) var totalTips     : String? = null,
    @SerializedName("tip_setting_id"  ) var tipSettingId  : String? = null,
    @SerializedName("employee_id"     ) var employeeId    : Int?    = null,
    @SerializedName("terminal_id"     ) var terminalId    : Int?    = null,
    @SerializedName("event"           ) var event         : String? = null,
    @SerializedName("created_at"      ) var createdAt     : String? = null,
    @SerializedName("terminal_name"   ) var terminalName  : String? = null,
    @SerializedName("reason"          ) var reason        : String? = null,
    @SerializedName("employee_name"   ) var employeeName  : String? = null,
    @SerializedName("order_type_name" ) var orderTypeName : String? = null,
    @SerializedName("custom_order_id" ) var customOrderId : Int?    = null

  )
}