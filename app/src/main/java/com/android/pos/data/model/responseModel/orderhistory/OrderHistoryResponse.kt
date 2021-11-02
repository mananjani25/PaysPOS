package com.android.pos.data.model.responseModel.orderhistory

import com.android.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class OrderHistoryResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse()
