package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.aidl.ICallback
import com.android.pos.aidl.IWoyouService
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD_VALUE
import com.android.pos.data.remote.Constants.BALANCE_INQUIRY
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.GIFT_CARD
import com.android.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.android.pos.data.remote.Constants.IS_PAYMENT_SCREEN
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.KITCHENANDCUSTOMER
import com.android.pos.data.remote.Constants.LARGE
import com.android.pos.data.remote.Constants.MAX_ITEM_QUANTITY
import com.android.pos.data.remote.Constants.MEDIUM
import com.android.pos.data.remote.Constants.ONLINE_ORDER_ENABLE
import com.android.pos.data.remote.Constants.OPEN_ORDER_ITEMS
import com.android.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.SELL_CARD
import com.android.pos.data.remote.Constants.SMALL
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.allorders.AllOrdersViewModel
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.ESCUtil
import com.android.pos.ui.fragments.settings.hardware.printer.PrinterViewModel
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.ui.fragments.settings.servicecharge.ServiceChargeListViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.DineInOrderCallBack
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.scanner.helpers.ScannerAppEngine
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.epsonio.DevType
import com.epson.epsonio.DeviceInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.zebra.scannercontrol.FirmwareUpdateEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class DashboardCategoryBoldPOS() : Fragment(), ItemListner, ItemClickListner,
    ScannerAppEngine.IScannerAppEngineDevEventsDelegate, ICallback, DineInOrderCallBack {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var presentation: CustomDisplay
    private var dineInList: List<DineInModel>? = null
    private var woyouService: IWoyouService? = null
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private val viewModelServiceCharge by viewModels<ServiceChargeListViewModel>()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val allOrdersViewModel by activityViewModels<AllOrdersViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val printerViewModel by viewModels<PrinterViewModel>()
    private var serviceChargesList: ArrayList<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private var orderTypeObserver: Observer<Resource<List<TbOrderType>>>? = null
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    private val TAG = "DashboardCategoryBold"
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    var isupdate = false
    var reorder = false
    var orderDiscount = 0.0
    var dineInResult: Bundle? = null
    var resultData: TbCustomer? = null
    var cashDiscountType = ""
    lateinit var cashDiscountModel: CashDiscountModel
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()
    var handler = Handler()

    @Inject
    lateinit var rolePermission: RolePermission
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int? = null

    @Inject
    lateinit var prefProvider: PrefProvider

    companion object {
        private lateinit var binding: FragmentDashboardCategoryBoldPosBinding
        fun newInstance() = DashboardCategoryBoldPOS()
    }

    fun keypadShow(b: Boolean) {

        if (b)
            binding.layoutHeader.txtKeypad.visible()
        else
            binding.layoutHeader.txtKeypad.gone()
    }

    fun onlineOrderBadgeDisplay(count: Int) {
        if (count != null) {
            if (count > 0) {
                binding.layoutHeader.txtBadgeCount?.visible()
                binding.layoutHeader.txtBadgeCount.blink()
                binding.layoutHeader.txtBadgeCount?.text = count.toString()
            } else {
                binding.layoutHeader.txtBadgeCount.clearAnimation()
                binding.layoutHeader.txtBadgeCount?.gone()
            }
        } else {
            binding.layoutHeader.txtBadgeCount.clearAnimation()
            binding.layoutHeader.txtBadgeCount?.gone()
        }
    }


    private fun View.blink(
        times: Int = Animation.INFINITE,
        duration: Long = 500L,
        offset: Long = 20L,
        minAlpha: Float = 0.45f,
        maxAlpha: Float = 1.0f,
        repeatMode: Int = Animation.REVERSE
    ) {
        startAnimation(AlphaAnimation(minAlpha, maxAlpha).also {
            it.duration = duration
            it.startOffset = offset
            it.repeatMode = repeatMode
            it.repeatCount = times
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkCashDrawerObserver()
        Binding()
        prefProvider.setValue(Constants.REDIRECT_FROM, "")
        //prefProvider.setValue(Constants.OPEN_ORDER_ITEMS, "")

        if(prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD){
            viewModel.clearGiftCardCart()
        }

//        hideSystemUI()

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        syncData()
        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                viewModel,
                passcodeViewModel,
                dineInViewModel

            )
        }
        prefProvider.setValueboolean(IS_PAYMENT_SCREEN, false)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
//        if (prefProvider.getValueboolean(ONLINE_ORDER_ENABLE, false)) {
//            binding.layoutHeader.linearOnlineorder?.visible()
//            viewModel.getOnlineOrderCount()
//        } else {
//            binding.layoutHeader.linearOnlineorder?.gone()
//        }
        getOrderTypes()
        observeSaveOrder()
        getKitchenReceiptSettings()
        addObserver()
        getServiceCharges()
        resultListener()
        observeQueueCreate()
        dineInUpdateOrder()
        navigateDineInOrder()
        getLoyaltyPrograms()

        checkDineInEditOrder()
        printerProgress()
        observeShowProgress()
        allOrdersPendingCountObserver()
        getDineInData()
        checkSearch()
        observeServiceChargeUpdate()
        observerSyncItemPriceChange()
        prefProvider.setValueboolean(Constants.ORDER_COMPLETED, false)



        binding.layoutHeader.ivLock.setOnClickListener {

            alert(
                getString(R.string.app_name),
                prefProvider.employeeName() + ", Are you sure, you want to clockout?"
            ) {
                positiveButton(getString(android.R.string.ok)) {
                    viewModel.clockOut()


                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
        }

        return binding.root
    }


    private fun checkCashDrawerObserver() {
        viewModel.checkCashDrawerPer.observe(viewLifecycleOwner) {
            if (it) {
                checkCashDrawerPer()
            }
        }
    }

    private fun checkCashDrawerPer() {
        Log.e("UserPermissionCash", "InsideCheckPermissionCash: ")
        if (rolePermission.hasCashDrawerPermission()) {
            Log.e("UserPermissionCash", "HasRole")
            binding.layoutHeader.imgCashdDrawer.visible()
        } else {
            Log.e("UserPermissionCash", "HasRoleNo")
            binding.layoutHeader.imgCashdDrawer.gone()
        }

    }

    private fun observerSyncItemPriceChange() {
        viewModel.syncInventroyForPriceChange.observe(requireActivity(), Observer {
            if (isAdded) {
                loadCartFragment(CartFragment(this, this, dineInCallback = this, isFromDashboard = true))
                loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
            }
        })

    }

    private fun observeServiceChargeUpdate() {

        viewModelServiceCharge.dataupdate.observe(requireActivity(), Observer {
            getServiceCharges()
        })
    }

    private fun checkSearch() {
        binding.layoutHeader.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString() == " ") {
                    binding.layoutHeader.edtSearch.setText("")
                }
                if (requireActivity().supportFragmentManager.findFragmentById(R.id.frameLayout)?.javaClass?.name.equals(
                        "com.android.pos.ui.fragments.dashboard.bolddashboard.AddItemFragment", true
                    )
                ) {

                    requireActivity().supportFragmentManager.popBackStackImmediate(
                        AddItemFragment.javaClass.getName(),
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                }


            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })
    }

    private fun getwebOrderingCountObserver() {
        viewModel.onlineOrderCount.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it != null) {

                    Log.d(TAG, "getwebOrderingCount: " + it.count)
                    onlineOrderBadgeDisplay(it.count)
                }
            }
        }
    }

    private fun allOrdersPendingCountObserver() {
        allOrdersViewModel.allOrderCounts("","").observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        Log.d(TAG, "getAllOrderCounts: ${it.data?.data}")

                        val allOrdersPendingCount = it.data?.data?.all_orders?.pending ?: 0
                        onlineOrderBadgeDisplay(allOrdersPendingCount)

                    }

                    Status.ERROR -> {
                        Log.e(TAG, "getAllOrderCounts: ERROR - ${it.message}")
                    }

                    Status.LOADING -> {
                        Log.d(TAG, "getAllOrderCounts: LOADING")
                    }
                }
            }
        }
    }

    private fun getOnlineOrderIsEnableOrNot() {
        viewModel.enableOnlineOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (prefProvider.getValueboolean(ONLINE_ORDER_ENABLE, false)) {
                    binding.layoutHeader.linearOnlineorder?.visible()
                    viewModel.getOnlineOrderCount()
                } else {
                    binding.layoutHeader.linearOnlineorder?.gone()
                }
            }
        }
    }

    private fun getLoyaltyPrograms() {
        viewModel.activeLoyaltyProgram = prefProvider.getActiveLoyaltyData()
        viewModel.activeLoyaltyProgramLiveData.observe(requireActivity()) {
            if (it.status == Status.SUCCESS && it.data != null) {
                LogUtil.logE("Loyalty", "getLoyaltyPrograms fetched..")
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            } else {
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            }
        }
    }

    private fun resultListener() {

        setFragmentResultListener("request_key_customer") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                LogUtil.logE(TAG, "gotBundlebundle:  ${Gson().toJson(bundle)}")
                setUpCustomer(result, bundle)
            }
        }

        setFragmentResultListener("request_key_discount_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            val value = bundle.getDouble("value")
            if (result != null && viewModel.totalPrice != 0.0) {
                try {
                    orderDiscount = MethodUtils.roundOffAmountDouble(result.percentage)

                    val discountApplyPrice = viewModel.totalPrice
                    Log.d(TAG, "resultListener: orderDiscount : " + orderDiscount)
                    Log.d(TAG, "resultListener: totalprice : " + viewModel.totalPrice)
                    val price = discountApplyPrice - orderDiscount
                    Log.d(TAG, "resultListener: " + cartList.size)
                    if (viewModel.cartModel != null) {
                        viewModel.cartModel!!.discountPrice = orderDiscount
                        viewModel.cartModel!!.discountSelectdValue = value
                        viewModel.cartModel!!.discountType = result.discountType
                        if (result.id != -1) {
                            viewModel.cartModel!!.discountId = result.id
                        }
                        viewModel.addCart(viewModel.cartModel!!)
                    }
                    Log.d(TAG, "resultListener: " + Gson().toJson(viewModel.cartModel!!))
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            }
        }

        setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            val item = bundle.getParcelable<TbItem>("item")
            if (result != null) {
                when (result.discountType) {
                    requireContext().getString(R.string.disc_percentage) -> {

                        item?.discountPrice = item?.let { totalPrice(it) }?.let {
                            calculateDiscountPercentage(
                                it,
                                result.percentage
                            )
                        }!!
                        //discountPrice = item.discountPrice / item.itemQuantity
                        item.discountId = result.id
                        item.discountType = result.discountType

                        viewModel.newCartLogicModifier(cartList, item, Constants.UPDATE, false)

                    }

                    "Amount" -> {

                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType


                        viewModel.newCartLogicModifier(cartList, item, Constants.UPDATE, false)
                    }

                    else -> {
                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType

                        viewModel.newCartLogicModifier(cartList, item, Constants.UPDATE, false)

                    }
                }

            } else {
                item?.discountPrice = 0.0
                item?.discountType = ""
                item?.isManualSales = false
                item?.discountId = 0
                // discountPrice = data.discountPrice
            }

        }

        setFragmentResultListener("request_key_note") { requestKey: String, bundle: Bundle ->
            val note = bundle.getString("note")
            val isOrderNote = bundle.getBoolean("isOrderNote")
            val singleItem = bundle.getParcelable<TbItem>("item")
            // val cartList = bundle.getParcelableArrayList<CartModel>("cartList")
            var dineInArrayList: List<DineInModel>? = null
/*
            if (prefProvider.getValue(
                    ORDER_TYPE,
                    TAKEOUT
                ) ==DINE_IN
            ) {
                Log.e(TAG,"notecartlist$cartList")
                dineInArrayList = cartList?.get(0)?.dineInList
                dineInArrayList?.get(0)?.selectedPosition = bundle.getInt("headerPos")
            }
*/


            if (isOrderNote) {
                viewModel.addOrderNote(note.toString())
                /* cartList[0].note = note.toString()
                 viewModel.addCart(cartList[0])*/
            } else {
                singleItem?.note = note.toString()
                singleItem?.let {
                    dineInArrayList?.let { it1 ->
                        viewModel.newCartLogicModifier(
                            cartList,
                            it,
                            Constants.UPDATE,
                            false
                        )
                    }
                }
            }

        }

    }

    private fun setUpCustomer(result: TbCustomer, bundle: Bundle) {
        if (cartList.isNotEmpty()) {
            cartList[0].deliveryType = bundle.getString("TYPE").toString()
            cartList[0].customer = result
            viewModel.addCart(cartList[0])
            viewModel.setcheckedLoyaltyApply(false)
        }
    }

    private fun calculateDiscountPercentage(originalPrice: Double, percentage: Double): Double {
        return MethodUtils.roundOffAmountDouble((originalPrice * percentage) / 100)
    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price
            }

            (model.price) + totalPrice
        } else {

            model.price

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onClick()

        viewModel.showClockOutProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity() as MainActivity)
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
        if (prefProvider.getValueboolean(ONLINE_ORDER_ENABLE, false)) {
            binding.layoutHeader.linearOnlineorder?.visible()
        } else {
            binding.layoutHeader.linearOnlineorder?.gone()
        }

        isupdate = requireArguments().getBoolean("update")
        viewModel.setOpenOrderUpdate(isupdate)
        reorder = requireArguments().getBoolean("reorder")

        if (isupdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }


        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        val customer = prefProvider.getCustomerData()
        customer?.let {
            viewModel.selectedCustomer = customer
        }


        if (isAdded) {

            setFragmentResultListener("request_key_customer_dine_in") { _, bundle ->
                val result = bundle.getParcelable<TbCustomer>("data")
                if (result != null) {
                    LogUtil.logE(TAG, "assignResult:  ${Gson().toJson(result)}")
                    if (cartList.isEmpty()) {
                        cartList =
                            bundle.getParcelableArrayList<CartModel>("cartList") as ArrayList<CartModel>
                    }
                    val dineInList = cartList[0].dineInList
                    LogUtil.logE(TAG, "getdineInListSize:  ${dineInList?.size}")

                    if (dineInList?.isNotEmpty() == true) {

                        var position = bundle.getInt("position")
                        LogUtil.logE(TAG, "getCustomerAssignPos:  ${position}")

                        if (dineInList.size >= position && position != 0) {


                            dineInList.get(position).customer = result

                            LogUtil.logE(TAG, "UpdateCustomerPostition ${position}")
                            LogUtil.logE(
                                TAG,
                                "UpdateCustomer ${dineInList.get(position).customer}"
                            )

                            viewModel.dineInCartUpdate(
                                cartList,
                                dineInList
                            )
                        }
                    }
                }
            }
        }

