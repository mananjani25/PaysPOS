package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.VariationDashboardListAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardCategoryBoldPOS : Fragment(), ItemListner {
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentDashboardCategoryBoldPosBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private val TAG = "DashboardCategoryBold"
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    var isupdate = false
    var orderDiscount = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        resultListener()
        addObserver()
        getServiceCharges()
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun resultListener() {

        setFragmentResultListener("request_key_customer") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                Log.e(TAG, "gotBundlebundle:  ${Gson().toJson(bundle)}")
                setUpCustomer(result, bundle)
            }
        }

        setFragmentResultListener("request_key_discount_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            if (result != null && viewModel.totalPrice != 0.0) {
                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice
                val price = discountApplyPrice - orderDiscount

                if (cartList.isNotEmpty()) {
                    cartList[0].discountPrice = orderDiscount
                    cartList[0].discountType = result.discountType
                    if (result.id != -1) {
                        cartList[0].discountId = result.id
                    }
                    viewModel.addCart(cartList[0])
                }


            }
        }
    }

    private fun setUpCustomer(result: TbCustomer, bundle: Bundle) {
        if (cartList.isNotEmpty()) {
            cartList[0].customer = result
            viewModel.addCart(cartList[0])
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()
        if (arguments != null) {
            isupdate = arguments?.getBoolean("update")!!
        }
        syncData()
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        loadCartFragment(CartFragment())
        loadCategoryFragment(CategoryFragment(this))
        binding.layoutHeader.txtUserName.text =
            prefProvider.getValue(EMPLOYEE_NAME, "").toString()

    }


    private fun syncData() {

        val sync = prefProvider.getValueboolean(Constants.SYNC_DATA, false)
        if (!sync)
            viewModel.syncInventoryModule()
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = childFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
            if (arguments != null) {
                putBundle("updateBundle", arguments)
            }
        }
        frag.arguments = result
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadKeyPadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()
    }

    private fun onClick() {

        binding.layoutHeader.txtTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_transactionFragment)

        }
        binding.layoutHeader.txtDineIn.setOnClickListener {

        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_menuFragment)

        }
        binding.layoutHeader.txtOpenOrder.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)
        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_passcode,
                bundle
            )
        }
        binding.layoutHeader.ivLock.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_reportEODFragment)
        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            binding.layoutHeaderCheckout.rlRoot.visibility = View.GONE
            binding.layoutHeader.rlRoot.visibility = View.VISIBLE
            loadCategoryFragment(CategoryFragment(this))
        }

        binding.layoutHeader.imgSync.setOnClickListener {
            viewModel.syncInventoryModule()
        }
        /* binding.layoutHeader.txtOpenOrder.setOnClickListener {
         loadCategoryFragment(CategoryFragment(this))
         binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.btnColor))
         binding.layoutHeader.txtOpenOrder.setTypeface(
             binding.layoutHeader.txtOpenOrder.typeface,
             Typeface.BOLD
         )
         binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.txtColor))
         binding.layoutHeader.txtKeypad.setTypeface(
             binding.layoutHeader.txtKeypad.typeface,
             Typeface.NORMAL
         )
     }*/
        binding.layoutHeader.txtKeypad.setOnClickListener {
            //loadKeyPadFragment(KeyPadManualSaleFragment())
            binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.btnColor))
            binding.layoutHeader.txtKeypad.setTypeface(
                binding.layoutHeader.txtKeypad.typeface,
                Typeface.BOLD
            )
            binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.txtColor))
            binding.layoutHeader.txtOpenOrder.setTypeface(
                binding.layoutHeader.txtOpenOrder.typeface,
                Typeface.NORMAL
            )

            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_manualSalesNew)
        }


    }


    override fun onItemSelected(item: TbItem) {
        Log.e(TAG, "getitem:  ${Gson().toJson(item)}")
        Log.e(TAG, "OrderTYpe:  ${prefProvider.getValue(ORDER_TYPE, TAKEOUT)}")

        /*
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.OPEN_ORDER) {

             if (cartList.isEmpty()) {
                 val model = CartModel()
                 model.employeeID =
                     prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                 model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                 model.orderType = prefProvider.getValue(ORDER_TYPE, TAKEOUT)
                 model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
                 model.serviceCharge = serviceChargesList
                 viewModel.ordertypelist.forEach {
                     if (it.orderType == Constants.OPEN_ORDER) {
                         model.orderTypeId = it.id
                     }
                 }
                 cartList.add(model)
             }

         } else {
             createCart()
         }
 */

        val fragment = AddItemFragment.newInstance(item, this, cartList)
        loadCategoryFragment(fragment)
    }

    private fun createCart(): ArrayList<CartModel>? {
        if (cartList.isEmpty()) {
            val model = CartModel()
            model.employeeID =
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            model.orderType = TAKEOUT
            model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            model.serviceCharge = serviceChargesList
            // model.orderTypeId = 1
            viewModel.ordertypelist.forEach {
                if (it.orderType.lowercase() == TAKEOUT.lowercase()) {
                    model.orderTypeId = it.id
                }
            }
            cartList.add(model)
            viewModel.addCart(cartList[0])
            return cartList
        }

        return cartList
    }

    override fun onCancelItemSelected() {
        loadCategoryFragment(CategoryFragment(this))

    }

    override fun onCategorySelected(item: TbItem) {

    }

    private fun addObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            Log.e(TAG, "MAllWords:::  ${Gson().toJson(it)}" )
            if (it.isEmpty()) {
                cartList.clear()
                cartList = arrayListOf()

            } else {
                cartList.clear()
                cartList = arrayListOf()
                cartList.addAll(it.toCollection(arrayListOf()))
            }


        }

        viewModel.orderTypes().observe(requireActivity()) {
            if (it.data != null) {
                ordertypelist = it.data as ArrayList<TbOrderType>
                viewModel.setOrderTypeList(ordertypelist)
            }
        }

        viewModelPayment.QueueCreateSaveOrder.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let {
                viewModel.deleteCart()
                prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

            }
        }
    }

    private fun getServiceCharges() {

        serviceChargesObserve = Observer {

            if (it.status == Status.SUCCESS) {
                serviceChargesList = it.data

            }

        }

        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }


    private fun checkItemQty(
        data: TbItem,
        variationAdapter: VariationDashboardListAdapter?
    ): Boolean {

        if (data.variationsAttributes.isNotEmpty()) {

            val stockQty = variationAdapter?.getItem()?.stockQty

            return if (stockQty?.isNotEmpty() == true) {

                stockQty.toInt() >= 1

            } else {
                false
            }

        } else {

            return if (data.isManualSales) {
                true
            } else {
                data.quantity >= 1
            }
        }

        return false
    }


}