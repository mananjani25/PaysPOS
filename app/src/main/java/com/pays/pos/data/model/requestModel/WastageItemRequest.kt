package com.pays.pos.data.model.requestModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class WastageItemRequest(

    @SerializedName("wastage_item")
    val wastageItem: WastageItem

) {
    @Parcelize
    class WastageItem(

        @SerializedName("order_id")
        val orderId: Int,
        @SerializedName("table_no")
        val tableNo: Int?,
        @SerializedName("employee_id")
        val employeeId: Int?,
        @SerializedName("item_quantity")
        val itemQuantity: Int?,
        @SerializedName("item_name")
        val itemName: String?,
        @SerializedName("wastage_reason_id")
        val wastageReasonId: Int?,
        @SerializedName("terminal_id")
        val terminalId: Int?,
        @SerializedName("order_item_id")
        val orderItemId: Int?,
        @SerializedName("wastage_note")
        val wastageNote: String?,
        @SerializedName("wastage_item_modifiers")
        var wastageItemModifiersAttributes: List<OrderItemModifierAttribute> = emptyList()
    ) : Parcelable
}