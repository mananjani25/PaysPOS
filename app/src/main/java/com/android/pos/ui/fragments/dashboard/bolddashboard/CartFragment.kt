package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CUSTOMER_ID
import com.android.pos.data.remote.Constants.DELIVERY_TYPE
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_LIST_EDIT
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE_LIST
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.GIFT_CARD
import com.android.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.android.pos.data.remote.Constants.IS_LAST_ITEM_DELETE
import com.android.pos.data.remote.Constants.IS_PAX_PAYMENT_FAILED
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
import com.android.pos.data.remote.Constants.OPEN_ORDER_DIRECT_PAY
import com.android.pos.data.remote.Constants.OPEN_ORDER_UPDATE_FOR_PRINT
import com.android.pos.data.remote.Constants.OPTION_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.PHONE_ORDER
import com.android.pos.data.remote.Constants.PICK_UP
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.WHOLE_AMOUNT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.OrderTypeAdapter
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.DineInOrderCallBack
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.ItemClickListner
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.disableItemAnimator
import com.android.pos.utils.extensions.getColor
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.invisible
import com.android.pos.utils.extensions.isVisible
import com.android.pos.utils.extensions.runOnUiThread
import com.android.pos.utils.extensions.setOnSingleClickListener
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.getCustomerDisplay
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class CartFragment(
    val itemClickListner: ItemClickListner?,
    val itemListner: ItemListner?,
    val isFromPaymentDinein: Boolean = false,
    val guestCalModel: GuestPaymentCalculationModel? = null,
    val isGuestPayment: Boolean = false,
    val dineInCallback: DineInOrderCallBack? = null,
    val isFromDashboard: Boolean? = false,
) :
    Fragment(), MyCallback,
    DineInAdapter.DineInCallback, ItemCallback {

    private var oldItemSize: Int? = 0

    @Inject
    lateinit var apiService: ApiService


    private lateinit var presentation: CustomDisplay
    private var isSaveOrder: Boolean = false
    private lateinit var binding: FragmentCartBinding
    var fragmentId: Int? = null
    var checkoutHeaderId: Int = 0
    var dashboardHeaderId: Int = 0
    var numOfGuest: Int = 0
    private var isOrderUpdate: Boolean = false
    private lateinit var cartAdapter: CartAdapter
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var future_delivery_date: String = ""
    private var paymentId: Int? = null
    private var future_delivery_time: String = ""
    lateinit var cashDiscountModel: CashDiscountModel
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    var cashDiscountType = ""
    var cartlist: ArrayList<CartModel> = arrayListOf()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    var updateBundle: Bundle? = null
    var isFromPayment: Boolean = false
    var isActiveOrder: Boolean = false
    var reorder: Boolean = false

    private val DELAY_MILLIS = 200L

    private var previousClickTimeMillis = 0L

    private lateinit var nameObserver: Observer<List<CartModel>>
    private lateinit var dineInCartAdapter: DineInAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private var assignCustomer: TbCustomer? = null
    private var orderFloorDetails: GetOrderDetailsResponse.Data.FloorPlanTable =
        GetOrderDetailsResponse.Data.FloorPlanTable()
    private var serviceChargesList: List<TbServiceCharge>? = null

    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable? = null

    var taxClickable = false
    var cashDiscountSurcharge = 0.0

    private var orderTypeAdapter: OrderTypeAdapter? = null
    var splitValue = -1

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"

    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        LogUtil.logE("bundleData", arguments.toString())

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                viewModel,
                passcodeViewModel,
                dineInViewModel

            )
            //presentation.show()
        }

        checkOrderType()

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
                    LogUtil.logE(TAG, "isOrderUpdateReq:  $isOrderUpdate")
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
                        LogUtil.logE("ORDER_TYPE", "Updated check")
                        updateActiveOrderFlag()
                    }
                    uiSave()

                }

            }


        setUpData()
        return binding.root
    }

    private fun checkOrderType() {

//        saveVisibility()
        if (prefProvider.getValue(ORDER_TYPE, "").isEmpty()) {
            binding.rlCartView.gone()
            binding.rvOrderType.visible()
            binding.orderTypeDisplay.text =
                getString(R.string.current_order)
        } else {
            binding.rlCartView.visible()
            binding.rvOrderType.gone()

            if (prefProvider.getValue(
                    ORDER_TYPE,
                    TAKEOUT
                ) == DINE_IN || prefProvider.getValueboolean(
                    Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                    false
                )
            ) {
                binding.txtAddCustomer.invisible()
            } else {
                binding.txtAddCustomer.visible()
            }

            if (isFromDashboard!!) {
                val builder = SpannableStringBuilder()
                val str1 = SpannableString(getString(R.string.current_order) + ": ")
                str1.setSpan(ForegroundColorSpan(getColor(R.color.txtColor)), 0, str1.length, 0)
                builder.append(str1)
                val str2 = SpannableString(prefProvider.getValue(ORDER_TYPE_NAME, ""))
                str2.setSpan(ForegroundColorSpan(getColor(R.color.btnColor)), 0, str2.length, 0)
                builder.append(str2)

                binding.orderTypeDisplay.setText(builder, TextView.BufferType.SPANNABLE)

                binding.orderTypeDisplay.setOnClickListener {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryBoldPOS_to_changeOrderTypeDialog,
                    )
                }
            } else {
                binding.orderTypeDisplay.text =
                    getString(R.string.current_order) + ": " + prefProvider.getValue(
                        ORDER_TYPE_NAME,
                        ""
                    )
            }
        }
    }

    private fun displayCustomer() {


        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        LogUtil.logE("Customer Name", name)
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
            binding.txtAddCustomer.setPadding(
                0,
                resources.getDimension(R.dimen._5sdp).toInt(),
                resources.getDimension(R.dimen._10sdp).toInt(),
                resources.getDimension(R.dimen._5sdp).toInt()
            )
            binding.imgOrderMenu.isEnabled = false
            binding.imgOrderMenu.isClickable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefProvider.setValueboolean(OPEN_ORDER_DIRECT_PAY, false)

        initListeners()
        setCartAdapter()
        getDineInData()
        callback()
        setupLoyalytyPoints()
        addObserver()
        setupTaxAdapter()
        getOrderTypes()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) {
                Log.d(TAG, "splitDetector onCreateView: " + it.getInt("splitvalue"))
                splitValue = it.getInt("splitvalue")
            }


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
                            if (binding.relativeLoylatyPoints.isVisible()) {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._70sdp).toInt()
                            } else {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._60sdp).toInt()
                            }
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
                    if (this::presentation.isInitialized) {
                        presentation.show()
                        presentation.onDisplayChanged()
                        presentation.onTaxClicked(true)
                    }
                } else {
                    if (binding.relativeLoylatyPoints.isVisible()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._70sdp).toInt()
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._50sdp).toInt()
                    }
                    taxClickable = false
                    binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.relativeDynamicTax.gone()
                    if (this::presentation.isInitialized) {
                        presentation.show()
                        presentation.onDisplayChanged()
                        presentation.onTaxClicked(false)
                    }
                }
            }

        }


        if (prefProvider.getValueInt(CUSTOMER_ID, -1) != -1) {
            displayCustomer()
        }
        //  getCartList()

        if (updateBundle != null) {
            isOrderUpdate = requireArguments().getBoolean("update")
            LogUtil.logE(TAG, "isOrderUpdateReq:  $isOrderUpdate")
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
                LogUtil.logE("ORDER_TYPE", "Updated check")

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
            LogUtil.logE("ORDER_TYPE", "Updated check1")
        }

        uiSave()

    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        val taxlist = arrayListOf<TaxData>()
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
            viewModel.setcheckedLoyaltyApply(p1, binding.txtTotal)
