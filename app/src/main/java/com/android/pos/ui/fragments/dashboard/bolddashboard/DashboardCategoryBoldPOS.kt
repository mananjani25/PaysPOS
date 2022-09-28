package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.LARGE
import com.android.pos.data.remote.Constants.MEDIUM
import com.android.pos.data.remote.Constants.ONLINE_ORDER_ENABLE
import com.android.pos.data.remote.Constants.OPEN_ORDER_ITEMS
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.SMALL
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.ui.fragments.settings.servicecharge.ServiceChargeListViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.scanner.helpers.ScannerAppEngine
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.zebra.scannercontrol.FirmwareUpdateEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardCategoryBoldPOS() : Fragment(), ItemListner, ItemClickListner,
    ScannerAppEngine.IScannerAppEngineDevEventsDelegate {
    private var dineInList: List<DineInModel>? = null
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private val viewModelServiceCharge by viewModels<ServiceChargeListViewModel>()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    private var serviceChargesList: ArrayList<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private var orderTypeObserver: Observer<Resource<List<TbOrderType>>>? = null
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
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

    fun onlineOrderBadgeDisplay(count: Int) {
        if (count != null) {
            if (count > 0) {
                binding.layoutHeader.txtBadgeCount?.visible()
                binding.layoutHeader.txtBadgeCount?.text = count.toString()
            } else {
                binding.layoutHeader.txtBadgeCount?.gone()
            }
        } else {
            binding.layoutHeader.txtBadgeCount?.gone()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        syncData()

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        if (prefProvider.getValueboolean(ONLINE_ORDER_ENABLE, false)) {
            binding.layoutHeader.linearOnlineorder?.visible()
            viewModel.getOnlineOrderCount()
        } else {
            binding.layoutHeader.linearOnlineorder?.gone()
        }
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
        getwebOrderingCountObserver()
        getDineInData()
        checkSearch()
        observeServiceChargeUpdate()
        prefProvider.setValueboolean(Constants.ORDER_COMPLETED, false)
        binding.lifecycleOwner = this
        return binding.root
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
                    orderDiscount = result.percentage

                    val discountApplyPrice = viewModel.totalPrice
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

                        viewModel.cartLogic(cartList, item, Constants.UPDATE, false)

                    }
                    "Amount" -> {

                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType


                        viewModel.cartLogic(cartList, item, Constants.UPDATE, false)
                    }
                    else -> {
                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType

                        viewModel.cartLogic(cartList, item, Constants.UPDATE, false)

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
                        viewModel.cartLogic(
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
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
            }
        } else {
            loadCartFragment(CartFragment(this, this))
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }
        binding.layoutHeader.txtUserName.text =
            prefProvider.getValue(EMPLOYEE_NAME, "")


        getOnlineOrderIsEnableOrNot()

        initScanner()

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
        } else {
            viewModel.getOnlineOrderCount()
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
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_transactionFragment)
                }
            }

        }
        binding.layoutHeader.txtDineIn.setOnClickListener {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                if (cartList.isNotEmpty()) {
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
                if (rolePermission.hasTablePermission(it)) {
                    prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_dineInFragment)
                }
            }

        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_menuFragment)
            }

        }
        binding.layoutHeader.txtOpenOrder.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
            }
        }
        binding.layoutHeader.txtOnlineOrder?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_onlineOrderFragment)
            }
        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_passcode,
                    bundle
                )
            }
        }
        binding.layoutHeader.ivLock.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_reportEODFragment)
            }
        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            binding.layoutHeaderCheckout.rlRoot.visibility = View.GONE
            binding.layoutHeader.rlRoot.visibility = View.VISIBLE
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }

        binding.layoutHeader.imgSync.setOnClickListener {
            viewModel.syncInventoryModule(false)
        }
        binding.layoutHeader.imgCashdDrawer.setOnClickListener {
            try {
                SunmiPrintHelper.getInstance().openCashBox()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
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
            if (rolePermission.hasManualSalesPermission(binding.root)) {
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
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_manualSalesNew,
                    bundle
                )
            }
        }


    }


    override fun onItemSelected(item: TbItem) {
        Log.e(TAG, "onItemSelectedItem:  ${Gson().toJson(item)}")

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
            val backStateName: String = AddItemFragment.javaClass.getName()
            val fragment = AddItemFragment.newInstance(item, this, cartList, false)
            val fm: FragmentManager = requireActivity().supportFragmentManager
            fm.beginTransaction().add(binding.frameLayout.id, fragment).setReorderingAllowed(true)
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

                    LogUtil.logE(
                        TAG,
                        "dineInCartListData:  ${Gson().toJson(cartList[0].dineInList)}"
                    )
                    if (cartList[0].dineInList?.isNotEmpty() == true) {
                        var dineInList = cartList[0].dineInList
                        dineInList!![0]?.selectedPosition = viewModel.dineInHeaderPosition
                        viewModel.cartLogic(
                            cartList,
                            item,
                            Constants.ADD,
                            false,
                            dineInList = dineInList
                        )
                    }
                } else {
                    viewModel.cartLogic(cartList, item, Constants.ADD, false)
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
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
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
                    viewModel.getOnlineOrderCount()
                    Log.d(TAG, "addObserver: " + Gson().toJson(viewModel.cartModel))
                    Log.d(TAG, "addObserver: " + Gson().toJson(cartList))

                }
            }
        }
        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            Log.e(TAG, "MAllWords:::  ${Gson().toJson(it)}")
            if (it.isEmpty()) {
                cartList.clear()
                cartList = arrayListOf()

            } else {
                cartList.clear()
                cartList = arrayListOf()
                cartList.addAll(it.toCollection(arrayListOf()))
            }

            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {

                binding.layoutHeader.txtKeypad.visibility = View.GONE
            } else {
                binding.layoutHeader.txtKeypad.visibility = View.VISIBLE
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
                    if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
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
        dineInList.add(DineInModel(0, true, 0, "Whole Table", floorPlanTable = orderFloorDetails))
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

        viewModel.cartLogic(cartList, null, Constants.ADD, false, dineInList = dineInList)


    }

    private fun navigateDineInOrder() {
        viewModel._Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", baseResponse.order.totalAmount)
                    bundle.putParcelable("dineInList", baseResponse)
                    bundle.putBoolean("isGuestPaid", false)
                    bundle.putInt("orderId", baseResponse.order.id)
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
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

                prefProvider.setValue(ORDER_TYPE, DINE_IN)
                prefProvider.setValue(Constants.ORDER_TYPE_NAME, DINE_IN)
                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, 2)

                cartList[0].note = arguments?.getString("order_note").toString()
                LogUtil.logE("AAjeDine", "cartdiscountPrice  ${cartList[0].discountPrice}")
                LogUtil.logE(
                    "AAjeDine",
                    "dineTotalDiscount  ${arguments?.getDouble("totalDiscount")}"
                )
                cartList[0].discountPrice = arguments?.getDouble("totalDiscount") ?: 0.0
                viewModel.cartLogic(cartList, null, Constants.ADD, false, dineInList = dineInList)
                // viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0

            }


        }
    }

    private fun dineInUpdateOrder() {


        viewModel.updateOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {


                val bundle = Bundle()
                if (cartList.isNotEmpty())
                    bundle.putParcelable("cartList", cartList[0])
                bundle.putBoolean("isGuestPaid", false)
                if (cartList.isNotEmpty())
                    cartList[0].orderId?.let { it1 -> bundle.putInt("orderId", it1) }
                prefProvider.setValue(ORDER_TYPE, TAKEOUT)
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

    override fun onPause() {
        arguments?.clear()
        super.onPause()
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
                                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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
            setService(createOrderResponse.data)


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
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                }

            }


            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type, createOrderResponse.data)

            } else {
                LogUtil.logE(TAG, "PrinterIsNotNull:")
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                }

            }
        }

        /* } else {

         }
 */
    }

    private fun setService(data: CreateOrderResponse.Data) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(data)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(data)
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
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                }
                LogUtil.logE(TAG, "QueueCreateAgain")

                clearCustomer()
                if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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
                                                        initKitchenPrinter(
                                                            it.data.get(i),
                                                            Constants.KITCHEN,
                                                            createOrderResponse
                                                        )
                                                    }
                                                }

                                            } else {
                                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                                                }
                                            }
                                        }

                                    }
                                }


                            }


                        } else {
                            viewModel.downloadFinished(false)
                            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                        }

                    }
                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

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


            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {

                builder = Builder(pname, PrinterClass.language, requireActivity())
                LogUtil.logE(TAG, "kitchenFonts:  ${kitchenSettingModel.fonts}")
                LogUtil.logE(TAG, "kitfontSize:  ${fontSizeH}")

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)
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

                    addBuilderText(builder, receiptModel?.order?.orderType.toString())
                }

                /* if (receiptModel?.order?.orderType.trim().lowercase() == "OpenOrder".trim()
                     .lowercase()
               ) {*/
                builder.addFeedLine(1)
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

                addBuilderText(builder, receiptModel?.order?.deliveryType.toString())

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


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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
                builder.addFeedLine(1)
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
                builder.addFeedLine(1)
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

                addBuilderText(builder, receiptModel?.order?.deliveryType.toString())

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


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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

            builder?.addFeedLine(2)

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

                timeOut = 1000
            }

            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    timeOut, status
                )

                PrinterClass.closePrinter()
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                }

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                viewModel.downloadFinished(false)
                if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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
            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.id)
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(receiptModel?.order?.orderType.toString())
            }
            PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())


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


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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

                        if (receiptModel?.order?.orderType.trim().lowercase() == "Open Order".trim()
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

            PrintSunmiUtils.cutPaper()

            viewModel.downloadFinished(false)

            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
        }

    }

    private fun generateKitchenReceiptSunmiInner(

        receiptModel: CreateOrderResponse.Data
    ) {
        try {

            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.id)
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.headerText(receiptModel?.order?.orderType.toString())
            }
            PrintSunmiUtils.headerText(receiptModel?.order?.deliveryType.toString())

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
                    it
                )
            }

            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(receiptModel?.order?.note.toString())

            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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

                        if (receiptModel?.order?.orderType.trim().lowercase() == "Open Order".trim()
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

            PrintSunmiUtils.cutPaperInner()

            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)


        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.downloadFinished(false)
            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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

    override fun scannerBarcodeEvent(barcodeData: ByteArray?, barcodeType: Int, scannerID: Int) {
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

            viewModel.cartLogic(cartList, item, Constants.ADD, false)

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
}