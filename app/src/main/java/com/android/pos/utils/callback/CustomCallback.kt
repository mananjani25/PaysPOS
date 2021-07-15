package com.android.pos.utils.callback

import android.view.View
import com.android.pos.data.model.responseModel.EmployeeResponse

interface CustomCallback {
    fun onItemClickListener(view: View?, data: EmployeeResponse.Data)
}