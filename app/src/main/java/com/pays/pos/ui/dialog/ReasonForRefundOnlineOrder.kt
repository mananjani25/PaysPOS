package com.pays.pos.ui.dialog

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings.Global
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.MagtekOnlineOrderRefundResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ReasonrefundonlineorderBinding

import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.onlineorder.OnlineDetailViewModel
import com.pays.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.pays.pos.utils.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pax.poslink.*
import com.pays.pos.ui.fragments.allorders.AllOrdersViewModel
import com.pays.pos.utils.extensions.toast
import com.pays.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import javax.inject.Inject

@AndroidEntryPoint
class ReasonForRefundOnlineOrder : DialogFragment() {
    private var magensa_response_data: String = ""
    private var paxData: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    private var posLink: PosLink = PosLink()


    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    private val viewModel by activityViewModels<OnlineDetailViewModel>()
    private lateinit var binding: ReasonrefundonlineorderBinding
    private var refundAmount = 0.0
    private var isfromTransaction: Boolean = false

    @Inject
    lateinit var apiModule1: ApiModule1
    private lateinit var refundData: RefundRequestModelOnlineOrder

    private var requiredNABServerPostAPICall = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = ReasonrefundonlineorderBinding.inflate(
            inflater, container, false
        )

        /* putString("pax_ref_num", paymentOrderDetailsResponse.data.ref_num)
         putString("pax_ecrref_num", paymentOrderDetailsResponse.data.ecr_ref_num)*/
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        refundData = arguments?.getParcelable("refundData")!!
        refundAmount = arguments?.getDouble("refundAmount")!!
        isfromTransaction = arguments?.getBoolean("isfromTransaction") == true
        magensa_response_data = arguments?.getString("magensa_response_data").toString()

        requiredNABServerPostAPICall = arguments?.getBoolean("requiredNABServerPostAPICall")!!
        if (arguments?.containsKey("pax_response_data")==true){
            paxData = arguments?.getString("pax_response_data") + ""
        }else{
            paxData = arguments?.getString("pax_data") + ""
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeShowProgress()

        navigateToOnlineOrder()
        binding.lifecycleOwner = this
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context, R.color.bg_color))
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.tvTagRefundAmount.text =
            requireActivity()?.getString(R.string.tv_refund) + " " + requireActivity()?.getString(R.string.symbole) + "" + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )
        binding.tvRefundAmount.text =
            requireActivity()?.getString(R.string.symbole) + " " + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.txtDone.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                MethodUtils.hideSoftKeyboard(requireActivity())
                if (refundAmount != 0.0 || refundAmount > 0.0) {
                    if (paxData.isNotEmpty()) {
                        runBlocking {
                            startServerPOSTRefund()
                        }
                    } else if (magensa_response_data.isNotEmpty() && !magensa_response_data.contains("null")) {
//                Magensa implementation
                        val jsonParser = JsonParser()
                        var jsonObject: JsonObject? = null
                        try {
                            jsonObject = jsonParser.parse(magensa_response_data).asJsonObject
                        } catch (e: Exception) {
                            val a =0
                        }

                        val model = Gson().fromJson(
                            jsonObject, MagtekOnlineOrderRefundResponse::class.java
                        )

                        val jsonArray: JsonArray?

                        /*This is the Magensa implementation - START*/
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
                                                Constants.REFUND1
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
                                        model.customerTransactionID ?: "", it1, Constants.REFUND1
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
                                    Constants.REFUND1
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
                                        Constants.REFUND1
                                    )
                                }
                                networkCall(jsonArray, 1)
                            }
                            Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                                jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                                    magtekRequestUtils.processReferenceIDTSYS(
                                        (refundAmount),
                                        model.customerTransactionID ?: "", it1, Constants.REFUND1
                                    )
                                }
                                networkCall(jsonArray, 1)
                            }


                        }
//This is the Magensa implementation - END

                    }
