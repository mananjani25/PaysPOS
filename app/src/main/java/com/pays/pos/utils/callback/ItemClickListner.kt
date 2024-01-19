package com.pays.pos.utils.callback

import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem



interface ItemClickListner   {
    fun onItemUpdate(item: TbCartItem, position: Int)
    fun onCartItemUpdate(item: TbCartItem, position: Int)
    fun onDineInOrderCleared()
}