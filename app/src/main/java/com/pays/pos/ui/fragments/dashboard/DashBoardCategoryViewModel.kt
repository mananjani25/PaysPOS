package com.pays.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
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
import androidx.paging.cachedIn
import com.google.gson.Gson
import com.pays.pos.MainApplication
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.ActivePaymentGateway
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.CartModelBackup
import com.pays.pos.data.entities.CashDiscountModel
import com.pays.pos.data.entities.CategoryWithInventory
import com.pays.pos.data.entities.ItemModifierSets
import com.pays.pos.data.entities.LoyaltyProgramsModel
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.entities.OrderTypeBackup
import com.pays.pos.data.entities.RedeemLoyaltyInfo
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbBusinessDetails
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCategory
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbDynamicPaymentRecords
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbLabelPrinterSettings
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.DineInOrderDetailAttributes
import com.pays.pos.data.model.GuestPaymentCalculationModel
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.model.requestModel.CustomerAttributes
import com.pays.pos.data.model.requestModel.GuestItemsAttributes
import com.pays.pos.data.model.requestModel.GuestsAttributes
import com.pays.pos.data.model.requestModel.OrderAttributeRequestModel
import com.pays.pos.data.model.requestModel.OrderItemModifierAttribute
import com.pays.pos.data.model.requestModel.OrderItemTaxesAttribute
import com.pays.pos.data.model.requestModel.OrderItemVariationAttribute
import com.pays.pos.data.model.requestModel.OrderItemsAttribute
import com.pays.pos.data.model.requestModel.OrderModifierTaxesAttribute
import com.pays.pos.data.model.requestModel.OrderRequestModel
import com.pays.pos.data.model.requestModel.OrderServiceChargesAttribute
import com.pays.pos.data.model.requestModel.PaymentAttributes
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.OnlineOrderNotificationCount
import com.pays.pos.data.model.responseModel.OrderTypeResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.model.responseModel.allOrders.AllOrdersCountResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ADD
import com.pays.pos.data.remote.Constants.BASE_URL_NEW
import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.BUSINESS_NAME
import com.pays.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.pays.pos.data.remote.Constants.BUSINESS_WEBSITE
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_RATE
import com.pays.pos.data.remote.Constants.CUSTOMER_SIGN_REQUIRED_ON_CD
import com.pays.pos.data.remote.Constants.DEFAULT_ORDER
import com.pays.pos.data.remote.Constants.DEJAVOO_AUTH_KEY
import com.pays.pos.data.remote.Constants.DEJAVOO_AUTH_TOKEN
import com.pays.pos.data.remote.Constants.DEJAVOO_REGISTER_ID
import com.pays.pos.data.remote.Constants.DEJAVOO_TPN
import com.pays.pos.data.remote.Constants.DELETE
import com.pays.pos.data.remote.Constants.DINEIN_FLOORPLAN_SHOW_TABLENAME
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.GIFT_CARD
import com.pays.pos.data.remote.Constants.IS_LAST_ITEM_DELETE
import com.pays.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.pays.pos.data.remote.Constants.IS_SYNC_MARKUP
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER
import com.pays.pos.data.remote.Constants.LOCK_SCREEN_TRANSACTION
import com.pays.pos.data.remote.Constants.LOYALTY_ADDED
import com.pays.pos.data.remote.Constants.MANUAL_SALE_CATEGORY_ID
import com.pays.pos.data.remote.Constants.MANUAL_SALE_ITEM_ID
import com.pays.pos.data.remote.Constants.MAX_ITEM_QUANTITY
import com.pays.pos.data.remote.Constants.ONLINE_ORDER_ENABLE
import com.pays.pos.data.remote.Constants.ONLY_SHOW_PRICE_GREATER_THAN_ZERO
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.data.remote.Constants.PAX_SERIAL_NO
import com.pays.pos.data.remote.Constants.PAX_TERMINAL_ID
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.REPORT_END_TIME
import com.pays.pos.data.remote.Constants.REPORT_START_TIME
import com.pays.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.pays.pos.data.remote.Constants.SERVICECHARGE_TAKEOUT_OPENORDER
import com.pays.pos.data.remote.Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY
import com.pays.pos.data.remote.Constants.SYNC_SETTING_TIME_STAMP
import com.pays.pos.data.remote.Constants.SYNC_TIME_STAMP
import com.pays.pos.data.remote.Constants.SYSTEM_TIMEZONE
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.UPDATE
import com.pays.pos.data.remote.Constants.VALOR_APP_ID
import com.pays.pos.data.remote.Constants.VALOR_APP_KEY
import com.pays.pos.data.remote.Constants.VALOR_CHANNEL_ID
import com.pays.pos.data.remote.Constants.VALOR_EPI
import com.pays.pos.data.remote.Constants.VENUE_LOGO
import com.pays.pos.data.remote.NetworkConnectionInterceptor
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.DineInTableAdapter
import com.pays.pos.ui.fragments.transactions.TransactionViewModel
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.Pref
import com.pays.pos.utils.TimeFormatUtils
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.pays.pos.utils.workmanager.ThreadPoolManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import javax.inject.Inject
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.math.log


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

    private var syncMarkeup: Boolean = false
    private var isGuestPay: Boolean = false
    var dineInHeaderPosition: Int = 0
    var currentSelectedHeaderDineIn: Int = 0
    var listItems: ArrayList<TbCartItem> = arrayListOf()
    var dineInSelectedItemHeaderPos: Int = 0
    var selectedItemPositionDine: Int = 0
    val TAG = "DashBoardCateViewModel"
    var totalPrice: Double = 0.0
    var totalPriceUpdated = MutableLiveData<Double>()
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var isSelectCount = 1
    var nonCashAdj: Double = 0.0
    var totalServiceCharge = 0.0
    var cashdiscountAmount = 0.0
    var cashDiscountType = ""
    var totalDiscount = 0.0
    var wholetotalPrice = 0.0
    var tip = 0.0
    var order_note = ""

    /* This loggingOut variable is used to restrict the dialog which is shown after the user is logged out. the dialog is fetched from Customer's list api */
    var loggingOut = false
    var cartModel: CartModel? = null
    var manualCartOrderNote: String? = ""
    var currentCartItems: ArrayList<TbCartItem> = arrayListOf()
    var currentDineCartItems: ArrayList<TbCartItem> = arrayListOf()
    var duplicateCurrentCartItem: ArrayList<TbCartItem> = arrayListOf()

    /**
     * When item update is in progress , restrict other actions like switch guest
     */
    /*-----------Customer Loyalty------------*/
    val getBusinessData = posRepository.getBusinessData()
    val loyaltyPoints = taxServiceChargeRepository.loyaltyPointList()

    private val _loadCustomersList = MutableLiveData<Pair<Int, Boolean>>()
    val loadCustomersList: LiveData<Pair<Int, Boolean>> = _loadCustomersList

    private val _clickTakeOut = MutableLiveData<Event<Boolean>>()
    val clickTakeOut: LiveData<Event<Boolean>> = _clickTakeOut

    private val _physicalGiftCardCheck = MutableLiveData<Event<Int>>()
    val physicalcardexistsornot: LiveData<Event<Int>> = _physicalGiftCardCheck


    private val _earnedLoyaltyPoints = MutableLiveData<Event<Int>>()
    val earnedLoyaltyPoints: LiveData<Event<Int>> = _earnedLoyaltyPoints

    private val _updateCartFooterObservable = MutableLiveData<Event<Boolean>>()
    val updateCartFooterObservable: LiveData<Event<Boolean>> = _updateCartFooterObservable

    private val _isLoyaltyPointShowObservable = MutableLiveData<Event<Boolean>>()
    val isLoyaltyPointShowObservable : LiveData<Event<Boolean>> = _isLoyaltyPointShowObservable

    private val _changeCustDispSignInButtonTitle = MutableLiveData<String>()
    val changeCustDispSignInButtonTitle: LiveData<String> = _changeCustDispSignInButtonTitle

    public val removedCustomerFromManualSaleObs = MutableLiveData<Boolean>()

    private val _passcodeScreenActive = MutableLiveData<Boolean>()
    val passcodeScreenActive: LiveData<Boolean> = _passcodeScreenActive

    /*-----------Customer Loyalty------------*/


    private val _printOrderIdInStickyReceipt = MutableLiveData<Boolean>()
    val printOrderIdInStickyReceipt: LiveData<Boolean> = _printOrderIdInStickyReceipt

    val itemsFiredToTheKitchenSuccesfully = MutableLiveData<Boolean>()

    var isItemEditInProgress = false

    var isItemEditing = false

    /**
     * To check if removed last item from the cart
     */
    val lastItemRemoveFromCart = MutableLiveData<Pair<Boolean, Int>>()

    /* This variable is used to track the selected category, if this variable is not 0 then the category will be selected, it was added to solve BIS-4045 */
    var selectedCatetory: Int = 0

    /**
     * Cart Item modifiers Before Update
     */
    var cartItemModifiersBeforeUpdate: List<Modifier>? = null

    /**
     * Dine In
     */
    var dineInItemsBeforeUpdate = arrayListOf<TbCartItem>()
    var oldDineInItems: ArrayList<TbCartItem> = arrayListOf()
    var oldItemListIds: ArrayList<Int> = arrayListOf()
    var isDineInUpdate = false
    var dineInResult = MutableLiveData<Boolean>(false)
    var dineInResultCreateOrder = MutableLiveData<Boolean>(false)
    var orderRequestModel: OrderRequestModel? = null
    var orderAttributeRequestModel = OrderAttributeRequestModel()
    var dineInItemClickedFromCart = false
    var dineInAdapterBackup: DineInTableAdapter? = null

    //destroyed guests for dine in
    val destroyedDineGuestsList: ArrayList<DineInModel> = ArrayList()

    /**
     *  currentDineInItems keeps track of all dine in Items even if they are destroyed
     */
    var currentDineInItems = arrayListOf<TbCartItem>()

    val syncDineIn = MutableLiveData<Boolean>()
    var isUpaidReceiptPrinted = false

    var updateRequested = true
    var currentDestination = ""


    /** PRE AUTH **/
    val isPreAuthCartOpened = MutableLiveData<Boolean>(false)


    /**
     * Get SUNMI OS VERSION
     */
    fun getSunmiFrameWorkVersion() =
        prefProvider?.getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
            .toTypedArray()

    /***
     * PreAuth Payment Attribute retrieved from PAX "PRE AUTH" response
     */
    var paymentAttributes: PaymentAttributes? = null

    /**
     * Tracking main cart discount
     */
    var mainCartDiscount = 0.0
    var customCartUpdateDiscount = 0.0

    /***
     * Added to resolve BIS 1693 - Add discount issue
     */
    var currentTotalPrice = 0.0
    var clickedItemQuantity = 1
    var isFromKeyPad = false

    var latestUpdatedCartItem = 0
    var assignCustomer: TbCustomer? = null
    var orderItemDiscount = 0.0
    var selectedCustomer: TbCustomer? = null
    var activeLoyaltyProgram: LoyaltyProgramsModel? = null
    var redeemLoyaltyInfo: RedeemLoyaltyInfo = RedeemLoyaltyInfo()
    var paymentType: String = "cash"
    var taxDynamicList: ArrayList<TaxData> = arrayListOf()
    var destroyedList: ArrayList<TbItem> = arrayListOf()
    var destroyedCartItemsList: ArrayList<TbCartItem> = arrayListOf()
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

    var activeOrderTypeText: String = ""
    var activeOrderTypeName: String = ""
    var activeOrderTypeId: Int? = 0

    var openOrderUpdate: Boolean? = false
    private val _removeGuestSuccess = MutableLiveData<Event<String>>()
    val removeGuestSuccess: LiveData<Event<String>> = _removeGuestSuccess

    val noteTbCartItem: MutableLiveData<TbCartItem> = MutableLiveData<TbCartItem>()

    private val _latestDiscount = MutableLiveData<Double>()
    val latestDiscount: LiveData<Double> = _latestDiscount

    public val tipButtonOnCustomerDisplayClicked = MutableLiveData<Boolean>()

    public val tipErrorObservable = MutableLiveData<String>()

    var isUpdatedOnce = false

    /**
     * When Item in cart clicked
     * */
    var isCartItemClicked = false


    /*----------------------BIS-5469---------------------*/

    public val customerCardPrice = MutableLiveData<Double>()
    public val customerCashPrice = MutableLiveData<Double>()
    public val customerNormalPrice = MutableLiveData<Double>()

    /*----------------------BIS-5469---------------------*/


    /*-------------VALOR Payment gateway------------------ */
    public val takenTipUsingValor = MutableLiveData<Event<TransactionViewModel>>()

    /*-------------VALOR Payment gateway------------------ */

    /**
     * Tip has been added , Either from customer display or from checkoutFragment
     */
    val customerGivenTip = MutableLiveData<Boolean>(false)
    val customerGivenTipBefore = MutableLiveData<Boolean>(false)
    val applySurchargeOnTip = false
    val splitChanged = MutableLiveData<Int>(1)
    var paymentInProgress = MutableLiveData(false)
    var removeMainCart = MutableLiveData<Boolean>(false)
    var tipBeforeEnabled = false
    var tipRemovedObserver = MutableLiveData<Boolean>(false)

    /**
     * For amount wise split
     **/
    val isAmountWiseSplit = MutableLiveData<Boolean>(false)

    /**
     * For amount wise split
     **/
    val amountWiseSplit = MutableLiveData<Double>(0.0)

    var employeeGivenTip = false
    var totalTipAmount = 0.0
    var totalAmount = 0.0
    var finalAmount = 0.0
    var paymentTypeForTip = ""
    val processingTipForCard = MutableLiveData(false)

    /**
     * Fields used to check navigation from fragments
     */
    var fromAllOrderFragment = false
    var fromAllOrderFragmentUpdate = false
    var fromSaveOrderToAllOrders = false

    //order completed Home button clicked
    val orderCompleted = MutableLiveData<Boolean>()
    val orderCompletedCount = MutableLiveData(0)

    /**
     * Issue related to BIS-435
     */
    var cartFragmentRestarted = false

    /**
     * Field used to resolve multiple issues like cart going blank
     */
    val fragmentNeedToBeUpdated = MutableLiveData<Boolean>(false)

    /**
     * Field used to resolve multiple issues related dine in table
     */
    val dineInTableNeedToBeRestart = MutableLiveData<Boolean>(false)


    /**
     * This field used to resolve BIS-3473 issue - when we add same item with different modifier then its doesn't reflect in cart
     */
    val doesItemContainsModifiers = MutableLiveData<Boolean>()

    /**
     * resolved for manual cart item adding issue, i.e. the item was not getting added to the normal cart so we restarted the screen
     */
    var boldPosNeedToRefresh = false

    /* Below 4 variables are used as backup variables to solve the BIS-3973, when the cart's last item is deleted the the metadata is also getting removed, these variables will keep the metadata with them. */
    public var backupOrderId: Int? = null
    public var backupPaymentId: Int? = null
    public var backupPaymentOfflineId: String? = ""
    public var backupOrderOfflineId: String? = ""

    /**
     * BIS - 3500 issue resolved
     */
    val autoSyncEnabled = MutableLiveData<Boolean>()
    val disableCursor = MutableLiveData<Boolean>()

    //Fetch all Items from TBITEM
    val allInventoryItems = posRepository.getItemsList()