/*
        setFragmentResultListener("request_key_customer_dine_in") { _, bundle ->
            result = bundle
            resultData = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                Log.e(TAG,"REQUEST_KEY_CUSTOMER_DINE_IN$result")
            }
        }
*/

        if (prefProvider.getValueboolean(SPLIT_ENABLE, false)) {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                prefProvider.setValueboolean(IS_FROM_ALL_ORDER,false)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
            }
        } else {
            loadCartFragment(CartFragment(this, this, dineInCallback = this, isFromDashboard = true))
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }
        binding.layoutHeader.txtUserName.text =
            prefProvider.getValue(EMPLOYEE_NAME, "")


        //getOnlineOrderIsEnableOrNot()
        //allOrdersPendingCountObserver()

        initScanner()

        checkCashDrawerPer()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initScanner() {
        //barcode event listener
        (activity as MainActivity).addDevEventsDelegate(this)
    }

    private fun syncData() {

        val sync = prefProvider.getValueboolean(Constants.SYNC_DATA, false)
        if (!sync) {
            ProgressUtils.showProgressDialog(requireActivity())
            viewModel.syncInventoryModule(false)
            viewModel.syncDone.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    Log.d(TAG, "syncDataDone: $it")
                    if (it) {
                        //binding.maskLayout?.gone()
                        getConnectedPrinters()
                    } else {
                        binding.maskLayout?.visible()
                    }
                }
            }
        } else {
            //viewModel.getOnlineOrderCount()
            allOrdersPendingCountObserver()
        }
    }

    override fun onResume() {
        super.onResume()

//        hideNavigation()

        if (prefProvider.getValueboolean(Constants.IS_SYNC_MARKUP, false)) {
            viewModel.markupInventory()
        }
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = childFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
            putBoolean("reorder", reorder)
            if (arguments != null) {
                putBundle("updateBundle", arguments)
            }
            putBoolean("update", isupdate)
            if (isupdate) {
                orderId?.let { putInt("orderId", it) }
                paymentId?.let { putInt("paymentId", it) }
                putString("paymentOfflineId", paymentOfflineId)
                putString("orderOfflineId", orderOfflineId)
            }


        }
        frag.arguments = result
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).addToBackStack(null)
            .commit()
    }

    private fun loadKeyPadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()
    }

    private fun onClick() {

        binding.layoutHeader.txtTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_transactionFragment)
                }
            }

        }
        /* binding.layoutHeader.txtDineIn.setOnClickListener {
              dineInClickEvent(it)


        }*/
        binding.layoutHeader.imgDrawer.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_menuFragment)
            }

        }
        binding.layoutHeader.txtOpenOrder.setOnClickListener {
//           try {
//               if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
//                   prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
//                   findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
//               }
//           } catch (e: Exception) {
//               e.printStackTrace()
//           }
        }
        binding.layoutHeader.txtOnlineOrder.setOnClickListener {
            try {
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                viewModel.deleteCart()
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_passcode,
                    bundle
                )
            }
        }
