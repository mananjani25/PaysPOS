package com.pays.pos.data.model.responseModel.item


import com.pays.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class ItemsResponse(
    @SerializedName("data")
    val `data`: List<Item>,
) : BaseResponse()
