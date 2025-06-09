package com.pays.pos.ui.fragments.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.model.responseModel.GetTransactionListResponse
import com.pays.pos.data.model.responseModel.MagtekOnlineOrderRefundResponse
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.FILE_PATH
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.FragmentTransactionBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.TransactionAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.utils.*
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.PaginationScrollListener
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.payments.design.*
import com.pays.payments.gateways.dejavoo.DejavooPaymentGateway
import com.pays.payments.gateways.valor.ValorPaymentGateway
import com.pays.pos.data.model.valor.ValorSuccessResponse
import com.pays.pos.logger.CashBoxEvent
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class TransactionFragment : Fragment(), AdapterView.OnItemSelectedListener, ItemCallback {

    private var selectedPos: Int = 0
    private var checkFilter: Boolean = false
    private var singleTransaction: GetTransactionListResponse.Data.Payment? = null
    private var tipAmount: Double = 0.0
    private lateinit var binding: FragmentTransactionBinding
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    private lateinit var transactionAdapter: TransactionAdapter
    private val viewModel by viewModels<TransactionViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private lateinit var terminalListGlobal: ArrayList<VenueDetailsResponse.Data.Terminal>
    private lateinit var orderTypeListGlobal: ArrayList<TbOrderType>
    private lateinit var teamRoleListGlobal: ArrayList<TeamRole>
    private lateinit var teamEmployeeListGlobal: ArrayList<Employee>
    private var tipTypeList = ArrayList<String>()
    private var paymentTypeList = ArrayList<String>()
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()
    private var spinnerTouched = false
    val TAG = "TransactionFragment"

    // PAX variables
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()
    var CARDBIN = ""
    var cardLastDigits = ""
    var CardName = ""
    var EDCType = ""

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var apiModule1: ApiModule1

    private var employeeTimeSheet = ArrayList<GetTransactionListResponse.Data.Payment>()

    private var TOTAL_PAGES = 0
    var PAGE_START = 1
    private var isLoading = false
    private var currentPage = PAGE_START
    private var isLastPage = false

    private lateinit var presentation: CustomDisplay
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val magtekProViewModel by viewModels<MagtekViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_transaction, container, false)

        binding.viewModel = viewModel

        currentPage = 1

        binding.lifecycleOwner = this

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel
            )
        }

        setCashEventObserver()

        startDatePickerObserver()
        endDatePickerObserver()
        setUpRecyclerView()
        getEmployeesTimeSheetObserver()
        getRoleListObserver()
        setupSnackbar()
        observeShowProgress()
        loadTeams()
        loadTerminals()
        getOrderType()
        navigate()
        orderUpdateTips()
        setUpPaymentTypeSpinnerAdapter()
        binding.includeView.spTerminals.onItemSelectedListener = this
        binding.includeView.spRoles.onItemSelectedListener = this
        binding.includeView.spEmployees.onItemSelectedListener = this
        binding.includeView.spOrders.onItemSelectedListener = this
        binding.includeView.spTipTypes.onItemSelectedListener = this
        binding.includeView.spTransactionTypes.onItemSelectedListener = this
        setUpTipTypeSpinnerAdapter()
        initPOSLink()
        getMerchantDataObserver()

        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                checkFilter = true
                currentPage = 1
                apiCallTimeSheet()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }
        }

        endTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            var fromDate = SimpleDateFormat("MM/dd/yyyy hh:mm a").parse(viewModel.startDate.value)
