package com.android.pos.ui.fragments.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.PaymentFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
open class PaymentFragment : Fragment(), View.OnClickListener {
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
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var paymentAmount: Double = 0.0
    private lateinit var binding: PaymentFragmentBinding
    private val TAG = "PaymentFragment"

    @Inject
    lateinit var prefProvider: PrefProvider


    companion object {
        fun newInstance() = PaymentFragment()
    }

    private val viewModel by viewModels<PaymentViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = viewModel

        cartList = requireArguments().getParcelable("cartList")

        setupData()
        callbackSetup()
        observeShowProgress()
        observeData()


        return binding.root
    }


    @SuppressLint("SetTextI18n")
    private fun callbackSetup() {

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            val splitAfterAmount = totalPrice / splitValue

            MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
            val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

            binding.txtSplitAmount.text = getString(R.string.edit_split_amount)

            binding.txtSplitValue.text =
                "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

        }

        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")


            if (tipAmount == 0.00) {
                binding.txtTotalAmount.text =
                    MethodUtils.roundOffAmount(totalPrice)
            } else {
                binding.txtTotalAmount.text =
                    MethodUtils.roundOffAmount(totalPrice + tipAmount) + " (" + MethodUtils.roundOffAmount(
                        tipAmount
                    ) + " Tip Added)"
            }


            getCashPaymentOptionList(totalPrice + tipAmount)
            MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
            MethodUtils.setPriceTextView(binding.txtTotal, totalPrice + tipAmount)
        }


    }

    private fun setupData() {

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
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
        if (totalDiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totalDiscount
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                findNavController().popBackStack()
            }

            R.id.txtOriginalAmount -> {

                paymentAmount = totalPrice + tipAmount
                makePayment()
            }
            R.id.txtSecondAmount -> {
                paymentAmount = secondValue.toDouble()
                makePayment()
            }
            R.id.txtThirdAmount -> {
                paymentAmount = thirdValue
                makePayment()
            }
            R.id.txtFourthAmount -> {
                paymentAmount = fourthValue
                makePayment()
            }


            R.id.llCash -> {

                makePayment()


            }
            R.id.txtCustom -> {
                findNavController().navigate(R.id.action_paymentFragment_to_customAmountFragment)
            }
            R.id.txtSplitAmount -> {

                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice)
                bundle.putInt("splitValue", splitValue)
                findNavController().navigate(
                    R.id.action_paymentFragment_to_splitAmountFragment,
                    bundle
                )
            }

            R.id.txtAddTips -> {
                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice)
                bundle.putDouble("totalTip", tipAmount)
                findNavController().navigate(
                    R.id.action_paymentFragment_to_addTipsDialog,
                    bundle
                )
            }
        }

    }

    private fun makePayment() {

        if (isUpdate)
            viewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )

        val myRequest = cartList?.let {

            viewModel.createOrderRequest(
                it,
                subTotalPrice,
                totalPrice + tipAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                future_delivery_date,
                future_delivery_date,
                true,
                totalDiscount,
                tipAmount
            )
        }
        if (myRequest != null) {
            viewModel.totalPayAmount(paymentAmount)
            viewModel.submit(myRequest)
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

    }


    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice + tipAmount)
                bundle.putDouble("paymentAmount", paymentAmount)
                bundle.putInt("orderID", it.data.order.id)
                bundle.putParcelable("receiptData", it.data)
                findNavController().navigate(
                    R.id.action_paymentFragment_to_orderCompleteFragment,
                    bundle
                )

                it.data.order

            }
        })

    }
}