package com.pays.pos.ui.fragments.dineInNew

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.Group
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.DineinCartPaymentModel
import com.pays.pos.data.model.GuestDataModel
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ADD
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.pays.pos.data.remote.Constants.DINE_IN_DISCOUNT
import com.pays.pos.data.remote.Constants.DINE_IN_GUEST_PAYMENT_DATA
import com.pays.pos.data.remote.Constants.DINE_IN_SERVICECHARGE
import com.pays.pos.data.remote.Constants.DINE_IN_SUBTOTAL
import com.pays.pos.data.remote.Constants.DINE_IN_TAX
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE_LIST
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.GUEST_POSITION
import com.pays.pos.data.remote.Constants.IS_GUEST_PAYMNET
import com.pays.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.MERGEDANDOCCUPIED
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.pays.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.pays.pos.databinding.FragmentDineInOrderTableBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.DineInTableAdapter
import com.pays.pos.ui.fragments.checkout.CheckoutDineInPaymentViewModel
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pays.pos.ui.fragments.dineInNew.customerDisplay.CustomDisplayPays
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DineInOrderTablePays : Fragment(), DineInTableAdapter.DineInTableListner {

    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private var fireItemsList: ArrayList<TbCartItem> = arrayListOf()

    private var passSCTotal: Double = 0.0
    private var passDiscountTotal: Double = 0.0
    private val listOfMoveItemIds: ArrayList<Int> = arrayListOf()
    private var wholeTableDiscount: Double = 0.0
    private var cashDiscountGlobal: Double = 0.0
    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = listOf()
    private var toFinalAmt: Double = 0.0
    private var totalTaxAmt: Double = 0.00
    private var isFireAll: Boolean = false
    private var clickedPos: Int = 0
    private lateinit var binding: FragmentDineInOrderTableBinding
    private var cartList: CartModel? = null
    private var dineInData: CreateOrderResponse.Data? = null
    private var orderId: Int? = null
    private var order_note = ""
    private var globalOrderDiscount = 0.0
    var update_order_Discount = 0.0
    private var getOrderDetailsResponse: GetOrderDetailsResponse.Data? = null
    private val TAG = "DineInOrderTable"
    private lateinit var dineInTableAdapter: DineInTableAdapter
    private var totalPrice: Double = 0.0
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    var cashDiscount: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    var totalGuestCount = 0
    var eligibleGuestsForDivision = 0
    var divideCashDiscount = 0.0
    private var paymentAmount: Double = 0.0
    private var subTotalWT = 0.0
    private var discountsubTotalWT = 0.0
    private var subTotalDInin = 0.0
    private var WTOnlyTax = 0.0
    private var WTServiceTax = 0.0
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var serviceCharge = 0.0
    private var finalTaxAmt = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    var totalAmount = 0.0
    var totalAmtnew: Double = 0.0
    var totalAmtnewDiscount: Double = 0.0
    private var allCustomerList: ArrayList<TbCustomer> = arrayListOf()
    private var popupWindow: PopupWindow? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    var dragFrom = -1
    var dragTo = -1
    var notPayAnyAmount: Boolean = false
    var paidGuestAmount = 0
    var serviceChargeList: ArrayList<TbServiceCharge> = arrayListOf()
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    lateinit var cashDiscountModel: CashDiscountModel
    var optionType = ""
    private lateinit var pd: Dialog
    private var totalDiscountWO = 0.0
    private var clickedPosition: Int = -1

    @Inject
    lateinit var prefProvider: PrefProvider
    private val viewModel by viewModels<DineInOrderTableViewModelPays>()
    private val viewModelPayment by activityViewModels<CheckoutDineInPaymentViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in_order_table,
            container,
            false
        )
        prefProvider.setValueboolean(Constants.IS_PAYMENT_SCREEN, false)
        binding.lifecycleOwner = this



        progressDialog()
        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "")
//        getCustomerDisplay(requireContext())?.let { display ->
//            presentation = CustomDisplay(
//                display,
//                requireContext(),
//                viewLifecycleOwner,
//                dashboardViewModel,
//                passcodeViewModel,
//                viewModel
//            )
//        }
        observeShowProgress()
        setupSnackbar()
        getCustomerList()
        observeServiceCharge()
        //singleItemFireObserver()
        getCustomerPrinterList()
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()
        //observeFireAll()
        observeQueueCreated()

        observeTipsList()
        observeAddGuest()



        navigateDineInOrderNew()
        reorderedItemObserver()
        removeGuestObserver()
        wastageItemObserver()
        observeUnMergeTable()
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_guestcount",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var count: Int = bundle.getInt("count")
            addGuestToOrder(count)
        }

        //showStaticLoader()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    prefProvider.setValue(ORDER_TYPE, "")
                    prefProvider.setValue(ORDER_TYPE_NAME, "")
                    dashboardViewModel.cartModel = null
                    findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
                }

            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        return binding.root
    }

    private fun showStaticLoader() {
        if (!SunmiPrinterApi.getInstance().isConnected) {
            ProgressUtils.showProgressDialog(requireActivity())
            Handler().postDelayed({
                ProgressUtils.dismissProgressDialog()
            }, 5000)


        } else {
            ProgressUtils.showProgressDialog(requireActivity())
            Handler().postDelayed({
                ProgressUtils.dismissProgressDialog()
            }, 1000)
        }
    }


    private fun observeAddGuest() {
        viewModel.updateOrder.observe(viewLifecycleOwner) { event ->
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), event.getContentIfNotHandled().toString()
            ) { _, _ ->
                prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                viewModel.Basedata.removeObservers(viewLifecycleOwner)
                navigateDineInOrderNew()
                orderId?.let { viewModel.apiCallOrderDetails(it) }
            }
        }
    }

    private fun checkVariation(tbItem: TbCartItem, item: TbCartItem): Boolean {

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
        if (listOfDataMod.containsAll(listOfDataModSelecteItem) && listOfDataMod.size == listOfDataModSelecteItem.size) {
            if (tbItem.variationsAttributes.get(0).id == item.variationsAttributes.get(0).id) {
                isSame = true
            } else {
                isSame = false
            }
        } else {
            isSame = false
        }


        return isSame
    }

    fun checkModifierNewLogic(tbItem: TbCartItem, item: TbCartItem): Boolean {
        var isSame = false
        var listOfDataMod: ArrayList<Int> = arrayListOf()
        var tbMod: HashMap<Int, Int> = hashMapOf()
        var itemMod: HashMap<Int, Int> = hashMapOf()
        if (tbItem.modifiers.isNotEmpty()) {
            tbItem.modifiers.forEach {
                tbMod.put(it.id ?: 0, it.modifier_quantity)
                listOfDataMod.add(it.id ?: 0)
            }
        }

        var listOfDataModSelected: ArrayList<Int> = arrayListOf()
        if (item.modifiers.isNotEmpty()) {
            item.modifiers.forEach {

                itemMod.put(it.id ?: 0, it.itemQuantity)
                listOfDataModSelected.add(it.id ?: 0)

            }
        }

        if (tbItem.modifiers.isEmpty() && item.modifiers.isEmpty()) return true

        if (listOfDataMod.size == listOfDataModSelected.size) {

            isSame = true
            var selectedList: ArrayList<Boolean> = arrayListOf()

            tbMod.forEach {
                if (itemMod.containsKey(it.key) && it.value == itemMod.get(it.key)) {
                    selectedList.add(true)

                } else {
                    selectedList.add(false)
                }
            }

            if (selectedList.contains(false)) {
                isSame = false
            }
        } else {
            isSame = false
        }
        return isSame
    }

    private fun getCustomerList() {
        viewModel.customer().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                allCustomerList = it.toCollection(arrayListOf())
                if (this::presentation.isInitialized) {
                    presentation.setCustomerList(it.toCollection(arrayListOf()))
                }
            }
        }
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                kitchenSettingModel = it
                getKitchenPrinters()
            }
        }
    }

    private fun observeServiceCharge() {
        viewModel.getServiceChargeList.observe(viewLifecycleOwner) {
            if (it.data?.isNotEmpty() == true) {
                if (it.status == Status.SUCCESS) {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_DINEIN_ORDER,
                            false
                        )
                    ) {
                        LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                        serviceChargeList = arrayListOf()
                        it.data.forEach { service ->
                            if (service.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                serviceChargeList = it.data.toCollection(arrayListOf())
                                dineInTableAdapter.setSurchargeList(serviceChargeList)
                            }
                        }
                    }

                }

            }

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //getData()

        dineInTableAdapter = DineInTableAdapter()
        binding.rvItemList.adapter = dineInTableAdapter
        dineInTableAdapter.setListner(this)

        listOfMoveItemIds.clear()

        if (arguments?.getBoolean("isFromFloor") == true || arguments?.getBoolean("isGuestPaid") == true) {
            floorPlanModel = arguments?.getParcelable("floorPlan")


            if (floorPlanModel == null) {
                orderId = arguments?.getInt("orderId")

                orderId?.let { viewModel.apiCallOrderDetails(it) }

            } else {
                if (floorPlanModel?.currentOrderDetails != null) {
                    orderId = floorPlanModel?.currentOrderDetails?.orderId
                    floorPlanModel?.currentOrderDetails?.orderId?.let {
                        viewModel.apiCallOrderDetails(
                            it
                        )
                    }
                }

                // binding.txtTitle.setText("" + floorPlanModel?.tableName)
            }


        } else {

            orderId = arguments?.getInt("orderId")
            orderId?.let { viewModel.apiCallOrderDetails(it) }
            //  binding.txtTitle.setText("Order Details")

        }
        onClick()
        addItemToWastageResultListener()

    }

    private fun getData() {
        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()

    }

    private fun onClick() {

        binding.imgMergeTable.setOnClickListener {
            getOrderDetailsResponse?.floorPlanTable?.id?.let { it1 -> viewModel.unMergeTable(it1) }
        }

        binding.imgPrintAll.setOnClickListener {
            getCustomerPrinters("")

        }

        binding.llInfo1.setOnClickListener {
            showPopupWindow(it)

        }

        binding.txtFloorPlan.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dineInFragmentPays)

        }
        binding.txtAddguest.setOnClickListener {
            findNavController().navigate(
                R.id.action_dineInOrderTable_to_addguestcount
            )
        }
        binding.txtFireAll.setOnClickListener {

            checkForAutoFire(false)
            /*val list = dineInTableAdapter.getList()
            val idsStr = java.lang.StringBuilder()
            list.forEach {
                if (it.isHeader == 1) {
                    it.item?.orderItemId?.let { it1 -> idsStr.append(it1) }
                }
            }
            var fireIds = android.text.TextUtils.join(",", idsStr.toSet())
            orderId?.let { it1 ->
                isFireAll = true
                viewModel.fireItemToKitchen(
                    it1,
                    true,
                    fireIds,
                    true
                )
            }*/
        }

        binding.btnPayNew.setOnClickListener {
            try {

//                dashboardViewModel.currentCartItems = arrayListOf()
//                dashboardViewModel.deleteCartItems()

                //new Calculation for total Discount
                var listWT: ArrayList<TbCartItem> = arrayListOf()
                var list = dineInTableAdapter.getList()


                /**
                 * Adding guestDineInPosition for Each Item
                 */

                var headerPositionCounter = -1
                var k=0
                while(k < list.size) {
                    if( list[k].isHeader == 0) {
                        headerPositionCounter++

                        var innerCounter = k+1

                        while(true){
                            if (innerCounter == list.size)
                                break

                            if(list[innerCounter].isHeader == 1){
                                list[innerCounter].item?.apply{
                                    guestIndexForDineIn = headerPositionCounter
                                    employeeID = getOrderDetailsResponse?.employeeId?:0
                                    orderType = "DineIn"
                                }
                            }else
                                break

                            innerCounter++

                        }
                    }
                    k++
                }


                for (i in 0 until list.size) {
                    if (list.get(i).title.equals("Whole Table", true) && i + 1 <= list.size) {
                        if (list[i + 1].isHeader == 1) {
                            for (j in i + 1 until list.size) {
                                if (list.get(j).isHeader == 1) {
                                    listWT.add(list.get(j).item!!)
                                    dashboardViewModel.currentCartItems.add(list[j].item!!)
                                } else {
                                    break
                                }
                            }
                        }
                    }
                }

                LogUtil.logE(TAG, "listWTItems ${Gson().toJson(listWT)}")
                prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)

                var dividedOrderDiscount = 0.0
                eligibleGuestsForDivision =
                    dineInTableAdapter.getList()[0].eligibleGuestsForDivision
                totalGuestCount = eligibleGuestsForDivision
                var tmpOrderDis = 0.0
                if (paidGuestAmount > 0 && globalOrderDiscount > 0.0) {
                    LogUtil.logE(
                        "globalOrderDiscount",
                        "globalOrderDiscount  ${globalOrderDiscount}"
                    )
                    var eachGuestDiscount =
                        MethodUtils.roundOffAmountDouble(globalOrderDiscount / eligibleGuestsForDivision)

                    dividedOrderDiscount =
                        globalOrderDiscount - (eachGuestDiscount * paidGuestAmount)
                    LogUtil.logE(TAG, "DividedOrwrs ${dividedOrderDiscount}")

                    tmpOrderDis = globalOrderDiscount - (eachGuestDiscount * paidGuestAmount)


                    var wholeDisDivide =
                        MethodUtils.roundOffAmountDouble(wholeTableDiscount / eligibleGuestsForDivision)
                    if (wholeDisDivide > dividedOrderDiscount) {
                        dividedOrderDiscount = wholeDisDivide - dividedOrderDiscount
                    } else {
                        dividedOrderDiscount -= wholeDisDivide
                    }
                    totalDiscount -= wholeDisDivide

                    subTotalDInin -= tmpOrderDis
                    LogUtil.logE(
                        "dividedOrderDiscount",
                        "dividedOrderDiscount  ${dividedOrderDiscount}"
                    )
                    LogUtil.logE("WRqwrfarf", "wholeDisDivide  ${wholeDisDivide}")
                    LogUtil.logE(TAG, "subtotal :: " + subTotalDInin)
                }

                LogUtil.logE("WholeTableDis", "wholeDis  ${wholeTableDiscount}")


                LogUtil.logE("totalDiscount", "totalDiscount  ${totalDiscount}")
                if (totalDiscount > tmpOrderDis) {
                    totalDiscount -= tmpOrderDis
                }


                val adapterList = dineInTableAdapter.getList()
                var offlineId = randomOfflineId()

                cartList = getCartModel(adapterList.toCollection(arrayListOf()))
                cartList?.note = order_note
                cartList!!.taxlistDynamic = listOf()
                var temp_itemsList: ArrayList<TbCartItem> = arrayListOf()
                /*cartList?.dineInList?.forEach { dineModel ->
                    if (!dineModel.isPaid) {
                        temp_itemsList.addAll(dineModel.items)
                    }
                }*/
                temp_itemsList.addAll(dashboardViewModel.currentCartItems)


                temp_itemsList.forEach { item ->
                    cartList = taxBifurcationCalculation(
                        item,
                        cartList!!
                    )
                }


                var listreemaining: List<TaxData> = emptyList()
//                listWT.forEach { wholetableitems ->
                dashboardViewModel.currentCartItems.forEach { wholetableitems ->
                    wholetableitems.taxes?.forEachIndexed { index, taxData ->
                        var modifierPrice: Double = 0.0
                        var totaltaxtemp: Double = 0.0
                        val price =
                            (wholetableitems.price * wholetableitems.itemQuantity) - (wholetableitems.discountPrice * wholetableitems.itemQuantity)

                        wholetableitems.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice =
                            price + modifierPrice
                        totaltaxtemp += if (taxData.taxType == "Percentage") {
                            if (totalPrice < 0.0) {

                                String.format("%.2f", 0.00)
                                    .toDouble()
                            } else {
                                val itemTaxPrice =
                                    (taxData.rate * totalPrice) / 100
                                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                                MethodUtils.getTwoDecimal(itemTaxPrice)
                                /* String.format("%.2f", itemTaxPrice)
                                 .toDouble()*/
                            }

                        } else {
                            Log.d("yash", "taxCalculation: " + taxData.taxType)
                            if (totalPrice <= 0.0) {
                                String.format("%.2f", 0.00)
                                    .toDouble()
                            } else {

                                MethodUtils.getTwoDecimal(taxData.rate * wholetableitems.itemQuantity)
                                /*  String.format("%.2f", taxData.rate * wholetableitems.itemQuantity)
                                  .toDouble()*/
                            }

                        }

                        Log.d(TAG, "onClick: wholetable total tax $totaltaxtemp")
                        var found = -1
                        totaltaxtemp /= eligibleGuestsForDivision
                        var temp_remaining = totaltaxtemp * paidGuestAmount
                        var temp_subtotal =
                            totalPrice / eligibleGuestsForDivision
                        cartList?.taxlistDynamic?.forEachIndexed { indexcart, cartTaxtData ->
                            if (cartTaxtData.taxType == taxData.taxType) {
                                found = indexcart
                            }
                        }
                        if (found != -1) {
                            if (found <= cartList?.taxlistDynamic?.size!! - 1) {
                                if (cartList?.taxlistDynamic?.get(found)?.taxType != "Percentage") {
                                    cartList?.taxlistDynamic?.get(found)?.subTotalAmount =
                                        cartList?.taxlistDynamic?.get(found)?.subTotalAmount!!.plus(
                                            temp_subtotal
                                        )
                                }
                                cartList?.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                                    cartList?.taxlistDynamic?.get(found)?.totalTaxTypePrice!!.minus(
                                        temp_remaining
                                    )
                            }

                        } else {
                            var data = taxData
                            data.subTotalAmount = temp_subtotal
                            data.totalTaxTypePrice = temp_remaining
                            listreemaining = listOf(data)
                        }

                    }
                }

                listreemaining.forEach { remainingdata ->
                    cartList?.taxlistDynamic =
                        concatenate(cartList?.taxlistDynamic!!, listOf(remainingdata))
                }

                var alreadyDone = false
                viewModelPayment.addCart(cartList!!)
                CoroutineScope(Dispatchers.IO).launch {
                    if(listWT.isNotEmpty()) {

                        viewModelPayment.addItemToCart(
                            dashboardViewModel.currentCartItems,
                            listWT.first(),
                            ADD,
                            false
                        )
//
//
//                        dashboardViewModel.addItemToCartItems(it)

                    }
                }


                totalTax = 0.0
                var subTotal = 0.0
                var amtToPay = 0.0
                val totalItem: ArrayList<TbCartItem> = arrayListOf()
                for (i in 0 until adapterList.size) {
                    if (adapterList.get(i).isHeader == 1) {
                        adapterList.get(i).item?.let {
                            totalItem.add(it)
                        }
                    }
                }
                if (totalItem.isNotEmpty()) {
                    totalItem.forEach {

                        it.orderType = "DineIn"
                         it.employeeID = getOrderDetailsResponse?.employeeId ?:0

                        if (!it.isPaid) {
                            subTotal += (it.price * it.itemQuantity) - it.discountPrice
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach {
                                    subTotal += it.itemQuantity * it.price
                                }
                            }

                            it.taxes?.forEach { tax ->
                                totalTax += if (tax.taxType == "Percentage") {

                                    var modifierPrice = 0.0
                                    val price =
                                        (it.price * it.quantity) - it.discountPrice

                                    it.modifiers.forEach {
                                        modifierPrice += (it.price * it.itemQuantity)
                                    }

                                    val totalPrice = price + modifierPrice

                                    val itemTaxPrice =
                                        (tax.rate * totalPrice) / 100
                                    LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                                    MethodUtils.getTwoDecimal(itemTaxPrice)
                                    /* String.format("%.2f", itemTaxPrice)
                                     .toDouble()*/
                                } else {
                                    MethodUtils.getTwoDecimal(tax.rate * it.quantity)

                                    /* String.format(
                                     "%.2f",
                                     tax.rate * it.quantity
                                 )
                                     .toDouble()*/
                                }


                            }
                        }
                    }
                }
                // subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt


                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    var divideGuest = totalGuestCount - paidGuestAmount
                    if (paidGuestAmount == 0) {
                        divideCashDiscount = MethodUtils.calculateCashDiscount(
                            toFinalAmt,
                            prefProvider,
                            requireContext()
                        )
                    } else {
                        divideCashDiscount =
                            MethodUtils.calculateCashDiscount(
                                toFinalAmt,
                                prefProvider,
                                requireContext()
                            ) / eligibleGuestsForDivision
                    }
                } else {
                    divideCashDiscount = 0.0
                }
                var dineInOrderModel = DineInPaymentUpdateModel()
                dineInOrderModel.id = orderId
                var model = GuestPaymentRequest(
                    GuestPaymentAttributes().apply {
                        amount = MethodUtils.roundOffAmountDouble(toFinalAmt)
                        cardName = ""
                        cardNumber = ""
                        cardType = ""
                        cashDiscount = 0.0
                        cash_discount_or_surcharge = divideCashDiscount
                        cashDiscountFee = 0.0
                        employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                        taxAmount = MethodUtils.roundOffAmountDouble(finalTaxAmt)
                        subTotalPrice = MethodUtils.roundOffAmountDouble(subTotalWT)
                        offlineId = getOrderDetailsResponse?.offlineId ?: ""
                        payableType = "GuestTab"
                        paymentType = "Cash"
                        transactionId = randomOfflineId()
                        terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                        serviceChargeAmount = MethodUtils.roundOffAmountDouble(serviceCharge)


                    },
                    dineInOrderModel
                )
