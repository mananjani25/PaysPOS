package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.databinding.DialogIssueRefundBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.requestModel.RefundRequestModel
import com.android.pos.ui.adapter.RefundItemListAdapter
import com.android.pos.utils.AlertUtils
import kotlin.collections.ArrayList


@AndroidEntryPoint
class IssueRefundDialog : DialogFragment(), TextWatcher {

    private lateinit var refundData: RefundRequestModel
    private var totalServiceCharge: Double = 0.0
    private var refundAmount: Double = 0.0
    var totalTax = 0.0
    var totalItemPrice = 0.0
    private lateinit var binding: DialogIssueRefundBinding
    private lateinit var paymentOrderDetailsResponse: GetPaymentOrderDetailsResponse
    var isFromTrans = false
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private lateinit var refundItemListAdapter: RefundItemListAdapter
    private var isItem = false
    private var payment_id = 0
    private var subTotalPrice: Double = 0.0


    companion object {
        fun newInstance() = IssueRefundDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_issue_refund, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        paymentOrderDetailsResponse = arguments?.getParcelable("orderDetailsResponse")!!
        payment_id = arguments?.getInt("paymentId")!!
        binding.orderDetails = paymentOrderDetailsResponse
        binding.edtAmount.addTextChangedListener(this)

        setUpRecyclerView()


        binding.rgRefundType.setOnCheckedChangeListener { group, checkedId ->

            if (checkedId == R.id.rbItems) {
                isItem = false
                binding.llItemList.visibility = View.VISIBLE
                binding.llRefundAmount.visibility = View.GONE
                binding.tvRefundItemDetails.visibility = View.VISIBLE
                binding.tvRefundPaymentDetails.visibility = View.GONE
            } else if (checkedId == R.id.rbAmount) {
                isItem = true
                binding.llItemList.visibility = View.GONE
                binding.llRefundAmount.visibility = View.VISIBLE
                binding.tvRefundPaymentDetails.visibility = View.VISIBLE
                binding.tvRefundItemDetails.visibility = View.GONE

                val mData = paymentOrderDetailsResponse.data

                if (mData.order.refund_detail.refunded_amount.equals(0.0)) {

                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        (mData.sub_total + mData.tips + mData.tax_amount + mData.service_charge_amount - (mData.loyalty_amount!! + mData.cash_discount_or_surcharge + mData.total_discount))
                    )
                } else {
                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        ((mData.amount - mData.tips) - mData.order.refund_detail.refunded_amount)
                    )
                }

            }
        }

        binding.txtDone.setOnClickListener {
            if (isItem) {
                if (TextUtils.isEmpty(binding.edtAmount.text.toString())) {
                    AlertUtils.showCustomAlert(requireActivity(), "Please Enter Amount To Refund")
                } else {
                    subTotalPrice = binding.edtAmount.text.toString().toDouble()

                    refundData = RefundRequestModel().apply {
                        paymentRefund = RefundRequestModel.PaymentRefund().apply {
                            amount = subTotalPrice
                            orderId = paymentOrderDetailsResponse.data.order_id
                            paymentId = payment_id
                            employeeId = paymentOrderDetailsResponse.data.employee_id
                            terminalId = paymentOrderDetailsResponse.data.terminal_id
                            taxRefunded = totalTax
                            tipsRefunded = paymentOrderDetailsResponse.data.tips
                            serviceChargeRefunded = totalServiceCharge
                        }
                    }

                    val bundle = Bundle().apply {
                        putParcelable("refundData", refundData)
                        putDouble("refundAmount", subTotalPrice)
                    }
                    findNavController().navigate(
                        R.id.action_issueRefundFragment_to_reasonForRefundDialog,
                        bundle
                    )
                }
            } else {
                if (refundItemListAdapter.selectedItemList().size == 0) {
                    AlertUtils.showCustomAlert(requireActivity(), "Please Select Item To Refund")
                } else {
                    //  refundAmount = binding.edtAmount.text.toString().toDouble()
                    val bundle = Bundle().apply {
                        putParcelable("refundData", refundData)
                        putDouble("refundAmount", totalItemPrice)
                        Log.d("subTotalPriceRefund", "::$totalItemPrice")
                    }
                    findNavController().navigate(
                        R.id.action_issueRefundFragment_to_reasonForRefundDialog,
                        bundle
                    )
                }
            }

        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    private fun setUpRecyclerView() {
        refundItemListAdapter = RefundItemListAdapter(viewModel)
        binding.rvItemListRefund.adapter = refundItemListAdapter

        refundItemListAdapter.addItems(
            paymentOrderDetailsResponse.data.order.order_items,
            paymentOrderDetailsResponse.data.service_charge_amount,
            paymentOrderDetailsResponse.data.cash_discount_or_surcharge,
            paymentOrderDetailsResponse.data.cash_discount_type,
            paymentOrderDetailsResponse.data.payment_type
        )

        refundItemListAdapter.showItemSubTotal = {

            calculationOfItems()

        }

        /*refundData = RefundRequestModel().apply {
            paymentRefund = RefundRequestModel.PaymentRefund().apply {
                amount = subTotalPrice
                orderId = orderDetailsResponse.data.id
                paymentId = orderDetailsResponse.data.payments[0].id
                employeeId = orderDetailsResponse.data.employeeId
                terminalId = orderDetailsResponse.data.terminalId
                taxRefunded = totalTax
                serviceChargeRefunded = totalServiceCharge
            }
        }*/


    }

    private fun calculationOfItems() {

        totalServiceCharge = 0.0
        totalItemPrice = 0.0
        totalTax = 0.0
        val orderItemRefundsAttributesList =
            ArrayList<RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute>()

        //  totalItemPrice = 0.0
        refundItemListAdapter.selectedItemList().forEach { it ->
            subTotalPrice = 0.0
            if (it.isChecked) {
                subTotalPrice += it.totalPrice - it.discountAmount

                it.orderItemTaxes.forEach { tax ->
                    totalTax += tax.taxTotalAmount
                }

                it.orderItemModifiers.forEach { modifiers ->
                    subTotalPrice += (modifiers.price * modifiers.quantity)
                }

                totalServiceCharge = paymentOrderDetailsResponse.data.service_charge_amount
                totalItemPrice += subTotalPrice
            }
            val orderItemRefundsAttributeModel =
                RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute()

            orderItemRefundsAttributeModel.amount = subTotalPrice
            orderItemRefundsAttributeModel.employeeId = it.employeeId
            orderItemRefundsAttributeModel.orderId = it.orderId
            orderItemRefundsAttributeModel.refundType = 0
            orderItemRefundsAttributeModel.paymentId = payment_id
            orderItemRefundsAttributeModel.orderItemId = it.id
            orderItemRefundsAttributeModel.quantity = it.quantity
            orderItemRefundsAttributesList.add(orderItemRefundsAttributeModel)
        }
        totalItemPrice += totalTax + totalServiceCharge


        if (paymentOrderDetailsResponse.data.payment_type == "Cash") {
            if (paymentOrderDetailsResponse.data.cash_discount_type == "CashDiscount") {
                totalItemPrice -= paymentOrderDetailsResponse.data.cash_discount_or_surcharge
            }
        } else if (paymentOrderDetailsResponse.data.payment_type == "Card") {
            if (paymentOrderDetailsResponse.data.cash_discount_type == "SurCharge") {
                totalItemPrice += paymentOrderDetailsResponse.data.cash_discount_or_surcharge
            }
        }




        refundData = RefundRequestModel().apply {
            paymentRefund = RefundRequestModel.PaymentRefund().apply {
                amount = totalItemPrice
                orderId = paymentOrderDetailsResponse.data.order_id
                paymentId = payment_id
                employeeId = paymentOrderDetailsResponse.data.employee_id
                terminalId = paymentOrderDetailsResponse.data.terminal_id
                orderItemRefundsAttributes = orderItemRefundsAttributesList
                taxRefunded = totalTax
                serviceChargeRefunded = totalServiceCharge
                tipsRefunded = paymentOrderDetailsResponse.data.tips
            }
        }
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

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    var current = ""
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            binding.edtAmount.removeTextChangedListener(this)


            val cleanString: String = s!!.replace("""[$,.%]""".toRegex(), "")


            val parsed = cleanString.toDouble()

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted

            binding.edtAmount.setText(formatted.replace("""[$,%]""".toRegex(), ""))
            binding.edtAmount.setSelection(formatted.replace("""[$,%]""".toRegex(), "").length)




            if (paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                if (binding.edtAmount.text.toString()
                        .toDouble() > paymentOrderDetailsResponse.data.order.total_amount
                ) {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString((paymentOrderDetailsResponse.data.order.total_amount - paymentOrderDetailsResponse.data.order.total_tips).toDouble()))
                }
            } else {
                val newPrice =
                    (paymentOrderDetailsResponse.data.order.total_amount - paymentOrderDetailsResponse.data.order.total_tips) - paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount
                if (binding.edtAmount.text.toString().toDouble() > newPrice) {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(newPrice.toDouble()))
                }
            }


            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }
}