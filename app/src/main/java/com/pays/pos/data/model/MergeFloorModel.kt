package com.pays.pos.data.model

data class MergeFloorModel(val id: Int, val name: String) {
    override fun toString(): String {
        return name
    }
}
