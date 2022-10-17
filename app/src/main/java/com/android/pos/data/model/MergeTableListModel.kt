package com.android.pos.data.model

import com.android.pos.data.model.responseModel.GetFloorPlanDetailResponse

data class MergeTableListModel(
    val listTable: ArrayList<MergeTableModel>,
    val listFloorPlan: ArrayList<MergeFloorModel>,
    var selectedTableId: Int? = null,
    var orderId: Int? = null,
    var selectedFloorPlanId: Int? = null,
    var tableSelectedPosition: Int? = null,
    var tableChairCount: Int? = null,
    var orderDetails: GetFloorPlanDetailResponse.OrderDetails? = null,
    var secondaryChairCount:Int?=null
)
