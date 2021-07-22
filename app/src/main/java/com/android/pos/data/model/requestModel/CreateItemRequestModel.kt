package com.android.pos.data.model.requestModel

data class CreateItemRequestModel(
    var id: Int? = null,
    var itemName: String = "",
    var price: Double = 0.0,
    var sku: String = "",
    var description: String = ""

)

