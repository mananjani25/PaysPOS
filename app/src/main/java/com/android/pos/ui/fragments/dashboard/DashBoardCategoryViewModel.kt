package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private var totalPrice: Double = 0.0

    val venueData = posRepository.syncVenueData()


    val venueDataLocal = posRepository.venueDataLocal()

    var mAllWords = posRepository.getCartList()

    private fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            appDatabase.cartDao().add(cartModel)
        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {


        if (cartList != null && cartList.isEmpty()) {
            val cartModel = addCartModel(item)
            addCart(cartModel)
        } else {

            val list = cartList?.get(0)?.items?.toMutableList()
            if (list != null && list.isNotEmpty()) {

                if (type == "ADD" || type == "UPDATE") {
                    var index = -1

                    list?.forEachIndexed { pos, tbItem ->
                        if (tbItem.itemId == item.itemId) {
                            index = pos
                            return@forEachIndexed
                        }
                    }
                    if (index != -1) {
                        val model = cartList?.get(0)?.items?.get(index)
                        if (model != null) {
                            if (type == "UPDATE") {
                                model.itemQuantity = item.itemQuantity
                            } else
                                model.itemQuantity = model.itemQuantity + 1
                            list?.set(index, model)
                        }
                    } else {
                        item.itemQuantity = 1
                        list?.add(item)
                    }
                } else if (type == "DELETE") {

                    list.remove(item)
                }
                val cartModel = CartModel().apply {
                    cartId = cartList[0].cartId
                    items = list
                }
                addCart(cartModel)
            }


        }
    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val cartModel = CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            item.itemQuantity = 1
            inventoryModelList.add(item)
            items = inventoryModelList
        }
        return cartModel
    }

    @SuppressLint("SetTextI18n")
    fun itemCalculation(itemList: List<TbItem>?, txtTotalAmount: AppCompatTextView) {

        var totalCount = 0
        var subTotalPrice = 0.0
        var totalTax = 0.0
        var totalServiceCharge = 0.0
        totalPrice = 0.0

        itemList?.forEach {
            totalCount += it.itemQuantity
            subTotalPrice += it.price * it.itemQuantity
        }


        totalPrice = subTotalPrice + totalTax + totalServiceCharge

        txtTotalAmount.text = "Pay $" + String.format(
            "%.2f",
            totalPrice
        )

    }


}