package com.android.pos.data.model

import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem

data class DineInModel(
    val id: Int? = null,
    val isTypeHeader: Boolean = true,
    var selectedPosition: Int,
    val title: String? = null,
    var itemPosition: Int? = null,
    var headerPosition: Int? = null,
    val items: ArrayList<TbItem> = arrayListOf(),
    var customer: TbCustomer? = null,
    var isFired:Boolean = false
)