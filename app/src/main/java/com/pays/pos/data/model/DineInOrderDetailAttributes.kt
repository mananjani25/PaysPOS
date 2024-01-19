package com.pays.pos.data.model

import com.google.gson.annotations.SerializedName

data class DineInOrderDetailAttributes(

    @SerializedName("id") var id: Int? = null,
    @SerializedName("total_guest_count") var totalGuestCount: Int? = null,
    @SerializedName("order_id") var orderId: Int? = null,
    @SerializedName("floor_plan_id") var floorPlanId: Int? = null,
    @SerializedName("floor_plan_table_id") var floorPlanTableId: Int? = null,
    @SerializedName("table_type") var tableType: String? = null,
    @SerializedName("table_number") var tableNumber: Int? = null,
    @SerializedName("chair_count") var chairCount: Int? = null,
    @SerializedName("table_name") var tableName: String? = null,
    @SerializedName("floor_plan_name") var floorPlanName: String? = null,


)