//            LogUtil.logE(TAG, "getPassmodel  ${Gson().toJson(model)}")
                val bundle = Bundle()
//            viewModelPayment.totalPrice = MethodUtils.roundOffAmobtnPayuntDouble(toFinalAmt)
//            viewModelPayment.subTotalPrice = MethodUtils.roundOffAmountDouble(subTotalDInin)
//            viewModelPayment.totalTax = MethodUtils.roundOffAmountDouble(finalTaxAmt)
//            viewModelPayment.cashdiscountAmount =
//                MethodUtils.roundOffAmountDouble(divideCashDiscount)
//            viewModelPayment.totalServiceCharge = MethodUtils.roundOffAmountDouble(serviceCharge)
//            viewModelPayment.totalDiscount = MethodUtils.roundOffAmountDouble(totalDiscount)

                LogUtil.logE(
                    "AAJE",
                    "subTotalDInin:  ${MethodUtils.roundOffAmountDouble(subTotalDInin)}"
                )
                bundle.putDouble("totalPrice", MethodUtils.roundOffAmountDouble(toFinalAmt))
                bundle.putDouble("subTotalPrice", MethodUtils.roundOffAmountDouble(subTotalDInin))
                bundle.putDouble("totalTax", MethodUtils.roundOffAmountDouble(finalTaxAmt))
                bundle.putParcelable("model", model)
                bundle.putBoolean("update", true)
                bundle.putDouble(
                    "divideCashDiscount",
                    MethodUtils.roundOffAmountDouble(divideCashDiscount)
                )
                LogUtil.logE("DineCheck", "totalDiscount  ${totalDiscount}")
                bundle.putParcelable("floorPlan", floorPlanModel)
                bundle.putDouble(
                    "totalServiceCharge",
                    MethodUtils.roundOffAmountDouble(serviceCharge)
                )
                bundle.putDouble("totalDiscount", MethodUtils.roundOffAmountDouble(totalDiscount))
                bundle.putDouble(
                    "totalOrderPassDiscount",
                    MethodUtils.roundOffAmountDouble(passDiscountTotal)
                )

                bundle.putDouble(
                    "totalOrderPassSC",
                    MethodUtils.roundOffAmountDouble(passSCTotal)
                )
                bundle.putString("orderOfflineId", offlineId)
                bundle.putString("paymentOfflineId", randomOfflineId())
                bundle.putBoolean("isTotalPayment", true)
                bundle.putBoolean("isLastPayment", true)
                bundle.putBoolean("isGuestPay", false)
                bundle.putParcelable(PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                bundle.putDouble(DINE_IN_SUBTOTAL, subTotalWT)
                bundle.putDouble(DINE_IN_TAX, viewModel.totalTaxAmount)
                bundle.putDouble(DINE_IN_DISCOUNT, viewModel.totalDiscountAmount)
                bundle.putDouble(DINE_IN_SERVICECHARGE, serviceCharge)
                var appliedServiceCharge: ArrayList<OrderServiceChargesAttribute> = arrayListOf()
                getOrderDetailsResponse?.orderServiceCharges?.forEach { service ->
                    var data: OrderServiceChargesAttribute = OrderServiceChargesAttribute(
                        amount = service.amount,
                        name = service.name,
                        rate = service.rate,
                        serviceChargeId = service.serviceChargeId,
                        max_guest_count = service.max_guest_count,
                        min_guest_count = service.min_guest_count,
                        id = service.id,
                        order_type = service.order_type
                    )
                    appliedServiceCharge.add(data)
                }
                bundle.putParcelableArrayList(
                    "serviceChargeAppliedList",
                    appliedServiceCharge
                )


                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")
                bundle.putBoolean(IS_GUEST_PAYMNET, false)
                bundle.putParcelableArrayList(
                    DINE_IN_ADAPTER_LIST, dineInTableAdapter.getList().toCollection(
                        arrayListOf()
                    )
                )

                LogUtil.logE(TAG, "passOrderId:  ${orderId}")
                bundle.putInt("orderId", orderId ?: -1)
                bundle.putInt(GUEST_POSITION, 0)
                prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)
                prefProvider.setValueInt(
                    Constants.ORDER_TYPE_ID,
                    getOrderDetailsResponse?.orderTypeId ?: 2
                )
                prefProvider.setValue(
                    Constants.ORDER_TYPE_NAME,
                    getOrderDetailsResponse?.orderTypeName ?: DINE_IN
                )

                if (findNavController().currentDestination?.id == R.id.dineInOrderTable)
                    findNavController().navigate(
                        R.id.action_dineInOrderTable_to_checkoutDineIN,
                        bundle
                    )


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        binding.txtHome.setOnClickListener {
            prefProvider.setValue(ORDER_TYPE, "")
            prefProvider.setValue(ORDER_TYPE_NAME, "")
            dineInTableAdapter.setList(arrayListOf())
            dashboardViewModel.cartModel = null
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }
        binding.txtHomeBottom.setOnClickListener {
            prefProvider.setValue(ORDER_TYPE, "")
            prefProvider.setValue(ORDER_TYPE_NAME, "")
            dineInTableAdapter.setList(arrayListOf())
            dashboardViewModel.cartModel = null
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }

        binding.txtEditOrder.setOnClickListener {
            val list = dineInTableAdapter.getList()
            val newList: ArrayList<DineInModel> = arrayListOf()

            dashboardViewModel.dineInHeaderPosition = 0
            dashboardViewModel.isDineInUpdate = true

            var headerPositionCounter = -1

            for (i in 0 until list.size) {
                val model = DineInModel()
                if (list[i].isHeader == 0) {
                    headerPositionCounter++
                    val listTbItem: ArrayList<TbCartItem> = arrayListOf()
                    model.id = list[i].id
                    model.isPaid = list[i].isPaid
                    model.title = list[i].title
                    model.customer = list[i].customer
                    model.isFired = list[i].isFired
                    model.guestDividerAmt = list[i].guestDividerAmt
                    model.guestDividedAmt = list[i].guestDividedAmt


                    for (j in i + 1 until list.size) {
                        if (list[j].isHeader == 1) {


                            /*  for (m in j until list.size){
                                  if(list.get(m).isHeader == 1 && list.get(m).item?.itemId == list[j].item?.itemId){

                                      list[j].item?.customItemID = kotlin.random.Random.nextInt(1,10000)
                                  }
                                  else{
                                      break
                                  }
                              }*/


                            list[j].item?.let { it1 ->
                                if (it1.discountPrice != 0.0) {
                                    it1.discountPrice =
                                        MethodUtils.roundOffAmountDouble(it1.discountPrice / it1.itemQuantity)
                                }
                                LogUtil.logE(TAG, "updateItemForDiscount  ${Gson().toJson(it1)}")
                                listTbItem.add(it1)
                            }

                        } else {
                            break
                        }

                    }
//                    model.items = listTbItem
//                    dashboardViewModel.currentCartItems = listTbItem
                    // IMPORTANT - remove this as this is just for logs
                    listTbItem.forEach {
                        it.guestIndexForDineIn = headerPositionCounter
                        it.taxes = arrayListOf()

                        it.employeeID = getOrderDetailsResponse?.employeeId?:0

                        it.orderType = "DineIn"


                        dashboardViewModel.currentCartItems.add(it)
                        Log.d(TAG, "testDineInUpdate onClick: " + Gson().toJson(it))
                    }

                    newList.add(model)


                }


            }



            LogUtil.logE(TAG, "listOfMoveItemIds:  ${listOfMoveItemIds.size}")
            if (newList.isNotEmpty()) newList[0].listOfItemsMoved.addAll(
                listOfMoveItemIds.toCollection(
                    arrayListOf()
                )
            )

            //   prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, Gson().toJson(newList))


            Log.e(TAG, "newListDineIn  ${Gson().toJson(newList)}")
            val bundle = Bundle()
            bundle.putBoolean("is_dine_in_edit", true)
            bundle.putParcelableArrayList(
                "dine_in_list",
                newList
            )
            bundle.putParcelableArrayList("dine_in_cart_items", dashboardViewModel.currentCartItems)

           // dashboardViewModel.oldDineInItems = dashboardViewModel.currentCartItems

            LogUtil.logE(
                "OrderFre",
                "APIDISc  ${getOrderDetailsResponse?.totalDiscount?.toDouble()}"
            )
            LogUtil.logE("OrderFre", "totalDiscount  ${totalDiscount}")
            LogUtil.logE("OrderFre", "OrderDiscount  ${update_order_Discount}")

            bundle.putDouble(
                "totalDiscount",
                update_order_Discount
            )
            bundle.putString("order_note", order_note)
            bundle.putParcelable("tableDetails", getOrderDetailsResponse?.floorPlanTable)
            viewModelPayment.deleteCart()
            orderId?.let { it1 -> bundle.putInt("orderId", it1) }
            Log.e(
                TAG,
                "dineIndorderId:   ${orderId}  >> ${getOrderDetailsResponse?.orderTypeName ?: DINE_IN}"
            )

            prefProvider.setValueInt("DINE_IN_ORDER_UPDATE",orderId?:0)
            prefProvider.setValueboolean(DINE_IN_UPDATE, true)
            prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
            prefProvider.setValueInt(
                Constants.ORDER_TYPE_ID,
                getOrderDetailsResponse?.orderTypeId ?: 2
            )
            prefProvider.setValue(
                Constants.ORDER_TYPE_NAME,
                getOrderDetailsResponse?.orderTypeName ?: DINE_IN
            )
            prefProvider.setValueInt(
                Constants.DINE_IN_TABLE_ID,
                prefProvider.getValueInt(Constants.ORDER_TYPE_ID, 0)
            )
            prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)

            /*   prefProvider.setValu
            e(Constants.ORDER_TYPE, DINE_IN)
               prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
               prefProvider.setValueInt(ORDER_TYPE_ID, 2)*/


            prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, true)

            findNavController().navigate(
                R.id.action_dineInOrderTable_to_dashboardCategoryNew,
                bundle
            )


        }

        binding.btnSendOrder.setOnClickListener {
            var guestAttribute = dineInData?.order?.guestAttributes

            var guestList = dineInTableAdapter.getList()
            for (i in 0 until guestList.size) {
                guestList.get(i).isFired = true
                guestList.get(i).items.forEach {
                    it.isFired = true
                }
            }

            Log.d(TAG, "1041 dineinlisttest setList: " + guestList.toCollection(arrayListOf()))
            dineInTableAdapter.setList(
                guestList.toCollection(arrayListOf()),
                notPayAnyAmount,
            )

            if (guestAttribute != null) {

                val ids: MutableList<Int> = ArrayList()


                /*  for (i in 0 until guestAttribute.size) {
                      if (guestAttribute[i].guestItemAttributes != null) {
                          guestAttribute[i].guestItemAttributes.forEach {
                              it.orderItemId?.let { it1 -> ids.add(it1) }
                          }
                      }
                  }*/



                for (i in 0 until guestAttribute.size) {
                    guestAttribute[i].guestItemAttributes.forEach { it ->

                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }
                var idStr = ids.toString()


                // viewModel.fireItemToKitchen(dineInData?.order?.id!!, true, idStr)

            } else {

                var guestAttribute = dineInTableAdapter.getList()
                val ids: MutableList<Int> = ArrayList()

                for (i in 0 until guestAttribute.size) {
                    guestAttribute[i].items.forEach { it ->
                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }
                var idStr = Gson().toJson(ids.toTypedArray())


                viewModel.fireItemToKitchen(orderId!!, true, idStr, false)
            }

        }

        /*  binding.imgClose.setOnClickListener {
              findNavController().popBackStack()
          }*/

        binding.llInfo.setOnClickListener {
            showPopupWindow(it)
        }
    }

    private fun variationAtt(variation: GetOrderDetailsResponse.Data.OrderItem.OrderItemVariationAttribute): List<VariationsAttribute> {

        val variationsAttributeList = ArrayList<VariationsAttribute>()

        if (variation != null) {
            val variationsAttribute = VariationsAttribute()
            variationsAttribute.id = variation.variationId
            variationsAttribute.name = variation.name
            variationsAttribute.price = variation.price
            variationsAttribute.orderVariationId = variation.id
            variationsAttributeList.add(variationsAttribute)

        }

        return variationsAttributeList
    }

    private fun modifiersIds(orderItemModifiers: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                it.modifier_set_id?.let { it1 -> selectedIds.add(it1) }
            }
        }
        var uniqueSelectedId = HashSet<Int>(selectedIds)
        return uniqueSelectedId.toList()
    }

    private fun addGuestToOrder(count: Int) {
        if (count == 0) {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), getString(R.string.minimum_guest_count_should_be_one)
            ) { _, _ ->
            }
            return
        }
        val adapterList = dineInTableAdapter.getList()
        cartList = getCartModel(adapterList.toCollection(arrayListOf()))
        Log.d(TAG, "addGuestToOrder: " + Gson().toJson(cartList))
        var existing_count = cartList?.dineInList!!.size - 1
        var total_count = existing_count + count
        if (total_count <= 15) {
            var existinglist: ArrayList<DineInModel> = arrayListOf()
            existinglist.addAll(cartList?.dineInList!!.toMutableList())
            Log.d(TAG, "addGuestToOrder size: " + existinglist.size)
            val dineInList: java.util.ArrayList<DineInModel> = arrayListOf()
            if (existinglist.isNotEmpty()) {
                // List of available counts from list to add new guest
                var availableName: ArrayList<Int> = arrayListOf()
                for (i in 1 until 16) {
                    var filteredList: List<DineInModel> = arrayListOf()
                    filteredList = dineInTableAdapter.getList()
                        .filter { item -> item.title?.substringAfter("Guest ") == i.toString() }
                        ?: arrayListOf()
                    if (filteredList.isEmpty()) {
                        availableName.add(i)
                    }
                    if (availableName.size >= count) {
                        break
                    }
                }

                for (i in 1..count) {
                    dineInList.add(
                        DineInModel(
                            0,
                            false,
                            0,
                            "Guest ${availableName[i - 1]}",
                            floorPlanTable = cartList!!.dineInList!![0].floorPlanTable

                        )
                    )
                }

            }
            existinglist.addAll(dineInList)
            cartList?.dineInList = existinglist.toList()
            Log.d(TAG, "addGuestToOrder size: " + cartList!!.dineInList?.size)
            Log.d(TAG, "addGuestToOrder: " + Gson().toJson(cartList?.dineInList))
            prefProvider.setValueboolean(DINE_IN_UPDATE, true)
            // RESET Data after coming back from checkout screen by clicking on guest pay (to resolve calculation issue for guest division)
            dashboardViewModel.totalDiscount = getOrderDetailsResponse?.totalDiscount ?: 0.0
            dashboardViewModel.subTotalPrice = getOrderDetailsResponse?.subTotal ?: 0.0
            // END RESET
            val request = dashboardViewModel.updateOrder(cartList!!)
            orderId?.let { viewModel.updateOrder(it, request) }
        } else {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), "You can't add more than 15 Guest in an order."
            ) { _, _ ->
            }
        }

    }

    private fun getTotalTaxBirfurcation(item: TbCartItem, itemtype: TaxData): Double {
        var totaltaxtemp: Double = 0.0
        var modifierPrice = 0.0
        val price =
            (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

        item.modifiers.forEach {
            modifierPrice += (it.price * it.itemQuantity)
        }

        val totalPrice =
            price + modifierPrice /*- (discountPrice * item.itemQuantity)*/


        totaltaxtemp += if (itemtype.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (itemtype.rate * totalPrice) / 100
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                // MethodUtils.getTwoDecimal(itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + itemtype.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                // MethodUtils.getTwoDecimal(itemtype.rate * item.itemQuantity)
                itemtype.rate * item.itemQuantity
            }

        }
        return totaltaxtemp
    }

    private fun taxBifurcationCalculation(
        item: TbCartItem,
        cartModel: CartModel
    ): CartModel {
        item.taxes?.forEachIndexed { indextax, itemtype ->
            if (itemtype.isActive) {
                if (cartModel.taxlistDynamic?.isNotEmpty() == true) {
                    var found = -1
                    cartModel.taxlistDynamic!!.forEachIndexed { index, itemData ->
                        if (itemtype.orderTaxId == itemData.orderTaxId) {
                            found = index
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

                            val totalPrice =
                                price + modifierPrice

                            itemtype.subTotalAmount = itemtype.subTotalAmount.plus(totalPrice)
                        }
                        itemtype.totalTaxTypePrice = getTotalTaxBirfurcation(item, itemtype)
                        cartModel.taxlistDynamic =
                            concatenate(cartModel.taxlistDynamic!!, listOf(itemtype))
                        prefProvider.setValue(Constants.taxListDynamic,Gson().toJson(cartModel.taxlistDynamic))

                    } else {
                        if (itemtype.taxType != "Percentage") {
                            var modifierPrice: Double = 0.0
                            val price =
                                (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                            item.modifiers.forEach {
                                modifierPrice += (it.price * it.itemQuantity)
                            }
                            val totalPrice =
                                price + modifierPrice
                            cartModel.taxlistDynamic!![found].subTotalAmount =
                                cartModel.taxlistDynamic!![found].subTotalAmount?.plus(totalPrice)
                        }

                        cartModel.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                            cartModel.taxlistDynamic!![found].totalTaxTypePrice.plus(
                                getTotalTaxBirfurcation(
                                    item,
                                    itemtype
                                )
                            )
                    }
                } else {
                    if (itemtype.taxType != "Percentage") {
                        var modifierPrice: Double = 0.0
                        val price =
                            (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                        item.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice =
                            price + modifierPrice
                        itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                    }
                    itemtype.totalTaxTypePrice = getTotalTaxBirfurcation(item, itemtype)
                    cartModel.taxlistDynamic = listOf(itemtype)
                    prefProvider.setValue(Constants.taxListDynamic,Gson().toJson(cartModel.taxlistDynamic))

                }
            }

        }
        return cartModel
    }

    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    @SuppressLint("SetTextI18n")
    private fun showPopupWindow(view: View) {

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window, null)

        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)
        val txtTotalcashAdj: AppCompatTextView = popupView.findViewById(R.id.txtnoncashadj)
        val chkLoyaltyAmount: CheckBox = popupView.findViewById(R.id.chkLoyaltyAmount)
        val groupLoyalty: Group = popupView.findViewById(R.id.groupLoyalty)
        chkLoyaltyAmount.visibility = View.GONE
        groupLoyalty.visibility = View.GONE

        val linear_NonCashDiscount: LinearLayoutCompat =
            popupView.findViewById(R.id.linear_NonCashDiscount)

        if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {
            linear_NonCashDiscount.visibility = View.VISIBLE
            txtTotalcashAdj.text = "$" + MethodUtils.getTwoDecimal(
                MethodUtils.calculateCashDiscount(
                    toFinalAmt,
                    prefProvider,
                    requireContext()
                )
            )

        } else {
            linear_NonCashDiscount.visibility = View.GONE
        }




        txtSubTotal.text = "$" + String.format(
            "%.2f",
            subTotalDInin
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            serviceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            totalDiscount
        )

        Log.e("DineInOrderTable", "finalTaxAmt:  ${finalTaxAmt}")

        txtTotalAmount.text = binding.txtTotalAmountNew.text.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            finalTaxAmt
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

        })
        popupWindow!!.showAtLocation(view, Gravity.TOP, 600, 650);
