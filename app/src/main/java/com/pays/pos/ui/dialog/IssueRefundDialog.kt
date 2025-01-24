package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.RadioButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.GetPaymentOrderDetailsResponse
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_RATE
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.OPEN_ORDER
import com.pays.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.databinding.DialogIssueRefundBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.RefundItemListAdapter
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import okhttp3.internal.toImmutableList
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs


@AndroidEntryPoint
class IssueRefundDialog : DialogFragment(), TextWatcher {

    private var loyaltyAmount: Double = 0.0
    private var orderDiscount: Double = 0.0
    private var cashdiscountdiv: Double = 0.0
    private lateinit var refundData: RefundRequestModel
    private var totalServiceCharge: Double = 0.0
    private var refundAmount: Double = 0.0
    var totalTax = 0.0
    var totalItemPrice = 0.0
    private lateinit var binding: DialogIssueRefundBinding
    private lateinit var paymentOrderDetailsResponse: GetPaymentOrderDetailsResponse
    var isFromTrans = false
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private val transactionViewModel by activityViewModels<TransactionDetailsViewModel>()
    private lateinit var refundItemListAdapter: RefundItemListAdapter
    private var isItem = false
    private var payment_id = 0
    private var subTotalPrice: Double = 0.0
    lateinit var prefProvider: PrefProvider
    private var serviceChargesList: List<TbServiceCharge>? = arrayListOf()
    private var isSplitPayment = false
    private var requiredNABServerPostAPICall = false
    private var paxData = ""
    private var guestCount: Int = 0

    private var screenTotalAmount = ""

    //    this variable is added because the loyalty deduction was causing price deterioration, hence we are adding the selected items price and passing to next screen. The selected addition is stored in below variable
    private var totalCalculatedFromSelected = 0.0

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    var isAmountRefund: Boolean = false
    var isItemRefund: Boolean = false

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

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        paymentOrderDetailsResponse = arguments?.getParcelable("orderDetailsResponse")!!
        payment_id = arguments?.getInt("paymentId")!!
        isSplitPayment = arguments?.getBoolean("isSplitPayment")!!
        requiredNABServerPostAPICall = arguments?.getBoolean("requiredNABServerPostAPICall")!!
        paxData = arguments?.getString("pax_data") + ""
        screenTotalAmount = arguments?.getString("screenTotalAmount") + ""
        serviceChargesList = arguments?.getParcelableArrayList("serviceChargesList")!!
        guestCount = arguments?.getInt("guestCount") ?: 0
        binding.orderDetails = paymentOrderDetailsResponse
        binding.edtAmount.addTextChangedListener(this)
        prefProvider = PrefProvider(requireContext())

        isAmountRefund = arguments?.getBoolean("isAmountRefund")!!
        isItemRefund = arguments?.getBoolean("isItemRefund")!!


        if (isAmountRefund) {
            binding.apply {
                rbItems.performClick()
                rbAmount.performClick()
                rbItems.visibility = View.GONE
                rbAmount.visibility = View.VISIBLE


                llRefundAmount.visibility = View.VISIBLE
                llItemList.visibility = View.GONE


                isItem = true
                binding.llItemList.visibility = View.GONE
                binding.llRefundAmount.visibility = View.VISIBLE
                binding.tvRefundPaymentDetails.visibility = View.VISIBLE
                binding.tvRefundItemDetails.visibility = View.GONE
                binding.rbItems.background =
                    requireActivity().getDrawable(R.drawable.background_square_border_grey)
                binding.rbAmount.background =
                    requireActivity().getDrawable(R.drawable.btn_background_secondary)
                binding.rbAmount.setTextColor(requireActivity().resources.getColor(R.color.white))
                binding.rbItems.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
                val mData = paymentOrderDetailsResponse.data

                if (mData.order.refund_detail.refunded_amount.equals(0.0)) {

                    if (mData.payment_type == "Card") {
                        var totalamount_tip = mData.amount + tipCalculation(mData.tips)
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            (totalamount_tip)
                        )
                        Log.d("edtAmount: ", "edtAmount " + (totalamount_tip * 100).toString())
                        binding.edtAmount.setText((totalamount_tip * 100).toString())
                    } else {
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            (mData.amount)
                        )

                        binding.edtAmount.setText((mData.amount * 100).toString())
                    }
                } else {
                    if (mData.payment_type == "Card") {
                        val price =
                            (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            price
                        )

                        binding.edtAmount.setText(price.toString())
                        //binding.edtAmount.setText((price * 100).toString())
                    } else {
                        val price =
                            (mData.amount) - mData.order.refund_detail.refunded_amount
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            price
                        )

                        binding.edtAmount.setText((price * 100).toString())
                    }
                }

