package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.ViewGroup
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.CategorySearchData
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.BUNDLE_ORDER_ID
import com.android.pos.data.remote.Constants.BUNDLE_ORDER_OFFLINE_ID
import com.android.pos.data.remote.Constants.BUNDLE_PAYMENT_ID
import com.android.pos.data.remote.Constants.BUNDLE_PAYMENT_OFFLINE_ID
import com.android.pos.data.remote.Constants.CUSTOMER_ID
import com.android.pos.data.remote.Constants.CUSTOMER_NAME
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_LIST_EDIT
import com.android.pos.data.remote.Constants.DINE_IN_STATUS
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.HORIZONTAL
import com.android.pos.data.remote.Constants.IS_ORDER_UPDATE
import com.android.pos.data.remote.Constants.MANUALSALE
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.PERCENTAGE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.remote.Constants.VERTICAL
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.SwipeHelper
import com.android.pos.ui.adapter.*
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DashboardCategoryNew : Fragment(), CategoryItemAdapter1.CategoryItemList, MyCallback,
    DineInAdapter.DineInCallback, CategoryTabAdapter1.TabListner,
    ItemCallback, View.OnClickListener {

    private var orderDiscount: Double = 0.0
    private var categoryItemAdapter1: CategoryItemAdapter1? = null
    private var categoryTabAdapter1: CategoryTabAdapter1? = null
    private var clickManualSales: Boolean = false
    private var customerUpdate: Boolean = false
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var isOrderUpdate: Boolean = false
    private var future_delivery_time: String = ""
    private var popupWindow: PopupWindow? = null
    private var orderType: TbOrderType? = null
    private var future_delivery_date: String = ""
    private var assignCustomer: TbCustomer? = null
    private var serviceChargesList: List<TbServiceCharge>? = null
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
    private lateinit var dineInCartAdapter: DineInAdapter

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // prefProvider.setValueboolean(IS_CLOCKOUT, false)
        binding = FragmentDashboardCategoryNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        getDeviceId()

        isOrderUpdate = requireArguments().getBoolean("update")
        if (isOrderUpdate) {
            Log.e(TAG, "bundle isOrderUpdate : $isOrderUpdate")
            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()

            //save pref
            prefProvider.setValueboolean(IS_ORDER_UPDATE, value = true)
            prefProvider.setValueInt(BUNDLE_ORDER_ID, value = orderId ?: 0)
            prefProvider.setValueInt(BUNDLE_PAYMENT_ID, value = paymentId ?: 0)
            prefProvider.setValue(BUNDLE_PAYMENT_OFFLINE_ID, value = paymentOfflineId)
            prefProvider.setValue(BUNDLE_ORDER_OFFLINE_ID, value = orderOfflineId)
        } else {
            //check pref
            if (prefProvider.getValueboolean(IS_ORDER_UPDATE, defaultValue = false)) {
                isOrderUpdate = true
                orderId = prefProvider.getValueInt(BUNDLE_ORDER_ID, defaultValue = 0)
                paymentId = prefProvider.getValueInt(BUNDLE_PAYMENT_ID, defaultValue = 0)
                paymentOfflineId =
                    prefProvider.getValue(BUNDLE_PAYMENT_OFFLINE_ID, defaultValue = "")
                orderOfflineId = prefProvider.getValue(BUNDLE_ORDER_OFFLINE_ID, defaultValue = "")
            }
        }

        Log.e(TAG, "isOrderUpdate : $isOrderUpdate")
        if (isOrderUpdate) {
            binding.layoutCart.txtSave.text = getString(R.string.update)
            Log.e(TAG, "save 171")
        } else {
            binding.layoutCart.txtSave.text = getString(R.string.save)
            Log.e(TAG, "save 173")
        }

        navigateDineInOrder()
        dineInUpdateOrder()
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()

        binding.footer.imgClock.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.clockout_message)
            ) {
                positiveButton(getString(android.R.string.ok)) {
                    val bundle = Bundle()
                    bundle.putBoolean("isDashboard", true)
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_passcode,
                        bundle
                    )
                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeShowProgress()
        syncData()
        hideOrderType()
        setupAdapter()
        setVenueData()
        configureDrawer()
        onClick()
        getOrderTypes()
        getServiceCharges()
        setupSnackbar()
        getBackstack()
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
        binding.footer.txtEmployeeName.text = prefProvider.getValue(EMPLOYEE_NAME, "")

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
        }

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                setUpCustomer(result, bundle)
            }
        }
        setFragmentResultListener("request_key_customer_open_order") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                setUpCustomer(result, bundle)


            }
        }

        setFragmentResultListener("request_key_orderType") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbOrderType>("data")
            if (result != null) {
                chooseOrderType(result)

            }
        }
        setFragmentResultListener("request_key_discount_order") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            if (result != null) {
                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice

                val price = discountApplyPrice - orderDiscount

                MethodUtils.setPriceTextView(binding.layoutCart.txtTotalAmount, price)
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


    }

    private fun syncData() {

        val sync = prefProvider.getValueboolean(Constants.SYNC_DATA, false)
        if (!sync)
            viewModel.syncInventoryModule()
    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            var dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")
            Log.e(TAG, "dineInListEditOrder:  ${Gson().toJson(dineInList)}")
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

                prefProvider.setValue(ORDER_TYPE, DINE_IN)
                prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
                prefProvider.setValueInt(ORDER_TYPE_ID, 2)

                Log.e(TAG, "PassedDineInListSize  ${Gson().toJson(dineInList)}")
                viewModel.cartLogic(cartList, null, ADD, dineInList = dineInList)

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
        ProgressUtils.dismissProgressDialog()
    }

    private fun removeCustomerViewSet() {
        binding.layoutCart.txtCrtNewCustomer.text = "Remove Customer"
    }

    private fun setUpCustomer(
        result: TbCustomer,
        bundle: Bundle
    ) {
        prefProvider.setValue(CUSTOMER_NAME, result.first_name + " " + result.last_name)
        binding.layoutCart.txtCustomerName.text = result.first_name + " " + result.last_name
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
            prefProvider.setValueInt(ORDER_TYPE_ID, orderType!!.id)
            prefProvider.setValue(ORDER_TYPE_NAME, orderType!!.name)
            prefProvider.setValue(ORDER_TYPE, orderType!!.orderType)
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

        viewModel.orderTypes().observe(requireActivity(), {
            Log.e("ORDER_TYPE_SIZE", "${Gson().toJson(it.data)}")
            it.data?.let { it1 -> orderTypeAdapter.addAll(it1) }
        })

    }

    private fun getServiceCharges() {

        viewModel.serviceCharges.observe(requireActivity(), {
            serviceChargesList = it.data
            getCartList()
        })
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

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
        binding.layoutCart.rvCart.adapter = cartAdapter
        binding.layoutCart.rvCartDineIn.adapter = dineInCartAdapter
        // dineInCartAdapter.itemAdapter.setCallback(this)


        if (isAdded)
            viewModel.mAllWords(prefProvider.getValue(ORDER_TYPE, "").toString()).observe(
                requireActivity(), {
                    cartList = it as ArrayList<CartModel>
                    Log.e(TAG, "cartSize:  ${cartList.size}")
                    Log.e(TAG, "OrderType:  ${prefProvider.getValue(ORDER_TYPE, "").toString()}")
                    Log.e(TAG, "cartList:  ${Gson().toJson(cartList)}")
                    if (cartList.isNotEmpty()) {

                        if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                            viewModel.setServiceCharges(serviceChargesList)
                            cartList.get(0).serviceCharge = serviceChargesList
                            binding.layoutCart.rvCart.visibility = View.GONE
                            binding.layoutCart.rvCartDineIn.visibility = View.VISIBLE
                            binding.layoutCart.llPayment.visibility = View.VISIBLE
                            Log.e(TAG, "GotAddedDineIn ${cartList[0].items}")

                            var dineList: List<DineInModel>? =
                                cartList.get(0).dineInList

                           /* if (dineList != null) {

                                for (i in 0 until dineList.size) {
                                    if (dineList.get(i).items != null && dineList.get(i).items.isNotEmpty()) {
                                        var itr = dineList.get(i).items.iterator()
                                        while (itr.hasNext()) {
                                            if (itr.next().isDestroy && itr.next().isEdited) {
                                                dineList.get(i).items.remove(itr.next())
                                            }
                                        }




                                    }
                                }
                            }*/

                            if (dineList != null) {
                                Log.e(TAG, "PassesdineList: ${Gson().toJson(dineList)}")
                                dineInCartAdapter.setList(dineList.toCollection(arrayListOf()))

                            }

                            if (cartList[0].items?.isNotEmpty() == true) {

                                var dineList = dineInCartAdapter.getList()
                                cartList[0].items?.forEach {

                                    if (it.isManualSales && dineList.isNotEmpty()) {

                                        if (it?.timeStamp == null || it?.timeStamp?.lowercase() == "null".lowercase()) {
                                            it.timeStamp = viewModel.randomOfflineId()
                                        }
                                        dineList[0].items.add(it)

                                    }
                                    cartList[0].items?.toCollection(arrayListOf())?.clear()
                                    cartList[0].items = listOf()

                                }

                                viewModel.cartLogic(cartList, null, ADD, dineInList = dineList)

                            }

                            val list1 = cartList.get(0).dineInList
                            if (isAdded) {

                                setFragmentResultListener("request_key_customer_dine_in") { requestKey, bundle ->
                                    val result = bundle.getParcelable<TbCustomer>("data")
                                    if (result != null) {


                                        if (list1?.isNotEmpty() == true) {

                                            var position = bundle.getInt("position")

                                            val dineInList = list1
                                            if (dineInList.size >= position && position != 0) {

                                                dineInList.get(position).customer = result

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
                            binding.layoutCart.llPayment.visibility = View.VISIBLE

                            cartAdapter.addCart(cartList[0].items)
                        }


                        viewModel.itemCalculation(
                            cartList,
                            binding.layoutCart.txtTotalAmount

                        )

                        if (prefProvider.getValue(ORDER_TYPE, "")
                                .toString() == TAKEOUT || prefProvider.getValue(ORDER_TYPE, "")
                                .toString() == Constants.DINE_IN
                        ) {
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
                            }
                        }

                        if (prefProvider.getValueInt("ORDER_ID", -1) != -1) {
                            gotoPayment()
                        }
                    } else {
                        viewModel.itemCalculation(
                            cartList,
                            binding.layoutCart.txtTotalAmount
                        )
                        binding.layoutCart.rvCart.visibility = View.GONE
                        binding.layoutCart.llPayment.visibility = View.GONE

                    }
                }
            )
    }

    private fun hideOrderType() {
        Log.e(TAG, "HideOrderType  ${prefProvider.getValue(ORDER_TYPE, "")}")
        Log.e(TAG, "DineInSt: ${prefProvider.getValueboolean(DINE_IN_STATUS, true)}")
        if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
            binding.layoutCart.llCart.visibility = View.VISIBLE
            binding.lltakeout.visibility = View.GONE
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString() == DINE_IN) {
                Log.e(TAG, "getDineInStatus  ${prefProvider.getValueboolean(DINE_IN_STATUS, true)}")
                if ((prefProvider.getValueboolean(DINE_IN_STATUS, true)) == false) {
                    binding.layoutCart.llShowMenu.visibility = View.GONE
                    binding.layoutCart.viewDineIn.visibility = View.GONE
                    binding.layoutCart.rvCart.visibility = View.GONE
                    binding.layoutCart.rvCartDineIn.visibility = View.GONE

                    //viewModel.deleteCart()
                    //isOrderUpdate = false

                    if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(ORDER_TYPE, "")
                    }
                    hideOrderType()

                } else {

                    binding.layoutCart.txtOrderType.setText("Dine In")
                }
            } else {
                binding.layoutCart.txtOrderType.text =
                    prefProvider.getValue(ORDER_TYPE_NAME, TAKEOUT).toString()
            }
            if (prefProvider.getValue(ORDER_TYPE_NAME, TAKEOUT) == DINE_IN) {
                binding.layoutCart.llShowMenu.visibility = View.GONE
                binding.layoutCart.viewDineIn.visibility = View.VISIBLE

                binding.layoutCart.rvCart.visibility = View.GONE
                binding.layoutCart.rvCartDineIn.visibility = View.VISIBLE
                if (requireArguments().getBoolean("isFromDineIn")) {
                    getDineInCartList()
                } else if (arguments?.getBoolean("is_dine_in_edit") == true) {
                    Log.e(TAG, "is_dine_in_edit_true")
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


        for (i in 0 until categoryList1.size) {
            for (j in categoryList1[i].inventoryLists!!.indices) {
                searchList.add(
                    CategorySearchData(
                        categoryList1.get(i).inventoryLists!!.get(j)!!.itemId,
                        categoryList1.get(i).inventoryLists!!.get(j)!!.name,
                        categoryList1.get(i).inventoryLists?.get(j)?.imageUrl.toString(),
                        categoryList1.get(i).category.name,
                        categoryList1.get(i).category.id
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
        binding.layoutMenu.autoSearch.setOnItemClickListener { parent, view, position, id ->
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
            viewModel.syncInventoryModule()
        }

        binding.footer.linearMore.setOnClickListener {
            dialogPOSMenu()

        }

        binding.footer.linearTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_transactionFragment)
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

        val imgMore: ImageView = footerView.findViewById(R.id.imgMore)
        val txtMore: TextView = footerView.findViewById(R.id.txtMore)
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

        txtBusinessName.text = getString(R.string.business_name) + " :- " + prefProvider.getValue(
            Constants.BUSINESS_NAME,
            ""
        )


        linearHome.setOnClickListener {
            closeDialog(dialog)
        }
        linearCust.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_customer)
            closeDialog(dialog)
        }
        linearReports.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_reports)
            dialog.dismiss()
        }
        linearTeam.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_teamList)
            dialog.dismiss()
        }
        linearInventory.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_inventory)
            dialog.dismiss()
        }
        linearSetting.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_settings)
            dialog.dismiss()
        }

        linearOrders.setOnClickListener {

            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
            dialog.dismiss()
        }
        linearTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_transactionFragment)
            dialog.dismiss()
        }
        linearCash.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_cashLogFragment)
            dialog.dismiss()
        }


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

    private fun horizontalTabList() {
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
        binding.layoutMenu.txtKeypad.setOnClickListener {

            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                findNavController().navigate(R.id.action_dashboardCategoryNew_to_manualSales)
            } else {
                clickManualSales = true
                orderTypeDialog()
            }


        }


    }

    private fun setVenueData() {
        viewModel.venueDataLocal().observe(
            viewLifecycleOwner,
            {
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
                                            categoryList1[i].category.name,
                                            true,
                                            0
                                        )
                                    )
                                } else {
                                    tabList.add(
                                        CategoryTabModel(
                                            categoryList1[i].category.id,
                                            categoryList1[i].category.name,
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
                    }
                    Status.ERROR ->
                        ProgressUtils.dismissProgressDialog()

                    Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

                }
            })

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

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window, null)

        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)
        Log.e(TAG, "subTotalPrice:   ${viewModel.subTotalPrice - (cartList[0].discountPrice)}")

        txtSubTotal.text = "$" + String.format(
            "%.2f",
            viewModel.subTotalPrice - (cartList[0].discountPrice)
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            viewModel.totalServiceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            viewModel.totalDiscount + cartList[0].discountPrice
        )

        val total = viewModel.totalPrice - cartList[0].discountPrice

        MethodUtils.setPriceTextView(txtTotalAmount, total)

        //  txtTotalAmount.text = total.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            viewModel.totalTax
        )

