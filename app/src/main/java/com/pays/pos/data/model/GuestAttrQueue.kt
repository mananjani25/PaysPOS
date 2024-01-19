package com.pays.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.entities.TypeConvertersQueueDineIn
import com.pays.pos.data.model.responseModel.CreateOrderResponse

@TypeConverters(TypeConvertersQueueDineIn::class)
@Entity
data class GuestAttrQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    var listOfItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem>
)
