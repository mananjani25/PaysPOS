package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.CategorySearchData
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.AMOUNT_TYPE
import com.android.pos.data.remote.Constants.BUNDLE_ISLOYALTYAPPLIED
import com.android.pos.data.remote.Constants.BUNDLE_ORDER_ID
import com.android.pos.data.remote.Constants.BUNDLE_ORDER_OFFLINE_ID
import com.android.pos.data.remote.Constants.BUNDLE_PAYMENT_ID
import com.android.pos.data.remote.Constants.BUNDLE_PAYMENT_OFFLINE_ID
import com.android.pos.data.remote.Constants.CASHDIS_SURCHARGEENABLE
import com.android.pos.data.remote.Constants.CASH_DIS_STORED
import com.android.pos.data.remote.Constants.CUSTOMER_ID
import com.android.pos.data.remote.Constants.CUSTOMER_NAME
import com.android.pos.data.remote.Constants.DEFAULT_ORDER
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_INGUEST_SELECTED
import com.android.pos.data.remote.Constants.DINE_IN_LIST_EDIT
import com.android.pos.data.remote.Constants.DINE_IN_STATUS
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.HORIZONTAL
import com.android.pos.data.remote.Constants.IS_NEXT_AMOUNT
import com.android.pos.data.remote.Constants.IS_ORDER_UPDATE
import com.android.pos.data.remote.Constants.LAYOUT_ORIENTATION
import com.android.pos.data.remote.Constants.LOYALTY_ADDED
import com.android.pos.data.remote.Constants.MANUALSALE
import com.android.pos.data.remote.Constants.MERGED
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.OPTION_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.PERCENTAGE
import com.android.pos.data.remote.Constants.RATE_OR_AMOUNT
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.remote.Constants.VERTICAL
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.activities.SwipeHelper
import com.android.pos.ui.adapter.*
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.*
import com.android.pos.utils.MethodUtils.Companion.isDoubleClick
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.*
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.scanner.helpers.ScannerAppEngine
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.workmanager.UploadWorker2
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.zebra.scannercontrol.FirmwareUpdateEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject


