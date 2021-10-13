package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName
import android.os.Parcelable

import kotlinx.parcelize.Parcelize


@Parcelize
data class GetFloorPlanResponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("type")
    val type: String
) : Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("floor_plan_tables")
        val floorPlanTables: List<FloorPlanTable>,
        @SerializedName("id")
        val id: Int,
        @SerializedName("name")
        val name: String
    ) : Parcelable {
        @Parcelize
        data class FloorPlanTable(
            @SerializedName("chair_count")
            val chairCount: Int,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("floor_plan_id")
            val floorPlanId: Int,
            @SerializedName("height")
            val height: Double,
            @SerializedName("id")
            val id: Int,
            @SerializedName("status")
            val status: String,
            @SerializedName("style")
            val style: String,
            @SerializedName("table_name")
            val tableName: String,
            @SerializedName("table_number")
            val tableNumber: Int,
            @SerializedName("table_type")
            val tableType: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("width")
            val width: Double,
            @SerializedName("x_position")
            val xPosition: Double,
            @SerializedName("y_position")
            val yPosition: Double,
            @SerializedName("current_order_details")
            var currentOrderDetails: CurrentOrderDetails
        ) : Parcelable {
            @Parcelize
            data class CurrentOrderDetails(
                @SerializedName("no_of_guests") var noOfGuests: Int,
                @SerializedName("employee_id") var employeeId: Int,
                @SerializedName("employee_name") var employeeName: String,
                @SerializedName("customer_id") var customerId: String,
                @SerializedName("customer_name") var customerName: String,
                @SerializedName("order_id") var orderId: Int,
                @SerializedName("total_amount") var totalAmount: Int

            ) : Parcelable
        }
    }
}