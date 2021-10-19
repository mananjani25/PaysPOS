package com.android.pos.data.model.responseModel.category


import com.android.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class CategoriesResponse(
    @SerializedName("data")
    val `data`: List<Category>,
) : BaseResponse()