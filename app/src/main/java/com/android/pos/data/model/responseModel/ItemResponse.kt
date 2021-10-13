package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ItemResponse(
    @SerializedName("data")
    val `data`: VenueDataResponse.Data.Category.Item,

    ) : BaseResponse()
