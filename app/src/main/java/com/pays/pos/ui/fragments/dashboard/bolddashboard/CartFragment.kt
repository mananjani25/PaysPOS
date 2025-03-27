package com.pays.pos.ui.fragments.dashboard.bolddashboard

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.payments.design.Dejavoo
import com.pays.payments.design.PaymentGatewayFactory
import com.pays.payments.design.PaymentGatewayType
import com.pays.payments.design.TransactionType
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.CashDiscountModel
import com.pays.pos.data.entities.OrderTypeBackup
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.DineInOrderDetailAttributes
import com.pays.pos.data.model.GuestPaymentCalculationModel
import com.pays.pos.data.model.PreAuthData
import com.pays.pos.data.model.requestModel.OrderItemsAttribute
import com.pays.pos.data.model.requestModel.PaymentAttributes
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CUSTOMER_ID
import com.pays.pos.data.remote.Constants.DELETE
import com.pays.pos.data.remote.Constants.DELIVERY
import com.pays.pos.data.remote.Constants.DELIVERY_TYPE
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.GIFT_CARD
import com.pays.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.pays.pos.data.remote.Constants.IS_LAST_ITEM_DELETE
import com.pays.pos.data.remote.Constants.IS_PAX_PAYMENT_FAILED
import com.pays.pos.data.remote.Constants.IS_PAYMENT_SCREEN
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_ID
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_OFFLINE_ID
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_PAYMENT_ID
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID
import com.pays.pos.data.remote.Constants.LOYALTY_ADDED
import com.pays.pos.data.remote.Constants.MANUAL_SALE
import com.pays.pos.data.remote.Constants.OPEN_ORDER
import com.pays.pos.data.remote.Constants.OPEN_ORDER_DIRECT_PAY
import com.pays.pos.data.remote.Constants.OPEN_ORDER_UPDATE_FOR_PRINT
import com.pays.pos.data.remote.Constants.OPTION_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.data.remote.Constants.PERCENTAGE
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.PICK_UP
import com.pays.pos.data.remote.Constants.PRE_AUTH_AMOUNT
import com.pays.pos.data.remote.Constants.PRE_AUTH_DETAILS
import com.pays.pos.data.remote.Constants.REDIRECT_FROM
import com.pays.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.WHOLE_AMOUNT
import com.pays.pos.databinding.FragmentCartBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.logger.MessageEvent
import com.pays.pos.logger.SyncCustomerEvent
import com.pays.pos.ui.adapter.DineInAdapter
import com.pays.pos.ui.adapter.OrderTypeAdapter
import com.pays.pos.ui.adapter.boldpos.CartItemsAdapter
import com.pays.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.Event
import com.pays.pos.utils.InternetUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.DineInOrderCallBack
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.ItemClickListner
import com.pays.pos.utils.callback.ItemListner
import com.pays.pos.utils.callback.MyCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.disableItemAnimator
import com.pays.pos.utils.extensions.getColor
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.invisible
import com.pays.pos.utils.extensions.isVisible
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import com.pays.pos.utils.subTotalToDouble
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory


@AndroidEntryPoint
class CartFragment : Fragment, MyCallback, DineInAdapter.DineInCallback, ItemCallback {

    constructor(
        itemClickListner: ItemClickListner?,
        itemListner: ItemListner?,
        isFromPaymentDinein: Boolean = false,
        guestCalModel: GuestPaymentCalculationModel? = null,
        isGuestPayment: Boolean = false,
        dineInCallback: DineInOrderCallBack? = null,
        isFromDashboard: Boolean? = false
    ) : this() {
        this.itemClickListner = itemClickListner
        this.itemListner = itemListner
        this.isFromPaymentDinein = isFromPaymentDinein
        this.guestCalModel = guestCalModel
        this.isGuestPayment = isGuestPayment
        this.dineInCallback = dineInCallback
        this.isFromDashboard = isFromDashboard
    }

    constructor() : super()

    /*--------------------Constructor params--------------------*/
    var itemClickListner: ItemClickListner? = null
    var itemListner: ItemListner? = null
    var isFromPaymentDinein: Boolean = false
    var guestCalModel: GuestPaymentCalculationModel? = null
    var isGuestPayment: Boolean = false
    var dineInCallback: DineInOrderCallBack? = null
    var isFromDashboard: Boolean? = false
    /*--------------------Constructor params--------------------*/

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
    private lateinit var cartItemsAdapter: CartItemsAdapter
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var future_delivery_date: String = ""
    private var paymentId: Int? = null
    private var future_delivery_time: String = ""
    lateinit var cashDiscountModel: CashDiscountModel
    private val dineInViewModel by activityViewModels<DineInOrderTableViewModel>()
    var cashDiscountType = ""
    var cartlist: ArrayList<CartModel> = arrayListOf()
    var tempList: JSONArray? = null
    private var tempStored: Boolean = false
    var itemModified = false
    var cartModelsList: ArrayList<CartModel> = arrayListOf()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val viewModelPayment by activityViewModels<PaymentViewModel>()
    var updateBundle: Bundle? = null
    var isFromPayment: Boolean = false
    var isActiveOrder: Boolean = false
    var reorder: Boolean = false

    private val DELAY_MILLIS = 200L

    private var previousClickTimeMillis = 0L

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

    @Inject
    lateinit var rolePermission: RolePermission
    private val TAG = "CartFragment"

    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

        viewModel.updateCartFooter.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandled()?.let { _it ->
                if (_it) {
                    Log.d("CurrentItems::", Gson().toJson(viewModel.currentCartItems))

                    addObserver()

                }
            }

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
        if (arguments?.getInt("fragmentId") != null) fragmentId = arguments?.getInt("fragmentId")
        if (arguments?.getInt("checkoutHeaderId") != null) checkoutHeaderId =
            arguments?.getInt("checkoutHeaderId")!!

        if (arguments?.getInt("dashboardHeaderId") != null) dashboardHeaderId =
            arguments?.getInt("dashboardHeaderId")!!

        isFromPayment = arguments?.getBoolean("isFromPayment") ?: false
        isActiveOrder = arguments?.getBoolean("isFromActiveOrder") ?: false

        if (!prefProvider.getValueboolean(Constants.BACK_FROM_PAYMENT, false)) {
            prefProvider.setValueboolean(Constants.NO_NEED_TO_PRINT, false)
        }
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
                            isOrderUpdate, orderId, paymentId, paymentOfflineId, orderOfflineId
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
        setUpdateCartFooterObservable()
        return binding.root
    }

    private fun getLoyaltyPointListObserver(view: View?) {
        viewModel.loyaltyPoints.observe(viewLifecycleOwner) { loyaltyPoints ->
            loyaltyPoints.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        try {
                            if (view != null) {
                                ProgressUtils.dismissProgressDialog()
                                run breaking@{
                                    if (resource.data?.isNotEmpty() == true) {
                                        resource.data.forEach {
                                            if (it.isEnable && !it.isDeleted) {
                                                var data: TbCustomer? =
                                                    prefProvider.getCustomerData()
                                                if (data != null) {
                                                    if (viewModel.loyaltyPointCondition(data) && cartModelsList.isNotEmpty()) {

                                                        binding.liinearInfoLayout.layoutParams.height =
                                                            resources.getDimension(R.dimen._70sdp)
                                                                .toInt()

                                                        if (prefProvider.getValue(
                                                                ORDER_TYPE,
                                                                ""
                                                            ) != DINE_IN
                                                        )
                                                            binding.relativeLoylatyPoints.visibility =
                                                                View.VISIBLE
                                                        binding.lblLoyaltyPoints.visibility =
                                                            View.VISIBLE
                                                        binding.lblLoyaltyBalance.visibility =
                                                            View.VISIBLE
                                                        LogUtil.logE(TAG, "InsideLoyalty")
                                                        LogUtil.logE(
                                                            TAG,
                                                            Gson().toJson(viewModel.redeemLoyaltyInfo)
                                                        )
                                                        binding.txtLoyaltyAmount.text = "- $${
                                                            String.format(
                                                                "%.2f",
                                                                viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                                            )
                                                        }"
                                                        binding.txtLoyaltyPoints.text =
                                                            "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                                        binding.txtLoyaltyBalance.text = "${
                                                            if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                                                                viewModel.redeemLoyaltyInfo.remainingLoyaltyPoints
                                                            } else {
                                                                viewModel.redeemLoyaltyInfo.availablePoints
                                                            }
                                                        }"
                                                        binding.checkloylaty.isChecked =
                                                            viewModel.redeemLoyaltyInfo.needToApplyLoyalty

                                                    }
                                                    else {
                                                        if (!isFromPayment) {
                                                            binding.checkloylaty.isChecked = false
                                                            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = false
//                                                            prefProvider.setValueboolean( Constants.LOYALTY_ADDED, false )
//                                                            prefProvider.setValueboolean( Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
                                                        }
                                                    }
                                                }
                                                return@breaking
                                            } else {
                                                if (!isFromPayment) {
                                                    binding.relativeLoylatyPoints.gone()
                                                    binding.lblLoyaltyPoints.gone()
                                                    binding.lblLoyaltyBalance.gone()
                                                    binding.checkloylaty.isChecked = false
                                                }
                                            }
                                        }
                                    } else {
                                        if (!isFromPayment) {
                                            binding.relativeLoylatyPoints.gone()
                                            binding.lblLoyaltyPoints.gone()
                                            binding.lblLoyaltyBalance.gone()
                                            binding.checkloylaty.isChecked = false
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            //                            binding.btnSignUpOrCheckIn.gone()
                            //                            binding.tvRewards.gone()
                            //                            binding.tvMessage.text="Please login to start"
                        }
                    }

                    Status.ERROR -> {
                        if (!isFromPayment) {
                            binding.relativeLoylatyPoints.gone()
                            binding.lblLoyaltyPoints.gone()
                            binding.lblLoyaltyBalance.gone()
                            binding.checkloylaty.isChecked = false
                        }
                    }

                    Status.LOADING -> {
                    }
                }
            }
        }
    }

    private fun setUpdateCartFooterObservable() {
        viewModel.updateCartFooterObservable.observe(viewLifecycleOwner,
            object : androidx.lifecycle.Observer<Event<Boolean>> {
                override fun onChanged(t: Event<Boolean>?) {
                    t?.getContentIfNotHandled()?.let {
                        if (it) {
                            updateCartFooter(viewModel.currentCartItems)
                        }
                    }
                }
            })
    }

    fun enablePreAuth() {
        if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER && !isFromPayment) {

            binding.preAuthOption?.visible()

            binding.preAuthOption?.apply {

                isChecked = false
                isEnabled = true
                setTextColor(Color.RED)

                if (prefProvider.getValue(PRE_AUTH_DETAILS, "").isNotEmpty()) {
                    visible()
                    isChecked = true
                    isEnabled = false
                    setTextColor(Color.GREEN)
                } else {
                    isChecked = false
                    isEnabled = true
                    setTextColor(Color.RED)
                    setOnClickListener {

                        if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                            if (prefProvider.isManager() || prefProvider.isAdmin()) {
                                if (prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    makePaxPreAuthRequest()
                                } else if (!prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    makeDejavooPreAuthPaymentRequest()
                                } else {
                                    isChecked = false
                                    activity?.let {
                                        AlertUtils.showCustomAlertWithListenerWithOK(
                                            it,
                                            getString(R.string.pax_connect_error),
                                            null
                                        )
                                    }
                                }
                            } else {
                                isChecked = false
                                AlertUtils.showCustomAlert(
                                    requireContext(),
                                    "You do not have permission to access this feature.\nPlease contact your manager."
                                )
                            }
                        } else {
                            isChecked = false
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                "Please check your Network Connectivity."
                            )
                        }
                    }
                }

                try {
                    if (viewModelPayment.preAuthData != null && viewModelPayment.preAuthData!!.refNum.isNotEmpty() || viewModelPayment.preAuthData!!.refNum.isNotEmpty()) {
                        isChecked = true
                        isEnabled = false
                        setTextColor(Color.GREEN)
                    }
                } catch (e: Exception) {

                }


            }
        } else binding.preAuthOption?.gone()
    }

    private fun setUpPreAuthData() {
        view?.let {
            viewModel.isPreAuthCartOpened.observe(viewLifecycleOwner) {
                if (it) {
                    enablePreAuth()
                } else {
                    binding.preAuthOption?.gone()
                }
            }
        }

       

//        if(prefProvider.getValue(ORDER_TYPE,"") == OPEN_ORDER && !isFromPayment) {
//
//            binding.preAuthOption?.visible()
//
//            binding.preAuthOption?.apply {
//
//                isChecked = false
//                isEnabled = true
//                setTextColor(Color.RED)
//
//                if(prefProvider.getValue(PRE_AUTH_DETAILS,"").isNotEmpty() ) {
//                    visible()
//                    isChecked = true
//                    isEnabled = false
//                    setTextColor(Color.GREEN)
//                }else {
//                    isChecked = false
//                    isEnabled = true
//                    setTextColor(Color.RED)
//                    setOnClickListener {
//
//                        if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
//                            if (prefProvider.isManager() || prefProvider.isAdmin()) {
//                                if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
//                                    makePaxPreAuthRequest()
//                                }else if (!prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
//                                    makeDejavooPreAuthPaymentRequest()
//                                }
//                                else {
//                                    isChecked = false
//                                    activity?.let {
//                                        AlertUtils.showCustomAlertWithListenerWithOK(
//                                            it,
//                                            getString(R.string.pax_connect_error),
//                                            null
//                                        )
//                                    }
//                                }
//                            } else {
//                                isChecked = false
//                                AlertUtils.showCustomAlert(
//                                    requireContext(),
//                                    "You do not have permission to access this feature.\nPlease contact your manager."
//                                )
//                            }
//                        } else {
//                            isChecked = false
//                            AlertUtils.showCustomAlert(requireContext(), "Please check your Network Connectivity.")
//                        }
//                    }
//                }
//
//                try {
//                    if (viewModelPayment.preAuthData !=null && viewModelPayment.preAuthData!!.refNum.isNotEmpty() || viewModelPayment.preAuthData!!.refNum.isNotEmpty()) {
//                        isChecked = true
//                        isEnabled = false
//                        setTextColor(Color.GREEN)
//                    }
//                }catch (e:Exception) {
//
//                }
//
//
//            }
//        } else
//            binding.preAuthOption?.gone()
    }


    @Inject
    lateinit var paymentGatewayFactory: PaymentGatewayFactory
    lateinit var paymentCoroutineScope: CoroutineScope

    val paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->
            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeValorPaymentRequest()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }

    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(xmlContent.byteInputStream())
    }

    private fun makeDejavooPreAuthPaymentRequest() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            val amt = PRE_AUTH_AMOUNT
            val tip_amt = 0

            /* Process Payment */
            var dejavoo = Dejavoo(
                registerId = "4986101",
                authKey = "kwg2GRbykg",
                tpn = "659324491704",
                paymentType = "Credit",
                transType = "Auth",
                amount = amt.toString(),
                tip = "",
                refId = "AuthRef${System.currentTimeMillis()}",
                printReceipt = false,
                performedBy = prefProvider.employeeName(),
                isProd = false,
                txnType = TransactionType.CREDIT_SALE
            )

            context?.let {
                paymentGateway.performPreAuth(
                    it.applicationContext,
                    dejavoo,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )

                        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                        factory.setNamespaceAware(true)
                        val xpp: XmlPullParser = factory.newPullParser()
                        xpp.setInput(StringReader(transactionJsonResponse))
                        var eventType = xpp.eventType

                        val parsedXml =
                            parseXml(transactionJsonResponse)/*.getElementsByTagName("xmp").item(0)?.textContent.toString()*/
                        var Message = ""
                        var RefId = ""
                        var RegisterId = ""
                        var TPN = ""
                        var AuthCode = ""
                        var PNRef = ""
                        var TransNum = ""
                        var ResultCode = ""
                        var RespMSG = ""
                        var PaymentType = ""
                        var Voided = ""
                        var TransType = ""
                        var SN = ""
                        var ExtData = ""
                        with(parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes) {
                            for (i in 0 until this.length) {

                                when ((this.item(i) as Element).tagName.toString()) {
                                    "Message" -> Message =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RefId" -> RefId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RegisterId" -> RegisterId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TPN" -> TPN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "AuthCode" -> AuthCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PNRef" -> PNRef =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransNum" -> TransNum =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ResultCode" -> ResultCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RespMSG" -> RespMSG =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PaymentType" -> PaymentType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "Voided" -> Voided =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransType" -> TransType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "SN" -> SN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ExtData" -> ExtData =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    else -> {

                                    }
                                }
                            }
                        }

                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            dismissProgressDialogWithAlert(RespMSG.replace("%20", " "))
                        } else if (Message.contains("Approved")) {

                            viewModelPayment.setPAXData(RefId, ExtData)

                            val paymentAttributes = PaymentAttributes()

                            paymentAttributes.apply {
                                amount = PRE_AUTH_AMOUNT
                                cardName = "Card"
                                cardNumber = ""
                                ecr_ref_num = RefId
                                employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                                ext_data = "${Constants.DEJAVOO} : $ExtData?"
                                global_uniq_id = PNRef
                                pax_transaction_token = ""
                                payableType = "Order"
                                paymentType = "Card"
                                ref_num = TransNum
                                subTotal = 1.0
                                terminalId = prefProvider.getTerminalId()
                            }

                            prefProvider.setValue(
                                PRE_AUTH_DETAILS,
                                Gson().toJson(paymentAttributes)
                            )
                            viewModel.paymentAttributes = paymentAttributes

                            viewModelPayment.preAuthData = PreAuthData(
                                ecrRefNum = paymentAttributes.ecr_ref_num,
                                refNum = paymentAttributes.ref_num
                            )

                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Dejavoo: ", errorMessage)
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeDejavooPaymentRequest()-> ${
                                        Gson().toJson(
                                            errorMessage
                                        )
                                    } "
                                )
                            )
