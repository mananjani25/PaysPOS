package com.android.pos.data.model

import com.android.pos.data.model.responseModel.GetFloorPlanDetailResponse

data class MergeTableModel(
    val id: Int,
    val name: String,
    val floorId: Int,
    val floorName: String,
    val isOccupied: Boolean = false,
    val orderId: Int? = null,
    var orderDetails: GetFloorPlanDetailResponse.OrderDetails? = null,
    var chairCount:Int?=null
) {
    override fun toString(): String {
        return name
    }
}
