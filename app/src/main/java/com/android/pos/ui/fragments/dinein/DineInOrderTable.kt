package com.android.pos.ui.fragments.dinein

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
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineinCartPaymentModel
import com.android.pos.data.model.GuestDataModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.android.pos.data.remote.Constants.DINE_IN_DISCOUNT
import com.android.pos.data.remote.Constants.DINE_IN_GUEST_PAYMENT_DATA
import com.android.pos.data.remote.Constants.DINE_IN_SERVICECHARGE
import com.android.pos.data.remote.Constants.DINE_IN_SUBTOTAL
import com.android.pos.data.remote.Constants.DINE_IN_TAX
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE_LIST
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.GUEST_POSITION
import com.android.pos.data.remote.Constants.IS_GUEST_PAYMNET
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.MERGEDANDOCCUPIED
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.android.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.SUNMI_PRINTER
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.android.pos.databinding.FragmentDineInOrderTableBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInTableAdapter
import com.android.pos.ui.fragments.checkout.CheckoutDineInPaymentViewModel
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.utils.*
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DineInOrderTable : Fragment(), DineInTableAdapter.DineInTableListner {
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

    @Inject
    lateinit var prefProvider: PrefProvider
    private val viewModel by viewModels<DineInOrderTableViewModel>()
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
        binding.lifecycleOwner = this
        progressDialog()
        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        observeShowProgress()
        setupSnackbar()
        getCustomerList()
        observeServiceCharge()
        singleItemFireObserver()
        getCustomerPrinterList()
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()
        observeFireAll()
        observeQueueCreated()

        observeTipsList()
        observeAddGuest()

        navigateDineInOrderNew()
        observeUnMergeTable()
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_guestcount",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var count: Int = bundle.getInt("count")
            addGuestToOrder(count)
        }
        return binding.root
    }

    private fun observeAddGuest() {
        viewModel.updateOrder.observe(viewLifecycleOwner) { event ->
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), event.getContentIfNotHandled().toString()
            ) { _, _ ->
                orderId?.let { viewModel.apiCallOrderDetails(it) }
            }
        }
    }


    private fun getCustomerList() {
        viewModel.customer().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                allCustomerList = it.toCollection(arrayListOf())
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
                        Log.e(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
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
            findNavController().navigate(R.id.action_dineInOrderTable_to_dineInFragment)

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
            //new Calculation for total Discount
            var listWT: ArrayList<TbItem> = arrayListOf()
            var list = dineInTableAdapter.getList()

            for (i in 0 until list.size) {
                if (list.get(i).title.equals("Whole Table", true) && i + 1 <= list.size) {
                    if (list[i + 1].isHeader == 1) {
                        for (j in i + 1 until list.size) {
                            if (list.get(j).isHeader == 1) {
                                listWT.add(list.get(j).item!!)
                            } else {
                                break
                            }
                        }
                    }
                }
            }
            Log.e(TAG, "listWTItems ${Gson().toJson(listWT)}")

            var dividedOrderDiscount = 0.0
            totalGuestCount = getOrderDetailsResponse?.guestAttributes?.size!! - 1
            if (paidGuestAmount > 0 && globalOrderDiscount > 0.0) {
                Log.e("globalOrderDiscount", "globalOrderDiscount  ${globalOrderDiscount}")
                var eachGuestDiscount =
                    MethodUtils.roundOffAmountDouble(globalOrderDiscount / (getOrderDetailsResponse?.guestAttributes?.size!! - 1))

                dividedOrderDiscount = globalOrderDiscount - (eachGuestDiscount * paidGuestAmount)


                var wholeDisDivide =
                    MethodUtils.roundOffAmountDouble(wholeTableDiscount / (getOrderDetailsResponse?.guestAttributes?.size!! - 1))
                if (wholeDisDivide > dividedOrderDiscount) {
                    dividedOrderDiscount = wholeDisDivide - dividedOrderDiscount
                } else {
                    dividedOrderDiscount -= wholeDisDivide
                }
                totalDiscount -= wholeDisDivide
                subTotalDInin -= dividedOrderDiscount
                Log.e("dividedOrderDiscount", "dividedOrderDiscount  ${dividedOrderDiscount}")
                Log.e("WRqwrfarf", "wholeDisDivide  ${wholeDisDivide}")
                Log.e(TAG, "subtotal :: " + subTotalDInin)
            }

            Log.e("WholeTableDis", "wholeDis  ${wholeTableDiscount}")


            Log.e("totalDiscount", "totalDiscount  ${totalDiscount}")
            if (totalDiscount > dividedOrderDiscount) {
                totalDiscount -= dividedOrderDiscount
            }


            val adapterList = dineInTableAdapter.getList()
            var offlineId = randomOfflineId()

            cartList = getCartModel(adapterList.toCollection(arrayListOf()))
            cartList?.note = order_note
            Log.e(TAG, "getcartList  ${Gson().toJson(cartList)}")
            cartList!!.taxlistDynamic = listOf()
            var temp_itemsList: ArrayList<TbItem> = arrayListOf()
            cartList?.dineInList?.forEach { dineModel ->
                if (!dineModel.isPaid) {
                    temp_itemsList.addAll(dineModel.items)
                }
            }


            temp_itemsList.forEach { item ->
                cartList = taxBifurcationCalculation(
                    item,
                    cartList!!
                )
            }


            Log.d(TAG, "onClick: listof Tax:  " + Gson().toJson(cartList?.taxlistDynamic))

            var listreemaining: List<TaxData> = emptyList()
            listWT.forEach { wholetableitems ->
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
                            Log.e("itemTaxPrice", "" + itemTaxPrice)
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

                    Log.d(TAG, "onClick: wholetable total tax $totaltaxtemp")
                    var found = -1
                    totaltaxtemp /= (getOrderDetailsResponse?.guestAttributes?.size!! - 1)
                    var temp_remaining = totaltaxtemp * paidGuestAmount
                    var temp_subtotal =
                        totalPrice / (getOrderDetailsResponse?.guestAttributes?.size!! - 1)
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


            Log.d(TAG, "onClick: listof Tax: after  " + Gson().toJson(cartList?.taxlistDynamic))
            viewModelPayment.addCart(cartList!!)
            Log.e(TAG, "getcartListAfterAdd  ${Gson().toJson(cartList)}")

            totalTax = 0.0
            var subTotal = 0.0
            var amtToPay = 0.0
            val totalItem: ArrayList<TbItem> = arrayListOf()
            for (i in 0 until adapterList.size) {
                if (adapterList.get(i).isHeader == 1) {
                    adapterList.get(i).item?.let {
                        totalItem.add(it)
                    }
                }
            }
            if (totalItem.isNotEmpty()) {
                totalItem.forEach {
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
                                Log.e("itemTaxPrice", "" + itemTaxPrice)
                                String.format("%.2f", itemTaxPrice)
                                    .toDouble()
                            } else {

                                String.format(
                                    "%.2f",
                                    tax.rate * it.quantity
                                )
                                    .toDouble()
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
                        ) / (getOrderDetailsResponse?.guestAttributes?.size!! - 1)
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
                    offlineId = offlineId
                    payableType = "GuestTab"
                    paymentType = "Cash"
                    transactionId = randomOfflineId()
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    serviceChargeAmount = MethodUtils.roundOffAmountDouble(serviceCharge)


                },
                dineInOrderModel
            )
//            Log.e(TAG, "getPassmodel  ${Gson().toJson(model)}")
            val bundle = Bundle()
//            viewModelPayment.totalPrice = MethodUtils.roundOffAmobtnPayuntDouble(toFinalAmt)
//            viewModelPayment.subTotalPrice = MethodUtils.roundOffAmountDouble(subTotalDInin)
//            viewModelPayment.totalTax = MethodUtils.roundOffAmountDouble(finalTaxAmt)
//            viewModelPayment.cashdiscountAmount =
//                MethodUtils.roundOffAmountDouble(divideCashDiscount)
//            viewModelPayment.totalServiceCharge = MethodUtils.roundOffAmountDouble(serviceCharge)
//            viewModelPayment.totalDiscount = MethodUtils.roundOffAmountDouble(totalDiscount)

            Log.e("AAJE", "subTotalDInin:  ${MethodUtils.roundOffAmountDouble(subTotalDInin)}")
            bundle.putDouble("totalPrice", MethodUtils.roundOffAmountDouble(toFinalAmt))
            bundle.putDouble("subTotalPrice", MethodUtils.roundOffAmountDouble(subTotalDInin))
            bundle.putDouble("totalTax", MethodUtils.roundOffAmountDouble(finalTaxAmt))
            bundle.putParcelable("model", model)
            bundle.putBoolean("update", true)
            bundle.putDouble(
                "divideCashDiscount",
                MethodUtils.roundOffAmountDouble(divideCashDiscount)
            )
            Log.e("DineCheck", "totalDiscount  ${totalDiscount}")
            bundle.putParcelable("floorPlan", floorPlanModel)
            bundle.putDouble("totalServiceCharge", MethodUtils.roundOffAmountDouble(serviceCharge))
            bundle.putDouble("totalDiscount", MethodUtils.roundOffAmountDouble(totalDiscount))
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

            Log.e(TAG, "passOrderId:  ${orderId}")
            bundle.putInt("orderId", orderId ?: -1)
            bundle.putInt(GUEST_POSITION, 0)
            prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)

            if (findNavController().currentDestination?.id == R.id.dineInOrderTable)
                findNavController().navigate(R.id.action_dineInOrderTable_to_checkoutDineIN, bundle)


        }


        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }
        binding.txtHomeBottom.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }

        binding.txtEditOrder.setOnClickListener {
            val list = dineInTableAdapter.getList()
            val newList: ArrayList<DineInModel> = arrayListOf()


            for (i in 0 until list.size) {
                val model = DineInModel()
                if (list[i].isHeader == 0) {
                    val listTbItem: ArrayList<TbItem> = arrayListOf()
                    model.id = list[i].id
                    model.isPaid = list[i].isPaid
                    model.title = list[i].title
                    model.customer = list[i].customer
                    model.isFired = list[i].isFired
                    model.guestDividerAmt = list[i].guestDividerAmt
                    model.guestDividedAmt = list[i].guestDividedAmt


                    for (j in i + 1 until list.size) {
                        if (list[j].isHeader == 1) {
                            list[j].item?.let { it1 ->
                                if (it1.discountPrice != 0.0) {
                                    it1.discountPrice =
                                        MethodUtils.roundOffAmountDouble(it1.discountPrice / it1.itemQuantity)
                                }
                                Log.e(TAG, "updateItemForDiscount  ${Gson().toJson(it1)}")
                                listTbItem.add(it1)
                            }

                        } else {
                            break
                        }

                    }

                    model.items = listTbItem
                    newList.add(model)


                }


            }

            //   prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, Gson().toJson(newList))

            val bundle = Bundle()
            bundle.putBoolean("is_dine_in_edit", true)
            bundle.putParcelableArrayList(
                "dine_in_list",
                newList
            )

            Log.e("OrderFre", "APIDISc  ${getOrderDetailsResponse?.totalDiscount?.toDouble()}")
            Log.e("OrderFre", "totalDiscount  ${totalDiscount}")
            Log.e("OrderFre", "OrderDiscount  ${globalOrderDiscount}")

            bundle.putDouble(
                "totalDiscount",
                globalOrderDiscount
            )
            bundle.putString("order_note", order_note)
            bundle.putParcelable("tableDetails", getOrderDetailsResponse?.floorPlanTable)
            viewModelPayment.deleteCart()
            orderId?.let { it1 -> bundle.putInt("orderId", it1) }

            prefProvider.setValueboolean(DINE_IN_UPDATE, true)
            prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
            prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
            prefProvider.setValueInt(Constants.DINE_IN_TABLE_ID, 2)
            prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)

            /*   prefProvider.setValu
            e(Constants.ORDER_TYPE, DINE_IN)
               prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
               prefProvider.setValueInt(ORDER_TYPE_ID, 2)*/

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

            dineInTableAdapter.setList(guestList.toCollection(arrayListOf()))

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

    private fun addGuestToOrder(count: Int) {

        val adapterList = dineInTableAdapter.getList()
        cartList = getCartModel(adapterList.toCollection(arrayListOf()))
        Log.d(TAG, "addGuestToOrder: " + Gson().toJson(cartList))
        var existing_count = cartList?.dineInList!!.size - 1
        var total_count = existing_count+count
        if(total_count<=15)  {
            var existinglist: ArrayList<DineInModel> = arrayListOf()
            existinglist.addAll(cartList?.dineInList!!.toMutableList())
            Log.d(TAG, "addGuestToOrder size: " + existinglist.size)
            val dineInList: java.util.ArrayList<DineInModel> = arrayListOf()
            if (existinglist.isNotEmpty()) {
                for (i in 1..count) {
                    dineInList.add(
                        DineInModel(
                            0,
                            false,
                            0,
                            "Guest ${existing_count.plus(i)}",
                            floorPlanTable = cartList!!.dineInList!![0].floorPlanTable

                        )
                    )
                }

            }
            existinglist.addAll(dineInList)
            cartList?.dineInList = existinglist.toList()
            Log.d(TAG, "addGuestToOrder size: " + cartList!!.dineInList?.size)
            Log.d(TAG, "addGuestToOrder: " + Gson().toJson(cartList?.dineInList))
            val request = viewModel.updateOrderRequest(cartList!!)
            orderId?.let { viewModel.updateOrder(it, request) }
        }else{
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), "You can't add more than 15 Guest in an order."
            ) { _, _ ->
            }
        }

    }

    private fun getTotalTaxBirfurcation(item: TbItem, itemtype: TaxData): Double {
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
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                String.format("%.2f", itemTaxPrice)
                    .toDouble()
            }

        } else {
            Log.d("yash", "taxCalculation: " + itemtype.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", itemtype.rate * item.itemQuantity)
                    .toDouble()
            }

        }
        return totaltaxtemp
    }

    private fun taxBifurcationCalculation(
        item: TbItem,
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
            txtTotalcashAdj.text = "$" + String.format(
                "%.2f",
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
                Log.e(TAG, "ShowProgress ${it}")
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

        viewModel.msgText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
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
        listItemWT: ArrayList<TbItem>,
        listItemGuestSelected: ArrayList<TbItem>
    ) {

        //New Drag and Drop

        var divideDiscount = divideDiscount2
        Log.e("WholeTabDis", "Fasf  ${wholeTableDiscount}")
        var dividedWtDis: Double = MethodUtils.roundOffAmountDouble(
            wholeTableDiscount / (getOrderDetailsResponse?.guestAttributes?.size?.minus(1)!!)
        )

        divideDiscount += dividedWtDis
        Log.e("saff", "afadivideDiscount ${divideDiscount}")

        Log.e("TODAYBOLD", "subTotalB  ${subTotalGuest + dividedGuestAmt}")
        Log.e("TODAYBOLD", "totalGuest ${totalGuest}")
        Log.e("TODAYBOLD", "taxGuest ${taxGuest}")
        Log.e("TODAYBOLD", "serviceChargeGuest  ${serviceChargeGuest}")
        Log.e("TODAYBOLD", "divideDiscount ${divideDiscount}")
        val bundle = Bundle()
        bundle.putDouble("subTotalB", subTotalGuest + dividedGuestAmt)
        bundle.putDouble("totalB", totalGuest)
        bundle.putDouble("totalTaxB", taxGuest)
        bundle.putDouble("serviceChargeB", serviceChargeGuest)
        bundle.putDouble("dicountB", divideDiscount)


        val adapterList = dineInTableAdapter.getList()


        var subTotal = 0.0
        var amtToPay = 0.0
        val totalItem: ArrayList<TbItem> = arrayListOf()
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


        totalGuestCount = getOrderDetailsResponse?.guestAttributes?.size?.minus(1) ?: 1
        Log.e("TODAY", "totalGuestCount:  ${totalGuestCount}")
        Log.e("TODAY", "toFinalAmt:  ${toFinalAmt}")
        var divideCashDiscount = MethodUtils.calculateCashDiscount(
            toFinalAmt,
            prefProvider,
            requireContext()
        ) / totalGuestCount

        Log.e(TAG, "divideCashDiscount:  ${divideCashDiscount}")
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

        Log.e("AAjeChange", "listItemWT:  ${Gson().toJson(listItemWT)}")
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
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
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


            if (serviceChargeList?.isNotEmpty() == true) {
                Log.e("AajeChange", "serviceChargeList:  ${Gson().toJson(serviceChargeList)}")
                var isApplied = false
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
                                serviceCharge += MethodUtils.roundOffAmountDouble((wholeNewSubtotal * it.percentage) / 100)
                                return@forEach
                            }
                        }
                    }
                }
                if (!isApplied) {
                    serviceChargeList.forEach { service ->
                        if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                            serviceCharge += MethodUtils.roundOffAmountDouble((wholeNewSubtotal * service.percentage) / 100)
                            return@forEach
                        }
                    }
                }


            }

        }
        Log.e("AajeChange", "wholeNewSubtotal  ${wholeNewSubtotal}")
        Log.e("AajeChange", "WTTaxes  ${WTTaxes}")
        Log.e("AajeChange", "serviceCharge  ${serviceCharge}")
        Log.e("AajeChange", "totalGuestCount  ${totalGuestCount}")

        wholeNewSubtotal = MethodUtils.roundOffAmountDouble(wholeNewSubtotal / totalGuestCount)
        WTTaxes = MethodUtils.roundOffAmountDouble(WTTaxes / totalGuestCount)
        serviceCharge = MethodUtils.roundOffAmountDouble(serviceCharge / totalGuestCount)

        Log.e(
            "FinalDetails",
            "subTotal:  ${MethodUtils.roundOffAmountDouble(subTotalGuest + wholeNewSubtotal)}"
        )
        Log.e(
            "FinalDetails",
            "guestTotalTax:  ${MethodUtils.roundOffAmountDouble(taxGuest + WTTaxes)}"
        )
        Log.e(
            "FinalDetails",
            "dividedDiscount:  ${MethodUtils.roundOffAmountDouble(divideDiscount)}"
        )
        Log.e(
            "FinalDetails",
            "dividedCashdiscount :  ${MethodUtils.roundOffAmountDouble(divideCashDiscount)}"
        )
        Log.e(
            "FinalDetails",
            "guestTotalServiceCharge:  ${MethodUtils.roundOffAmountDouble(serviceChargeGuest + serviceCharge)}"
        )

        val newGuestModel = GuestDataModel(
            subTotal = MethodUtils.roundOffAmountDouble(subTotalGuest + wholeNewSubtotal),
            totalTax = MethodUtils.roundOffAmountDouble(taxGuest + WTTaxes),
            totalAmount = totalGuest,
            totalDiscount = MethodUtils.roundOffAmountDouble(divideDiscount),
            cashDiscount = MethodUtils.roundOffAmountDouble(divideCashDiscount),
            totalServiceCharge = MethodUtils.roundOffAmountDouble(serviceChargeGuest + serviceCharge)

        )
        Log.e(TAG, "newGuestModel:  ${Gson().toJson(newGuestModel)}")
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
        Log.e("MainTo", "wholeEmpty:  ${wholeEmpty}")
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

        Log.e("FinalLast", "FinalLast ${isLastPayment}")


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

        prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)
        bundle.putParcelable("dineinPaymentModel", dineinCartPaymentModel)
        cartList = getCartModel(adapterList.toCollection(arrayListOf()))

        var temp_itemslist: ArrayList<TbItem> = arrayListOf()