//                        dismissProgressDialogWithAlert()
                    }
                )
            }
        }
    }


    private fun dismissProgressDialogWithAlert(errorMessage: String? = null) {
        runOnUiThread {
            ProgressUtils.dismissProgressDialog()
            dismissProgressDialog()
            if (errorMessage != null) {
                AlertUtils.showCustomAlert(requireContext(), errorMessage)
            }
        }
    }

    // To check selected order type
    private fun checkOrderType() {

//        setUpPreAuthData()

//        saveVisibility()
        if (prefProvider.getValue(ORDER_TYPE, "").isEmpty()) {

            if (viewModel.fromSaveOrderToAllOrders) {
                binding.rlCartView.visible()
                binding.rvOrderType.gone()
                Log.e("Dashboard Tracking", "Dashboard tracking rvOrderVisible TRUE")
            } else {
                binding.rlCartView.gone()
                binding.rvOrderType.visible()
                Log.e("Dashboard Tracking", "Dashboard tracking rvOrderVisible FALSE")
            }

            binding.orderTypeDisplay.text = getString(R.string.current_order)
        } else {
            binding.rlCartView.visible()
            binding.rvOrderType.gone()

            if (prefProvider.getValue(
                    ORDER_TYPE, TAKEOUT
                ) == DINE_IN || prefProvider.getValueboolean(
                    Constants.IS_ADD_VALUE_IN_GIFT_CARD, false
                )
            ) {
                binding.txtAddCustomer.invisible()
                binding.rvCartDineIn.visible()
            } else {
                if (isAdded && parentFragmentManager != null) {
                    if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text == getString(
                            R.string.add_customer2
                        )
                    ) {
                        binding.txtAddCustomer.gone()
                    } else if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text != getString(
                            R.string.add_customer2
                        )
                    ) {
                        binding.txtAddCustomer.visible()
                        binding.txtAddCustomer.isEnabled = false
                    }
                }

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

                binding.orderTypeDisplay.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(p0: View?) {
                        if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                            findNavController().navigate(
                                R.id.action_dashboardCategoryBoldPOS_to_changeOrderTypeDialog,
                            )
                        }
                    }
                })
                /*binding.orderTypeDisplay.setOnClickListener {
                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_changeOrderTypeDialog,
                        )
                    }
                }*/
            } else {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        binding.orderTypeDisplay.text =
                            getString(R.string.current_order) + ": " + prefProvider.getValue(
                                ORDER_TYPE_NAME, ""
                            )

                    }
                })
//                binding.orderTypeDisplay.text =
//                    getString(R.string.current_order) + ": " + prefProvider.getValue(
//                        ORDER_TYPE_NAME,
//                        ""
//                    )
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

        viewModel.dineInHeaderPosition = 0
        viewModel.currentSelectedHeaderDineIn = 0

        initListeners()
        setCartAdapter()
        getDineInData()
        callback()
        setupLoyalytyPoints()
        addObserver()
        setupTaxAdapter()
        getOrderTypes()
        getLoyaltyPointListObserver(view)

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

        setCustomerDisplayLoyalty()
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
                    isOrderUpdate, orderId, paymentId, paymentOfflineId, orderOfflineId
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

        if (!isFromPayment) {
            var totalAmount = binding.txtTotal.text.toString().replace(Regex("[^0-9.]"), "").toDouble()
            totalAmount += viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
            if (viewModel.activeLoyaltyProgram?.amount != null && totalAmount >= viewModel.activeLoyaltyProgram?.amount!! && totalAmount != 0.00) {
                binding.checkloylaty.visible()
                binding.txtLoyaltyAmount.visible()
                binding.txtLoyaltyPoints.visible()
                binding.txtlabelloyaltyPoints.visible()
            } else {
                binding.checkloylaty.gone()
                binding.checkloylaty.isChecked =false
                binding.txtLoyaltyAmount.gone()
                binding.txtLoyaltyPoints.gone()
                binding.txtlabelloyaltyPoints.gone()
            }
        }

        uiSave()

    }


/*----------------Customer Loyalty-----------------*/

    private fun setCustomerDisplayLoyalty() {
        viewModel.clickTakeOut.observe(
            viewLifecycleOwner,
            object : androidx.lifecycle.Observer<Event<Boolean>> {
                override fun onChanged(t: Event<Boolean>?) {
//                    CoroutineScope(Dispatchers.Main).launch {
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            binding.rvOrderType.layoutManager?.childCount?.let {
                                for (position in 0..it) {
                                    if (binding.rvOrderType.findViewHolderForAdapterPosition(
                                            position
                                        )?.itemView?.findViewById<TextView>(
                                            R.id.txtTitle
                                        )?.text?.contains(/*"Take out"*/binding.orderTypeDisplay.text.toString(),
                                            ignoreCase = true
                                        ) ?: false
                                    ) {
                                        performClickOnOrderTypeAndSetCustomer(position)
                                        break
                                    } else if (!binding.orderTypeDisplay.text.toString().trim()
                                            .contains(':')
                                    ) {
                                        performClickOnOrderTypeAndSetCustomer(0)
                                        break
                                    }
                                }
                            }
                        }
                    })
//                    }
                }
            })
    }

    private fun performClickOnOrderTypeAndSetCustomer(position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.rvOrderType.findViewHolderForAdapterPosition(
                position
            )?.itemView?.performClick()
            if (prefProvider.getValueInt(CUSTOMER_ID, -1) != -1) {
                displayCustomer()
            }
//                                binding.rvOrderType.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
        }
    }
/*----------------Customer Loyalty-----------------*/

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
                isOrderUpdate, orderId, paymentId, paymentOfflineId, orderOfflineId
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
            binding.txtLoyaltyBalance.text = "${
                if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                    viewModel.redeemLoyaltyInfo.remainingLoyaltyPoints
                } else {
                    viewModel.redeemLoyaltyInfo.availablePoints
                }
            }"