@AndroidEntryPoint
class DashboardCategoryNew : Fragment(), CategoryItemAdapter1.CategoryItemList, MyCallback,
    DineInAdapter.DineInCallback, CategoryTabAdapter1.TabListner,
    ItemCallback, View.OnClickListener, ScannerAppEngine.IScannerAppEngineDevEventsDelegate {

    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private var openORderType: String = ""
    private lateinit var nameObserver: Observer<List<CartModel>>

    //  private var isOpenOrderUpdate: Boolean = false
    private var orderDiscount: Double = 0.0
    private var categoryItemAdapter1: CategoryItemAdapter1? = null
    private var categoryTabAdapter1: CategoryTabAdapter1? = null
    private var clickManualSales: Boolean = false

    //private var customerUpdate: Boolean = false
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var isOrderUpdate: Boolean = false
    private var isReOrder: Boolean = false
    private var future_delivery_time: String = ""
    private var popupWindow: PopupWindow? = null
    private var totalDiscountMannualAdded = 0.0
    private var orderType: TbOrderType? = null
    private var future_delivery_date: String = ""
    private var assignCustomer: TbCustomer? = null
    var cashDiscount: Double = 0.0
    var amountToBepaid = 0.0
    private var serviceChargesList: ArrayList<TbServiceCharge>? = null
    private var singleItem: TbItem? = null
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentDashboardCategoryNewBinding
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()

    private val TAG = "DashboardCategoryNew"

    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList1: MutableList<CategoryWithInventory> = arrayListOf()
    private var itemList1: ArrayList<TbItem?> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    private lateinit var searchAdapter: CategorySearchAdapter
    private lateinit var searchList: ArrayList<CategorySearchData>
    private lateinit var cartAdapter: CartAdapter
    private lateinit var orderTypeAdapter: OrderTypeAdapter
    private val viewModelPayment by viewModels<PaymentViewModel>()
    private var optionType: String = ""
    private lateinit var dineInCartAdapter: DineInAdapter
    lateinit var cashDiscountModel: CashDiscountModel
    var cashDiscountType = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentDashboardCategoryNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        getLoyaltyPrograms()


        isOrderUpdate = requireArguments().getBoolean("update")

        if (isOrderUpdate) {
            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
            viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                requireArguments().getBoolean("isLoyaltyApplied")

            //save pref
            prefProvider.setValueboolean(IS_ORDER_UPDATE, value = true)
            prefProvider.setValueInt(BUNDLE_ORDER_ID, value = orderId ?: 0)
            prefProvider.setValueInt(BUNDLE_PAYMENT_ID, value = paymentId ?: 0)
            prefProvider.setValue(BUNDLE_PAYMENT_OFFLINE_ID, value = paymentOfflineId)
            prefProvider.setValueboolean(
                BUNDLE_ISLOYALTYAPPLIED,
                value = viewModel.redeemLoyaltyInfo.needToApplyLoyalty
            )
        } else {
            //check pref
            if (prefProvider.getValueboolean(IS_ORDER_UPDATE, defaultValue = false)) {
                isOrderUpdate = true
                orderId = prefProvider.getValueInt(BUNDLE_ORDER_ID, defaultValue = 0)
                paymentId = prefProvider.getValueInt(BUNDLE_PAYMENT_ID, defaultValue = 0)
                paymentOfflineId =
                    prefProvider.getValue(BUNDLE_PAYMENT_OFFLINE_ID, defaultValue = "")
                orderOfflineId = prefProvider.getValue(BUNDLE_ORDER_OFFLINE_ID, defaultValue = "")
                viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                    prefProvider.getValueboolean(BUNDLE_ISLOYALTYAPPLIED, false)
            }

        }

        isReOrder = requireArguments().getBoolean("reorder")

        viewModel.isOrderUpdate = isOrderUpdate
        if (isOrderUpdate) {
            binding.layoutCart.txtSave.text = getString(R.string.update)
        } else {
            binding.layoutCart.txtSave.text = getString(R.string.save)
        }


        navigateDineInOrder()
        dineInUpdateOrder()
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()
        observeQueueCreate()
        queuePrinterObserver()
        prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
        prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
        prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)



        binding.footer.linearEmpnameRole.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_passcode, bundle)
        }

        binding.footer.linearClockout.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_reportEODFragment)
        }

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        return binding.root
    }

    fun clearPrefrenceOfOrder() {
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue("WholeTotal", "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")

        prefProvider.setValue(Constants.SUB_TOTAL_DINEIN, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TIPS_AMOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TAX_CHARGE_DINEIN, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE_DINEIN, "")
        prefProvider.setValue(Constants.TOTAL_PRICE_DINEIN, "")
        optionType = prefProvider.getValue(OPTION_TYPE, "")
    }

    private fun getLoyaltyPrograms() {
        LogUtil.logE("Loyalty", "getLoyaltyPrograms called..")
        viewModel.activeLoyaltyProgram = prefProvider.getActiveLoyaltyData()
        viewModel.activeLoyaltyProgramLiveData.observe(requireActivity(), {
            if (it.status == Status.SUCCESS && it.data != null) {
                LogUtil.logE("Loyalty", "getLoyaltyPrograms fetched..")
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            }
        })
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeShowProgress()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeShowProgress()
        syncData()
        getBackstack()
//        clearPrefrenceOfOrder()
        hideOrderType()
        setupAdapter()
        setVenueData()
        configureDrawer()
        onClick()
        getOrderTypes()
        getServiceCharges()
        setupSnackbar()
        swipeListener()
        observeSaveOrder()
        tableStatusCheck()
        tableStatusSucess()
        checkDineInEditOrder()
        binding.layoutCart.llShowMenu.setOnClickListener(this)
        binding.layoutCart.txtCrtNewCustomer.setOnClickListener(this)
        binding.layoutCart.txtClearItems.setOnClickListener(this)
        binding.layoutCart.llInfo.setOnClickListener(this)
        binding.layoutCart.btnPay.setOnClickListener(this)
        binding.layoutCart.btnSave.setOnClickListener(this)
        binding.layoutCart.llCartMenu.setOnClickListener(this)
        binding.layoutCart.imgOrderMenu.setOnClickListener(this)
        binding.layoutCart.txtAddDiscount.setOnClickListener(this)

        binding.footer.txtEmployeeName.text =
            prefProvider.getValue(EMPLOYEE_NAME, "").toString()
        binding.root.setOnClickListener {
            if (binding.layoutCart.llCustomerDialog.visibility == View.VISIBLE) {
                binding.layoutCart.llCustomerDialog.visibility = View.GONE
            }
        }

        binding.root.setOnClickListener {
            if (binding.layoutCart.llOrderMenu.visibility == View.VISIBLE) {
                binding.layoutCart.llOrderMenu.visibility = View.GONE
            }
        }

        if (prefProvider.getValue(CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.layoutCart.txtCustomerName.text =
                prefProvider.getValue(CUSTOMER_NAME, "").toString()
            removeCustomerViewSet()

            //loyalty
            val customer = prefProvider.getCustomerData()
            customer?.let {
                viewModel.selectedCustomer = customer
                if (viewModel.loyaltyPointCondition(customer)) {
                    binding.layoutCart.txtLoyaltyPoints.visible()
                    "${getString(R.string.loyalty_points)}: ${customer.final_reward}".also {
                        binding.layoutCart.txtLoyaltyPoints.text = it
                    }
                } else {
                    binding.layoutCart.txtLoyaltyPoints.gone()
                }
            } ?: binding.layoutCart.txtLoyaltyPoints.gone()
        } else {
            binding.layoutCart.txtLoyaltyPoints.gone()
        }


        setFragmentResultListener("request_key_customer") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                LogUtil.logE(TAG, "bundleSelectBundle:  ${Gson().toJson(bundle)}")
                setUpCustomer(result, bundle)
            }
        }
        setFragmentResultListener("request_key_customer_open_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                LogUtil.logE(TAG, "gotBundlebundle:  ${Gson().toJson(bundle)}")
                setUpCustomer(result, bundle)


            }
        }

        setFragmentResultListener("request_key_orderType") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbOrderType>("data")
            if (result != null) {
                chooseOrderType(result)

            }
        }
        setFragmentResultListener("request_key_discount_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            if (result != null && viewModel.totalPrice != 0.0) {
                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice

                val price = discountApplyPrice - orderDiscount

                MethodUtils.setPriceTextView(
                    binding.layoutCart.txtTotalAmount,
                    price
                )
                if (cartList.isNotEmpty()) {
                    cartList[0].discountPrice = orderDiscount
                    cartList[0].discountType = result.discountType
                    if (result.id != -1) {
                        cartList[0].discountId = result.id
                    }
                    viewModel.addCart(cartList[0])
                }


            }
        }

        //barcode events
        initScanner()
    }


    private fun initScanner() {
        //barcode event listener
//        (activity as MainActivity).addDevEventsDelegate(this)
    }


    private fun syncData() {

        val sync = prefProvider.getValueboolean(Constants.SYNC_DATA, false)
        if (!sync)
            viewModel.syncInventoryModule(true)
    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            var dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")

            if (dineInList?.isNotEmpty() == true) {
                binding.layoutCart.txtOrderType.setText("Dine In")

                dineInCartAdapter = DineInAdapter()
                dineInCartAdapter.setListner(this)
                //dineInCartAdapter.setList(dineInList)


                if (cartList.isEmpty()) {
                    val cartModel = CartModel().apply {
                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                        employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                        locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                        orderTypeId = 2
                        orderType = DINE_IN
                        orderTypeName = DINE_IN

                        serviceCharge = serviceChargesList
                        orderId = arguments?.getInt("orderId")

                    }
                    cartList.add(cartModel)
                }

                var orderTableData: GetOrderDetailsResponse.Data.FloorPlanTable? =
                    arguments?.getParcelable("tableDetails")

                dineInList.forEach {
                    it.floorPlanTable = orderTableData
                }

                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
                prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
                prefProvider.setValueInt(ORDER_TYPE_ID, prefProvider.getValueInt(ORDER_TYPE_ID, 0))

                viewModel.newCartLogicModifier(cartList, null, ADD, false, dineInList = dineInList)
                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0

            }


        }
    }

    private fun setupAdapter() {
        categoryTabAdapter1 = CategoryTabAdapter1(requireContext(), tabList, this)
        binding.rvTabLayout.adapter = categoryTabAdapter1

        categoryItemAdapter1 = CategoryItemAdapter1(requireContext(), itemList1, this)
        binding.rvPagerCategory.adapter = categoryItemAdapter1

    }

    override fun onPause() {
        super.onPause()
//        ProgressUtils.dismissProgressDialog()

        serviceChargesObserve?.let { viewModel.serviceCharges.removeObserver(it) }

    }

    private fun removeCustomerViewSet() {
        binding.layoutCart.txtCrtNewCustomer.text = "Remove Customer"
    }

    private fun saveCustomerData(customer: TbCustomer?) {
        viewModel.selectedCustomer = customer
        prefProvider.saveCustomerData(customer)
    }

    private fun setUpCustomer(
        result: TbCustomer,
        bundle: Bundle
    ) {
        prefProvider.setValue(CUSTOMER_NAME, result.first_name + " " + result.last_name)
        binding.layoutCart.txtCustomerName.text = result.first_name + " " + result.last_name
        saveCustomerData(result)
        //loyalty
        if (viewModel.loyaltyPointCondition(result)) {
            binding.layoutCart.txtLoyaltyPoints.visible()
            binding.layoutCart.txtLoyaltyPoints.text =
                "${getString(R.string.loyalty_points)}: ${result.final_reward}"
        } else {
            binding.layoutCart.txtLoyaltyPoints.gone()
        }

        removeCustomerViewSet()
        assignCustomer = result

        result.id?.let { prefProvider.setValueInt(CUSTOMER_ID, it) }

        if (isOrderUpdate) {
            if (cartList.isEmpty()) {
                val model = CartModel()
                model.serviceCharge = viewModel.serviceChargesList

                cartList.add(model)
                cartList[0].customer = assignCustomer
                viewModel.addCart(cartList[0])
            } else {
                cartList[0].customer = assignCustomer
                viewModel.addCart(cartList[0])
            }
        }
        val openOrder = bundle.getBoolean("OPEN_ORDER")
        if (openOrder) {
            future_delivery_date = bundle.getString("DATE").toString()
            future_delivery_time = bundle.getString("TIME").toString()
            openORderType = bundle.getString("TYPE").toString()
            prefProvider.setValueInt(ORDER_TYPE_ID, orderType?.id ?: 3)
            prefProvider.setValue(ORDER_TYPE_NAME, orderType?.name ?: OPEN_ORDER)
            LogUtil.logE("!_@_", "523 ${orderType?.orderType ?: ""}")
            prefProvider.setValue(ORDER_TYPE, orderType?.orderType ?: OPEN_ORDER)
            hideOrderType()
        }
    }

    private fun swipeListener() {


        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.layoutCart.rvCart) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                val markAsUnreadButton = markAsUnreadButton(position)
                val archiveButton = archiveButton(position)
                return listOf(deleteButton, markAsUnreadButton, archiveButton)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.layoutCart.rvCart)

    }


    private fun getBackstack() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(Constants.KEY)
            ?.observe(viewLifecycleOwner) { it ->
                if (it == MANUALSALE) {
                    hideOrderType()
                }
            }
    }

    private fun getOrderTypes() {
        orderTypeAdapter = OrderTypeAdapter()
        orderTypeAdapter.setCallback(this)
        binding.rvOrderType.adapter = orderTypeAdapter

        viewModel.orderTypes().observe(requireActivity()) {
            it.data?.let { it1 -> orderTypeAdapter.addAll(it1) }
        }

    }

    private fun getServiceCharges() {

        serviceChargesObserve = Observer {

            if (it.status == Status.SUCCESS) {
                serviceChargesList = it.data as ArrayList<TbServiceCharge>?
                getCartList()
            }

        }

        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }


    private fun hideMenu() {
        if (binding.layoutCart.llCustomerDialog.visibility == View.VISIBLE) {
            binding.layoutCart.llCustomerDialog.visibility = View.GONE
        } else {
            binding.layoutCart.llCustomerDialog.visibility = View.VISIBLE
        }
    }

    private fun hideOrderMenu() {
        if (binding.layoutCart.llOrderMenu.visibility == View.VISIBLE) {
            binding.layoutCart.llOrderMenu.visibility = View.GONE
        } else {
            binding.layoutCart.llOrderMenu.visibility = View.VISIBLE
        }
    }

    private fun getCartList() {

        LogUtil.logE("Loyalty", "getCartList called..")

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
        binding.layoutCart.rvCart.adapter = cartAdapter
        binding.layoutCart.rvCartDineIn.adapter = dineInCartAdapter


        nameObserver = Observer {

           // bindData(it)

            removeObserver()
        }


//        if (isAdded)
//            addObserver()
    }

    private fun addObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""), prefProvider.getValueInt(
                EMPLOYEE_ID, 0
            )
        ).observe(
            requireActivity(), nameObserver
        )

    }

    private fun removeObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""), prefProvider.getValueInt(
                EMPLOYEE_ID, 0
            )
        ).removeObserver(nameObserver)
        //  addObserver()
    }

    private fun bindData(it: List<CartModel>?) {
        LogUtil.logE("bindData", "YesAdded")

        cartList = it as ArrayList<CartModel>
        viewModel.destroyedList.clear()

        if (cartList.isNotEmpty()) {
            cartList[0].items?.filter { item -> item.isDestroy }?.let {
                viewModel.destroyedList.addAll(it)
            }

            refreshOrderTypeLabel()

            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                viewModel.setServiceCharges(serviceChargesList)
                cartList.get(0).serviceCharge = serviceChargesList
                binding.layoutCart.rvCart.visibility = View.GONE
                binding.layoutCart.rvCartDineIn.visibility = View.VISIBLE
                binding.layoutCart.llPayment.visibility = View.VISIBLE

                val dineList: List<DineInModel>? =
                    cartList.get(0).dineInList


                if (dineList != null) {
                    dineInCartAdapter.setList(dineList.toCollection(arrayListOf()) , viewModel.listItems)

                }

                if (cartList[0].items?.isNotEmpty() == true) {

                    val dineList = cartList[0].dineInList ?: dineInCartAdapter.getList()
                    // mannual sale added in dineinn //yash
                    cartList[0].items?.forEach {

                        if (it.isManualSales && dineList.isNotEmpty()) {

                            if (it.timeStamp == null || it.timeStamp?.lowercase() == "null".lowercase()) {
                                it.timeStamp = viewModel.randomOfflineId()
                            }
                            dineList.get(0).selectedPosition =
                                prefProvider.getValueInt(Constants.DINE_INGUEST_SELECTED, 0)
                            dineList[prefProvider.getValueInt(
                                Constants.DINE_INGUEST_SELECTED,
                                0
                            )].items.add(it)
                            Log.d(
                                "yash",
                                "bindData: dineine HEaderPositonn " + prefProvider.getValueInt(
                                    Constants.DINE_INGUEST_SELECTED,
                                    0
                                )
                            )

                        }
                        cartList[0].items?.toCollection(arrayListOf())?.clear()
                        cartList[0].items = listOf()

                    }

                    viewModel.newCartLogicModifier(cartList, null, ADD, false, dineInList = dineList)

                }

                val list1 = cartList.get(0).dineInList
                if (isAdded) {

                    setFragmentResultListener("request_key_customer_dine_in") { _, bundle ->
                        val result = bundle.getParcelable<TbCustomer>("data")
                        if (result != null) {


                            if (list1?.isNotEmpty() == true) {

                                var position = bundle.getInt("position")

                                val dineInList = list1
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


            } else {


                binding.layoutCart.rvCart.visibility = View.VISIBLE
                binding.layoutCart.rvCartDineIn.visibility = View.GONE
                if (cartList[0].items?.isEmpty() == true) {
                    binding.layoutCart.llPayment.gone()
                } else
                    binding.layoutCart.llPayment.visible()

                LogUtil.logE(TAG, "cartList[0].items > ${cartList[0].items?.size}")
                LogUtil.logE(TAG, "viewModel.destroyedList > ${viewModel.destroyedList.size}")
                cartAdapter.addCart(cartList[0].items)
            }
            viewModel.itemCalculation(
                cartList,
                binding.layoutCart.txtTotalAmount,
                requireContext()
            )

            val orderType = prefProvider.getValue(ORDER_TYPE, "")
            LogUtil.logE("!_@_", "rlSave -------- $orderType ")
            if (orderType == TAKEOUT || orderType == DINE_IN) {
                LogUtil.logE("!_@_", "rlSave -- GONE ")
                binding.layoutCart.rlSave.visibility = View.GONE
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == DINE_IN) {
                    binding.layoutCart.txtTotalAmount.visibility = View.GONE
                    binding.layoutCart.txtPay.visibility = View.GONE
                    binding.layoutCart.txtDineInProceed.visibility = View.VISIBLE
                    if (prefProvider.getValueboolean(DINE_IN_UPDATE, false) == true) {

                        binding.layoutCart.txtDineInProceed.setText("Update and Proceed")
                    } else {
                        binding.layoutCart.txtDineInProceed.setText("Proceed To Fire")
                    }
                } else {
                    binding.layoutCart.txtDineInProceed.visibility = View.GONE
                    binding.layoutCart.rvCart.visible()
                    if (cartList[0].items?.isEmpty() == true) {
                        binding.layoutCart.llPayment.gone()
                    } else
                        binding.layoutCart.llPayment.visible()
                    //  binding.layoutCart.llPayment.visible()
                }
            } else {
                LogUtil.logE("!_@_", "rlSave -- VISIBLE ")
                binding.layoutCart.rlSave.visibility = View.VISIBLE
                binding.layoutCart.rvCart.visible()
                //  binding.layoutCart.llPayment.visible()
                if (cartList[0].items?.isEmpty() == true) {
                    binding.layoutCart.llPayment.gone()
                } else
                    binding.layoutCart.llPayment.visible()
            }


        } else {

            viewModel.itemCalculation(
                cartList,
                binding.layoutCart.txtTotalAmount,
                requireContext()
            )

            binding.layoutCart.rvCart.gone()
            binding.layoutCart.llPayment.gone()


        }


        if (prefProvider.getValueInt("ORDER_ID", -1) != -1) {
            LogUtil.logE(TAG, "ManualSale ORderIDNOt Null")
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString() != DINE_IN) {
                lifecycleScope.launchWhenResumed {
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                        val bundle = bundleOf(IS_NEXT_AMOUNT to true)

                        /*  findNavController().navigate(
                              R.id.action_dashboardCategoryNew_to_paymentFragment, bundle
                          )*/
                    }
                }
            }

            gotoPayment()
        }
    }

    private fun hideOrderType() {
        LogUtil.logE(TAG, "ORDERTYPE:  ${prefProvider.getValue(ORDER_TYPE, "")}")

        if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
            binding.layoutCart.llCart.visibility = View.VISIBLE
            binding.lltakeout.visibility = View.GONE
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString() == DINE_IN) {
                if (!prefProvider.getValueboolean(DINE_IN_STATUS, true)) {
                    binding.layoutCart.llShowMenu.visibility = View.GONE
                    binding.layoutCart.viewDineIn.visibility = View.GONE
                    binding.layoutCart.rvCart.visibility = View.GONE
                    binding.layoutCart.rvCartDineIn.visibility = View.GONE

                    if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(ORDER_TYPE, "")
                    }
                    hideOrderType()

                } else {

                    binding.layoutCart.txtOrderType.text = "Dine In"
                }
            } else {
                binding.layoutCart.txtOrderType.text =
                    prefProvider.getOrderTypeName(ORDER_TYPE_NAME, DEFAULT_ORDER).toString()
            }
            if (prefProvider.getOrderTypeName(ORDER_TYPE_NAME, DEFAULT_ORDER) == DINE_IN) {
                binding.layoutCart.llShowMenu.visibility = View.GONE
                binding.layoutCart.viewDineIn.visibility = View.VISIBLE

                binding.layoutCart.rvCart.visibility = View.GONE
                binding.layoutCart.rvCartDineIn.visibility = View.VISIBLE
                if (requireArguments().getBoolean("isFromDineIn")) {
                    getDineInCartList()
                } else if (arguments?.getBoolean("is_dine_in_edit") == true) {
                    checkDineInEditOrder()
                }

            } else {
                binding.layoutCart.llShowMenu.visibility = View.VISIBLE
                binding.layoutCart.viewDineIn.visibility = View.GONE
                binding.layoutCart.rvCart.visibility = View.VISIBLE
                binding.layoutCart.rvCartDineIn.visibility = View.GONE
            }
            getCartList()


        } else {
            binding.layoutCart.txtOrderType.text = ""
            binding.lltakeout.visibility = View.VISIBLE
            binding.layoutCart.llCart.visibility = View.GONE


        }
    }


    private fun searchCategory() {

        searchList = arrayListOf()
        categoryList1.forEach { categories ->
            val itemList = categories.inventoryLists
            itemList?.filter { it?.isHide == true }?.forEach { tbItem ->
                searchList.add(
                    CategorySearchData(
                        tbItem?.itemId ?: 0,
                        tbItem?.name ?: "",
                        tbItem?.imageUrl.toString(),
                        categories.category.name ?: "",
                        categories.category.id
                    )
                )
            }
        }
        searchAdapter =
            CategorySearchAdapter(
                requireActivity(),
                R.layout.search_category_item,
                searchList
            )
        binding.layoutMenu.autoSearch.threshold = 3
        binding.layoutMenu.autoSearch.setAdapter(searchAdapter)
        binding.layoutMenu.autoSearch.setOnItemClickListener { parent, _, position, _ ->
            val model: CategorySearchData = parent.getItemAtPosition(position) as CategorySearchData
            binding.layoutMenu.autoSearch.setText(model.title)
            resetTabbySearch(model)


        }
    }

    private fun onClick() {
        binding.layoutMenu.imgOptionMenu.setOnClickListener {
            showPopup(binding.layoutMenu.imgOptionMenu)
        }

        binding.layoutMenu.imgSync.setOnClickListener {
//            viewModel.syncInventoryModule()
        }

        binding.footer.linearMore.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_menuFragment)
            //dialogPOSMenu()

        }

        binding.footer.linearTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_transactionFragment)

            }
        }
        binding.footer.linearOpenOrders.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
        }


    }

    private fun dialogPOSMenu() {

        val dialog = Dialog(requireContext(), android.R.style.Theme_Light)

        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )


        dialog.setContentView(R.layout.menu_pos)
        dialog.setCanceledOnTouchOutside(false)

        val imgClose: ImageView = dialog.findViewById(R.id.imgClose)
        val footerView: View = dialog.findViewById(R.id.footer)

        val imgCalculator: ImageView = footerView.findViewById(R.id.imgCalculator)
        val txtCheckOut: TextView = footerView.findViewById(R.id.txtCheckOut)
        val txtEmployeename: TextView = footerView.findViewById(R.id.txtEmployeeName)
        val imgMore: ImageView = footerView.findViewById(R.id.imgMore)
        val txtMore: TextView = footerView.findViewById(R.id.txtMore)
        val linearEmprole: LinearLayoutCompat = footerView.findViewById(R.id.linear_empname_role)
        val linearclockout: LinearLayoutCompat = footerView.findViewById(R.id.linear_clockout)
        val linearMore: LinearLayout = footerView.findViewById(R.id.linearMore)
        val linearHome: LinearLayout = dialog.findViewById(R.id.linearHome)
        val linearOrders: LinearLayout = dialog.findViewById(R.id.linearOrders)
        val linearTransaction: LinearLayout = dialog.findViewById(R.id.linearTransaction)
        val linearCash: LinearLayout = dialog.findViewById(R.id.linearCash)
        val linearReports: LinearLayout = dialog.findViewById(R.id.linearReports)
        val linearCust: LinearLayout = dialog.findViewById(R.id.linearCust)
        val linearTeam: LinearLayout = dialog.findViewById(R.id.linearTeam)
        val linearInventory: LinearLayout = dialog.findViewById(R.id.linearInventory)
        val linearSetting: LinearLayout = dialog.findViewById(R.id.linearSetting)
        val linearSupport: LinearLayout = dialog.findViewById(R.id.linearSupport)
        val txtSignOut: TextView = dialog.findViewById(R.id.txtSignOut)
        val txtBusinessName: TextView = dialog.findViewById(R.id.txtBusinessName)
        val linearOpenOrders: LinearLayout = dialog.findViewById(R.id.linearOpenOrders)
        val footer: RelativeLayout = dialog.findViewById(R.id.footer)
        val linearHardware: LinearLayout = dialog.findViewById(R.id.linearHardware)
        val footerTransaction: LinearLayout =
            footer.findViewById<LinearLayout>(R.id.linearTransaction)
        val linearCheckOut: LinearLayout = footer.findViewById(R.id.linearCheckOut)

        linearEmprole.setOnClickListener {
            dialog.dismiss()
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_passcode, bundle)
        }
        linearclockout.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_reportEODFragment)
        }
        txtEmployeename.text =
            prefProvider.getValue(EMPLOYEE_NAME, "").toString()
        txtBusinessName.text = getString(R.string.business_name) + ": " + prefProvider.getValue(
            Constants.BUSINESS_NAME,
            ""
        )

        linearCheckOut.setOnClickListener {
            dialog.dismiss()
        }

        linearHome.setOnClickListener {
            closeDialog(dialog)
        }
        linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_hardware)
            dialog.dismiss()
        }

        linearCust.setOnClickListener {
            if (rolePermission.hasCustomerPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_customer)
                closeDialog(dialog)
            }
        }
        linearReports.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_reports)
            dialog.dismiss()
        }

        linearOpenOrders.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
            dialog.dismiss()
        }

        linearTeam.setOnClickListener {
            if (rolePermission.hasEmployeePermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_teamList)
                dialog.dismiss()
            }
        }
        linearInventory.setOnClickListener {
            if (rolePermission.hasInventoryPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_inventory)
                dialog.dismiss()
            }
        }
        linearSetting.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_settings)
            dialog.dismiss()
        }

        linearOrders.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
            dialog.dismiss()
        }

        footerTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_transactionFragment)
                dialog.dismiss()
            }
        }
        linearTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_transactionFragment)
                dialog.dismiss()
            }
        }
        linearCash.setOnClickListener {
            if (rolePermission.hasCashLogPermission(binding.root)) {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_cashLogFragment)
                dialog.dismiss()
            }
        }

