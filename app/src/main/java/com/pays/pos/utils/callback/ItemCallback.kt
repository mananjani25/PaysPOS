package com.pays.pos.utils.callback

import android.view.View

interface ItemCallback {
    fun onItemClickListener(view: View?, pos: Int)
}