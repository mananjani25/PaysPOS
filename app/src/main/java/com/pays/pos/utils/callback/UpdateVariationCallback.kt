package com.pays.pos.utils.callback

import android.view.View
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.model.responseModel.EmployeeListResponse

interface UpdateVariationCallback {
    fun onItemClickListener(position: Int, variation: VariationsAttribute)
}