//                var alreadyRefundedAmount = paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount
//                var allTotalAmount = (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
//
//                val subTotalPriceNew = allTotalAmount - alreadyRefundedAmount
//                binding.edtAmount.setText(allTotalAmount.toString())


            }
        } else {
            if (isItemRefund) {
                binding.apply {
                    rbItems.performClick()
                    rbItems.visibility = View.VISIBLE
                    rbAmount.visibility = View.GONE
                    rgRefundType.check(R.id.rbItems)

                    llRefundAmount.visibility = View.GONE
                    llItemList.visibility = View.VISIBLE
                }
            }
        }


        if (!isSplitPayment) {
            setUpRecyclerView()
        } else {
            isItem = true
            binding.rbAmount.isChecked = true
            binding.rbItems.visibility = View.GONE
            binding.llItemList.visibility = View.GONE
            binding.llRefundAmount.visibility = View.VISIBLE
            binding.tvRefundPaymentDetails.visibility = View.VISIBLE
            binding.tvRefundItemDetails.visibility = View.GONE

            val mData = paymentOrderDetailsResponse.data

            if (mData.order.refund_detail.refunded_amount.equals(0.0)) {

                if (mData.payment_type == "Card") {
                    val totalamount_tip = mData.amount + tipCalculation(mData.tips)
                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        (totalamount_tip)
                    )
                    binding.edtAmount.setText((totalamount_tip * 100).toString())
                } else {
                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        (mData.amount)
                    )

                    binding.edtAmount.setText((mData.amount * 100).toString())
                }
            } else {
                if (mData.payment_type == "Card") {
                    val price =
                        (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        price
                    )

                    binding.edtAmount.setText(price.toString())
                    //binding.edtAmount.setText((price * 100).toString())
                } else {
                    val price =
                        (mData.amount) - mData.order.refund_detail.refunded_amount
                    MethodUtils.setRefundPriceTextView(
                        binding.tvTotalRefundAmount,
                        price
                    )

                    binding.edtAmount.setText((price * 100).toString())
                }

            }

        }

        binding.rgRefundType.setOnCheckedChangeListener { group, checkedId ->

            if (checkedId == R.id.rbItems) {
                isItem = false
                binding.llItemList.visibility = View.VISIBLE
                binding.llRefundAmount.visibility = View.GONE
                binding.tvRefundItemDetails.visibility = View.VISIBLE
                binding.tvRefundPaymentDetails.visibility = View.GONE
                binding.rbItems.background =
                    requireActivity().getDrawable(R.drawable.btn_background_secondary)
                binding.rbAmount.background =
                    requireActivity().getDrawable(R.drawable.background_square_border_grey)

                binding.rbItems.setTextColor(requireActivity().resources.getColor(R.color.white))
                binding.rbAmount.setTextColor(requireActivity().resources.getColor(R.color.txtColor))

            } else if (checkedId == R.id.rbAmount) {
                isItem = true
                binding.llItemList.visibility = View.GONE
                binding.llRefundAmount.visibility = View.VISIBLE
                binding.tvRefundPaymentDetails.visibility = View.VISIBLE
                binding.tvRefundItemDetails.visibility = View.GONE
                binding.rbItems.background =
                    requireActivity().getDrawable(R.drawable.background_square_border_grey)
                binding.rbAmount.background =
                    requireActivity().getDrawable(R.drawable.btn_background_secondary)
                binding.rbAmount.setTextColor(requireActivity().resources.getColor(R.color.white))
                binding.rbItems.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
                val mData = paymentOrderDetailsResponse.data

                if (mData.order.refund_detail.refunded_amount.equals(0.0)) {

                    if (mData.payment_type == "Card") {
                        var totalamount_tip = mData.amount + tipCalculation(mData.tips)
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            (totalamount_tip)
                        )
                        Log.d("edtAmount: ", "edtAmount " + (totalamount_tip * 100).toString())
                        binding.edtAmount.setText((totalamount_tip * 100).toString())
                    } else {
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            (mData.amount)
                        )

                        binding.edtAmount.setText((mData.amount * 100).toString())
                    }
                } else {
                    if (mData.payment_type == "Card") {
                        val price =
                            (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            price
                        )


                        binding.edtAmount.setText(price.toString())
                        //binding.edtAmount.setText((price * 100).toString())
                    } else {
                        val price =
                            (mData.amount) - mData.order.refund_detail.refunded_amount
                        MethodUtils.setRefundPriceTextView(
                            binding.tvTotalRefundAmount,
                            price
                        )

                        binding.edtAmount.setText((price * 100).toString())
                    }
                }

//                var alreadyRefundedAmount = paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount
//                var allTotalAmount = (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
//
//                val subTotalPriceNew = allTotalAmount - alreadyRefundedAmount
//                binding.edtAmount.setText(allTotalAmount.toString())

