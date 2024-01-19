package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.TbOrderType
import com.google.gson.annotations.SerializedName

data class OrderTypeResponse(
    @SerializedName("data")
    val `data`: List<TbOrderType>
) : BaseResponse()
