package com.android.pos.data.model

data class DineInFloorNameModel(
    var floorName: String = "",
    var floorTypeList: List<FloorType>? = null
) {

    data class FloorType(
        var floorType: String = ""
    )

}