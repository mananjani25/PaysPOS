package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.pays.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.pays.pos.data.model.requestModel.PaymentAttributes
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize

data class PrinterQueueReponse(
    @SerializedName("data") val data: List<Data>?=null,
    @SerializedName("type") val type: String="",
    @SerializedName("status") val status: Int?=null,
    @SerializedName("message") val message: String=""
) : Parcelable {
    @Parcelize
    data class Data(

        @SerializedName("id") val id: Int,
        @SerializedName("printer_list") val printer_list: List<String>,
        @SerializedName("order_type") val order_type: String,
        @SerializedName("printer_id") val printer_id: String,
        @SerializedName("location_id") val location_id: Int,
        @SerializedName("terminal_name") val terminal_name: String,
        @SerializedName("printer_failed_ids") val printer_failed_ids: List<String>,
        @SerializedName("terminal_id") val terminal_id: Int,
        @SerializedName("order_data") val order_data:OrderData,
       // @SerializedName("order_item_attributes") val order_item_attributes: List<CreateOrderResponse.Data.Order.OrderItem>
    ) : Parcelable {

        @Parcelize
        data class OrderData (

            @SerializedName("cash_discount_or_surcharge") val cash_discount_or_surcharge : Int,
            @SerializedName("cash_discount_type") val cash_discount_type : String,
            @SerializedName("date") val date : String,
            @SerializedName("delivery_type") val delivery_type : String,
            @SerializedName("employee_id") val employee_id : Int,
            @SerializedName("future_delivery_date") val future_delivery_date : String,
            @SerializedName("future_delivery_time") val future_delivery_time : String,
            @SerializedName("guests_attributes") val guests_attributes : List<String>,
            @SerializedName("is_loyalty_applied") val is_loyalty_applied : Boolean,
            @SerializedName("location_id") val location_id : Int,
            @SerializedName("loyalty_amount") val loyalty_amount : Int,
            @SerializedName("loyalty_program_id") val loyalty_program_id : String,
            @SerializedName("merged_order_ids") val merged_order_ids : List<String>,
            @SerializedName("note") val note : String,
            @SerializedName("offline_id") val offline_id : String,
            @SerializedName("open_order_type") val open_order_type : String,
            @SerializedName("order_items_attributes") val order_items_attributes : List<CreateOrderResponse.Data.Order.OrderItem>,
            @SerializedName("order_service_charges_attributes") val order_service_charges_attributes : List<CreateServiceChargeRequestModel.ServiceCharge>,
            @SerializedName("order_type_id") val order_type_id : Int,
            @SerializedName("payment_attributes") val payment_attributes : PaymentAttributes,
            @SerializedName("payment_status") val payment_status : Int,
            @SerializedName("service_charge_enabled") val service_charge_enabled : Boolean,
            @SerializedName("sub_total") val sub_total : Int,
            @SerializedName("tax_enabled") val tax_enabled : Boolean,
            @SerializedName("terminal_id") val terminal_id : Int,
            @SerializedName("total_amount") val total_amount : Int,
            @SerializedName("total_discount") val total_discount : Int,
            @SerializedName("total_service_charges") val total_service_charges : Int,
            @SerializedName("total_tax_amount") val total_tax_amount : Int,
            @SerializedName("total_tips") val total_tips : Int,
            @SerializedName("used_reward_points") val used_reward_points : Int
        ):Parcelable{

        }

    }

}