//    var customerCashAmount: String = ""

    val customerCardAmount = MutableLiveData<String>()
    val customerCashAmount = MutableLiveData<String>()
//    val cashAmount: LiveData<Event<String>> = _cashAmount

    public val changeAvailable = MutableLiveData<Event<String>>()

    fun setCollectMore(title: String) {
        changeAvailable.postValue(Event(title))
    }

    fun setTipErrorObservable(message: String) {
        tipErrorObservable.postValue(message)
    }

    /*------------Customer Loyalty----------*/
    fun setCustomerLoyaltyOnCustomerThankyouScreen(reward: Int) {
        _earnedLoyaltyPoints.postValue(Event(reward))
    }

    fun callUpdateCartFooter(value: Boolean) {
        _updateCartFooterObservable.postValue(Event(value))
    }

    fun isLoyaltyPointVisible(value: Boolean) {
        _isLoyaltyPointShowObservable.postValue(Event(value))
    }

    fun changeCustomerDispSignButtonTitle(value: String) {
        _changeCustDispSignInButtonTitle.postValue(value)
    }

    fun removedCustomerFromManualSale(value: Boolean) {
        removedCustomerFromManualSaleObs.postValue(value)
    }

    fun setPasscodeScreenActive(value: Boolean) {
        _passcodeScreenActive.postValue(value)
    }
    /*------------Customer Loyalty----------*/

    //Fetch all orders count
    fun allOrderCounts(
        startDate: String?,
        endDate: String?
    ): LiveData<Resource<AllOrdersCountResponse>> =
        posRepository.allOrderCounts(startDate, endDate)

    //all order types
    fun fetchAllOrderTypes() = posRepository.orderTypes()

    //fetch order types from server
    suspend fun fetchOrderTypesFromServer(): Resource<OrderTypeResponse> {
        return posRepository.fetchOrderTypesFromServer()
    }

    // Added to resolve Add Discount issue BIS-3547
    var discountNeedToUpdate = true
    var cartFooterNeedToBeUpdated = true

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    fun observeLatestCartModel(): LiveData<List<CartModel>> = posRepository.observeCartModel()

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

    /*-----------Customer Create----------------*/
    private val _createCustomerObservable = MutableLiveData<Pair<Boolean, OrderRequestModel?>>()
    val createCustomerObservable: LiveData<Pair<Boolean, OrderRequestModel?>> get() = _createCustomerObservable

    fun createCustomer(createCustomerRequestModel: CreateCustomerRequestModel, orderRequestModel: OrderRequestModel) {
        viewModelScope.launch {
            val result = posRepository.createCustomer(createCustomerRequestModel)
            when (result.status) {
                Status.SUCCESS -> {
                    _createCustomerObservable.postValue(Pair(true, orderRequestModel))
                }

                Status.ERROR -> {
                    _createCustomerObservable.postValue(Pair(true, null))
                    _snackbarText.value = Event(result.message ?: "Unable to sync customer")
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {}
            }
        }
    }

    fun getCustomerDetailsFromId(customerId: String): LiveData<TbCustomer> = posRepository.getCustomerDetailsByID(customerId)
    /*-----------Customer Create----------------*/

    fun setcheckedLoyaltyApply(isapply: Boolean, txtTotalAmount: AppCompatTextView? = null) {
        redeemLoyaltyInfo.needToApplyLoyalty = isapply
        if (txtTotalAmount != null) {
            redeemLoyaltyInfo.getAmountToBePaid()?.let {
                totalPrice = it


                if (prefProvider.getValue(Constants.ORDER_TYPE, "") != Constants.DINE_IN) {
                    MethodUtils.setPriceTextView(
                        txtTotalAmount, it
                    )
                }
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

    //    Added by Rahul for solving Discount issue
    public suspend fun getManualSaleFromCart(employee_id: Int): CartModel {
        return posRepository.getManualSaleFromCart(employee_id)
    }

    fun setCartModel(cartList: List<CartModel>) {
        this.cartModel = generateCombinedItems(cartList[0])
    }

    fun setUpdatedCartModel(updatedCartModel: CartModel) {
        this.cartModel = updatedCartModel
    }

    fun setCurrentCartItems(cartItems: List<TbCartItem>) {
        this.currentCartItems = cartItems.toCollection(ArrayList())
    }

    fun setLatestCartItemPosition(position: Int) {
        latestUpdatedCartItem = position
    }

    val serviceCharges = posRepository.serviceChargeList()

    /*
     * Returns active order types
     */
    val getOrderTypes = posRepository.getOrderTypes()

    /*
     * Returns all active or Deactivated order types
     */
    fun getAllOrderTypes(): List<TbOrderType> {
        return posRepository.getAllOrderTypes()
    }

    val activeLoyaltyProgramLiveData = posRepository.getActiveLoyaltyProgramFromDb()

    val taxList = posRepository.taxList()

    val discountList = posRepository.disocuntList()

    val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    val _removeLastItem = MutableLiveData<Event<Boolean>>()
    val removeLastItem: LiveData<Event<Boolean>> = _removeLastItem

    val updateCartFooter = MutableLiveData<Event<Boolean>>()

    val _syncProgressDialog = MutableLiveData<Event<Boolean>>()
    val syncProgressDialog: LiveData<Event<Boolean>> = _syncProgressDialog

    private val _showClockOutProgress = MutableLiveData<Event<Boolean>>()
    val showClockOutProgress: LiveData<Event<Boolean>> = _showClockOutProgress


    private val _enableOnlineOrder = MutableLiveData<Event<Boolean>>()
    val enableOnlineOrder: LiveData<Event<Boolean>> = _enableOnlineOrder

    private val _masterTerminal = MutableLiveData<Event<Boolean>>()
    val masterTeminalLiveData: LiveData<Event<Boolean>> = _masterTerminal

    private val _checkCashDrawerPermission = MutableLiveData<Boolean>()
    val checkCashDrawerPer: LiveData<Boolean> = _checkCashDrawerPermission

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _isGiftCardSold = MutableLiveData<Event<Boolean>>()
    val isGiftCardSold: LiveData<Event<Boolean>> = _isGiftCardSold

    private val _syncDone = MutableLiveData<Event<Boolean?>>()
    val syncDone: LiveData<Event<Boolean?>> = _syncDone


    private val _taxSyncDone = MutableLiveData<Event<Boolean?>>()
    val taxSyncDone: LiveData<Event<Boolean?>> = _taxSyncDone

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

    val _thankyouAmount = MutableLiveData<String>()

    val refreshLiveData = MutableLiveData<Boolean>()

    var barcodeFoundDbItemLiveData: LiveData<Resource<TbItem>>? = null

    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    fun getItemsbyId(itemId: Int) = posRepository.getItemsbyId(itemId)

    fun getItemByProductCode(productCode: String) = posRepository.getItemByProductCode(productCode)

    fun checkCategoryHideOrNot(id: Int) = posRepository.checkCategoryHideOrNot(id)


    fun getItemByCategoryId(id: Int) = posRepository.getItemByCategoryId(id)


    fun refreshCartFragment() {
        fragmentNeedToBeUpdated.postValue(true)
    }

    /*if (value) {
        *//* Uncomment the below code, if the customer Display is not refreshing everytime *//*
        reloadCustomerDisplay.postValue(value)
    }*/
    val reloadCustomerDisplay = MutableLiveData<Boolean>()
    fun reloadCustomerDisplay(value: Boolean) {
        /* Uncomment the below code, if the customer Display is not refreshing everytime */
//        reloadCustomerDisplay.postValue(true)
    }

    fun itemsByCat(id: Int): kotlinx.coroutines.flow.Flow<PagingData<TbItem>> = Pager(
        config = PagingConfig(
            pageSize = 12, enablePlaceholders = false, initialLoadSize = 12
        )
    ) {
        appDatabase.itemDao().getItemListByCategory(id)

    }.flow.cachedIn(viewModelScope)
    /* The above .cachedIn(viewModelScope) is added by Rahul to solve the, Attempt to collect twice from pageEventFlow issue. */

    /*
        fun getCartList(orderType:String,employee_Id: Int) : List<CartModel>{
            viewModelcartList.clear()
            viewModelcartList = arrayListOf()

            posRepository.getCartList(orderType,employee_Id)
        }

    */
    // observer for cart modification
    fun mAllWords(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {

        return posRepository.getCartList(orderType, employee_Id)


    }


    fun mAllWordsFlow(orderType: String, employee_Id: Int): Flow<List<CartModel>> {


        return posRepository.getCartListFlow(orderType, employee_Id)


    }

    fun getAllCartItems(orderType: String, employee_Id: Int): Flow<List<TbCartItem>> {
        return posRepository.getAllCartItems(orderType, employee_Id)
    }

    /*------------Customer Loyalty---------------*/
    suspend fun allCustomerList(): List<TbCustomer> {
        return posRepository.allCustomerList()
    }

    suspend fun fetchCustomerFromPhoneNumber(phoneNumber: String): List<TbCustomer?>? {
        return posRepository.fetchCustomerFromPhoneNumber(phoneNumber)
    }

    fun clickOnTakeOut() {
        Log.d(TAG, "dashboardCategoryViewModel: clickOnTakeOut()...")
        _clickTakeOut.postValue(Event(true))
    }
    /*------------Customer Loyalty---------------*/


    fun getAllDineInCartItems(orderType: String): Flow<List<TbCartItem>> {
        return posRepository.getAllDineInCartItems(orderType)
    }


    suspend fun orderTypeByName(orderTypeName: String): Int {
        return posRepository.orderTypeByName(orderTypeName)
    }

    fun getDineInCartItems(guestIndexForDineIn: Int): List<TbCartItem> {
        return posRepository.getDineInCartItems(guestIndexForDineIn)
    }

    suspend fun allSplit(): List<SplitDetailListModel> {
        return posRepository.allSplit()
    }

    fun updateDineInCartItemGuestDineInPositions(removedGuestIndex: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.updateDineInCartItemGuestDineInPositions(removedGuestIndex)
        }
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

    fun getManualSaleCartItems(orderType: String, employee_Id: Int): LiveData<List<TbCartItem>> {

        return posRepository.getManualSaleCartItems(orderType, employee_Id)

    }

    var serviceChargesList: ArrayList<TbServiceCharge> = arrayListOf()


    // store updated cart in database
    var currentCartIdWhenInserted = 0L
    fun addCartGetId(cartModel: CartModel) {
        var mCartModel = cartModel
        System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {

            var listItems: ArrayList<TbCartItem> = arrayListOf()
            cartModel.items?.forEach {
                listItems.add(TbCartItem().convertToCartItem(it, it))

            }
            Log.e(TAG, "checkConvertedItem: ${listItems.size}")

            for (i in 0 until listItems.size) {
                listItems.get(i).taxes?.let { it ->
                    for (j in 0 until it.size) {
                        mCartModel = taxBifurcationCalculationNew(
                            cartModel = mCartModel,
                            item = listItems.get(i),
                            type = ADD,
                            orderTaxID = false
                        )
                    }
                }

            }

            currentCartIdWhenInserted = posRepository.addItemCartGetId(mCartModel)!!
            destroyedList.clear()

        }

    }

    fun addCart(cartModel: CartModel) {
        var mCartModel = cartModel
        System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {

            try {

                var listItems: ArrayList<TbCartItem> = arrayListOf()
                cartModel.items?.forEach {
                    listItems.add(TbCartItem().convertToCartItem(it, it))

                }
                Log.e(TAG, "checkConvertedItem: ${listItems.size}")

                for (i in 0 until listItems.size) {
                    listItems.get(i).taxes?.let { it ->
                        for (j in 0 until it.size) {
                            mCartModel = taxBifurcationCalculationNew(
                                cartModel = mCartModel,
                                item = listItems.get(i),
                                type = ADD,
                                orderTaxID = false
                            )
                        }
                    }

                }

                if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN)
                    if (mCartModel.discountSelectdValue == null) {
                        mCartModel.discountSelectdValue = 0.0
                    }

                posRepository.addItemCart(mCartModel)
                destroyedList.clear()

            } catch (e: Exception) {
                Log.e("DINE IN CRASH", e.message.toString())
            }
        }

    }

    fun deleteCartModel(cartModel: CartModel) {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.deleteCartModel(cartModel)
        }
    }

    fun updateCartModel(cartModel: CartModel) {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.updateCartModel(cartModel)
        }
    }

    fun addItemToCartItems(tbCartItem: TbCartItem) {
//        CoroutineScope(Dispatchers.Default).launch { //the dispatcher was default, the code inside the dispatcher was not executing, so I(Rahul Sharma) changed it to Dispatcher.IO
        CoroutineScope(Dispatchers.IO).launch {

            posRepository.addItemToCart(tbCartItem)
            destroyedCartItemsList.clear()

            val currentTimeMillis = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = dateFormat.format(Date(currentTimeMillis))

            println("Current System Time in milliseconds: $currentTimeMillis")
            println("Formatted Time: $formattedTime + ${Gson().toJson(tbCartItem)}")
        }
    }

    fun addOrderItemsToCartItems(tbCartItem: List<TbCartItem>) {

        // runBlocking {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.addCartItemsList(tbCartItem)

            prefProvider.setValue(
                Constants.OLD_ITEM_BASE,
                Gson().toJson(appDatabase.cartDao().getAllCartItems())
            )


            if (isUpdatedOnce) {

                isUpdatedOnce = false


                prefProvider.setValue(
                    Constants.OLD_ITEM_BASE_CUSTOM_ITEM,
                    Gson().toJson(appDatabase.cartDao().getAllCartItems())
                )

            }


            destroyedCartItemsList.clear()
        }

        //delay(2000)
        // }
    }

    private fun deleteItemFromCartItem(tbCartItem: TbCartItem) {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.deleteItemFromCartItems(tbCartItem)
        }
    }

    private fun removeItemFromCartItems(itemId: Int, guestIndexForDineIn: Int) {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.removeItemFromCart(itemId, guestIndexForDineIn)
        }
    }

    suspend fun getLatestPrimaryKey(): Int {
        return posRepository.getLatestPrimaryKey()
    }

    fun addOrderNote(note: String) {
        if (cartModel != null) {
            cartModel!!.note = note
            prefProvider.setValue(Constants.orderNoteNew, note)
            updateCartModel(cartModel!!)

        }
    }

    fun createEmptyCart(model: CartModel) {
        viewModelScope.launch {
            posRepository.createEmptyCart(model)
        }
    }

    fun addCartModelBackup(cartModelBackup: String) {
        viewModelScope.launch {
            posRepository.addCartModelBackup(cartModelBackup)
        }
    }

    suspend fun getCartModelBackup(): List<CartModelBackup> = posRepository.getCartModelBackup()


    // combine two similar items
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

    // Store removed items from dine in order in case of Update order to send in server request
    fun addDineInRemovedItems(cartModel: CartModel): CartModel {
        var destroyedItems: ArrayList<TbItem> = arrayListOf()
        cartModel.items?.forEach {

            destroyedItems.add(it)

        }
        Log.e(TAG, "checkTotalDITems:  ${destroyedItems.size}")
        cartModel.items = destroyedItems

        return cartModel
    }

    suspend fun deleteCartItem(cartItemId: Int) {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        viewModelScope.launch {
            posRepository.deleteCartItems(cartItemId)
        }
    }

    suspend fun deleteCartItemsByIdGuestIndex(itemId: Int, guestIndexForDineIn: Int) {
        viewModelScope.launch {
            posRepository.deleteCartItemsByIdGuestIndex(itemId, guestIndexForDineIn)
        }
    }

    suspend fun updateDineInCartItemsByIdGuestIndex(
        itemQuantity: Int,
        itemId: Int,
        modifiers: String,
        guestIndexForDineIn: Int,
    ) {
        viewModelScope.launch {
            posRepository.updateDineInCartItemsByIdGuestIndex(
                itemQuantity,
                itemId,
                modifiers,
                guestIndexForDineIn
            )
        }
    }

    suspend fun deleteManualCartModel() {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        viewModelScope.launch {
            posRepository.deleteManualCartModel()
        }
    }


    fun deleteCartItems() {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        viewModelScope.launch {
            posRepository.deleteCartItems()
        }
    }

    fun deleteCart(isLastItem: Boolean = false) {
        try {
            prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
            cartModel = null
            manualCartOrderNote = ""

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                        Gson().toJson(Thread.currentThread().stackTrace)
                    }"
                )
            )
            GlobalScope.launch {
                deleteOrderTypeBackupByName(
                    prefProvider.employeeId()
                )
                posRepository.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
                destroyedList.clear()

                //Added to clear all the data when last item is removed from the cart.
                if (currentCartItems.size <= 1 && isLastItem) {
                    deleteCartItems()
                    clearCartModelBackup()
                }

                currentCartItems.clear()
                duplicateCurrentCartItem.clear()
            }
        } catch (e: Exception) {
            Log.d("deleteCart", "Preference is null")
        }
    }

    fun deleteCartBeforeSwitch() {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR deleteCartBeforeSwitch() Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        GlobalScope.launch {
            posRepository.deleteOldCartBeforeSwitch(prefProvider.getValueInt(EMPLOYEE_ID, 0))
        }
    }

    fun clearCartModelBackup() {
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )
        viewModelScope.launch {
            _changeCustDispSignInButtonTitle.postValue("")
            posRepository.clearCartModelBackup()
        }
    }

    fun deleteManualSaleCart() {
        viewModelScope.launch {
            _changeCustDispSignInButtonTitle.postValue("")
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            order_note = ""
            posRepository.deleteManualSaleCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
        }

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR DashboardCategoryViewModel.kt_Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )

    }

    fun deleteManualSaleItemsFromCartItems() {
        viewModelScope.launch {
            totalPrice = 0.0
            subTotalPrice = 0.0
            totalTax = 0.0
            totalDiscount = 0.0
            totalServiceCharge = 0.0
            totalCount = 0
            order_note = ""
            posRepository.deleteManualSaleItemsFromCartItem(
                prefProvider.getValueInt(
                    EMPLOYEE_ID,
                    0
                )
            )

        }

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                    Gson().toJson(Thread.currentThread().stackTrace)
                }"
            )
        )

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

    fun getCustomerPrinterList(): List<PrinterResponse.Data.CustomerReceiptPrinters> {
        return posRepository.fetchCustomerPrintersList()
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    suspend fun getManualSaleCartItemsList(orderType: String, employee_Id: Int): List<TbCartItem> {
        return posRepository.getManualSaleCartItemsList(orderType, employee_Id)
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

    fun manualSaleCartLogicNew(cartList: List<TbCartItem>?, item: TbCartItem, type: String) {
        /*Added by Rahul for solving Discount issue */
        if (item.discountType.isEmpty()) {
            item.discountType = Constants.AMOUNT
        }
        var cartModel = addCartModelNew(item, true)
        /*Added by Rahul for solving Discount issue */
        if (cartModel?.discountType!!.isEmpty()) {
            cartModel.discountType = Constants.AMOUNT
        }

        if (cartList != null && cartList.isEmpty()) {
            if (type == UPDATE) {
                cartList.forEach { items ->
                    cartModel = cartModel?.let {
                        taxBifurcationCalculationNew(
                            items,
                            it, type, false
                        )
                    }
                }
            } else {
                cartModel = cartModel?.let { taxBifurcationCalculationNew(item, it, type, false) }
            }
            cartModel?.let { addCart(it) }


            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                item.apply {
                    guestIndexForDineIn = dineInHeaderPosition
                    orderType = "DineIn"
                    employeeID = prefProvider.employeeId()
                }
            }


            addItemToCartItems(item)
        } else {
            val list = cartList?.toMutableList()

            if (!list.isNullOrEmpty()) {


                if (type == ADD) {
                    list.add(item)
                } else if (type == UPDATE) {

                    order_note = cartList[0].note
                    if (mPosition != -1) {
                        val model = cartList[mPosition]

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
                        val model = cartList[mPosition]
                        if (model != null) {
                            //delete from cart
                            if (item.isEdited) {
                                model.isEdited = item.isEdited
                                model.isDestroy = true
                            } else {
                                list.remove(item)
                                deleteItemFromCartItem(item)
                                EventBus.getDefault().post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                            Gson().toJson(Thread.currentThread().stackTrace)
                                        }"
                                    )
                                )
                            }
                        }
                    } else {
                        //list.remove(item)
                    }
                }

                if (type == UPDATE) {
                    cartList.forEach { itemData ->
                        cartModel = cartModel?.let {
                            taxBifurcationCalculationNew(
                                itemData, it, type, false
                            )
                        }
                    }
                } else {
                    cartModel = cartModel?.let {
                        taxBifurcationCalculationNew(
                            item,
                            it, type, false
                        )
                    }
                }

                cartModel?.let { updateCartModel(it) }

                if (list.isNotEmpty() && type != DELETE) {
                    val newUpdatedList = list.filter { it.itemId == item.itemId }
                    if (newUpdatedList.isNotEmpty()) {
                        newUpdatedList.forEach {
                            addItemToCartItems(it)
                        }
                    }
                }

                if (list.isEmpty()) {
                    deleteManualSaleItemsFromCartItems()
                }
            } else {

                if (type == DELETE) {
                    deleteManualSaleItemsFromCartItems()
                } else {
                    cartModel = cartModel?.let {
                        taxBifurcationCalculationNew(
                            item,
                            it, type, false
                        )
                    }
                    cartModel?.let { addCart(it) }
                    addItemToCartItems(item)
                }

            }
        }

    }


    //update Item Quantity

    fun updateItemQuantity(id: Int, itemQuantity: Int) {
        viewModelScope.launch {
            posRepository.updateItemQuantity(id, itemQuantity)
        }
    }

    fun addGuestFromDashBoard(cartList: List<CartModel>?) {
        addCart(cartList!![0])
    }

    // this method manages all the calculations like, taxes, service charge, discount, surcharge /cash discount on ADD/Update/Delete items from cart
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
                        // calculated taxes of all items
                        cartModel = taxBifurcationCalculation(items, cartModel!!, type, false)
                    }
                } else {
                    // calculated taxes of all items
                    cartModel = taxBifurcationCalculation(item, cartModel!!, type, false)
                }
            } else if (dineInList.isNotEmpty()) {
                // tax calculation for dine in items
                dineInList.forEach { dineInModel ->
                    dineInModel.items.forEach { itemData ->
                        cartModel = taxBifurcationCalculation(itemData, cartModel!!, type, false)
                    }

                }
            }
            // commented addCart to fix BIS-840