/*
                checkBroadPOSVersion()
*/
                } else {
                    AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
                }


            }
        })

    }

    private suspend fun startServerPOSTRefund() {

//                NAB Server POST API implementation
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
            var url=""
            if (Constants.isPaxInDebugMode){
                url=Constants.paxDebug
            }else{
                url=Constants.paxLive
            }
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

                        if (AUTH_RESP_TEXT.lowercase().contains("unable")) {
                            /*Make refund Call*/
                            makeRefundCall(
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
                        } else {
                            /*Make server call*/
                            refundCall()
                            Handler(Looper.getMainLooper()).post(object :
                                java.lang.Runnable {
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

            Handler(Looper.getMainLooper()).post(object :
                java.lang.Runnable {
                override fun run() {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            })

        }.await()

    }

    fun makeRefundCall(
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
        var url=""
        if (Constants.isPaxInDebugMode){
            url=Constants.paxDebug
        }else{
            url=Constants.paxLive
        }
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
                        ProgressUtils.dismissProgressDialog()
                    } else {

                        Handler(Looper.getMainLooper()).post(object :
                            java.lang.Runnable {
                            override fun run() {
                                ProgressUtils.dismissProgressDialog()
                            }
                        })
                        try {
                            AlertUtils.showCustomAlert(requireActivity(), AUTH_RESP_TEXT)

                        } catch (e: Exception) {
                            try {
                                AlertUtils.showCustomAlert(requireContext(), AUTH_RESP_TEXT)
                            } catch (e: Exception) {

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
                val params: MutableMap<String, String> = java.util.HashMap()
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
                val params: MutableMap<String, String> = java.util.HashMap()
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

    /*  private fun checkBroadPOSVersion() {
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
  */
    /*  private fun getBatchLocalReport() {
          GlobalScope.launch {
              posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

              CoroutineScope(Dispatchers.Main).launch {
                  ProgressUtils.showProgressDialog(requireActivity())
              }

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

                      CoroutineScope(Dispatchers.Main).launch {
                          ProgressUtils.showProgressDialog(requireActivity())
                      }

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
                          Experiment- START //Experiment Success
                          refund.ExtData = "<HRef>$referenceNo</HRef>"
                          Experiment- END
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

                              if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT") {
                                  AlertUtils.showCustomAlertWithListenerWithOKCancel(
                                      requireContext(),
                                      getString(R.string.pax_connect_error), getString(R.string.reconnect),
                                  )
                                  { _, _ ->
                                      // Add connect to PAX logic
                                      magtekProViewModel.initPOSLink(requireContext())
                                  }
                              Toast.makeText(
                                      requireContext(),
                                      R.string.pax_connect_error,
                                      Toast.LENGTH_LONG
                                  ).show()
                              } else {
                                  Toast.makeText(
                                      requireContext(),
                                      "getMerchantDetails Failed ${result.Code} ${result.Msg}",
                                      Toast.LENGTH_LONG
                                  ).show()
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

      private fun voidViaPAX() {
          if (refundAmount != 0.0 || refundAmount > 0.0) {
              if (paymentType == "Card") {
                  GlobalScope.launch {
                      posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))

                      CoroutineScope(Dispatchers.Main).launch {
                          ProgressUtils.showProgressDialog(requireActivity())
                      }
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

      }*/

    private fun navigateToOnlineOrder() {
        viewModel.dataRefundDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        if (isfromTransaction) {
                            val bundle = Bundle().apply {
                                putInt("orderId", refundData.paymentRefund?.orderId!!)
                                putBoolean("isFromOnlineOrderRefund", true)
                                putInt("paymentId", refundData.paymentRefund?.paymentId!!)
                            }

                            findNavController().navigate(
                                R.id.action_reasonForRefundDialogonline_to_transactionfragment,
                                bundle
                            )
                        } else {
                            val result = Bundle().apply {
                                refundData.paymentRefund?.orderId?.let { it1 ->
                                    putInt(
                                        "order_id", it1
                                    )
                                }
                            }
                            requireActivity().supportFragmentManager.setFragmentResult(
                                "request_for_rejectOrder", result
                            )
                            findNavController().navigateUp()
                        }

                    }

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
                call: Call<PaymentResponse>, response: Response<PaymentResponse>
            ) {
                ProgressUtils.dismissProgressDialog()
                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null && response.body()!![0].transactionOutput?.isTransactionApproved == true) {

                        refundCall()

                    } else {
                        if (response.body()!![0].mPPGv4WSFault != null) AlertUtils.showCustomAlert(
                            requireContext(),
                            response.body()!![0].mPPGv4WSFault?.faultCode + "\n" + response.body()!![0].mPPGv4WSFault?.faultReason
                        )
                    }
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {

                ProgressUtils.dismissProgressDialog()
            }
        })
    }

    private fun refundCall() {
        viewModel.refundPaymentApiCall(
            refundAmount, refundData, binding.edtReasonForRefund.text.toString(), "Card"
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
}