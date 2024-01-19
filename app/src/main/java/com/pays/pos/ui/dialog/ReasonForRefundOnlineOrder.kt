package com.pays.pos.ui.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class ReasonForRefundOnlineOrder : DialogFragment() {
    private var magensa_response_data: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    private val viewModel by activityViewModels<OnlineDetailViewModel>()
    private lateinit var binding: ReasonrefundonlineorderBinding
    private var refundAmount = 0.0
    private var isfromTransaction:Boolean = false
    @Inject
    lateinit var apiModule1: ApiModule1
    private lateinit var refundData: RefundRequestModelOnlineOrder
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            ReasonrefundonlineorderBinding.inflate(
                inflater,
                container,
                false
            )
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        refundData = arguments?.getParcelable("refundData")!!
        refundAmount = arguments?.getDouble("refundAmount")!!
        isfromTransaction = arguments?.getBoolean("isfromTransaction") == true
        magensa_response_data = arguments?.getString("magensa_response_data").toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeShowProgress()

        navigateToOnlineOrder()
        binding.lifecycleOwner = this
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.tvTagRefundAmount.text = requireActivity()?.getString(R.string.tv_refund) + " " +
                requireActivity()?.getString(R.string.symbole) + "" + String.format(
            requireActivity().getString(R.string.format), refundAmount
        )
        binding.tvRefundAmount.text =
            requireActivity()?.getString(R.string.symbole) + " " + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.txtDone.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            Log.d(TAG, "onViewCreated: " + Gson().toJson(magensa_response_data))
            val jsonParser = JsonParser()
            val jsonObject = jsonParser.parse(magensa_response_data).asJsonObject
            Log.d(TAG, "onViewCreated: " + jsonObject)
            if (refundAmount != 0.0 || refundAmount > 0.0) {
                val model = Gson().fromJson(
                    jsonObject,
                    MagtekOnlineOrderRefundResponse::class.java
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


            } else {
                AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
            }


        }

    }

    private fun navigateToOnlineOrder() {
        viewModel.dataRefundDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        if(isfromTransaction){
                            val bundle = Bundle().apply {
                                putInt("orderId", refundData.paymentRefund?.orderId!!)
                                putBoolean("isFromOnlineOrderRefund",true)
                                putInt("paymentId", refundData.paymentRefund?.paymentId!!)
                            }

                            findNavController().navigate(
                                R.id.action_reasonForRefundDialogonline_to_transactionfragment, bundle
                            )
                        }else{
                            val result = Bundle().apply {
                                refundData.paymentRefund?.orderId?.let { it1 -> putInt("order_id", it1) }
                            }
                            requireActivity().supportFragmentManager.setFragmentResult(
                                "request_for_rejectOrder",
                                result
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
                call: Call<PaymentResponse>,
                response: Response<PaymentResponse>
            ) {
                ProgressUtils.dismissProgressDialog()
                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
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
            "Card"
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