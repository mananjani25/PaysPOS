package com.pays.pos.data.model.responseModel.category


import com.pays.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class CreateCategoryResponse(
    @SerializedName("data")
    val `data`: Category,
) : BaseResponse()