//            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = p1
            prefProvider.setValueboolean(Constants.LOYALTY_ADDED, p1)
            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, p1)
            binding.tvPayNow.text = "Pay ${binding.txtTotal.text}"
//            addObserver()
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.onDisplayChanged()
            }
        }

    }

    private fun callback() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_customer",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val data: TbCustomer = bundle.getParcelable<TbCustomer>("data") as TbCustomer
            viewModel.assignCustomer = data
            viewModel.setcheckedLoyaltyApply(false)
            viewModel.selectedCustomer = data
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_guestcount",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val count: Int = bundle.getInt("count")
            addGuestToOrder(count)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_order_type_change",
            viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            var data: TbOrderType = bundle.getParcelable<TbCustomer>("orderData") as TbOrderType

            checkOrderType()
            addObserver()

            if (cartlist.isNotEmpty()) {
                cartlist[0].orderTypeName = data.name
                cartlist[0].orderType = data.orderType
                cartlist[0].orderTypeId = data.id
                viewModel.addCart(cartModel = cartlist[0])
            }
        }
    }


    private fun getDineInData() {
        if (updateBundle != null) {
            if (updateBundle?.getBoolean("isFromDineIn") == true) {
                LogUtil.logE(TAG, "isFromDinein")
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
        numOfGuest = updateBundle!!.get("numberOfGuest") as Int


        dineInFloorTableModel =
            updateBundle?.get("floorplan") as GetFloorPlanResponse.Data.FloorPlanTable
        var orderDEtails: GetOrderDetailsResponse.Data.FloorPlanTable? = null
        if (updateBundle!!.get("tableDetails") != null) {
            orderDEtails =
                updateBundle!!.get("tableDetails") as GetOrderDetailsResponse.Data.FloorPlanTable?
        }
        if (orderDEtails != null) {
            orderFloorDetails = orderDEtails
        }
        if (orderDEtails != null) {
            if (orderFloorDetails.id == null) {
                orderFloorDetails.apply {
                    id = dineInFloorTableModel?.id
                    chairCount = dineInFloorTableModel?.chairCount
                    floorPlanId = dineInFloorTableModel?.floorPlanId
                    tableName = dineInFloorTableModel?.tableName.toString()
                    status = dineInFloorTableModel?.status.toString()
                    tableNumber = dineInFloorTableModel?.tableNumber

                }

            }
        } else {
            orderFloorDetails.apply {
                id = dineInFloorTableModel?.id
                chairCount = dineInFloorTableModel?.chairCount
                floorPlanId = dineInFloorTableModel?.floorPlanId
                tableName = dineInFloorTableModel?.tableName.toString()
                status = dineInFloorTableModel?.status.toString()
                tableNumber = dineInFloorTableModel?.tableNumber
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
                    floorPlanTable = orderFloorDetails,

                    )
            )
        }



        dineInCartAdapter.setList(dineInList)

        if (cartlist.isEmpty()) {
            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                employeeID = prefProvider.getValueInt(EMPLOYEE_ID, -1)
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, -1)
                orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
                orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()

                serviceCharge = getServiceChargeFromGuestCount(numOfGuest)

            }
            cartlist.add(cartModel)
        }

        viewModel.dineInHeaderPosition = 0
        viewModel.dineInSelectedItemHeaderPos = 0
        cartlist.get(0).orderType = Constants.DINE_IN
        viewModel.newCartLogicModifier(
            cartlist,
            null,
            Constants.ADD,
            false,
            dineInList = dineInList
        )

    }

    private fun checkDineInEditOrder() {
        if (arguments?.getBoolean("is_dine_in_edit") == true) {
            val dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")

            if (dineInList?.isNotEmpty() == true) {
                //  binding.layoutCart.txtOrderType.setText("Dine In")

                dineInCartAdapter = DineInAdapter()
                dineInCartAdapter.setListner(this)
                //dineInCartAdapter.setList(dineInList)


                if (cartlist.isEmpty()) {
                    val cartModel = CartModel().apply {
                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                        employeeID = prefProvider.getValueInt(EMPLOYEE_ID, -1)
                        locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                        orderTypeId = 2
                        orderType = Constants.DINE_IN
                        orderTypeName = Constants.DINE_IN

                        serviceCharge = serviceChargesList
                        orderId = arguments?.getInt("orderId")
                        listOfItemRemoved = dineInList[0].listOfItemsMoved

                    }

                    orderId = arguments?.getInt("orderId")
                    cartlist.add(cartModel)
                }

                var orderTableData: GetOrderDetailsResponse.Data.FloorPlanTable? =
                    arguments?.getParcelable("tableDetails")

                dineInList.forEach {
                    it.floorPlanTable = orderTableData
                }

                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
                prefProvider.setValueInt(
                    ORDER_TYPE_ID,
                    prefProvider.getValueInt(ORDER_TYPE_ID, 0)
                )

                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
                viewModel.totalDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
                viewModel.newCartLogicModifier(
                    cartlist,
                    null,
                    Constants.ADD,
                    false,
                    dineInList = dineInList
                )


            }


        }
    }

    private fun addDineInObserver() {

        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, ""),
            prefProvider.getValueInt(EMPLOYEE_ID, 0)
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
                if (binding.relativeLoylatyPoints.isVisible()) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._70sdp).toInt()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._50sdp).toInt()
                }
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.visible()
            taxBirfurcationAdapter.setList(taxlistData)
            binding.relativeDynamicTax.gone()
        } else {
            if (viewModel.order_note.isNotEmpty()) {
                if (binding.relativeLoylatyPoints.isVisible()) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._70sdp).toInt()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._60sdp).toInt()
                }

            } else {
                if (binding.relativeLoylatyPoints.isVisible()) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._70sdp).toInt()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._50sdp).toInt()
                }
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.gone()
            binding.relativeDynamicTax.gone()
            taxClickable = false
        }
    }

    fun reSetTaxBifurcationData() {
        taxBirfurcationAdapter.clearList()
        if (binding.relativeLoylatyPoints.isVisible()) {
            binding.liinearInfoLayout.layoutParams.height =
                resources.getDimension(R.dimen._70sdp).toInt()
        } else {
            binding.liinearInfoLayout.layoutParams.height =
                resources.getDimension(R.dimen._50sdp).toInt()
        }
        taxClickable = false
        binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.imgDropdown.gone()
        binding.relativeDynamicTax.gone()
    }

    private fun addObserver() {

        removeGuestObserver()
        LogUtil.logE("CreateCartEmpIdRecd", "" + prefProvider.getValueInt(EMPLOYEE_ID, 0))

        if (prefProvider.getValue(REDIRECT_FROM, "") == MANUAL_SALE) {
            viewModel.manualSaleItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                LogUtil.logE(TAG, "listSize  ${Gson().toJson(it)}")
                if (it.isNotEmpty()) {
                    if (isFromPayment) {
                        viewModel.selectedCustomer = null
                        viewModel.redeemLoyaltyInfo.isLoyaltyApplied = false
                        viewModel.redeemLoyaltyInfo.needToApplyLoyalty = false
                        if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                ORDER_TYPE,
                                TAKEOUT
                            ) != Constants.GIFT_CARD
                        ) {
                            binding.linearCashDiscount.visible()
                            if (prefProvider.getValue(
                                    OPTION_TYPE,
                                    "CashDiscount"
                                ) == "CashDiscount"
                            ) {
                                binding.labelCashSurcharge?.text = "Cash Discount"
                            } else {
                                showSurchargeWithPercentage()
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
                    binding.tvPayNow.text =
                        "Pay " + MethodUtils.roundOffAmount(viewModel.totalPrice)
                    Log.e("totalDiscount", viewModel.totalDiscount.toString())
                    binding.txtDiscount.text = "-" +
                            MethodUtils.roundOffAmount(viewModel.totalDiscount)
                    if (prefProvider.getValue(
                            OPTION_TYPE, "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                        binding.txtNoncashAdj.text =
                            "-" + MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                    } else {
                        binding.txtNoncashAdj.text =
                            MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                    }
                    var data: TbCustomer? = prefProvider.getCustomerData()
                    if (data != null) {
                        if (viewModel.loyaltyPointCondition(data)) {
                            if (isFromPayment) {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._50sdp).toInt()
                                binding.relativeLoylatyPoints.visibility = View.GONE
                                binding.lblLoyaltyPoints.visibility = View.GONE
                                binding.lblLoyaltyBalance.visibility = View.GONE
                            } else {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._70sdp).toInt()
                                binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                binding.lblLoyaltyBalance.visibility = View.VISIBLE
                                LogUtil.logE(TAG, "InsideLoyalty")
                                LogUtil.logE(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
                                binding.txtLoyaltyAmount.text =
                                    "- $${
                                        String.format(
                                            "%.2f",
                                            viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                        )
                                    }"
                                binding.txtLoyaltyPoints.text =
                                    "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                binding.txtLoyaltyBalance.text =
                                    "${viewModel.selectedCustomer?.final_reward}"
                                binding.checkloylaty.isChecked =
                                    viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                            }
                        }
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._50sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.GONE
                        binding.lblLoyaltyPoints.visibility = View.GONE
                        binding.lblLoyaltyBalance.visibility = View.GONE
                    }

                    viewModel.itemCalculation(it, binding.txtTotal, requireContext())
                    setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)


                } else {
                    cartlist = arrayListOf()
                    viewModel.clearListTax()
                    cartAdapter.clearList()
                    reSetTaxBifurcationData()
                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtTax.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(0.00)
                    if (prefProvider.getValue(
                            OPTION_TYPE, "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                        binding.txtNoncashAdj.text =
                            "-" + MethodUtils.roundOffAmount(0.00)
                    } else {
                        binding.txtNoncashAdj.text =
                            MethodUtils.roundOffAmount(0.00)
                    }
                    binding.relativeOrderNotes?.visibility = View.GONE
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(0.00)
                    binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.00)
                    var data: TbCustomer? = prefProvider.getCustomerData()
                    if (data != null) {
                        if (viewModel.loyaltyPointCondition(data)) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.VISIBLE
                            binding.lblLoyaltyPoints.visibility = View.VISIBLE
                            binding.lblLoyaltyBalance.visibility = View.VISIBLE
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._50sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.GONE
                            binding.lblLoyaltyPoints.visibility = View.GONE
                            binding.lblLoyaltyBalance.visibility = View.GONE
                        }
                    }


                }

                //prefProvider.setValue(REDIRECT_FROM, "")
            }

        } else {
            if (view != null) {


                viewModel.mAllWordsFlow(
                    prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                ).asLiveData().observe(viewLifecycleOwner) {

                    CoroutineScope(Dispatchers.IO).launch {


                        if (it.isEmpty()) {

//                        if (oldItemSize != null && oldItemSize != 1)

                            // Flag is used to update cart if last item from the cart will be deleted
                            if (prefProvider.getValueboolean(IS_LAST_ITEM_DELETE, false)) {
                                prefProvider.setValueboolean(
                                    IS_LAST_ITEM_DELETE,
                                    false
                                ) // reset flag after updating cart
                            } else {
                                return@launch
                            }
                        } else {
                            val currentTimeMillis = System.currentTimeMillis()

                            if (currentTimeMillis >= previousClickTimeMillis + DELAY_MILLIS) {
                                previousClickTimeMillis = currentTimeMillis
                            } else {
                                return@launch
                            }
                        }


                        saveVisibility()


                        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                            runOnUiThread(Runnable {
                            binding.rvCartDineIn.visible()
                            binding.rvCartList.gone()
                            checkOrderType()
                            if (it.isNotEmpty()) {

                                cartlist = it as ArrayList<CartModel>
                                if (isFromPayment) {

                                    if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                            ORDER_TYPE,
                                            TAKEOUT
                                        ) != Constants.GIFT_CARD
                                    ) {
                                        binding.linearCashDiscount.visible()
                                        if (prefProvider.getValue(
                                                OPTION_TYPE,
                                                "CashDiscount"
                                            ) == "CashDiscount"
                                        ) {
                                            binding.labelCashSurcharge?.text = "Cash Discount"
                                        } else {
                                            showSurchargeWithPercentage()
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
                                    if (dineInList?.get(0)?.selectedPosition != -1) {

                                        dineInList?.get(0)?.selectedPosition =
                                            viewModel.dineInHeaderPosition
                                    }

                                    dineInCartAdapter.setList(
                                        dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                                    )


                                }
                                LogUtil.logE(TAG, "getPAyment:  ${isFromPayment}")
                                LogUtil.logE(TAG, "isGuestPayment:  ${isGuestPayment}")

                                if (isFromPaymentDinein) {
                                    viewModelPayment.dineInWholeDiscount =
                                        guestCalModel?.wholeOrderPassDiscount
                                    viewModelPayment.dineInWholeSC = guestCalModel?.wholeOrderPassSC
                                    viewModel.itemCalculationForDineInPayment(
                                        it[0],
                                        binding.txtTotal,
                                        requireContext(),
                                        guestCalModel!!,
                                        isGuestPayment
                                    )
                                } else {
                                    LogUtil.logE(TAG, "WithOutDineIn")
                                    viewModel.itemCalculationCartModel(
                                        it[0],
                                        binding.txtTotal,
                                        requireContext()
                                    )
                                }



                                viewModel.setCartModel(it)
                                if (prefProvider.getValueboolean(
                                        Constants.DINE_IN_UPDATE,
                                        false
                                    )
                                ) {
                                    binding.txtDineInProceed.setText("Update and Proceed")

                                    var getOldList = prefProvider.getValue(DINE_IN_UPDATE_LIST, "")
                                    if (getOldList.isEmpty()) {
                                        var listItemDine: ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                                            arrayListOf()
                                        var data = it[0].dineInList
                                        LogUtil.logE(TAG, "getDataSizeDin ${data?.size}")
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
                                                                it.modifier_quantity,
                                                                isModifier = it.isChecked,
                                                                itemId = "",
                                                                modifierId = 0,
                                                                it.modifierSetId,
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
                                                        custom_item_id = item.id,
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

                                Log.e(
                                    "CheckCalculation",
                                    "taxlistDynamic: ${Gson().toJson(it[0].taxlistDynamic)}"
                                )

                                var listOfTax: ArrayList<TaxData> = arrayListOf()
                                var noItem = false
                                var listItems: ArrayList<TbItem> = arrayListOf()
                                it[0].dineInList?.forEach {

                                    listItems.addAll(it.items)

                                }

                                it[0].taxlistDynamic?.let { it1 ->
                                    if (prefProvider.getValue(
                                            ORDER_TYPE,
                                            ""
                                        ) == DINE_IN && listItems.isEmpty() && prefProvider.getValueboolean(
                                            Constants.DINE_IN_UPDATE,
                                            false
                                        ) == false
                                    ) {
                                        listOfTax.addAll(arrayListOf())
                                        setTaxBifurcationData(arrayListOf())

                                    } else {
                                        listOfTax.addAll(it1)
                                        setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)
                                    }

                                }

                                binding.txtSubTotal.text =
                                    MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                                binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                                binding.txtServiceCharge.text =
                                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                                binding.txtDiscount.text =
                                    "-" + MethodUtils.roundOffAmount(viewModel.totalDiscount)
                                if (prefProvider.getValue(
                                        OPTION_TYPE, "CashDiscount"
                                    ) == "CashDiscount"
                                ) {
                                    binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                                    binding.txtNoncashAdj.text =
                                        "-" + MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                                } else {
                                    binding.txtNoncashAdj.text =
                                        MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                                }
                                if (viewModel.order_note.isNotEmpty()) {
                                    binding.relativeOrderNotes?.visibility = View.VISIBLE
                                    binding.txtOrderNote?.text = viewModel.order_note
                                } else {
                                    binding.relativeOrderNotes?.visibility = View.GONE
                                }

                                binding.relativeLoylatyPoints.visibility = View.GONE
                                binding.lblLoyaltyPoints.visibility = View.GONE
                                binding.lblLoyaltyBalance.visibility = View.GONE

                            } else {
                                cartlist = arrayListOf()
                                if (isFromPayment) {
                                    if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                            ORDER_TYPE,
                                            TAKEOUT
                                        ) != Constants.GIFT_CARD
                                    ) {
                                        binding.linearCashDiscount.visible()
                                        if (prefProvider.getValue(
                                                OPTION_TYPE,
                                                "CashDiscount"
                                            ) == "CashDiscount"
                                        ) {
                                            binding.labelCashSurcharge?.text = "Cash Discount"
                                        } else {
                                            showSurchargeWithPercentage()
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
                                binding.lblLoyaltyBalance.visibility = View.GONE


                            }

                            })
                        } else {

                            runOnUiThread(Runnable {
                                binding.rvCartDineIn.gone()
                                binding.rvCartList.visible()
                                checkOrderType()
                            })

                            if (it.isNotEmpty()) {


                                val filterItems = arrayListOf<TbItem>()
                                it[0].items?.filter {
                                    !it.isDestroy
                                }.let {

                                    filterItems.addAll(it!!.toCollection(arrayListOf()))
                                }

                                Log.e("mAllWords", "filterItems  ${filterItems.size}")
                                runOnUiThread(Runnable {
                                    cartAdapter.setList(filterItems)
                                    binding.rlCartView.visible()
                                    binding.rvOrderType.gone()
                                })

                                oldItemSize = it[0].items?.size


                                viewModel.destroyedList.clear()
                                it[0].items?.filter { item -> item.isDestroy }?.let {
                                    viewModel.destroyedList.addAll(it)
                                }

                                runOnUiThread(Runnable {
                                    if (isFromPayment) {
                                        viewModel.selectedCustomer = prefProvider.getCustomerData()

                                        if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                ORDER_TYPE,
                                                TAKEOUT
                                            ) != Constants.GIFT_CARD
                                        ) {

                                            binding.linearCashDiscount.visible()
                                            if (prefProvider.getValue(
                                                    OPTION_TYPE,
                                                    "CashDiscount"
                                                ) == "CashDiscount"
                                            ) {
                                                binding.labelCashSurcharge?.text = "Cash Discount"
                                            } else {
                                                showSurchargeWithPercentage()
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
                                })


                                if (filterItems.isNotEmpty()) {
                                    //binding.rvCartList.smoothScrollToPosition(filterItems.size - 1)
//                                Handler(Looper.myLooper()!!).postDelayed(
//                                    { binding.rvCartList.smoothScrollToPosition(filterItems.size - 1) },
//                                    200
//                                )
                                    runOnUiThread(kotlinx.coroutines.Runnable {
                                        binding.rvCartList.smoothScrollToPosition(filterItems.size - 1)
                                    })
                                }

                                cartlist = it as ArrayList<CartModel>
                                runOnUiThread(kotlinx.coroutines.Runnable {
                                    viewModel.itemCalculationCartModel(
                                        it[0],
                                        binding.txtTotal,
                                        requireContext()
                                    )
                                    viewModel.setCartModel(it)
                                    it[0].taxlistDynamic?.toCollection(arrayListOf())
                                        ?.let { it1 -> setTaxBifurcationData(it1) }

                                    if (viewModel.order_note.isNotEmpty()) {
                                        binding.relativeOrderNotes?.visibility = View.VISIBLE
                                        binding.txtOrderNote?.text = viewModel.order_note
                                    } else {
                                        binding.relativeOrderNotes?.visibility = View.GONE
                                    }
                                    binding.txtSubTotal.text =
                                        MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                                    binding.txtTax.text =
                                        MethodUtils.roundOffAmount(viewModel.totalTax)
                                    binding.txtServiceCharge.text =
                                        MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                                    binding.tvPayNow.text =
                                        "Pay " + binding.txtTotal.text.toString()
                                    Log.e("totalDiscount", viewModel.totalDiscount.toString())
                                    binding.txtDiscount.text =
                                        "-" + MethodUtils.roundOffAmount(viewModel.totalDiscount)
                                    if (prefProvider.getValue(
                                            OPTION_TYPE, "CashDiscount"
                                        ) == "CashDiscount"
                                    ) {
                                        binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                                        binding.txtNoncashAdj.text =
                                            "-" + MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                                    } else {
                                        binding.txtNoncashAdj.text =
                                            MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)
                                    }
                                    var data: TbCustomer? = prefProvider.getCustomerData()
                                    if (data != null) {
                                        if (viewModel.loyaltyPointCondition(data)) {
                                            if (isOrderUpdate) {
                                                viewModel.setcheckedLoyaltyApply(
                                                    prefProvider.getValueboolean(
                                                        IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                                        false,
                                                    ),
                                                    binding.txtTotal
                                                )
                                            } else {
                                                viewModel.setcheckedLoyaltyApply(
                                                    prefProvider.getValueboolean(
                                                        LOYALTY_ADDED,
                                                        false
                                                    ),
                                                    binding.txtTotal
                                                )
                                            }
                                            if (isFromPayment) {
                                                if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                                                    binding.liinearInfoLayout.layoutParams.height =
                                                        resources.getDimension(R.dimen._70sdp)
                                                            .toInt()
                                                    binding.relativeLoylatyPoints.visibility =
                                                        View.VISIBLE
                                                    binding.lblLoyaltyPoints.visibility =
                                                        View.VISIBLE
                                                    binding.lblLoyaltyBalance.visibility =
                                                        View.VISIBLE
                                                    binding.txtLabelLoyaltyAmounts.visibility =
                                                        View.VISIBLE
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
                                                    binding.txtLoyaltyBalance.text =
                                                        "${viewModel.selectedCustomer?.final_reward}"

                                                } else {
                                                    binding.liinearInfoLayout.layoutParams.height =
                                                        resources.getDimension(R.dimen._50sdp)
                                                            .toInt()
                                                    binding.relativeLoylatyPoints.visibility =
                                                        View.GONE
                                                    binding.lblLoyaltyPoints.visibility = View.GONE
                                                    binding.lblLoyaltyBalance.visibility = View.GONE
                                                }
                                            } else {
                                                binding.liinearInfoLayout.layoutParams.height =
                                                    resources.getDimension(R.dimen._70sdp).toInt()
                                                binding.relativeLoylatyPoints.visibility =
                                                    View.VISIBLE
                                                binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                                binding.lblLoyaltyBalance.visibility = View.VISIBLE
                                                Log.e(TAG, "InsideLoyalty")
                                                Log.e(
                                                    TAG,
                                                    Gson().toJson(viewModel.redeemLoyaltyInfo)
                                                )
                                                binding.txtLoyaltyAmount.text =
                                                    "- $${
                                                        String.format(
                                                            "%.2f",
                                                            viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                                        )
                                                    }"
                                                binding.txtLoyaltyPoints.text =
                                                    "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                                binding.txtLoyaltyBalance.text =
                                                    "${viewModel.selectedCustomer?.final_reward}"
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
                                        binding.lblLoyaltyBalance.visibility = View.GONE
                                    }

                                })

                            } else {

                                runOnUiThread(Runnable {
                                    cartlist = arrayListOf()
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
                                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.00)
                                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.00)
                                    binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
                                    binding.txtDiscount.text =
                                        "-" + MethodUtils.roundOffAmount(0.00)
                                    if (prefProvider.getValue(
                                            OPTION_TYPE, "CashDiscount"
                                        ) == "CashDiscount"
                                    ) {
                                        binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                                        binding.txtNoncashAdj.text =
                                            "-" + MethodUtils.roundOffAmount(0.00)
                                    } else {
                                        binding.txtNoncashAdj.text =
                                            MethodUtils.roundOffAmount(0.00)
                                    }
                                    binding.txtServiceCharge.text =
                                        MethodUtils.roundOffAmount(0.00)
                                    binding.tvPayNow.text =
                                        "Pay " + MethodUtils.roundOffAmount(0.00)
                                    var data: TbCustomer? = prefProvider.getCustomerData()
                                    if (data != null) {
                                        if (viewModel.loyaltyPointCondition(data)) {
                                            binding.liinearInfoLayout.layoutParams.height =
                                                resources.getDimension(R.dimen._70sdp).toInt()
                                            binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                            binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                            binding.lblLoyaltyBalance.visibility = View.VISIBLE

                                            binding.txtLoyaltyAmount.text =
                                                "$0.00"
                                            binding.txtLoyaltyPoints.text =
                                                "$0.00"
                                            binding.txtLoyaltyBalance.text = "0"
                                        } else {
                                            binding.liinearInfoLayout.layoutParams.height =
                                                resources.getDimension(R.dimen._50sdp).toInt()
                                            binding.relativeLoylatyPoints.visibility = View.GONE
                                            binding.lblLoyaltyPoints.visibility = View.GONE
                                            binding.lblLoyaltyBalance.visibility = View.GONE
                                        }
                                    } else {
                                        binding.liinearInfoLayout.layoutParams.height =
                                            resources.getDimension(R.dimen._50sdp).toInt()
                                        binding.relativeLoylatyPoints.visibility = View.GONE
                                        binding.lblLoyaltyPoints.visibility = View.GONE
                                        binding.lblLoyaltyBalance.visibility = View.GONE
                                    }

                                })


                            }

                        }

                        runOnUiThread(Runnable {
                            if (this@CartFragment::presentation.isInitialized) {
                                if (!presentation.isShowing)
                                    presentation.show()
                                if (it.isNotEmpty()) {
                                    presentation.updateCustomerDisplay(it)
                                } else {
                                    presentation.onLogOutOrClockOutWithApiService(apiService)
                                }
                            }

                            if (prefProvider.getValue(
                                    ORDER_TYPE,
                                    TAKEOUT
                                ) == DINE_IN || prefProvider.getValueboolean(
                                    Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                    false
                                )
                            ) {
                                binding.txtAddCustomer.invisible()
                            } else {
                                binding.txtAddCustomer.visible()
                            }

                            if (isFromPayment || isFromPaymentDinein) {
                                binding.rvOrderType.gone()
                                binding.rlCartView.visible()
                            }
                        })

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
                    LogUtil.logE("observeShowProgress3", isSaveOrder.toString())
                    if (it) {
                        if (isSaveOrder) {
                            ProgressUtils.showProgressDialog(requireActivity())
                            isSaveOrder = false
                        } else {
                            ProgressUtils.showProgressDialog(
                                if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
                                    getString(R.string.reattempting_the_payment)
                                } else {
                                    "Please wait payment under process"
                                },
                                requireActivity()
                            )

                        }
                    } else {
                        ProgressUtils.dismissProgressDialog()
                    }
                }
            }

            viewModelPayment.showProgressCash.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    LogUtil.logE("observeShowProgress3", isSaveOrder.toString())
                    if (it) {
                        ProgressUtils.showProgressDialog(requireActivity())
                    } else {
                        ProgressUtils.dismissProgressDialog()
                    }
                }
            }
        }
    }

    private fun showSurchargeWithPercentage() {
        val amountType = prefProvider.getValue(Constants.AMOUNT_TYPE, "")
        val rateOrAmount = prefProvider.getValue(Constants.RATE_OR_AMOUNT, "0")
        if (amountType == "Percentage") {
            binding.labelCashSurcharge.text = "${Constants.SURCHARGE_TEXT} (${rateOrAmount}%)"
        } else {
            binding.labelCashSurcharge.text = Constants.SURCHARGE_TEXT
        }
    }

    private fun saveVisibility() {
        if (prefProvider.getValue(ORDER_TYPE, "") == TAKEOUT) {
            binding.tvSave.visible()
//            val param: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
//                0,
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                2.0f
//            )
//            binding.tvPayNow.layoutParams = param
        } else {
            binding.tvSave.visible()
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
        LogUtil.logE(TAG, "itemClicked  ${Gson().toJson(data)}")


        itemClickListner?.onItemUpdate(data, position)


    }

    override fun onHeaderSelected(position: Int) {
        prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, position)
        Log.d(TAG, "onHeaderSelected: header position : $position")
        viewModel.dineInHeaderPosition = position
    }

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {
        LogUtil.logE(TAG, "onDineinItemClick ${position}")

        viewModel.selectedItemPositionDine = position
        // viewModel.dineInHeaderPosition = headerPosition
        viewModel.dineInSelectedItemHeaderPos = headerPosition

        item.headerPositionDinein = headerPosition
        itemClickListner?.onItemUpdate(item, position)
        /* if (prefProvider.getValue(ORDER_TYPE, "") == Constants.DINE_IN) {
             val dineinList = dineInCartAdapter.getList()
             dineinList.get(0).selectedPosition = viewModel.dineInHeaderPosition

             viewModel.cartLogic(cartlist, item, Constants.UPDATE, false, dineInList = dineinList)

         }*/
    }

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {
        LogUtil.logE(TAG, "onCustomerClicked  ${isRemoved}")
        viewModel.dineInHeaderPosition = position
        if (isRemoved) {
            if (cartlist.get(0).dineInList?.size!! >= position) {
                val dineIn = cartlist.get(0).dineInList
                dineIn?.get(position)?.customer = null
                viewModel.dineInCartUpdate(cartlist, dineIn!!)
            }

        } else {

            var listOfCustomersID: ArrayList<Int> = arrayListOf()
            cartlist[0].dineInList?.forEach {
                if (it.customer != null) {
                    listOfCustomersID.add(it.customer?.id ?: 0)

                }

            }
            val bundle = bundleOf(
                "DINE_IN" to true,
                "position" to position,
                "cartList" to cartlist,
                "listOfCustomersID" to listOfCustomersID
            )
            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment, bundle
            )
        }


    }

    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    private fun addGuestToOrder(count: Int) {
        if (count == 0) {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), getString(R.string.minimum_guest_count_should_be_one)
            ) { _, _ ->
            }
            return
        }
        var existing_count = dineInCartAdapter.getList().size - 1
        var total_count = existing_count + count
        if (total_count <= 15) {
            var existinglist: ArrayList<DineInModel> = arrayListOf()
            cartlist[0].dineInList?.forEach {
                if (!it.isDestroy) {
                    existinglist.add(it)
                }
            }

            val dineInList: java.util.ArrayList<DineInModel> = arrayListOf()
            if (existinglist.isNotEmpty()) {
                // List of available counts from list to add new guest
                var availableName: ArrayList<Int> = arrayListOf()
                for (i in 1 until 16) {
                    var filteredList: List<DineInModel> = arrayListOf()
                    filteredList = dineInCartAdapter.getList()
                        .filter { item -> item.title?.substringAfter("Guest ") == i.toString() }
                        ?: arrayListOf()
                    if (filteredList.isEmpty()) {
                        availableName.add(i)
                    }
                    if (availableName.size >= count) {
                        break
                    }
                }

                for (i in 1..count) {

                    // Check if cartList already contains destroyed guest, if contains change the flag else add new guest
                    try {
                        var commonDineInModel =
                            cartlist[0].dineInList?.single { item -> item.title == "Guest ${availableName[i - 1]}" }
                        if (commonDineInModel != null) {
                            commonDineInModel.isDestroy = false
                            dineInList.add(commonDineInModel)
                        }
                    } catch (e: Exception) {
                        dineInList.add(
                            DineInModel(
                                0,
                                false,
                                0,
                                "Guest ${availableName[i - 1]}",
                                floorPlanTable = cartlist[0].dineInList!![0].floorPlanTable

                            )
                        )
                    }
                }
            }
            existinglist.addAll(dineInList)
            cartlist[0].dineInList = existinglist.toList()
            viewModel.addGuestFromDashBoard(cartlist)
        } else {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), "You can't add more than 15 Guest in an order."
            ) { _, _ ->
            }
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

                viewModel.newCartLogicModifier(
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

    override fun onRemoveGuest(position: Int) {
        if (dineInCartAdapter.getList().isNotEmpty() && dineInCartAdapter.getList().size > 2) {
            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
                cartlist.get(0).dineInList?.forEach {
                    if (it.title == dineInCartAdapter.getList()[position].title) {
                        it.apply {
                            this.isDestroy = true
                        }
                    }
                }
            } else {
                val dineIn = cartlist[0].dineInList as ArrayList<DineInModel>
                val destroyedGuestsList: ArrayList<DineInModel> = ArrayList()
                dineIn.filter { (it.title == dineInCartAdapter.getList()[position].title) }
                    .forEach { destroyedGuestsList.add(it) }
                dineIn.removeAll(destroyedGuestsList.toSet())
                cartlist[0].dineInList = dineIn
            }
            viewModel.addCart(cartlist[0])
        } else {
            viewModel.unableToRemoveGuest(getString(R.string.minimum_one_guest_is_required))
        }
    }

    private fun removeGuestObserver() {
        viewModel.removeGuestSuccess.observe(viewLifecycleOwner) { event ->
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), event.getContentIfNotHandled().toString()
            ) { _, _ -> }
        }
    }

    private fun showMessage() {
        AlertUtils.showCustomAlert(requireContext(), "Order should be less than 1 million usd.")
    }


    private fun setCartAdapter() {

        binding.rvCartList.isNestedScrollingEnabled = false
        binding.rvCartList.disableItemAnimator()
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
                prefProvider.setValue(Constants.REDIRECT_FROM, "")
                updateActiveOrderFlagClear()
                itemListner?.onCancelItemSelected()
                if (prefProvider.getValue(ORDER_TYPE, "").toString() == Constants.DINE_IN) {
                    prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                    prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)
                    viewModel.removeItemDineInList.clear()

                    if (cartlist.size > 0) {

                        val dList = cartlist[0].dineInList ?: arrayListOf()
                        LogUtil.logE(TAG, "dList:  ${Gson().toJson(dList)}")
                        if (dList.isNotEmpty()) {
                            dList[0].floorPlanTable?.id?.let {

                                if (dList[0].floorPlanTable?.status.toString() == Constants.MERGED) {
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

                    prefProvider.setValue(ORDER_TYPE, "")
                    prefProvider.setValue(ORDER_TYPE_NAME, "")
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)

                    getOrderTypes()



                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    clearUpdateFlag()
                    binding.linearButtonView.visible()
                    binding.relPreoceedToFire.gone()
                    arguments?.clear()

                    itemClickListner?.onDineInOrderCleared()

                    viewModel.deleteOrderAfterMarkup()

                } else {
                    clearCustomer()
                    viewModel.deleteCart()
                    cartlist.clear()
                    isOrderUpdate = false
                    prefProvider.setValue(ORDER_TYPE, "")
                    prefProvider.setValue(ORDER_TYPE_NAME, "")
                    prefProvider.setValue(DELIVERY_TYPE, "")
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)

                    itemClickListner?.onDineInOrderCleared()
                    uiSave()
                    getOrderTypes()

                    viewModel.deleteOrderAfterMarkup()

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
        binding.lblLoyaltyBalance.visibility = View.GONE
        displayCustomer()
        refreshItemCalculation()
        prefProvider.setValueboolean(IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        prefProvider.setValueboolean(LOYALTY_ADDED, false)
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
        }
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
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
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
                            Log.e(TAG, ".destroyedListRelPR:  ${viewModel.destroyedList.size}")
                            Log.e("IssueBIS777", "getITems:  ${viewModel.cartModel?.items?.size}")
                            Log.e(
                                "IssueBIS777",
                                "getITemsFromScreen:  ${viewModel.cartModel?.items?.size}"
                            )

                            if (cartlist[0] != null) {

                                cartlist[0] = viewModel.generateCombinedItems(cartlist[0])
                            } else {
                                cartlist[0] = viewModel.addDineInRemovedItems(viewModel.cartModel!!)
                            }

                            val request = viewModel.updateOrder(cartlist[0])
                            Log.e(TAG, "checkITemSize:  ${cartlist[0].items?.size}")




                            if (cartlist[0].orderId != 0) {
                                cartlist[0].orderId?.let { viewModel.updateOrderCall(it, request) }
                            } else {
                                orderId?.let { it1 -> viewModel.updateOrderCall(it1, request) }
                            }

                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
                            prefProvider.setValueboolean(DINE_IN_LIST_EDIT, false)
                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)


                        }

                    } else {

                        createDineInOrder()
                    }
                }
            } else {
                showMessage()
            }
        }

        binding.txtAddCustomer.setOnSingleClickListener {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                if (isFromPayment) {
                    findNavController().navigate(R.id.action_paymentBoldPosFragment_to_addCustomerToGiftCard)
                } else {
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_addCustomerToGiftCard)
                }
                return@setOnSingleClickListener
            }
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
                } else if (viewModel.getSplitCount() > 1 || splitValue > 1) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireActivity(),
                        "Customer can not be changed during split payment."
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

        binding.imgOrderMenu.setOnSingleClickListener {


            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.cart_menu, popupMenu.menu)
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false
                popupMenu.menu.findItem(R.id.menu_add_guest).isVisible = true
            } else {
                popupMenu.menu.findItem(R.id.menu_add_guest).isVisible = false
            }


            if (cartlist.isEmpty()) {
                popupMenu.menu.findItem(R.id.menu_discount).isVisible = false
                popupMenu.menu.findItem(R.id.menu_order_note).isVisible = false
//                popupMenu.menu.findItem(R.id.menu_clear_cart).isVisible = false
            }

            if (prefProvider.getValueInt(CUSTOMER_ID, -1) == -1) {
                popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                try {
                    when (menuItem.itemId) {
                        R.id.menu_clear_cart -> {
                            popupMenu.dismiss() //For resolving BIS-273
                            clearCart()
                        }

                        R.id.menu_remove_customer -> {

                            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == PHONE_ORDER) {
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    getString(R.string.customer_cannot_be_remove_at_the_moment)
                                ) { _, _ -> }
                            } else {
                                if (cartlist.isNotEmpty() && cartlist[0].customer != null) {
                                    cartlist[0].customer = null
                                    viewModel.addCart(cartlist[0])
                                }
                                clearCustomer()
                            }


                        }

                        R.id.menu_add_guest -> {
                            findNavController().navigate(
                                R.id.action_dashboardCategoryBoldPOS_to_addguestcount
                            )
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
                            bundle.putString("isFrom", "orderDiscount")
                            if (prefProvider.isAdmin() || prefProvider.isManager()) {

                                findNavController().navigate(
                                    R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                                    bundle
                                )
                            } else {
                                findNavController().navigate(
                                    R.id.actionboldpos_to_pascodeManagerDailog, bundle
                                )
                            }


                        }
                        /*R.id.menu_note -> {

                        }*/
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            }
            popupMenu.show()

        }

        binding.tvPayNow.setOnClickListener {

            if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER) {

                prefProvider.setValueboolean(OPEN_ORDER_DIRECT_PAY, true)
            }
            prefProvider.setValueboolean(Constants.TIP_ADDED, false)

            if (prefProvider.getValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)) {
                prefProvider.setValue(
                    Constants.OPEN_ORDER_ITEMS,
                    Gson().toJson(cartAdapter.cartList)
                )
            }

            if (cartAdapter.cartList.isNotEmpty()) {
                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
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
                        prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
                        clearObserver()
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment,
                            bundle
                        )
                    }
                } else {
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
                        clearObserver()
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
            try {
                prefProvider.setValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)
                if (isOrderUpdate == false) {
                    prefProvider.setValue(
                        Constants.OPEN_ORDER_ITEMS,
                        ""
                    )

                }
                if (cartAdapter.cartList.isNotEmpty()) {
                    prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)
                    prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)

                    prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
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
                        if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER) {
                            ordertype = prefProvider.getValue(ORDER_TYPE, "")
                            ordertypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 0)
                        } else {
                            run breaking@{
                                viewModel.ordertypelist.forEach {
                                    if (it.orderType == OPEN_ORDER) {
                                        ordertype = it.orderType
                                        ordertypeId = it.id
                                        return@breaking
                                    }
                                }
                            }


                        }

                        if (prefProvider.getValue(ORDER_TYPE, "").toString()
                                .trim() == PHONE_ORDER.toString().trim()
                        ) {

                            viewModel.ordertypelist.forEach {
                                if (it.orderType == PHONE_ORDER) {
                                    ordertype = it.orderType
                                    ordertypeId = it.id
                                }


                            }
                        }


                        Log.e("ordertypeId :: ", ordertypeId.toString())

                        if (viewModel.restrictedAmount(binding.txtTotal)) {


                            viewModelPayment.updateOrder(
                                isOrderUpdate,
                                orderId,
                                paymentId,
                                paymentOfflineId,
                                orderOfflineId
                            )

                            val cartList = viewModel.generateCombinedItems(viewModel.cartModel!!)
                            cartList.openOrderType = Constants.PICK_UP
                            cartList.orderType = ordertype
                            cartList.orderTypeId = ordertypeId

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

                            LogUtil.logE(TAG, "UpdateOrderItemsList  ${cartList.items?.size}")
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
                            isSaveOrder = true
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun clearObserver() {
        viewLifecycleOwnerLiveData.removeObservers(viewLifecycleOwner)
        viewModel.mAllWordsFlow(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).asLiveData().removeObservers(viewLifecycleOwner)

        viewModel.venueDataLocal().removeObservers(viewLifecycleOwner)
        onDestroy()

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
            floorModel.tableName = cartlist.get(0).dineInList?.get(1)?.floorPlanTable?.tableName


        }
        viewModel.ordertypelist.forEach {
            if (it.orderType.toLowerCase() == Constants.DINE_IN.toLowerCase()) {
                cartlist[0].orderTypeId = it.id
                cartlist[0].orderType = it.orderType
            }
        }
        cartlist[0].openOrderType = Constants.PICK_UP
        var finalDiscount = 0.0
//        if (BuildConfig.DEBUG == false) {
//            finalDiscount = cartlist[0].discountPrice + viewModel.totalDiscount
//        } else {
        finalDiscount = viewModel.totalDiscount
//        }
        Log.e("checkDiscount", "totalDiscount:  ${viewModel.totalDiscount}")
        Log.e("checkDiscount", "totalDiscountdiscountPrice:  ${cartlist[0].discountPrice}")

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
            totalDiscount = finalDiscount,
            0.0,
            floorPlanDetails = floorModel
        )

        if (orderRequestModel != null) {
            viewModel.submit(orderRequestModel)
        }

    }

    private fun getOrderTypes() {
        orderTypeAdapter = OrderTypeAdapter()
        orderTypeAdapter?.setCallback(this)
        binding.rvOrderType?.adapter = orderTypeAdapter

        viewModel.orderTypes().observe(requireActivity()) {

            it.data?.let { it1 -> orderTypeAdapter?.addAll(it1.filter { it.primaryOrderType }) }
        }

    }

    override fun onItemClickListener(view: View?, pos: Int) {

        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }

        val model = orderTypeAdapter?.getItem(pos)

        model?.id?.let { prefProvider.setValueInt(ORDER_TYPE_ID, it) }
        model?.name?.let { prefProvider.setValue(ORDER_TYPE_NAME, it) }

        Log.e(TAG, "checkOrderType  ${model?.orderType}")

        prefProvider.setValue(DELIVERY_TYPE, PICK_UP)

        prefProvider.setValue(Constants.REDIRECT_FROM, "")

        if (model?.orderType == DINE_IN) {
            Log.e(TAG, "InsideDine inNew")
            checkOrderType()
            dineInCallback?.onDineInClickListener()
        } else if (model?.orderType == PHONE_ORDER) {

            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_phoneOrderFragment
            )
            model.orderType.let { prefProvider.setValue(ORDER_TYPE, it) }

        } else {
            Log.e(TAG, "InsideDine inNoDine")
            model?.orderType?.let { prefProvider.setValue(ORDER_TYPE, it) }
            checkOrderType()

            addObserver()

            DashboardCategoryBoldPOS.newInstance().keypadShow(true)
            increaseOnGoingOrderCounter()
        }


    }

    override fun onStart() {
        super.onStart()

        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String?) {
        // Do something
        if (event != null) {
            Log.e("onMessageEvent", event)
        }
        prefProvider.setValue(DELIVERY_TYPE, "")

        viewModel.cartModel = null

        checkOrderType()

        addObserver()

        DashboardCategoryBoldPOS.newInstance().keypadShow(true)

        increaseOnGoingOrderCounter()


    }

    private fun increaseOnGoingOrderCounter() {
        lifecycleScope.launch {
            viewModel.increaseOnGoingOrderCounter()
        }
    }
}


