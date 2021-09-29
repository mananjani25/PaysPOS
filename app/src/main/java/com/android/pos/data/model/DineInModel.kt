package com.android.pos.data.model

import com.android.pos.data.entities.TbItem

data class DineInModel(
    val id: Int,
    val isTypeHeader: Boolean = true,
        var selectedPosition: Int,
    val title:String?=null,
    val items: ArrayList<TbItem> = arrayListOf()
)