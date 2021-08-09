package com.android.pos.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.typeconvert.TypeConvertersItems

@TypeConverters(TypeConvertersItems::class)
@Entity(tableName = "CartModel")
class CartModel {

    @PrimaryKey(autoGenerate = true)
    var cartId: Int = 0
    var terminalId: Int = 0
    var employeeID: Int = 0
    var items: List<TbItem>? = null
    var isOpenOrder: Boolean = false
    var isMaual:Boolean = false




}