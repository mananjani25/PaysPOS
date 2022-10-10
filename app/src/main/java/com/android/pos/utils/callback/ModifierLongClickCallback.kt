package com.android.pos.utils.callback

import android.view.View

interface ModifierLongClickCallback {
    fun onLongClickListener(modifier_id:Int?, pos: Int,itemQuantity:Int?)
}