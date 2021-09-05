package com.android.pos.utils.callback

import com.android.pos.data.entities.OptionSet

interface DeleteOptionSetCallback {
    fun onItemClickListener(position: Int?, optionSet: OptionSet?)
}