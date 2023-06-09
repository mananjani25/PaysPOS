package com.android.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.pos.data.model.responseModel.CreateOrderResponse

@Entity
data class GuestAttrQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    var listOfItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem>
)
