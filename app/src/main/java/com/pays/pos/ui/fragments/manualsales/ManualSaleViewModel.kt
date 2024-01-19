package com.pays.pos.ui.fragments.manualsales

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ADD
import com.pays.pos.data.remote.Constants.DELETE
import com.pays.pos.data.remote.Constants.UPDATE
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class ManualSaleViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private var mPosition: Int = 0
    private lateinit var aa: LiveData<Int>
    private val TAG = "ManualSaleViewModel"

    var serviceCharge = posRepository.serviceChargeList()

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
        deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID,0))
    }


    public fun cartList(employee_id: Int): LiveData<List<CartModel>> {
        return posRepository.getManualSaleList(employee_id)
    }

    fun addCart(cartModel: CartModel) {
        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }


    fun deleteCart(employee_id: Int) {
        viewModelScope.launch {
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            posRepository.deleteManualSaleCart(employee_id)


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


                if (type == ADD) {
                    list.add(item)
                } else if (type == UPDATE) {


                    if (mPosition != -1) {
                        val model = cartList[0].items?.get(mPosition)

                        if (model != null) {
                            model.itemQuantity = item.itemQuantity
                            if (item.isEdited) {
                                model.isEdited = item.isEdited
                            }
                            list[mPosition] = model
                        }
                    }

                } else if (type == DELETE) {

                    if (mPosition != -1) {
                        val model = cartList[0].items?.get(mPosition)
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
                    deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID,0))
                }
            } else {

                if (type == DELETE) {
                    deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID,0))
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
                        LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                        totalTax += String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else if (tax.taxType == "Dollar") {
                        val itemTaxPrice = tax.rate * item.itemQuantity
                        LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                        totalTax += String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    }
                }
            }

            totalDiscount = itemList?.map {
                it.discountPrice
            }.sum()
        }
        subTotalPrice -= totalDiscount
        val serviceChargeList = serviceCharge.value?.data
        LogUtil.logE(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargeList)}")
        if (serviceChargeList != null && serviceChargeList.isNotEmpty()) {

            serviceChargeList.forEach {
                if (it.isEnabled) {
                    totalServiceCharge = (subTotalPrice * it.percentage) / 100
                    LogUtil.logE("totalServiceCharge", totalServiceCharge.toString())
                }
            }
        }

        totalPrice = (subTotalPrice + totalTax + totalServiceCharge)

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

    fun loyaltyPointCondition(customer: TbCustomer?): Boolean {
        return (customer?.enroll_to_loyalty == true && activeLoyaltyProgram != null && activeLoyaltyProgram?.rewardPoint ?: 0 <= customer.final_reward ?: 0)
    }

    private fun checkAppliedLoyaltyProgram(
        customer: TbCustomer?,
        total: Double,
        txtTotalAmount: TextView
    ) {

        redeemLoyaltyInfo.total = total
        val availablePoints1 = customer?.final_reward ?: 0

        if (customer == null) {
            //loyalty cant be applied if customer is not selected.
            redeemLoyaltyInfo.needToApplyLoyalty = false
        } else if (loyaltyPointCondition(customer)) {
            activeLoyaltyProgram?.let {
                redeemLoyaltyInfo.loyaltyProgramsModel = activeLoyaltyProgram

                val availablePoints = it.rewardPoint.times((customer.final_reward?.floorDiv(it.rewardPoint)!!))
                    ?:0
                //if customer has more points than required(minimum limit)
                var availableLoyaltyAmount = 0.0
                if (it.rewardPoint == 0) {
                    it.rewardPoint = 1
                }
                availableLoyaltyAmount =
                    availablePoints * it.amount / it.rewardPoint
                if (availableLoyaltyAmount > redeemLoyaltyInfo.total) {
                    var pointDouble = (redeemLoyaltyInfo.total * it.rewardPoint) / it.amount
                    pointDouble = (it.rewardPoint * (pointDouble.div(it.rewardPoint)).toInt()).toDouble()
                    redeemLoyaltyInfo.usedLoyaltyPoints = ceil(pointDouble).toInt()
                    redeemLoyaltyInfo.usedLoyaltyAmount = pointDouble * it.amount / it.rewardPoint
                    if (redeemLoyaltyInfo.usedLoyaltyAmount >= redeemLoyaltyInfo.total) {
                        redeemLoyaltyInfo.remainingAmount =
                            redeemLoyaltyInfo.usedLoyaltyAmount - redeemLoyaltyInfo.total
                    } else {
                        redeemLoyaltyInfo.remainingAmount =
                            redeemLoyaltyInfo.total - redeemLoyaltyInfo.usedLoyaltyAmount
                    }
                    redeemLoyaltyInfo.remainingLoyaltyPoints =
                        availablePoints - redeemLoyaltyInfo.usedLoyaltyPoints
                } else {
                    redeemLoyaltyInfo.usedLoyaltyAmount =
                        (availablePoints * it.amount / it.rewardPoint)
                    redeemLoyaltyInfo.usedLoyaltyPoints = availablePoints
                    redeemLoyaltyInfo.remainingLoyaltyPoints = 0
                    if (redeemLoyaltyInfo.usedLoyaltyAmount >= redeemLoyaltyInfo.total) {
                        redeemLoyaltyInfo.remainingAmount =
                            redeemLoyaltyInfo.usedLoyaltyAmount - redeemLoyaltyInfo.total
                    } else {
                        redeemLoyaltyInfo.remainingAmount =
                            redeemLoyaltyInfo.total - redeemLoyaltyInfo.usedLoyaltyAmount
                    }
                }
                //redeemLoyaltyInfo.isLoyaltyApplied = true
            }
        } else {
            redeemLoyaltyInfo.remainingAmount = redeemLoyaltyInfo.total
            redeemLoyaltyInfo.remainingLoyaltyPoints = availablePoints1
            redeemLoyaltyInfo.usedLoyaltyPoints = 0
            redeemLoyaltyInfo.usedLoyaltyAmount = 0.0
            //redeemLoyaltyInfo.isLoyaltyApplied = false
        }

        MethodUtils.setPriceTextView(txtTotalAmount, redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0)

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

    fun setPosition(position: Int) {
        mPosition = position
    }
}