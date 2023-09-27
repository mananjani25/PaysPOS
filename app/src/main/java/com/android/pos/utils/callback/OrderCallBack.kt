package com.android.pos.utils.callback

import android.view.View

interface OrderCallBack {
    fun onItemClickListener(view: View?, pos: Int, status: String)
    fun noDataAvailableFilter()
    fun hideNoDataAvailable()
}