//        linearSupport.setOnClickListener {
//            throw RuntimeException("Test Crash") // Force a crash
//
//
//            dialog.dismiss()
//
//        }


        txtSignOut.setOnClickListener {


            alert("", "Are you sure you want to Logout?") {
                this.positiveButton("Logout") {
                    viewModel.logoutAPI()
                }
                this.negativeButton("Cancel") {
                }

            }

            closeDialog(dialog)
        }

        imgCalculator.setColorFilter(resources.getColor(R.color.txtColor))
        txtCheckOut.setTextColor(resources.getColor(R.color.txtColor))
        imgMore.setColorFilter(resources.getColor(R.color.txt_color_blue))
        txtMore.setTextColor(resources.getColor(R.color.txt_color_blue))

        imgClose.setOnClickListener {
            closeDialog(dialog)
        }

        dialog.show()
    }

    private fun closeDialog(dialog: Dialog?) {
        dialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
     //   ProgressUtils.dismissProgressDialog()
    }

    private fun horizontalTabList() {
        prefProvider.setValue(LAYOUT_ORIENTATION, "0")
        val params = binding.rvTabLayout.layoutParams
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
        params.width = 0
        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = HORIZONTAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()


        val set = ConstraintSet()
        set.clone(binding.constraintParent)

        //Category TabList Horizontal View Set
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.START,
            binding.root.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.TOP,
            binding.viewLine.id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM,
            binding.root.id,
            ConstraintSet.BOTTOM
        )
        set.setVerticalBias(binding.rvTabLayout.id, 0F)


        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(), 8)
        //CategoryList RecyclerView View Set
        val params1 = binding.rvTabLayout.layoutParams
        params1.height = 0
        params1.width = LinearLayout.LayoutParams.WRAP_CONTENT
        binding.rvTabLayout.layoutParams = params1

        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.TOP,
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.START,
            binding.root.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )

        set.applyTo(binding.constraintParent)

    }

    private fun verticalTabList() {
        prefProvider.setValue(LAYOUT_ORIENTATION, "1")
        val params: ViewGroup.LayoutParams = binding.rvTabLayout.layoutParams
        params.height = 0
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT


        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = VERTICAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()


        val set = ConstraintSet()
        set.clone(binding.constraintParent)

        //Category TabList REcyclerViewTab View Set
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.TOP,
            binding.root.findViewById<View>(R.id.layoutMenu).id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.START,
            binding.rvPagerCategory.id,
            ConstraintSet.END
        )


        //CategoryList REcyclerView View Set
        val params1 = binding.rvTabLayout.layoutParams
        params1.height = 0
        params1.width = LinearLayout.LayoutParams.WRAP_CONTENT
        binding.rvTabLayout.layoutParams = params1

        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(), 6)


        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.START,
            binding.constraintParent.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.TOP,
            binding.root.findViewById<View>(R.id.layoutMenu).id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.END,
            binding.rvTabLayout.id,
            ConstraintSet.START
        )


        set.applyTo(binding.constraintParent)

    }

    private fun configureDrawer() {


        binding.layoutMenu.txtKeypad.setOnSingleClickListener {

            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                if (rolePermission.hasManualSalesPermission(binding.root)) {
                    findNavController().navigate(R.id.action_dashboardCategoryNew_to_manualSales)
                }
            } else {
                clickManualSales = true
                orderTypeDialog()
            }


        }


    }

    private fun setVenueData() {
        viewModel.venueDataLocal().observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Status.SUCCESS -> {

                    val tbCategory = it.data
                    if (tbCategory != null) {

                        categoryList1 = tbCategory as MutableList<CategoryWithInventory>

                        tabList.clear()
                        for (i in 0 until categoryList1.size) {

                            if (i == 0) {
                                tabList.add(
                                    CategoryTabModel(
                                        categoryList1[i].category.id,
                                        categoryList1[i].category.name ?: "",
                                        true,
                                        0
                                    )
                                )
                            } else {
                                tabList.add(
                                    CategoryTabModel(
                                        categoryList1[i].category.id,
                                        categoryList1[i].category.name ?: "",
                                        false,
                                        0
                                    )
                                )
                            }
                        }

                        if (categoryList1.isNotEmpty()) {

                            categoryTabAdapter1?.addAll(tabList)

                            itemList1.clear()
                            itemList1.add(
                                0,
                                TbItem()
                            )
                            categoryList1[0].inventoryLists?.filter {
                                it!!.isHide
                            }?.let { it1 ->
                                itemList1.addAll(it1)
                            }

                            searchCategory()
                            categoryItemAdapter1?.addAll(itemList1)

                        }

                    }

                    ProgressUtils.dismissProgressDialog()

                    if (!prefProvider.getValue(LAYOUT_ORIENTATION, "").isNullOrEmpty()) {
                        if (prefProvider.getValue(LAYOUT_ORIENTATION, "") == "0")
                            horizontalTabList()
                        else
                            verticalTabList()
                    }

                }
                Status.ERROR ->
                    ProgressUtils.dismissProgressDialog()

                //Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

            }
        }

    }

    private fun showPopup(view: View) {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.PopupMenuOverlapAnchor)
        val popup = PopupMenu(contextThemeWrapper, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.tab_category_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.menuVertical -> {
                    verticalTabList()
                    true
                }
                R.id.menuHorizontal -> {
                    horizontalTabList()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    @SuppressLint("SetTextI18n")
    private fun showPopupWindow(view: View) {

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window_new, null)

        //set pop up data
        setPopUpData(popupView)

        //listeners
        val chkLoyalty: CheckBox = popupView.findViewById(R.id.chkLoyaltyAmount)
        chkLoyalty.setOnCheckedChangeListener { _, p1 ->
            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = p1
            prefProvider.setValueboolean(LOYALTY_ADDED, p1)
            //refresh pop up data and final calculation
            setPopUpData(popupView)
            refreshItemCalculation()
        }

        if (prefProvider.getValueboolean(LOYALTY_ADDED, false)) {
            if (!chkLoyalty.isChecked) {
                chkLoyalty.isChecked = true
                setPopUpData(popupView)
                refreshItemCalculation()
            }
        } else {
            chkLoyalty.isChecked = false
        }

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow?.setBackgroundDrawable(BitmapDrawable())
        popupWindow?.isOutsideTouchable = true


        popupWindow?.setOnDismissListener(PopupWindow.OnDismissListener {
            //TODO do sth here on dismiss
        })
        popupWindow?.showAtLocation(view, Gravity.TOP, 600, 650);
//        } else {
//            popupWindow!!.dismiss()
//            popupWindow = null
//        }

    }

    private fun setPopUpData(popupView: View) {
        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)
        val txtLoyaltyAmount: AppCompatTextView = popupView.findViewById(R.id.txtLoyaltyAmount)
        //   val groupLoyalty: Group = popupView.findViewById(R.id.groupLoyalty)
        val chkLoyalty: CheckBox = popupView.findViewById(R.id.chkLoyaltyAmount)
        val txtLoyaltyPoints: AppCompatTextView = popupView.findViewById(R.id.txtLoyaltyPoints)
        val lblLoyaltyPoints: AppCompatTextView = popupView.findViewById(R.id.lblLoyaltyPoints)
        val lblLoyaltyAmount: AppCompatTextView = popupView.findViewById(R.id.lblLoyaltyAmount)

        LogUtil.logE(TAG, "subTotalPrice:   ${viewModel.subTotalPrice - (cartList[0].discountPrice)}")


        val txtTotalcashAdj: AppCompatTextView = popupView.findViewById(R.id.txtnoncashadj)
        val linearCCashDiscount: LinearLayoutCompat =
            popupView.findViewById(R.id.lineaarCashDiscount)
        val linear_NonCashDiscount: LinearLayoutCompat =
            popupView.findViewById(R.id.linear_NonCashDiscount)

        txtSubTotal.text = "$" + String.format(
            "%.2f",
            if (viewModel.subTotalPrice < 0.0) 0.0 else viewModel.subTotalPrice
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            if (viewModel.subTotalPrice < 0.0) 0.0 else viewModel.totalServiceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            viewModel.totalDiscount + cartList[0].discountPrice
        )


        //  txtTotalAmount.text = total.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            if (viewModel.subTotalPrice < 0.0) 0.0 else viewModel.totalTax
        )

        //display the loyalty point
        val customer = viewModel.selectedCustomer
        if (viewModel.loyaltyPointCondition(customer)) {

            lblLoyaltyPoints.visible()
            txtLoyaltyPoints.visible()
            lblLoyaltyAmount.visible()
            txtLoyaltyAmount.visible()

            LogUtil.logE(TAG, "InsideLoyalty")
            LogUtil.logE(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
            amountToBepaid = viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
            txtLoyaltyAmount.text =
                "- $${String.format("%.2f", viewModel.redeemLoyaltyInfo.usedLoyaltyAmount)}"
            txtLoyaltyPoints.text = "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"


            // groupLoyalty.gone()
            chkLoyalty.visible()
            chkLoyalty.isChecked = viewModel.redeemLoyaltyInfo.needToApplyLoyalty
        } else {
            amountToBepaid = viewModel.totalPrice
            lblLoyaltyPoints.gone()
            txtLoyaltyPoints.gone()
            lblLoyaltyAmount.gone()
            txtLoyaltyAmount.gone()

            //   groupLoyalty.gone()
            chkLoyalty.gone()
        }

        //display total price to be paid
        if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {
            linear_NonCashDiscount.visibility = View.VISIBLE
            txtTotalcashAdj.text = "$" + String.format(
                "%.2f",
                MethodUtils.calculateCashDiscount(
                    amountToBepaid,
                    prefProvider,
                    requireContext()
                )
            )
        } else {
            linear_NonCashDiscount.visibility = View.GONE
        }


        MethodUtils.setPriceTextView(txtTotalAmount, amountToBepaid)
    }

    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list
        for (i in 0 until tabList.size) {

            if (tabList[i].id == model.categoryID) {
                tabList[i].isSelected = true
                tabPos = i
            } else {
                tabList.get(i).isSelected = false
            }

        }

        // (binding.rvTabLayout.adapter as CategoryTabAdapter1).list.clear()
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()

        val listCategry = arrayListOf<TbItem?>()
        listCategry.add(
            0,
            TbItem()
        )
        categoryList1[tabPos].inventoryLists?.let { it1 ->
            listCategry.addAll(
                it1
            )
        }
        (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list.clear()
        (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list = listCategry
        binding.rvPagerCategory.adapter?.notifyDataSetChanged()


    }

    override fun onClick(item: TbItem) {

        if (prefProvider.getValue(ORDER_TYPE, "") != "") {

            if (item.modifier_set_ids.isEmpty() && item.variationsAttributes.isEmpty()) {

                // qty check logic
                var itemQty = 1
                if (cartList.isNotEmpty()) {
                    cartList[0].items?.filter { it.itemId == item.itemId }?.map {
                        itemQty += it.itemQuantity
                    }
                }
                if (item.quantity >= itemQty) {
                    item.itemQuantity = 1

                    if (cartList.isEmpty()) {
                        viewModel.setServiceCharges(serviceChargesList)
                    }
                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                        if (cartList.isNotEmpty()) {
                            cartList[0].orderType = DINE_IN
                        }

                        // yash simple dashboard add cart
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                        viewModel.newCartLogicModifier(cartList, item, ADD, false, dineInList = dineInList)


                    } else {
                        //check is_edited flag
                        makeItemEdited(item)

                        viewModel.newCartLogicModifier(cartList, item, ADD, false)
                    }
                } else {
                    item.itemQuantity = -1
                    AlertUtils.showCustomAlert(
                        requireActivity(),
                        getString(R.string.qty_validation)
                    )
                }
            } else {
                ItemPopup(item, true)
            }
        } else {
            orderTypeDialog()
        }

    }

    override fun onClickedCreateItem() {
        findNavController().navigate(R.id.action_dashboardCategoryNew_to_createItem)
    }

    @SuppressLint("SetTextI18n")
    override fun onItemClickListener(view: View?, data: TbCartItem, position: Int) {

        if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {

//            ItemPopup(data, false)
        } else {
            orderTypeDialog()
        }
    }

    override fun onCartItemClickListener(view: View?, data: TbCartItem, position: Int) {
        TODO("Not yet implemented")
    }

    private fun makeItemEdited(item: TbItem) {
        if (isOrderUpdate) {
            //for open order and edit cart
            item.isEdited = true
        }
    }

    private fun orderTypeDialog() {

        val bundle = Bundle().apply {
            putParcelableArrayList("data", orderTypeAdapter.list)
        }
        findNavController().navigate(
            R.id.action_dashboardCategoryNew_to_orderTypeDialog,
            bundle
        )

    }

    private fun ItemPopup(data: TbItem, isItemClick: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = lp

        dialog.setContentView(R.layout.dialog_update_quantity)

        val imgClose: AppCompatImageView = dialog.findViewById(R.id.imgBack)
        val txtSave: AppCompatTextView = dialog.findViewById(R.id.txtSave)
        val txtTitle: AppCompatTextView = dialog.findViewById(R.id.txtTitle)
        val txtQty: AppCompatEditText = dialog.findViewById(R.id.txtQty)
        val llPlus: LinearLayoutCompat = dialog.findViewById(R.id.llPlus)
        val llMinus: LinearLayoutCompat = dialog.findViewById(R.id.llMinus)
        val btnRemove: AppCompatTextView = dialog.findViewById(R.id.btnRemove)
        val btnAddDiscount: AppCompatTextView = dialog.findViewById(R.id.btnAddDiscount)
        val edtNote: AppCompatEditText = dialog.findViewById(R.id.edtNote)
        val rvModifierSets: RecyclerView = dialog.findViewById(R.id.rvModifierSets)
        val rvVariationList: RecyclerView = dialog.findViewById(R.id.rvVariationList)
        val txtItemName: AppCompatTextView = dialog.findViewById(R.id.txtItemName)
        val edtItemName: AppCompatEditText = dialog.findViewById(R.id.edtItemName)

        edtItemName.visibility = View.GONE
        var discountPrice = data.discountPrice


        var adapter: ItemModifierSetAdapter? = null
        var variationAdapter: VariationDashboardListAdapter? = null

        var qty = data.itemQuantity


        txtQty.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                val enteredString = s.toString()
                if (enteredString.startsWith("0")) {

                    if (enteredString.length > 0) {
                        txtQty.setText(enteredString.substring(1))
                    } else {
                        txtQty.setText("")
                    }
                } else if (s.toString().trim().isNotEmpty() && s.toString().toInt() > 10000) {
                    txtQty.setText("10000")
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        if (isItemClick) {
            qty = 1
            txtQty.setText(qty.toString())
            btnRemove.visibility = View.GONE
            btnAddDiscount.visibility = View.GONE
        }

        if (data.modifier_set_ids.isNotEmpty()) {
            adapter = ItemModifierSetAdapter(viewModel, data.itemId, viewLifecycleOwner)
            rvModifierSets.adapter = adapter

            val intArray = IntArray(data.modifier_set_ids.size) { i ->
                data.modifier_set_ids[i]
            }
            viewModel.modifierSet(intArray).observe(requireActivity(), {
                if (it.data != null && it.data.isNotEmpty()) {
                    rvModifierSets.visibility = View.VISIBLE
                    it.data.let { it1 -> adapter.add(it1) }

                    if (!isItemClick) {
//                        adapter.setData(data.modifiers)
                    }

                } else rvModifierSets.visibility = View.GONE

            })
        }
        if (data.variationsAttributes.isNotEmpty()) {
            rvVariationList.layoutManager = GridLayoutManager(activity, 3);
            variationAdapter = VariationDashboardListAdapter()
            rvVariationList.adapter = variationAdapter
            viewModel.getItemsbyId(data.itemId).observe(viewLifecycleOwner, {

                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            it.data?.let {
                                rvVariationList.visibility = View.VISIBLE
                                txtItemName.text = it.name + ":-  Choose One"
                                variationAdapter.addVariations(it.variationsAttributes)
                                if (data.variationsAttributes.isNotEmpty() && data.variationsAttributes[0].id != null) {
                                    variationAdapter.selectItem(
                                        data.variationsAttributes[0].id ?: 0
                                    )
                                }
                                showPriceTitle(
                                    variationsAttribute = null,
                                    variationAdapter,
                                    data,
                                    txtTitle,
                                    isItemClick
                                )

                            }

                        }
                        Status.ERROR -> {
                            rvVariationList.visibility = View.GONE
                        }
                        Status.LOADING -> {
                            rvVariationList.visibility = View.GONE
                        }
                    }
                }


            })

        } else {
            rvVariationList.visibility = View.GONE
            txtItemName.visibility = View.GONE
            showPriceTitle(
                variationsAttribute = null,
                variationAdapter,
                data,
                txtTitle,
                isItemClick
            )
        }

        edtNote.setText(data.note)



        txtQty.setText(qty.toString())

        variationAdapter?.showVariationPriceClick = { it: VariationsAttribute ->
            if (!isDoubleClick()) {
                if (it.priceType == "Variable") {
                    val bundle = Bundle().apply {
                        putParcelable("variationAttribute", it)
                    }
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_addVariablePriceDialog, bundle
                    )
                } else if (it.priceType == "Fixed") {
                    showPriceTitle(it, variationAdapter = null, data, txtTitle, isItemClick)
                }
            }
        }

        //allow to update price only if variation exists
        if (data.variationsAttributes.isNotEmpty()) {
            val resultVariationDetails =
                getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_VARIATION_DETAILS)
            resultVariationDetails?.observe(viewLifecycleOwner) {
                variationAdapter?.updateVariation(it)
                showPriceTitle(it, variationAdapter = null, data, txtTitle, isItemClick)
            }
        }

        imgClose.setOnClickListener {
            dialog.dismiss()
            totalDiscountMannualAdded = 0.0
        }
        txtSave.setOnClickListener {


            if (data.price == 0.0 && data.variationsAttributes.isNotEmpty()) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    "Please enter atleast one price of item"
                )
                return@setOnClickListener
            }

            LogUtil.logE("checkItemQty", checkItemQty(data, variationAdapter).toString())

            if (!checkItemQty(data, variationAdapter)) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    getString(R.string.qty_validation)
                )
                return@setOnClickListener
            }

            val variationList = ArrayList<VariationsAttribute>()
            if (data.variationsAttributes.isNotEmpty()) {
                val variation = variationAdapter?.getItem()!!
                variationList.add(variation)
                data.name = data.name.substringBefore(" (") + " (" + variation.name + ")"
                data.variationsAttributes = variationList
            }


            if (minMaxValidationCheck(adapter)) {
                dialog.dismiss()

                data.note = edtNote.text.toString().trim()
                data.itemQuantity = txtQty.text.toString().toInt()
                if (!isItemClick) {


                    if (discountPrice == 0.00) {
                        data.discountPrice = (discountPrice
                                * txtQty.text.toString().toInt()
                                )
                    } else {
                        data.discountPrice = discountPrice
                    }

                }


                val modifiers = adapter?.getSelectedModifiers()
                if (modifiers != null) {
                    modifiers.forEach {
                        it.itemQuantity = data.itemQuantity
                    }
                    data.modifiers = modifiers
                }

                if (cartList.isEmpty()) {
                    viewModel.setServiceCharges(serviceChargesList)
                }

                //check is_edited flag

//                if (totalDiscountMannualAdded > 0) {
//                    data.discountPrice = totalDiscountMannualAdded
//                } else {
//                    data.discountPrice = totalDiscountMannualAdded
//                }

                makeItemEdited(data)

                if (isItemClick) {

                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
                        dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                        viewModel.newCartLogicModifier(cartList, data, ADD, false, dineInList = dineInList)
                    } else {
                        viewModel.newCartLogicModifier(cartList, data, ADD, false)
                    }
                } else {
                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
                        viewModel.newCartLogicModifier(cartList, data, UPDATE, false, dineInList = dineInList)

                    } else {

                        viewModel.newCartLogicModifier(cartList, data, UPDATE, false)
                    }
                }
            } else {
                AlertUtils.showCustomAlert(
                    binding.root.context,
                    binding.root.context.getString(R.string.you_can_add)
                )

            }

            totalDiscountMannualAdded = 0.0
        }

        llPlus.setOnClickListener {

            qty += 1
            txtQty.setText(qty.toString())

            if (data.variationsAttributes.isNotEmpty()) {

                val stockQty = variationAdapter?.getItem()?.stockQty

                if (stockQty?.isNotEmpty() == true) {

                    if (stockQty.toInt() >= qty) {
                        txtQty.setText(qty.toString())
                    } else {
                        qty -= 1
                        stockValidationAlert(qty, txtQty)
                    }

                } else {
                    qty -= 1
                    stockValidationAlert(qty, txtQty)
                }

            } else {

                if (data.isManualSales) {
                    txtQty.setText(qty.toString())
                } else {

                    if (data.quantity >= qty) {
                        txtQty.setText(qty.toString())
                    } else {
                        qty -= 1
                        stockValidationAlert(qty, txtQty)
                    }
                }
            }


        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            txtQty.setText(qty.toString())


        }
        btnRemove.setOnClickListener {

            makeItemEdited(data)
            viewModel.newCartLogicModifier(cartList, data, DELETE, false)
            dialog.dismiss()
        }
        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    when {
                        result.discountType == requireContext().getString(R.string.disc_percentage) -> {

                            data.discountPrice = calculateDiscountPercentage(
                                itemPriceWithModifies(data),
                                result.percentage
                            )
                            totalDiscountMannualAdded = data.discountPrice
                            discountPrice = data.discountPrice / data.itemQuantity
                            data.discountId = result.id
                            data.discountType = result.discountType
                            data.isManualSales = false
                            txtTitle.text = data.name + "  $" + String.format(
                                "%.2f",
                                (totalPrice(data) - data.discountPrice)
                            )

                            viewModel.newCartLogicModifier(cartList, data, UPDATE, false)

                        }
                        result.discountType == "Amount" -> {
                            totalDiscountMannualAdded = data.discountPrice
                            data.discountPrice = result.percentage
                            data.discountId = 0
                            data.discountType = result.discountType
                            data.isManualSales = false
                            discountPrice = data.discountPrice / data.itemQuantity

                            viewModel.newCartLogicModifier(cartList, data, UPDATE, false)
                        }
                        else -> {
                            totalDiscountMannualAdded = data.discountPrice
                            data.discountPrice = result.percentage
                            data.discountId = 0
                            data.discountType = result.discountType
                            data.isManualSales = false
                            discountPrice = data.discountPrice / data.itemQuantity

                            viewModel.newCartLogicModifier(cartList, data, UPDATE, false)
                        }
                    }

                } else {
                    data.discountPrice = 0.0
                    data.discountType = ""
                    data.isManualSales = false
                    data.discountId = 0
                    discountPrice = data.discountPrice
                    // viewModel.cartLogic(cartList, data, Constants.UPDATE)
                }

            }


            val bundle = Bundle().apply {
                putBoolean("isFromDetails", true)
                putParcelable("model", data)
            }

            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_addDiscountDialog,
                bundle
            )
        }


        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun checkItemQty(
        data: TbItem,
        variationAdapter: VariationDashboardListAdapter?
    ): Boolean {

        if (data.variationsAttributes.isNotEmpty()) {

            val stockQty = variationAdapter?.getItem()?.stockQty

            return if (stockQty?.isNotEmpty() == true) {

                stockQty.toInt() >= 1

            } else {
                false
            }

        } else {

            return if (data.isManualSales) {
                true
            } else {
                data.quantity >= 1
            }
        }

        return false
    }

    private fun stockValidationAlert(qty: Int, txtQty: AppCompatEditText) {
        txtQty.setText(qty.toString())
        AlertUtils.showCustomAlert(
            requireActivity(),
            getString(R.string.qty_validation)
        )
    }


    @SuppressLint("SetTextI18n")
    private fun showPriceTitle(
        variationsAttribute: VariationsAttribute?,
        variationAdapter: VariationDashboardListAdapter?,
        data: TbItem,
        txtTitle: AppCompatTextView,
        isItemClick: Boolean
    ) {
        var variation: VariationsAttribute? = null
        if (variationAdapter != null) {
            variation = variationAdapter.getItem()
        } else if (variationsAttribute != null) {
            variation = variationsAttribute
        }



        if (variation != null) {
            variation.price?.let {
                data.price = it
            }

        } else {
            data.price = data.price
        }
        if (data.discountPrice != 0.0) {
            txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                "%.2f", (totalPrice(data) - data.discountPrice)
            )
        } else {
            if (isItemClick) {
                txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                    "%.2f",
                    data.price
                )
            } else
                txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                    "%.2f",
                    totalPrice(data)
                )
        }
    }


    private fun minMaxValidationCheck(adapter: ItemModifierSetAdapter?): Boolean {

        if (adapter != null) {
            val list = adapter.getAll()
            if (list.size == 1) {
                list.forEach {
                    return (it.min_required == 0) || minLogic(
                        it.min_required,
                        it.modifiers
                    )
                }
            } else {

                var min_required = 0

                val mlist = ArrayList<Modifier>()

                list.forEach {
                    min_required += it.min_required
                    mlist.addAll(it.modifiers)
                }

                return (min_required == 0) || minLogic(
                    min_required,
                    mlist
                )

            }
        }
        return true
    }

    private fun minLogic(
        maxCount: Int,
        modifiers: List<Modifier>
    ): Boolean {

        if (maxCount == 0) {
            return true
        }
        var totalMinMax = 0

        modifiers.forEach {
            if (it.isChecked) {
                totalMinMax += 1
            }
        }

        return maxCount <= totalMinMax
    }

    @SuppressLint("RestrictedApi")
    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                   // ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModelPayment.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress2", it.toString())
                if (it) {
                  //  ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModel.callCashDiscount.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    viewModel.getCashDiscountDetails(active = 1)
                        ?.observe(viewLifecycleOwner, { cashDiscountData ->
                            if (cashDiscountData != null) {
                                cashDiscountModel = cashDiscountData
                                prefProvider.setValueboolean(CASHDIS_SURCHARGEENABLE, true)
                                prefProvider.setValue(AMOUNT_TYPE, cashDiscountData.amount_type)
                                prefProvider.setValue(OPTION_TYPE, cashDiscountData.option_type)
                                prefProvider.setValue(
                                    RATE_OR_AMOUNT,
                                    cashDiscountData.rate_or_amount.toString()
                                )
                                prefProvider.setValueboolean(CASH_DIS_STORED, true)
                                cashDiscountType = cashDiscountData.option_type
                                Log.d(TAG, "onCreateView: " + cashDiscountModel.rate_or_amount)
                            } else {
                                prefProvider.setValueboolean(CASHDIS_SURCHARGEENABLE, false)
                                prefProvider.setValue(AMOUNT_TYPE, "")
                                prefProvider.setValue(OPTION_TYPE, "")
                                prefProvider.setValue(
                                    RATE_OR_AMOUNT,
                                    "0"
                                )
                                prefProvider.setValueboolean(CASH_DIS_STORED, true)
                                cashDiscountType = ""
                            }
                        })

                }
            }
        })


        viewModel.logout.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    WorkManager.getInstance(requireActivity()).cancelAllWork()
                    val data = Data.Builder()
                        //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                        // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                        .put("location_id", prefProvider?.getValueInt(Constants.LOCATION_ID, 0))
                        .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, ""))
                        .put(
                            Constants.IS_PRINTER_QUEUE_ENABLE, prefProvider?.getValueboolean(
                                Constants.IS_PRINTER_QUEUE_ENABLE, false
                            )
                        )
                        .put("is_cancel_work",true)
                        .build()

                    val uploadWorkRequest =
                        OneTimeWorkRequest.Builder(
                            UploadWorker2::class.java
                        ).addTag(Constants.PRINTER_QUEUE_BACKGROUND)
                            .setInputData(data)
                            .build()


                    val workManager = WorkManager.getInstance(requireContext())


                    prefProvider.setClear()
                    viewModel.clearTable()
                    prefProvider.setValueboolean(Constants.CHECK_QUEUE_CANCEL,true)
                    try {

                        workManager.enqueueUniqueWork(
                            Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.REPLACE,
                            uploadWorkRequest
                        )

                    } catch (e: java.lang.Exception) {

                        e.printStackTrace()
                    }
                    prefProvider.setValue(Constants.AUTH_TOKEN, "")
                    findNavController().navigate(R.id.action_global_login)


                }
            }
        })
    }

    private fun observeSaveOrder() {

        viewModelPayment.QueueStart.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { it ->
                // AlertUtils.showCustomAlert(requireActivity(), it.message)
                binding.layoutCart.txtSave.text = getString(R.string.save)
                viewModel.deleteCart()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }

                clearCustomer()
                hideOrderType()
                clearUpdateFlag()
                getKitchenPrinters(it)


            }
        })

    }

    private fun getKitchenPrinters(createOrderResponse: CreateOrderResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                //    ProgressUtils.showProgressDialog(requireActivity())

                    if (it.data?.isNotEmpty() == true) {

                        for (i in 0 until it.data.size) {

                            initKitchenPrinter(
                                it.data.get(i),
                                Constants.KITCHEN,
                                createOrderResponse
                            )
                        }
                    } else {
                        ProgressUtils.dismissProgressDialog()
                        findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
                    }

                }
                Status.LOADING -> {
                  //  ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                    findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)

                }
            }

        })

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        orderType = orderTypeAdapter.getItem(pos)

        chooseOrderType(orderType!!)
    }

    private fun chooseOrderType(orderType: TbOrderType) {

        when (orderType.orderType) {
            TAKEOUT -> {
                prefProvider.setValueInt(ORDER_TYPE_ID, orderType.id)
                prefProvider.setValue(ORDER_TYPE_NAME, orderType.name)
                prefProvider.setValue(ORDER_TYPE, orderType.orderType)
                hideOrderType()
            }
            DINE_IN -> {
                prefProvider.setValueInt(ORDER_TYPE_ID, orderType.id)
                prefProvider.setValue(ORDER_TYPE_NAME, orderType.name)
                prefProvider.setValue(ORDER_TYPE, orderType.orderType)
                findNavController().graph.startDestination = R.id.dashboardCategoryNew
                if (findNavController().currentDestination?.id == R.id.orderTypeDialog) {
                    findNavController().navigate(
                        R.id.action_orderTypeDialog_to_dineInFragment
                    )
                } else {
                    findNavController().navigate(R.id.action_dashboardCategoryNew_to_dineInFragment)
                }
            }
            OPEN_ORDER -> {

                /*findNavController().navigate(
                    R.id.action_dashboardCategoryNew_to_openOrderCustomerFragment
                )*/
                if (findNavController().currentDestination?.id == R.id.orderTypeDialog) {

                    findNavController().navigate(
                        R.id.action_orderTypeDialog_to_openOrderCustomerFragmentNew
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_openOrderCustomerFragmentNew
                    )
                }
            }
        }

//        if (clickManualSales) {
//            clickManualSales = false
//            findNavController().navigate(R.id.action_dashboardCategoryNew_to_manualSales)
//        }
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtAddDiscount -> {

                hideOrderMenu()
                val bundle = Bundle()
                bundle.putBoolean("isOrderDiscount", true)
                bundle.putDouble("totalPrice", viewModel.totalPrice)
                if (cartList.isNotEmpty()) {
                    bundle.putDouble("orderDiscountPrice", cartList[0].discountPrice)
                    bundle.putString("orderDiscountType", cartList[0].discountType)
                }
                findNavController().navigate(
                    R.id.action_dashboardCategoryNew_to_addDiscountDialog,
                    bundle
                )
            }

            R.id.llShowMenu -> {
                hideMenu()
            }
            R.id.imgOrderMenu -> {
                hideOrderMenu()
            }
            R.id.txtCrtNewCustomer -> {
                if (prefProvider.getValue(CUSTOMER_NAME, "").toString().isNotEmpty()) {
                    prefProvider.setValueboolean(LOYALTY_ADDED, false)
                    clearCustomer()
                } else {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_assignCustomerOrderFragment
                    )
                }

            }
            R.id.txtClearItems -> {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_items_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        // Do positive stuff here
                        prefProvider.setValueInt(DINE_INGUEST_SELECTED, 0)

                        totalDiscountMannualAdded = 0.0
                        if (prefProvider.getValue(ORDER_TYPE, "").toString() == DINE_IN) {

                            val dList = dineInCartAdapter.getList()
                            if (dList.isNotEmpty()) {
                                dList[1].floorPlanTable?.id?.let {

                                    if (dList[1]?.floorPlanTable?.status.toString() == MERGED) {
                                        viewModel.getTableStatus(it, MERGED)
                                    } else {
                                        viewModel.getTableStatus(
                                            it, "Available"
                                        )
                                    }
                                }
                            }
                            viewModel.deleteCart()
                            isOrderUpdate = false

                            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                                prefProvider.setValue(ORDER_TYPE, "")
                            }
                            binding.layoutCart.txtSave.text = getString(R.string.save)
                            clearCustomer()
                            hideOrderType()
                            hideOrderMenu()
                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                            clearUpdateFlag()

                        } else {
                            viewModel.deleteCart()

                            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                                prefProvider.setValue(ORDER_TYPE, "")
                            }
                            binding.layoutCart.txtSave.text = getString(R.string.save)
                            clearCustomer()
                            hideOrderType()
                            hideOrderMenu()
                            clearUpdateFlag()
                            prefProvider.setValueboolean(LOYALTY_ADDED, false)
                        }
                    }
                    negativeButton(R.string.tv_cancel) {
                        // Do negative stuff here
                    }
                }

            }

            R.id.llInfo -> {

                showPopupWindow(v)
            }

            R.id.btnSave -> {

//                if (prefProvider.getValue(ORDER_TYPE, "").toString() != TAKEOUT) {

                if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN) {

                    if (viewModel.restrictedAmount(binding.layoutCart.txtTotalAmount)) {
                        val cartList = viewModel.generateCombinedItems(cartList[0])
                        cartList.openOrderType = openORderType
                        if (!isOrderUpdate)
                            cartList.customer = assignCustomer


                        //set the latest parameter in viewmodel
                        viewModelPayment.updateOrder(
                            isOrderUpdate,
                            orderId,
                            paymentId,
                            paymentOfflineId,
                            orderOfflineId
                        )

                        // (viewModel.redeemLoyaltyInfo.getAmountToBePaid() + (viewModel.redeemLoyaltyInfo.cashDiscount
                        //                            ?: 0.0)),


//                    (viewModel.redeemLoyaltyInfo.remainingLoyaltyAmount + (viewModel.redeemLoyaltyInfo.cashDiscount
//                        ?: 0.0)),

                        var totalAmountTobeSave = 0.0
                        if (viewModel.redeemLoyaltyInfo.isLoyaltyApplied == true) {
                            totalAmountTobeSave =
                                (viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0)
                        } else {
                            totalAmountTobeSave =
                                (binding.layoutCart.txtTotalAmount.text.toString().subSequence(
                                    2,
                                    binding.layoutCart.txtTotalAmount.text.length
                                ) as String).toDouble()
                        }

                        if (cartList.futureDeliveryDate.isNotEmpty()) {
                            future_delivery_date = cartList.futureDeliveryDate
                        }

                        val request = viewModelPayment.createOpenOrderRequest(
                            cartList,
                            viewModel.subTotalPrice,
                            totalAmountTobeSave,
                            viewModel.totalServiceCharge,
                            viewModel.totalTax,
                            OPEN_ORDER,
                            future_delivery_date,
                            future_delivery_time,
                            false,
                            viewModel.totalDiscount + cartList.discountPrice,
                            0.00,
                            -1,
                            viewModel.redeemLoyaltyInfo,
                            MethodUtils.calculateCashDiscount(
                                viewModel.totalPrice,
                                prefProvider,
                                requireContext()
                            ),
                            false,
                            "Cash",
                            cashDiscountType

                        )
                        viewModelPayment.saveOrder(true)
                        viewModelPayment.submit(request)
                    } else {
                        showMessage()
                    }
//                }
                }
            }

            R.id.btnPay -> {

//                prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT, "")
//                prefProvider.setValueInt(Constants.SPLIT_NO, -1)
//                prefProvider.setValue(Constants.SPLIT_PAY_TYPE, "")
//                prefProvider.setValueInt("ORDER_ID", -1)

                if (viewModel.restrictedAmount(binding.layoutCart.txtTotalAmount)) {
                    gotoPayment()
                } else {
                    showMessage()
                }
            }
            R.id.llCartMenu -> {

            }
        }
    }

    private fun showMessage() {

        AlertUtils.showCustomAlert(requireContext(), "Order should be less than 1 million usd.")
    }

    private fun gotoPayment() {
        LogUtil.logE(TAG, "cartList:  ${cartList.size}")
        if (cartList.isNotEmpty()) {
            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
                var itemCount = 0
                for (i in cartList.indices) {
                    for (j in cartList[i].dineInList?.indices!!) {
                        if (cartList[i].dineInList?.get(j)?.items?.size!! > 0) {
                            itemCount++
                            break
                        }
                    }
                    if (itemCount != 0) {
                        break
                    }

                }

                if (itemCount == 0) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        getString(R.string.please_add_Atleast_one_item_in_cart)
                    ) { _, _ ->
                    }
                } else {
                    val request = viewModel.updateOrder(cartList[0])

                    prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                    prefProvider.setValueboolean(DINE_IN_LIST_EDIT, false)
                    prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                    cartList[0].orderId?.let { viewModel.updateOrderCall(it, request) }


                }

            } else {

                val bundle = Bundle()

                var final_total = (binding.layoutCart.txtTotalAmount.text.toString().subSequence(
                    2,
                    binding.layoutCart.txtTotalAmount.text.length
                ) as String).toDouble()
                bundle.putDouble("totalPrice", final_total)
                bundle.putParcelable(
                    "redeemLoyalty",
                    viewModel.redeemLoyaltyInfo
                )
                bundle.putDouble(
                    "cashDiscountSurcharge",
                    MethodUtils.calculateCashDiscount(final_total, prefProvider, requireContext())
                )

                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    prefProvider.setValue(
                        "cashDiscountSurCharge",
                        MethodUtils.calculateCashDiscount(
                            final_total,
                            prefProvider,
                            requireContext()
                        ).toString()
                    )
                }
                bundle.putDouble(
                    "subTotalPrice",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.subTotalPrice
                )
                bundle.putDouble(
                    "totalTax",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.totalTax
                )
                bundle.putDouble(
                    "totalDiscount",
                    viewModel.totalDiscount + cartList[0].discountPrice
                )
                bundle.putDouble(
                    "totalServiceCharge",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.totalServiceCharge
                )
                bundle.putString("future_delivery_date", future_delivery_date)
                bundle.putString("future_delivery_time", future_delivery_time)
                cartList[0].customer = assignCustomer
                val cartModel = viewModel.generateCombinedItems(cartList[0])
                cartModel.openOrderType = openORderType
                LogUtil.logE(TAG, "PaymentPAsscartModel: ${Gson().toJson(cartModel)}")
                bundle.putParcelable("cartList", cartModel)
                if (isOrderUpdate) {
                    bundle.putBoolean("update", true)
                    orderId?.let { bundle.putInt("orderId", it) }
                    paymentId?.let { bundle.putInt("paymentId", it) }
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                }
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == DINE_IN) {
                    var itemCount = 0
                    for (i in cartList.indices) {
                        for (j in cartList[i].dineInList?.indices!!) {
                            if (cartList[i].dineInList?.get(j)?.items?.size!! > 0) {
                                itemCount++
                                break
                            }
                        }
                        if (itemCount != 0) {
                            createDineInRequest()
                            break
                        }
                    }

                    if (itemCount == 0) {
                        bundle.clear()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            getString(R.string.please_add_Atleast_one_item_in_cart)
                        ) { _, _ ->
                        }

                    }
                } else {
                    prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, final_total.toString())
                    prefProvider.setValue(
                        Constants.SUB_TOTAL_ACTUAL,
                        viewModel.subTotalPrice.toString()
                    )
                    prefProvider.setValue(
                        Constants.TOTAL_DISCOUNT_ACTUAL,
                        viewModel.totalDiscount.toString()
                    )
                    prefProvider.setValue(
                        Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
                        viewModel.totalServiceCharge.toString()
                    )
                    prefProvider.setValue(
                        Constants.TAX_CHARGE_ACTUAL,
                        viewModel.totalTax.toString()
                    )
                    prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")



                    lifecycleScope.launchWhenStarted {
                        if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                            if (prefProvider.getValueInt("ORDER_ID", -1) != -1) {
                                bundle.putBoolean(IS_NEXT_AMOUNT, true)
                            }
                            findNavController().navigate(
                                R.id.action_dashboardCategoryNew_to_paymentFragment,
                                bundle
                            )
                        }
                    }
                }
            }
        } else if (cartList.isEmpty() && prefProvider.getValueInt("ORDER_ID", -1) != -1) {

            val bundle = bundleOf(IS_NEXT_AMOUNT to true)
            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_paymentFragment,
                bundle
            )

        }

    }

    private fun createDineInRequest() {

        var floorModel = DineInOrderDetailAttributes(
            floorPlanId = dineInFloorTableModel?.floorPlanId,
            floorPlanTableId = dineInFloorTableModel?.id,
            tableType = dineInFloorTableModel?.tableType,
            tableName = dineInFloorTableModel?.tableName,
            tableNumber = dineInFloorTableModel?.tableNumber,
            chairCount = dineInFloorTableModel?.chairCount,
            floorPlanName = "Party Dining",
            totalGuestCount = dineInCartAdapter.getList().size + 1


        )

        if (floorModel.floorPlanId == null) {
            floorModel.floorPlanId = cartList.get(0).dineInList?.get(1)?.floorPlanTable?.floorPlanId
            floorModel.floorPlanTableId = cartList.get(0).dineInList?.get(1)?.floorPlanTable?.id
            floorModel.tableType = cartList.get(0).dineInList?.get(1)?.floorPlanTable?.tableType
            floorModel.tableNumber = cartList.get(0).dineInList?.get(1)?.floorPlanTable?.tableNumber
            floorModel.chairCount = cartList.get(0).dineInList?.get(1)?.floorPlanTable?.chairCount


        }
        val orderRequestModel = viewModel.createDineInOrderRequest(
            cartModel = cartList[0],
            subTotalPrice = viewModel.subTotalPrice,
            totalPrice = viewModel.totalPrice - cartList[0].discountPrice,
            totalServiceCharge = viewModel.totalServiceCharge,
            totalTax = viewModel.totalTax,
            ORDER_TYPE = DINE_IN,
            "",
            "",
            false,
            totalDiscount = viewModel.totalDiscount + cartList[0].discountPrice,
            0.0,
            floorPlanDetails = floorModel
        )

        if (orderRequestModel != null) {
            viewModel.submit(orderRequestModel)
        }

    }

    private fun clearCustomer() {
        binding.layoutCart.txtCrtNewCustomer.text = "Add Customer"
        binding.layoutCart.txtCustomerName.text = "Add Customer"
        binding.layoutCart.txtLoyaltyPoints.gone()
        binding.layoutCart.txtLoyaltyPoints.text = ""
        prefProvider.setValue(CUSTOMER_NAME, "")
        prefProvider.setValueInt(CUSTOMER_ID, -1)
        saveCustomerData(null)
        refreshItemCalculation()
    }

    fun calculateDiscountPercentage(originalPrice: Double, percentage: Double): Double {
        return MethodUtils.roundOffAmountDouble((originalPrice * percentage) / 100)
    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }

    private fun itemPriceWithModifies(model: TbItem): Double {

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

    private fun deleteButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            "Delete",
            14.0f,
            R.color.delete,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_item_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            val item = cartAdapter.getItem(position)
                            //cartAdapter.removeItem(position)
                            makeItemEdited(item)
                            viewModel.newCartLogicModifier(cartList, item, DELETE, false)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }
                }
            })
    }

    private fun markAsUnreadButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            "Note",
            14.0f,
            R.color.addNote,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {

                    setFragmentResultListener("request_key_note") { requestKey: String, bundle: Bundle ->
                        val note = bundle.getString("note")

                        singleItem!!.note = note.toString()
                        singleItem?.let { viewModel.newCartLogicModifier(cartList, it, UPDATE, false) }
                    }

                    singleItem = cartAdapter.getItem(position)
                    val bundle = Bundle().apply {
                        putString("note", singleItem!!.note)
                    }

                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_addNoteDialog,
                        bundle
                    )
                }
            })
    }

    private fun archiveButton(pos: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            "Discount",
            14.0f,
            R.color.addDiscount,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {

                    val data = cartAdapter.getItem(pos)

                    setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                        val result = bundle.getParcelable<TbDiscount>("data")

                        if (result != null) {
                            when (result.discountType) {
                                PERCENTAGE -> {

                                    data.discountPrice = calculateDiscountPercentage(
                                        itemPriceWithModifies(data),
                                        result.percentage
                                    )

                                    data.discountId = result.id
                                    data.discountType = result.discountType
                                    data.isManualSales = false

                                    viewModel.newCartLogicModifier(cartList, data, UPDATE, false)


                                }
                                "Amount" -> {

                                    data.discountPrice = result.percentage
                                    data.discountId = 0
                                    data.discountType = result.discountType
                                    data.isManualSales = false
                                    // discountPrice = data.discountPrice

                                    viewModel.newCartLogicModifier(cartList, data, UPDATE, false)

                                }
                                else -> {

                                    data.discountPrice = result.percentage
                                    data.discountId = 0
                                    data.discountType = result.discountType
                                    data.isManualSales = false
                                    // discountPrice = data.discountPrice

                                    viewModel.newCartLogicModifier(cartList, data, UPDATE, false)

                                }
                            }

                        } else {
                            data.discountPrice = 0.0
                            data.discountType = ""
                            data.isManualSales = false
                            data.discountId = 0
                            viewModel.newCartLogicModifier(cartList, data, UPDATE, false)
                        }

                    }

                    val bundle = Bundle().apply {
                        putBoolean("isFromDetails", true)
                        putParcelable("model", cartAdapter.getItem(pos))
                    }

                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_addDiscountDialog,
                        bundle
                    )
                }
            })
    }

    private fun getDineInCartList() {
        dineInCartAdapter = DineInAdapter()
        binding.layoutCart.rvCartDineIn.adapter = dineInCartAdapter
        val numOfGuest: Int by lazy {
            requireArguments().getInt("numberOfGuest")
        }

        dineInFloorTableModel = arguments?.getParcelable("floorplan")


        var orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? =
            arguments?.getParcelable("tableDetails")
        if (orderDEtails != null) {
            orderFloorDetails = orderDEtails
        }
        if (orderFloorDetails.id == null) {
            orderFloorDetails.apply {
                id = dineInFloorTableModel?.id
                chairCount = dineInFloorTableModel?.chairCount
                floorPlanId = dineInFloorTableModel?.floorPlanId
                tableName = dineInFloorTableModel?.tableName.toString()
                status = dineInFloorTableModel?.status.toString()


            }

        }

        val dineInList: ArrayList<DineInModel> = arrayListOf()
        dineInList.add(DineInModel(0, true, 0, "Whole Table",))
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



        dineInCartAdapter.setList(dineInList,viewModel.listItems)

        if (cartList.isEmpty()) {
            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
                orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
                orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()

                serviceCharge = serviceChargesList

            }
            cartList.add(cartModel)
        }

        cartList.get(0).orderType = DINE_IN
        viewModel.newCartLogicModifier(cartList, null, ADD, false, dineInList = dineInList)

    }

    override fun onHeaderSelected(position: Int) {
        prefProvider.setValueInt(DINE_INGUEST_SELECTED, position)
        Log.d(TAG, "onHeaderSelected: header position : $position")
    }

    @SuppressLint("SetTextI18n")
    override fun onItemSelected(headerPosition: Int, position: Int, data: TbCartItem) {
        val isItemClick = false
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = lp

        dialog.setContentView(R.layout.dialog_update_quantity)

        val imgClose: AppCompatImageView = dialog.findViewById(R.id.imgBack)
        val txtSave: AppCompatTextView = dialog.findViewById(R.id.txtSave)
        val txtTitle: AppCompatTextView = dialog.findViewById(R.id.txtTitle)
        val txtQty: AppCompatEditText = dialog.findViewById(R.id.txtQty)
        val llPlus: LinearLayoutCompat = dialog.findViewById(R.id.llPlus)
        val llMinus: LinearLayoutCompat = dialog.findViewById(R.id.llMinus)
        val btnRemove: AppCompatTextView = dialog.findViewById(R.id.btnRemove)
        val btnAddDiscount: AppCompatTextView = dialog.findViewById(R.id.btnAddDiscount)
        val edtNote: AppCompatEditText = dialog.findViewById(R.id.edtNote)
        val rvModifierSets: RecyclerView = dialog.findViewById(R.id.rvModifierSets)
        val rvVariationList: RecyclerView = dialog.findViewById(R.id.rvVariationList)
        val txtItemName: AppCompatTextView = dialog.findViewById(R.id.txtItemName)
        val edtItemName: AppCompatEditText = dialog.findViewById(R.id.edtItemName)

        edtItemName.visibility = View.GONE
        var discountPrice = data.discountPrice / data.itemQuantity


        var adapter: ItemModifierSetAdapter? = null
        var variationAdapter: VariationDashboardListAdapter? = null

        var qty = data.itemQuantity
        if (isItemClick) {
            qty = 1
            txtQty.setText(qty.toString())
            btnRemove.visibility = View.GONE
            btnAddDiscount.visibility = View.GONE
        } else {
            btnRemove.visibility = View.VISIBLE
            btnAddDiscount.visibility = View.VISIBLE
        }

        if (data.modifier_set_ids.isNotEmpty()) {
            adapter = ItemModifierSetAdapter(viewModel, data.itemId, viewLifecycleOwner)
            rvModifierSets.adapter = adapter

            val intArray = IntArray(data.modifier_set_ids.size) { i ->
                data.modifier_set_ids[i]
            }
            viewModel.modifierSet(intArray).observe(requireActivity(), {
                if (it.data != null && it.data.isNotEmpty()) {
                    rvModifierSets.visibility = View.VISIBLE
                    it.data.let { it1 -> adapter.add(it1) }

                    if (!isItemClick) {
//                        adapter.setData(data.modifiers)
                    }

                } else rvModifierSets.visibility = View.GONE

            })
        }
        if (data.variationsAttributes.isNotEmpty()) {
            rvVariationList.layoutManager = GridLayoutManager(activity, 3);
            variationAdapter = VariationDashboardListAdapter()
            rvVariationList.adapter = variationAdapter
            viewModel.getItemsbyId(data.itemId).observe(viewLifecycleOwner, {

                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            it.data?.let {
                                rvVariationList.visibility = View.VISIBLE
                                txtItemName.text = it.name + ":-  Choose One"
                                variationAdapter.addVariations(it.variationsAttributes)
//                                showPriceTitle(
//                                    variationsAttribute = null,
//                                    variationAdapter,
//                                    data,
//                                    txtTitle,
//                                    isItemClick
//                                )

                            }

                        }
                        Status.ERROR -> {
                            rvVariationList.visibility = View.GONE
                        }
                        Status.LOADING -> {
                            rvVariationList.visibility = View.GONE
                        }
                    }
                }


            })

        } else {
            rvVariationList.visibility = View.GONE
            txtItemName.visibility = View.GONE
//            showPriceTitle(
//                variationsAttribute = null,
//                variationAdapter,
//                data,
//                txtTitle,
//                isItemClick
//            )
        }

        edtNote.setText(data.note)



        txtQty.setText(qty.toString())

        variationAdapter?.showVariationPriceClick = { it: VariationsAttribute ->
//            showPriceTitle(it, variationAdapter = null, data, txtTitle, isItemClick)
        }



        imgClose.setOnClickListener {
            dialog.dismiss()
        }
        txtSave.setOnClickListener {


            val variationList = ArrayList<VariationsAttribute>()
            if (data.variationsAttributes.isNotEmpty()) {
                val variation = variationAdapter?.getItem()!!
                variationList.add(variation)
                data.name = data.name.substringBefore(" (") + " (" + variation.name + ")"
                data.variationsAttributes = variationList
            }


            if (minMaxValidationCheck(adapter)) {
                dialog.dismiss()

                data.note = edtNote.text.toString().trim()
                data.itemQuantity = txtQty.text.toString().toInt()
                if (!isItemClick) {
                    data.discountPrice = (discountPrice * txtQty.text.toString().toInt())
                }


                val modifiers = adapter?.getSelectedModifiers()
                if (modifiers != null) {
                    modifiers.forEach {
                        it.itemQuantity = data.itemQuantity
                    }
                    data.modifiers = modifiers
                }

                if (cartList.isEmpty()) {
                    viewModel.setServiceCharges(serviceChargesList)
                }

                if (isItemClick) {

                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition =
                            dineInCartAdapter.getHeaderPosition()

//                        viewModel.newCartLogicModifier(cartList, data, ADD, false, dineInList = dineInList)
                    } else {
//                        viewModel.newCartLogicModifier(cartList, data, ADD, false)
                    }
                } else
                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition = headerPosition
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
//                        viewModel.newCartLogicModifier(cartList, data, UPDATE, false, dineInList = dineInList)

                    } else {

                        //  viewModel.cartLogic(cartList, data, UPDATE)
                    }
            } else {
                AlertUtils.showCustomAlert(
                    binding.root.context,
                    binding.root.context.getString(R.string.you_can_add)
                )

            }
        }

        llPlus.setOnClickListener {
            qty += 1
            txtQty.setText(qty.toString())

            if (data.variationsAttributes.isNotEmpty()) {

                val stockQty = variationAdapter?.getItem()?.stockQty

                if (stockQty?.isNotEmpty() == true) {

                    if (stockQty.toInt() >= qty) {
                        txtQty.setText(qty.toString())
                    } else {
                        qty -= 1
                        stockValidationAlert(qty, txtQty)
                    }

                } else {
                    qty -= 1
                    stockValidationAlert(qty, txtQty)
                }

            } else {

                if (data.isManualSales) {
                    txtQty.setText(qty.toString())
                } else {

                    if (data.quantity >= qty) {
                        txtQty.setText(qty.toString())
                    } else {
                        qty -= 1
                        stockValidationAlert(qty, txtQty)
                    }
                }
            }

        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            txtQty.setText(qty.toString())


        }
        btnRemove.setOnClickListener {

            cartList[0].orderType = DINE_IN
//            viewModel.newCartLogicModifier(
//                cartList,
//                data,
//                DELETE, false,
//                dineInList = dineInCartAdapter.getList()
//            )

            dialog.dismiss()
        }
        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

