package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.EmployeeListResponse

interface MyCallback {
    fun onItemClickListener(view: View?, data: TbItem,position:Int?=null)
}