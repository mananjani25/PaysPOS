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
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
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
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private val TAG = "DashboardCategoryBold"
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    var isupdate = false
    var orderDiscount = 0.0
    var dineInResult: Bundle? = null
    var resultData: TbCustomer? = null
    var cashDiscountType = ""
    lateinit var cashDiscountModel: CashDiscountModel
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()

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
        resultListener()
        addObserver()
        getDineInData()

        getLoyaltyPrograms()
        getServiceCharges()
        syncData()
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun getLoyaltyPrograms() {
        Log.e("Loyalty", "getLoyaltyPrograms called..")
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
            if (result != null && viewModel.totalPrice != 0.0) {
                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice
                val price = discountApplyPrice - orderDiscount

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
            val singleItem = bundle.getParcelable<TbItem>("item")

            singleItem?.note = note.toString()
            singleItem?.let { viewModel.cartLogic(cartList, it, Constants.UPDATE, false) }
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
        if (arguments != null) {
            isupdate = arguments?.getBoolean("update")!!
        }

        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        val customer = prefProvider.getCustomerData()
        customer?.let {
            viewModel.selectedCustomer = customer
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

        loadCartFragment(CartFragment(this))
        loadCategoryFragment(CategoryFragment(this))
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
        }
        frag.arguments = result
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
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
            loadCategoryFragment(CategoryFragment(this))
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

            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_manualSalesNew)
        }


    }


    override fun onItemSelected(item: TbItem) {
        if (item.modifiers.isNotEmpty() || item.variationsAttributes.isNotEmpty()) {
            val fragment = AddItemFragment.newInstance(item, this, cartList, false)
            loadCategoryFragment(fragment)
        } else {

            if (cartList.isEmpty()) {
                viewModel.createCart(cartList)
            }
            item.itemQuantity = 1
            Log.e(TAG,"ItemClickedOrderType  ${prefProvider.getValue(ORDER_TYPE, TAKEOUT)}")
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                var dineInList = cartList[0].dineInList
                dineInList!![0]?.selectedPosition = viewModel.dineInHeaderPosition
                viewModel.cartLogic(cartList, item, Constants.ADD, false, dineInList = dineInList)
            } else {

                viewModel.cartLogic(cartList, item, Constants.ADD, false)
            }
        }
    }


    override fun onCancelItemSelected() {
        loadCategoryFragment(CategoryFragment(this))

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

        viewModel.orderTypes().observe(requireActivity()) {
            if (it.data != null) {
                ordertypelist = it.data as ArrayList<TbOrderType>
                viewModel.setOrderTypeList(ordertypelist)
            }
        }

        viewModelPayment.QueueCreateSaveOrder.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let {
                viewModel.deleteCart()
                clearCustomer()
                prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

            }
        }
    }

    private fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)

    }

    private fun getServiceCharges() {

        serviceChargesObserve = Observer {

            if (it.status == Status.SUCCESS) {
                Log.e(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                serviceChargesList = it.data
                viewModel.serviceChargesList = it.data ?: arrayListOf()

            }

        }

        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }


    override fun onItemUpdate(item: TbItem) {
        Log.e(TAG, "dashboardPosItem:  ${Gson().toJson(item)}")
        val frag: Fragment = AddItemFragment.newInstance(item, this, cartList, true)
        loadCategoryFragment(frag)
    }


    private fun getDineInData() {

        if (arguments?.getBoolean("isFromDineIn") == true) {

            getDineInCartList()
        }


    }

    private fun getDineInCartList() {
        val numOfGuest: Int by lazy {
            requireArguments().getInt("numberOfGuest")
        }

        dineInFloorTableModel = arguments?.getParcelable("floorplan")
        Log.e(TAG, "dineInFloorTableModel:  ${Gson().toJson(dineInFloorTableModel)}")


        var orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? =
            arguments?.getParcelable("tableDetails")
        Log.e(TAG, "orderFloorDetails:  ${Gson().toJson(orderDEtails)}")
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

        cartList.get(0).orderType = Constants.DINE_IN
        viewModel.cartLogic(cartList, null, Constants.ADD, false, dineInList = dineInList)


    }
}