//        if (popupWindow == null) {
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow!!.setBackgroundDrawable(BitmapDrawable())
        popupWindow!!.isOutsideTouchable = true


        popupWindow!!.setOnDismissListener(PopupWindow.OnDismissListener {
            //TODO do sth here on dismiss
        })
        popupWindow!!.showAtLocation(view, Gravity.TOP, 600, 650);
//        } else {
//            popupWindow!!.dismiss()
//            popupWindow = null
//        }

    }

    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list
        Log.e(TAG, "searchTabList  ${Gson().toJson(tabList)}")
        Log.e(TAG, "searchmodel  ${Gson().toJson(model)}")
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

        if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {

            if (item.modifier_set_ids.isEmpty() && item.variationsAttributes.isEmpty()) {
                item.itemQuantity = 1
                if (cartList.isEmpty()) {
                    viewModel.setServiceCharges(serviceChargesList)
                }
                if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                    //TODO bugsolve
                    if (cartList.isNotEmpty()) {
                        cartList[0].orderType = DINE_IN
                    }

                    Log.e(TAG, "HeaderPosition:  ${dineInCartAdapter.getHeaderPosition()}")
                    /*cartList.get(0).dineInList?.get(dineInCartAdapter.getHeaderPosition())?.items?.add(
                        item
                    )*/
                    val dineInList = dineInCartAdapter.getList()
                    //dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(item)
                    dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                    viewModel.cartLogic(cartList, item, ADD, dineInList = dineInList)
                    Log.e(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargesList)}")


                } else {
                    //check is_edited flag
                    makeItemEdited(item)

                    viewModel.cartLogic(cartList, item, ADD)
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
    override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {

        if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {

            ItemPopup(data, false)
        } else {
            orderTypeDialog()
        }
    }

    private fun makeItemEdited(item: TbItem) {
        if (isOrderUpdate) {
            //for open order and edit cart
            item.isEdited = true
        }
        Log.e(TAG, "isOrderUpdate $isOrderUpdate")
        Log.e(TAG, "data.isEdited ${item.isEdited}")
    }

    private fun orderTypeDialog() {

        val bundle = Bundle().apply {
            putParcelableArrayList("data", orderTypeAdapter.list)
        }
        Log.e(TAG, "currentDestination:   ${findNavController().currentDestination}")
        findNavController().navigate(
            R.id.action_dashboardCategoryNew_to_orderTypeDialog,
            bundle
        )

    }

    private fun ItemPopup(data: TbItem, isItemClick: Boolean) {
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
        var discountPrice = data.discountPrice


        var adapter: ItemModifierSetAdapter? = null
        var variationAdapter: VariationDashboardListAdapter? = null

        var qty = data.itemQuantity
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
                        adapter.setData(data.modifiers)
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
        }
        txtSave.setOnClickListener {

            /*showPriceTitle(
                variationsAttribute = null,
                variationAdapter,
                data,
                txtTitle,
                isItemClick
            )*/

            if (data.price == 0.0 && data.variationsAttributes.isNotEmpty()) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    "Please enter atleast one price of item"
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
                makeItemEdited(data)

                if (isItemClick) {

                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
                        dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                        viewModel.cartLogic(cartList, data, ADD, dineInList = dineInList)
                    } else {
                        viewModel.cartLogic(cartList, data, ADD)
                    }
                } else {
                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition = dineInCartAdapter.getHeaderPosition()
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
                        viewModel.cartLogic(cartList, data, UPDATE, dineInList = dineInList)

                    } else {

                        viewModel.cartLogic(cartList, data, UPDATE)
                    }
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


        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            txtQty.setText(qty.toString())


        }
        btnRemove.setOnClickListener {


            viewModel.cartLogic(cartList, data, DELETE)
            dialog.dismiss()
        }
        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    Log.e(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                        data.discountPrice = calculateDiscountPercentage(
                            totalPrice(data),
                            result.percentage
                        )
                        discountPrice = data.discountPrice / data.itemQuantity
                        data.discountId = result.id
                        data.discountType = result.discountType
                        data.isManualSales = false
                        Log.e(TAG, "insideDiscountmodel:  ${Gson().toJson(data)}")
                        //   viewModel.cartLogic(cartList, data, Constants.UPDATE)
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
                            (totalPrice(data) - data.discountPrice)
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
                            (totalPrice(data) - data.discountPrice)
                        )
                    } else {
                        /*  data.discountPrice = 0.0
                          data.discountType = ""
                          data.isManualSales = false
                          data.discountId = 0
                          discountPrice = data.discountPrice*/

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
        Log.e(TAG, "txtTitle text >> ${txtTitle.text}")
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

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModelPayment.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModel.logout.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    prefProvider.setClear()
                    viewModel.clearTable()
                    prefProvider.setValue(Constants.AUTH_TOKEN, "")
                    findNavController().navigate(R.id.action_global_login)


                }
            }
        })
    }

    private fun observeSaveOrder() {

        viewModelPayment.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { it ->
                AlertUtils.showCustomAlert(requireActivity(), it.message)
                binding.layoutCart.txtSave.text = getString(R.string.save)
                Log.e(TAG, "save 1794")
                clearCustomer()
                hideOrderType()
                getKitchenPrinters(it)


            }
        })

    }

    private fun getKitchenPrinters(createOrderResponse: CreateOrderResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = it.data
                        for (i in 0 until kitchenPrinterList.size) {

                            initKitchenPrinter(kitchenPrinterList.get(i), Constants.KITCHEN,createOrderResponse)
                        }
                    }

                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()

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
        Log.e(TAG, "CurrentDestination:   ${findNavController().currentDestination}")

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

                        if (prefProvider.getValue(ORDER_TYPE, "").toString() == DINE_IN) {

                            dineInCartAdapter.getList().get(1).floorPlanTable?.id?.let {
                                viewModel.getTableStatus(
                                    it, "Available"
                                )
                            }
                            viewModel.deleteCart()
                            isOrderUpdate = false

                            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                                prefProvider.setValue(ORDER_TYPE, "")
                            }
                            binding.layoutCart.txtSave.text = getString(R.string.save)
                            Log.e(TAG, "save 1923")
                            clearCustomer()
                            hideOrderType()
                            hideOrderMenu()
                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                            requireArguments().remove("update")

                        } else {
                            viewModel.deleteCart()
                            Log.e(TAG, "isOrderUpdate 1777 $isOrderUpdate")
                            isOrderUpdate = false
                            prefProvider.setValueboolean(IS_ORDER_UPDATE, false)

                            if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                                prefProvider.setValue(ORDER_TYPE, "")
                            }
                            binding.layoutCart.txtSave.text = getString(R.string.save)
                            Log.e(TAG, "save 1939")
                            clearCustomer()
                            hideOrderType()
                            hideOrderMenu()
                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                            requireArguments().remove("update")
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

                    val cartList = cartList[0]

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

                    val request = viewModelPayment.createOrderRequest(
                        cartList,
                        viewModel.subTotalPrice,
                        viewModel.totalPrice - cartList.discountPrice,
                        viewModel.totalServiceCharge,
                        viewModel.totalTax,
                        OPEN_ORDER,
                        future_delivery_date,
                        future_delivery_time,
                        false,
                        viewModel.totalDiscount + cartList.discountPrice,
                        0.00,
                        -1
                    )
                    viewModelPayment.saveOrder(true)
                    viewModelPayment.submit(request)
                }
