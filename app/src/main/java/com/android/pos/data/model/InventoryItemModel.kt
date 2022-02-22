package com.android.pos.data.model

data class InventoryItemModel(val id: Int, val title: String,val count:Int?, var isSelected: Boolean = false)