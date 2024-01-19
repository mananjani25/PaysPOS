package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
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
        var floorPlanTables: List<FloorPlanTable>?,
        @SerializedName("id")
        val id: Int,
        @SerializedName("name")
        val name: String
    ) : Parcelable {
        @Parcelize
        data class FloorPlanTable(
            @SerializedName("chair_count")
            var chairCount: Int,
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
            @SerializedName("lock_by_id")
            val lock_by_id: Int? = null,
            @SerializedName("lock_by_name")
            val lock_by_name: String? = null,
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
            @SerializedName("y_top")
            val yTop: Double,
            @SerializedName("x_left")
            val xLeft: Double,

            @SerializedName("child_table")
            val childTable: Boolean,
            @SerializedName("parent_table")
            val parentTable: Boolean,
            @SerializedName("merged_floor_plan_table_id")
            val mergedFloorPlanTable_id: Int?,
            @SerializedName("current_order_details")
            var currentOrderDetails: CurrentOrderDetails?,
            @SerializedName("merged_child_table_details")
            val merged_child_table_details: List<MergedChildTableDetails>?,
        ) : Parcelable {
            @Parcelize
            data class CurrentOrderDetails(
                @SerializedName("no_of_guests") var noOfGuests: Int,
                @SerializedName("employee_id") var employeeId: Int,
                @SerializedName("employee_name") var employeeName: String,
                @SerializedName("customer_id") var customerId: String?,
                @SerializedName("customer_name") var customerName: String?,
                @SerializedName("order_id") var orderId: Int,
                @SerializedName("total_amount") var totalAmount: Double

            ) : Parcelable

            @Parcelize
            data class MergedChildTableDetails(

                @SerializedName("table_name") val table_name: String,
                @SerializedName("table_number") val table_number: Int,
                @SerializedName("chair_count") val chair_count: Int
            ) : Parcelable
        }
    }
}