//        } else {
//            popupWindow!!.dismiss()
//            popupWindow = null
//        }

    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE(TAG, "ShowProgress ${it}")
                if (it) {
                    if (pd != null && !pd.isShowing) {
                        pd.show()
                    }

                } else {
                    if (pd != null && pd.isShowing) {
                        pd.dismiss()
                    }
                }
            }
        }

        viewModel.showProgressCash.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(
                        "Please wait payment under process",
                        requireActivity()
                    )
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.msgText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Log.d("###17MAR23", "Item Fired Done: Called - End All")
//                if(pd != null && pd.isShowing){
//                    pd.dismiss()
//                }'
                //binding.maskLayout?.gone()
                ProgressUtils.dismissProgressDialog()
                if (it.toString() != "null") {
                    AlertUtils.showCustomAlert(requireContext(), it)
                }
                dineInTableAdapter.updateStatus(clickedPos, isFireAll)
            }
        }


    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }


    override fun onGuestPay(
        dineInModel: DineInModel,
        position: Int,
        subTotalGuest: Double,
        totalGuest: Double,
        taxGuest: Double,
        serviceChargeGuest: Double,
        divideDiscount2: Double,
        dividedGuestAmt: Double,
        listItemWT: ArrayList<TbCartItem>,
        listItemGuestSelected: ArrayList<TbCartItem>
    ) {

        //New Drag and Drop


        Log.e(TAG, "divideDiscount2:  ${divideDiscount2}")
        var divideDiscount = divideDiscount2
        LogUtil.logE("WholeTabDis", "Fasf  ${wholeTableDiscount}")
        eligibleGuestsForDivision = dineInTableAdapter.getList()[0].eligibleGuestsForDivision
        var dividedWtDis: Double = MethodUtils.roundOffAmountDouble(
            wholeTableDiscount / eligibleGuestsForDivision
        )

        divideDiscount += dividedWtDis
        LogUtil.logE("saff", "afadivideDiscount ${divideDiscount}")

        LogUtil.logE("TODAYBOLD", "subTotalB  ${subTotalGuest + dividedGuestAmt}")
        LogUtil.logE("TODAYBOLD", "totalGuest ${totalGuest}")
        LogUtil.logE("TODAYBOLD", "taxGuest ${taxGuest}")
        LogUtil.logE("TODAYBOLD", "serviceChargeGuest  ${serviceChargeGuest}")
        LogUtil.logE("TODAYBOLD", "divideDiscount ${divideDiscount}")
        val bundle = Bundle()
        bundle.putDouble("subTotalB", subTotalGuest + dividedGuestAmt)
        bundle.putDouble("totalB", totalGuest)
        bundle.putDouble("totalTaxB", taxGuest)
        bundle.putDouble("serviceChargeB", serviceChargeGuest)
        bundle.putDouble("dicountB", divideDiscount)


        val adapterList = dineInTableAdapter.getList()


        var subTotal = 0.0
        var amtToPay = 0.0
        val totalItem: ArrayList<TbCartItem> = arrayListOf()
        for (i in position + 1 until adapterList.size) {
            if (adapterList.get(i).isHeader == 1) {
                adapterList.get(i).item?.let {
                    totalItem.add(it)
                }
            } else {
                break
            }
        }
        if (totalItem.isNotEmpty()) {
            totalItem.forEach {
                subTotal += it.price * it.itemQuantity

            }
        }
        subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt


        totalGuestCount = eligibleGuestsForDivision ?: 1
        LogUtil.logE("TODAY", "totalGuestCount:  ${totalGuestCount}")
        LogUtil.logE("TODAY", "toFinalAmt:  ${toFinalAmt}")

        var divideCashDiscount = 0.0
        if (getOrderDetailsResponse?.payments?.size!! > 1) {
            var payguest = totalGuestCount - paidGuestAmount
            divideCashDiscount = MethodUtils.calculateCashDiscount(
                toFinalAmt,
                prefProvider,
                requireContext()
            ) / payguest

        } else {
            divideCashDiscount = MethodUtils.calculateCashDiscount(
                toFinalAmt,
                prefProvider,
                requireContext()
            ) / totalGuestCount
        }


        LogUtil.logE(TAG, "divideCashDiscount:  ${divideCashDiscount}")
        var orderOfflineId = randomOfflineId()


        val paymentAttr = GuestPaymentAttributes().apply {
            amount = totalGuest
            cardName = ""
            cardNumber = ""
            cardType = ""
            cashDiscount = divideDiscount
            cashDiscountFee = 0.0
            cash_discount_or_surcharge = divideCashDiscount
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            taxAmount = taxGuest
            subTotalPrice = subTotalGuest
            offlineId = orderOfflineId
            payableType = "GuestTab"
            paymentType = "Cash"
            transactionId = orderOfflineId
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            order_id = orderId
            paymentAttributes = listOf(GuestPaymentAttributes().apply {
                amount = totalGuest
                cardName = ""
                cardNumber = ""
                cardType = ""
                cashDiscount = divideDiscount
                cashDiscountFee = 0.0
                cash_discount_or_surcharge = divideCashDiscount
                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                taxAmount = taxGuest
                subTotalPrice = subTotalGuest
                offlineId = randomOfflineId()
                payableType = "GuestTab"
                paymentType = "Cash"
                transactionId = randomOfflineId()
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                order_id = orderId

            })

        }
        var dineInOrderModel = DineInPaymentUpdateModel()
        dineInOrderModel.id = orderId
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValue(Constants.SUB_TOTAL_DINEIN, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TIPS_AMOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TAX_CHARGE_DINEIN, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE_DINEIN, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")
        prefProvider.setValue(Constants.TOTAL_PRICE_DINEIN, "")
        prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
        var modelReq = DineInOrderPayment(dineInOrderModel)
        var model = GuestPaymentRequest(paymentAttr, dineInOrderModel)



        dineInModel.id?.let { bundle.putInt("id", it) }
        bundle.putParcelable("cartList", cartList)
        bundle.putDouble("totalPrice", MethodUtils.roundOffAmountDouble(totalGuest))
        bundle.putDouble("totalDiscount", MethodUtils.roundOffAmountDouble(divideDiscount))
        bundle.putDouble("divideCashDiscount", MethodUtils.roundOffAmountDouble(divideCashDiscount))
        bundle.putInt("totalGuestCount", totalGuestCount)
        bundle.putInt("paidGuestCount", paidGuestAmount)
        bundle.putDouble("subTotalPrice", MethodUtils.roundOffAmountDouble(subTotalGuest))
        bundle.putDouble("totalTax", MethodUtils.roundOffAmountDouble(taxGuest))
        bundle.putParcelable("model", model)
        bundle.putParcelable("floorPlan", floorPlanModel)
        bundle.putDouble("totalServiceCharge", MethodUtils.roundOffAmountDouble(serviceChargeGuest))
        bundle.putParcelable("orderPayment", modelReq)
        bundle.putString("orderOfflineId", orderOfflineId)
        bundle.putString("paymentOfflineId", paymentAttr.offlineId)
        bundle.putInt("orderId", orderId ?: 0)
        bundle.putBoolean("isGuestPay", true)
        bundle.putParcelable(PRINT_DATA_DINE_IN, getOrderDetailsResponse)
        bundle.putDouble(DINE_IN_SUBTOTAL, MethodUtils.roundOffAmountDouble(subTotalGuest))
        bundle.putDouble(DINE_IN_TAX, MethodUtils.roundOffAmountDouble(taxGuest))
        bundle.putDouble(DINE_IN_DISCOUNT, MethodUtils.roundOffAmountDouble(divideDiscount))
        bundle.putDouble(
            DINE_IN_SERVICECHARGE,
            MethodUtils.roundOffAmountDouble(serviceChargeGuest)
        )
        var appliedServiceCharge: ArrayList<OrderServiceChargesAttribute> = arrayListOf()
        getOrderDetailsResponse?.orderServiceCharges?.forEach { service ->
            var data: OrderServiceChargesAttribute = OrderServiceChargesAttribute(
                amount = service.amount,
                name = service.name,
                rate = service.rate,
                serviceChargeId = service.serviceChargeId,
                max_guest_count = service.max_guest_count,
                min_guest_count = service.min_guest_count,
                id = service.id,
                order_type = service.order_type
            )
            appliedServiceCharge.add(data)
        }
        bundle.putParcelableArrayList(
            "serviceChargeAppliedList",
            appliedServiceCharge
        )


        bundle.putBoolean(IS_GUEST_PAYMNET, true)
        bundle.putInt(GUEST_POSITION, position)
        bundle.putParcelableArrayList(
            DINE_IN_ADAPTER_LIST, dineInTableAdapter.getList().toCollection(
                arrayListOf()
            )
        )
        //whole table amount divided calculation
        var wholeNewSubtotal = 0.0
        var WTTaxes = 0.0
        var serviceCharge = 0.0

        LogUtil.logE("AAjeChange", "listItemWT:  ${Gson().toJson(listItemWT)}")
        listItemWT.forEach {
            val obj = it
            wholeNewSubtotal += (obj.price * obj.itemQuantity)

            if (obj.modifiers.isNotEmpty()) {
                obj.modifiers.forEach {
                    wholeNewSubtotal += it.price * it.itemQuantity
                }
            }


            obj.taxes?.forEach { tax ->
                if (tax.isActive) {
                    WTTaxes += if (tax.taxType == "Percentage") {

                        var modifierPrice = 0.0
                        val price =
                            (obj.price * obj.itemQuantity) - obj.discountPrice

                        obj.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice

                        val itemTaxPrice =
                            (tax.rate * totalPrice) / 100
                        LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else {

                        String.format(
                            "%.2f",
                            tax.rate * obj.itemQuantity
                        ).toDouble()
                    }
                }


            }
        }
        if (serviceChargeList?.isNotEmpty() == true) {
            Log.e("AajeChange", "serviceChargeList:  ${Gson().toJson(serviceChargeList)}")
            var isApplied = false
            var chSubTotal =
                wholeNewSubtotal / (getOrderDetailsResponse?.guestAttributes?.size?.minus(1) ?: 1)
            Log.e(TAG, "chSubTotal  ${chSubTotal}")
            chSubTotal -= divideDiscount
            String.format("%.2f", chSubTotal).toDouble()

            Log.e(TAG, "globalOrderDiscountForGPAy:   ${globalOrderDiscount}")
            Log.e(TAG, "totalGuestCountGPAy:   ${totalGuestCount}")
            Log.e(TAG, "totalGuestAfterserviceCharge ${serviceCharge}")


            var orderDiscountGuestDivided =
                MethodUtils.roundOffAmountDouble(globalOrderDiscount / totalGuestCount)
            Log.e(TAG, "orderDiscountGuestDividedGpay:  ${orderDiscountGuestDivided}")
            serviceChargeList.forEach {
                if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {

                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                totalGuestCount
                            )
                        ) {
                            Log.d(
                                TAG,
                                "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + totalGuestCount
                            )
                            isApplied = true
                            Log.e(
                                TAG,
                                "checkSer  ${it.percentage}  check 2ndSubTital ${chSubTotal + orderDiscountGuestDivided}"
                            )
                            serviceCharge += MethodUtils.roundOffAmountDouble(((chSubTotal + orderDiscountGuestDivided) * it.percentage) / 100)
                            return@forEach
                        }
                    }
                }
            }
            if (!isApplied) {
                serviceChargeList.forEach { service ->
                    if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                        serviceCharge += MethodUtils.roundOffAmountDouble(((chSubTotal + orderDiscountGuestDivided) * service.percentage) / 100)
                        return@forEach
                    }
                }
            }


        }
        LogUtil.logE("AajeChange", "wholeNewSubtotal  ${wholeNewSubtotal}")
        LogUtil.logE("AajeChange", "WTTaxes  ${WTTaxes}")
        LogUtil.logE("AajeChange", "serviceCharge  ${serviceCharge}")
        LogUtil.logE("AajeChange", "totalGuestCount  ${totalGuestCount}")

        wholeNewSubtotal = MethodUtils.roundOffAmountDouble(wholeNewSubtotal / totalGuestCount)
        WTTaxes = MethodUtils.roundOffAmountDouble(WTTaxes / totalGuestCount)
//        serviceCharge = MethodUtils.roundOffAmountDouble(serviceCharge / totalGuestCount)

        LogUtil.logE(
            "FinalDetails",
            "subTotal:  ${MethodUtils.roundOffAmountDouble(subTotalGuest + wholeNewSubtotal)}"
        )
        LogUtil.logE(
            "FinalDetails",
            "guestTotalTax:  ${MethodUtils.roundOffAmountDouble(taxGuest + WTTaxes)}"
        )
        LogUtil.logE(
            "FinalDetails",
            "dividedDiscount:  ${MethodUtils.roundOffAmountDouble(divideDiscount)}"
        )
        LogUtil.logE(
            "FinalDetails",
            "dividedCashdiscount :  ${MethodUtils.roundOffAmountDouble(divideCashDiscount)}"
        )
        LogUtil.logE(
            "FinalDetails",
            "guestTotalServiceCharge:  ${MethodUtils.roundOffAmountDouble(serviceChargeGuest)}"
        )

        val newGuestModel = GuestDataModel(
            subTotal = MethodUtils.roundOffAmountDouble(subTotalGuest + wholeNewSubtotal),
            totalTax = MethodUtils.roundOffAmountDouble(taxGuest + WTTaxes),
            totalAmount = totalGuest,
            totalDiscount = MethodUtils.roundOffAmountDouble(divideDiscount),
            cashDiscount = MethodUtils.roundOffAmountDouble(divideCashDiscount),
            totalServiceCharge = MethodUtils.roundOffAmountDouble(serviceChargeGuest)

        )
        LogUtil.logE(TAG, "newGuestModel:  ${Gson().toJson(newGuestModel)}")
        bundle.putParcelable(DINE_IN_GUEST_PAYMENT_DATA, newGuestModel)
        //orderId?.let { it1 -> bundle.putInt("orderId", it1) }
//        orderId?.let { it1 -> prefProvider.setValueInt("ORDER_ID", it1) }
        var wholeTableAmt = 0.0
        var paidAmount = 0.0
        for (i in 0 until adapterList.size) {
            adapterList.get(i).item.let {
                wholeTableAmt += it?.price?.times(it.itemQuantity) ?: 0.0
                if (it?.isPaid == true) {
                    paidAmount += it.price.times(it.itemQuantity)
                }


            }
            if (adapterList.get(i).isPaid) {
                paidAmount += adapterList.get(0).guestDividedAmt
            }

        }
        var isLastPayment = false
        if (totalGuestCount.minus(1) == paidGuestAmount) {
            isLastPayment = true
            bundle.putBoolean("isLastPayment", true)
        } else {
            isLastPayment = false
            bundle.putBoolean("isLastPayment", false)
        }

        var wholeEmpty = false
        getOrderDetailsResponse?.guestAttributes?.forEach {
            if (it.name.equals("Whole Table", true)) {
                if (it.guestItemAttributes.isEmpty()) {
                    wholeEmpty = true

                }
            }
        }

        var tempGuestWithItem = 0
        var guestItemWithoutItem = 0
        LogUtil.logE("MainTo", "wholeEmpty:  ${wholeEmpty}")
        if (wholeEmpty) {
            getOrderDetailsResponse?.guestAttributes?.forEach {
                if (!it.name.equals(
                        "Whole Table",
                        true
                    ) && !it.isPaid
                ) {
                    if (it.guestItemAttributes.isNotEmpty()) {
                        tempGuestWithItem++
                    } else {
                        guestItemWithoutItem++
                    }

                }

            }
        }
        if (wholeEmpty && tempGuestWithItem == 1) {
            isLastPayment = true
            bundle.putBoolean("isLastPayment", true)
        }

        LogUtil.logE("FinalLast", "FinalLast ${isLastPayment}")
        Log.e("checkDividedDis", "divideCashDiscount:  ${divideCashDiscount}")


        val dineinCartPaymentModel: DineinCartPaymentModel? = null

        dineinCartPaymentModel?.isTotalPayment = false
        dineinCartPaymentModel?.isLastPayment = isLastPayment
        dineinCartPaymentModel?.totalPrice = MethodUtils.roundOffAmountDouble(totalGuest)
        dineinCartPaymentModel?.subTotalPrice = MethodUtils.roundOffAmountDouble(subTotalGuest)
        dineinCartPaymentModel?.isGuestPay = true
        dineinCartPaymentModel?.splitModel = modelReq
        dineinCartPaymentModel?.totalServiceCharge =
            MethodUtils.roundOffAmountDouble(serviceChargeGuest)
        dineinCartPaymentModel?.totalDiscount = MethodUtils.roundOffAmountDouble(divideDiscount)
        dineinCartPaymentModel?.divideCashDiscount =
            MethodUtils.roundOffAmountDouble(divideCashDiscount)
        dineinCartPaymentModel?.totalTax = MethodUtils.roundOffAmountDouble(taxGuest)
        dineinCartPaymentModel?.getOrderDetailsResponse = getOrderDetailsResponse
        dineinCartPaymentModel?.dineInAdapterList =
            dineInTableAdapter.getList().toCollection(arrayListOf())
        dineinCartPaymentModel?.guestRequestModel = model
        dineinCartPaymentModel?.totalGuestCount = totalGuestCount
        dineinCartPaymentModel?.cartList = cartList
        dineinCartPaymentModel?.guestId = id
        dineinCartPaymentModel?.paidGuestCount = paidGuestAmount
        dineinCartPaymentModel?.guestSelectedPos = position
        dineinCartPaymentModel?.floorPlanModel = floorPlanModel

        prefProvider.setValue(Constants.ORDER_TYPE, prefProvider.getValue(Constants.ORDER_TYPE, ""))
        prefProvider.setValueInt(Constants.ORDER_TYPE_ID, getOrderDetailsResponse?.orderTypeId ?: 2)
        prefProvider.setValue(
            Constants.ORDER_TYPE_NAME,
            getOrderDetailsResponse?.orderTypeName ?: DINE_IN
        )
        bundle.putParcelable("dineinPaymentModel", dineinCartPaymentModel)


        cartList = getCartModel(adapterList.toCollection(arrayListOf()))

        var temp_itemslist: ArrayList<TbCartItem> = arrayListOf()


