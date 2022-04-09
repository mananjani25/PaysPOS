package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.ui.adapter.TeamsAdapter

interface CustomCallback {
    fun onItemClickListener(view: View?, data: Employee)
    fun onOptionClickListener(view: View?, pos: TeamsAdapter.ItemViewHolder)
}