//                        data.discountPrice = calculateDiscountPercentage(
//                            totalPrice(data),
//                            result.percentage
//                        )
                        discountPrice = data.discountPrice / data.itemQuantity
                        data.discountId = result.id
                        data.discountType = result.discountType
                        data.isManualSales = false
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
//                            (totalPrice(data) - data.discountPrice)
                        )

                    } else if (data.price > result.percentage) {

                        data.discountPrice = result.percentage
                        data.discountId = 0
                        data.discountType = result.discountType
                        data.isManualSales = false
                        discountPrice = data.discountPrice / data.itemQuantity

                        //viewModel.cartLogic(cartList, data, Constants.UPDATE)
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
//                            (totalPrice(data) - data.discountPrice)
                        )
                    }

                } else {
                    data.discountPrice = 0.0
                    data.discountType = ""
                    data.isManualSales = false
                    data.discountId = 0
                    discountPrice = data.discountPrice
                }

            }


            val bundle = Bundle().apply {
                putBoolean("isFromDetails", true)
                putParcelable("model", data)
            }

            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_addDiscountDialog,
                bundle
            )
        }


        dialog.setCanceledOnTouchOutside(false)
        dialog.show()


    }

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {
        if (isRemoved) {
            if (cartList.get(0).dineInList?.size!! >= position) {
                val dineIn = cartList.get(0).dineInList
                dineIn?.get(position)?.customer = null
                viewModel.dineInCartUpdate(cartList, dineIn!!)
            }

        } else {

            val bundle = bundleOf("DINE_IN" to true, "position" to position)
            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_assignCustomerOrderFragment, bundle
            )
        }


    }

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbCartItem) {
        alert(
            getString(R.string.app_name),
            getString(R.string.delete_item_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here
                cartList.get(0).orderType = DINE_IN

                /*viewModel.newCartLogicModifier(
                    cartList,
                    data,
                    DELETE, false,
                    dineInList = dineInCartAdapter.getList()
                )*/


            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff heref
            }
        }

    }

    override fun onRemoveGuest(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onTabSelected(pos: Int) {

        val listCategories = categoryItemAdapter1?.list

        listCategories?.clear()
        listCategories?.add(
            0,
            TbItem()
        )
        categoryList1[pos].inventoryLists?.filter {
            it!!.isHide
        }?.let { it1 ->
            listCategories?.addAll(it1)
        }
        listCategories?.let { categoryItemAdapter1?.addAll(it) }

    }

    private fun navigateDineInOrder() {
        viewModel._Basedata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {


                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", baseResponse.order.totalAmount)
                    bundle.putParcelable("cartList", cartList[0])
                    bundle.putParcelable("dineInList", baseResponse)
                    bundle.putBoolean("isGuestPaid", false)
                    bundle.putInt("orderId", baseResponse.order.id)


                    prefProvider.setValue(Constants.ORDER_TYPE, "")
                    prefProvider.setValue(Constants.CUSTOMER_NAME, "")
                    prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                    viewModel.deleteCart()
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_dineInOrderTable,
                        bundle
                    )
                }
            }
        })
    }

    private fun tableStatusCheck() {
        viewModel.tableCheck.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    status
                ) { _, _ ->


                }

            }
        })
    }

    private fun tableStatusSucess() {
        viewModel.tableCheckSuccess.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->

                viewModel.deleteCart()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }
                binding.layoutCart.txtSave.text = getString(R.string.save)
                clearCustomer()
                hideOrderType()
                hideOrderMenu()
                clearUpdateFlag()


            }
        })

    }


    private fun dineInUpdateOrder() {


        viewModel.updateOrder.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                viewModel.deleteCart()
                isOrderUpdate = false

                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }
                binding.layoutCart.txtSave.text = getString(R.string.save)
                clearCustomer()
                hideOrderType()
                hideOrderMenu()
                val bundle = Bundle()
                bundle.putParcelable("cartList", cartList[0])
                bundle.putBoolean("isGuestPaid", false)

                cartList[0].orderId?.let { it1 -> bundle.putInt("orderId", it1) }


                findNavController().navigate(
                    R.id.action_dashboardCategoryNew_to_dineInOrderTable,
                    bundle
                )