//            addObserver()
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.onDisplayChanged()
            }
        }

    }

    private fun callback() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_customer", viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val data: TbCustomer = bundle.getParcelable<TbCustomer>("data") as TbCustomer
            viewModel.assignCustomer = data
            viewModel.setcheckedLoyaltyApply(false)
            viewModel.selectedCustomer = data
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_guestcount", viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val count: Int = bundle.getInt("count")
            addGuestToOrder(count)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_order_type_change", viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            var data: TbOrderType = bundle.getParcelable<TbCustomer>("orderData") as TbOrderType

            checkOrderType()
            addObserver()
            viewModel.cartModel?.let {
                it.orderTypeName = data.name
                it.orderType = data.orderType
                it.orderTypeId = data.id
                viewModel.updateCartModel(cartModel = it)
            }

            if (viewModel.currentCartItems.isNotEmpty()) {
                viewModel.currentCartItems.forEach {
                    it.orderTypeName = data.name
                    it.orderType = data.orderType
                    it.orderTypeId = data.id
                }
                viewModel.addOrderItemsToCartItems(viewModel.currentCartItems)
            }
        }
    }


    private fun getDineInData() {
        if (updateBundle != null) {

            viewModel.dineInHeaderPosition = 0
            viewModel.dineInSelectedItemHeaderPos = 0

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
        viewModel.serviceChargesList.let { serviceChargeList ->
            serviceChargeList.forEach { serviceCharge ->
                if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                    serviceCharge.max_guest_count?.let { maxGuestCount ->
                        if (maxGuestCount >= maxValue) {
                            maxValue = maxGuestCount
                            serviceChargeId = serviceCharge.id
                        }
                    }
                }
            }
        }
        return serviceChargeId
    }

    // calculate service charge base on guest count for dine in order type
    private fun getServiceChargeFromGuestCount(guestCount: Int): List<TbServiceCharge> {
        var list: List<TbServiceCharge> = listOf()
        var isApplied = false
        viewModel.serviceChargesList.forEach {
            if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER && it.min_guest_count != null && it.max_guest_count != null && guestCount > 0) {
                if (isInRange(it.min_guest_count, it.max_guest_count, guestCount)) {
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



        dineInCartAdapter.setList(dineInList, viewModel.currentCartItems)

        if (cartModelsList.isEmpty()) {
            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                employeeID = prefProvider.getValueInt(EMPLOYEE_ID, -1)
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, -1)
                orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
                orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()

                serviceCharge = getServiceChargeFromGuestCount(numOfGuest)

                if (orderTypeId == -1) {
                    runBlocking {
                        async {
                            viewModel.getOrderTypeBackupList(employeeID)?.let {
                                orderTypeId = (it.get(0).orderType) ?: -1
                            }
                        }.await()
                    }
                }
            }

            cartModelsList.add(cartModel)
        }

        viewModel.dineInHeaderPosition = 0
        viewModel.dineInSelectedItemHeaderPos = 0
        cartModelsList.get(0).orderType = Constants.DINE_IN

        try {
            viewModel.newCartLogicModifier(
                cartModelsList, null, Constants.ADD, false, dineInList = dineInList
            )

        }catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun checkDineInEditOrder() {


        if (viewModel.currentCartItems.isEmpty()) {
            val dineInList: ArrayList<DineInModel> =
                ArrayList(viewModel?.dineInAdapterBackup?.getList()) ?: arrayListOf()
//
//            val guestcount = viewModel.dineInAdapterBackup?.getList()?.count { it.isHeader == 0 } ?: 1
//
//            dineInList.add(DineInModel(0, true, 0, "Whole Table", floorPlanTable = orderFloorDetails))
//            for (i in 1..guestcount) {
//                dineInList.add(
//                    DineInModel(
//                        0,
//                        false,
//                        0,
//                        "Guest $i",
//                        floorPlanTable = orderFloorDetails,
//
//                        )
//                )
//            }


            dineInCartAdapter.setList(dineInList, viewModel.currentCartItems)



            if (cartModelsList.isEmpty()) {
                val cartModel = CartModel().apply {
                    terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                    employeeID = prefProvider.getValueInt(EMPLOYEE_ID, -1)
                    locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, -1)
                    orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
                    orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()

                    serviceCharge = getServiceChargeFromGuestCount(numOfGuest)

                    if (orderTypeId == -1) {
                        runBlocking {
                            async {
                                viewModel.getOrderTypeBackupList(employeeID)?.let {
                                    orderTypeId = (it.get(0).orderType) ?: -1
                                }
                            }.await()
                        }
                    }
                }

                cartModelsList.add(cartModel)
            }

            viewModel.dineInHeaderPosition = 0
            viewModel.dineInSelectedItemHeaderPos = 0
            cartModelsList.get(0).orderType = Constants.DINE_IN
            viewModel.newCartLogicModifier(
                cartModelsList, null, Constants.ADD, false, dineInList = dineInList
            )

        }


//        if (/*arguments?.getBoolean("is_dine_in_edit") == */false) {
//            val dineInList = arguments?.getParcelableArrayList<DineInModel>("dine_in_list")
//            val dineInItemsList =
//                arguments?.getParcelableArrayList<TbCartItem>("dine_in_cart_items")
//
//            if (dineInList?.isNotEmpty() == true) {
//                //  binding.layoutCart.txtOrderType.setText("Dine In")
//
//                dineInCartAdapter = DineInAdapter()
//                dineInCartAdapter.setListner(this)
//                //dineInCartAdapter.setList(dineInList)
//                dineInCartAdapter.setList(dineInList, viewModel.currentCartItems)
//
//
//                if (cartModelsList.isEmpty()) {
//                    val cartModel = CartModel().apply {
//                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
//                        employeeID = prefProvider.getValueInt(EMPLOYEE_ID, -1)
//                        locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
//                        orderTypeId = 2
//                        orderType = Constants.DINE_IN
//                        orderTypeName = Constants.DINE_IN
//
//                        serviceCharge = serviceChargesList
//                        orderId = arguments?.getInt("orderId")
//                        listOfItemRemoved = dineInList[0].listOfItemsMoved
//
//                    }
//
//                    orderId = arguments?.getInt("orderId")
//                    cartModelsList.add(cartModel)
//                }
//
//                var orderTableData: GetOrderDetailsResponse.Data.FloorPlanTable? =
//                    arguments?.getParcelable("tableDetails")
//
//                dineInList.forEach {
//                    it.floorPlanTable = orderTableData
//                }
//
//                prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
//                prefProvider.setValueInt(
//                    ORDER_TYPE_ID, prefProvider.getValueInt(ORDER_TYPE_ID, 0)
//                )
//
//                viewModel.orderItemDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
//                viewModel.totalDiscount = arguments?.getDouble("totalDiscount") ?: 0.0
//                /*viewModel.newCartLogicModifier(
//                    cartlist,
//                    null,
//                    Constants.ADD,
//                    false,
//                    dineInList = dineInList
//                )*/
//
//                // IMPORTANT - remove this as this is just for logs
////                dineInItemsList?.forEach {
////                    it.taxes = arrayListOf()
////                    Log.d(TAG, "testDineInUpdate dineInItemsList: " + Gson().toJson(it))
////                }
//                // IMPORTANT - remove this as this is just for logs
////                viewModel.currentCartItems.forEach {
////                    it.taxes = arrayListOf()
////                    Log.d(TAG, "testDineInUpdate dineInItemsList: " + viewModel.currentCartItems)
////                }
//
//                CoroutineScope(Dispatchers.IO).launch {
//                    viewModel.currentCartItems.forEach {
//                        viewModel.addItemToCartItems(it)
//                    }
//                }
//
//              //  viewModel.updateDineInCart(viewModel.currentCartItems, null, ADD, false, dineInList)
//
//
//            }
//
//
//        }

        binding.rvCartDineIn.visibility = View.VISIBLE
        // AlertUtils.showAlert(requireContext(),"CART VISIBILITY = ${binding.rvCartDineIn.visibility == View.VISIBLE}")
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
            Log.e(TAG, "checkListBeforeUpdate  ${Gson().toJson(taxlistData)}")
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
            viewModel.getManualSaleCartItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT), prefProvider.getValueInt(EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                LogUtil.logE(TAG, "listSize  ${Gson().toJson(it)}")
                viewModel.setCurrentCartItems(it)
                if (it.isNotEmpty()) {
                    if (isFromPayment) {
                        viewModel.selectedCustomer = null
                        viewModel.redeemLoyaltyInfo.isLoyaltyApplied = false
                        viewModel.redeemLoyaltyInfo.needToApplyLoyalty = false
                        if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                ORDER_TYPE, TAKEOUT
                            ) != Constants.GIFT_CARD
                        ) {
                            binding.linearCashDiscount.visible()
                            if (prefProvider.getValue(
                                    OPTION_TYPE, "CashDiscount"
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



                    it.toCollection(arrayListOf()).let { it1 -> cartItemsAdapter.submitList(it1) }

                    if (viewModel.cartModel?.taxlistDynamic?.isNotEmpty() == true) {
                        Log.d(TAG, "addObserver: " + viewModel.cartModel?.taxlistDynamic?.size)
                        setupTaxAdapter()
                        taxBirfurcationAdapter.setList(viewModel.cartModel?.taxlistDynamic as ArrayList<TaxData>)
                    }
                    if (viewModel.order_note.isNotEmpty()) {
                        binding.relativeOrderNotes?.visibility = View.VISIBLE
                        binding.txtOrderNote?.text = viewModel.order_note
                    } else {
                        binding.relativeOrderNotes?.visibility = View.GONE
                    }

                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                    binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                    Log.d("S_CHARGE_1::", viewModel.totalServiceCharge.toString())
                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                    binding.tvPayNow.text =
                        "Pay " + MethodUtils.roundOffAmount(viewModel.totalPrice)
                    if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                        binding.txtDiscountText?.text =
                            "Discount (${MethodUtils.roundOffAmountDouble(viewModel.cartModel?.discountSelectdValue)}%)"
                    } else {
                        binding.txtDiscountText?.text = "Discount"
                    }
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
                            if (isFromPayment) {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._50sdp).toInt()
                                binding.relativeLoylatyPoints.visibility = View.GONE

                                binding.lblLoyaltyPoints.visibility = View.GONE
                                binding.lblLoyaltyBalance.visibility = View.GONE
                            } else {
                                binding.liinearInfoLayout.layoutParams.height =
                                    resources.getDimension(R.dimen._70sdp).toInt()

                                if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                                    binding.relativeLoylatyPoints.visibility = View.VISIBLE
                                binding.lblLoyaltyPoints.visibility = View.VISIBLE
                                binding.lblLoyaltyBalance.visibility = View.VISIBLE
                                LogUtil.logE(TAG, "InsideLoyalty")
                                LogUtil.logE(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
                                binding.txtLoyaltyAmount.text = "- $${
                                    String.format(
                                        "%.2f", viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                    )
                                }"
                                binding.txtLoyaltyPoints.text =
                                    "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                binding.txtLoyaltyBalance.text = "${
                                    if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                                        viewModel.redeemLoyaltyInfo.remainingLoyaltyPoints
                                    } else {
                                        viewModel.redeemLoyaltyInfo.availablePoints
                                    }
                                }"
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

                    viewModel.itemCalculationCartModelNew(it, binding.txtTotal, requireContext())
                    viewModel.cartModel?.let { cm ->
                        setTaxBifurcationData(cm.taxlistDynamic as ArrayList<TaxData>)
                    }


                } else {
                    cartModelsList = arrayListOf()
                    viewModel.clearListTax()
                    cartItemsAdapter.submitList(emptyList())
                    reSetTaxBifurcationData()
                    binding.txtTotal.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtTax.text = MethodUtils.roundOffAmount(0.00)
                    binding.txtDiscountText?.text = "Discount"
                    binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(0.00)
                    if (prefProvider.getValue(
                            OPTION_TYPE, "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                        binding.txtNoncashAdj.text = "-" + MethodUtils.roundOffAmount(0.00)
                    } else {
                        binding.txtNoncashAdj.text = MethodUtils.roundOffAmount(0.00)
                    }
                    binding.relativeOrderNotes?.visibility = View.GONE
                    binding.txtServiceCharge.text = MethodUtils.roundOffAmount(0.00)
                    binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.00)
                    var data: TbCustomer? = prefProvider.getCustomerData()
                    if (data != null) {
                        if (viewModel.loyaltyPointCondition(data)) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                            if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
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

                viewModel.observeLatestCartModel().observe(viewLifecycleOwner) {
                    var latestCartModel: CartModel? = null

//                    to solve BIS-4962

//                    Log.e( "AddItemFragment.kt", "currentCartItems:    ${Gson().toJson(viewModel.currentCartItems)}" )

                    if (it.isNotEmpty())
                        cartModelsList = ArrayList(it)

                    if (it != null)
                        if (it.isNotEmpty() && (viewModel.cartFooterNeedToBeUpdated || prefProvider.getValue(
                                ORDER_TYPE,
                                TAKEOUT
                            ) == Constants.DINE_IN)
                        ) {
                            latestCartModel = it[0]

                            if (latestCartModel.discountPrice == 0.0) {
                                latestCartModel.discountPrice = viewModel.customCartUpdateDiscount
                                viewModel.customCartUpdateDiscount = 0.0
                            }


                            if (viewModel.currentCartItems.isNotEmpty() && prefProvider.getValue(
                                    ORDER_TYPE,
                                    TAKEOUT
                                ) == DINE_IN
                            )
//                            latestCartModel.let { cartModel ->
//                                viewModel.taxBifurcationCalculationNew(
//                                    viewModel.currentCartItems.first(),
//                                    cartModel, "UPDATE", false
//                                )
//                            }

                                viewModel.setUpdatedCartModel(latestCartModel)
                            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                                viewModel.setCartModel(it)
                                if (latestCartModel.dineInList?.isNotEmpty() == true) {
                                    var dineInList = latestCartModel.dineInList
                                    if (dineInList?.get(0)?.selectedPosition != -1) {

                                        dineInList?.get(0)?.selectedPosition =
                                            viewModel.dineInHeaderPosition
                                    }

                                    dineInCartAdapter.setList(
                                        dineInList?.toCollection(arrayListOf()) ?: arrayListOf(),
                                        viewModel.currentCartItems
                                    )


                                }

                                LogUtil.logE(TAG, "getPAyment:  ${isFromPayment}")
                                LogUtil.logE(TAG, "isGuestPayment:  ${isGuestPayment}")

                                if (isFromPaymentDinein) {
                                    viewModelPayment.dineInWholeDiscount =
                                        guestCalModel?.wholeOrderPassDiscount
                                    viewModelPayment.dineInWholeSC = guestCalModel?.wholeOrderPassSC
                                    viewModel.itemCalculationForDineInPaymentNew(
                                        it[0],
                                        binding.txtTotal,
                                        requireContext(),
                                        guestCalModel!!,
                                        isGuestPayment
                                    )
                                } else {
                                    LogUtil.logE(TAG, "WithOutDineIn")
                                    viewModel.itemCalculationCartModelNew(
                                        viewModel.currentCartItems,
                                        binding.txtTotal,
                                        requireContext()
                                    )
                                }

                                var listOfTax: ArrayList<TaxData> = arrayListOf()


                                latestCartModel.taxlistDynamic?.let { it1 ->
                                    if (prefProvider.getValue(
                                            ORDER_TYPE, ""
                                        ) == DINE_IN && viewModel.currentCartItems.isEmpty() && !prefProvider.getValueboolean(
                                            Constants.DINE_IN_UPDATE, false
                                        )
                                    ) {
                                        //listOfTax.addAll(arrayListOf())
                                        setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)

                                    } else {
                                        // listOfTax.addAll(it1)
                                        setTaxBifurcationData(it[0].taxlistDynamic as ArrayList<TaxData>)
                                    }

                                }

                                if (viewModel.currentCartItems.isEmpty()) {
                                    viewModel.getAllCartItems(
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE, TAKEOUT
                                        ), prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                                    ).asLiveData().value?.let { it1 ->
                                        viewModel.cartModel =
                                            taxBifurcationCalculationUpdate(it[0], it1)
                                    }
                                } else {
                                    viewModel.cartModel = taxBifurcationCalculationUpdate(
                                        it[0], viewModel.currentCartItems
                                    )
                                }

                                updateCartFooter(
                                    viewModel.currentCartItems
                                )

                            } else {

                                Log.e(
                                    TAG,
                                    "CheckCartFragTax 12: ${Gson().toJson(it[0].taxlistDynamic)}"
                                )

                                // Added to resolve Add Discount issue BIS-3547
                                if (viewModel.discountNeedToUpdate && viewModel.cartFooterNeedToBeUpdated)

                                    if (viewModel.currentCartItems.isNotEmpty()) updateCartFooter(
                                        viewModel.currentCartItems
                                    )
                                    else {
                                        viewModel.getAllCartItems(
                                            prefProvider.getValue(
                                                Constants.ORDER_TYPE, TAKEOUT
                                            ), prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                                        ).asLiveData().value?.let { it1 ->
                                            updateCartFooter(
                                                it1.toList()
                                            )
                                        }
                                    }
                                else {
                                    viewModel.apply {
                                        discountNeedToUpdate = true
                                        cartFooterNeedToBeUpdated = true
                                    }
                                }

                                if (viewModel.currentCartItems.isEmpty()) {
                                    viewModel.getAllCartItems(
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE, TAKEOUT
                                        ), prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                                    ).asLiveData().value?.let { it1 ->
                                        viewModel.cartModel =
                                            taxBifurcationCalculationUpdate(it[0], it1)
                                    }
                                } else {
                                    viewModel.cartModel = taxBifurcationCalculationUpdate(
                                        it[0], viewModel.currentCartItems
                                    )
                                }

                            }
                        }
                }


                viewModel.getAllDineInCartItems(
                    "DineIn"
                    //, prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                ).asLiveData().observe(viewLifecycleOwner) { it ->
                    val itemCount = it.size

                    if (prefProvider.getValue(Constants.ORDER_TYPE, "") == "DineIn") {
                        Log.e("DINE IN ITEM COUNT", "DINE IN ORDER COUNT")


                        Log.d("BRUNO", "addObserver: CALLED")
                        Log.d("19OCT", "addObserver: CCI 1 = ${Gson().toJson(it)}")

                        viewModel.duplicateCurrentCartItem =
                            if (viewModel.currentCartItems.isNotEmpty()) viewModel.currentCartItems else viewModel.duplicateCurrentCartItem

                        viewModel.setCurrentCartItems(it)

                        if (viewModel.currentCartItems.isNotEmpty()) {

                            CoroutineScope(Dispatchers.IO).launch {

                                if (it.isEmpty()) {
                                    // Flag is used to update cart if last item from the cart will be deleted
                                    try {
                                        if (this@CartFragment::prefProvider.isInitialized && prefProvider.getValueboolean(
                                                IS_LAST_ITEM_DELETE, false
                                            )
                                        ) {
                                            Log.d("02nov23", "updateCart: LAST ITEM DELETED TRUE")
                                            prefProvider.setValueboolean(
                                                IS_LAST_ITEM_DELETE, false
                                            ) // reset flag after updating cart
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    val currentTimeMillis = System.currentTimeMillis()

                                    if (currentTimeMillis >= previousClickTimeMillis + DELAY_MILLIS) {
                                        previousClickTimeMillis = currentTimeMillis
                                    } else {
                                        // return@launch
                                    }
                                }

                                saveVisibility()


                                if (prefProvider.getValue(
                                        ORDER_TYPE,
                                        TAKEOUT
                                    ) == Constants.DINE_IN
                                ) {
                                    runOnUiThread(Runnable {
                                        binding.rvCartDineIn.visible()
                                        binding.rvCartList.gone()
                                        checkOrderType()
                                        if (it.isNotEmpty()) {

                                            //                                        cartlist = it
                                            if (isFromPayment) {

                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                        ORDER_TYPE, TAKEOUT
                                                    ) != Constants.GIFT_CARD
                                                ) {
                                                    binding.linearCashDiscount.visible()
                                                    if (prefProvider.getValue(
                                                            OPTION_TYPE, "CashDiscount"
                                                        ) == "CashDiscount"
                                                    ) {
                                                        binding.labelCashSurcharge?.text =
                                                            "Cash Discount"
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

                                            orderId?.let { it1 ->
                                                prefProvider.setValueInt(
                                                    Constants.ORDER_ID,
                                                    it1
                                                )
                                            }

                                            if (prefProvider.getValueboolean(
                                                    Constants.DINE_IN_UPDATE, false
                                                )
                                            ) {
                                                binding.txtDineInProceed.setText("Update and Proceed")

                                                var getOldList = prefProvider.getValue(
                                                    Constants.DINE_IN_UPDATE_LIST, ""
                                                )
                                                if (getOldList.isEmpty()) {
                                                    var listItemDine: ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                                                        arrayListOf()
                                                    //                                                var data = it[0].dineInList
                                                    //                                                LogUtil.logE(TAG, "getDataSizeDin ${data?.size}")
                                                    //                                                data?.forEach {
                                                    it.forEach { item ->
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
                                                                orderId = viewModel.cartModel?.orderId
                                                                    ?: 0,
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
                                                    //                                                }
                                                    prefProvider.getValueInt(orderId.toString(), -1)
                                                    prefProvider.setValue(
                                                        Constants.DINE_IN_UPDATE_LIST,
                                                        Gson().toJson(listItemDine)
                                                    )
                                                }

                                            } else {
                                                binding.txtDineInProceed.setText("Proceed To Fire")
                                                //                                            cartAdapter.notifyDataSetChanged()
                                            }


                                            viewModel.currentCartItems.clear()
                                            viewModel.currentCartItems.addAll(it)
                                            //viewModel.oldDineInItems.clear()
                                            Log.e(
                                                TAG,
                                                "getDineInListSize  ${viewModel.currentCartItems.size}"
                                            )
                                            dineInCartAdapter.setItemList(viewModel.currentCartItems)

                                            binding.txtSubTotal.text =
                                                MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                                            binding.txtTax.text =
                                                MethodUtils.roundOffAmount(viewModel.totalTax)
                                            Log.d(
                                                "S_CHARGE_2::",
                                                viewModel.totalServiceCharge.toString()
                                            )
                                            binding.txtServiceCharge.text =
                                                MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                                            if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                                                binding.txtDiscountText?.text = "Discount (${
                                                    MethodUtils.roundOffAmountDouble(viewModel.cartModel?.discountSelectdValue)
                                                }%)"
                                            } else {
                                                binding.txtDiscountText?.text = "Discount"
                                            }
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
                                                binding.relativeOrderNotes?.visibility =
                                                    View.VISIBLE
                                                binding.txtOrderNote?.text = viewModel.order_note
                                            } else {
                                                binding.relativeOrderNotes?.visibility = View.GONE
                                            }

                                            binding.relativeLoylatyPoints.visibility = View.GONE
                                            binding.lblLoyaltyPoints.visibility = View.GONE
                                            binding.lblLoyaltyBalance.visibility = View.GONE


                                        } else {
                                            //                                        cartlist = arrayListOf()
                                            if (isFromPayment) {
                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                        ORDER_TYPE, TAKEOUT
                                                    ) != Constants.GIFT_CARD
                                                ) {
                                                    binding.linearCashDiscount.visible()
                                                    if (prefProvider.getValue(
                                                            OPTION_TYPE, "CashDiscount"
                                                        ) == "CashDiscount"
                                                    ) {
                                                        binding.labelCashSurcharge?.text =
                                                            "Cash Discount"
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

                                            binding.relativeOrderNotes?.visibility = View.GONE
                                            binding.liinearInfoLayout.layoutParams.height =
                                                resources.getDimension(R.dimen._50sdp).toInt()
                                            binding.relativeLoylatyPoints.visibility = View.GONE
                                            binding.lblLoyaltyPoints.visibility = View.GONE
                                            binding.lblLoyaltyBalance.visibility = View.GONE


                                        }
                                        updateCartFooter(viewModel.currentCartItems)
                                    })
                                } else {

                                    runOnUiThread(Runnable {
                                        binding.rvCartDineIn.gone()
                                        binding.rvCartList.visible()
                                        checkOrderType()
                                    })

                                    if (it.isNotEmpty()) {

                                        val filterItems = arrayListOf<TbCartItem>()
                                        it.filter {
                                            !it.isDestroy
                                        }.let {

                                            filterItems.addAll(it.toCollection(arrayListOf()))
                                        }

                                        runOnUiThread {
                                            Log.e("FRAGMENT RESTARTED", "Fragment car line 1419")


                                            if (viewModel.cartFragmentRestarted) {

                                                //  viewModel.fragmentNeedToBeUpdated.value = true
                                                //viewModel.cartFragmentRestarted = false
                                            } else {

                                                Log.e(
                                                    "FRAGMENT RESTARTED",
                                                    "CART FRAGMENT NOT RESTARTED"
                                                )


                                            }

                                            if (viewModel.currentCartItems.isNotEmpty()) cartItemsAdapter.submitList(
                                                filterItems
                                            )

                                            binding.rvCartList.postDelayed({
                                                if (cartItemsAdapter.currentList.isNotEmpty()) {
                                                    binding.rvCartList.smoothScrollToPosition(
                                                        cartItemsAdapter.currentList.size - 1
                                                    )
                                                }
                                            }, 200)
                                            binding.rlCartView.visible()
                                            binding.rvOrderType.gone()
                                        }

                                        oldItemSize = it.size

                                        viewModel.destroyedCartItemsList.clear()
                                        it.filter { item -> item.isDestroy }
                                            .let { listOfCartItems ->
                                                viewModel.destroyedCartItemsList.addAll(
                                                    listOfCartItems
                                                )
                                            }

                                        runOnUiThread(Runnable {
                                            if (isFromPayment) {
                                                viewModel.selectedCustomer =
                                                    prefProvider.getCustomerData()

                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                        ORDER_TYPE, TAKEOUT
                                                    ) != Constants.GIFT_CARD
                                                ) {

                                                    binding.linearCashDiscount.visible()
                                                    if (prefProvider.getValue(
                                                            OPTION_TYPE, "CashDiscount"
                                                        ) == "CashDiscount"
                                                    ) {
                                                        binding.labelCashSurcharge?.text =
                                                            "Cash Discount"
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


                                            if (viewModel.currentCartItems.isEmpty()) {
                                                viewModel.getAllCartItems(
                                                    prefProvider.getValue(
                                                        Constants.ORDER_TYPE, TAKEOUT
                                                    ),
                                                    prefProvider.getValueInt(
                                                        Constants.EMPLOYEE_ID,
                                                        0
                                                    )
                                                ).asLiveData().value?.let { it1 ->
                                                    viewModel.cartModel =
                                                        viewModel.cartModel?.let { it2 ->
                                                            taxBifurcationCalculationUpdate(
                                                                it2, it1
                                                            )
                                                        }
                                                }
                                            } else {
                                                viewModel.cartModel =
                                                    viewModel.cartModel?.let { it1 ->
                                                        taxBifurcationCalculationUpdate(
                                                            it1, viewModel.currentCartItems
                                                        )
                                                    }
                                            }
                                            /* binding.rvCartList.removeAllViews()
                                         binding.rvCartList.removeAllViewsInLayout()*/
                                        })


                                    }

                                    runOnUiThread(kotlinx.coroutines.Runnable {
                                        // Added to resolve Add Discount issue BIS-3547
                                        if (viewModel.discountNeedToUpdate && viewModel.cartFooterNeedToBeUpdated) updateCartFooter(
                                            it
                                        )
                                        else {
                                            viewModel.apply {
                                                discountNeedToUpdate = true
                                                cartFooterNeedToBeUpdated = true
                                            }
                                        }
                                    })
                                }

                                runOnUiThread(Runnable {
                                    if (this@CartFragment::presentation.isInitialized) {
                                        if (!presentation.isShowing) presentation.show()
                                        if (it.isNotEmpty()) {
                                            presentation.updateCustomerDisplay(it)
                                        } else {
                                            presentation.onLogOutOrClockOutWithApiService(apiService)
                                        }
                                    }

                                    if (prefProvider.getValue(
                                            ORDER_TYPE, TAKEOUT
                                        ) == DINE_IN || prefProvider.getValueboolean(
                                            Constants.IS_ADD_VALUE_IN_GIFT_CARD, false
                                        )
                                    ) {
                                        binding.txtAddCustomer.invisible()
                                    } else {
                                        if (isAdded && parentFragmentManager != null) {
                                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text == getString(
                                                    R.string.add_customer2
                                                )
                                            ) {
                                                binding.txtAddCustomer.gone()
                                            } else if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text != getString(
                                                    R.string.add_customer2
                                                )
                                            ) {
                                                binding.txtAddCustomer.visible()
                                                binding.txtAddCustomer.isEnabled = false
                                            }
                                        }
                                    }

                                    if (isFromPayment || isFromPaymentDinein) {
                                        binding.rvOrderType.gone()
                                        binding.rlCartView.visible()
                                    }
                                })

                            }
                            // Added for tracking coroutine data

                        } else {
                            Log.e("Cart Blank Tracked", "Cart Going BLANK ->>>>>>")
                            //  getDineInCartList()
                            if (itemCount > 0)
                                viewModel.setCurrentCartItems(viewModel.duplicateCurrentCartItem)

                        }
                    }
                }


                viewModel.getAllCartItems(
                    prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT),
                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                ).asLiveData().observe(viewLifecycleOwner) { it ->

                    val itemCount = it.size
                    Log.d("BRUNO", "addObserver: CALLED")
                    Log.d("19OCT", "addObserver: CCI 1 = ${Gson().toJson(it)}")
                    if (it.isNotEmpty()) {
                        Log.e("checkFirstItemQty", "itemQuantity:  ${it.get(0).itemQuantity}")
                    }

                    if (prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT) != "DineIn") {

                        viewModel.duplicateCurrentCartItem =
                            if (viewModel.currentCartItems.isNotEmpty()) viewModel.currentCartItems else viewModel.duplicateCurrentCartItem

                        viewModel.setCurrentCartItems(it)

                        if (viewModel.currentCartItems.isNotEmpty() && prefProvider.getValue(
                                Constants.ORDER_TYPE,
                                TAKEOUT
                            ) != "DineIN"
                        ) {

                            CoroutineScope(Dispatchers.IO).launch {

                                if (it.isEmpty()) {
                                    // Flag is used to update cart if last item from the cart will be deleted
                                    try {
                                        if (this@CartFragment::prefProvider.isInitialized && prefProvider.getValueboolean(
                                                IS_LAST_ITEM_DELETE, false
                                            )
                                        ) {
                                            Log.d("02nov23", "updateCart: LAST ITEM DELETED TRUE")
                                            prefProvider.setValueboolean(
                                                IS_LAST_ITEM_DELETE, false
                                            ) // reset flag after updating cart
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    val currentTimeMillis = System.currentTimeMillis()

                                    if (currentTimeMillis >= previousClickTimeMillis + DELAY_MILLIS) {
                                        previousClickTimeMillis = currentTimeMillis
                                    } else {
                                        // return@launch
                                    }
                                }

                                saveVisibility()

                                if (prefProvider.getValue(
                                        ORDER_TYPE,
                                        TAKEOUT
                                    ) == Constants.DINE_IN
                                ) {
                                    runOnUiThread(Runnable {
                                        binding.rvCartDineIn.visible()
                                        binding.rvCartList.gone()
                                        checkOrderType()
                                        if (it.isNotEmpty()) {

                                            //                                        cartlist = it
                                            if (isFromPayment) {

                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                        ORDER_TYPE, TAKEOUT
                                                    ) != Constants.GIFT_CARD
                                                ) {
                                                    binding.linearCashDiscount.visible()
                                                    if (prefProvider.getValue(
                                                            OPTION_TYPE, "CashDiscount"
                                                        ) == "CashDiscount"
                                                    ) {
                                                        binding.labelCashSurcharge?.text =
                                                            "Cash Discount"
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

                                            if (prefProvider.getValueboolean(
                                                    Constants.DINE_IN_UPDATE, false
                                                )
                                            ) {
                                                binding.txtDineInProceed.setText("Update and Proceed")

                                                var getOldList = prefProvider.getValue(
                                                    Constants.DINE_IN_UPDATE_LIST, ""
                                                )
                                                if (getOldList.isEmpty()) {
                                                    var listItemDine: ArrayList<GetOrderDetailsResponse.Data.OrderItem> =
                                                        arrayListOf()
                                                    //                                                var data = it[0].dineInList
                                                    //                                                LogUtil.logE(TAG, "getDataSizeDin ${data?.size}")
                                                    //                                                data?.forEach {
                                                    it.forEach { item ->
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
                                                                orderId = viewModel.cartModel?.orderId
                                                                    ?: 0,
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
                                                    //                                                }
                                                    prefProvider.setValue(
                                                        Constants.DINE_IN_UPDATE_LIST,
                                                        Gson().toJson(listItemDine)
                                                    )
                                                }

                                            } else {
                                                binding.txtDineInProceed.setText("Proceed To Fire")
                                                //                                            cartAdapter.notifyDataSetChanged()
                                            }


                                            viewModel.currentCartItems.clear()
                                            viewModel.currentCartItems.addAll(it + viewModel.oldDineInItems)
                                            //viewModel.oldDineInItems.clear()
                                            Log.e(
                                                TAG,
                                                "getDineInListSize  ${viewModel.currentCartItems.size}"
                                            )
                                            dineInCartAdapter.setItemList(viewModel.currentCartItems)

                                            binding.txtSubTotal.text =
                                                MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                                            binding.txtTax.text =
                                                MethodUtils.roundOffAmount(viewModel.totalTax)
                                            Log.d(
                                                "S_CHARGE_3::",
                                                viewModel.totalServiceCharge.toString()
                                            )
                                            binding.txtServiceCharge.text =
                                                MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                                            if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                                                binding.txtDiscountText?.text = "Discount (${
                                                    MethodUtils.roundOffAmountDouble(viewModel.cartModel?.discountSelectdValue)
                                                }%)"
                                            } else {
                                                binding.txtDiscountText?.text = "Discount"
                                            }
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
                                                binding.relativeOrderNotes?.visibility =
                                                    View.VISIBLE
                                                binding.txtOrderNote?.text = viewModel.order_note
                                            } else {
                                                binding.relativeOrderNotes?.visibility = View.GONE
                                            }

                                            binding.relativeLoylatyPoints.visibility = View.GONE
                                            binding.lblLoyaltyPoints.visibility = View.GONE
                                            binding.lblLoyaltyBalance.visibility = View.GONE


                                        } else {
                                            //                                        cartlist = arrayListOf()
                                            if (isFromPayment) {
                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
                                                        ORDER_TYPE, TAKEOUT
                                                    ) != Constants.GIFT_CARD
                                                ) {
                                                    binding.linearCashDiscount.visible()
                                                    if (prefProvider.getValue(
                                                            OPTION_TYPE, "CashDiscount"
                                                        ) == "CashDiscount"
                                                    ) {
                                                        binding.labelCashSurcharge?.text =
                                                            "Cash Discount"
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

                                        val filterItems = arrayListOf<TbCartItem>()
                                        it.filter {
                                            !it.isDestroy
                                        }.let {

                                            filterItems.addAll(it.toCollection(arrayListOf()))
                                        }

                                        runOnUiThread {
                                            Log.e("FRAGMENT RESTARTED", "Fragment car line 1419")


                                            if (viewModel.cartFragmentRestarted) {

                                                //  viewModel.fragmentNeedToBeUpdated.value = true
                                                //viewModel.cartFragmentRestarted = false
                                            } else {

                                                Log.e(
                                                    "FRAGMENT RESTARTED",
                                                    "CART FRAGMENT NOT RESTARTED"
                                                )


                                            }

                                            if (viewModel.currentCartItems.isNotEmpty()) cartItemsAdapter.submitList(
                                                filterItems
                                            )

                                            binding.rvCartList.postDelayed({
                                                if (cartItemsAdapter.currentList.isNotEmpty()) {
                                                    binding.rvCartList.smoothScrollToPosition(
                                                        cartItemsAdapter.currentList.size - 1
                                                    )
                                                }
                                            }, 200)
                                            binding.rlCartView.visible()
                                            binding.rvOrderType.gone()
                                        }

                                        oldItemSize = it.size

                                        viewModel.destroyedCartItemsList.clear()
                                        it.filter { item -> item.isDestroy }
                                            .let { listOfCartItems ->
                                                viewModel.destroyedCartItemsList.addAll(
                                                    listOfCartItems
                                                )
                                            }

                                        runOnUiThread(Runnable {
                                            if (isFromPayment) {
                                                viewModel.selectedCustomer =
                                                    prefProvider.getCustomerData()

//                                                if (MethodUtils.isEnableCashDiscount(requireContext()) && prefProvider.getValue(
//                                                        ORDER_TYPE, TAKEOUT
//                                                    ) != Constants.GIFT_CARD
//                                                ) {
//
//                                                    binding.linearCashDiscount.visible()
//                                                    if (prefProvider.getValue(
//                                                            OPTION_TYPE, "CashDiscount"
//                                                        ) == "CashDiscount"
//                                                    ) {
//                                                        binding.labelCashSurcharge?.text =
//                                                            "Cash Discount"
//                                                    } else {
//                                                        showSurchargeWithPercentage()
//                                                    }
//                                                } else {
//                                                    binding.linearCashDiscount.gone()
//                                                }
                                                binding.linearCashDiscount.visible()
                                                if (prefProvider.getValue(
                                                        OPTION_TYPE, "CashDiscount"
                                                    ) == "CashDiscount"
                                                ) {
                                                    binding.labelCashSurcharge?.text =
                                                        "Cash Discount"
                                                } else {
                                                    showSurchargeWithPercentage()
                                                }
                                                binding.linearButtonView.gone()
                                                binding.relPreoceedToFire.gone()
                                            } else {
                                                binding.linearButtonView.visible()
                                                binding.relPreoceedToFire.gone()
                                            }


                                            if (viewModel.currentCartItems.isEmpty()) {
                                                viewModel.getAllCartItems(
                                                    prefProvider.getValue(
                                                        Constants.ORDER_TYPE, TAKEOUT
                                                    ),
                                                    prefProvider.getValueInt(
                                                        Constants.EMPLOYEE_ID,
                                                        0
                                                    )
                                                ).asLiveData().value?.let { it1 ->
                                                    viewModel.cartModel =
                                                        viewModel.cartModel?.let { it2 ->
                                                            taxBifurcationCalculationUpdate(
                                                                it2, it1
                                                            )
                                                        }
                                                }
                                            } else {
                                                viewModel.cartModel =
                                                    viewModel.cartModel?.let { it1 ->
                                                        taxBifurcationCalculationUpdate(
                                                            it1, viewModel.currentCartItems
                                                        )
                                                    }
                                            }
                                            /* binding.rvCartList.removeAllViews()
                                         binding.rvCartList.removeAllViewsInLayout()*/
                                        })
                                    }

                                    runOnUiThread(kotlinx.coroutines.Runnable {
                                        // Added to resolve Add Discount issue BIS-3547
                                        if (viewModel.discountNeedToUpdate && viewModel.cartFooterNeedToBeUpdated) updateCartFooter(
                                            it
                                        )
                                        else {
                                            viewModel.apply {
                                                discountNeedToUpdate = true
                                                cartFooterNeedToBeUpdated = true
                                            }
                                        }
                                    })
                                }

                                runOnUiThread(Runnable {
                                    if (this@CartFragment::presentation.isInitialized) {
                                        if (!presentation.isShowing) presentation.show()
                                        if (it.isNotEmpty()) {
                                            presentation.updateCustomerDisplay(it)
                                        } else {
                                            presentation.onLogOutOrClockOutWithApiService(apiService)
                                        }
                                    }

                                    if (prefProvider.getValue(
                                            ORDER_TYPE, TAKEOUT
                                        ) == DINE_IN || prefProvider.getValueboolean(
                                            Constants.IS_ADD_VALUE_IN_GIFT_CARD, false
                                        )
                                    ) {
                                        binding.txtAddCustomer.invisible()
                                    } else {
                                        if (isAdded) {
                                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text == getString(
                                                    R.string.add_customer2
                                                )
                                            ) {
                                                binding.txtAddCustomer.gone()
                                            } else if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment && binding.txtAddCustomer.text != getString(
                                                    R.string.add_customer2
                                                )
                                            ) {
                                                binding.txtAddCustomer.visible()
                                                binding.txtAddCustomer.isEnabled = false
                                            }
                                        }
                                    }

                                    if (isFromPayment || isFromPaymentDinein) {
                                        binding.rvOrderType.gone()
                                        binding.rlCartView.visible()
                                    }
                                })

                            }
                            // Added for tracking coroutine data

                        } else {
                            Log.e("Cart Blank Tracked", "Cart Going BLANK ->>>>>>")

                            // if(itemCount > 0)
                            viewModel.setCurrentCartItems(viewModel.duplicateCurrentCartItem)

                        }
                    } else {

                        /**
                         *  currentDineInItems keeps track of all dine in Items even if they are destroyed
                         */
                        viewModel.currentDineInItems =
                            kotlin.collections.ArrayList(it.filter { it.orderType == DINE_IN })
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
                                }, requireActivity()
                            )

                        }
                    } else {
                        ProgressUtils.dismissProgressDialog()
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            dismissProgressDialog()
                        })
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

    private fun updateCartFooter(it: List<TbCartItem>) {
        if (it.isNotEmpty()) {

            viewModel.itemCalculationCartModelNew(
                it, binding.txtTotal, requireContext()
            )

            Log.e(TAG, "CheckCartFragTax ${Gson().toJson(viewModel.cartModel?.taxlistDynamic)}")
            viewModel.cartModel?.taxlistDynamic?.toCollection(arrayListOf())
                ?.let { it1 -> setTaxBifurcationData(it1) }

            if (viewModel.order_note.isNotEmpty()) {
                binding.relativeOrderNotes?.visibility = View.VISIBLE
                binding.txtOrderNote?.text = viewModel.order_note
            } else {
                binding.relativeOrderNotes?.visibility = View.GONE
            }
            binding.txtSubTotal.text = MethodUtils.roundOffAmount(viewModel.subTotalPrice)
            binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)

            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {

                var serviceCharge = 0.0

                if (prefProvider.getValueboolean(SERVICECHARGE_DINEIN_ORDER, false)) {


                    val total = viewModel.subTotalPrice + viewModel.totalTax

                    val guestCount = dineInCartAdapter.getList().count {
                        it.isHeader == 0
                    }

//                    viewModel.serviceChargesList =
//                        ArrayList(viewModel.cartModel?.serviceCharge ?: arrayListOf())

                    val serviceChargesList = getServiceChargeFromGuestCount(guestCount - 1)



                    serviceChargesList.forEach {
                        if(it.min_guest_count!=null && it.max_guest_count!=null)
                            if (it.max_guest_count >= guestCount - 1 && it.min_guest_count <= guestCount - 1)
                                serviceCharge += (viewModel.subTotalPrice * it.percentage) / 100
                        }
                    }


                viewModel.totalServiceCharge = serviceCharge

                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
            } else {
                Log.d("S_CHARGE_5::", viewModel.totalServiceCharge.toString())
                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
            }
            Log.e("totalDiscount", viewModel.totalDiscount.toString())

            var cartCompletePrice = getCompleteCartPrice()

            viewModel.apply {
                if (cartModel?.discountSelectdValue != 0.0 && cartModel != null) {
                    totalDiscount = cartCompletePrice * cartModel?.discountSelectdValue!! / 100.0
                    val remaining = cartCompletePrice - totalDiscount

                    cartModel?.discountPrice = totalDiscount

                    try {
                        binding.txtSubTotal.text = MethodUtils.roundOffAmount(remaining)
                    } catch (e: Exception) {
                    }
                    subTotalPrice = remaining
                } else {
                    viewModel.totalDiscount = 0.0
                    try {
                        cartModel?.discountPrice = 0.0
                    } catch (e: Exception) {

                    }
                }
            }


            if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                binding.txtDiscountText?.text =
                    "Discount (${MethodUtils.roundOffAmountDouble(viewModel.cartModel?.discountSelectdValue)}%)"
            } else {
                binding.txtDiscountText?.text = "Discount"
            }

            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(viewModel.totalDiscount)


            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                binding.txtTotal.text =
                    MethodUtils.roundOffAmount(viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge)

                Log.e(
                    "Dine in",
                    "1 BEFORE DATA ALREADY UPDATED ${viewModel.totalPriceUpdated.value} = sub = ${viewModel.subTotalPrice} , tax = ${viewModel.totalTax}, service charges = ${viewModel.totalServiceCharge}\""
                )
                viewModel.totalPriceUpdated.value =
                    viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge

                prefProvider.getValue(
                    Constants.WHOLE_AMOUNT,
                    "${viewModel.totalPriceUpdated ?: 0.0}"
                )

                Log.e(
                    "Dine in",
                    "2 DATA ALREADY UPDATED CART ${viewModel.totalPriceUpdated.value} = sub = ${viewModel.subTotalPrice} , tax = ${viewModel.totalTax}, service charges = ${viewModel.totalServiceCharge}"
                )

                Log.e(
                    "Service charges",
                    "Toatal updated price = subtotal = ${viewModel.subTotalPrice} - Tax = ${viewModel.totalTax} - Service charge ${viewModel.totalServiceCharge} AND THEN final total = ${viewModel.totalPriceUpdated.value}"
                )

            } else {

                viewModel.itemCalculationCartModelNew(
                    it, binding.txtTotal, requireContext()
                )
            }

            binding.tvPayNow.text = "Pay " + binding.txtTotal.text.toString()

            if (prefProvider.getValue(
                    OPTION_TYPE, "CashDiscount"
                ) == "CashDiscount"
            ) {


                if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                    viewModel.cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        viewModel.totalPriceUpdated.value ?: 0.0,
                        prefProvider,
                        requireContext()
                    )
                }

                binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                binding.txtNoncashAdj.text =
                    "-" + MethodUtils.roundOffAmount(viewModel.cashdiscountAmount)

                val total =
                    viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge

                viewModel.customerCardPrice.value = total
                viewModel.customerCashPrice.value = total - viewModel.totalDiscount


            } else {

                if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                    viewModel.cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        viewModel.totalPriceUpdated.value ?: 0.0,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                        val total =
                            viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge - viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                        viewModel.cashdiscountAmount = MethodUtils.getLatestCashDiscountOrSurCharge(
                            total,
                            prefProvider,
                            requireContext()
                        )
                    } else {
                        val total =
                            viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge

                        viewModel.cashdiscountAmount = MethodUtils.getLatestCashDiscountOrSurCharge(
                            total,
                            prefProvider,
                            requireContext()
                        )


                        if (viewModel.cashDiscountType.equals("CashDiscount", ignoreCase = true)) {
                            //Cash Discount
                            viewModel.customerCardPrice.value = total
                            viewModel.customerCashPrice.value = total - viewModel.cashdiscountAmount
                        } else if (viewModel.cashDiscountType.equals(
                                "Surcharge",
                                ignoreCase = true
                            )
                        ) {
                            //Surcharge
                            viewModel.customerCashPrice.value = total
                            viewModel.customerCardPrice.value = total + viewModel.cashdiscountAmount
                        } else {
                            viewModel.customerNormalPrice.value = total
                        }

                    }

                }

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
                            ), binding.txtTotal
                        )
                    } else {
                        viewModel.setcheckedLoyaltyApply(
                            prefProvider.getValueboolean(
                                LOYALTY_ADDED, false
                            ), binding.txtTotal
                        )
                    }
                    /* To handle BIS-4672, we need to check isFromPayment variable, it is coming true, it should come false */
                    if (isFromPayment) {
                        if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                            if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                                binding.relativeLoylatyPoints.visibility = View.VISIBLE
                            binding.lblLoyaltyPoints.visibility = View.VISIBLE
                            binding.lblLoyaltyBalance.visibility = View.VISIBLE
                            binding.txtLabelLoyaltyAmounts.visibility = View.VISIBLE

                            binding.checkloylaty.visibility = View.GONE
                            binding.txtLoyaltyAmount.text = "- $${
                                String.format(
                                    "%.2f", viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                                )
                            }"


                            try {
                                binding.txtLoyaltyPoints.text =
                                    "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                                /*  binding.txtLoyaltyBalance.text =
                                        "${viewModel.selectedCustomer?.final_reward}"*/

                                binding.txtLoyaltyBalance.text =
                                    "${viewModel.redeemLoyaltyInfo.remainingLoyaltyPoints}"
                            } catch (e: Exception) {

                            }
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._50sdp).toInt()
                            binding.relativeLoylatyPoints.visibility = View.GONE
                            binding.lblLoyaltyPoints.visibility = View.GONE
                            binding.lblLoyaltyBalance.visibility = View.GONE


                        }
                        if (isAdded)
                            if (findNavController().currentDestination!!.label!!.contains(
                                    "Dashboard",
                                    ignoreCase = true
                                )
                            ) {
                                isFromPayment = false
                                arguments?.apply {
                                    putBoolean("isFromPayment", false)
                                }

//                                updateCartFooter(it)
                                /*Refreshing the current fragment*/
                                val id = findNavController().currentDestination?.id
                                findNavController().popBackStack(id!!, true)
                                findNavController().navigate(id)
                            }
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._70sdp).toInt()
                        if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                            binding.relativeLoylatyPoints.visibility = View.VISIBLE
                        binding.lblLoyaltyPoints.visibility = View.VISIBLE
                        binding.lblLoyaltyBalance.visibility = View.VISIBLE
                        var totalAmount = binding.txtTotal.text.toString().replace(Regex("[^0-9.]"), "").toDouble()
                        totalAmount += viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                        if (totalAmount >= viewModel.activeLoyaltyProgram?.amount!! && totalAmount != 0.00) {
                            binding.checkloylaty.visible()
                            binding.txtLoyaltyAmount.visible()
                            binding.txtLoyaltyPoints.visible()
                            binding.txtlabelloyaltyPoints.visible()
                        } else {
                            binding.checkloylaty.gone()
                            binding.checkloylaty.isChecked = false
                            binding.txtLoyaltyAmount.gone()
                            binding.txtLoyaltyPoints.gone()
                            binding.txtlabelloyaltyPoints.gone()

                        }


                        Log.e(TAG, "InsideLoyalty")
                        Log.e(
                            TAG, Gson().toJson(viewModel.redeemLoyaltyInfo)
                        )
                        binding.txtLoyaltyAmount.text = "- $${
                            String.format(
                                "%.2f", viewModel.redeemLoyaltyInfo.usedLoyaltyAmount
                            )
                        }"
                        binding.txtLoyaltyPoints.text =
                            "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"

                        if (viewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                            binding.txtLoyaltyBalance.text =
                                "${viewModel.redeemLoyaltyInfo.remainingLoyaltyPoints}"
                        } else {
                            binding.txtLoyaltyBalance.text =
                                "${viewModel.selectedCustomer?.final_reward}"
                        }
                        /*binding.txtLoyaltyBalance.text =
                            "${viewModel.selectedCustomer?.final_reward}"*/
                        binding.checkloylaty.isChecked =
                            viewModel.redeemLoyaltyInfo.needToApplyLoyalty
                    }
                } else {
                    /* This condition will be called when a customer will be added with loyalty but when cart is active with items, the user changes the customer which has no loyalty */
                    Log.d("Loyalty::", "Not available")
                    binding.apply {
                        liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._50sdp).toInt()
                        relativeLoylatyPoints.visibility = View.GONE
                        lblLoyaltyPoints.visibility = View.GONE
                        lblLoyaltyBalance.visibility = View.GONE

                    }
                    /* binding.liinearInfoLayout.layoutParams.height =
                                   resources.getDimension(R.dimen._50sdp).toInt()
                     binding.relativeLoylatyPoints.visibility = View.GONE
                     binding.lblLoyaltyPoints.visibility = View.GONE
                     binding.lblLoyaltyBalance.visibility = View.GONE*/
                    /*  prefProvider.setValue(
                          Constants.CUSTOMER_NAME,
                          ""
                      )
                      viewModel.selectedCustomer=null
                      prefProvider.setValue(
                          Constants.RECEIPT_CUSTOMER_NAME,
                          ""
                      )
                      prefProvider.setValue(
                          Constants.PREF_CUSTOMER,
                          ""
                      )*/
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
                    prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
