package com.android.pos.data.model.requestModel


import androidx.room.PrimaryKey
import com.android.pos.data.entities.BusinessAddress
import com.android.pos.data.entities.TbBusinessDetails
import com.google.gson.annotations.SerializedName

class CashInOutPaymentModel {

    @SerializedName("id")
    var id: Int = 0

    @SerializedName("is_captured")
    var isCaptured: Boolean = false

}
