package com.android.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName


data class GetFloorPlanDetailResponse(
    @SerializedName("data") val data : List<Data>,
    @SerializedName("type") val type : String,
    @SerializedName("status") val status : Int,
    @SerializedName("message") val message : String
){
    data class Data (

        @SerializedName("id") val id : Int,
        @SerializedName("name") val name : String,
        @SerializedName("floor_plan_tables") val floor_plan_tables : List<FloorPlanTables>
    )

    data class FloorPlanTables (

        @SerializedName("id") val id : Int,
        @SerializedName("height") val height : Double,
        @SerializedName("width") val width : Double,
        @SerializedName("x_position") val x_position : Double,
        @SerializedName("y_position") val y_position : Double,
        @SerializedName("table_name") val table_name : String,
        @SerializedName("table_number") val table_number : Int,
        @SerializedName("status") val status : String,
        @SerializedName("chair_count") val chair_count : Int,
        @SerializedName("floor_plan_id") val floor_plan_id : Int,
        @SerializedName("table_type") val table_type : String,
        @SerializedName("style") val style : String,
        @SerializedName("merged_floor_plan_table_id") val merged_floor_plan_table_id : String,
        @SerializedName("lock_by_id") val lock_by_id : String,
        @SerializedName("lock_by_name") val lock_by_name : String,
        @SerializedName("terminal_id") val terminal_id : String,
        @SerializedName("child_table") val child_table : Boolean,
        @SerializedName("parent_table") val parent_table : Boolean,
        @SerializedName("order_details") val order_details : GetFloorPlanResponse.Data.FloorPlanTable.CurrentOrderDetails
    )


}