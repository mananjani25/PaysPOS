package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CUSTOMER_ID
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_LIST_EDIT
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE_LIST
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_ID
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_OFFLINE_ID
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_PAYMENT_ID
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID
import com.android.pos.data.remote.Constants.LOYALTY_ADDED
import com.android.pos.data.remote.Constants.MANUAL_SALE
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.OPTION_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.WHOLE_AMOUNT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.checkout.CheckoutDineInPaymentViewModel
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
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
class CartFragment(
    val itemClickListner: ItemClickListner?,
    val itemListner: ItemListner?,
    val isFromPaymentDinein: Boolean = false,
    val guestCalModel: GuestPaymentCalculationModel? = null,
    val isGuestPayment: Boolean = false,
) :
    Fragment(), MyCallback,
    DineInAdapter.DineInCallback {
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
    private val dineInPayViewModel by activityViewModels<CheckoutDineInPaymentViewModel>()
    var updateBundle: Bundle? = null
    var isFromPayment: Boolean = false
    var isActiveOrder: Boolean = false
    var reorder: Boolean = false

    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private lateinit var nameObserver: Observer<List<CartModel>>
    private lateinit var dineInCartAdapter: DineInAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private var assignCustomer: TbCustomer? = null
    private var openORderType: String = ""
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()
    private var serviceChargesList: List<TbServiceCharge>? = null

    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null

    var taxClickable = false
    var cashDiscountSurcharge = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"


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

        if (arguments?.getBoolean("reorder") != null) {
            reorder = arguments?.getBoolean("reorder")!!
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
        isActiveOrder = arguments?.getBoolean("isFromActiveOrder") ?: false


        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) { it ->
                if (it.getBundle("updateBundle") != null) {
                    updateBundle = it.getBundle("updateBundle")
                    isOrderUpdate = updateBundle?.getBoolean("update") ?: false
                    Log.e(TAG, "isOrderUpdateReq:  $isOrderUpdate")
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
                        updateActiveOrderFlag()
                    }
                    uiSave()

                }

            }




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
            binding.imgOrderMenu.visibility = View.GONE
            val params: RelativeLayout.LayoutParams =
                binding.txtAddCustomer.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
            binding.txtAddCustomer.layoutParams = params
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
        addObserver()
        setupTaxAdapter()


        if (taxBirfurcationAdapter.taxlist.size == 0) {
            binding.imgDropdown.gone()
        } else {
            binding.imgDropdown.visible()
        }
        binding.linearTaxDetail.setOnClickListener {
            if (taxBirfurcationAdapter.taxlist.size > 0) {
                if (!taxClickable) {
                    Log.d(TAG, "onViewCreated: " + taxBirfurcationAdapter.taxlist.size)
                    taxClickable = true
                    if (taxBirfurcationAdapter.taxlist.size == 1) {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._60sdp).toInt()
                        }
                    } else if (taxBirfurcationAdapter.taxlist.size == 2) {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._80sdp).toInt()
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                        }
                    } else {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._100sdp).toInt()
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._95sdp).toInt()
                        }

                    }
                    binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                    binding.relativeDynamicTax.visible()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._50sdp).toInt()
                    taxClickable = false
                    binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.relativeDynamicTax.gone()
                }
            }

        }


        if (prefProvider.getValueInt(Constants.CUSTOMER_ID, -1) != -1) {
            displayCustomer()
        }
        //  getCartList()

        if (updateBundle != null) {
            isOrderUpdate = requireArguments().getBoolean("update")
            Log.e(TAG, "isOrderUpdateReq:  $isOrderUpdate")
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

                updateActiveOrderFlag()


            }
        } else {
            isOrderUpdate = false
            if (isFromPayment) {
                if (isActiveOrder) {
                    viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                        arguments?.getBoolean("isLoyaltyApplied") ?: false
                } else {

                    if (prefProvider.getValueboolean(IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)) {
                        isActiveOrder = true
                        viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                            prefProvider.getValueboolean(IS_UPDATE_ORDER_LOYALTY_APPLIED, false)

                    }
                }
            }
            //   prefProvider.setValue(ORDER_TYPE, TAKEOUT)
            Log.e("ORDER_TYPE", "Updated check1")
        }

        uiSave()

    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        var taxlist = arrayListOf<TaxData>()
        taxBirfurcationAdapter.setList(taxlist)
    }

    private fun updateActiveOrderFlag() {
        if (prefProvider.getValueboolean(IS_UPDATE_ORDER, false)) {
            isOrderUpdate = true
            orderId = prefProvider.getValueInt(IS_UPDATE_ORDER_ID, -1)
            paymentId = prefProvider.getValueInt(IS_UPDATE_ORDER_PAYMENT_ID, -1)
            paymentOfflineId = prefProvider.getValue(IS_UPDATE_ORDER_PAY_OFFLINE_ID, "")
            orderOfflineId = prefProvider.getValue(IS_UPDATE_ORDER_OFFLINE_ID, "")

            viewModelPayment.updateOrder(
                isOrderUpdate,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )


            viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
                prefProvider.getValueboolean(IS_UPDATE_ORDER_LOYALTY_APPLIED, false)

            if (prefProvider.getValueboolean(IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)) {
                isActiveOrder = true
            }

        }
    }

    private fun uiSave() {

        if (isOrderUpdate) {
            binding.tvSave.text = getString(R.string.update)
        } else {
            binding.tvSave.text = getString(R.string.save)
        }
    }

    private fun setupLoyalytyPoints() {
        binding.checkloylaty.setOnCheckedChangeListener { _, p1 ->
            viewModel.setcheckedLoyaltyApply(p1)
//            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = p1
            prefProvider.setValueboolean(Constants.LOYALTY_ADDED, p1)
            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, p1)
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
            viewModel.setcheckedLoyaltyApply(false)
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
                Log.e(TAG, "isFromDinein")
                getDineInCartList()
            } else if (updateBundle?.getBoolean("is_dine_in_edit") == true) {
                checkDineInEditOrder()
            }

        }

    }

    fun checkMaxGuestCountId(): Int {
        var maxValue = 0
        var serviceChargeId = 0
        viewModel.serviceChargesList.forEach { serviceCharge ->
            if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                if (serviceCharge.max_guest_count!! >= maxValue) {
                    maxValue = serviceCharge.max_guest_count
                    serviceChargeId = serviceCharge.id
                }
            }
        }
        return serviceChargeId
    }

    fun getServiceChargeFromGuestCount(guestcount: Int): List<TbServiceCharge> {
        var list: List<TbServiceCharge> = listOf()
        var isApplied = false
        viewModel.serviceChargesList.forEach {
            if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                if (isInRange(it.min_guest_count!!, it.max_guest_count!!, guestcount)) {
                    isApplied = true
                    list = listOf(it)
                }
            }
        }
        if (!isApplied) {
            viewModel.serviceChargesList.forEach { service ->
                if (service.id == checkMaxGuestCountId()) {
                    list = listOf(service)
                    return@forEach
                }
            }
        }
        return list
    }

    fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
        return (minn <= value && value <= maxx)
    }

    private fun getDineInCartList() {

        binding.rvCartDineIn.visible()
        binding.rvCartList.gone()
        binding.rvCartDineIn.adapter = dineInCartAdapter
        val numOfGuest: Int by lazy {
            updateBundle!!.getInt("numberOfGuest")
        }

        dineInFloorTableModel = arguments?.getParcelable("floorplan")


        val orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? =
            arguments?.getParcelable("tableDetails")
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
        dineInList.add(DineInModel(0, true, 0, "Whole Table"))
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

                serviceCharge = getServiceChargeFromGuestCount(numOfGuest)

            }
            cartlist.add(cartModel)
        }

        viewModel.dineInHeaderPosition = 0
        viewModel.dineInSelectedItemHeaderPos = 0
        cartlist.get(0).orderType = Constants.DINE_IN
        viewModel.cartLogic(cartlist, null, Constants.ADD, false, dineInList = dineInList)

    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            var dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")

            if (dineInList?.isNotEmpty() == true) {
                //  binding.layoutCart.txtOrderType.setText("Dine In")

                dineInCartAdapter = DineInAdapter()
                dineInCartAdapter.setListner(this)
                //dineInCartAdapter.setList(dineInList)


                dineInList.forEach { it ->
                    it.items.forEach { items ->
                        if (!items.isSelectedItem) {
                            viewModel.selectedItems(items.itemId, 1)
                        }
                    }
                }
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

                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
                Log.e(TAG, "DineInEditDiscount ${arguments?.getDouble("totalDiscount")}")
                viewModel.totalDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
                viewModel.cartLogic(cartlist, null, Constants.ADD, false, dineInList = dineInList)


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

    fun setTaxBifurcationData(taxlistData: ArrayList<TaxData>) {
        if (taxlistData?.isNotEmpty()) {
            Log.d(TAG, "addObserver: " + taxlistData.size)
            setupTaxAdapter()
            taxClickable = false
            if (viewModel.order_note.isNotEmpty()) {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._70sdp).toInt()
            } else {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.visible()
            taxBirfurcationAdapter.setList(taxlistData)
            binding.relativeDynamicTax.gone()
        } else {
            if (viewModel.order_note.isNotEmpty()) {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._60sdp).toInt()
            } else {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.gone()
            binding.relativeDynamicTax.gone()
            binding.liinearInfoLayout.layoutParams.height =
                resources.getDimension(R.dimen._50sdp).toInt()
            taxClickable = false
        }
    }

    fun reSetTaxBifurcationData() {
        taxBirfurcationAdapter.clearList()
        binding.liinearInfoLayout.layoutParams.height =
            resources.getDimension(R.dimen._50sdp).toInt()
        taxClickable = false
        binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.imgDropdown.gone()
        binding.relativeDynamicTax.gone()
    }

    private fun addObserver() {


        Log.e("ORDER_TYPE", prefProvider.getValue(ORDER_TYPE, TAKEOUT))

        if (arguments?.getString(REDIRECT_FROM) == MANUAL_SALE) {
            viewModel.manualSaleItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                if (it.isNotEmpty()) {
                    if (isFromPayment) {
                        if (MethodUtils.isEnableCashDiscount(requireContext())) {
                            binding.linearCashDiscount.visible()
                            if (prefProvider.getValue(
                                    OPTION_TYPE,
                                    "CashDiscount"
                                ) == "CashDiscount"
                            ) {
                                binding.labelCashSurcharge?.text = "Cash Discount"
                            } else {
                                binding.labelCashSurcharge?.text = "SurCharge"
                            }
                        } else {
                            binding.linearCashDiscount.gone()
                        }

                        binding.linearButtonView.gone()
                        binding.relPreoceedToFire.gone()
                    } else {
                        binding.linearButtonView.visible()
                        binding.relPreoceedToFire.gone()
                    }


                    binding.rvCartDineIn.gone()
                    binding.rvCartList.visible()

                    it[0].items?.toCollection(arrayListOf())
                        ?.let { it1 -> cartAdapter.setList(it1) }

                    cartlist = it as ArrayList<CartModel>

                    viewModel.setCartModel(it)
                    if (it[0].taxlistDynamic?.isNotEmpty() == true) {
                        Log.d(TAG, "addObserver: " + it[0].taxlistDynamic?.size)
                        setupTaxAdapter()
                        taxBirfurcationAdapter.setList(it[0].taxlistDynamic as ArrayList<TaxData>)
                    }
                    if (viewModel.order_note.isNotEmpty()) {
                        binding.relativeOrderNotes?.visibility = View.VISIBLE
                        binding.txtOrderNote?.text = viewModel.order_note
                    } else {
                        binding.relativeOrderNotes?.visibility = View.GONE
                    }

                    binding.txtSubTotal.text =
                        MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                    binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                    binding.tvPayNow.text = "Pay " + binding.txtTotal.text.toString()
                    Log.e("totalDiscount", viewModel.totalDiscount.toString())
                    binding.txtDiscount.text = "-" +
                            MethodUtils.roundOffAmount(viewModel.totalDiscount)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                    var data: TbCustomer? = prefProvider.getCustomerData()
                    if (data != null) {
                        if (viewModel.loyaltyPointCondition(data)) {
                            if (isFromPayment) {
                                if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                                    binding.liinearInfoLayout.layoutParams.height =
                                        resources.getDimension(R.dimen._70sdp).toInt()
                                    binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                    binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                    binding.txtLabelLoyaltyAmounts.visibility = View.VISIBLE
                                    binding.checkloylaty.visibility = View.GONE
                                    binding.txtLoyaltyAmount.text =
                                        "- $${
                                            String.format(
                                                "%.2f",
                                                viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                            )
                                        }"
                                    binding.txtLoyaltyPoints.text =
                                        "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                } else {
                                    binding.liinearInfoLayout.layoutParams.height =
                                        resources.getDimension(R.dimen._50sdp).toInt()
                                    binding.relativeLoylatyPoints.visibility = View.GONE
                                    binding.lblLoyaltyPoints.visibility = View.GONE
                                }
                            } else {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._70sdp).toInt()
                                binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                Log.e(TAG, "InsideLoyalty")
                                Log.e(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
                                binding.txtLoyaltyAmount.text =
                                    "- $${
                                        String.format(
                                            "%.2f",
                                            viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                        )
                                    }"
                                binding.txtLoyaltyPoints.text =
                                    "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                binding.checkloylaty.isChecked =
                                    viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                            }
                        }
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._50sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.GONE
                        binding.lblLoyaltyPoints.visibility = View.GONE
                    }

                    viewModel.itemCalculation(it, binding.txtTotal, requireContext())
                    setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)


                } else {
                    viewModel.clearListTax()
                    cartAdapter.clearList()
                    reSetTaxBifurcationData()
                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                    binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(0.0)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.relativeOrderNotes?.visibility = View.GONE
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(0.0)
                    binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.0)
                    var data: TbCustomer? = prefProvider.getCustomerData()
                    if (data != null) {
                        if (viewModel.loyaltyPointCondition(data)) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.VISIBLE
                            binding.lblLoyaltyPoints.visibility = View.VISIBLE
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._50sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.GONE
                            binding.lblLoyaltyPoints.visibility = View.GONE
                        }
                    }


                }
            }

        } else {
            if (view != null) {
                viewModel.mAllWords(
                    prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                ).observe(requireActivity()) {
                    Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                    Log.e(TAG, "listSizeOrderType: ${prefProvider.getValue(ORDER_TYPE, TAKEOUT)}")

                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                        binding.rvCartDineIn.visible()
                        binding.rvCartList.gone()
                        if (it.isNotEmpty()) {
                            Log.e(TAG, "cartListDine:  ${Gson().toJson(it)}")

                            cartlist = it as ArrayList<CartModel>
                            if (isFromPayment) {
                                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                                    binding.linearCashDiscount.visible()
                                    if (prefProvider.getValue(
                                            OPTION_TYPE,
                                            "CashDiscount"
                                        ) == "CashDiscount"
                                    ) {
                                        binding.labelCashSurcharge?.text = "Cash Discount"
                                    } else {
                                        binding.labelCashSurcharge?.text = "SurCharge"
                                    }
                                } else {
                                    binding.linearCashDiscount.gone()
                                }
                                binding.linearButtonView.gone()
                                binding.relPreoceedToFire.gone()
                            } else {
                                binding.linearButtonView.gone()
                                binding.relPreoceedToFire.visible()
                            }
                            if (it[0].dineInList?.isNotEmpty() == true) {
                                var dineInList = it[0].dineInList

                                dineInCartAdapter.setList(
                                    dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                                )


                            }
                            Log.e(TAG, "getPAyment:  ${isFromPayment}")
                            Log.e(TAG, "isGuestPayment:  ${isGuestPayment}")

                            if (isFromPaymentDinein) {
                                Log.e(TAG, "guestCalModel:  ${Gson().toJson(guestCalModel)}")
                                viewModel.itemCalculationForDineInPayment(
                                    it[0],
                                    binding.txtTotal,
                                    requireContext(),
                                    guestCalModel!!,
                                    isGuestPayment
                                )
                            } else {
                                Log.e(TAG, "WithOutDineIn")
                                viewModel.itemCalculationCartModel(
                                    it[0],
                                    binding.txtTotal,
                                    requireContext()
                                )
                            }
                            /*  viewModel.itemCalculationCartModel(
                              it[0],
                              binding.txtTotal,
                              requireContext()
                          )*/


                            viewModel.setCartModel(it)
                            if (prefProvider.getValueboolean(
                                    Constants.DINE_IN_UPDATE,
                                    false
                                )
                            ) {
                                binding.txtDineInProceed.setText("Update and Proceed")

                                var getOldList = prefProvider.getValue(DINE_IN_UPDATE_LIST, "")
                                Log.e(TAG, "getOldList  ${Gson().toJson(getOldList)}")
                                if (getOldList.isEmpty()) {
                                    var listItemDine: ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                                        arrayListOf()
                                    var data = it[0].dineInList
                                    Log.e(TAG, "getDataSizeDin ${data?.size}")
                                    data?.forEach {
                                        it.items.forEach { item ->
                                            var modifiers: ArrayList<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier> =
                                                arrayListOf()
                                            if (item.modifiers.isNotEmpty()) {
                                                item.modifiers.forEach {
                                                    var modelMod =
                                                        GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier(
                                                            categoryId = "",
                                                            id = it.id ?: 0,
                                                            isModifier = it.isChecked,
                                                            itemId = "",
                                                            modifierId = "",
                                                            name = it.name,
                                                            orderId = 0,
                                                            orderItemId = 0,
                                                            orderItemTaxes = arrayListOf(),
                                                            price = it.price,
                                                            quantity = it.itemQuantity,
                                                            timestamp = ""
                                                        )

                                                    modifiers.add(modelMod)
                                                }
                                            }
                                            listItemDine.add(
                                                GetOrderDetailsResponse.Data.OrderItem(
                                                    categoryId = item.categoryId,
                                                    completedInKitchen = false,
                                                    discountAmount = 0.0,
                                                    discountId = 0,
                                                    discountType = "",
                                                    employeeId = 0,
                                                    float = 0.0,
                                                    id = item.orderItemId ?: 0,
                                                    isPaid = item.isPaid,
                                                    isFired = item.isFired,
                                                    isPrinted = false,
                                                    itemId = item.itemId,
                                                    note = item.note,
                                                    orderId = cartlist[0].orderId ?: 0,
                                                    orderItemModifiers = modifiers,
                                                    orderItemTaxes = arrayListOf(),
                                                    price = item.price,
                                                    quantity = item.itemQuantity,
                                                    timestamp = item.timeStamp ?: "",
                                                    totalPrice = item.price,
                                                    itemName = item.name,
                                                    refundedAmount = 0.0,
                                                    refundedQuantity = 0,

                                                    order_item_variation = null


                                                )
                                            )

                                        }
                                    }
                                    prefProvider.setValue(
                                        Constants.DINE_IN_UPDATE_LIST,
                                        Gson().toJson(listItemDine)
                                    )
                                }

                            } else {
                                binding.txtDineInProceed.setText("Proceed To Fire")
                            }
                            setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)
                            binding.txtSubTotal.text =
                                MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                            binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                            binding.txtServiceCharge.text =
                                MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                            binding.txtDiscount.text =
                                "-" + MethodUtils.roundOffAmount(viewModel.totalDiscount)
                            binding.txtNoncashAdj.text =
                                MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                            if (viewModel.order_note.isNotEmpty()) {
                                binding.relativeOrderNotes?.visibility = View.VISIBLE
                                binding.txtOrderNote?.text = viewModel.order_note
                            } else {
                                binding.relativeOrderNotes?.visibility = View.GONE
                            }

                            binding.relativeLoylatyPoints.visibility = View.GONE
                            binding.lblLoyaltyPoints.visibility = View.GONE

                        } else {
                            if (isFromPayment) {
                                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                                    binding.linearCashDiscount.visible()
                                    if (prefProvider.getValue(
                                            OPTION_TYPE,
                                            "CashDiscount"
                                        ) == "CashDiscount"
                                    ) {
                                        binding.labelCashSurcharge?.text = "Cash Discount"
                                    } else {
                                        binding.labelCashSurcharge?.text = "SurCharge"
                                    }
                                } else {
                                    binding.linearCashDiscount.gone()
                                }
                                binding.linearButtonView.gone()
                                binding.relPreoceedToFire.gone()
                            } else {
                                binding.linearButtonView.gone()
                                binding.relPreoceedToFire.visible()
                            }
                            dineInCartAdapter.clearList()
                            viewModel.clearListTax()
                            reSetTaxBifurcationData()
//                        var data: TbCustomer? = prefProvider.getCustomerData()
//                        if (data != null) {
//                            if (viewModel.loyaltyPointCondition(data)) {
//                                binding.liinearInfoLayout.layoutParams.height =
//                                    resources.getDimension(R.dimen._70sdp).toInt()
//                                binding.relativeLoylatyPoints.visibility = View.VISIBLE
//                                binding.lblLoyaltyPoints.visibility = View.VISIBLE
//                            } else {
//                                binding.liinearInfoLayout.layoutParams.height =
//                                    resources.getDimension(R.dimen._40sdp).toInt()
//                                binding.relativeLoylatyPoints.visibility = View.GONE
//                                binding.lblLoyaltyPoints.visibility = View.GONE
//                            }
//                        }
                            binding.relativeOrderNotes?.visibility = View.GONE
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._50sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.GONE
                            binding.lblLoyaltyPoints.visibility = View.GONE


                        }


                    } else {
                        binding.rvCartDineIn.gone()
                        binding.rvCartList.visible()
                        if (it.isNotEmpty()) {
                            viewModel.destroyedList.clear()
                            it[0].items?.filter { item -> item.isDestroy }?.let {
                                viewModel.destroyedList.addAll(it)
                            }
                            if (isFromPayment) {
                                viewModel.selectedCustomer = prefProvider.getCustomerData()
                                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                                    binding.linearCashDiscount.visible()
                                    if (prefProvider.getValue(
                                            OPTION_TYPE,
                                            "CashDiscount"
                                        ) == "CashDiscount"
                                    ) {
                                        binding.labelCashSurcharge?.text = "Cash Discount"
                                    } else {
                                        binding.labelCashSurcharge?.text = "SurCharge"
                                    }
                                } else {
                                    binding.linearCashDiscount.gone()
                                }
                                binding.linearButtonView.gone()
                                binding.relPreoceedToFire.gone()
                            } else {
                                binding.linearButtonView.visible()
                                binding.relPreoceedToFire.gone()
                            }
                            binding.rvCartList.removeAllViews()
                            binding.rvCartList.removeAllViewsInLayout()

                            var filterItems = arrayListOf<TbItem>()
                            it[0].items?.filter {
                                !it.isDestroy
                            }.let {

                                filterItems.addAll(it!!.toCollection(arrayListOf()))
                            }

                            cartAdapter.setList(filterItems)
                            /*it[0].items?.toCollection(arrayListOf())
                            ?.let { it1 ->
                                cartAdapter.setList(it1)
                            }*/

                            cartlist = it as ArrayList<CartModel>
                            viewModel.itemCalculationCartModel(
                                it[0],
                                binding.txtTotal,
                                requireContext()
                            )
                            viewModel.setCartModel(it)
                            setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)
                            if (viewModel.order_note.isNotEmpty()) {
                                binding.relativeOrderNotes?.visibility = View.VISIBLE
                                binding.txtOrderNote?.text = viewModel.order_note
                            } else {
                                binding.relativeOrderNotes?.visibility = View.GONE
                            }
                            binding.txtSubTotal.text =
                                MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                            binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                            binding.txtServiceCharge.text =
                                MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                            binding.tvPayNow.text = "Pay " + binding.txtTotal.text.toString()
                            Log.e("totalDiscount", viewModel.totalDiscount.toString())
                            binding.txtDiscount.text =
                                "-" + MethodUtils.roundOffAmount(viewModel.totalDiscount)
                            binding.txtNoncashAdj.text =
                                MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                            var data: TbCustomer? = prefProvider.getCustomerData()
                            if (data != null) {
                                if (viewModel.loyaltyPointCondition(data)) {
                                    if (isOrderUpdate) {
                                        viewModel.setcheckedLoyaltyApply(
                                            prefProvider.getValueboolean(
                                                IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                                false
                                            )
                                        )
                                    } else {
                                        viewModel.setcheckedLoyaltyApply(
                                            prefProvider.getValueboolean(
                                                LOYALTY_ADDED,
                                                false
                                            )
                                        )
                                    }
                                    if (isFromPayment) {
                                        if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                                            binding.liinearInfoLayout.layoutParams.height =
                                                resources.getDimension(R.dimen._70sdp).toInt()
                                            binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                            binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                            binding.txtLabelLoyaltyAmounts.visibility = View.VISIBLE
                                            binding.checkloylaty.visibility = View.GONE
                                            binding.txtLoyaltyAmount.text =
                                                "- $${
                                                    String.format(
                                                        "%.2f",
                                                        viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                                    )
                                                }"
                                            binding.txtLoyaltyPoints.text =
                                                "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"

                                        } else {
                                            binding.liinearInfoLayout.layoutParams.height =
                                                resources.getDimension(R.dimen._50sdp).toInt()
                                            binding.relativeLoylatyPoints.visibility = View.GONE
                                            binding.lblLoyaltyPoints.visibility = View.GONE
                                        }
                                    } else {
                                        binding.liinearInfoLayout.layoutParams.height =
                                            resources.getDimension(R.dimen._70sdp).toInt()
                                        binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                        binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                        Log.e(TAG, "InsideLoyalty")
                                        Log.e(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
                                        binding.txtLoyaltyAmount.text =
                                            "- $${
                                                String.format(
                                                    "%.2f",
                                                    viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                                )
                                            }"
                                        binding.txtLoyaltyPoints.text =
                                            "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                        binding.checkloylaty.isChecked =
                                            viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                                        Log.d(
                                            TAG,
                                            "addObserver crash: " + viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                                        )
                                        Log.d(
                                            TAG,
                                            "addObserver crash: " + viewModel.redeemLoyaltyInfo
                                        )
                                    }
                                }
                            } else {
                                binding.relativeLoylatyPoints.visibility = View.GONE
                                binding.lblLoyaltyPoints.visibility = View.GONE
                            }


                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._50sdp).toInt()
                            taxClickable = false
                            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                            binding.relativeDynamicTax.gone()
                            binding.imgDropdown.gone()
                            viewModel.clearListTax()
                            cartAdapter.clearList()
                            reSetTaxBifurcationData()
                            binding.relativeOrderNotes?.visibility = View.GONE
                            binding.txtTotal.text = MethodUtils.roundOffAmount(0.0)
                            binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.0)
                            binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(0.0)
                            binding.txtNoncashAdj.text =
                                MethodUtils.roundOffAmount(0.0)
                            binding.txtServiceCharge.text =
                                MethodUtils.roundOffAmount(0.0)
                            binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.0)
                            var data: TbCustomer? = prefProvider.getCustomerData()
                            if (data != null) {
                                if (viewModel.loyaltyPointCondition(data)) {
                                    binding.liinearInfoLayout.layoutParams.height =
                                        resources.getDimension(R.dimen._70sdp).toInt()
                                    binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                    binding.lblLoyaltyPoints.visibility = View.VISIBLE

                                    binding.txtLoyaltyAmount.text =
                                        "$0.00"
                                    binding.txtLoyaltyPoints.text =
                                        "$0.00"

                                } else {
                                    binding.liinearInfoLayout.layoutParams.height =
                                        resources.getDimension(R.dimen._50sdp).toInt()
                                    binding.relativeLoylatyPoints.visibility = View.GONE
                                    binding.lblLoyaltyPoints.visibility = View.GONE
                                }
                            } else {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._50sdp).toInt()
                                binding.relativeLoylatyPoints.visibility = View.GONE
                                binding.lblLoyaltyPoints.visibility = View.GONE
                            }


                        }

                    }

                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                        binding.txtAddCustomer.gone()
                    } else {
                        binding.txtAddCustomer.visible()
                    }

                }
            }

        }


        if (view != null) {
            viewModel.showProgress.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    if (it) {
                        ProgressUtils.showProgressDialog(requireActivity())
                    } else {
                        ProgressUtils.dismissProgressDialog()
                    }
                }
            }
        }
        if (view != null) {
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
    }


    private fun clearUpdateFlag() {
        isOrderUpdate = false
        prefProvider.setValueboolean(Constants.IS_ORDER_UPDATE, value = false)
        requireArguments().remove("update")

    }


    override fun onPause() {
        super.onPause()
        arguments?.clear()
        //requireArguments().clear()
        ProgressUtils.dismissProgressDialog()
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int) {
        Log.e(TAG, "itemClicked  ${Gson().toJson(data)}")


        itemClickListner?.onItemUpdate(data)


    }

    override fun onHeaderSelected(position: Int) {
        prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, position)
        Log.d(TAG, "onHeaderSelected: header position : $position")
        viewModel.dineInHeaderPosition = position
    }

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {
        Log.e(TAG, "onDineinItemClick ${position}")

        viewModel.selectedItemPositionDine = position
        // viewModel.dineInHeaderPosition = headerPosition
        viewModel.dineInSelectedItemHeaderPos = headerPosition

        itemClickListner?.onItemUpdate(item)
        /* if (prefProvider.getValue(ORDER_TYPE, "") == Constants.DINE_IN) {
             val dineinList = dineInCartAdapter.getList()
             dineinList.get(0).selectedPosition = viewModel.dineInHeaderPosition

             viewModel.cartLogic(cartlist, item, Constants.UPDATE, false, dineInList = dineinList)

         }*/
    }

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {
        Log.e(TAG, "onCustomerClicked  ${isRemoved}")
        if (isRemoved) {
            if (cartlist.get(0).dineInList?.size!! >= position) {
                val dineIn = cartlist.get(0).dineInList
                dineIn?.get(position)?.customer = null
                viewModel.dineInCartUpdate(cartlist, dineIn!!)
            }

        } else {

            val bundle = bundleOf("DINE_IN" to true, "position" to position, "cartList" to cartlist)
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
                    Constants.DELETE, false,
                    dineInList = dineInCartAdapter.getList()
                )


            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff heref
            }
        }
    }


    private fun showMessage() {
        AlertUtils.showCustomAlert(requireContext(), "Order should be less than 1 million usd.")
    }


    private fun setCartAdapter() {
        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        binding.rvCartList.adapter = cartAdapter
        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)
        dineInCartAdapter.isFromPayment(isFromPayment)
        binding.rvCartDineIn.adapter = dineInCartAdapter
    }

    private fun clearCart() {
        alert(
            getString(R.string.app_name),
            getString(R.string.delete_items_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here

                taxBirfurcationAdapter.clearList()
                viewModel.clearListTax()
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                updateActiveOrderFlagClear()
                itemListner?.onCancelItemSelected()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {
                    prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                    prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)
                    viewModel.removeItemDineInList.clear()

                    if (cartlist.size > 0) {

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
                    }

                    clearCustomer()
                    viewModel.deleteCart()
                    cartlist.clear()
                    isOrderUpdate = false
                    dineInCartAdapter.clearList()

                    viewModel.clearListTax()
                    binding.rvCartDineIn.gone()
                    // prefProvider.setValue(DINE_IN_UPDATE_LIST, "")
//                    uiSave()

                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)



                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    clearUpdateFlag()
                    binding.linearButtonView.visible()
                    binding.relPreoceedToFire.gone()
                    arguments?.clear()

                    itemClickListner?.onDineInOrderCleared()


                } else {
                    clearCustomer()
                    viewModel.deleteCart()
                    cartlist.clear()
                    isOrderUpdate = false
                    prefProvider.setValue(ORDER_TYPE, TAKEOUT)
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
                    itemClickListner?.onDineInOrderCleared()
                    uiSave()

                }
            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff here
            }
        }

    }

    private fun updateActiveOrderFlagClear() {

        isOrderUpdate = false
        isActiveOrder = false
        viewModel.updateActiveOrderFlagClear()

    }


    private fun clearCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        viewModel.selectedCustomer = null
        viewModel.assignCustomer = null
        binding.liinearInfoLayout.layoutParams.height =
            resources.getDimension(R.dimen._50sdp).toInt()
        binding.relativeLoylatyPoints.visibility = View.GONE
        binding.lblLoyaltyPoints.visibility = View.GONE
        displayCustomer()
        refreshItemCalculation()
        prefProvider.setValueboolean(IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        prefProvider.setValueboolean(LOYALTY_ADDED, false)
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

        binding.relPreoceedToFire.setOnClickListener {
            if (viewModel.restrictedAmount(binding.txtTotal)) {
                //cartlist[0] = viewModel.generateCombinedItems(viewModel.cartModel!!)

                if (cartlist.isNotEmpty()) {
                    if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
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
                            //cartlist[0] = viewModel.addDineInRemovedItems(viewModel.cartModel!!)

                            val request = viewModel.updateOrder(cartlist[0])

                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                            prefProvider.setValueboolean(DINE_IN_LIST_EDIT, false)
                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)



                            cartlist[0].orderId?.let { viewModel.updateOrderCall(it, request) }


                        }

                    } else {

                        createDineInOrder()
                    }
                }
            } else {
                showMessage()
            }
        }

        binding.txtAddCustomer.setOnClickListener {
            if (isFromPayment) {
                if (prefProvider.getValueboolean(
                        Constants.LOYALTY_ADDED,
                        false
                    ) || prefProvider.getValueboolean(
                        Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                        false
                    )
                ) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireActivity(),
                        "You can not change customer from checkout when loyalty points added. Please go back and change customer."
                    ) { _, _ ->
                    }
                } else {
                    viewModel.setIsFromAddCustomer(true)
                    findNavController().navigate(R.id.action_paymentBoldPosFragment_to_assignCustomerOrderFragment)
                }
            } else {
                if (isOrderUpdate) {
                    var bundle: Bundle = Bundle()
                    bundle.putInt("orderId", orderId!!)
                    bundle.putInt("paymentId", paymentId!!)
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                    bundle.putBoolean(
                        "isLoyaltyApplied",
                        viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                    )
                    bundle.putBoolean("update", isOrderUpdate)
                    findNavController().navigate(
                        R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment,
                        bundle
                    )
                } else {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment)
                }

            }
        }

        binding.imgOrderMenu.setOnClickListener {


            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.cart_menu, popupMenu.menu)
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN)
                if (prefProvider.getValue(Constants.CUSTOMER_NAME, "").isEmpty())
                    popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false
            if (cartlist.isEmpty()) {
                popupMenu.menu.findItem(R.id.menu_discount).isVisible = false
                popupMenu.menu.findItem(R.id.menu_order_note).isVisible = false
                popupMenu.menu.findItem(R.id.menu_clear_cart).isVisible = false
            }

            if (prefProvider.getValueInt(CUSTOMER_ID, -1) == -1) {
                popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false
            }
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
                    R.id.menu_order_note -> {
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_addNoteDialog,
                            bundleOf("isOrderNote" to true, "cartList" to cartlist)
                        )
                    }
                    R.id.menu_discount -> {

                        val bundle = Bundle()
                        bundle.putBoolean("isOrderDiscount", true)
                        bundle.putDouble("totalPrice", viewModel.subTotalPrice)
                        if (cartlist.isNotEmpty()) {
                            bundle.putDouble("orderDiscountPrice", cartlist[0].discountPrice)
                            bundle.putString("orderDiscountType", cartlist[0].discountType)
                            bundle.putDouble("selectedvalue", cartlist[0].discountSelectdValue)
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
                viewModel.setTipAmount(0.0)
                if (isOrderUpdate) {
                    var bundle: Bundle = Bundle()
                    bundle.putInt("orderId", orderId!!)
                    bundle.putInt("paymentId", paymentId!!)
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment,
                            bundle
                        )
                    }
                } else {
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
                    }
                }
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
                            viewModel.totalDiscount,
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

                        isOrderUpdate = false
                        binding.tvSave.text = getString(R.string.save)

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

    private fun createDineInOrder() {
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
                if (isOrderUpdate) {
                    bundle.putBoolean("update", true)
                    orderId?.let { bundle.putInt("orderId", it) }
                    paymentId?.let { bundle.putInt("paymentId", it) }
                    bundle.putString("paymentOfflineId", paymentOfflineId)
                    bundle.putString("orderOfflineId", orderOfflineId)
                }

                var itemCount = 0
                for (i in cartlist.indices) {
                    for (j in cartlist[i].dineInList?.indices!!) {
                        if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                            itemCount++
                            break
                        }
                    }
                    if (itemCount != 0) {
                        createDineInRequest()
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
            }
        }
    }

    private fun createDineInRequest() {

        var floorModel = DineInOrderDetailAttributes(
            floorPlanId = dineInFloorTableModel?.floorPlanId,
            floorPlanTableId = dineInFloorTableModel?.id,
            tableType = dineInFloorTableModel?.tableType,
            tableName = dineInFloorTableModel?.tableName,
            tableNumber = dineInFloorTableModel?.tableNumber,
            chairCount = dineInFloorTableModel?.chairCount,
            floorPlanName = "",
            totalGuestCount = dineInCartAdapter.getList().size - 1


        )

        if (floorModel.floorPlanId == null) {
            floorModel.floorPlanId = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.floorPlanId
            floorModel.floorPlanTableId = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.id
            floorModel.tableType = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.tableType
            floorModel.tableNumber = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.tableNumber
            floorModel.chairCount = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.chairCount


        }
        viewModel.ordertypelist.forEach {
            if (it.orderType.toLowerCase() == Constants.DINE_IN.toLowerCase()) {
                cartlist[0].orderTypeId = it.id
                cartlist[0].orderType = it.orderType
            }
        }
        cartlist[0].openOrderType = Constants.PICK_UP

        val orderRequestModel = viewModel.createDineInOrderRequest(
            cartModel = cartlist[0],
            subTotalPrice = viewModel.subTotalPrice,
            totalPrice = viewModel.totalPrice - cartlist[0].discountPrice,
            totalServiceCharge = viewModel.totalServiceCharge,
            totalTax = viewModel.totalTax,
            ORDER_TYPE = Constants.DINE_IN,
            "",
            "",
            false,
            totalDiscount = viewModel.totalDiscount,
            0.0,
            floorPlanDetails = floorModel
        )

        if (orderRequestModel != null) {
            viewModel.submit(orderRequestModel)
        }

    }
}


