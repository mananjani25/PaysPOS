package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardCategoryBoldPOS() : Fragment(), ItemListner, ItemClickListner {
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentDashboardCategoryBoldPosBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private var orderTypeObserver: Observer<Resource<List<TbOrderType>>>? = null
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private val TAG = "DashboardCategoryBold"
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    var isupdate = false
    var orderDiscount = 0.0
    var dineInResult: Bundle? = null
    var resultData: TbCustomer? = null
    var cashDiscountType = ""
    lateinit var cashDiscountModel: CashDiscountModel
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()

    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int? = null

    @Inject
    lateinit var prefProvider: PrefProvider


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        getOrderTypes()
        observeSaveOrder()
        getKitchenReceiptSettings()
        addObserver()
        getServiceCharges()
        resultListener()

        dineInUpdateOrder()
        navigateDineInOrder()
        getLoyaltyPrograms()
        syncData()
        checkDineInEditOrder()

        binding.lifecycleOwner = this
        return binding.root
    }

    private fun getLoyaltyPrograms() {
        viewModel.activeLoyaltyProgram = prefProvider.getActiveLoyaltyData()
        viewModel.activeLoyaltyProgramLiveData.observe(requireActivity()) {
            if (it.status == Status.SUCCESS && it.data != null) {
                Log.e("Loyalty", "getLoyaltyPrograms fetched..")
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            }
        }
    }

    private fun resultListener() {

        setFragmentResultListener("request_key_customer") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                Log.e(TAG, "gotBundlebundle:  ${Gson().toJson(bundle)}")
                setUpCustomer(result, bundle)
            }
        }

        setFragmentResultListener("request_key_discount_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            val value = bundle.getDouble("value")
            if (result != null && viewModel.totalPrice != 0.0) {
                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice
                val price = discountApplyPrice - orderDiscount

                if (cartList.isNotEmpty()) {
                    cartList[0].discountPrice = orderDiscount
                    cartList[0].discountSelectdValue = value
                    cartList[0].discountType = result.discountType
                    if (result.id != -1) {
                        cartList[0].discountId = result.id
                    }
                    viewModel.addCart(cartList[0])
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
                        item.isManualSales = false
                        viewModel.cartLogic(cartList, item, Constants.UPDATE, false)

                    }
                    "Amount" -> {

                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType
                        item?.isManualSales = false

                        viewModel.cartLogic(cartList, item, Constants.UPDATE, false)
                    }
                    else -> {
                        item?.discountPrice = result.percentage
                        item?.discountId = 0
                        item?.discountType = result.discountType
                        item?.isManualSales = false

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
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()

        isupdate = requireArguments().getBoolean("update")

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
                    Log.e(TAG, "assignResult:  ${Gson().toJson(result)}")
                    val dineInList = cartList.get(0).dineInList
                    Log.e(TAG, "getdineInListSize:  ${dineInList?.size}")

                    if (dineInList?.isNotEmpty() == true) {

                        var position = bundle.getInt("position")
                        Log.e(TAG, "getCustomerAssignPos:  ${position}")

                        if (dineInList.size >= position && position != 0) {


                            dineInList.get(position).customer = result

                            Log.e(TAG, "UpdateCustomerPostition ${position}")
                            Log.e(
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
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
        } else {
            loadCartFragment(CartFragment(this, this))
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }
        binding.layoutHeader.txtUserName.text =
            prefProvider.getValue(EMPLOYEE_NAME, "")


    }


    private fun syncData() {

        val sync = prefProvider.getValueboolean(Constants.SYNC_DATA, false)
        if (!sync)
            viewModel.syncInventoryModule()
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
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_transactionFragment)

        }
        binding.layoutHeader.txtDineIn.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_dineInFragment)
        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_menuFragment)

        }
        binding.layoutHeader.txtOpenOrder.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_passcode,
                bundle
            )
        }
        binding.layoutHeader.ivLock.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_reportEODFragment)
        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            binding.layoutHeaderCheckout.rlRoot.visibility = View.GONE
            binding.layoutHeader.rlRoot.visibility = View.VISIBLE
            loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))
        }

        binding.layoutHeader.imgSync.setOnClickListener {
            viewModel.syncInventoryModule()
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
            //loadKeyPadFragment(KeyPadManualSaleFragment())
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


    override fun onItemSelected(item: TbItem) {
        Log.e(TAG, "getitem:  ${Gson().toJson(item)}")
        Log.e(TAG, "OrderTYpe:  ${prefProvider.getValue(ORDER_TYPE, TAKEOUT)}")
        Log.e(TAG, "dineInHeaderPosition  ${viewModel.dineInHeaderPosition}")
        Log.e(
            TAG,
            "dineInHeaderdineInSelectedItemHeaderPos  ${viewModel.dineInSelectedItemHeaderPos}"
        )

        if (item.modifier_set_ids.isNotEmpty() || item.variationsAttributes.isNotEmpty()) {
            val fragment = AddItemFragment.newInstance(item, this, cartList, false)
            loadCategoryFragment(fragment)
        } else {

            Log.e(TAG, "cartListItemAddSize: ${cartList.size}")
            if (cartList.isEmpty()) {
                viewModel.createCart(cartList)
            }
            item.itemQuantity = 1
            if (cartList.size > 0) {

                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                    Log.e(TAG,"dineInCartListData:  ${Gson().toJson(cartList[0].dineInList)}")
                    if (cartList[0].dineInList!!.isNotEmpty()) {
                        var dineInList = cartList[0].dineInList
                        dineInList!![0]?.selectedPosition = viewModel.dineInHeaderPosition
                        Log.e("Dinerrer", "Dinerrer")
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


    override fun onCancelItemSelected() {
        Log.e(TAG, "dineInHeaderPosition  ${viewModel.dineInHeaderPosition}")
        Log.e(
            TAG,
            "dineInHeaderdineInSelectedItemHeaderPos  ${viewModel.dineInSelectedItemHeaderPos}"
        )
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN){

        }
        loadCategoryFragment(CategoryFragment(this, binding.layoutHeader.edtSearch))

    }

    override fun onCategorySelected(item: TbItem) {

    }

    private fun addObserver() {
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
                serviceChargesList = it.data
                viewModel.serviceChargesList = it.data ?: arrayListOf()
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
                getDineInData()
            }

        }
    }


    override fun onItemUpdate(item: TbItem) {
        Log.e(TAG, "dashboardPosItem:  ${Gson().toJson(item)}")
        val frag: Fragment = AddItemFragment.newInstance(item, this, cartList, true)
        loadCategoryFragment(frag)
    }

    override fun onDineInOrderCleared() {
        arguments?.clear()
        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_self)
        //   findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_self)
    }


    private fun getDineInData() {
        if (arguments?.getBoolean("isFromDineIn") == true) {
            Log.e(TAG, "isFromDineInTrue")
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
        if (orderFloorDetails.id == null) {
            orderFloorDetails.apply {
                id = dineInFloorTableModel?.id
                chairCount = dineInFloorTableModel?.chairCount
                floorPlanId = dineInFloorTableModel?.floorPlanId
                tableName = dineInFloorTableModel?.tableName.toString()
                status = dineInFloorTableModel?.status.toString()

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
                    floorPlanTable = orderFloorDetails

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

                viewModel.cartLogic(cartList, null, Constants.ADD, false, dineInList = dineInList)
                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0

            }


        }
    }

    private fun dineInUpdateOrder() {


        viewModel.updateOrder.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {


                val bundle = Bundle()
                bundle.putParcelable("cartList", cartList[0])
                bundle.putBoolean("isGuestPaid", false)
                cartList[0].orderId?.let { it1 -> bundle.putInt("orderId", it1) }
                prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                Log.e(TAG, "deleteCartDineIn")
                viewModel.deleteCart()
                clearCustomer()




                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_dineInOrderTable,
                    bundle
                )

            }
        })

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
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                ProgressUtils.dismissProgressDialog()
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type, createOrderResponse.data)

            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
            ProgressUtils.dismissProgressDialog()
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
        }

    }

    private fun observeSaveOrder() {

        viewModelPayment.QueueStart.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { it ->
                // AlertUtils.showCustomAlert(requireActivity(), it.message)
                viewModel.deleteCart()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                }

                clearCustomer()
                getKitchenPrinters(it)


            }
        }

    }

    private fun getKitchenPrinters(createOrderResponse: CreateOrderResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e(TAG, "getKitchenPrinterList:  ${Gson().toJson(it.data)}")
                    ProgressUtils.dismissProgressDialog()
                    ProgressUtils.showProgressDialog(requireActivity())

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
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
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


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
                /*PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
               */
            }


        } catch (e: Exception) {
            e.printStackTrace()
            ProgressUtils.dismissProgressDialog()
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
        }

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it

            }
        })
    }


}