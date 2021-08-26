package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.typeconvert.TypeConvertersItems
import kotlinx.parcelize.Parcelize

@Parcelize
@TypeConverters(TypeConvertersItems::class)
@Entity(tableName = "CartModel")
class CartModel : Parcelable {

    @PrimaryKey(autoGenerate = true)
    var cartId: Int = 0
    var terminalId: Int = 0
    var employeeID: Int = 0
    var locationId: Int = 0
    var items: List<TbItem>? = emptyList()
    var serviceCharge: List<TbServiceCharge>? = emptyList()
    var isOpenOrder: Boolean = false
    var isMaual: Boolean = false
    var note: String = ""
    var orderType: String = ""
    var orderTypeName: String = ""
    var orderTypeId: Int = 0
    var customer: TbCustomer? = null
    var futureDeliveryDate: String = ""
    var futureDeliveryTime: String = ""


}