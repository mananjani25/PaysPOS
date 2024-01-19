package com.pays.pos.data.model

import com.pays.pos.data.model.responseModel.GetFloorPlanDetailResponse

data class MergeTableModel(
    val id: Int? = null,
    val name: String,
    val floorId: Int,
    val floorName: String,
    val isOccupied: Boolean = false,
    val orderId: Int? = null,
    var orderDetails: GetFloorPlanDetailResponse.OrderDetails? = null,
    var chairCount: Int? = null
) {
    override fun toString(): String {
        return name
    }
}