//        temp_itemslist.addAll(listItemWT)
        temp_itemslist.addAll(listItemGuestSelected)
        cartList?.taxlistDynamic = listOf()
        temp_itemslist.forEach { item ->
            cartList = taxBifurcationCalculation(item, cartList!!)
        }

        var remaining_list: List<TaxData> = emptyList()
        Log.e(TAG, "getcartListbeforeAdd  ${Gson().toJson(cartList?.taxlistDynamic)}")
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
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
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
                    }
                } else {
                    var data = taxData
                    data.subTotalAmount = temp_subtotal
                    data.totalTaxTypePrice = temp_remaining
                    remaining_list = listOf(data)
                }

            }
        }
        remaining_list.forEach { remainingdata ->
            cartList?.taxlistDynamic =
                concatenate(cartList?.taxlistDynamic!!, listOf(remainingdata))
        }


        Log.d(TAG, "getcartListAfterAdd: remaining : ${Gson().toJson(remaining_list)}")
        Log.e(TAG, "getcartListAfterAdd  ${Gson().toJson(cartList?.taxlistDynamic)}")

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

    override fun onSendItemToKitchen(item: TbItem) {

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

    override fun onWholeTableToKitchen(ids: String, list: ArrayList<TbItem>) {
        Log.e(TAG, "WholeTableITem")
        viewModel.fireItemToKitchen(orderId ?: 0, true, ids, true)
        for (i in 0 until kitchenPrinterList.size) {

            initKitchenPrinter(kitchenPrinterList.get(i), Constants.KITCHEN, list)
        }
    }

    override fun singleItemFired(id: String, position: Int, item: TbItem) {

        clickedPos = position
        viewModel.fireItemToKitchen(orderId ?: 0, true, id, false, item)

        var listItem: ArrayList<TbItem> = arrayListOf()
        listItem.add(item)
        for (i in 0 until kitchenPrinterList.size) {

            initKitchenPrinter(kitchenPrinterList.get(i), Constants.KITCHEN, listItem)
        }

    }

    override fun onGuestPrint(
        listItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
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

    private fun guestPrint(
        paymentStatus: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        wtItems: ArrayList<TbItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {

        Log.e(TAG, "customerListSize  ${customerList.size}")
        if (customerList.isNotEmpty()) {
            customerList.forEach {
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

    private fun navigateDineInOrderNew() {

        viewModel.Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
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
                        baseResponse.floorPlanTable?.merged_child_table_details.forEach {
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

                    //extract logic from API data and drag & drop code
                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val model = DineInModel()
                        var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                        model.title = baseResponse.guestAttributes.get(i).name
                        model.isHeader = 0
                        model.id = baseResponse.guestAttributes[i].id
                        model.isPaid = baseResponse.guestAttributes.get(i).isPaid
                        if (baseResponse.guestAttributes.get(i).guestItemAttributes.isNotEmpty()) {
                            if (baseResponse.guestAttributes.get(i).name.trim()
                                    .lowercase() != "Whole Table".trim().lowercase()
                            ) {
                                totalGuestCount++
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

                        dineInList.add(model)

                        var guestSubTotal: Double = 0.0
                        var guestTotalTax: Double = 0.0
                        var guestServiceCharge: Double = 0.0


                        for (j in 0 until baseResponse.guestAttributes.get(i).guestItemAttributes.size) {
                            if (baseResponse.orderItems.isNotEmpty()) {
                                baseResponse.orderItems.forEach {
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
                                        val item = TbItem()
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
                                                model.id = mod.id
                                                model.itemQuantity = mod.quantity
                                                model.name = mod.name
                                                model.orderModifierId = mod.orderItemId
                                                model.price = mod.price


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
                                        item.timeStamp = it.timestamp


                                        itemDineIn.isHeader = 1
                                        itemDineIn.item = item
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

                                                            val totalPrice = price + modifierPrice

                                                            val itemTaxPrice =
                                                                (tax.rate * totalPrice) / 100
                                                            Log.e("itemTaxPrice", "" + itemTaxPrice)
                                                            String.format("%.2f", itemTaxPrice)
                                                                .toDouble()
                                                        } else {

                                                            String.format(
                                                                "%.2f",
                                                                tax.rate * it.quantity
                                                            )
                                                                .toDouble()
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

                                                        String.format("%.2f", itemTaxPrice)
                                                            .toDouble()
                                                    } else {
                                                        if (totalPrice <= 0.0) {
                                                            String.format("%.2f", 0.00)
                                                                .toDouble()
                                                        } else {
                                                            String.format(
                                                                "%.2f",
                                                                tax.rate * oi.quantity
                                                            )
                                                                .toDouble()
                                                        }
                                                    }

                                                }
                                            }

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
                                        Log.e("TODO", "totalPriceWT:  ${totalPriceWT}")

                                        guestShareTotal =
                                            totalPriceWT / (baseResponse.guestAttributes.size - 1)
                                        Log.e("TODO", "subTotalWT:  ${subTotalWT}")
                                        Log.e("TODO", "totalTaxWT:  ${totalTaxWT}")
                                        Log.e("TODO", "serviceChargeWT:  ${serviceChargeWT}")

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

                    }
                    totalServiceChargeAmount = 0.0
                    if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                        var isApplied = false
                        serviceChargeList.forEach {
                            if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                if (isInRange(
                                        it.min_guest_count!!,
                                        it.max_guest_count!!,
                                        baseResponse.guestAttributes.size - 1
                                    )
                                ) {
                                    Log.d(
                                        TAG,
                                        "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + baseResponse.guestAttributes.size.minus(
                                            1
                                        )
                                    )
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


                    var finalAmount =
                        totalSubTotal + totalTaxAmount + totalServiceChargeAmount - orderDiscount
                    Log.e("TODO", "finalAmount  ${finalAmount}")
                    Log.e("TODO", "guestShareTotal  ${guestShareTotal}")
                    dineInList.get(0).guestDividedAmt = guestShareTotal
                    dineInList.get(0).totalGuestCount = baseResponse.guestAttributes.size - 1
                    dineInList.get(0).wholeTableSubTotal =
                        subTotalWT / (baseResponse.guestAttributes.size - 1)
                    dineInList.get(0).wholeTableTax =
                        totalTaxWT / (baseResponse.guestAttributes.size - 1)
                    dineInList.get(0).wholeTableSurTax =
                        serviceChargeWT / (baseResponse.guestAttributes.size - 1)
                    Log.e("WholeDiscount", "wholeTableDiscount  ${wholeTableDiscount}")
                    dineInList.get(0).wholeTableDiscont =
                        MethodUtils.roundOffAmountDouble(wholeTableDiscount / (baseResponse.guestAttributes.size - 1))

                    dineInList.get(0).orderDiscount = orderDiscount
                    dineInList.get(0).orderTotalAmount =
                        MethodUtils.roundOffAmountDouble(baseResponse.subTotal + baseResponse.totalTaxAmount + baseResponse.totalServiceCharges)



                    Log.e(TAG, "totalTaxAmount:  ${totalTaxAmount}")
                    viewModel.totalTaxAmount = totalTaxAmount
                    subTotalDInin = totalSubTotal - orderDiscount
                    serviceCharge = totalServiceChargeAmount
                    totalDiscount = orderDiscount + totalItemDiscount
                    finalTaxAmt = totalTaxAmount
                    Log.e(TAG, "GotsubTotalDInin  ${subTotalDInin}")
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
                    Log.e("TODO", "paidGuestCount:  ${paidGuestCount}")
                    if (paidGuestCount > 0) {

                        paidGuestAmount = paidGuestCount

                        if (paidGuestCount > 0) {
                            paidGuestAmount = paidGuestCount
                            var perGTotal =
                                subTotalWT / (baseResponse.guestAttributes.size - 1)
                            Log.e(TAG, "perGTotal:  ${perGTotal}")
                            subTotalDInin = totalSubTotal - (perGTotal * paidGuestCount)
                        } else {
                            subTotalDInin = totalSubTotal
                        }

                        //subTotalDInin -= baseResponse.totalDiscount


                        Log.d("TODO", "suTotalPaidGuest: " + subTotalDInin)

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
                                                totalGuestCount
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

                        var unpaidCount = (baseResponse.guestAttributes.size - 1) - paidGuestCount

                        if (paidGuestCount > 0) {
                            var tempTax = totalTaxWT / (baseResponse.guestAttributes.size - 1)

                            var guestTax = totalTaxAmount - totalTaxWT
                            finalTaxAmt = (tempTax * unpaidCount) + guestTax
                            //finalTaxAmt = (subTotalWT / totalGuestCount) * unpaidCount + totalTaxAmt
                        }

                        var perGuestorderDis =
                            orderDiscount / (baseResponse.guestAttributes.size - 1)
                        var orderDis = orderDiscount - (perGuestorderDis * paidGuestCount)



                        Log.e("MYFN", "subTotalDInin  ${subTotalDInin}")
                        Log.e("MYFN", "serviceCharge  ${serviceCharge}")
                        Log.e("MYFN", "finalTaxAmt  ${finalTaxAmt}")
                        Log.e("MYFN", "orderDis  ${orderDis}")


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

                    Log.e(TAG, "paidGuestCount  ${paidGuestCount}")
                    if (dineInList.isNotEmpty()) {
                        dineInTableAdapter.setList(dineInList)
                        checkForAutoFire(true)


                        //  binding.txtTotalAmountNew.setText("${MethodUtils.roundOffAmount(totalAmtnew)}")

                        if (!notPayAnyAmount) {
                            touchHelper.attachToRecyclerView(binding.rvItemList)
                        }
                    }

                    Log.e(TAG, "notPayAnyAmount  ${notPayAnyAmount}")
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

    fun getTaxFromTotalPrice(
        orderItemTaxe: TaxData,
        totalPrice: Double,
        item: TbItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                String.format("%.2f", itemTaxPrice)
                    .toDouble()
            }

        } else {
            Log.d("yash", "taxCalculation: " + orderItemTaxe.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", orderItemTaxe.rate * item.quantity)
                    .toDouble()
            }
        }
        return totaltaxtemp
    }

    private fun getTaxBirfucationList(orderItems: List<TbItem>): ArrayList<TaxData> {
        var taxListDynamic: ArrayList<TaxData> = arrayListOf()
        if (orderItems.isNotEmpty()) {
            orderItems.forEach { orderItem ->
                var totalPrice = orderItem.price * orderItem.quantity
                orderItem.modifiers.forEach { orderItemModifier ->
                    totalPrice += orderItemModifier.price * orderItemModifier.itemQuantity
                }
                Log.d(TAG, "navigate: itemPrice : $totalPrice")
                var totaltaxtemp = 0.0
                orderItem.taxes!!.forEach { orderItemTaxe ->
                    if (taxListDynamic?.isNotEmpty() == true) {
                        var found = -1
                        taxListDynamic.forEachIndexed { index, taxData ->
                            if (taxData.orderTaxId == orderItemTaxe.orderTaxId) {
                                found = index
                                return@forEachIndexed
                            }
                        }
                        if (found == -1) {
                            var taxData: TaxData = TaxData(
                                orderItemTaxe.createdAt,
                                orderItemTaxe.orderTaxId!!,
                                0,
                                orderItemTaxe.name,
                                orderItemTaxe.rate,
                                orderItemTaxe.taxType,
                                orderItemTaxe.updatedAt,
                                true,
                                orderItemTaxe.isDefault,
                                false,
                                "",
                                orderItemTaxe.itemIds,
                                orderItemTaxe.orderTaxId,
                                false,
                                getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                ),
                                totalPrice
                            )
                            taxListDynamic?.add(taxData)
                        } else {
                            taxListDynamic!![found].totalTaxTypePrice =
                                taxListDynamic!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                )
                            taxListDynamic!![found].subTotalAmount =
                                taxListDynamic!![found].subTotalAmount + totalPrice
                        }
                        Log.d(TAG, "found : " + found)
                    } else {
                        var taxData: TaxData = TaxData(
                            orderItemTaxe.createdAt,
                            orderItemTaxe.orderTaxId!!,
                            0,
                            orderItemTaxe.name,
                            orderItemTaxe.rate,
                            orderItemTaxe.taxType,
                            orderItemTaxe.updatedAt,
                            true,
                            orderItemTaxe.isDefault,
                            false,
                            "",
                            orderItemTaxe.itemIds,
                            orderItemTaxe.orderTaxId,
                            false,
                            getTaxFromTotalPrice(
                                orderItemTaxe,
                                totalPrice,
                                orderItem
                            ),
                            totalPrice
                        )
                        taxListDynamic.add(taxData)
                    }


                    Log.d(TAG, "navigate: " + totaltaxtemp)
                }

            }

            Log.d(TAG, "navigate: list " + Gson().toJson(taxListDynamic))
        }
        return taxListDynamic
    }

    fun getCartModel(list: ArrayList<DineInModel>): CartModel {
        var model = CartModel()
        var listItem: ArrayList<TbItem> = arrayListOf()
        var dineInItems: ArrayList<TbItem> = arrayListOf()
        var newDineInList: ArrayList<DineInModel> = arrayListOf()
        var dineinModel: DineInModel = DineInModel()
        Log.e(TAG, "orderDiscountGEt:  ${list[0].orderDiscount}")
        Log.e(TAG, "dineExtractList  ${Gson().toJson(list)}")
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
                            dineinModel.items.addAll(dineInItems)
                            dineInItems = arrayListOf()
                            break
                        }
                    } else {
                        dineinModel.items.addAll(dineInItems)
                        dineInItems = arrayListOf()
                        break
                    }


                }

                newDineInList.add(dineinModel)
            }

        }
        model.orderType = "DineIn"
        model.dineInList = newDineInList
        model.items = listItem
        model.discountPrice = list[0].orderDiscount
        model.serviceCharge = list[0].serviceChargeList
        model.employeeID = prefProvider.getValueInt(EMPLOYEE_ID, 0)
        model.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
        model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
        model.taxlistDynamic = getTaxBirfucationList(listItem)


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


        var oldList = dineInTableAdapter.getList()
        var newList: ArrayList<DineInModel> = arrayListOf()
        var wholeTableAmt = 0.0
        var WTTax = 0.0
        var WTServiceCharge = 0.0
        var totalPaid = 0.0
        var guestShare = 0.0
        var totalTablePrice = 0.0
        var WTDiscount = 0.0
        var guestCount = 0

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
            }


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
                                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                                    String.format("%.2f", itemTaxPrice)
                                        .toDouble()
                                } else {

                                    String.format(
                                        "%.2f",
                                        tax.rate * it.itemQuantity
                                    )
                                        .toDouble()
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
                                (guestCount - 1)
                            )
                        ) {
                            isApplied = true
                            Log.d(
                                TAG,
                                "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + (guestCount - 1)
                            )
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
        Log.e("AfterMove", "wholeTableAmt  ${wholeTableAmt}")
        Log.e("AfterMove", "WTServiceCharge  ${WTServiceCharge}")
        Log.e("AfterMove", "WTTax  ${WTTax}")
        Log.e("AfterMove", "WTTax  ${WTDiscount}")
        Log.e("AfterMode", "guestCount  ${guestCount}")
        guestShare = (wholeTableAmt + WTServiceCharge + WTTax) / (guestCount - 1)


        for (i in 0 until oldList.size) {
            var model = DineInModel()
            if (oldList.get(i).isHeader == 1) {
                model.item = oldList.get(i).item
                model.totalTableAmt = totalTablePrice


            } else {
                model.title = oldList.get(i).title
                model.customer = oldList.get(i).customer

                model.guestDividedAmt = guestShare
                Log.d("two", "navigateDineInOrder: " + model.guestDividedAmt)
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
        newList.get(0).wholeTableSubTotal =
            wholeTableAmt / (guestCount - 1)
        newList.get(0).wholeTableTax =
            WTTax / (guestCount - 1)
        newList.get(0).wholeTableSurTax =
            WTServiceCharge / (guestCount - 1)
        newList.get(0).orderDiscount = getOrderDetailsResponse?.totalDiscount ?: 0.0
        newList.get(0).orderTotalAmount = subTotalDInin
        totalGuestCount = guestCount - 1


        dineInTableAdapter.setList(newList)

        updateOrderCall()

    }

    private fun updateOrderCall() {
        val list = dineInTableAdapter.getList()

        val orderModel = OrderAttributeRequestModel()

        orderModel.apply {
            id = orderId
            date = TimeFormatUtils.getCurrentDate()
            deliveryType = "DineIn"
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 1)
            offlineId = getOrderDetailsResponse?.offlineId.toString()
            openOrderType = "DineIn"
            orderTypeId = 2
            paymentStatus = 0
            getOrderDetailsResponse?.subTotal?.let {
                subTotal = it
            }
            getOrderDetailsResponse?.totalAmount?.let {
                totalAmount = it
            }
            getOrderDetailsResponse?.totalServiceCharges?.let {
                totalServiceCharges = it
            }

            val listOrderAttribute: ArrayList<OrderItemsAttribute> = arrayListOf()

            /* getOrderDetailsResponse?.orderItems?.let {
                 for (i in 0 until it.size) {
                     val model = OrderItemsAttribute()
                     model.category_id = it.get(i).categoryId
                     model.discountAmount = it.get(i).discountAmount
                     model.discountType = it.get(i).discountType
                     model.editTimestamp = it.get(i).timestamp
                     model.employeeId = it.get(i).employeeId
                     model.id = it.get(i).id
                     model.isEdited = true
                     model.itemId = it.get(i).itemId
                     model.orderId = it.get(i).orderId
                     model.price = it.get(i).price
                     model.quantity = it.get(i).quantity
                     model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                     model.timestamp = System.currentTimeMillis().toString()
                     model.totalPrice = it.get(i).totalPrice
                     if (it.get(i).orderItemModifiers.isNotEmpty()) {
                         var listModifiers: ArrayList<OrderItemModifierAttribute> =
                             arrayListOf()
                         it.get(i).orderItemModifiers.forEach {
                             val model = OrderItemModifierAttribute()
                             model.id = it.id
                             model.order_item_id = it.orderItemId
                             model.name = it.name
                             model.orderId = it.orderId
                             model.price = it.price
                             model.totalPrice = it.price
                             model.quantity = it.quantity
                             it.modifierId?.let {
                                 model.modifier_set_id = it.toInt()
                             }
                             var listTaxAttributes: ArrayList<OrderModifierTaxesAttribute> =
                                 arrayListOf()

                             if (it.orderItemTaxes.isNotEmpty()) {

                                 it.orderItemTaxes.forEach {
                                     val model = OrderModifierTaxesAttribute()
                                     model.id = it.id
                                     model.amount = it.amount
                                     model.isDefault = it.isDefault
                                     model.name = it.name

                                     listTaxAttributes.add(model)
                                 }


                             }
                             model.order_item_taxes_attributes = listTaxAttributes

                             listModifiers.add(model)

                         }
                         model.orderItemModifiersAttributes = listModifiers
                     }


                     listOrderAttribute.add(
                         model
                     )


                 }
             }*/

            val guestAttributes: ArrayList<GuestsAttributes> = arrayListOf()
            for (i in 0 until list.size) {

                val model = GuestsAttributes()
                if (list.get(i).isHeader == 1) {

                } else {

                }


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


                }


                dragFrom = -1
                dragTo = -1
                updateAdapterData()

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
        paymentType: String,
        guestPrint: Boolean,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {


        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, customerReceiptPrinters.ipAddress)

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

                            } else {
                                generatePrintSunmi(customerReceiptPrinters, type, "")
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

                    } else {
                        generatePrintSunmi(customerReceiptPrinters, type, "")
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
                        paymentType,
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
                    setService1()
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
                        Log.e(TAG, "PrinterException: " + e.message)
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

                            } else {
                                generatePrint(customerReceiptPrinters, type, "")
                            }

                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Log.e(TAG, "PrinterIsNotNull:")
                }
            }
        }
    }

    private fun generateGuestPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
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
                    Log.e(TAG, "getTaxP  ${Gson().toJson(tax)}")
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

                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else {

                        String.format("%.2f", tax.rate * it.itemQuantity)
                            .toDouble()
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
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
            )

            builder.addFeedLine(1)

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

            builder.addText(getOrderDetailsResponse?.orderType + "\n")

            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)

                }
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
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

            Log.e(TAG, "listWTitemsItemsGet  ${Gson().toJson(listWTitems)}")
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

            if (guestServiceCharge != null) {
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
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
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
                    Log.e(TAG, "getTaxP  ${Gson().toJson(tax)}")
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

                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else {

                        String.format("%.2f", tax.rate * it.itemQuantity)
                            .toDouble()
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
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )

            getOrderDetailsResponse?.orderType?.let { PrintSunmiUtils.printOrderType(it) }

            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {

                    PrintSunmiUtils.orderId("OrderID:" + getOrderDetailsResponse?.id)

                }

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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
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

            if (guestServiceCharge != null) {


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
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
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
                    Log.e(TAG, "getTaxP  ${Gson().toJson(tax)}")
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

                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else {

                        String.format("%.2f", tax.rate * it.itemQuantity)
                            .toDouble()
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

                PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
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
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )
            SunmiPrintHelper.getInstance().lineWrap(1)
            getOrderDetailsResponse?.orderType?.let { PrintSunmiUtils.headerText(it) }
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

            if (guestServiceCharge != null) {


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

                Log.e(
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
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
            )

            builder.addFeedLine(1)

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

            builder.addText(getOrderDetailsResponse?.orderType + "\n")

            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)

                }
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
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
            Log.e(TAG, "subTotalWT  ${subTotalDInin}")

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

            if (serviceCharge != null) {
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
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
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

            PrintSunmiUtils.paymentType("Unpaid")

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )


            getOrderDetailsResponse?.orderType?.let { PrintSunmiUtils.printOrderType(it) }


            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {

                    PrintSunmiUtils.orderId("OrderID:" + getOrderDetailsResponse?.id)

                }

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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
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

            if (serviceCharge != null) {

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
            }


            if (customerSettingModel.showQrCode) {

                getOrderDetailsResponse?.digitalReceiptUrl?.let { Log.e("digitalReceiptUrl", it) }

                getOrderDetailsResponse?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCode(it) }

            }

            SunmiPrinterApi.getInstance().lineWrap(5)
            SunmiPrinterApi.getInstance().cutPaper(1, 1)

            SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generatePrintSunmiInner(
    ) {


        try {
            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {

                PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
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
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )


            SunmiPrintHelper.getInstance().lineWrap(1)
            getOrderDetailsResponse?.orderType?.let { PrintSunmiUtils.headerText(it) }
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

            if (serviceCharge != null) {

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
        item: ArrayList<TbItem>

    ) {


        if (data.name.startsWith(SUNMI_PRINTER, true)) {

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
                        }

                        override fun onConnect() {
                            println("onConnect")
                            generateKitchenReceiptSunmi(data, type, item)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {

                if (SunmiPrinterApi.getInstance().isConnected)
                    generateKitchenReceiptSunmi(data, type, item)
            }

        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(data, type, item)
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
                    Log.e(TAG, "PrinterException: " + e.message)
                    printer = null
                    return
                }

                if (printer != null) {
                    PrinterClass.setPrinter(printer)

                    generateKitchenReceipt(data, type, item)

                }

            } else {
                Log.e(TAG, "PrinterIsNotNull:")
            }
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbItem>
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

                    addBuilderText(builder, getOrderDetailsResponse?.orderType.toString())
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

                builder.addText(
                    padLine(
                        "OrderID:" + getOrderDetailsResponse?.id,
                        "",
                        33
                    )
                )

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
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
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


                addOrdersForKitchenDineIn(builder, item, fontSizeH, fontSizeW)

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

                    addBuilderText(builder, getOrderDetailsResponse?.orderType.toString())
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

                builder.addText(
                    padLine(
                        "OrderID:" + getOrderDetailsResponse?.id,
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

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
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
                        },
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
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


                addOrdersForKitchenCustoemrPrinter(builder, item, fontSizeH, fontSizeW)

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

                timeOut = 1000
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
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setService(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbItem>
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(data, type, item)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(data, type, item)
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService1(
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generatePrintSunmiInner()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService1()
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService2(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
        subTotalGuest: Double,
        total: Double,
        taxGuest: Double,
        serviceChargeGuest: Double,
        divideDiscount: Double
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

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
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbItem>
    ) {
        try {

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.printOrderType(getOrderDetailsResponse?.orderType.toString())

                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.addValue(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")

            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.orderId(
                padLine(
                    "OrderID:" + getOrderDetailsResponse?.id,
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )

            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.receiptID(
                padLine(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    },
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )

            SunmiPrinterApi.getInstance().lineWrap(1)

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

            PrintSunmiUtils.addHorizontal()

            SunmiPrinterApi.getInstance().lineWrap(1)

            addOrdersForKitchenDineIn(item)

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getOrderDetailsResponse?.note.toString())
            }


            PrintSunmiUtils.cutPaper()


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbItem>
    ) {
        try {

            PrintSunmiUtils.fontSizeInner(Constants.LARGE)

            SunmiPrintHelper.getInstance().initPrinter()

            PrintSunmiUtils.headerText("OrderID:" + getOrderDetailsResponse?.id)
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {

                PrintSunmiUtils.headerText(getOrderDetailsResponse?.orderType.toString())

                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.normalTextCenter(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")")

            SunmiPrintHelper.getInstance().lineWrap(1)

            PrintSunmiUtils.normalText(
                padLine(
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    },
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 23
                ).toString()
            )

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showTeamMember) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Employee:" + getOrderDetailsResponse?.employee?.name, "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 23
                    ).toString()
                )

            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            PrintSunmiUtils.normalText(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        getOrderDetailsResponse?.createdAt.toString()
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 23
                ).toString()
            )

            PrintSunmiUtils.addHorizontalInner()

            SunmiPrintHelper.getInstance().lineWrap(1)

            addOrdersForKitchenDineInInner(item)

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInner(getOrderDetailsResponse?.note.toString())
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
                        findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
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
            Log.e(TAG, "FireAllStatusObserved")

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

                        listModifiers.add(modifierModel)


                    }
                    orderItem.orderItemModifiersAttributes = listModifiers

                    orderItemsAttributes.add(orderItem)


                }

                var orderRequest = OrderAttributeRequestModel()

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
        var list = dineInTableAdapter.getList()
        val builder = ArrayList<String>()
        var listItem: ArrayList<TbItem> = arrayListOf()
        Log.e(TAG, "dineInList:  ${Gson().toJson(list)}")
        list.forEach {
            if (it.isHeader == 1) {
                it.item?.let {
                    if (!it.isFired) {
                        listItem.add(it)
                    }
                }
                it.item?.orderItemId?.let {
                    builder.add(it.toString())

                }
                //  it.item?.isFired = true

            }

        }
        Log.e(TAG, "listItem:  ${Gson().toJson(listItem)}")
        val serializedObject: String =
            prefProvider.getValue(Constants.DINE_IN_UPDATE_LIST, "")
        Log.e(TAG, "serializedObjectData:  ${Gson().toJson(serializedObject)}")
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

            Log.e(TAG, "arrayItems:  ${Gson().toJson(arrayItems)}")
            var itemIds: ArrayList<Int> = arrayListOf()
            arrayItems.forEach {
                itemIds.add(it.id)
            }

            arrayItems.forEachIndexed { index, orderItem ->
                if (getOrderDetailsResponse?.orderItems?.size!!.minus(1) >= index) {
                    if (itemIds.contains(getOrderDetailsResponse?.orderItems?.get(index)?.id)) {
                        Log.e("InsideLoop", "Inside")
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
                                    var tbItem: TbItem = TbItem()
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
            var autoPrintEnable = false
            kitchenPrinterList.forEach { kit ->
                if (isCheckAndFire) {
                    kit.orderTypes.forEach {
                        if (it.orderTypeId == getOrderDetailsResponse?.orderTypeId) {
                            Log.e(TAG, "orderTypeIdSettings  ${it.orderTypeName}")
                            Log.e(TAG, "orderTypeIdMainData  ${getOrderDetailsResponse?.orderType}")
                            it.printerSettings.forEach {
                                if (it.printType.lowercase()
                                        .equals(Constants.KITCHEN.lowercase()) && it.autoPrinting
                                ) {

                                    autoPrintEnable = true
                                    initKitchenPrinter(kit, Constants.KITCHEN, listItem)
                                }
                            }
                        }

                    }
                } else {
                    initKitchenPrinter(kit, Constants.KITCHEN, listItem)

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

    override fun onPause() {
        super.onPause()
        if (pd != null && pd.isShowing) {
            pd.dismiss()
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


