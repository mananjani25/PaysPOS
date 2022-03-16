package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.R.attr.button
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
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
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


@AndroidEntryPoint
class CartFragment : Fragment(), MyCallback, DineInAdapter.DineInCallback {
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
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    var updateBundle: Bundle? = null
    var isFromPayment: Boolean = false
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private lateinit var nameObserver: Observer<List<CartModel>>
    private lateinit var dineInCartAdapter: DineInAdapter
    private var assignCustomer: TbCustomer? = null
    private var openORderType: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear_cart ->
                // Not implemented here
                clearCart()
        }
        return false
    }

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

    private fun displayCustomer() {

        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        Log.e("Customer Name", name)
        if (name.isNotEmpty() && name!=null) {
            binding.txtAddCustomer.text = name
        } else {
            binding.txtAddCustomer.text = getString(R.string.add_customer2)
        }

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
      //  getCartList()


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
                Log.e("ORDER_TYPE", "Updated check")
            }
        } else {
            isOrderUpdate = false
            prefProvider.setValue(ORDER_TYPE, TAKEOUT)
            Log.e("ORDER_TYPE", "Updated check1")
        }

        if (isOrderUpdate) {
            binding.tvSave.text = getString(R.string.update)
        } else {
            binding.tvSave.text = getString(R.string.save)
        }

    }

    private fun addObserver() {

        Log.e("ORDER_TYPE", prefProvider.getValue(ORDER_TYPE, TAKEOUT))

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            if (it.isNotEmpty()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                it[it.size - 1].items?.toCollection(arrayListOf())
                    ?.let { it1 -> cartAdapter.setList(it1) }
                viewModel.itemCalculationCartModel(
                    it[0],
                    binding.txtTotal,
                    requireContext()
                )
                viewModel.setCartModel(it)
                binding.txtSubTotal.text = MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(viewModel.totalPrice)
                binding.txtDiscount.text = MethodUtils.roundOffAmount(viewModel.totalDiscount)
            } else {
                cartAdapter.clearList()
                binding.txtTotal.text = MethodUtils.roundOffAmount(0.0)
                binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.0)
                binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                binding.txtDiscount.text = MethodUtils.roundOffAmount(0.0)
                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(0.0)
                binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.0)
            }

            displayCustomer()
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

    fun initListeners() {

        binding.txtAddCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment)
        }

        binding.imgOrderMenu.setOnClickListener {

            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.cart_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_clear_cart -> {
                        clearCart()
                    }
                    R.id.menu_remove_customer -> {

                        if (cartlist.isNotEmpty() && cartlist[0].customer != null) {
                            cartlist[0].customer = null
                            viewModel.addCart(cartlist[0])
                        }
                        clearCustomer()
                        displayCustomer()
                    }
                    R.id.menu_discount -> {

                        val bundle = Bundle()
                        bundle.putBoolean("isOrderDiscount", true)
                        bundle.putDouble("totalPrice", viewModel.totalPrice)
                        if (cartlist.isNotEmpty()) {
                            bundle.putDouble("orderDiscountPrice", cartlist[0].discountPrice)
                            bundle.putString("orderDiscountType", cartlist[0].discountType)
                        }
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                            bundle
                        )

                    }
                    R.id.menu_note -> {
                        clearCart()
                    }
                }
                true
            }
            popupMenu.show()

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
                if (prefProvider.getValue(ORDER_TYPE, "") != Constants.DINE_IN) {

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

                        val totalAmountTobeSave =
                            if (viewModel.redeemLoyaltyInfo.isLoyaltyApplied == true) {
                                (viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0)
                            } else {
                                viewModel.totalPrice
                            }

                        cartList.openOrderType = openORderType
                        if (!isOrderUpdate)
                            cartList.customer = assignCustomer

                        val formatterdate = SimpleDateFormat("yyyy-MM-dd")
                        val formattertime = SimpleDateFormat("hh:mm a")
                        val date = Date()
                        future_delivery_date = formatterdate.format(date)
                        future_delivery_time = formattertime.format(date)

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

    private fun clearCart() {
        alert(
            getString(R.string.app_name),
            getString(R.string.delete_items_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here
                prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)

                clearCustomer()

                viewModel.deleteCart()


                if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                    prefProvider.setValue(ORDER_TYPE, "")
                    Log.e("ORDER_TYPE", "Updated check2")
                }

                prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)

                displayCustomer()

            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff here
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



    private fun getCartList() {
        Log.e("Loyalty", "getCartList called..")

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
        binding.rvCartList.adapter = cartAdapter
        //binding.layoutCart.rvCartDineIn.adapter = dineInCartAdapter


        nameObserver = Observer {

            bindData(it)

          //  removeObserver()
        }


        if (isAdded)
            addObserver()
    }

    private fun removeObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""), prefProvider.getValueInt(
                Constants.EMPLOYEE_ID, 0
            )
        ).removeObserver(nameObserver)
        //  addObserver()
    }

    private fun bindData(it: List<CartModel>?) {
        Log.e("bindData", "YesAdded")

        cartlist = it as ArrayList<CartModel>
        viewModel.destroyedList.clear()

        if (cartlist.isNotEmpty()) {
            cartlist[0].items?.filter { item -> item.isDestroy }?.let {
                viewModel.destroyedList.addAll(it)
            }

            refreshOrderTypeLabel()

            if (prefProvider.getValue(ORDER_TYPE, "") == Constants.DINE_IN) {
                binding.rvCartList.visibility = View.GONE
                //binding.layoutCart.rvCartDineIn.visibility = View.VISIBLE
                //binding.layoutCart.llPayment.visibility = View.VISIBLE


                if (cartlist[0].items?.isNotEmpty() == true) {
                    val dineList = cartlist[0].dineInList ?: dineInCartAdapter.getList()

                    // mannual sale added in dineinn //yash
                    cartlist[0].items?.forEach {

                        if (it.isManualSales && dineList.isNotEmpty()) {

                            if (it.timeStamp == null || it.timeStamp?.lowercase() == "null".lowercase()) {
                                it.timeStamp = viewModel.randomOfflineId()
                            }
                            dineList.get(0).selectedPosition =
                                prefProvider.getValueInt(Constants.DINE_INGUEST_SELECTED, 0)
                            dineList[prefProvider.getValueInt(
                                Constants.DINE_INGUEST_SELECTED,
                                0
                            )].items.add(it)
                            Log.d(
                                "yash",
                                "bindData: dineine HEaderPositonn " + prefProvider.getValueInt(
                                    Constants.DINE_INGUEST_SELECTED,
                                    0
                                )
                            )

                        }
                        cartlist[0].items?.toCollection(arrayListOf())?.clear()
                        cartlist[0].items = listOf()

                    }

                    viewModel.cartLogic(cartlist, null, Constants.ADD, dineInList = dineList)

                }

                val list1 = cartlist.get(0).dineInList
                if (isAdded) {

                    setFragmentResultListener("request_key_customer_dine_in") { _, bundle ->
                        val result = bundle.getParcelable<TbCustomer>("data")
                        if (result != null) {


                            if (list1?.isNotEmpty() == true) {

                                var position = bundle.getInt("position")

                                val dineInList = list1
                                if (dineInList.size >= position && position != 0) {


                                    dineInList.get(position).customer = result

                                    Log.e(TAG, "UpdateCustomerPostition ${position}")
                                    Log.e(
                                        TAG,
                                        "UpdateCustomer ${dineInList.get(position).customer}"
                                    )

                                    viewModel.dineInCartUpdate(
                                        cartlist,
                                        dineInList
                                    )
                                }
                            }
                        }
                    }
                }


            } else {


                binding.rvCartList.visibility = View.VISIBLE
                // binding.layoutCart.rvCartDineIn.visibility = View.GONE
                if (cartlist[0].items?.isEmpty() == true) {
                    // binding.layoutCart.llPayment.gone()
                } else
                    //binding.layoutCart.llPayment.visible()

                Log.e(TAG, "cartList[0].items > ${cartlist[0].items?.size}")
                Log.e(TAG, "viewModel.destroyedList > ${viewModel.destroyedList.size}")
                cartAdapter.addCart(cartlist[0].items)
            }
            viewModel.itemCalculation(
                cartlist,
                binding.txtTotal,
                requireContext()
            )

            val orderType = prefProvider.getValue(ORDER_TYPE, "")
            Log.e("!_@_", "rlSave -------- $orderType ")
            if (orderType == TAKEOUT || orderType == Constants.DINE_IN) {
                Log.e("!_@_", "rlSave -- GONE ")
                binding.tvSave.visibility = View.GONE
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {
                    binding.txtTotal.visibility = View.GONE
                    binding.tvPayNow.visibility = View.GONE
                   // binding.layoutCart.txtDineInProceed.visibility = View.VISIBLE
                    if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false) == true) {

                       // binding.layoutCart.txtDineInProceed.setText("Update and Proceed")
                    } else {
                       // binding.layoutCart.txtDineInProceed.setText("Proceed To Fire")
                    }
                } else {
                    //binding.layoutCart.txtDineInProceed.visibility = View.GONE
                    binding.rvCartList.visible()
                    if (cartlist[0].items?.isEmpty() == true) {
                        binding.tvPayNow.gone()
                    } else
                        binding.tvPayNow.visible()
                    //  binding.layoutCart.llPayment.visible()
                }
            } else {
                Log.e("!_@_", "rlSave -- VISIBLE ")
                binding.tvSave.visibility = View.VISIBLE
                binding.rvCartList.visible()
                //  binding.layoutCart.llPayment.visible()
                if (cartlist[0].items?.isEmpty() == true) {
                    binding.tvPayNow.gone()
                } else
                    binding.tvPayNow.visible()
            }


        } else {

            viewModel.itemCalculation(
                cartlist,
                binding.tvPayNow,
                requireContext()
            )

            binding.rvCartList.gone()
            binding.tvPayNow.gone()


        }


        if (prefProvider.getValueInt("ORDER_ID", -1) != -1) {
            Log.e(TAG, "ManualSale ORderIDNOt Null")
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString() != Constants.DINE_IN) {
                lifecycleScope.launchWhenResumed {
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                        val bundle = bundleOf(Constants.IS_NEXT_AMOUNT to true)

                        /*  findNavController().navigate(
                              R.id.action_dashboardCategoryNew_to_paymentFragment, bundle
                          )*/
                    }
                }
            }

            gotoPayment()
        }
    }
    private fun refreshOrderTypeLabel() {
        //set order type label
        var label = prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString()
        if (label.equals(Constants.OPEN_ORDER, true) || label.equals(Constants.OPEN_ORDER_, true)) {
            label = Constants.OPEN_ORDER_
        }
        Log.e(TAG, "OrderType Label : $label")
        //binding.layoutCart.txtOrderType.text = label
    }


    private fun gotoPayment() {
        Log.e(TAG, "cartList:  ${cartlist.size}")
        if (cartlist.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false)) {
                var itemCount = 0
                for (i in cartlist.indices) {
                    for (j in cartlist[i].dineInList?.indices!!) {
                        if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                            itemCount++
                            break
                        }
                    }
                    if (itemCount != 0) {
                        break
                    }

                }

                if (itemCount == 0) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        getString(R.string.please_add_Atleast_one_item_in_cart)
                    ) { _, _ ->
                    }
                } else {
                    val request = viewModel.updateOrder(cartlist[0])

                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    prefProvider.setValueboolean(Constants.DINE_IN_LIST_EDIT, false)
                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    cartlist[0].orderId?.let { viewModel.updateOrderCall(it, request) }


                }

            } else {

                val bundle = Bundle()

                var final_total = (binding.txtTotal.text.toString().subSequence(
                    2,
                    binding.txtTotal.text.length
                ) as String).toDouble()
                bundle.putDouble("totalPrice", final_total)
                bundle.putParcelable(
                    "redeemLoyalty",
                    viewModel.redeemLoyaltyInfo
                )
                bundle.putDouble(
                    "cashDiscountSurcharge",
                    MethodUtils.calculateCashDiscount(final_total, prefProvider, requireContext())
                )

                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    prefProvider.setValue(
                        "cashDiscountSurCharge",
                        MethodUtils.calculateCashDiscount(
                            final_total,
                            prefProvider,
                            requireContext()
                        ).toString()
                    )
                }
                bundle.putDouble(
                    "subTotalPrice",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.subTotalPrice
                )
                bundle.putDouble(
                    "totalTax",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.totalTax
                )
                bundle.putDouble(
                    "totalDiscount",
                    viewModel.totalDiscount + cartlist[0].discountPrice
                )
                bundle.putDouble(
                    "totalServiceCharge",
                    if (viewModel.subTotalPrice < 0) 0.0 else viewModel.totalServiceCharge
                )
                bundle.putString("future_delivery_date", future_delivery_date)
                bundle.putString("future_delivery_time", future_delivery_time)
                cartlist[0].customer = assignCustomer
                val cartModel = viewModel.generateCombinedItems(cartlist[0])
               // Log.e(TAG, "openORderType  ${openORderType}")
                cartModel.openOrderType = openORderType
                Log.e(TAG, "PaymentPAsscartModel: ${Gson().toJson(cartModel)}")
                bundle.putParcelable("cartList", cartModel)
                if (isOrderUpdate) {
                    bundle.putBoolean("update", true)
                    orderId?.let { bundle.putInt("orderId", it) }
                    paymentId?.let { bundle.putInt("paymentId", it) }
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                }
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {
                    var itemCount = 0
                    for (i in cartlist.indices) {
                        for (j in cartlist[i].dineInList?.indices!!) {
                            if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                                itemCount++
                                break
                            }
                        }
                        if (itemCount != 0) {
                            //createDineInRequest()
                            break
                        }
                    }

                    if (itemCount == 0) {
                        bundle.clear()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            getString(R.string.please_add_Atleast_one_item_in_cart)
                        ) { _, _ ->
                        }

                    }
                } else {
                    prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, final_total.toString())
                    prefProvider.setValue(
                        Constants.SUB_TOTAL_ACTUAL,
                        viewModel.subTotalPrice.toString()
                    )
                    prefProvider.setValue(
                        Constants.TOTAL_DISCOUNT_ACTUAL,
                        viewModel.totalDiscount.toString()
                    )
                    prefProvider.setValue(
                        Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
                        viewModel.totalServiceCharge.toString()
                    )
                    prefProvider.setValue(
                        Constants.TAX_CHARGE_ACTUAL,
                        viewModel.totalTax.toString()
                    )
                    prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")



                    lifecycleScope.launchWhenStarted {
                        if (findNavController().currentDestination?.id == R.id.dashboardCategoryNew) {
                            if (prefProvider.getValueInt("ORDER_ID", -1) != -1) {
                                bundle.putBoolean(Constants.IS_NEXT_AMOUNT, true)
                            }
                            findNavController().navigate(
                                R.id.action_dashboardCategoryNew_to_paymentFragment,
                                bundle
                            )
                        }
                    }
                }
            }
        } else if (cartlist.isEmpty() && prefProvider.getValueInt("ORDER_ID", -1) != -1) {

            val bundle = bundleOf(Constants.IS_NEXT_AMOUNT to true)
            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_paymentFragment,
                bundle
            )

        }

    }




    override fun onPause() {
        super.onPause()
        ProgressUtils.dismissProgressDialog()
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {

    }

    override fun onHeaderSelected(position: Int) {

    }

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {
    }

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {
    }

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {
    }


}