//                .getTime() / 1000
//            var endDate = SimpleDateFormat("dd/MM/yyyy hh:mm a").parse(
//                timeCalculateForStartEndTime(
//                    hour,
//                    minute,
//                    "isend"
//                )
//            ).getTime() / 1000


            val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())

            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)

            viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")

            val startDate1 = dateFormat.parse(viewModel.startDate.value)
            val endDate1 = dateFormat.parse(timeCalculateForStartEndTime(hour, minute, "isend"))

            if (startDate1 <= endDate1) {
                checkFilter = true
                currentPage = 1
                if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30)
                    apiCallTimeSheet()
                else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireActivity(),
                        "Please Select date in 30 Days."
                    ) { _, _ ->
                    }
                }
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "The end date cannot be earlier than the start date. Please select a valid date range."
                ) { _, _ ->
                }
            }
        }

        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                startTime,
                myCalendar2.get(2),
                myCalendar2.get(2),
                false
            ).show()
        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                endTime,
                myCalendar3.get(2),
                myCalendar3.get(2),
                false
            ).show()

        }


        //   viewModel.setCurrentDate(myCalendar)

        binding.includeView.imgDrawer.setOnSingleClickListener {
            if (findNavController().currentDestination?.id == R.id.transactionFragment) {
                findNavController().navigate(R.id.action_transactionFragment_to_menfragment)
            }

        }

        binding.includeView.txtTitle.text = getString(R.string.transactions)

        binding.includeView.txtHome.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.transactionFragment) {
                findNavController().navigate(R.id.action_transactionFragment_to_dashboardCategoryNew)
            }
        }


        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")

            if (singleTransaction?.paymentType == "Card") {
                //Add condition according to params i.e magtek or pax data in API response
                Log.d("RefNum11: ", "RefNum ${singleTransaction?.ref_num}")
                /*if (singleTransaction?.ref_num.isNullOrEmpty()) {
                    magtekCall(tipAmount)
                } else {
                    adjustPaxTips()
                }*/
                when (prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE, "")) {
                    Constants.PAX -> {
                        if (!singleTransaction?.ref_num.isNullOrEmpty() && prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            )
                        ) {
                            adjustPaxTips()
                        } else {
                            if (!singleTransaction?.ref_num.isNullOrEmpty() && !prefProvider.getValueboolean(
                                    Constants.IS_PAX_CONNECTED,
                                    false
                                )
                            ) {
                                AlertUtils.showCustomAlert(
                                    requireContext(),
                                    "Please connect to PAX device"
                                )
                            }
                        }
                    }
                    Constants.VALOR, Constants.VELOR -> {
                        adjustValorTips()
                    }
                    Constants.DEJAVOO -> {
                        adjustDejavooTips()
                    }

                    else -> {
                        if (singleTransaction?.ref_num.isNullOrEmpty()) {
                            magtekCall(tipAmount)
                        } else {
                            if (!singleTransaction?.ref_num.isNullOrEmpty() && !prefProvider.getValueboolean(
                                    Constants.IS_PAX_CONNECTED,
                                    false
                                )
                            ) {
                                AlertUtils.showCustomAlert(
                                    requireContext(),
                                    "Please connect a payment device"
                                )
                            }
                        }
                    }
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    launch {
                        tipCall(false)
                    }
                    launch {
                        cashLogEventCall(bundle)
                    }
                    launch {
                        openCashDrawer()
                    }
                }
            }
        }

        binding.includeView.spTerminals.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spRoles.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spEmployees.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spOrders.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spTipTypes.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }
        binding.includeView.spTransactionTypes.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvTeamTimeSheet.layoutManager = layoutManager

        binding.rvTeamTimeSheet.addOnScrollListener(object :
            PaginationScrollListener(layoutManager) {


            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun getTotalPageCount(): Int {
                return 0
            }


            override fun loadMoreItems() {
                currentPage += 1
                if (currentPage <= TOTAL_PAGES) {
                    isLoading = true
                    apiCallTimeSheet()
                } else {
                    transactionAdapter.showLoading(false)
                }

            }

        })



        if (findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>(KEY)?.value == null)
            apiCallTimeSheet()
        else {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>(KEY)
                ?.observe(viewLifecycleOwner) { it ->
                    it.getInt("selectedorderType")
                        .let { it1 -> binding.includeView.spOrders.setSelection(it1) }
                    it.getInt("selectedtransactionType")
                        .let { it1 -> binding.includeView.spTransactionTypes.setSelection(it1) }
                    it.getInt("selectedroleType")
                        .let { it1 -> binding.includeView.spRoles.setSelection(it1) }
                    it.getInt("selectedemployeeType")
                        .let { it1 -> binding.includeView.spEmployees.setSelection(it1) }
                    it.getInt("selectedterminalType")
                        .let { it1 -> binding.includeView.spTerminals.setSelection(it1) }
                    apiCallTimeSheet()
                }
        }
        return binding.root
    }

    private fun setCashEventObserver() {
        viewModel.cashLogUpdate.observe(viewLifecycleOwner,object:androidx.lifecycle.Observer<Event<Boolean>>{
            override fun onChanged(t: Event<Boolean>?) {
                t?.getContentIfNotHandled()?.let {
                    if (it){
                        runOnUiThread(Runnable {
                            ProgressUtils.dismissProgressDialog()
                        })
                    }
                }
            }
        })
    }

    private fun openCashDrawer() {
        if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
            EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
        } else {
            SunmiPrintHelper.getInstance().openCashBox()
        }
    }

    lateinit var paymentCoroutineScope: CoroutineScope
    val paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CustomDisplay adjustValorTips()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }


    private fun adjustDejavooTips() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = PaymentGatewayFactory(
                ValorPaymentGateway(),
                DejavooPaymentGateway()
            ).create(gatewayType)

            singleTransaction?.ref_num?.let { dejavooRefTxnId ->
                var dejavoo = Dejavoo(
                    registerId = prefProvider.getValue(
                        Constants.DEJAVOO_REGISTER_ID, ""
                    ),
                    authKey = prefProvider.getValue(
                        Constants.DEJAVOO_AUTH_KEY, ""
                    ),
                    tpn = prefProvider.getValue(
                        Constants.DEJAVOO_TPN, ""
                    ),
                    paymentType = "Credit",
                    transType = "TipAdjust",
                    amount = singleTransaction?.totalAmount.toString(),
                    tip = tipAmount.toString(),
                    refId = dejavooRefTxnId,
                    printReceipt = false,
                    performedBy = prefProvider.employeeName(),
                    isProd = Constants.paymentLive,
                    txnType = TransactionType.TIP_ADJUSTMENT
                )
                paymentGateway.processPayment(
                    requireContext().applicationContext,
                    dejavoo,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        tipCall(true)

                    },
                    onFailure = {
                        ProgressUtils.dismissProgressDialog()

                        /*runOnUiThread(Runnable {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                errorMessage
                            )
                        })*/

                    }
                )
                /* Process Tip Adjust */
            }
        }
    }

    private fun cashLogEventCall(bundle: Bundle) {
        if (bundle.containsKey("tipAmount")) {
            if (bundle.getDouble("tipAmount") > 0.0) {
                getTipDetails(bundle.getDouble("tipAmount"),singleTransaction?.orderId, singleTransaction?.id ?: -1)
//                makeCashEventCallToUpdateTip(bundle.getDouble("tipAmount"))
            }
        }

    }

    private fun getTipDetails(tippedAmount: Double, orderId: Int?, id:Int = -1){
        Log.e("PAYMENT_ID", "Tip adjust paymentId - ${singleTransaction?.id ?: id}")
        viewModel.getCashEventDetails(tippedAmount, orderId?:-1,singleTransaction?.id ?: id,"in",0)
    }

    private fun makeCashEventCallToUpdateTip(tippedAmount: Double) {
        val cashLogRequest = CashLogRequest(
            tippedAmount,
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
            "in",
            singleTransaction?.orderId ?: -1,
            singleTransaction?.id ?: -1,
            "Tip added to the order",
            prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
            null,
            null
        )
        dashboardViewModel.makeCashInOutCallFromCustomerDisplay(cashLogRequest)

    }

    @Inject
    lateinit var apiService: ApiService

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
            presentation.showSplashLayout()
        }
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                }
            })
    }

    // get merchant details of PAX device
    private fun getMerchantDataObserver() {
        magtekProViewModel.merchantData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { response ->
                Log.d("merchantData: ", "merchantData observe")
                val resultCode = response.resultCode
                val status = response.resultTxt
                val mID = response.VarValue
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                prefProvider.setValue(
                    Constants.MERCHANT_ID,
                    mID
                )
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    adjustPaxTips()
//                    AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    // Adjust tip on transactions done via PAX
    private fun adjustPaxTips() {
        GlobalScope.launch {
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    context!!,
                    FILE_PATH + SettingINI.FILENAME
                )
            )
            val tip_amt = (tipAmount * 100).toInt()
            Log.d("Amt: ", "tip $tip_amt RefNo ${singleTransaction?.ref_num}")

            CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("ADJUST")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = tip_amt.toString()
            mPaymentRequest.OrigRefNum = singleTransaction?.ref_num
            //Added for TSYS ADJUST issue
            mPaymentRequest.ECRRefNum = singleTransaction?.ref_num
            mPaymentRequest.ExtData = "<Force>T</Force>"

            posLink.PaymentRequest = mPaymentRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.PaymentResponse

                val response = msg.obj as com.pax.poslink.PaymentResponse
                val resultCode = response.ResultCode
                val resultTxt = response.ResultTxt
                val approvedAmount = response.ApprovedAmount
                val ExtData = response.ExtData

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                var tipAmount = response.ApprovedTipAmount
                val globalUID = response.PaymentTransInfo.GlobalUid

                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $globalUID"
                )
                Log.d(
                    "Payment Details: ",
                    "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${
                        Gson().toJson(response)
                    }"
                )

                if (resultCode == "000000") {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
//                            makePaymentCreditCard()
                            tipCall(true)
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            resultTxt,
                            object :
                                DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    try {
                                        p0?.dismiss()
                                    } catch (e: Exception) {
                                    }
                                }
                            })
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    AlertUtils.showCustomAlertWithListenerWithOKCancel(
                        requireContext(),
                        getString(R.string.pax_connect_error), getString(R.string.reconnect),
                    )
                    { _, _ ->
                        // Add connect to PAX logic
                        magtekProViewModel.initPOSLink(requireContext())
                    }
                    /*if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT"){
                        AlertUtils.showCustomAlertWithListenerWithOKCancel(
                            requireContext(),
                            getString(R.string.pax_connect_error), getString(R.string.reconnect),
                        )
                        { _, _ ->
                            // Add connect to PAX logic
                            magtekProViewModel.initPOSLink(requireContext())
                        }
//                        Toast.makeText(requireContext(), R.string.pax_connect_error, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "getMerchantDetails Failed ${result.Code} ${result.Msg}", Toast.LENGTH_LONG).show()
                    }*/
                }
            }

        }
    }

    @Inject
    lateinit var paymentGatewayFactory: PaymentGatewayFactory

    private fun adjustValorTips() {

        GlobalScope.launch {
            val tip_amt = (tipAmount * 100).toInt()

            withContext(Dispatchers.Main) {
                ProgressUtils.showProgressDialog(requireActivity())
            }

            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            singleTransaction?.ref_num.let { valorRefTxId ->
                context?.let {
                    var valor = Valor(
                        apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                        appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                        epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                        endpoint = Constants.VALOR_TIP_ADJUST,
                        txnType = TransactionType.TIP_ADJUSTMENT,
                        channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                        transMode = "",
                        transCode = "",
                        reqTxnId = valorRefTxId.toString(),
                        amount = "",
                        tipAmount = tipAmount.toString(),
                        tipEntry = "1",
                        txn_type = "",
                        isProd = Constants.paymentLive,
                        surchargeIndicator = "",
                        sale_refund = "",
                        ref_txn_id = "",
                        transactionId = ""
                    )

                    paymentGateway.processPayment(
                        context = it.applicationContext,
                        valor,
                        onSuccess = { tResponse ->
                            var transactionJsonResponse = Gson().fromJson<ValorSuccessResponse>(
                                tResponse,
                                ValorSuccessResponse::class.java
                            )
                            transactionJsonResponse.nameValuePairs?.let {
                                if (it.msg != null) {
                                    if (it.msg!!.contains(
                                            "APPROVED"
                                        )
                                    ) {
                                        tipCall(true)
                                    } else {
                                        ProgressUtils.dismissProgressDialog()
                                        /* runOnUiThread(Runnable {
                                             AlertUtils.showCustomAlert(
                                                 requireContext(),
                                                 it.msg
                                             )
                                         })*/
                                    }
                                }
                            }
                        },
                        onFailure = { errorMessage ->
                            println("Payment Failed: $errorMessage")

                            ProgressUtils.dismissProgressDialog()

                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(),
                                errorMessage,
                                object :
                                    DialogInterface.OnClickListener {
                                    override fun onClick(p0: DialogInterface?, p1: Int) {
                                        try {
                                            p0?.dismiss()
                                        } catch (e: Exception) {
                                        }
                                    }
                                })
                        }
                    )
                }
            }
            /* Process Tip Adjust */

        }
    }

    // Update tip in order
    private fun tipCall(isCard: Boolean) {
        singleTransaction?.let { viewModel.orderUpdateTip(it.id, tipAmount, isCard) }
    }

    private fun apiCallTimeSheet() {
        MethodUtils.hideKeyboard(requireActivity())
        viewModel.apiCallTimeSheet(
            currentPage,
            getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
            getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
            getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
            getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
            getTipType(binding.includeView.spTipTypes.selectedItemPosition),
            getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)

        )

    }

    // validate time range filter
    private fun differnceTrue(date1: String, date2: String?): Long {
        var dateType1: Date
        var dateType2: Date
        var daydifference = "0".toLong()
//        11/30/2021 09:40 AM
        try {
            var dates = SimpleDateFormat("MM/dd/yyyy")
            dateType1 = dates.parse(date1.substringBefore(" "))
            dateType2 = dates.parse(date2?.substringBefore(" "))
            var differencedate = abs(dateType1.time - dateType2.time)
            daydifference = differencedate / (24 * 60 * 60 * 1000)
            Log.d("yash", "differnceTrue: " + daydifference)
            return daydifference
        } catch (e: Exception) {
        }
        return daydifference
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefProvider = PrefProvider(requireActivity())
        viewModel.setCurrentDate(
            myCalendar
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchFilter()
    }

    fun timeCalculateForStartEndTime(hour: Int, minute: Int, isStart: String): String {
        var timestring = ""
        var hoursfinal: Int = 0
        if ((hour == 12 && minute > 0) || (hour > 12 && minute > 0) || (hour > 12 && minute == 0)) {
            if (hour == 12) {
                hoursfinal = hour
            } else {
                hoursfinal = hour - 12
            }
            if (hoursfinal < 10) {
                if (minute < 10) {
                    timestring = "0$hoursfinal:0$minute PM"
                } else {
                    timestring = "0$hoursfinal:$minute PM"
                }
            } else {
                if (minute < 10) {
                    timestring = "$hoursfinal:0$minute PM"
                } else {
                    timestring = "$hoursfinal:$minute PM"
                }
            }
        } else {
            if (hour == 0) {
                if (minute < 10) {
                    timestring = "${hour.plus(12)}:0$minute AM"
                } else {
                    timestring = "${hour.plus(12)}:$minute AM"
                }
            } else {
                if (hour < 10) {
                    if (minute < 10) {
                        timestring = "0$hour:0$minute AM"
                    } else {
                        timestring = "0$hour:$minute AM"
                    }
                } else {
                    if (minute < 10) {
                        timestring = "$hour:0$minute AM"
                    } else {
                        timestring = "$hour:$minute AM"
                    }
                }
            }

        }

        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        var startDatestring = ""
        if (isStart == "isstart") {
            startDatestring = sdf.format(myCalendar.time)
        } else {
            startDatestring = sdf.format(myCalendar1.time)
        }
        return "$startDatestring $timestring"
    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                currentPage = 1
                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                currentPage = 1
                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }
        }
    }


    private fun setUpRoleSpinnerAdapter(teamRoleList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            teamRoleList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spRoles.adapter = spinnerAdapter

    }

    private fun setUpEmployeeSpinnerAdapter(employeeList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            employeeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spEmployees.adapter = spinnerAdapter

    }

    private fun setUpTerminalSpinnerAdapter(terminalList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTerminals.adapter = spinnerAdapter

    }

    private fun setUpOrderTypeSpinnerAdapter(orderTypeList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            orderTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spOrders.adapter = spinnerAdapter

    }

    private fun setUpTipTypeSpinnerAdapter() {

        tipTypeList.clear()
        tipTypeList.add(getString(R.string.tv_all_tip_types))
        tipTypeList.add(getString(R.string.tv_adjusted))
        tipTypeList.add(getString(R.string.tv_unadjusted))

        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            tipTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTipTypes.adapter = spinnerAdapter

    }

    private fun setUpPaymentTypeSpinnerAdapter() {

        paymentTypeList.clear()
        paymentTypeList.add(getString(R.string.tv_all_payment_types))
        paymentTypeList.add(getString(R.string.tv_cash_payment))
        paymentTypeList.add(getString(R.string.tv_card_payment))
        paymentTypeList.add(getString(R.string.tv_giftcard_payment))
        paymentTypeList.add(getString(R.string.external))

        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            paymentTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTransactionTypes.adapter = spinnerAdapter

    }

    private fun getTerminalId(position: Int): Int? {
        return if (this::terminalListGlobal.isInitialized) {

            if (position == -1) {
                terminalListGlobal?.get(0)?.id
            } else {
                terminalListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getRoleId(position: Int): Int? {
        return if (this::teamRoleListGlobal.isInitialized) {

            if (position == -1) {
                teamRoleListGlobal?.get(0)?.id
            } else {
                teamRoleListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getEmployeeId(position: Int): Int? {
        return if (this::teamEmployeeListGlobal.isInitialized) {

            if (position == -1) {
                teamEmployeeListGlobal?.get(0)?.id
            } else {
                teamEmployeeListGlobal?.get(position)?.id
            }
        } else {
            -1
        }
    }

    private fun getOrderId(position: Int): Int? {
        return if (this::orderTypeListGlobal.isInitialized) {

            if (position == -1) {
                orderTypeListGlobal?.get(0)?.id
            } else {
                orderTypeListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getTipType(position: Int): String {
        return tipTypeList[position]
    }

    private fun getPaymentType(position: Int): String {
        return paymentTypeList[position].toString()
    }


    private fun setUpRecyclerView() {
        binding.rvTeamTimeSheet.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        transactionAdapter = TransactionAdapter(viewModel, prefProvider)
        transactionAdapter.setCallback(this)
        binding.rvTeamTimeSheet.adapter = transactionAdapter


    }

    private fun getEmployeesTimeSheetObserver() {
        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                if (timeSheet.data.payments.isNotEmpty()) {
                    binding.txtNodata.visibility = View.GONE
                    binding.rvTeamTimeSheet.visibility = View.VISIBLE
                    // employeeTimeSheet.addAll(timeSheet.data.payments)
                    TOTAL_PAGES = timeSheet.data.pagination.maxPageSize.toInt()

                    transactionAdapter.showLoading(false)

                    if (checkFilter) {
                        checkFilter = false
                        transactionAdapter.clear()
                    }
                    transactionAdapter.addAll(timeSheet.data.payments)

                    isLoading = false
                    if (currentPage != TOTAL_PAGES) {

                        transactionAdapter.showLoading(true)
                    }


                } else {
                    LogUtil.logE(TAG, "itemCount ${transactionAdapter.itemCount}")
                    /* binding.rvTeamTimeSheet.visibility = View.GONE
                     binding.txtNodata.visibility = View.VISIBLE
                     binding.txtNodata.text = timeSheet.message*/
                    if (transactionAdapter.itemCount == 0) {
                        binding.rvTeamTimeSheet.visibility = View.GONE
                        binding.txtNodata.visibility = View.VISIBLE
                        binding.txtNodata.text = timeSheet.message.toString()
                    } else {
                        binding.txtNodata.visibility = View.GONE
                        binding.rvTeamTimeSheet.visibility = View.VISIBLE
                        // employeeTimeSheet.addAll(timeSheet.data.payments)
                        TOTAL_PAGES = timeSheet.data.pagination.maxPageSize.toInt()

                        transactionAdapter.showLoading(false)

                        if (checkFilter) {
                            checkFilter = false
                            transactionAdapter.clear()
                        }
                        transactionAdapter.addAll(timeSheet.data.payments)

                        isLoading = false
                        if (currentPage != TOTAL_PAGES) {

                            transactionAdapter.showLoading(true)
                        }

                    }
                }


            }
        }

    }

    private fun getRoleListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { roleList ->

                            teamRoleListGlobal = roleList as ArrayList<TeamRole>
                            val isPresent = teamRoleListGlobal.any { it.name == "All Roles" }

                            if (!isPresent) {
                                // teamRoleListGlobal.removeAt(0)
                                teamRoleListGlobal.add(0, TeamRole(-1, "All Roles", null, null))
                            }
                            val roleName = teamRoleListGlobal.map { it.name }

                            setUpRoleSpinnerAdapter(roleName as ArrayList<String>)

                            Log.d("callapi", "::callapi")

                        }

                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        try {
                            ProgressUtils.showProgressDialog(requireActivity())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        }
    }

    private fun loadTeams() {

        viewModel.employeeData.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { employeeList ->
                            teamEmployeeListGlobal = employeeList as ArrayList<Employee>

                            val isPresent =
                                teamEmployeeListGlobal.any { it.name == "All Employees" }
                            if (!isPresent) {
                                //  teamEmployeeListGlobal.removeAt(0)
                                teamEmployeeListGlobal.add(
                                    0,
                                    Employee(
                                        "",
                                        "",
                                        -1,
                                        false,
                                        "",
                                        "",
                                        -1,
                                        "All Employees",
                                        "",
                                        "",
                                        "",
                                        false,
                                        -1,
                                        "",
                                        -1,
                                        0.0,
                                        false
                                    )
                                )
                            }
                            val roleName = teamEmployeeListGlobal.map { it.name }

                            setUpEmployeeSpinnerAdapter(roleName as ArrayList<String>)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        try {
                            ProgressUtils.showProgressDialog(requireActivity())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun loadTerminals() {

        viewModel.getTerminalListDatabse.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { terminalList ->
                            terminalListGlobal =
                                terminalList as ArrayList<VenueDetailsResponse.Data.Terminal>

                            val isPresent = terminalListGlobal.any { it.name == "All Terminals" }

                            if (!isPresent) {
                                //    terminalListGlobal.removeAt(0)
                                terminalListGlobal.add(
                                    0,
                                    VenueDetailsResponse.Data.Terminal(
                                        "",
                                        -1,
                                        -1,
                                        false,
                                        "All Terminals",
                                        "",
                                        ""
                                    )
                                )
                            }

                            val roleName = terminalListGlobal.map { it.name }

                            setUpTerminalSpinnerAdapter(roleName as ArrayList<String>)

                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {

                        try {
                            ProgressUtils.showProgressDialog(requireActivity())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun getOrderType() {

        viewModel.orderTypes.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { terminalList ->
                            orderTypeListGlobal =
                                terminalList as ArrayList<TbOrderType>

                            val isPresent = orderTypeListGlobal.any { it.name == "All Orders" }

                            if (!isPresent) {
                                //   orderTypeListGlobal.removeAt(0)
                                orderTypeListGlobal.add(
                                    0,
                                    TbOrderType("", -1, false, -1, "All Orders", "", -1, "")
                                )
                            }
                            val roleName = orderTypeListGlobal.map { it.name }

                            setUpOrderTypeSpinnerAdapter(roleName as ArrayList<String>)

                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        try {
                            ProgressUtils.showProgressDialog(requireActivity())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        if (spinnerTouched) {
            currentPage = 1
            checkFilter = true

            transactionAdapter.clear()
            apiCallTimeSheet()
        }
        spinnerTouched = false
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.TEAM_MEMBER
        )
        navController.popBackStack()
    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (currentPage == 1) {
                    if (it) {
                        ProgressUtils.showProgressDialog(requireActivity())
                    } else {
                        ProgressUtils.dismissProgressDialog()
                    }
                }
            }
        }

    }


    private fun navigate() {
        viewModel.transactionDetails.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                if (!it.payableType.equals("Invoice", true)
                    && !it.payableType.equals(
                        "GiftCard",
                        true
                    )
                    &&
                    !it.payableType.equals("GiftCardAmountTab", true)
                ) {
                    val bundle = Bundle().apply {
                        putInt("orderId", it.orderDetails.id)
                        putInt("paymentId", it.id)
                        putBoolean("isFromTrans", true)
                        putString("customerName",(it.customer?.firstName ?: "") + " " + (it.customer?.lastName ?: ""))
                        putInt(
                            "selectedorderType",
                            binding.includeView.spOrders.selectedItemPosition
                        )
                        putInt(
                            "selectedtransactionType",
                            binding.includeView.spTransactionTypes.selectedItemPosition
                        )
                        putInt(
                            "selectedroleType",
                            binding.includeView.spRoles.selectedItemPosition
                        )
                        putInt(
                            "selectedemployeeType",
                            binding.includeView.spEmployees.selectedItemPosition
                        )
                        putInt(
                            "selectedterminalType",
                            binding.includeView.spTerminals.selectedItemPosition
                        )
                    }


                    if (findNavController().currentDestination?.id == R.id.transactionFragment) {
                        findNavController().navigate(
                            R.id.action_transactionFragment_to_transactionDetailsFragment,
                            bundle
                        )
                    }
                }
            }
        }

    }

    private fun orderUpdateTips() {
        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                binding.root.showAlert(it.message)

            }
        }

        viewModel.data2.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                transactionAdapter.updateTip(selectedPos, it)
            }
        }

    }

    override fun onItemClickListener(view: View?, pos: Int) {

        selectedPos = pos
        singleTransaction = transactionAdapter.getItem(pos)
        if (singleTransaction != null && !singleTransaction?.payableType.equals(
                "GiftCard",
                true
            ) && !singleTransaction?.payableType.equals(
                "Invoice", true
            ) &&
            !singleTransaction?.payableType.equals(
                "GiftCardAmountTab", true
            )
        ) {

            singleTransaction?.let {
                val refundedAmount =
                    String.format("%.2f", singleTransaction?.refundedAmount).toDouble()
                val totalAmount = String.format("%.2f", singleTransaction?.totalAmount).toDouble()

                if (refundedAmount == totalAmount) {

                    AlertUtils.showCustomAlert(
                        requireContext(),
                        "Tips cannot be added to transactions that have been refunded."
                    )

                } else {
                    val bundle = Bundle()
                    bundle.putDouble("totalTip", singleTransaction!!.tips)
                    bundle.putBoolean("isFromTransaction", true)
                    singleTransaction?.amount?.let { bundle.putDouble("totalPrice", it) }
                    if (findNavController().currentDestination?.id != R.id.addTipsDialog) {
                        findNavController().navigate(
                            R.id.action_transactionFragment_to_addTipsDialog,
                            bundle
                        )
                    }
                }
            }
        }


    }

    private fun magtekCall(refundAmount: Double) {
        if (singleTransaction?.orderDetails?.orderType == "OnlineWebOrder") {
            val model = Gson().fromJson(
                singleTransaction?.magensaResponse,
                MagtekOnlineOrderRefundResponse::class.java
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
                        singleTransaction?.amount?.times(100)?.let {
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

                    val amount = singleTransaction?.amount?.plus(refundAmount)

                    jsonArray = amount?.times(100)?.let {
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

                    val amount = singleTransaction?.amount?.plus(refundAmount)

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        amount?.times(100)?.let {
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
                        singleTransaction?.totalAmount.let {
                            it?.let { it2 ->
                                magtekRequestUtils.processReferenceIDTSYSCapture(
                                    it2,
                                    model.customerTransactionID ?: "",
                                    it1,
                                    (tipAmount)
                                )
                            }
                        }
                    }
                    networkCall(jsonArray, 1)
                }


            }
        } else {
            val model = Gson().fromJson(
                singleTransaction?.magensaResponse,
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
                        singleTransaction?.amount?.times(100)?.let {
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

                    val amount = singleTransaction?.amount?.plus(refundAmount)

                    jsonArray = amount?.times(100)?.let {
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

                    val amount = singleTransaction?.amount?.plus(refundAmount)

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        amount?.times(100)?.let {
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
                        singleTransaction?.totalAmount.let {
                            it?.let { it2 ->
                                magtekRequestUtils.processReferenceIDTSYSCapture(
                                    it2,
                                    model.customerTransactionID ?: "",
                                    it1,
                                    (tipAmount)
                                )
                            }
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

    private fun searchFilter() {
        binding.includeView.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.includeView.autoSearch.setText("")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                transactionAdapter.showLoading(false)
                transactionAdapter.filter.filter(s.toString().trim())
            }
        })
    }
}