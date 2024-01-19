package com.pays.pos.data.model.requestModel


import androidx.room.PrimaryKey
import com.pays.pos.data.entities.BusinessAddress
import com.pays.pos.data.entities.TbBusinessDetails
import com.google.gson.annotations.SerializedName

class CashInOutModel {

    @SerializedName("payment_attributes")
    var paymentAttributes: CashInOutPaymentModel? = null

}
