package com.android.pos.data.model

data class MergeTableModel(
    val id: Int,
    val name: String,
    val floorId: Int,
    val floorName: String,
    val isOccupied: Boolean = false,
    val orderId: Int? = null
) {
    override fun toString(): String {
        return name
    }
}
