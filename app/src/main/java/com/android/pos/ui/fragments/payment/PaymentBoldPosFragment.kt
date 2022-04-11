package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.DineinCartPaymentModel
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentPaymentBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.checkout.CheckoutDineInFragmentNew
import com.android.pos.ui.fragments.checkout.CheckoutDineInPaymentViewModel
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.TAG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentBoldPosFragment : Fragment() {
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int = -1
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var dineinCartPaymentModel: DineinCartPaymentModel? = null

    private val dineInPaymentViewModel by viewModels<CheckoutDineInPaymentViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentPaymentBoldPosBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentBoldPosBinding.inflate(inflater, container, false)
        binding.layoutHeaderCheckout.rlRoot.visibility = View.VISIBLE
        binding.lifecycleOwner = this

        orderId = arguments?.getInt("orderId")

        Log.e("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (prefProvider.getValue(
                ORDER_TYPE,
                TAKEOUT
            ) == DINE_IN && requireArguments().getBoolean("isGuestPay")

        ) {
            dineinCartPaymentModel = requireArguments().getParcelable<DineinCartPaymentModel>("dineinPaymentModel")
            var model = GuestPaymentCalculationModel(
                requireArguments().getDouble("subTotalB"),
                requireArguments().getDouble("totalB"),
                requireArguments().getDouble("serviceChargeB"),
                requireArguments().getDouble("totalTaxB"),
                requireArguments().getDouble("divideCashDiscount"),
                requireArguments().getDouble("dicountB"),
                requireArguments().getInt("id"),
                dineinCartPaymentModel

            )

            loadCartFragment(CartFragment(null,null,true,model))
        } else {
            loadCartFragment(CartFragment(null, null))
        }
        if (prefProvider.getValue(ORDER_TYPE, "") == Constants.DINE_IN) {
            var dineInOrderId = requireArguments().getInt("orderId")
            Handler(Looper.getMainLooper()).postDelayed({
                loadCategoryFragment(CheckoutDineInFragmentNew.newInstacne(dineInOrderId))
            }, 100)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                loadCategoryFragment(CheckoutDetailsFragmentNew())
            }, 100)

        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            Log.d(TAG, "onViewCreated: " + prefProvider.getValueboolean(SPLIT_ENABLE, false))
            if (prefProvider.getValueboolean(Constants.SPLIT_ENABLE, false)) {
                AlertUtils.showCustomAlert(requireContext(), "Please complete all payment.")
            } else {
                findNavController().popBackStack()
            }
        }

        listeners()
        setFragmentResultListener(
            "request_key_tips"
        ) { requestKey: String, bundle: Bundle ->

        }
    }

    private fun listeners() {
        binding.layoutHeaderCheckout.tvAddTip.setOnClickListener {
            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_addTipDialog,
                bundleOf("totalTip" to viewModel.tipTransactionAmount)
            )
        }
        binding.layoutHeaderCheckout.tvAddDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_paymentBoldPosFragment_to_addDiscountDialogFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.e(TAG, "onPause")
        if (!prefProvider.getValueboolean(SPLIT_ENABLE, false)) {
            removeCustomer()
            dineInPaymentViewModel.deleteCart()
            prefProvider.setValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
        }
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putBoolean("isFromPayment", true)
            putString(REDIRECT_FROM, arguments?.getString(REDIRECT_FROM))
            // putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
        }
        frag.arguments = result
        //frag.arguments = arguments

        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager

        Log.e("orderId :: ", orderId.toString())
        val bundle = Bundle().apply {
            orderId?.let { putInt("orderId", it) }
            putInt("paymentId", paymentId)
            putString("orderOfflineId", orderOfflineId)
            putString("paymentOfflineId", paymentOfflineId)
            putString(REDIRECT_FROM, arguments?.getString(REDIRECT_FROM))

        }
        fragment.arguments = bundle
        // fragment.arguments = arguments

        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
        // binding.frameLayout?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }

    fun removeCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValueInt("ORDER_ID", -1)
        prefProvider.setValueInt(Constants.PAYMENT_ID, 0)
        prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.SUB_TOTAL_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_ACTUAL, "0.0")
        prefProvider.setValue(
            Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
            "0.0"
        )
        prefProvider.setValue(Constants.TAX_CHARGE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")
    }

}