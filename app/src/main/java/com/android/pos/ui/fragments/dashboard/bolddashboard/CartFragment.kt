package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.alert
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CartFragment() : Fragment() {
    private lateinit var binding: FragmentCartBinding
    var fragmentId: Int? = null
    var checkoutHeaderId: Int = 0
    var dashboardHeaderId: Int = 0
    private var isOrderUpdate: Boolean = false
    private var isLoyaltyApplied: Boolean = false
    private lateinit var cartAdapter: CartAdapter
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var future_delivery_date: String = ""
    private var paymentId: Int? = null
    private var future_delivery_time: String = ""
    lateinit var cashDiscountModel: CashDiscountModel
    var cashDiscountType = ""
    var cartlist: ArrayList<CartModel> = arrayListOf()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by viewModels<PaymentViewModel>()
    var updateBundle: Bundle? = null
    var isFromPayment: Boolean = false


    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"


    companion object {
        fun newInstacne(isFromPayment: Boolean): CartFragment {
            val bundle = bundleOf("isFromPayment" to isFromPayment)
            val frag = CartFragment()
            frag.arguments = bundle
            return frag

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        Log.e("bundleData", arguments.toString())

        if (arguments?.getBundle("updateBundle") != null) {
            updateBundle = arguments?.getBundle("updateBundle")
        }


        if (arguments?.getBoolean("isFromPayment") != null) {
            isFromPayment = arguments?.getBoolean("isFromPayment")!!
        }
        if (arguments?.getInt("fragmentId") != null)
            fragmentId = arguments?.getInt("fragmentId")
        if (arguments?.getInt("checkoutHeaderId") != null)
            checkoutHeaderId = arguments?.getInt("checkoutHeaderId")!!

        if (arguments?.getInt("dashboardHeaderId") != null)
            dashboardHeaderId = arguments?.getInt("dashboardHeaderId")!!

        isFromPayment = arguments?.getBoolean("isFromPayment") ?: false
        setUpData()
        return binding.root
    }

    private fun setUpData() {
        if (isFromPayment) {
            binding.linearButtonView.visibility = View.GONE
            binding.imgOrderMenu.visibility = View.INVISIBLE
            binding.imgOrderMenu.isEnabled = false
            binding.imgOrderMenu.isClickable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        setCartAdapter()
        addObserver()


        if (isFromPayment) {
            binding.linearButtonView.visibility = View.GONE
        } else {
            binding.linearButtonView.visibility = View.VISIBLE
        }
        if (updateBundle != null) {
            isOrderUpdate = updateBundle?.getBoolean("update")!!
            if (isOrderUpdate) {
                orderId = updateBundle?.getInt("orderId")
                paymentId = updateBundle?.getInt("paymentId")
                paymentOfflineId = updateBundle?.getString("paymentOfflineId").toString()
                orderOfflineId = updateBundle?.getString("orderOfflineId").toString()
                viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                    updateBundle?.getBoolean("isLoyaltyApplied")!!
            } else {
                prefProvider.setValue(ORDER_TYPE, TAKEOUT)
            }
        } else {
            isOrderUpdate = false
            prefProvider.setValue(ORDER_TYPE, TAKEOUT)
        }

        if (isOrderUpdate) {
            binding.tvSave.text = getString(R.string.update)
        } else {
            binding.tvSave.text = getString(R.string.save)
        }

    }

    private fun addObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(com.android.pos.data.remote.Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            Log.e(TAG,"getCartList:  ${Gson().toJson(it)}")
            if (it.isNotEmpty()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                it[it.size - 1].items?.toCollection(arrayListOf())
                    ?.let { it1 -> cartAdapter.setList(it1) }
                viewModel.itemCalculation(
                    it,
                    binding.txtTotal,
                    requireContext()
                )
                viewModel.setCartModel(it)

                binding.txtSubTotal.text = MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(viewModel.totalPrice)

            } else {
                cartAdapter.clearList()
            }
        }

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
        viewModelPayment.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
        viewModelPayment.QueueCreateSaveOrder.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let {
                viewModel.deleteCart()
                if (prefProvider.getValue(Constants.ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(Constants.ORDER_TYPE, "")
                }
                clearCustomer()
                redirectToActiveOrder()
            }
        }
    }

    fun redirectToActiveOrder() {
        try {
            var intent: Intent = Intent()
            intent.action = "SEND_TO_ACTIVE_ORDER"
            requireActivity().sendBroadcast(intent)
        } catch (e: Exception) {
        }
    }

    private fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        saveCustomerData(null)
        refreshItemCalculation()
    }

    private fun saveCustomerData(nothing: Nothing?) {

    }

    private fun refreshItemCalculation() {
        viewModel.itemCalculation(
            cartlist,
            binding.txtTotal,
            requireContext()
        )
    }


/*
    private fun getCartList() {

        Log.e("Loyalty", "getCartList called..")

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
        binding.layoutCart.rvCart.adapter = cartAdapter
        binding.layoutCart.rvCartDineIn.adapter = dineInCartAdapter


        nameObserver = Observer {

            bindData(it)

            removeObserver()
        }


        if (isAdded)
            addObserver()
    }
*/

    fun initListeners() {
        binding.imgOrderMenu.setOnClickListener {

            hideOrderMenu()

        }

        binding.llClearCart.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.delete_items_message)
            ) {
                positiveButton(getString(R.string.tv_delete)) {
                    // Do positive stuff here
                    prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)



                    viewModel.deleteCart()

                    if (prefProvider.getValue(Constants.ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(Constants.ORDER_TYPE, "")
                    }

                    hideOrderMenu()
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)


                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
        }

        binding.tvPayNow.setOnClickListener {
            if (cartAdapter.cartList.isNotEmpty()) {

                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                ) { _, _ ->
                }
            }

        }
        binding.tvSave.setOnClickListener {
            if (cartAdapter.cartList.isNotEmpty()) {
                if (prefProvider.getValue(Constants.ORDER_TYPE, "") != Constants.DINE_IN) {

                    var ordertype = ""
                    var ordertypeId = 0
                    viewModel.ordertypelist.forEach {
                        if (it.orderType == Constants.OPEN_ORDER) {
                            ordertype = it.orderType
                            ordertypeId = it.id
                        }
                    }

                    if (viewModel.restrictedAmount(binding.txtTotal)) {
                        val cartList = viewModel.generateCombinedItems(viewModel.cartModel!!)
                        cartList.openOrderType = Constants.PICK_UP
                        cartList.orderType = ordertype
                        cartList.orderTypeId = ordertypeId


                        viewModelPayment.updateOrder(
                            isOrderUpdate,
                            orderId,
                            paymentId,
                            paymentOfflineId,
                            orderOfflineId
                        )

                        var totalAmountTobeSave = 0.0
                        if (viewModel.redeemLoyaltyInfo.isLoyaltyApplied == true) {
                            totalAmountTobeSave =
                                (viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0)
                        } else {
                            totalAmountTobeSave =
                                viewModel.totalPrice
                        }

                        if (cartList.futureDeliveryDate.isNotEmpty()) {
                            future_delivery_date = cartList.futureDeliveryDate
                        }

                        val request = viewModelPayment.createOpenOrderRequest(
                            cartList,
                            viewModel.subTotalPrice,
                            totalAmountTobeSave,
                            viewModel.totalServiceCharge,
                            viewModel.totalTax,
                            Constants.OPEN_ORDER_,
                            future_delivery_date,
                            future_delivery_time,
                            false,
                            viewModel.totalDiscount + cartList.discountPrice,
                            0.00,
                            -1,
                            viewModel.redeemLoyaltyInfo,
                            MethodUtils.calculateCashDiscount(
                                viewModel.totalPrice,
                                prefProvider,
                                requireContext()
                            ),
                            false,
                            "Cash",
                            cashDiscountType

                        )
                        viewModelPayment.saveOrder(true)
                        viewModelPayment.submit(request)
                    } else {
                        showMessage()
                    }
//                }
                }
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                ) { _, _ ->
                }
            }
        }
    }

    private fun showMessage() {
        AlertUtils.showCustomAlert(requireContext(), "Order should be less than 1 million usd.")
    }


    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val bundle = Bundle().apply {
            fragmentId?.let {
                putInt("fragmentId", it)
                orderId?.let { it1 -> putInt("orderId", it1) }
                paymentId?.let { it1 -> putInt("paymentId", it1) }
                paymentOfflineId?.let { it1 -> putString("paymentOfflineId", it1) }
                orderOfflineId?.let { it1 -> putString("orderOfflineId", it1) }
            }
        }
        fragment.arguments = bundle
        fragmentId?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }

/*   fun loadCategoryFragment(fragment: Fragment) {
      val fm: FragmentManager = requireActivity().supportFragmentManager
      val bundle=Bundle().apply {
          fragmentId?.let { putInt("fragmentId", it) }
      }
      fragment.arguments=bundle
      fragmentId?.let { fm.beginTransaction().replace(it, fragment).commit() }
              var tbItems: ArrayList<TbItem> = arrayListOf()
              it[0].items?.toCollection(arrayListOf())?.let { it1 -> tbItems.addAll(it1) }
              cartAdapter.setList(tbItems)
          }

      }*/


    private fun setCartAdapter() {
        cartAdapter = CartAdapter()
        binding.rvCartList.adapter = cartAdapter
    }

    private fun hideOrderMenu() {
        if (binding.llClearCart.visibility == View.VISIBLE) {
            binding.llClearCart.visibility = View.GONE
        } else {
            binding.llClearCart.visibility = View.VISIBLE
        }
    }

}