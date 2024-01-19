package com.pays.pos.utils.callback

import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.responseModel.EmployeeListResponse

interface OperationCallback {
    fun onItemClickListener(int: Employee)
}