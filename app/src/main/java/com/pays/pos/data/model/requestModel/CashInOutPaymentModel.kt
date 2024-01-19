package com.pays.pos.data.model.requestModel


import androidx.room.PrimaryKey
import com.pays.pos.data.entities.BusinessAddress
import com.pays.pos.data.entities.TbBusinessDetails
import com.google.gson.annotations.SerializedName

class CashInOutPaymentModel {

    @SerializedName("id")
    var id: Int = 0

    @SerializedName("is_captured")
    var isCaptured: Boolean = false

}
