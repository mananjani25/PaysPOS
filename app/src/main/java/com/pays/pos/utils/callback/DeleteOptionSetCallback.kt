package com.pays.pos.utils.callback

import com.pays.pos.data.entities.OptionSet

interface DeleteOptionSetCallback {
    fun onItemClickListener(position: Int?, optionSet: OptionSet?)
}