//                    prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                }
            } else {
                binding.relativeLoylatyPoints.visibility = View.GONE
                binding.lblLoyaltyPoints.visibility = View.GONE
                binding.lblLoyaltyBalance.visibility = View.GONE

            }
        } else {

            if(prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                cartModelsList = arrayListOf()
            binding.liinearInfoLayout.layoutParams.height =
                resources.getDimension(R.dimen._50sdp).toInt()
            taxClickable = false
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.relativeDynamicTax.gone()
            binding.imgDropdown.gone()
            viewModel.clearListTax()
            cartItemsAdapter.submitList(emptyList())
            reSetTaxBifurcationData()
            binding.relativeOrderNotes?.visibility = View.GONE
            binding.checkloylaty.isChecked = false
            binding.txtTotal.text = MethodUtils.roundOffAmount(0.00)
            binding.txtSubTotal.text = MethodUtils.roundOffAmount(0.00)
            binding.txtTax.text = MethodUtils.roundOffAmount(0.0)
            binding.txtDiscountText?.text = "Discount"
            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(0.00)
            if (prefProvider.getValue(
                    OPTION_TYPE, "CashDiscount"
                ) == "CashDiscount"
            ) {
                binding.txtNoncashAdj.setTextColor(getColor(R.color.colorRed))
                binding.txtNoncashAdj.text = "-" + MethodUtils.roundOffAmount(0.00)
            } else {
                binding.txtNoncashAdj.text = MethodUtils.roundOffAmount(0.00)
            }
            binding.txtServiceCharge.text = MethodUtils.roundOffAmount(0.00)
            binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(0.00)
            var data: TbCustomer? = prefProvider.getCustomerData()
            if (data != null) {
                if (viewModel.loyaltyPointCondition(data)) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._70sdp).toInt()
                    if (prefProvider.getValue(ORDER_TYPE, "") != DINE_IN)
                        binding.relativeLoylatyPoints.visibility = View.VISIBLE
                    binding.lblLoyaltyPoints.visibility = View.VISIBLE
                    binding.lblLoyaltyBalance.visibility = View.VISIBLE

                    binding.txtLoyaltyAmount.text = "$0.00"
                    binding.txtLoyaltyPoints.text = "$0.00"
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

    override fun onItemClickListener(view: View?, data: TbCartItem, position: Int) {
        LogUtil.logE(TAG, "itemClicked  ${Gson().toJson(data)}")


        itemClickListner?.onItemUpdate(data, position)


    }

    override fun onCartItemClickListener(view: View?, data: TbCartItem, position: Int) {
        LogUtil.logE(TAG, "itemClicked  ${Gson().toJson(data)}")


        setCurrentSubTotal(data.itemQuantity)
        Log.e("Discount Tracking", "Subtotal Cart Price = ${viewModel.currentTotalPrice}")
        itemClickListner?.onCartItemUpdate(data, position)


    }

    override fun onHeaderSelected(position: Int) {

        if (!viewModel.isItemEditing) {

            prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, position)
            Log.d(TAG, "onHeaderSelected: header position : $position")
            viewModel.dineInHeaderPosition = position
            viewModel.currentSelectedHeaderDineIn = position
        } else
            AlertUtils.showCustomAlert(
                requireContext(),
                "Cannot change guest as already updating another item"
            )
    }

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbCartItem) {

        if (!item.isFired) {
            LogUtil.logE(TAG, "onDineinItemClick ${position}")

            viewModel.selectedItemPositionDine = position
            // viewModel.dineInHeaderPosition = headerPosition
            viewModel.dineInSelectedItemHeaderPos = headerPosition

            item.headerPositionDinein = headerPosition
            itemClickListner?.onItemUpdate(item, position)
        } else {
            AlertUtils.showCustomAlert(
                requireContext(),
                "Cannot update this item as the item is already fired."
            )
        }
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
            if (cartModelsList.isEmpty())
                viewModel.cartModel?.let { cartModelsList.add(0, it) }

            if (cartModelsList.get(0).dineInList?.size!! >= position) {
                val dineIn = cartModelsList.get(0).dineInList
                dineIn?.get(position)?.customer = null
                viewModel.cartModel?.dineInList?.get(position)?.customer = null
                cartModelsList[0].dineInList?.get(position)?.customer = null
                viewModel.dineInCartUpdate(cartModelsList, dineIn!!)
            }

        } else {


            if (cartModelsList.isEmpty())
                viewModel.cartModel?.let { cartModelsList.add(0, it) }

            if (cartModelsList.isNotEmpty()) {

                var listOfCustomersID: ArrayList<Int> = arrayListOf()
                cartModelsList[0].dineInList?.forEach {
                    if (it.customer != null) {
                        listOfCustomersID.add(it.customer?.id ?: 0)

                    }

                }
                val bundle = bundleOf(
                    "DINE_IN" to true,
                    "position" to position,
                    "cartList" to cartModelsList,
                    "listOfCustomersID" to listOfCustomersID
                )
                try {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryBoldPOS_to_assignCustomerOrderFragment, bundle
                    )
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            } else
                AlertUtils.showCustomAlert(
                    requireContext(),
                    "Unable to assign customer ! Please add at least one Item."
                )
        }


    }

    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    // To add guest and dynamic guest name to dine in order
    private fun addGuestToOrder(count: Int) {
        if (count == 0) {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), getString(R.string.minimum_guest_count_should_be_one)
            ) { _, _ ->
            }
            return
        }

        if (cartModelsList.isEmpty())
            viewModel.cartModel?.let { cartModelsList.add(0, it) }


        if (cartModelsList.isEmpty()) {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(), "Unable to Add Guest !"
            ) { _, _ ->
            }
            return
        } else {
            var existing_count = dineInCartAdapter.getList().size - 1
            var total_count = existing_count + count
            if (total_count <= 15) {
                var existinglist: ArrayList<DineInModel> = arrayListOf()
                cartModelsList[0].dineInList?.forEach {
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
                                cartModelsList[0].dineInList?.single { item -> item.title == "Guest ${availableName[i - 1]}" }
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
                                    floorPlanTable = cartModelsList[0].dineInList!![0].floorPlanTable

                                )
                            )
                        }
                    }
                }
                existinglist.addAll(dineInList)
                cartModelsList[0].dineInList = existinglist.toList()
                viewModel.addGuestFromDashBoard(cartModelsList)
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), "You can't add more than 15 Guest in an order."
                ) { _, _ ->
                }
            }
        }
    }

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbCartItem) {

        alert(
            getString(R.string.app_name), getString(R.string.delete_item_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                // Do positive stuff here
                cartModelsList.get(0).orderType = Constants.DINE_IN

                /*viewModel.newCartLogicModifier(
                    cartlist,
                    data,
                    Constants.DELETE, false,
                    dineInList = dineInCartAdapter.getList()
                )*/

                viewModel.updateDineInCart(
                    viewModel.currentCartItems, data, DELETE, false, dineInCartAdapter.getList()
                )

            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff heref
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (prefProvider.getValueboolean(IS_PAYMENT_SCREEN, false)) {
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.onDisplayChanged()

                val tipListViewModel by activityViewModels<TipListViewModel>()
                presentation.checkForTipBeforeTransaction(tipListViewModel)
            }
        }
    }

    // To remove guest from order
    override fun onRemoveGuest(position: Int) {

        if (cartModelsList.isNotEmpty()) {
            if (viewModel.currentCartItems.any { it.guestIndexForDineIn == position }) {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    "Can't remove guest as it contains items."
                )
                return
            }
        }

        if (cartModelsList.isEmpty())
            viewModel.cartModel?.let { cartModelsList.add(0, it) }


        if (dineInCartAdapter.getList()
                .isNotEmpty() && dineInCartAdapter.getList().size > 2 && cartModelsList.isNotEmpty()
        ) {
            if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {

                val dineIn = cartModelsList[0].dineInList as ArrayList<DineInModel>

                cartModelsList.get(0).dineInList?.forEach {
                    if (it.title == dineInCartAdapter.getList()[position].title) {


                        it.apply {
                            this.isDestroy = true
                            viewModel.destroyedDineGuestsList.add(it)
                            viewModel.updateDineInCartItemGuestDineInPositions(position)
                        }

                    }
                }

                dineIn.removeAll(viewModel.destroyedDineGuestsList)
                cartModelsList[0].dineInList = dineIn

            } else {
                val dineIn = cartModelsList[0].dineInList as ArrayList<DineInModel>
                val destroyedGuestsList: ArrayList<DineInModel> = ArrayList()
                dineIn.filter { (it.title == dineInCartAdapter.getList()[position].title) }
                    .forEach { destroyedGuestsList.add(it) }

                val index = position
//                Toast.makeText(requireContext(), "Removed guest at index $index", Toast.LENGTH_LONG)
//                    .show()

                dineIn.removeAll(destroyedGuestsList.toSet())

                viewModel.updateDineInCartItemGuestDineInPositions(position)

                cartModelsList[0].dineInList = dineIn
            }
            viewModel.addCart(cartModelsList[0])

            /**
             * After removing any guest select whole table by default
             * */
            try {
                viewModel.dineInHeaderPosition = 0
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else if (cartModelsList.isEmpty()) {
            AlertUtils.showCustomAlert(requireContext(), "Unable to remove guest")
        } else {
            viewModel.unableToRemoveGuest(getString(R.string.minimum_one_guest_is_required))
        }
    }

    // Update UI after removing guest from order
    private fun removeGuestObserver() {
        if (isAdded) {
            viewModel.removeGuestSuccess.observe(viewLifecycleOwner) { event ->
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), event.getContentIfNotHandled().toString()
                ) { _, _ -> }
            }
        }
    }

    private fun showMessage() {
        AlertUtils.showCustomAlert(requireContext(), "Order should be less than 1 million usd.")
    }


    private fun setCartAdapter() {

        binding.rvCartList.isNestedScrollingEnabled = false
        binding.rvCartList.disableItemAnimator()
        cartItemsAdapter = CartItemsAdapter()
        cartItemsAdapter.setCallback(this)
        binding.rvCartList.adapter = cartItemsAdapter
        binding.rvCartList.itemAnimator = null
        dineInCartAdapter = DineInAdapter(viewModel)
        dineInCartAdapter.setListner(this)
        dineInCartAdapter.isFromPayment(isFromPayment)
        binding.rvCartDineIn.adapter = dineInCartAdapter

        viewModel.lastItemRemoveFromCart.observe(viewLifecycleOwner) { pair ->
            if (pair.first) {
                val dineInList = dineInCartAdapter.getList()
                val found = dineInList.any { it.items.any { it.cartItemId == pair.second } }

                Toast.makeText(requireContext(), "ITEMS - $found", Toast.LENGTH_LONG).show()

            }
        }

    }

    private fun clearCart() {
        alert(
            getString(R.string.app_name), getString(R.string.delete_items_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                runBlocking {
                    supervisorScope {
                        launch {
                            try {
                                viewModel.wholetotalPrice = 0.0
                                viewModel.totalPrice = 0.0
                                viewModel.changeCustomerDispSignButtonTitle("")
                                viewModel.selectedCatetory = 0
                                // Do positive stuff here
                                prefProvider.setValueboolean(Constants.BACK_FROM_PAYMENT, false)
                                prefProvider.setValueboolean(Constants.NO_NEED_TO_PRINT, false)
                                taxBirfurcationAdapter.clearList()
                                viewModel.clearListTax()
                                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                                prefProvider.setValue(Constants.REDIRECT_FROM, "")
                                prefProvider.setValue(Constants.DELIVERY_TYPE, "")

                                /*Remove the added tip - START*/
                                prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                                prefProvider.setValue(Constants.TIP_ADDED_AMOUNT, "")
                                prefProvider.setValueInt(Constants.TIP_ADDED_ID, 0)
                                /*Remove the added tip - END*/


                                /*Clear the Update Order fields - START*/
                                prefProvider.setValue(Constants.OPEN_ORDER_ITEMS_OLD, "")
                                prefProvider.setValue(Constants.OPEN_ORDER_ITEMS, "")
                            /*    prefProvider.deleteValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM)
                                prefProvider.deleteValue(Constants.OPEN_ORDER_ITEMS)

                                prefProvider.setValueboolean(Constants.OPEN_ORDER_UPDATE_FOR_PRINT, false)

                                prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, false)

                                prefProvider.setValueboolean(
                                    Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                    false
                                )

                                prefProvider.setValueboolean(
                                    Constants.LOYALTY_ADDED,
                                    false
                                )

                                prefProvider.setValueboolean(
                                    Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER,
                                    false
                                )

                                prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, -1)*/
                                /*Clear the Update Order fields - END*/

                                updateActiveOrderFlagClear()
                                itemListner?.onCancelItemSelected()
                                if (prefProvider.getValue(ORDER_TYPE, "")
                                        .toString() == Constants.DINE_IN
                                ) {
                                    prefProvider.setValue(Constants.DINE_IN_UPDATE_LIST, "")
                                    prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)
                                    viewModel.removeItemDineInList.clear()

                                    //if (cartModelsList.size > 0) {

                                    val dList = viewModel.cartModel?.dineInList ?: arrayListOf()
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
                                    //}


                                    clearCustomer()
                                    viewModel.deleteCart()
                                    EventBus.getDefault().post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                                Gson().toJson(Thread.currentThread().stackTrace)
                                            }"
                                        )
                                    )
                                    cartModelsList.clear()
                                    viewModel.currentCartItems.clear()
                                    viewModel.duplicateCurrentCartItem.clear()
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
                                    EventBus.getDefault().post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                                Gson().toJson(Thread.currentThread().stackTrace)
                                            }"
                                        )
                                    )
                                    cartModelsList.clear()
                                    isOrderUpdate = false
                                    prefProvider.setValue(ORDER_TYPE, "")
                                    prefProvider.setValue(ORDER_TYPE_NAME, "")
                                    prefProvider.setValue(DELIVERY_TYPE, "")
                                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)

                                    itemClickListner?.onDineInOrderCleared()
                                    uiSave()
                                    getOrderTypes()

                                    viewModel.deleteOrderAfterMarkup()

                                    binding.checkloylaty.isChecked = false
                                    viewModel.redeemLoyaltyInfo.usedLoyaltyAmount = 0.0
                                    viewModel.redeemLoyaltyInfo.usedLoyaltyPoints = 0
                                    viewModel.redeemLoyaltyInfo.remainingAmount = 0.0
                                    viewModel.redeemLoyaltyInfo.total = 0.0

                                }

                                viewModel.currentCartItems = arrayListOf()
                                viewModel.duplicateCurrentCartItem = arrayListOf()
                                viewModel.fragmentNeedToBeUpdated.value = false

                                viewModelPayment.clearPreAuthDetails()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        launch {
                            viewModel.deleteOrderTypeBackupByName(
                                prefProvider.employeeId()
                            )
                        }
                    }

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
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
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
        binding.checkloylaty.isChecked = false
        viewModel.redeemLoyaltyInfo.needToApplyLoyalty = false
        viewModel.redeemLoyaltyInfo.isLoyaltyApplied = false
        setupLoyalytyPoints()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
        }
    }


    private fun refreshItemCalculation() {
        if (viewModel.currentCartItems.size > 0) {
            viewModel.itemCalculationCartModelNew(
                viewModel.currentCartItems, binding.txtTotal, requireContext()
            )
        }

    }

    private fun setCurrentSubTotal(itemQuantity: Int) {
        val subTotalText = binding.txtSubTotal.text.toString()
        val subTotal = subTotalText.subTotalToDouble()
        viewModel.currentTotalPrice = subTotal
        viewModel.clickedItemQuantity = itemQuantity
    }

    private fun initListeners() {

        binding.relPreoceedToFire.setOnClickListener {

            if (viewModel.restrictedAmount(binding.txtTotal)) {


                if (viewModel.currentCartItems.isNotEmpty())
                    binding.relPreoceedToFire.gone()
                else
                    AlertUtils.showCustomAlert(requireContext(), "Please add at least one Item.")

                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                //cartModelsList[0] = viewModel.generateCombinedItems(viewModel.cartModel!!)

                if (cartModelsList.isNotEmpty()) {
                    if (prefProvider.getValueboolean(DINE_IN_UPDATE, true)) {
                        var itemCount = 0
                        /*for (i in cartlist.indices) {
                            for (j in cartlist[i].dineInList?.indices!!) {
                                if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                                    itemCount++
                                    break
                                }
                            }
                            if (itemCount != 0) {
                                break
                            }

                        }*/
                        itemCount = viewModel.currentCartItems.size

                        if (itemCount == 0) {
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(),
                                getString(R.string.please_add_Atleast_one_item_in_cart)
                            ) { _, _ ->
                                restrictButtonClick(true)

                            }
                        } else {
                            Log.e(TAG, ".destroyedListRelPR:  ${viewModel.destroyedList.size}")
                            Log.e("IssueBIS777", "getITems:  ${viewModel.cartModel?.items?.size}")
                            Log.e(
                                "IssueBIS777",
                                "getITemsFromScreen:  ${viewModel.cartModel?.items?.size}"
                            )

                            if (cartModelsList[0] != null) {

                                cartModelsList[0] =
                                    viewModel.generateCombinedItems(cartModelsList[0])
                            } else {
                                cartModelsList[0] =
                                    viewModel.addDineInRemovedItems(viewModel.cartModel!!)
                            }

                            val valuess = cartModelsList[0]


                            val dineIn = cartModelsList[0].dineInList as ArrayList<DineInModel>

                            cartModelsList[0].dineInList =
                                dineIn + viewModel.destroyedDineGuestsList

                            viewModel.destroyedDineGuestsList.clear()


                            val request = viewModel.updateOrder(cartModelsList[0])


                            /***
                             * Added this delay to resolve items getting added two times after moving items
                             */
                            Handler().postDelayed({

                                request.order.guestsAttributes.forEach {
                                    it.guestItemsAttributes.forEach { it.Destroy = true }
                                }

                                if (cartModelsList[0].orderId != 0) {
                                    cartModelsList[0].orderId?.let {
                                        viewModel.updateOrderCall(
                                            it, request
                                        )
                                    }
                                } else {
                                    orderId?.let { it1 ->
                                        viewModel.updateOrderCall(
                                            it1,
                                            request
                                        )
                                    }
                                }
                            }, 300)


//                                viewModel.dineInResult.observe(viewLifecycleOwner) { returnResult ->
//
//                                    if (returnResult) {
//                                        viewModel.updateRequested = false
//                                        if (cartModelsList[0].orderId != 0) {
//                                            cartModelsList[0].orderId?.let {
//                                                viewModel.updateOrderCall(
//                                                    it, request
//                                                )
//                                            }
//                                        } else {
//                                            orderId?.let { it1 ->
//                                                viewModel.updateOrderCall(
//                                                    it1,
//                                                    request
//                                                )
//                                            }
//                                        }
//
//                                        // viewModel.dineInResult.value = false
//                                    }
//                                }
//                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)
//                            prefProvider.setValueboolean(DINE_IN_LIST_EDIT, false)
//                            prefProvider.setValueboolean(DINE_IN_UPDATE, false)


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
            try {
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
                            Constants.LOYALTY_ADDED, false
                        ) || prefProvider.getValueboolean(
                            Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false
                        )
                    ) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireActivity(),
                            "You can not change customer from checkout when loyalty points added. Please go back and change customer."
                        ) { _, _ ->
                        }
                    } else if (viewModel.getSplitCount() > 1 || splitValue > 1) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireActivity(), "Customer can not be changed during split payment."
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
                            "isLoyaltyApplied", viewModel.redeemLoyaltyInfo.needToApplyLoyalty
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
            } catch (e: Exception) {

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


            /*if (viewModel.currentCartItems.isEmpty()) {
                popupMenu.menu.findItem(R.id.menu_discount).isVisible = false
                popupMenu.menu.findItem(R.id.menu_order_note).isVisible = false
//                popupMenu.menu.findItem(R.id.menu_clear_cart).isVisible = false
            }*/

            if (prefProvider.getValueInt(CUSTOMER_ID, -1) == -1) {
                popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                try {
                    when (menuItem.itemId) {
                        R.id.menu_clear_cart -> {
                            popupMenu.dismiss() //For resolving BIS-273
                            clearCart()
                            cleanOrderBackupDetails()

//                            //Clear PREAUTH data
//                            prefProvider.setValue(PRE_AUTH_DETAILS,"")
////                            viewModel.apply {
////                                paymentAttributes = null
////                                authPaymentResponse = null
////                                allOrderResponse = null
////                            }
////
//                            viewModelPayment.preAuthData = null
                        }


                        R.id.menu_remove_customer -> {

                            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == PHONE_ORDER) {
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    getString(R.string.customer_cannot_be_remove_at_the_moment)
                                ) { _, _ -> }
                            } else {
                                if (cartModelsList.isNotEmpty() && cartModelsList[0].customer != null) {
                                    cartModelsList[0].customer = null
                                    viewModel.addCart(cartModelsList[0])
                                    viewModel.deleteCustomer(cartlist[0].cartId)
                                }

                                clearCustomer()
                                viewModel.changeCustomerDispSignButtonTitle(getString(R.string.sign_up_or_check_in))

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
                                bundleOf("isOrderNote" to true, "cartList" to cartModelsList)
                            )
                        }

                        R.id.menu_discount -> {
                            val bundle = Bundle()


                            setCurrentSubTotal(1)

                            bundle.putBoolean("isOrderDiscount", true)
                            bundle.putDouble("totalPrice", viewModel.currentTotalPrice)

                            Log.e(
                                "Discount Tracking",
                                "Subtotal Cart Price = ${viewModel.currentTotalPrice}"
                            )

                            viewModel.cartModel?.let {
                                if (it.discountType.isEmpty()) {
                                    viewModel.cartModel?.discountType = PERCENTAGE
                                }
                            }

                            viewModel.cartModel?.let {
                                calculateDiscount()
                            }

                            if (viewModel.cartModel != null) {
                                bundle.putDouble(
                                    "orderDiscountPrice", viewModel.cartModel?.discountPrice ?: 0.0
                                )
                                bundle.putString(
                                    "orderDiscountType", viewModel.cartModel?.discountType
                                )
                                bundle.putDouble(
                                    "selectedvalue",
                                    viewModel.cartModel?.discountSelectdValue ?: 0.0
                                )
                            }
                            bundle.putString("isFrom", "orderDiscount")

                            /*Added by Rahul for solving Discount issue */
                            for (key in bundle.keySet()) {
                                Log.d("BUNDLE_PRINT_CART", "Key: $key, value: ${bundle.get(key)}")
                            }

                            if (prefProvider.isAdmin() || prefProvider.isManager()) {
                                Log.e(TAG, "added in 1")

                                findNavController().navigate(
                                    R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                                    bundle
                                )
                            } else {
                                Log.e(TAG, "added in 2")
                                if (rolePermission.hasDiscountPermission(binding.root)) {

                                    Log.e(TAG, "added in 3")

                                    findNavController().navigate(
                                        R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                                        bundle
                                    )
                                }
                                /*findNavController().navigate(
                                    R.id.actionboldpos_to_pascodeManagerDailog, bundle
                                )*/
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

//        binding.tvPayNow.setOnClickListener(object : View.OnClickListener {
//            override fun onClick(p0: View?) {
//
//                if(InternetUtils.isInternetAvailable(requireContext().applicationContext)) {
//
//                runBlocking {
//
//                    lifecycleScope.launch {
//                        viewModel.addCartModelBackup(Gson().toJson(viewModel.cartModel).toString())
//                    }
//
//                    delay(500)
//                    if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER) {
//
//                        prefProvider.setValueboolean(OPEN_ORDER_DIRECT_PAY, true)
//                    }
//                    prefProvider.setValueboolean(Constants.TIP_ADDED, false)
//
////
////
////                /*Added By Rahul - Move the current items to the new preference key - START*/
//                    prefProvider.setValue(
//                        Constants.OPEN_ORDER_ITEMS_OLD,
//                        prefProvider.getValue(Constants.OPEN_ORDER_ITEMS, "")
//                    )
////                /*Move the current items to the new preference key - END*/
//
//                    if (prefProvider.getValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)) {
//                        prefProvider.setValue(
//                            Constants.OPEN_ORDER_ITEMS,
//                            Gson().toJson(cartItemsAdapter.currentList)
//                        )
//                    }
//
//                    /*insert into db if the cart model is not present in the db*/
//                    var cartJob = CoroutineScope(Dispatchers.IO).launch {
//                        delay(1000)
//                        try {
//                            var currentCartModel: CartModel? =
//                                viewModel.getCartModelFromID(viewModel.cartModel!!.cartId)
//                            if (currentCartModel == null) {
//                                viewModel.createEmptyCart(viewModel.cartModel!!)
//                            }
//                        } catch (e: Exception) {
//
//                        }
//                    }
//                    cartJob.join()
//                    if (cartItemsAdapter.currentList.isNotEmpty()) {
//                        prefProvider.setValue(ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, ""))
//                        prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
//                        prefProvider.setValue("PaidAmount", "")
//                        prefProvider.setValue(WHOLE_AMOUNT, "")
//                        prefProvider.setValueInt("cardCount", 0)
//                        prefProvider.setValue(Constants.SUB_TOTAL, "")
//                        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
//                        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
//                        prefProvider.setValue(Constants.TIP, "")
//                        prefProvider.setValue(Constants.TAX_CHARGE, "")
//                        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
//                        viewModel.setTipAmount(0.0)
//                        if (isOrderUpdate) {
//                            var bundle: Bundle = Bundle()
//                            bundle.putInt("orderId", orderId!!)
//                            bundle.putInt("paymentId", paymentId!!)
//                            bundle.putString("paymentOfflineId", paymentOfflineId)
//                            bundle.putString("orderOfflineId", orderOfflineId)
//                            bundle.putString(
//                                Constants.OLD_ITEM,
//                                prefProvider.getValue(Constants.OLD_ITEM, "")
//                            )
//                            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
//                                prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
//                                clearObserver()
//                                findNavController().navigate(
//                                    R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment,
//                                    bundle
//                                )
//                            }
//                        } else {
//                            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
//                                prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
//                                clearObserver()
//                                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
//                            }
//                        }
//                    } else {
//                        AlertUtils.showCustomAlertWithListenerWithOK(
//                            requireContext(),
//                            resources.getString(R.string.please_add_Atleast_one_item_in_cart)
//                        ) { _, _ ->
//                        }
//                    }
//
//                }
//                }else {
//                    AlertUtils.showCustomAlert(requireContext(), "Please check your Network Connectivity.")
//                }
//
//            }
//
//        })

        binding.tvPayNow.setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    cleanOrderBackupDetails()
                    if ((binding.orderTypeDisplay.text.toString().lowercase().contains("phone"))) {
                        var deliveryType = prefProvider.getValue(DELIVERY_TYPE, "")
                        if (deliveryType.isNotEmpty() && deliveryType.equals(
                                DELIVERY,
                                ignoreCase = true
                            )
                        ) {
                            if ((binding.txtAddCustomer.text.toString()
                                    .contains("+")) && (binding.txtAddCustomer.text.toString()
                                    .lowercase().contains("add"))
                            ) {
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    getString(R.string.add_customer_message),
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            p0?.dismiss()
                                        }

                                    })
                                return
                            }
                        }
                    }

                    viewModel.selectedCatetory = 0
                    prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                    prefProvider.setValueInt("ORDER_ID", -1)

                    if (InternetUtils.isInternetAvailable(requireContext().applicationContext)) {

                        lifecycleScope.launch {
                            viewModel.addCartModelBackup(
                                Gson().toJson(viewModel.cartModel).toString()
                            )
                        }

//                    runBlocking {

//                    delay(500)
                        if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER) {

                            prefProvider.setValueboolean(OPEN_ORDER_DIRECT_PAY, true)
                        }
                        prefProvider.setValueboolean(Constants.TIP_ADDED, false)

//                /*Added By Rahul - Move the current items to the new preference key - START*/
                        prefProvider.setValue(
                            Constants.OPEN_ORDER_ITEMS_OLD,
                            prefProvider.getValue(Constants.OPEN_ORDER_ITEMS, "")
                        )
//                /*Move the current items to the new preference key - END*/

                        if (prefProvider.getValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)) {
                            prefProvider.setValue(
                                Constants.OPEN_ORDER_ITEMS,
                                Gson().toJson(cartItemsAdapter.currentList)
                            )
                        }

                        /*insert into db if the cart model is not present in the db*/
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                var currentCartModel: CartModel? =
                                    viewModel.getCartModelFromID(viewModel.cartModel!!.cartId)
                                if (currentCartModel == null) {
                                    viewModel.createEmptyCart(viewModel.cartModel!!)
                                }
                            } catch (e: Exception) {
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CartFragment.kt tvPayNow ${e.printStackTrace()}"))
                            }
                        }

                        if (cartItemsAdapter.currentList.isNotEmpty()) {
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
                                bundle.putString(
                                    "orderType_to_check_kiosk",
                                    prefProvider.getValue(ORDER_TYPE, "")
                                )

                                bundle.putString(
                                    Constants.OLD_ITEM,
                                    prefProvider.getValue(Constants.OLD_ITEM, "")
                                )

                                EventBus.getDefault().post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} CartFragment.kt-> binding.tvPayNow.setOnClickListener_if (isOrderUpdate) -> bundle = ${
                                            Gson().toJson(bundle)
                                        }"
                                    )
                                )

                                if (isAdded)
                                    if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                                        prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
                                        clearObserver()
                                        findNavController().navigate(
                                            R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment,
                                            bundle
                                        )
                                    }
                            } else {
                                if (isAdded)
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
                                restrictButtonClick(true)
                            }
                        }

