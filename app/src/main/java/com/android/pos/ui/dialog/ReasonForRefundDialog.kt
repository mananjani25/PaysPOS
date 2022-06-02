package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.requestModel.RefundRequestModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.REFUND1
import com.android.pos.databinding.DialogRefundReasonBinding
import com.android.pos.di.ApiModule1
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.magtek.MagtekRequestUtils
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


@AndroidEntryPoint
class ReasonForRefundDialog : DialogFragment() {


    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = arrayListOf()
    private var paymentType: String = ""
    private var magensa_response_data: String = ""
    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogRefundReasonBinding
    private lateinit var refundData: RefundRequestModel
    private val viewModel by viewModels<TransactionDetailsViewModel>()

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

        getCustomerPrinters()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        refundData = arguments?.getParcelable("refundData")!!
        refundAmount = arguments?.getDouble("refundAmount")!!
        magensa_response_data = arguments?.getString("magensa_response_data").toString()
        paymentType = arguments?.getString("paymentType").toString()


        binding.tvTagRefundAmount.text = requireActivity()?.getString(R.string.tv_refund) + " " +
                requireActivity()?.getString(R.string.symbole) + "" + String.format(
            requireActivity().getString(R.string.format), refundAmount
        )


        binding.tvRefundAmount.text =
            requireActivity()?.getString(R.string.symbole) + " " + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )


        binding.txtDone.setOnClickListener {
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
                                            (refundAmount * 100).toInt(),
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
                                        (refundAmount * 100).toInt(),
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
                                    (refundAmount * 100).toInt(),
                                    model.customerTransactionID ?: "", it1, REFUND1
                                )
                            }
                            networkCall(jsonArray, 1)
                        }

                        Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                            jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                                magtekRequestUtils.processReferenceIDRefund(
                                    (refundAmount * 100).toInt(),
                                    model.customerTransactionID ?: "", it1,
                                    model.transactionOutput.authCode
                                )
                            }
                            networkCall(jsonArray, 1)
                        }

                        Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                            jsonArray = magtekRequestUtils.processTokenChase(
                                (refundAmount * 100).toInt(),
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
                                    (refundAmount * 100).toInt(),
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
                                    (refundAmount * 100).toInt(),
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
                AlertUtils.showCustomAlert(requireActivity(), "Please Enter Amount To Refund")
            }


        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
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
                    Log.e("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null && response.body()!![0].transactionOutput?.isTransactionApproved == true) {

                        refundCall()

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

                        customerList.forEach {
                            PrinterClass.closePrinter()
                            if (PrinterClass.getPrinter() == null) {

                                var printer: Print? = Print(requireContext())
                                val enabled = Print.FALSE
                                try {
                                    var interval: Int = 1000
                                    if (it.printer_type == Constants.BLUETOOTH) {
                                        interval = PrinterClass.BLUETOOTH_TIMEOUT
                                    }
                                    printer?.openPrinter(

                                        if (it.printer_type == Constants.BLUETOOTH) {
                                            Print.DEVTYPE_BLUETOOTH
                                        } else {
                                            Print.DEVTYPE_TCP
                                        },
                                        it.ipAddress,
                                        enabled,
                                        1000
                                    )
                                    // printer?.setStatusChangeEventCallback(this)

                                } catch (e: Exception) {

                                    //Log.e(TAG, "PrinterException: " + e.message)
                                    printer = null
                                    e.printStackTrace()
                                }

                                try {

                                    if (printer != null) {
                                        PrinterClass.setPrinter(printer)

                                        var builder: Builder? = null
                                        try {
                                            builder =
                                                Builder(
                                                    if (it.name.substring(0, 6)
                                                            .toString()
                                                            .lowercase() == "TM-m30".lowercase()
                                                    ) {
                                                        "TM-m30"
                                                    } else {
                                                        it.name
                                                    }, PrinterClass.language, requireActivity()
                                                )

                                            val status = IntArray(1)
                                            val battery = IntArray(1)
                                            builder.addPulse(
                                                com.epson.epos2.printer.Printer.DRAWER_HIGH,
                                                com.epson.epos2.printer.Printer.PULSE_100
                                            )

                                            PrinterClass.getPrinter()?.sendData(
                                                builder,
                                                PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                                            )
                                            PrinterClass.closePrinter()

                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                } catch (e: Exception) {

                                    e.printStackTrace()
                                }

                            }
                        }
                        val bundle = Bundle().apply {
                            putInt("orderId", refundData.paymentRefund?.orderId!!)
                            putInt("paymentId", refundData.paymentRefund?.paymentId!!)
                        }

                        findNavController().navigate(
                            R.id.action_reasonForRefundDialog_to_transactionDetailsFragment, bundle
                        )
                        //  findNavController().navigateUp()
                    }
                }
            }
        }

    }


}