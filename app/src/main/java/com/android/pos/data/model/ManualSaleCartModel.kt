package com.android.pos.data.model

import com.android.pos.data.entities.TbItem

data class ManualSaleCartModel(
    var id: Int = 0,
    var customerName: String? = null,
    var isTax: Boolean = false,
    var itemPrice:String? = null,
)