//                    }
                    } else {
                        AlertUtils.showCustomAlert(
                            requireContext(), "Please check your Network Connectivity."
                        )
                    }

                }

            })

        binding.tvSave.setOnSingleClickListener(
            object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    restrictButtonClick(false)

                    prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                    viewModel.selectedCatetory = 0

                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CartFragment -> tvSave()"))
                    if (!binding.tvSave.text.toString().trim()
                            .equals("update", ignoreCase = true)
                    ) {
                        prefProvider.setValueInt("ORDER_ID", -1)
                    }

                    if (viewModel.backupOrderId != null &&
                        viewModel.backupPaymentId != null &&
                        viewModel.backupPaymentOfflineId?.isNotEmpty() ?: false &&
                        viewModel.backupOrderOfflineId?.isNotEmpty() ?: false
                    ) {
                        isOrderUpdate = true
                        orderId = viewModel.backupOrderId
                        paymentId = viewModel.backupPaymentId
                        paymentOfflineId = viewModel.backupPaymentOfflineId ?: "Failing"
                        orderOfflineId = viewModel.backupOrderOfflineId ?: "Failing"
                    }
                    if (InternetUtils.isInternetAvailable(requireContext().applicationContext)) {
                        runBlocking {
                            try {
                                prefProvider.setValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)
                                if (isOrderUpdate == false) {

                                    /*Added By Rahul */
                                    prefProvider.setValue(
                                        Constants.OPEN_ORDER_ITEMS_OLD, ""
                                    )
                                    prefProvider.setValue(Constants.OLD_ITEM, "")

                                }

                                if (cartItemsAdapter.currentList.isNotEmpty()) {
                                    prefProvider.setValueboolean(
                                        Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false
                                    )
                                    prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)

                                    prefProvider.setValue(
                                        ORDER_TYPE, prefProvider.getValue(ORDER_TYPE, "")
                                    )
                                    prefProvider.setValue("PaidAmount", "")
                                    prefProvider.setValue(WHOLE_AMOUNT, "")
                                    prefProvider.setValueInt("cardCount", 0)
                                    prefProvider.setValue(Constants.SUB_TOTAL, "")
                                    prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                                    prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(Constants.TIP, "")
                                    prefProvider.setValue(Constants.TAX_CHARGE, "")
                                    prefProvider.setValue(Constants.SERVICE_CHARGE, "")
                                    if (prefProvider.getValue(
                                            ORDER_TYPE,
                                            ""
                                        ) != Constants.DINE_IN
                                    ) {

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


                                        Log.e("TAGGER", "ordertype :: $ordertype")
                                        Log.e("TAGGER", "ordertypeId :: $ordertypeId")

                                        if (viewModel.restrictedAmount(binding.txtTotal)) {


                                            viewModelPayment.updateOrder(
                                                isOrderUpdate,
                                                orderId,
                                                paymentId,
                                                paymentOfflineId,
                                                orderOfflineId
                                            )


                                            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - START*/
                                            if (viewModel.cartModel == null) {
                                                var currentCartItems = arrayListOf<TbItem>()
                                                for (tbItem in viewModel.currentCartItems) {
                                                    currentCartItems.add(
                                                        TbItem().convertCartToItem(
                                                            tbItem, tbItem
                                                        )
                                                    )
                                                }
                                                var isManual = false
                                                if (prefProvider.getValue(
                                                        Constants.REDIRECT_FROM,
                                                        ""
                                                    )
                                                        .equals("manual_sale")
                                                ) {
                                                    isManual = true
                                                } else {
                                                    isManual = false
                                                }
                                                viewModel.cartModel = CartModel().apply {
                                                    terminalId = prefProvider.getValueInt(
                                                        Constants.TERMINAL_ID, -1
                                                    )
                                                    employeeID = prefProvider.getValueInt(
                                                        Constants.EMPLOYEE_ID, -1
                                                    )
                                                    locationId = prefProvider.getValueInt(
                                                        Constants.LOCATION_ID, -1
                                                    )
                                                    orderTypeId = prefProvider.getValueInt(
                                                        Constants.ORDER_TYPE_ID, -1
                                                    )
                                                    orderType =
                                                        prefProvider.getValue(ORDER_TYPE, "")
                                                            .toString()
                                                    orderTypeName = prefProvider.getValue(
                                                        Constants.ORDER_TYPE_NAME, ""
                                                    ).toString()
                                                    items = currentCartItems
                                                    isOpenOrder = false
                                                    isMaual = isManual
                                                    isEdited = false
                                                    customer = Gson().fromJson(
                                                        prefProvider.getValue("pref_customer", "")
                                                            .toString(), TbCustomer::class.java
                                                    )
                                                    taxlistDynamic = Gson().fromJson(
                                                        prefProvider.getValue("taxlistDynamic", "")
                                                            .toString(),
                                                        object :
                                                            TypeToken<List<TaxData>?>() {}.getType()
                                                    )
                                                }

                                                viewModel.addCart(viewModel.cartModel!!)

                                            }
                                            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - END*/

                                            val cartModel = viewModel.cartModel

                                            cartModel?.openOrderType = Constants.PICK_UP
                                            cartModel?.orderType = ordertype
                                            cartModel?.orderTypeId = ordertypeId

                                            val totalAmountTobeSave =
                                                if (viewModel.redeemLoyaltyInfo.isLoyaltyApplied == true) {
                                                    (viewModel.redeemLoyaltyInfo.getAmountToBePaid()
                                                        ?: 0.0)
                                                } else {
                                                    viewModel.totalPrice
                                                }

                                            cartModel?.openOrderType = Constants.PICK_UP
                                            if (!isOrderUpdate) cartModel?.customer = assignCustomer

                                            if (isOrderUpdate) {
                                                viewModel.setCartEdited(1, cartModel?.cartId)
                                            } else {
                                                viewModel.setCartEdited(0, cartModel?.cartId)
                                            }

                                            val formatterdate = SimpleDateFormat("yyyy-MM-dd")
                                            val formattertime = SimpleDateFormat("hh:mm a")
                                            val date = Date()
                                            future_delivery_date = formatterdate.format(date)
                                            future_delivery_time = formattertime.format(date)

                                            LogUtil.logE(
                                                TAG,
                                                "UpdateOrderItemsListOLD::  ${
                                                    Gson().toJson(
                                                        viewModel.currentCartItems
                                                    )
                                                }"
                                            )

                                            LogUtil.logE(
                                                TAG,
                                                "UpdateOrderItemsListDUPLI::  ${
                                                    Gson().toJson(
                                                        viewModel.duplicateCurrentCartItem
                                                    )
                                                }"
                                            )

                                            /**
                                             * Pre Auth for OPEN ORDER
                                             */
                                            val paymentType =
                                                object : TypeToken<PaymentAttributes>() {}.type

                                            var isPreAuth =
                                                prefProvider.getValue(PRE_AUTH_DETAILS, "")
                                                    .isNotEmpty()

                                            var paymentAttributes: PaymentAttributes? = null
                                            if (isPreAuth) {
                                                if (prefProvider.getValue(
                                                        ORDER_TYPE,
                                                        ""
                                                    ) != OPEN_ORDER
                                                ) {
                                                    isPreAuth = false
                                                    paymentAttributes = null
                                                } else {
                                                    paymentAttributes = Gson().fromJson(
                                                        prefProvider.getValue(
                                                            PRE_AUTH_DETAILS,
                                                            ""
                                                        ), paymentType
                                                    ) as PaymentAttributes

                                                    //If any existing order set to Pre Auth by user then need to send OrderId for payment attribute
                                                    if (isOrderUpdate) {
                                                        paymentAttributes.order_id = orderId
                                                    }
                                                }
                                            } else if (isOrderUpdate) {

                                            }
                                            //END Pre AUTH

                                            val request = cartModel?.let {
                                                viewModelPayment.createOpenOrderRequestNew(
                                                    viewModel.currentCartItems,
                                                    it,
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
                                                    cashDiscountType,
                                                    isPreAuth = isPreAuth,
                                                    paymentAttributes

                                                )
                                            }



                                            isSaveOrder = true
                                            viewModelPayment.saveOrder(true)

                                            viewModelPayment.orderCreateCallSent = true

                                            /*Removing this for now, because it was not behaving as per requirement*/

                                            /*  try {
                                                  *//*This is added because: when we remove the updated values and make the item as default, then it was taking as updated*//*

                                        val listType = object :
                                            TypeToken<List<OnlineOrderResponseModel.Data.OrderItem>>() {}.type

                                        var redundantDatas =
                                            Gson().fromJson<List<OnlineOrderResponseModel.Data.OrderItem>>(
                                                prefProvider.getValue(Constants.OPEN_ORDER_ITEMS, ""),
                                                listType
                                            )


                                        with(request?.order){
                                            this?.orderItemsAttributes?.forEach {orderItemAttribute->
                                                redundantDatas.forEach { reduntantData->
                                                    if ((orderItemAttribute.employeeId==reduntantData.employeeId) && (orderItemAttribute.category_id==reduntantData.categoryId) && (orderItemAttribute.id==reduntantData.id) && (orderItemAttribute.itemId==reduntantData.itemId)){
                                                        if (orderItemAttribute.note.equals(reduntantData.note) && (orderItemAttribute.price==reduntantData.price) && (orderItemAttribute.totalPrice==reduntantData.totalPrice) && (orderItemAttribute.discountAmount==reduntantData.discountAmount) && (orderItemAttribute.discountType==reduntantData.discountType)
                                                            && (orderItemAttribute.quantity==reduntantData.quantity)  && (orderItemAttribute.itemName.equals(reduntantData.itemName)
                                                                    && (areTaxesEqual(orderItemAttribute,reduntantData)) && (areModifiersEqual(orderItemAttribute,reduntantData))
                                                                    && (areVariationsEqual(orderItemAttribute,reduntantData)))){

                                                            orderItemAttribute.isItemEdited=false

                                                        }
                                                    }
                                                }
                                            }
                                        }


                                    } catch (e: Exception) {
                                    }
*/
                                            EventBus.getDefault().post(
                                                MessageEvent(
                                                    "${Constants.LINE_BREAK_TAB} CartFragment.kt_binding.tvSave.setOnSingleClickListener , request?.order?.offlineId -> ${request?.order?.offlineId} , request -> ${
                                                        Gson().toJson(request)
                                                    } _2"
                                                )
                                            )

                                            //FILE ASSERTION
//                                            MainActivity.writeToFile(Gson().toJson(request),"Save_".plus(request?.order?.offlineId),activity?.filesDir,activity!!)

                                            viewModel.fromAllOrderFragment = false
                                            request?.let { it1 -> viewModelPayment.submit(it1) }
                                            if (!prefProvider.getValueboolean(
                                                    Constants.NO_NEED_TO_PRINT, false
                                                )
                                            ) {
                                                //print
                                                Log.d(
                                                    TAG,
                                                    "checkUpdation calling submit -> printing "
                                                )
                                                if (!viewModelPayment.orderCreateCallSent) request?.let { it1 ->
                                                    prefProvider.setValue(PRE_AUTH_DETAILS, "")
                                                    viewModelPayment.submit(
                                                        it1
                                                    )
                                                }
                                            } else {
                                                //no print
                                                Log.d(TAG, "checkUpdation not printing ")
                                                viewModelPayment.noUpdatesFound()
                                            }

                                            isOrderUpdate = false
                                            binding.tvSave.text = getString(R.string.save)

                                            viewModel.fromSaveOrderToAllOrders = true

                                        } else {
                                            showMessage()
                                        }
                                    }

                                    viewModel.currentCartItems = arrayListOf()
                                    viewModel.duplicateCurrentCartItem = arrayListOf()
                                } else {
                                    prefProvider.setValue(Constants.OLD_ITEM, "")
                                    prefProvider.setValue(
                                        Constants.OPEN_ORDER_ITEMS, ""
                                    )
                                    AlertUtils.showCustomAlertWithListenerWithOK(
                                        requireContext(),
                                        resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                                    ) { _, _ ->
                                        restrictButtonClick(true)
                                    }
                                }
                            } catch (e: Exception) {
                                prefProvider.setValue(Constants.OLD_ITEM, "")
                                prefProvider.setValue(
                                    Constants.OPEN_ORDER_ITEMS, ""
                                )
                                e.printStackTrace()
                            }
                        }
                    } else {
                        AlertUtils.showCustomAlert(
                            requireContext(), "Please check your Network Connectivity."
                        )
                    }
                }

                private fun areVariationsEqual(
                    orderItemAttribute: OrderItemsAttribute,
                    reduntantData: OnlineOrderResponseModel.Data.OrderItem
                ): Boolean {

                    if (orderItemAttribute.orderItemVariationAttributes != null && reduntantData.order_item_variation != null) {
                        if ((orderItemAttribute.orderItemVariationAttributes?.orderId == reduntantData.order_item_variation?.orderId) && (orderItemAttribute.orderItemVariationAttributes?.order_item_id == reduntantData.order_item_variation?.order_item_id) && (orderItemAttribute.orderItemVariationAttributes?.price == reduntantData.order_item_variation?.price) && (orderItemAttribute.orderItemVariationAttributes?.totalPrice == reduntantData.order_item_variation?.totalPrice) && (orderItemAttribute.orderItemVariationAttributes?.quantity == reduntantData.order_item_variation?.quantity) && (orderItemAttribute.orderItemVariationAttributes?.variationId == reduntantData.order_item_variation?.variationId)) {
                            return true
                        } else {
                            return false
                        }
                    } else {
                        return (orderItemAttribute.orderItemVariationAttributes == reduntantData.order_item_variation)
                    }


                    return false

                }

                private fun areModifiersEqual(
                    orderItemAttribute: OrderItemsAttribute,
                    reduntantData: OnlineOrderResponseModel.Data.OrderItem
                ): Boolean {

                    if (orderItemAttribute.orderItemModifiersAttributes != null && reduntantData.orderItemModifiers != null) {
                        if (orderItemAttribute.orderItemModifiersAttributes.isNotEmpty() && reduntantData.orderItemModifiers.isNotEmpty()) {
                            if (orderItemAttribute.orderItemModifiersAttributes.size == reduntantData.orderItemModifiers.size) {
                                orderItemAttribute.orderItemModifiersAttributes.forEach { modAttribute ->
                                    reduntantData.orderItemModifiers.forEach { modData ->
                                        if ((modAttribute.id == modData.id) && (modAttribute.name.equals(
                                                modData.name
                                            )) && (modAttribute.modifier_id == modData.modifierId) && (modAttribute.modifier_quantity == modData.modifier_quantity) && (modAttribute.modifier_set_id == modData.modifierSetId) && (modAttribute.order_item_id == modData.orderItemId) && (modAttribute.price == modData.price) && (modAttribute.quantity == modData.quantity)
                                        ) {
                                            return true
                                        }


                                    }
                                }
                            } else {
                                return false
                            }
                        } else {
                            return (orderItemAttribute.orderItemModifiersAttributes == reduntantData.orderItemModifiers)
                        }
                    } else {
                        return (orderItemAttribute.orderItemModifiersAttributes == reduntantData.orderItemModifiers)
                    }

                    return false
                }

                private fun areTaxesEqual(
                    orderItemAttribute: OrderItemsAttribute,
                    reduntantData: OnlineOrderResponseModel.Data.OrderItem
                ): Boolean {

                    if (orderItemAttribute.orderItemTaxesAttributes != null && reduntantData.orderItemTax != null) {
                        if (orderItemAttribute.orderItemTaxesAttributes.isNotEmpty() && reduntantData.orderItemTax.isNotEmpty()) {

                            if (orderItemAttribute.orderItemTaxesAttributes.size == reduntantData.orderItemTax.size) {
                                orderItemAttribute.orderItemTaxesAttributes.forEach { taxAttribute ->
                                    reduntantData.orderItemTax.forEach { taxData ->
                                        if ((taxAttribute.id == taxData.id) && (taxAttribute.name.equals(
                                                taxData.name
                                            )) && (taxAttribute.orderId == taxData.orderId) && (taxAttribute.orderItemId == taxData.orderItemId) && (taxAttribute.rate == taxData.rate) && (taxAttribute.taxId == taxData.taxId)
                                        ) {
                                            return true
                                        }


                                    }
                                }
                            } else {
                                return false
                            }
                        } else {
                            return (orderItemAttribute.orderItemTaxesAttributes == reduntantData.orderItemTax)

                        }
                    } else {
                        return (orderItemAttribute.orderItemTaxesAttributes == reduntantData.orderItemTax)
                    }

                    return false
                }

            })
    }

    private fun cleanOrderBackupDetails() {
        viewModel.backupOrderId = null
        viewModel.backupPaymentId = null
        viewModel.backupPaymentOfflineId = ""
        viewModel.backupOrderOfflineId = ""
        viewModelPayment.orderId = null
    }

    private fun restrictButtonClick(value: Boolean) {
        binding.tvSave.isEnabled = value

        Handler().postDelayed({
            binding.tvSave.isEnabled = value
        }, 2000)

    }

    private fun calculateDiscount() {

        viewModel.cartModel?.let {
            if (it.discountSelectdValue == null || it.discountSelectdValue == 0.0) {
                it.discountSelectdValue = (viewModel.totalDiscount / getCompleteCartPrice()) * 100
            }
        }

    }

    private fun getCompleteCartPrice(): Double {
        var cartCompletePrice: Double = 0.0
        viewModel.currentCartItems.forEach {
            if (!it.isDestroy) {
                var itemPrice = it.price

                it.modifiers.forEach { mod ->
                    if (!mod._destroy) {
                        itemPrice += (mod.price * mod.modifier_quantity)
                    }
                }

                itemPrice *= it.itemQuantity
                cartCompletePrice += itemPrice
            }
        }
        return cartCompletePrice
    }

    private fun checkUpdation() {
        Log.d(TAG, "checkUpdation: cartlist " + Gson().toJson(cartlist))
        if (/*tempList.length()>0 && cartlist.size>0 &&*/ tempList?.length() == cartlist[cartlist.size - 1].items?.size) {
            for (i in 0 until tempList!!.length()) {
                val tempItemModifierList =
                    ((tempList!!.get(i) as JSONObject).get("modifiers") as JSONArray)
                if (((tempList!!.get(i) as JSONObject).get("name") != cartlist[cartlist.size - 1].items!![i].name) || ((tempList!!.get(
                        i
                    ) as JSONObject).get("itemQuantity") != cartlist[cartlist.size - 1].items!![i].itemQuantity) || (tempItemModifierList.length() != cartlist[cartlist.size - 1].items!![i].modifiers.size)
                ) {
                    itemModified = true
                } else {
                    for (j in 0 until (tempItemModifierList.length())) {
                        if (((tempItemModifierList.get(j) as JSONObject).get("name") != cartlist[cartlist.size - 1].items!![i].modifiers[j].name) || ((tempItemModifierList.get(
                                j
                            ) as JSONObject).get("modifier_quantity") != cartlist[cartlist.size - 1].items!![i].modifiers[j].modifier_quantity)
                        ) {
                            itemModified = true
                        }
                    }
                }
            }
            if (!itemModified) {
                //no print
                prefProvider.setValueboolean(Constants.NO_NEED_TO_PRINT, true)
                Log.d(TAG, "checkUpdation: NO_NEED_TO_PRINT true")
            } else {
                //print
                prefProvider.setValueboolean(Constants.NO_NEED_TO_PRINT, false)
                Log.d(TAG, "checkUpdation: NO_NEED_TO_PRINT false")
            }
        } else {
            //print
            prefProvider.setValueboolean(Constants.NO_NEED_TO_PRINT, false)
            Log.d(TAG, "checkUpdation: NO_NEED_TO_PRINT false")
        }
    }


    private fun clearObserver() {
        viewLifecycleOwnerLiveData.removeObservers(viewLifecycleOwner)
        viewModel.getAllCartItems(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).asLiveData().removeObservers(viewLifecycleOwner)

        viewModel.venueDataLocal().removeObservers(viewLifecycleOwner)
        onDestroy()
    }

    // create/ update dine in order
    private fun createDineInOrder() {
        if (cartModelsList.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.DINE_IN_UPDATE, false)) {
                var itemCount = 0
                /*for (i in cartlist.indices) {
                for (j in cartlist[i].dineInList?.indices!!) {
                    if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                        itemCount++
                        break
                    }
                }
                if (itemCount != 0) {
                    break
                }

            }*/
                itemCount = viewModel.currentCartItems.size

                if (itemCount == 0) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(), getString(R.string.please_add_Atleast_one_item_in_cart)
                    ) { _, _ ->
                        restrictButtonClick(true)

                    }
                } else {
                    val request = viewModel.updateOrder(cartModelsList[0])

                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    prefProvider.setValueboolean(Constants.DINE_IN_LIST_EDIT, false)
                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                    cartModelsList[0].orderId?.let { viewModel.updateOrderCall(it, request) }


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
                for (i in cartModelsList.indices) {
                    /*for (j in cartlist[i].dineInList?.indices!!) {
                    if (cartlist[i].dineInList?.get(j)?.items?.size!! > 0) {
                        itemCount++
                        break
                    }
                }*/
                    itemCount = viewModel.currentCartItems.size
                    if (itemCount != 0) {
                        createDineInRequest()
                        break
                    }
                }

                if (itemCount == 0) {
                    bundle.clear()
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(), getString(R.string.please_add_Atleast_one_item_in_cart)
                    ) { _, _ ->
                        restrictButtonClick(true)

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
            floorModel.floorPlanId =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.floorPlanId
            floorModel.floorPlanTableId =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.id
            floorModel.tableType =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.tableType
            floorModel.tableNumber =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.tableNumber
            floorModel.chairCount =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.chairCount
            floorModel.tableName =
                cartModelsList.get(0).dineInList?.get(1)?.floorPlanTable?.tableName


        }
        viewModel.ordertypelist.forEach {
            if (it.orderType.toLowerCase() == Constants.DINE_IN.toLowerCase()) {
                cartModelsList[0].orderTypeId = it.id
                cartModelsList[0].orderType = it.orderType
            }
        }
        cartModelsList[0].openOrderType = Constants.PICK_UP
        var finalDiscount = 0.0
//        if (BuildConfig.DEBUG == false) {
//            finalDiscount = cartModelsList[0].discountPrice + viewModel.totalDiscount
//        } else {
        finalDiscount = viewModel.totalDiscount
//        }
        Log.e("checkDiscount", "totalDiscount:  ${viewModel.totalDiscount}")
        Log.e(
            "checkDiscount", "totalDiscountdiscountPrice:  ${cartModelsList[0].discountPrice}"
        )


        viewModel.createDineInOrderRequest(
            cartModel = cartModelsList[0],
            subTotalPrice = viewModel.subTotalPrice,
            totalPrice = viewModel.totalPrice - cartModelsList[0].discountPrice,
            totalServiceCharge = viewModel.totalServiceCharge,
            totalTax = viewModel.totalTax,
            ORDER_TYPE = Constants.DINE_IN,
            "",
            "",
            false,
            totalDiscount = finalDiscount,
            0.0,
            floorPlanDetails = floorModel,
            cartItems = viewModel.currentCartItems
        )


        viewModel.dineInResultCreateOrder.observe(viewLifecycleOwner) { it ->

            if (it) {
                if (viewModel.orderRequestModel != null) {
                    viewModel.submit(viewModel.orderRequestModel!!)
                    viewModel.orderRequestModel = null
                }
                viewModel.dineInResultCreateOrder.value = false
            }
        }

    }

    private fun getOrderTypes() {
        orderTypeAdapter = OrderTypeAdapter()
        orderTypeAdapter?.setCallback(this)
        binding.rvOrderType?.adapter = orderTypeAdapter

        viewModel.orderTypes().observe(requireActivity()) {

            Log.e(TAG, "checkAllOrderTypes:  ${Gson().toJson(it.data)}")

            val orderTypesToShow = it?.data?.let { it1 -> ArrayList(it1) }

            /**
             * List contains order types that we don't want to show on POS order types
             *
             */
            val orderTypesToRemove = listOf(
                "OnlineWebOrder",
                "OnlineOrder",
                "KioskTakeout",
                "OnlineWebOrder",
                "KioskOpenorder"
            )

            orderTypesToRemove.forEach { orderTypeToRemove ->

                val found =
                    orderTypesToShow?.filter { it.orderType.equals(orderTypeToRemove, true) }

                if (found?.isNotEmpty() == true)
                    orderTypesToShow.remove(found.first())
            }

            orderTypesToShow?.removeIf { orderType ->
                orderTypesToRemove.contains(orderType.orderType)
            }


            orderTypesToShow?.let { it1 -> orderTypeAdapter?.addAll(it1.filter { it.primaryOrderType }) }
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

        model?.id?.let {
            CoroutineScope(Dispatchers.IO).async {
                try {
                    var orderTypeBackup = OrderTypeBackup()
                    orderTypeBackup.orderType = it
                    orderTypeBackup.employeeId = prefProvider.employeeId()
                    orderTypeBackup.orderTypeName = model.name
                    viewModel.insertOrderTypeBackup(orderTypeBackup)
                } catch (e: Exception) {
                }
            }
        }

        Log.e(TAG, "checkOrderType  ${model?.orderType}")

        //  prefProvider.setValue(DELIVERY_TYPE, PICK_UP)
        try {
            if (model!!.orderType.equals(Constants.PHONE_ORDER, ignoreCase = true)) {
                prefProvider.setValue(DELIVERY_TYPE, "")
            } else {
                prefProvider.setValue(DELIVERY_TYPE, PICK_UP)
            }
        } catch (e: Exception) {
            prefProvider.setValue(DELIVERY_TYPE, "")
        }

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

            viewModelPayment.preAuthData = null
            checkOrderType()

            addObserver()

            DashboardCategoryBoldPOS.newInstance().keypadShow(true)
            increaseOnGoingOrderCounter()
        }

        //CLEAR PREAUTH DATA
        prefProvider.setValue(PRE_AUTH_DETAILS, "")
//        viewModel.apply {
//            paymentAttributes = null
//            authPaymentResponse = null
//            allOrderResponse = null
//        }
//
        viewModelPayment.preAuthData = null
    }

    // PRE AUTHORISE CARD
    private fun makePaxPreAuthRequest() {
        var posLink: PosLink = PosLink()

        // PAX variables
        lateinit var mPaymentRequest: PaymentRequest
        var CARDBIN = ""
        var cardLastDigits = ""
        var CardName = ""
        var EDCType = ""
        var GlobalUID = ""
        var RefNumber = ""
        var ECRRefNumber = ""
        var PAXtoken = ""
        var ExtData = ""

        GlobalScope.launch {
            Log.d(
                "getCommSettingFromFile ",
                "getCommSettingFromFile: " + Gson().toJson(
                    SettingINI.getCommSettingFromFile(
                        requireContext(),
                        "/storage/emulated/0/Download/" + SettingINI.FILENAME
                    )
                )
            )
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    requireContext(),
                    "/storage/emulated/0/Download/" + SettingINI.FILENAME
                )
            )
            val amt = PRE_AUTH_AMOUNT * 10
            val tip_amt = 0
            ECRRefNumber = System.currentTimeMillis().toString()
            var broadPOS_version = prefProvider.getValue(
                Constants.BROADPOS_VERSION,
                ""
            )

            CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("AUTH")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = amt.toString()
            mPaymentRequest.TipAmt = tip_amt.toString()
            mPaymentRequest.ECRRefNum = ECRRefNumber
            if (broadPOS_version.contains("TSYS")) {
                mPaymentRequest.ExtData = "<Force>T</Force>"
            } else if (broadPOS_version.contains("Rapid")) {
                mPaymentRequest.ExtData = "<Force>T</Force><TokenRequest>1</TokenRequest>"
            }

            Log.d("ECRRefNum", "ECRRefNum: $ECRRefNumber")

            posLink.PaymentRequest = mPaymentRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.PaymentResponse

                val response = msg.obj as com.pax.poslink.PaymentResponse
                val resultCode = response.ResultCode
                val resultTxt = response.ResultTxt
                val approvedAmount = response.ApprovedAmount
                ExtData = response.ExtData
                RefNumber = response.RefNum

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                var tipAmount = response.ApprovedTipAmount
                GlobalUID = response.PaymentTransInfo.GlobalUid
                viewModelPayment.setPAXData(RefNumber, GlobalUID)
