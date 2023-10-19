package com.android.pos.utils.callback

import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbItem



interface ItemClickListner   {
    fun onItemUpdate(item: TbItem, position: Int)
    fun onCartItemUpdate(item: TbCartItem, position: Int)
    fun onDineInOrderCleared()
}