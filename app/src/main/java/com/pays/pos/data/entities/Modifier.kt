package com.pays.pos.data.entities

import android.os.Parcelable
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Modifier : Parcelable {


    @SerializedName("id")
    var id: Int? = null

    @SerializedName("modifier_set_id")
    var modifierSetId: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("price")
    var price: Double = 0.00

    @SerializedName("sort")
    var sort: Int = 0

    @SerializedName("_destroy")
    var _destroy: Boolean = false


    @SerializedName("is_deleted")
    var isDeleted: Boolean = false

    var isChecked: Boolean = false

    @SerializedName("itemQuantity")
    var itemQuantity: Int = 1

    @SerializedName("modifier_quantity")
    var modifier_quantity: Int = 1

    var orderModifierId: Int? = null


    var orderItemTaxes: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier.OrderItemTaxe?> =
        emptyList()
}