//                prefProvider.setValue(Constants.GLOBAL_ID, globalUID!!)

//                dineInDataModel.guestPaymentReq?.paymentAttributes?.let { it ->
//                    it.cardName = response.CardType
//                    it.cardNumber = cardLastDigits
//                    it.cardType = 0.toString()
//
//                }

                //implementation("org.dom4j:dom4j:2.1.3")
                PAXtoken = response.PaymentTransInfo.Token
                Log.d("token:", "token $PAXtoken")
                Log.d("token:", "EXT $ExtData")
                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $GlobalUID $RefNumber"
                )
                Log.d(
                    "Payment Details: ",
                    "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${
                        Gson().toJson(response)
                    }"
                )

                if (resultCode == "000000") {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
                            Log.e("PRE AUTH DATA ", Gson().toJson(response.ExtData))

                            binding.preAuthOption?.apply {
                                isChecked = true
                                isEnabled = false
                                setTextColor(Color.GREEN)
                            }

                            val paymentAttributes = PaymentAttributes()

                            paymentAttributes.apply {
                                amount = PRE_AUTH_AMOUNT
                                cardName = CardName
                                cardNumber = ""
                                ecr_ref_num = ECRRefNumber
                                employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                                ext_data = ExtData
                                global_uniq_id = GlobalUID
                                pax_transaction_token = ""
                                payableType = "Order"
                                paymentType = "Card"
                                ref_num = response.RefNum
                                subTotal = 1.0
                                terminalId = prefProvider.getTerminalId()
                            }

                            prefProvider.setValue(
                                PRE_AUTH_DETAILS,
                                Gson().toJson(paymentAttributes)
                            )
                            viewModel.paymentAttributes = paymentAttributes

                            viewModelPayment.preAuthData = PreAuthData(
                                ecrRefNum = paymentAttributes.ecr_ref_num,
                                refNum = paymentAttributes.ref_num
                            )

                        }
                    }
                } else {

                    runOnUiThread(object : java.lang.Runnable {
                        override fun run() {
                            dismissProgressDialog()
                        }
                    })
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            resultTxt,
                            object :
                                DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    try {

                                        binding.preAuthOption?.apply {
                                            isChecked = false
                                            isEnabled = true
                                            setTextColor(Color.RED)
                                        }

                                        p0?.dismiss()
                                    } catch (e: Exception) {
                                    }
                                }
                            })
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
//                        connectBP()
                    }
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()

        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        prefProvider.setValue(Constants.OLD_ITEM, "")



        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)

        viewModel.fromSaveOrderToAllOrders = false
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


    /*------------Customer Loyalty--------------*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SyncCustomerEvent?) {
        event?.let {
            runOnUiThread(object : Runnable {
                override fun run() {
                    binding.txtAddCustomer.apply {
                        text = event?.customerName
                    }
                }
            })
        }
    }

    /* @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: CreateCustomerEvent?) {
        if (event?.performCreate?:false) {
//        Create Customer, this control has came from CustomerDisplay.kt, when customer is not present when giving the phone number.
            addCustomerViewModel.phoneNo.value = event?.phoneNumber
            addCustomerViewModel.submit(arrayListOf(), false, true)
        }else{
            dashboardViewModel.clickOnTakeOut()
        }
    }*/
    /*------------Customer Loyalty--------------*/


    private fun increaseOnGoingOrderCounter() {
        lifecycleScope.launch {
            viewModel.increaseOnGoingOrderCounter()
        }
    }

    private fun taxBifurcationCalculationUpdate(
        cartModel: CartModel, listItems: List<TbCartItem>
    ): CartModel {
        var listTaxData: ArrayList<TaxData> = arrayListOf()
        Log.e(TAG, "checkCurrentItems:  ${listItems.size}")
        listItems.forEach { item ->

            item.taxes?.forEachIndexed { indextax, itemtype ->
                if (itemtype.isActive && !itemtype.isDeleted) {

                    if (itemtype.taxType != "Percentage") {
                        var modifierPrice: Double = 0.0
                        val price =
                            (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)

                        item.modifiers.forEach {
                            var modQty = it.modifier_quantity * item.itemQuantity
                            modifierPrice += (it.price * modQty)
                        }

                        val totalPrice = price + modifierPrice
                        itemtype.subTotalAmount = itemtype.subTotalAmount?.plus(totalPrice)
                    }

                    var ttaxPrice = getTotalTaxBirfurcationNew(item, itemtype)
                    Log.e("checkTotalTax", "TaxPrice 2: ${ttaxPrice}")
                    itemtype.totalTaxTypePrice = ttaxPrice
                    if (listTaxData.isNotEmpty()) {

                        var checkLocal = false
                        for (i in 0 until listTaxData.size) {
                            if (itemtype.name.equals(listTaxData.get(i).name, true)) {
                                checkLocal = true
                                listTaxData.get(i).totalTaxTypePrice += ttaxPrice
                                break
                            } else {
                                checkLocal = false

                            }

                        }
                        if (checkLocal == false) {
                            listTaxData.add(itemtype)
                        }

                    } else {
                        listTaxData.add(itemtype)
                    }


                }


            }
        }

        Log.e(TAG, "checkSize  ${listTaxData.size}")
        Log.e(TAG, "checkSizeWithData  ${Gson().toJson(listTaxData)}")
        cartModel.taxlistDynamic = listTaxData
        prefProvider.setValue(
            Constants.taxListDynamic, Gson().toJson(listTaxData)
        )

        return cartModel
    }

    private fun getTotalTaxBirfurcationNew(
        item: TbCartItem, itemtype: TaxData
    ): Double {
        var totaltaxtemp: Double = 0.0
        var modifierPrice = 0.0
        val price = (item.price * item.itemQuantity) - (item.discountPrice * item.itemQuantity)
        Log.e("checkTotalTax", "checkItemPrice:  ${price}")

        item.modifiers.forEach {
            var modQty = it.modifier_quantity * item.itemQuantity
            modifierPrice += (it.price * modQty)
        }

        val totalPrice = price + modifierPrice /*- (discountPrice * item.itemQuantity)*/

        Log.e("checkTotalTax", "totalPrice:  ${totalPrice}  taxRate  ${itemtype.rate}")

        totaltaxtemp += if (itemtype.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00).toDouble()
            } else {
                val itemTaxPrice = (itemtype.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + itemtype.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00).toDouble()
            } else {
                String.format("%.2f", itemtype.rate * item.itemQuantity).toDouble()
            }

        }
        return totaltaxtemp
    }

    private var builder: Dialog? = null

    private fun showProgressDialog() {


        if (builder == null)
            builder = Dialog(requireContext())

        val inflater = LayoutInflater.from(context)

        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        builder?.window?.setBackgroundDrawable(
//            ColorDrawable(Color.WHITE)
//        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.setCancelable(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (!builder?.isShowing!!) {
            val activity: Activity = requireActivity()
            if (!activity.isFinishing && !activity?.isDestroyed) {
                try {
                    builder?.show()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun dismissProgressDialog() {
        try {
            if (builder != null && builder?.isShowing == true) {
                builder?.dismiss()
                builder = null
            }
        } catch (e: java.lang.Exception) {
            Log.d("pos", "dismissProgressDialog: " + e.message)
        }

    }
}
