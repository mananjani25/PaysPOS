package com.android.pos.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.requestModel.OrderRequestModel
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.DialogPayByGuestBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TimeFormatUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
class PayByGuestDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: DialogPayByGuestBinding

    private val viewModel by activityViewModels<DineInOrderTableViewModel>()
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var isUpdate: Boolean = false
    private var tipAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var fourthValue: Double = 0.0
    private var thirdValue: Double = 0.0
    private var secondValue: Int = 0
    private var cartList: CartModel? = null
    private var splitValue: Int = -1
    private var totalPrice: Double = 0.0
    private var totaldiscount: Double = 0.0
    private val paymentViewModel by viewModels<PaymentViewModel>()
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var isTotalPayment: Boolean = false
    private var paymentAmount: Double = 0.0

    private var guestId: Int = 0
    private var guestRequestModel: GuestPaymentRequest? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private var isLastPayment: Boolean = false

    private var isSplitByNo: Boolean = false
    private var isSplitByAmount: Boolean = false
    private var splitAfterAmount: Double = 0.0

    private val TAG = "PayByGuestDialog"

    @Inject
    lateinit var prefProvider: PrefProvider


    companion object {
        fun newInstance() = PayByGuestDialog()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPayByGuestBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeShowProgress()
        navigateOnPaymentSuccess()
        wholePaymentObservor()
        if (arguments?.getBoolean("isTotalPayment") == true) {
            isTotalPayment = true
            orderId = arguments?.getInt("orderId")
            setTotalPaymentData()
            binding.txtCustom.setOnClickListener(this)
            binding.imgBack.setOnClickListener(this)
            binding.llCash.setOnClickListener(this)
            binding.txtOriginalAmount.setOnClickListener(this)
            binding.txtSecondAmount.setOnClickListener(this)
            binding.txtThirdAmount.setOnClickListener(this)
            binding.txtFourthAmount.setOnClickListener(this)
            binding.txtAddTips.setOnClickListener(this)

        } else {
            floorPlanModel = arguments?.getParcelable("floorPlan")
            orderId = arguments?.getInt("orderId")

            cartList = requireArguments().getParcelable("cartList")
            guestId = requireArguments().getInt("id")
            guestRequestModel = requireArguments().getParcelable("model")
            isLastPayment = requireArguments().getBoolean("isLastPayment")
            setupData()
        }

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            if (splitValue != -1) {

                isSplitByNo = true
                isSplitByAmount = false
                splitAfterAmount = (totalPrice + tipAmount) / splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            } else {
                isSplitByAmount = true
                isSplitByNo = false
                val splitValue = bundle.getDouble("splitByAmount")

                splitAfterAmount = splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            }
        }

    }

    private fun wholePaymentObservor() {
        paymentViewModel.msgText.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), it) { _, _ ->
                    if (isTotalPayment) {
                        findNavController().navigate(R.id.action_payByGuestDialog_to_dashboardCategoryNew)

                    } else {
                        val bundle = bundleOf("orderId" to orderId, "isGuestPaid" to true)

                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_dineInOrderTable,
                            bundle
                        )
                    }

                }

            }
        })

    }

    private fun setTotalPaymentData() {

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()
        getCashPaymentOptionList(totalPrice)
        MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        if (totaldiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totaldiscount
            )
        }


    }

    private fun setupData() {

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()

        isUpdate = requireArguments().getBoolean("update")
        if (isUpdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        getCashPaymentOptionList(totalPrice)

        MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        if (totaldiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totaldiscount
            )
        }


        // binding.txtSplitAmount.setOnClickListener(this)
        binding.txtCustom.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llCash.setOnClickListener(this)
        binding.txtOriginalAmount.setOnClickListener(this)
        binding.txtSecondAmount.setOnClickListener(this)
        binding.txtThirdAmount.setOnClickListener(this)
        binding.txtFourthAmount.setOnClickListener(this)
        binding.txtAddTips.setOnClickListener(this)
        binding.txtSplitAmount.setOnClickListener(this)


        if (cartList?.orderType == Constants.DINE_IN) {

        }
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice + tipAmount)


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                dialog?.dismiss()
                findNavController().popBackStack()
            }
            R.id.txtSplitAmount -> {

                val bundle = Bundle()
                bundle.putDouble("totalPrice", (totalPrice + tipAmount))
                bundle.putInt("splitValue", splitValue)
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_splitAmountFragment,
                    bundle
                )
            }

            R.id.txtOriginalAmount -> {
                if (isTotalPayment) {
                    makePayment(0.0)

                } else {

                    guestRequestModel?.let { viewModel.payByGuest(guestId, it, isLastPayment) }
                }

            }
            R.id.txtSecondAmount -> {
                if (isTotalPayment) {
                    makePayment(0.0)

                } else {
                    guestRequestModel?.let { viewModel.payByGuest(guestId, it, isLastPayment) }
                }
            }

            R.id.txtThirdAmount -> {
                if (isTotalPayment) {
                    makePayment(0.0)

                } else {
                    guestRequestModel?.let { viewModel.payByGuest(guestId, it, isLastPayment) }
                }
            }
            R.id.txtFourthAmount -> {
                if (isTotalPayment) {
                    makePayment(0.0)

                } else {
                    guestRequestModel?.let { viewModel.payByGuest(guestId, it, isLastPayment) }
                }
            }


        }

    }

    private fun makePayment(amount: Double) {
        Log.e(TAG, "amountForPayment   ${amount}")
        var requestModel = createRequestForTotalAmount()

        orderId?.let { paymentViewModel.dineInWholePayment(requestModel, it) }

    }

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    @SuppressLint("SetTextI18n")
    private fun getCashPaymentOptionList(totalPrice: Double) {
        Log.e(TAG, "totalPrice  $totalPrice")
        secondValue = floor(totalPrice + 1).toInt()
        Log.e(TAG, "secondValue  $secondValue")
        val newVal = totalPrice + 1
        thirdValue = calculateCashOption(newVal)
        Log.e(TAG, "thirdValuethirdValue:   ${thirdValue}")
        if (secondValue.toDouble() == thirdValue) {
            if (secondValue > 1000) {
                thirdValue += 100
            } else {
                thirdValue += 50
            }

        }
        fourthValue = calculateCashOption(thirdValue)
        if (thirdValue == fourthValue) {
            fourthValue += 100
        } else {
            fourthValue += 50
        }

        MethodUtils.setPriceTextView(binding.txtOriginalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSecondAmount, secondValue.toDouble())
        MethodUtils.setPriceTextView(binding.txtThirdAmount, thirdValue)
        MethodUtils.setPriceTextView(binding.txtFourthAmount, fourthValue)


    }

    private fun calculateCashOption(value: Double): Double {
        if (value > 1000) {
            return ceil(value / 100) * 100

        } else if (value > 500) {
            return ceil(value / 50) * 50
        } else {
            val arrAmount = arrayOf(
                5,
                10,
                20,
                50,
                100,
                110,
                120,
                150,
                200,
                210,
                220,
                250,
                300,
                310,
                320,
                350,
                400,
                410,
                420,
                450,
                500
            )
            val myValue = value.toInt()
            Log.e(TAG, "myValue:  ${myValue}")
            var searchIndex: Int = -1
            val filterValue = arrAmount.filter {
                it >= value
            }.first()
            searchIndex = arrAmount.indexOf(filterValue)
            Log.e(TAG, "filterValue:  ${filterValue}")
            Log.e(TAG, "searchIndex:  ${searchIndex}")

            /* arrAmount.forEachIndexed { index, i ->
                 if (i >= value) {
                      = i
                     return@forEachIndexed
                 }
             }*/

            if (arrAmount.contains(myValue)) {
                searchIndex += 1
            }

            if (searchIndex >= arrAmount.size) {
                return 550.0
            } else {
                val lastAmount = arrAmount[searchIndex]
                return lastAmount.toDouble()
            }


//            if (searchIndex > 0)
        }
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

        viewModel.msgText.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
            }
        })


    }

    private fun navigateOnPaymentSuccess() {
        viewModel.onPayment.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { str ->
                Log.e(TAG, "getstr:   $str")

                AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), str) { _, _ ->


                    if (isTotalPayment) {
                        findNavController().navigate(R.id.action_payByGuestDialog_to_dashboardCategoryNew)
                    } else {
                        val bundle = Bundle()
                        bundle.putBoolean("isGuestPaid", true)
                        bundle.putParcelable("floorPlan", floorPlanModel)
                        orderId?.let { bundle.putInt("orderId", it) }

                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_dineInOrderTable,
                            bundle
                        )

                    }
                    /* val navController = findNavController()
                     navController.previousBackStackEntry?.savedStateHandle?.set(
                         com.android.pos.data.remote.Constants.KEY,
                         Constants.GUESTPAID
                     )

                     navController.popBackStack()
 */
                }

            }
        })
    }

    private fun createRequestForTotalAmount(): OrderRequestModel {
        val orderModel = OrderAttributeRequestModel()

        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()

            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            note = ""
            openOrderType = "DineIn"
            orderTypeId = 2
            paymentStatus = 1
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            totalAmount =
                MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                    tipAmount
                )
            totalDiscount = totaldiscount
            totalServiceCharges = totalServiceCharge
            totalTaxAmount = totalTax
            totalTips = tipAmount

        }

        val model = OrderRequestModel(
            completed_all_payments = true,
            order = orderModel
        )

        return model


    }

}