//        binding.layoutHeader.ivLock.setOnClickListener {
//            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
//                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
//                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_reportEODFragment)
//            }
//        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
            binding.layoutHeaderCheckout.rlRoot.visibility = View.GONE
            binding.layoutHeader.rlRoot.visibility = View.VISIBLE
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }

        binding.layoutHeader.imgSync?.setOnClickListener {
            prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
            viewModel.syncInventoryModule(false)
        }
        binding.layoutHeader.imgCashdDrawer.setOnClickListener {
            Log.d(TAG, "CASH-DRAWER: STEP 1 ")
            getCustomerPrinters()
            /*try {
                SunmiPrintHelper.getInstance().openCashBox()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }*/
        }

        /* binding.layoutHeader.txtOpenOrder.setOnClickListener {
         loadCategoryFragment(CategoryFragment(this))
         binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.btnColor))
         binding.layoutHeader.txtOpenOrder.setTypeface(
             binding.layoutHeader.txtOpenOrder.typeface,
             Typeface.BOLD
         )
         binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.txtColor))
         binding.layoutHeader.txtKeypad.setTypeface(
             binding.layoutHeader.txtKeypad.typeface,
             Typeface.NORMAL
         )
     }*/
        binding.layoutHeader.txtKeypad.setOnClickListener {
            try {
            if (rolePermission.hasManualSalesPermission(binding.root)) {
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                viewModel.deleteManualSaleCart()
                binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.btnColor))
                binding.layoutHeader.txtKeypad.setTypeface(
                    binding.layoutHeader.txtKeypad.typeface,
                    Typeface.BOLD
                )
                binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.txtColor))
                binding.layoutHeader.txtOpenOrder.setTypeface(
                    binding.layoutHeader.txtOpenOrder.typeface,
                    Typeface.NORMAL
                )
                var bundle: Bundle = Bundle()
                bundle.putParcelableArrayList("carttlist", cartList)
                if(prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                    if ((cartList[0].dineInList?.size ?: 0) > 0) {
                        cartList[0].dineInList?.get(0)?.selectedPosition =
                            viewModel.dineInHeaderPosition
                    }
                    bundle.putInt("selectedHeaderPosition", viewModel.dineInHeaderPosition)
                }
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_manualSalesNew,
                    bundle
                )
            }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    private fun getCustomerPrinters() {
        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.d(TAG, "CASH-DRAWER: STEP 2 ")
                    if (it.data?.isNotEmpty() == true) {
                        for (i in 0 until it.data.size) {
                            Log.d(TAG, "CASH-DRAWER: PrinterName($i) = ${it.data[i].name}")
                            if (it.data[i].name.startsWith("CloudPrint", true) == true) {
                                if (woyouService != null) {
                                    woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                                } else {
                                    val aa = ByteArray(5)

                                    aa[0] = 0x10
                                    aa[1] = 0x14
                                    aa[2] = 0x00
                                    aa[3] = 0x00
                                    aa[4] = 0x00

                                    try {
                                        SunmiPrinterApi.getInstance().sendRawData(aa)
                                    } catch (e: java.lang.Exception) {
                                        e.printStackTrace()
                                    }

                                    try {
                                        SunmiPrintHelper.getInstance().openCashBox()
                                    } catch (e: java.lang.Exception) {
                                        e.printStackTrace()
                                    }
                                }

                            } else if (it.data[i].name.startsWith(SUNMI_INNER_PRINTER, true) == true) {

                                if (woyouService != null) {
                                    woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                                } else {
                                    val aa = ByteArray(5)

                                    aa[0] = 0x10
                                    aa[1] = 0x14
                                    aa[2] = 0x00
                                    aa[3] = 0x00
                                    aa[4] = 0x00


                                    try {
                                        SunmiPrinterApi.getInstance().sendRawData(aa)
                                    } catch (e: java.lang.Exception) {
                                        e.printStackTrace()
                                    }
                                    try {
                                        SunmiPrintHelper.getInstance().openCashBox()
                                    } catch (e: java.lang.Exception) {
                                        e.printStackTrace()
                                    }

                                }

                            } else {
                                Log.d(TAG, "CASH-DRAWER: STEP 3 in TM-m30 ")


                                try {
                                    var mPrinter =
                                        Printer(
                                            Printer.TM_M30,
                                            Printer.MODEL_ANK,
                                            (activity as MainActivity).applicationContext
                                        )


                                    var printerAdd =
                                        if (it.data[i].printer_type == Constants.BLUETOOTH) "BT:" + it.data[i].macAddress else "TCP:" + it.data[i].ipAddress
                                    mPrinter.connect(
                                        printerAdd,
                                        Printer.PARAM_DEFAULT
                                    )

                                    mPrinter.addPulse(
                                        com.epson.epos2.printer.Printer.DRAWER_HIGH,
                                        com.epson.epos2.printer.Printer.PULSE_100
                                    )

                                    try {

                                        mPrinter.sendData(Printer.PARAM_DEFAULT)
                                        mPrinter.disconnect()
                                    } catch (e: java.lang.Exception) {
                                        try {
                                            mPrinter.disconnect()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                        e.printStackTrace()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                /*var builder: Builder = Builder(
                                    if (it.data[i].name.substring(0, 6).toString()
                                            .lowercase() == "TM-m30".lowercase()
                                    ) {
                                        "TM-m30"
                                    } else {
                                        it.data[i].name
                                    }, PrinterClass.language, requireActivity()
                                )

                                Log.d(TAG, "CASH-DRAWER: STEP 4")
                                builder.addPulse(
                                    com.epson.epos2.printer.Printer.DRAWER_HIGH,
                                    com.epson.epos2.printer.Printer.PULSE_100
                                )

                                val status = IntArray(1)
                                val battery = IntArray(1)
                                try {
                                    Log.d(TAG, "CASH-DRAWER: STEP 5")
                                    PrinterClass.getPrinter()?.sendData(
                                        builder,
                                        PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                                    )
                                } catch (e: java.lang.Exception) {
                                    Log.d(TAG, "CASH-DRAWER: STEP 5 with error = ${e.localizedMessage} ")
                                    e.printStackTrace()
                                }*/


                            }
                        }

                    }

                }

            }
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            LogUtil.logE(TAG, "onServiceConnected  1")
            woyouService = IWoyouService.Stub.asInterface(service)

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            LogUtil.logE(TAG, "onServiceDisConnected  2")
            woyouService = null


        }

    }

    private fun Binding() {
        val intent = Intent()
        intent.setPackage("com.android.pos")
        intent.action = "com.android.pos.aidl.IWoyouService"
        MainApplication.getInstance()?.applicationContext?.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }


    override fun onItemSelected(item: TbItem) {

        when (item.name) {
            SELL_CARD -> {
                // clear customer if added any for previous order type
                viewModel.clearCustomer()
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_purchaseGiftCard)
                }
            }
            ADD_VALUE -> {
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_addValueInGiftCard)
                }
            }
            BALANCE_INQUIRY -> {
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_balanceInquiry)
                }
            }
            else -> {
                item.timeStamp = randomOfflineId()

                Log.e("viewModel.cartModel",Gson().toJson(viewModel.cartModel))

                prefProvider.setValue(Constants.REDIRECT_FROM, "")

                if (cartList.isEmpty() && viewModel.cartModel != null) {
                    viewModel.cartModel?.let {
                        cartList.add(it)
                    }
                }

                if (cartList.isEmpty() && viewModel.cartModel != null) {
                    cartList = arrayListOf()
                    cartList = viewModel.createCart(cartList)
                    if (cartList[0].employeeID != prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)) {
                        cartList[0] = viewModel.cartModel!!
                    }
                }


                if (item.modifier_set_ids.isNotEmpty() || item.variationsAttributes.isNotEmpty()) {
                    /*  if (item.variationsAttributes.isNotEmpty()) {
                          item.variationsAttributes.get(0).isChecked = true
                      }*/
                    item.modifiers.forEach { it.isChecked = false }
                    item.variationsAttributes.forEach { it ->
                        if (it.priceType == "Variable") {
                            it.price = null
                        }
                    }
                    val backStateName: String = AddItemFragment.javaClass.getName()
                    val fragment = AddItemFragment.newInstance(item, this, cartList, false)
                    val fm: FragmentManager = requireActivity().supportFragmentManager
                    fm.beginTransaction().add(binding.frameLayout.id, fragment)
                        .setReorderingAllowed(true)
                        .addToBackStack(backStateName).commit()
                    //  loadCategoryFragment(fragment)
                } else {
                    Log.e(TAG, "cartListItemAddSize: ${cartList.size}")
                    if (cartList.isEmpty()) {
                        viewModel.createCart(cartList)
                    }
                    item.itemQuantity = 1
                    if (cartList.size > 0) {

                        if (cartList.isNotEmpty())
                            cartList[0].employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)

                        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                            if (cartList[0].dineInList?.isEmpty() == true) {
                                cartList[0].dineInList = dineInList
                            }


                            if (cartList[0].dineInList?.isNotEmpty() == true) {
                                Log.e(
                                    "checkDineHeaderPos",
                                    "dineInHeaderPosition:  ${viewModel.dineInHeaderPosition}"
                                )

                        if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false)) {
                            item.isEdited = true
                        }

                        var dineInList = cartList[0].dineInList
                        dineInList!![0]?.selectedPosition = viewModel.dineInHeaderPosition
                        viewModel.newCartLogicModifier(
                            cartList,
                            item,
                            Constants.ADD,
                            false,
                            dineInList = dineInList
                        )
                    }
                } else {
                    viewModel.newCartLogicModifier(cartList, item, Constants.ADD, false)
                }
            }

                }
            }
        }

    }


    override fun onCancelItemSelected(isCancel: Boolean) {
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN && isCancel) {
            arguments?.clear()
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_self)
        } else {

            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }

    }

    override fun onCategorySelected(item: TbItem) {

    }

    private fun addObserver() {

        viewModel.showProgress.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    //ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    // ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.clockOut.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                AlertUtils.showCustomAlert(requireContext(), it)
                val bundle = Bundle()
                bundle.putBoolean("isDashboard", false)
                bundle.putBoolean("isSwap", false)
                viewModel.deleteCart()
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryBoldPOS_to_passcode,
                        bundle
                    )
                }

            }
        }
        viewModel.callCashDiscount.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    viewModel.getCashDiscountDetails(active = 1)
                        ?.observe(viewLifecycleOwner) { cashDiscountData ->
                            if (cashDiscountData != null) {
                                cashDiscountModel = cashDiscountData
                                prefProvider.setValueboolean(
                                    Constants.CASHDIS_SURCHARGEENABLE,
                                    true
                                )
                                prefProvider.setValue(
                                    Constants.AMOUNT_TYPE,
                                    cashDiscountData.amount_type
                                )
                                prefProvider.setValue(
                                    Constants.OPTION_TYPE,
                                    cashDiscountData.option_type
                                )
                                prefProvider.setValue(
                                    Constants.RATE_OR_AMOUNT,
                                    cashDiscountData.rate_or_amount.toString()
                                )
                                prefProvider.setValueboolean(Constants.CASH_DIS_STORED, true)
                                cashDiscountType = cashDiscountData.option_type
                                Log.d(TAG, "onCreateView: " + cashDiscountModel.rate_or_amount)
                            } else {
                                prefProvider.setValueboolean(
                                    Constants.CASHDIS_SURCHARGEENABLE,
                                    false
                                )
                                prefProvider.setValue(Constants.AMOUNT_TYPE, "")
                                prefProvider.setValue(Constants.OPTION_TYPE, "")
                                prefProvider.setValue(
                                    Constants.RATE_OR_AMOUNT,
                                    "0"
                                )
                                prefProvider.setValueboolean(Constants.CASH_DIS_STORED, true)
                                cashDiscountType = ""
                            }
                        }
                    //viewModel.getOnlineOrderCount()
                    allOrdersPendingCountObserver()
                    Log.d(TAG, "addObserver: " + Gson().toJson(viewModel.cartModel))
                    Log.d(TAG, "addObserver: " + Gson().toJson(cartList))

                }
            }
        }
        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {

            if (prefProvider.getValue(ORDER_TYPE, "").trim().isEmpty()) {
                binding.layoutHeader.txtKeypad.gone()
            } else {
                binding.layoutHeader.txtKeypad.visible()
            }
            if (it.isEmpty()) {

                cartList.clear()
                cartList = arrayListOf()

            } else {
                if (it[0].orderId != 0) {
                    it[0].orderId?.let { it1 -> viewModel.setOrderId(it1) }
                }
                cartList.clear()
                cartList = arrayListOf()
                cartList.addAll(it.toCollection(arrayListOf()))
            }

            if (this::presentation.isInitialized) {
                if (!presentation.isShowing)
                    presentation.show()
                if (cartList.isNotEmpty()) {
                    presentation.updateCustomerDisplay(it)
                } else {
                    presentation.onLogOutOrClockOutWithApiService(apiService)
                }
            }


                if (prefProvider.getValue(ORDER_TYPE, "").trim().isEmpty()) {
                    binding.layoutHeader.txtKeypad.gone()
                } else {
                    binding.layoutHeader.txtKeypad.visible()
                }

        }

        viewModel.itemQuantityCheck.observe(viewLifecycleOwner){event->
            event.getContentIfNotHandled()?.let {
                if(it){
                    AlertUtils.showCustomAlert(requireContext(), "${getString(R.string.item_quantity_cannot_exceed)} ${MAX_ITEM_QUANTITY}")
                }
            }
        }
        /*  viewModelPayment.QueueCreateSaveOrder.observe(requireActivity()) {
              it.getContentIfNotHandled()?.let {
                  viewModel.deleteCart()
                  clearCustomer()
                  prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                  findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

              }
          }*/
    }

    private fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        viewModel.selectedCustomer = null
        viewModel.assignCustomer = null
        prefProvider.saveCustomerData(null)
        prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
    }


    private fun getServiceCharges() {

        serviceChargesObserve = Observer {
            if (it.status == Status.SUCCESS) {
                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_DINEIN_ORDER,
                            false
                        )
                    ) {
                        LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                        serviceChargesList = ArrayList()
                        viewModel.serviceChargesList.clear()
                        it.data?.forEach { service ->
                            if (service.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                serviceChargesList?.add(service)
                                viewModel.serviceChargesList.add(service)
                            }
                        }
                    }
                    Log.d(
                        TAG,
                        "getServiceCharges: finall " + Gson().toJson(viewModel.serviceChargesList)
                    )
                } else {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                            false
                        )
                    ) {
                        LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                        serviceChargesList = ArrayList()
                        viewModel.serviceChargesList.clear()
                        it.data?.forEach { service ->
                            if (service.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                                serviceChargesList?.add(service)
                                viewModel.serviceChargesList.add(service)
                            }
                        }
                        Log.d(
                            TAG,
                            "getServiceCharges: finall " + Gson().toJson(viewModel.serviceChargesList)
                        )

                    }
                }
            }

        }
        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }

    private fun getOrderTypes() {

        viewModel.getOrderTypes.observe(requireActivity()) {
            if (it.status == Status.SUCCESS) {
                if (it.data != null) {
                    ordertypelist = it.data.toCollection(arrayListOf())
                    viewModel.setOrderTypeList(ordertypelist)
                }
            }

        }
    }


    override fun onItemUpdate(item: TbItem) {
        prefProvider.setValueInt(Constants.CAT_ID_SELECTED, item.categoryId)
        val backStateName: String = AddItemFragment.javaClass.getName()
        val fragment = AddItemFragment.newInstance(item, this, cartList, true)
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().add(binding.frameLayout.id, fragment).setReorderingAllowed(true)
            .addToBackStack(backStateName).commit()
        /*val frag: Fragment = AddItemFragment.newInstance(item, this, cartList, true)
        loadCategoryFragment(frag)*/
    }

    override fun onDineInOrderCleared() {
        arguments?.clear()
        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_self)
        //   findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_self)
    }


    private fun getDineInData() {
        if (arguments?.getBoolean("isFromDineIn") == true) {
            LogUtil.logE(TAG, "isFromDineInTrue")
            getDineInCartList()
        }
    }


    private fun getDineInCartList() {
        val numOfGuest: Int by lazy {
            requireArguments().getInt("numberOfGuest")
        }

        dineInFloorTableModel = arguments?.getParcelable("floorplan")

        var orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? =
            arguments?.getParcelable("tableDetails")

        if (orderDEtails != null) {
            orderFloorDetails = orderDEtails
        }
        if (orderDEtails != null) {
            if (orderFloorDetails.id == null) {
                orderFloorDetails.apply {
                    id = dineInFloorTableModel?.id
                    chairCount = dineInFloorTableModel?.chairCount
                    floorPlanId = dineInFloorTableModel?.floorPlanId
                    tableName = dineInFloorTableModel?.tableName.toString()
                    status = dineInFloorTableModel?.status.toString()
                    tableNumber = dineInFloorTableModel?.tableNumber

                }

            }
        } else {
            orderFloorDetails.apply {
                id = dineInFloorTableModel?.id
                chairCount = dineInFloorTableModel?.chairCount
                floorPlanId = dineInFloorTableModel?.floorPlanId
                tableName = dineInFloorTableModel?.tableName.toString()
                status = dineInFloorTableModel?.status.toString()
                tableNumber = dineInFloorTableModel?.tableNumber
            }
        }


        val dineInList: java.util.ArrayList<DineInModel> = arrayListOf()
        dineInList.add(
            DineInModel(
                0,
                true,
                0,
                "Whole Table",
                floorPlanTable = orderFloorDetails
            )
        )
        for (i in 1..numOfGuest) {
            dineInList.add(
                DineInModel(
                    0,
                    false,
                    0,
                    "Guest $i",
                    floorPlanTable = orderFloorDetails,

                    )
            )
        }
        var orderTypeId = -1
        ordertypelist.forEach {
            if (it.orderType.lowercase() == DINE_IN.lowercase()) {
                orderTypeId = it.id
            }
        }

        if (cartList.isEmpty()) {
            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                this.orderTypeId = orderTypeId
                orderType = prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT).toString()
                orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
                this.serviceCharge = viewModel.serviceChargesList

            }

            cartList.add(cartModel)
        }

        cartList.get(0).orderType = Constants.DINE_IN

        viewModel.newCartLogicModifier(
            cartList,
            null,
            Constants.ADD,
            false,
            dineInList = dineInList
        )


    }

    private fun navigateDineInOrder() {
        viewModel._Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
                    Log.e(TAG, "orderIdBaseResponse:  ${baseResponse.order.id}")
                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", baseResponse.order.totalAmount)
                    bundle.putParcelable("dineInList", baseResponse)
                    bundle.putBoolean("isGuestPaid", false)
                    bundle.putInt("orderId", baseResponse.order.id)
                    prefProvider.setValue(ORDER_TYPE, "")
                    viewModel.deleteCart()
                    findNavController().navigate(
                        R.id.action_dashboardCategoryBoldPOS_to_dineInOrderTable,
                        bundle
                    )
                }
            }
        }
    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            var dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")

            if (dineInList?.isNotEmpty() == true) {
                if (cartList.isEmpty()) {
                    var orderTypeIdN = 0
                    ordertypelist.forEach {
                        if (it.orderType == DINE_IN) {
                            orderTypeIdN = it.id
                        }
                    }

                    val cartModel = CartModel().apply {
                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                        employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                        locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                        orderTypeId = orderTypeIdN
                        orderType = DINE_IN
                        orderTypeName = DINE_IN

                        serviceCharge = serviceChargesList
                        orderId = arguments?.getInt("orderId")
                        listOfItemRemoved = dineInList[0].listOfItemsMoved

                    }
                    cartList.add(cartModel)
                }

                var orderTableData: GetOrderDetailsResponse.Data.FloorPlanTable? =
                    arguments?.getParcelable("tableDetails")

                dineInList.forEach {
                    it.floorPlanTable = orderTableData
                }

                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
                prefProvider.setValueInt(
                    Constants.ORDER_TYPE_ID, prefProvider.getValueInt(
                        ORDER_TYPE_ID, 0
                    )
                )

                cartList[0].note = arguments?.getString("order_note").toString()
                LogUtil.logE("AAjeDine", "cartdiscountPrice  ${cartList[0].discountPrice}")
                LogUtil.logE(
                    "AAjeDine",
                    "dineTotalDiscount  ${arguments?.getDouble("totalDiscount")}"
                )
                cartList[0].discountPrice = arguments?.getDouble("totalDiscount") ?: 0.0
                Log.e(TAG, "wsfaklnlbsaf ${cartList.size}")
                viewModel.newCartLogicModifier(
                    cartList,
                    null,
                    Constants.ADD,
                    false,
                    dineInList = dineInList,
                    isFromDineInScreen = true
                )
                // viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0

            }


        }
    }

    private fun dineInUpdateOrder() {


        viewModel.updateOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                var frag =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.frameLayout)

                if (frag?.javaClass?.name == AddItemFragment.javaClass.getName()) {
                    requireActivity().supportFragmentManager.popBackStackImmediate(
                        AddItemFragment.javaClass.getName(),
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                }

                val bundle = Bundle()
                if (cartList.isNotEmpty())
                    bundle.putParcelable("cartList", cartList[0])
                bundle.putBoolean("isGuestPaid", false)
                Log.e(TAG, "orderIdDineIn:  ${viewModel.orderId}")
                viewModel.orderId?.let { it1 -> bundle.putInt("orderId", it1) }
                /*if (cartList.isNotEmpty() && cartList[0].orderId != 0) {
                    cartList[0].orderId?.let { it1 -> bundle.putInt("orderId", it1) }
                } else {
                    orderId?.let { it1 -> bundle.putInt("orderId", it1) }
                }*/
                prefProvider.setValue(ORDER_TYPE, "")
                prefProvider.setValue(ORDER_TYPE_NAME, "")
                LogUtil.logE(TAG, "deleteCartDineIn")
                viewModel.deleteCart()
                clearCustomer()


                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_dineInOrderTable,
                    bundle
                )

            }
        }

    }

    @Inject
    lateinit var apiService: ApiService

    override fun onPause() {
        arguments?.clear()
        super.onPause()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        createOrderResponse: CreateOrderResponse
    ) {


        if (data.name.startsWith("CloudPrint", true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)

            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")

                            viewModel.downloadFinished(false)
                            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                            }
                        }

                        override fun onConnect() {
                            println("onConnect")
                            generateKitchenReceiptSunmi(data, type, createOrderResponse.data)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generateKitchenReceiptSunmi(data, type, createOrderResponse.data)
            }

        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {


            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            setService(data,createOrderResponse.data)


        } else {

            if (!data.name.substring(0, 6).toString().lowercase().contains("TM-m".lowercase())) {

                var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    Log.e(TAG, "YesContains")
                    Printer(
                        Printer.TM_M30,
                        Printer.MODEL_ANK, requireContext()
                    )
                } else {
                    Printer(
                        Printer.TM_U220,
                        Printer.MODEL_ANK, requireContext()
                    )
                }

                mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                    Log.e(
                        TAG,
                        "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}"
                    )

                        try {
                            printer.disconnect()
                            requireActivity().runOnUiThread {
                                viewModel.downloadFinished(false)
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                                }
                            }

                        } catch (e: java.lang.Exception) {
                            try {
                                requireActivity().runOnUiThread {
                                    viewModel.downloadFinished(false)
                                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                                    }
                                }
                            }catch (e:Exception){

                            }
                            e.printStackTrace()
                        }

                }
                try {
                    Log.e(TAG, "printerDataType:  ${data.printer_type}")

                    var printerAdd =
                        if (data.printer_type == Constants.BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                    mPrinter.connect(
                        printerAdd,
                        Printer.PARAM_DEFAULT
                    )
                    mPrinter.startMonitor()

                    generateReceiptForU220(mPrinter, data, type, createOrderResponse.data)

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }



            } else {


                try {
                    PrinterClass.closePrinter()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                /*if (PrinterClass.getPrinter() == null) {*/
                var printer: Print? = Print(requireContext())
                if (printer != null) {
                    /* printer.setStatusChangeEventCallback(this)
             printer.setBatteryStatusChangeEventCallback(this)*/
                }

                val enabled = Print.FALSE

                try {

                    printer?.openPrinter(
                        if (data.printer_type == Constants.BLUETOOTH) {
                            Print.DEVTYPE_BLUETOOTH
                        } else {
                            Print.DEVTYPE_TCP
                        },
                        data.ipAddress,
                        enabled,
                        1000
                    )
                    //  printer?.setStatusChangeEventCallback(this)

                } catch (e: Exception) {
                    LogUtil.logE(TAG, "PrinterException: " + e.message)
                    printer = null
                    viewModel.downloadFinished(false)
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                    }

                }


                if (printer != null) {
                    PrinterClass.setPrinter(printer)

                    generateKitchenReceipt(data, type, createOrderResponse.data)

                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                    viewModel.downloadFinished(false)
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                    }

                }
            }
        }

        /* } else {

         }
 */
    }

    private fun generateReceiptForU220(
        builder: Printer,
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        receiptModel: CreateOrderResponse.Data
    ) {
        try {
            val pname = if (data.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                data.name
            }

            var fontSizeH = 1
            var fontSizeW = 1
            when (kitchenSettingModel.fonts) {
                SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }
                MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }
                LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }


            if (data.name.substring(0, 4)
                    .equals("TM-U", true) || data.name.contains("U")
            ) {

                LogUtil.logE(TAG, "kitchenFonts:  ${kitchenSettingModel.fonts}")
                LogUtil.logE(TAG, "kitfontSize:  ${fontSizeH}")

                builder.addFeedUnit(30)
                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    "OrderID:" + receiptModel?.order?.id
                )

                builder.addFeedUnit(30)
                builder.addFeedLine(1)
                if (kitchenSettingModel.showOrderType) {


                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderTextForU220(builder, receiptModel?.order?.orderTypeName)
                }

                /* if (receiptModel?.order?.orderType.trim().lowercase() == "OpenOrder".trim()
                     .lowercase()
               ) {*/
                /*builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                addBuilderTextForU220(builder, receiptModel?.order?.deliveryType.toString())
*/

                if (kitchenSettingModel.showTeamMember) {

                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            33
                        )
                    )

                }
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLineForU220(builder)

                receiptModel?.order?.orderItems?.let {
                    addOrdersForKitchenU220(
                        builder!!,
                        it,
                        fontSizeH,
                        fontSizeW,
                        data.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(receiptModel?.order?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                    if (receiptModel?.order?.customer != null) {

                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty()) {
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        receiptModel?.order?.customer?.phones?.get(
                                            0
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }

                        if (kitchenSettingModel.showCustomerAddress) {

                            if (receiptModel?.order?.orderType.trim()
                                    .lowercase() == "Open Order".trim()
                                    .lowercase() && receiptModel?.order?.deliveryType.trim()
                                    .lowercase() == "Pickup".trim()
                                    .lowercase()
                            ) {

                            } else if (receiptModel.order?.customer?.addresses?.isNotEmpty()) {

                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )

                                receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    ?.forEach {

                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            builder!!.addText(
                                                it.fullAddress
                                            )
                                        }
                                    }

                                // builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                            }
                        }

                    }
                }
            }

            builder?.addFeedLine(5)

            builder?.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            status[0] = 0
            val battery = IntArray(1)


            try {
                builder.sendData(Printer.PARAM_DEFAULT)
               // viewModel.downloadFinished(false)
               /* if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                }*/

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                }
                /*PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
               */
            }


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
            }
        }

    }

    private fun setService( kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,data: CreateOrderResponse.Data) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(kitchenReceiptPrinters,data)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(kitchenReceiptPrinters,data)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun observeSaveOrder() {

        viewModelPayment.QueueStart.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { it ->
                // AlertUtils.showCustomAlert(requireActivity(), it.message)
                viewModel.deleteCart()
                viewModel.updateActiveOrderFlagClear()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }
                prefProvider.setValue(ORDER_TYPE_NAME, "")
                LogUtil.logE(TAG, "QueueCreateAgain")

                clearCustomer()
                if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                } else {
                    getKitchenPrinters(it)
                }
                // getKitchenPrinters(it)


            }
        }

    }

    private fun getKitchenPrinters(createOrderResponse: CreateOrderResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    LogUtil.logE(TAG, "getKitchenPrinterList:  ${Gson().toJson(it.data)}")
                    LogUtil.logE(TAG, "isUpdateOrder  ${viewModelPayment.isUpdateOrder}")

                    requireActivity().runOnUiThread {
                        ProgressUtils.dismissProgressDialog()
                        viewModel.downloadFinished(true)

                        if (viewModelPayment.isUpdateOrder) {
                            var model = prefProvider.getValue(OPEN_ORDER_ITEMS, "")
                            LogUtil.logE(TAG, "getItemsModel  ${Gson().toJson(model)}")
                            var printOrderItems:
                                    ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                arrayListOf()

                            val serializedObject: String =
                                prefProvider.getValue(OPEN_ORDER_ITEMS, "")
                            if (serializedObject.isNotEmpty()) {
                                val gson = Gson()
                                val type = object :
                                    TypeToken<List<CreateOrderResponse.Data.Order.OrderItem?>?>() {}.type
                                var arrayItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                    gson.fromJson<Any>(
                                        serializedObject,
                                        type
                                    ) as ArrayList<CreateOrderResponse.Data.Order.OrderItem>

                                LogUtil.logE(TAG, "arrayItems:  ${Gson().toJson(arrayItems)}")
                                var itemIds: ArrayList<Int> = arrayListOf()
                                arrayItems.forEach {
                                    itemIds.add(it.id)
                                }

                                createOrderResponse.data.order.orderItems.forEachIndexed { index, orderItem ->

                                    if (itemIds.contains(orderItem.id)) {
                                        if (index < arrayItems.size) {
                                            if (arrayItems[index].quantity != orderItem.quantity) {
                                                if (orderItem.quantity > arrayItems[index].quantity) {
                                                    orderItem.quantity =
                                                        orderItem.quantity - arrayItems[index].quantity
                                                    if (!printOrderItems.contains(orderItem)) {
                                                        printOrderItems.add(orderItem)
                                                    }
                                                }
                                            } else {
                                            }
                                        }


                                    } else {
                                        printOrderItems.add(orderItem)
                                    }


                                }


                                createOrderResponse.data.order.orderItems = arrayListOf()

                                createOrderResponse.data.order.orderItems = printOrderItems
                            }

                            prefProvider.setValue(OPEN_ORDER_ITEMS, "")

                        }


                        if (it.data?.isNotEmpty() == true && createOrderResponse.data.order.orderItems.isNotEmpty()) {
                            var  allstatus = false

                            for (i in 0 until it.data.size) {
                                it.data[i].orderTypes.forEach { order ->
                                    if (order.orderTypeId == createOrderResponse.data.order.orderTypeId) {
                                        order.printerSettings.forEach { set ->
                                            if (set.printType.equals(
                                                    Constants.KITCHEN,
                                                    true
                                                ) && set.autoPrinting
                                            ) {
                                                if (checkItemsforPrinter(
                                                        createOrderResponse.data.order.orderItems
                                                            ?: arrayListOf(),
                                                        it.data[i].printerCategories.toCollection(
                                                            arrayListOf()
                                                        )
                                                    )
                                                ) {
                                                    LogUtil.logE(
                                                        TAG,
                                                        "statusPrinter  ${it.data[i].status}"
                                                    )
                                                    if (it.data[i].status) {
                                                        allstatus = true
                                                        initKitchenPrinter(
                                                            it.data.get(i),
                                                            Constants.KITCHEN,
                                                            createOrderResponse
                                                        )
                                                    }
                                                }

                                            } else {
                                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                                                }
                                            }
                                        }

                                    }
                                }


                            }
                            if (!allstatus){
                                viewModel.downloadFinished(false)
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                                }
                            }

                        } else {
                            viewModel.downloadFinished(false)
                            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                            }
                        }

                    }
                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)

                }
            }

        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        receiptModel: CreateOrderResponse.Data
    ) {
        var builder: Builder? = null
        try {
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            var fontSizeH = 1
            var fontSizeW = 1
            when (kitchenSettingModel.fonts) {
                SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }
                MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }
                LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }

            builder?.addFeedLine(2)

            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {

                builder = Builder(pname, PrinterClass.language, requireActivity())
                LogUtil.logE(TAG, "kitchenFonts:  ${kitchenSettingModel.fonts}")
                LogUtil.logE(TAG, "kitfontSize:  ${fontSizeH}")

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText(
                        "OrderID:" + receiptModel.order.custom_order_id
                    )
                } else {
                    builder.addText(
                        "OrderID:" + receiptModel.order.id
                    )
                }


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)
                if (kitchenSettingModel.showOrderType) {


                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.orderTypeName?.toString())
                }

                /* if (receiptModel?.order?.orderType.trim().lowercase() == "OpenOrder".trim()
                     .lowercase()
               ) {*/
                /*   builder.addFeedLine(1)
                   builder.addTextFont(Builder.FONT_E)
                   builder.addTextLang(Builder.LANG_EN)
                   builder.addTextSize(fontSizeH, fontSizeW)
                   builder.addTextStyle(
                       Builder.FALSE,
                       Builder.FALSE,
                       Builder.TRUE,
                       Builder.COLOR_1
                   )
                   builder.addTextAlign(Builder.ALIGN_CENTER)

                   addBuilderText(builder, receiptModel?.order?.deliveryType.toString())*/

                /*}*/


