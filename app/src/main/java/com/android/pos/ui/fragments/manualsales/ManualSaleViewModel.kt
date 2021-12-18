package com.android.pos.ui.fragments.manualsales

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.MethodUtils
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
    private lateinit var aa: LiveData<Int>
    private val TAG = "ManualSaleViewModel"

    val serviceCharge = posRepository.serviceChargeList()
    val cartList = posRepository.getManualSaleList()
    val returnedVal = posRepository.getManualCategoryId()
    val activeLoyaltyProgramLiveData = posRepository.getActiveLoyaltyProgramFromDb()

    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var totalDiscount = 0.0
    var totalServiceCharge = 0.0
    var selectedCustomer: TbCustomer? = null
    var activeLoyaltyProgram: LoyaltyProgramsModel? = null
    var redeemLoyaltyInfo: RedeemLoyaltyInfo = RedeemLoyaltyInfo()

    init {
        deleteCart()
    }

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

        // prefProvider.setValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
        addCart(cartList[0])
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {

        if (cartList != null && cartList.isEmpty()) {

            val model = addCartModel(item)
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()

            if (list != null && list.isNotEmpty()) {

                if (type == ADD || type == UPDATE) {
                    var index = -1

                    list.forEachIndexed { pos, tbItem ->
                        if (item != null) {
                            if (tbItem.itemId == item.itemId) {
                                index = pos
                                return@forEachIndexed
                            }
                        }
                    }
                    if (index != -1) {
                        val model = cartList[0].items?.get(index)
                        if (model != null) {
                            if (type == "UPDATE") {
                                if (item != null) {
                                    model.itemQuantity = item.itemQuantity
                                    if (item.isEdited) {
                                        model.isEdited = item.isEdited
                                    }
                                }
                                list[index] = model
                            } else {
                                if (index != -1) {
                                    if (item != null) {
                                        model.itemQuantity =
                                            item.itemQuantity + model.itemQuantity
                                    }

                                    list[index] = model
                                } else {
                                    if (item != null) {
                                        model.itemQuantity = item.itemQuantity
                                        if (item.isEdited) {
                                            model.isEdited = item.isEdited
                                        }
                                    }
                                    list[index] = model
                                }
                            }

                        }
                    } else {
                        if (item != null) {
                            list.add(item)
                        }
                    }
                } else if (type == DELETE) {

                    var index = -1

                    list.forEachIndexed { pos, tbItem ->
                        if (item != null) {
                            if (tbItem.itemId == item.itemId) {
                                index = pos
                                return@forEachIndexed
                            }
                        }
                    }
                    if (index != -1) {
                        val model = cartList[0].items?.get(index)
                        if (model != null) {
                            //delete from cart
                            if (item.isEdited) {
                                model.isEdited = item.isEdited
                                model.isDestroy = true
                            } else {
                                list.remove(item)
                            }
                        }
                    } else {
                        //list.remove(item)
                    }
                }

                val cartModel = cartList[0]
                cartModel.items = list
                addCart(cartModel)

                if (list.isEmpty()) {
                    // delete carts
                    deleteCart()
                }
            } else {

                if (type == DELETE) {
                    deleteCart()
                } else {
                    val cartModel = cartList?.get(0)
                    cartModel?.items = listOf(item)
                    if (cartModel != null) {
                        addCart(cartModel)
                    }
                }

            }
        }

    }

    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        itemList: List<TbItem>?,
        txtTotalAmount: TextView
    ) {

        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalTax = 0.0
        totalDiscount = 0.0
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

            totalDiscount = itemList?.map {
                it.discountPrice
            }.sum()
        }

        val serviceChargeList = serviceCharge.value?.data
        Log.e(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargeList)}")
        if (serviceChargeList != null && serviceChargeList.isNotEmpty()) {

            serviceChargeList.forEach {
                if (it.isEnabled) {
                    totalServiceCharge = (subTotalPrice * it.percentage) / 100
                    Log.e("totalServiceCharge", totalServiceCharge.toString())
                }
            }
        }

        totalPrice = (subTotalPrice + totalTax + totalServiceCharge) - totalDiscount

        //loyalty point and price calculation
        checkAppliedLoyaltyProgram(
            customer = selectedCustomer,
            total = totalPrice,
            txtTotalAmount = txtTotalAmount
        )

        /*txtTotalAmount.text = "$" + String.format(
            "%.2f",
            totalPrice
        )*/
    }

    private fun checkAppliedLoyaltyProgram(
        customer: TbCustomer?,
        total: Double,
        txtTotalAmount: TextView
    ) {

        Log.e("Loyalty", "checkAppliedLoyaltyProgram..")

        redeemLoyaltyInfo.total = total
        val availablePoints = customer?.final_reward ?: 0

        if (customer == null) {
            //loyalty cant be applied if customer is not selected.
            redeemLoyaltyInfo.needToApplyLoyalty = false
        } else if (customer.enroll_to_loyalty == true
            && activeLoyaltyProgram != null && activeLoyaltyProgram?.rewardPoint ?: 0 <= availablePoints
        ) {
            activeLoyaltyProgram?.let {
                redeemLoyaltyInfo.loyaltyProgramsModel = activeLoyaltyProgram

                //if customer has more points than required(minimum limit)
                val availableLoyaltyAmount =
                    availablePoints * it.amount / it.rewardPoint
                if (availableLoyaltyAmount > redeemLoyaltyInfo.total) {
                    //if loyalty amount is more than total price

                    redeemLoyaltyInfo.usedLoyaltyPoints =
                        (redeemLoyaltyInfo.total * it.rewardPoint / it.amount).toInt()
                    redeemLoyaltyInfo.usedLoyaltyAmount =
                        (redeemLoyaltyInfo.usedLoyaltyPoints * it.amount / it.rewardPoint)
                    redeemLoyaltyInfo.remainingAmount =
                        redeemLoyaltyInfo.total - redeemLoyaltyInfo.usedLoyaltyAmount
                    redeemLoyaltyInfo.remainingLoyaltyPoints =
                        availablePoints - redeemLoyaltyInfo.usedLoyaltyPoints
                } else {
                    redeemLoyaltyInfo.usedLoyaltyAmount =
                        (availablePoints * it.amount / it.rewardPoint)
                    redeemLoyaltyInfo.usedLoyaltyPoints = availablePoints
                    redeemLoyaltyInfo.remainingLoyaltyPoints = 0
                    redeemLoyaltyInfo.remainingAmount =
                        redeemLoyaltyInfo.total - redeemLoyaltyInfo.usedLoyaltyAmount
                }
                //redeemLoyaltyInfo.isLoyaltyApplied = true
            }
        } else {
            redeemLoyaltyInfo.remainingAmount = redeemLoyaltyInfo.total
            redeemLoyaltyInfo.remainingLoyaltyPoints = availablePoints
            redeemLoyaltyInfo.usedLoyaltyPoints = 0
            redeemLoyaltyInfo.usedLoyaltyAmount = 0.0
            //redeemLoyaltyInfo.isLoyaltyApplied = false
        }
        MethodUtils.setPriceTextView(txtTotalAmount, redeemLoyaltyInfo.getAmountToBePaid())

    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val serviceChargeList = serviceCharge.value?.data
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
            serviceCharge = serviceChargeList
        }
        return cartModel
    }
}