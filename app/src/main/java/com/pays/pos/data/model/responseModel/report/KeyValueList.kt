package com.pays.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName

data class KeyValueList(
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: String?
)
