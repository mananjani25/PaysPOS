package com.pays.pos.data.model

import com.pays.pos.data.entities.TbItem

data class ManualSaleCartModel(
    var id: Int = 0,
    var customerName: String? = null,
    var isTax: Boolean = false,
    var itemPrice:String? = null,
)