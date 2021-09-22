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
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.DialogIssueRefundBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*
import androidx.navigation.fragment.findNavController
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
    private lateinit var orderDetailsResponse: GetOrderDetailsResponse
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private lateinit var refundItemListAdapter: RefundItemListAdapter
    private var isItem = false
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

        orderDetailsResponse = arguments?.getParcelable("orderDetailsResponse")!!
        binding.orderDetails = orderDetailsResponse
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
                            orderId = orderDetailsResponse.data.id
                            paymentId = orderDetailsResponse.data.payments[0].id
                            employeeId = orderDetailsResponse.data.employeeId
                            terminalId = orderDetailsResponse.data.terminalId
                            taxRefunded = totalTax
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
                        putDouble("refundAmount", subTotalPrice)
                        Log.d("subTotalPriceRefund", "::" + subTotalPrice)
                    }
                    findNavController().navigate(
                        R.id.action_issueRefundFragment_to_reasonForRefundDialog,
                        bundle
                    )
                }
            }

        }

        return binding.root
    }

    private fun setUpRecyclerView() {
        refundItemListAdapter = RefundItemListAdapter(viewModel)
        binding.rvItemListRefund.adapter = refundItemListAdapter

        refundItemListAdapter.addItems(orderDetailsResponse.data.orderItems)

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
        subTotalPrice = 0.0
        totalServiceCharge = 0.0
        totalTax = 0.0
        val orderItemRefundsAttributesList =
            ArrayList<RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute>()

        //  totalItemPrice = 0.0
        refundItemListAdapter.selectedItemList().forEach { it ->
            if (it.isChecked) {

                it.orderItemTaxes.forEach { tax ->
                    totalTax += tax.taxTotalAmount
                }

                it.orderItemModifiers.forEach { modifiers ->
                    modifiers.orderItemTaxes.forEach { taxes ->
                        totalTax += taxes.taxTotalAmount
                    }

                }

                subTotalPrice += it.totalPrice

                it.orderItemModifiers.forEach { modifiers ->
                    subTotalPrice += (modifiers.price * modifiers.quantity)
                }
                orderDetailsResponse.data.orderServiceCharges.forEach {
                    totalServiceCharge = (subTotalPrice * it.rate) / 100
                }
            }
            val orderItemRefundsAttributeModel =
                RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute()

            orderItemRefundsAttributeModel.amount = subTotalPrice
            orderItemRefundsAttributeModel.employeeId = it.employeeId
            orderItemRefundsAttributeModel.orderId = it.orderId
            orderItemRefundsAttributeModel.refundType = 0
            orderItemRefundsAttributeModel.orderItemId = it.itemId
            orderItemRefundsAttributeModel.quantity = it.quantity
            orderItemRefundsAttributesList.add(orderItemRefundsAttributeModel)
        }
        subTotalPrice += totalTax + totalServiceCharge

        refundData = RefundRequestModel().apply {
            paymentRefund = RefundRequestModel.PaymentRefund().apply {
                amount = subTotalPrice
                orderId = orderDetailsResponse.data.id
                paymentId = orderDetailsResponse.data.payments[0].id
                employeeId = orderDetailsResponse.data.employeeId
                terminalId = orderDetailsResponse.data.terminalId
                orderItemRefundsAttributes = orderItemRefundsAttributesList
                taxRefunded = totalTax
                serviceChargeRefunded = totalServiceCharge
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




            if (binding.edtAmount.text.toString()
                    .toDouble() > orderDetailsResponse.data.totalAmount
            ) {
                binding.edtAmount.setText(MethodUtils.roundOffAmountString(orderDetailsResponse.data.totalAmount))
            }

            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }
}