//                }

            }

            R.id.btnPay -> {

//                prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT, "")
//                prefProvider.setValueInt(Constants.SPLIT_NO, -1)
//                prefProvider.setValue(Constants.SPLIT_PAY_TYPE, "")
//                prefProvider.setValueInt("ORDER_ID", -1)

                gotoPayment()
            }
            R.id.llCartMenu -> {

            }
        }
    }

    private fun gotoPayment() {
        if (cartList.isNotEmpty()) {
            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {

                if (cartList.isNotEmpty()) {
                    var request = viewModel.updateOrder(cartList[0])

                    Log.e(TAG, "DineinSenOrder  ${cartList[0].orderId}")
                    prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                    prefProvider.setValueboolean(DINE_IN_LIST_EDIT, false)
                    prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                    cartList[0].orderId?.let { viewModel.updateOrderCall(it, request) }


                }
            } else {


                val bundle = Bundle()
                bundle.putDouble(
                    "totalPrice",
                    viewModel.totalPrice - cartList[0].discountPrice
                )
                bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                bundle.putDouble("totalTax", viewModel.totalTax)
                bundle.putDouble(
                    "totalDiscount",
                    viewModel.totalDiscount + cartList[0].discountPrice
                )
                bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                bundle.putString("future_delivery_date", future_delivery_date)
                bundle.putString("future_delivery_time", future_delivery_time)
                cartList[0].customer = assignCustomer
                bundle.putParcelable("cartList", cartList[0])

                if (isOrderUpdate) {
                    bundle.putBoolean("update", true)
                    orderId?.let { bundle.putInt("orderId", it) }
                    paymentId?.let { bundle.putInt("paymentId", it) }
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                }

                if (prefProvider.getValue(ORDER_TYPE, "").toString() == DINE_IN) {

                    createDineInRequest()


                } else {

                    lifecycleScope.launchWhenStarted {
                        if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {

                            findNavController().navigate(
                                R.id.action_dashboardCategoryNew_to_paymentFragment,
                                bundle
                            )
                        }
                    }
                }

            }
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
        prefProvider.setValue(CUSTOMER_NAME, "")
        prefProvider.setValueInt(CUSTOMER_ID, -1)
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
                            cartAdapter.removeItem(position)
                            viewModel.cartLogic(cartList, item, DELETE)
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
                        singleItem?.let { viewModel.cartLogic(cartList, it, UPDATE) }
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
                                        totalPrice(data),
                                        result.percentage
                                    )

                                    data.discountId = result.id
                                    data.discountType = result.discountType
                                    data.isManualSales = false

                                    viewModel.cartLogic(cartList, data, UPDATE)


                                }
                                "" -> {

                                    data.discountPrice = result.percentage
                                    data.discountId = 0
                                    data.discountType = result.discountType
                                    data.isManualSales = false
                                    // discountPrice = data.discountPrice

                                    viewModel.cartLogic(cartList, data, UPDATE)

                                }
                            }

                        } else {
                            data.discountPrice = 0.0
                            data.discountType = ""
                            data.isManualSales = false
                            data.discountId = 0
                            viewModel.cartLogic(cartList, data, UPDATE)
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
        Log.e(TAG, "orderFloorDetailsAdd  ${Gson().toJson(orderFloorDetails)}")

        val dineInList: ArrayList<DineInModel> = arrayListOf()
        dineInList.add(DineInModel(0, true, 0, "Whole Table"))
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



        dineInCartAdapter.setList(dineInList)

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

        /* Log.e(TAG, "OrderId:   ${orderType!!.id}")
         Log.e(TAG, "OrderName:   ${orderType!!.name}")
         Log.e(TAG, "OrderOrderType:   ${orderType!!.orderType}")
         prefProvider.setValueInt(ORDER_TYPE_ID, orderType!!.id)
         prefProvider.setValue(ORDER_TYPE_NAME, orderType!!.name)
         prefProvider.setValue(ORDER_TYPE, orderType!!.orderType)
    */
        cartList.get(0).orderType = DINE_IN
        viewModel.cartLogic(cartList, null, ADD, dineInList = dineInList)

    }

    override fun onHeaderSelected(position: Int) {

    }

    override fun onItemSelected(headerPosition: Int, position: Int, data: TbItem) {
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
                        adapter.setData(data.modifiers)
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
            showPriceTitle(it, variationAdapter = null, data, txtTitle, isItemClick)
        }



        imgClose.setOnClickListener {
            dialog.dismiss()
        }
        txtSave.setOnClickListener {

            /*showPriceTitle(
                variationsAttribute = null,
                variationAdapter,
                data,
                txtTitle,
                isItemClick
            )*/
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

                        viewModel.cartLogic(cartList, data, ADD, dineInList = dineInList)
                    } else {
                        viewModel.cartLogic(cartList, data, ADD)
                    }
                } else
                    if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                        cartList.get(0).orderType = DINE_IN
                        val dineInList = dineInCartAdapter.getList()
                        dineInList.get(0).selectedPosition = headerPosition
                        // dineInList.get(dineInCartAdapter.getHeaderPosition()).items.add(data)
                        viewModel.cartLogic(cartList, data, UPDATE, dineInList = dineInList)

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


        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            txtQty.setText(qty.toString())


        }
        btnRemove.setOnClickListener {
            Log.e(TAG, "RemoveMayItem")

            cartList.get(0).orderType = DINE_IN

            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {


                var list = dineInCartAdapter.getList()
                Log.e(
                    TAG,
                    "DeleteItem  ${Gson().toJson(list.get(headerPosition).items.get(position))}"
                )
                var item: TbItem = list.get(headerPosition).items.get(position)
                item.isEdited = true
                item.isDestroy = true
                list.get(headerPosition).items[position] = item
                viewModel.dineInCartUpdate(cartList, list)
                /*list.get(headerPosition).*/


            } else {

                viewModel.cartLogic(
                    cartList,
                    data,
                    DELETE,
                    dineInList = dineInCartAdapter.getList()
                )
            }
            dialog.dismiss()
        }
        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    Log.e(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                        data.discountPrice = calculateDiscountPercentage(
                            totalPrice(data),
                            result.percentage
                        )
                        discountPrice = data.discountPrice / data.itemQuantity
                        data.discountId = result.id
                        data.discountType = result.discountType
                        data.isManualSales = false
                        Log.e(TAG, "insideDiscountmodel:  ${Gson().toJson(data)}")
                        //   viewModel.cartLogic(cartList, data, Constants.UPDATE)
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
                            (totalPrice(data) - data.discountPrice)
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
                            (totalPrice(data) - data.discountPrice)
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
                var dineIn = cartList.get(0).dineInList
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

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {
        Log.e(TAG, "ItemDineDeleteHeader ${position}")
        Log.e(TAG, "ItemDineDelete ${itemPosition}")

        alert(
            getString(R.string.app_name),
            getString(R.string.delete_item_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here
                cartList.get(0).orderType = DINE_IN

                viewModel.cartLogic(
                    cartList,
                    data,
                    DELETE,
                    dineInList = dineInCartAdapter.getList()
                )


            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff here
            }
        }

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


                    Log.e(TAG, "BaseResponseInDash ${Gson().toJson(baseResponse)}")
                    var bundle = Bundle()
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
                Log.e(TAG, "getstr:   $status")

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
                Log.e(TAG, "getstr:   $status")

                viewModel.deleteCart()
                Log.e(TAG, "isOrderUpdate 2560 $isOrderUpdate")
                isOrderUpdate = false
                prefProvider.setValueboolean(IS_ORDER_UPDATE, false)

                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                }
                binding.layoutCart.txtSave.text = getString(R.string.save)
                clearCustomer()
                hideOrderType()
                hideOrderMenu()


            }
        })

    }

    private fun getDeviceId() {

        val androidId: String = Settings.Secure.getString(
            requireActivity().contentResolver,
            Settings.Secure.ANDROID_ID
        )
        Log.e(TAG, "androidId:  ${androidId}")
    }


    private fun dineInUpdateOrder() {


        viewModel.updateOrder.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
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
                    Log.e(TAG, "save 2835")
                    clearCustomer()
                    hideOrderType()
                    hideOrderMenu()
                    var bundle = Bundle()
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
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type,createOrderResponse.data)

            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        receiptModel:CreateOrderResponse.Data
    ) {
        var builder: Builder? = null
        try {
            Log.e(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
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

            Log.e(
                TAG,
                "ConvertDateTime:  ${Constants.getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString())}"
            )
            builder.addText(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString()),
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

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {
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
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

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

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}