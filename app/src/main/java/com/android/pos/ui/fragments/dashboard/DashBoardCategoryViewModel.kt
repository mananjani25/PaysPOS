package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.android.pos.MainApplication
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.OnlineOrderNotificationCount
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.BASE_URL_NEW
import com.android.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.android.pos.data.remote.Constants.BUSINESS_NAME
import com.android.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.android.pos.data.remote.Constants.BUSINESS_WEBSITE
import com.android.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE
import com.android.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_RATE
import com.android.pos.data.remote.Constants.CUSTOMER_SIGN_REQUIRED_ON_CD
import com.android.pos.data.remote.Constants.DEFAULT_ORDER
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.DINEIN_FLOORPLAN_SHOW_TABLENAME
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_LIST_EDIT
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.GIFT_CARD
import com.android.pos.data.remote.Constants.IS_LAST_ITEM_DELETE
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.IS_SYNC_MARKUP
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER
import com.android.pos.data.remote.Constants.LOCK_SCREEN_TRANSACTION
import com.android.pos.data.remote.Constants.LOYALTY_ADDED
import com.android.pos.data.remote.Constants.MANUAL_SALE_CATEGORY_ID
import com.android.pos.data.remote.Constants.MANUAL_SALE_ITEM_ID
import com.android.pos.data.remote.Constants.MAX_ITEM_QUANTITY
import com.android.pos.data.remote.Constants.ONLINE_ORDER_ENABLE
import com.android.pos.data.remote.Constants.ONLY_SHOW_PRICE_GREATER_THAN_ZERO
import com.android.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.PAX_SERIAL_NO
import com.android.pos.data.remote.Constants.PAX_TERMINAL_ID
import com.android.pos.data.remote.Constants.REPORT_END_TIME
import com.android.pos.data.remote.Constants.REPORT_START_TIME
import com.android.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.android.pos.data.remote.Constants.SERVICECHARGE_TAKEOUT_OPENORDER
import com.android.pos.data.remote.Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY
import com.android.pos.data.remote.Constants.SYNC_SETTING_TIME_STAMP
import com.android.pos.data.remote.Constants.SYNC_TIME_STAMP
import com.android.pos.data.remote.Constants.SYSTEM_TIMEZONE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.remote.Constants.VENUE_LOGO
import com.android.pos.data.remote.NetworkConnectionInterceptor
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.utils.*
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.workmanager.ThreadPoolManager
import com.google.gson.Gson
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.set
import kotlin.math.ceil


