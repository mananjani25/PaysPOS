package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.MagtekOnlineOrderRefundResponse
import com.android.pos.data.model.responseModel.TimeDetailsResponse
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CUSTOMER_SIGN_REQUIRED_ON_CD
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.MANUAL_SALE
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.ViewCustomDisplayBinding
import com.android.pos.di.ApiModule1
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ActiveTipsListAdapter
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.DineInTableAdapterCD
import com.android.pos.ui.adapter.boldpos.CartAdapterCustomerDisplay
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.magtek.MagtekRequestUtils
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.ui.fragments.settings.tip.TipListViewModel
import com.android.pos.ui.fragments.transactions.TransactionViewModel
import com.android.pos.utils.*
import com.android.pos.utils.MethodUtils.Companion.toPrecision
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.*
import com.android.pos.utils.paxUtils.AppThreadPool
import com.android.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.android.pos.utils.paxUtils.SettingINI
import com.android.pos.utils.statusUtils.Status
import com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class CustomDisplay(
    display: Display,
    context: Context,
    val lifecycleOwner: LifecycleOwner,
    private val dashBoardCategoryViewModel: DashBoardCategoryViewModel,
    val passcodeViewModel: PasscodeViewModel,
    val dineInViewModel: DineInOrderTableViewModel
) : Presentation(context, display), MyCallback, DineInAdapter.DineInCallback,
    ActiveTipsListAdapter.DiscountInterface {

    private var mWholeTotalPrice: Double = 0.0
    private var signatureInBase64: String = ""
    private var mIsSignatureRequired: Boolean = false
    private lateinit var apiModule1: ApiModule1
    private lateinit var magtekRequestUtils: MagtekRequestUtils
    private lateinit var magensaResponse: String
    private var tippedAmount: Double = 0.0
    private var tipRate: Double = 0.0
    private var mOrderID: Int = 0
    private var mIsCardPayment: Boolean = false
    lateinit var mTransactionViewModel: TransactionViewModel
    lateinit var mTipListViewModel: TipListViewModel
    lateinit var mPaymentViewModel: PaymentViewModel

    private var dineInPaymentDetails: GuestPaymentCalculationModel? = null
    private var isGuestPay: Boolean = false
    private var toFinalAmt: Double = 0.0
    private lateinit var dineInCartAdapter: DineInAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private lateinit var dineInTableAdapter: DineInTableAdapterCD
    private lateinit var cartAdapter: CartAdapterCustomerDisplay
    private var wholeTableDiscount: Double = 0.0
    private var cashDiscountGlobal: Double = 0.0
    private var subTotalDInin = 0.0
    private var globalOrderDiscount = 0.0
    var update_order_Discount = 0.0
    private var finalTaxAmt = 0.0
    var paidGuestAmount = 0
    private var totalDiscount: Double = 0.0
    private lateinit var binding: ViewCustomDisplayBinding
    lateinit var prefProvider: PrefProvider
    var serviceChargeList: java.util.ArrayList<TbServiceCharge> = arrayListOf()
    private var allCustomerList: java.util.ArrayList<TbCustomer> = arrayListOf()
    private var serviceCharge = 0.0

    var notPayAnyAmount: Boolean = false

    lateinit var tipsListViewModel: TipListViewModel
    var activeTipsListAdapter: ActiveTipsListAdapter? = null

    private var showCashCreditPrice = false

    private val TAG = "CustomDisplay"
    // PAX variables
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()
    var CARDBIN = ""
    var cardLastDigits = ""
    var CardName = ""
    var EDCType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ViewCustomDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefProvider = PrefProvider(context)
        setupCartList()
        getCustomerList()
        observeServiceCharge()
        setupTaxAdapter()
        initPOSLink()
    }


    private fun setupCartList() {

        cartAdapter = CartAdapterCustomerDisplay()
        cartAdapter.setCallback(this)

        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)

        binding.rvCartList.layoutManager = LinearLayoutManager(context)

        if (prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT) == Constants.DINE_IN) {
            binding.rvCartList.adapter = dineInCartAdapter
        } else {
            binding.rvCartList.adapter = cartAdapter
        }

    }

    override fun onDisplayChanged() {
        super.onDisplayChanged()
        //prefProvider = PrefProvider(context)
        Log.d(
            TAG, "onDisplayChanged: ${
                prefProvider.getValue(
                    REDIRECT_FROM,
                    ""
                )
            }"
        )
        if (this::prefProvider.isInitialized && prefProvider.getValue(
                REDIRECT_FROM,
                ""
            ) == MANUAL_SALE
        ) {
            dashBoardCategoryViewModel.manualSaleItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(lifecycleOwner) {
                it?.let {
                    updateCustomerDisplay(it)
                }
            }
        } else {
            dashBoardCategoryViewModel.mAllWords(
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(lifecycleOwner) {
                it?.let {
                    updateCustomerDisplay(it)
                }
            }
        }

    }

    private fun observeServiceCharge() {
        dineInViewModel.getServiceChargeList.observe(lifecycleOwner) { it ->
            if (it.data?.isNotEmpty() == true) {
                if (it.status == Status.SUCCESS) {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_DINEIN_ORDER,
                            false
                        )
                    ) {
                        serviceChargeList = arrayListOf()
                        it.data.forEach { service ->
                            if (service.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                serviceChargeList = it.data.toCollection(arrayListOf())
                                // dineInTableAdapter.setSurchargeList(serviceChargeList)
                            }
                        }
                    }

                }

            }

        }

    }

    fun updateCustomerDisplay(cartList: List<CartModel>) {
        Log.d(TAG, "updateCustomerDisplay: OUTSIDE")
        showCashCreditPrice = prefProvider.getValueboolean(
            Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY,
            false
        )
        if (this::binding.isInitialized) {
            Log.d(TAG, "updateCustomerDisplay: INSIDE")
            if (cartList.isNotEmpty()) {

                binding.mainCartLayout.visibility = View.VISIBLE
                binding.splashLayout.visibility = View.GONE

                val isDineIn = prefProvider.getValue(
                    Constants.ORDER_TYPE, Constants.TAKEOUT
                ) == Constants.DINE_IN

                if (isDineIn) {
                    if (cartList[0].dineInList?.isNotEmpty() == true) {
                        val dineInList = cartList[0].dineInList

                        dineInCartAdapter.setList(
                            dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                        )
                        binding.rowHeaderLayoutDineIn?.visible()
                        binding.rowHeaderLayout.gone()
                    }
                } else {
                    binding.rowHeaderLayoutDineIn?.gone()
                    binding.rowHeaderLayout.visible()
                    if (MethodUtils.isEnableCashDiscount(context) && showCashCreditPrice && prefProvider.getValue(
                            ORDER_TYPE,
                            TAKEOUT
                        ) != Constants.GIFT_CARD
                    ) {
                        binding.txtTotalLabel.gone()
                        binding.txtCashLabel.visible()
                        binding.txtCardLabel.visible()
                    } else {
                        binding.txtTotalLabel.visible()
                        binding.txtCashLabel.gone()
                        binding.txtCardLabel.gone()

                    }
                    cartList[0].items?.toCollection(arrayListOf())?.let { it1 ->
                        cartAdapter.setList(it1)
                    }
                    displayCustomer()
                }
                if (prefProvider.getValue(
                        ORDER_TYPE,
                        ""
                    ) == DINE_IN && prefProvider.getValueboolean(
                        Constants.IS_PAYMENT_SCREEN,
                        false
                    )
                ) {
                    var string_gson = prefProvider.getValue(Constants.SPLIT_DINEIN_MODEL, "")
                    var temp_model =
                        Gson().fromJson(string_gson, GuestPaymentCalculationModel::class.java)


                    binding.txtServiceCharge.text =
                        MethodUtils.roundOffAmount(temp_model?.serviceCharge ?: 0.0)
                    binding.txtTotal.text = MethodUtils.roundOffAmount(temp_model?.total ?: 0.0)
                    binding.txtDiscount.text =
                        "-" + MethodUtils.roundOffAmount(temp_model?.totalDiscount ?: 0.0)
                    binding.txtTax.text = MethodUtils.roundOffAmount(temp_model?.tax ?: 0.0)
                    binding.txtNoncashAdj.text =
                        MethodUtils.roundOffAmount(temp_model?.cashDiscount ?: 0.0)
                    binding.txtSubTotal.text =
                        MethodUtils.roundOffAmount(temp_model?.subTotal ?: 0.0)

                } else {
                    setupTotalsNew(isDineIn)
                }
            }
        }
    }

    private var isInsideCheckout = false

    fun showSurcharge(isInCheckout: Boolean) {
        isInsideCheckout = isInCheckout
        if (isInCheckout) {
            if (MethodUtils.isEnableCashDiscount(context)) {
                binding.lnrLayoutCashDiscountSurcharge?.visible()
                if (prefProvider.getValue(
                        Constants.OPTION_TYPE,
                        "CashDiscount"
                    ) == "CashDiscount"
                ) {
                    binding.txtCashDiscountSurchargeLabel?.text = "Cash Discount"
                } else {
                    binding.txtCashDiscountSurchargeLabel?.text = Constants.SURCHARGE_TEXT
                }
            } else {
                binding.lnrLayoutCashDiscountSurcharge?.gone()
            }
        }
    }

    private fun setupTotalsNew(isDineIn: Boolean) {
        showCashCreditPrice = prefProvider.getValueboolean(
            Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY,
            false
        )
        dashBoardCategoryViewModel.apply {
            if (MethodUtils.isEnableCashDiscount(context) && !isDineIn && showCashCreditPrice && prefProvider.getValue(
                    ORDER_TYPE,
                    TAKEOUT
                ) != Constants.GIFT_CARD
            ) {
                binding.txtSubTotalCash?.visible()
                binding.txtSubTotalCard?.visible()
                binding.txtTaxCash?.visible()
                binding.txtTaxCard?.visible()
                binding.txtServiceChargeCash?.visible()
                binding.txtServiceChargeCard?.visible()
                binding.txtDiscountCash?.visible()
                binding.txtDiscountCard?.visible()

                binding.lnrLayoutCashTotal?.visible()
                binding.lnrLayoutCardTotal?.visible()
                binding.txtOrderTotal?.gone()

                if (prefProvider.getValue(
                        Constants.OPTION_TYPE,
                        "CashDiscount"
                    ) == "CashDiscount"
                ) {

                    binding.txtSubTotalCash?.text = getCashDiscountedPrice(subTotalPrice)
                    binding.txtSubTotalCard?.text = MethodUtils.roundOffAmount(subTotalPrice)

                    binding.txtTaxCash?.text = getCashDiscountedPrice(totalTax)
                    binding.txtTaxCard?.text = MethodUtils.roundOffAmount(totalTax)

                    binding.txtServiceChargeCash?.text = getCashDiscountedPrice(totalServiceCharge)
                    binding.txtServiceChargeCard?.text = MethodUtils.roundOffAmount(totalServiceCharge)

                    binding.txtTotalCash?.text = getCashDiscountedPrice(totalPrice)
                    binding.txtTotalCard?.text = MethodUtils.roundOffAmount(totalPrice)

                } else {
                    binding.txtSubTotalCash?.text = MethodUtils.roundOffAmount(subTotalPrice)
                    binding.txtSubTotalCard?.text = getSurchargedPrice(subTotalPrice)

                    binding.txtTaxCash?.text = MethodUtils.roundOffAmount(totalTax)
                    binding.txtTaxCard?.text = getSurchargedPrice(totalTax)

                    binding.txtServiceChargeCash?.text = MethodUtils.roundOffAmount(totalServiceCharge)
                    binding.txtServiceChargeCard?.text = getSurchargedPrice(totalServiceCharge)

                    binding.txtTotalCash?.text = MethodUtils.roundOffAmount(totalPrice)
                    binding.txtTotalCard?.text = getSurchargedPrice(totalPrice)

                }

                binding.txtDiscountCash?.text = "-${MethodUtils.roundOffAmount(totalDiscount)}"
                binding.txtDiscountCard?.text = "-${MethodUtils.roundOffAmount(totalDiscount)}"

            } else {
                binding.txtSubTotalCash?.gone()
                binding.txtSubTotalCard?.text = MethodUtils.roundOffAmount(subTotalPrice)

                binding.txtTaxCash?.gone()
                binding.txtTaxCard?.text = MethodUtils.roundOffAmount(totalTax)

                binding.txtServiceChargeCash?.gone()
                binding.txtServiceChargeCard?.text = MethodUtils.roundOffAmount(totalServiceCharge)

                binding.txtDiscountCash?.gone()
                binding.txtDiscountCard?.text = "-${MethodUtils.roundOffAmount(totalDiscount)}"

                binding.lnrLayoutCashTotal?.gone()
                binding.lnrLayoutCardTotal?.gone()
                binding.txtOrderTotal?.visible()


                if (MethodUtils.isEnableCashDiscount(context) && prefProvider.getValue(ORDER_TYPE, TAKEOUT) != Constants.GIFT_CARD) {

                    if (prefProvider.getValue(
                            Constants.OPTION_TYPE,
                            "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        if(isInsideCheckout){
                            binding.txtOrderTotal?.text = getCashDiscountedPrice(totalPrice)
                        }else{
                            binding.txtOrderTotal?.text = MethodUtils.roundOffAmount(totalPrice)
                        }

                        binding.txtCashDiscountSurchargeCard?.text = "-"+MethodUtils.roundOffAmount(cashdiscountAmount)
                    } else {
                        if(isInsideCheckout){
                            binding.txtOrderTotal?.text = getSurchargedPrice(totalPrice)
                        }else{
                            binding.txtOrderTotal?.text = MethodUtils.roundOffAmount(totalPrice)
                        }
                        binding.txtCashDiscountSurchargeCard?.text =
                            MethodUtils.roundOffAmount(cashdiscountAmount)
                    }

                } else {
                    binding.txtOrderTotal?.text = MethodUtils.roundOffAmount(totalPrice)
                }



            }

        }

    }

    private fun getSurchargedPrice(amount: Double): String {
        return MethodUtils.roundOffAmount(amount + getCashDiscountOrSurChargeAmount(amount))
    }

    private fun getCashDiscountedPrice(amount: Double): String {
        return MethodUtils.roundOffAmount(amount - getCashDiscountOrSurChargeAmount(amount))
    }

    private fun getCashDiscountOrSurChargeAmount(amount: Double): Double {
        return MethodUtils.calculateCashDiscount(
            amount,
            prefProvider,
            context
        )
    }

    private fun displayCustomer() {

        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        if (name.isNotEmpty()) {
            binding.txtCustomerName.visible()
            binding.txtLoyaltyPointsLabel.visible()
            if (dashBoardCategoryViewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                binding.tvLoyaltyBalance.visible()
                binding.tvLoyaltyPoints.visible()
                binding.tvLoyaltyBalance.text =
                    "${context.resources.getString(R.string.applied_loyalty_balance)}: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyAmount}"
                binding.tvLoyaltyPoints.text =
                    "${context.resources.getString(R.string.applied_loyalty_points)}: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
            } else {
                binding.tvLoyaltyBalance.invisible()
                binding.tvLoyaltyPoints.invisible()
            }
            binding.txtLoyaltyPointsLabel.text =
                "Loyalty Balance: ${
                    if (dashBoardCategoryViewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                        dashBoardCategoryViewModel.redeemLoyaltyInfo.remainingLoyaltyPoints
                    } else {
                        dashBoardCategoryViewModel.redeemLoyaltyInfo.availablePoints
                    }}"
            binding.txtCustomerName.text = name

        } else {
            binding.txtLoyaltyPointsLabel.invisible()
            binding.txtCustomerName.invisible()
            binding.tvLoyaltyBalance.invisible()
            binding.tvLoyaltyPoints.invisible()
            binding.relativeLoylatyPoints.gone()
            binding.lblLoyaltyPoints.gone()
            if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._80sdp).toInt()
            } else {
                binding.liinearInfoLayout.layoutParams.height =
                    resources.getDimension(R.dimen._70sdp).toInt()

            }
        }

    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        taxBirfurcationAdapter.setList(arrayListOf())
    }

    fun onTaxClicked(shouldShow: Boolean) {
        if (this::binding.isInitialized && this::taxBirfurcationAdapter.isInitialized) {
            if (shouldShow) {

                if (taxBirfurcationAdapter.taxlist.size == 1) {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._80sdp).toInt()
                    } else {
                        if (binding.relativeLoylatyPoints.isVisible()) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._80sdp).toInt()
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                        }
                    }
                } else if (taxBirfurcationAdapter.taxlist.size == 2) {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._100sdp).toInt()
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._90sdp).toInt()
                    }
                } else {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._110sdp).toInt()
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._105sdp).toInt()
                    }

                }

                binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                binding.relativeDynamicTax.visible()

            } else {

                if (binding.relativeLoylatyPoints.isVisible()) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._80sdp).toInt()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._60sdp).toInt()
                }

                binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                binding.relativeDynamicTax.gone()

            }
        }
    }

    private fun TimeDetailsResponse?.removeWhiteSpaces(): CharSequence? {
        return this?.data?.date?.replace("\n", "")?.replace("  ", "")
            ?.replace(",", ", ")
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int) {}

    override fun onHeaderSelected(position: Int) {}

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {}

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {}

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {}
    override fun onRemoveGuest(position: Int) {}

    fun showThankYou(paidAmount: Double) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()
            askForTipLayout.gone()
            addTipKeypadLayout.gone()
            progressLayout.gone()

            thankYouLayout.visible()
            txtPaidAmount.text = "Paid $${paidAmount.toPrecision(2)}"
        }
    }

    fun showProgress() {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()
            askForTipLayout.gone()
            addTipKeypadLayout.gone()
            thankYouLayout.gone()

            progressLayout.visible()
        }
    }

    fun onLogOutOrClockOutWithApiService(apiService: ApiService) {
        binding.apply {
            mainCartLayout.gone()
            thankYouLayout.gone()
            splashLayout.visible()
            val email = prefProvider.getValue(Constants.EMAIL, "")
            if (email.isNotEmpty()) {
                callTimeApi(apiService)
            }
        }
    }

    private fun callTimeApi(apiService: ApiService) {
        lifecycleOwner.lifecycleScope.launch {
            val response = apiService.getTimeDetails(prefProvider.getValueInt(TERMINAL_ID,-1))

            binding.currentTime.text = response.data.time
            binding.currentDate.text = response.removeWhiteSpaces()
        }.runCatching {
            Log.d(
                "CustomDisplay",
                "onLogOutOrClockOutWithApiService: Some Exception"
            )
        }
    }

    fun onLogOutOrClockOut() {
        binding.apply {
            mainCartLayout.gone()
            thankYouLayout.gone()
            splashLayout.visible()
        }
    }

    private fun getCustomerList() {

        try {
            dineInViewModel.customer().observe(lifecycleOwner) { it ->
                if (it.isNotEmpty()) {
                    allCustomerList.clear()
                    allCustomerList.addAll(it)
                }
            }
        }catch (e:Exception){
            Log.d("getCustomerList","exception : ${e.toString()}")
        }


    }


    fun showTableDetails(baseResponse: GetOrderDetailsResponse.Data) {

        getCustomerList()
        getDineInOrderDetails(baseResponse)
//        val cartlist: java.util.ArrayList<CartModel> = arrayListOf()
//        cartlist.add(CartModel().apply { dineInList = dineInListItems })
//        updateCustomerDisplay(cartlist)

    }

    private fun getDineInOrderDetails(
        baseResponse: GetOrderDetailsResponse.Data,
        subTotal: Double? = 0.0,
        TotalServiceCharge: Double? = 0.0,
        totalTax: Double? = 0.0,
        totalAmount: Double? = 0.0,
        cashOrSurCharge: Double? = 0.0,
        totalDis: Double? = 0.0
    ) {
        if (baseResponse != null) {
            binding.mainCartLayout.visibility = View.VISIBLE
            binding.splashLayout.visibility = View.GONE

            //table name,chair for mergedOccupied,single table details in Header
            if (baseResponse.floorPlanTable.status == Constants.MERGEDANDOCCUPIED) {

                var listTableMerge: java.util.ArrayList<String> = arrayListOf()
                listTableMerge.add(baseResponse.floorPlanTable.tableNumber.toString())
                baseResponse.floorPlanTable?.merged_child_table_details?.forEach {
                    listTableMerge.add(it.table_number.toString())

                }
                var txtMergedTbNo = android.text.TextUtils.join(",", listTableMerge)
                //  binding.txtTitle.text = "Table " + txtMergedTbNo

            } else {
                //binding.txtTitle.text = baseResponse.floorPlanTable.tableName
            }

            //getOrderDetailsResponse = baseResponse
            var subTotalWT: Double = 0.0
            var totalTaxWT: Double = 0.0
            var serviceChargeWT: Double = 0.0
            var totalPriceWT: Double = 0.0
            var guestShareTotal: Double = 0.0
            var isPaid = true
            var isAllFired = true
            var noItem = true

            //Calculation for Whole Table Price and add dvide guest share

            var dineInList: java.util.ArrayList<DineInModel> = arrayListOf()
            var totalSubTotal: Double = 0.0
            var totalTaxAmount: Double = 0.0
            var totalServiceChargeAmount: Double = 0.0
            var totalFinalAmount: Double = 0.0
            var totalCashDiscount: Double = 0.0
            var totalItemDiscount: Double = 0.0

            var totalGuestCount = 0
            //extract logic from API data and drag & drop code
            for (i in 0 until baseResponse.guestAttributes.size) {
                val model = DineInModel()
                var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                model.title = baseResponse.guestAttributes.get(i).name
                model.isHeader = 0
                model.id = baseResponse.guestAttributes[i].id
                model.isPaid = baseResponse.guestAttributes.get(i).isPaid
                if (baseResponse.guestAttributes.get(i).guestItemAttributes.isNotEmpty()) {
                    if (baseResponse.guestAttributes.get(i).name.trim()
                            .lowercase() != "Whole Table".trim().lowercase()
                    ) {
                        totalGuestCount++
                    }

                }
                model.serviceChargeList = serviceChargeList


                if (baseResponse.guestAttributes.get(
                        i
                    ).customerId != 0
                ) {
                    Log.e(
                        "CheckCustomerList",
                        "allCustomerList:  ${Gson().toJson(allCustomerList)}"
                    )
                    allCustomerList.forEach {
                        if (it.id == baseResponse.guestAttributes.get(i).customerId) {
                            model.customer = it
                        }
                    }

                }

                dineInList.add(model)

                var guestSubTotal: Double = 0.0
                var guestTotalTax: Double = 0.0
                var guestServiceCharge: Double = 0.0


                for (j in 0 until baseResponse.guestAttributes.get(i).guestItemAttributes.size) {
                    if (baseResponse.orderItems.isNotEmpty()) {
                        baseResponse.orderItems.forEach {
                            if (guestItem[j].timestamp != null) {
                                if (it.timestamp.trim()
                                        .lowercase() == guestItem[j].timestamp.trim()
                                        .lowercase()
                                ) {
//                                        val guestAttr = baseResponse.guestAttributes.get(j)
                                    //Whole Table Calculation
                                    totalItemDiscount += it.discountAmount

                                    if (baseResponse.guestAttributes.get(i).isPaid) {
                                        notPayAnyAmount = true
                                    }

                                    //for add item in tbItem List and extract/convert data from API
                                    val itemDineIn: DineInModel = DineInModel()
                                    val item = TbItem()
                                    item.isPaid = it.isPaid
                                    item.discountPrice = it.discountAmount
                                    item.discountId = it.discountId
                                    item.discountType = it.discountType.toString()

                                    item.name = it.itemName
                                    item.itemId = it.itemId
                                    item.categoryId = it.categoryId
                                    item.guestItemId = guestItem[j].id

                                    var listTaxes: java.util.ArrayList<TaxData> =
                                        arrayListOf()
                                    it.orderItemTaxes.forEach {
                                        listTaxes.add(
                                            TaxData(
                                                createdAt = it.createdAt,
                                                id = it.id,
                                                locationId = prefProvider.getValueInt(
                                                    Constants.LOCATION_ID,
                                                    0
                                                ),
                                                name = it.name,
                                                rate = it.rate,
                                                taxType = it.taxType,
                                                updatedAt = it.updatedAt,
                                                isActive = true,
                                                isDefault = it.isDefault,
                                                isCustomAmount = false,
                                                itemPricing = "",
                                                itemIds = arrayListOf(),
                                                orderTaxId = it.taxId
                                            )
                                        )
                                    }
                                    item.taxes = listTaxes
                                    if (it.orderItemModifiers.isNotEmpty()) {
                                        var modifiers: java.util.ArrayList<Modifier> =
                                            arrayListOf()
                                        it.orderItemModifiers.forEach { mod ->
                                            val model = Modifier()
                                            model.id = mod.modifierId
                                            model.itemQuantity = mod.quantity
                                            model.name = mod.name
                                            model.orderModifierId = mod.id
                                            model.price = mod.price
                                            model.modifierSetId = mod.modifier_set_id
                                            model.modifier_quantity =
                                                mod.modifier_quantity!!


                                            if (mod.orderItemTaxes.isNotEmpty()) {
                                                model.orderItemTaxes =
                                                    mod.orderItemTaxes
                                            }

                                            modifiers.add(model)


                                        }
                                        item.modifiers = modifiers

                                    }
                                    item.price = it.price
                                    item.itemQuantity = it.quantity
                                    item.orderItemId = it.id
                                    item.note = it.note
                                    item.isFired = guestItem.get(j).is_fired
                                    item.timeStamp = it.timestamp
                                    if (it.orderItemModifiers.isNotEmpty()) {
                                        item.modifier_set_ids =
                                            modifiersIds(it.orderItemModifiers)
                                    }
                                    if (it.order_item_variation != null) {
                                        item.variationsAttributes =
                                            variationAtt(it.order_item_variation!!)
                                    }
                                    itemDineIn.isHeader = 1
                                    itemDineIn.item = item
                                    itemDineIn.empName =
                                        baseResponse.floorPlanTable.lockByName.toString()

                                    dineInList.add(itemDineIn)


                                    if (!it.isPaid) {

                                        totalSubTotal += (it.quantity * it.price) - it.discountAmount
                                        if (it.orderItemModifiers.isNotEmpty()) {
                                            it.orderItemModifiers.forEach { mod ->
                                                totalSubTotal += mod.price * mod.quantity

                                            }
                                        }

                                        if (it.orderItemTaxes.isNotEmpty()) {
                                            it.orderItemTaxes.forEach { tax ->
                                                if (!it.isPaid) {
                                                    totalTaxAmount += if (tax.taxType == "Percentage") {

                                                        var modifierPrice = 0.0
                                                        val price =
                                                            (it.price * it.quantity) - it.discountAmount

                                                        it.orderItemModifiers.forEach {
                                                            modifierPrice += (it.price * it.quantity)
                                                        }

                                                        val totalPrice =
                                                            price + modifierPrice

                                                        val itemTaxPrice =
                                                            (tax.rate * totalPrice) / 100
                                                        LogUtil.logE(
                                                            "itemTaxPrice",
                                                            "" + itemTaxPrice
                                                        )

                                                        itemTaxPrice
                                                        // MethodUtils.getTwoDecimal(itemTaxPrice)
                                                        /* String.format("%.2f", itemTaxPrice)
                                                 .toDouble()*/


                                                    } else {
                                                        //   MethodUtils.getTwoDecimal(tax.rate * it.quantity)
                                                        tax.rate * it.quantity


                                                    }


                                                }
                                            }

                                        }
                                    }

                                    if (!it.isPaid) {
                                        isPaid = it.isPaid
                                    }
                                    if (!it.isFired) {
                                        isAllFired = false
                                    }


                                }
                            }

                        }

                    }
                }


            }


            for (k in 0 until baseResponse.guestAttributes.size) {
                val obj = baseResponse.guestAttributes.get(k)
                if (baseResponse.guestAttributes.get(k).isPaid) {
                    notPayAnyAmount = true
                }

                obj.guestItemAttributes.forEach {
                    baseResponse.orderItems.forEach { oi ->
                        if (it.timestamp.trim().lowercase() == oi.timestamp.trim()
                                .lowercase()
                        ) {
                            if (obj.name.trim()
                                    .lowercase() == "Whole Table".trim().lowercase()
                            ) {

                                wholeTableDiscount += oi.discountAmount

                                if (!oi.isPaid) {

                                    subTotalWT += (oi.quantity * oi.price) - oi.discountAmount
                                    if (oi.orderItemModifiers.isNotEmpty()) {
                                        oi.orderItemModifiers.forEach { mod ->
                                            subTotalWT += mod.price * mod.quantity

                                        }
                                    }

                                }


                                if (oi.orderItemTaxes.isNotEmpty()) {
                                    oi.orderItemTaxes.forEach { tax ->
                                        var modifierPrice = 0.0
                                        val price =
                                            (oi.price * oi.quantity)

                                        oi.orderItemModifiers.forEach { mod ->
                                            modifierPrice += (mod.price * mod.quantity)
                                        }

                                        val totalPrice =
                                            price + modifierPrice - oi.discountAmount

                                        if (!oi.isPaid) {
                                            totalTaxWT += if (tax.taxType == "Percentage") {
                                                val itemTaxPrice =
                                                    (tax.rate * totalPrice) / 100

                                                itemTaxPrice

                                                /*  String.format("%.2f", itemTaxPrice)
                                          .toDouble()*/
                                            } else {
                                                if (totalPrice <= 0.0) {
                                                    String.format("%.2f", 0.00)
                                                        .toDouble()
                                                } else {
                                                    tax.rate * oi.quantity
                                                    /*String.format(
                                            "%.2f",
                                            tax.rate * oi.quantity
                                        )
                                            .toDouble()*/
                                                }
                                            }

                                        }
                                    }
                                    totalTaxWT =
                                        String.format("%.2f", totalTaxWT).toDouble()

                                }
                                serviceChargeWT = 0.0
                                if (prefProvider.getValueboolean(
                                        Constants.SERVICECHARGE_DINEIN_ORDER,
                                        false
                                    )
                                ) {
                                    var isApplied = false
                                    serviceChargeList.forEach {
                                        if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                            if (isInRange(
                                                    it.min_guest_count!!,
                                                    it.max_guest_count!!,
                                                    baseResponse.guestAttributes.size - 1
                                                )
                                            ) {
                                                isApplied = true
                                                serviceChargeWT += (subTotalWT * it.percentage) / 100
                                                return@forEach
                                            }
                                        }
                                    }
                                    if (!isApplied) {
                                        serviceChargeList.forEach { service ->
                                            if (service.id == checkMaxGuestCountId(
                                                    serviceChargeList
                                                )
                                            ) {
                                                serviceChargeWT += (subTotalWT * service.percentage) / 100
                                                return@forEach
                                            }
                                        }
                                    }
                                }

                                totalPriceWT = subTotalWT + totalTaxWT + serviceChargeWT
                                LogUtil.logE("TODO", "totalPriceWT:  ${totalPriceWT}")

                                guestShareTotal =
                                    totalPriceWT / (baseResponse.guestAttributes.size - 1)
                                LogUtil.logE("TODO", "subTotalWT:  ${subTotalWT}")
                                LogUtil.logE("TODO", "totalTaxWT:  ${totalTaxWT}")
                                LogUtil.logE(
                                    "TODO",
                                    "serviceChargeWT:  ${serviceChargeWT}"
                                )

                            }

                        }

                    }


                }


            }


            var orderDiscount = 0.0
            var newLocalDiscountCal = 0.0
            if (baseResponse.totalDiscount - totalItemDiscount > 0) {
                orderDiscount = baseResponse.totalDiscount - totalItemDiscount
                globalOrderDiscount = baseResponse.totalDiscount - totalItemDiscount
                update_order_Discount = baseResponse.totalDiscount - totalItemDiscount
            }
            totalServiceChargeAmount = 0.0
            if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                var isApplied = false
                serviceChargeList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                baseResponse.guestAttributes.size - 1
                            )
                        ) {
                            isApplied = true
                            totalServiceChargeAmount += (totalSubTotal * it.percentage) / 100
                            return@forEach
                        }
                    }
                }
                if (!isApplied) {
                    serviceChargeList.forEach { service ->
                        if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                            totalServiceChargeAmount += (totalSubTotal * service.percentage) / 100
                            return@forEach
                        }
                    }
                }
            }


            totalSubTotal = MethodUtils.getTwoDecimal(totalSubTotal)
            totalTaxAmount = MethodUtils.getTwoDecimal(totalTaxAmount)
            totalServiceChargeAmount =
                MethodUtils.getTwoDecimal(totalServiceChargeAmount)

            Log.e("JAN2023", "totalSubTotal  ${totalSubTotal}")
            Log.e("JAN2023", "totalTaxAmount  ${totalTaxAmount}")
            Log.e("JAN2023", "totalServiceChargeAmount  ${totalServiceChargeAmount}")
            Log.e("JAN2023", "orderDiscount  ${orderDiscount}")

            var finalAmount =
                totalSubTotal + totalTaxAmount + totalServiceChargeAmount - orderDiscount
            LogUtil.logE("TODO", "finalAmount  ${finalAmount}")
            LogUtil.logE("TODO", "guestShareTotal  ${guestShareTotal}")
            dineInList.get(0).guestDividedAmt =
                guestShareTotal
            dineInList.get(0).totalGuestCount = baseResponse.guestAttributes.size - 1
            dineInList.get(0).wholeTableSubTotal =
                subTotalWT / (baseResponse.guestAttributes.size - 1)
            dineInList.get(0).wholeTableTax =
                totalTaxWT / (baseResponse.guestAttributes.size - 1)
            dineInList.get(0).wholeTableSurTax =
                serviceChargeWT / (baseResponse.guestAttributes.size - 1)
            LogUtil.logE("WholeDiscount", "wholeTableDiscount  ${wholeTableDiscount}")
            dineInList.get(0).wholeTableDiscont =
                MethodUtils.roundOffAmountDouble(wholeTableDiscount / (baseResponse.guestAttributes.size - 1))

            dineInList.get(0).orderDiscount = orderDiscount
            dineInList.get(0).orderTotalAmount =
                MethodUtils.roundOffAmountDouble(baseResponse.subTotal + baseResponse.totalTaxAmount + baseResponse.totalServiceCharges)



            LogUtil.logE(TAG, "totalTaxAmount:  ${totalTaxAmount}")
            dineInViewModel.totalTaxAmount = totalTaxAmount
            subTotalDInin = totalSubTotal - orderDiscount
            serviceCharge = totalServiceChargeAmount
            totalDiscount = orderDiscount + totalItemDiscount
            finalTaxAmt = totalTaxAmount
            LogUtil.logE(TAG, "GotsubTotalDIninfinalAmount  ${finalAmount}")

            //  toFinalAmt = finalAmount


            var paidGuestCount = 0

            for (i in 0 until baseResponse.guestAttributes.size) {
                val obj = baseResponse.guestAttributes.get(i)

                if (obj.isPaid) {
                    paidGuestCount++
                }
            }
            /*   for (i in 0 until dineInList.size) {
       if (i != 0 && dineInList.size > i + 1) {
           if (dineInList.get(i + 1).item != null && dineInList.get(i + 1).item?.isPaid == true) {
               paidGuestCount++
           }
       }
   }*/
            LogUtil.logE("Customerdiaply", "paidGuestCount:  ${paidGuestCount}")
            if (paidGuestCount > 0) {

                paidGuestAmount = paidGuestCount

                if (paidGuestCount > 0) {
                    paidGuestAmount = paidGuestCount
                    var perGTotal =
                        subTotalWT / (baseResponse.guestAttributes.size - 1)
                    LogUtil.logE(TAG, "perGTotal:  ${perGTotal}")
                    subTotalDInin = totalSubTotal - (perGTotal * paidGuestCount)
                } else {
                    subTotalDInin = totalSubTotal
                }

                //subTotalDInin -= baseResponse.totalDiscount

                var tempServicecharge = 0.0
                if (paidGuestCount > 0) {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_DINEIN_ORDER,
                            false
                        )
                    ) {
                        var isApplied = false
                        serviceChargeList.forEach {
                            if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                                if (isInRange(
                                        it.min_guest_count!!,
                                        it.max_guest_count!!,
                                        baseResponse.guestAttributes.size - 1
                                    )
                                ) {
                                    isApplied = true
                                    tempServicecharge += (subTotalDInin * it.percentage) / 100
                                    return@forEach
                                }
                            }

                        }
                        if (!isApplied) {
                            serviceChargeList.forEach { service ->
                                if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                                    tempServicecharge += (subTotalDInin * service.percentage) / 100
                                    return@forEach
                                }
                            }
                        }


                    }
                    serviceCharge = tempServicecharge

                }

                var unpaidCount =
                    (baseResponse.guestAttributes.size - 1) - paidGuestCount

                if (paidGuestCount > 0) {
                    var tempTax = totalTaxWT / (baseResponse.guestAttributes.size - 1)

                    var guestTax = totalTaxAmount - totalTaxWT
                    finalTaxAmt = (tempTax * unpaidCount) + guestTax
                    //finalTaxAmt = (subTotalWT / totalGuestCount) * unpaidCount + totalTaxAmt
                }

                var perGuestorderDis =
                    orderDiscount / (baseResponse.guestAttributes.size - 1)
                var orderDis = orderDiscount - (perGuestorderDis * paidGuestCount)


                var finalAmount = subTotalDInin + serviceCharge + finalTaxAmt - orderDis

                Log.e("toFinalCustomDisplay", "finalAmount:  ${finalAmount}")
                toFinalAmt = finalAmount


            } else {
                toFinalAmt = finalAmount
            }
            Log.e(
                "checkGuestPayFlag",
                "checkGuestPayFlag:  ${dashBoardCategoryViewModel.getIsGuestPay()}"
            )
            if (prefProvider.getValue(
                    ORDER_TYPE,
                    ""
                ) == DINE_IN && prefProvider.getValueboolean(
                    Constants.IS_PAYMENT_SCREEN,
                    false
                ) == true
            ) {
                binding.txtTotal.text = MethodUtils.roundOffAmount(
                    totalAmount ?: 0.0
                )

                binding.txtSubTotal.text = MethodUtils.roundOffAmount(
                    subTotal ?: 0.0
                )

                binding.txtTax.text = MethodUtils.roundOffAmount(
                    totalTax ?: 0.0
                )

                binding.txtServiceCharge.text = MethodUtils.roundOffAmount(
                    TotalServiceCharge ?: 0.0
                )


                binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(
                    totalDis ?: 0.0
                )

            } else {

                binding.txtTotal.text = MethodUtils.roundOffAmount(
                    toFinalAmt
                )

                binding.txtSubTotal.text = MethodUtils.roundOffAmount(
                    subTotalDInin
                )

                binding.txtTax.text = MethodUtils.roundOffAmount(
                    finalTaxAmt
                )

                binding.txtServiceCharge.text = MethodUtils.roundOffAmount(
                    serviceCharge
                )


                binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(
                    baseResponse.totalDiscount
                )
            }

            if (prefProvider.getValueboolean(
                    Constants.CASHDIS_SURCHARGEENABLE,
                    false
                )
            ) {

                cashDiscountGlobal = MethodUtils.calculateCashDiscount(
                    finalAmount,
                    prefProvider,
                    context
                )

            }


            if (dineInList.isNotEmpty()) {
                dineInTableAdapter = DineInTableAdapterCD()
                binding.rvCartList.adapter = dineInTableAdapter
                dineInTableAdapter.setList(dineInList)


            }


        }


    }

    fun showTipsAddedNew(
        tipAmountForCard: Double,
        tipAmountForCash: Double,
        WholetotalPrice: Double,
        isSplitCase: Boolean = false
    ) {
        if (isSplitCase) {
            //clear tip selection in CustomerDisplay
            activeTipsListAdapter?.clearSelectedItem()
            shouldHighlightNoTipLayout(false)
            shouldHighlightOtherTipLayout(false)
        }
        if (MethodUtils.isEnableCashDiscount(context) && showCashCreditPrice) {
            if (tipAmountForCash == 0.00 && tipAmountForCard == 0.00) {
                binding.lnrLayoutTip?.gone()
            } else {
                binding.lnrLayoutTip?.visible()
                binding.txtTipLabel?.text = "Tip"
                binding.txtTipCash?.visible()
                binding.txtTipCard?.visible()
                binding.txtTipCash?.text =
                    "" + MethodUtils.roundOffAmount(tipAmountForCash)
                binding.txtTipCard?.text =
                    "" + MethodUtils.roundOffAmount(tipAmountForCard)

            }
        } else {
            if (tipAmountForCash == 0.00) {
                binding.lnrLayoutTip?.gone()
            } else {
                binding.lnrLayoutTip?.visible()
                val percentageTip = String.format(
                    "%.0f", MethodUtils.calculatePercentageFromAmount(
                        tipAmountForCash,
                        WholetotalPrice
                    )
                )

                binding.txtTipLabel?.text = "Tip ($percentageTip%)"
                binding.txtTipCash?.invisible()
                binding.txtTipCard?.visible()
                binding.txtTipCard?.text =
                    "" + MethodUtils.roundOffAmount(tipAmountForCash)

            }

        }
    }

    private fun setupActiveTipsList(tipListViewModel: TipListViewModel) {
        activeTipsListAdapter = ActiveTipsListAdapter()
        binding.apply {
            rvActiveTipsList.apply {
                layoutManager = GridLayoutManager(context, 4)
                adapter = activeTipsListAdapter
            }
        }
        tipsListViewModel = tipListViewModel
    }

    private fun observeActiveTipsList(wholeTotalPrice: Double) {
        tipsListViewModel.getTipActiveList.observe(lifecycleOwner) {

            LogUtil.logE(TAG, "ActiveTipsList ${Gson().toJson(it)}")

            if (it.data?.isNotEmpty() == true) {

                activeTipsListAdapter?.clearAll()

                it.data.forEach { data ->
                    data.isChecked = false
                }
                binding.rvActiveTipsList.layoutManager = GridLayoutManager(context, it.data.size)
                activeTipsListAdapter?.setList(it.data, wholeTotalPrice)
                activeTipsListAdapter?.setListner(this)
                lifecycleOwner.lifecycleScope.launch {
                    //delay(5000)
                    //binding.rvActiveTipsList.smoothScrollToPosition(tipsList.size - 1)
                }
            }
        }
    }

    private fun showMainCart() {
        binding.apply {
            binding.mainCartLayout.visible()
            addTipKeypadLayout.gone()
            askForTipLayout.gone()
            binding.splashLayout.gone()
            binding.thankYouLayout.gone()
        }
    }

    private fun showTipKeypad(wholeTotalPrice: Double) {
        binding.apply {

            askForTipLayout.gone()
            splashLayout.gone()
            mainCartLayout.gone()
            thankYouLayout.gone()

            addTipKeypadLayout.visible()
            binding.edtAmount.addTextChangedListener(
                AmountTextWatcher(
                    binding.edtAmount,
                    true
                )
            )
            setKeyPad()

            edtAmount.setText(MethodUtils.roundOffAmountString(0.00))

            txtContinue.setOnClickListener {

                tippedAmount =
                    edtAmount.text.toString().replace("$", "").trim().toDouble()

                if (mIsCardPayment) {
                    if (mIsSignatureRequired) {
                        showWouldYouLikeToAddTipScreen(
                            tipsListViewModel,
                            mTransactionViewModel,
                            mWholeTotalPrice,
                            mOrderID,
                            mIsCardPayment,
                            mPaymentViewModel,
                            magtekRequestUtils,
                            apiModule1,
                            true
                        )
                    } else {
//                        magtekCall(wholeTotalPrice)

                        /*if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                            magtekCall(wholeTotalPrice)
                        } else {
                            adjustPaxTips()
                        }*/

                        if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                            magtekCall(wholeTotalPrice)
                        } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                            adjustPaxTips()
                        } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                            AlertUtils.showCustomAlert(
                                context,
                                "Please connect to PAX device"
                            )
                        }
                    }
                } else {
                    callUpdateTip()
                }
            }

        }
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ","onFinish")
                }
            })
    }

    private fun adjustPaxTips() {
        GlobalScope.launch {
            posLink.SetCommSetting(SettingINI.getCommSettingFromFile(Constants.FILE_PATH + SettingINI.FILENAME))
            val tip_amt = (tippedAmount*100).toInt()
            Log.d("Amt: ","tip $tip_amt RefNo ${mPaymentViewModel.paxReferenceNo}")

            /*CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }*/
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("ADJUST")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = tip_amt.toString()
            mPaymentRequest.OrigRefNum = mPaymentViewModel.paxReferenceNo
            //Added for TSYS ADJUST issue
            mPaymentRequest.ECRRefNum = mPaymentViewModel.paxReferenceNo
            mPaymentRequest.ExtData = "<Force>T</Force>"

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
                val ExtData = response.ExtData

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                var tipAmount = response.ApprovedTipAmount
                val globalUID = response.PaymentTransInfo.GlobalUid

                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $globalUID"
                )
                Log.d("Payment Details: ", "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${Gson().toJson(response)}")

                if (resultCode == "000000") {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
                            callUpdateTip()
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        Log.d("resultCode not 000000:","param $resultCode $resultTxt")
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
//                    ProgressUtils.dismissProgressDialog()
                    if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT"){
                        Log.d("Error: ","Please check your internet connection")
//                        Toast.makeText(requireContext(), "Please check your internet connection", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("Error: ","getMerchantDetails Failed ${result.Code} ${result.Msg}")
//                        Toast.makeText(requireContext(), "getMerchantDetails Failed ${result.Code} ${result.Msg}", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
    }

    private fun setKeyPad() {
        binding.incKeypad.tvOne.setOnSingleClickListener {
            calculateValue("1", false)
        }

        binding.incKeypad.tvTwo.setOnSingleClickListener {
            calculateValue("2", false)
        }

        binding.incKeypad.tvThree.setOnSingleClickListener {
            calculateValue("3", false)
        }

        binding.incKeypad.tvFour.setOnSingleClickListener {
            calculateValue("4", false)
        }

        binding.incKeypad.tvFive.setOnSingleClickListener {
            calculateValue("5", false)
        }

        binding.incKeypad.tvSix.setOnSingleClickListener {
            calculateValue("6", false)
        }

        binding.incKeypad.tvSeven.setOnSingleClickListener {
            calculateValue("7", false)
        }

        binding.incKeypad.tvEight.setOnSingleClickListener {
            calculateValue("8", false)
        }

        binding.incKeypad.tvNine.setOnSingleClickListener {
            calculateValue("9", false)
        }

        binding.incKeypad.tvZero.setOnSingleClickListener {
            calculateValue("0", false)
        }

        binding.incKeypad.txtClearAll.setOnSingleClickListener {
            binding.edtAmount.setText("0.00")
        }

        binding.incKeypad.txtClearLast.setOnSingleClickListener {
            calculateValue("", true)
        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))
        } else {
            binding.edtAmount.append(number)
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun modifiersIds(orderItemModifiers: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = java.util.ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                it.modifier_set_id?.let { it1 -> selectedIds.add(it1) }
            }
        }
        var uniqueSelectedId = HashSet<Int>(selectedIds)
        return uniqueSelectedId.toList()
    }

    private fun variationAtt(variation: GetOrderDetailsResponse.Data.OrderItem.OrderItemVariationAttribute): List<VariationsAttribute> {

        val variationsAttributeList = java.util.ArrayList<VariationsAttribute>()

        if (variation != null) {
            val variationsAttribute = VariationsAttribute()
            variationsAttribute.id = variation.variationId
            variationsAttribute.name = variation.name
            variationsAttribute.price = variation.price
            variationsAttribute.orderVariationId = variation.id
            variationsAttributeList.add(variationsAttribute)

        }

        return variationsAttributeList
    }

    fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
        return (minn <= value && value <= maxx)
    }

    fun checkMaxGuestCountId(serviceChargeList: java.util.ArrayList<TbServiceCharge>): Int {
        var maxValue = 0
        var serviceChargeId = 0
        serviceChargeList.forEach { serviceCharge ->
            if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                if (serviceCharge.max_guest_count!! >= maxValue) {
                    maxValue = serviceCharge.max_guest_count
                    serviceChargeId = serviceCharge.id
                }
            }
        }
        return serviceChargeId
    }

    fun setGuestPay(
        value: Boolean,
        model: GuestPaymentCalculationModel
    ) {
        isGuestPay = value
        binding.mainCartLayout.visibility = View.VISIBLE
        binding.splashLayout.visibility = View.GONE
        dineInPaymentDetails = model

    }

    fun setCustomerList(list: List<TbCustomer>) {
        allCustomerList.clear()
        allCustomerList.addAll(list)
    }


    fun showWouldYouLikeToAddTipScreen(
        tipListViewModel: TipListViewModel,
        transactionViewModel: TransactionViewModel,
        wholeTotalPrice: Double,
        orderId: Int,
        isCardPayment: Boolean,
        paymentViewModel: PaymentViewModel,
        magRequestUtils: MagtekRequestUtils,
        apiModule1: ApiModule1,
        fromKeypad: Boolean = false
    ) {
        mTipListViewModel = tipListViewModel
        mOrderID = orderId
        mIsCardPayment = isCardPayment
        mIsSignatureRequired =
            prefProvider.getValueboolean(CUSTOMER_SIGN_REQUIRED_ON_CD, false)
        mTransactionViewModel = transactionViewModel
        mPaymentViewModel = paymentViewModel
        magensaResponse = mPaymentViewModel.magensaResponse ?: ""
        magtekRequestUtils = magRequestUtils
        this.apiModule1 = apiModule1
        mWholeTotalPrice = wholeTotalPrice

        binding.apply {
            askForTipLayout.visible()
            setupActiveTipsList(mTipListViewModel)
            observeActiveTipsList(wholeTotalPrice)

            mainCartLayout.gone()
            splashLayout.gone()
            thankYouLayout.gone()
            addTipKeypadLayout.gone()

            if (fromKeypad && tippedAmount > 0.0) {
                shouldHighlightOtherTipLayout(true)
                shouldHighlightNoTipLayout(false)
                binding.txtOtherLabel.text = "Other ($${tippedAmount.toPrecision(2)})"
            } else {
                shouldHighlightOtherTipLayout(false)
                binding.txtOtherLabel.text = "Other"
            }

            if (mIsSignatureRequired && mIsCardPayment) {
                signRootLayout.visible()
                tvContinue.visible()
                disableConfirmButton()
                signLinearLayout.gravity = Gravity.TOP
            } else {
                signRootLayout.gone()
                tvContinue.gone()
                signLinearLayout.gravity = Gravity.CENTER_VERTICAL
            }

            binding.clearSignLayout.setOnClickListener {
                binding.signaturePad.clear()
            }

            binding.otherRootLayout.setOnClickListener {
                activeTipsListAdapter?.clearSelectedItem()
                showTipKeypad(wholeTotalPrice)
            }

            binding.noTipRootLayout.setOnClickListener {
                showThankYou(mWholeTotalPrice)
            }

            binding.tvContinue.setOnSingleClickListener {

                signatureInBase64 = bitmapToBase64(signaturePad.signatureBitmap)

//                magtekCall(wholeTotalPrice)

                if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                    magtekCall(wholeTotalPrice)
                } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                    adjustPaxTips()
                } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                    AlertUtils.showCustomAlert(
                        context,
                        "Please connect to PAX device"
                    )
                }

            }

            signaturePad.setOnSignedListener(object : OnSignedListener {

                override fun onStartSigning() {
                    yourSignatureLabel.invisible()
                    clearSignLayout.visible()
                    if (tippedAmount > 0.0) {
                        enableConfirmButton()
                    }
                }

                override fun onSigned() {
                    clearSignLayout.visible()
                    if (tippedAmount > 0.0) {
                        enableConfirmButton()
                    }
                }

                override fun onClear() {
                    disableConfirmButton()
                    yourSignatureLabel.visible()
                    clearSignLayout.invisible()
                }

            })

        }
    }

    private fun shouldHighlightNoTipLayout(isHighlight: Boolean) {
        if (isHighlight) {
            binding.noTipRootLayout.setBackgroundColor(Color.parseColor("#ED5950"))
            binding.txtNoTipLabel.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.noTipRootLayout.setBackgroundColor(Color.parseColor("#363636"))
            binding.txtNoTipLabel.setTextColor(Color.parseColor("#ED5950"))
        }

    }

    private fun shouldHighlightOtherTipLayout(isHighlight: Boolean) {
        if (isHighlight) {
            binding.otherRootLayout.setBackgroundColor(Color.parseColor("#ED5950"))
            binding.txtOtherLabel.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.otherRootLayout.setBackgroundColor(Color.parseColor("#363636"))
            binding.txtOtherLabel.setTextColor(Color.parseColor("#ED5950"))
            binding.txtOtherLabel.text = "Other"
        }

    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT).replace("\n", "")
    }

    private fun callUpdateTip() {
        lifecycleOwner.lifecycleScope.launch {
            showProgress()
            mTransactionViewModel.updateTipWithSignature(
                mOrderID,
                signatureInBase64,
                tippedAmount
            )
            mTransactionViewModel.updateTipData.observe(lifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    if (it.status == 200) {
                        prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                        showThankYou(mWholeTotalPrice + tippedAmount)
                    } else {
                        showErrorLayout(it.message)
                    }
                }
            }

        }
    }

    private fun showErrorLayout(message: String) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()
            askForTipLayout.gone()
            addTipKeypadLayout.gone()
            progressLayout.gone()
            thankYouLayout.gone()

            errorLayout.visible()
            txtErrorMessage.text = message

            tvTryAgain.setOnClickListener {
                errorLayout.gone()
                askForTipLayout.visible()
            }
        }
    }

    override fun selectedItem(
        model: GetTipReponse.Data,
        pos: Int,
        wholeTotalPrice: Double
    ) {
        tipRate = model.rate
        tippedAmount = MethodUtils.percentageCalculation(wholeTotalPrice, model.rate)
        Log.d("selectedItem: ","tip params $tipRate $tippedAmount")
        if (mIsCardPayment && !mIsSignatureRequired) {
//            callUpdateTip()
            if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                magtekCall(wholeTotalPrice)
            } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                adjustPaxTips()
            } else if(!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)){
                AlertUtils.showCustomAlert(
                    context,
                    "Please connect to PAX device"
                )
            }
        } else if (!mIsCardPayment) {
            callUpdateTip()
        }
        if (!binding.signaturePad.isEmpty) {
            enableConfirmButton()
        }
    }

    private fun enableConfirmButton() {
        binding.tvContinue.isEnabled = true
        binding.tvContinue.setBackgroundColor(Color.parseColor("#ED5950"))
    }

    private fun disableConfirmButton() {
        binding.tvContinue.isEnabled = false
        binding.tvContinue.setBackgroundColor(Color.GRAY)
    }

    private fun magtekCall(
        wholeTotalPrice: Double
    ) {
        showProgress()
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == "OnlineWebOrder") {
            val model = Gson().fromJson(
                magensaResponse,
                MagtekOnlineOrderRefundResponse::class.java
            )
            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*if (model != null) {
            jsonArray =
                model.transactionOutput.token.let { it1 ->
                    tippedAmount.times(100).let {
                        magtekRequestUtils.processTokenFirstData(
                            it,
                            it1,
                            model.customerTransactionID ?: "",
                            model.transactionOutput.transactionOutputDetails[0].value,
                            Constants.CAPTURE
                        )
                    }
                }

            networkCall(jsonArray, 0, apiModule1)
        }*/
                    callUpdateTip()
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput.token.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (tippedAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0, apiModule1)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        wholeTotalPrice.times(100).let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                Constants.CAPTURE,
                                (tippedAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (tippedAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = wholeTotalPrice.plus(tippedAmount)

                    jsonArray = amount.times(100).let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0, apiModule1)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = wholeTotalPrice.plus(tippedAmount)

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        amount.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tippedAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        wholeTotalPrice.let {
                            it.let { it2 ->
                                magtekRequestUtils.processReferenceIDTSYSCapture(
                                    it2,
                                    model.customerTransactionID ?: "",
                                    it1,
                                    (tippedAmount)
                                )
                            }
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }


            }
        } else {
            val model = Gson().fromJson(
                magensaResponse,
                PaymentResponse.PaymentResponseItem::class.java
            )


            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*if (model != null) {
            jsonArray =
                model.transactionOutput?.token?.let { it1 ->
                    tippedAmount.times(100).let {
                        magtekRequestUtils.processTokenFirstData(
                            it,
                            it1,
                            model.customerTransactionID ?: "",
                            model.transactionOutput.transactionOutputDetails[0].value,
                            Constants.CAPTURE
                        )
                    }
                }

            networkCall(jsonArray, 0, apiModule1)
        }*/
                    callUpdateTip()
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput?.token?.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (tippedAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0, apiModule1)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        wholeTotalPrice.times(100).let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                Constants.CAPTURE,
                                (tippedAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (tippedAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amt = wholeTotalPrice.plus(tippedAmount)

                    jsonArray = amt.times(100).let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0, apiModule1)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amt = wholeTotalPrice.plus(tippedAmount)

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        amt.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tippedAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {


                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        wholeTotalPrice.let {
                            it.let { it2 ->
                                magtekRequestUtils.processReferenceIDTSYSCapture(
                                    it2,
                                    model.customerTransactionID ?: "",
                                    it1,
                                    (tippedAmount)
                                )
                            }
                        }
                    }
                    networkCall(jsonArray, 1, apiModule1)
                }


            }
        }

    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int, apiModule1: ApiModule1) {
        //ProgressUtils.showProgressDialog(context as Activity)

        val call = if (i == 1) {
            jsonArray1?.let { apiModule1.getRetrofit1().processReferenceID(it) }
        } else {
            jsonArray1?.let { apiModule1.getRetrofit1().processToken(it) }
        }

        call!!.enqueue(object : Callback<PaymentResponse> {

            override fun onResponse(
                call: Call<PaymentResponse>,
                response: Response<PaymentResponse>
            ) {
                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null) {

                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {
                            callUpdateTip()
                        } else {
                            showErrorLayout(response.body()!![0].transactionOutput?.transactionMessage.toString())
                        }

                    } else {
                        if (response.body()!![0].mPPGv4WSFault != null) {
                            showErrorLayout(
                                response.body()!![0].mPPGv4WSFault?.faultCode + "\n" +
                                        response.body()!![0].mPPGv4WSFault?.faultReason.toString()
                            )
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                t.localizedMessage?.let { showErrorLayout(it) }
            }
        })
    }

    fun updateTotals(cashTotal: String, cardTotal: String) {

        if (MethodUtils.isEnableCashDiscount(context) && !showCashCreditPrice) {
            if (prefProvider.getValue(
                    Constants.OPTION_TYPE,
                    "CashDiscount"
                ) == "CashDiscount"
            ) {
                binding.txtOrderTotal?.text = cashTotal
            } else {
                binding.txtOrderTotal?.text = cardTotal
            }
        } else {
            binding.txtTotalCash?.text = cashTotal
            binding.txtTotalCard?.text = cardTotal
        }
    }
}
