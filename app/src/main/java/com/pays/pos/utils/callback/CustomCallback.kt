package com.pays.pos.utils.callback

import android.view.View
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.responseModel.EmployeeListResponse
import com.pays.pos.ui.adapter.TeamsAdapter

interface CustomCallback {
    fun onItemClickListener(view: View?, data: Employee)
    fun onOptionClickListener(view: View?, pos: TeamsAdapter.ItemViewHolder)
}