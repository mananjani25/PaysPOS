package com.pays.pos.data.model

import com.google.gson.annotations.SerializedName

data class CreateIds(
    @SerializedName("order_item_ids")
    var id: Array<Int>
)
