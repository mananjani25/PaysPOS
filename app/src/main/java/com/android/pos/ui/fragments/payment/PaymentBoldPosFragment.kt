package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.CheckOutDineInDataModel
import com.android.pos.data.model.DineinCartPaymentModel
import com.android.pos.data.model.GuestDataModel
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.requestModel.DineInOrderPayment
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.requestModel.OrderServiceChargesAttribute
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.android.pos.data.remote.Constants.IS_PAX_PAYMENT_FAILED
import com.android.pos.data.remote.Constants.MANUAL_SALE
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.SPLIT_DINEIN_CHECKOUT
import com.android.pos.data.remote.Constants.SPLIT_DINEIN_MODEL
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.data.remote.Constants.SPLIT_IS_GUESTPAY
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentPaymentBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.checkout.CheckoutDineInFragmentNew
import com.android.pos.ui.fragments.checkout.CheckoutDineInPaymentViewModel
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.TAG
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.getCustomerDisplay
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentBoldPosFragment : Fragment() {
    private lateinit var presentation: CustomDisplay
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int = -1
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var dineinCartPaymentModel: DineinCartPaymentModel? = null
    private var guestRequestModel: GuestPaymentRequest? = null
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val paymentViewModel by viewModels<DineInOrderTableViewModel>()

    private val dineInPaymentViewModel by viewModels<CheckoutDineInPaymentViewModel>()
    private var isFromActiveOrder: Boolean = false


    companion object {
        public lateinit var binding: FragmentPaymentBoldPosBinding
        fun newInstance(): PaymentBoldPosFragment {
            val frag = PaymentBoldPosFragment()
            return frag

        }

    }


    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentBoldPosBinding.inflate(inflater, container, false)
        prefProvider.setValueboolean(Constants.IS_PAYMENT_SCREEN, true)
        val onBackPressedCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPress()
                }

            }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        binding.layoutHeaderCheckout.rlRoot.visibility = View.VISIBLE
        binding.lifecycleOwner = this
        isFromActiveOrder = arguments?.getBoolean("isFromActiveOrder") ?: false
        orderId = arguments?.getInt("orderId")
        viewModel.setSplitCount(1)
        LogUtil.logE("orderId :: ", orderId.toString())
        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                viewModel,
                passcodeViewModel,
                paymentViewModel
            )
        }
        if (orderId != null) {
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        return binding.root
    }


    public fun addTipHideShow(isBoolean: Boolean) {
        if (isBoolean) {
            binding.layoutHeaderCheckout.tvAddTip.gone()
        } else {
            binding.layoutHeaderCheckout.tvAddTip.visible()

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setTipAmount(0.0)
        if (prefProvider.getValue(
                ORDER_TYPE,
                TAKEOUT
            ) == DINE_IN

        ) {

            if (arguments != null) {
                val isGuest = arguments?.getBoolean("isGuestPay") ?: false

                prefProvider.setValueboolean(SPLIT_IS_GUESTPAY, isGuest)
                if (isGuest) {

                    val data =
                        requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)
                    LogUtil.logE("GuestData", "Data ${Gson().toJson(data)}")
                    dineinCartPaymentModel =
                        requireArguments().getParcelable<DineinCartPaymentModel>("dineinPaymentModel")
                    var model = GuestPaymentCalculationModel(
                        data?.subTotal?.toDouble() ?: 0.0,
                        data?.totalAmount?.toDouble() ?: 0.0,
                        data?.totalServiceCharge?.toDouble() ?: 0.0,
                        data?.totalTax?.toDouble() ?: 0.0,
                        data?.cashDiscount?.toDouble() ?: 0.0,
                        data?.totalDiscount?.toDouble() ?: 0.0,
                        requireArguments().getInt("id"),
                        dineinCartPaymentModel


                    )
                    guestRequestModel = requireArguments().getParcelable("model")
                    viewModel.setGuestPay(true)
                    if(this::presentation.isInitialized){
                        presentation.show()
                        presentation.onDisplayChanged()
                        presentation.setGuestPay(true, model)
                    }

                    prefProvider.setValue(SPLIT_DINEIN_MODEL, Gson().toJson(model))
                    loadCartFragment(CartFragment(null, null, true, model, true))
                } else {
                    LogUtil.logE(TAG, "elsePAymentDion")
                    var model = GuestPaymentCalculationModel(
                        requireArguments().getDouble("subTotalPrice"),
                        requireArguments().getDouble("totalPrice"),
                        requireArguments().getDouble("totalServiceCharge"),
                        requireArguments().getDouble("totalTax"),
                        0.0,
                        requireArguments().getDouble("totalDiscount"),
                        wholeOrderPassDiscount = requireArguments().getDouble("totalOrderPassDiscount"),
                        wholeOrderPassSC = requireArguments().getDouble("totalOrderPassSC")
                    )
                    Log.e(TAG, "getDineInDetails  ${Gson().toJson(model)}")
                    prefProvider.setValue(SPLIT_DINEIN_MODEL, Gson().toJson(model))
                    loadCartFragment(CartFragment(null, null, true, model, false))
                }
            } else {
                if (prefProvider.getValueboolean(SPLIT_IS_GUESTPAY, false)) {
                    var string_gson = prefProvider.getValue(SPLIT_DINEIN_MODEL, "")
                    var temp_model =
                        Gson().fromJson(string_gson, GuestPaymentCalculationModel::class.java)
                    var model = GuestPaymentCalculationModel(
                        temp_model.subTotal,
                        temp_model.total,
                        temp_model.serviceCharge,
                        temp_model.tax,
                        temp_model.cashDiscount,
                        temp_model.totalDiscount,
                        temp_model.guestId,
                        temp_model.model
                    )
                    loadCartFragment(CartFragment(null, null, true, model, true))
                } else {
                    if (prefProvider.getValue(SPLIT_DINEIN_MODEL, "") != null) {
                        var string_gson = prefProvider.getValue(SPLIT_DINEIN_MODEL, "")
                        var temp_model =
                            Gson().fromJson(string_gson, GuestPaymentCalculationModel::class.java)
                        var model = GuestPaymentCalculationModel(
                            temp_model.subTotal,
                            temp_model.total,
                            temp_model.serviceCharge,
                            temp_model.tax,
                            0.0,
                            temp_model.totalDiscount,
                        )
                        loadCartFragment(CartFragment(null, null, true, model, false))
                    }

                }
            }


        } else {
            loadCartFragment(CartFragment(null, null))
        }
        if (prefProvider.getValue(ORDER_TYPE, "") == Constants.DINE_IN) {
            if (arguments != null) {
                val serviceChargeAppliedList: ArrayList<OrderServiceChargesAttribute> =
                    arguments?.getParcelableArrayList<OrderServiceChargesAttribute>("serviceChargeAppliedList")
                        ?: arrayListOf<OrderServiceChargesAttribute>()
                var dineInOrderId = requireArguments().getInt("orderId")
                var isGuest = requireArguments().getBoolean("isGuestPay") ?: false
                var isLastPayment = requireArguments().getBoolean("isLastPayment") ?: false
                var splitModel: DineInOrderPayment =
                    requireArguments().getParcelable("orderPayment") ?: DineInOrderPayment()

                Log.e("CheckGuestPayment", "isGuestisGuest  ${isGuest}")
                Handler(Looper.getMainLooper()).postDelayed({
                    val dineInModel = CheckOutDineInDataModel(
                        requireArguments().getInt("id") ?: 0,
                        isGuest,
                        isLastPayment,
                        guestRequestModel,
                        dineInOrderId,
                        splitModel,
                        dineInAdapterList = requireArguments()?.getParcelableArrayList(Constants.DINE_IN_ADAPTER_LIST),
                        dineInOrderDetails = requireArguments()?.getParcelable(Constants.PRINT_DATA_DINE_IN),
                        guestPaymentModel = requireArguments()?.getParcelable(Constants.DINE_IN_GUEST_PAYMENT_DATA),
                        guestPosition = requireArguments()?.getInt(Constants.GUEST_POSITION),
                        serviceChargeAppliedList
                    )
                    LogUtil.logE(TAG, "dineInModel:  ${Gson().toJson(dineInModel)}")
                    prefProvider.setValue(SPLIT_DINEIN_CHECKOUT, Gson().toJson(dineInModel))
                    loadCategoryFragment(CheckoutDineInFragmentNew(dineInModel))
                }, 100)
            } else {
                if (prefProvider.getValue(SPLIT_DINEIN_CHECKOUT, "") != null) {
                    var string_gson = prefProvider.getValue(SPLIT_DINEIN_CHECKOUT, "")
                    var temp_model =
                        Gson().fromJson(string_gson, CheckOutDineInDataModel::class.java)
                    Log.e(
                        "CheckGuestPAymentOrNot",
                        "temp_modeltemp_model   ${Gson().toJson(temp_model)}"
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dineInModel = CheckOutDineInDataModel(
                            temp_model.guestId,
                            temp_model.isFromGuest,
                            temp_model.isLastPayment,
                            temp_model.guestPaymentReq,
                            temp_model.orderId,
                            temp_model.splitModel,
                            dineInAdapterList = temp_model.dineInAdapterList,
                            dineInOrderDetails = temp_model.dineInOrderDetails,
                            guestPaymentModel = temp_model.guestPaymentModel,
                            guestPosition = temp_model.guestPosition,
                            temp_model.servicChargeAppliedlist
                        )
                        LogUtil.logE(TAG, "dineInModel:  ${Gson().toJson(dineInModel)}")
                        loadCategoryFragment(CheckoutDineInFragmentNew(dineInModel))
                    }, 100)
                }
            }

        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                loadCategoryFragment(
                    CheckoutDetailsFragmentNew(
                        arguments?.getBoolean("isFromActiveOrder") == true
                    )
                )
            }, 100)

        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            onBackPress()
        }

        listeners()
        setFragmentResultListener(
            "request_key_tips"
        ) { requestKey: String, bundle: Bundle ->

        }
    }

    private fun onBackPress(){
        Log.d(TAG, "onViewCreated: " + prefProvider.getValueboolean(SPLIT_ENABLE, false))
        if (prefProvider.getValueboolean(Constants.SPLIT_ENABLE, false)) {
            AlertUtils.showCustomAlert(requireContext(), "Please complete all payment.")
        } else if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
            AlertUtils.showCustomAlert(requireContext(), getString(R.string.pax_transaction_error_message))
        } else {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == OPEN_ORDER) {
                val navController = findNavController()
                var bundle: Bundle = Bundle()
                if (orderId != null) {
                    bundle.putBoolean("update", true)
                    bundle.putInt("orderId", orderId!!)
                    bundle.putInt("paymentId", paymentId!!)
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                    var bundle1: Bundle = Bundle()
                    bundle1.putBundle("updateBundle", bundle)

                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "data", bundle1
                        )
                    }
                    if(prefProvider.getValueboolean(IS_FROM_ALL_ORDER,false)){
                        navController.navigate(R.id.action_paymentBoldPosFragment_to_allOrdersFragment)
                    }else{
                        navController.popBackStack()
                    }

                } else {
                    if(prefProvider.getValueboolean(IS_FROM_ALL_ORDER,false)){
                        findNavController().navigate(R.id.action_paymentBoldPosFragment_to_allOrdersFragment)
                    }else{
                        if(prefProvider.getValue(REDIRECT_FROM, "") == MANUAL_SALE) {
                            viewModel.cartModel = null
                        }
                        findNavController().popBackStack()
                    }
                }

        }
    }

    private fun listeners() {
        binding.layoutHeaderCheckout.tvAddTip.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                findNavController().navigate(
                    R.id.action_paymentBoldPosFragment_to_addTipDialog,
                    bundleOf(
                        "totalTip" to viewModel.tipTransactionAmount,
                        "splitCount" to viewModel.isSelectCount
                    )
                )
            }
        }
        binding.layoutHeaderCheckout.tvAddDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_paymentBoldPosFragment_to_addDiscountDialogFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        prefProvider.setValueboolean(Constants.IS_PAYMENT_SCREEN, false)
    }

    override fun onPause() {
        super.onPause()
        LogUtil.logE(TAG, "onPause")
        if (!prefProvider.getValueboolean(SPLIT_ENABLE, false)) {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                removeCustomer()
                dineInPaymentViewModel.deleteCart()
                prefProvider.setValue(Constants.ORDER_TYPE, "")
            }
            if (!viewModel.onClickAddCustomer) {
                if (isFromActiveOrder) {
                    removeCustomer()
                    viewModel.deleteCart()
                    prefProvider.setValue(Constants.ORDER_TYPE, "")

                }
            }
        }


    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putBoolean("isFromPayment", true)
            arguments?.getBoolean("isLoyaltyApplied")?.let { putBoolean("isLoyaltyApplied", it) }
            arguments?.getBoolean("isFromActiveOrder")?.let { putBoolean("isFromActiveOrder", it) }
            putString(REDIRECT_FROM, prefProvider.getValue(REDIRECT_FROM, ""))
            // putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
        }
        frag.arguments = result
        //frag.arguments = arguments

        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        try {
            val fm: FragmentManager = requireActivity().supportFragmentManager

            LogUtil.logE("orderId :: ", orderId.toString())
            val bundle = Bundle().apply {
                orderId?.let { putInt("orderId", it) }
                putInt("paymentId", paymentId)
                putString("orderOfflineId", orderOfflineId)
                putString("paymentOfflineId", paymentOfflineId)
                putString(REDIRECT_FROM, prefProvider.getValue(REDIRECT_FROM, ""))

            }
            fragment.arguments = bundle
            // fragment.arguments = arguments

            fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
            // binding.frameLayout?.let { fm.beginTransaction().replace(it, fragment).commit() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

private operator fun Double?.div(toInt: Int?): Double {
   return this?:0.0.toDouble()
}
