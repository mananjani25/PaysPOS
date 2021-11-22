package com.android.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName


data class GetFloorPlanDetailResponse(
    @SerializedName("data") val data : Data,
    @SerializedName("type") val type : String,
    @SerializedName("status") val status : Int,
    @SerializedName("message") val message : String
){
    data class Data (

        @SerializedName("id") val id : Int,
        @SerializedName("name") val name : String,
        @SerializedName("floor_plan_tables") val floor_plan_tables : List<String>
    )


}