package com.android.pos.data.model

data class MergeTableListModel(
    val listTable: ArrayList<MergeTableModel>,
    val listFloorPlan: ArrayList<MergeFloorModel>,
    var selectedTableId: Int? = null,
    var orderId: Int? = null
)
