package com.pays.pos.data.model.requestModel

class CreateCategoryRequestModel {
    var id: Int? = null
    var name: String = ""
    var active: Boolean = true
    var location_id: Int = -1
    var item_ids: ArrayList<Int>? = null
    var image: String? = null
}

