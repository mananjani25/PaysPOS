package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.typeconvert.TypeConvertersItems
import kotlinx.parcelize.Parcelize

@Parcelize
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
    var selectedTableName: String = ""
    var dineInList: List<DineInModel>? = emptyList()

    var discountPrice: Double = 0.0
    var discountSelectdValue: Double = 0.0
    var discountType: String = ""
    var discountId: Int? = null
    var isFired: Boolean = false
    var isEdited: Boolean = false
    var orderId: Int? = null
    var openOrderType = ""
    var deliveryType = ""
    var reorder: Boolean = false
    var taxlistDynamic: List<TaxData>? = emptyList()
    var listOfItemRemoved:List<Int> = emptyList()

}