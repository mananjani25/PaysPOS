package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.MANUALSALE
import com.android.pos.data.remote.Constants.MANUAL_SALE
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.WHOLE_AMOUNT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Resource
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


@AndroidEntryPoint
class CartFragment(val itemClickListner:ItemClickListner?) : Fragment(), MyCallback, DineInAdapter.DineInCallback {
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
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()
    private var serviceChargesList: List<TbServiceCharge>? = null

    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null

    var cashDiscountSurcharge = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"


    companion object {
        @JvmStatic
        fun newInstacne(isFromPayment: Boolean)=
            CartFragment(null).apply {
                arguments=Bundle().apply {
                    putBoolean("isFromPayment", isFromPayment)
                }
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
        setCartAdapter()

        if (arguments?.getBundle("updateBundle") != null) {
            updateBundle = arguments?.getBundle("updateBundle")
        }

        if (arguments?.getBoolean("isFromDineIn") == true) {
            Log.e(TAG, "YesIsFromDineIn")
            // getDineInCartList()


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
        if (name.isNotEmpty() && name != null) {
            binding.txtAddCustomer.text = name
        } else {
            binding.txtAddCustomer.text = getString(R.string.add_customer2)
        }

    }

    private fun setUpData() {
        if (isFromPayment) {
            binding.linearButtonView.visibility = View.GONE
            binding.relPreoceedToFire.visibility = View.GONE
            binding.imgOrderMenu.visibility = View.INVISIBLE
            binding.imgOrderMenu.isEnabled = false
            binding.imgOrderMenu.isClickable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        setCartAdapter()
        getDineInData()
        callback()
        setupLoyalytyPoints()
        //getDineInData()
        addObserver()



        if (prefProvider.getValueInt(Constants.CUSTOMER_ID, -1) != -1) {
            displayCustomer()
        }
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

                viewModelPayment.updateOrder(
                    isOrderUpdate,
                    orderId,
                    paymentId,
                    paymentOfflineId,
                    orderOfflineId
                )

            } else {
                //  prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                Log.e("ORDER_TYPE", "Updated check")
            }
        } else {
            isOrderUpdate = false
            //   prefProvider.setValue(ORDER_TYPE, TAKEOUT)
            Log.e("ORDER_TYPE", "Updated check1")
        }

        if (isOrderUpdate) {
            binding.tvSave.text = getString(R.string.update)
        } else {
            binding.tvSave.text = getString(R.string.save)
        }

    }

    private fun setupLoyalytyPoints() {
        binding.checkloylaty.setOnCheckedChangeListener { _, p1 ->
            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = p1
            prefProvider.setValueboolean(Constants.LOYALTY_ADDED, p1)
            addObserver()
        }

    }

    private fun callback() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_customer",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var data: TbCustomer = bundle.getParcelable<TbCustomer>("data") as TbCustomer
            viewModel.assignCustomer = data
            viewModel.selectedCustomer = data
        }
    }

    private fun removeObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).removeObserver(nameObserver)
        //  addObserver()
    }

    private fun getDineInData() {
        if (updateBundle != null) {
            if (updateBundle?.getBoolean("isFromDineIn") == true) {
                getDineInCartList()
            } else if (updateBundle?.getBoolean("is_dine_in_edit") == true) {
                checkDineInEditOrder()
            }

        }

    }

    private fun getDineInCartList() {
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)

        binding.rvCartList.adapter = dineInCartAdapter
        val numOfGuest: Int by lazy {
            requireArguments().getInt("numberOfGuest")
        }

        dineInFloorTableModel = arguments?.getParcelable("floorplan")


        var orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? =
            arguments?.getParcelable("tableDetails")
        Log.e(TAG, "orderFloorDetails:  ${Gson().toJson(orderDEtails)}")
        if (orderDEtails != null) {
            orderFloorDetails = orderDEtails
        }
        if (orderFloorDetails.id == null) {
            orderFloorDetails.apply {
                id = dineInFloorTableModel?.id
                chairCount = dineInFloorTableModel?.chairCount
                floorPlanId = dineInFloorTableModel?.floorPlanId
                tableName = dineInFloorTableModel?.tableName.toString()
                status = dineInFloorTableModel?.status.toString()


            }

        }


        val dineInList: ArrayList<DineInModel> = arrayListOf()
        dineInList.add(DineInModel(0, true, 0, "Whole Table", floorPlanTable = orderFloorDetails))
        for (i in 1..numOfGuest) {
            dineInList.add(
                DineInModel(
                    0,
                    false,
                    0,
                    "Guest $i",
                    floorPlanTable = orderFloorDetails

                )
            )
        }



        dineInCartAdapter.setList(dineInList)

        if (cartlist.isEmpty()) {
            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
                orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
                orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()

                serviceCharge = serviceChargesList

            }
            cartlist.add(cartModel)
        }

        cartlist.get(0).orderType = Constants.DINE_IN
        viewModel.cartLogic(cartlist, null, Constants.ADD,false, dineInList = dineInList)

    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            var dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")

            if (dineInList?.isNotEmpty() == true) {
                //  binding.layoutCart.txtOrderType.setText("Dine In")

                dineInCartAdapter = DineInAdapter()
                dineInCartAdapter.setListner(this)
                //dineInCartAdapter.setList(dineInList)


                if (cartlist.isEmpty()) {
                    val cartModel = CartModel().apply {
                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                        employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                        locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                        orderTypeId = 2
                        orderType = Constants.DINE_IN
                        orderTypeName = Constants.DINE_IN

                        serviceCharge = serviceChargesList
                        orderId = arguments?.getInt("orderId")

                    }
                    cartlist.add(cartModel)
                }

                var orderTableData: GetOrderDetailsResponse.Data.FloorPlanTable? =
                    arguments?.getParcelable("tableDetails")

                dineInList.forEach {
                    it.floorPlanTable = orderTableData
                }

                prefProvider.setValue(ORDER_TYPE, Constants.DINE_IN)
                prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.DINE_IN)
                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, 2)

                viewModel.cartLogic(cartlist, null, Constants.ADD,false, dineInList = dineInList)
                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0

            }


        }
    }

    private fun addDineInObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(
            requireActivity(), nameObserver
        )

    }

    private fun addObserver() {

        Log.e("ORDER_TYPE", prefProvider.getValue(ORDER_TYPE, TAKEOUT))

        if (arguments?.getString(REDIRECT_FROM)== MANUAL_SALE){
            viewModel.manualSaleItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                if (it.isNotEmpty()) {
                    it[0].items?.toCollection(arrayListOf())
                        ?.let { it1 -> cartAdapter.setList(it1) }
        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            Log.e(TAG, "listSize  ${Gson().toJson(it)}")
            Log.e(TAG, "getOrderType:  ${prefProvider.getValue(ORDER_TYPE, TAKEOUT)}")
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                if (it.isNotEmpty()) {
                    binding.rvCartList.adapter = dineInCartAdapter
                    cartlist = it as ArrayList<CartModel>
                    binding.linearButtonView.gone()
                    binding.relPreoceedToFire.visible()

                    if (it[0].dineInList?.isNotEmpty() == true) {
                        var dineInList = it[0].dineInList

                        dineInCartAdapter.setList(
                            dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                        )


                    }

                    cartlist = it as ArrayList<CartModel>
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
                    Log.e("totalDiscount", viewModel.totalDiscount.toString())
                    binding.txtDiscount.text = MethodUtils.roundOffAmount(viewModel.totalDiscount)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                } else {
                    cartAdapter.clearList()
                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtDiscount.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.0)
                }
            }

        }
        else{
            viewModel.mAllWords(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                } else {
                    binding.linearButtonView.visible()
                    binding.relPreoceedToFire.gone()
                    dineInCartAdapter.clearList()

                }
            } else {
                if (it.isNotEmpty()) {
                    binding.linearButtonView.visible()
                    binding.relPreoceedToFire.gone()
                    binding.rvCartList.adapter = cartAdapter
                    it[0].items?.toCollection(arrayListOf())
                        ?.let { it1 -> cartAdapter.setList(it1) }

                    cartlist = it as ArrayList<CartModel>
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
                    Log.e("totalDiscount", viewModel.totalDiscount.toString())
                    binding.txtDiscount.text = MethodUtils.roundOffAmount(viewModel.totalDiscount)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                } else {
                    cartAdapter.clearList()
                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtDiscount.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.0)
                }
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

    }

    private fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        viewModel.selectedCustomer = null
        viewModel.assignCustomer = null
        binding.liinearInfoLayout.layoutParams.height =
            resources.getDimension(R.dimen._40sdp).toInt()
        binding.relativeLoylatyPoints.visibility = View.GONE
        binding.lblLoyaltyPoints.visibility = View.GONE
        displayCustomer()
        refreshItemCalculation()
    }


    private fun refreshItemCalculation() {
        if (cartlist.size > 0) {
            viewModel.itemCalculationCartModel(
                cartlist[0],
                binding.txtTotal,
                requireContext()
            )
        }

    }

    private fun initListeners() {

        binding.txtAddCustomer.setOnClickListener {
            if (isFromPayment) {
                findNavController().navigate(R.id.action_paymentBoldPosFragment_to_assignCustomerOrderFragment)
            } else {
                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment)
            }
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
                    /*R.id.menu_note -> {

                    }*/
                }
                true
            }
            popupMenu.show()

        }

        binding.tvPayNow.setOnClickListener {
            if (cartAdapter.cartList.isNotEmpty()) {
                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, TAKEOUT))
                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")
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

                prefProvider.setValue(ORDER_TYPE, OPEN_ORDER)
                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")
                if (prefProvider.getValue(ORDER_TYPE, "") != Constants.DINE_IN) {

                    var ordertype = ""
                    var ordertypeId = 0
                    viewModel.ordertypelist.forEach {
                        if (it.orderType == OPEN_ORDER) {
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

                        cartList.openOrderType = Constants.PICK_UP
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
                            OPEN_ORDER,
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
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {
                    Log.e(TAG, "DineInClearTable")


                    val dList = cartlist[0].dineInList ?: arrayListOf()
                    Log.e(TAG, "dList:  ${Gson().toJson(dList)}")
                    if (dList.isNotEmpty()) {
                        dList[0].floorPlanTable?.id?.let {

                            if (dList[0]?.floorPlanTable?.status.toString() == Constants.MERGED) {
                                viewModel.getTableStatus(it, Constants.MERGED)
                            } else {
                                viewModel.getTableStatus(
                                    it, "Available"
                                )
                            }
                        }
                    }
                    viewModel.deleteCart()
                    isOrderUpdate = false
                    dineInCartAdapter.clearList()

                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)


                    clearCustomer()
                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    clearUpdateFlag()
                    binding.linearButtonView.visible()
                    binding.relPreoceedToFire.gone()


                } else {
                    clearCustomer()
                    viewModel.deleteCart()
                    cartlist.clear()
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)


                }



                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {

                    val dList = dineInCartAdapter.getList()
                    if (dList.isNotEmpty()) {
                        dList[0].floorPlanTable?.id?.let {

                            if (dList[1]?.floorPlanTable?.status.toString() == Constants.MERGED) {
                                viewModel.getTableStatus(it, Constants.MERGED)
                            } else {
                                viewModel.getTableStatus(
                                    it, "Available"
                                )
                            }
                        }
                    }
                    viewModel.deleteCart()
                    isOrderUpdate = false

                    if (prefProvider.getValue(ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                    }
                    val dineInList = ArrayList<DineInModel>()
                    dineInCartAdapter.setList(dineInList)
                    dineInCartAdapter.notifyDataSetChanged()
                    binding.tvPayNow.text = "Pay"
                    clearCustomer()

                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)

                } else {
                    clearCustomer()
                    viewModel.deleteCart()
                    cartlist.clear()
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
                }
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



    private fun setCartAdapter() {
        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
    }


    override fun onPause() {
        super.onPause()
        ProgressUtils.dismissProgressDialog()
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {
        Log.e(TAG, "itemClicked  ${Gson().toJson(data)}")
        itemClickListner?.onItemUpdate(data)


    }

    override fun onHeaderSelected(position: Int) {
        prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, position)
        Log.d(TAG, "onHeaderSelected: header position : $position")
    }

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {
    }

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {
        if (isRemoved) {
            if (cartlist.get(0).dineInList?.size!! >= position) {
                val dineIn = cartlist.get(0).dineInList
                dineIn?.get(position)?.customer = null
                viewModel.dineInCartUpdate(cartlist, dineIn!!)
            }

        } else {

            val bundle = bundleOf("DINE_IN" to true, "position" to position)
            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment, bundle
            )
        }


    }


    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {

        alert(
            getString(R.string.app_name),
            getString(R.string.delete_item_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here
                cartlist.get(0).orderType = Constants.DINE_IN

                viewModel.cartLogic(
                    cartlist,
                    data,
                    Constants.DELETE,false,
                    dineInList = dineInCartAdapter.getList()
                )


            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff heref
            }
        }
    }



    private fun clearUpdateFlag() {
        isOrderUpdate = false
        prefProvider.setValueboolean(Constants.IS_ORDER_UPDATE, value = false)
        requireArguments().remove("update")

    }


}