//                builder.addTextLineSpace(30)
//                builder.addFeedUnit(30)
//                builder.addTextFont(Builder.FONT_E)
//                //  builder.addTextAlign(Builder.ALIGN_LEFT)
//                builder.addTextLang(Builder.LANG_EN)
//                builder.addTextSize(fontSizeH, fontSizeW)
//                builder.addTextStyle(
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.COLOR_1
//                )
//
//
//                builder.addText(
//                    padLine(
//                        "ReceiptID:" + receiptModel?.order?.offlineId,
//                        "",
//                        33
//                    )
//                )
                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            33
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLine(builder)

                receiptModel?.order?.orderItems?.let {
                    addOrdersForKitchen(
                        builder!!,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(receiptModel?.order?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                    if (receiptModel?.order?.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty()) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        receiptModel?.order?.customer?.phones?.get(
                                            0
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
                 builder.addFeedUnit(30)
                 builder.addTextFont(Builder.FONT_E)
                 builder.addTextAlign(Builder.ALIGN_LEFT)
                 //builder.addTextLineSpace(20)
                 builder.addTextLang(Builder.LANG_EN)
                 builder.addTextSize(1, 1)
                 builder.addTextStyle(
                     Builder.FALSE,
                     Builder.FALSE,
                     Builder.TRUE,
                     Builder.COLOR_1
                 )
                 builder.addText(receiptModel?.order?.customer?.email)*/

                        if (kitchenSettingModel.showCustomerAddress) {

                            if (receiptModel?.order?.orderType.trim()
                                    .lowercase() == "Open Order".trim()
                                    .lowercase() && receiptModel?.order?.deliveryType.trim()
                                    .lowercase() == "Pickup".trim()
                                    .lowercase()
                            ) {

                            } else if (receiptModel.order?.customer?.addresses?.isNotEmpty()) {

                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )

                                receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    ?.forEach {

                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            builder!!.addText(
                                                it.fullAddress
                                            )
                                        }
                                    }

                                // builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                            }
                        }

                    }
                }
            } else {

                builder = Builder(pname, PrinterClass.language, requireActivity())
                LogUtil.logE(TAG, "kitchenFonts:  ${kitchenSettingModel.fonts}")
                LogUtil.logE(TAG, "kitfontSize:  ${fontSizeH}")


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText(
                        "OrderID:" + receiptModel?.order?.custom_order_id
                    )
                } else {
                    builder.addText(
                        "OrderID:" + receiptModel?.order?.id
                    )
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)
                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.orderType.toString())
                }

                /* if (receiptModel?.order?.orderType.trim().lowercase() == "OpenOrder".trim()
                     .lowercase()
               ) {*/
                /*   builder.addFeedLine(1)
                   builder.addTextFont(Builder.FONT_E)
                   builder.addTextLang(Builder.LANG_EN)
                   builder.addTextSize(fontSizeH, fontSizeW)
                   builder.addTextStyle(
                       Builder.FALSE,
                       Builder.FALSE,
                       Builder.TRUE,
                       Builder.COLOR_1
                   )
                   builder.addTextAlign(Builder.ALIGN_CENTER)

                   addBuilderText(builder, receiptModel?.order?.deliveryType.toString())*/

                /*}*/


