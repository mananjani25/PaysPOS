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
import com.android.pos.data.entities.TbServiceCharge
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
    var totalDiscount = 0.0
    var totalServiceCharge = 0.0
    private fun addCart(cartModel: CartModel) {
        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            posRepository.deleteManualSaleCart()


        }
    }

    fun saveManualSaleData(cartList: List<CartModel>) {

        prefProvider.setValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
        addCart(cartList.get(0))
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {
        if (cartList != null && cartList.isEmpty()) {
            val model = addCartModel(item)
            Log.e(TAG, "AddCartModel:  ${Gson().toJson(model)}")
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()
            Log.e(TAG, "ViewModellist:  ${Gson().toJson(item)}")
            if (list != null && list.isNotEmpty()) {
                if (type == ADD) {
                    item.itemQuantity = 1
                    list.add(item)

                } else if (type == UPDATE) {
                    var index = -1
                    list.forEachIndexed { pos, tbItem ->
                        if (tbItem.customItemID == item.customItemID) {
                            index = pos
                            return@forEachIndexed
                        }

                    }
                    Log.e(TAG, "indexValue ${index}")
                    val model = cartList[0].items?.get(index)
                    if (index != -1) {
                        if (model != null) {
                            if (type == "UPDATE") {
                                model.itemQuantity = item.itemQuantity
                                model.discountPrice = item.discountPrice
                                model.price = item.price
                                model.discountId = item.discountId

                                list.set(index, model)


                            } else {
                                model.itemQuantity = model.itemQuantity++

                            }

                        }
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
        serviceChargesList: List<TbServiceCharge>?
    ) {

        Log.e(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargesList)}")
        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalTax = 0.0
        totalDiscount = 0.0
        totalServiceCharge = 0.0

        itemList?.forEach { item ->
            totalCount += item.itemQuantity
            subTotalPrice += item.price

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

            totalDiscount = itemList?.map {
                it.discountPrice
            }.sum()
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



        totalPrice = (subTotalPrice + totalTax + totalServiceCharge) - totalDiscount

        txtTotalAmount.text = "$" + String.format(
            "%.2f",
            totalPrice
        )

    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val cartModel = CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
            orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
            orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
            item.itemQuantity = 1
            inventoryModelList.add(item)
            items = inventoryModelList
            isMaual = true
        }
        return cartModel
    }
}