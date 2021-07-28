package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ItemsResponse(
    @SerializedName("data")
    val `data`: List<VenueDataResponse.Data.Category.Item>,

    ) : BaseResponse()
