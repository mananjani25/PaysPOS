package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentPayFullAmountBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemListner
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PayFullAmountFragment(val bundle: Bundle?) : Fragment(), ItemListner {
    private lateinit var binding: FragmentPayFullAmountBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private val TAG = "DashboardCategoryBold"
    var frameLayoutId = 0
    var paymentType = "Cash"
    var cashDiscountSurcharge = 0.0
    var cardActualAmount = 0.0
    var llRoot = 0
    private var remainingAmount: Double = 0.0
    var cashDiscountType = ""
    var paymentAmount = 0.0
    private var redeemLoyaltyInfo: RedeemLoyaltyInfo? = null
    var totalPrice = 0.0
    private var splitValue: Int = -1
    var tipAmount = 0.0
    private var cartItems: List<TbItem>? = null
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var tipID = null
    var totalServiceCharge = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    var totalDiscount = 0.0

    private var cartList: CartModel? = null

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentPayFullAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        frameLayoutId = bundle?.getInt("frameLayoutId")!!
        llRoot = bundle.getInt("llRoot")!!
        observeShowProgress()
        getCartData()
        observeData()

    }

    private fun getCartData() {
        cartList = viewModel.cartModel
        totalPrice = viewModel.totalPrice
        subTotalPrice = viewModel.subTotalPrice
        totalServiceCharge = viewModel.totalServiceCharge
        totalTax = viewModel.totalTax

        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            cashDiscountSurcharge = MethodUtils.calculateCashDiscount(
                viewModel.totalPrice,
                prefProvider,
                requireContext()
            )
        }
        paymentviewModel.saveActualValue(
            totalPrice,
            subTotalPrice,
            totalTax,
            totalServiceCharge,
            tipAmount,
            totalDiscount,
            MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext()),
            cardActualAmount
        )

        MethodUtils.getCashPaymentOptionList(
            totalPrice,
            binding.tvCash1,
            binding.tvCash2,
            binding.tvCash3
        )

    }


    private fun onClick() {
        binding.llManualCardEntry.setOnClickListener {
            val viewPager: FrameLayout = activity?.findViewById(frameLayoutId) as FrameLayout
            viewPager.visibility = View.VISIBLE

            val llRoot: LinearLayout = activity?.findViewById(llRoot) as LinearLayout
            llRoot.visibility = View.GONE

            loadManualCardEntryFragment(ManualCardEntryFragment())
        }


        binding.llPaycash.setOnClickListener {
            paymentAmount = totalPrice
            makeCashPayment()
        }
        binding.tvCash1.setOnClickListener {

        }
        binding.llCreditCard.setOnClickListener {
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCreditCard.setTextColor(resources.getColor(R.color.white))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.llManualCardEntry.setOnClickListener {
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvManualCard.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash1.setOnClickListener {
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash1.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash2.setOnClickListener {
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash2.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash3.setOnClickListener {
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash3.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCustom.setOnClickListener {
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustom.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvPaymentLink.setOnClickListener {
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
    }

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(frameLayoutId, fragment).commit()
    }


    override fun onItemSelected(item: TbItem) {

    }

    override fun onCancelItemSelected() {

    }

    private fun observeData() {

        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when (paymentType) {
                    "Cash" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", totalPrice)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }


                        bundle.putDouble("WholetotalPrice", totalPrice)
                        bundle.putDouble(
                            "remainingAmount",
                            0.0
                        )
                        bundle.putInt("orderID", it.data.order.id ?: 0)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putInt("splitValue", -1)
                        bundle.putBoolean("isSpilt", false)
                        bundle.putBoolean("isSplitByNo", false)
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

                        prefProvider.setValueInt("ORDER_ID", -1)


                    }
                }
            }
        }
    }

    private fun observeShowProgress() {

        paymentviewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun makeCashPayment() {
        paymentType = "Cash"
        Log.e(TAG, "cartList:  ${Gson().toJson(cartList)}")
        Log.e(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
        val myRequest = cartList?.let {

            paymentviewModel.createOrderRequest(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                TAKEOUT,
                future_delivery_date,
                future_delivery_time,
                true,
                totalDiscount,
                tipAmount,
                splitValue,
                redeemLoyaltyInfo,
                cashDiscountSurcharge,
                true,
                paymentType, cashDiscountType,
                tipID
            )
        }
        Log.e(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(totalPrice)
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
                    paymentReq!!,
                    SpitByOrderPaymentModel(listOf(paymentReq))
                )

                paymentviewModel.splitByOrder(aa, false)

            }
        }
    }

}