package com.pays.pos.utils.callback

import android.view.View
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem

interface MyCallback {
    fun onItemClickListener(view: View?, data: TbCartItem,position:Int)
    fun onCartItemClickListener(view: View?, data: TbCartItem,position:Int)
}