@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository,
    private val rolePermission: RolePermission,
    private val networkConnectionInterceptor: NetworkConnectionInterceptor
) : ViewModel() {

    fun getRepository(): PosRepository {
        return posRepository
    }

    private var syncMarkeup: Boolean = false
    private var isGuestPay: Boolean = false
    var dineInHeaderPosition: Int = 0
    var dineInSelectedItemHeaderPos: Int = 0
    var selectedItemPositionDine: Int = 0
    val TAG = "DashBoardCateViewModel"
    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var isSelectCount = 1
    var nonCashAdj: Double = 0.0
    var totalServiceCharge = 0.0
    var cashdiscountAmount = 0.0
    var cashDiscountType = ""
    var totalDiscount = 0.0
    var tip = 0.0
    var order_note = ""
    var cartModel: CartModel? = null
    var assignCustomer: TbCustomer? = null
    var orderItemDiscount = 0.0
    var selectedCustomer: TbCustomer? = null
    var activeLoyaltyProgram: LoyaltyProgramsModel? = null
    var redeemLoyaltyInfo: RedeemLoyaltyInfo = RedeemLoyaltyInfo()
    var paymentType: String = "cash"
    var taxDynamicList: ArrayList<TaxData> = arrayListOf()
    var destroyedList: ArrayList<TbItem> = arrayListOf()
    var removeItemDineInList: ArrayList<TbItem> = arrayListOf()
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    var isOrderUpdate: Boolean = false
    val returnedVal = posRepository.getManualCategoryId()
    var viewModelcartList: ArrayList<CartModel> = arrayListOf()
    private var mPosition: Int = 0
    var tipTransactionAmount = 0.0
    private val _updateOrder = MutableLiveData<Event<Any?>>()
    val updateOrder: LiveData<Event<Any?>> = _updateOrder
    private val _itemQuantityCheck = MutableLiveData<Event<Boolean?>>()
    val itemQuantityCheck: LiveData<Event<Boolean?>> = _itemQuantityCheck
    var orderId: Int? = 0

    var openOrderUpdate: Boolean? = false
    private val _removeGuestSuccess = MutableLiveData<Event<String>>()
    val removeGuestSuccess: LiveData<Event<String>> = _removeGuestSuccess


    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    fun setGuestPay(value: Boolean) {
        isGuestPay = value
    }

    fun getIsGuestPay(): Boolean {
        return isGuestPay
    }

    fun venueDataLocal(): LiveData<Resource<List<CategoryWithInventory?>>> {
        return posRepository.venueDataLocal()
    }

    fun setOpenOrderUpdate(value: Boolean) {
        this.openOrderUpdate = value
    }

    fun setOrderId(id: Int) {
        this.orderId = id
    }

    fun setSplitCount(selectcount: Int) {
        this.isSelectCount = selectcount
    }

    fun getSplitCount(): Int {
        return this.isSelectCount
    }

    fun orderTypes(): LiveData<Resource<List<TbOrderType>>> {
        return posRepository.orderTypesDb()
    }

    fun clearListTax() {
        taxDynamicList = arrayListOf()
        taxDynamicList.clear()
    }

    fun setcheckedLoyaltyApply(isapply: Boolean, txtTotalAmount: AppCompatTextView? = null) {
        redeemLoyaltyInfo.needToApplyLoyalty = isapply
        if (txtTotalAmount != null) {
            redeemLoyaltyInfo.getAmountToBePaid()?.let {
                totalPrice = it

                MethodUtils.setPriceTextView(
                    txtTotalAmount, it
                )
            }
        }
        Log.d(TAG, "setcheckedLoyaltyApply: " + redeemLoyaltyInfo.needToApplyLoyalty)
    }

    fun setOrderTypeList(ordertypelist: ArrayList<TbOrderType>) {
        this.ordertypelist = ordertypelist
    }

    fun setTipAmount(tipAmount1: Double) {
        this.tipTransactionAmount = tipAmount1
    }

    fun setCartModel(cartList: List<CartModel>) {
        this.cartModel = generateCombinedItems(cartList[0])
    }


    val serviceCharges = posRepository.serviceChargeList()
    val getOrderTypes = posRepository.getOrderTypes()

    val activeLoyaltyProgramLiveData = posRepository.getActiveLoyaltyProgramFromDb()

    val taxList = posRepository.taxList()

    val discountList = posRepository.disocuntList()

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _showClockOutProgress = MutableLiveData<Event<Boolean>>()
    val showClockOutProgress: LiveData<Event<Boolean>> = _showClockOutProgress


    private val _enableOnlineOrder = MutableLiveData<Event<Boolean>>()
    val enableOnlineOrder: LiveData<Event<Boolean>> = _enableOnlineOrder

    private val _checkCashDrawerPermission = MutableLiveData<Boolean>()
    val checkCashDrawerPer: LiveData<Boolean> = _checkCashDrawerPermission

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _syncDone = MutableLiveData<Event<Boolean?>>()
    val syncDone: LiveData<Event<Boolean?>> = _syncDone

    private val _logout = MutableLiveData<Event<Boolean>>()
    val logout: LiveData<Event<Boolean>> = _logout

    private val _increaseCounter = MutableLiveData<Event<Boolean>>()
    val increaseCounter: LiveData<Event<Boolean>> = _increaseCounter

    private val _onlineOrderCount = MutableLiveData<Event<OnlineOrderNotificationCount.Data>>()
    val onlineOrderCount: LiveData<Event<OnlineOrderNotificationCount.Data>> = _onlineOrderCount

    val _tableStatusSuccess = MutableLiveData<Event<Int>>()
    val tableCheckSuccess: LiveData<Event<Int>> = _tableStatusSuccess

    val _syncInventroyForPriceChange = MutableLiveData<Event<Boolean>>()
    val syncInventroyForPriceChange: LiveData<Event<Boolean>> = _syncInventroyForPriceChange

    val _tableStatus = MutableLiveData<Event<String>>()
    val tableCheck: LiveData<Event<String>> = _tableStatus

    val _callCashDiscount = MutableLiveData<Event<Boolean>>()
    val callCashDiscount: LiveData<Event<Boolean>> = _callCashDiscount

    private val _queueStart = MutableLiveData<Event<CreateOrderResponse?>>()
    val QueueStart: LiveData<Event<CreateOrderResponse?>> = _queueStart

    var onClickAddCustomer = false
    val _Basedata = MutableLiveData<Event<CreateOrderResponse.Data?>>()

    private val _clockOut = MutableLiveData<Event<String>>()
    val clockOut: LiveData<Event<String>> = _clockOut


    var barcodeFoundDbItemLiveData: LiveData<Resource<TbItem>>? = null

    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    fun getItemsbyId(itemId: Int) = posRepository.getItemsbyId(itemId)

    fun getItemByProductCode(productCode: String) = posRepository.getItemByProductCode(productCode)

    fun checkCategoryHideOrNot(id: Int) = posRepository.checkCategoryHideOrNot(id)


    fun getItemByCategoryId(id: Int) = posRepository.getItemByCategoryId(id)


    fun itemsByCat(id: Int): kotlinx.coroutines.flow.Flow<PagingData<TbItem>> = Pager(
        config = PagingConfig(
            pageSize = 40, enablePlaceholders = false, initialLoadSize = 40
        )
    ) {
        appDatabase.itemDao().getItemListByCategory(id)

    }.flow


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

    fun mAllWordsFlow(orderType: String, employee_Id: Int): Flow<List<CartModel>> {


        return posRepository.getCartListFlow(orderType, employee_Id)


    }

    fun manualSale(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getCartList(orderType, employee_Id)

    }

    fun setIsFromAddCustomer(isclickOnAddcustomer: Boolean) {
        this.onClickAddCustomer = isclickOnAddcustomer
    }

    fun manualSaleItems(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getManualSaleItems(orderType, employee_Id)

    }

    var serviceChargesList: ArrayList<TbServiceCharge> = arrayListOf()


    fun addCart(cartModel: CartModel) {
        System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            /*  val millis =   measureTimeMillis {

                 }
                 Log.d(TAG, "addCart: $millis")*/
            //removeItemDineInList.clear()
            posRepository.addItemCart(generateCombinedItems(cartModel))
            destroyedList.clear()
        }

    }

    fun addOrderNote(note: String) {
        if (cartModel != null) {
            cartModel!!.note = note
            addCart(cartModel!!)

        }
    }

    fun createEmptyCart(model: CartModel) {
        viewModelScope.launch {
            posRepository.createEmptyCart(model)
        }


    }

    fun generateCombinedItems(cartModel: CartModel): CartModel {
        val combinedItems = arrayListOf<TbItem>()
        cartModel.items?.let {

            if (isOrderUpdate) {
                it.forEach {
                    if (it.orderItemId != null && it.isDestroy == true) {
                        combinedItems.add(it)
                    }
                }
            } else {
                combinedItems.addAll(it)
            }
        }
        if (cartModel.isOpenOrder && isOrderUpdate) {
            combinedItems.addAll(destroyedList)
        }
        cartModel.items = combinedItems
        return cartModel
    }

    fun addDineInRemovedItems(cartModel: CartModel): CartModel {
        var destroyedItems: ArrayList<TbItem> = arrayListOf()
        cartModel.items?.forEach {

            destroyedItems.add(it)

        }
        Log.e(TAG, "checkTotalDITems:  ${destroyedItems.size}")
        cartModel.items = destroyedItems

        /*  val items = arrayListOf<TbItem>()
          cartModel.items.let { it?.let { it1 -> items.addAll(it1) } }
          items.addAll(removeItemDineInList)
          cartModel.items = items
          removeItemDineInList.clear()
          removeItemDineInList = arrayListOf()*/
        return cartModel
    }

    fun deleteCart() {
        try {
            prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
            cartModel = null
            GlobalScope.launch {
                posRepository.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
                destroyedList.clear()
            }
        } catch (e: Exception) {
            Log.d("deleteCart", "Preference is null")
        }
    }


    fun deleteManualSaleCart() {
        viewModelScope.launch {
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            order_note = ""
            posRepository.deleteManualSaleCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))

        }
    }


    fun dineInCartUpdate(
        cartList: List<CartModel>?, dineInList: List<DineInModel>
    ) {

        val cartModel = cartList?.get(0)
        cartModel?.dineInList = dineInList
        cartModel?.orderType = DINE_IN
        cartModel?.let {
            addCart(it)


        }

    }

    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }


    fun manualSalecartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {

        if (cartList != null && cartList.isEmpty()) {
            var model = addCartModel(item, true)
            if (type == UPDATE) {
                cartModel?.items?.forEach { items ->
                    model = taxBifurcationCalculation(items, model, type, false)
                }
            } else {
                model = taxBifurcationCalculation(item, model, type, false)
            }
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()

            if (list != null && list.isNotEmpty()) {


                if (type == ADD) {
                    list.add(item)
                } else if (type == UPDATE) {

                    order_note = cartList[0].note
                    if (mPosition != -1) {
                        val model = cartList[0].items?.get(mPosition)

                        if (model != null) {
                            model.itemQuantity = item.itemQuantity
                            model.note = item.note
                            model.discountPrice = item.discountPrice
                            model.discountType = item.discountType
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

                var cartModel = cartList[0]
                if (type == UPDATE) {
                    cartModel.items?.forEach { itemData ->
                        cartModel = taxBifurcationCalculation(
                            itemData, cartModel, type, false
                        )
                    }
                } else {
                    cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                }
                cartModel.items = list
                addCart(cartModel)

                if (list.isEmpty()) {
                    deleteManualSaleCart()
                }
            } else {

                if (type == DELETE) {
                    deleteManualSaleCart()
                } else {
                    var cartModel = cartList?.get(0)
                    cartModel = taxBifurcationCalculation(item!!, cartModel!!, type, false)
                    cartModel?.items = listOf(item)
                    if (cartModel != null) {

                        addCart(cartModel!!)
                    }
                }

            }
        }

    }


    fun addGuestFromDashBoard(cartList: List<CartModel>?) {
        addCart(cartList!![0])
    }


    fun newCartLogicModifier(
        cartList: List<CartModel>?,
        item: TbItem?,
        type: String,
        isManualSales: Boolean,
        dineInList: List<DineInModel> = arrayListOf(),
        isFromDineInScreen: Boolean = false,
        position: Int = -1
    ) {
        prefProvider.setValueboolean(
            IS_LAST_ITEM_DELETE, false
        )  // reset flag in case of adding or updating item
        Log.e("DashViewModModel", "checkCartSize: ${cartList?.size}")
        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare

            Log.e("DashViewModModel", "checkItem:  ${Gson().toJson(item?.modifiers)}")

            var cartModel = item?.let { addCartModel(it, isManualSales) }
            if (item != null) {
                if (type == UPDATE) {
                    cartModel?.items?.forEach { items ->
                        cartModel = taxBifurcationCalculation(items, cartModel!!, type, false)
                    }
                } else {
                    cartModel = taxBifurcationCalculation(item, cartModel!!, type, false)
                }
            } else if (dineInList.isNotEmpty()) {
                dineInList.forEach { dineInModel ->
                    dineInModel.items.forEach { itemData ->
                        cartModel = taxBifurcationCalculation(itemData, cartModel!!, type, false)
                    }

                }
            }
            // commented addCart to fix BIS-840
            /*if (cartModel != null) {
                addCart(cartModel!!)
            }*/


        } else {
            if (cartList?.get(0)?.orderType == DINE_IN) {
                var isDineInItem1000 = false
                var cartModel = cartList[0]
                order_note = cartList[0].note


                cartModel.dineInList = dineInList
                var dinein = dineInList
                if (dinein.isNotEmpty() && dinein != null) {
                    val selectedHeader = dineInList.get(0).selectedPosition
                    if (dineInList[selectedHeader].items.isNotEmpty() || dineInList[selectedHeader].items != null) {
                        if (type == ADD) {
                            var index = -1
                            var list = dineInList[selectedHeader].items
                            for (i in list.indices) {
                                if (item != null) {
                                    if (item.isManualSales) {


                                        if (list[i].manualSaleId == item.manualSaleId) {
                                            Log.d(TAG, "cartLogic: " + i)
                                            index = i
                                            break
                                        }

                                    } else {
                                        Log.e("AddedInElse", "GotMod")

                                        if (list[i].itemId == item.itemId && list[i].itemQuantity > 1000) {
                                            isDineInItem1000 = true
                                            _itemQuantityCheck.value = Event(true)
                                            break
                                        } else {
                                            if (list[i].itemId == item.itemId && checkVariation(
                                                    list[i], item
                                                ) && checkModifierNewLogic(list[i], item)
                                            ) {
                                                Log.d(TAG, "cartLogic: " + i)

                                                var listTmp = combineItem(
                                                    list.toCollection(arrayListOf()), item, i
                                                )
                                                list.clear()
                                                Log.e(
                                                    TAG,
                                                    "newCartLogicModifier: position of selected Item " + item.id
                                                )
                                                list.addAll(listTmp.toMutableList())

                                                Log.e(TAG, "getMergeCombineItem  ${list.size}")

                                                index = -2
                                                break


                                            } else if (list[i].itemId == item.itemId && (!checkVariation(
                                                    list[i], item
                                                ) && !checkModifierNewLogic(list[i], item))
                                            ) {
                                                var isBreak: Boolean = false




                                                list[i].modifiers.forEach { modifier ->
                                                    item.modifiers.forEach { mod ->
                                                        if (mod.id == modifier.id && mod.modifier_quantity == modifier.modifier_quantity) {
                                                            if (list[i].variationsAttributes.isNotEmpty() && list[i].variationsAttributes[0].id == item.variationsAttributes[0].id) {
                                                                item.id += 1
                                                                isBreak = true
                                                                Log.e(
                                                                    TAG,
                                                                    "newCartLogicModifier: isBreak"
                                                                )

                                                                return@forEach
                                                            } /*else if (list[i].variationsAttributes.isEmpty() == true) {
                                                        item.id += 1
                                                        isBreak = true
                                                        Log.d(TAG, "newCartLogicModifier: isBreak")

                                                        return@forEach
                                                    }*/
//

                                                        } else if (mod.id == modifier.id && mod.modifier_quantity != modifier.modifier_quantity) {
                                                            item.id += 1
                                                            isBreak = false
                                                            Log.e(
                                                                TAG, "newCartLogicModifier: isBreak"
                                                            )

                                                            return@forEach
                                                        }

                                                    }



                                                    if (isBreak) {
                                                        return@forEach
                                                    }
                                                }


//
                                                if (list[i].variationsAttributes.isNotEmpty() == true) {
                                                    if (list[i].variationsAttributes.get(0).id != item.variationsAttributes.get(
                                                            0
                                                        ).id
                                                    ) {
                                                        item.id += 1

                                                    }


                                                }

                                                if (isBreak) {
                                                    index = i
                                                    break
                                                }


                                            }
                                        }

                                    }

                                }
                            }

                            if (!isDineInItem1000) {
                                list.forEach {
                                    if (it.id == item?.id && it.itemId == item.itemId) {
                                        item.id += 1
                                    }
                                }

                                if (isFromDineInScreen) {
                                    list.forEach { it1 ->
                                        var listd = list.filter { it.itemId == it1.itemId }
                                        if (listd.size > 1) {
                                            it1.customItemID =
                                                kotlin.random.Random.nextInt(10, 10000)

                                        }


                                    }


                                }

                                if (index != -1 && index != -2) {
                                    val model = cartList[0].items?.get(index)
                                    Log.d(TAG, "cartLogic: " + index)
                                    if (model != null) {
                                        if (index != -1) {
                                            if (item != null) {
                                                model.name = item.name
                                                model.itemQuantity =
                                                    model.itemQuantity + item.itemQuantity
                                                model.price = item.price
                                                model.variationsAttributes =
                                                    item.variationsAttributes
                                                item.modifiers.forEach {
                                                    it.itemQuantity =
                                                        item.itemQuantity * it.modifier_quantity/*model.modifiers.forEach { tbmodfier ->
                                                        if (it.id == tbmodfier.id) {
                                                            it.modifier_quantity =
                                                                it.itemQuantity / item.itemQuantity
                                                            it.itemQuantity =
                                                                it.itemQuantity + tbmodfier.itemQuantity
                                                        }
                                                    }*/

                                                }


                                                model.modifiers = item.modifiers
                                                if (item.isEdited) {
                                                    model.isEdited = item.isEdited
                                                }

                                                if (prefProvider.getValueboolean(
                                                        IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                    )
                                                ) {
                                                    model.isEdited = true
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
                                                if (prefProvider.getValueboolean(
                                                        IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                    )
                                                ) {
                                                    model.isEdited = true
                                                }
                                                itemDiscountApply(model, item)
                                                model.isDestroy = false
                                            }
                                            list[index] = model
                                        }

                                    }
                                } else if (index != -2) {
                                    Log.d(TAG, "cartLogic: " + index)
                                    if (item != null) {
                                        item.singleItemPrice = item.price
                                        item.modifiers.forEach { it ->
                                            it.modifier_quantity = it.itemQuantity
                                            item.singleItemPrice += it.price * it.itemQuantity
                                        }
                                        item.isDestroy = false
                                        if (prefProvider.getValueboolean(
                                                IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                            )
                                        ) {
                                            item.isEdited = true
                                        } else {
                                            item.isEdited = item.isEdited
                                        }

                                        Log.e("checkISItemEdit", "isEdited  ${item.isEdited}")
                                        list.add(item)
                                    }
                                } else if (prefProvider.getValue(
                                        ORDER_TYPE, TAKEOUT
                                    ) == DINE_IN && prefProvider.getValueboolean(
                                        DINE_IN_UPDATE, false
                                    ) == true
                                ) {
                                    Log.e(
                                        TAG,
                                        "checkInsideSc  ${item?.isDestroy}  updateOrder ${isOrderUpdate}"
                                    )

                                    var flagD: Boolean = false
                                    list.forEach {
                                        if (item?.itemId == it.itemId && it.isDestroy == true) {

                                            flagD = true

                                        }

                                    }

                                    if (flagD) {
                                        item?.let { list.add(it) }
                                    }

                                }
                                Log.e("GEtDineInData", "getList  ${Gson().toJson(list)}")
                                Log.e("GEtDineInData", "destroyTerer  ${item?.isDestroy}")
                                Log.e(
                                    TAG, "checkDinein  ${
                                        prefProvider.getValueboolean(
                                            DINE_IN_UPDATE, false
                                        )
                                    }"
                                )
                                cartList[0].items = list
                            }

                        } else if (type == UPDATE) {
                            var index = -1
                            var list = dineInList[selectedHeader].items
                            val selectedHeader = dineInList.get(0).selectedPosition



                            for (i in list.indices) {
                                if (item != null) {
                                    if (item.isManualSales) {


                                        if (list[i].manualSaleId == item.manualSaleId) {
                                            Log.d(TAG, "cartLogic: " + i)
                                            index = i
                                            break
                                        }

                                    } else {
                                        if (list[i].id == item.id && list[i].itemId == item.itemId && list[i].timeStamp == item.timeStamp) {
                                            Log.d(TAG, "cartLogic: " + i)
                                            index = i
                                            break
                                        } else if (list[i].itemId == item.itemId && (item.modifiers.isNotEmpty() || item.variationsAttributes.isNotEmpty()) && checkVariation(
                                                list[i], item
                                            ) && checkModifierNewLogic(list[i], item)
                                        ) {
                                            var listTmp = combineItem(
                                                list.toCollection(arrayListOf()), item, i
                                            )
                                            list.clear()
                                            Log.e(
                                                TAG,
                                                "newCartLogicModifier: position of selected Item " + item.id
                                            )
                                            var indexJ = -1

                                            for (j in 0 until listTmp.size) {
                                                if (listTmp[j].id == item.id) {
                                                    indexJ = j
                                                    break
                                                }

                                            }
                                            var ttempllist = ArrayList(listTmp).apply {
                                                if (indexJ != -1) {
                                                    Log.e(TAG, "GETIndexJ  ${indexJ}")
                                                    removeAt(indexJ)
                                                }
                                            }
                                            list.addAll(ttempllist.toMutableList())
                                            index = -2
                                            break
                                        }

                                    }

                                }
                            }
                            if (index == -2) {
                                Log.e(TAG, "Itis NotMinus  ")

                            } else if (index != -1) {
                                val model = list.get(index)
                                Log.d(TAG, "cartLogic: " + index)
                                if (model != null) {
                                    if (item != null) {
                                        model.name = item.name
                                        model.price = item.price
                                        Log.e(
                                            "CheckDineinBug",
                                            "variationsAttributesSize  ${item.variationsAttributes.size}"
                                        )
                                        model.variationsAttributes = item.variationsAttributes

                                        if (item.isEdited) {
                                            model.isEdited = item.isEdited
//                                            model.guestItemId = item.guestItemId
                                        }
                                        if (prefProvider.getValueboolean(
                                                DINE_IN_UPDATE, false
                                            )
                                        ) {
                                            model.isEdited = true
                                        }
                                        item.singleItemPrice = item.price
                                        item.modifiers.forEach {
                                            it.itemQuantity =
                                                (it.modifier_quantity * item.itemQuantity)
                                            it.isChecked = item.isChecked/* model.modifiers.forEach { tbmodifier ->
                                                 if (it.id == tbmodifier.id) {
                                                     if (it.modifier_quantity != tbmodifier.modifier_quantity) {
                                                         tbmodifier.modifier_quantity =
                                                             it.modifier_quantity
                                                     } else {
                                                         it.modifier_quantity =
                                                             it.itemQuantity / model.itemQuantity
                                                     }

                                                     it.itemQuantity =
                                                         (it.modifier_quantity * item.itemQuantity)
                                                     it.isChecked = item.isChecked
                                                 }
                                             }*/
                                            item.singleItemPrice += it.price * it.modifier_quantity
                                        }
                                        Log.d(TAG, "newCartLogicModifier: " + item.singleItemPrice)
                                        model.itemQuantity = item.itemQuantity
                                        model.modifiers = item.modifiers
                                        model.isDestroy = false
                                        item.orderItemId?.let {
                                            model.orderItemId = it
                                        }
                                        model.taxes = item.taxes
                                        model.modifier_set_ids = item.modifier_set_ids
                                        model.itemId = item.itemId
                                        model.guestItemId = item.guestItemId
                                        model.timeStamp = item.timeStamp
                                        itemDiscountApply(model, item)

                                    }
                                    Log.d(TAG, "cartLogic:itemname " + model.name)
                                    list.set(index, model)
//                                    list[index] = model

                                }
                            }
                        } else if (type == DELETE) {
                            var list: ArrayList<TbItem> = arrayListOf()
                            if (item != null) {
                                list = dineInList[item.headerPositionDinein].items
                            } else {
                                list = dineInList[dineInList.get(0).selectedPosition].items
                            }
                            var index = -1
                            Log.e(TAG, "checkReOrder  ${cartModel?.reorder}")

                            for (i in list.indices) {
                                if (item != null) {
                                    if (!item.isManualSales) {
                                        if (cartModel?.reorder == false && list[i].itemId == item.itemId && item.modifiers.isEmpty() && checkVariation(
                                                list[i], item
                                            ) && list[i].id == item.id
                                        ) {
                                            index = i
                                            Log.d(TAG, "newCartLogicModifier normal item: ${i}")
                                            break
                                        } else if (list[i].itemId == item.itemId && cartModel?.reorder == false && list[i].id == item.id && checkVariation(
                                                list[i], item
                                            ) && checkModifierNewLogic(list[i], item)
                                        ) {
                                            Log.e(TAG, "CheckedBefore")
                                            index = i
                                            break
                                        } else if (cartModel?.reorder == true) {
                                            if (list[i].orderItemId == item.orderItemId) {
                                                index = i
                                                Log.e(TAG, "indexReorder:  ${index}")
                                                break
                                            }

                                        } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && (!checkVariation(
                                                list[i], item
                                            ) || !checkModifierNewLogic(list[i], item))
                                        ) {
                                            if (list[i].id == item.id) {
                                                Log.e(TAG, "CheckedBefore   ${i}")
                                                index = i
                                                break
                                            }
                                        }

                                    } else {
                                        if (list[i].manualSaleId == item.manualSaleId) {
                                            index = i
                                            break
                                        } else if (cartModel.reorder == true) {
                                            if (list[i].orderItemId == item.orderItemId) {
                                                index = i
                                                Log.e(TAG, "indexReorder23:  ${index}")
                                                break
                                            }

                                        }
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
                                        list.remove(model)
                                    }
                                }
                            } else {
                                //list.remove(item)

                            }

                            cartModel.items = list
                            Log.e(
                                "ModNewLogic",
                                "dineInList:   ${Gson().toJson(cartModel.dineInList)}"
                            )
                            Log.e("ModNewLogic", "checkItems:  ${Gson().toJson(cartModel.items)}")

                            //cartModel.dineInList = dinein


                        }
                    }
                    var cartModel = cartList[0]
                    if (type == UPDATE) {
                        Log.e("CheckSelectedHeaderPos", "CheckPOS ${selectedHeader}")
                        var list = dineInList.get(selectedHeader)?.items
                        dineInList?.forEach { itemData ->
                            itemData.items.forEach {
                                cartModel = taxBifurcationCalculation(
                                    it, cartModel, type, false
                                )
                            }
                        }

                        Log.e(
                            TAG, "taxlistDynamicData:  ${Gson().toJson(cartModel.taxlistDynamic)}"
                        )
                        cartModel.items = list
                    } else {
                        if (item != null) {
                            cartModel = taxBifurcationCalculation(item, cartModel, type, false)
                        }
                    }
                    addCart(cartModel)
                } else {

                    if (type == DELETE) {
                        deleteCart()
                    } else {

                        var cartModel = cartList.get(0)
                        cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                        if (item != null) {
                            if (item.modifiers.isNotEmpty()) {
                                item.modifiers.forEach { mod ->
                                    mod.modifier_quantity = mod.itemQuantity
                                }
                            }
                            cartModel.dineInList?.get(dineInSelectedItemHeaderPos)?.items?.add(item)
                        }

                        if (cartModel != null) {
                            addCart(cartModel!!)
                        }
                    }


                }

            } else {
                var isItem1000 = false
                val list = cartList?.get(0)?.items?.toMutableList()
                if (list != null && list.isNotEmpty()) {

                    if (type == ADD) {
                        var index = -1
                        for (i in list.indices) {
                            if (item != null) {

                                if (item.isManualSales) {


                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        Log.d(TAG, "cartLogic: " + i)
                                        index = i
                                        break
                                    }

                                } else {

                                    Log.e("AddedInElse", "Item Qty = ${list[i].itemQuantity}")
                                    if (list[i].itemId == item.itemId && list[i].itemQuantity > 1000) {
                                        isItem1000 = true
                                        _itemQuantityCheck.value = Event(true)
                                        break
                                    } else {
                                        if (list[i].itemId == item.itemId && (!checkVariation(
                                                list[i], item
                                            ) && !checkModifierNewLogic(list[i], item))
                                        ) {
                                            var isBreak: Boolean = false
                                            Log.e(
                                                "checkORderUpdate", "isOrderUpdate:  ${
                                                    prefProvider.getValueboolean(
                                                        IS_UPDATE_ORDER, false
                                                    )
                                                }"
                                            )
                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER, false
                                                )
                                            ) {
                                                try {
                                                    item.isEdited = true
                                                    list[i].isEdited = true
                                                } catch (e: java.lang.Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            list[i].modifiers.forEach { modifier ->
                                                item.modifiers.forEach { mod ->
                                                    if (mod.id == modifier.id && mod.modifier_quantity == modifier.modifier_quantity) {
                                                        if (list[i].variationsAttributes.isNotEmpty() && list[i].variationsAttributes[0].id == item.variationsAttributes[0].id) {
                                                            item.id += 1
                                                            isBreak = true
                                                            Log.d(
                                                                TAG, "newCartLogicModifier: isBreak"
                                                            )

                                                            return@forEach
                                                        } /*else if (list[i].variationsAttributes.isEmpty() == true) {
                                                        item.id += 1
                                                        isBreak = true
                                                        Log.d(TAG, "newCartLogicModifier: isBreak")

                                                        return@forEach
                                                    }*/
//

                                                    } else if (mod.id == modifier.id && mod.modifier_quantity != modifier.modifier_quantity) {
                                                        item.id += 1
                                                        isBreak = false
                                                        Log.d(TAG, "newCartLogicModifier: isBreak")

                                                        return@forEach
                                                    }

                                                }



                                                if (isBreak) {
                                                    return@forEach
                                                }
                                            }


//
                                            if (list[i].variationsAttributes.isNotEmpty() == true) {
                                                if (list[i].variationsAttributes.get(0).id != item.variationsAttributes.get(
                                                        0
                                                    ).id
                                                ) {
                                                    item.id += 1

                                                }


                                            }

                                            if (isBreak) {
                                                index = i
                                                break
                                            }


                                        } else if (list[i].itemId == item.itemId && checkVariation(
                                                list[i], item
                                            ) && checkModifierNewLogic(list[i], item)
                                        ) {
                                            Log.d(TAG, "cartLogic: " + i)

                                            Log.e(
                                                "checkORderUpdate", "isOrderUpdate:  ${
                                                    prefProvider.getValueboolean(
                                                        IS_UPDATE_ORDER, false
                                                    )
                                                }"
                                            )

                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER, false
                                                )
                                            ) {
                                                try {
                                                    item.isEdited = true
                                                    list[i].isEdited = true
                                                } catch (e: java.lang.Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            var listTmp = combineItem(
                                                list.toCollection(arrayListOf()), item, i
                                            )
                                            list.clear()
                                            Log.d(
                                                TAG,
                                                "newCartLogicModifier: position of selected Item " + item.id
                                            )
                                            list.addAll(listTmp.toMutableList())

                                            Log.e(TAG, "getMergeCombineItem  ${list.size}")

                                            index = -2
                                            break


                                        }
                                    }


                                }

                            }
                        }

                        if (!isItem1000) {
                            list.forEach {
                                if (it.id == item?.id && it.itemId == item.itemId) {
                                    item.id += 1
                                }
                            }



                            if (index != -1 && index != -2) {
                                val model = cartList[0].items?.get(index)
                                Log.e("DashViewModModel", "getIndexFirst  ${index}")
                                if (model != null) {
                                    if (index != -1) {
                                        if (item != null) {
                                            model.name = item.name
                                            model.itemQuantity =
                                                model.itemQuantity + item.itemQuantity
                                            model.price = item.price
                                            model.variationsAttributes = item.variationsAttributes
                                            item.modifiers.forEach {
                                                it.itemQuantity =
                                                    (it.modifier_quantity * item.itemQuantity)/*  model.modifiers.forEach { tbmodfier ->
                                                      if (it.id == tbmodfier.id) {
                                                          it.modifier_quantity =
                                                              it.itemQuantity / item.itemQuantity
                                                          it.itemQuantity =
                                                              it.itemQuantity + tbmodfier.itemQuantity
                                                      }
                                                  }*/

                                            }


                                            model.modifiers = item.modifiers
                                            if (item.isEdited) {
                                                model.isEdited = item.isEdited
                                            }

                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                )
                                            ) {
                                                model.isEdited = true
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
                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                )
                                            ) {
                                                model.isEdited = true
                                            }
                                            itemDiscountApply(model, item)
                                            model.isDestroy = false
                                        }
                                        list[index] = model
                                    }

                                }
                            } else if (index != -2) {
                                Log.e("DashViewModModel", "getIndexSecond  ${index}")
                                if (item != null) {
                                    item.singleItemPrice = item.price

                                    item.modifiers.forEach { it ->
                                        Log.e(TAG, "moditemQuantity  ${it.itemQuantity}")
                                        Log.e(TAG, "itemQuantity  ${item.itemQuantity}")

                                        /*  if (type == ADD){
                                              it.modifier_quantity = it.itemQuantity / item.itemQuantity

                                                  it.itemQuantity = item.itemQuantity

                                          }
                                          else{*/
                                        it.itemQuantity = it.modifier_quantity * item.itemQuantity

                                        /*}*/

                                        item.singleItemPrice += it.price * it.itemQuantity
                                    }
                                    item.isDestroy = false
                                    if (prefProvider.getValueboolean(
                                            IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                        )
                                    ) {
                                        item.isEdited = true
                                    }
                                    list.add(item)
                                }
                            } else {
                                //For BIS-3219 issue
                                //This is for adding the item after deleting it after coming from all orders update order click
                                var newItem: TbItem? = null
                                var indexToRemove = -1
                                list.forEachIndexed { index, tbItem ->
                                    if (tbItem.isDestroy && tbItem.itemId == item?.itemId) {
                                        newItem = tbItem
                                        indexToRemove = index
                                    }
                                }

                                if (indexToRemove != -1) {
                                    list.removeAt(indexToRemove)
                                }

                                if (newItem != null) {
                                    newItem?.itemQuantity = 1
                                    newItem?.isDestroy = false
                                    list.add(newItem!!)
                                }

                                Log.e("DashViewModModel", "getIndexThird  ${index}")

                            }
                        }
                    } else if (type == UPDATE) {
                        var index = -1

                        var idsF = list.filter { it.itemId == item?.itemId }
                        Log.e("CheckUpdate", "checkIdsfSize  ${idsF.size}")

                        for (i in list.indices) {
                            if (item != null) {
                                if (item.isManualSales) {


                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        Log.d(TAG, "cartLogic: " + i)
                                        index = i
                                        break
                                    }

                                } else {


                                    if (idsF.size > 1 && list[i].itemId == item.itemId && checkVariation(
                                            list[i], item
                                        ) && checkModifierNewLogic(
                                            list[i], item
                                        ) && item.id != list[i].id
                                    ) {


                                        var listTmp =
                                            combineItem(list.toCollection(arrayListOf()), item, i)
                                        list.clear()
                                        Log.e(
                                            "CheckUpdate",
                                            "newCartLogicModifier: position of selected Item " + item.id
                                        )/*var indexJ = -1

                                        for (j in 0 until listTmp.size) {
                                            if (listTmp[j].id == item.id
                                            ) {
                                                indexJ = j
                                                break
                                            }

                                        }*/
                                        var ttempllist = ArrayList(listTmp).apply {
                                            if (position != -1) {
                                                Log.e("CheckUpdate", "GETIndexJ  ${position}")
                                                this[position].isDestroy = true
//                                                    removeAt(indexJ)
                                            }
                                        }
                                        list.addAll(ttempllist.toMutableList())
                                        index = -2
                                        break
                                    } else if (list[i].id == item.id && list[i].itemId == item.itemId) {

                                        var isBreakInside = false


                                        if (idsF.size > 1) {

                                            for (k in i until list.size) {
                                                if (list[k].id != item.id && list[k].itemId == item.itemId && checkVariation(
                                                        list[k], item
                                                    ) && checkModifierNewLogic(list[k], item)
                                                ) {

                                                    var listTmp = combineItem(
                                                        list.toCollection(arrayListOf()), item, k
                                                    )
                                                    list.clear()
                                                    Log.e(
                                                        "CheckUpdate",
                                                        "newCartLogicModifier: position of selected Item " + item.id
                                                    )/*var indexJ = -1

                                                    for (j in 0 until listTmp.size) {
                                                        if (listTmp[j].id == item.id
                                                        ) {
                                                            indexJ = j
                                                            break
                                                        }

                                                    }*/
                                                    var ttempllist = ArrayList(listTmp).apply {
                                                        if (position != -1) {
                                                            Log.e(
                                                                "CheckUpdate",
                                                                "GETIndexJ  ${position}"
                                                            )
                                                            this[position].isDestroy = true
//                                                                removeAt(indexJ)
                                                        }
                                                    }
                                                    list.addAll(ttempllist.toMutableList())
                                                    index = -2
                                                    isBreakInside = true
                                                    break

                                                }

                                            }
                                        }
                                        if (isBreakInside) {
                                            break
                                        }

                                        if (isBreakInside == false) {
                                            Log.e("CheckUpdate", "cartLogicFInd: " + i)
                                            index = i
                                            break
                                        }
                                    }

                                }

                            }
                        }
                        if (index == -2) {

                        } else if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            Log.e("CheckUpdate", "cartLogic: " + index)
                            if (model != null) {
                                if (item != null) {
                                    model.name = item.name
                                    model.price = item.price
                                    model.variationsAttributes = item.variationsAttributes
                                    if (item.isEdited) {
                                        model.isEdited = item.isEdited
                                    }
                                    if (prefProvider.getValueboolean(
                                            IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                        )
                                    ) {
                                        model.isEdited = true
                                    }
                                    item.singleItemPrice = item.price
                                    item.modifiers.forEach {
                                        it.itemQuantity = (it.modifier_quantity * item.itemQuantity)/*model.modifiers.forEach { tbmodifier ->
                                            if (it.id == tbmodifier.id) {
                                                if (it.modifier_quantity != tbmodifier.modifier_quantity) {
                                                    tbmodifier.modifier_quantity =
                                                        it.modifier_quantity
                                                } else {
                                                    it.modifier_quantity =
                                                        it.itemQuantity / model.itemQuantity
                                                }

                                                it.itemQuantity =
                                                    (it.modifier_quantity * item.itemQuantity)
                                            }
                                        }*/
                                        item.singleItemPrice += it.price * it.modifier_quantity
                                    }
                                    Log.d(TAG, "newCartLogicModifier: " + item.singleItemPrice)
                                    model.itemQuantity = item.itemQuantity
                                    model.modifiers = item.modifiers
                                    model.isDestroy = false
                                    itemDiscountApply(model, item)

                                }
                                Log.d(TAG, "cartLogic:itemname " + model.name)
                                list[index] = model

                            }
                        }
                    } else if (type == DELETE) {

                        var index = -1
                        Log.e(TAG, "CheckDeleteItem ${Gson().toJson(item)}")
                        Log.e(TAG, "getListedItems  ${Gson().toJson(list[0])}")
                        Log.e(TAG, "checkReOrder  ${cartModel?.reorder}")

                        for (i in list.indices) {
                            if (item != null) {
                                if (!item.isManualSales) {
                                    if (cartModel?.reorder == false && list[i].itemId == item.itemId && item.modifiers.isEmpty() && checkVariation(
                                            list[i], item
                                        ) && item.id == list[i].id
                                    ) {
                                        index = i
                                        Log.d(TAG, "newCartLogicModifier normal item: ${i}")
                                        break
                                    } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && list[i].id == item.id && checkVariation(
                                            list[i], item
                                        ) && checkModifierNewLogic(list[i], item)
                                    ) {
                                        Log.e(TAG, "CheckedBefore")
                                        index = i
                                        break
                                    } else if (cartModel?.reorder == true) {
                                        if (list[i].orderItemId == item.orderItemId) {
                                            index = i
                                            Log.e(TAG, "indexReorder:  ${index}")
                                            break
                                        }

                                    } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && (!checkVariation(
                                            list[i], item
                                        ) || !checkModifierNewLogic(list[i], item))
                                    ) {
                                        if (list[i].id == item.id) {
                                            Log.e(TAG, "CheckedBefore   ${i}")
                                            index = i
                                            break
                                        }
                                    }

                                } else {
                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        index = i
                                        break
                                    } else if (cartModel?.reorder == true) {
                                        if (list[i].orderItemId == item.orderItemId) {
                                            index = i
                                            Log.e(TAG, "indexReorder23:  ${index}")
                                            break
                                        }

                                    }
                                }
                            }

                        }

                        if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            if (model != null) {
                                //delete from cart
                                if (item?.isEdited == true) {
                                    model.isEdited = item.isEdited
                                    model.isDestroy = true
                                } else {
                                    if (list.size == 1) {
                                        prefProvider.setValueboolean(IS_LAST_ITEM_DELETE, true)
                                    }
                                    list.remove(model)
                                }
                            }
                        } else {
                            //list.remove(item)
                        }
                    }

                    var cartModel = cartList[0]
                    if (type == UPDATE) {
                        cartModel.items?.forEach { itemData ->
                            cartModel = taxBifurcationCalculation(
                                itemData, cartModel, type, false
                            )
                        }
                    } else {
                        if (item != null) {
                            cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                        }
                    }
                    cartModel.items = list
                    addCart(cartModel)/*if (list.isEmpty()) {
                        // delete carts
                        deleteCart()
                    }*/
                } else {

                    if (type == DELETE) {
                        deleteCart()
                    } else {

                        var cartModel = cartList?.get(0)
                        cartModel = taxBifurcationCalculation(item!!, cartModel!!, type, false)
                        if (item != null) {
                            if (item.modifiers.isNotEmpty()) {
                                item.modifiers.forEach { mod ->
//                                    if (item.itemQuantity > mod.itemQuantity) {
//                                        mod.modifier_quantity = mod.itemQuantity
                                    mod.itemQuantity = item.itemQuantity * mod.modifier_quantity
//                                    } else {
//                                        mod.modifier_quantity = mod.itemQuantity / item.itemQuantity
//                                    }

                                }
                            }
                            cartModel?.items = listOf(item)
                        }

                        if (cartModel != null) {
                            addCart(cartModel!!)
                        }
                    }


                }
            }

        }
    }


    fun cartLogic(
        cartList: List<CartModel>?,
        item: TbItem?,
        type: String,
        isManualSales: Boolean,
        dineInList: List<DineInModel> = arrayListOf()
    ) {

        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare
            var cartModel = item?.let { addCartModel(it, isManualSales) }
            if (item != null) {
                if (type == UPDATE) {
                    cartModel?.items?.forEach { items ->
                        cartModel = taxBifurcationCalculation(items, cartModel!!, type, false)
                    }
                } else {
                    cartModel = taxBifurcationCalculation(item, cartModel!!, type, false)
                }
            } else if (dineInList.isNotEmpty()) {
                dineInList.forEach { dineInModel ->
                    dineInModel.items.forEach { itemData ->
                        cartModel = taxBifurcationCalculation(itemData, cartModel!!, type, false)
                    }

                }
            }
            if (cartModel != null) {
                addCart(cartModel!!)
            }


        } else {
            if (cartList?.get(0)?.orderType == DINE_IN) {
                var cartModel = cartList[0]
                order_note = cartList[0].note
                cartModel.dineInList = dineInList
                if (type == ADD || type == UPDATE) {
                    var index = -1
                    val dineIn = dineInList
                    if (dineIn != null && dineIn.isNotEmpty()) {
                        val selectedHeader = dineInList.get(0).selectedPosition

                        dineIn.get(selectedHeader).items.forEachIndexed { pos, tbItem ->
                            if (item != null) {
                                item.isDestroy = false
                                if (tbItem.itemId == item.itemId && checkVariation(
                                        tbItem, item
                                    ) && checkModifier(tbItem, item)
                                ) {

                                    index = pos
                                    return@forEachIndexed

                                }
                            }


                        }




                        if (index != -1) {
                            val model =
                                cartList[0].dineInList?.get(selectedHeader)?.items?.get(index)
                            if (model != null) {
                                if (type == "UPDATE") {
                                    if (item != null) {
                                        model.note = item.note
                                        model.itemQuantity = item.itemQuantity
                                        itemDiscountApply(model, item)
                                    }
                                    model.isDestroy = false
                                    if (prefProvider.getValueboolean(
                                            Constants.DINE_IN_UPDATE, false
                                        )
                                    ) {
                                        model.isEdited = true
                                    }
                                    model.modifiers.forEach {
                                        item?.modifiers?.forEach { itemmodif ->
                                            if (itemmodif.id == it.id) {
                                                it.itemQuantity = itemmodif.itemQuantity ?: 1
                                            }
                                        }
                                    }

                                    dineIn.get(selectedHeader).items[index] = model
                                    dineIn.get(selectedHeader).floorPlanTable =
                                        dineInList.get(0).floorPlanTable

                                    cartModel.dineInList = dineIn
                                    if (item != null) {
                                        if (type == UPDATE) {
                                            dineInList.forEach { dineInModel ->
                                                dineInModel.items.forEach { itemData ->
                                                    cartModel = taxBifurcationCalculation(
                                                        itemData, cartModel, type, false
                                                    )
                                                }

                                            }
                                        } else {
                                            cartModel = taxBifurcationCalculation(
                                                item, cartModel, type, false
                                            )
                                        }
                                    } else if (dineInList.isNotEmpty()) {
                                        dineInList.forEach { dineInModel ->
                                            dineInModel.items.forEach { itemData ->
                                                cartModel = taxBifurcationCalculation(
                                                    itemData, cartModel, type, false
                                                )
                                            }

                                        }
                                    }
                                    addCart(cartModel)
                                } else {
                                    if (index != -1) {
                                        if (item != null) {
                                            if (model.isDestroy) {
                                                model.itemQuantity = item.itemQuantity
                                                model.isDestroy = false
                                            } else {
                                                model.itemQuantity =
                                                    item.itemQuantity + model.itemQuantity
                                            }
                                            item.modifiers.forEach {
                                                model.modifiers.forEach { modelModifier ->
                                                    if (modelModifier.id == it.id) {
                                                        it.itemQuantity =
                                                            it.modifier_quantity * model.itemQuantity
                                                    }
                                                }

                                            }
                                            model.modifiers = item.modifiers
//                                            itemDiscountApply(model, item)
                                        }

                                        if (prefProvider.getValueboolean(
                                                Constants.DINE_IN_UPDATE, false
                                            )
                                        ) {
                                            model.isEdited = true
                                        }

                                        dineIn.get(selectedHeader).items[index] = model

                                        dineIn.get(0).floorPlanTable =
                                            dineInList.get(0).floorPlanTable
                                        cartModel.dineInList = dineIn
                                        if (item != null) {
                                            if (type == UPDATE) {
                                                dineInList.forEach { dineInModel ->
                                                    dineInModel.items.forEach { itemData ->
                                                        cartModel = taxBifurcationCalculation(
                                                            itemData, cartModel, type, false
                                                        )
                                                    }

                                                }
                                            } else {
                                                cartModel = taxBifurcationCalculation(
                                                    item!!, cartModel, type, false
                                                )
                                            }
                                        } else if (dineInList.isNotEmpty()) {
                                            dineInList.forEach { dineInModel ->
                                                dineInModel.items.forEach { itemData ->
                                                    cartModel = taxBifurcationCalculation(
                                                        itemData, cartModel, type, false
                                                    )
                                                }

                                            }
                                        }
                                        addCart(cartModel)
                                    } else {
                                        cartModel.dineInList = dineInList
                                        if (item != null) {
                                            if (type == UPDATE) {
                                                dineInList.forEach { dineInModel ->
                                                    dineInModel.items.forEach { itemData ->
                                                        cartModel = taxBifurcationCalculation(
                                                            itemData, cartModel, type, false
                                                        )
                                                    }

                                                }
                                            } else {
                                                cartModel = taxBifurcationCalculation(
                                                    item!!, cartModel, type, false
                                                )
                                            }

                                        } else if (dineInList.isNotEmpty()) {
                                            dineInList.forEach { dineInModel ->
                                                dineInModel.items.forEach { itemData ->
                                                    cartModel = taxBifurcationCalculation(
                                                        itemData, cartModel, type, false
                                                    )
                                                }

                                            }
                                        }
                                        addCart(cartModel)

                                    }
                                }


                            }
                        } else {

                            if (item != null) {
                                item.timeStamp = randomOfflineId()
                                if (prefProvider.getValueboolean(
                                        Constants.DINE_IN_UPDATE, false
                                    )
                                ) {
                                    item.isEdited = true
                                }
                                item.isDestroy = false

                                dineInList.get(dineInList.get(0).selectedPosition).items.add(
                                    item
                                )
                            }
                            cartModel.dineInList = dineInList
                            if (item != null) {
                                if (type == UPDATE) {
                                    dineInList.forEach { dineInModel ->
                                        dineInModel.items.forEach { itemData ->
                                            cartModel = taxBifurcationCalculation(
                                                itemData, cartModel, type, false
                                            )
                                        }

                                    }
                                } else {
                                    cartModel = taxBifurcationCalculation(
                                        item!!, cartModel, type, false
                                    )
                                }
                            } else if (dineInList.isNotEmpty()) {
                                dineInList.forEach { dineInModel ->
                                    dineInModel.items.forEach { itemData ->
                                        cartModel = taxBifurcationCalculation(
                                            itemData,
                                            cartModel,
                                            type,
                                            true,
                                        )
                                    }

                                }
                            }
                            addCart(cartModel)
                        }


                    }


                } else if (type == DELETE) {

                    var dine = dineInList.toMutableList()


                    if (item?.isEdited == true) {
                        dineInSelectedItemHeaderPos?.let {
                            dine.get(it).items.get(selectedItemPositionDine).isDestroy = true
                            dine.get(it).items.get(selectedItemPositionDine).isEdited = true
                            removeItemDineInList.add(
                                dine.get(it).items.get(
                                    selectedItemPositionDine
                                )
                            )
                            dineInSelectedItemHeaderPos?.let {
                                dine.get(it).items.remove(
                                    dine.get(dineInSelectedItemHeaderPos).items.get(
                                        selectedItemPositionDine
                                    )
                                )
                            }
                        }
                    } else {
                        dineInSelectedItemHeaderPos?.let {
                            dine.get(it).items.remove(
                                dine.get(dineInSelectedItemHeaderPos).items.get(
                                    selectedItemPositionDine
                                )
                            )
                        }
                    }

                    cartModel.dineInList = dine
                    cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                    addCart(cartModel)


                    /*dineInList.toMutableList().remove(
                        dineInList.get(dineInList.get(0).selectedPosition).items.get(
                            dineInList.get(0).selectedPosition
                        )
                    )*/
                } else if (type == DINE_IN_LIST_EDIT) {
                    cartModel.orderTypeName = DINE_IN
                    cartModel.orderType = DINE_IN
                    cartModel.dineInList = dineInList
                    cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                    addCart(cartModel)

                }


            } else {
                // already cart ma hoy to add/update/delete kare flag wise
                val list = cartList?.get(0)?.items?.toMutableList()
                if (list != null && list.isNotEmpty()) {

                    if (type == ADD || type == UPDATE) {


                        var index = -1

                        for (i in list.indices) {
                            if (item != null) {
                                if (item.isManualSales) {


                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        Log.d(TAG, "cartLogic: " + i)
                                        index = i
                                        break
                                    }

                                } else {
                                    if (list[i].itemId == item.itemId && checkVariation(
                                            list[i], item
                                        ) && checkModifier(list[i], item)
                                    ) {
                                        Log.d(TAG, "cartLogic: " + i)
                                        index = i
                                        break
                                    }
                                }

                            }
                        }

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
                                        if (prefProvider.getValueboolean(
                                                IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                            )
                                        ) {
                                            model.isEdited = true
                                        }
                                        item.modifiers.forEach {
                                            it.itemQuantity =
                                                (it.modifier_quantity * item.itemQuantity)/*model.modifiers.forEach { tbmodifier ->
                                                if (it.id == tbmodifier.id) {
                                                    tbmodifier.itemQuantity =
                                                        it.itemQuantity
                                                }
                                            }*/
                                        }
                                        model.modifiers = item.modifiers
                                        model.isDestroy = false
                                        itemDiscountApply(model, item)

                                    }
                                    list[index] = model
                                } else {
                                    if (index != -1) {
                                        if (item != null) {
                                            if (model.isDestroy) {
                                                model.itemQuantity = item.itemQuantity
                                                model.isDestroy = false
                                            } else {
                                                model.itemQuantity =
                                                    item.itemQuantity + model.itemQuantity
                                            }
                                            item.modifiers.forEach {
                                                it.itemQuantity =
                                                    (it.modifier_quantity * item.itemQuantity)/* model.modifiers.forEach { tbmodfier ->
                                                     if (it.id == tbmodfier.id) {
                                                         it.itemQuantity + model.itemQuantity
                                                     }
                                                 }*/

                                            }

//                                            for (i in model.modifiers) {
//                                                for (j in item.modifiers) {
//                                                    if (i.id == j.id) {
//                                                        j.itemQuantity = j.itemQuantity
//                                                        j.orderModifierId = i.orderModifierId
//                                                    }
//                                                }
//                                            }

                                            model.modifiers = item.modifiers
                                            if (item.isEdited) {
                                                model.isEdited = item.isEdited
                                            }

                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                )
                                            ) {
                                                model.isEdited = true
                                            }
                                            //   itemDiscountApply(model, item)
                                        }

                                        list[index] = model
                                    } else {
                                        if (item != null) {
                                            model.itemQuantity = item.itemQuantity
                                            if (item.isEdited) {
                                                model.isEdited = item.isEdited
                                            }
                                            if (prefProvider.getValueboolean(
                                                    IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                                )
                                            ) {
                                                model.isEdited = true
                                            }
                                            itemDiscountApply(model, item)
                                            model.isDestroy = false
                                        }
                                        list[index] = model
                                    }
                                }

                            }
                        } else {
                            Log.d(TAG, "cartLogic: " + index)
                            if (item != null) {
                                item.isDestroy = false
                                if (prefProvider.getValueboolean(
                                        IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                    )
                                ) {
                                    item.isEdited = true
                                }
                                list.add(item)
                            }
                        }
                    } else if (type == DELETE) {

                        var index = -1
                        Log.e(TAG, "CheckDeleteItem ${Gson().toJson(item)}")
                        Log.e(TAG, "getListedItems  ${Gson().toJson(list[0])}")
                        Log.e(TAG, "checkReOrder  ${cartModel?.reorder}")

                        for (i in list.indices) {
                            if (item != null) {
                                if (!item.isManualSales) {

                                    if (cartModel?.reorder == false && list[i].itemId == item.itemId && checkVariation(
                                            list[i], item
                                        ) && checkModifier(list[i], item)
                                    ) {
                                        Log.e(TAG, "CheckedBefore")
                                        index = i
                                        break
                                    } else if (cartModel?.reorder == true) {
                                        if (list[i].orderItemId == item.orderItemId) {
                                            index = i
                                            Log.e(TAG, "indexReorder:  ${index}")
                                            break
                                        }

                                    }

                                } else {
                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        index = i
                                        break
                                    } else if (cartModel?.reorder == true) {
                                        if (list[i].orderItemId == item.orderItemId) {
                                            index = i
                                            Log.e(TAG, "indexReorder23:  ${index}")
                                            break
                                        }

                                    }
                                }
                            }

                        }

