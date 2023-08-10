package com.android.pos.ui.fragments.transactions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.*
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.android.pos.data.remote.Constants.PHONE_ORDER
import com.android.pos.data.remote.Constants.SHIPPING_ADDRESS
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.SUNMI_PRINTER
import com.android.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.android.pos.databinding.FragmentTransactionDetailsBinding
import com.android.pos.di.ApiModule1
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.OrderDetailsItemListAdapter
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.magtek.MagtekRequestUtils
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.utils.*
import com.android.pos.utils.TimeFormatUtils.convertCurrentDate
import com.android.pos.utils.TimeFormatUtils.convertCurrentTime
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionDetailsBinding
    private val viewModel by viewModels<TransactionDetailsViewModel>()

    private lateinit var orderDetailsItemAdapter: OrderDetailsItemListAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private var orderIDglobal = 0
    var taxClickable = false

    @Inject
    lateinit var apiModule1: ApiModule1


    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    private var tipAmount: Double = 0.0

    //    private lateinit var orderDetailsResponse: GetOrderDetailsResponse
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private lateinit var paymentDetailsResponse: GetPaymentOrderDetailsResponse
    private var orderId: Int = -1
    var mLastClickTime: Long = 0
    private val TAG = "TransactionDetailsFr"
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private var paymentId: Int = -1
    private var isFromTrans: Boolean = false
    private var isFromOnlineOrderRefund: Boolean = false
    private var serviceChargesList: ArrayList<TbServiceCharge>? = arrayListOf()
    private var taxlistbirfurcation: ArrayList<TaxData>? = arrayListOf()
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
        orderUpdateTips()
        if (isFromOnlineOrderRefund) {
            acceptedAndDeclineOrder()
        }




        return binding.root
    }


    private fun setUpRecyclerView() {
        orderDetailsItemAdapter = OrderDetailsItemListAdapter()
        binding.rvOrderItems.adapter = orderDetailsItemAdapter

        taxBirfurcationAdapter = TaxBirfurcationAdapter("transaction")
        binding.rvTax.adapter = taxBirfurcationAdapter
    }

    private fun orderUpdateTips() {
        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), it.message.toString()
                ) { _, _ ->
                    viewModel.apiCallPaymentDetails(paymentId)
                }

            }
        }


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
        binding.linearTaxDetail.setOnClickListener {
            if (taxBirfurcationAdapter.taxlist.size > 0) {
                if (!taxClickable) {
                    Log.d(TAG, "onViewCreated: " + taxBirfurcationAdapter.taxlist.size)
                    taxClickable = true
                    binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                    binding.relativeDynamicTax.visible()
                } else {
                    taxClickable = false
                    binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.relativeDynamicTax.gone()
                }
            }

        }

        binding.tvtipadd.setOnClickListener {
            if (!paymentDetailsResponse.data.payable_type.equals(
                    "GiftCard",
                    true
                ) && !paymentDetailsResponse.data.payable_type.equals(
                    "Invoice", true
                )
            ) {


                val bundle = Bundle()
                bundle.putDouble("totalTip", paymentDetailsResponse.data.tips)
                bundle.putBoolean("isFromTransaction", true)
                paymentDetailsResponse.data.amount.let { bundle.putDouble("totalPrice", it) }
                findNavController().navigate(
                    R.id.action_transactionDetailsFragment_to_tipdialog,
                    bundle
                )
            }
        }
        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            if (tipAmount > 0) {
                val percentageTip = MethodUtils.calculatePercentageFromAmount(
                    tipAmount,
                    paymentDetailsResponse.data.amount
                )
                binding.tvTipLabel.text = "Tip (${
                    String.format(
                        if (percentageTip > 1) "%.0f" else "%.2f",
                        percentageTip
                    )
                }%)"
            } else {
                binding.tvTipLabel.text = "Tip (0%)"
            }

            if (paymentDetailsResponse.data?.payment_type == "Card") {
                magtekCall(tipAmount)
            } else {
                tipCall(false)
            }


        }
        binding.tvIssueRefund.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return@setOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime();
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
                    putInt("guestCount", paymentDetailsResponse.data.guestCount ?: 0)
                }
                bundle.putString("isFrom", "refundOnline")
                if (prefProvider.isManager() || prefProvider.isAdmin()) {
                    findNavController().navigate(
                        R.id.action_transaction_to_reasonForrefundonline,
                        bundle
                    )

                } else {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_pascodeManagerDailog,
                        bundle
                    )
                }

            } else {
                val bundle = Bundle().apply {
                    paymentDetailsResponse.data.order.order_items.forEach {
                        it.isChecked = true
                    }
                    putInt("paymentId", paymentId)
                    putParcelable("orderDetailsResponse", paymentDetailsResponse)
                    putBoolean("isSplitPayment", isSplitPayment)
                    putParcelableArrayList("serviceChargesList", serviceChargesList)
                    putInt("guestCount", paymentDetailsResponse.data.guestCount ?: 0)
                }
                bundle.putString("isFrom", "refund")
                if (prefProvider.isManager() || prefProvider.isAdmin()) {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_issueRefundFragment,
                        bundle
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_pascodeManagerDailog,
                        bundle
                    )
                }

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

    private fun tipCall(isCard: Boolean) {
        paymentDetailsResponse.data.let { viewModel.orderUpdateTip(it.id, tipAmount, isCard) }
    }

    private fun magtekCall(refundAmount: Double) {
        if (paymentDetailsResponse.data.order.order_type == "OnlineWebOrder") {
            val model = Gson().fromJson(
                paymentDetailsResponse.data.magensa_response_data,
                MagtekOnlineOrderRefundResponse::class.java
            )
            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*val amount = refundAmount

                    if (model != null) {
                        jsonArray =
                            model.transactionOutput?.token.let { it1 ->
                                amount.times(100).let {
                                    magtekRequestUtils.processTokenFirstData(
                                        it,
                                        it1,
                                        model.customerTransactionID ?: "",
                                        model.transactionOutput.transactionOutputDetails[0].value,
                                        Constants.CAPTURE
                                    )
                                }
                            }

                        networkCall(jsonArray, 0)
                    }*/
                    tipCall(true)
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput.token?.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (refundAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID.let { it1 ->
                        paymentDetailsResponse.data?.amount?.times(100)?.let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "", it1, Constants.CAPTURE,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (refundAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = amount.times(100)?.let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        amount?.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        paymentDetailsResponse.data.amount.let {
                            magtekRequestUtils.processReferenceIDTSYSCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                (tipAmount)
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }


            }
        } else {
            val model = Gson().fromJson(
                paymentDetailsResponse.data.magensa_response_data,
                PaymentResponse.PaymentResponseItem::class.java
            )


            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*val amount = refundAmount

                    if (model != null) {
                        jsonArray =
                            model.transactionOutput?.token?.let { it1 ->
                                amount.times(100).let {
                                    magtekRequestUtils.processTokenFirstData(
                                        it,
                                        it1,
                                        model.customerTransactionID ?: "",
                                        model.transactionOutput.transactionOutputDetails[0].value,
                                        Constants.CAPTURE
                                    )
                                }
                            }

                        networkCall(jsonArray, 0)
                    }*/
                    tipCall(true)
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput?.token?.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (refundAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        paymentDetailsResponse.data.amount.times(100).let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "", it1, Constants.CAPTURE,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (refundAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = amount?.times(100).let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        amount.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {


                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        paymentDetailsResponse.data.amount.let {
                            magtekRequestUtils.processReferenceIDTSYSCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                (tipAmount)
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }


            }
        }

    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

        ProgressUtils.showProgressDialog(requireActivity())

        val call = if (i == 1) {
            jsonArray1?.let { apiModule1.getRetrofit1().processReferenceID(it) }
        } else {
            jsonArray1?.let { apiModule1.getRetrofit1().processToken(it) }
        }

        call!!.enqueue(object : Callback<PaymentResponse> {

            override fun onResponse(
                call: Call<PaymentResponse>,
                response: Response<PaymentResponse>
            ) {
                ProgressUtils.dismissProgressDialog()
                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null) {

                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {

                            tipCall(true)

                        } else {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].transactionOutput?.transactionMessage
                            )
                        }


                    } else {
                        if (response.body()!![0].mPPGv4WSFault != null)
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].mPPGv4WSFault?.faultCode + "\n" +
                                        response.body()!![0].mPPGv4WSFault?.faultReason
                            )
                    }
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {

                ProgressUtils.dismissProgressDialog()
            }
        })
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


                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    binding.txtSubTitle.text = "Order ${it.data.custom_order_id}"
                    binding.orderDetailOrderId.text = it.data.custom_order_id.toString()

                } else {
                    binding.txtSubTitle.text = "Order ${it.data.order_id}"
                    binding.orderDetailOrderId.text = it.data.order_id.toString()
                }

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
                if (it.data.service_charge_details.isNotEmpty()) {
                    serviceChargesList = arrayListOf()
                    var serviceOrderType = ""
                    if (it.data.order.order_type == DINE_IN) {
                        serviceOrderType = Constants.SERVICECHARGE_DINEIN_ORDER
                    } else {
                        serviceOrderType = Constants.SERVICECHARGE_TAKEOUT_OPENORDER
                    }
                    it.data.service_charge_details.forEach { service ->
                        var data: TbServiceCharge = TbServiceCharge(
                            id = service.id!!,
                            order_service_charge_id = service.serviceChargeId,
                            name = service.name,
                            percentage = service.rate,
                            createdAt = service.created_at.toString(),
                            updatedAt = service.updated_at,
                            min_guest_count = service.min_guest_count,
                            max_guest_count = service.max_guest_count,
                            isChecked = true,
                            isActive = true,
                            isEnabled = true,
                            order_type = serviceOrderType,
                            locationId = it.data.order.location_id
                        )
                        serviceChargesList?.add(data)
                    }

                } else {
                    serviceChargesList = arrayListOf()
                }

                if (paymentDetailsResponse.data.tips > 0) {
                    val percentageTip = MethodUtils.calculatePercentageFromAmount(
                        paymentDetailsResponse.data.tips,
                        paymentDetailsResponse.data.amount
                    )
                    binding.tvTipLabel.text = "Tip (${
                        String.format(
                            if (percentageTip > 1) "%.0f" else "%.2f",
                            percentageTip
                        )
                    }%)"
                } else {
                    binding.tvTipLabel.text = "Tip (0%)"
                }

                if (it.data.order.customer != null) {
                    binding.tvCustomerName.text =
                        it.data.order.customer.firstName + " " + it.data.order.customer.lastName
                } else {
                    binding.tvCustomerName.text = ""
                }
                binding.orderDetails = it
                orderDetailsItemAdapter.addOrderDetailsItems(it.data.order.order_items)
                Log.e("OrderTypeId", it.data.order.order_type_id.toString())
                if(it.data.order.order_type_id.equals(5) || it.data.order.order_type_id.equals(2) || it.data.order.order_type_id.equals(6)){   // order_id 3 is for To go Open Order and order_id 1 for takeout
                    binding.txtPrintKitchenReceipt.visibility = View.GONE
                }else{
                    binding.txtPrintKitchenReceipt.visibility = View.VISIBLE
                }
                binding.llDiscount.visibility = View.VISIBLE
                if (paymentDetailsResponse.data.total_discount != 0.0) {
                    binding.txtDiscount.text = "- $" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.total_discount
                    )
                } else {
                    binding.txtDiscount.text = "- $" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.order.total_discount
                    )
                }
                if (!isSplitPayment) {
                    if (paymentDetailsResponse.data.tax_amount != 0.0) {
                        binding.imgDropdown.visible()
                    }
                    if (paymentDetailsResponse.data.order.order_items.isNotEmpty()) {
                        paymentDetailsResponse.data.order.order_items.forEach { orderItem ->
                            var totalPrice = orderItem.price * orderItem.quantity
                            totalPrice -= orderItem.discountAmount
                            orderItem.orderItemModifiers.forEach { orderItemModifier ->
                                totalPrice += orderItemModifier.price * orderItemModifier.quantity
                            }
                            Log.d(TAG, "navigate: itemPrice : $totalPrice")
                            var totaltaxtemp = 0.0
                            orderItem.orderItemTaxes.forEach { orderItemTaxe ->
                                if (taxlistbirfurcation?.isNotEmpty() == true) {
                                    var found = -1
                                    taxlistbirfurcation?.forEachIndexed { index, taxData ->
                                        if (taxData.orderTaxId == orderItemTaxe.taxId) {
                                            found = index
                                            return@forEachIndexed
                                        }
                                    }
                                    if (found == -1) {
                                        var taxData: TaxData = TaxData(
                                            orderItemTaxe.createdAt,
                                            orderItemTaxe.id,
                                            0,
                                            orderItemTaxe.name,
                                            orderItemTaxe.rate,
                                            orderItemTaxe.taxType,
                                            orderItemTaxe.updatedAt,
                                            true,
                                            orderItemTaxe.isDefault,
                                            false,
                                            "",
                                            listOf(orderItemTaxe.orderItemId),
                                            orderItemTaxe.taxId,
                                            false,
                                            getTaxFromTotalPrice(
                                                orderItemTaxe,
                                                totalPrice,
                                                orderItem
                                            ),
                                            totalPrice
                                        )
                                        taxlistbirfurcation?.add(taxData)
                                    } else {
                                        taxlistbirfurcation!![found].totalTaxTypePrice =
                                            taxlistbirfurcation!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                                orderItemTaxe,
                                                totalPrice,
                                                orderItem
                                            )
                                        taxlistbirfurcation!![found].subTotalAmount =
                                            taxlistbirfurcation!![found].subTotalAmount + totalPrice
                                    }
                                    Log.d(TAG, "found : " + found)
                                } else {
                                    var taxData: TaxData = TaxData(
                                        orderItemTaxe.createdAt,
                                        orderItemTaxe.id,
                                        0,
                                        orderItemTaxe.name,
                                        orderItemTaxe.rate,
                                        orderItemTaxe.taxType,
                                        orderItemTaxe.updatedAt,
                                        true,
                                        orderItemTaxe.isDefault,
                                        false,
                                        "",
                                        listOf(orderItemTaxe.orderItemId),
                                        orderItemTaxe.taxId,
                                        false,
                                        getTaxFromTotalPrice(
                                            orderItemTaxe,
                                            totalPrice,
                                            orderItem
                                        ),
                                        totalPrice
                                    )
                                    taxlistbirfurcation?.add(taxData)
                                }


                                Log.d(TAG, "navigate: " + totaltaxtemp)
                            }

                        }

                        taxBirfurcationAdapter.setList(taxlistbirfurcation!!)
                        Log.d(TAG, "navigate: list " + Gson().toJson(taxlistbirfurcation))
                    }
                } else {
                    binding.relativeDynamicTax.gone()
                    binding.imgDropdown.gone()
                    binding.linearPaymentinfo.layoutParams.height =
                        resources.getDimension(R.dimen._78sdp).toInt()
                    binding.linearSummary.layoutParams.height =
                        resources.getDimension(R.dimen._85sdp).toInt()
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
                    binding.tvtipadd.visibility = View.GONE
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
                    binding.tvtipadd.visibility = View.GONE
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

    fun getTaxFromTotalPrice(
        orderItemTaxe: GetOrderDetailsResponse.Data.OrderItem.OrderItemTaxe,
        totalPrice: Double,
        item: GetOrderDetailsResponse.Data.OrderItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
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
                            if (it.status) {
                                initPrinter(it, Constants.CUSTOMER)
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
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService()
            }


        } else {

            viewLifecycleOwner.lifecycleScope.launch {
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
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
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
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }
            }
        }

    }

    private fun setService(
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateInnerPrintSunmi()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(
                )
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
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

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(Constants.VENUE_LOGO, "")}")

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
                    builder.addText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + paymentDetailsResponse.data.order_id)
                }

                builder.addFeedLine(1)
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

            if (customerSettingModel.showVenueAddress) {
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
            }
            if (customerSettingModel.showVenuePhone) {
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
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }


            if (customerSettingModel.showWebsiteAddress) {
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
            }
            if (customerSettingModel.showOrderType) {
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

                builder.addText(paymentDetailsResponse.data.order.order_type_name + "\n")
            }

            if (paymentDetailsResponse.data?.order?.order_type.equals(PHONE_ORDER, true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("OnlineOrder", true)
            ) {
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

                builder.addText(paymentDetailsResponse.data.order.delivery_type + "\n")
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


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
                        "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
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

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {
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




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && paymentDetailsResponse.data?.cash_discount_or_surcharge != 0.0
                && customerSettingModel.showCashDisSurCharg
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

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse.data.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
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

                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse.data.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {

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



            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {
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
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
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

                if(!(paymentDetailsResponse.data.card_name).isNullOrBlank()) {
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
                }

                if(!(paymentDetailsResponse.data.card_type).isNullOrBlank()) {
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
                }

                if(!(paymentDetailsResponse.data.card_number).isNullOrBlank()) {
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
                }

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
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty() && paymentDetailsResponse?.data?.order?.customer?.phones?.get(
                                paymentDetailsResponse?.data?.order?.customer.phones?.size - 1
                            ).phoneNumber.isNotEmpty()
                        ) {
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

                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }


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


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())
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
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
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

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.order_id)
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

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty() && customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)
            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.order_type_name.trim())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineOrder", true)
            ) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.delivery_type.trim())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (customerSettingModel.fonts == Constants.LARGE) {


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
                    "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
                    "",
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

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {

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




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && customerSettingModel.showCashDisSurCharg) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
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


                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {


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


            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {

                PrintSunmiUtils.refundAmount(
                    padLine(
                        "Refund Amount",
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
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


                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }


//                            PrintSunmiUtils.customerAddress(
//                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
//                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
//                                )?.fullAddress
//                            )
                        }
                    }

                    SunmiPrinterApi.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNote(paymentDetailsResponse?.data.order?.note)
                SunmiPrinterApi.getInstance().lineWrap(2)
            }

            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)
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

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.order_id)
                }
            }

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
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty() && customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.normalTextCenter(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.order_type_name.trim())
            }

            if (paymentDetailsResponse.data.order.order_type.trim()
                    .equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.trim()
                    .equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.trim().equals(PHONE_ORDER, true)
            ) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.delivery_type)
            }


            if (customerSettingModel.fonts == Constants.LARGE) {


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

                PrintSunmiUtils.normalText("ReceiptID:" + paymentDetailsResponse?.data.order.offline_id)

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + paymentDetailsResponse?.data?.order.employee
                        } else {
                            ""
                        },
                        "", PrintSunmiUtils.lineChar()
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
                        "", PrintSunmiUtils.lineChar()
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
                            "", PrintSunmiUtils.lineChar()
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
                    }, PrintSunmiUtils.lineChar()
                ).toString()
                PrintSunmiUtils.normalText(str1)

            }

            val sub = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                PrintSunmiUtils.lineChar()
            ).toString()

            PrintSunmiUtils.normalText(sub)



            if (paymentDetailsResponse.data?.tax_amount != null) {


                PrintSunmiUtils.normalText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        PrintSunmiUtils.lineChar()
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        PrintSunmiUtils.lineChar()
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
                        }, PrintSunmiUtils.lineChar()
                    ).toString()
                )
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && customerSettingModel.showCashDisSurCharg) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
                    val surCharge =
                        padLine(
                            "SurCharge",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            }, PrintSunmiUtils.lineChar()
                        ).toString()

                    PrintSunmiUtils.normalText(surCharge)


                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {


                    val cashDisc = padLine(
                        "Cash Discount",
                        if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                        }, PrintSunmiUtils.lineChar()
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
                        }, PrintSunmiUtils.lineChar()
                    ).toString()
                    PrintSunmiUtils.normalText(loyaltyAmount)

                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                    val loyaltyPoint = padLine(
                        "Used Loyalty Points",
                        paymentDetailsResponse?.data?.used_reward_points.toString(),
                        PrintSunmiUtils.lineChar()
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
                    PrintSunmiUtils.lineChar()
                ).toString()
            )


            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {

                PrintSunmiUtils.boldText(
                    padLine(
                        "Refund Amount",
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        PrintSunmiUtils.lineChar()
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
                PrintSunmiUtils.lineChar()
            ).toString()

            PrintSunmiUtils.normalText(tranId)

            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                val tranType = padLine(
                    "Transaction Type",
                    "Card", PrintSunmiUtils.lineChar()
                ).toString()

                PrintSunmiUtils.normalTextTest(tranType)

                PrintSunmiUtils.cardDetailsInner(
                    paymentDetailsResponse.data.card_name,
                    paymentDetailsResponse.data.card_type,
                    paymentDetailsResponse.data.card_number,customerSettingModel.fonts
                )


            } else {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Transaction Type",
                        "Cash", PrintSunmiUtils.lineChar()
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


                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            PrintSunmiUtils.normalText(
//                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
//                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
//                                )?.fullAddress
//                            )
                        }
                    }

                    SunmiPrintHelper.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNoteInner(paymentDetailsResponse?.data.order?.note)
                SunmiPrintHelper.getInstance().lineWrap(2)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
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