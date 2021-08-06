package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.EmployeeListResponse

interface CustomCallback {
    fun onItemClickListener(view: View?, data: Employee)
}