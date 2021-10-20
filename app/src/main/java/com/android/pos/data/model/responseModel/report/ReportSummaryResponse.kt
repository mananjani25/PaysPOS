package com.android.pos.data.model.responseModel.report

import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CashLogResponse
import com.google.gson.annotations.SerializedName

data class ReportSummaryResponse(
    @SerializedName("data")
    val `data`: Data
): BaseResponse()
