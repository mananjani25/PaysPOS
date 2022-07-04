package com.android.pos.ui.fragments.transactions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.android.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.SUNMI_PRINTER
import com.android.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.android.pos.databinding.FragmentTransactionDetailsBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.OrderDetailsItemListAdapter
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.utils.*
import com.android.pos.utils.TimeFormatUtils.convertCurrentDate
import com.android.pos.utils.TimeFormatUtils.convertCurrentTime
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionDetailsBinding
    private val viewModel by viewModels<TransactionDetailsViewModel>()

    private lateinit var orderDetailsItemAdapter: OrderDetailsItemListAdapter
    private var orderIDglobal = 0

    //    private lateinit var orderDetailsResponse: GetOrderDetailsResponse
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private lateinit var paymentDetailsResponse: GetPaymentOrderDetailsResponse
    private var orderId: Int = -1
    private val TAG = "TransactionDetailsFr"
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private var paymentId: Int = -1
    private var isFromTrans: Boolean = false
    private var isFromOnlineOrderRefund: Boolean = false
    private var serviceChargesList: ArrayList<TbServiceCharge>? = arrayListOf()
    private var isSplitPayment = false


    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_transaction_details,
            container,
            false
        )

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        orderId = arguments?.getInt("orderId")!!
        paymentId = arguments?.getInt("paymentId")!!
        isFromTrans = arguments?.getBoolean("isFromTrans")!!
        isFromOnlineOrderRefund = arguments?.getBoolean("isFromOnlineOrderRefund")!!
//        if (isFromTrans) {
        viewModel.apiCallPaymentDetails(paymentId)
