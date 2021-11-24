package com.android.pos.data.model

data class MergeTableModel(val id: Int, val name: String,val floorId:Int,val floorName:String) {
    override fun toString(): String {
        return name
    }
}