//                        list.forEachIndexed { pos, tbItem ->
//                            if (item != null) {
//                                if (!item.isManualSales) {
//                                    if (tbItem.itemId == item.itemId && checkVariationDelete(
//                                            tbItem,
//                                            item
//                                        ) && checkModifierDelete(tbItem, item)
//                                    ) {
//                                        index = pos
//                                        return@forEachIndexed
//                                    }
//                                } else {
//                                    if (tbItem.manualSaleId == item.manualSaleId) {
//                                        index = pos
//                                        return@forEachIndexed
//                                    }
//                                }
//
//                            }
//                        }


                        if (index != -1) {
                            val model = cartList[0].items?.get(index)
                            if (model != null) {
                                //delete from cart
                                if (item?.isEdited == true) {
                                    model.isEdited = item.isEdited
                                    model.isDestroy = true
                                } else {
                                    list.remove(model)
                                }
                            }
                        } else {
                            //list.remove(item)
                        }
                    }

                    var cartModel = cartList[0]
                    if (type == UPDATE) {
                        cartModel.items?.forEach { itemData ->
                            cartModel = taxBifurcationCalculation(
                                itemData, cartModel, type, false
                            )
                        }
                    } else {
                        cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                    }
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

                        var cartModel = cartList?.get(0)
                        cartModel = taxBifurcationCalculation(item!!, cartModel!!, type, false)
                        if (item != null) cartModel?.items = listOf(item)
                        if (cartModel != null) {
                            addCart(cartModel!!)
                        }
                    }


                }
            }


        }
    }

    private fun combineItem(list: ArrayList<TbItem>, item: TbItem, index: Int): List<TbItem> {
        Log.e(TAG, "newItemitemQuantity  ${Gson().toJson(item)}")
        Log.e(TAG, "newItemitemQuantity  ${item.itemQuantity}")
        Log.e(TAG, "newItemitemQuantityOld  ${list[index].itemQuantity}")
//        if(list[index].itemQuantity < MAX_ITEM_QUANTITY){
//        }
        if ((list[index].itemQuantity + item.itemQuantity) <= MAX_ITEM_QUANTITY) {
            list[index].itemQuantity += item.itemQuantity
            list[index].modifiers.forEach { listmod ->
                item.modifiers.forEach { itemmod ->
                    if (itemmod.id == listmod.id) {
                        listmod.itemQuantity = itemmod.modifier_quantity * list[index].itemQuantity
                    }
                }
            }
        } else {
            _itemQuantityCheck.value = Event(true)
        }

        Log.d(TAG, "combineItem: " + list[index].itemQuantity)
        Log.d(TAG, "combineItem: " + Gson().toJson(list[index].modifiers))
        return list

    }

    private fun checkSameModifier(tbItem: TbItem, item: TbItem): Boolean {

        var isSame = false

        if (tbItem.modifiers.size != item.modifiers.size) {

            run breaking@{
                tbItem.modifiers.forEach { tbItemM ->

                    item.modifiers.forEach { itemM ->

                        isSame = tbItemM.id == itemM.id
                    }
                    if (isSame) return@breaking
                }
            }

        } else {
            isSame = true
        }


        return isSame
    }

    private fun itemDiscountApply(model: TbItem, item: TbItem) {
        model.discountPrice = item.discountPrice
        model.discountType = item.discountType
        model.discountId = item.discountId
        model.isManualSales = item.isManualSales
        model.note = item.note
    }

    fun addItemToCart(
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

            val list = cartList?.get(0)?.items?.toMutableList()
            if (list != null && list.isNotEmpty()) {

                if (type == ADD || type == UPDATE) {
                    var index = -1

                    list.forEachIndexed { pos, tbItem ->
                        if (item != null) {
                            if (tbItem.itemId == item.itemId && checkVariation(
                                    tbItem, item
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
                                        model.itemQuantity = item.itemQuantity + model.itemQuantity
                                        item.modifiers.forEach {
                                            it.itemQuantity =
                                                model.itemQuantity * it.modifier_quantity
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

                val cartModel = cartList[0]
                cartModel.items = list
                addCart(cartModel)

                if (list.isEmpty()) {
                    // delete cart
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


    fun checkModifierNewLogic(tbItem: TbItem, item: TbItem): Boolean {
        var isSame = false
        var listOfDataMod: ArrayList<Int> = arrayListOf()
        var tbMod: HashMap<Int, Int> = hashMapOf()
        var itemMod: HashMap<Int, Int> = hashMapOf()
        Log.e(TAG, "checkItemTbMod  ${Gson().toJson(tbItem.modifiers)}")
        Log.e(TAG, "checkItemMod  ${Gson().toJson(item.modifiers)}")

        if (tbItem.modifiers.isNotEmpty()) {
            tbItem.modifiers.forEach {
                if (!it._destroy) {
                    tbMod.put(it.id ?: 0, it.modifier_quantity)
                    listOfDataMod.add(it.id ?: 0)
                }
            }
        }

        var listOfDataModSelected: ArrayList<Int> = arrayListOf()
        if (item.modifiers.isNotEmpty()) {
            item.modifiers.forEach {
                if (!it._destroy) {
                    itemMod.put(it.id ?: 0, it.modifier_quantity)
                    listOfDataModSelected.add(it.id ?: 0)
                }

            }
        }

        if (tbItem.modifiers.isEmpty() && item.modifiers.isEmpty()) return true

        Log.e(TAG, "checkModSize  ${tbMod.size}")
        Log.e(TAG, "checkModSizeItemMod  ${itemMod.size}")

        if (listOfDataMod.size == listOfDataModSelected.size) {

            isSame = true
            Log.e(TAG, "Item.modifiers  ${Gson().toJson(item.modifiers)}")
            Log.e(TAG, "TbItem.modifiers   ${Gson().toJson(tbItem.modifiers)}")


            var selectedList: ArrayList<Boolean> = arrayListOf()

            Log.e("CheckModData", "checktbMod:  ${Gson().toJson(tbMod)}")
            Log.e("CheckModData", "checkContaine  ${Gson().toJson(itemMod)}")
            tbMod.forEach {
                Log.e(TAG, "GetKey ${it.key}  GetValue ${it.value}")
                if (itemMod.containsKey(it.key) && it.value == itemMod.get(it.key)) {
                    selectedList.add(true)

                } else {
                    selectedList.add(false)
                }
            }

            Log.e(TAG, "selectedList:  ${Gson().toJson(selectedList)}")
            if (selectedList.contains(false)) {
                isSame = false
            }

            /*item.modifiers.forEach { itmod ->
                tbItem.modifiers.forEach { tbmod ->
                    if (tbmod.id == itmod.id && tbmod.modifier_quantity != itmod.itemQuantity) {
                        Log.d(
                            TAG,
                            "checkModifierNewLogic: ${tbmod.modifier_quantity}   ${itmod.itemQuantity}"
                        )

                        isSame = false
                        return@forEach

                    }

                    if (isSame == false) {
                        return@forEach
                    }

                }
            }*/


        } else {
            isSame = false
        }

        Log.e(TAG, "ReturnIssame ${isSame}")
        return isSame

    }

    private fun checkModifier(tbItem: TbItem, item: TbItem): Boolean {


        var isSame = true

        var listOfDataMod: ArrayList<Int> = arrayListOf()

        if (tbItem.modifiers.isNotEmpty()) {
            tbItem.modifiers.forEach {
                listOfDataMod.add(it.id ?: 0)
            }
        }

        var listOfDataModSelected: ArrayList<Int> = arrayListOf()
        if (item.modifiers.isNotEmpty()) {
            item.modifiers.forEach {
                listOfDataModSelected.add(it.id ?: 0)
            }
        }

        if (tbItem.modifiers.isEmpty() && item.modifiers.isEmpty()) return true

        if (listOfDataMod.containsAll(listOfDataModSelected) && listOfDataMod.size == listOfDataModSelected.size) {
            isSame = true
        } else {
            isSame = false
        }

        return isSame


    }

    private fun checkVariation(tbItem: TbItem, item: TbItem): Boolean {

        var isSame = true

        var listOfDataMod: ArrayList<Int> = arrayListOf()
        if (tbItem.variationsAttributes.isNotEmpty()) {
            tbItem.variationsAttributes.forEach {
                listOfDataMod.add(it.id ?: 0)
            }
        }

        var listOfDataModSelecteItem: ArrayList<Int> = arrayListOf()
        if (item.variationsAttributes.isNotEmpty()) {
            item.variationsAttributes.forEach {
                listOfDataModSelecteItem.add(it.id ?: 0)
            }
        }

        if (tbItem.variationsAttributes.isEmpty() && item.variationsAttributes.isEmpty()) return true
        isSame =
            if (listOfDataMod.containsAll(listOfDataModSelecteItem) && listOfDataMod.size == listOfDataModSelecteItem.size) {
                if (tbItem.variationsAttributes[0].id == item.variationsAttributes[0].id) {
                    (tbItem.variationsAttributes[0].price == item.variationsAttributes[0].price)
                } else {
                    false
                }
            } else {
                false
            }


        Log.e(TAG, "CheckVariationAdd ${isSame}")
        return isSame
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


    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        cartList: List<CartModel>?,
        txtTotalAmount: AppCompatTextView,
        context: Context,
        isFromManualSales: Boolean = false
    ) {

        if (cartList != null && cartList.isNotEmpty()) {
            var totalAmmount = 0.0
            nonCashAdj = 0.0
            totalPrice = 0.0
            totalCount = 0
            subTotalPrice = 0.0
            totalDiscount = 0.0
            totalTax = 0.0
            totalServiceCharge = 0.0
            var amountToBePaid = 0.0

            if (cartList[0].orderType == DINE_IN) {

                var dineInItems = 0
                if (isFromManualSales) {

                    dineInItems = cartList[0].items?.size ?: 0
                    cartList[0].items?.forEach { item ->
                        totalCount += item.itemQuantity

                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                        taxCalculation(item, cartList[0].discountPrice / dineInItems)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)
                        }
                    }
                    calculateDineInServiceCharge(cartList[0])
                    subTotalPrice -= (cartList[0].discountPrice)

                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }

                    totalDiscount += cartList[0].discountPrice
                    cartList[0].items?.forEach {
                        totalDiscount += (it.discountPrice * it.itemQuantity)
                    }

                } else {

                    cartList[0].dineInList?.forEach { dine ->
                        dineInItems += dine.items.size
                    }

                    cartList[0].dineInList?.forEach { dine ->


                        dine.items.forEach { item ->
                            totalCount += item.itemQuantity

                            subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                            taxCalculation(item, cartList[0].discountPrice / dineInItems)

                            item.modifiers.forEach {
                                subTotalPrice += (it.price * it.itemQuantity)
                            }
                        }


                    }

                    calculateDineInServiceCharge(cartList[0])
                    subTotalPrice -= (cartList[0].discountPrice)

                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }

                    totalDiscount += cartList[0].discountPrice

                    cartList[0].dineInList?.forEach {
                        it.items.forEach {
                            totalDiscount += (it.discountPrice * it.itemQuantity)
                        }
                    }

                }
                totalPrice = (subTotalPrice + totalTax + totalServiceCharge)
                amountToBePaid = totalPrice - totalDiscount
                Log.e("ManualSale", "amountToBePaid:   ${amountToBePaid}")

                MethodUtils.setPriceTextView(txtTotalAmount, amountToBePaid)
            } else {


                if (cartList[0].items?.isEmpty() == false) {


                    val itemCount = cartList[0].items?.size

                    cartList[0].items?.forEach { item ->
                        totalCount += item.itemQuantity
                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                        taxCalculation(item, cartList[0].discountPrice / itemCount!!)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)

                        }
                    }

                    order_note = cartList[0].note
                    subTotalPrice -= cartList[0].discountPrice

                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    serviceChargeCalculation(cartList)


                    totalDiscount += cartList[0].discountPrice

                    cartList[0].items!!.forEach {
                        totalDiscount += (it.discountPrice * it.itemQuantity)
                    }


                    totalPrice = (subTotalPrice + totalTax + totalServiceCharge)
                    cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")

                    //loyalty point and price calculation
                    amountToBePaid = totalPrice


                    if (selectedCustomer == null) {
                        var fnAmount = amountToBePaid
                        MethodUtils.setPriceTextView(
                            txtTotalAmount, fnAmount
                        )
                    } else {
                        var fnAmount = amountToBePaid
                        checkAppliedLoyaltyProgram(
                            selectedCustomer, fnAmount, txtTotalAmount
                        )
                    }

                    if (MethodUtils.isEnableCashDiscount(context)) {
                        cashdiscountAmount = MethodUtils.calculateCashDiscount(
                            totalPrice, prefProvider, context
                        )
                    } else {
                        cashdiscountAmount = 0.0
                    }
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
        } else {
            nonCashAdj = 0.0
            totalPrice = 0.0
            totalCount = 0
            subTotalPrice = 0.0
            totalDiscount = 0.0
            totalTax = 0.0
            totalServiceCharge = 0.0
        }
        //totalAmmount = totalPrice-cartList[0].discountPrice

    }

    @SuppressLint("SetTextI18n")
    fun itemCalculationCartModel(
        cartModel: CartModel, txtTotalAmount: AppCompatTextView, context: Context
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
        if (cartModel.orderType == DINE_IN) {

            var dineInItems = 0

            cartModel.dineInList?.forEach { dine ->
                dineInItems += dine.items.size
            }

            cartModel.dineInList?.forEach { dine ->

                dine.items.forEach { item ->
                    if (item.isDestroy != true) {
                        totalCount += item.itemQuantity
                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                        taxCalculation(item, cartModel.discountPrice / dineInItems)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)
                        }
                    }
                }


            }
            order_note = cartModel.note
            calculateDineInServiceCharge(cartModel)
//            serviceChargeCalculationModel(cartModel)
            subTotalPrice -= cartModel.discountPrice
            if (subTotalPrice < 0) {
                subTotalPrice = 0.0
            }

            var totalDis = cartModel.discountPrice
            var totalDineItemDis = 0.0
            cartModel.dineInList?.forEach {
                it.items.forEach {
                    totalDineItemDis += (it.discountPrice * it.itemQuantity)

                }
            }



            totalDiscount += cartModel.discountPrice
            cartModel.dineInList?.forEach {
                it.items.forEach {
                    totalDiscount += (it.discountPrice * it.itemQuantity)

                }
            }


            var finalTotal = 0.0

            (MethodUtils.getTwoDecimal(subTotalPrice) + MethodUtils.getTwoDecimal(totalTax) + MethodUtils.getTwoDecimal(
                totalServiceCharge
            ))

            cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
            //loyalty point and price calculation
            amountToBePaid = finalTotal
            if (selectedCustomer == null) {
                totalPrice = amountToBePaid
                MethodUtils.setPriceTextView(
                    txtTotalAmount, amountToBePaid
                )
            } else {
                checkAppliedLoyaltyProgram(
                    selectedCustomer, amountToBePaid, txtTotalAmount
                )
                redeemLoyaltyInfo.getAmountToBePaid()?.let {
                    totalPrice = it
                }
            }

            if (MethodUtils.isEnableCashDiscount(context)) {
                cashdiscountAmount = MethodUtils.calculateCashDiscount(
                    totalPrice, prefProvider, context
                )
            } else {
                cashdiscountAmount = 0.0
            }


        } else {

            if (cartModel.items?.isEmpty() == false) {

                if (cartModel.reorder) {


                    val itemCount = cartModel.items?.size

                    cartModel.items?.forEach { item ->
                        totalCount += item.itemQuantity
                        totalDiscount += (item.discountPrice * item.itemQuantity)
                        subTotalPrice += (item.price * item.itemQuantity)
                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)
                        }
                        taxCalculationReorder(item)


                    }
                    String.format("%.2f", totalTax).toDouble()
                    subTotalPrice -= cartModel.discountPrice
                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    serviceChargeCalculationModel(cartModel)

                    totalDiscount += cartModel.discountPrice
                    order_note = cartModel.note

                    var finalTotal = 0.0
                    finalTotal = (subTotalPrice + totalTax + totalServiceCharge)



                    cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                    //loyalty point and price calculation
                    amountToBePaid = finalTotal
                    if (selectedCustomer == null) {
                        totalPrice = amountToBePaid
                        MethodUtils.setPriceTextView(
                            txtTotalAmount, amountToBePaid
                        )
                    } else {
                        checkAppliedLoyaltyProgram(
                            selectedCustomer, amountToBePaid, txtTotalAmount
                        )
                        redeemLoyaltyInfo.getAmountToBePaid()?.let {
                            totalPrice = it
                        }
                    }

                    if (MethodUtils.isEnableCashDiscount(context)) {
                        cashdiscountAmount = MethodUtils.calculateCashDiscount(
                            totalPrice, prefProvider, context
                        )
                    } else {
                        cashdiscountAmount = 0.0
                    }


                } else {

                    nonCashAdj = 0.0
                    totalPrice = 0.0
                    totalCount = 0
                    subTotalPrice = 0.0
                    totalDiscount = 0.0
                    totalTax = 0.0
                    totalServiceCharge = 0.0

                    val itemCount = cartModel.items?.size
                    var taxList: ArrayList<TaxData> = arrayListOf()
                    cartModel.items?.forEach { item ->
                        if (!item.isDestroy) {
                            totalCount += item.itemQuantity
                            totalDiscount += item.discountPrice * item.itemQuantity
                            subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                            taxCalculation(item, cartModel.discountPrice / itemCount!!)

                            item.modifiers.forEach {
                                subTotalPrice += (it.price * it.itemQuantity)

                            }
                        }
                    }
                    //   String.format("%.2f", totalTax).toDouble()
//                    taxDynamicList = cartModel.taxlistDynamic!!.toCollection(ArrayList())

                    subTotalPrice -= cartModel.discountPrice
                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    serviceChargeCalculationModel(cartModel)

                    totalDiscount += cartModel.discountPrice


                    order_note = cartModel.note

                    var finalTotal = 0.0
                    finalTotal = (subTotalPrice + totalTax + totalServiceCharge)

                    redeemLoyaltyInfo.needToApplyLoyalty = prefProvider.getValueboolean(
                        LOYALTY_ADDED, false
                    )
                    totalPrice = finalTotal



                    cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                    //loyalty point and price calculation
                    amountToBePaid = finalTotal

                    if (selectedCustomer == null) {
                        totalPrice = amountToBePaid
                        MethodUtils.setPriceTextView(
                            txtTotalAmount, amountToBePaid
                        )
                    } else {
                        checkAppliedLoyaltyProgram(
                            selectedCustomer, amountToBePaid, txtTotalAmount
                        )
                        redeemLoyaltyInfo.getAmountToBePaid()?.let {
                            totalPrice = it
                        }
                    }

                    if (MethodUtils.isEnableCashDiscount(context) && prefProvider.getValue(
                            ORDER_TYPE, TAKEOUT
                        ) != GIFT_CARD
                    ) {
                        cashdiscountAmount = MethodUtils.calculateCashDiscount(
                            totalPrice, prefProvider, context
                        )
                    } else {
                        cashdiscountAmount = 0.0
                    }

                }


            } else {

                nonCashAdj = 0.0
                totalPrice = 0.0
                totalCount = 0
                subTotalPrice = 0.0
                totalDiscount = 0.0
                totalTax = 0.0
                totalServiceCharge = 0.0
                amountToBePaid = 0.0
                MethodUtils.setPriceTextView(txtTotalAmount, totalPrice)
            }
        }
        //totalAmmount = totalPrice-cartList[0].discountPrice


    }


    fun loyaltyPointCondition(customer: TbCustomer?): Boolean {
        return (customer?.enroll_to_loyalty == true && activeLoyaltyProgram != null && activeLoyaltyProgram?.rewardPoint ?: 0 <= customer.final_reward ?: 0)
    }

    private fun checkAppliedLoyaltyProgram(
        customer: TbCustomer?, total: Double, txtTotalAmount: AppCompatTextView
    ) {

        redeemLoyaltyInfo.total = total
        val availablePoints1 = customer?.final_reward ?: 0

        if (customer == null) {
            //loyalty cant be applied if customer is not selected.
            redeemLoyaltyInfo.needToApplyLoyalty = false
        } else if (loyaltyPointCondition(customer)) {
            activeLoyaltyProgram?.let {
                redeemLoyaltyInfo.loyaltyProgramsModel = activeLoyaltyProgram

                val availablePoints =
                    it.rewardPoint.times((customer.final_reward?.floorDiv(it.rewardPoint)!!)) ?: 0
                //if customer has more points than required(minimum limit)
                var availableLoyaltyAmount = 0.0
                if (it.rewardPoint == 0) {
                    it.rewardPoint = 1
                }
                availableLoyaltyAmount = availablePoints * it.amount / it.rewardPoint
                if (availableLoyaltyAmount > redeemLoyaltyInfo.total) {
                    var pointDouble = (redeemLoyaltyInfo.total * it.rewardPoint) / it.amount
                    pointDouble =
                        (it.rewardPoint * (pointDouble.div(it.rewardPoint)).toInt()).toDouble()
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
        redeemLoyaltyInfo.getAmountToBePaid()?.let {
            MethodUtils.setPriceTextView(
                txtTotalAmount, it
            )
        }

    }

    private fun serviceChargeCalculation(cartList: List<CartModel>) {
        val serviceChargesList = cartList[0].serviceCharge
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        totalServiceCharge += (subTotalPrice * it.percentage) / 100
                    }
                }
            }

        }
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

    fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
        return (minn <= value && value <= maxx)
    }

    private fun calculateDineInServiceCharge(cartModel: CartModel) {
        var guestCount = cartModel.dineInList?.size?.minus(1)
        if (serviceChargesList.isNotEmpty() && serviceChargesList != null) {
            if (prefProvider.getValueboolean(SERVICECHARGE_DINEIN_ORDER, false)) {
                var isApplied = false
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!, it.max_guest_count!!, guestCount!!
                            )
                        ) {
                            isApplied = true
                            Log.d(
                                TAG,
                                "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + guestCount
                            )
                            totalServiceCharge += (subTotalPrice * it.percentage) / 100
                            return@forEach
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
            if (prefProvider.getValueboolean(SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        var serviceTotal = (subTotalPrice * it.percentage) / 100
                        totalServiceCharge += String.format("%.2f", serviceTotal).toDouble()
                    }
                }
            }

        }
    }


    private fun getTotalTaxBirfurcation(
        item: TbItem, itemtype: TaxData, type: String
    ): Double {
        var totaltaxtemp: Double = 0.0
        var modifierPrice = 0.0
        val price = (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

        item.modifiers.forEach {
            modifierPrice += (it.price * it.itemQuantity)
        }

        val totalPrice = price + modifierPrice /*- (discountPrice * item.itemQuantity)*/


        totaltaxtemp += if (itemtype.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00).toDouble()
            } else {
                val itemTaxPrice = (itemtype.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + itemtype.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00).toDouble()
            } else {
                String.format("%.2f", itemtype.rate * item.itemQuantity).toDouble()
            }

        }
        return totaltaxtemp
    }

    private fun taxBifurcationCalculation(
        item: TbItem, cartModel: CartModel, type: String, orderTaxID: Boolean
    ): CartModel {
        item.taxes?.forEachIndexed { indextax, itemtype ->
            if (itemtype.isActive && !itemtype.isDeleted) {
                if (cartModel.taxlistDynamic?.isNotEmpty() == true) {
                    var found = -1
                    cartModel.taxlistDynamic!!.forEachIndexed { index, itemData ->
                        if (orderTaxID) {
                            if (itemtype.orderTaxId == itemData.orderTaxId) {
                                found = index
                            }
                        } else {
                            if (itemtype.id == itemData.id) {
                                found = index
                            }
                        }

                    }
                    Log.d(TAG, "taxBifurcationCalculation: " + found)
                    if (found == -1) {
                        if (itemtype.taxType != "Percentage") {
                            var modifierPrice: Double = 0.0
                            val price =
                                (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                            item.modifiers.forEach {
                                modifierPrice += (it.price * it.itemQuantity)
                            }

                            val totalPrice = price + modifierPrice
                            itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                        } else {
                            itemtype.subTotalAmount = 0.0
                        }
                        itemtype.totalTaxTypePrice = getTotalTaxBirfurcation(item, itemtype, type)
                        cartModel.taxlistDynamic =
                            concatenate(cartModel.taxlistDynamic!!, listOf(itemtype))
                    } else {
                        if (itemtype.taxType != "Percentage") {
                            var modifierPrice: Double = 0.0
                            val price =
                                (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                            item.modifiers.forEach {
                                modifierPrice += (it.price * it.itemQuantity)
                            }
                            val totalPrice = price + modifierPrice
                            if (type == DELETE) {
                                if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                    cartModel.taxlistDynamic!![found].subTotalAmount =
                                        cartModel.taxlistDynamic!![found].subTotalAmount.minus(
                                            totalPrice
                                        )
                                }
                            } else {
                                if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                    cartModel.taxlistDynamic!![found].subTotalAmount =
                                        cartModel.taxlistDynamic!![found].subTotalAmount.plus(
                                            totalPrice
                                        )
                                }
                            }

                        } else {
                            if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                cartModel.taxlistDynamic!![found].subTotalAmount = 0.0
                            }
                        }
                        if (type == ADD || type == UPDATE) {
                            if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                                    cartModel.taxlistDynamic!![found].totalTaxTypePrice.plus(
                                        getTotalTaxBirfurcation(
                                            item, itemtype, type
                                        )
                                    )
                            }
                        } else if (type == DELETE) {
                            if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                                    cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice?.minus(
                                        getTotalTaxBirfurcation(
                                            item, itemtype, type
                                        )
                                    )!!
                            }
                            if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                if (cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice == 0.0) {
                                    var temp_arraylist: ArrayList<TaxData> =
                                        cartModel.taxlistDynamic!!.toCollection(
                                            arrayListOf()
                                        )
                                    temp_arraylist.removeAt(found)
                                    cartModel.taxlistDynamic = temp_arraylist.toList()

                                }
                            }
                            if (cartModel.taxlistDynamic!!.isNotEmpty()) {
                                if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                    if (cartModel.taxlistDynamic?.get(found)?.taxType != "Percentage") {
                                        if (cartModel.taxlistDynamic?.get(found)?.subTotalAmount == 0.0) {
                                            var temp_arraylist: ArrayList<TaxData> =
                                                cartModel.taxlistDynamic!!.toCollection(
                                                    arrayListOf()
                                                )
                                            temp_arraylist.removeAt(found)
                                            cartModel.taxlistDynamic = temp_arraylist.toList()

                                        }
                                    }
                                }

                            }


                        }

                    }
                } else {
                    if (itemtype.taxType != "Percentage") {
                        var modifierPrice: Double = 0.0
                        val price =
                            (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                        item.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice
                        itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                    }
                    itemtype.totalTaxTypePrice = getTotalTaxBirfurcation(item, itemtype, type)
                    cartModel.taxlistDynamic = listOf(itemtype)
                }
            }

        }

        return cartModel
    }

    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    private fun taxCalculation(item: TbItem, discountPrice: Double) {

        item.taxes?.forEach { tax ->
            if (tax.isActive && !tax.isDeleted) {


                var modifierPrice = 0.0
                val price =
                    (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                item.modifiers.forEach {
                    modifierPrice += (it.price * it.itemQuantity)
                }

                val totalPrice = price + modifierPrice /*- (discountPrice * item.itemQuantity)*/

                Log.e("GetTaxTotalPrice", "totalPrice:   ${totalPrice}")

                totalTax += if (tax.taxType == "Percentage") {
                    Log.d("yash", "taxCalculation: " + tax.taxType)


                    if (totalPrice < 0.0) {

                        String.format("%.2f", 0.00).toDouble()
                    } else {
                        val itemTaxPrice = (tax.rate * totalPrice) / 100
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                        //String.format("%.2f",itemTaxPrice).toDouble()
                        String.format("%.2f", itemTaxPrice).toDouble()
                        //  MethodUtils.getTwoDecimal(itemTaxPrice)
                    }

                } else {
                    Log.d("yash", "taxCalculation: " + tax.taxType)

                    if (totalPrice <= 0.0) {
                        String.format("%.2f", 0.00).toDouble()
                    } else {
                        //   MethodUtils.getTwoDecimal(tax.rate * item.itemQuantity)
                        /*String.format("%.2f", tax.rate * item.itemQuantity)
                            .toDouble()*/
                        String.format("%.2f", tax.rate * item.itemQuantity).toDouble()

                    }

                }
            }
        }
        String.format("%.2f", totalTax).toDouble()

        Log.e("CheckTotalTax", "totalTax:   ${totalTax}")
    }

    private fun taxCalculationReorder(item: TbItem) {
        item.taxes?.forEach { tax ->
            if (tax.isActive && !tax.isDeleted) {

                var modifierPrice = 0.0
                val price = (item.price * item.itemQuantity) - (item.discountPrice)

                item.modifiers.forEach {
                    modifierPrice += (it.price * it.itemQuantity)
                }

                val totalPrice = price + modifierPrice /*- (discountPrice * item.itemQuantity)*/


                totalTax += if (tax.taxType == "Percentage") {
                    Log.d("yash", "taxCalculation: " + tax.taxType)


                    if (totalPrice < 0.0) {

                        String.format("%.2f", 0.00).toDouble()
                    } else {
                        val itemTaxPrice = (tax.rate * totalPrice) / 100
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                        itemTaxPrice.toDouble()
                    }

                } else {
                    Log.d("yash", "taxCalculation: " + tax.taxType)

                    if (totalPrice <= 0.0) {
                        String.format("%.2f", 0.00).toDouble()
                    } else {
                        String.format("%.2f", tax.rate * item.itemQuantity).toDouble()
                    }

                }
            }
        }
    }

    fun setServiceCharges(mList: ArrayList<TbServiceCharge>?) {

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
        // networkConnectionInterceptor.setHostBaseUrl(prefProvider.getValue(BASE_URL_NEW,""))
        Log.d(TAG, "baseUrl${prefProvider.getValue(BASE_URL_NEW, "")}")
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

    fun getOnlineOrderCount() {
        //  _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.getOnlineOrderNotificationCount()
            when (resource.status) {
                Status.SUCCESS -> {
                    //  _showProgress.value = Event(false)
                    resource.data?.let { it ->
                        _onlineOrderCount.value = Event(it.data)
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    //  _showProgress.value = Event(true)
                }

            }
        }
    }

    private suspend fun callLogoutApi() {
        _showProgress.value = Event(true)
        val data = HashMap<String, String>()
        data["email"] = prefProvider.getValue(Constants.EMAIL, "").toString()


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

    fun clearTableAll() {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.clearTableManually()
        }
    }


    fun clearTable() {

        GlobalScope.launch {
            posRepository.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
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

        if (future_delivery_date.isNotEmpty()) orderAttributeRequestModel.futureDeliveryDate =
            future_delivery_date
        orderAttributeRequestModel.deliveryType = cartModel.openOrderType
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId = randomOfflineId()
        orderAttributeRequestModel.openOrderType = cartModel.orderType
        orderAttributeRequestModel.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
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
                taxData.percentage_value = MethodUtils.roundOffAmountDouble(taxData.rate)
            } else {
                taxData.percentage_value =
                    MethodUtils.roundOffAmountDouble((100 * taxData.totalTaxTypePrice) / taxData.subTotalAmount!!)
            }
        }
        orderAttributeRequestModel.tax_bifurcation_data = Gson().toJson(cartModel.taxlistDynamic)

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = "" + customerId
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
        //        orderAttributeRequestModel.orderServiceChargesAttributes =
        //            orderServiceChargesAttributes(cartModel, subTotalPrice)

        orderAttributeRequestModel.orderServiceChargesAttributes =
            dineInServiceChargeAppliedAttribute(cartModel)

        for (i in 0 until cartModel.dineInList?.size!!) {
            cartModel.dineInList?.get(i)?.items?.forEach { item ->
                if (item.timeStamp == null || item.timeStamp!!.lowercase() == "null".lowercase()) {
                    item.timeStamp = randomOfflineId().toString()
                }
            }
        }
        orderAttributeRequestModel.orderItemsAttributes = dineInOrderItemAttributed(cartModel)

        orderAttributeRequestModel.guestsAttributes = getGuestsAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(false, orderAttributeRequestModel)


        return orderRequestModel
    }

    fun dineInServiceChargeAppliedAttribute(cartModel: CartModel): List<OrderServiceChargesAttribute> {
        var guestCount = cartModel.dineInList?.size?.minus(1)
        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        if (prefProvider.getValueboolean(SERVICECHARGE_DINEIN_ORDER, false)) {
            var isApplied = false
            serviceChargesList.forEach {
                if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                    if (isInRange(
                            it.min_guest_count!!, it.max_guest_count!!, guestCount!!
                        )
                    ) {
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

                        isApplied = true
                        Log.d(
                            TAG,
                            "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + guestCount
                        )
                        totalServiceCharge += (subTotalPrice * it.percentage) / 100
                        return@forEach
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
        return orderServiceChargesAttributeList
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
        cartModel: CartModel, subTotalPrice: Double
    ): List<OrderServiceChargesAttribute> {
        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        val serviceChargesList = cartModel.serviceCharge
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {
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
            } else if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER, false
                )
            ) {
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

    private fun orderServiceChargesDineinAttributes(
        cartModel: CartModel, subTotalPrice: Double
    ): List<OrderServiceChargesAttribute> {
        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        val serviceChargesList = cartModel.serviceCharge
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {
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
            } else if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER, false
                )
            ) {
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
        Log.e(TAG, "dineInListData:   ${Gson().toJson(cartModel.dineInList)}")
        cartModel.dineInList?.forEach { it ->
            val model = GuestsAttributes()
            model.name = it.title.toString()
            model.Destroy = it.isDestroy
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
                it.items.sortedBy { it.dineInSort }
                it.items.forEach { tb ->


                    listItems.add(GuestItemsAttributes(id = tb.guestItemId,
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
                var addressList: ArrayList<CustomerAttributes.AddressesAttribute> = arrayListOf()
                var phoneList: ArrayList<CustomerAttributes.PhonesAttribute> = arrayListOf()
                for (i in 0.until(it.customer?.addresses?.size!!)) {

                    var address = CustomerAttributes.AddressesAttribute()
                    address.address1 = it.customer?.addresses?.get(i)?.address1.toString()
                    address.address2 = it.customer?.addresses?.get(i)?.address2.toString()
                    address.addressableId = it.customer?.addresses?.get(i)?.id
                    address.city = it.customer?.addresses?.get(i)?.city.toString()
                    address.country = it.customer?.addresses?.get(i)?.country.toString()/*address.latitude = it.customer?.addresses?.get(i)?.latitude!!.toDouble()
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
                    phoneModel.phoneNumber = it.customer?.phones?.get(i)?.phone_number.toString()
                    phoneList.add(phoneModel)
                }
                val customerModel = CustomerAttributes()/*  customerModel.addressesAttributes = addressList
                  customerModel.birthDate = it.customer?.birth_date.toString()
                  customerModel.firstName = it.customer?.first_name.toString()
                  customerModel.lastName = it.customer?.last_name.toString()*/
                customerModel.id = it.customer?.id/* customerModel.companyName = it.customer?.company.toString()
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
        var orderItemsAttributeList: ArrayList<OrderItemsAttribute> = arrayListOf()

        for (i in 0 until cartModel.dineInList?.size!!) {
            cartModel.dineInList?.get(i)?.items?.forEach { item ->

                val orderItemsAttribute = OrderItemsAttribute()

                orderItemsAttribute.category_id = item.categoryId

                orderItemsAttribute.id = item.orderItemId
                orderItemsAttribute.custom_item_id = item.id

                orderItemsAttribute.discountAmount = (item.discountPrice * item.itemQuantity)
                orderItemsAttribute.discountType = item.discountType
                if (item.discountId != -1) orderItemsAttribute.discountId = item.discountId
                orderItemsAttribute.employeeId = cartModel.employeeID
                orderItemsAttribute.isCount = 0
                orderItemsAttribute.isEdited = item.isEdited
                orderItemsAttribute.isDestroy = item.isDestroy
                orderItemsAttribute.isPaid = item.isPaid
                orderItemsAttribute.isPrinted = if (prefProvider.getValueboolean(
                        DINE_IN_UPDATE, false
                    ) == true && item.isEdited == true
                ) false else if (prefProvider.getValueboolean(
                        DINE_IN_UPDATE, false
                    ) == true && item.isEdited == false
                ) true else false
                orderItemsAttribute.isTaxRemoved = false
                orderItemsAttribute.itemId = if (item.isManualSales) item.itemId else item.itemId
                orderItemsAttribute.is_manual_sales = item.isManualSales
                orderItemsAttribute.itemName = item.name
                orderItemsAttribute.note = item.note
                orderItemsAttribute.price = item.price
                orderItemsAttribute.quantity = item.itemQuantity
                orderItemsAttribute.terminalId = cartModel.terminalId
                orderItemsAttribute.isFired = cartModel.isFired
                item.dineInSort = if (item.dineInSort == 0) {
                    orderItemsAttributeList.size + 1
                } else {
                    item.dineInSort
                }
                orderItemsAttribute.sort = item.dineInSort


                orderItemsAttribute.timestamp = item.timeStamp.toString()
                orderItemsAttribute.totalPrice =
                    MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
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


        cartModel.items?.forEach {
            cartModel?.dineInList?.forEach { m ->
                m.items.forEach { oi ->
                    if (oi.name.equals(it.name, true) == false && it.isDestroy) {
                        var model = OrderItemsAttribute()
                        model.category_id = it.categoryId
                        model.itemId = it.itemId
                        model.id = it.orderItemId
                        model.isDestroy = it.isDestroy
                        model.isEdited = it.isEdited
                        orderItemsAttributeList.add(model)

                    }
                }
            }

        }



        LogUtil.logE(
            "removeItemDine",
            "removeItemDineInList  ${removeItemDineInList.size}  checkUpdate ${isOrderUpdate}"
        )
        if (removeItemDineInList.isNotEmpty()) {
            orderItemsAttributeList = addDestroyedItemsinDinein(orderItemsAttributeList)
        }


        return orderItemsAttributeList


    }

    private fun orderItemTaxesAttributes(
        items: TbItem, orderId: Int? = null
    ): List<OrderItemTaxesAttribute> {


        val orderItemTaxesAttributeList: ArrayList<OrderItemTaxesAttribute> = arrayListOf()
        items.taxes?.forEach { tax ->

            if (tax.isActive) {

                val orderItemTaxesAttribute = OrderItemTaxesAttribute()



                orderItemTaxesAttribute.isDefault = tax.isDefault
                orderItemTaxesAttribute.isTaxRemoved = true
                orderItemTaxesAttribute.name = tax.name.toString()
                orderItemTaxesAttribute.rate = tax.rate

                if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {

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


                var total_price = 0.0
                if (items.price != 0.0) {
                    total_price = (items.price - items.discountPrice) * items.itemQuantity
                }
                items.modifiers.forEach { mod ->
                    total_price += mod.price * mod.itemQuantity
                }
                if (tax.taxType == "Percentage") {
                    val itemTaxPrice = (tax.rate * total_price) / 100
                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble(itemTaxPrice)
                } else {

                    val ss = tax.rate * items.itemQuantity

                    orderItemTaxesAttribute.taxTotalAmount = MethodUtils.roundOffAmountDouble((ss))
                }




                orderItemTaxesAttribute.taxType = tax.taxType.toString()
                orderItemTaxesAttributeList.add(orderItemTaxesAttribute)
            }
        }


        return orderItemTaxesAttributeList
    }

    fun orderItemModifierAttributes(
        item: TbItem, terminalId: Int
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> = arrayListOf()

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                if (prefProvider.getValueboolean(
                        DINE_IN_UPDATE, false
                    ) && it.orderModifierId != null
                ) id = it.orderModifierId
                name = it.name
                price = it.price
                modifier_id = it.id
                order_item_id = item.orderItemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                it.modifierSetId?.let { modifier_set_id = it }
                quantity = it.itemQuantity
                order_item_taxes_attributes = orderModifierTaxesAttributes(item, it, terminalId)
                modifier_quantity = it.modifier_quantity
                _destroy = it._destroy
            }
            orderItemModifierAttributeList.add(orderItemModifierAttribute)
        }

        return orderItemModifierAttributeList
    }

    private fun orderModifierTaxesAttributes(
        items: TbItem, modifier: Modifier, terminalId: Int
    ): List<OrderModifierTaxesAttribute> {

        val orderItemTaxesAttributeList: ArrayList<OrderModifierTaxesAttribute> = arrayListOf()

        modifier.orderItemTaxes.forEach { tax ->
            val orderModifierTaxesAttribute = OrderModifierTaxesAttribute()/*  if (isUpdateOrder && tax.orderTaxId != null)
                  orderModifierTaxesAttribute.id = tax.orderTaxId
    */
            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
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

    fun clockOut() {
        _showClockOutProgress.value = Event(true)
        val data = HashMap<String, String>()
        data["employee_id"] = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1).toString()
        data["terminal_id"] = prefProvider.getValueInt(Constants.TERMINAL_ID, -1).toString()

        viewModelScope.launch {
            val resource = posRepository.employeeClockOut(data)
            when (resource.status) {
                Status.SUCCESS -> {
                    prefProvider.setValueboolean(Constants.IS_CLOCKOUT, false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {


                                _clockOut.value = Event(it.message)
                                _showClockOutProgress.value = Event(false)
                            }
                        } else {
                            _snackbarText.value = Event(resource.message.toString())
                            _showClockOutProgress.value = Event(false)
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showClockOutProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showClockOutProgress.value = Event(true)
                }
            }
        }
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

        Log.e(TAG, "totalDiscountDineIn  ${totalDiscount}")
        val orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
        var ttotalDiscount = totalDiscount
//        if (BuildConfig.DEBUG == false) {
//            ttotalDiscount = cartModel.discountPrice + totalDiscount
//        } else {
        ttotalDiscount = totalDiscount
//        }

        Log.e(TAG, "ttotalDiscount:  ${ttotalDiscount}")
        LogUtil.logE(TAG, "getCartmodelId  ${cartModel.orderId}")
        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()
            id = if (cartModel.orderId != null && cartModel.orderId != 0) {
                cartModel.orderId
            } else {
                null
            }

            employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            note = cartModel.note
            openOrderType = "DineIn"
            orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
            orderTypeName = prefProvider.getValue(ORDER_TYPE_NAME, DINE_IN)
            tax_bifurcation_data = Gson().toJson(cartModel.taxlistDynamic)
            taxEnabled = true
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            totalAmount = MethodUtils.roundOffAmountDouble(totalPrice)
            totalDiscount = MethodUtils.roundOffAmountDouble(ttotalDiscount)
            totalServiceCharges = totalServiceCharge
            totalTaxAmount = totalTax
            orderItemsAttributes = dineInOrderItemAttributed(cartModel)
            offlineId = null
            deletedGuestItems = cartModel.listOfItemRemoved.toCollection(arrayListOf())
            //            paymentAttributes =
//                paymentAttributes(
//                    cartModel,
//                    totalPrice,
//                    subTotalPrice,
//                    totalServiceCharge,
//                    totalTax,
//                    totalDiscount, 0.0
//                )

            orderServiceChargesAttributes = orderServiceChargesAttributes(cartModel, subTotalPrice)

            guestsAttributes = getGuestsAttributes(cartModel)


            /*var listTbItem: ArrayList<TbItem> = arrayListOf()
            for (i in 0 until cartModel.dineInList?.size!!) {
                listTbItem.addAll(cartModel.dineInList!!.get(i).items)
            }
            */


        }


        return OrderRequestModel(false, orderModel)


    }

    private fun addDestroyedItemsinDinein(cartModel: ArrayList<OrderItemsAttribute>): ArrayList<OrderItemsAttribute> {

        if (removeItemDineInList.isNotEmpty()) {
            removeItemDineInList.forEach {
                val orderItemsAttribute = OrderItemsAttribute()
                orderItemsAttribute.category_id = it.categoryId
                if (it.orderItemId != null) orderItemsAttribute.id = it.orderItemId
                orderItemsAttribute.isDestroy = it.isDestroy
                orderItemsAttribute.isEdited = it.isEdited
                orderItemsAttribute.isFired = it.isFired
                orderItemsAttribute.itemId = it.itemId
                cartModel.add(orderItemsAttribute)

            }

        }

        removeItemDineInList.clear()


        return cartModel

    }

    private fun orderItemsAttributes(cartModel: CartModel): List<OrderItemsAttribute> {

        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> = arrayListOf()
        LogUtil.logE(TAG, "insideSize  ${cartModel.items?.size}")

        cartModel.items?.forEach { item ->

            val orderItemsAttribute = OrderItemsAttribute()

            if (item.orderItemId != null) orderItemsAttribute.id = item.orderItemId


            orderItemsAttribute.category_id = item.categoryId
            orderItemsAttribute.custom_item_id = item.id

            if (cartModel.reorder) {
                orderItemsAttribute.discountAmount = (item.discountPrice)
            } else {
                orderItemsAttribute.discountAmount = (item.discountPrice * item.itemQuantity)
            }


            orderItemsAttribute.discountType = item.discountType
            if (item.discountId != -1) orderItemsAttribute.discountId = item.discountId
            orderItemsAttribute.employeeId = cartModel.employeeID
            orderItemsAttribute.isCount = 0
            orderItemsAttribute.isEdited = item.isEdited
            orderItemsAttribute.isDestroy = item.isDestroy
            orderItemsAttribute.isPaid = item.isPaid
            orderItemsAttribute.isPrinted = true
            orderItemsAttribute.isTaxRemoved = false
            orderItemsAttribute.itemId = item.itemId
            orderItemsAttribute.is_manual_sales = item.isManualSales
            orderItemsAttribute.itemName = item.name
            orderItemsAttribute.note = item.note
            orderItemsAttribute.price = item.price
            orderItemsAttribute.quantity = item.itemQuantity
            orderItemsAttribute.terminalId = cartModel.terminalId
            orderItemsAttribute.timestamp = MethodUtils.randomOfflineId(
                prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
            )
            orderItemsAttribute.totalPrice =
                MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
            orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
            orderItemsAttribute.orderItemModifiersAttributes =
                orderItemModifierAttributes(item, cartModel.terminalId)
            orderItemsAttribute.isFired = item.isFired

            orderItemsAttribute.orderItemVariationAttributes = orderItemVariationAttributes(item)

            if (item.variationsAttributes.isNotEmpty()) {
                orderItemsAttribute.variationId = item.variationsAttributes[0].id
            }

            orderItemsAttributeList.add(orderItemsAttribute)
        }
        LogUtil.logE(
            TAG, "orderItemsAttributeList:  ${Gson().toJson(orderItemsAttributeList)}"
        )
        return orderItemsAttributeList
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

    fun deleteOrderAfterMarkup() {
        if (prefProvider.getValueboolean(IS_SYNC_MARKUP, false)) {
            syncMarkeup = false
            markupInventory()
        }


        viewModelScope.launch {
            decreaseOnGoingOrderCounter(false)
        }

    }

    suspend fun increaseOnGoingOrderCounter() {

        _showProgress.value = Event(true)

        val resource = posRepository.increaseOnGoingOrderCounter()
        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                _increaseCounter.value = Event(false)
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

    suspend fun decreaseOnGoingOrderCounter(b: Boolean) {
        _showProgress.value = Event(true)

        val resource = posRepository.decreaseOnGoingOrderCounter()
        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)

                if (b) {
                    logoutAPI()
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

    fun markupInventory() {


        if (prefProvider.getValue(ORDER_TYPE, "").isEmpty() && !syncMarkeup) {
            prefProvider.setValue(
                SYNC_TIME_STAMP, ""
            )
            syncMarkeup = true
            syncInventoryModule(true)
        }
    }


    fun syncInventoryModule(b: Boolean) {
        var needToUpdate = false
        _showProgress.value = Event(true)
        _syncDone.value = Event(false)
        viewModelScope.launch {
            val resource = posRepository.syncInventory(
                prefProvider.getValueInt(TERMINAL_ID, -1), prefProvider.getValue(
                    SYNC_TIME_STAMP, ""
                )
            )
            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("SyncInventory", "SyncSuccess")

                    resource.data.let { response ->
                        if (response?.status == 200) {
                            Log.d("BINGE", "syncInventoryModule: START")
                            _showProgress.value = Event(false)
//                            posRepository.saveDatabase(response)

                            val mData = response.data
                            val mCategory = mData.categories
                            val categoryModelList = ArrayList<TbCategory>()
                            val inventoryModelList = ArrayList<TbItem>()

                            val modifierSetList = ArrayList<ModifierSet>()
                            val itemModifierSetList = ArrayList<ItemModifierSets>()

                            mCategory.forEach { category ->

                                if (category.name.lowercase() == "Manual Sales".lowercase()) {
                                    prefProvider.setValueInt(MANUAL_SALE_CATEGORY_ID, category.id)
                                }
                                val model = TbCategory().apply {
                                    createdAt = ""
                                    id = category.id
                                    active = category.active
                                    name = category.name
                                    sort = category.sort
                                    updatedAt = ""
                                    locationId = category.locationId
                                    item_ids = category.itemIds
                                    thumbImgUrl = category.thumbImgUrl
                                    originalImgUrl = category.originalImgUrl
                                    isDeleted = category.isDeleted
                                }
                                categoryModelList.add(model)
                                Log.d("TAG", "Sync getAllCategoryList: response of API : "+Gson().toJson(model))

                                if (category.name == GIFT_CARD && category.sort == 1){
                                    prefProvider.setValueboolean(Constants.GIFT_CARD_AT_FIRST,true)
                                }
                                if (category.name == Constants.GIFT_CARD){
                                    prefProvider.setValueInt(Constants.GIFT_CARD_SORT,category.sort)
                                }
                                if (category.name == Constants.DEFAULT_CATEGORY){
                                    prefProvider.setValueInt(Constants.DEFAULT_CATEGORY_SORT,category.sort)
                                }

                                /*if (posRepository.getAllSortNumbers().contains(category.sort) && !needToUpdate){
                                    needToUpdate = true
                                    if (prefProvider.getValueboolean(Constants.GIFT_CARD_AT_FIRST,false)) {
                                        posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+mCategory.size)
                                    } else {
                                        posRepository.updateSorting(Constants.GIFT_CARD_CATEGORY,prefProvider.getValueInt(Constants.GIFT_CARD_SORT,0)+mCategory.size)
                                        posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+mCategory.size+1)
                                    }
                                }*/
                                // if created new category by admin web panel
                                /*if (prefProvider.getValueboolean(Constants.GIFT_CARD_AT_FIRST,false)){
                                    posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+1)
                                }else{
                                    posRepository.updateSorting(Constants.GIFT_CARD_CATEGORY,prefProvider.getValueInt(Constants.GIFT_CARD_SORT,0)+1)
                                    posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+2)
                                }*/

                                category.items.forEach {
                                    if (it.name?.lowercase() == "Manual Sales".lowercase()) {
                                        prefProvider.setValueInt(MANUAL_SALE_ITEM_ID, it.id)
                                    }

                                    it.modifierSets.forEach { modifierset ->
                                        val itemModifierSets = ItemModifierSets().apply {
                                            itemId = it.id
                                            modifierSetId = modifierset.id!!
                                            minRequired = modifierset.min_required
                                            maxAllowed = modifierset.max_allowed
                                            isDeleted = modifierset.isDeleted
                                        }
                                        itemModifierSetList.add(itemModifierSets)
                                    }

                                    //new optimise code
                                    inventoryModelList.add(TbItem().convertToItem(it, category))
                                }


                            }


                            modifierSetList.addAll(mData.modifierSets)

                            delay(1000)

                            appDatabase.categoryDao().addAll(categoryModelList)

                            val listInventory: ArrayList<TbItem> = arrayListOf()
                            ThreadPoolManager.instance.executeTask {

                                inventoryModelList.forEachIndexed { index, it ->
                                    val item = posRepository.getSingleItem(it.itemId)

                                    if (item != null) {

                                        val model = TbItem().convertToItem1(it, item)

                                        listInventory.add(model)


                                    } else {
                                        listInventory.add(it)
                                    }


                                }


                                viewModelScope.launch {
                                    appDatabase.itemDao().addAllItem(listInventory)
                                }

                            }

                            val listModifierSet: ArrayList<ModifierSet> = arrayListOf()
                            ThreadPoolManager.instance.executeTask {

                                modifierSetList.forEach {

                                    val modifierSet = posRepository.getSingleModifier(it.id!!)

                                    if (modifierSet != null) {
                                        it.modifiers.forEach {
                                            modifierSet.modifiers.forEach { mod ->
                                                if (mod.id == it.id) {
                                                    mod.itemQuantity = it.itemQuantity
                                                    mod.name = it.name
                                                    mod.price = it.price
                                                    mod.isDeleted = it.isDeleted
                                                    mod.isChecked = it.isChecked
                                                    mod.sort = it.sort

                                                }
                                            }
                                        }

                                        val model = TbItem().convertToModifier(it, modifierSet)

                                        listModifierSet.add(model)

                                    } else {
                                        listModifierSet.add(it)
                                    }

                                }

                                viewModelScope.launch {
                                    appDatabase.modifierSetDao().addAll(listModifierSet)
                                }
                            }



                            appDatabase.itemModifierSetsDao().addAll(itemModifierSetList)
                            appDatabase.optionSetDao().addAll(mData.optionSets)

                            prefProvider.setValue(SYNC_TIME_STAMP, response.data.timeStamp)

                            if (syncMarkeup) {
                                syncMarkeup = false
                                prefProvider.setValueboolean(IS_SYNC_MARKUP, false)
                            }
                            Log.d("BINGE", "syncInventoryModule: END")
                        } else {
                            _tableStatus.value = response?.let { Event(it.message) }
                        }

                        if (!b) syncSettingModule()

                    }
                }

                Status.ERROR -> {
                    Log.e("SyncInventory", "SyncError")
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    Log.e("SyncInventory", "SyncLoading")
                    _showProgress.value = Event(true)
                }
            }

        }
    }


    fun syncSettingModule() {
        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {
                    Log.d("BINGE", "syncSettingModule: START")
                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            resource.data?.let { it ->
                                if (it.settingData.data.teamRoles.isNotEmpty()) {
                                    posRepository.addTeamRoleFromDb(it.settingData.data.teamRoles)
                                    rolePermission.findCurrentUserRoleAndSave(it.settingData.data.teamRoles)
                                    _checkCashDrawerPermission.value = true
                                } else {

                                    ThreadPoolManager.instance.executeTask {

                                        rolePermission.findCurrentUserRoleAndSave(
                                            appDatabase.teamRoleDao().allRoleList()
                                        )
                                    }


                                }

                                if (it.settingData.data.isMasterTeminal) {

                                    prefProvider.setValueboolean(
                                        Constants.CHECK_QUEUE_CANCEL, false
                                    )
                                    prefProvider.setValueboolean(Constants.IS_MASTER_TERMINAL, true)
                                } else {
                                    prefProvider.setValueboolean(
                                        Constants.IS_PRINTER_QUEUE_STARTS, false
                                    )
                                    prefProvider.setValueboolean(
                                        Constants.IS_MASTER_TERMINAL, false
                                    )
                                }

                                val intent = Intent()
                                intent.action = Constants.MASTER_TEMINAL_CHANGED
                                MainApplication.getInstance()?.baseContext?.sendBroadcast(intent)


                                try {

                                    if (it.settingData.data.logo != null) {
                                        if (it.settingData.data.logo.logoUrl.isNotEmpty() && !prefProvider.getValue(
                                                Constants.VENUE_LOGO_URL, ""
                                            ).equals(it.settingData.data.logo.thumb.thumbUrl)
                                        ) {
                                            val policy: StrictMode.ThreadPolicy =
                                                StrictMode.ThreadPolicy.Builder().permitAll()
                                                    .build()

                                            StrictMode.setThreadPolicy(policy)

                                            val bitmap =
                                                getBitmapFromURL(it.settingData.data.logo.thumb.thumbUrl)
                                            var baseBitmap =
                                                bitmap?.let { it1 -> encodeTobase64(it1) }
                                            if (baseBitmap?.isNotEmpty() == true) {
                                                Log.d(TAG, "syncSettingModule: " + baseBitmap)
                                                baseBitmap?.let { it1 ->
                                                    prefProvider.setValue(
                                                        VENUE_LOGO, it1
                                                    )
                                                }
                                            }
                                            prefProvider.setValue(
                                                Constants.VENUE_LOGO_URL,
                                                it.settingData.data.logo.thumb.thumbUrl
                                            )
                                        }


                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                prefProvider.setValue(
                                    PAX_SERIAL_NO, it.settingData.data.SerialNo ?: ""
                                )
                                prefProvider.setValue(
                                    PAX_TERMINAL_ID, it.settingData.data.PAXTerminalID ?: ""
                                )

                                prefProvider.setValue(
                                    BUSINESS_NAME, it.settingData.data.businessName
                                )
                                prefProvider.setValue(
                                    SYSTEM_TIMEZONE, it.settingData.data.timeZone
                                )
                                prefProvider.setValue(
                                    BUSINESS_PHONE_NO, it.settingData.data.phoneNumber
                                )
                                if (it.settingData.data.address != null) {
                                    prefProvider.setValue(
                                        BUSINESS_ADDRESS, it.settingData.data.address
                                    )
                                }
                                prefProvider.setValueboolean(
                                    IS_PRINTER_QUEUE_ENABLE,
                                    it.settingData.data.isPrinterQueueEnable
                                )

                                prefProvider.setValueboolean(
                                    CUSTOMER_SIGN_REQUIRED_ON_CD,
                                    it.settingData.data.customer_sign_required_on_cd
                                )

                                prefProvider.setValueboolean(
                                    SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY,
                                    it.settingData.data.show_cash_credit_price_on_customer_display
                                )

                                prefProvider.setValue(
                                    BUSINESS_WEBSITE, it.settingData.data.businessWebsite.toString()
                                )
                                prefProvider.setValue(
                                    REPORT_START_TIME, it.settingData.data.report_start_time
                                )
                                prefProvider.setValue(
                                    REPORT_END_TIME, it.settingData.data.report_end_time
                                )

                                prefProvider.setValueboolean(
                                    SERVICECHARGE_TAKEOUT_OPENORDER,
                                    it.settingData.data.service_charge_enable
                                )
                                prefProvider.setValueboolean(
                                    SERVICECHARGE_DINEIN_ORDER,
                                    it.settingData.data.enable_dine_in_service_charge
                                )
                                prefProvider.setValueboolean(
                                    LOCK_SCREEN_TRANSACTION,
                                    it.settingData.data.lock_screen_after_each_transaction
                                )
                                prefProvider.setValueboolean(
                                    DINEIN_FLOORPLAN_SHOW_TABLENAME,
                                    it.settingData.data.show_table_name
                                )


                                if (prefProvider.getValueboolean(
                                        ONLY_SHOW_PRICE_GREATER_THAN_ZERO, false
                                    ) != it.settingData.data.only_show_price_greater_than_zero
                                ) {
                                    _syncInventroyForPriceChange.value = Event(true)

                                }



                                prefProvider.setValueboolean(
                                    ONLY_SHOW_PRICE_GREATER_THAN_ZERO,
                                    it.settingData.data.only_show_price_greater_than_zero
                                )
                                prefProvider.setValueboolean(
                                    ORDER_NUMBER_STARTING_FROM_ONE,
                                    it.settingData.data.order_number_starting_from_one
                                )
                                posRepository.addCashDiscountsFromDb(it.settingData.data.cash_discounts)
//                                taxServiceChargeRepository.deleteTaxFromDb()
                                if (it.settingData.data.taxes.isNotEmpty()) {
                                    taxServiceChargeRepository.addAllTaxDatabase(it.settingData.data.taxes)
                                }
//                                posRepository.deleteNotesFromDb()
                                posRepository.addAllNotesDatabase(it.settingData.data.notes)
//                                tipDiscountRepository.deleteDiscountsFromDb()
                                tipDiscountRepository.addDiscount(it.settingData.data.discounts)

                                /* serviceChargesList.clear()
                                 serviceChargesList = it.data.service_charges.toCollection(
                                     arrayListOf()
                                 )*/

                                if (it.settingData.data.service_charges.isNotEmpty()) {
                                    taxServiceChargeRepository.deleteServiceChargesFromDb()
                                    taxServiceChargeRepository.addServiceCharges(it.settingData.data.service_charges)
                                }
//                                posRepository.deleteTerminalsFromDb()
                                posRepository.addTerminalsDatabase(it.settingData.data.terminals)
//                                tipDiscountRepository.deleteTipsFromDb()
                                tipDiscountRepository.addTips(it.settingData.data.tip_settings)
//                                posRepository.deleteCustomerReceiptSettingsFromDb()
                                posRepository.addCancelOrderReasonFromDb(it.settingData.data.cancelOrderReasons)
                                posRepository.addWastageReasonInDb(it.settingData.data.wastageReasons)
//                                posRepository.deleteCustomerPrinters()
//                                posRepository.deleteKitchenPrinters()
                                posRepository.addKitchenPrinter(it.settingData.data.printers.kitchenPrinterList)

                                val custList = it.settingData.data.printers.customerPrinterList
                                custList.forEach {
                                    it.name = it.name.ifEmpty { "" }
                                    it.modalName = it.modalName.ifEmpty { "" }
                                }
                                posRepository.addCustomerPrinter(custList)
                                it.settingData.data.customerReceipt?.let { it1 ->
                                    posRepository.addCustomerReceiptSettings(
                                        it1
                                    )
                                }
                                posRepository.deleteKitchenReceiptSettingsFromDb()
                                it.settingData.data.kitchenReceipt?.let { it1 ->
                                    posRepository.addKitchenReceiptSettings(
                                        it1
                                    )
                                }
//                                posRepository.deleteLoyaltyProgramFromDb()
                                posRepository.addLoyaltyProgramFromDb(it.settingData.data.loyaltyPrograms)
//                                posRepository.deleteSurcharge()
                                posRepository.addCashDiscountsFromDb(it.settingData.data.cash_discounts)
                                posRepository.deleteEODReportSettings()
                                it.settingData.data.shift_report_configuration?.let { it1 ->
                                    posRepository.addEODReportSettings(
                                        it1
                                    )
                                }

                                if (it.settingData.data.loyaltyPrograms.isNotEmpty()) {
                                    it.settingData.data.loyaltyPrograms.forEach {
                                        if (it.isEnable && !it.isDeleted) {
                                            prefProvider.saveActiveLoyaltyData(it)
                                        }
                                    }
                                }
                                if (it.settingData.data.cash_discounts.isNotEmpty()) {
                                    it.settingData.data.cash_discounts.forEach {
                                        if (it.is_active) {
                                            prefProvider.setValue(
                                                CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, it.amount_type
                                            )
                                            prefProvider.setValue(
                                                CASH_DISCOUNT_SURCHARGE_RATE,
                                                it.rate_or_amount.toString()
                                            )
                                        }
                                    }
                                }

//                                posRepository.deleteTeamRoleFromDb()
//                                posRepository.addTeamRoleFromDb(it.data.teamRoles)
//                                posRepository.deleteAllEmployee()
                                posRepository.employeeListAddAllFromSeeting(it.settingData.data.employee)
//                                rolePermission.findCurrentUserRoleAndSave(it.data.teamRoles)
//                                posRepository.deleteOrderTypeFromDb()
                                posRepository.addOrderType(it.settingData.data.orderTypes)
                                posRepository.addAllCountryList(it.settingData.data.phoneCountrylist)
                                posRepository.addTimeZones(it.settingData.data.time_zone_options)
                                posRepository.addBusinessDetails(TbBusinessDetails().apply {
                                    id = prefProvider.getLocationId()
                                    business_name = it.settingData.data.businessName
                                    business_website = it.settingData.data.businessWebsite
                                    phone_number = it.settingData.data.phoneNumber
                                    phone_number_1_country =
                                        it.settingData.data.phone_number_1_country.toString()
                                    phone_number_2_country =
                                        it.settingData.data.phone_number_2_country.toString()
                                    phone_number_2 = it.settingData.data.phoneNumber2.toString()
                                    time_zone = it.settingData.data.business_time_zone.toString()
                                    customer_contact_email =
                                        it.settingData.data.customerContactEmail.toString()
                                    businessAddress = listOf(it.settingData.data.business_address)
                                })



                                it.settingData.data.terminals.forEach { terminal ->
                                    if (terminal.id == prefProvider.getValueInt(
                                            Constants.TERMINAL_ID, 0
                                        )
                                    ) {
                                        prefProvider.setValueboolean(
                                            ONLINE_ORDER_ENABLE,
                                            terminal.enabled_for_receiving_web_order!!
                                        )
                                        _enableOnlineOrder.value = Event(true)
                                    }
                                }
                                _callCashDiscount.value = Event(true)

                                prefProvider.setValue(Constants.MAGENSA_SETTINGS, "")

                                if (it.settingData.data.magensaSettings.isNotEmpty()) {
                                    prefProvider.setValue(
                                        Constants.MAGENSA_SETTINGS,
                                        Gson().toJson(it.settingData.data.magensaSettings[0])
                                    )
                                } else prefProvider.setValue(Constants.MAGENSA_SETTINGS, "")

                                if (it.settingData.data.shift_report_configuration != null) {
                                    prefProvider.setValue(
                                        Constants.SHIFT_REPORT_SETTINGS,
                                        Gson().toJson(it.settingData.data.shift_report_configuration)
                                    )
                                } else prefProvider.setValue(
                                    Constants.SHIFT_REPORT_SETTINGS, ""
                                )



                                MainApplication.getInstance()?.let { it1 ->
                                    Pref.setValue(
                                        it1, Constants.MAGENSA_SETTINGS1, ""
                                    )
                                }

                                if (it.settingData.data.magensaSettings.isNotEmpty()) MainApplication.getInstance()
                                    ?.let { it1 ->
                                        Pref.setValue(
                                            it1,
                                            Constants.MAGENSA_SETTINGS1,
                                            Gson().toJson(it.settingData.data.magensaSettings[0])
                                        )
                                    }

                            }
                            _showProgress.value = Event(false)
                            prefProvider.setValueboolean(Constants.SYNC_DATA, true)
                            prefProvider.setValue(
                                SYNC_SETTING_TIME_STAMP, venueDetailsResponse.settingData.timeStamp
                            )
                            _syncDone.value = Event(true)

                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                    Log.d("BINGE", "syncSettingModule: END")
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

        val request: Request = Request.Builder().url(url).build()

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
                        prefProvider.setValue(VENUE_LOGO, base64)

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
    }


    fun createCart(cartList: ArrayList<CartModel>): ArrayList<CartModel> {
        if (cartList.isEmpty()) {
            val model = CartModel()
            model.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            model.terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            model.orderType = prefProvider.getValue(ORDER_TYPE, "")
            model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            model.serviceCharge = serviceChargesList
            // model.orderTypeId = 1
            ordertypelist.forEach {
                if (it.name.lowercase() == prefProvider.getOrderTypeName(
                        ORDER_TYPE_NAME, DEFAULT_ORDER
                    ).lowercase()
                ) {
                    model.orderTypeId = it.id
                }
            }
            model.items = null
            cartList.add(0, model)
            LogUtil.logE(TAG, "CartIsEmpty::")
            return cartList
        }

        return cartList
    }

    fun itemCalculationForDineInPayment(
        cartModel: CartModel,
        txtTotal: AppCompatTextView,
        context: Context,
        model: GuestPaymentCalculationModel,
        isGuestPayment: Boolean
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
        if (cartModel.orderType == DINE_IN) {
            if (isGuestPayment) {

                subTotalPrice = model.subTotal
                totalTax = model.tax
                totalServiceCharge = model.serviceCharge
                totalDiscount = model.totalDiscount
                order_note = cartModel.note

                totalServiceCharge = model.serviceCharge
                // serviceChargeCalculationModel(cartModel)
                subTotalPrice -= model.totalDiscount


                var finalTotal = 0.0
                finalTotal = model.total
                cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                //loyalty point and price calculation
                amountToBePaid = finalTotal

                Log.e("checkDineInFinalAmt", "finalTotal:  ${finalTotal}")
                totalPrice = MethodUtils.roundOffAmountDouble(finalTotal)

                if (MethodUtils.isEnableCashDiscount(context)) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalPrice, prefProvider, context
                    )

                    //   cashdiscountAmount = model.cashDiscount

                } else {
                    cashdiscountAmount = 0.0
                }

                Log.e("checkDineInFinalAmt", "checkCashDiscountAmt:  ${cashdiscountAmount}")
                val nf: NumberFormat = NumberFormat.getNumberInstance()
                nf.maximumFractionDigits = 2
                val rounded: String = nf.format(cashdiscountAmount)
                cashdiscountAmount = rounded.toDouble()
                MethodUtils.setPriceTextView(txtTotal, model.total)


            } else {
                LogUtil.logE(TAG, "NotDineInGuest")
                subTotalPrice = model.subTotal
                totalTax = model.tax
                totalServiceCharge = model.serviceCharge
                totalDiscount = model.totalDiscount
                order_note = cartModel.note
                totalServiceCharge = model.serviceCharge

                var finalTotal = 0.0
                finalTotal = model.total
                cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                //loyalty point and price calculation
                amountToBePaid = finalTotal
                totalPrice = finalTotal

                LogUtil.logE(TAG, "newDAstotalPrice  ${totalPrice}")/*if (selectedCustomer == null) {
                    totalPrice = amountToBePaid

                } else {
                    redeemLoyaltyInfo.getAmountToBePaid()?.let {
                        totalPrice = it
                    }
                }*/

                if (MethodUtils.isEnableCashDiscount(context)) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalPrice, prefProvider, context
                    )
                } else {
                    cashdiscountAmount = 0.0
                }
                val nf: NumberFormat = NumberFormat.getNumberInstance()
                nf.maximumFractionDigits = 2
                val rounded: String = nf.format(cashdiscountAmount)
                cashdiscountAmount = rounded.toDouble()
                Log.d(TAG, "itemCalculationForDineInPayment: " + cashdiscountAmount)


                MethodUtils.setPriceTextView(txtTotal, model.total)


            }
        } else {

            if (cartModel.items?.isEmpty() == false) {


                val itemCount = cartModel.items?.size
                cartModel.items?.forEach { item ->
                    totalCount += item.itemQuantity

                    subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


//                    subTotalPrice += if (!item.isManualSales) {
//                        (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)
//                    } else {
//                        (item.price * item.itemQuantity) - item.discountPrice
//                    }

                    taxCalculation(item, cartModel.discountPrice / itemCount!!)

                    item.modifiers.forEach {
                        subTotalPrice += (it.price * it.itemQuantity)

                    }
                }

                order_note = cartModel.note
                subTotalPrice -= cartModel.discountPrice

                if (subTotalPrice < 0) {
                    subTotalPrice = 0.0
                }
                serviceChargeCalculationModel(cartModel)



                totalDiscount += cartModel.discountPrice

                cartModel.items!!.forEach {
                    totalDiscount += (it.discountPrice * it.itemQuantity)
                }

                var finalTotal = 0.0
                finalTotal = (subTotalPrice + totalTax + totalServiceCharge)



                cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
                //loyalty point and price calculation
                amountToBePaid = finalTotal
                if (selectedCustomer == null) {
                    totalPrice = amountToBePaid/* MethodUtils.setPriceTextView(
                         txtTotalAmount,
                         amountToBePaid
                     )*/
                } else {/* checkAppliedLoyaltyProgram(
                         selectedCustomer,
                         amountToBePaid,
                         txtTotalAmount
                     )*/
                    redeemLoyaltyInfo.getAmountToBePaid()?.let {
                        totalPrice = it
                    }
                }

                if (MethodUtils.isEnableCashDiscount(context)) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalPrice, prefProvider, context
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                val nf: NumberFormat = NumberFormat.getNumberInstance()
                nf.maximumFractionDigits = 2
                val rounded: String = nf.format(cashdiscountAmount)
                cashdiscountAmount = rounded.toDouble()
                Log.e("amountToBePaid", "" + totalPrice)
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

    var isLoading = MutableLiveData<Boolean>()

    fun downloadFinished(value: Boolean) {
        isLoading.value = value
    }


    fun updateActiveOrderFlagClear() {

        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, false)
        prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, -1)
        prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_PAYMENT_ID, -1)
        prefProvider.setValue(Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID, "")
        prefProvider.setValue(Constants.IS_UPDATE_ORDER_OFFLINE_ID, "")
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)

    }

    fun unableToRemoveGuest(message: String = "") {
        _removeGuestSuccess.value = Event(message)
    }

    // To clear gift-card cart
    fun clearGiftCardCart() {
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValue(ORDER_TYPE, "")
        prefProvider.setValue(ORDER_TYPE_NAME, "")
        prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        clearCustomer()
        deleteCart()
    }

    // To clear customer if creating gift card
    fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        selectedCustomer = null
        assignCustomer = null
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        prefProvider.setValueboolean(LOYALTY_ADDED, false)
    }

}