//                var alreadyRefundedAmount = paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount
//                var allTotalAmount = paymentOrderDetailsResponse.data.order.total_amount
//
//                val subTotalPriceNew = allTotalAmount - alreadyRefundedAmount
//                binding.edtAmount.setText(subTotalPriceNew.toString())


            }
        }

        binding.txtDone.setOnClickListener {
            startRefundProcess()
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    private fun startRefundProcess() {
        if (isItem) {
            if (TextUtils.isEmpty(binding.edtAmount.text.toString())) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    getString(R.string.msg_amount_refund)
                )
            } else {

                //  val totalAmountRefund = binding.edtAmount.text.toString().toDouble()

                refundData = RefundRequestModel().apply {
                    paymentRefund = RefundRequestModel.PaymentRefund().apply {
                        amount = subTotalPrice
                        orderId = paymentOrderDetailsResponse.data.order_id
                        paymentId = payment_id
                        employeeId = paymentOrderDetailsResponse.data.employee_id
                        terminalId = paymentOrderDetailsResponse.data.terminal_id
                        taxRefunded = paymentOrderDetailsResponse.data.tax_amount
                        tipsRefunded =
                            if (paymentOrderDetailsResponse.data.payment_type == "Cash") 0.0 else paymentOrderDetailsResponse.data.tips
                        serviceChargeRefunded =
                            paymentOrderDetailsResponse.data.service_charge_amount
                        cash_discount_or_surcharge_refunded =
                            paymentOrderDetailsResponse.data.cash_discount_or_surcharge
                        subtotal_refunded = paymentOrderDetailsResponse.data.sub_total
                    }
                }


                transactionViewModel.orderItemAttribututes = null

                val refundAmount = binding.edtAmount.text.toString().toDouble()

                val bundle = Bundle().apply {
                    putParcelable("refundData", refundData)
                    putDouble("refundAmount", refundAmount)
                    putString("pax_ref_num", paymentOrderDetailsResponse.data.ref_num)

                    putString(
                        "pax_ecrref_num",
                        paymentOrderDetailsResponse.data.ecr_ref_num
                    )
                    putString(
                        "pax_token",
                        paymentOrderDetailsResponse.data.pax_transaction_token
                    )
                    putString("pax_ext_data", paymentOrderDetailsResponse.data.ext_data)
                    putString("paymentType", paymentOrderDetailsResponse.data.payment_type)
                    putString(
                        "magensa_response_data",
                        paymentOrderDetailsResponse.data.magensa_response_data
                    )

                    if (paymentOrderDetailsResponse.data.order.order_type == "OnlineOrder" && paymentOrderDetailsResponse.data.pax_data != null) {
                        /*Online order refund should pass a new parameter so that the next screen will detect the parameter and process the operation accordingly, because there are two processes
                        * 1. PAX Gateway refund
                        * 2. NAB Server POST API Call */
                        putString("pax_data", paxData)
                        putBoolean(
                            "requiredNABServerPostAPICall",
                            requiredNABServerPostAPICall
                        )
                    }

                }
                if (findNavController().currentDestination?.id == R.id.issueRefundFragment) {
                    findNavController().navigate(
                        R.id.action_issueRefundFragment_to_reasonForRefundDialog,
                        bundle
                    )
                }
            }
        } else {

            if (!checkIfSelectedItems()) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    "Please Select Item To Refund"
                )
            } else {

                val ordersItemList =
                    mutableListOf<RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute>()


                refundData = RefundRequestModel().apply {
                    paymentRefund = RefundRequestModel.PaymentRefund().apply {
                        refundItemListAdapter.selectedItemList().forEach { it ->

                            amount = subTotalPrice
                            orderId = paymentOrderDetailsResponse.data.order_id
                            paymentId = payment_id
                            employeeId = paymentOrderDetailsResponse.data.employee_id
                            terminalId = paymentOrderDetailsResponse.data.terminal_id
                            taxRefunded = paymentOrderDetailsResponse.data.tax_amount
                            tipsRefunded =
                                if (paymentOrderDetailsResponse.data.payment_type == "Cash") 0.0 else paymentOrderDetailsResponse.data.tips
                            serviceChargeRefunded =
                                paymentOrderDetailsResponse.data.service_charge_amount
                            cash_discount_or_surcharge_refunded =
                                paymentOrderDetailsResponse.data.cash_discount_or_surcharge
                            subtotal_refunded = paymentOrderDetailsResponse.data.sub_total

                            if (it.isChecked) {

                                val order =
                                    RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute()
                                        .apply {

                                            orderId = it.orderId
                                            orderItemId = it.id
                                            paymentId = payment_id
                                            amount = it.totalPrice
                                            quantity = it.quantity
                                            refundType = 0
                                            employeeId =
                                                prefProvider.getValueInt(
                                                    Constants.EMPLOYEE_ID,
                                                    0
                                                )
                                        }

                                ordersItemList.add(order)

                                return@forEach
                            }
                        }
                    }
                }
                refundData.paymentRefund?.orderItemRefundsAttributes = ordersItemList
                transactionViewModel.orderItemAttribututes = ordersItemList


                calculationOfItems()
                if (paymentOrderDetailsResponse.data.is_loyalty_applied ?: false) {
                    var count = 0
                    refundItemListAdapter.selectedItemList().forEach {
                        if (it.isChecked)
                            count++
                    }

                    if (count == refundItemListAdapter.selectedItemList().size) {
                        totalItemPrice = screenTotalAmount.toDouble()
                    }
                }
                var totalCheckedItemPrice = 0.0
                refundItemListAdapter.selectedItemList().forEach {
                    if (it.isChecked)
                        totalCheckedItemPrice += it.deductedPrice
                }

                val refundedAmount = paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount

                if ((totalCheckedItemPrice + refundedAmount) > screenTotalAmount.toDouble()) {
                    AlertUtils.showCustomAlert(
                        requireActivity(),
                        getString(R.string.the_refund_amount_cannot_exceed_the_total_order_value)
                    )
                } else {
                    val bundle = Bundle().apply {
                        putParcelable("refundData", refundData)
                        //putString("orderItemRefundsAttributes", Gson().toJson(ordersItemList))
                        putDouble("refundAmount", totalCheckedItemPrice)
                        putString("pax_ref_num", paymentOrderDetailsResponse.data.ref_num)
                        putString(
                            "pax_ecrref_num",
                            paymentOrderDetailsResponse.data.ecr_ref_num
                        )
                        putString(
                            "pax_token",
                            paymentOrderDetailsResponse.data.pax_transaction_token
                        )
                        putString("pax_ext_data", paymentOrderDetailsResponse.data.ext_data)
                        putString("paymentType", paymentOrderDetailsResponse.data.payment_type)
                        putString(
                            "magensa_response_data",
                            paymentOrderDetailsResponse.data.magensa_response_data
                        )
                        Log.d("subTotalPriceRefund", "::$totalItemPrice")


                        if (paymentOrderDetailsResponse.data.order.order_type == "OnlineOrder" && paymentOrderDetailsResponse.data.pax_data != null) {
                            /*Online order refund should pass a new parameter so that the next screen will detect the parameter and process the operation accordingly, because there are two processes
                            * 1. PAX Gateway refund
                            * 2. NAB Server POST API Call */
                            putString("pax_data", paxData)
                            putBoolean(
                                "requiredNABServerPostAPICall",
                                requiredNABServerPostAPICall
                            )
                        }
                    }



                    if (findNavController().currentDestination?.id == R.id.issueRefundFragment) {
                        findNavController().navigate(
                            R.id.action_issueRefundFragment_to_reasonForRefundDialog,
                            bundle
                        )
                    }
                }
            }
        }
    }

    fun checkIfSelectedItems(): Boolean {
        var found: Boolean = false
        refundItemListAdapter.selectedItemList().forEach { it ->
            if (it.isChecked) {
                found = true
                return@forEach
            }
        }
        return found
    }


    fun setUpAmountTab() {
        binding.rbItems.gone()
        binding.rvItemListRefund.gone()
        isItem = true
        binding.llItemList.visibility = View.GONE
        binding.llRefundAmount.visibility = View.VISIBLE
        binding.tvRefundPaymentDetails.visibility = View.VISIBLE
        binding.tvRefundItemDetails.visibility = View.GONE
        binding.rbItems.background =
            requireActivity().getDrawable(R.drawable.background_square_border_grey)
        binding.rbAmount.background =
            requireActivity().getDrawable(R.drawable.btn_background_secondary)
        binding.rbAmount.setTextColor(requireActivity().resources.getColor(R.color.white))
        binding.rbItems.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
        val mData = paymentOrderDetailsResponse.data

        if (mData.order.refund_detail.refunded_amount.equals(0.0)) {

            if (mData.payment_type == "Card") {
                var totalamount_tip = mData.amount + tipCalculation(mData.tips)
                MethodUtils.setRefundPriceTextView(
                    binding.tvTotalRefundAmount,
                    (totalamount_tip)
                )
                Log.d("edtAmount: ", "edtAmount " + (totalamount_tip * 100).toString())
                binding.edtAmount.setText((totalamount_tip * 100).toString())
            } else {
                MethodUtils.setRefundPriceTextView(
                    binding.tvTotalRefundAmount,
                    (mData.amount)
                )

                binding.edtAmount.setText((mData.amount * 100).toString())
            }
        } else {
            if (mData.payment_type == "Card") {
                val price =
                    (mData.amount + tipCalculation(mData.tips)) - mData.order.refund_detail.refunded_amount
                MethodUtils.setRefundPriceTextView(
                    binding.tvTotalRefundAmount,
                    price
                )


                binding.edtAmount.setText(price.toString())
                //binding.edtAmount.setText((price * 100).toString())
            } else {
                val price =
                    (mData.amount) - mData.order.refund_detail.refunded_amount
                MethodUtils.setRefundPriceTextView(
                    binding.tvTotalRefundAmount,
                    price
                )

                binding.edtAmount.setText((price * 100).toString())
            }
        }

    }


    private fun setUpRecyclerView() {
        refundItemListAdapter = RefundItemListAdapter(viewModel)
        binding.rvItemListRefund.adapter = refundItemListAdapter

        var totalItemDiscount = 0.0

        paymentOrderDetailsResponse.data.order.order_items.forEach {
            totalItemDiscount += it.discountAmount
        }

        if (paymentOrderDetailsResponse.data.loyalty_amount == 0.0) {

            if (paymentOrderDetailsResponse.data.order.order_type == DINE_IN) {


                if (paymentOrderDetailsResponse.data.payable_type == "Guest") {

                    binding.rbItems.visible()
                    binding.rvItemListRefund.visible()

                    var orderItems = mutableListOf<GetOrderDetailsResponse.Data.OrderItem>()

                    val it = paymentOrderDetailsResponse

                    //  if (it.data.payable_type == "Guest") {

                    it.data.order.order_items.forEach { item ->
                        if (item.guestIndexForDineIn == it.data.guest_index_for_dine_in && item.guestIndexForDineIn != 0) {
                            orderItems.add(item)
                        }
                    }
                    //     }

//                    else {
//                        orderItems = it.data.order.order_items.toMutableList()
//                    }


                    refundItemListAdapter.addItems(
                        (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount),
                        orderItems,
                        serviceChargesList,
                        paymentOrderDetailsResponse.data.cash_discount_or_surcharge,
                        if (paymentOrderDetailsResponse.data.cash_discount_type != null) paymentOrderDetailsResponse.data.cash_discount_type else "",
                        paymentOrderDetailsResponse.data.payment_type,
                        if (paymentOrderDetailsResponse.data.total_discount > totalItemDiscount) paymentOrderDetailsResponse.data.total_discount - totalItemDiscount else 0.0,
                        paymentOrderDetailsResponse.data.loyalty_amount,
                        paymentOrderDetailsResponse.data.tips,
                        paymentOrderDetailsResponse.data.order.order_type
                    )
                    refundItemListAdapter.setSelectedItemList(
                        ArrayList(orderItems),
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, ""),
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_RATE, "")
                    )
                } else {
                   setUpAmountTab()
                }
            } else {
                if (isAmountRefund) {
                    binding.rbItems.gone()
                    binding.rvItemListRefund.gone()
                }else if(isItemRefund){
                    binding.rbItems.visible()
                    binding.rvItemListRefund.visible()
                }else{
                    binding.rbItems.visible()
                    binding.rvItemListRefund.visible()
                }
                refundItemListAdapter.addItems(
                    (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount),
                    paymentOrderDetailsResponse.data.order.order_items,
                    serviceChargesList,
                    paymentOrderDetailsResponse.data.cash_discount_or_surcharge,
                    if (paymentOrderDetailsResponse.data.cash_discount_type != null) paymentOrderDetailsResponse.data.cash_discount_type else "",
                    paymentOrderDetailsResponse.data.payment_type,
                    if (paymentOrderDetailsResponse.data.total_discount > totalItemDiscount) paymentOrderDetailsResponse.data.total_discount - totalItemDiscount else 0.0,
                    paymentOrderDetailsResponse.data.loyalty_amount,
                    paymentOrderDetailsResponse.data.tips,
                    paymentOrderDetailsResponse.data.order.order_type
                )

                refundItemListAdapter.setSelectedItemList(
                    paymentOrderDetailsResponse.data.order.order_items.toCollection(
                        arrayListOf()
                    ),
                    prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, ""),
                    prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_RATE, "")
                )
            }
        } else {

            if (paymentOrderDetailsResponse.data.order.order_type == DINE_IN) {


                if (paymentOrderDetailsResponse.data.payable_type == "Guest") {

                    var orderItems = mutableListOf<GetOrderDetailsResponse.Data.OrderItem>()

                    val it = paymentOrderDetailsResponse

                   // if (it.data.payable_type == "Guest") {

                        it.data.order.order_items.forEach { item ->
                            if (item.guestIndexForDineIn == it.data.guest_index_for_dine_in && item.guestIndexForDineIn != 0) {
                                orderItems.add(item)
                            }
                        }
//                    } else {
//                        orderItems = it.data.order.order_items.toMutableList()
//                    }


                    refundItemListAdapter.addItems(
                        (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount),
                        orderItems,
                        serviceChargesList,
                        paymentOrderDetailsResponse.data.cash_discount_or_surcharge,
                        if (paymentOrderDetailsResponse.data.cash_discount_type != null) paymentOrderDetailsResponse.data.cash_discount_type else "",
                        paymentOrderDetailsResponse.data.payment_type,
                        if (paymentOrderDetailsResponse.data.total_discount > totalItemDiscount) paymentOrderDetailsResponse.data.total_discount - totalItemDiscount else 0.0,
                        paymentOrderDetailsResponse.data.loyalty_amount,
                        paymentOrderDetailsResponse.data.tips,
                        paymentOrderDetailsResponse.data.order.order_type
                    )

                    refundItemListAdapter.setSelectedItemList(
                       ArrayList(orderItems),
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, ""),
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_RATE, "")
                    )


                } else {
                    setUpAmountTab()
                }
            } else {

                refundItemListAdapter.addItems(
//    IF GETTING MORE PROBLEMS, THEN UNCOMMENT IT AND REMOVE THE IMMEDIATE BELOW LINE        (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount),
                    (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount + paymentOrderDetailsResponse.data.cash_discount_or_surcharge),
                    paymentOrderDetailsResponse.data.order.order_items,
                    serviceChargesList,
                    paymentOrderDetailsResponse.data.cash_discount_or_surcharge,
                    if (paymentOrderDetailsResponse.data.cash_discount_type != null) paymentOrderDetailsResponse.data.cash_discount_type else "",
                    paymentOrderDetailsResponse.data.payment_type,
                    if (paymentOrderDetailsResponse.data.total_discount > totalItemDiscount) paymentOrderDetailsResponse.data.total_discount - totalItemDiscount else 0.0,
                    paymentOrderDetailsResponse.data.loyalty_amount,
                    paymentOrderDetailsResponse.data.tips,
                    paymentOrderDetailsResponse.data.order.order_type
                )

                refundItemListAdapter.setSelectedItemList(
                    paymentOrderDetailsResponse.data.order.order_items.toCollection(
                        arrayListOf()
                    ),
                    prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, ""),
                    prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_RATE, "")
                )
            }
        }


