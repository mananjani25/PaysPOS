package com.pays.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.DineInOrderDetailAttributes
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TimeFormatUtils
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.ceil

@HiltViewModel
class CheckoutDineInPaymentViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository,
    private val rolePermission: RolePermission

) : ViewModel() {

    var dineInHeaderPosition: Int = 0
    var dineInSelectedItemHeaderPos: Int = 0
    var selectedItemPositionDine: Int = 0
    val TAG = "CheckoutDinePayViewMo"
    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var nonCashAdj: Double = 0.0
    var totalServiceCharge = 0.0
    var cashdiscountAmount = 0.0
    var cashDiscountType = ""
    var totalDiscount = 0.0
    var tip = 0.0
    var cartModel: CartModel? = null
    var assignCustomer: TbCustomer? = null
    var orderItemDiscount = 0.0
    var selectedCustomer: TbCustomer? = null
    var activeLoyaltyProgram: LoyaltyProgramsModel? = null
    var redeemLoyaltyInfo: RedeemLoyaltyInfo = RedeemLoyaltyInfo()
    var paymentType: String = "cash"
    var destroyedList: ArrayList<TbItem> = arrayListOf()
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    var isOrderUpdate: Boolean = false
    val returnedVal = posRepository.getManualCategoryId()
    var viewModelcartList: ArrayList<CartModel> = arrayListOf()

    private var mPosition: Int = 0

    private val _updateOrder = MutableLiveData<Event<Any?>>()
    val updateOrder: LiveData<Event<Any?>> = _updateOrder

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()


    fun venueDataLocal(): LiveData<Resource<List<CategoryWithInventory?>>> {
        return posRepository.venueDataLocal()
    }

    fun orderTypes(): LiveData<Resource<List<TbOrderType>>> {
        return posRepository.orderTypesDb()
    }

    fun setOrderTypeList(ordertypelist: ArrayList<TbOrderType>) {
        this.ordertypelist = ordertypelist
    }

    fun setCartModel(cartList: List<CartModel>) {
        this.cartModel = generateCombinedItems(cartList[0])
    }

    val getOrderTypes = posRepository.getOrderTypes()

    val activeLoyaltyProgramLiveData = posRepository.getActiveLoyaltyProgramFromDb()

    val taxList = posRepository.taxList()

    val discountList = posRepository.disocuntList()

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _logout = MutableLiveData<Event<Boolean>>()
    val logout: LiveData<Event<Boolean>> = _logout

    val _tableStatusSuccess = MutableLiveData<Event<Int>>()
    val tableCheckSuccess: LiveData<Event<Int>> = _tableStatusSuccess

    val _tableStatus = MutableLiveData<Event<String>>()
    val tableCheck: LiveData<Event<String>> = _tableStatus

    val _callCashDiscount = MutableLiveData<Event<Boolean>>()
    val callCashDiscount: LiveData<Event<Boolean>> = _callCashDiscount

    private val _queueStart = MutableLiveData<Event<CreateOrderResponse?>>()
    val QueueStart: LiveData<Event<CreateOrderResponse?>> = _queueStart

    val _Basedata = MutableLiveData<Event<CreateOrderResponse.Data?>>()

    var barcodeFoundDbItemLiveData: LiveData<Resource<TbItem>>? = null


    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    fun getItemsbyId(itemId: Int) = posRepository.getItemsbyId(itemId)

    fun getItemByProductCode(productCode: String) = posRepository.getItemByProductCode(productCode)

    /*
        fun getCartList(orderType:String,employee_Id: Int) : List<CartModel>{
            viewModelcartList.clear()
            viewModelcartList = arrayListOf()

            posRepository.getCartList(orderType,employee_Id)
        }

    */
    fun mAllWords(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getCartList(orderType, employee_Id)

    }

    // To get dine cart from database
    fun mAllWordsDineIn(empId: Int): LiveData<List<DineInCartModel>> {
        return posRepository.getCartDineInList(empId)

    }

    // To get manual sales cart from database
    fun manualSale(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getCartList(orderType, employee_Id)

    }

    fun manualSaleItems(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getManualSaleItems(orderType, employee_Id)

    }

    var serviceChargesList: List<TbServiceCharge> = emptyList()

    // To update cart and save in database
    fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                posRepository.addItemCart(cartModel)
            }
            destroyedList.clear()
        }
    }

    fun createEmptyCart(model: CartModel) {
        viewModelScope.launch {
            posRepository.createEmptyCart(model)
        }


    }

    // merge items if added tow similat items in cart
    fun generateCombinedItems(cartModel: CartModel): CartModel {
        val combinedItems = arrayListOf<TbItem>()
        cartModel.items?.let { combinedItems.addAll(it) }
        if (cartModel.isOpenOrder && isOrderUpdate) {
            combinedItems.addAll(destroyedList)
        }
        cartModel.items = combinedItems
        return cartModel
    }

    fun deleteDineInCart() {
        viewModelScope.launch {
            posRepository.deleteDineInCart()
            destroyedList.clear()
        }

    }

    // to delete cart from database
    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0))
            destroyedList.clear()
        }
    }


    // To delete manual sales cart from DB
    fun deleteManualSaleCart() {
        viewModelScope.launch {
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            posRepository.deleteManualSaleCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0))

        }
    }

    // To update dine cart
    fun dineInCartUpdate(
        cartList: List<CartModel>?,
        dineInList: List<DineInModel>
    ) {

        val cartModel = cartList!!.get(0)
        cartModel.dineInList = dineInList
        cartModel.orderType = Constants.DINE_IN
        cartModel.let {
            addCart(it)


        }

    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }


    fun manualSalecartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {

        if (cartList != null && cartList.isEmpty()) {

            val model = addCartModel(item, true)
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()

            if (list != null && list.isNotEmpty()) {


                if (type == Constants.ADD) {
                    list.add(item)
                } else if (type == Constants.UPDATE) {


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

                } else if (type == Constants.DELETE) {

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
                    deleteCart()
                }
            } else {

                if (type == Constants.DELETE) {
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

    // This methods contains logic of maintaining any cart modification
    fun cartLogic(
        cartList: List<CartModel>?,
        item: TbItem?,
        type: String,
        isManualSales: Boolean,
        dineInList: List<DineInModel> = arrayListOf()
    ) {

        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare
            val cartModel = item?.let { addCartModel(it, isManualSales) }
            if (cartModel != null) {
                addCart(cartModel)
            }


        } else {
            if (cartList?.get(0)?.orderType == Constants.DINE_IN) {
                val cartModel = cartList[0]
                cartModel.dineInList = dineInList
                if (type == Constants.ADD || type == Constants.UPDATE) {
                    var index = -1
                    val dineIn = dineInList
                    if (dineIn != null && dineIn.isNotEmpty()) {
                        val selectedHeader = dineInList.get(0).selectedPosition

                       /* dineIn.get(selectedHeader).items.forEachIndexed { pos, tbItem ->
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


                        }*/


                        if (index != -1) {
                            val model =
                                cartList[0].dineInList?.get(selectedHeader)?.items?.get(index)
                            if (model != null) {
                                if (type == "UPDATE") {
                                    if (item != null) {
                                        model.itemQuantity = item.itemQuantity
                                    }
                                    if (prefProvider.getValueboolean(
                                            Constants.DINE_IN_UPDATE,
                                            false
                                        )
                                    ) {
                                        model.isEdited = true
                                    }

                                    dineIn.get(selectedHeader).items[index] = model
                                    dineIn.get(selectedHeader).floorPlanTable =
                                        dineInList.get(0).floorPlanTable

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

                                        if (prefProvider.getValueboolean(
                                                Constants.DINE_IN_UPDATE,
                                                false
                                            )
                                        ) {
                                            model.isEdited = true
                                        }

                                        dineIn.get(selectedHeader).items[index] = model

                                        dineIn.get(0).floorPlanTable =
                                            dineInList.get(0).floorPlanTable
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
                                item.timeStamp = randomOfflineId()
                                if (prefProvider.getValueboolean(
                                        Constants.DINE_IN_UPDATE,
                                        false
                                    )
                                ) {
                                    item.isEdited = true
                                }

                                dineInList.get(dineInList.get(0).selectedPosition).items.add(item)
                            }
                            cartModel.dineInList = dineInList

                            addCart(cartModel)
                        }


                    }


                } else if (type == Constants.DELETE) {

                    var dine = dineInList.toMutableList()

                    LogUtil.logE(TAG, "dineInHeaderPosition:  ${dineInHeaderPosition}")
                    LogUtil.logE(TAG, "selectedItemPositionDine  ${selectedItemPositionDine}")

                    dineInHeaderPosition?.let {
                        dine.get(it).items.remove(
                            dine.get(dineInHeaderPosition).items.get(
                                selectedItemPositionDine
                            )
                        )
                    }

                    cartModel.dineInList = dine
                    addCart(cartModel)
                    /*dineInList.toMutableList().remove(
                        dineInList.get(dineInList.get(0).selectedPosition).items.get(
                            dineInList.get(0).selectedPosition
                        )
                    )*/
                } else if (type == Constants.DINE_IN_LIST_EDIT) {
                    cartModel.orderTypeName = Constants.DINE_IN
                    cartModel.orderType = Constants.DINE_IN
                    cartModel.dineInList = dineInList
                    addCart(cartModel)

                }


            } else {
                // already cart ma hoy to add/update/delete kare flag wise
                val list = cartList?.get(0)?.items?.toMutableList()
                if (list != null && list.isNotEmpty()) {

                    if (type == Constants.ADD || type == Constants.UPDATE) {
                        var index = -1

                        /*for (i in list.indices) {
                            if (item != null) {
                                if (list[i].itemId == item.itemId && checkVariation(
                                        list[i],
                                        item
                                    ) && checkModifier(list[i], item)
                                ) {
                                    Log.d(TAG, "cartLogic: " + i)
                                    index = i
                                    break
                                }
                            }
                        }*/
//                        list.forEachIndexed { pos, tbItem ->
//                            if (item != null) {
//                                if (tbItem.itemId == item.itemId && checkVariation(
//                                        tbItem,
//                                        item
//                                    ) && checkModifier(tbItem, item)
//                                ) {
//                                    //   if (checkModifier(tbItem, item)) {
//                                    index = pos
//                                    return@forEachIndexed
//                                    //  }
//                                }
//                            }
//
//                            /*if (tbItem.itemId == item.itemId && checkModifier(tbItem, item)) {
//                            index = pos
//                            return@forEachIndexed
//                        }*/
//                        }
                        if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            Log.d(TAG, "cartLogic: " + index)
                            if (model != null) {
                                if (type == "UPDATE") {
                                    if (item != null) {
                                        model.itemQuantity = item.itemQuantity
                                        if (item.isEdited) {
                                            model.isEdited = item.isEdited
                                        }
                                        item.modifiers.forEach {
                                            it.itemQuantity = model.itemQuantity
                                        }
                                        model.modifiers = item.modifiers

                                        itemDiscountApply(model, item)

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
                                            if (item.isEdited) {
                                                model.isEdited = item.isEdited
                                            }
                                            itemDiscountApply(model, item)
                                        }

                                        list[index] = model
                                    } else {
                                        if (item != null) {
                                            model.itemQuantity = item.itemQuantity
                                            if (item.isEdited) {
                                                model.isEdited = item.isEdited
                                            }
                                            itemDiscountApply(model, item)
                                        }
                                        list[index] = model
                                    }
                                }

                            }
                        } else {
                            Log.d(TAG, "cartLogic: " + index)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                    } else if (type == Constants.DELETE) {

                        var index = -1

                        list.forEachIndexed { pos, tbItem ->
                            if (item != null) {
                                if (tbItem.itemId == item.itemId) {
                                    index = pos
                                    return@forEachIndexed
                                }
                            }
                        }
                        LogUtil.logE(TAG, "DeleteIndex  ${index}")
                        if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            if (model != null) {
                                //delete from cart
                                if (item?.isEdited == true) {
                                    model.isEdited = item.isEdited
                                    model.isDestroy = true
                                } else {
                                    LogUtil.logE(TAG, "listRemoveItem")
                                    list.remove(model)
                                }
                            }
                        } else {
                            //list.remove(item)
                        }
                    }

                    val cartModel = cartList[0]
                    cartModel.items = list
                    addCart(cartModel)
                    Log.d(TAG, "cartLogic: " + list.size)
                    if (list.isEmpty()) {
                        // delete carts
                        deleteCart()
                    }
                } else {

                    if (type == Constants.DELETE) {
                        deleteCart()
                    } else {

                        LogUtil.logE(TAG, "AddedListNull")
                        val cartModel = cartList?.get(0)
                        if (item != null)
                            cartModel?.items = listOf(item)
                        if (cartModel != null) {
                            addCart(cartModel)
                        }
                    }


                }
            }


        }
    }

    private fun itemDiscountApply(model: TbItem, item: TbItem) {
        model.discountPrice = item.discountPrice
        model.discountType = item.discountType
        model.discountId = item.discountId
        model.isManualSales = item.isManualSales
        model.note = item.note
    }

    suspend fun addItemToCart(
        list: ArrayList<TbCartItem>?,
        item: TbCartItem?,
        type: String, isManualSales: Boolean,
        dineInList: List<DineInModel> = arrayListOf()
    ) {
            if (list != null && list.isNotEmpty()) {

                if (type == Constants.ADD || type == Constants.UPDATE) {
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

                    }
                    if (index != -1) {
                        val model = list.get(index)
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
                                        item.modifiers.forEach {
                                            it.itemQuantity = model.itemQuantity
                                        }
                                        model.modifiers = item.modifiers
                                        if (item.isEdited) {
                                            model.isEdited = item.isEdited
                                        }
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
                } else if (type == Constants.DELETE) {

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
                        val model = list.get(index)
                        if (model != null) {
                            //delete from cart
                            if (item?.isEdited == true) {
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

//                val cartModel = cartList[0]
//                cartModel.items = list
//                addCart(cartModel)
                list.forEach {
                    posRepository.addItemToCart(it)
                }

                if (list.isEmpty()) {
                    // delete cart
                    deleteCart()
                }
            } else {

                if (type == Constants.DELETE) {
                    deleteCart()
                } /*else {

                    LogUtil.logE(TAG, "AddedListNull")
                    val cartModel = cartList?.get(0)
                    cartModel?.items = listOf(item!!)
                    LogUtil.logE(TAG, "cartModel:  ${Gson().toJson(cartModel)}")
                    if (cartModel != null) {

                        addCart(cartModel)
                    }
                }*/


            }

    }

    private fun checkModifier(tbItem: TbCartItem, item: TbCartItem): Boolean {

        if (item.modifiers.isEmpty()) return true

        var checkModifier = false

        item.modifiers.forEach { itemM ->
            if (tbItem.modifiers.isNotEmpty()) {
                tbItem.modifiers.forEach {
                    return (itemM.id == it.id).also { checkModifier = it }
                }
            } else {
                return true.also { checkModifier = it }
            }
        }
        return checkModifier
    }

    // to check if two items are similar
        private fun checkVariation(tbItem: TbCartItem, item: TbCartItem): Boolean {

        if (item.variationsAttributes.isEmpty()) return true

        var variation = false

        item.variationsAttributes.forEach { itemM ->
            tbItem.variationsAttributes.forEach {
                variation = itemM.id == it.id
            }
        }
        return variation
    }

    fun addCartModel(item: TbItem, isManualSales: Boolean): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
            orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
            orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
            isMaual = isManualSales
            serviceCharge = serviceChargesList
            customer = assignCustomer
            item.itemQuantity = item.itemQuantity
            inventoryModelList.add(item)
            items = inventoryModelList
        }
    }

    fun getCashDiscountDetails(active: Int): LiveData<CashDiscountModel>? {
        return posRepository.getCashDisDetail(active)
    }

    // To manage calculation of cart items on item added / updated /removed
    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        cartList: List<CartModel>?,
        txtTotalAmount: AppCompatTextView,
        context: Context
    ) {

        LogUtil.logE("itemCalculation", "------------------>")

        var totalAmmount = 0.0
        nonCashAdj = 0.0
        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalDiscount = 0.0
        totalTax = 0.0
        totalServiceCharge = 0.0
        var amountToBePaid = 0.0
        if (cartList != null && cartList.isNotEmpty()) {
            if (cartList[0].orderType == Constants.DINE_IN) {

                cartList[0].dineInList?.forEach { dine ->

                    dine.items.forEach { item ->
                        totalCount += item.itemQuantity
                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                        taxCalculation(item)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)
                        }
                    }


                }

                calculateDineInServiceCharge(cartList[0])
                subTotalPrice -= (cartList[0].discountPrice)


                cartList[0].dineInList?.forEach {
                    it.items.forEach {
                        totalDiscount += (it.discountPrice * it.itemQuantity)

                    }
                }


                totalPrice = (subTotalPrice + totalTax + totalServiceCharge)
                amountToBePaid = totalPrice - totalDiscount

                MethodUtils.setPriceTextView(txtTotalAmount, amountToBePaid)
            } else {


                if (cartList[0].items?.isEmpty() == false) {


                    cartList[0].items?.forEach { item ->
                        totalCount += item.itemQuantity

                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                        taxCalculation(item)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)

                        }
                    }


                    serviceChargeCalculation(cartList)
                    subTotalPrice -= cartList[0].discountPrice


                    totalDiscount += cartList[0].discountPrice

                    cartList[0].items!!.forEach {
                        totalDiscount += (it.discountPrice * it.itemQuantity)
                    }

                    LogUtil.logE("totalDiscount", totalDiscount.toString())

                    totalPrice = (subTotalPrice + totalTax + totalServiceCharge)


                    //loyalty point and price calculation
                    amountToBePaid = totalPrice


                    if (selectedCustomer == null) {
                        var fnAmount = amountToBePaid
                        MethodUtils.setPriceTextView(
                            txtTotalAmount,
                            fnAmount
                        )
                    } else {
                        var fnAmount = amountToBePaid
                        checkAppliedLoyaltyProgram(
                            selectedCustomer,
                            fnAmount,
                            txtTotalAmount
                        )
                    }

                    LogUtil.logE("amountToBePaid", "" + amountToBePaid)
                } else {

                    nonCashAdj = 0.0
                    totalPrice = 0.0
                    totalCount = 0
                    subTotalPrice = 0.0
                    totalDiscount = 0.0
                    totalTax = 0.0
                    totalServiceCharge = 0.0
                    amountToBePaid = 0.0
                }
            }
        }
        //totalAmmount = totalPrice-cartList[0].discountPrice

        LogUtil.logE("itemCalculation 1", "------------------>")
    }

    @SuppressLint("SetTextI18n")
    fun itemCalculationCartModel(
        cartModel: CartModel,
        txtTotalAmount: AppCompatTextView,
        context: Context
    ) {


        var totalAmmount = 0.0
        nonCashAdj = 0.0
        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalDiscount = 0.0
        totalTax = 0.0
        totalServiceCharge = 0.0
        var amountToBePaid = 0.0
        if (cartModel.orderType == Constants.DINE_IN) {

            cartModel.dineInList?.forEach { dine ->

                dine.items.forEach { item ->
                    totalCount += item.itemQuantity
                    subTotalPrice +=
                        (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                    taxCalculation(item)

                    item.modifiers.forEach {
                        subTotalPrice += (it.price * it.itemQuantity)
                    }
                }


            }

            serviceChargeCalculationModel(cartModel)
            subTotalPrice -= cartModel.discountPrice
            totalDiscount += cartModel.discountPrice
            cartModel.dineInList?.forEach {
                it.items.forEach {
                    totalDiscount +=
                        (it.discountPrice * it.itemQuantity)
                }
            }


            var finalTotal = 0.0
            finalTotal = (subTotalPrice + totalTax + totalServiceCharge)
            cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
            //loyalty point and price calculation
            amountToBePaid = finalTotal
            if (selectedCustomer == null) {
                totalPrice = amountToBePaid
                MethodUtils.setPriceTextView(
                    txtTotalAmount,
                    amountToBePaid
                )
            } else {
                checkAppliedLoyaltyProgram(
                    selectedCustomer,
                    amountToBePaid,
                    txtTotalAmount
                )
                redeemLoyaltyInfo.getAmountToBePaid()?.let {
                    totalPrice = it
                }
            }

            if (MethodUtils.isEnableCashDiscount(context)) {
                cashdiscountAmount = MethodUtils.calculateCashDiscount(
                    totalPrice,
                    prefProvider,
                    context
                )
            } else {
                cashdiscountAmount = 0.0
            }


        } else {

            if (cartModel.items?.isEmpty() == false) {


                cartModel.items?.forEach { item ->
                    totalCount += item.itemQuantity
                    subTotalPrice +=
                        (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                    taxCalculation(item)

                    item.modifiers.forEach {
                        subTotalPrice += (it.price * it.itemQuantity)

                    }
                }


                serviceChargeCalculationModel(cartModel)
                subTotalPrice -= cartModel.discountPrice

                totalDiscount += cartModel.discountPrice

                cartModel.items!!.forEach {
                    totalDiscount +=
                        (it.discountPrice * it.itemQuantity)
                }

                var finalTotal = 0.0
                finalTotal = (subTotalPrice + totalTax + totalServiceCharge)



                cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                //loyalty point and price calculation
                amountToBePaid = finalTotal
                if (selectedCustomer == null) {
                    totalPrice = amountToBePaid
                    MethodUtils.setPriceTextView(
                        txtTotalAmount,
                        amountToBePaid
                    )
                } else {
                    checkAppliedLoyaltyProgram(
                        selectedCustomer,
                        amountToBePaid,
                        txtTotalAmount
                    )
                    redeemLoyaltyInfo.getAmountToBePaid()?.let {
                        totalPrice = it
                    }
                }

                if (MethodUtils.isEnableCashDiscount(context)) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalPrice,
                        prefProvider,
                        context
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                LogUtil.logE("amountToBePaid", "" + totalPrice)
            } else {

                nonCashAdj = 0.0
                totalPrice = 0.0
                totalCount = 0
                subTotalPrice = 0.0
                totalDiscount = 0.0
                totalTax = 0.0
                totalServiceCharge = 0.0
                amountToBePaid = 0.0
            }
        }
        //totalAmmount = totalPrice-cartList[0].discountPrice


    }

    // To check if loyalty program is applicable or not
    fun loyaltyPointCondition(customer: TbCustomer?): Boolean {
        return (customer?.enroll_to_loyalty == true && activeLoyaltyProgram != null && activeLoyaltyProgram?.rewardPoint ?: 0 <= customer.final_reward ?: 0)
    }

    // check and calculate loyalty points value by selected customer and available points and redeemption eligibility
    private fun checkAppliedLoyaltyProgram(
        customer: TbCustomer?,
        total: Double,
        txtTotalAmount: AppCompatTextView
    ) {

        redeemLoyaltyInfo.total = total
        val availablePoints = customer?.final_reward ?: 0

        if (customer == null) {
            //loyalty cant be applied if customer is not selected.
            redeemLoyaltyInfo.needToApplyLoyalty = false
        } else if (loyaltyPointCondition(customer)) {
            activeLoyaltyProgram?.let {
                redeemLoyaltyInfo.loyaltyProgramsModel = activeLoyaltyProgram

                //if customer has more points than required(minimum limit)
                var availableLoyaltyAmount = 0.0
                if (it.rewardPoint == 0) {
                    it.rewardPoint = 1
                }
                availableLoyaltyAmount =
                    availablePoints * it.amount / it.rewardPoint
                if (availableLoyaltyAmount > redeemLoyaltyInfo.total) {
                    var pointDouble = (redeemLoyaltyInfo.total * it.rewardPoint) / it.amount
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
            redeemLoyaltyInfo.remainingLoyaltyPoints = availablePoints
            redeemLoyaltyInfo.usedLoyaltyPoints = 0
            redeemLoyaltyInfo.usedLoyaltyAmount = 0.0
            //redeemLoyaltyInfo.isLoyaltyApplied = false
        }
        redeemLoyaltyInfo.getAmountToBePaid()?.let {
            MethodUtils.setPriceTextView(
                txtTotalAmount,
                it
            )
        }

    }

    // To calculate service charge or cash discount of total amount
    private fun serviceChargeCalculation(cartList: List<CartModel>) {
        val serviceChargesList = cartList[0].serviceCharge

        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        totalServiceCharge += (subTotalPrice * it.percentage) / 100
                    }
                }
            }

        }
    }

    fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
        return (minn <= value && value <= maxx)
    }
    fun checkMaxGuestCountId(): Int {
        var maxValue = 0
        var serviceChargeId = 0
        serviceChargesList.forEach { serviceCharge ->
            if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                if (serviceCharge.max_guest_count!! >= maxValue) {
                    maxValue = serviceCharge.max_guest_count
                    serviceChargeId = serviceCharge.id
                }
            }
        }
        return serviceChargeId
    }
    private fun calculateDineInServiceCharge(cartModel: CartModel) {
        var guestCount = cartModel.dineInList?.size?.minus(1)
        if (serviceChargesList.isNotEmpty() && serviceChargesList != null) {
            LogUtil.logE(TAG, "dashboardserviceChargesList:  ${Gson().toJson(serviceChargesList)}")
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                var isApplied = false
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                guestCount!!
                            )
                        ) {
                            isApplied = true
                            totalServiceCharge += (subTotalPrice * it.percentage) / 100
                            return@forEach
                            Log.d(
                                TAG,
                                "calculateDineInServiceCharge: Checkout " + it.min_guest_count + "....." + it.max_guest_count + " in between " + guestCount
                            )
                        }
                    }
                }
                if (!isApplied) {
                    serviceChargesList.forEach { service ->
                        if (service.id == checkMaxGuestCountId()) {
                            totalServiceCharge += (subTotalPrice * service.percentage) / 100
                            return@forEach
                        }
                    }
                }
            }

        }

    }

    private fun serviceChargeCalculationModel(cartModel: CartModel) {
        val serviceChargesList = cartModel.serviceCharge

        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        totalServiceCharge += (subTotalPrice * it.percentage) / 100
                    }
                }
            }
        }
    }

    private fun taxCalculation(item: TbItem) {
        item.taxes?.forEach { tax ->
            if (tax.isActive) {
                totalTax += if (tax.taxType == "Percentage") {
                    Log.d("yash", "taxCalculation: " + tax.taxType)

                    var modifierPrice = 0.0
                    val price =
                        (item.price * item.itemQuantity) - item.discountPrice

                    item.modifiers.forEach {
                        modifierPrice += (it.price * it.itemQuantity)
                    }

                    val totalPrice = price + modifierPrice

                    val itemTaxPrice =
                        (tax.rate * totalPrice) / 100
                    LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                    String.format("%.2f", itemTaxPrice)
                        .toDouble()
                } else {
                    Log.d("yash", "taxCalculation: " + tax.taxType)
                    String.format("%.2f", tax.rate * item.itemQuantity)
                        .toDouble()
                }
            }
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
                                callLogoutApi()
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

    private suspend fun callLogoutApi() {
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

    fun createDineInOrderRequest(
        cartModel: CartModel,
        subTotalPrice: Double,
        totalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        ORDER_TYPE: String,
        future_delivery_date: String,
        future_delivery_time: String,
        isPaid: Boolean,
        totalDiscount: Double,
        tipAmount: Double,
        floorPlanDetails: DineInOrderDetailAttributes
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()

        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()

        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date
        orderAttributeRequestModel.deliveryType = cartModel.openOrderType
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId = randomOfflineId()
        orderAttributeRequestModel.openOrderType = cartModel.orderType
        orderAttributeRequestModel.orderTypeId = cartModel.orderTypeId
        orderAttributeRequestModel.orderTypeName = prefProvider.getValue(ORDER_TYPE_NAME, DINE_IN)
        orderAttributeRequestModel.paymentStatus = 0
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
        orderAttributeRequestModel.totalAmount =
            MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                tipAmount
            )
        orderAttributeRequestModel.dineInOrderDetailsAttr = floorPlanDetails
        orderAttributeRequestModel.totalDiscount = totalDiscount
        orderAttributeRequestModel.totalServiceCharges =
            MethodUtils.roundOffAmountDouble(totalServiceCharge)
        orderAttributeRequestModel.totalTaxAmount = MethodUtils.roundOffAmountDouble(totalTax)
        orderAttributeRequestModel.totalTips = MethodUtils.roundOffAmountDouble(tipAmount)
        cartModel.taxlistDynamic?.forEach { taxData ->
            if (taxData.taxType == "Percentage") {
                taxData.percentage_value =
                    MethodUtils.roundOffAmountDouble(taxData.rate)
            } else {
                taxData.percentage_value =
                    MethodUtils.roundOffAmountDouble((100 * taxData.totalTaxTypePrice) / taxData.subTotalAmount!!)
            }
        }
        orderAttributeRequestModel.tax_bifurcation_data = Gson().toJson(cartModel.taxlistDynamic)

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = ""+customerId
        }

        /*  orderAttributeRequestModel.paymentAttributes =
              paymentAttributes(
                  cartModel,
                  totalPrice,
                  subTotalPrice,
                  totalServiceCharge,
                  totalTax,
                  totalDiscount, tipAmount
              )
    */
        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)

        orderAttributeRequestModel.guestsAttributes = getGuestsAttributes(cartModel)
        LogUtil.logE(
            TAG,
            "guestsAttributesData:  ${Gson().toJson(orderAttributeRequestModel.guestsAttributes)}"
        )

        orderAttributeRequestModel.orderItemsAttributes = dineInOrderItemAttributed(cartModel)


        val orderRequestModel = OrderRequestModel(false, orderAttributeRequestModel)


        return orderRequestModel
    }

    fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        LogUtil.logE("timeStampFinal", timeStampFinal)

        return timeStampFinal
    }

    protected open fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }

    private fun paymentAttributes(
        cartModel: CartModel,
        totalPrice: Double,
        subTotalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        totalDis: Double,
        tipAmount: Double
    ): PaymentAttributes {
        return PaymentAttributes().apply {
//            if (isUpdateOrder)
//                id = paymentId
            amount =
                MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                    tipAmount
                )
//            cardName = ""
//            cardNumber = ""
//            cardType = 0
            cash_discount_or_surcharge = 0.0
            cashDiscountFee = 0.0
            employeeId = cartModel.employeeID
            offlineId = randomOfflineId()
            payableType = "Order"
            paymentType = "Cash"
            serviceChargeAmount = MethodUtils.roundOffAmountDouble(totalServiceCharge)
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            taxAmount = MethodUtils.roundOffAmountDouble(totalTax)
            terminalId = cartModel.terminalId
            tips = MethodUtils.roundOffAmountDouble(tipAmount)
            tipsAdjusted = false
            totalDiscount = MethodUtils.roundOffAmountDouble(totalDis)


            //           transactionId = ""
        }
    }
    private fun orderServiceChargesAttributes(
        cartModel: CartModel,
        subTotalPrice: Double
    ): List<OrderServiceChargesAttribute> {
        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        val serviceChargesList = cartModel.serviceCharge
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                    orderServiceChargesAttribute.amount =
                        MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                    orderServiceChargesAttribute.name = it.name
                    orderServiceChargesAttribute.rate = it.percentage
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttribute.order_type = it.order_type
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttributeList.add(orderServiceChargesAttribute)
                }
            } else if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                serviceChargesList.forEach {
                    val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                    orderServiceChargesAttribute.amount =
                        MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                    orderServiceChargesAttribute.name = it.name
                    orderServiceChargesAttribute.rate = it.percentage
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttribute.order_type = it.order_type
                    orderServiceChargesAttribute.max_guest_count = it.max_guest_count
                    orderServiceChargesAttribute.min_guest_count = it.min_guest_count
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttributeList.add(orderServiceChargesAttribute)
                }
            }

        }
        return orderServiceChargesAttributeList
    }

    private fun getGuestsAttributes(cartModel: CartModel): List<GuestsAttributes> {
        val orderItemsAttributeList: ArrayList<GuestsAttributes> = arrayListOf()
        cartModel.dineInList?.forEach { it ->
            val model = GuestsAttributes()
            model.name = it.title.toString()
            if (it.id != 0) {
                model.id = it.id
            }
            if (it.items.isNotEmpty()) {
                var listItems: ArrayList<GuestItemsAttributes> = arrayListOf()
                var subTotal = 0.0
                var totalTax = 0.0
                var totalTips = 0.0
                var totalDiscount = 0.0
                var totalAmount = 0.0
                it.items.forEach { tb ->


                    listItems.add(
                        GuestItemsAttributes(
                            id = tb.guestItemId,
                            orderItemId = tb.orderItemId,
                            quantity = tb.itemQuantity,
                            itemId = tb.itemId,
                            amount = tb.price,
                            timestamp = tb.timeStamp,
                            guestId = it.id?.let { it }

                        )

                    )



                    subTotal += tb.price
                    tb.taxes?.forEach {
                        totalTax += it.rate
                    }
                    totalDiscount += tb.discountPrice

                }
                totalAmount = (subTotal + totalTax) - totalDiscount
                model.totalAmount = totalAmount
                model.totalTax = totalTax
                model.totalTips = totalTips
                if (it.id != null && it.id != 0) {
                    model.id = it.id
                }



                model.guestItemsAttributes = listItems
            }


            if (it.customer != null) {
                model.customerId = it.customer?.id
                var addressList: ArrayList<CustomerAttributes.AddressesAttribute> =
                    arrayListOf()
                var phoneList: ArrayList<CustomerAttributes.PhonesAttribute> = arrayListOf()
                for (i in 0.until(it.customer?.addresses?.size!!)) {

                    var address = CustomerAttributes.AddressesAttribute()
                    address.address1 = it.customer?.addresses?.get(i)?.address1.toString()
                    address.address2 = it.customer?.addresses?.get(i)?.address2.toString()
                    address.addressableId = it.customer?.addresses?.get(i)?.id
                    address.city = it.customer?.addresses?.get(i)?.city.toString()
                    address.country = it.customer?.addresses?.get(i)?.country.toString()
                    /*address.latitude = it.customer?.addresses?.get(i)?.latitude!!.toDouble()
                    address.longitude = it.customer?.addresses?.get(i)?.longitude!!.toDouble()*/
                    address.latitude = 0.0
                    address.longitude = 0.0
                    address.state = it.customer?.addresses?.get(i)?.state.toString()
                    addressList.add(address)
                }
                for (i in 0 until it.customer?.phones?.size!!) {
                    val phoneModel = CustomerAttributes.PhonesAttribute()
                    phoneModel.id = it.customer?.phones?.get(i)?.id
                    phoneModel.customerId = it.customer?.id
                    phoneModel.phoneNumber =
                        it.customer?.phones?.get(i)?.phone_number.toString()
                    phoneList.add(phoneModel)
                }
                val customerModel = CustomerAttributes()
                /*  customerModel.addressesAttributes = addressList
                  customerModel.birthDate = it.customer?.birth_date.toString()
                  customerModel.firstName = it.customer?.first_name.toString()
                  customerModel.lastName = it.customer?.last_name.toString()*/
                customerModel.id = it.customer?.id
                /* customerModel.companyName = it.customer?.company.toString()
                 customerModel.phonesAttributes = phoneList
                 customerModel.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
    */
                //  model.customerAttributes = customerModel

            } else {
                model.customerId = 0
            }
            orderItemsAttributeList.add(model)


        }

        return orderItemsAttributeList

    }

    private fun dineInOrderItemAttributed(cartModel: CartModel): List<OrderItemsAttribute> {
        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> =
            arrayListOf()

        for (i in 0 until cartModel.dineInList?.size!!) {
            cartModel.dineInList?.get(i)?.items?.forEach { item ->

                val orderItemsAttribute = OrderItemsAttribute()

                orderItemsAttribute.category_id = item.categoryId
                orderItemsAttribute.custom_item_id = item.id

                orderItemsAttribute.id = item.orderItemId

                orderItemsAttribute.discountAmount = (item.discountPrice * item.itemQuantity)
                orderItemsAttribute.discountType = item.discountType
                if (item.discountId != -1)
                    orderItemsAttribute.discountId = item.discountId
                orderItemsAttribute.employeeId = cartModel.employeeID
                orderItemsAttribute.isCount = 0
                orderItemsAttribute.isEdited = item.isEdited
                orderItemsAttribute.isDestroy = item.isDestroy
                LogUtil.logE(TAG, "Passes: ${item.isDestroy}")
                orderItemsAttribute.isPaid = item.isPaid
                orderItemsAttribute.isPrinted = false
                orderItemsAttribute.isTaxRemoved = false
                orderItemsAttribute.itemId =
                    if (item.isManualSales) item.itemId else item.itemId
                orderItemsAttribute.is_manual_sales = item.isManualSales
                orderItemsAttribute.itemName = item.name
                orderItemsAttribute.note = item.note
                orderItemsAttribute.price = item.price
                orderItemsAttribute.quantity = item.itemQuantity
                orderItemsAttribute.terminalId = cartModel.terminalId


                LogUtil.logE(TAG, "TimeStampMo: ${item.timeStamp}")
                if (item.timeStamp == null || item.timeStamp?.lowercase() == "null".lowercase()) {
                    orderItemsAttribute.timestamp = randomOfflineId()
                    LogUtil.logE(TAG, "Timetimestamp  ${orderItemsAttribute.timestamp}")
                } else {
                    orderItemsAttribute.timestamp = item.timeStamp.toString()
                }
                orderItemsAttribute.totalPrice =
                    MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
                LogUtil.logE(TAG, "orderId:  ${cartModel.orderId}")
                orderItemsAttribute.orderItemTaxesAttributes =
                    orderItemTaxesAttributes(item, orderId = cartModel.orderId)

                orderItemsAttribute.orderItemModifiersAttributes =
                    orderItemModifierAttributes(item, cartModel.terminalId)

                orderItemsAttribute.orderItemVariationAttributes =
                    orderItemVariationAttributes(item)



                if (item.variationsAttributes.isNotEmpty()) {
                    orderItemsAttribute.variationId = item.variationsAttributes[0].id
                }

                orderItemsAttributeList.add(orderItemsAttribute)
            }
        }
        return orderItemsAttributeList


    }

    private fun orderItemTaxesAttributes(
        items: TbItem,
        orderId: Int? = null
    ): List<OrderItemTaxesAttribute> {


        val orderItemTaxesAttributeList: ArrayList<OrderItemTaxesAttribute> =
            arrayListOf()
        items.taxes?.forEach { tax ->

            if (tax.isActive) {

                val orderItemTaxesAttribute = OrderItemTaxesAttribute()



                orderItemTaxesAttribute.isDefault = tax.isDefault
                orderItemTaxesAttribute.isTaxRemoved = true
                orderItemTaxesAttribute.name = tax.name.toString()
                orderItemTaxesAttribute.rate = tax.rate

                if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false)) {

                    if (items.orderItemId == null) {

                        orderItemTaxesAttribute.taxId = tax.id
                    } else {
                        tax?.orderTaxId?.let {
                            orderItemTaxesAttribute.taxId = it
                        }
                        orderItemTaxesAttribute.id = tax.id
                        orderItemTaxesAttribute.orderItemId = items.orderItemId
                        orderItemTaxesAttribute.orderId = orderId
                    }
                } else {
                    orderItemTaxesAttribute.taxId = tax.id
                }


                //orderItemTaxesAttribute.orderId = tax.orde

                /*if (isUpdateOrder) {
                    orderItemTaxesAttribute.orderId = orderId
                    orderItemTaxesAttribute.orderItemId = items.orderItemId
                }*/

                if (tax.taxType == "Percentage") {
                    val itemTaxPrice =
                        (tax.rate * ((items.price - items.discountPrice) * items.itemQuantity)) / 100
                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble(itemTaxPrice)
                } else {

                    val ss = tax.rate * items.itemQuantity

                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble((ss))
                }




                orderItemTaxesAttribute.taxType = tax.taxType.toString()
                orderItemTaxesAttributeList.add(orderItemTaxesAttribute)
            }
        }


        return orderItemTaxesAttributeList
    }

    private fun orderItemModifierAttributes(
        item: TbItem,
        terminalId: Int
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> =
            arrayListOf()

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                /*if (isUpdateOrder && it.orderModifierId != null)
                    id = it.orderModifierId
    */
                if (it.orderModifierId != null) {
                    id = it.orderModifierId
                }
                name = it.name
                price = it.price
                order_item_id = item.orderItemId
                modifier_id = it.id
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                it.modifierSetId?.let { modifier_set_id = it }
                quantity = it.itemQuantity
                order_item_taxes_attributes = orderModifierTaxesAttributes(item, it, terminalId)
                modifier_quantity = it.modifier_quantity!!
            }
            orderItemModifierAttributeList.add(orderItemModifierAttribute)
        }

        return orderItemModifierAttributeList
    }

    private fun orderModifierTaxesAttributes(
        items: TbItem,
        modifier: Modifier,
        terminalId: Int
    ): List<OrderModifierTaxesAttribute> {

        val orderItemTaxesAttributeList: ArrayList<OrderModifierTaxesAttribute> =
            arrayListOf()

        modifier.orderItemTaxes.forEach { tax ->
            val orderModifierTaxesAttribute = OrderModifierTaxesAttribute()
            /*  if (isUpdateOrder && tax.orderTaxId != null)
                  orderModifierTaxesAttribute.id = tax.orderTaxId
    */
            if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false)) {
                if (items.isEdited && items.orderItemId == null) {
                    orderModifierTaxesAttribute.tax_id = tax?.taxId
                } else {
                    orderModifierTaxesAttribute.tax_id = tax?.taxId
                    orderModifierTaxesAttribute.id = tax?.id

                    LogUtil.logE(TAG, "IDTax:  ${tax?.id}")
                }
            } else {
                orderModifierTaxesAttribute.tax_id = tax?.id
            }



            orderModifierTaxesAttribute.order_item_modifier_id = modifier.id
            //orderModifierTaxesAttribute.tax_id = tax.id
            orderModifierTaxesAttribute.isDefault = tax?.isDefault == true
            orderModifierTaxesAttribute.is_tax_removed = false
            orderModifierTaxesAttribute.is_modifier = true
            orderModifierTaxesAttribute.category_id = items.categoryId
            orderModifierTaxesAttribute.terminal_id = terminalId
            orderModifierTaxesAttribute.modifier_id = modifier.id!!
            orderModifierTaxesAttribute.timestamp = System.currentTimeMillis().toString()
            orderModifierTaxesAttribute.name = tax?.name.toString()
            if (tax?.rate != null) {
                orderModifierTaxesAttribute.amount = tax?.rate
            }

            /* if (isUpdateOrder) {
                 orderModifierTaxesAttribute.order_id = orderId
                 orderModifierTaxesAttribute.order_item_id = items.orderItemId
             }
    */
            if (tax?.taxType == "Percentage") {
                val itemTaxPrice =
                    (tax?.rate?.times((modifier.price * modifier.itemQuantity)))?.div(100)

                itemTaxPrice?.let {
                    orderModifierTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble(it)
                }
            } else {

                val ss = tax?.rate?.times(modifier.itemQuantity)

                orderModifierTaxesAttribute.taxTotalAmount =
                    MethodUtils.roundOffAmountDouble((ss!!))
            }


            orderItemTaxesAttributeList.add(orderModifierTaxesAttribute)
        }



        return orderItemTaxesAttributeList

    }

    private fun orderItemVariationAttributes(
        item: TbItem
    ): OrderItemVariationAttribute? {

        if (item.variationsAttributes.isNotEmpty()) {

            item.variationsAttributes.forEach {

                val orderItemVariationAttribute = OrderItemVariationAttribute()
                orderItemVariationAttribute.name = it.name.toString()
                orderItemVariationAttribute.price = it.price ?: 0.0
                orderItemVariationAttribute.totalPrice = (it.price ?: 0.0) * item.itemQuantity
                orderItemVariationAttribute.variationId = it.id ?: 0
                orderItemVariationAttribute.quantity = item.itemQuantity

                /*if (isUpdateOrder) {
                    orderItemVariationAttribute.orderId = orderId
                    orderItemVariationAttribute.order_item_id = item.orderItemId
                    orderItemVariationAttribute.id = it.orderVariationId
                }*/

                return orderItemVariationAttribute

            }

        }

        return null
    }

    fun submit(orderRequestModel: OrderRequestModel) {

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<CreateOrderResponse> =

                posRepository.createOrder(orderRequestModel)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let { createOrderResponse ->
                                _Basedata.value = Event(createOrderResponse.data)

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

    fun getTableStatus(tableId: Int, status: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.getTableStatus(
                tableId, prefProvider.getValueInt(
                    Constants.EMPLOYEE_ID, 0
                ), prefProvider.getValueInt(Constants.TERMINAL_ID, 0), status, true
            )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _tableStatusSuccess.value = Event(response.status)

                        } else {
                            _tableStatus.value = response?.let { Event(it.message) }
                        }

                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }

    }

    fun updateOrder(cartModel: CartModel): OrderRequestModel {
        val orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
        var ttotalDiscount = totalDiscount
        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()

            employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            note = ""
            openOrderType = "DineIn"
            orderTypeId = 2
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            totalAmount = MethodUtils.roundOffAmountDouble(totalPrice)
            totalDiscount = MethodUtils.roundOffAmountDouble(ttotalDiscount)
            totalServiceCharges = totalServiceCharge
            totalTaxAmount = totalTax
            orderItemsAttributes = dineInOrderItemAttributed(cartModel)
            paymentAttributes =
                paymentAttributes(
                    cartModel,
                    totalPrice,
                    subTotalPrice,
                    totalServiceCharge,
                    totalTax,
                    totalDiscount, 0.0
                )

            orderServiceChargesAttributes =
                orderServiceChargesAttributes(cartModel, subTotalPrice)

            guestsAttributes = getGuestsAttributes(cartModel)


            /*var listTbItem: ArrayList<TbItem> = arrayListOf()
            for (i in 0 until cartModel.dineInList?.size!!) {
                listTbItem.addAll(cartModel.dineInList!!.get(i).items)
            }
            */


        }


        return OrderRequestModel(false, orderModel)


    }

    val getTaxList = taxServiceChargeRepository.getTaxList()


    fun updateOrderCall(orderId: Int, orderRequestModel: OrderRequestModel) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.updateOrder(orderId, orderRequestModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _updateOrder.value = Event(resource.data?.message)

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


    fun showErrorMessage(errorMessage: String) {
        _snackbarText.value = Event(errorMessage)
    }

    fun restrictedAmount(txtTotalAmount: AppCompatTextView): Boolean {

        val cleanAmount: String =
            txtTotalAmount.text.toString().trim().replace("""[$,]""".toRegex(), "").trim()
        if (cleanAmount.isNotEmpty()) {

            if (cleanAmount.toDouble() >= 1000000) {
                return false
            }
        }

        return true

    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input: InputStream = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            // Log exception
            null
        }
    }

    fun encodeTobase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b: ByteArray = baos.toByteArray()
        val imageEncoded: String = android.util.Base64.encodeToString(b, Base64.DEFAULT)
        Log.d("Image Log:", imageEncoded)
        return imageEncoded
    }

    fun downaloadVenueImage(url: String) {
        val client = OkHttpClient()

        val request: Request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {
                return

            }

            override fun onResponse(response: Response) {
                if (response.isSuccessful) {
                    val bitmap = BitmapFactory.decodeStream(response.body().byteStream())
                    var base64 = ""
                    base64 = encodeTobase64(bitmap) ?: ""

                    if (base64.isNotEmpty()) {
                        prefProvider.setValue(Constants.VENUE_LOGO, base64)

                    }


                }

            }

        })
    }

    fun saveManualSaleData(cartList: List<CartModel>) {

        // prefProvider.setValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
        addCart(cartList[0])
    }

    fun setPosition(position: Int) {
        mPosition = position
        LogUtil.logE(TAG, "mSelectedPosition$mPosition")
    }


    fun createCart(cartList: ArrayList<CartModel>): ArrayList<CartModel> {
        if (cartList.isEmpty()) {
            val model = CartModel()
            model.employeeID =
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            model.terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            model.orderType = prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
            model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            model.serviceCharge = serviceChargesList
            // model.orderTypeId = 1
            ordertypelist.forEach {
                if (it.orderType.lowercase() == Constants.TAKEOUT.lowercase()) {
                    model.orderTypeId = it.id
                }
            }
            cartList.add(0, model)
            LogUtil.logE(TAG, "CartIsEmpty::")
            return cartList
        }

        return cartList
    }
}