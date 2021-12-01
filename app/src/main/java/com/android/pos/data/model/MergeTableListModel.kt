package com.android.pos.data.model

data class MergeTableListModel(
    val listTable: ArrayList<MergeTableModel>,
    val listFloorPlan: ArrayList<MergeFloorModel>,
    var selectedTableId: Int? = null,
    var orderId: Int? = null,
    var selectedFloorPlanId: Int? = null,
    var tableSelectedPosition: Int? = null,
    var tableChairCount: Int? = null
)
