package com.pays.pos.ui.dialog

import android.content.*
import android.graphics.Point
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.aidl.ICallback
import com.pays.pos.aidl.IWoyouService
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.EXTERNAL_PAYMENT
import com.pays.pos.data.remote.Constants.REFUND1
import com.pays.pos.databinding.DialogRefundReasonBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.toast
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pax.poslink.*
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import java.util.HashMap
import javax.inject.Inject


@AndroidEntryPoint
class ReasonForRefundDialog : DialogFragment(), ICallback {


    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = arrayListOf()
    private var paymentType: String = ""
    private var referenceNo: String? = null
    private var paxECRreferenceNo: String? = null
    private var paxToken: String? = null
    private var paxExtData = ""
    private var magensa_response_data: String = ""
    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogRefundReasonBinding
    private lateinit var refundData: RefundRequestModel
   // private lateinit var orderItemRefundsAttributes:String
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private val transactionViewModel by activityViewModels<TransactionDetailsViewModel>()
    private val magtekProViewModel by viewModels<MagtekViewModel>()
    private var woyouService: IWoyouService? = null

    private var requiredNABServerPostAPICall = false
    private var paxData = ""

    // PAX variables
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    @Inject
    lateinit var apiModule1: ApiModule1

    companion object {
        fun newInstance() = ReasonForRefundDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_refund_reason, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        Binding()
        getCustomerPrinters()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        refundData = arguments?.getParcelable("refundData")!!
    //    orderItemRefundsAttributes = arguments?.getString("orderItemRefundsAttributes").toString()
        refundAmount = arguments?.getDouble("refundAmount")!!
        magensa_response_data = arguments?.getString("magensa_response_data").toString()
        paymentType = arguments?.getString("paymentType").toString()
        referenceNo = arguments?.getString("pax_ref_num").toString()
        paxToken = arguments?.getString("pax_token").toString()
        paxExtData = arguments?.getString("pax_ext_data").toString()
        paxECRreferenceNo = arguments?.getString("pax_ecrref_num").toString()

        requiredNABServerPostAPICall = arguments?.getBoolean("requiredNABServerPostAPICall")!!
        paxData = arguments?.getString("pax_data")+""


        refundData.paymentRefund?.orderItemRefundsAttributes = transactionViewModel.orderItemAttribututes
        Log.d(
            "PAX params:",
            "pax params: paxToken-$paxToken paxECRreferenceNo-$paxECRreferenceNo referenceNo-$referenceNo paxExtData-${
                Gson().toJson(paxExtData)
            }"
        )

        binding.txtTitle.text = paymentType

        binding.tvTagRefundAmount.text = requireActivity()?.getString(R.string.tv_refund) + " " +
                requireActivity()?.getString(R.string.symbole) + "" + String.format(
            requireActivity().getString(R.string.format), refundAmount
        )


