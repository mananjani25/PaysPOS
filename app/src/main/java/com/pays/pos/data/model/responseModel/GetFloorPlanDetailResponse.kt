package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class GetFloorPlanDetailResponse(
    @SerializedName("data") val data: List<Data>,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String
) : Parcelable {
    @Parcelize
    data class Data(

        @SerializedName("id") val id: Int,
        @SerializedName("name") val name: String,
        @SerializedName("floor_plan_tables") val floor_plan_tables: List<FloorPlanTables>
    ) : Parcelable {}

    @Parcelize
    data class FloorPlanTables(

        @SerializedName("id") val id: Int,
        @SerializedName("height") val height: Double,
        @SerializedName("width") val width: Double,
        @SerializedName("x_position") val x_position: Double,
        @SerializedName("y_position") val y_position: Double,
        @SerializedName("table_name") val table_name: String,
        @SerializedName("table_number") val table_number: Int,
        @SerializedName("status") val status: String,
        @SerializedName("chair_count") val chair_count: Int,
        @SerializedName("floor_plan_id") val floor_plan_id: Int,
        @SerializedName("table_type") val table_type: String,
        @SerializedName("style") val style: String,
        @SerializedName("merged_floor_plan_table_id") val merged_floor_plan_table_id: Int,
        @SerializedName("lock_by_id") val lock_by_id: Int,
        @SerializedName("lock_by_name") val lock_by_name: String,
        @SerializedName("terminal_id") val terminal_id: Int,
        @SerializedName("child_table") val child_table: Boolean,
        @SerializedName("parent_table") val parent_table: Boolean,
        @SerializedName("order_details") val order_details: OrderDetails? = null
    ) : Parcelable {}

    @Parcelize
    data class OrderDetails(

        @SerializedName("id") val id: Int,
        @SerializedName("creation_time_on_terminal") val creation_time_on_terminal: String,
        @SerializedName("offline_id") val offline_id: String,
        @SerializedName("employee_id") val employee_id: Int,
        @SerializedName("terminal_id") val terminal_id: Int,
        @SerializedName("location_id") val location_id: Int,
        @SerializedName("date") val date: String,
        @SerializedName("index_of_date") val index_of_date: String,
        @SerializedName("note") val note: String,
        @SerializedName("order_type_id") val order_type_id: Int,
        @SerializedName("customer_id") val customer_id: Int,
        @SerializedName("open_order_type_id") val open_order_type_id: String,
        @SerializedName("integer") val integer: String,
        @SerializedName("service_charge_enabled") val service_charge_enabled: Boolean,
        @SerializedName("tax_enabled") val tax_enabled: Boolean,
        @SerializedName("total_cash_discount_fee") val total_cash_discount_fee: Int,
        @SerializedName("sub_total") val sub_total: Double,
        @SerializedName("total_discount") val total_discount: Double,
        @SerializedName("discount_type_id") val discount_type_id: String,
        @SerializedName("payment_status") val payment_status: String,
        @SerializedName("total_tips") val total_tips: Double,
        @SerializedName("total_tax_amount") val total_tax_amount: Double,
        @SerializedName("future_delivery_date") val future_delivery_date: String,
        @SerializedName("is_edited") val is_edited: Boolean,
        @SerializedName("edited_order_timestamp") val edited_order_timestamp: String,
        @SerializedName("edit_order_count") val edit_order_count: String,
        @SerializedName("future_delivery_time") val future_delivery_time: String,
        @SerializedName("total_service_charges") val total_service_charges: Double,
        @SerializedName("open_order_type") val open_order_type: String,
        @SerializedName("delivery_type") val delivery_type: String,
        @SerializedName("delivery_employee_id") val delivery_employee_id: String,
        @SerializedName("discount_id") val discount_id: Int,
        @SerializedName("created_at") val created_at: String,
        @SerializedName("updated_at") val updated_at: String,
        @SerializedName("loyalty_program_id") val loyalty_program_id: String,
        @SerializedName("loyalty_amount") val loyalty_amount: Double,
        @SerializedName("is_loyalty_applied") val is_loyalty_applied: Boolean,
        @SerializedName("used_reward_points") val used_reward_points: Int,
        @SerializedName("order_items") val order_items: List<GetOrderDetailsResponse.Data.OrderItem>,
        @SerializedName("order_service_charges") val order_service_charges: List<GetOrderDetailsResponse.Data.OrderServiceCharge>,
        @SerializedName("payments") val payments: List<GetOrderDetailsResponse.Data.Payment>,
        @SerializedName("employee") val employee: GetOrderDetailsResponse.Data.Employee,
        @SerializedName("customer") val customer: GetOrderDetailsResponse.Data.Customer,
        @SerializedName("order_type") val order_type: String,
        //@SerializedName("venue_details") val venue_details: VenueDetailsResponse.Data,
        @SerializedName("refund_detail") val refund_detail: GetOrderDetailsResponse.Data.RefundDetails,
        @SerializedName("digital_receipt_url") val digital_receipt_url: String,
        @SerializedName("total_amount") val total_amount: Double,
        @SerializedName("floor_plan_table") val floor_plan_table: FloorPlanTables,
        @SerializedName("guest_attributes") val guest_attributes: List<CreateOrderResponse.Data.Order.GuestAttributes>,
        @SerializedName("merged_table_nos") val merged_table_nos: String
    ) : Parcelable

}