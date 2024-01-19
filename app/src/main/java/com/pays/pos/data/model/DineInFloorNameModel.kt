package com.pays.pos.data.model

data class DineInFloorNameModel(
    var floorName: String = "",
    var floorTypeList: List<FloorType>? = null
) {

    data class FloorType(
        var floorType: String = "",
        var noOFChairs: Int = -1,
        var tableName: String = ""
    )

}