//        temp_itemslist.addAll(listItemWT)
        temp_itemslist.addAll(listItemGuestSelected)
        cartList?.taxlistDynamic = listOf()
        temp_itemslist.forEach { item ->
            cartList = taxBifurcationCalculation(item, cartList!!)
        }

        var remaining_list: List<TaxData> = emptyList()
        Log.e(TAG, "getcartListbeforeAdd  ${Gson().toJson(cartList?.taxlistDynamic)}")
        var listTaxBirfucaWholeTb: ArrayList<TaxData> = arrayListOf()
        listItemWT.forEach { wholetableitems ->
            wholetableitems.taxes?.forEachIndexed { index, taxData ->
                var modifierPrice: Double = 0.0
                var totaltaxtemp: Double = 0.0
                val price =
                    (wholetableitems.price * wholetableitems.itemQuantity) - (wholetableitems.discountPrice * wholetableitems.itemQuantity)

                wholetableitems.modifiers.forEach {
                    modifierPrice += (it.price * it.itemQuantity)
                }

                val totalPrice =
                    price + modifierPrice
                totaltaxtemp += if (taxData.taxType == "Percentage") {
                    if (totalPrice < 0.0) {

                        String.format("%.2f", 0.00)
                            .toDouble()
                    } else {
                        val itemTaxPrice =
                            (taxData.rate * totalPrice) / 100
                        LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    }

                } else {
                    Log.d("yash", "taxCalculation: " + taxData.taxType)
                    if (totalPrice <= 0.0) {
                        String.format("%.2f", 0.00)
                            .toDouble()
                    } else {
                        String.format("%.2f", taxData.rate * wholetableitems.itemQuantity)
                            .toDouble()
                    }

                }


                var found = -1
                totaltaxtemp /= (getOrderDetailsResponse?.guestAttributes?.size!! - 1)
                var temp_remaining = totaltaxtemp
                var temp_subtotal =
                    totalPrice / (getOrderDetailsResponse?.guestAttributes?.size!! - 1)
                cartList?.taxlistDynamic?.forEachIndexed { indexcart, cartTaxtData ->
                    if (cartTaxtData.orderTaxId == taxData.orderTaxId) {
                        found = indexcart
                    }
                }
                listTaxBirfucaWholeTb.forEachIndexed { index, whtbtax ->
                    if (whtbtax.orderTaxId == whtbtax.orderTaxId) {
                        found = index
                    }
                }
                Log.d(TAG, "onClick: wholetable total tax $totaltaxtemp")
                if (found != -1) {
                    if (found <= cartList?.taxlistDynamic?.size!! - 1) {
                        if (cartList?.taxlistDynamic?.get(found)?.taxType != "Percentage") {
                            cartList?.taxlistDynamic?.get(found)?.subTotalAmount =
                                cartList?.taxlistDynamic?.get(found)?.subTotalAmount!!.plus(
                                    temp_subtotal
                                )
                        }
                        cartList?.taxlistDynamic?.get(found)?.totalTaxTypePrice =
                            cartList?.taxlistDynamic?.get(found)?.totalTaxTypePrice!!.plus(
                                temp_remaining
                            )
                    } else {
                        if (listTaxBirfucaWholeTb[found].taxType != "Percentage") {
                            listTaxBirfucaWholeTb[found].subTotalAmount += temp_subtotal
                        }
                        listTaxBirfucaWholeTb[found].totalTaxTypePrice += temp_remaining
                    }
                } else {
                    if (cartList?.taxlistDynamic.isNullOrEmpty()) {
                        var data = taxData
                        if (data.taxType != "Percentage") {
                            data.subTotalAmount = temp_subtotal
                        }
                        data.totalTaxTypePrice = temp_remaining
                        listTaxBirfucaWholeTb.add(data)
                    } else {
                        var data = taxData
                        if (data.taxType != "Percentage") {
                            data.subTotalAmount = temp_subtotal
                        }
                        data.totalTaxTypePrice = temp_remaining
                        remaining_list = listOf(data)
                    }

                }

            }
        }
        dashboardViewModel.setGuestPay(true)
        if (cartList?.taxlistDynamic.isNullOrEmpty()) {
            cartList?.taxlistDynamic = listTaxBirfucaWholeTb.toList()
        } else {
            remaining_list.forEach { remainingdata ->
                cartList?.taxlistDynamic =
                    concatenate(cartList?.taxlistDynamic!!, listOf(remainingdata))
            }
        }



        Log.d(TAG, "onGuestPay: onlywholetable birfurcation" + Gson().toJson(listTaxBirfucaWholeTb))
        Log.d(TAG, "getcartListAfterAdd: remaining : ${Gson().toJson(remaining_list)}")
        LogUtil.logE(TAG, "getcartListAfterAdd  ${Gson().toJson(cartList?.taxlistDynamic)}")

        viewModelPayment.addCart(cartList!!)
        findNavController().navigate(
            R.id.action_dineInOrderTable_to_checkoutDineIN,
            bundle
        )


    }

    fun checkMaxGuestCountId(serviceChargeList: ArrayList<TbServiceCharge>): Int {
        var maxValue = 0
        var serviceChargeId = 0
        serviceChargeList.forEach { serviceCharge ->
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

    override fun onSendItemToKitchen(item: TbCartItem) {

        val ids: MutableList<Int> = ArrayList()
        item.orderItemId?.let { ids.add(it) }


        /*  for (i in 0 until guestAttribute.size) {
              if (guestAttribute[i].guestItemAttributes != null) {
                  guestAttribute[i].guestItemAttributes.forEach {
                      it.orderItemId?.let { it1 -> ids.add(it1) }
                  }
              }
          }*/


        var idStr = Gson().toJson(ids.toString())


        viewModel.fireItemToKitchen(orderId!!, true, idStr, false, item)

    }

    override fun onWholeTableToKitchen(
        ids: String,
        list: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
    ) {
        LogUtil.logE(TAG, "WholeTableITem")
        viewModel.fireItemToKitchen(orderId ?: 0, true, ids, true)
        for (i in 0 until kitchenPrinterList.size) {
            if (kitchenPrinterList[i].status) {
                if (!prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                    initKitchenPrinter(
                        kitchenPrinterList.get(i),
                        Constants.KITCHEN,
                        list,
                        listItemWithGuest
                    )
                }
            }
        }
    }

    override fun singleItemFired(
        id: String,
        position: Int,
        item: TbCartItem,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
    ) {

        clickedPos = position
        viewModel.fireItemToKitchen(orderId ?: 0, true, id, false, item)

        var listItem: ArrayList<TbCartItem> = arrayListOf()
        listItem.add(item)
        for (i in 0 until kitchenPrinterList.size) {
            if (kitchenPrinterList[i].status) {
                if (!prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                    initKitchenPrinter(
                        kitchenPrinterList.get(i), Constants.KITCHEN, listItem, listItemWithGuest
                    )
                }
            }
        }

    }

    override fun onGuestPrint(
        listItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double,
        total: Double,
        taxGuest: Double,
        serviceChargeGuest: Double,
        divideDiscount: Double
    ) {

        if (listItem.isNotEmpty()) {
            if (listItem[0].isPaid) {
                guestPrint(
                    "Paid",
                    listItem,
                    guestName,
                    listWTitems,
                    subTotalGuest,
                    total,
                    taxGuest,
                    serviceChargeGuest,
                    divideDiscount
                )
            } else {
                guestPrint(
                    "Unpaid", listItem, guestName, listWTitems, subTotalGuest,
                    total,
                    taxGuest,
                    serviceChargeGuest,
                    divideDiscount
                )

            }
        } else if (listItem.isEmpty() && listWTitems.isNotEmpty()) {
            guestPrint(
                "Unpaid", listItem, guestName, listWTitems, subTotalGuest,
                total,
                taxGuest,
                serviceChargeGuest,
                divideDiscount
            )
        }

    }

    override fun onRemoveGuest(position: Int) {
        if ((getOrderDetailsResponse?.guestAttributes?.size?.minus(1) ?: 0) > 1) {
            // Remove guest from list
            dineInTableAdapter.getList()[position].apply { this.isDestroy = true }
            updateOrderCall(isFromReorder = false)
        } else {
            viewModel.unableToRemoveGuest(getString(R.string.minimum_one_guest_is_required))
        }
    }

    private fun guestPrint(
        paymentStatus: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        wtItems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {

        LogUtil.logE(TAG, "customerListSize  ${customerList.size}")
        if (customerList.isNotEmpty()) {
            customerList.forEach {
                if (it.status) {
                    initPrinter(
                        it,
                        Constants.CUSTOMER,
                        paymentStatus,
                        true,
                        listGuestItem,
                        guestName,
                        wtItems,
                        subTotalGuest,
                        total,
                        taxGuest,
                        serviceChargeGuest,
                        divideDiscount

                    )
                }

            }
        }

    }

    private fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss


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

    // To set guestItemId to all Items after performing reordering
    private fun reorderedItemObserver() {
        viewModel.reorderItemsSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                var newList = dineInTableAdapter.getList()
                if (baseResponse != null) {
                    listOfMoveItemIds.clear()
                    prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                    // Assign guest item id to existing item
                    for (i in 0 until baseResponse.order.guestAttributes.size) {
                        var guestItemsList =
                            baseResponse.order.guestAttributes[i].guestItemAttributes
                        if (guestItemsList.isNotEmpty()) {
                            guestItemsList.forEach {
                                newList.forEach { dineInItem ->
                                    if (dineInItem.isHeader == 1) {
                                        if (it.itemId == dineInItem.item?.itemId && it.orderItemId == dineInItem.item?.orderItemId) {
                                            dineInItem.item?.guestItemId = it.id
                                        }
                                    } else {
                                        if (dineInItem.title == baseResponse.order.guestAttributes[i].name) {
                                            dineInItem.itemsCount = guestItemsList.size
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "2255 dineinlisttest setList: " + newList as ArrayList<DineInModel>)
                    dineInTableAdapter.setList(
                        newList as ArrayList<DineInModel>
                    )
                }
            }
        }
    }

    private fun removeGuestObserver() {
        viewModel.removeGuestSuccess.observe(viewLifecycleOwner) { event ->
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), event.getContentIfNotHandled().toString()
            ) { _, _ -> }
        }
    }

    private fun wastageItemObserver() {
        viewModel.wastageItemsSuccess.observe(viewLifecycleOwner) { event ->
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), event.getContentIfNotHandled().toString()
            ) { _, _ ->
                viewModel.Basedata.removeObservers(viewLifecycleOwner)
                navigateDineInOrderNew(true)
                orderId?.let { viewModel.apiCallOrderDetails(it) }
            }
        }
    }

    private fun navigateDineInOrderNew(isFromWastage: Boolean = false) {

        viewModel.Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
                    Log.d("###17MAR23", "Basedata.observe: Called - Start")
                    passDiscountTotal = baseResponse.totalDiscount
                    passSCTotal = baseResponse.totalServiceCharges
                    wholeTableDiscount = 0.0
                    order_note = baseResponse.note
                    globalOrderDiscount = 0.0
                    //Manan's Code
                    //for Merge Icon
                    if (baseResponse.floorPlanTable.status == MERGEDANDOCCUPIED) {
                        binding.imgMergeTable.visibility = View.VISIBLE
                        binding.imgMergeTable.setImageDrawable(
                            requireContext().resources.getDrawable(
                                R.drawable.ic_unmerge
                            )
                        )
                    } else {
                        binding.imgMergeTable.visibility = View.GONE
                    }

                    //table name,chair for mergedOccupied,single table details in Header
                    if (baseResponse.floorPlanTable.status == MERGEDANDOCCUPIED) {

                        var listTableMerge: ArrayList<String> = arrayListOf()
                        listTableMerge.add(baseResponse.floorPlanTable.tableNumber.toString())
                        baseResponse.floorPlanTable.merged_child_table_details?.forEach {
                            listTableMerge.add(it.table_number.toString())

                        }
                        var txtMergedTbNo = android.text.TextUtils.join(",", listTableMerge)
                        binding.txtTitle.text = "Table " + txtMergedTbNo

                    } else {
                        binding.txtTitle.text = baseResponse.floorPlanTable.tableName
                    }

                    getOrderDetailsResponse = baseResponse
                    var subTotalWT: Double = 0.0
                    var totalTaxWT: Double = 0.0
                    var serviceChargeWT: Double = 0.0
                    var totalPriceWT: Double = 0.0
                    var guestShareTotal: Double = 0.0
                    var isPaid = true
                    var isAllFired = true
                    var noItem = true

                    //Calculation for Whole Table Price and add dvide guest share

                    var dineInList: ArrayList<DineInModel> = arrayListOf()
                    var totalSubTotal: Double = 0.0
                    var totalTaxAmount: Double = 0.0
                    var totalServiceChargeAmount: Double = 0.0
                    var totalFinalAmount: Double = 0.0
                    var totalCashDiscount: Double = 0.0
                    var totalItemDiscount: Double = 0.0
                    var eligibleGuestsForDivision = 0

                    //extract logic from API data and drag & drop code
                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val model = DineInModel()
                        var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                        model.title = baseResponse.guestAttributes.get(i).name
                        model.isHeader = 0
                        model.id = baseResponse.guestAttributes[i].id
                        model.isPaid = baseResponse.guestAttributes.get(i).isPaid
                        model.itemsCount =
                            baseResponse.guestAttributes.get(i).guestItemAttributes.size

                        // guest item sorting
                        for (p in 0 until baseResponse.guestAttributes[i].guestItemAttributes.size) {
                            baseResponse.orderItems.forEach { orderItem ->
                                if (orderItem.itemId == baseResponse.guestAttributes[i].guestItemAttributes[p].itemId) {
                                    baseResponse.guestAttributes[i].guestItemAttributes[p].sort =
                                        orderItem.sort
                                }
                            }
                        }
                        guestItem =
                            baseResponse.guestAttributes[i].guestItemAttributes.sortedBy { it.sort }

                        if (baseResponse.guestAttributes.get(i).guestItemAttributes.isNotEmpty()) {
                            if (baseResponse.guestAttributes.get(i).name.trim()
                                    .lowercase() != "Whole Table".trim().lowercase()
                            ) {
                                totalGuestCount++
                            }
                            if (baseResponse.guestAttributes.get(i).name.trim()
                                    .lowercase() != "Whole Table".trim()
                                    .lowercase() && baseResponse.guestAttributes[i].guestItemAttributes.isNotEmpty()
                            ) {
                                eligibleGuestsForDivision++
                            }

                        }
                        model.serviceChargeList = serviceChargeList


                        if (baseResponse?.guestAttributes?.get(i)?.customerId != null && baseResponse.guestAttributes.get(
                                i
                            ).customerId != 0
                        ) {
                            allCustomerList.forEach {
                                if (it.id == baseResponse.guestAttributes.get(i).customerId) {
                                    model.customer = it
                                }
                            }

                        }
                        // Applied sort value to headers
                        model.sort = dineInList.size
                        dineInList.add(model)

                        var guestSubTotal: Double = 0.0
                        var guestTotalTax: Double = 0.0
                        var guestServiceCharge: Double = 0.0


                        for (j in guestItem.indices) {
                            if (baseResponse.orderItems.isNotEmpty()) {
                                var sortedItems = baseResponse.orderItems.sortedBy { it.sort }
                                sortedItems.forEach {
                                    if (guestItem[j].timestamp != null) {
                                        if (it.timestamp.trim()
                                                .lowercase() == guestItem[j].timestamp.trim()
                                                .lowercase()
                                        ) {
//                                        val guestAttr = baseResponse.guestAttributes.get(j)
                                            //Whole Table Calculation
                                            totalItemDiscount += it.discountAmount

                                            /*     if (baseResponse.guestAttributes.get(i).isPaid) {
                                                 notPayAnyAmount = true
                                             }*/

                                            //for add item in tbItem List and extract/convert data from API
                                            val itemDineIn: DineInModel = DineInModel()
                                            val item = TbCartItem()
                                            item.isPaid = it.isPaid
                                            item.discountPrice = it.discountAmount
                                            item.discountId = it.discountId
                                            item.discountType = it.discountType.toString()

                                            item.name = it.itemName
                                            item.itemId = it.itemId
                                            item.categoryId = it.categoryId
                                            item.guestItemId = guestItem[j].id

                                            var listTaxes: ArrayList<TaxData> = arrayListOf()
                                            it.orderItemTaxes.forEach {
                                                listTaxes.add(
                                                    TaxData(
                                                        createdAt = it.createdAt,
                                                        id = it.id,
                                                        locationId = prefProvider.getValueInt(
                                                            LOCATION_ID,
                                                            0
                                                        ),
                                                        name = it.name,
                                                        rate = it.rate,
                                                        taxType = it.taxType,
                                                        updatedAt = it.updatedAt,
                                                        isActive = true,
                                                        isDefault = it.isDefault,
                                                        isCustomAmount = false,
                                                        itemPricing = "",
                                                        itemIds = arrayListOf(),
                                                        orderTaxId = it.taxId
                                                    )
                                                )
                                            }
                                            item.taxes = listTaxes
                                            if (it.orderItemModifiers.isNotEmpty()) {
                                                var modifiers: ArrayList<Modifier> = arrayListOf()
                                                it.orderItemModifiers.forEach { mod ->
                                                    val model = Modifier()
                                                    model.id = mod.modifierId
                                                    model.itemQuantity = mod.quantity
                                                    model.name = mod.name
                                                    model.orderModifierId = mod.id
                                                    model.price = mod.price
                                                    model.modifierSetId = mod.modifier_set_id
                                                    model.modifier_quantity =
                                                        mod.modifier_quantity!!


                                                    if (mod.orderItemTaxes.isNotEmpty()) {
                                                        model.orderItemTaxes = mod.orderItemTaxes
                                                    }

                                                    modifiers.add(model)


                                                }
                                                item.modifiers = modifiers

                                            }
                                            item.price = it.price
                                            item.itemQuantity = it.quantity
                                            item.orderItemId = it.id
                                            item.note = it.note
                                            item.isFired = guestItem.get(j).is_fired
                                            // Applied sort to dineInItem
                                            item.sort = if (item.dineInSort == 0) {
                                                dineInList.size
                                            } else {
                                                it.sort
                                            }
                                            item.dineInSort = item.sort
                                            item.timeStamp = it.timestamp
                                            if (it.orderItemModifiers.isNotEmpty()) {
                                                item.modifier_set_ids =
                                                    modifiersIds(it.orderItemModifiers)
                                            }
                                            if (it.order_item_variation != null) {
                                                item.variationsAttributes =
                                                    variationAtt(it.order_item_variation!!)
                                            }
                                            itemDineIn.isHeader = 1
                                            itemDineIn.item = item
                                            itemDineIn.sort = item.sort
                                            itemDineIn.empName =
                                                baseResponse.floorPlanTable.lockByName.toString()

                                            dineInList.add(itemDineIn)


                                            if (!it.isPaid) {

                                                totalSubTotal += (it.quantity * it.price) - it.discountAmount
                                                if (it.orderItemModifiers.isNotEmpty()) {
                                                    it.orderItemModifiers.forEach { mod ->
                                                        totalSubTotal += mod.price * mod.quantity

                                                    }
                                                }

                                                if (it.orderItemTaxes.isNotEmpty()) {
                                                    it.orderItemTaxes.forEach { tax ->
                                                        if (!it.isPaid) {
                                                            totalTaxAmount += if (tax.taxType == "Percentage") {

                                                                var modifierPrice = 0.0
                                                                val price =
                                                                    (it.price * it.quantity) - it.discountAmount

                                                                it.orderItemModifiers.forEach {
                                                                    modifierPrice += (it.price * it.quantity)
                                                                }

                                                                val totalPrice =
                                                                    price + modifierPrice

                                                                val itemTaxPrice =
                                                                    (tax.rate * totalPrice) / 100


                                                                itemTaxPrice
                                                                // MethodUtils.getTwoDecimal(itemTaxPrice)
                                                                /* String.format("%.2f", itemTaxPrice)
                                                                     .toDouble()*/


                                                            } else {
                                                                //   MethodUtils.getTwoDecimal(tax.rate * it.quantity)
                                                                tax.rate * it.quantity


                                                            }


                                                        }
                                                    }

                                                }
                                            }

                                            if (!it.isPaid) {
                                                isPaid = it.isPaid
                                            }
                                            if (!it.isFired) {
                                                isAllFired = false
                                            }


                                        }
                                    }

                                }

                            }
                        }


                    }
                    if (eligibleGuestsForDivision == 0) {
                        eligibleGuestsForDivision = 1
                    }


                    for (k in 0 until baseResponse.guestAttributes.size) {
                        val obj = baseResponse.guestAttributes.get(k)
                        if (baseResponse.guestAttributes.get(k).isPaid) {
                            notPayAnyAmount = true
                        }

                        obj.guestItemAttributes.forEach {
                            baseResponse.orderItems.forEach { oi ->
                                if (it.timestamp.trim().lowercase() == oi.timestamp.trim()
                                        .lowercase()
                                ) {
                                    if (obj.name.trim()
                                            .lowercase() == "Whole Table".trim().lowercase()
                                    ) {

                                        wholeTableDiscount += oi.discountAmount

                                        if (!oi.isPaid) {

                                            subTotalWT += (oi.quantity * oi.price) - oi.discountAmount
                                            if (oi.orderItemModifiers.isNotEmpty()) {
                                                oi.orderItemModifiers.forEach { mod ->
                                                    subTotalWT += mod.price * mod.quantity

                                                }
                                            }

                                        }


                                        if (oi.orderItemTaxes.isNotEmpty()) {
                                            oi.orderItemTaxes.forEach { tax ->
                                                var modifierPrice = 0.0
                                                val price =
                                                    (oi.price * oi.quantity)

                                                oi.orderItemModifiers.forEach { mod ->
                                                    modifierPrice += (mod.price * mod.quantity)
                                                }

                                                val totalPrice =
                                                    price + modifierPrice - oi.discountAmount

                                                if (!oi.isPaid) {
                                                    totalTaxWT += if (tax.taxType == "Percentage") {
                                                        val itemTaxPrice =
                                                            (tax.rate * totalPrice) / 100

                                                        itemTaxPrice

                                                        /*  String.format("%.2f", itemTaxPrice)
                                                              .toDouble()*/
                                                    } else {
                                                        if (totalPrice <= 0.0) {
                                                            String.format("%.2f", 0.00)
                                                                .toDouble()
                                                        } else {
                                                            tax.rate * oi.quantity
                                                            /*String.format(
                                                                "%.2f",
                                                                tax.rate * oi.quantity
                                                            )
                                                                .toDouble()*/
                                                        }
                                                    }

                                                }
                                            }
                                            totalTaxWT =
                                                String.format("%.2f", totalTaxWT).toDouble()

                                        }
                                        serviceChargeWT = 0.0
                                        if (prefProvider.getValueboolean(
                                                Constants.SERVICECHARGE_DINEIN_ORDER,
                                                false
                                            )
                                        ) {
                                            var isApplied = false
                                            serviceChargeList.forEach {
                                                if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                                    if (isInRange(
                                                            it.min_guest_count!!,
                                                            it.max_guest_count!!,
                                                            baseResponse.guestAttributes.size - 1
                                                        )
                                                    ) {
                                                        isApplied = true
                                                        serviceChargeWT += (subTotalWT * it.percentage) / 100
                                                        return@forEach
                                                    }
                                                }
                                            }
                                            if (!isApplied) {
                                                serviceChargeList.forEach { service ->
                                                    if (service.id == checkMaxGuestCountId(
                                                            serviceChargeList
                                                        )
                                                    ) {
                                                        serviceChargeWT += (subTotalWT * service.percentage) / 100
                                                        return@forEach
                                                    }
                                                }
                                            }
                                        }

                                        totalPriceWT = subTotalWT + totalTaxWT + serviceChargeWT

                                        guestShareTotal =
                                            totalPriceWT / eligibleGuestsForDivision

                                    }

                                }

                            }


                        }


                    }


                    var orderDiscount = 0.0
                    var newLocalDiscountCal = 0.0
                    if (baseResponse.totalDiscount - totalItemDiscount > 0) {
                        orderDiscount = baseResponse.totalDiscount - totalItemDiscount
                        globalOrderDiscount = baseResponse.totalDiscount - totalItemDiscount
                        update_order_Discount = baseResponse.totalDiscount - totalItemDiscount
                    }
                    totalServiceChargeAmount = 0.0
                    if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                        var isApplied = false
                        serviceChargeList.forEach {
                            if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                if (isInRange(
                                        it.min_guest_count!!,
                                        it.max_guest_count!!,
                                        eligibleGuestsForDivision
                                    )
                                ) {

                                    isApplied = true
                                    totalServiceChargeAmount += (totalSubTotal * it.percentage) / 100
                                    return@forEach
                                }
                            }
                        }
                        if (!isApplied) {
                            serviceChargeList.forEach { service ->
                                if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                                    totalServiceChargeAmount += (totalSubTotal * service.percentage) / 100
                                    return@forEach
                                }
                            }
                        }
                    }


                    totalSubTotal = MethodUtils.getTwoDecimal(totalSubTotal)
                    totalTaxAmount = MethodUtils.getTwoDecimal(totalTaxAmount)
                    totalServiceChargeAmount = MethodUtils.getTwoDecimal(totalServiceChargeAmount)


                    totalDiscountWO = baseResponse.totalDiscount
                    totalDiscountWO -= totalItemDiscount
                    val finalAmount =
                        totalSubTotal + totalTaxAmount + totalServiceChargeAmount - orderDiscount

                    // Convert Order discount into percentage to divide in guest (To resolve minus guest amount issue)
                    if (dineInList.isNotEmpty()) {
                        dineInList[0].orderDiscountPercentage = MethodUtils.roundOffAmountDouble(
                            MethodUtils.calculatePercentageFromAmount(
                                totalDiscountWO,
                                (baseResponse.subTotal + totalDiscountWO)
                            )
                        )
                        dineInList.get(0).guestDividedAmt =
                            guestShareTotal
                        dineInList.get(0).totalGuestCount = baseResponse.guestAttributes.size - 1
                        dineInList.get(0).eligibleGuestsForDivision = eligibleGuestsForDivision
                        dineInList.get(0).wholeTableSubTotal =
                            subTotalWT / eligibleGuestsForDivision
                        dineInList.get(0).wholeTableTax =
                            totalTaxWT / eligibleGuestsForDivision
                        dineInList.get(0).wholeTableSurTax =
                            serviceChargeWT / eligibleGuestsForDivision
                        dineInList.get(0).wholeTableDiscont =
                            MethodUtils.roundOffAmountDouble(wholeTableDiscount / eligibleGuestsForDivision)

                        dineInList.get(0).orderDiscount = orderDiscount
                        dineInList.get(0).orderTotalAmount =
                            MethodUtils.roundOffAmountDouble(baseResponse.subTotal + baseResponse.totalTaxAmount + baseResponse.totalServiceCharges)
                    }



                    viewModel.totalTaxAmount = totalTaxAmount
                    subTotalDInin = totalSubTotal - orderDiscount
                    serviceCharge = totalServiceChargeAmount
                    totalDiscount = orderDiscount + totalItemDiscount
                    finalTaxAmt = totalTaxAmount
                    binding.txtTotalAmountNew.text = MethodUtils.roundOffAmount(
                        finalAmount
                    )
                    toFinalAmt = finalAmount


                    var paidGuestCount = 0

                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val obj = baseResponse.guestAttributes.get(i)

                        if (obj.isPaid) {
                            paidGuestCount++
                        }
                    }
                    /*    for (i in 0 until dineInList.size) {
                            if (i != 0 && dineInList.size > i + 1) {
                                if (dineInList.get(i + 1).item != null && dineInList.get(i + 1).item?.isPaid == true) {
                                    paidGuestCount++
                                }
                            }
                        }*/
                    if (paidGuestCount > 0) {

                        paidGuestAmount = paidGuestCount

                        if (paidGuestCount > 0) {
                            paidGuestAmount = paidGuestCount
                            var perGTotal =
                                subTotalWT / eligibleGuestsForDivision
                            LogUtil.logE(TAG, "perGTotal:  ${perGTotal}")
                            subTotalDInin = totalSubTotal - (perGTotal * paidGuestCount)
                        } else {
                            subTotalDInin = totalSubTotal
                        }

                        //subTotalDInin -= baseResponse.totalDiscount


                        var tempServicecharge = 0.0
                        if (paidGuestCount > 0) {
                            if (prefProvider.getValueboolean(
                                    Constants.SERVICECHARGE_DINEIN_ORDER,
                                    false
                                )
                            ) {
                                var isApplied = false
                                serviceChargeList.forEach {
                                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                        if (isInRange(
                                                it.min_guest_count!!,
                                                it.max_guest_count!!,
                                                eligibleGuestsForDivision
                                            )
                                        ) {
                                            isApplied = true
                                            tempServicecharge += (subTotalDInin * it.percentage) / 100
                                            return@forEach
                                        }
                                    }

                                }
                                if (!isApplied) {
                                    serviceChargeList.forEach { service ->
                                        if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                                            tempServicecharge += (subTotalDInin * service.percentage) / 100
                                            return@forEach
                                        }
                                    }
                                }


                            }
                            serviceCharge = tempServicecharge

                        }

                        var unpaidCount = eligibleGuestsForDivision - paidGuestCount

                        if (paidGuestCount > 0) {
                            var tempTax = totalTaxWT / eligibleGuestsForDivision

                            var guestTax = totalTaxAmount - totalTaxWT
                            finalTaxAmt = (tempTax * unpaidCount) + guestTax
                            //finalTaxAmt = (subTotalWT / totalGuestCount) * unpaidCount + totalTaxAmt
                        }
                        var paidGuestorderDis = orderDiscount
