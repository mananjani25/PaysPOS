package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.model.responseModel.EmployeeListResponse

interface DeleteOptionCallback {
    fun onItemClickListener(position: Int?, optionSet: OptionSet)
}