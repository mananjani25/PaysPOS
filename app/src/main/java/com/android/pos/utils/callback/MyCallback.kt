package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbItem

interface MyCallback {
    fun onItemClickListener(view: View?, data: TbItem,position:Int)
    fun onCartItemClickListener(view: View?, data: TbCartItem,position:Int)
}