        binding.tvRefundAmount.text =
            requireActivity()?.getString(R.string.symbole) + " " + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )


        binding.txtDone.setOnClickListener {
            Log.d("referenceNo: ", "referenceNo $referenceNo")
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
            /*if (!referenceNo.isNullOrEmpty()) {
                refundViaPAX()
            } else {
                doneClick()
            }*/


           if (requiredNABServerPostAPICall && paxData.isNotEmpty()){
               runBlocking {
                   proceedWithServerPostApiRefund()
               }
           }else{
               if (referenceNo.isNullOrEmpty()) {
                   doneClick()
               } else if (!referenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                       Constants.IS_PAX_CONNECTED,
                       false
                   )
               ) {
//                refundViaPAX()
                   getBatchLocalReport()
               } else if (!referenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
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

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        //POSLink initialization for PAX
        initPOSLink()
        getMerchantDataObserver()
//        getBatchLocalReport()

        return binding.root
    }

    private suspend fun proceedWithServerPostApiRefund() {

        if (paxData.isNotEmpty()) {
            var CUST_NBR = ""
            var MERCH_NBR = ""
            var DBA_NBR = ""
            var TERMINAL_NBR = ""
            var TRAN_TYPE = "CCE7"
            var BATCH_ID = ""
            var TRAN_NBR = ""
            var ORIG_AUTH_GUID = ""
            var CARD_ENT_METH = ""
            var AMOUNT = ""
            var AUTH_GUID = ""

            var key = ""
            var value = ""
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            factory.setNamespaceAware(true)
            val xpp: XmlPullParser = factory.newPullParser()

            xpp.setInput(StringReader(paxData))
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    println("Start document")
                } else if (eventType == XmlPullParser.START_TAG) {

                    try {
                        key = xpp.getAttributeValue(0)
                    } catch (e: Exception) {
                        key = ""
                    }
                } else if (eventType == XmlPullParser.END_TAG) {

                } else if (eventType == XmlPullParser.TEXT) {
                    println("Text " + xpp.text)
                    if (!value.equals(xpp.text)) {
                        value = xpp.text
                    }
                }

                if (key.equals("CUST_NBR")) {
                    CUST_NBR = value
                } else if (key.equals("MERCH_NBR")) {
                    MERCH_NBR = value
                } else if (key.equals("DBA_NBR")) {
                    DBA_NBR = value
                } else if (key.equals("TERMINAL_NBR")) {
                    TERMINAL_NBR = value
                } else if (key.equals("BATCH_ID")) {
                    BATCH_ID = value
                } else if (key.equals("TRAN_NBR")) {
                    TRAN_NBR = value
                } else if (key.equals("AUTH_GUID")) {
                    AUTH_GUID = value
                } else if (key.equals("AUTH_AMOUNT")) {
                    AMOUNT = value
                }

                eventType = xpp.next()
            }


            CoroutineScope(Dispatchers.IO).async {
                val queue = Volley.newRequestQueue(requireContext())
                val url = "https://secure.epxuap.com/"
                val getRequest: StringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    object : com.android.volley.Response.Listener<String?> {
                        override fun onResponse(response: String?) {
                            // response
                            var AUTH_RESP_TEXT = ""
                            Log.d("Response", response!!)
                            xpp.setInput(StringReader(response))
                            var eventType = xpp.eventType
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                if (eventType == XmlPullParser.START_DOCUMENT) {
                                    println("Start document")
                                } else if (eventType == XmlPullParser.START_TAG) {

                                    try {
                                        key = xpp.getAttributeValue(0)
                                    } catch (e: Exception) {
                                        key = ""
                                    }
                                } else if (eventType == XmlPullParser.END_TAG) {

                                } else if (eventType == XmlPullParser.TEXT) {
                                    println("Text " + xpp.text)
                                    if (!value.equals(xpp.text)) {
                                        value = xpp.text
                                    }
                                }

                                if (key.equals("AUTH_RESP_TEXT")) {
                                    AUTH_RESP_TEXT = value
                                }

                                eventType = xpp.next()
                            }

                            if (AUTH_RESP_TEXT.contains("UNABLE")) {
                                /*Make refund Call*/
                                makeRefundCallToNAB(
                                    xpp,
                                    CUST_NBR,
                                    MERCH_NBR,
                                    DBA_NBR,
                                    TERMINAL_NBR,
                                    TRAN_TYPE,
                                    BATCH_ID,
                                    TRAN_NBR,
                                    AMOUNT,
                                    AUTH_GUID
                                )
                            } else if (AUTH_RESP_TEXT.contains("APPROVAL")) {
                                /*Make our server call*/

                                    refundCall()

                                Handler(Looper.getMainLooper()).post(object:java.lang.Runnable{
                                    override fun run() {
                                        ProgressUtils.dismissProgressDialog()
                                    }
                                })

                            }

                        }
                    },
                    object : com.android.volley.Response.ErrorListener {
                        override fun onErrorResponse(error: VolleyError) {
                            // TODO Auto-generated method stub
                            Log.d("ERROR", "error => $error")
                        }
                    }
                ) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["Accept"] = "*/*"
                        params["Cache-Control"] = "no-cache"
                        params["Host"] = "secure.epxuap.com"
                        params["Accept-Encoding"] = "gzip, deflate, br"
                        params["Connection"] = "keep-alive"
                        params["Content-Type"] = "application/x-www-form-urlencoded"
                        return params
                    }

                    @Throws(AuthFailureError::class)
                    override fun getParams(): Map<String, String>? {
                        val params: MutableMap<String, String> = HashMap()
                        params["CUST_NBR"] = CUST_NBR
                        params["MERCH_NBR"] = MERCH_NBR
                        params["DBA_NBR"] = DBA_NBR
                        params["TERMINAL_NBR"] = TERMINAL_NBR
                        params["TRAN_TYPE"] = "CCE7"
                        params["BATCH_ID"] = BATCH_ID
                        params["TRAN_NBR"] = TRAN_NBR
                        params["CARD_ENT_METH"] = "Z"
                        params["INDUSTRY_TYPE"] = "E"
                        params["ORIG_AUTH_GUID"] = ORIG_AUTH_GUID
                        return params
                    }
                }
                queue.add(getRequest)

                Handler(Looper.getMainLooper()).post(object:java.lang.Runnable{
                    override fun run() {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                })

            }.await()
        }
    }

    fun makeRefundCallToNAB(
        xpp: XmlPullParser,
        CUST_NBR: String,
        MERCH_NBR: String,
        DBA_NBR: String,
        TERMINAL_NBR: String,
        TRAN_TYPE: String,
        BATCH_ID: String,
        TRAN_NBR: String,
        AMOUNT: String,
        ORIG_AUTH_GUID: String
    ) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "https://secure.epxuap.com/"
        val getRequest: StringRequest = object : StringRequest(
            Request.Method.POST, url,
            object : com.android.volley.Response.Listener<String?> {
                override fun onResponse(response: String?) {
                    // response
                    Log.d("Response", response!!)
                    var AUTH_RESP_TEXT = ""
                    var key = ""
                    var value = ""

                    Log.d("Response", response!!)
                    xpp.setInput(StringReader(response))
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_DOCUMENT) {
                            println("Start document")
                        } else if (eventType == XmlPullParser.START_TAG) {

                            try {
                                key = xpp.getAttributeValue(0)
                            } catch (e: Exception) {
                                key = ""
                            }
                        } else if (eventType == XmlPullParser.END_TAG) {

                        } else if (eventType == XmlPullParser.TEXT) {
                            println("Text " + xpp.text)
                            if (!value.equals(xpp.text)) {
                                value = xpp.text
                            }
                        }

                        if (key.equals("AUTH_RESP_TEXT")) {
                            AUTH_RESP_TEXT = value
                        }

                        eventType = xpp.next()
                    }

                    if (AUTH_RESP_TEXT.contains("APPROVAL")) {


                        /*if (!(context as AppCompatActivity).isFinishing()) {
                            requireActivity().runOnUiThread(object:Runnable{
                                override fun run() {
                                    AlertUtils.showAlert(requireActivity(),AUTH_RESP_TEXT)
                                }

                            })

                        }*/
                        /* else{
                            try{
                                AlertUtils.showAlert(requireActivity(),AUTH_RESP_TEXT)

                            }catch (e:Exception){
                                try{
                                    AlertUtils.showAlert(requireContext(),AUTH_RESP_TEXT)
                                }catch (e:Exception){

                                }
                            }
                        }*/
                            refundCall()

                        Handler(Looper.getMainLooper()).post(object:java.lang.Runnable{
                            override fun run() {
                                ProgressUtils.showProgressDialog(requireActivity())
                            }
                        })

                    }else{
                        Handler(Looper.getMainLooper()).post(object :
                            java.lang.Runnable {
                            override fun run() {
                                ProgressUtils.dismissProgressDialog()
                            }
                        })
                        try{
                            AlertUtils.showCustomAlert(requireActivity(),AUTH_RESP_TEXT)

                        }catch (e:Exception){
                            try{
                                AlertUtils.showCustomAlert(requireContext(),AUTH_RESP_TEXT)
                            }catch (e:Exception){

                            }
                        }
                    }




                }
            },
            object : com.android.volley.Response.ErrorListener {
                override fun onErrorResponse(error: VolleyError) {
                    // TODO Auto-generated method stub
                    Log.d("ERROR", "error => $error")
                }
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Accept"] = "*/*"
                params["Cache-Control"] = "no-cache"
                params["Host"] = "secure.epxuap.com"
                params["Accept-Encoding"] = "gzip, deflate, br"
                params["Connection"] = "keep-alive"
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["CUST_NBR"] = CUST_NBR
                params["MERCH_NBR"] = MERCH_NBR
                params["DBA_NBR"] = DBA_NBR
                params["TERMINAL_NBR"] = TERMINAL_NBR
                params["TRAN_TYPE"] = "CCE9"
                params["BATCH_ID"] = BATCH_ID
                params["TRAN_NBR"] = TRAN_NBR
                params["CARD_ENT_METH"] = "Z"
                params["AMOUNT"] = AMOUNT
                params["ORIG_AUTH_GUID"] = ORIG_AUTH_GUID
                params["INDUSTRY_TYPE"] = "E"
                return params
            }
        }
        queue.add(getRequest)


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
                    checkBroadPOSVersion()
//                    refundViaPAX()
//                    AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    private fun checkBroadPOSVersion() {
        GlobalScope.launch {
            posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

            val manageRequest = ManageRequest()
            manageRequest.TransType = manageRequest.ParseTransType("INIT")
            posLink.ManageRequest = manageRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.ManageResponse

                val response = msg.obj as ManageResponse
                response.ResultCode
                response.resultTxt

                val response11 = response.ExtData
                Log.d("response11: ", "response11-${Gson().toJson(response11)}")
                val regex = Regex("<AppName>(.*?)</AppName>")
                val matchResult = regex.find(response11)
                val versionName = matchResult?.groupValues?.getOrNull(1)

                if (versionName != null) {
                    println("versionName value: $versionName")
                    if (versionName.contains("TSYS")) {
                        Log.d("versionName:", "versionName $versionName")
                        refundViaPAXTSYS()
                    } else if (versionName.contains("Rapid")) {
                        Log.d("versionName:", "versionName $versionName")
                        refundViaPAX("Rapid")
                    } else if (versionName.contains("EPX")) {
                        Log.d("versionName:", "versionName $versionName")
                        refundViaPAX("EPX")
                    }
                } else {
                    println("versionName value not found")
                }
            }
        }
    }

    private fun getBatchLocalReport() {
        GlobalScope.launch {
            posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

            Log.d("paxRefNo: ", "paxRefNo: ${referenceNo}")
            try{
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }catch (e:Exception){}


            val report = ReportRequest()
            report.TransType = report.ParseTransType("LOCALDETAILREPORT") //recommend
            report.EDCType = report.ParseEDCType("CREDIT")
            report.RefNum = referenceNo
            report.ECRRefNum = paxECRreferenceNo

            posLink.ReportRequest = report
            val result = posLink.ProcessTrans()
            Log.d("result batch: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.ReportResponse

                val response = msg.obj as com.pax.poslink.ReportResponse
                val resultCode = response.ResultCode
                val resultTxt = response.ResultTxt

                if (resultCode == "000000") {
                    voidViaPAX()
                } else if (resultCode == "100023") {
                    //Transaction not found in current batch
                    //refundViaPAX()
                    checkBroadPOSVersion()
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

                Log.d(
                    "Params:",
                    "Report $resultCode $resultTxt ${response.ExtData}  ${Gson().toJson(response)}"
                )
            }
        }
    }

    private fun refundViaPAXTSYS() {
        if (refundAmount != 0.0 || refundAmount > 0.0) {
            if (paymentType == "Card") {
                GlobalScope.launch {
                    posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

                    val amt = (refundAmount * 100).toInt()
                    val refund = PaymentRequest()
                    refund.TenderType = refund.ParseTenderType("CREDIT")
                    refund.TransType = refund.ParseTransType("RETURN")
                    //Added for TSYS RETURN 100003 issue
                    refund.ECRRefNum = paxECRreferenceNo

                    refund.Amount = amt.toString()
                    posLink.PaymentRequest = refund
                    val result = posLink.ProcessTrans()
                    Log.d("result: ", result.Code.toString() + " " + result.Msg)
                    if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                        val msg = Message()
                        msg.what = Constants.TRANSACTION_SUCCESSED
                        msg.obj = posLink.PaymentResponse

                        val response = msg.obj as com.pax.poslink.PaymentResponse
                        val resultCode = response.ResultCode
                        val resultTxt = response.ResultTxt

                        if (resultCode == "000000") {
                            CoroutineScope(Dispatchers.Main).launch {
                                refundCall()
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.dismissProgressDialog()
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    resultTxt,
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            try {
                                                p0?.dismiss()
                                            } catch (e: Exception) {
                                            }
                                        }
                                    })
//                                requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlertWithListenerWithOKCancel(
                                requireContext(),
                                getString(R.string.pax_connect_error),
                                getString(R.string.reconnect),
                            )
                            { _, _ ->
                                // Add connect to PAX logic
                                magtekProViewModel.initPOSLink(requireContext())
                            }
                        }
                    }
                }
            } else {
                refundCall()
            }
        } else {
            AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
        }

    }

    private fun refundViaPAX(processor:String) {
        if (refundAmount != 0.0 || refundAmount > 0.0) {
            if (paymentType == "Card") {
                GlobalScope.launch {
                    posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

                    /*CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }*/

                    val response11 = paxExtData
                    val regex = Regex("<ExpDate>(\\d{4})</ExpDate>")
                    val matchResult = regex.find(response11)
                    val expDateValue = matchResult?.groupValues?.getOrNull(1)

                    if (expDateValue != null) {
                        println("ExpDate value: $expDateValue")
                    } else {
                        println("ExpDate value not found")
                    }

                    if (processor.equals("rapid",ignoreCase = true)) {
                        val amt = (refundAmount * 100).toInt()
                        val refund = PaymentRequest()
                        refund.TenderType = refund.ParseTenderType("CREDIT")
                        refund.TransType = refund.ParseTransType("RETURN")
                        refund.ExtData = "<ExpDate>$expDateValue</ExpDate><Token>$paxToken</Token>"

                        refund.Amount = amt.toString()
                        posLink.PaymentRequest = refund
                        val result = posLink.ProcessTrans()
                        Log.d("result: ", result.Code.toString() + " " + result.Msg)
                        if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                            val msg = Message()
                            msg.what = Constants.TRANSACTION_SUCCESSED
                            msg.obj = posLink.PaymentResponse

                            val response = msg.obj as com.pax.poslink.PaymentResponse
                            val resultCode = response.ResultCode
                            val resultTxt = response.ResultTxt

                            if (resultCode == "000000") {
                                CoroutineScope(Dispatchers.Main).launch {
                                    refundCall()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    ProgressUtils.dismissProgressDialog()
                                    requireActivity().toast(
                                        "$resultCode $resultTxt",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            }
                        }

                    }

                    else if (processor.equals("epx", ignoreCase = true)) {
                        val amt = (refundAmount * 100).toInt()
                        val refund = PaymentRequest()
                        refund.ECRRefNum = posLink.ReportRequest.ECRRefNum
                        refund.TenderType = refund.ParseTenderType("CREDIT")
                        refund.TransType = refund.ParseTransType("RETURN")
                        refund.OrigRefNum = referenceNo
                        /*Experiment- START*/ //Experiment Success
                        refund.ExtData = "<HRef>$referenceNo</HRef>"
                        /*Experiment- END*/
//                    refund.ExtData = "<ExpDate>$expDateValue</ExpDate><Token>$paxToken</Token>"
                        refund.Amount = amt.toString()
                        posLink.PaymentRequest = refund
                        val result = posLink.ProcessTrans()
                        Log.d("result: ", result.Code.toString() + " " + result.Msg)
                        if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                            val msg = Message()
                            msg.what = Constants.TRANSACTION_SUCCESSED
                            msg.obj = posLink.PaymentResponse

                            val response = msg.obj as com.pax.poslink.PaymentResponse
                            val resultCode = response.ResultCode
                            val resultTxt = response.ResultTxt

                            if (resultCode == "000000") {
                                CoroutineScope(Dispatchers.Main).launch {
                                    refundCall()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    ProgressUtils.dismissProgressDialog()
                                    requireActivity().toast(
                                        "$resultCode $resultTxt",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            }
                        }
                    }

                    else {
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

                            /*if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT") {
                                AlertUtils.showCustomAlertWithListenerWithOKCancel(
                                    requireContext(),
                                    getString(R.string.pax_connect_error), getString(R.string.reconnect),
                                )
                                { _, _ ->
                                    // Add connect to PAX logic
                                    magtekProViewModel.initPOSLink(requireContext())
                                }
                            *//*Toast.makeText(
                                    requireContext(),
                                    R.string.pax_connect_error,
                                    Toast.LENGTH_LONG
                                ).show()*//*
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "getMerchantDetails Failed ${result.Code} ${result.Msg}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }*/
                        }
                    }
                }
            } else {
                refundCall()
            }
        } else {
            AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
        }
    }

    private fun voidViaPAX() {
        if (refundAmount != 0.0 || refundAmount > 0.0) {
            if (paymentType == "Card") {
                GlobalScope.launch {
                    posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

                    /*CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }*/
                    val amt = (refundAmount * 100).toInt()
                    Log.d("amt: ", "amtxx $amt")
                    val refund = PaymentRequest()
                    refund.TenderType = refund.ParseTenderType("CREDIT")
                    refund.TransType = refund.ParseTransType("VOID")
                    refund.ECRRefNum = System.currentTimeMillis().toString()
                    refund.OrigRefNum = referenceNo

//                    refund.Amount = amt.toString()
                    posLink.PaymentRequest = refund
                    val result = posLink.ProcessTrans()
                    Log.d("result void: ", result.Code.toString() + " " + result.Msg)
                    if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                        val msg = Message()
                        msg.what = Constants.TRANSACTION_SUCCESSED
                        msg.obj = posLink.PaymentResponse

                        val response = msg.obj as com.pax.poslink.PaymentResponse
                        val resultCode = response.ResultCode
                        val resultTxt = response.ResultTxt

                        if (resultCode == "000000") {
                            CoroutineScope(Dispatchers.Main).launch {
                                refundCall()
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.dismissProgressDialog()
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    resultTxt,
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            try {
                                                p0?.dismiss()
                                            } catch (e: Exception) {
                                            }
                                        }
                                    })
//                                requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlertWithListenerWithOKCancel(
                                requireContext(),
                                getString(R.string.pax_connect_error),
                                getString(R.string.reconnect),
                            )
                            { _, _ ->
                                // Add connect to PAX logic
                                magtekProViewModel.initPOSLink(requireContext())
                            }
                        }
                    }
                }
            } else {
                refundCall()
            }
        } else {
            AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
        }

    }

    private fun doneClick() {
        if (refundAmount != 0.0 || refundAmount > 0.0) {
            if (paymentType == "Card") {

                val model = Gson().fromJson(
                    magensa_response_data,
                    PaymentResponse.PaymentResponseItem::class.java
                )

                val jsonArray: JsonArray?

                when {

                    Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                        if (model != null) {
                            jsonArray =
                                model.transactionOutput?.token?.let { it1 ->
                                    magtekRequestUtils.processTokenFirstData(
                                        (refundAmount * 100),
                                        it1,
                                        model.customerTransactionID ?: "",
                                        model.transactionOutput.transactionOutputDetails[0].value,
                                        REFUND1
                                    )
                                }

                            networkCall(jsonArray, 0)
                        }
                    }


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
                            magtekRequestUtils.processReferenceIDEPX(
                                (refundAmount * 100),
                                model.customerTransactionID ?: "", it1, REFUND1
                            )
                        }
                        networkCall(jsonArray, 1)
                    }

                    Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                        jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                            magtekRequestUtils.processReferenceIDRefund(
                                (refundAmount * 100),
                                model.customerTransactionID ?: "", it1,
                                model.transactionOutput.authCode
                            )
                        }
                        networkCall(jsonArray, 1)
                    }

                    Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                        jsonArray = magtekRequestUtils.processTokenChase(
                            (refundAmount * 100),
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            REFUND1
                        )

                        networkCall(jsonArray, 0)
                    }
                    Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                        jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                            magtekRequestUtils.processReferenceIHeartland(
                                (refundAmount * 100),
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                REFUND1
                            )
                        }
                        networkCall(jsonArray, 1)
                    }
                    Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                        jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                            magtekRequestUtils.processReferenceIDTSYS(
                                (refundAmount),
                                model.customerTransactionID ?: "", it1, REFUND1
                            )
                        }
                        networkCall(jsonArray, 1)
                    }


                }


            } else {
                refundCall()
            }
        } else {
            AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
        }
    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

