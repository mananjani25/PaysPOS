package com.android.pos.data.model.responseModel.item


import com.android.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class ItemResponseNew(
    @SerializedName("data")
    val `data`: Item
) : BaseResponse()