//        refundItemListAdapter.setSelectedItemList(
//            paymentOrderDetailsResponse.data.order.order_items.toCollection(
//                arrayListOf()
//            ),
//            prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE, ""),
//            prefProvider.getValue(CASH_DISCOUNT_SURCHARGE_RATE, "")
//        )
        refundItemListAdapter.showItemSubTotal = {

            //    calculationOfItems()

        }

    }

    private fun calculationOfItems() {

        totalServiceCharge = 0.0
        totalItemPrice = 0.0
        totalTax = 0.0
        orderDiscount = 0.0
        loyaltyAmount = 0.0
        cashdiscountdiv = 0.0
        var subtotal_divid = 0.0
        val orderItemRefundsAttributesList =
            ArrayList<RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute>()
        var selectedOrderDiscountDivided = 0.0
        var selectedLoyaltyPointDivided = 0.0
        var selectedCashDiscountDivided = 0.0
        var selectedTipDivided = 0.0
        var totalItemDiscount = 0.0
        paymentOrderDetailsResponse.data.order.order_items.forEach {
            totalItemDiscount += it.discountAmount
        }
        orderDiscount = paymentOrderDetailsResponse.data.total_discount - totalItemDiscount


        refundItemListAdapter.selectedItemList().forEach { orderItemselected ->
            if (orderItemselected.isChecked) {
                totalItemPrice +=
                    (orderItemselected.price * orderItemselected.quantity) - orderItemselected.discountAmount

                orderItemselected.orderItemModifiers.forEach { modifiers ->
                    totalItemPrice += (modifiers.price * modifiers.quantity)
                }


                var itemTotalTax = 0.0
                var itemServiceCharge = 0.0
                orderItemselected.orderItemTaxes.forEach { tax ->
                    itemTotalTax += if (tax.taxType == "Percentage") {
                        var modifierPrice = 0.0
                        val price =
                            (orderItemselected.price * orderItemselected.quantity) - orderItemselected.discountAmount

                        orderItemselected.orderItemModifiers.forEach { modifiers ->
                            modifierPrice += (modifiers.price * modifiers.quantity)
                        }

                        val totalPrice = price + modifierPrice
                        if (totalPrice < 0.0) {

                            String.format("%.2f", 0.00)
                                .toDouble()
                        } else {
                            val itemTaxPrice =
                                (tax.rate * totalPrice) / 100
                            Log.e("itemTaxPrice", "" + itemTaxPrice)

                            String.format("%.2f", itemTaxPrice)
                                .toDouble()

                        }

                    } else {
                        var modifierPrice = 0.0
                        val price =
                            (orderItemselected.price * orderItemselected.quantity) - orderItemselected.discountAmount

                        orderItemselected.orderItemModifiers.forEach { modifiers ->
                            (modifiers.price * modifiers.quantity)
                        }
                        val totalPrice = price + modifierPrice
                        Log.d("yash", "taxCalculation: " + tax.taxType)
                        if (totalPrice <= 0.0) {
                            String.format("%.2f", 0.00)
                                .toDouble()
                        } else {
                            String.format("%.2f", tax.rate * orderItemselected.quantity)
                                .toDouble()
                        }
                    }
                }
                totalTax += itemTotalTax


                var temp_totalPrice = 0.0
                temp_totalPrice += (orderItemselected.price * orderItemselected.quantity) - orderItemselected.discountAmount
                orderItemselected.orderItemModifiers.forEach { modifiers ->
                    temp_totalPrice += (modifiers.price * modifiers.quantity)
                }


                // order Discount Divide calculation

                selectedOrderDiscountDivided += (temp_totalPrice * orderDiscount) / (paymentOrderDetailsResponse.data.sub_total + orderDiscount)
                val nfone: NumberFormat = NumberFormat.getNumberInstance()
                nfone.maximumFractionDigits = 3
                val rounded1: String = nfone.format(selectedOrderDiscountDivided)
                selectedOrderDiscountDivided = rounded1.toDouble()

                var itemwiseOrderDiscount = 0.0
                refundItemListAdapter.selectedItemList().forEach { orderItems ->
                    if (orderItems.isChecked) {
                        itemwiseOrderDiscount =
                            (temp_totalPrice * orderDiscount) / (paymentOrderDetailsResponse.data.sub_total + orderDiscount)
                    }
                }
                //                item serviceCharge
                if (serviceChargesList?.isNotEmpty() == true) {
                    serviceChargesList?.forEach {
                        itemServiceCharge += ((temp_totalPrice - itemwiseOrderDiscount) * it.percentage) / 100
                    }
                }
                totalServiceCharge += itemServiceCharge


                // loyalty point
                var item_total_price_included = 0.0
                item_total_price_included =
                    (temp_totalPrice + itemServiceCharge + itemTotalTax) - itemwiseOrderDiscount
                if (paymentOrderDetailsResponse.data.loyalty_amount!! > 0) {
                    selectedLoyaltyPointDivided += (paymentOrderDetailsResponse.data.loyalty_amount!! * item_total_price_included) / (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount + paymentOrderDetailsResponse.data.loyalty_amount!!)
                }
                if (paymentOrderDetailsResponse.data.tips > 0) {
                    if (item_total_price_included == 0.0) {
                        if ((paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount) == 0.0) {
                            selectedTipDivided =
                                paymentOrderDetailsResponse.data.tips / paymentOrderDetailsResponse.data.order.order_items.size
                        } else {
                            selectedTipDivided += (paymentOrderDetailsResponse.data.tips * item_total_price_included) / (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount)
                        }
                    } else {
                        selectedTipDivided += (paymentOrderDetailsResponse.data.tips * item_total_price_included) / (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount)
                    }
                }
                if (paymentOrderDetailsResponse.data.cash_discount_or_surcharge > 0) {
                    selectedCashDiscountDivided += (paymentOrderDetailsResponse.data.cash_discount_or_surcharge * item_total_price_included) / (paymentOrderDetailsResponse.data.sub_total + paymentOrderDetailsResponse.data.service_charge_amount + paymentOrderDetailsResponse.data.tax_amount)
                }

            }

        }
        totalItemPrice += (String.format("%.2f", totalServiceCharge)
            .toDouble() + String.format("%.2f", totalTax)
            .toDouble())
        val nf6: NumberFormat = NumberFormat.getNumberInstance()
        nf6.maximumFractionDigits = 2
        val rounded6: String = nf6.format(selectedOrderDiscountDivided)
        selectedOrderDiscountDivided = rounded6.toDouble()


        if (totalItemPrice >= selectedOrderDiscountDivided) {
            totalItemPrice -= selectedOrderDiscountDivided
        }
        val nf1: NumberFormat = NumberFormat.getNumberInstance()
        nf1.maximumFractionDigits = 2
        val rounded: String = nf1.format(selectedTipDivided)
        selectedTipDivided = rounded.replace(",", "").toDouble()

        /*if (paymentOrderDetailsResponse.data.payment_type == "Card") {
            if (totalItemPrice >= selectedTipDivided) {
                totalItemPrice += selectedTipDivided
            } else if (totalItemPrice == 0.0) {
                totalItemPrice += paymentOrderDetailsResponse.data.tips / paymentOrderDetailsResponse.data.order.order_items.size
            } else {
                totalItemPrice += paymentOrderDetailsResponse.data.tips
            }
        }*/
        val nf3: NumberFormat = NumberFormat.getNumberInstance()
        nf3.maximumFractionDigits = 2
        val rounded3: String = nf3.format(selectedLoyaltyPointDivided)
        selectedLoyaltyPointDivided = rounded3.toDouble()
        selectedLoyaltyPointDivided = MethodUtils.roundOffAmountDouble(selectedLoyaltyPointDivided)
        totalItemPrice = MethodUtils.roundOffAmountDouble(totalItemPrice)
        if (totalItemPrice >= selectedLoyaltyPointDivided) {
            totalItemPrice -= selectedLoyaltyPointDivided
        }

        val nf2: NumberFormat = NumberFormat.getNumberInstance()
        nf2.maximumFractionDigits = 2
        val rounded2: String = nf2.format(selectedCashDiscountDivided)
        selectedCashDiscountDivided = rounded2.toDouble()

        if (selectedCashDiscountDivided > 0.0) {
            if (paymentOrderDetailsResponse.data.payment_type == "Cash") {
                if (paymentOrderDetailsResponse.data.cash_discount_type == "CashDiscount") {
                    totalItemPrice -= selectedCashDiscountDivided
                }
            } else if (paymentOrderDetailsResponse.data.payment_type == "Card") {
                if (paymentOrderDetailsResponse.data.cash_discount_type == "SurCharge") {
                    totalItemPrice += selectedCashDiscountDivided
                }
            }
        }
        totalItemPrice = MethodUtils.roundOffAmountDouble(totalItemPrice)

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
                subtotal_refunded = subtotal_divid
                cash_discount_or_surcharge_refunded =
                    cashdiscountdiv
                tipsRefunded = if (paymentOrderDetailsResponse.data.payment_type == "Cash") {
                    0.0
                } else {
                    if (paymentOrderDetailsResponse.data.payment_type == "Card") {
                        paymentOrderDetailsResponse.data.tips
                    } else
                        0.0
                }

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

            var parsed = 0.0
            if (cleanString.isNotEmpty()) {

                parsed = cleanString.toDouble()
            }

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted

            binding.edtAmount.setText(formatted.replace("""[$,%]""".toRegex(), ""))
            binding.edtAmount.setSelection(formatted.replace("""[$,%]""".toRegex(), "").length)



            if (paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                if (paymentOrderDetailsResponse.data.payment_type == "Card") {
                    if (binding.edtAmount.text.toString()
                            .toDouble() > paymentOrderDetailsResponse.data.amount + tipCalculation(
                            paymentOrderDetailsResponse.data.tips
                        )
                    ) {
                        var finalRefund: Double =
                            paymentOrderDetailsResponse.data.amount + tipCalculation(
                                paymentOrderDetailsResponse.data.tips
                            )
                        binding.edtAmount.setText(MethodUtils.roundOffAmountString((finalRefund).toDouble()))
                    }
                } else {
                    if (binding.edtAmount.text.toString()
                            .toDouble() > paymentOrderDetailsResponse.data.amount
                    ) {
                        binding.edtAmount.setText(MethodUtils.roundOffAmountString((paymentOrderDetailsResponse.data.amount).toDouble()))
                    }
                }

            } else {
                val newPrice: Double =
                    if (paymentOrderDetailsResponse.data.payment_type == "Card") {
                        (paymentOrderDetailsResponse.data.amount + tipCalculation(
                            paymentOrderDetailsResponse.data.tips
                        ) - paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount) /*paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount*/
                    } else {
                        paymentOrderDetailsResponse.data.amount - paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount
                    }


                if (binding.edtAmount.text.toString().toDouble() > newPrice) {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(newPrice))
                }
            }

//            var allTotalAmount = 0.0/*paymentOrderDetailsResponse.data.order.total_amount*/
//            var  alreadyRefundedAmount= 0.0 /*paymentOrderDetailsResponse.data.order.refund_detail.refunded_amount*/
//
//            paymentOrderDetailsResponse.data.order.order_items.forEach {
//                allTotalAmount += it.price
//                alreadyRefundedAmount += it.refundedAmount
//            }
//
//            val subTotalPriceNew = (allTotalAmount - alreadyRefundedAmount)
//
//            binding.edtAmount.setText(subTotalPriceNew.toString())


            binding.edtAmount.addTextChangedListener(this)
        }
    }

    private fun tipCalculation(tip: Double): Double {
        return if (paymentOrderDetailsResponse.data.global_uniq_id != "") {
            tip
        } else {
            if (magtekRequestUtils.gatewayName() == Constants.TSYS_GATEWAY) tip else 0.0
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }
}