//            if (cartModel != null) {
//                addCart(cartModel!!)
//            }


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
                                                            }
//                                                            else if (list[i].variationsAttributes.isEmpty() == true) {
//                                                        item.id += 1
//                                                        isBreak = true
//                                                        Log.d(TAG, "newCartLogicModifier: isBreak")
//
//                                                        return@forEach
//                                                    }
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

                            // to validate item quantity for dine in cart(each item quantity must be less than or equal 1000)
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
                                                // To add/update item data of cart
                                                model.name = item.name
                                                model.itemQuantity =
                                                    model.itemQuantity + item.itemQuantity
                                                model.price = item.price
                                                model.variationsAttributes =
                                                    item.variationsAttributes
                                                item.modifiers.forEach {
                                                    it.itemQuantity =
                                                        item.itemQuantity * it.modifier_quantity
//                                                model.modifiers.forEach { tbmodfier ->
//                                                        if (it.id == tbmodfier.id) {
//                                                            it.modifier_quantity =
//                                                                it.itemQuantity / item.itemQuantity
//                                                            it.itemQuantity =
//                                                                it.itemQuantity + tbmodfier.itemQuantity
//                                                        }
//                                                    }

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
                                        // to update modifier data of any item
                                        item.singleItemPrice = item.price
                                        item.modifiers.forEach { it ->
//                                            it.modifier_quantity = it.itemQuantity
                                            it.itemQuantity =
                                                it.modifier_quantity * item.itemQuantity
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
                                        // Check if added or updated item matches with any other existing item in the cart. If matches with any other item than combine items
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
                                // if item matches with any other item in cart then update quantity and other data
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
                                            it.isChecked = item.isChecked
//                                             model.modifiers.forEach { tbmodifier ->
//                                                 if (it.id == tbmodifier.id) {
//                                                     if (it.modifier_quantity != tbmodifier.modifier_quantity) {
//                                                         tbmodifier.modifier_quantity =
//                                                             it.modifier_quantity
//                                                     } else {
//                                                         it.modifier_quantity =
//                                                             it.itemQuantity / model.itemQuantity
//                                                     }
//
//                                                     it.itemQuantity =
//                                                         (it.modifier_quantity * item.itemQuantity)
//                                                     it.isChecked = item.isChecked
//                                                 }
//                                             }
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
                            // to remove item from cart. Removed item will be sent with _destroy = true flag
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
                        // deletes whole cart
                        deleteCart()
                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                    Gson().toJson(Thread.currentThread().stackTrace)
                                }"
                            )
                        )
                    } else {

                        var cartModel = cartList.get(0)
                        cartModel = taxBifurcationCalculation(item!!, cartModel, type, false)
                        if (item != null) {
                            if (item.modifiers.isNotEmpty()) {
                                item.modifiers.forEach { mod ->
//                                    mod.modifier_quantity = mod.itemQuantity
                                    mod.itemQuantity =
                                        (mod.modifier_quantity * item.itemQuantity)
                                }
                            }
                            cartModel.dineInList?.get(dineInSelectedItemHeaderPos)?.items?.add(item)
                        }

                        if (cartModel != null) {
                            // Update cart with modification
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
                                                        }
//                                                    else if (list[i].variationsAttributes.isEmpty() == true) {
//                                                        item.id += 1
//                                                        isBreak = true
//                                                        Log.d(TAG, "newCartLogicModifier: isBreak")
//
//                                                        return@forEach
//                                                    }
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
                                                    (it.modifier_quantity * item.itemQuantity)
//                                              model.modifiers.forEach { tbmodfier ->
//                                                      if (it.id == tbmodfier.id) {
//                                                          it.modifier_quantity =
//                                                              it.itemQuantity / item.itemQuantity
//                                                          it.itemQuantity =
//                                                              it.itemQuantity + tbmodfier.itemQuantity
//                                                      }
//                                                  }

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

//                                          if (type == ADD){
//                                              it.modifier_quantity = it.itemQuantity / item.itemQuantity
//
//                                                  it.itemQuantity = item.itemQuantity
//
//                                          }
//                                          else{
                                        it.itemQuantity = it.modifier_quantity * item.itemQuantity

//                                        }

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
                                        )
//                                        var indexJ = -1
//
//                                        for (j in 0 until listTmp.size) {
//                                            if (listTmp[j].id == item.id
//                                            ) {
//                                                indexJ = j
//                                                break
//                                            }
//
//                                        }
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
                                                    )
//                                                    var indexJ = -1
//
//                                                    for (j in 0 until listTmp.size) {
//                                                        if (listTmp[j].id == item.id
//                                                        ) {
//                                                            indexJ = j
//                                                            break
//                                                        }
//
//                                                    }
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
                                        it.itemQuantity = (it.modifier_quantity * item.itemQuantity)