//                builder.addTextLineSpace(30)
//                builder.addFeedUnit(30)
//                builder.addTextFont(Builder.FONT_E)
//                //  builder.addTextAlign(Builder.ALIGN_LEFT)
//                builder.addTextLang(Builder.LANG_EN)
//                builder.addTextSize(fontSizeH, fontSizeW)
//                builder.addTextStyle(
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.COLOR_1
//                )
//
//
//                builder.addText(
//                    padLine(
//                        "ReceiptID:" + receiptModel?.order?.offlineId,
//                        "",
//                        if (kitchenSettingModel.fonts == LARGE) {
//                            24
//                        } else {
//                            48
//                        }
//                    )
//                )
                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            if (kitchenSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        if (kitchenSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addFeedLine(1)



                addHorizontalLine(builder)

                receiptModel?.order?.orderItems?.let {
                    addOrdersForKitchenCustomer(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters?.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(receiptModel?.order?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                    if (receiptModel?.order?.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")


                        addHorizontalLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty()) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        receiptModel?.order?.customer?.phones?.get(
                                            0
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
                 builder.addFeedUnit(30)
                 builder.addTextFont(Builder.FONT_E)
                 builder.addTextAlign(Builder.ALIGN_LEFT)
                 //builder.addTextLineSpace(20)
                 builder.addTextLang(Builder.LANG_EN)
                 builder.addTextSize(1, 1)
                 builder.addTextStyle(
                     Builder.FALSE,
                     Builder.FALSE,
                     Builder.TRUE,
                     Builder.COLOR_1
                 )
                 builder.addText(receiptModel?.order?.customer?.email)*/

                        if (kitchenSettingModel.showCustomerAddress) {

                            if (receiptModel?.order?.orderType.trim()
                                    .lowercase() == "Open Order".trim()
                                    .lowercase() && receiptModel?.order?.deliveryType.trim()
                                    .lowercase() == "Pickup".trim()
                                    .lowercase()
                            ) {

                            } else if (receiptModel.order?.customer?.addresses?.isNotEmpty()) {

                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )

                                receiptModel.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    .forEach {

                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            builder.addText(
                                                it.fullAddress
                                            )
                                        }
                                    }
                                // builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                            }
                        }

                    }
                }

            }

            builder?.addFeedLine(5)

            builder?.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            status[0] = 0
            val battery = IntArray(1)

            var timeOut = PrinterClass.SEND_TIMEOUT
            if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                timeOut = PrinterClass.BLUETOOTH_TIMEOUT
            }

            if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
            ) {

                timeOut = 10000
            }

            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    timeOut, status
                )

                PrinterClass.closePrinter()
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                }

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
                }
                /*PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
               */
            }


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
            }
        }

    }

    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        receiptModel: CreateOrderResponse.Data
    ) {
        try {

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().printerInit()
            SunmiPrinterApi.getInstance().lineWrap(4)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.custom_order_id)
            } else {
                PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.id)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(receiptModel?.order?.orderTypeName?.toString())
            }
            //    PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())


            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + receiptModel?.order?.employee?.name, "",
                        if (kitchenSettingModel.fonts == LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


            }


            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        receiptModel?.order?.createdAt.toString()
                    ),
                    "",
                    if (kitchenSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )

            PrintSunmiUtils.addHorizontal()

            receiptModel?.order?.orderItems?.let {
                addOrdersForKitchen(
                    it,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )
            }

            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(receiptModel?.order?.note.toString())

            }


            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                if (receiptModel?.order?.customer != null) {

                    PrintSunmiUtils.customerDetails()


                    if (kitchenSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)


                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty()) {


                            receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                PrintSunmiUtils.customerPhone(
                                    MethodUtils.getUSFormatNumber(it)
                                )
                            }
                        }

                    }


                    if (kitchenSettingModel.showCustomerAddress) {

                        if (receiptModel?.order?.orderType.trim()
                                .lowercase() == "Open Order".trim()
                                .lowercase() && receiptModel?.order?.deliveryType.trim()
                                .lowercase() == "Pickup".trim()
                                .lowercase()
                        ) {

                        } else if (receiptModel.order?.customer?.addresses?.isNotEmpty()) {


//                            receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                PrintSunmiUtils.customerAddress(
//                                    it
//                                )
//                            }

                            receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }


                        }
                    }

                }
            }

            SunmiPrinterApi.getInstance().lineWrap(2)
            PrintSunmiUtils.cutPaper()

            viewModel.downloadFinished(false)

            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
        }

    }

    private fun generateKitchenReceiptSunmiInner(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        receiptModel: CreateOrderResponse.Data
    ) {
        try {

            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(4)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.headerText(receiptModel?.order?.orderTypeName)
            }
            //   PrintSunmiUtils.headerText(receiptModel?.order?.deliveryType.toString())

            SunmiPrintHelper.getInstance().lineWrap(1)


            if (kitchenSettingModel.showTeamMember) {
                PrintSunmiUtils.normalTextLarge("Employee:" + receiptModel?.order?.employee?.name)
            }


            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    receiptModel?.order?.createdAt.toString()
                )
            )

            PrintSunmiUtils.addHorizontalInner()

            receiptModel?.order?.orderItems?.let {

                addOrdersForKitchenInner(
                    it,
                    kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )
            }

            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(receiptModel?.order?.note.toString())

            }


            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                if (receiptModel?.order?.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()


                    if (kitchenSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalTextLarge(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)


                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty()) {


                            receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                PrintSunmiUtils.normalTextLarge(
                                    MethodUtils.getUSFormatNumber(it)
                                )
                            }
                        }

                    }


                    if (kitchenSettingModel.showCustomerAddress) {

                        if (receiptModel?.order?.orderType.trim()
                                .lowercase() == "Open Order".trim()
                                .lowercase() && receiptModel?.order?.deliveryType.trim()
                                .lowercase() == "Pickup".trim()
                                .lowercase()
                        ) {

                        } else if (receiptModel.order?.customer?.addresses?.isNotEmpty()) {


//                            receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                PrintSunmiUtils.normalTextLarge(
//                                    it
//                                )
//                            }

                            receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalTextLarge(
                                            it.fullAddress
                                        )
                                    }
                                }


                        }
                    }

                }
            }

            SunmiPrintHelper.getInstance().lineWrap(2)

            PrintSunmiUtils.cutPaperInner()

            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)
        }

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {

            if (it != null) {
                kitchenSettingModel = it
            }
        }
    }

    private fun printerProgress() {

        if (view != null) {
            viewModel.isLoading.observe(viewLifecycleOwner) { event ->

                if (event) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }

            }
        }
    }

    private fun observeShowProgress() {

        printerViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    binding.maskLayout?.visible()
                    //ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    binding.maskLayout?.gone()
                    //ProgressUtils.dismissProgressDialog()
                    getConnectedPrinters()
                }
            }
        }
    }

    private fun observeQueueCreate() {
        viewModelPayment.queueStartSaveOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)
            }
        }
    }

    private fun createQueuePrinter(createOrder: CreateOrderResponse) {
        val listPrinter: List<Int> = listOf()
        if (cartList.isNotEmpty()) {
            val orderRequest = cartList?.let {

                viewModelPayment.createOrderRequest(
                    it[0],
                    viewModel.subTotalPrice,
                    viewModel.totalPrice,
                    viewModel.totalServiceCharge,
                    viewModel.totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    "",
                    "",
                    false,
                    viewModel.totalDiscount,
                    0.0,
                    0,
                    null,
                    0.0,
                    false,
                    "Cash",
                    cashDiscountType,
                    isPrinterQueue = true,
                    offlineId = createOrder.data.order.offlineId
                )
            }
            val createRequest = CreateQueuePrinterRequestModel(
                location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 0),
                order_type = prefProvider.getValue(ORDER_TYPE, ""),
                printer_id = listPrinter,
                order_item_attributes = orderRequest?.order?.orderItemsAttributes ?: listOf(),
                order_data = orderRequest?.order ?: OrderAttributeRequestModel(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)

            )
            viewModelPayment.createQueuePrinter(createRequest, createOrder)
        }
    }

    override fun scannerBarcodeEvent(
        barcodeData: ByteArray?,
        barcodeType: Int,
        scannerID: Int
    ) {
        LogUtil.logE(TAG, "scannerBarcodeEvent: ${barcodeData?.let { String(it) }}")

        //Check product code in db
        val productCode = barcodeData?.let { String(it) }

        if (productCode.isNullOrEmpty()) {
            viewModel.showErrorMessage("Product code is not available !!")
            return
        }
        try {

            viewModel.getItemByProductCode(productCode ?: "").observe(viewLifecycleOwner) {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            LogUtil.logE(TAG, "TBITEMDATA  ${resource.data}")
                            if (resource.data != null) {

                                //data found. | Add in cart
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                    viewModel.checkCategoryHideOrNot(resource.data.categoryId)
                                        .observe(viewLifecycleOwner) {
                                            when (it.status) {
                                                Status.SUCCESS -> {
                                                    if (it.data != null && resource.data.isHide) {
                                                        //add item in the cart

                                                        addItemInCartThroughBarcode(resource.data)
                                                    }
                                                }
                                                Status.ERROR -> {
                                                }
                                                Status.LOADING -> {

                                                }


                                            }
                                        }


                                }
                            } else {
                                //data not found. Create New Item
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                    LogUtil.logE(TAG, "productCode  ${productCode}")
                                    val bundle = Bundle()
                                    bundle.putString("productCode", productCode ?: "")
                                    findNavController().navigate(
                                        R.id.action_dashboardCategoryBoldPOS_to_createItem,
                                        bundle
                                    )
                                }
                            }
                        }
                        Status.ERROR -> {
                        }
                        Status.LOADING -> {

                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun scannerFirmwareUpdateEvent(firmwareUpdateEvent: FirmwareUpdateEvent?) {
        TODO("Not yet implemented")
    }

    override fun scannerImageEvent(imageData: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun scannerVideoEvent(videoData: ByteArray?) {
        TODO("Not yet implemented")
    }

    private fun addItemInCartThroughBarcode(item: TbItem?) {


        // qty check logic
        var itemQty = 1
        var itemQuantity = 1
        if (cartList.isNotEmpty()) {
            cartList[0].items?.filter { it.itemId == item?.itemId }?.map {
                LogUtil.logE(TAG, "ScanItemQuantity: ${it.itemQuantity}")
                itemQty = it.itemQuantity


            }
        }
        if (item?.quantity ?: 0 >= itemQty) {
            item?.itemQuantity = 1
            //item?.itemQuantity = 0

            if (cartList.isEmpty()) {
                viewModel.setServiceCharges(serviceChargesList)
            }

            viewModel.newCartLogicModifier(cartList, item, Constants.ADD, false)

        } else {
            item?.itemQuantity = -1
            AlertUtils.showCustomAlert(
                requireActivity(),
                getString(R.string.qty_validation)
            )
        }


        /*if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
            cartList.get(0).orderType = DINE_IN
            val dineInList = dineInCartAdapter.getList()
            // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
            dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
            viewModel.cartLogic(cartList, item, ADD, dineInList = dineInList)
        } else {
            viewModel.cartLogic(cartList, item, ADD)
        }*/

    }

    override fun asBinder(): IBinder {
        return woyouService?.asBinder()!!

    }

    override fun onRunResult(isSuccess: Boolean, code: Int, msg: String?) {

    }

    override fun onDineInClickListener() {
        dineInClickEvent()
    }

    fun dineInClickEvent() {
        if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
            Log.e(TAG, "DineinNewCh ORderTypeYES")
            if (cartList.isNotEmpty()) {
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                val dList = cartList[0].dineInList ?: arrayListOf()
                LogUtil.logE(TAG, "dList:  ${Gson().toJson(dList)}")
                var updateDinein = arguments?.getBoolean("is_dine_in_edit") ?: false
                if (!updateDinein) {
                    if (dList.isNotEmpty()) {
                        dList[0].floorPlanTable?.id?.let {
                            viewModel.getTableStatus(
                                it, "Available"
                            )
                        }
                        prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_dineInFragment)
                    }
                } else {
                    prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_dineInFragment)
                }
            }
        } else {
            Log.e(TAG, "DineinNewCh NoOrderType")
            if (rolePermission.hasTablePermission(binding.root)) {
                // prefProvider.setValue(ORDER_TYPE, DINE_IN)
                prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_dineInFragment)
            }
        }
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

    fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }

    private fun getConnectedPrinters() {

        printerViewModel.printerList().observe(viewLifecycleOwner) {
            when (it.status) {

                Status.SUCCESS -> {
                    //ProgressUtils.dismissProgressDialog()
                    //binding.maskLayout?.gone()

                    val data = it.data
                    LogUtil.logE(TAG, "getConnectedPrinters:  ${Gson().toJson(data)}")

                    if (data?.isNotEmpty() == true) {
                        var isInnerPrinterConnected = false
                        for (i in data.indices) {
                            if (data[i].name.startsWith(SUNMI_INNER_PRINTER, true) && (data[i].receiptPrintType == CUSTOMER || data[i].receiptPrintType == KITCHENANDCUSTOMER)) {
                                isInnerPrinterConnected = true
                                break
                            }
                        }
                        if (!isInnerPrinterConnected) {
                            searchBluetooth()
                        }else{
                            binding.maskLayout?.gone()
                        }

                    } else {
                        searchBluetooth()
                    }

                }

                Status.ERROR -> {
                    LogUtil.logE(TAG, "getConnectedPrinters - ${it.message}")
                    //ProgressUtils.dismissProgressDialog()
                    binding.maskLayout?.gone()

                }

                Status.LOADING -> {
                    //ProgressUtils.showProgressDialog(requireActivity())
                    binding.maskLayout?.visible()
                }
            }

        }

    }

    @SuppressLint("MissingPermission")
    private fun searchBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter?.isEnabled == true) {

            val availableDevices: Set<BluetoothDevice> = mBluetoothAdapter!!.bondedDevices

            val innerPrinterModel: PrinterListModel

            val filteredPrintersList =
                availableDevices.filter { it.name.startsWith(SUNMI_INNER_PRINTER, true) }

            if (filteredPrintersList.isNotEmpty()) {
                val foundPrinter = filteredPrintersList[0]
                innerPrinterModel = PrinterListModel(
                    printerName = foundPrinter.name,
                    connectionType = Constants.BLUETOOTH,
                    deviceModel = DeviceInfo(
                        DevType.BLUETOOTH,
                        foundPrinter.address,
                        foundPrinter.name,
                        foundPrinter.address,
                        foundPrinter.address
                    ),
                    type = Constants.AVAILABLE,
                    uuid = UUID.randomUUID()
                )
                setupInnerPrinterAttributes(innerPrinterModel)

            }else{
                binding.maskLayout?.gone()
            }

        }else{
            binding.maskLayout?.gone()
        }
    }

    private fun setupInnerPrinterAttributes(innerPrinterModel: PrinterListModel) {
        val list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> = arrayListOf()
        for (i in 0 until ordertypelist.size) {
            list.add(
                CreatePrinterRequestModel.PrinterSettingsAttributes(
                    printType = CUSTOMER,
                    orderTypeId = ordertypelist[i].id,
                )
            )
        }

        val createPrinter = CreatePrinterRequestModel(
            name = innerPrinterModel.printerName,
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
            macAddress = innerPrinterModel.deviceModel?.macAddress,
            modalName = innerPrinterModel.deviceModel?.printerName,
            terminalIds = listOf(prefProvider.getValueInt(Constants.TERMINAL_ID, 1)),
            status = true,
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1),
            receiptPrintType = CUSTOMER,
            printer_type = innerPrinterModel.connectionType,
            ip_address = innerPrinterModel.deviceModel?.ipAddress,
            printerSettingsAttributes = list

        )
        printerViewModel.createPrinter(createPrinter)
    }

    private fun printByBluTooth(content: String) {
        try {
            if (true) {
                BluetoothUtil.sendData(ESCUtil.boldOn())
            } else {
                BluetoothUtil.sendData(ESCUtil.boldOff())
            }
            if (true) {
                BluetoothUtil.sendData(ESCUtil.underlineWithOneDotWidthOn())
            } else {
                BluetoothUtil.sendData(ESCUtil.underlineOff())
            }

            BluetoothUtil.sendData(content.toByteArray(charset("GB18030")))
            BluetoothUtil.sendData(ESCUtil.nextLine(3))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}