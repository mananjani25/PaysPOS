package com.android.pos.utils.callback

import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.EmployeeListResponse

interface OperationCallback {
    fun onItemClickListener(int: Employee)
}