//                                        model.modifiers.forEach { tbmodifier ->
//                                            if (it.id == tbmodifier.id) {
//                                                if (it.modifier_quantity != tbmodifier.modifier_quantity) {
//                                                    tbmodifier.modifier_quantity =
//                                                        it.modifier_quantity
//                                                } else {
//                                                    it.modifier_quantity =
//                                                        it.itemQuantity / model.itemQuantity
//                                                }
//
//                                                it.itemQuantity =
//                                                    (it.modifier_quantity * item.itemQuantity)
//                                            }
//                                        }
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
                    addCart(cartModel)
                } else {

                    if (type == DELETE) {
                        deleteCart()
                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                    Gson().toJson(Thread.currentThread().stackTrace)
                                }"
                            )
                        )
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

    fun updateCart(
        cartList: List<TbCartItem>?,
        item: TbCartItem?,
        type: String,
        isManualSales: Boolean,
        isFromDetail: Boolean = false,
        dineInList: List<DineInModel> = arrayListOf(),
        isFromDineInScreen: Boolean = false,
        position: Int = -1,
    ) { //Item came correct upto here
        prefProvider.setValueboolean(
            IS_LAST_ITEM_DELETE, false
        )  // reset flag in case of adding or updating item
        Log.e("DashViewModModel", "checkCartSize: ${cartList?.size}")
        var cartModel = item?.let { addCartModelNew(it, isManualSales) }
        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare

            Log.e("DashViewModModel", "checkItem:  ${Gson().toJson(item?.modifiers)}")


            if (item != null) {
                if (type == UPDATE) {
                    cartList.forEach { items ->
                        cartModel = taxBifurcationCalculationNew(items, cartModel!!, type, false)
                    }
                } else {
                    cartModel = taxBifurcationCalculationNew(item, cartModel!!, type, false)
                }

                addCart(cartModel!!)
                addItemToCartItems(item)
            } else if (dineInList.isNotEmpty()) {
                /*dineInList.forEach { dineInModel ->
                    dineInModel.items.forEach { itemData ->
                        cartModel = taxBifurcationCalculation(itemData, cartModel!!, type, false)
                    }

                }*/
            }

        } else {

            var isItem1000 = false
            val list = cartList?.toMutableList()
            if (!list.isNullOrEmpty()) {

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
                                    if (list[i].itemId == item.itemId && (!checkVariationNew(
                                            list[i], item
                                        ) && !checkModifierNew(list[i], item))
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
                                                /*Added to check if the merged item is showing update or not - START*/
                                                item.isItemEdited = true
                                                list[i].isItemEdited = true
                                                /*Added to check if the merged item is showing update or not - END*/
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


                                    } else if (list[i].itemId == item.itemId && checkVariationNew(
                                            list[i], item
                                        ) && checkModifierNew(list[i], item)
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

                                                /* *//*Added to check if the merged item is showing update or not - START*//*
                                                item.isItemEdited=true
                                                list[i].isItemEdited=true
                                                *//*Added to check if the merged item is showing update or not - END*//*
*/
                                            } catch (e: java.lang.Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        var listTmp = combineItemNew(
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
                            val model = cartList[index]
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

                                            /*Added to check if the merged item is showing update or not - START*/
                                            item.isItemEdited = true
                                            /*Added to check if the merged item is showing update or not - END*/
                                        }
                                        itemDiscountApplyNew(model, item)
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

                                            /*Added to check if the merged item is showing update or not - START*/
                                            item.isItemEdited = true
                                            /*Added to check if the merged item is showing update or not - END*/
                                        }
                                        itemDiscountApplyNew(model, item)
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

                                    /*Added to check if the merged item is showing update or not - START*/
//                                    item.isItemEdited=true
                                    /*Added to check if the merged item is showing update or not - END*/

                                }
                                list.add(item)
                            }
                        } else {
                            //For BIS-3219 issue
                            //This is for adding the item after deleting it after coming from all orders update order click
                            var newItem: TbCartItem? = null
                            var indexToRemove = -1
                            list.forEachIndexed { index, tbItem -> //Item came correct upto here
                                if (tbItem.isDestroy && tbItem.itemId == item?.itemId) {
                                    newItem = tbItem
                                    indexToRemove = index
                                }
//                                if (!tbItem.isDestroy && tbItem.itemId==item?.itemId){
//                                    item.itemQuantity=tbItem.itemQuantity
//                                }
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
                            if (item.isManualSales || item.itemId == 1) {


                                if (list[i].manualSaleId == item.manualSaleId && list[i].cartItemId == item.cartItemId) {
                                    Log.d(TAG, "cartLogic: " + i)
                                    index = i
                                    break
                                }

                            } else {


                                if (idsF.size > 1 && list[i].itemId == item.itemId && checkVariationNew(
                                        list[i], item
                                    ) && checkModifierNew(
                                        list[i], item
                                    ) && item.id != list[i].id
                                ) {


                                    var listTmp =
                                        combineItemNew(list.toCollection(arrayListOf()), item, i)
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
                                            if (list[k].id != item.id && list[k].itemId == item.itemId && checkVariationNew(
                                                    list[k], item
                                                ) && checkModifierNew(list[k], item)
                                            ) {

                                                var listTmp = combineItemNew(
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
                        val model = cartList[index]
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
                                    it.itemQuantity = (it.modifier_quantity * item.itemQuantity)
                                    item.singleItemPrice += it.price * it.modifier_quantity
                                }
                                Log.d(TAG, "newCartLogicModifier: " + item.singleItemPrice)
                                model.itemQuantity = item.itemQuantity
                                model.modifiers = item.modifiers
                                model.isDestroy = false
                                itemDiscountApplyNew(model, item)

                            }
                            Log.d(TAG, "cartLogic:itemname " + model.name)
                            list[index] = model

                        }
                    }
                } else
                    if (type == DELETE) {

                        var index = -1
                        Log.e(TAG, "CheckDeleteItem ${Gson().toJson(item)}")
                        Log.e(TAG, "getListedItems  ${Gson().toJson(list[0])}")
                        Log.e(TAG, "checkReOrder  ${cartModel?.reorder}")

                        for (i in list.indices) {
                            if (item != null) {
                                if (!item.isManualSales && item.itemId != 1) {
                                    if (cartModel?.reorder == false && list[i].itemId == item.itemId && item.modifiers.isEmpty() && checkVariationNew(
                                            list[i], item
                                        ) && item.id == list[i].id
                                    ) {
                                        index = i
                                        Log.d(TAG, "newCartLogicModifier normal item: ${i}")
                                        break
                                    } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && list[i].id == item.id && checkVariationNew(
                                            list[i], item
                                        ) && checkModifierNew(list[i], item)
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

                                    } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && (!checkVariationNew(
                                            list[i], item
                                        ) || !checkModifierNew(list[i], item))
                                    ) {
                                        if (list[i].id == item.id) {
                                            Log.e(TAG, "CheckedBefore   ${i}")
                                            index = i
                                            break
                                        }
                                    }

                                } else {
                                    if (list[i].manualSaleId == item.manualSaleId && list[i].cartItemId == item.cartItemId) {
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
                            val model = cartList[index]
                            if (model != null) {
                                //delete from cart
                                if (item?.isEdited == true) {
                                    model.isEdited = item.isEdited
                                    model.isDestroy = true
                                    addItemToCartItems(model)//Update deleted item with its updated fields like isEdited and isDestroy
                                } else {
                                    if (list.size == 1) {
                                        prefProvider.setValueboolean(IS_LAST_ITEM_DELETE, true)
                                    }
                                    list.remove(model)
                                    deleteItemFromCartItem(model)
                                    EventBus.getDefault().post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                                Gson().toJson(Thread.currentThread().stackTrace)
                                            }"
                                        )
                                    )
                                }
                            }
                        } else {
                            //list.remove(item)
                        }
                    }


                if (type == UPDATE) {
                    cartList.forEach { itemData ->
                        cartModel = taxBifurcationCalculationNew(
                            itemData, cartModel!!, type, false
                        )
                    }
                } else {
                    if (item != null) {
                        cartModel = taxBifurcationCalculationNew(item!!, cartModel!!, type, false)
                    }
                }
                updateCartModel(cartModel!!)

                if (list.isNotEmpty() && type != DELETE && !isFromDetail) {

                    /*val newUpdatedList: List<TbCartItem> = if(hasCustomItemIds(list)){
                        list.filter { it.customItemID == item?.customItemID }
                    }else{
                        list.filter { it.itemId == item?.itemId }
                    }*/

                    val newUpdatedList = list.filter { it.itemId == item?.itemId }

                    if (newUpdatedList.isNotEmpty()) {
                        newUpdatedList.forEach {
                            addItemToCartItems(it)
                        }
                    }

                }
            } else {

                if (type == DELETE) {
                    deleteCart()
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                Gson().toJson(Thread.currentThread().stackTrace)
                            }"
                        )
                    )
                } else {

                    val newCartModel: CartModel =
                        taxBifurcationCalculationNew(item!!, cartModel!!, type, false)
                    if (item.modifiers.isNotEmpty()) {
                        item.modifiers.forEach { mod ->
                            mod.itemQuantity = item.itemQuantity * mod.modifier_quantity
                        }
                    }

                    addCart(newCartModel)
                    addItemToCartItems(item)
                }


            }

        }
    }

    fun updateDineInCart(
        cartList: List<TbCartItem>?,
        item: TbCartItem?,
        type: String,
        isManualSales: Boolean,
        dineInList: List<DineInModel> = arrayListOf(),
        isFromDineInScreen: Boolean = false,
        position: Int = -1,
    ) {
        var addNewEntry = false
        Log.d(TAG, "updateDineInCart: type: " + type)
        prefProvider.setValueboolean(
            IS_LAST_ITEM_DELETE, false
        )  // reset flag in case of adding or updating item
        Log.e("DashViewModModel", "updateDineInCart checkCartSize: ${cartList?.size}")
        var cartModel = addCartModelNew(item, isManualSales)
        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare

            Log.e("DashViewModModel", "checkItem:  ${Gson().toJson(item?.modifiers)}")

            if (item != null) {
                if (type == UPDATE) {
                    cartList.forEach { items ->
                        cartModel =
                            cartModel?.let { taxBifurcationCalculationNew(items, it, type, false) }
                    }
                } else {
                    cartModel = cartModel?.let {
                        taxBifurcationCalculationNew(
                            item,
                            it, type, false
                        )
                    }
                }
                Log.d("DashViewModModel", "addItemToCartItems 2397: ")
                item.guestIndexForDineIn = this.dineInHeaderPosition
                currentCartItems.add(item)
                addItemToCartItems(item)
            } else if (dineInList.isNotEmpty()) {
                dineInList.forEach { dineInModel ->
                    cartList.forEach { itemData ->
                        cartModel =
                            cartModel?.let {
                                taxBifurcationCalculationNew(
                                    itemData,
                                    it, type, false
                                )
                            }
                    }

                }
                item?.let {
                    Log.d("DashViewModModel", "addItemToCartItems 2407: ")
                    item.guestIndexForDineIn = this.dineInHeaderPosition
                    currentCartItems.add(it)
                    addItemToCartItems(it)
                }
            }

        } else {
            Log.d("DashViewModModel", "else called: ")
            var isDineInItem1000 = false
            if (cartModel == null) {
                cartModel = this.cartModel
            }
            order_note = cartModel?.note.toString()
            var list = cartList!!.toMutableList()


            cartModel?.dineInList = dineInList
            var dinein = dineInList
            Log.d("DashViewModModel", "dinein list size: " + dinein.size)
            if (dinein.isNotEmpty() && dinein != null) {
//                val selectedHeader = dineInList.get(0).selectedPosition
                if (list.isNotEmpty()) {
                    if (type == ADD) {
                        var index = -1
                        for (i in list.indices) {
                            if (item != null) {
                                if (item.isManualSales) {
                                    Log.d("DashViewModModel", "manual sales: " + item.isManualSales)

                                    if (list[i].manualSaleId == item.manualSaleId) {
                                        Log.d(TAG, "cartLogic: " + i)
                                        index = i
                                        break
                                    }

                                } else {
                                    Log.e("AddedInElse", "DashViewModModel GotMod")

                                    if (list[i].itemId == item.itemId && list[i].itemQuantity > 1000) {
                                        isDineInItem1000 = true
                                        _itemQuantityCheck.value = Event(true)
                                        break
                                    } else {
                                        if (list[i].itemId == item.itemId && list[i].guestIndexForDineIn == dineInHeaderPosition) {
                                            if (list[i].itemId == item.itemId && checkVariationNew(
                                                    list[i], item
                                                ) && checkModifierNew(list[i], item) && !list[i].isFired
                                            ) {

//                                            if (list[i].guestIndexForDineIn == item.guestIndexForDineIn) {
                                                Log.d(TAG, "DashViewModModel cartLogic: " + i)

                                                var listTmp = combineItemNew(
                                                    list.toCollection(arrayListOf()), item, i
                                                )
                                                list.clear()
                                                Log.e(
                                                    TAG,
                                                    "DashViewModModel newCartLogicModifier: position of selected Item " + item.id
                                                )
                                                list.addAll(listTmp.toMutableList())

                                                Log.e(
                                                    TAG,
                                                    "DashViewModModel getMergeCombineItem  ${list.size}"
                                                )

                                                index = -2
                                                break
//                                            }

                                            } else if (list[i].itemId == item.itemId && (!checkVariationNew(
                                                    list[i], item
                                                ) && !checkModifierNew(list[i], item))
                                            ) {
                                                addNewEntry = true
                                                Log.d(TAG, "DashViewModModel else if called: ")
                                                var isBreak: Boolean = false


                                                list[i].modifiers.forEach { modifier ->
                                                    item.modifiers.forEach { mod ->
                                                        if (mod.id == modifier.id && mod.modifier_quantity == modifier.modifier_quantity) {
                                                            if (list!![i].variationsAttributes.isNotEmpty() && list!![i].variationsAttributes[0].id == item.variationsAttributes[0].id) {
                                                                item.id += 1
                                                                isBreak = true
                                                                Log.e(
                                                                    TAG,
                                                                    "DashViewModModel newCartLogicModifier: if isBreak"
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
                                                                TAG,
                                                                "DashViewModModel newCartLogicModifier: else if isBreak"
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


                                            } else {
                                                Log.d(
                                                    TAG,
                                                    "DashViewModModel variation else called : "
                                                )

                                            }
                                        } else {
//                                            item.itemId = item.itemId+1
//                                            Log.d("DashViewModModel", "addItemToCartItems 2551: "+item.itemQuantity)
//                                            item.guestIndexForDineIn = this.dineInHeaderPosition
//                                            addItemToCartItems(item)
//                                            addCart(cartModel!!)
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
                                    var listd = list!!.filter { it.itemId == it1.itemId }
                                    if (listd.size > 1) {
                                        it1.customItemID =
                                            kotlin.random.Random.nextInt(10, 10000)

                                    }


                                }


                            }

                            if (index != -1 && index != -2) {
                                val model = list[index]
                                Log.d(TAG, "DashViewModModel 2570 cartLogic: " + index)
                                if (model != null) {
                                    if (index != -1) {
                                        if (item != null) {
                                            model.name = item.name
                                            model.taxes = item.taxes
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
                                            itemDiscountApplyNew(model, item)
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
                                            itemDiscountApplyNew(model, item)
                                            model.isDestroy = false
                                        }
                                        list[index] = model
                                    }

                                }
                            } else if (index != -2) {
                                Log.d(TAG, "DashViewModModel 2630 cartLogic: " + index)
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
                                    "DashViewModModel checkInsideSc  ${item?.isDestroy}  updateOrder ${isOrderUpdate}"
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
                            Log.e(
                                "GEtDineInData",
                                "DashViewModModel getList  ${Gson().toJson(list)}"
                            )
                            Log.e(
                                "GEtDineInData",
                                "DashViewModModel destroyTerer  ${item?.isDestroy}"
                            )
                            Log.e(
                                TAG, "DashViewModModel checkDinein  ${
                                    prefProvider.getValueboolean(
                                        DINE_IN_UPDATE, false
                                    )
                                }"
                            )

                            if (item != null) {
//                                Log.d("DashViewModModel", "addItemToCartItems 2687: ")
//                                item.guestIndexForDineIn = this.dineInHeaderPosition
//                                addItemToCartItems(item)
                            }

                        }

                    } else if (type == UPDATE) {
                        var index = -1
//                        val selectedHeader = dineInList.get(0).selectedPosition


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
                                    } else if (list[i].itemId == item.itemId && (item.modifiers.isNotEmpty() || item.variationsAttributes.isNotEmpty()) && checkVariationNew(
                                            list[i], item
                                        ) && checkModifierNew(list[i], item)
                                    ) {
                                        var listTmp = combineItemNew(
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
                                    itemDiscountApplyNew(model, item)

                                }
                                Log.d(TAG, "cartLogic:itemname " + model.name)
                                list.set(index, model)
//                                    list[index] = model

                            }
                        }
                    } else
                        if (type == DELETE) {
                            var list: ArrayList<TbCartItem> = arrayListOf()
                            if (item != null) {
                                list = cartList as ArrayList<TbCartItem>
                            } else {
                                list = cartList as ArrayList<TbCartItem>
                            }
                            var index = -1
                            Log.e(TAG, "checkReOrder  ${cartModel?.reorder}")

                            for (i in list.indices) {
                                if (item != null) {
                                    if (!item.isManualSales) {
                                        if (cartModel?.reorder == false && list[i].itemId == item.itemId && item.modifiers.isEmpty() && checkVariationNew(
                                                list[i], item
                                            ) && list[i].id == item.id
                                        ) {
                                            index = i
                                            Log.d(TAG, "newCartLogicModifier normal item: ${i}")
                                            break
                                        } else if (list[i].itemId == item.itemId && cartModel?.reorder == false && list[i].id == item.id && checkVariationNew(
                                                list[i], item
                                            ) && checkModifierNew(list[i], item)
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

                                        } else if (cartModel?.reorder == false && list[i].itemId == item.itemId && (!checkVariationNew(
                                                list[i], item
                                            ) || !checkModifierNew(list[i], item))
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
                                Log.e(TAG, "foundIndexDineInUpdate: $index  listSize: ${list.size}")
                                val model = list.get(index)
                                if (model != null) {
                                    //delete from cart
                                    if (item?.isEdited == true) {
                                        model.isEdited = item.isEdited
                                        model.isDestroy = true
                                    } else {
                                        list.remove(model)
                                        /*currentCartItems.remove(model)
                                        model.guestIndexForDineIn?.let {
                                            removeItemFromCartItems(model.itemId ,
                                                it
                                            )
                                        }*/
                                    }
                                }

                                currentCartItems.remove(item)
                                if (item?.orderItemId != 0) {

                                    item?.isDestroy = true
                                    item?.isEdited = true
                                    cartModel?.dineInList?.forEach {

                                        it.items.forEach { it ->
                                            if (it.cartItemId == item?.cartItemId) {
                                                it.isDestroy = true
                                                it.isEdited = true
                                                return@forEach
                                            }
                                        }
                                    }

                                    CoroutineScope(Dispatchers.IO).launch {
                                        cartModel?.dineInList!!.forEach { it ->

                                            it.items.forEach { tbItem ->
                                                if (tbItem.cartItemId == item?.cartItemId)
                                                    tbItem.isDestroy = true
                                            }
                                        }

                                        item?.let { appDatabase.cartDao().updateItemDestroy(it) }
                                    }

                                } else {
                                    item?.guestIndexForDineIn?.let {
                                        removeItemFromCartItems(
                                            item.itemId,
                                            it
                                        )
                                    }
                                }
                            } else {
                                //list.remove(item)

                            }
                            if (item != null) {
//                            Log.d("DashViewModModel", "addItemToCartItems 2896: ")
//                            item.guestIndexForDineIn = this.dineInHeaderPosition
//                            currentCartItems.add(item)
//                            addItemToCartItems(item)
                            }
                            Log.e(
                                "ModNewLogic",
                                "dineInList:   ${Gson().toJson(cartModel?.dineInList)}"
                            )
                            //cartModel.dineInList = dinein


                        }
                }
                if (type == UPDATE) {
                    for (i in 0 until dineInList.size) {
                        list.forEach {
                            cartModel = taxBifurcationCalculationNew(
                                it, cartModel!!, type, false
                            )
                        }
                    }


                    Log.e(
                        TAG, "taxlistDynamicData:  ${Gson().toJson(cartModel?.taxlistDynamic)}"
                    )
                } else {
                    if (item != null) {
                        cartModel =
                            cartModel?.let { taxBifurcationCalculationNew(item, it, type, false) }
                    }
                }

                updateCartModel(cartModel!!)

                if (item != null) {
                    val newUpdatedItemList =
                        list.filter { it.itemId == item?.itemId && it.guestIndexForDineIn == dineInHeaderPosition }
                    var newUpdatedItem: TbCartItem = if (newUpdatedItemList.isNotEmpty()) {
                        // IMPORTANT -- remove this.. this is for log purpose only
//                        newUpdatedItemList.forEach {
//                            it.taxes = arrayListOf()
//                            Log.d(TAG, "newUpdatedItemList updateDineInCart: " + Gson().toJson(it))
//                        }

                        newUpdatedItemList[0]
                    } else {
                        list.first { it.itemId == item.itemId }
                    }

                    if (type != DELETE) {
                        Log.d("DashViewModModel", "addItemToCartItems 2926: ")
                        Log.d(
                            "DashViewModModel",
                            "newUpdatedItem:: guestIndexForDineIn: " + dineInHeaderPosition
                        )
                        // IMPORTANT -- remove this.. this is for log purpose only
                        //  newUpdatedItem.taxes = arrayListOf()
                        Log.d(
                            "DashViewModModel",
                            "newUpdatedItem:: " + Gson().toJson(newUpdatedItem)
                        )
                        newUpdatedItem.guestIndexForDineIn = this.dineInHeaderPosition

                        if (addNewEntry) {
                            viewModelScope.launch {
                                newUpdatedItem.cartItemId =
                                    this@DashBoardCategoryViewModel.getLatestPrimaryKey() + 1
                            }
                            //   currentCartItems.add(newUpdatedItem)
                            addItemToCartItems(newUpdatedItem)
                        } else {
                            //    currentCartItems.add(newUpdatedItem)
                            addItemToCartItems(newUpdatedItem)
                        }
                        cartModel?.let { addCart(it) }
                    }
                } else {
                    for (i in 0 until list.size) {
                        var newUpdatedItem = list[i]
                        if (type != DELETE) {
                            Log.d("DashViewModModel", "addItemToCartItems 2926: ")
                            Log.d(
                                "DashViewModModel",
                                "newUpdatedItem:: guestIndexForDineIn: " + dineInHeaderPosition
                            )
                            // IMPORTANT -- remove this.. this is for log purpose only
                            // newUpdatedItem.taxes = arrayListOf()
                            Log.d(
                                "DashViewModModel",
                                "newUpdatedItem:: " + Gson().toJson(newUpdatedItem)
                            )
                            newUpdatedItem.guestIndexForDineIn = list[i].guestIndexForDineIn
                            if (addNewEntry) {
                                viewModelScope.launch {
                                    newUpdatedItem.cartItemId =
                                        this@DashBoardCategoryViewModel.getLatestPrimaryKey() + 1
                                }
                                //    currentCartItems.add(newUpdatedItem)
                                //   addItemToCartItems(newUpdatedItem)
                            } else {
                                //   currentCartItems.add(newUpdatedItem)
                                addItemToCartItems(newUpdatedItem)
                            }
                            cartModel?.let { addCart(it) }
                        }
                    }
                }
            } else {

                if (type == DELETE) {
                    deleteCart()
                } else {

                    cartModel =
                        cartModel?.let { taxBifurcationCalculationNew(item!!, it, type, false) }
                    if (item?.modifiers?.isNotEmpty() == true) {
                        item.modifiers.forEach { mod ->
                            mod.modifier_quantity = mod.itemQuantity
                        }
                    }
                    Log.d("DashViewModModel", "addItemToCartItems 2941: ")
                    item?.guestIndexForDineIn = this.dineInHeaderPosition
                    item?.let { currentCartItems.add(it) }
                    item?.let { addItemToCartItems(it) }

                    cartModel?.let { addCart(it) }
                }


            }

        }
        //   oldDineInItems.clear()
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

    private fun combineItemNew(
        list: ArrayList<TbCartItem>,
        item: TbCartItem,
        index: Int
    ): List<TbCartItem> {
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

    private fun itemDiscountApplyNew(model: TbCartItem, item: TbCartItem) {
        model.discountPrice = item.discountPrice
        model.discountType = item.discountType
        model.discountId = item.discountId
        model.isManualSales = item.isManualSales
        model.note = item.note
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

    fun checkModifierNew(tbItem: TbCartItem, item: TbCartItem): Boolean {
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


        } else {
            isSame = false
        }

        Log.e(TAG, "ReturnIssame ${isSame}")
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

    fun checkVariationNew(tbItem: TbCartItem, item: TbCartItem): Boolean {

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
                    if ((tbItem.variationsAttributes[0].price == item.variationsAttributes[0].price)) {
                        tbItem.guestIndexForDineIn == item.guestIndexForDineIn
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }


        Log.e(TAG, "DashViewModModel CheckVariationAdd ${isSame}")
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
            if (!orderType.equals(Constants.PHONE_ORDER, ignoreCase = true)) {
                deliveryType = ""
            }
            orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
            isMaual = isManualSales
            serviceCharge = serviceChargesList
            customer = assignCustomer
            item.itemQuantity = item.itemQuantity

            inventoryModelList.add(item)
            items = inventoryModelList
        }
    }

    fun addCartModelNew(item: TbCartItem?, isManualSales: Boolean): CartModel? {
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
            orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
            if (!orderType.equals(Constants.PHONE_ORDER, ignoreCase = true)
            ) {
                deliveryType = ""
            }
            if (orderTypeId == -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    var job = launch {
                        posRepository.getOrderTypeBackupList(employeeID)?.let {
                            try {
                                orderTypeId = (it.get(0).orderType) ?: -1
                            } catch (e: Exception) {
                            }
                        }
                    }
                    job.join()
                }
            }
            orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
            isMaual = isManualSales
            serviceCharge = serviceChargesList
            customer = assignCustomer
            if (item != null) {
                item.itemQuantity = item.itemQuantity
            }

            note = cartModel?.note ?: ""
            discountPrice = cartModel?.discountPrice ?: 0.0
            discountSelectdValue = cartModel?.discountSelectdValue ?: 0.0
        }
    }

    suspend fun getOrderTypeBackupList(employeeId: Int): List<OrderTypeBackup> {
        return posRepository.getOrderTypeBackupList(employeeId)
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
                            it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                                it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                            it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
    fun itemCalculationNew(
        cartModel: CartModel?,
        cartList: List<TbCartItem>?,
        txtTotalAmount: AppCompatTextView,
        context: Context,
        isFromManualSales: Boolean = false
    ) {

        if (!cartList.isNullOrEmpty()) {
            var totalAmmount = 0.0
            nonCashAdj = 0.0
            totalPrice = 0.0
            totalCount = 0
            subTotalPrice = 0.0
            totalDiscount = 0.0
            totalTax = 0.0
            totalServiceCharge = 0.0
            var amountToBePaid = 0.0

            if (cartModel?.orderType == " ") {

                // for dine in changes , replace if with cartModel?.orderType == DINE_IN

            } else {

                if (cartList.isNotEmpty()) {

                    val itemCount = cartList.size

                    cartList.forEach { item ->
                        totalCount += item.itemQuantity
                        subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                        taxCalculationNew(item, cartList[0].discountPrice / itemCount)

                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)

                        }
                    }

                    order_note = cartModel?.note ?: ""
                    subTotalPrice -= cartModel?.discountPrice ?: 0.0

                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    cartModel?.let { serviceChargeCalculationNew(it) }

                    totalDiscount += cartModel?.discountPrice ?: 0.0

                    cartList.forEach {
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
                            it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                            it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                                it.itemQuantity = it.modifier_quantity * item.itemQuantity
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

    fun itemCalculationCartModelNew(
        cartItems: List<TbCartItem>,
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

        if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
            totalServiceCharge = 0.0

        var amountToBePaid = 0.0

        runBlocking {

            if (cartModel == null) {

                CoroutineScope(Dispatchers.IO).async {
                    cartModel = getManualSaleFromCart(prefProvider.getValueInt(EMPLOYEE_ID, -1))

                    if (cartModel == null && getAllCartModels().isNotEmpty()) {
                        cartModel = getAllCartModels().firstOrNull()
                    }

                }.await()
            }

            if (cartItems.isNotEmpty()) {

                if (cartModel?.reorder == true) {


                    val itemCount = cartItems.size

                    cartItems.forEach { item ->
                        totalCount += item.itemQuantity
                        totalDiscount += (item.discountPrice * item.itemQuantity)
                        subTotalPrice += (item.price * item.itemQuantity)
                        item.modifiers.forEach {
                            subTotalPrice += (it.price * it.itemQuantity)
                        }
                        taxCalculationReorderNew(item)


                    }
                    String.format("%.2f", totalTax).toDouble()
                    subTotalPrice -= cartModel?.discountPrice ?: 0.0
                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    cartModel?.let {
                        serviceChargeCalculationModel(it)
                    }

                    totalDiscount += cartModel?.discountPrice ?: 0.0
                    order_note = cartModel?.note ?: ""

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

                    if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                        totalServiceCharge = 0.0

                    val itemCount = cartItems.size
                    var taxList: ArrayList<TaxData> = arrayListOf()
                    cartItems.forEach { item ->
                        if (!item.isDestroy) {
                            totalCount += item.itemQuantity
                            totalDiscount += item.discountPrice * item.itemQuantity
                            subTotalPrice += (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)


                            taxCalculationNew(item, 0.0 / itemCount)

                            item.modifiers.forEach {

                                if (!it._destroy)
                                    subTotalPrice += (it.price * it.itemQuantity)

                            }
                        }
                    }
                    subTotalPrice -= cartModel?.discountPrice ?: 0.0
                    Log.e("InternetSoft", "subTotalPrice:   ${subTotalPrice}")
                    if (subTotalPrice < 0) {
                        subTotalPrice = 0.0
                    }
                    cartModel?.let {
                        serviceChargeCalculationModel(it)
                    }

                    totalDiscount += cartModel?.discountPrice ?: 0.0
                    if (totalDiscount > 0.0) {
                        Log.e("CheckOrderNote", "Discount added -> ${cartModel?.discountPrice ?: 0.0}")
                    }
                    Log.e("CheckOrderNote", "AfterAddingDiscount -> ${cartModel?.note ?: "null"}")
                    order_note = cartModel?.note ?: (order_note ?: "")

                    var finalTotal = 0.0
                    if (subTotalPrice == 00.0 || subTotalPrice == 0.00 || subTotalPrice == 00.00) {
                        subTotalPrice = 0.00
                        totalTax = 0.00
                        totalServiceCharge = 0.00
                        finalTotal = 0.00
                    } else {

                        finalTotal = (subTotalPrice + totalTax + totalServiceCharge)
                    }

                    redeemLoyaltyInfo.needToApplyLoyalty = prefProvider.getValueboolean(
                        LOYALTY_ADDED, false
                    )
                    totalPrice = finalTotal
                    try {
                        if (finalTotal >= prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0")
                                .toDouble()
                        ) {
                            wholetotalPrice = finalTotal
                        }
                    } catch (e: Exception) {

                    }


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
                        if (prefProvider.getValue(Constants.ORDER_TYPE, "") != Constants.DINE_IN) {
                            redeemLoyaltyInfo.getAmountToBePaid()?.let {
                                totalPrice = it
                            }
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

    }


    fun loyaltyPointCondition(customer: TbCustomer?): Boolean {
        return (customer?.enroll_to_loyalty == true && activeLoyaltyProgram != null && activeLoyaltyProgram?.rewardPoint ?: 0 <= customer.final_reward ?: 0)
    }

    private fun checkAppliedLoyaltyProgram(
        customer: TbCustomer?, total: Double, txtTotalAmount: AppCompatTextView
    ) {

        redeemLoyaltyInfo.total = total
        val availablePoints1 = customer?.final_reward ?: 0
        redeemLoyaltyInfo.availablePoints = availablePoints1
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
                        availablePoints1 - redeemLoyaltyInfo.usedLoyaltyPoints
                } else {
                    redeemLoyaltyInfo.usedLoyaltyAmount =
                        (availablePoints * it.amount / it.rewardPoint)
                    redeemLoyaltyInfo.usedLoyaltyPoints = availablePoints
                    redeemLoyaltyInfo.remainingLoyaltyPoints =
                        availablePoints1 - redeemLoyaltyInfo.usedLoyaltyPoints
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

    private fun serviceChargeCalculationNew(cartModel: CartModel) {
        val serviceChargesList = cartModel.serviceCharge
        if (!serviceChargesList.isNullOrEmpty()) {
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
                // Added by Rahul Pandit to Solve PA1-I882 START
                if (prefProvider.getValue(ORDER_TYPE,"") == PHONE_ORDER){
                    totalServiceCharge = 0.0
                    return
                }
                // Added by Rahul Pandit to Solve PA1-I882 END
                serviceChargesList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        var serviceTotal = (subTotalPrice * it.percentage) / 100
                        totalServiceCharge += String.format("%.2f", serviceTotal).toDouble()
                        callUpdateCartFooter(true)
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
            it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
        Log.e("CheckTotalTax", "Return: ${totaltaxtemp}")
        return totaltaxtemp
    }

    //  To calculate each item tax
    private fun getTotalTaxBirfurcationNew(
        item: TbCartItem, itemtype: TaxData, type: String
    ): Double {
        var totaltaxtemp: Double = itemtype.totalTaxTypePrice
        var modifierPrice = 0.0
        val price = (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)
        Log.e("checkTotalTax", "checkItemPrice:  ${price}  prevTAx  ${itemtype.totalTaxTypePrice}")

        Log.e("ItemModSize", "CheckModSize: ${item.modifiers.size}")
        item.modifiers.forEach {
            Log.e("ItemMod", "itemQuantity:  ${it.itemQuantity} andMODQU  ${it.modifier_quantity}")
            var modQty = it.modifier_quantity * item.itemQuantity
            Log.e("ItemModQty", "modQty:  ${modQty}")
            modifierPrice += (it.price * modQty)
        }

        val totalPrice = price + modifierPrice /*- (discountPrice * item.itemQuantity)*/

        Log.e("checkTotalTax", "totalPrice:  ${totalPrice}  taxRate  ${itemtype.rate}")


        totaltaxtemp += if (itemtype.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00).toDouble()
            } else {
                val itemTaxPrice = (itemtype.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "itemPriceTaxApp" + itemTaxPrice)
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
        Log.e(TAG, "checkTotalRet ${totaltaxtemp}")
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
                                it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                        prefProvider.setValue(
                            Constants.taxListDynamic,
                            Gson().toJson(cartModel.taxlistDynamic)
                        )
                    } else {
                        if (itemtype.taxType != "Percentage") {
                            var modifierPrice: Double = 0.0
                            val price =
                                (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                            item.modifiers.forEach {
                                it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                                Log.e(
                                    "CheckPassing",
                                    "checkSubT: ${cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice}"
                                )
                                Log.e(
                                    "CheckPassing", "checkTaxBifur:  ${
                                        getTotalTaxBirfurcation(
                                            item, itemtype, type
                                        )
                                    }"
                                )
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
                                    prefProvider.setValue(
                                        Constants.taxListDynamic,
                                        Gson().toJson(cartModel.taxlistDynamic)
                                    )


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
                                            prefProvider.setValue(
                                                Constants.taxListDynamic,
                                                Gson().toJson(cartModel.taxlistDynamic)
                                            )


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
                            it.modifier_quantity = it.modifier_quantity * item.itemQuantity
                            modifierPrice += (it.price * it.modifier_quantity)
                        }

                        val totalPrice = price + modifierPrice
                        itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                    }
                    itemtype.totalTaxTypePrice = getTotalTaxBirfurcation(item, itemtype, type)
                    cartModel.taxlistDynamic = listOf(itemtype)
                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(cartModel.taxlistDynamic)
                    )

                }
            }

        }

        return cartModel
    }

    fun taxBifurcationCalculationNew(
        item: TbCartItem, cartModel: CartModel, type: String, orderTaxID: Boolean
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
                                Log.e("CheckNotFound", "quantityMod:  ${it.itemQuantity}")
                                modifierPrice += (it.price * it.itemQuantity)
                            }

                            val totalPrice = price + modifierPrice
                            itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                        } else {
                            itemtype.subTotalAmount = 0.0
                        }
                        var ttaxPrice = getTotalTaxBirfurcationNew(item, itemtype, type)
                        Log.e("checkTotalTax", "TaxPrice: ${ttaxPrice}")
                        itemtype.totalTaxTypePrice = ttaxPrice


                        try {
                            cartModel.taxlistDynamic =
                                concatenate(cartModel.taxlistDynamic!!, listOf(itemtype))


                            prefProvider.setValue(
                                Constants.taxListDynamic,
                                Gson().toJson(cartModel.taxlistDynamic)
                            )
                        } catch (E: Exception) {
                        }

                    } else {
                        if (itemtype.taxType != "Percentage") {
                            var modifierPrice: Double = 0.0
                            val price =
                                (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                            item.modifiers.forEach {
                                Log.e("CheckNotFound", "quantityMod 2:  ${it.itemQuantity}")
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
                                Log.e(
                                    "CheckPassing", "method: ${
                                        getTotalTaxBirfurcationNew(
                                            item, itemtype, type
                                        )
                                    }"
                                )
                                Log.e(
                                    "CheckPassing",
                                    "amount: ${cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice}"
                                )
                                cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                                    cartModel.taxlistDynamic!![found].totalTaxTypePrice.plus(
                                        getTotalTaxBirfurcationNew(
                                            item, itemtype, type
                                        )
                                    )
                            }
                        } else if (type == DELETE) {
                            if (found <= cartModel.taxlistDynamic?.size!! - 1) {
                                cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                                    cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice?.minus(
                                        getTotalTaxBirfurcationNew(
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
                                    prefProvider.setValue(
                                        Constants.taxListDynamic,
                                        Gson().toJson(cartModel.taxlistDynamic)
                                    )


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
                                            prefProvider.setValue(
                                                Constants.taxListDynamic,
                                                Gson().toJson(cartModel.taxlistDynamic)
                                            )


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

                    Log.e("CheckItem", "item: ${Gson().toJson(item)}")
                    var ttaxPrice = getTotalTaxBirfurcationNew(item, itemtype, type)
                    Log.e("checkTotalTax", "TaxPrice 2: ${ttaxPrice}")
                    itemtype.totalTaxTypePrice = ttaxPrice
                    cartModel.taxlistDynamic = listOf(itemtype)
                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(cartModel.taxlistDynamic)
                    )

                }
            }

        }

        return cartModel
    }

    inline fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    private fun taxCalculation(item: TbItem, discountPrice: Double) {

        item.taxes?.forEach { tax ->
            if (tax.isActive && !tax.isDeleted) {


                Log.e("TodayCheck", "itemQuan ${item.itemQuantity}  quantity: ${item.quantity}")
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

    private fun taxCalculationNew(item: TbCartItem, discountPrice: Double) {

        item.taxes?.forEach { tax ->
            if (tax.isActive && !tax.isDeleted) {


                var modifierPrice = 0.0
                val price =
                    (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                item.modifiers.filterNot { it._destroy }.forEach {
                    it.itemQuantity = it.modifier_quantity * item.itemQuantity
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
                    it.itemQuantity = it.modifier_quantity * item.itemQuantity
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

    private fun taxCalculationReorderNew(item: TbCartItem) {
        item.taxes?.forEach { tax ->
            if (tax.isActive && !tax.isDeleted) {

                var modifierPrice = 0.0
                val price = (item.price * item.itemQuantity) - (item.discountPrice)

                item.modifiers.forEach {
                    var modQty = item.itemQuantity * it.modifier_quantity
                    modifierPrice += (it.price * modQty)
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
                    Handler(Looper.getMainLooper()).postDelayed(object : java.lang.Runnable {
                        override fun run() {
                            prefProvider?.setValueboolean(Constants.IS_PAX_CONNECTED, false)
                        }

                    }, 500)

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
        floorPlanDetails: DineInOrderDetailAttributes,
        cartItems: ArrayList<TbCartItem>
    ) {

        orderAttributeRequestModel = OrderAttributeRequestModel()

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

        /*for (i in 0 until cartModel.dineInList?.size!!) {
            cartModel.dineInList?.get(i)?.items?.forEach { item ->
                if (item.timeStamp == null || item.timeStamp!!.lowercase() == "null".lowercase()) {
                    item.timeStamp = randomOfflineId().toString()
                }
            }
        }*/
        cartItems.forEach { item ->
            if (item.timeStamp == null || item.timeStamp!!.lowercase() == "null".lowercase()) {
                item.timeStamp = randomOfflineId().toString()
            }
        }

        orderAttributeRequestModel.orderItemsAttributes =
            dineInOrderItemAttributed(cartModel, cartItems)


        orderAttributeRequestModel.guestsAttributes = getGuestsAttributesCreateOrder(cartModel)

//
//        val orderRequestModel = OrderRequestModel(false, orderAttributeRequestModel)
//
//
//        return orderRequestModel
    }

    fun dineInServiceChargeAppliedAttribute(cartModel: CartModel): List<OrderServiceChargesAttribute> {
        var guestCount = cartModel.dineInList?.size?.minus(1)
        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun getGuestsAttributesCreateOrder(cartModel: CartModel): List<GuestsAttributes> {
        val orderItemsAttributeList: ArrayList<GuestsAttributes> = arrayListOf()
        Log.e(TAG, "dineInListData:   ${Gson().toJson(cartModel.dineInList)}")
        CoroutineScope(Dispatchers.IO).launch {
            cartModel.dineInList?.forEachIndexed { index, it ->
                var cartItems = getDineInCartItems(index) as ArrayList<TbCartItem>
                val model = GuestsAttributes()
                model.name = it.title.toString()
                model.Destroy = it.isDestroy
                if (it.id != 0) {
                    model.id = it.id
                }
                if (cartItems?.isNotEmpty() == true) {
                    var listItems: ArrayList<GuestItemsAttributes> = arrayListOf()
                    var subTotal = 0.0
                    var totalTax = 0.0
                    var totalTips = 0.0
                    var totalDiscount = 0.0
                    var totalAmount = 0.0
                    cartItems.sortedBy { it.dineInSort }
                    cartItems.forEach { tb ->


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
                    var addressList: ArrayList<CustomerAttributes.AddressesAttribute> =
                        arrayListOf()
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
                        phoneModel.phoneNumber =
                            it.customer?.phones?.get(i)?.phone_number.toString()
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
            orderRequestModel = OrderRequestModel(false, orderAttributeRequestModel)
            dineInResultCreateOrder.postValue(true)
        }

        return orderItemsAttributeList

    }

    private fun getGuestsAttributes(cartModel: CartModel): List<GuestsAttributes> {
        val orderItemsAttributeList: ArrayList<GuestsAttributes> = arrayListOf()
        Log.e(TAG, "dineInListData:   ${Gson().toJson(cartModel.dineInList)}")


        CoroutineScope(Dispatchers.IO).launch {

            cartModel.dineInList?.forEachIndexed { index, it ->

                var cartItems = getDineInCartItems(index) as ArrayList<TbCartItem>
                val model = GuestsAttributes()
                model.name = it.title.toString()
                model.Destroy = it.isDestroy
                if (it.id != 0) {
                    model.id = it.id
                }
                if (cartItems?.isNotEmpty() == true) {
                    var listItems: ArrayList<GuestItemsAttributes> = arrayListOf()
                    var subTotal = 0.0
                    var totalTax = 0.0
                    var totalTips = 0.0
                    var totalDiscount = 0.0
                    var totalAmount = 0.0
                    cartItems.sortedBy { it.dineInSort }
                    cartItems.forEach { tb ->

                        Log.e(
                            TAG,
                            "dineInListDestory: IS_DESTROY: ${it.isDestroy} IS_DESTROY: ${tb.isDestroy}"
                        )

                        listItems.add(
                            GuestItemsAttributes(
                                id = tb.guestItemId,
                                orderItemId = tb.orderItemId,
                                quantity = tb.itemQuantity,
                                itemId = tb.itemId,
                                amount = tb.price,
                                timestamp = tb.timeStamp,
                                guestId = it.id?.let { it },
                                Destroy = tb.isDestroy
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
                    var phoneList: ArrayList<CustomerAttributes.PhonesAttribute> =
                        arrayListOf()
                    for (i in 0.until(it.customer?.addresses?.size!!)) {

                        var address = CustomerAttributes.AddressesAttribute()
                        address.address1 =
                            it.customer?.addresses?.get(i)?.address1.toString()
                        address.address2 =
                            it.customer?.addresses?.get(i)?.address2.toString()
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
                        phoneModel.phoneNumber =
                            it.customer?.phones?.get(i)?.phone_number.toString()
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

                model.Destroy = it.isDestroy
                model.isPaid = it.isPaid
                orderItemsAttributeList.add(model)

            }

            if (currentDestination == DINE_IN_UPDATE)
                dineInResult.postValue(true)

        }

        return orderItemsAttributeList

    }

    private fun dineInOrderItemAttributed(
        cartModel: CartModel,
        cartItems: ArrayList<TbCartItem>
    ): List<OrderItemsAttribute> {
        var orderItemsAttributeList: ArrayList<OrderItemsAttribute> = arrayListOf()

//        for (i in 0 until cartModel.dineInList?.size!!) {
        cartItems.forEach { item ->

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
            orderItemsAttribute.guestIndexForDineIn = item.guestIndexForDineIn

            orderItemsAttribute.isFired = item.isFired

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
                orderItemModifierAttributes(item, cartModel.terminalId, orderId ?: -1)

            orderItemsAttribute.orderItemVariationAttributes =
                orderItemVariationAttributes(item)



            if (item.variationsAttributes.isNotEmpty()) {
                orderItemsAttribute.variationId = item.variationsAttributes[0].id
            }

            orderItemsAttributeList.add(orderItemsAttribute)
        }
//        }


//        cartModel.items?.forEach {
//            cartModel?.dineInList?.forEach { m ->
        cartItems.forEach { it ->
            if (/*oi.name.equals(it.name, true) == false &&*/ it.isDestroy) {
                var model = OrderItemsAttribute()
                model.category_id = it.categoryId
                model.itemId = it.itemId
                model.id = it.orderItemId
                model.isDestroy = it.isDestroy
                model.isEdited = it.isEdited
                model.isFired = it.isFired
                orderItemsAttributeList.add(model)

            }
        }
//            }

//        }


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
        items: TbCartItem, orderId: Int? = null
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
                       // orderItemTaxesAttribute.orderId = orderId
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
        item: TbCartItem, terminalId: Int, orderId: Int = -1 ,
        isFromWastageItem: Boolean = false
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> = arrayListOf()

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                if (prefProvider.getValueboolean(
                        DINE_IN_UPDATE, false
                    ) && it.orderModifierId != null
                ) if(!isFromWastageItem) id = it.orderModifierId
                name = it.name
                price = it.price
                modifier_id = it.id
                order_item_id = item.orderItemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                it.modifierSetId?.let { modifier_set_id = it }
                quantity = it.itemQuantity
                order_item_taxes_attributes = orderModifierTaxesAttributes(item, it, terminalId)
                modifier_quantity = it.modifier_quantity

                if(orderId != -1)
                    this.orderId = orderId

                order_item_modifier_id = it.orderModifierId
                _destroy = it._destroy
            }
            orderItemModifierAttributeList.add(orderItemModifierAttribute)
        }

        return orderItemModifierAttributeList
    }

    private fun orderModifierTaxesAttributes(
        items: TbCartItem, modifier: Modifier, terminalId: Int
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
        item: TbCartItem
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
        Log.e("checkSubmit","${orderRequestModel.order.orderTypeName}")

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

    fun updateOrder(
        cartModel: CartModel,
        isFromDineInTable: Boolean = false,
        isAddGuest: Boolean = false
    ): OrderRequestModel {

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

            val isDineInUpdate = prefProvider.getValueboolean(DINE_IN_UPDATE, false)

            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
                orderId = prefProvider.getValueInt("DINE_IN_ORDER_UPDATE", 0)
                cartModel.orderId = orderId
            }
            orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
            orderTypeName = prefProvider.getValue(ORDER_TYPE_NAME, DINE_IN)
            tax_bifurcation_data = Gson().toJson(cartModel.taxlistDynamic)
            taxEnabled = true
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            totalAmount = MethodUtils.roundOffAmountDouble(totalPrice)
            totalDiscount = MethodUtils.roundOffAmountDouble(ttotalDiscount)
            totalServiceCharges = totalServiceCharge
            totalTaxAmount = totalTax

            val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
            if (customerId != -1) {
                customer_id = "" + customerId
            }

            /**
             *  currentDineInItems keeps track of all dine in Items even if they are destroyed
             */
            orderItemsAttributes = if (isFromDineInTable)
                dineInOrderItemAttributed(cartModel, currentCartItems)
            else
                dineInOrderItemAttributed(cartModel, currentDineInItems)

            /**
             * This is added only for adding new guest from the DineInTable Pays
             * */
            if (isAddGuest)
                orderItemsAttributes = dineInOrderItemAttributed(cartModel, currentCartItems)


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

        currentCartItems.forEach { item ->

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


    fun syncInventoryModule(b: Boolean, isMigrationOn: Boolean = false) {
        var needToUpdate = false
        CoroutineScope(Dispatchers.Main).launch {
            _showProgress.value = Event(true)
            _syncDone.value = Event(false)
        }

        viewModelScope.launch {
            val resource = posRepository.syncInventory(
                prefProvider.getValueInt(TERMINAL_ID, -1),

                if (isMigrationOn)
                    ""
                else
                    prefProvider.getValue(SYNC_TIME_STAMP, "")
            )
            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("SyncInventory", "SyncSuccess")

                    resource.data.let { response ->
                        if (response?.status == 200) {
                            Log.d("BINGE", "syncInventoryModule: START")
                            CoroutineScope(Dispatchers.Main).launch {
                                _showProgress.value = Event(false)
                            }

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
                                Log.d(
                                    "TAG",
                                    "Sync getAllCategoryList: response of API : " + Gson().toJson(
                                        model
                                    )
                                )

                                if (category.name == GIFT_CARD) {
                                    if (category.sort == 1) {
                                        prefProvider.setValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            true
                                        )
                                    } else {
                                        prefProvider.setValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            false
                                        )
                                    }
                                }
                                if (category.name == Constants.GIFT_CARD) {
                                    prefProvider.setValueInt(
                                        Constants.GIFT_CARD_SORT,
                                        category.sort
                                    )
                                }
                                if (category.name == Constants.DEFAULT_CATEGORY) {
                                    prefProvider.setValueInt(
                                        Constants.DEFAULT_CATEGORY_SORT,
                                        category.sort
                                    )
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
                                if (mCategory.size == 1 && !category.isDeleted) {
                                    if (prefProvider.getValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            false
                                        )
                                    ) {
                                        posRepository.updateSorting(
                                            Constants.DEFAULT_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.DEFAULT_CATEGORY_SORT,
                                                0
                                            ) + 1
                                        )
                                    } else {
                                        posRepository.updateSorting(
                                            Constants.GIFT_CARD_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.GIFT_CARD_SORT,
                                                0
                                            ) + 1
                                        )
                                        posRepository.updateSorting(
                                            Constants.DEFAULT_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.DEFAULT_CATEGORY_SORT,
                                                0
                                            ) + 2
                                        )
                                    }
                                }

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


                            /*launch {
                                modifierSetList.addAll(mData.modifierSets.sortedBy { it.sort })
                            }*/
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
/*
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
                            }
*/

                            async(Dispatchers.IO) {
                                Log.d(TAG, "syncInventoryModule: async started")
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
                            }.await().let {
                                Log.d(TAG, "syncInventoryModule: await")
                                appDatabase.modifierSetDao().addAll(listModifierSet)
                                appDatabase.itemModifierSetsDao().addAll(itemModifierSetList)
                            }

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

                        Log.e("CheckBValue", "checkBValue: ${b}")
                        if (!b) syncSettingModule()

                    }

                    autoSyncEnabled.value = true
                }

                Status.ERROR -> {
                    Log.e("SyncInventory", "SyncError")
                    CoroutineScope(Dispatchers.Main).launch {
                        _snackbarText.value = Event(resource.message.toString())
                        _showProgress.value = Event(false)
                    }

                    autoSyncEnabled.value = true
                }

                Status.LOADING -> {
                    Log.e("SyncInventory", "SyncLoading")
                    CoroutineScope(Dispatchers.Main).launch {
                        _showProgress.value = Event(false)

                    }

                    autoSyncEnabled.value = true
                }
            }
            autoSyncEnabled.value = true
        }
    }

    fun syncTaxes(closeSpinner: Boolean = true) {
//        _taxSyncDone.value = Event(false)
        viewModelScope.launch {

            CoroutineScope(Dispatchers.Main).launch {
                _syncProgressDialog.value = Event(true)
            }
            val resource = posRepository.syncInventory(
                prefProvider.getValueInt(TERMINAL_ID, -1),

                prefProvider.getValue(SYNC_TIME_STAMP, "")
            )
            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("SyncInventory", "SyncSuccess")

                    resource.data.let { response ->
                        if (response?.status == 200) {
                            Log.d("BINGE", "syncInventoryModule: START")
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
                                Log.d(
                                    "TAG",
                                    "Sync getAllCategoryList: response of API : " + Gson().toJson(
                                        model
                                    )
                                )

                                if (category.name == GIFT_CARD) {
                                    if (category.sort == 1) {
                                        prefProvider.setValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            true
                                        )
                                    } else {
                                        prefProvider.setValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            false
                                        )
                                    }
                                }
                                if (category.name == Constants.GIFT_CARD) {
                                    prefProvider.setValueInt(
                                        Constants.GIFT_CARD_SORT,
                                        category.sort
                                    )
                                }
                                if (category.name == Constants.DEFAULT_CATEGORY) {
                                    prefProvider.setValueInt(
                                        Constants.DEFAULT_CATEGORY_SORT,
                                        category.sort
                                    )
                                }


                                // if created new category by admin web panel
                                if (mCategory.size == 1 && !category.isDeleted) {
                                    if (prefProvider.getValueboolean(
                                            Constants.GIFT_CARD_AT_FIRST,
                                            false
                                        )
                                    ) {
                                        posRepository.updateSorting(
                                            Constants.DEFAULT_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.DEFAULT_CATEGORY_SORT,
                                                0
                                            ) + 1
                                        )
                                    } else {
                                        posRepository.updateSorting(
                                            Constants.GIFT_CARD_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.GIFT_CARD_SORT,
                                                0
                                            ) + 1
                                        )
                                        posRepository.updateSorting(
                                            Constants.DEFAULT_CATEGORY,
                                            prefProvider.getValueInt(
                                                Constants.DEFAULT_CATEGORY_SORT,
                                                0
                                            ) + 2
                                        )
                                    }
                                }

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

                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(1000)
                                    if (closeSpinner) {
                                        _syncProgressDialog.postValue(Event(false))
                                    }
//                                    _syncProgressDialog.postValue(Event(false))
                                    _taxSyncDone.value = Event(true)
                                }

                            }




                            prefProvider.setValue(SYNC_TIME_STAMP, response.data.timeStamp)

                            if (syncMarkeup) {
                                syncMarkeup = false
                                prefProvider.setValueboolean(IS_SYNC_MARKUP, false)
                            }
                            Log.d("BINGE", "syncInventoryModule: END")
                        } else {
                            _tableStatus.value = response?.let { Event(it.message) }
                        }

//                        syncSettingModule()

                    }

                    autoSyncEnabled.value = true
                }

                Status.ERROR -> {
                    Log.e("SyncInventory", "SyncError")
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                    autoSyncEnabled.value = true
                }

                Status.LOADING -> {
                    Log.e("SyncInventory", "SyncLoading")
                    _showProgress.value = Event(false)

                    autoSyncEnabled.value = true
                }
            }

        }
    }

    suspend fun addOrderTypesToDatabase(orderTypes: List<TbOrderType>) {
        viewModelScope.launch {
            posRepository.addOrderType(orderTypes)
        }
    }

    /**
     * "Deletes the customer database table. Added to handle scenarios where a customer is deleted from the backend and the application is closed."
     */
    fun deleteCustomersTable() {
        viewModelScope.launch(Dispatchers.IO) {
            posRepository.deleteCustomersTable()
        }
    }

    fun syncSettingModule() {
        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("TOMIN", "SUCCESS")
                    Log.e("BINGE", "syncSettingModule: START")
                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            posRepository.deleteKitchenPrinters()
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

                                if (prefProvider.getValue(SYNC_SETTING_TIME_STAMP, "").isEmpty()) {
                                    prefProvider.setValueboolean(
                                        Constants.IS_FIRST_TIME_LOGIN,
                                        true
                                    )
                                } else {
                                    prefProvider.setValueboolean(
                                        Constants.IS_FIRST_TIME_LOGIN,
                                        false
                                    )
                                }

                                prefProvider.setValueboolean(
                                    IS_PRINTER_QUEUE_ENABLE,
                                    it.settingData.data.isPrinterQueueEnable
                                )

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

                                //pre auth option ON / OFF

                                try {
                                    prefProvider.setValueboolean(
                                        Constants.IS_PRE_AUTH_ENABLE,
                                        it.settingData.data.isPreAuthEnable
                                    )
                                    isPreAuthCartOpened.value = it.settingData.data.isPreAuthEnable
                                } catch (e: Exception) {

                                }


                                val intent = Intent()
                                intent.action = Constants.MASTER_TEMINAL_CHANGED
                                _masterTerminal.value = Event(true)
                                prefProvider.setValue(
                                    Constants.QUEUE_SYNC_TIME_STAMP,
                                    System.currentTimeMillis().toString()
                                )


                                /*  val intent = Intent()
                                  intent.action = Constants.MASTER_TEMINAL_CHANGED
                                  MainApplication.getInstance()?.baseContext?.sendBroadcast(intent)
                                  prefProvider.setValue(
                                      QUEUE_SYNC_TIME_STAMP,
                                      System.currentTimeMillis().toString()
                                  )
*/

                                /*--------------------Set the payment type------------------*/
                                CoroutineScope(Dispatchers.IO).launch {
                                    var activePaymentType = posRepository.getActivePaymentGateway()
                                    prefProvider.setValue(Constants.PAYMENT_GATEWAY_TYPE, venueDetailsResponse.settingData.data.activatedPaymentGateway ?: "")


                                    /* var apiKey = "k3FhfL$$8vu#NEDlfuJwP62MzIeA7Csz"
                                     var appID = "GmehAw69S9TEHKm3Bmz2yvxQybYJLgIp"
                                     var channelID = "bd967b4e0ccd6309c5ac16634bd367b6"
                                     var epi = "2319995597"*/

                                    var foundTerminal = venueDetailsResponse.settingData.data.terminals.filter { term ->
                                        term.name.equals(
                                            prefProvider.getValue(
                                                Constants.TERMINAL_NAME, ""
                                            ), ignoreCase = true
                                        )
                                    }
                                    if (venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.PAX, ignoreCase = true)) {
                                        prefProvider.clearValorPaymentDetails()
                                        prefProvider.clearDejavooPaymentDetails()
                                    } else if (venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.VALOR, ignoreCase = true)) {
                                        prefProvider.clearDejavooPaymentDetails()
                                        prefProvider.clearPaxPaymentDetails()
                                    } else if (venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.DEJAVOO, ignoreCase = true)) {
                                        prefProvider.clearValorPaymentDetails()
                                        prefProvider.clearPaxPaymentDetails()
                                    }
                                    if (foundTerminal.isNotEmpty()) {
                                        if (!venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.PAX, ignoreCase = true)) {
                                            if (venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(
                                                    Constants.VALOR,
                                                    ignoreCase = true
                                                ) || venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.VELOR, ignoreCase = true)
                                            ) {

                                                withContext(Dispatchers.Main) {
                                                    prefProvider.setValue(
                                                        VALOR_APP_ID, foundTerminal.get(0).app_id/*appID*/ ?: ""
                                                    )

                                                    prefProvider.setValue(
                                                        VALOR_APP_KEY, foundTerminal.get(0).app_key/*apiKey*/ ?: ""
                                                    )

                                                    prefProvider.setValue(
                                                        VALOR_EPI, foundTerminal.get(0).epi/*epi*/ ?: ""
                                                    )
                                                    prefProvider.setValue(
                                                        VALOR_CHANNEL_ID, foundTerminal.get(0).channel_id/*channelID*/ ?: ""
                                                    )
                                                }

                                            } else if (venueDetailsResponse.settingData.data.activatedPaymentGateway.equals(Constants.DEJAVOO)) {
                                                prefProvider.setValue(
                                                    DEJAVOO_AUTH_KEY, foundTerminal.get(0).dejavoo_auth_key/*appID*/ ?: ""
                                                )

                                                prefProvider.setValue(
                                                    DEJAVOO_REGISTER_ID, foundTerminal.get(0).dejavoo_register_id/*apiKey*/ ?: ""
                                                )

                                                prefProvider.setValue(
                                                    DEJAVOO_TPN, foundTerminal.get(0).dejavoo_tpn/*epi*/ ?: ""
                                                )
                                                prefProvider.setValue(
                                                    DEJAVOO_AUTH_TOKEN, foundTerminal.get(0).dejavoo_auth_token/*channelID*/ ?: ""
                                                )

                                            }
                                        }

                                    }
                                    if (activePaymentType.isEmpty()) {
//                                    Insert to DB
                                        posRepository.insertActivePaymentGateway(ActivePaymentGateway(type = venueDetailsResponse.settingData.data.activatedPaymentGateway))
                                    } else {
//                                    Update to DB
                                        activePaymentType.first().type = venueDetailsResponse.settingData.data.activatedPaymentGateway
                                        posRepository.updateActivePayment(activePaymentType.first())
                                    }
                                }
                                /*--------------------Set the payment type------------------*/

                                try {

                                    if (it.settingData.data.logo != null) {
                                        if (it.settingData.data.logo.logoUrl.isNotEmpty() && !prefProvider.getValue(
                                                Constants.VENUE_LOGO_URL, ""
                                            ).equals(it.settingData.data.logo.thumb.thumbUrl)
                                        ) {
                                            CoroutineScope(Dispatchers.IO).launch {
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


                                    }

                                } catch (e: Exception) {
//                                    e.printStackTrace()
                                }

                                /*------------VALOR---------------*/

                                /* var apiKey = "k3FhfL$$8vu#NEDlfuJwP62MzIeA7Csz"
                                 var appID = "GmehAw69S9TEHKm3Bmz2yvxQybYJLgIp"
                                 var channelID = "bd967b4e0ccd6309c5ac16634bd367b6"
                                 var epi = "2319995597"*/

                                /* var foundTerminal=it.settingData.data.terminals.filter { term-> term.name.equals(prefProvider.getValue(
                                     Constants.TERMINAL_NAME, ""
                                 ),ignoreCase = true) }
                                 if (foundTerminal.isNotEmpty()){
                                     prefProvider.setValue(
                                         VALOR_APP_ID, foundTerminal.get(0).app_id*//*appID*//* ?: ""
                                    )

                                    prefProvider.setValue(
                                        VALOR_APP_KEY, foundTerminal.get(0).app_key*//*apiKey*//* ?: ""
                                    )

                                    prefProvider.setValue(
                                        VALOR_EPI, foundTerminal.get(0).epi*//*epi*//* ?: ""
                                    )
                                    prefProvider.setValue(
                                        VALOR_CHANNEL_ID, foundTerminal.get(0).channel_id*//*channelID*//* ?: ""
                                    )
                                }*/
                                /*------------VALOR---------------*/


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

                                /*Added by Rahul Pandit to solve PA1-I812*/
                                withContext(Dispatchers.IO) {
                                    var allNotes = posRepository.allNoteList()
                                    allNotes.forEach { localNote ->
                                        var found = it.settingData.data.notes.filter { it.id == localNote.id && it.name.equals(localNote.name) }
                                        if (found.isEmpty()) {
                                            posRepository.deleteNoteDatabase(localNote.id)
                                        }
                                    }
                                }
                                /*Added by Rahul Pandit to solve PA1-I812*/

                                /*Added by Rahul Pandit to solve PA1-I812*/
                                withContext(Dispatchers.IO) {
                                    var allDiscounts = tipDiscountRepository.allDiscountsList()
                                    allDiscounts.forEach { localDiscount ->
                                        var found = it.settingData.data.discounts.filter { it.id == localDiscount.id && it.name.equals(localDiscount.name) }
                                        if (found.isEmpty()) {
                                            tipDiscountRepository.deleteDiscountDatabase(localDiscount.id)
                                        }
                                    }
                                }
                                /*Added by Rahul Pandit to solve PA1-I812*/

//                                tipDiscountRepository.deleteDiscountsFromDb()
//                                posRepository.deleteNotesFromDb()
                                posRepository.addAllNotesDatabase(it.settingData.data.notes)
                                if (it.settingData.data.discounts.size != tipDiscountRepository.allDiscountList().value?.data?.size) {
                                    tipDiscountRepository.deleteDiscountsFromDb()
                                }
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

                                withContext(Dispatchers.IO) {

                                    var allTips = tipDiscountRepository.allTipsList()
                                    allTips.forEach { localTip ->
                                        var found = it.settingData.data.tip_settings.filter { it.id == localTip.id && it.name.equals(localTip.name) }
                                        if (found.isEmpty()) {
                                            tipDiscountRepository.deleteTipDatabase(localTip.id)
                                        }
                                    }
                                }
//                                posRepository.deleteCustomerReceiptSettingsFromDb()
                                posRepository.addCancelOrderReasonFromDb(it.settingData.data.cancelOrderReasons)
                                /*Added by Rahul Pandit to solve PA1-I812*/
                                withContext(Dispatchers.IO) {
                                    var allReasons = posRepository.allCancelOrderReasons()
                                    allReasons.forEach { localReason ->
                                        var found = it.settingData.data.cancelOrderReasons.filter { it.id == localReason.id }
                                        if (found.isEmpty()) {
                                            posRepository.deleteCancelOrderReasonDatabase(localReason.id)
                                        }
                                    }
                                }
                                /*Added by Rahul Pandit to solve PA1-I812*/
                                posRepository.addWastageReasonInDb(it.settingData.data.wastageReasons)

                                /*Added by Rahul Pandit to solve PA1-I812*/
                                withContext(Dispatchers.IO) {
                                    var allReasons = posRepository.allWastageReasons()
                                    allReasons.forEach { localReason ->
                                        var found = it.settingData.data.wastageReasons.filter { it.id == localReason.id }
                                        if (found.isEmpty()) {
                                            posRepository.deleteWastageReasonDB(localReason.id)
                                        }
                                    }
                                }
                                /*Added by Rahul Pandit to solve PA1-I812*/

//                                posRepository.deleteCustomerPrinters()
//                                posRepository.deleteKitchenPrinters()
                                if (it.settingData.data.printers.kitchenPrinterList.isEmpty()) {
                                    posRepository.deleteKitchenPrinters()
                                } else {
                                    posRepository.addKitchenPrinter(it.settingData.data.printers.kitchenPrinterList)

                                }
                                val custList = it.settingData.data.printers.customerPrinterList
                                custList.forEach {
                                    it.name = it.name.ifEmpty { "" }
                                    it.modalName = it.modalName.ifEmpty { "" }
                                }
                                if (it.settingData.data.printers.customerPrinterList.isEmpty()) {
                                    posRepository.deleteCustomerPrinters()
                                } else {
                                    posRepository.addCustomerPrinter(custList)
                                }
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
                                posRepository.deleteLoyaltyProgramFromDb()
                                if (it.settingData.data.loyaltyPrograms.isNotEmpty()) {
                                    posRepository.addLoyaltyProgramFromDb(it.settingData.data.loyaltyPrograms)
                                }

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
                                            return@forEach
                                        } else {
                                            prefProvider.saveActiveLoyaltyData(null)
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

                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        runBlocking {
                                            try {
                                                var printOrderId = posRepository.getLabelPrinterSettingsData().printOrderId ?: true

                                                posRepository.insertOrUpdateLabelPrinter(it.settingData.data.oneItemPerReciept, printOrderId)
                                            } catch (e: Exception) {
                                                posRepository.insertOrUpdateLabelPrinter(it.settingData.data.oneItemPerReciept, true)
                                            }

                                        }

                                    }

                                } catch (e: Exception) {

                                }
//                                posRepository.deleteTeamRoleFromDb()
//                                posRepository.addTeamRoleFromDb(it.data.teamRoles)
//                                posRepository.deleteAllEmployee()
                                posRepository.employeeListAddAllFromSeeting(it.settingData.data.employee)
//                                rolePermission.findCurrentUserRoleAndSave(it.data.teamRoles)
//                                posRepository.deleteOrderTypeFromDb()

                                Log.e("checkHereDB", "OrderTypesComing")
                                posRepository.deleteOrderTypeFromDb()
                                //comment this scope due to dine in order type was not reflecting after sync from backend.
                                /*CoroutineScope(Dispatchers.IO).launch {
                                    var orderTypesList :kotlin.collections.ArrayList<TbOrderType> = posRepository.getAllOrderTypes() as ArrayList<TbOrderType>
                                    if (orderTypesList.size>=it.settingData.data.orderTypes.size){
                                        var removedIDs= arrayListOf<Int>()
                                        orderTypesList.removeAll(it.settingData.data.orderTypes)
                                        orderTypesList.forEach {
                                            launch {
                                                posRepository.deleteOrderTypesById(it.id)
                                            }
                                        }
                                    }

                                }*/



                                CoroutineScope(Dispatchers.IO).launch {
                                    it.settingData.data.dynamicPaymentRecords.forEach {
                                        if (it.deleted_at == null) {
                                            launch {
                                                insertDynamicPayment(it)
                                            }
                                        } else {
                                            launch {
                                                posRepository.deleteDynamicPaymentByName(
                                                    it.name + "",
                                                    it.createdAt + ""
                                                )
                                            }
                                        }
                                    }
//                                    var dynamicPaymentList :kotlin.collections.ArrayList<TbDynamicPaymentRecords> = posRepository.getAllDynamicPayments() as ArrayList<TbDynamicPaymentRecords>
                                    /* if (dynamicPaymentList.size>=it.settingData.data.dynamicPaymentRecords.size){
                                         var removedIDs= arrayListOf<Int>()
                                         dynamicPaymentList.removeAll(it.settingData.data.dynamicPaymentRecords)
                                         dynamicPaymentList.forEach {
                                             launch {
                                                 posRepository.deleteDynamicPaymentById(it.id)
                                             }
                                         }
                                     }*/
                                }

                                posRepository.addOrderType(it.settingData.data.orderTypes)

                                /* CoroutineScope(Dispatchers.IO).launch {
                                     insertDynamicPayment(it.settingData.data.dynamicPaymentRecords)
                                 }
 */
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
                    autoSyncEnabled.value = true
                }

                Status.ERROR -> {
                    Log.e("TOMIN", "ERROR")
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                    autoSyncEnabled.value = true
                }

                Status.LOADING -> {
                    Log.e("TOMIN", "LOADING")
                    _showProgress.value = Event(true)
                    autoSyncEnabled.value = true
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

    fun saveManualSaleDataNew(cartItems: List<TbCartItem>) {
        addOrderItemsToCartItems(cartItems)
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

                    activeOrderTypeText = it.name
                    activeOrderTypeName = it.orderType
                    activeOrderTypeId = it.id
                    var foundedList: List<OrderTypeBackup>? = null
                    CoroutineScope(Dispatchers.IO).async {
                        async {
                            foundedList = findOrderTypeBackup(
                                it.id,
                                activeOrderTypeText,
                                model.employeeID.toInt()
                            )
                        }.await()

                        async {
                            try {
                                foundedList?.let { founded ->
                                    if (founded.isNullOrEmpty()) {
                                        var orderTypebackup = OrderTypeBackup()
                                        orderTypebackup.orderType = it.id
                                        orderTypebackup.employeeId = model.employeeID
                                        orderTypebackup.orderTypeName = activeOrderTypeText
                                        insertOrderTypeBackup(orderTypebackup)
                                    }
                                }

                            } catch (e: Exception) {
                            }
                        }.await()
                    }

                }
            }
            prefProvider.setValueInt(Constants.ORDER_TYPE_ID, model.orderTypeId)
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
                        it.itemQuantity = it.modifier_quantity * item.itemQuantity
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

    fun itemCalculationForDineInPaymentNew(
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

        if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
            totalServiceCharge = 0.0
        var amountToBePaid = 0.0
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

        //totalAmmount = totalPrice-cartList[0].discountPrice


    }

    var isLoading = MutableLiveData<Boolean>()

    fun downloadFinished(value: Boolean) {
//        isLoading.value = value
        try {
            isLoading.postValue(value)
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }


    fun updateActiveOrderFlagClear() {
        try {

            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, false)
            prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, -1)
            prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_PAYMENT_ID, -1)
            prefProvider.setValue(Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID, "")
            prefProvider.setValue(Constants.IS_UPDATE_ORDER_OFFLINE_ID, "")
            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)
            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        } catch (e: java.lang.Exception) {
        }

    }

    fun unableToRemoveGuest(message: String = "") {
        _removeGuestSuccess.value = Event(message)
    }


    public fun addCashDiscountForCustomerDisplay(cashDiscount: Double = 0.0) {
        _latestDiscount.value = cashDiscount
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
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        clearCustomer()
        deleteCart()
    }

    // To clear customer if creating gift card
    fun clearCustomer() {
        prefProvider.saveCustomerData(null)
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        selectedCustomer = null
        assignCustomer = null
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        prefProvider.setValueboolean(LOYALTY_ADDED, false)
    }


    fun deleteCustomer(currentCartId: Int?) {
        CoroutineScope(Dispatchers.IO).launch {
//            updateOrderCall(orderId, orderRequestModel)
            clearCustomer()
            posRepository.removeCustomer(currentCartId!!)
        }

    }

    fun setCartEdited(isEdited: Int, currentCartId: Int?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                posRepository.setCartEdited(isEdited, currentCartId!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    suspend fun getAllCartModels(): List<CartModel> {
        return posRepository.getAllCartModels()
    }

    suspend fun getCartModelFromID(cartId: Int): CartModel? {
        return posRepository.getCartModelFromID(cartId)
    }

    suspend fun insertOrderTypeBackup(orderTypeBackup: OrderTypeBackup): Long? {
        return posRepository.addOrderTypeBackup(orderTypeBackup)
    }

    suspend fun findOrderTypeBackup(
        orderType: Int,
        orderTypeName: String,
        employeeId: Int
    ): List<OrderTypeBackup> {
        return posRepository.findOrderTypeBackup(orderType, employeeId, orderTypeName)
    }

    suspend fun deleteOrderTypeBackup(orderType: Int, employeeId: Int) {
        viewModelScope.launch {
            posRepository.deleteOrderTypeBackup(orderType, employeeId)
        }
    }

    fun deleteOrderTypeBackupByName(employeeId: Int) {
        viewModelScope.launch {
            posRepository.deleteOrderTypeBackupByName(employeeId)
        }
    }

    suspend fun updateOrderTypeBackup(orderType: Int, orderTypeName: String, employeeId: Int) {
        viewModelScope.launch {
            posRepository.updateOrderTypeBackup(orderType, orderTypeName, employeeId)
        }
    }


    suspend fun getLabelPrinterSettingsData(): TbLabelPrinterSettings {
        return posRepository.getLabelPrinterSettingsData()
    }

    fun updateOrderId(printOrderId: Boolean) {
        viewModelScope.launch {
            var insertedRows = posRepository.updateOrderId(printOrderId)
            if (insertedRows > 0) {
                _printOrderIdInStickyReceipt.postValue(true)
            } else {
                _printOrderIdInStickyReceipt.postValue(false)
            }
        }
    }

    /*-------------Customer Loyalty------------------*/
    fun addCustomersList(currentPage: Int, data: List<TbCustomer>, lastCall: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            var data = posRepository.addCustomersList(data)
            if (!lastCall) {
                _loadCustomersList.postValue(Pair(currentPage, true))
            }
//            _loadCustomersList.postValue(Pair(currentPage,false))
            /*Check for Identical if not identical then find then call the livedata which will call the recursive function*/
            Log.d("addCustomersList:S", "$data")
        }
    }
    /*-------------Customer Loyalty------------------*/

    //    ----------------- Dynamic Payments -----------------------------
    suspend fun insertDynamicPayment(tbDynamicPaymentRecords: TbDynamicPaymentRecords) {
        posRepository.insertDynamicPayments(tbDynamicPaymentRecords)
    }

    suspend fun insertDynamicPayment(tbDynamicPaymentRecords: List<TbDynamicPaymentRecords>) {
        posRepository.insertDynamicPayments(tbDynamicPaymentRecords)
    }

    fun getDynamicPaymentRecords(
        isActive: Boolean,
        locationId: Int
    ): Flow<List<TbDynamicPaymentRecords>> {
        return posRepository.getDynamicPaymentRecords(isActive, locationId)
    }
    //    ----------------- Dynamic Payments -----------------------------

    fun makeCashInOutCallFromCustomerDisplay(cashLogRequest: CashLogRequest) {
        viewModelScope.launch {
            val resource = posRepository.cashInOut(cashLogRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let {

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

    override fun onCleared() {
        Log.e("CheckOnClearedViewmodel", "DashboardCategoryBoldPOS")
        super.onCleared()
    }


    fun checkCardExistOrNot(cardNumber: String) {

        _showProgress.value = Event(true)
        viewModelScope.launch {
            var resource = posRepository.checkPhysicalCardExistsOrNot(cardNumber)

            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _showProgress.value = Event(false)

                            _physicalGiftCardCheck.value = Event(response?.status ?: 400)


                        } else {
                            _physicalGiftCardCheck.value = Event(response?.status ?: 400)
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                    _physicalGiftCardCheck.value = Event(resource.data?.status ?: 400)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }


    }

    fun checkCardExistOrNotOnSell(cardNumber: String) {

        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.checkPhysicalCardExistsOrNot(cardNumber)

            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _showProgress.value = Event(false)
                            _isGiftCardSold.value = Event(false)
                            _snackbarText.value = Event("This gift card has not been activated.")
//                            _physicalGiftCardCheck.value = Event(response?.status?: 400)

                        }
//                        else {
//                            _physicalGiftCardCheck.value= Event(response?.status?: 400)
//                        }
                    }
                }

                Status.ERROR -> {
                    if (resource.message?.isNotEmpty() == true && resource.message.contains("Gift Card number has already been taken.")) {
                        _isGiftCardSold.value = Event(true)
                    } else {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }
//                    _physicalGiftCardCheck.value= Event(resource.data?.status ?: 400)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }

    }
}