/* Old code
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    it.toString()
                ) { _, _ ->

                    viewModel.deleteCart()
                    isOrderUpdate = false

                    if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(ORDER_TYPE, "")
                    }
                    binding.layoutCart.txtSave.text = getString(R.string.save)
                    clearCustomer()
                    hideOrderType()
                    hideOrderMenu()
                    val bundle = Bundle()
                    bundle.putParcelable("cartList", cartList[0])
                    bundle.putBoolean("isGuestPaid", false)

                    cartList[0].orderId?.let { it1 -> bundle.putInt("orderId", it1) }


                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_dineInOrderTable,
                        bundle
                    )


                }


                //  dineInTableAdapter.updateStatus(clickedPos, isFireAll)


                // orderId?.let { it1 -> viewModel.apiCallOrderDetails(it1) }
*/
            }
        })

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it

            }
        })
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner, {
            if (it != null) {
                customerSettingModel = it


            }

        })

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        createOrderResponse: CreateOrderResponse
    ) {
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                /* printer.setStatusChangeEventCallback(this)
                 printer.setBatteryStatusChangeEventCallback(this)*/
            }

            val enabled = Print.TRUE

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
                ProgressUtils.dismissProgressDialog()
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)

            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type, createOrderResponse.data)

            }

        } else {
            LogUtil.logE(TAG, "PrinterIsNotNull:")
            ProgressUtils.dismissProgressDialog()
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
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

            builder = Builder(pname, PrinterClass.language, requireActivity())

            if (kitchenSettingModel.showOrderType) {


                builder.addFeedLine(0)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
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
            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, receiptModel?.order?.deliveryType.toString())

            /*}*/


            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "OrderID:" + receiptModel?.order?.id,
                    "",
                    33
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )


            builder.addText(
                padLine(
                    "ReceiptID:" + receiptModel?.order?.offlineId,
                    "",
                    33
                )
            )
            if (kitchenSettingModel.showTeamMember) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
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
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(requireContext(),receiptModel?.order?.createdAt.toString()),
                    "",
                    33
                )
            )

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            addHorizontalKitchenLine(builder)

            receiptModel?.order?.orderItems?.let { addOrdersForKitchen(builder, it) }

            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)
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
                builder.addText("Order Note")

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
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
                    builder.addTextSize(1, 1)
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
                    builder.addTextSize(1, 1)
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
                        builder.addTextSize(1, 1)
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
                            builder.addTextSize(1, 1)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber)
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

                        if (receiptModel?.order?.orderType.trim().lowercase() == "Open Order".trim()
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
                            builder.addTextSize(1, 1)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.SEND_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()
                ProgressUtils.dismissProgressDialog()
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
                /*PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
               */
            }


        } catch (e: Exception) {
            e.printStackTrace()
            ProgressUtils.dismissProgressDialog()
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
        }

    }

    private fun clearUpdateFlag() {
        isOrderUpdate = false
        prefProvider.setValueboolean(IS_ORDER_UPDATE, value = false)
        requireArguments().remove("update")

    }

    private fun refreshItemCalculation() {
        viewModel.itemCalculation(
            cartList,
            binding.layoutCart.txtTotalAmount,
            requireContext()
        )
    }

    private fun refreshOrderTypeLabel() {
        //set order type label
        var label = prefProvider.getValue(ORDER_TYPE, "").toString()
        if (label.equals(OPEN_ORDER, true)) {
            label = OPEN_ORDER
        }
        LogUtil.logE(TAG, "OrderType Label : $label")
        binding.layoutCart.txtOrderType.text = label
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
            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                if (cartList.isNotEmpty()) {
                    cartList[0].orderType = DINE_IN
                }

                val dineInList = dineInCartAdapter.getList()
                dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                viewModel.newCartLogicModifier(cartList, item, ADD, false, dineInList = dineInList)


            } else {
                //check is_edited flag
                //makeItemEdited(item)

                viewModel.newCartLogicModifier(cartList, item, ADD, false)
            }
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


    override fun scannerBarcodeEvent(barcodeData: ByteArray?, barcodeType: Int, scannerID: Int) {
        LogUtil.logE(TAG, "scannerBarcodeEvent: ${barcodeData?.let { String(it) }}")

        //Check product code in db
        val productCode = barcodeData?.let { String(it) }

        if (productCode.isNullOrEmpty()) {
            viewModel.showErrorMessage("Product code is not available !!")
            return
        }
        productCode?.let { viewModel.getItemByProductCode(it) }
        try {

            viewModel.getItemByProductCode(productCode ?: "")?.observe(viewLifecycleOwner, {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            if (resource.data != null) {
                                //data found. | Add in cart
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                                    //add item in the cart
                                    if (prefProvider.getValue(ORDER_TYPE, "").trim() != "") {
                                        addItemInCartThroughBarcode(resource.data)
                                    } else {
                                        orderTypeDialog()
                                    }
                                }
                            } else {
                                //data not found. Create New Item
                                if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                                    val bundle = Bundle()
                                    bundle.putString("productCode", productCode ?: "")
                                    findNavController().navigate(
                                        R.id.action_dashboardCategoryNew_to_createItem,
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
            })
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

    private fun observeQueueCreate() {
        viewModelPayment.queueStartSaveOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)
            }
        }
    }

    private fun queuePrinterObserver() {
        viewModelPayment.QueueCreateSaveOrder.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let {
               /* binding.layoutCart.txtSave.text = getString(R.string.save)
                viewModel.deleteCart()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }


                clearCustomer()
                hideOrderType()
                //getKitchenPrinters(it)
                clearUpdateFlag()
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)*/


            }
        }
    }

    private fun createQueuePrinter(createOrder: CreateOrderResponse) {
        val listPrinter: List<Int> = listOf()
        val orderRequest = cartList?.let {

            viewModelPayment.createOrderRequest(
                it[0],
                viewModel.subTotalPrice,
                viewModel.totalPrice,
                viewModel.totalServiceCharge,
                viewModel.totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                future_delivery_date,
                future_delivery_date,
                false,
                viewModel.totalDiscount,
                0.0,
                0,
                null,
                0.0,
                false,
                "Cash", cashDiscountType
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
}