//        } else {
//            viewModel.apiCallOrderDetails(orderId)
//
//        }
        observeTipsList()
        setupSnackbar()
        observeShowProgress()
        setUpRecyclerView()
        navigate()
        getCustomerReceiptSettings()

        if (isFromOnlineOrderRefund) {
            acceptedAndDeclineOrder()
        }



        viewModel.serviceCharges.observe(requireActivity()) {
            if (prefProvider.getValue(
                    Constants.ORDER_TYPE,
                    Constants.TAKEOUT
                ) == Constants.DINE_IN
            ) {
                serviceChargesList = arrayListOf()
                serviceChargesList = it.data as ArrayList<TbServiceCharge>?
            } else {
                if (prefProvider.getValueboolean(
                        Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                        false
                    )
                ) {
                    serviceChargesList = arrayListOf()
                    Log.e(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                    it.data?.forEach { service ->
                        if (service.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                            serviceChargesList?.add(service)
                        }
                    }

                }
            }


        }
        return binding.root
    }

    private fun setUpRecyclerView() {
        orderDetailsItemAdapter = OrderDetailsItemListAdapter()
        binding.rvOrderItems.adapter = orderDetailsItemAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var selectedorderType = arguments?.getInt("selectedorderType")
        var selectedtransactionType = arguments?.getInt("selectedtransactionType")
        var selectedroleType = arguments?.getInt("selectedroleType")
        var selectedemployeeType = arguments?.getInt("selectedemployeeType")
        var selectedterminalType = arguments?.getInt("selectedterminalType")
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    var bundle: Bundle = Bundle()
                    bundle.putInt("selectedorderType", selectedorderType!!)
                    bundle.putInt("selectedtransactionType", selectedtransactionType!!)
                    bundle.putInt("selectedroleType", selectedroleType!!)
                    bundle.putInt("selectedemployeeType", selectedemployeeType!!)
                    bundle.putInt("selectedterminalType", selectedterminalType!!)
                    sendBackData(bundle)

                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.imgBack.setOnClickListener {
            var bundle: Bundle = Bundle()
            bundle.putInt("selectedorderType", selectedorderType!!)
            bundle.putInt("selectedtransactionType", selectedtransactionType!!)
            bundle.putInt("selectedroleType", selectedroleType!!)
            bundle.putInt("selectedemployeeType", selectedemployeeType!!)
            bundle.putInt("selectedterminalType", selectedterminalType!!)
            sendBackData(bundle)
        }

        binding.txtHome.setOnClickListener {
            val navControll = findNavController()
            navControll.navigate(R.id.action_transactionDetailsFragment_to_dashboardboldpos)
        }

        binding.txtPrintReceipt.setOnClickListener {
            getCustomerPrinters()

        }
        binding.txtPrintKitchenReceipt.setOnClickListener {
            //getCustomerPrinters()

        }

        binding.tvIssueRefund.setOnClickListener {
            if (paymentDetailsResponse.data.order.order_type == "OnlineWebOrder") {
                lateinit var refundData: RefundRequestModelOnlineOrder
                var employeeIdtemp = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
                var orderItemRefundsAttributesList =
                    ArrayList<RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute>()
                paymentDetailsResponse.data.order.order_items.forEach { item ->
                    val orderItemRefundsAttributeModel =
                        RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute()
                    orderItemRefundsAttributeModel.amount = item.totalPrice
                    orderItemRefundsAttributeModel.employeeId = employeeIdtemp
                    orderItemRefundsAttributeModel.orderId = item.orderId
                    orderItemRefundsAttributeModel.refundType = 0
                    orderItemRefundsAttributeModel.paymentId = paymentDetailsResponse.data.id
                    orderItemRefundsAttributeModel.orderItemId = item.id
                    orderItemRefundsAttributeModel.quantity = item.quantity
                    orderItemRefundsAttributesList.add(orderItemRefundsAttributeModel)
                }

                refundData = RefundRequestModelOnlineOrder().apply {
                    paymentRefund = RefundRequestModelOnlineOrder.PaymentRefund().apply {
                        amount =
                            paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips
                        orderId = paymentDetailsResponse.data.order_id
                        paymentId = paymentDetailsResponse.data.id
                        employeeId = employeeIdtemp
                        taxRefunded = paymentDetailsResponse.data.tax_amount
                        tipsRefunded = paymentDetailsResponse.data.tips
                        terminalId = terminal_id
                        serviceChargeRefunded =
                            paymentDetailsResponse.data.service_charge_amount
                        cash_discount_or_surcharge_refunded =
                            paymentDetailsResponse.data.cash_discount_or_surcharge
                        subtotal_refunded = paymentDetailsResponse.data.sub_total
                        orderItemRefundsAttributes = orderItemRefundsAttributesList
                    }
                }
                val bundle = Bundle().apply {
                    putParcelable("refundData", refundData)
                    putDouble(
                        "refundAmount",
                        paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips
                    )
                    putString("paymentType", paymentDetailsResponse.data.payment_type)
                    putBoolean("isfromTransaction", true)
                    putString(
                        "magensa_response_data",
                        paymentDetailsResponse.data.magensa_response_data
                    )
                }
                findNavController().navigate(
                    R.id.action_transaction_to_reasonForrefundonline,
                    bundle
                )

            } else {
                val bundle = Bundle().apply {
                    paymentDetailsResponse.data.order.order_items.forEach {
                        it.isChecked = true
                    }
                    putInt("paymentId", paymentId)
                    putParcelable("orderDetailsResponse", paymentDetailsResponse)
                    putBoolean("isSplitPayment", isSplitPayment)
                    putParcelableArrayList("serviceChargesList", serviceChargesList)
                }
                findNavController().navigate(
                    R.id.action_transactionDetailsFragment_to_issueRefundFragment,
                    bundle
                )
            }

        }

        binding.txtTextReceipt.setOnClickListener {
            // 1 : text
            openReceiptDialog(1)
        }

        binding.txtEmailReceipt.setOnClickListener {
            // 1 : email
            openReceiptDialog(2)

        }
    }

    private fun sendBackData(bundle: Bundle) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(KEY, bundle)
        findNavController().popBackStack(R.id.transactionFragment, false)
    }

    private fun openReceiptDialog(type: Int) {
        val bundle = Bundle()
        bundle.putInt("orderId", orderId)
        bundle.putInt("type", type)
        findNavController().navigate(
            R.id.action_transactionDetailsFragment_to_sendReceiptFragment,
            bundle
        )
    }

    @SuppressLint("SetTextI18n")
    private fun navigate() {
        ProgressUtils.showProgressDialog(requireActivity())
        viewModel.dataPayment.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                orderIDglobal = it.data.order.id

                paymentDetailsResponse = it

                if (paymentDetailsResponse.data.order.order_split_type == "OrderAmountTab" || paymentDetailsResponse.data.order.order_split_type == "OrderGuestTab") {
                    isSplitPayment = true
                }

                if (paymentDetailsResponse.data.payment_type == "Card")
                    binding.txtCardDetails.text =
                        paymentDetailsResponse.data.card_name + " " + paymentDetailsResponse.data.card_number


                binding.tvDate.text =
                    convertCurrentDate(
                        it.data.order.created_at,
                        context
                    ) + " " + convertCurrentTime(
                        it.data.order.created_at,
                        context
                    )

                binding.tvTransactionTime.text =
                    convertCurrentTime(
                        it.data.order.created_at,
                        context
                    )

                binding.tvTransactionDate.text = convertCurrentDate(
                    it.data.order.created_at,
                    context
                )
                if (it.data.order.note.isNotEmpty()) {
                    binding.llNotes.visibility = View.VISIBLE
                    binding.tvNote.text = it.data.order.note.toString()
                }

                if (it.data.order.customer != null) {
                    binding.tvCustomerName.text =
                        it.data.order.customer.firstName + " " + it.data.order.customer.lastName
                } else {
                    binding.tvCustomerName.text = ""
                }
                binding.orderDetails = it
                orderDetailsItemAdapter.addOrderDetailsItems(it.data.order.order_items)





                binding.llDiscount.visibility = View.VISIBLE
                if (paymentDetailsResponse.data.total_discount != 0.0) {
                    binding.txtDiscount.text = "$" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.total_discount
                    )
                } else {
                    binding.txtDiscount.text = "$" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.order.total_discount
                    )
                }

                if (paymentDetailsResponse.data.is_loyalty_applied == true) {
                    binding.llLoyalty.visibility = View.VISIBLE
                    binding.llLoyaltyPoints.visibility = View.VISIBLE
                }

                if (!paymentDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                    binding.llRefundAmount.visibility = View.VISIBLE
                }

                /*if (orderDetailsResponse.data.totalAmount == orderDetailsResponse.data.refundDetails.refundedAmount) {
                    binding.tvIssueRefund.visibility = View.GONE
                }*/

                if (!paymentDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                    binding.tvIssueRefund.visibility = View.GONE
                }

                if (paymentDetailsResponse.data.order.open_order_type.equals(
                        "Open Order",
                        ignoreCase = true
                    ) &&
                    (paymentDetailsResponse.data.order.open_order_type.equals(
                        "unpaid",
                        ignoreCase = true
                    ) ||
                            paymentDetailsResponse.data.order.open_order_type.equals(
                                "cancelled",
                                ignoreCase = true
                            ))
                ) {
                    binding.tvIssueRefund.visibility = View.GONE
                }


                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (paymentDetailsResponse.data.payment_type == "Card") {
                        if (paymentDetailsResponse.data.cash_discount_type == "SurCharge") {
                            binding.linearCashDiscount.visibility = View.VISIBLE
                            binding.labelCashsurcharge?.text = "SurCharge"
                            binding.txtCashAmounntDiscount.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.txtColor
                                )
                            )
                            binding.txtCashAmounntDiscount.text = "+ $" + String.format(
                                "%.2f",
                                paymentDetailsResponse.data.cash_discount_or_surcharge
                            )
                        } else {
                            binding.linearCashDiscount.visibility = View.GONE
                        }
                    } else {
                        if (paymentDetailsResponse.data.cash_discount_type == "CashDiscount") {
                            binding.linearCashDiscount.visibility = View.VISIBLE
                            binding.labelCashsurcharge?.text = "Cash Discount"
                            binding.txtCashAmounntDiscount.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorRed
                                )
                            )
                            binding.txtCashAmounntDiscount.text = "- $" + String.format(
                                "%.2f",
                                paymentDetailsResponse.data.cash_discount_or_surcharge
                            )
                        } else {
                            binding.linearCashDiscount.visibility = View.GONE
                        }
                    }
                } else {
                    binding.linearCashDiscount.visibility = View.GONE
                }




                ProgressUtils.dismissProgressDialog()
            }
        }

    }

    private fun acceptedAndDeclineOrder() {
        var employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        viewModel.acceptedAndDeclineOrder(
            0,
            orderId,
            false,
            employee_id,
            terminal_id
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = it
            }
        }

    }

    private fun getCustomerPrinters() {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        customerList.forEach {
                            initPrinter(it, Constants.CUSTOMER)
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
        type: String
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
                            generatePrintSunmi(customerReceiptPrinters, type)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generatePrintSunmi(customerReceiptPrinters, type)
            }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            setService()

        } else {

            viewLifecycleOwner.lifecycleScope.launch {
                Log.e(TAG, "getPrinter:  ${PrinterClass.getPrinter()}")
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    var printer: Print? = Print(requireContext())
                    if (printer != null) {
                        //printer.setStatusChangeEventCallback(this)
                        //printer.setBatteryStatusChangeEventCallback(this)
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
                        // printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        Log.e(TAG, "PrinterException: " + e.message)
                        printer = null
                        return@launch
                    }
                    try {

                        if (printer != null) {
                            PrinterClass.setPrinter(printer)

                            generatePrint(customerReceiptPrinters, type)

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

    private fun setService(
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateInnerPrintSunmi()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(
                )
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
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

            Log.e(TAG, "getVanueLogo:  ${prefProvider.getValue(Constants.VENUE_LOGO, "")}")
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

            addBuilderText(builder, prefProvider.getValue(Constants.BUSINESS_NAME, "").toString())
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


            paymentDetailsResponse?.data?.order?.venue_website?.let {
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
                addBuilderText(builder, it)
            }
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
            builder.addText(paymentDetailsResponse.data.order.order_type + "\n")

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

                    builder.addText("OrderID:" + paymentDetailsResponse.data.order.id)

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

                builder.addText("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)

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


                    builder.addText("Employee:" + paymentDetailsResponse?.data.order.employee)

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


                    Log.e(TAG, "created_atDate:  ${paymentDetailsResponse?.data.order.created_at}")
                    Log.e(
                        TAG,
                        "ConvertDateTime:  ${
                            Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order.created_at.toString()
                            )
                        }"
                    )
                    builder.addText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
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
                            "OrderID:" + paymentDetailsResponse?.data.order.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
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
                                "Employee:" + paymentDetailsResponse?.data?.order.employee
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

                    Log.e(
                        TAG, "getOrderTimeDate:  ${
                            Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order?.created_at.toString()
                            )
                        }"
                    )

                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    paymentDetailsResponse?.data.order?.created_at.toString()
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

            paymentDetailsResponse?.data.order.order_items?.let {
                addOrderItemsTransaction(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            builder.addFeedLine(2)

            if (paymentDetailsResponse?.data.order.total_discount != null) {
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

                        if (paymentDetailsResponse?.data.total_discount != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.total_discount)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
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

            var totalDiscount: Double = 0.0
            paymentDetailsResponse?.data.order.total_discount?.let {
                totalDiscount = it
            }
            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (paymentDetailsResponse.data?.tax_amount != null) {
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
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null) {
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
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data?.tips != 0.0) {

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
                        "$" + paymentDetailsResponse.data.tips?.let {
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




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && paymentDetailsResponse.data?.cash_discount_or_surcharge != 0.0) {

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

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase()) {
                    builder.addText(
                        padLine(
                            "SurCharge",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                } else {

                    builder.addText(
                        padLine(
                            "Cash Discount",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {
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
                            "Used Loyalty Amount",
                            "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                                MethodUtils.roundOffAmountString(
                                    it.toDouble()
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

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {
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
                            "Used Loyalty Points",
                            paymentDetailsResponse?.data?.used_reward_points.toString(),
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
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)
            /* if (receiptModel?.order?.totalDiscount != 0.0) {
                 totalAmt =
                     (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalDiscount!!))

             }*/

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



            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0) {
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
                        "Refund Amount",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
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

                if (paymentDetailsResponse.data.order?.total_tips != 0.0) {
                    tip = paymentDetailsResponse.data.order?.total_tips.toString()
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
            }


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
                        paymentDetailsResponse.data.order.total_amount,
                        customerSettingModel.fonts
                    )

                }
            }

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
                    "Transaction ID",
                    "" + paymentDetailsResponse.data.id,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )



            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

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
                        "Card",
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
                        "",
                        paymentDetailsResponse.data.card_name,
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
                        "",
                        paymentDetailsResponse.data.card_type,
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
                        "",
                        paymentDetailsResponse.data.card_number,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            } else {


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
                        "Cash",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {
                    Log.e(
                        TAG,
                        "paymentDetailsResponse:  ${Gson().toJson(paymentDetailsResponse?.data.order?.customer)}"
                    )

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

                        builder.addText(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty()) {
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

                            var phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse?.data?.order?.customer?.phones?.get(
                                    paymentDetailsResponse?.data?.order?.customer.phones?.size - 1
                                ).phoneNumber
                            )
                            Log.e(TAG, "phoneNoFormatted:  ${phoneNoFormatted}")
                            builder.addText(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {

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

                            builder.addText(
                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
                                )?.fullAddress
                            )
                        }
                    }


                    /*  if (customerSettingModel.showCustomerAddress) {
                          if (paymentDetailsResponse?.data.order?.customer.addresses?.isNotEmpty() == true) {

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

                              builder.addText(orderDetailsResponse?.data?.customer?.addresses?.get(0)?.fullAddress)
                          }
                      }*/

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {

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

                builder.addText(paymentDetailsResponse?.data.order?.note)
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())
                Log.e(TAG, "BitmapHeight ${bitmap.height}")
                Log.e(TAG, "BitmapWidth ${bitmap.width}")
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
        type: String
    ) {
        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            SunmiPrinterApi.getInstance().printerInit()

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                printBusinessLogo()

            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty()) {
                PrintSunmiUtils.venueWebsite(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.order_type.trim())


            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    PrintSunmiUtils.orderId("OrderID:" + paymentDetailsResponse.data.order.id)
                }

                PrintSunmiUtils.receiptID("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)


                if (customerSettingModel.showTeam) {
                    PrintSunmiUtils.employee("Employee:" + paymentDetailsResponse?.data.order.employee)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderId(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
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

                val str = padLine(
                    if (customerSettingModel.showOrderIdTop) {
                        "OrderID:" + paymentDetailsResponse?.data.order.id
                    } else {
                        ""
                    },
                    "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.orderId(str.trim())

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + paymentDetailsResponse?.data?.order.employee
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.employee(empName)

                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order?.created_at.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontal()


            paymentDetailsResponse.data.order.order_items.let {
                addOrderItemsTransaction(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (paymentDetailsResponse?.data.order.total_discount != null) {


                val str1 = padLine(
                    "Total Discount",

                    if (paymentDetailsResponse?.data.total_discount != 0.0) {
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.total_discount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                    }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.totalDiscount(str1)

            }

            val sub = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.subTotal(sub)



            if (paymentDetailsResponse.data?.tax_amount != null) {


                PrintSunmiUtils.tax(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null) {

                PrintSunmiUtils.serviceCharge(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.tips != 0.0) {

                PrintSunmiUtils.tip(
                    padLine(
                        "Tips",
                        "$" + paymentDetailsResponse.data.tips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase()) {
                    val surCharge =
                        padLine(
                            "SurCharge",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                    PrintSunmiUtils.surCharge(surCharge)


                } else {


                    val cashDisc = padLine(
                        "Cash Discount",
                        if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()


                    PrintSunmiUtils.cashDiscount(cashDisc)

                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {

                    val loyaltyAmount = padLine(
                        "Used Loyalty Amount",
                        "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                            MethodUtils.roundOffAmountString(
                                it.toDouble()
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.loyaltyAmount(loyaltyAmount)

                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                    val loyaltyPoint = padLine(
                        "Used Loyalty Points",
                        paymentDetailsResponse?.data?.used_reward_points.toString(),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.loyaltyPoint(loyaltyPoint)

                }
            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            val totalAmt =
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)

            PrintSunmiUtils.totalPrice(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )


            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0) {

                PrintSunmiUtils.refundAmount(
                    padLine(
                        "Refund Amount",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
                SunmiPrinterApi.getInstance().lineWrap(1)
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.tips("Tips      _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
                    }

                }
            }


            if (customerSettingModel.showTipSuggestion) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.additionalTips()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipList(
                        tipsList,
                        paymentDetailsResponse.data.order.total_amount,
                        customerSettingModel.fonts
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            val tranId = padLine(
                "Transaction ID",
                "" + paymentDetailsResponse.data.id,
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.transactionId(tranId)



            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                val tranType = padLine(
                    "Transaction Type",
                    "Card", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.transactionType(tranType)


                PrintSunmiUtils.cardDetails(
                    paymentDetailsResponse.data.card_name,
                    paymentDetailsResponse.data.card_type,
                    paymentDetailsResponse.data.card_number
                )


            } else {

                PrintSunmiUtils.transactionType(
                    padLine(
                        "Transaction Type",
                        "Cash", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )

            }


            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {

                    PrintSunmiUtils.customerDetails()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse.data.order.customer.phones.get(
                                    paymentDetailsResponse.data.order.customer.phones.size - 1
                                ).phoneNumber
                            )
                            PrintSunmiUtils.customerPhone(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {

                            PrintSunmiUtils.customerAddress(
                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
                                )?.fullAddress
                            )
                        }
                    }

                    SunmiPrinterApi.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNote(paymentDetailsResponse?.data.order?.note)
                SunmiPrinterApi.getInstance().lineWrap(2)
            }


            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCode(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())

            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateInnerPrintSunmi(
    ) {
        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {


                PrintSunmiUtils.printLogoInner(prefProvider.getValue(Constants.VENUE_LOGO, ""))

            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                prefProvider.getValue(Constants.BUSINESS_ADDRESS, ""),
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "")
            )

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty()) {
                PrintSunmiUtils.normalTextCenter(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.order_type.trim())


            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    PrintSunmiUtils.normalText("OrderID:" + paymentDetailsResponse.data.order.id)
                }

                PrintSunmiUtils.normalText("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)


                if (customerSettingModel.showTeam) {
                    PrintSunmiUtils.normalText("Employee:" + paymentDetailsResponse?.data.order.employee)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
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

                val str = padLine(
                    if (customerSettingModel.showOrderIdTop) {
                        "OrderID:" + paymentDetailsResponse?.data.order.id
                    } else {
                        ""
                    },
                    "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.normalText(str.trim())

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + paymentDetailsResponse?.data?.order.employee
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(empName)

                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order?.created_at.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontalInner()


            paymentDetailsResponse.data.order.order_items.let {
                addOrderItemsTransactionInner(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (paymentDetailsResponse?.data.order.total_discount != null) {


                val str1 = padLine(
                    "Total Discount",

                    if (paymentDetailsResponse?.data.total_discount != 0.0) {
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.total_discount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                    }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str1)

            }

            val sub = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.normalText(sub)



            if (paymentDetailsResponse.data?.tax_amount != null) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.tips != 0.0) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Tips",
                        "$" + paymentDetailsResponse.data.tips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase()) {
                    val surCharge =
                        padLine(
                            "SurCharge",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                    PrintSunmiUtils.normalText(surCharge)


                } else {


                    val cashDisc = padLine(
                        "Cash Discount",
                        if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()


                    PrintSunmiUtils.normalText(cashDisc)

                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {

                    val loyaltyAmount = padLine(
                        "Used Loyalty Amount",
                        "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                            MethodUtils.roundOffAmountString(
                                it.toDouble()
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(loyaltyAmount)

                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                    val loyaltyPoint = padLine(
                        "Used Loyalty Points",
                        paymentDetailsResponse?.data?.used_reward_points.toString(),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(loyaltyPoint)

                }
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            val totalAmt =
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)

            PrintSunmiUtils.boldText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )


            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0) {

                PrintSunmiUtils.boldText(
                    padLine(
                        "Refund Amount",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
                SunmiPrintHelper.getInstance().lineWrap(1)
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.boldText("Tips      _____________")
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                    }

                }
            }


            if (customerSettingModel.showTipSuggestion) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.additionalTipsInner()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipListInner(
                        tipsList,
                        paymentDetailsResponse.data.order.total_amount,
                        customerSettingModel.fonts
                    )
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            val tranId = padLine(
                "Transaction ID",
                "" + paymentDetailsResponse.data.id,
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.normalText(tranId)



            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                val tranType = padLine(
                    "Transaction Type",
                    "Card", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.normalText(tranType)


                PrintSunmiUtils.cardDetailsInner(
                    paymentDetailsResponse.data.card_name,
                    paymentDetailsResponse.data.card_type,
                    paymentDetailsResponse.data.card_number
                )


            } else {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction Type",
                        "Cash", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )

            }


            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse.data.order.customer.phones.get(
                                    paymentDetailsResponse.data.order.customer.phones.size - 1
                                ).phoneNumber
                            )
                            PrintSunmiUtils.normalText(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {

                            PrintSunmiUtils.normalText(
                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
                                )?.fullAddress
                            )
                        }
                    }

                    SunmiPrintHelper.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNoteInner(paymentDetailsResponse?.data.order?.note)
                SunmiPrintHelper.getInstance().lineWrap(2)
            }


            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCodeInner(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())

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

        val manager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val display: Display = manager!!.defaultDisplay
        val point = Point()
        display.getSize(point)
        val width: Int = point.x
        val height: Int = point.y
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()

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