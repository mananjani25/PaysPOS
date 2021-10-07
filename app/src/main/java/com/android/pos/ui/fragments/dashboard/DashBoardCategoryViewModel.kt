package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.*
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.AUTH_TOKEN
import com.android.pos.data.remote.Constants.BUSINESS_NAME
import com.android.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.android.pos.data.remote.Constants.BUSINESS_WEBSITE
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.DINE_IN
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


    fun venueDataLocal(): LiveData<Resource<List<CategoryWithInventory?>>> {
        return posRepository.venueDataLocal()
    }

    fun orderTypes(): LiveData<Resource<List<TbOrderType>>> {
        return posRepository.orderTypes()
    }

    val serviceCharges = posRepository.serviceChargeList()


    val taxList = posRepository.taxList()

    val discountList = posRepository.disocuntList()

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _logout = MutableLiveData<Event<Boolean>>()
    val logout: LiveData<Event<Boolean>> = _logout

    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    fun getItemsbyId(itemId: Int) = posRepository.getItemsbyId(itemId)


    fun mAllWords(orderType: String): LiveData<List<CartModel>> {
        return posRepository.getCartList(orderType)
    }

    var serviceChargesList: List<TbServiceCharge> = emptyList()

    init {
        viewModelScope.launch {

            if (prefProvider.getValue(AUTH_TOKEN, "").toString().isNotEmpty()) {

                val resource = posRepository.syncVenueDetails()

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { venueDetailsResponse ->
                            if (venueDetailsResponse?.status == 200) {

                                resource.data?.let {
                                    Log.e(TAG, "FullData  ${Gson().toJson(it)}")

                                    prefProvider.setValue(BUSINESS_NAME, it.data.businessName)
                                    prefProvider.setValue(BUSINESS_PHONE_NO, it.data.phoneNumber)
                                    prefProvider.setValue(
                                        BUSINESS_WEBSITE,
                                        it.data.businessWebsite.toString()
                                    )

                                    taxServiceChargeRepository.addAllTaxDatabase(it.data.taxes)

                                    posRepository.addAllNotesDatabase(it.data.notes)
                                    tipDiscountRepository.addDiscount(it.data.discounts)
                                    taxServiceChargeRepository.addServiceCharges(it.data.service_charges)
                                    posRepository.addTerminalsDatabase(it.data.terminals)
                                    tipDiscountRepository.addTips(it.data.tip_settings)
                                    posRepository.addCustomerReceiptSettings(it.data.customerReceipt)
                                    posRepository.addKitchenReceiptSettings(it.data.kitchenReceipt)

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
    }

    fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart()
        }
    }

    fun dineInCartUpdate(
        cartList: List<CartModel>?,
        dineInList: List<DineInModel> = arrayListOf()
    ) {

        val cartModel = cartList?.get(0)
        cartModel?.dineInList = dineInList
        cartModel?.orderType = DINE_IN
        cartModel?.let {
            addCart(it)
            Log.e(TAG, "CustomerAdded")
        }

    }

    fun cartLogic(
        cartList: List<CartModel>?,
        item: TbItem?,
        type: String,
        dineInList: List<DineInModel> = arrayListOf()
    ) {

        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare
            val cartModel = item?.let { addCartModel(it) }
            if (cartModel != null) {
                addCart(cartModel)
            }


        } else {
            if (cartList?.get(0)?.orderType == DINE_IN) {
                val cartModel = cartList[0]
                cartModel.dineInList = dineInList
                if (type == ADD || type == UPDATE) {
                    var index = -1
                    val dineIn = dineInList
                    if (dineIn != null && dineIn.isNotEmpty()) {
                        val selectedHeader = dineInList.get(0).selectedPosition

                        dineIn.get(selectedHeader).items.forEachIndexed { pos, tbItem ->
                            if (item != null) {
                                if (tbItem.itemId == item.itemId && checkVariation(
                                        tbItem,
                                        item
                                    ) && checkModifier(tbItem, item)
                                ) {

                                    index = pos
                                    return@forEachIndexed

                                }
                            }


                        }
                        Log.e(TAG, "DineInIndax: ${index}")

                        if (index != -1) {
                            val model =
                                cartList[0].dineInList?.get(selectedHeader)?.items?.get(index)
                            if (model != null) {
                                if (type == "UPDATE") {
                                    if (item != null) {
                                        model.itemQuantity = item.itemQuantity
                                    }
                                    dineIn.get(selectedHeader).items[index] = model
                                    cartModel.dineInList = dineIn
                                    addCart(cartModel)
                                } else {
                                    if (index != -1) {
                                        if (item != null) {
                                            model.itemQuantity =
                                                item.itemQuantity + model.itemQuantity
                                            item.modifiers.forEach {
                                                it.itemQuantity = model.itemQuantity
                                            }
                                            model.modifiers = item.modifiers
                                        }

                                        dineIn.get(selectedHeader).items[index] = model
                                        cartModel.dineInList = dineIn
                                        addCart(cartModel)
                                    } else {
                                        cartModel.dineInList = dineInList
                                        addCart(cartModel)

                                    }
                                }


                            }
                        } else {

                            if (item != null) {
                                dineInList.get(dineInList.get(0).selectedPosition).items.add(item)
                            }
                            cartModel.dineInList = dineInList

                            addCart(cartModel)
                        }


                    }


                } else if (type == DELETE) {
                    Log.e(TAG, "HeaderPos:  ${dineInList.get(0).selectedPosition}")
                    Log.e(TAG, "ItemPos: ${dineInList.get(0).itemPosition}")
                    var dine = dineInList.toMutableList()

                    dine.get(0).headerPosition?.let {
                        dine.get(it).items.remove(
                            dine.get(dine.get(0).headerPosition!!).items.get(
                                dine.get(0).itemPosition!!
                            )
                        )
                    }
                    Log.e(TAG, "dinedinedine  ${Gson().toJson(dine)}")
                    cartModel.dineInList = dine
                    addCart(cartModel)
                    /*dineInList.toMutableList().remove(
                        dineInList.get(dineInList.get(0).selectedPosition).items.get(
                            dineInList.get(0).selectedPosition
                        )
                    )*/
                }


            } else {
                // already cart ma hoy to add/update/delete kare flag wise
                val list = cartList?.get(0)?.items?.toMutableList()
                if (list != null && list.isNotEmpty()) {

                    if (type == ADD || type == UPDATE) {
                        var index = -1

                        list.forEachIndexed { pos, tbItem ->
                            if (item != null) {
                                if (tbItem.itemId == item.itemId && checkVariation(
                                        tbItem,
                                        item
                                    ) && checkModifier(tbItem, item)
                                ) {
                                    //   if (checkModifier(tbItem, item)) {
                                    index = pos
                                    return@forEachIndexed
                                    //  }
                                }
                            }

                            /*if (tbItem.itemId == item.itemId && checkModifier(tbItem, item)) {
                            index = pos
                            return@forEachIndexed
                        }*/
                        }
                        if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            if (model != null) {
                                if (type == "UPDATE") {
                                    if (item != null) {
                                        model.itemQuantity = item.itemQuantity
                                    }
                                    list[index] = model
                                } else {
                                    if (index != -1) {
                                        if (item != null) {
                                            model.itemQuantity =
                                                item.itemQuantity + model.itemQuantity
                                            item.modifiers.forEach {
                                                it.itemQuantity = model.itemQuantity
                                            }
                                            model.modifiers = item.modifiers
                                        }

                                        list[index] = model
                                    } else {
                                        if (item != null) {
                                            model.itemQuantity = item.itemQuantity
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

                    if (type == DELETE) {
                        deleteCart()
                    } else {
                        val cartModel = cartList?.get(0)
                        cartModel?.items = listOf(item!!)
                        if (cartModel != null) {
                            addCart(cartModel)
                        }
                    }


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

    private fun checkVariation(tbItem: TbItem, item: TbItem): Boolean {

        if (item.variationsAttributes.isEmpty()) return true

        var variation = false

        item.variationsAttributes.forEach { itemM ->
            tbItem.variationsAttributes.forEach {
                variation = itemM.id == it.id
            }
        }
        return variation
    }

    fun addCartModel(item: TbItem): CartModel {
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
            if (cartList.get(0).orderType == DINE_IN) {

                cartList[0].dineInList?.forEach { dine ->

                    dine.items.forEach { item ->
                        totalCount += item.itemQuantity
                        Log.e(TAG, "ItemDiscountPrice:  ${item.discountPrice}")
                        Log.e(TAG, "ItemPrice:  ${item.price}")

                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice)

                        item.taxes?.forEach { tax ->
                            if (tax.isActive) {
                                if (tax.taxType == "Percentage") {

                                    val price =
                                        (item.price * item.itemQuantity) - item.discountPrice
                                    val itemTaxPrice =
                                        (tax.rate * price) / 100
                                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                                    totalTax += String.format("%.2f", itemTaxPrice)
                                        .toDouble()
                                } else {

                                    val ss = tax.rate * item.itemQuantity
                                    Log.e("tt", ss.toString())

                                    totalTax += String.format("%.2f", ss)
                                        .toDouble()
                                }
                            }
                        }


                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)

                            item.taxes?.forEach { tax ->
                                if (tax.isActive) {
                                    if (tax.taxType == "Percentage") {
                                        val itemTaxPrice =
                                            (tax.rate * (it.price * it.itemQuantity)) / 100
                                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                                        totalTax += String.format("%.2f", itemTaxPrice)
                                            .toDouble()
                                    } else {

                                        totalTax += String.format(
                                            "%.2f",
                                            tax.rate * item.itemQuantity
                                        )
                                            .toDouble()
                                    }
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

                cartList[0].dineInList?.forEach {
                    totalDiscount += it.items.map {
                        it.discountPrice
                    }.sum()
                }
                /*   totalDiscount = cartList[0].items!!.map {
                       it.discountPrice
                   }.sum()*/
                Log.e(TAG, "totalDiscount:  $totalDiscount")
                Log.e(TAG, "SubTotalPrice:   $subTotalPrice")
                Log.e(TAG, "totalTax:  $totalTax")
                Log.e(TAG, "totalServiceCharge:  $totalServiceCharge")

                totalPrice = (subTotalPrice + totalTax + totalServiceCharge) - totalDiscount


            } else {


                cartList[0].items?.forEach { item ->
                    totalCount += item.itemQuantity
                    Log.e(TAG, "ItemDiscountPrice:  ${item.discountPrice}")
                    Log.e(TAG, "ItemPrice:  ${item.price}")

                    subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice)

                    item.taxes?.forEach { tax ->
                        if (tax.isActive) {
                            if (tax.taxType == "Percentage") {

                                val price = (item.price * item.itemQuantity) - item.discountPrice
                                val itemTaxPrice =
                                    (tax.rate * price) / 100
                                Log.e("itemTaxPrice", "" + itemTaxPrice)
                                totalTax += String.format("%.2f", itemTaxPrice)
                                    .toDouble()
                            } else {

                                totalTax += String.format("%.2f", tax.rate * item.itemQuantity)
                                    .toDouble()
                            }
                        }
                    }


                    item.modifiers.forEach {
                        subTotalPrice += (it.price * it.itemQuantity)

                        item.taxes?.forEach { tax ->
                            if (tax.isActive) {
                                if (tax.taxType == "Percentage") {
                                    val itemTaxPrice =
                                        (tax.rate * (it.price * it.itemQuantity)) / 100
                                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                                    totalTax += String.format("%.2f", itemTaxPrice)
                                        .toDouble()
                                } else {

                                    totalTax += String.format("%.2f", tax.rate * item.itemQuantity)
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
                Log.e(TAG, "totalDiscount:  $totalDiscount")
                Log.e(TAG, "SubTotalPrice:   $subTotalPrice")
                Log.e(TAG, "totalTax:  $totalTax")
                Log.e(TAG, "totalServiceCharge:  $totalServiceCharge")

                totalPrice = (subTotalPrice + totalTax + totalServiceCharge) - totalDiscount

            }

            MethodUtils.setPriceTextView(txtTotalAmount, totalPrice - cartList[0].discountPrice)
        }

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


    fun logoutAPI() {

        _showProgress.value = Event(true)
        viewModelScope.launch {
            val dataClockout = HashMap<String, String>()
            dataClockout["passcode"] = prefProvider.getValue(Constants.PASSCODE, "").toString()
            dataClockout["terminal_id"] =
                prefProvider.getValueInt(Constants.TERMINAL_ID, -1).toString()

            val resourceClockout = posRepository.employeeClockOut(dataClockout)
            when (resourceClockout.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    prefProvider.setValueboolean(Constants.IS_CLOCKOUT, true)
                    resourceClockout.data.let {
                        if (it?.status == 200) {
                            resourceClockout.data?.let {
                                logoutApi()
                            }
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceClockout.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }


        }
    }

    private suspend fun logoutApi() {
        _showProgress.value = Event(true)
        val data = HashMap<String, String>()
        data["email"] =
            prefProvider.getValue(Constants.EMAIL, "").toString()
        val resource = posRepository.logout(data)
        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                resource.data?.let { it ->

                    _logout.value = Event(true)


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

    fun clearTable() {

        viewModelScope.launch {
            posRepository.clearTable()
        }
    }

}