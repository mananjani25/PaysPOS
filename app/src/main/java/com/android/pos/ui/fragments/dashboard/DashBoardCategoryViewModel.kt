package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository
) : ViewModel() {

    val TAG = "DashBoardCateViewModel"
    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var totalServiceCharge = 0.0
    var totalDiscount = 0.0
    var assignCustomer: TbCustomer? = null

    val venueData = posRepository.syncVenueData()

    fun venueDataLocal(): LiveData<Resource<List<CategoryWithInventory?>>> {
        return posRepository.venueDataLocal()
    }

    fun orderTypes(): LiveData<Resource<List<TbOrderType>>> {
        return posRepository.orderTypes()
    }

    val venueDataLocal = posRepository.venueDataLocal()

    val serviceCharges = posRepository.serviceChargeList()


    val taxList = posRepository.taxList()


    val discountList = posRepository.disocuntList()

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    fun mAllWords(orderType: String) = posRepository.getCartList(orderType)

    var serviceChargesList: List<TbServiceCharge> = emptyList()

    init {
        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            resource.data?.let {
                                Log.e(TAG, "FullData  ${Gson().toJson(it)}")
                                taxServiceChargeRepository.addAllTaxDatabase(it.data.taxes)

                                posRepository.addAllNotesDatabase(it.data.notes)
                                tipDiscountRepository.addDiscount(it.data.discounts)
                                taxServiceChargeRepository.addServiceCharges(it.data.service_charges)
                                posRepository.addTerminalsDatabase(it.data.terminals)

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    private fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart()
        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {


        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare
            val cartModel = addCartModel(item)
            addCart(cartModel)
        } else {

            // already cart ma hoy to add/update/delete kare flag wise
            val list = cartList?.get(0)?.items?.toMutableList()
            if (list != null && list.isNotEmpty()) {

                if (type == ADD || type == UPDATE) {
                    var index = -1

                    list.forEachIndexed { pos, tbItem ->
                        if (tbItem.itemId == item.itemId && checkModifier(tbItem, item)) {
                            index = pos
                            return@forEachIndexed
                        }
                    }
                    if (index != -1) {
                        val model = cartList[0].items?.get(index)
                        if (model != null) {
                            if (type == "UPDATE") {
                                model.itemQuantity = item.itemQuantity
                                list[index] = model
                            } else {
                                if (index != -1) {
                                    model.itemQuantity = item.itemQuantity + model.itemQuantity
                                    model.modifiers = item.modifiers

                                    list[index] = model
                                } else {
                                    model.itemQuantity = item.itemQuantity
                                    list[index] = model
                                }
                            }

                        }
                    } else {
                        list.add(item)
                    }
                } else if (type == DELETE) {
                    list.remove(item)
                }

                val cartModel = cartList[0]
                cartModel.items = list
                addCart(cartModel)

                if (list.isEmpty()) {
                    // delete carts
                    deleteCart()
                }
            } else {
                val cartModel = cartList?.get(0)
                cartModel?.items = list
                if (cartModel != null) {
                    addCart(cartModel)
                }
            }


        }
    }

    private fun checkModifier(tbItem: TbItem, item: TbItem): Boolean {

        if (item.modifiers.isEmpty()) return true

        var checkModifier = false

        item.modifiers.forEach { itemM ->
            tbItem.modifiers.forEach {
                checkModifier = itemM.id == it.id
            }
        }
        return checkModifier
    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
            orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
            orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
            serviceCharge = serviceChargesList
            customer = assignCustomer
            item.itemQuantity = item.itemQuantity
            inventoryModelList.add(item)
            items = inventoryModelList
        }
    }

    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        cartList: List<CartModel>?,
        txtTotalAmount: AppCompatTextView
    ) {


        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalDiscount = 0.0
        totalTax = 0.0
        totalServiceCharge = 0.0

        if (cartList != null && cartList.isNotEmpty()) {
            cartList[0].items?.forEach { item ->
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


                item.modifiers.forEach {
                    subTotalPrice += (it.price * it.itemQuantity)

                    item.taxes?.forEach { tax ->
                        if (tax.isActive) {
                            if (tax.taxType == "Percentage") {
                                val itemTaxPrice = (tax.rate * (it.price * it.itemQuantity)) / 100
                                Log.e("itemTaxPrice", "" + itemTaxPrice)
                                totalTax += String.format("%.2f", itemTaxPrice)
                                    .toDouble()
                            }
                        }
                    }
                }
            }

            val serviceChargesList = cartList[0].serviceCharge

            if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
                serviceChargesList.forEach {
                    if (it.isEnabled) {
                        totalServiceCharge = (subTotalPrice * it.percentage) / 100
                        Log.e("totalServiceCharge", totalServiceCharge.toString())
                    }
                }

            }

            totalDiscount = cartList[0].items!!.map {
                it.discountPrice
            }.sum()
            Log.e(TAG, "totalDiscount:  ${totalDiscount}")


            totalPrice = (subTotalPrice + totalTax + totalServiceCharge) - totalDiscount
        }

        MethodUtils.setPriceTextView(txtTotalAmount, totalPrice)


    }

    fun setServiceCharges(mList: List<TbServiceCharge>?) {

        if (mList != null) {
            this.serviceChargesList = mList
        }
    }

    fun addCustomer(customer: TbCustomer?) {
        assignCustomer = customer
    }

    fun getMinMax(_itemId: Int, modifierSetId: Int?): LiveData<ItemModifierSets?>? {


        return posRepository.getMinMax(_itemId, modifierSetId)

    }


}