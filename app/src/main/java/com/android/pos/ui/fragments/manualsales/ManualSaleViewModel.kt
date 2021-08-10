package com.android.pos.ui.fragments.manualsales

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualSaleViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private val TAG = "ManualSaleViewModel"

    val serviceCharge = posRepository.serviceChargeList()

    val cartList = posRepository.getManualSaleList()

    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var totalServiceCharge = 0.0
    private fun addCart(cartModel: CartModel) {
        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteManualSaleCart()


        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {
        if (cartList != null && cartList.isEmpty()) {
            val model = addCartModel(item)
            Log.e(TAG, "AddCartModel:  ${Gson().toJson(model)}")
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()
            if (list != null && list.isNotEmpty()) {
                if (type == ADD || type == UPDATE) {
                    var index = -1
                    list.forEachIndexed { pos, tbItem ->
                        if (tbItem.itemId == item.itemId) {
                            index = pos
                            return@forEachIndexed
                        }

                    }

                    Log.e(TAG, "indexValue ${index}")
                    if (index != -1) {
                        val model = cartList[0].items?.get(index)
                        if (model != null) {
                            if (type == "UPDATE") {
                                model.itemQuantity = item.itemQuantity
                            } else {
                                model.itemQuantity = model.itemQuantity++
                            }
                            list.set(index, model)
                        }
                    } else {
                        item.itemQuantity = 1
                        list.add(item)
                    }
                } else if (type == DELETE) {
                    list.remove(item)
                }
                val cartModel = CartModel().apply {
                    cartId = cartList[0].cartId
                    items = list
                    isMaual = true

                }

                Log.e(TAG, "cartModel:  ${Gson().toJson(cartModel)}")

                addCart(cartModel)
                /*if (list.isEmpty()) {
                    deleteCart()
                }

*/
            }

        }


    }

    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        itemList: List<TbItem>?,
        txtTotalAmount: TextView,
        serviceChargesList: List<GetServiceChargeResponse.Data>?
    ) {


        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalTax = 0.0
        totalServiceCharge = 0.0

        itemList?.forEach { item ->
            totalCount += item.itemQuantity
            subTotalPrice += item.price * item.itemQuantity

            item.taxes?.forEach { tax ->
                if (tax.isActive) {
                    if (tax.taxType == "Percentage") {
                        val itemTaxPrice = (tax.rate * (item.price * item.itemQuantity)) / 100
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                        totalTax += String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    }
                }
            }
        }

        Log.e(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargesList)}")
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {

            serviceChargesList.forEach {
                if (it.isEnabled) {
                    totalServiceCharge = (subTotalPrice * it.percentage) / 100
                    Log.e("totalServiceCharge", totalServiceCharge.toString())
                }
            }

        }



        totalPrice = subTotalPrice + totalTax + totalServiceCharge

        txtTotalAmount.text = "$" + String.format(
            "%.2f",
            totalPrice
        )

    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val cartModel = CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            item.itemQuantity = 1
            inventoryModelList.add(item)
            items = inventoryModelList
            isMaual = true
        }
        return cartModel
    }
}