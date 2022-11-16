package com.android.pos.data.model.requestModel


import androidx.room.PrimaryKey
import com.android.pos.data.entities.BusinessAddress
import com.android.pos.data.entities.TbBusinessDetails
import com.google.gson.annotations.SerializedName

class CashInOutModel {

    @SerializedName("payment_attributes")
    var paymentAttributes: CashInOutPaymentModel? = null

}
