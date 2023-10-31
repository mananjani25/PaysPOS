package com.android.pos.data.model

import android.os.Parcelable
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class DineInModel(
    var id: Int? = null,
    val isTypeHeader: Boolean = true,
    var selectedPosition: Int = 0,
    var title: String? = null,
    var itemPosition: Int? = null,
    var headerPosition: Int? = null,
    var items: ArrayList<TbItem> = arrayListOf(),
    var customer: TbCustomer? = null,
    var isFired: Boolean = true,
    var isPaid: Boolean = false,
    var isDestroy: Boolean = false,
    var guestDividedAmt: Double = 0.0,
    var wholeTableSubTotal: Double = 0.0,
    var orderDiscount: Double = 0.0,
    var orderDiscountPercentage: Double = 0.0,
    var wholeTableSurTax: Double = 0.0,
    var wholeTableTax: Double = 0.0,
    var wholeTableDiscont: Double = 0.0,
    var totalGuestCount: Int = 0,
    var eligibleGuestsForDivision: Int = 0,
    var isHeader: Int = 0,
    var item: TbCartItem? = null,
    var totalGuestPrice: Double = 0.0,
    var sort: Int = 0,
    var totalTableAmt: Double = 0.0,
    var guestDividerAmt: Double = 0.0,
    var serviceChargeList: ArrayList<TbServiceCharge>? = null,
    var floorPlanTable: GetOrderDetailsResponse.Data.FloorPlanTable? = null,
    var empName: String = "",
    var cashSurchargeDiscount: Double = 0.0,
    var orderTotalAmount: Double = 0.0,
    var listOfItemsMoved: java.util.ArrayList<Int> = arrayListOf(),
    var itemsCount: Int = 0,

) : Parcelable {
}