package com.android.pos.data.model

import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem

data class DineInModel(
    var id: Int? = null,
    val isTypeHeader: Boolean = true,
    var selectedPosition: Int = 0,
    var title: String? = null,
    var itemPosition: Int? = null,
    var headerPosition: Int? = null,
    var items: ArrayList<TbItem> = arrayListOf(),
    var customer: TbCustomer? = null,
    var isFired: Boolean = true,
    var isPaid: Boolean = false,
    var guestDividedAmt: Double = 0.0,
    var isHeader: Int = 0

)