//        ProgressUtils.showProgressDialog(requireActivity())
        viewModel.showProgressDialog(true)

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
//                ProgressUtils.dismissProgressDialog()
                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null) {

                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {

                            refundCall()

                        } else {
                            viewModel.showProgressDialog(false)
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].transactionOutput?.transactionMessage
                            )
                        }

                    } else {
                        viewModel.showProgressDialog(false)
                        if (response.body()!![0].mPPGv4WSFault != null) {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].mPPGv4WSFault?.faultCode + "\n" +
                                        response.body()!![0].mPPGv4WSFault?.faultReason
                            )
                        }
                    }
                } else {
//                    ProgressUtils.dismissProgressDialog()
                    viewModel.showProgressDialog(false)
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {

//                ProgressUtils.dismissProgressDialog()
                viewModel.showProgressDialog(false)
            }
        })
    }

    private fun refundCall() {
        viewModel.refundPaymentApiCall(
            refundAmount,
            refundData,
            binding.edtReasonForRefund.text.toString(),
            paymentType
        )
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

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

    private fun getCustomerPrinters() {
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

    private fun navigate() {

        viewModel.dataRefundDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        //  prefProvider.setValueboolean(IS_REFUND, true)

                        if (paymentType == "Card" || paymentType == EXTERNAL_PAYMENT) {
                            sendToTransaction()
                        } else if (customerList.isEmpty()) {
                            sendToTransaction()
                        } else {

                            val data = customerList
                            for (i in 0 until data.size) {
                                if (data[i].name.startsWith("CloudPrint", true)) {
                                    if (woyouService != null) {

                                        sendToTransaction()
                                        woyouService!!.sendRAWData(
                                            byteArrayOf(0x1B, 0x45, 0x01),
                                            this
                                        )
                                    } else {
                                        val aa = ByteArray(5)

                                        aa[0] = 0x10
                                        aa[1] = 0x14
                                        aa[2] = 0x00
                                        aa[3] = 0x00
                                        aa[4] = 0x00

                                        try {
                                            SunmiPrinterApi.getInstance().sendRawData(aa)
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }

                                        try {
                                            sendToTransaction()
                                            SunmiPrintHelper.getInstance().openCashBox()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                            sendToTransaction()
                                        }
                                    }

                                } else if (data[i].name.startsWith("InnerPrinter", true)) {

                                    if (woyouService != null) {
                                        sendToTransaction()
                                        woyouService!!.sendRAWData(
                                            byteArrayOf(0x1B, 0x45, 0x01),
                                            this
                                        )
                                    } else {
                                        val aa = ByteArray(5)

                                        aa[0] = 0x10
                                        aa[1] = 0x14
                                        aa[2] = 0x00
                                        aa[3] = 0x00
                                        aa[4] = 0x00


                                        try {
                                            SunmiPrinterApi.getInstance().sendRawData(aa)
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                        try {
                                            sendToTransaction()
                                            SunmiPrintHelper.getInstance().openCashBox()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                            sendToTransaction()
                                        }

                                    }

                                } else {
                                    try {
                                        var mPrinter =
                                            Printer(
                                                Printer.TM_M30,
                                                Printer.MODEL_ANK,
                                                (activity as MainActivity).applicationContext
                                            )


                                        var printerAdd =
                                            if (data[i].printer_type == Constants.BLUETOOTH) "BT:" + data[i].macAddress else "TCP:" + data[i].ipAddress
                                        mPrinter.connect(
                                            printerAdd,
                                            Printer.PARAM_DEFAULT
                                        )

                                        mPrinter.addPulse(
                                            com.epson.epos2.printer.Printer.DRAWER_HIGH,
                                            com.epson.epos2.printer.Printer.PULSE_100
                                        )

                                        try {
                                            mPrinter.sendData(Printer.PARAM_DEFAULT)
                                            mPrinter.disconnect()
                                            sendToTransaction()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                            try {
                                                mPrinter.disconnect()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            sendToTransaction()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            }

//                            customerList.forEach {
//                                PrinterClass.closePrinter()
//                                if (PrinterClass.getPrinter() == null) {
//
//                                    var printer: Print? = Print(requireContext())
//                                    val enabled = Print.FALSE
//                                    try {
//                                        var interval: Int = 1000
//                                        if (it.printer_type == Constants.BLUETOOTH) {
//                                            interval = PrinterClass.BLUETOOTH_TIMEOUT
//                                        }
//                                        printer?.openPrinter(
//
//                                            if (it.printer_type == Constants.BLUETOOTH) {
//                                                Print.DEVTYPE_BLUETOOTH
//                                            } else {
//                                                Print.DEVTYPE_TCP
//                                            },
//                                            it.ipAddress,
//                                            enabled,
//                                            1000
//                                        )
//                                        // printer?.setStatusChangeEventCallback(this)
//
//                                    } catch (e: Exception) {
//
//                                        //LogUtil.logE(TAG, "PrinterException: " + e.message)
//                                        printer = null
//                                        e.printStackTrace()
//                                        sendToTransaction()
//                                    }
//
//                                    try {
//
//                                        if (printer != null) {
//                                            PrinterClass.setPrinter(printer)
//
//                                            var builder: Builder? = null
//                                            try {
//                                                builder =
//                                                    Builder(
//                                                        if (it.name.substring(0, 6)
//                                                                .toString()
//                                                                .lowercase() == "TM-m30".lowercase()
//                                                        ) {
//                                                            "TM-m30"
//                                                        } else {
//                                                            it.name
//                                                        }, PrinterClass.language, requireActivity()
//                                                    )
//
//                                                val status = IntArray(1)
//                                                val battery = IntArray(1)
//                                                builder.addPulse(
//                                                    com.epson.epos2.printer.Printer.DRAWER_HIGH,
//                                                    com.epson.epos2.printer.Printer.PULSE_100
//                                                )
//                                                sendToTransaction()
//                                                PrinterClass.getPrinter()?.sendData(
//                                                    builder,
//                                                    PrinterClass.BLUETOOTH_TIMEOUT, status, battery
//                                                )
//                                                PrinterClass.closePrinter()
//
//                                            } catch (e: java.lang.Exception) {
//                                                e.printStackTrace()
//                                                sendToTransaction()
//                                            }
//                                        }
//
//                                    } catch (e: Exception) {
//
//                                        e.printStackTrace()
//                                        sendToTransaction()
//                                    }
//
//                                }
//                            }
                        }
                        //  findNavController().navigateUp()
                    }
                }
            }
        }

    }


    private fun sendToTransaction() {
        val bundle = Bundle().apply {
            putInt("orderId", refundData.paymentRefund?.orderId!!)
            putInt("paymentId", refundData.paymentRefund?.paymentId!!)
        }

        if (findNavController().currentDestination?.id == R.id.reasonForRefundDialog) {
            findNavController().navigate(
                R.id.action_reasonForRefundDialog_to_transactionDetailsFragment, bundle
            )
        }
    }


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            LogUtil.logE("TAG", "onServiceConnected  1")
            woyouService = IWoyouService.Stub.asInterface(service)

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            LogUtil.logE("TAG", "onServiceDisConnected  2")
            woyouService = null


        }

    }

    private fun Binding() {
        val intent = Intent()
        intent.setPackage("com.pays.pos")
        intent.action = "com.pays.pos.aidl.IWoyouService"
        MainApplication.getInstance()?.applicationContext?.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun asBinder(): IBinder {
        return woyouService?.asBinder()!!
    }

    override fun onRunResult(isSuccess: Boolean, code: Int, msg: String?) {
    }


}