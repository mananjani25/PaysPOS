package com.pays.pos.utils.callback

import android.view.View
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.EmployeeListResponse

interface ManualSaleOptionsCustomCallback {
    fun onItemClickListener(view: View?, data: TbItem,pos:Int)
}