//                        var perGuestorderDis =
//                            orderDiscount / (baseResponse.guestAttributes.size - 1)
//                        var orderDis = orderDiscount - (perGuestorderDis * paidGuestCount)
                        var orderDis = 0.0
                        for (payment in baseResponse.payments) {
                            paidGuestorderDis -= payment.totalDiscount
                        }
                        orderDis = paidGuestorderDis


                        var finalAmount = subTotalDInin + serviceCharge + finalTaxAmt - orderDis
                        binding.txtTotalAmountNew.text = MethodUtils.roundOffAmount(
                            finalAmount
                        )

                        toFinalAmt = finalAmount


                    }

                    if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {

                        cashDiscountGlobal = MethodUtils.calculateCashDiscount(
                            toFinalAmt,
                            prefProvider,
                            requireContext()
                        )

                    }

                    // Sorted list after adding all items
                    dineInList.sortBy { it.sort }
                    if (dineInList.isNotEmpty()) {
                        Log.d("###17MAR23", "dineInList.isNotEmpty(): Called - Start")
                        Log.d(TAG, "2909 dineinlisttest setList: " + dineInList)
                        dineInTableAdapter.setList(dineInList, notPayAnyAmount)
                        if (this::presentation.isInitialized) {
                            presentation.show()
                            presentation.onDisplayChanged()
                            presentation.showTableDetails(baseResponse)
                        }
                        if (!isFromWastage) {
                            checkForAutoFire(true)
                        }

                        //  binding.txtTotalAmountNew.setText("${MethodUtils.roundOffAmount(totalAmtnew)}")

                        if (!notPayAnyAmount) {
                            touchHelper.attachToRecyclerView(binding.rvItemList)
                        }
                    }


                    if (notPayAnyAmount) {
                        binding.txtAddguest.visibility = View.GONE
                        binding.txtEditOrder.visibility = View.GONE
                    } else {
                        binding.txtAddguest.visibility = View.VISIBLE
                        binding.txtEditOrder.visibility = View.VISIBLE
                    }


                }


            }


        }

    }


    fun getCartModel(list: ArrayList<DineInModel>): CartModel {
        var model = CartModel()
        var listItem: ArrayList<TbCartItem> = arrayListOf()
        var dineInItems: ArrayList<TbCartItem> = arrayListOf()
        var newDineInList: ArrayList<DineInModel> = arrayListOf()
        var dineinModel: DineInModel = DineInModel()

        for (i in 0 until list.size) {
            if (list[i].isHeader == 1) {
                list.get(i).item?.let {
                    if (it.discountPrice != 0.0) {
                        it.discountPrice =
                            MethodUtils.roundOffAmountDouble(it.discountPrice / it.itemQuantity)
                    }
                    listItem.add(it)
                }
            }

            if (list[i].isHeader == 0) {
                dineinModel = list[i]
                var starPos = i + 1

                for (j in starPos until list.size) {

                    if (list[j].isHeader == 1) {
                        dineInItems.add(list[j].item!!)

                        if (j == (list.size - 1)) {
                            dineinModel.items = arrayListOf()
//                            dineinModel.items.addAll(dineInItems)
                            dashboardViewModel.currentCartItems = arrayListOf()
                            dashboardViewModel.currentCartItems.addAll(dineInItems)
                            dineInItems = arrayListOf()
                            break
                        }
                    } else {
                        dineinModel.items = arrayListOf()
//                        dineinModel.items.addAll(dineInItems)
                        dashboardViewModel.currentCartItems = arrayListOf()
                        dashboardViewModel.currentCartItems.addAll(dineInItems)
                        dineInItems = arrayListOf()
                        break
                    }


                }

                newDineInList.add(dineinModel)
            }

        }
        model.orderType = "DineIn"
        model.dineInList = newDineInList
//        model.items = listItem
        dashboardViewModel.currentCartItems = listItem
        model.discountPrice = list[0].orderDiscount
        model.serviceCharge = serviceChargeList
        model.employeeID = prefProvider.getValueInt(EMPLOYEE_ID, 0)
        model.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
        model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
        model.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
        model.orderTypeName = prefProvider.getValue(ORDER_TYPE_NAME, DINE_IN)

        return model
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

    private fun updateAdapterData() {
        var oldList = dineInTableAdapter.getList() as ArrayList<DineInModel>
        var newList: ArrayList<DineInModel> = arrayListOf()
        var wholeTableAmt = 0.0
        var WTTax = 0.0
        var WTServiceCharge = 0.0
        var totalPaid = 0.0
        var guestShare = 0.0
        var totalTablePrice = 0.0
        var WTDiscount = 0.0
        var guestCount = 0
        var eligibleGuestsForDivision = 0
        if (dragTo != -1) {
            oldList.get(dragTo).item?.guestItemId?.let { listOfMoveItemIds.add(it) }
            oldList.get(dragTo).item?.guestItemId = null

            if (oldList[dragTo].item?.itemId != 1) {
                // To Check if similar item exist below moved item in current guest list, if exist then merge item
                for (k in dragTo + 1 until oldList.size) {
                    if (oldList[k].isHeader == 0) {
                        break
                    }
                    if (oldList[k].isHeader == 1) {
                        if (oldList[dragTo].item?.itemId == oldList[k].item?.itemId) {
                            if (checkVariation(oldList[dragTo].item!!, oldList[dragTo].item!!) &&
                                checkModifierNewLogic(oldList[k].item!!, oldList[dragTo].item!!)
                            ) {
                                var updatedQuantity: Int =
                                    oldList[dragTo].item?.itemQuantity!! + oldList[k].item?.itemQuantity!!
                                oldList[dragTo].item = oldList[k].item
                                oldList[dragTo].item?.itemQuantity = updatedQuantity
                                oldList[dragTo].item?.sort = dragTo
                                oldList.remove(oldList[k])
                                break
                            }
                        }
                    }
                }

                // To Check if similar item exist above moved item in current guest list, if exist then merge item
                for (l in dragTo - 1 downTo 0) {
                    if (oldList[l].isHeader == 0) {
                        break
                    }
                    if (oldList[l].isHeader == 1) {
                        if (oldList[dragTo].item?.itemId == oldList[l].item?.itemId) {
                            if (checkVariation(oldList[l].item!!, oldList[dragTo].item!!) &&
                                checkModifierNewLogic(oldList[l].item!!, oldList[dragTo].item!!)
                            ) {
                                var updatedQuantity: Int =
                                    oldList[dragTo].item?.itemQuantity!! + oldList[l].item?.itemQuantity!!
                                oldList[dragTo].item = oldList[l].item
                                oldList[dragTo].item?.itemQuantity = updatedQuantity
                                oldList[dragTo].item?.sort = dragTo
                                oldList.remove(oldList[l])
                                break
                            }
                        }
                    }
                }
            }

            dragFrom = -1
            dragTo = -1

            // Update guest wise itemsCount for all guest after reordering to manage eligible guests division
            var currentGuestIndex = 0
            for (m in 0 until oldList.size) {
                if (oldList[m].isHeader == 0) {
                    currentGuestIndex = m
                    oldList[currentGuestIndex].itemsCount = 0
                }
                if (oldList[m].isHeader == 1) {
                    oldList[currentGuestIndex].itemsCount++
                }
            }

            for (i in 0 until oldList.size) {
                if (oldList.get(i).isHeader == 1) {
                    if (oldList.get(i).item?.isPaid == true) {
                        oldList.get(i).item?.let {
                            totalPaid += (it.price * it.itemQuantity) - it.discountPrice
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach {
                                    totalPaid += it.price * it.itemQuantity
                                }
                            }
                        }

                    } else {

                        oldList.get(i).item?.let {
                            totalTablePrice += (it.price * it.itemQuantity) - it.discountPrice
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach {
                                    totalTablePrice += it.price * it.itemQuantity
                                }
                            }
                        }

                    }
                } else {
                    guestCount++
                    if (oldList[i].itemsCount > 0 && oldList[i].title != "Whole Table") {
                        eligibleGuestsForDivision++
                    }
                }


            }

            if (eligibleGuestsForDivision == 0) {
                eligibleGuestsForDivision = 1
            }


            for (i in 1 until oldList.size) {
                if (oldList.get(i).isHeader == 1) {
                    oldList.get(i).item?.let { it ->
                        WTDiscount += it.discountPrice
                        wholeTableAmt += (it.price * it.itemQuantity) - it.discountPrice

                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {
                                wholeTableAmt += it.price * it.itemQuantity

                            }
                        }
                        if (it.taxes?.isNotEmpty() == true) {
                            it.taxes?.forEach { tax ->
                                if (tax.isActive) {
                                    WTTax += if (tax.taxType == "Percentage") {

                                        var modifierPrice = 0.0
                                        val price =
                                            (it.price * it.itemQuantity) - it.discountPrice

                                        it.modifiers.forEach {
                                            modifierPrice += (it.price * it.itemQuantity)
                                        }

                                        val totalPrice = price + modifierPrice

                                        val itemTaxPrice =
                                            (tax.rate * totalPrice) / 100
                                        MethodUtils.getTwoDecimal(itemTaxPrice)
                                        /* String.format("%.2f", itemTaxPrice)
                                         .toDouble()*/
                                    } else {
                                        MethodUtils.getTwoDecimal(tax.rate * it.itemQuantity)

                                        /* String.format(
                                         "%.2f",
                                         tax.rate * it.itemQuantity
                                     )
                                         .toDouble()*/
                                    }
                                }
                            }
                        }
                    }


                } else {

                    break
                }

            }
            if (serviceChargeList.isNotEmpty() == true) {
                var isApplied = false
                serviceChargeList.forEach {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_DINEIN_ORDER,
                            false
                        )
                    ) {
                        if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                            if (isInRange(
                                    it.min_guest_count!!,
                                    it.max_guest_count!!,
                                    (eligibleGuestsForDivision)
                                )
                            ) {
                                isApplied = true

                                WTServiceCharge += (wholeTableAmt * it.percentage) / 100
                                return@forEach
                            }
                        }
                    }
                }
                if (!isApplied) {
                    serviceChargeList.forEach { service ->
                        if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                            WTServiceCharge += (wholeTableAmt * service.percentage) / 100
                            return@forEach
                        }
                    }
                }

            }

            guestShare = (wholeTableAmt + WTServiceCharge + WTTax) / (eligibleGuestsForDivision)


            for (i in 0 until oldList.size) {
                var model = DineInModel()
                if (oldList.get(i).isHeader == 1) {
                    model.item = oldList.get(i).item
                    model.totalTableAmt = totalTablePrice


                } else {
                    model.title = oldList.get(i).title
                    model.customer = oldList.get(i).customer

                    model.guestDividedAmt = guestShare

                    model.id = oldList.get(i).id
                }
                model.isHeader = oldList.get(i).isHeader
                newList.add(model)

            }
            var guestAmt = 0.0

            for (i in 0 until newList.size) {
                if (newList.get(i).isHeader == 1) {
                    newList.get(i).item?.let { it ->
                        guestAmt += (it.price * it.itemQuantity) - it.discountPrice
                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {
                                guestAmt += it.itemQuantity * it.price
                            }
                        }

                    }
                } else {
                    newList.get(i).totalGuestPrice = guestAmt + guestShare
                    guestAmt = 0.0
                }
            }
            newList.get(0).guestDividedAmt = guestShare
            newList.get(0).totalGuestCount = guestCount - 1
            newList.get(0).eligibleGuestsForDivision = eligibleGuestsForDivision
            newList.get(0).wholeTableSubTotal =
                wholeTableAmt / (eligibleGuestsForDivision)
            newList.get(0).wholeTableTax =
                WTTax / (eligibleGuestsForDivision)
            newList.get(0).wholeTableSurTax =
                WTServiceCharge / (eligibleGuestsForDivision)
            newList.get(0).orderDiscount = getOrderDetailsResponse?.totalDiscount ?: 0.0
            // Convert Order discount into percentage to divide in guest (To resolve minus guest amount issue)
            newList[0].orderDiscountPercentage = MethodUtils.roundOffAmountDouble(
                MethodUtils.calculatePercentageFromAmount(
                    totalDiscountWO,
                    (getOrderDetailsResponse?.subTotal ?: 0.0) + totalDiscountWO
                )
            )
            newList.get(0).orderTotalAmount = subTotalDInin
            totalGuestCount = eligibleGuestsForDivision

            newList[0].listOfItemsMoved.addAll(listOfMoveItemIds.toCollection(arrayListOf()))

            Log.d(TAG, "3297 dineinlisttest setList: " + newList)
            dineInTableAdapter.setList(
                newList
            )

            updateOrderCall(isFromReorder = true)
        }

    }

    private fun updateOrderCall(isFromReorder: Boolean) {
        cartList = getCartModel(dineInTableAdapter.getList().toCollection(arrayListOf()))
        cartList?.listOfItemRemoved = listOfMoveItemIds
        prefProvider.setValueboolean(DINE_IN_UPDATE, true)
        // RESET Data after coming back from checkout screen by clicking on guest pay (to resolve calculation issue for guest division)
        dashboardViewModel.totalDiscount = getOrderDetailsResponse?.totalDiscount ?: 0.0
        dashboardViewModel.subTotalPrice = getOrderDetailsResponse?.subTotal ?: 0.0
        // END RESET
        val orderRequestModel = dashboardViewModel.updateOrder(cartList!!)
        orderId?.let {
            viewModel.updateOrder(
                it,
                orderRequestModel,
                isFromReorder,
                message = "removed"
            )
        }
        listOfMoveItemIds.clear()
    }

    // Navigate to Add Item to Wastage dialog
    override fun onAddToWastage(position: Int, item: TbCartItem) {
        val bundle = Bundle().apply {
            putInt("itemQuantity", item.itemQuantity)
        }
        clickedPosition = position
        findNavController().navigate(
            R.id.action_dineInOrderTable_to_addItemToWastageDialog,
            bundle
        )
    }

    // Handle wastage item quantity and send data to server
    private fun addItemToWastageResultListener() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_add_to_wastage", viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            val itemQuantity: Int = bundle.getInt("itemQuantity", 0)
            var wastageReason: VenueDetailsResponse.Data.WastageReason? = null
            if (bundle.containsKey("wastageReason")) {
                wastageReason =
                    bundle.getParcelable<VenueDetailsResponse.Data.WastageReason>("wastageReason") as VenueDetailsResponse.Data.WastageReason
            }
            val wastageNote: String = bundle.getString("wastageNote", "")
            if (clickedPosition != -1 && dineInTableAdapter.getList()[clickedPosition].item != null) {
                prefProvider.setValueboolean(DINE_IN_UPDATE, true)
                val wastageRequest = WastageItemRequest.WastageItem(
                    orderId = orderId ?: -1,
                    tableNo = getOrderDetailsResponse?.floorPlanTable?.tableNumber,
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0),
                    itemQuantity = itemQuantity,
                    itemName = dineInTableAdapter.getList()[clickedPosition].item?.name,
                    wastageReasonId = if (wastageReason == null) {
                        null
                    } else {
                        wastageReason.id
                    },
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, -1),
                    wastageNote = wastageNote,
                    orderItemId = dineInTableAdapter.getList()[clickedPosition].item?.orderItemId,
                    wastageItemModifiersAttributes = dashboardViewModel.orderItemModifierAttributes(
                        dineInTableAdapter.getList()[clickedPosition].item!!,
                        prefProvider.getValueInt(TERMINAL_ID, -1)
                    )
                )
                val wastageItemRequest = WastageItemRequest(wastageRequest)
                viewModel.wastageItemApiCall(wastageItemRequest)
            }
        }
    }

    val touchHelper =
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (target.layoutPosition != 0 && dineInTableAdapter.getList()
                        .get(viewHolder.layoutPosition).isHeader != 0
                ) {
                    val oldPos = viewHolder.layoutPosition
                    val newPos = target.layoutPosition

                    if (dragFrom == -1) {
                        dragFrom = oldPos
                    }
                    dragTo = newPos

                    dineInTableAdapter.onItemMove(
                        viewHolder.layoutPosition,
                        target.layoutPosition
                    )
                    return true
                } else {
                    return false
                }

            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //updateAdapterData()


            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    /* reallyMoved(
                         adapter.getItem(dragFrom).sort,
                         adapter.getItem(dragTo).sort,
                         adapter.getItem(viewHolder.layoutPosition).id
                     )*/
                    updateAdapterData()
                }
            }
        })

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = it
            }
        }

    }

    private fun getCustomerPrinterList() {
        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerList = it.data
                    }
                }

                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }
        }
    }

    private fun getCustomerPrinters(paymentType: String) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerList = it.data

                        customerList.forEach {
                            if (it.status) {
                                initPrinter(
                                    it,
                                    Constants.CUSTOMER,
                                    paymentType,
                                    false,
                                    arrayListOf(),
                                    "",
                                    arrayListOf()
                                )
                            }


                        }


                    }


                }

                Status.ERROR -> {

                    ProgressUtils.dismissProgressDialog()

                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        }

    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentStatus: String,
        guestPrint: Boolean,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {


        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, customerReceiptPrinters.ipAddress)

            showStaticLoader()

            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                        }

                        override fun onConnect() {
                            println("onConnect")

                            if (guestPrint && getOrderDetailsResponse?.guestAttributes?.size!! > 2) {
                                generateGuestPrintSunmi(
                                    customerReceiptPrinters,
                                    type,
                                    paymentStatus,
                                    listGuestItem,
                                    guestName,
                                    listWTitems,
                                    subTotalGuest,
                                    total,
                                    taxGuest,
                                    serviceChargeGuest,
                                    divideDiscount,
                                )

                            } else {
                                // step 1
                                generatePrintSunmi(customerReceiptPrinters, type, paymentStatus)
                            }

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                if (SunmiPrinterApi.getInstance().isConnected) {
                    if (guestPrint && getOrderDetailsResponse?.guestAttributes?.size!! > 2) {

                        generateGuestPrintSunmi(
                            customerReceiptPrinters,
                            type,
                            paymentStatus,
                            listGuestItem,
                            guestName,
                            listWTitems,
                            subTotalGuest,
                            total,
                            taxGuest,
                            serviceChargeGuest,
                            divideDiscount,
                        )

                    } else {
                        generatePrintSunmi(customerReceiptPrinters, type, paymentStatus)
                    }
                }
            }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {


            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())

            if (guestPrint && getOrderDetailsResponse?.guestAttributes?.size!! > 2) {

                viewLifecycleOwner.lifecycleScope.launch {
                    delay(100)
                    setService2(
                        customerReceiptPrinters,
                        type,
                        paymentStatus,
                        listGuestItem,
                        guestName,
                        listWTitems,
                        subTotalGuest,
                        total,
                        taxGuest,
                        serviceChargeGuest,
                        divideDiscount
                    )
                }

            } else {


                viewLifecycleOwner.lifecycleScope.launch {
                    delay(100)
                    setService1(paymentStatus)
                }


            }


        } else {

            viewLifecycleOwner.lifecycleScope.launch {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    var printer: Print? = Print(requireContext())
                    if (printer != null) {
//                printer.setStatusChangeEventCallback(this)
//                printer.setBatteryStatusChangeEventCallback(this)
                    }

                    val enabled = Print.FALSE

                    try {
                        var interval: Int = 1000
                        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                            interval = PrinterClass.BLUETOOTH_TIMEOUT
                        }
                        printer?.openPrinter(

                            if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                                Print.DEVTYPE_BLUETOOTH
                            } else {
                                Print.DEVTYPE_TCP
                            },
                            customerReceiptPrinters.ipAddress,
                            enabled,
                            1000
                        )
                        //printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return@launch
                    }
                    try {

                        if (printer != null) {
                            PrinterClass.setPrinter(printer)
                            if (guestPrint && getOrderDetailsResponse?.guestAttributes?.size!! > 2) {

                                generateGuestPrint(
                                    customerReceiptPrinters,
                                    type,
                                    paymentStatus,
                                    listGuestItem,
                                    guestName,
                                    listWTitems,
                                    subTotalGuest,
                                    total,
                                    taxGuest,
                                    serviceChargeGuest,
                                    divideDiscount,
                                )

                            } else {
                                generatePrint(customerReceiptPrinters, type, "")
                            }

                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }
            }
        }
    }

    private fun generateGuestPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0

        val guestCount = dineInTableAdapter.getList().size - 1

        listGuestItem.forEach {
            guestSubTotal += (it.price * it.itemQuantity) - it.discountPrice

            it.modifiers.forEach { mod ->
                guestSubTotal += (mod.price * mod.itemQuantity)
            }


            it.taxes?.forEach { tax ->
                if (tax.isActive) {
                    LogUtil.logE(TAG, "getTaxP  ${Gson().toJson(tax)}")
                    guestTaxes += if (tax.taxType == "Percentage") {

                        var modifierPrice = 0.0
                        val price =
                            (it.price * it.itemQuantity) - it.discountPrice

                        it.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice

                        val itemTaxPrice =
                            (tax.rate * totalPrice) / 100

                        MethodUtils.getTwoDecimal(itemTaxPrice)
                        /* String.format("%.2f", itemTaxPrice)
                             .toDouble()*/
                    } else {

                        MethodUtils.getTwoDecimal(tax.rate * it.itemQuantity)
                        /*   String.format("%.2f", tax.rate * it.itemQuantity)
                               .toDouble()*/
                    }
                }

            }
            guestDiscount += it.discountPrice

        }

        guestDiscount += MethodUtils.roundOffAmountDouble(
            globalOrderDiscount / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                1
            ) ?: 1)
        )

        if (prefProvider.getValueboolean(SERVICECHARGE_DINEIN_ORDER, false)) {
            var isApplied = false
            serviceChargeList.forEach {

                if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                    if (isInRange(
                            it.min_guest_count!!,
                            it.max_guest_count!!,
                            guestCount
                        )
                    ) {
                        isApplied = true
                        guestServiceCharge += (guestSubTotal * it.percentage) / 100
                        return@forEach
                    }
                }

            }
            if (!isApplied) {
                serviceChargeList.forEach { service ->
                    if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                        guestServiceCharge += (guestSubTotal * service.percentage) / 100
                        return@forEach
                    }
                }
            }
        }

        var builder: Builder? = null
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )


            if (customerSettingModel.showOrderIdTop) {
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

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(Constants.VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            if (paymentType.isNotEmpty()) {
                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(paymentType + "\n")

            }
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(
                builder,
                prefProvider.getValue(Constants.BUSINESS_NAME, "").toString()
            )
            builder.addFeedLine(1)

            if (customerSettingModel.showVenueAddress) {

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_ADDRESS, "")
                        .toString()
                )
                builder.addFeedLine(1)
            }

            if (customerSettingModel.showVenuePhone) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showWebsiteAddress) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_WEBSITE, "")

                )

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showOrderType) {
                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                builder.addText(getOrderDetailsResponse?.orderTypeName + "\n")
            }

            if (customerSettingModel.fonts == Constants.LARGE) {

                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            LogUtil.logE(TAG, "listWTitemsItemsGet  ${Gson().toJson(listWTitems)}")
            for (i in 0 until listWTitems.size) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                var guestCount: Int =
                    (getOrderDetailsResponse?.guestAttributes?.size?.minus(1)) ?: 1
                if (guestCount < 1) {
                    guestCount = 1
                }


                addWholeTbItemToGuest(
                    builder, listWTitems.get(i), customerSettingModel.fonts,
                    customerSettingModel.showModifiers, guestCount, serviceChargeList,
                    prefProvider
                )
            }


            builder.addFeedLine(1)
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(guestName)


            listGuestItem.forEach {
                addOrderItemForDineIn(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )

            }
            builder.addFeedLine(2)

            if (getOrderDetailsResponse?.totalDiscount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Total Discount",

                        if (divideDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {

                            "-$" + MethodUtils.roundOffAmountString(divideDiscount)

                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )


            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(
                        subTotalGuest
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (guestTaxes != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(taxGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (guestServiceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceChargeGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (!paymentType.equals("Unpaid", true)) {
                if (cashDiscountGlobal > 0) {
                    var cashDis =
                        cashDiscountGlobal / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                            1
                        ) ?: 1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Cash Discount",
                            "-$" + MethodUtils.roundOffAmountString(cashDis),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }


            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)



            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    guestSubTotal + guestTaxes + guestServiceCharge + dineInTableAdapter.getList()
                        .get(0).guestDividedAmt
                )



            builder.addText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(total),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )



            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(total)

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(total)

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }



            if (customerSettingModel.showRefundAmount && !paymentType.equals("Unpaid", true)) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    builder.addText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }

            /* if (getOrderDetailsResponse?.totalTips == 0.0) {
                 builder.addFeedLine(1)
                 builder.addTextLineSpace(30)
                 builder.addFeedUnit(30)

                 builder.addTextFont(Builder.FONT_E)
                 // builder.addTextAlign(Builder.ALIGN_LEFT)
                 builder.addTextLang(Builder.LANG_EN)
                 addCustomerTextSize(builder, customerSettingModel.fonts)
                 builder.addTextStyle(
                     Builder.FALSE,
                     Builder.FALSE,
                     Builder.TRUE,
                     Builder.COLOR_1
                 )

                 var tip = ""

                 if (getOrderDetailsResponse?.totalTips != 0.0) {
                     tip = getOrderDetailsResponse?.totalTips.toString()
                 }
                 builder.addText(
                     padLine(
                         "Tips",
                         if (customerSettingModel.showTipLineForCash) {
                             "_____________"
                         } else {
                             ""
                         },
                         if (customerSettingModel.fonts == Constants.LARGE) {
                             24
                         } else {
                             48
                         }
                     )
                 )
             }*/


            if (customerSettingModel.showTipSuggestion) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Additional Tips",
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        builder,
                        tipsList,

                        MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)
            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )



                builder.addText(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            /*if (customerSettingModel.showCustomerAddress != false or customerSettingModel.showCustomerPhone != false or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {

                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }*/


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("Order Note")
                builder.addFeedLine(1)

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

                builder.addText(getOrderDetailsResponse?.note)
            }

            if (paymentType.equals("paid", ignoreCase = true)) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //customer signature line.
                builder.addText(
                    padLine(
                        "Customer Signature",
                        addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getOrderDetailsResponse?.digitalReceiptUrl.toString())

                val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()
                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateGuestPrintSunmi(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0

        val guestCount = dineInTableAdapter.getList().size - 1

        listGuestItem.forEach {
            guestSubTotal += (it.price * it.itemQuantity) - it.discountPrice

            it.modifiers.forEach { mod ->
                guestSubTotal += (mod.price * mod.itemQuantity)
            }


            it.taxes?.forEach { tax ->
                if (tax.isActive) {
                    LogUtil.logE(TAG, "getTaxP  ${Gson().toJson(tax)}")
                    guestTaxes += if (tax.taxType == "Percentage") {

                        var modifierPrice = 0.0
                        val price =
                            (it.price * it.itemQuantity) - it.discountPrice

                        it.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice

                        val itemTaxPrice =
                            (tax.rate * totalPrice) / 100

                        MethodUtils.getTwoDecimal(itemTaxPrice)
                        /*  String.format("%.2f", itemTaxPrice)
                              .toDouble()*/
                    } else {

                        MethodUtils.getTwoDecimal(tax.rate * it.itemQuantity)
                        /*String.format("%.2f", tax.rate * it.itemQuantity)
                            .toDouble()*/
                    }
                }

            }
            guestDiscount += it.discountPrice

        }

        guestDiscount += MethodUtils.roundOffAmountDouble(
            globalOrderDiscount / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                1
            ) ?: 1)
        )

        serviceChargeList.forEach {
            if (it.isEnabled) {
                guestServiceCharge += (guestSubTotal * it.percentage) / 100
            }
        }

        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            if (customerSettingModel.showOrderIdTop) {

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                printBusinessLogo()
            }

            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.paymentType(paymentType)

            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )
            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(Constants.BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getOrderDetailsResponse?.orderTypeName?.let { PrintSunmiUtils.printOrderType(it) }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.receiptID(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderId(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        PrintSunmiUtils.orderId(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                PrintSunmiUtils.orderId(
                    padLine(
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

                if (customerSettingModel.showTeam) {


                    PrintSunmiUtils.employee(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }
                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.orderTime(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        PrintSunmiUtils.orderTime(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    23
                                } else {
                                    48
                                }
                            ).toString()
                        )

                    }
                }
            }


            PrintSunmiUtils.addHorizontal()

            for (i in 0 until listWTitems.size) {

                var guestCount: Int =
                    (getOrderDetailsResponse?.guestAttributes?.size?.minus(1)) ?: 1
                if (guestCount < 1) {
                    guestCount = 1
                }


                addWholeTbItemToGuest(
                    listWTitems.get(i), customerSettingModel.fonts,
                    customerSettingModel.showModifiers, guestCount, serviceChargeList
                )
            }

            PrintSunmiUtils.printTextCenter(guestName)

            listGuestItem.forEach {
                addOrderItemForDineIn(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )

            }
            SunmiPrinterApi.getInstance().lineWrap(2)

            if (getOrderDetailsResponse?.totalDiscount != null) {

                PrintSunmiUtils.totalDiscount(
                    padLine(
                        "Total Discount",

                        if (guestDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {

                            "-$" + MethodUtils.roundOffAmountString(guestDiscount)

                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }


            PrintSunmiUtils.subTotal(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(
                        subTotalGuest
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (guestTaxes != null) {


                PrintSunmiUtils.tax(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(taxGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (guestServiceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {


                PrintSunmiUtils.serviceCharge(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceChargeGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (!paymentType.equals("Unpaid", true)) {
                if (cashDiscountGlobal > 0) {
                    val cashDis =
                        cashDiscountGlobal / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                            1
                        ) ?: 1)

                    PrintSunmiUtils.cashDiscount(
                        padLine(
                            "Cash Discount",

                            "-$" + MethodUtils.roundOffAmountString(cashDis),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }


            SunmiPrinterApi.getInstance().lineWrap(1)

            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    guestSubTotal + guestTaxes + guestServiceCharge + dineInTableAdapter.getList()
                        .get(0).guestDividedAmt
                )



            PrintSunmiUtils.totalPrice(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(total),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(total - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(total),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(total),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(total + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }






            if (customerSettingModel.showRefundAmount) {

                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    PrintSunmiUtils.totalPrice(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }


            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTips()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipList(
                        tipsList,
                        MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                        customerSettingModel.fonts
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                PrintSunmiUtils.transactionId(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {


                PrintSunmiUtils.transactionType(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getOrderDetailsResponse?.note!!)
                SunmiPrinterApi.getInstance().lineWrap(2)

            }

            if (paymentType.equals("paid", ignoreCase = true)) {
                if (customerSettingModel.fonts == Constants.LARGE) {
                    PrintSunmiUtils.tips("Customer Signature ____")

                } else {
                    PrintSunmiUtils.tips("Customer Signature           __________________")
                }
            }
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showQrCode) {
                PrintSunmiUtils.qrCode(getOrderDetailsResponse?.digitalReceiptUrl.toString())
            }

            SunmiPrinterApi.getInstance().lineWrap(5)
            SunmiPrinterApi.getInstance().cutPaper(1, 1)

            SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateGuestPrintSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0

        val guestCount = dineInTableAdapter.getList().size - 1

        listGuestItem.forEach {
            guestSubTotal += (it.price * it.itemQuantity) - it.discountPrice

            it.modifiers.forEach { mod ->
                guestSubTotal += (mod.price * mod.itemQuantity)
            }


            it.taxes?.forEach { tax ->
                if (tax.isActive) {
                    LogUtil.logE(TAG, "getTaxP  ${Gson().toJson(tax)}")
                    guestTaxes += if (tax.taxType == "Percentage") {

                        var modifierPrice = 0.0
                        val price =
                            (it.price * it.itemQuantity) - it.discountPrice

                        it.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice

                        val itemTaxPrice =
                            (tax.rate * totalPrice) / 100

                        MethodUtils.getTwoDecimal(itemTaxPrice)
                        /*
                                                String.format("%.2f", itemTaxPrice)
                                                    .toDouble()*/
                    } else {

                        MethodUtils.getTwoDecimal(tax.rate * it.itemQuantity)
                        /*   String.format("%.2f", tax.rate * it.itemQuantity)
                               .toDouble()*/
                    }
                }

            }
            guestDiscount += it.discountPrice

        }

        guestDiscount += MethodUtils.roundOffAmountDouble(
            globalOrderDiscount / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                1
            ) ?: 1)
        )

        serviceChargeList.forEach {
            if (it.isEnabled) {
                guestServiceCharge += (guestSubTotal * it.percentage) / 100
            }
        }

        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                PrintSunmiUtils.printLogoInner(
                    prefProvider.getValue(
                        Constants.VENUE_LOGO,
                        ""
                    )
                )
            }

            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.headerText(paymentType)

            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "", prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                )
                /* if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                     Constants.BUSINESS_PHONE_NO,
                     ""
                 ) else ""*/
            )
            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(
                    prefProvider.getValue(
                        Constants.BUSINESS_WEBSITE,
                        ""
                    )
                )
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getOrderDetailsResponse?.orderTypeName?.let { PrintSunmiUtils.headerText(it) }
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                PrintSunmiUtils.normalText(
                    padLine(
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        }, "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

                if (customerSettingModel.showTeam) {


                    PrintSunmiUtils.normalText(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }
                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.normalText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        MethodUtils.formatted()
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    23
                                } else {
                                    48
                                }
                            ).toString()
                        )

                    }
                }
            }


            PrintSunmiUtils.addHorizontalInner()

            for (i in 0 until listWTitems.size) {

                var guestCount: Int =
                    (getOrderDetailsResponse?.guestAttributes?.size?.minus(1)) ?: 1
                if (guestCount < 1) {
                    guestCount = 1
                }


                addWholeTbItemToGuestInner(
                    listWTitems.get(i), customerSettingModel.fonts,
                    customerSettingModel.showModifiers, guestCount, serviceChargeList
                )
            }

            PrintSunmiUtils.normalTextCenter(guestName)

            LogUtil.logE("addDineInInner", "111111111")
            listGuestItem.forEach {
                addOrderItemForDineInInner(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )

            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (getOrderDetailsResponse?.totalDiscount != null) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Total Discount",
                        if (guestDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(guestDiscount)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }


            PrintSunmiUtils.normalText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(
                        subTotalGuest
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (guestTaxes != null) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(taxGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (guestServiceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceChargeGuest),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (!paymentType.equals("Unpaid", true)) {
                if (cashDiscountGlobal > 0) {
                    val cashDis =
                        cashDiscountGlobal / (getOrderDetailsResponse?.guestAttributes?.size?.minus(
                            1
                        ) ?: 1)

                    PrintSunmiUtils.normalText(
                        padLine(
                            "Cash Discount",

                            "-$" + MethodUtils.roundOffAmountString(cashDis),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }


            SunmiPrintHelper.getInstance().lineWrap(1)

            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    guestSubTotal + guestTaxes + guestServiceCharge + dineInTableAdapter.getList()
                        .get(0).guestDividedAmt
                )



            PrintSunmiUtils.boldText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(total),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(total - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(total),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        total,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(total),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(total + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }



            if (customerSettingModel.showRefundAmount) {

                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    PrintSunmiUtils.boldText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }


            if (customerSettingModel.showTipSuggestion) {

                LogUtil.logE(
                    "showTipSuggestion",
                    MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes)
                        .toString()
                )

                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.additionalTipsInner()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipListInner(
                        tipsList,
                        MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                        customerSettingModel.fonts
                    )
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(getOrderDetailsResponse?.note!!)
                SunmiPrintHelper.getInstance().lineWrap(2)

            }


            if (paymentType.equals("paid", ignoreCase = true)) {
                if (customerSettingModel.fonts == Constants.LARGE) {
                    PrintSunmiUtils.boldText("Customer Signature ____")
                } else {
                    PrintSunmiUtils.boldText("Customer Signature           __________________")
                }
            }

            SunmiPrintHelper.getInstance().lineWrap(2)

            if (customerSettingModel.showQrCode) {
                PrintSunmiUtils.qrCodeInner(getOrderDetailsResponse?.digitalReceiptUrl.toString())
            }

            PrintSunmiUtils.cutPaperInner()

            SunmiPrintHelper.getInstance().deInitSunmiPrinterService(requireContext())

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String
    ) {
        var builder: Builder? = null
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )


            if (customerSettingModel.showOrderIdTop) {
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
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(Constants.VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            if (paymentType.isNotEmpty()) {
                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(paymentType + "\n")

            }
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addText("Unpaid")
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

            addBuilderText(
                builder,
                prefProvider.getValue(Constants.BUSINESS_NAME, "").toString()
            )
            builder.addFeedLine(1)
            if (customerSettingModel.showVenueAddress) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_ADDRESS, "")
                        .toString()
                )
                builder.addFeedLine(1)
            }

            if (customerSettingModel.showVenuePhone) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showWebsiteAddress) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_WEBSITE, "")
                )

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showOrderType) {
                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                builder.addText(getOrderDetailsResponse?.orderTypeName + "\n")
            }

            if (customerSettingModel.fonts == Constants.LARGE) {

                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)


            val dineInList = dineInTableAdapter.getList()
            for (i in 0 until dineInList.size) {
                if (dineInList[i].isHeader == 0) {

                    if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {

                        builder.addFeedLine(1)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_CENTER)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        if (dineInList[i].customer == null) {
                            builder.addText(dineInList[i].title)
                        } else {
                            builder.addText(
                                dineInList[i].customer?.first_name + " " +
                                        if (dineInList[i].customer?.last_name != null) {
                                            dineInList[i].customer?.last_name
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }


                } else {
                    dineInList.get(i).item?.let {
                        addOrderItemForDineIn(
                            builder,
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )
                    }

                }


            }



            builder.addFeedLine(2)

            if (getOrderDetailsResponse?.totalDiscount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Total Discount",

                        if (getOrderDetailsResponse?.totalDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {
                            getOrderDetailsResponse?.totalDiscount?.let {
                                "-$" + MethodUtils.roundOffAmountString(it)
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            LogUtil.logE(TAG, "subTotalWT  ${subTotalDInin}")

            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(subTotalDInin),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (viewModel.totalTaxAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(finalTaxAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (serviceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceCharge),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getOrderDetailsResponse?.totalTips != 0.0) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tips",
                        "$" + getOrderDetailsResponse?.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            var totalAmt =
                MethodUtils.roundOffAmountDouble(subTotalDInin + serviceCharge + finalTaxAmt)



            builder.addText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )




            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )


                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(totalAmt)

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }


            if (customerSettingModel.showRefundAmount) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    builder.addText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }

            /*if (getOrderDetailsResponse?.totalTips == 0.0) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                var tip = ""

                if (getOrderDetailsResponse?.totalTips != 0.0) {
                    tip = getOrderDetailsResponse?.totalTips.toString()
                }
                builder.addText(
                    padLine(
                        "Tips",
                        if (customerSettingModel.showTipLineForCash) {
                            "_____________"
                        } else {
                            ""
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }*/


            if (customerSettingModel.showTipSuggestion) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Additional Tips",
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        builder,
                        tipsList,
                        totalAmt,
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)
            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )



                builder.addText(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            /*if (customerSettingModel.showCustomerAddress != false or customerSettingModel.showCustomerPhone != false or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {

                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }*/


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("Order Note")
                builder.addFeedLine(1)

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

                builder.addText(getOrderDetailsResponse?.note)
            }


            if (paymentType.equals("paid", ignoreCase = true)) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //customer signature line.
                builder.addText(
                    padLine(
                        "Customer Signature",
                        addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getOrderDetailsResponse?.digitalReceiptUrl.toString())

                val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {


                requireActivity().runOnUiThread {
                    PrinterClass.getPrinter()?.sendData(
                        builder,
                        if (customerReceiptPrinters.name.substring(0, 6).toString()
                                .lowercase() == "TM-m30".lowercase() || customerReceiptPrinters.name.substring(
                                0,
                                6
                            ).toString().lowercase() == "TM-m10".lowercase()
                        ) {
                            PrinterClass.BLUETOOTH_TIMEOUT
                        } else {
                            PrinterClass.SEND_TIMEOUT

                        }, status, battery
                    )
                }
                PrinterClass.closePrinter()

                //findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun generatePrintSunmi(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String
    ) {

        try {
            PrintSunmiUtils.fontSize(customerSettingModel.fonts)
            if (customerSettingModel.showOrderIdTop) {

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.id)

                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                printBusinessLogo()
            }

            //  PrintSunmiUtils.paymentType("Unpaid")

            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.paymentType(paymentType)

            }



            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(Constants.BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getOrderDetailsResponse?.orderTypeName?.let { PrintSunmiUtils.printOrderType(it) }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.receiptID(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.orderTime(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        PrintSunmiUtils.orderTime(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                PrintSunmiUtils.orderId(
                    padLine(
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }
                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderTime(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        PrintSunmiUtils.orderTime(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    23
                                } else {
                                    48
                                }
                            ).toString()
                        )

                    }
                }
            }

            PrintSunmiUtils.addHorizontal()
            SunmiPrinterApi.getInstance().lineWrap(1)

            val dineInList = dineInTableAdapter.getList()
            for (i in 0 until dineInList.size) {
                if (dineInList[i].isHeader == 0) {

                    if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {

                        if (dineInList[i]?.customer == null) {

                            dineInList[i]?.title?.let { PrintSunmiUtils.printTextCenter(it) }

                        } else {

                            PrintSunmiUtils.printTextCenter(
                                dineInList[i]?.customer?.first_name + " " +
                                        if (dineInList[i]?.customer?.last_name != null) {
                                            dineInList[i].customer?.last_name
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }


                } else {
                    dineInList.get(i).item?.let {
                        addOrderItemForDineIn(
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )
                    }

                    SunmiPrinterApi.getInstance().lineWrap(1)
                }


            }



            SunmiPrinterApi.getInstance().lineWrap(2)

            if (getOrderDetailsResponse?.totalDiscount != null) {

                PrintSunmiUtils.totalDiscount(
                    padLine(
                        "Total Discount",

                        if (getOrderDetailsResponse?.totalDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {
                            getOrderDetailsResponse?.totalDiscount?.let {
                                "-$" + MethodUtils.roundOffAmountString(it)
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }


            PrintSunmiUtils.subTotal(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(subTotalDInin),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (viewModel.totalTaxAmount != null) {


                PrintSunmiUtils.tax(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(finalTaxAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (serviceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {

                PrintSunmiUtils.serviceCharge(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceCharge),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.totalTips != 0.0) {


                PrintSunmiUtils.tip(
                    padLine(
                        "Tips",
                        "$" + getOrderDetailsResponse?.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            val totalAmt =
                MethodUtils.roundOffAmountDouble(subTotalDInin + serviceCharge + finalTaxAmt)



            PrintSunmiUtils.totalPrice(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )



            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.totalPrice(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }



            if (customerSettingModel.showRefundAmount) {

                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    PrintSunmiUtils.totalPrice(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTips()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipList(
                        tipsList,
                        totalAmt,
                        customerSettingModel.fonts
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(2)

            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                PrintSunmiUtils.transactionId(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {


                PrintSunmiUtils.transactionType(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNote(getOrderDetailsResponse?.note!!)
            }

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (paymentType.equals("paid", ignoreCase = true)) {
                if (customerSettingModel.fonts == Constants.LARGE) {
                    PrintSunmiUtils.tips("Customer Signature ____")
                } else {
                    PrintSunmiUtils.tips("Customer Signature           __________________")
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showQrCode) {

                getOrderDetailsResponse?.digitalReceiptUrl?.let {
                    LogUtil.logE(
                        "digitalReceiptUrl",
                        it
                    )
                }

                getOrderDetailsResponse?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCode(it) }

            }

            SunmiPrinterApi.getInstance().lineWrap(5)
            SunmiPrinterApi.getInstance().cutPaper(1, 1)

            SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generatePrintSunmiInner(paymentStatus: String) {


        try {
            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                PrintSunmiUtils.printLogoInner(
                    prefProvider.getValue(
                        Constants.VENUE_LOGO,
                        ""
                    )
                )
            }


            PrintSunmiUtils.headerText("Unpaid")

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "", prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                )
                /*if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""*/
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(
                    prefProvider.getValue(
                        Constants.BUSINESS_WEBSITE,
                        ""
                    )
                )
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getOrderDetailsResponse?.orderTypeName?.let { PrintSunmiUtils.headerText(it) }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + getOrderDetailsResponse?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + getOrderDetailsResponse?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }
                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    getOrderDetailsResponse?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        MethodUtils.formatted()
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    23
                                } else {
                                    48
                                }
                            ).toString()
                        )

                    }
                }
            }

            PrintSunmiUtils.addHorizontalInner()
            SunmiPrintHelper.getInstance().lineWrap(1)

            val dineInList = dineInTableAdapter.getList()
            for (i in 0 until dineInList.size) {
                if (dineInList[i].isHeader == 0) {

                    if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {

                        if (dineInList[i]?.customer == null) {

                            dineInList[i]?.title?.let { PrintSunmiUtils.normalTextCenter(it) }

                        } else {

                            PrintSunmiUtils.normalTextCenter(
                                dineInList[i]?.customer?.first_name + " " +
                                        if (dineInList[i]?.customer?.last_name != null) {
                                            dineInList[i].customer?.last_name
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }


                } else {
                    LogUtil.logE("addDineInInner", "22222222")
                    dineInList.get(i).item?.let {
                        addOrderItemForDineInInner(
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )
                    }


                }

            }




            if (getOrderDetailsResponse?.totalDiscount != null) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Total Discount",

                        if (getOrderDetailsResponse?.totalDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                            "$" + MethodUtils.roundOffAmountString(0.00)
                        } else {
                            getOrderDetailsResponse?.totalDiscount?.let {
                                "-$" + MethodUtils.roundOffAmountString(it)
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }


            PrintSunmiUtils.normalText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(subTotalDInin),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )


            if (viewModel.totalTaxAmount != null) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(finalTaxAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (serviceCharge != null && (getOrderDetailsResponse?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(serviceCharge),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.totalTips != 0.0) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Tips",
                        "$" + getOrderDetailsResponse?.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            val totalAmt =
                MethodUtils.roundOffAmountDouble(subTotalDInin + serviceCharge + finalTaxAmt)


            SunmiPrintHelper.getInstance().lineWrap(1)

            PrintSunmiUtils.boldText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )



            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            } else if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        totalAmt,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


                PrintSunmiUtils.boldText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt + cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }



            if (customerSettingModel.showRefundAmount) {

                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    PrintSunmiUtils.boldText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()
                    )
                }
            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTipsInner()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipListInner(
                        tipsList,
                        totalAmt,
                        customerSettingModel.fonts
                    )
                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction ID",
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction Type",
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )
            }


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(getOrderDetailsResponse?.note!!)
            }



            SunmiPrintHelper.getInstance().lineWrap(2)

            if (paymentStatus.equals("paid", ignoreCase = true)) {
                if (customerSettingModel.fonts == Constants.LARGE) {
                    PrintSunmiUtils.boldText("Customer Signature ____")
                } else {
                    PrintSunmiUtils.boldText("Customer Signature           __________________")
                }
            }

            SunmiPrintHelper.getInstance().lineWrap(2)

            if (customerSettingModel.showQrCode) {

                getOrderDetailsResponse?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCodeInner(it) }

            }
            PrintSunmiUtils.cutPaperInner()

            SunmiPrintHelper.getInstance().deInitSunmiPrinterService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                tipsList = it
            }
        }
    }

    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?

        // initializing a variable for default display.

        // initializing a variable for default display.
        val display: Display = manager!!.defaultDisplay

        // creating a variable for point which
        // is to be displayed in QR Code.

        // creating a variable for point which
        // is to be displayed in QR Code.
        val point = Point()
        display.getSize(point)

        // getting width and
        // height of a point

        // getting width and
        // height of a point
        val width: Int = point.x
        val height: Int = point.y

        // generating dimension from width and height.

        // generating dimension from width and height.
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4


        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = it.data
                    }

                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }

                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()

                }
            }

        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf()

    ) {

        Log.d("###17MAR23", "initKitchenPrinter: Called - Start")
        Log.d("###17MAR23", "ProgressShow: Called - Start")

        if (data.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)

            if (!SunmiPrinterApi.getInstance().isConnected) {
//                runOnUiThread {
//                    ProgressUtils.showProgressDialog(requireActivity())
//                }
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            Log.d("###17MAR23", "SunmiPrinterFound: Called")
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                            Log.d("###17MAR23", "SunmiPrinterUnfound: Called")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            Log.d("###17MAR23", "SunmiPrinterConnect: Called")
                            //ProgressUtils.dismissProgressDialog()
                            generateKitchenReceiptSunmi(data, type, item, listItemWithGuest)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                            Log.d("###17MAR23", "SunmiPrinterDisConnect: Called")
                        }

                    })
            } else {

                if (SunmiPrinterApi.getInstance().isConnected)
                    generateKitchenReceiptSunmi(data, type, item, listItemWithGuest)
            }

        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(data, type, item, listItemWithGuest)
            }
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
                    if (printerStatusInfo.online == 1) {
                        try {
                            printer.disconnect()

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
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

                    generateReceiptForU220(data, type, item, mPrinter)

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }


            } else {

                PrinterClass.setPrinter(null)
                if (PrinterClass.getPrinter() == null) {
                    var printer: Print? = Print(requireContext())
                    if (printer != null) {
//                printer.setStatusChangeEventCallback(this)
//                printer.setBatteryStatusChangeEventCallback(this)
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
                        // printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return
                    }

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)

                        generateKitchenReceipt(data, type, item, listItemWithGuest)

                    }

                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }
            }
        }

    }

    private fun generateKitchenReceiptForU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        builder: Printer
    ) {
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
                Constants.SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }

                Constants.MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }

                Constants.LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }


            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {


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
                    "OrderID:" + getOrderDetailsResponse?.id
                )
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


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

                    addBuilderTextForU220(
                        builder,
                        getOrderDetailsResponse?.orderTypeName.toString()
                    )
                }

                builder.addFeedLine(2)
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


                builder.addText(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")


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
                            "Employee:" + getOrderDetailsResponse?.employee?.name, "",
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
                            getOrderDetailsResponse?.createdAt.toString()
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


                addOrdersForKitchenDineInU220(
                    builder,
                    item,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(
                        arrayListOf()
                    )
                )

                if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(getOrderDetailsResponse?.note.toString())
                }


            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            try {

                builder.sendData(Printer.PARAM_DEFAULT)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateReceiptForU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        builder: Printer
    ) {


        var fontSizeH = 1
        var fontSizeW = 1
        when (kitchenSettingModel.fonts) {
            Constants.SMALL -> {
                fontSizeH = 1
                fontSizeW = 1
            }

            Constants.MEDIUM -> {
                fontSizeH = 1
                fontSizeW = 2
            }

            Constants.LARGE -> {
                fontSizeH = 2
                fontSizeW = 2
            }


        }




        if (customerReceiptPrinters.name.substring(0, 4)
                .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
        ) {


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
                "OrderID:" + getOrderDetailsResponse?.id
            )
            builder.addFeedUnit(30)
            builder.addFeedLine(1)

            if (kitchenSettingModel.showOrderType) {


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

                addBuilderTextForU220(builder, getOrderDetailsResponse?.orderTypeName.toString())
            }

            builder.addFeedLine(2)
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


            builder.addText(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")


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
                        "Employee:" + getOrderDetailsResponse?.employee?.name, "",
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
                        getOrderDetailsResponse?.createdAt.toString()
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


            addOrdersForKitchenDineInU220(
                builder,
                item,
                fontSizeH,
                fontSizeW,
                customerReceiptPrinters.printerCategories.toCollection(
                    arrayListOf()
                )
            )

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                builder.addText(getOrderDetailsResponse?.note.toString())
            }


        }

        builder.addFeedLine(2)

        builder.addCut(Builder.CUT_FEED)

        try {
            builder.sendData(Printer.PARAM_DEFAULT)

        } catch (e: Exception) {
            e.printStackTrace()

        }


    }


    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
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
                Constants.SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }

                Constants.MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }

                Constants.LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }

            builder = Builder(pname, PrinterClass.language, requireActivity())


            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {


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
                        "OrderID:" + getOrderDetailsResponse?.custom_order_id
                    )
                } else {
                    builder.addText(
                        "OrderID:" + getOrderDetailsResponse?.id
                    )
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


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

                    addBuilderText(builder, getOrderDetailsResponse?.orderTypeName.toString())
                }

                builder.addFeedLine(2)
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


                builder.addText(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")


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
                            "Employee:" + getOrderDetailsResponse?.employee?.name, "",
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
                            getOrderDetailsResponse?.createdAt.toString()
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


                addOrdersForKitchenDineIn(
                    builder,
                    item,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(
                        arrayListOf()
                    ),
                    listItemWithGuest
                )

                if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(getOrderDetailsResponse?.note.toString())
                }


            } else {

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
                    builder.addText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
                } else {
                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


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

                    addBuilderText(builder, getOrderDetailsResponse?.orderTypeName.toString())
                }

                builder.addFeedLine(2)
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


                builder.addText(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")


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
//                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
//                            "ENTJKOIJH8745"
//                        } else {
//                            getOrderDetailsResponse?.offlineId
//                        },
//                        "",
//                        if (kitchenSettingModel.fonts == Constants.LARGE) {
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
                            "Employee:" + getOrderDetailsResponse?.employee?.name, "",
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
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
                            getOrderDetailsResponse?.createdAt.toString()
                        ),
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addFeedLine(1)


                addHorizontalLine(builder)

                addOrdersForKitchenCustoemrPrinter(
                    builder,
                    item,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(
                        arrayListOf()
                    ),
                    kitchenSettingModel.fonts,
                    listItemWithGuest
                )

                if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(getOrderDetailsResponse?.note.toString())
                }


            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
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
                    timeOut, status, battery
                )

                Handler(Looper.getMainLooper()).postDelayed(Runnable {

                }, 1000)
                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setService(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(data, type, item, listItemWithGuest)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(data, type, item, listItemWithGuest)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService1(paymentStatus: String) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generatePrintSunmiInner(paymentStatus)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService1(paymentStatus)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService2(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double,
        total: Double,
        taxGuest: Double,
        serviceChargeGuest: Double,
        divideDiscount: Double
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateGuestPrintSunmiInner(
                    customerReceiptPrinters,
                    type,
                    paymentType,
                    listGuestItem,
                    guestName,
                    listWTitems,
                    subTotalGuest,
                    total,
                    taxGuest,
                    serviceChargeGuest,
                    divideDiscount,
                )


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService2(
                    customerReceiptPrinters,
                    type,
                    paymentType,
                    listGuestItem,
                    guestName,
                    listWTitems,
                    subTotalGuest,
                    total,
                    taxGuest,
                    serviceChargeGuest,
                    divideDiscount,
                )
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf()
    ) {
        try {
            //ProgressUtils.showProgressDialog(requireActivity())

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().printerInit()
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.custom_order_id)
            } else {
                PrintSunmiUtils.orderIdLarge("OrderID:" + getOrderDetailsResponse?.id)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.printOrderType(getOrderDetailsResponse?.orderTypeName.toString())

                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.addValue(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")

            SunmiPrinterApi.getInstance().lineWrap(1)


//            SunmiPrinterApi.getInstance().lineWrap(1)
//            PrintSunmiUtils.receiptID(
//                padLine(
//                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
//                        "ENTJKOIJH8745"
//                    } else {
//                        getOrderDetailsResponse?.offlineId
//                    },
//                    "",
//                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
//                ).toString()
//            )
//
//            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showTeamMember) {


                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + getOrderDetailsResponse?.employee?.name, "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )

            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        getOrderDetailsResponse?.createdAt.toString()
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )

            SunmiPrinterApi.getInstance().enableUnderline(false)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance()
                .printText(addHorizontalKitchenLineSunmi(PrintSunmiUtils.fontSize))
            //  SunmiPrinterApi.getInstance().lineWrap(1)
            SunmiPrinterApi.getInstance().enableUnderline(false)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance()
                .printText(addHorizontalKitchenLineSunmiForLastHeaderLine(PrintSunmiUtils.fontSize))
            SunmiPrinterApi.getInstance().lineWrap(2)



            addOrdersForKitchenDineIn(
                item, customerReceiptPrinters.printerCategories.toCollection(
                    arrayListOf()
                ), listItemWithGuest
            )

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getOrderDetailsResponse?.note.toString())
            }


            PrintSunmiUtils.cutPaper()
            //ProgressUtils.dismissProgressDialog()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
    ) {
        try {


            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.headerText(getOrderDetailsResponse?.orderTypeName.toString())

                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.normalTextCenterLarge(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")

            SunmiPrintHelper.getInstance().lineWrap(1)



            if (kitchenSettingModel.showTeamMember) {


                PrintSunmiUtils.normalTextLarge(
                    "Employee:" + getOrderDetailsResponse?.employee?.name
                )

            }

            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    getOrderDetailsResponse?.createdAt.toString()
                )
            )

            PrintSunmiUtils.addHorizontalInner()
            PrintSunmiUtils.addHorizontalInner()

            SunmiPrintHelper.getInstance().lineWrap(1)

            addOrdersForKitchenDineInInner(item, listItemWithGuest)

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(getOrderDetailsResponse?.note.toString())
            }


            PrintSunmiUtils.cutPaperInner()


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun observeUnMergeTable() {
        viewModel.unMergeStatusUpdate.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), status.toString()
                ) { _, _ ->
                    if (findNavController().currentDestination?.id == R.id.dineInOrderTable) {
                        findNavController().navigate(R.id.action_dineInOrderTable_to_dineInFragmentPays)
                    }
                }


            }
        }
    }

    private fun observeQueueCreated() {
        viewModel.queueCreateSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
            }
        }
    }

    private fun observeFireAll() {
        viewModel.fireAllStatus.observe(viewLifecycleOwner) { event ->
            LogUtil.logE(TAG, "FireAllStatusObserved")

            if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {

                var orderItemsAttributes: ArrayList<OrderItemsAttribute> = arrayListOf()
                getOrderDetailsResponse?.orderItems?.forEach {
                    val orderItem = OrderItemsAttribute()
                    orderItem.category_id = it.categoryId
                    orderItem.discountAmount = it.discountAmount
                    orderItem.discountId = it.discountId
                    orderItem.discountType = it.discountType.toString()
                    orderItem.employeeId = it.employeeId
                    orderItem.id = it.id
                    orderItem.isPaid = it.isPaid
                    orderItem.itemId = it.itemId
                    orderItem.itemName = it.itemName
                    orderItem.note = it.note
                    orderItem.orderId = it.orderId
                    orderItem.note = it.note
                    var taxList: ArrayList<OrderItemTaxesAttribute> = arrayListOf()
                    it.orderItemTaxes.forEach { tax ->
                        var taxModel = OrderItemTaxesAttribute()
                        taxModel.taxType = tax.taxType
                        taxModel.orderId = tax.orderId
                        taxModel.name = tax.name
                        taxModel.taxTotalAmount = tax.amount ?: 0.0
                        taxModel.id = tax.id
                        taxModel.isDefault = tax.isDefault
                        taxModel.orderItemId = tax.orderItemId
                        taxModel.rate = tax.rate
                        taxModel.taxId = tax.taxId

                        taxList.add(taxModel)

                    }
                    orderItem.orderItemTaxesAttributes = taxList

                    var listModifiers: ArrayList<OrderItemModifierAttribute> = arrayListOf()
                    it.orderItemModifiers.forEach {
                        var modifierModel = OrderItemModifierAttribute()
                        modifierModel.id = it.id
                        modifierModel.order_item_id = it.orderItemId
                        modifierModel.orderId = it.orderId
                        modifierModel.name = it.name
                        modifierModel.quantity = it.quantity
                        modifierModel.modifier_set_id = it.modifierId?.toInt() ?: 0
                        modifierModel.price = it.price
                        modifierModel.modifier_quantity = it.modifier_quantity!!

                        listModifiers.add(modifierModel)


                    }
                    orderItem.orderItemModifiersAttributes = listModifiers

                    orderItemsAttributes.add(orderItem)


                }

                var orderRequest = OrderAttributeRequestModel()

                orderRequest.orderItemsAttributes = orderItemsAttributes
                orderRequest.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 0)
                orderRequest.totalAmount = getOrderDetailsResponse?.totalAmount ?: 0.0

                orderRequest.offlineId = getOrderDetailsResponse?.offlineId ?: randomOfflineId()
                orderRequest.id = getOrderDetailsResponse?.id ?: 0
                orderRequest.openOrderType = "Dine In"

                val createQueueRequest = CreateQueuePrinterRequestModel(
                    location_id = prefProvider.getValueInt(LOCATION_ID, 0),
                    order_type = "Dine In",
                    printer_id = listOf(),
                    order_item_attributes = orderItemsAttributes,
                    order_data = orderRequest,
                    terminal_id = prefProvider.getValueInt(TERMINAL_ID, 0)


                )
                viewModel.createQueuePrinter(createQueueRequest)
                fireItemsList.clear()
                fireItemsList = arrayListOf()
            }

        }
    }

    private fun singleItemFireObserver() {
        if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
            viewModel.fireSingleStatus.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let {
                    var itemList: ArrayList<OrderItemsAttribute> = arrayListOf()
                    val itemModel = OrderItemsAttribute()
                    itemModel.category_id = it.categoryId
                    itemModel.discountAmount = it.discountPrice
                    itemModel.discountId = it.discountId
                    itemModel.discountType = it.discountType
                    itemModel.itemName = it.name
                    itemModel.quantity = it.itemQuantity
                    var listModifiers: ArrayList<OrderItemModifierAttribute> = arrayListOf()
                    it.modifiers.forEach {
                        var modifierModel = OrderItemModifierAttribute()
                        modifierModel.price = it.price
                        modifierModel.id = it.id
                        modifierModel.name = it.name
                        modifierModel.totalPrice = it.price
                        modifierModel.quantity = it.itemQuantity

                        listModifiers.add(modifierModel)
                    }

                    itemModel.orderItemModifiersAttributes = listModifiers


                    var orderRequest = OrderAttributeRequestModel()

                    orderRequest.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 0)
                    orderRequest.totalAmount = getOrderDetailsResponse?.totalAmount ?: 0.0

                    orderRequest.offlineId =
                        getOrderDetailsResponse?.offlineId ?: randomOfflineId()
                    orderRequest.id = getOrderDetailsResponse?.id ?: 0
                    orderRequest.openOrderType = "Dine In"


                    val createQueueRequest = CreateQueuePrinterRequestModel(
                        location_id = prefProvider.getValueInt(LOCATION_ID, 0),
                        order_type = "Dine In",
                        printer_id = listOf(),
                        order_item_attributes = arrayListOf(),
                        order_data = orderRequest,
                        terminal_id = prefProvider.getValueInt(TERMINAL_ID, 0)


                    )
                    viewModel.createQueuePrinter(createQueueRequest)
                }


            }
        }

    }

    fun checkForAutoFire(isCheckAndFire: Boolean) {
        Log.d("###17MAR23", "checkForAutoFire: Called - Start - $isCheckAndFire")
        var list: List<DineInModel> = arrayListOf()
        list = dineInTableAdapter.getList() ?: arrayListOf()
        val builder = ArrayList<String>()
        var listItem: ArrayList<TbCartItem> = arrayListOf()
        var listItemWithGuest: LinkedHashMap<String, ArrayList<TbCartItem>> = linkedMapOf()

        for (i in 0 until list.size) {

            if (list[i].isHeader == 0) {
                if (i < list.size - 1 && list[i + 1].isHeader == 1) {
                    Log.e(TAG, "checkInsideEdge 1 ")
                    var listItemLocal: ArrayList<TbCartItem> = arrayListOf()
                    for (j in i + 1 until list.size) {
                        if (list[j].isHeader == 1) {
                            Log.e(TAG, "checkInsideEdge 2 ")
                            list[j].item?.let { listItemLocal.add(it) }
                            if (j == list.size - 1) {
                                list[i].title?.let { listItemWithGuest.put(it, listItemLocal) }
                                break
                            }

                        } else if (list[j].isHeader == 0) {
                            Log.e(TAG, "checkInsideEdge 3 ")

                            if (listItemLocal.isNotEmpty()) {
                                list[i].title?.let { listItemWithGuest.put(it, listItemLocal) }
                            }
                            break
                        }


                    }

                }
            }
        }
        Log.e(TAG, "getListOfHash  ${Gson().toJson(listItemWithGuest)}")
        LogUtil.logE(TAG, "dineInList:  ${Gson().toJson(list)}")
        fireItemsList = arrayListOf()
        list.forEach {
            if (it.isHeader == 1) {
                it.item?.let {
                    if (!it.isFired) {
                        fireItemsList.add(it)
                        listItem.add(it)
                    }
                }
                it.item?.orderItemId?.let {
                    builder.add(it.toString())

                }
                //  it.item?.isFired = true

            }

        }
        LogUtil.logE(TAG, "listItem:  ${Gson().toJson(listItem)}")
        val serializedObject: String =
            prefProvider.getValue(Constants.DINE_IN_UPDATE_LIST, "")
        LogUtil.logE(TAG, "serializedObjectData:  ${Gson().toJson(serializedObject)}")
        if (serializedObject.isNotEmpty()) {
            var printOrderItems:
                    ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                arrayListOf()

            val gson = Gson()
            val type = object :
                TypeToken<List<GetOrderDetailsResponse.Data.OrderItem?>?>() {}.type
            var arrayItems: ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                gson.fromJson<Any>(
                    serializedObject,
                    type
                ) as ArrayList<GetOrderDetailsResponse.Data.OrderItem>

            LogUtil.logE(TAG, "arrayItems:  ${Gson().toJson(arrayItems)}")
            var itemIds: ArrayList<Int> = arrayListOf()
            arrayItems.forEach {
                itemIds.add(it.id)
            }

            arrayItems.forEachIndexed { index, orderItem ->
                if ((getOrderDetailsResponse?.orderItems?.size?.minus(1) ?: -1) >= index) {
                    if (itemIds.contains(getOrderDetailsResponse?.orderItems?.get(index)?.id)) {
                        LogUtil.logE("InsideLoop", "Inside")
                        if (getOrderDetailsResponse?.orderItems?.get(index)?.quantity != orderItem.quantity) {
                            if (getOrderDetailsResponse?.orderItems?.get(index)?.quantity!! > arrayItems[index].quantity) {
                                getOrderDetailsResponse?.orderItems?.get(index)?.quantity =
                                    getOrderDetailsResponse?.orderItems?.get(index)?.quantity!! - arrayItems[index].quantity
                                if (!printOrderItems.contains(
                                        getOrderDetailsResponse?.orderItems?.get(
                                            index
                                        )
                                    )
                                ) {
                                    printOrderItems.add(
                                        getOrderDetailsResponse?.orderItems?.get(
                                            index
                                        )!!
                                    )
                                    var tbItem: TbCartItem = TbCartItem()
                                    tbItem.categoryId =
                                        getOrderDetailsResponse?.orderItems?.get(index)?.categoryId
                                            ?: 0
                                    tbItem.name =
                                        getOrderDetailsResponse?.orderItems?.get(index)?.itemName
                                            ?: ""
                                    tbItem.price =
                                        getOrderDetailsResponse?.orderItems?.get(index)?.price
                                            ?: 0.0
                                    tbItem.itemQuantity =
                                        getOrderDetailsResponse?.orderItems?.get(index)?.quantity
                                            ?: 0
                                    if (getOrderDetailsResponse?.orderItems?.get(index)?.orderItemModifiers?.isNotEmpty() == true) {
                                        var modifierList: ArrayList<Modifier> = arrayListOf()
                                        getOrderDetailsResponse?.orderItems?.get(index)?.orderItemModifiers?.forEach {
                                            val modifiers = Modifier()
                                            modifiers.price = it.price
                                            modifiers.name = it.name
                                            modifiers.itemQuantity = it.quantity
                                            modifiers.modifier_quantity = it.modifier_quantity!!
                                            modifierList.add(modifiers)
                                        }
                                        tbItem.modifiers = modifierList
                                    }

                                    listItem.add(tbItem)
                                }
                            }

                        } else {

                        }

                    }
                }

            }

            prefProvider.setValue(DINE_IN_UPDATE_LIST, "")
        }



        if (listItem.isNotEmpty()) {
            var orderItemsIds: ArrayList<Int> = arrayListOf()
            listItem.forEach {
                it.orderItemId?.let { it1 -> orderItemsIds.add(it1) }
            }
            listItemWithGuest.forEach {
                it.value.forEach { it1 ->
                    var orderITemId = it1.orderItemId
                    if (orderItemsIds.contains(it1.orderItemId) == false) {
                        var listNewITems: ArrayList<TbCartItem> = arrayListOf()
                        listNewITems.addAll(it.value)
                        if (listNewITems.isNotEmpty()) {
                            for (k in it.value.indices) {
                                if (it.value[k].orderItemId == orderITemId) {
                                    listNewITems.remove(it.value[k])
                                }
                            }
                            listItemWithGuest.put(it.key, listNewITems)
                        }
                    }
                }
            }

            // remove guest with no items
            val it: MutableIterator<Map.Entry<String, ArrayList<TbCartItem>>> =
                listItemWithGuest.entries.iterator()
            while (it.hasNext()) {
                if (it.next().value.isEmpty()) {
                    it.remove()
                }
            }

            var autoPrintEnable = false
            kitchenPrinterList.forEach { kit ->
                if (kit.status) {


                    if (isCheckAndFire) {
                        kit.orderTypes.forEach {
                            if (it.orderTypeId == getOrderDetailsResponse?.orderTypeId) {
                                LogUtil.logE(
                                    TAG,
                                    "printerSettings  ${Gson().toJson(it.printerSettings)}"
                                )
                                it.printerSettings.forEach {
                                    if (it.printType.lowercase()
                                            .equals(Constants.KITCHEN.lowercase()) && it.autoPrinting
                                    ) {

                                        if (checkItemsforPrinterDineIn(
                                                listItem, kit.printerCategories.toCollection(
                                                    arrayListOf()
                                                )
                                            )
                                        ) {
                                            LogUtil.logE(TAG, "printerName  ${kit.name} ")
                                            autoPrintEnable = true
                                            if (!prefProvider.getValueboolean(
                                                    IS_PRINTER_QUEUE_ENABLE,
                                                    false
                                                )
                                            ) {
                                                initKitchenPrinter(
                                                    kit,
                                                    Constants.KITCHEN,
                                                    listItem,
                                                    listItemWithGuest
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    } else {
                        if (!prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                            initKitchenPrinter(kit, Constants.KITCHEN, listItem, listItemWithGuest)
                        }

                    }
                }
            }
            if (!isCheckAndFire or (isCheckAndFire && autoPrintEnable)) {
                var fireAllIds = android.text.TextUtils.join(",", builder)

                viewModel.fireItemToKitchen(orderId ?: 0, true, fireAllIds, true)
                list.forEach {
                    if (it.isHeader == 1) {
                        it?.item?.isFired = true
                    }
                }
                dineInTableAdapter.updateStatus(0, true)
            }
        }


    }

    fun progressDialog() {

        pd = Dialog(requireActivity())
        pd.setContentView(R.layout.view_loading)
        // pd.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL)
//        pd.setMessage("Please Wait..")
        pd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pd.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        pd.setCanceledOnTouchOutside(false)
        pd.setCancelable(false)
        pd.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )


    }

    @Inject
    lateinit var apiService: ApiService

    override fun onPause() {
        super.onPause()
        if (pd != null && pd.isShowing) {
            pd.dismiss()
        }
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider.getValue(Constants.VENUE_LOGO, ""),
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }

}


