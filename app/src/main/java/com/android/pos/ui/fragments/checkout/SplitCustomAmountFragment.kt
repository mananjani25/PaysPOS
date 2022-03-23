package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.PaymentAttributes
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentSplitCustomAmountBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.ui.fragments.dashboard.bolddashboard.CategoryFragment
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemListner
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SplitCustomAmountFragment() : Fragment(), ItemListner {
    private lateinit var binding: FragmentSplitCustomAmountBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    var isSelectedCount = 1
    private var splitValue: Int = -1
    var tipAmount = 0.0
    var tipID = null
    var cashDiscountSurcharge = 0.0
    var cardActualAmount = 0.0
    private var remainingAmount: Double = 0.0
    var cashDiscountType = ""
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var redeemLoyaltyInfo: RedeemLoyaltyInfo? = null
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private var cartList: CartModel? = null
    var paymentType = "Cash"
    var split_totalprice = 0.0
    var split_subtotal = 0.0
    var split_totaltax = 0.0
    var split_servicecharge = 0.0
    var split_totaldiscount = 0.0


    var totalServiceCharge = 0.0
    var totalDiscount = 0.0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    private var WholetotalPrice: Double = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSplitCustomAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        cartList = viewModel.cartModel
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "").isEmpty()) {
            WholetotalPrice = viewModel.totalPrice
            prefProvider.setValue(
                Constants.WHOLE_AMOUNT,
                String.format("%.2f", viewModel.totalPrice)
            )
        } else {
            WholetotalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "").toDouble()
        }

        if (prefProvider.getValue(Constants.SUB_TOTAL, "").isEmpty()) {
            subTotalPrice = viewModel.subTotalPrice
            prefProvider.setValue(
                Constants.SUB_TOTAL,
                String.format("%.2f", viewModel.subTotalPrice)
            )
        } else {
            subTotalPrice = prefProvider.getValue(Constants.SUB_TOTAL, "").toDouble()
        }

        if (prefProvider.getValue(Constants.TAX_CHARGE, "").isEmpty()) {
            totalTax = viewModel.totalTax
            prefProvider.setValue(Constants.TAX_CHARGE, String.format("%.2f", viewModel.totalTax))
        } else {
            totalTax = prefProvider.getValue(Constants.TAX_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.SERVICE_CHARGE, "").isEmpty()) {
            totalServiceCharge = viewModel.totalServiceCharge
            prefProvider.setValue(
                Constants.SERVICE_CHARGE,
                String.format("%.2f", viewModel.totalServiceCharge)
            )
        } else {
            totalServiceCharge = prefProvider.getValue(Constants.SERVICE_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").isEmpty()) {
            totalDiscount = viewModel.totalDiscount
            prefProvider.setValue(
                Constants.TOTAL_DISCOUNT,
                String.format("%.2f", viewModel.totalDiscount)
            )
        } else {
            totalDiscount = prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TIP, "").isEmpty()) {
            tipAmount = viewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", viewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }

        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "").isEmpty()) {
                cashDiscountSurcharge = MethodUtils.calculateCashDiscount(
                    WholetotalPrice,
                    prefProvider,
                    requireContext()
                )
                prefProvider.setValue(
                    Constants.CASH_DISCOUNT_SURCHARGE,
                    String.format("%.2f", cashDiscountSurcharge)
                )
            } else {
                cashDiscountSurcharge =
                    prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "0.0").toDouble()
            }
        } else {
            cashDiscountSurcharge = 0.0
            prefProvider.setValue(
                Constants.CASH_DISCOUNT_SURCHARGE,
                String.format("%.2f", cashDiscountSurcharge)
            )
        }

        setData()
        val formatterdate = SimpleDateFormat("yyyy-MM-dd")
        val formattertime = SimpleDateFormat("hh:mm a")
        val date = Date()
        future_delivery_date = formatterdate.format(date)
        future_delivery_time = formattertime.format(date)


    }

    private fun observeData() {
        paymentviewModel.data.observe(viewLifecycleOwner) { event ->

            event.getContentIfNotHandled()?.let {
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
                when (paymentType) {
                    "Cash" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", split_totalprice)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        var wholePrice =
                            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        bundle.putDouble("WholetotalPrice", wholePrice)
                        var remainingValue = wholePrice - split_totalprice
                        bundle.putDouble(
                            "remainingAmount",
                            remainingValue
                        )

                        prefProvider.setValue(Constants.WHOLE_AMOUNT, remainingValue.toString())
                        bundle.putInt("orderID", it.data.order.id ?: 0)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putInt("splitValue", isSelectedCount)
                        bundle.putBoolean("isSpilt", true)
                        bundle.putBoolean("isSplitByNo", true)
                        bundle.putBoolean("isSplitByAmount", false)
                        bundle.putString("paymentType", "Cash")
                        bundle.putParcelable("cartList", cartList)
                        bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                        bundle.putDouble("TipAmount", tipAmount)

                        bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                        bundle.putBoolean("isFromActiveOrder", false)


                        findNavController().navigate(
                            R.id.action_paymentBoldPosFragment_to_orderComplete,
                            bundle
                        )
                    }
                }
            }
        }
    }

    private fun setData() {
        MethodUtils.setPriceTextView(
            binding.tvAmount,
            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
        )
    }


    override fun onItemSelected(item: TbItem) {

    }

    override fun onCancelItemSelected() {

    }

    override fun onCategorySelected(item: TbItem) {

    }

    private fun onClick() {
        binding.linearPaycash.setOnClickListener {
            observeData()
            when (isSelectedCount) {
                1 -> {
                    calculationSplit(isSelectedCount)
                }
                2 -> {
                    calculationSplit(isSelectedCount)
                }
                3 -> {
                    calculationSplit(isSelectedCount)
                }
                4 -> {
                    calculationSplit(isSelectedCount)
                }
                5 -> {
                    calculationSplit(isSelectedCount)
                }
                6 -> {
                    calculationSplit(isSelectedCount)
                }
                else -> {
                    calculationSplit(isSelectedCount)
                }

            }
        }
        binding.tvFullAmount.setOnClickListener {
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvFullAmount.setTextColor(resources.getColor(R.color.white))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 1
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
            )
            binding.tvFullAMounttxt.visibility = View.VISIBLE
        }

        binding.tv2ways.setOnClickListener {
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv2ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 2
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / 2
            )
            binding.tvFullAMounttxt.visibility = View.INVISIBLE

        }
        binding.tv3ways.setOnClickListener {
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv3ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 3
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / 3
            )
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
        }
        binding.tv4ways.setOnClickListener {
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv4ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 4
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / 4
            )
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
        }
        binding.tv5ways.setOnClickListener {
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv5ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 5
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / 5
            )
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
        }
        binding.tv6ways.setOnClickListener {
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv6ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 6
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / 6
            )
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
        }
        binding.tvCustom.setOnClickListener {
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustom.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            isSelectedCount = 1
        }
    }

    private fun calculationSplit(count: Int) {
        split_totalprice = String.format(
            "%.2f",
            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / count
        ).toDouble()
        split_subtotal = String.format("%.2f", subTotalPrice / count).toDouble()
        split_totaltax = String.format("%.2f", totalTax / count).toDouble()
        split_servicecharge = String.format("%.2f", totalServiceCharge / count).toDouble()
        split_totaldiscount = String.format("%.2f", totalDiscount / count).toDouble()
        makeCashPayment()
    }

    private fun makeCashPayment() {
        paymentType = "Cash"
        Log.e(TAG, "cartList:  ${Gson().toJson(cartList)}")
        paymentviewModel.saveOrder(false)
        val myRequest = cartList?.let {

            paymentviewModel.createOrderRequest(
                it,
                split_subtotal,
                split_totalprice,
                split_servicecharge,
                split_totaltax,
                Constants.TAKEOUT,
                future_delivery_date,
                future_delivery_time,
                true,
                split_totaldiscount,
                tipAmount,
                splitValue,
                redeemLoyaltyInfo,
                cashDiscountSurcharge,
                true,
                paymentType,
                cashDiscountType,
                tipID
            )
        }
        Log.e(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentviewModel.actual_Total)
            val orderId = prefProvider.getValueInt("ORDER_ID", -1)
            Log.e(TAG, "orderIdmyRequestOriginal ${orderId}")
            if (orderId == -1) {
                paymentviewModel.submit(myRequest)
            } else {

                val paymentReq = myRequest.order.paymentAttributes
                if (paymentReq != null) {
                    paymentReq.order_id = orderId
                }

                val aa = SpitByOrderRequestModel(
                    orderId,
                    true,
                    SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                )

                paymentviewModel.splitByOrder(aa, false)

            }
        }
    }


}