package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.GuestPaymentCalculationModel
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.TimeDetailsResponse
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.databinding.ViewCustomDisplayBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ActiveTipsListAdapter
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.DineInTableAdapterCD
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.settings.tip.TipListViewModel
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.callback.OnTipAddedListener
import com.android.pos.utils.extensions.*
import com.android.pos.utils.statusUtils.Status
import com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomDisplay(
    display: Display,
    context: Context,
    val lifecycleOwner: LifecycleOwner,
    private val dashBoardCategoryViewModel: DashBoardCategoryViewModel,
    val passcodeViewModel: PasscodeViewModel,
    val dineInViewModel: DineInOrderTableViewModel,
    val onTipAdded: (Double) -> Unit = {}
) : Presentation(context, display), MyCallback, DineInAdapter.DineInCallback,
    ActiveTipsListAdapter.DiscountInterface {

    private var tippedAmount: Double = 0.0
    private var tipRate: Double = 0.0
    private var dineInPaymentDetails: GuestPaymentCalculationModel? = null
    private var isGuestPay: Boolean = false
    private var toFinalAmt: Double = 0.0
    private lateinit var dineInCartAdapter: DineInAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private lateinit var dineInTableAdapter: DineInTableAdapterCD
    private lateinit var cartAdapter: CartAdapter
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
    lateinit var activeTipsListAdapter: ActiveTipsListAdapter

    private val TAG = "CustomDisplay"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ViewCustomDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefProvider = PrefProvider(context)
        setupList()
        getCustomerList()
        observeServiceCharge()
        setupTaxAdapter()
    }

    private fun setupActiveTipsList(tipListViewModel: TipListViewModel) {
        activeTipsListAdapter = ActiveTipsListAdapter()
        binding.apply {
            rvActiveTipsList.apply {
                adapter = activeTipsListAdapter
            }
        }
        tipsListViewModel = tipListViewModel
    }

    private fun observeActiveTipsList(wholeTotalPrice: Double) {
        tipsListViewModel.getTipActiveList.observe(lifecycleOwner) {

            LogUtil.logE(TAG, "ActiveTipsList ${Gson().toJson(it)}")

            if (it.data?.isNotEmpty() == true) {

                activeTipsListAdapter.clearAll()

                val tipsList = it.data as MutableList

                val noTipExists = tipsList.filter { tdr-> tdr.name == "No Tip" }
                val otherExists = tipsList.filter { tdr-> tdr.name == "Other" }

                if(noTipExists.isEmpty()){
                    tipsList.add(
                        0,
                        GetTipReponse.Data(
                            name = "No Tip",
                            id = 0,
                            locationId = 0,
                            rate = 0.0,
                            sort = 0
                        )
                    )
                }

                if(otherExists.isEmpty()){
                    tipsList.add(
                        GetTipReponse.Data(
                            name = "Other",
                            id = 0,
                            locationId = 0,
                            rate = 0.0,
                            sort = 0
                        )
                    )
                }

                tipsList.forEach { data ->
                    data.isChecked = false
                }
                activeTipsListAdapter.setList(tipsList, wholeTotalPrice)
                activeTipsListAdapter.setListner(this)
                lifecycleOwner.lifecycleScope.launch {
                    delay(5000)
                    binding.rvActiveTipsList.smoothScrollToPosition(tipsList.size - 1)
                }
            }
        }
    }

    private fun showMainCart(
        isTipped: Boolean,
        model: GetTipReponse.Data,
        wholeTotalPrice: Double
    ) {
        binding.apply {
            binding.mainCartLayout.visible()

            if (isTipped) {
                val tippedAmount = MethodUtils.percentageCalculation(
                    wholeTotalPrice,
                    model.rate
                )
                showTipsAddedVer2(model.rate, tippedAmount)
            }

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
            binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true))
            setKeyPad()

            edtAmount.setText(MethodUtils.roundOffAmountString(0.00))

            txtContinue.setOnClickListener {
                mainCartLayout.visible()

                val tippedAmount =
                    edtAmount.text.toString().replace("$", "").trim().toDouble()
                val tipRate = MethodUtils.calculatePercentageFromAmount(
                    tippedAmount,
                    wholeTotalPrice
                )

                showTipsAddedVer2(tipRate, tippedAmount)

                addTipKeypadLayout.gone()
                askForTipLayout.gone()
                splashLayout.gone()
                thankYouLayout.gone()
            }

            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                delay(1000)
                incKeypad.tvThree.performClick()
                delay(1000)
                incKeypad.tvFive.performClick()
                delay(1000)
                incKeypad.tvSix.performClick()

                delay(1000)
                incKeypad.txtClearLast.performClick()

                delay(1000)
                incKeypad.txtClearAll.performClick()

                delay(1000)
                incKeypad.tvEight.performClick()
                delay(1000)
                incKeypad.tvZero.performClick()
                delay(1000)
                incKeypad.tvFour.performClick()

                delay(2000)
                txtContinue.performClick()
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

    private fun setupList() {

        cartAdapter = CartAdapter()
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

        dashBoardCategoryViewModel.mAllWords(
            prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(lifecycleOwner) {
            it?.let {
                updateCustomerDisplay(it)
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

    private fun updateCustomerDisplay(cartList: List<CartModel>) {
        if (this::binding.isInitialized) {

            if (cartList.isNotEmpty()) {

                binding.mainCartLayout.visibility = View.VISIBLE
                binding.splashLayout.visibility = View.GONE

                if (prefProvider.getValue(
                        Constants.ORDER_TYPE, Constants.TAKEOUT
                    ) == Constants.DINE_IN
                ) {
                    if (cartList[0].dineInList?.isNotEmpty() == true) {
                        val dineInList = cartList[0].dineInList

                        dineInCartAdapter.setList(
                            dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                        )
                    }
                } else {
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
                    ) == true
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
                    setupTotals(cartList)
                }
            }
        }
    }

    fun showSurcharge(isInCheckout: Boolean) {
        if (isInCheckout) {
            if (MethodUtils.isEnableCashDiscount(context)) {
                binding.linearCashDiscount.visible()
                if (prefProvider.getValue(
                        Constants.OPTION_TYPE,
                        "CashDiscount"
                    ) == "CashDiscount"
                ) {
                    binding.labelCashSurcharge.text = "Cash Discount"
                } else {
                    binding.labelCashSurcharge.text = "SurCharge"
                }
            } else {
                binding.linearCashDiscount.gone()
            }
        }
    }

    private fun setupTotals(cartList: List<CartModel>) {
        dashBoardCategoryViewModel.apply {

            if (order_note.isNotEmpty()) {
                binding.relativeOrderNotes.visibility = View.VISIBLE
                binding.txtOrderNote.text = order_note
            } else {
                binding.relativeOrderNotes.visibility = View.GONE
            }

            binding.txtSubTotal.text = MethodUtils.roundOffAmount(subTotalPrice)
            binding.txtTax.text = MethodUtils.roundOffAmount(totalTax)
            binding.txtServiceCharge.text = MethodUtils.roundOffAmount(totalServiceCharge)
            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(totalDiscount)
            binding.txtNoncashAdj.text = MethodUtils.roundOffAmount(cashdiscountAmount)
            //showSurcharge(true)

            dashBoardCategoryViewModel.apply {
                val data: TbCustomer? = prefProvider.getCustomerData()
                if (data != null) {
                    if (loyaltyPointCondition(data) && redeemLoyaltyInfo.needToApplyLoyalty) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._90sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.VISIBLE
                        binding.lblLoyaltyPoints.visibility = View.VISIBLE

                        binding.txtLabelLoyaltyAmounts.visibility = View.VISIBLE
                        binding.checkloylaty.visibility = View.GONE
                        binding.txtLoyaltyAmount.text = "- $${
                            String.format(
                                "%.2f", redeemLoyaltyInfo.usedLoyaltyAmount
                            )
                        }"
                        binding.txtLoyaltyPoints.text = "${redeemLoyaltyInfo.usedLoyaltyPoints}"
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._60sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.GONE
                        binding.lblLoyaltyPoints.visibility = View.GONE
                    }
                }
            }

            if (taxBirfurcationAdapter.taxlist.size < 2) {
                binding.imgDropdown.gone()
            } else {
                binding.imgDropdown.visible()
            }

            if (cartList[0].taxlistDynamic?.isNotEmpty() == true) {
                taxBirfurcationAdapter.setList(cartList[0].taxlistDynamic as ArrayList<TaxData>)
            }

            itemCalculation(cartList, binding.txtTotal, context)
        }
    }

    private fun displayCustomer() {

        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        if (name.isNotEmpty()) {
            binding.txtCustomerName.visible()
            binding.txtLoyaltyPointsLabel.visible()
            binding.txtLoyaltyPointsLabel.text =
                "Loyalty Points: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
            binding.txtCustomerName.text = name

        } else {
            binding.txtLoyaltyPointsLabel.invisible()
            binding.txtCustomerName.invisible()
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
        return this?.data?.date?.replace("\n", "")?.replace("  ", "")?.replace(",", ", ")
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int) {}

    override fun onHeaderSelected(position: Int) {}

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {}

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {}

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {}

    fun showThankYou(paidAmount: String) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()
            askForTipLayout.gone()

            thankYouLayout.visible()
            txtPaidAmount.text = "Paid $paidAmount"
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
            val response = apiService.getTimeDetails()
            Log.d("TAG", "onLogOutOrClockOutWithApiService: ${response.data.time}")

            binding.currentTime.text = response.data.time
            binding.currentDate.text = response.removeWhiteSpaces()
        }.runCatching { Log.d("TAG", "onLogOutOrClockOutWithApiService: Some Exzception") }
    }

    fun onLogOutOrClockOut() {
        binding.apply {
            mainCartLayout.gone()
            thankYouLayout.gone()
            splashLayout.visible()
        }
    }

    private fun getCustomerList() {
        dineInViewModel.customer().observe(lifecycleOwner) { it ->
            if (it.isNotEmpty()) {
                allCustomerList.clear()
                allCustomerList.addAll(it)
            }
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
                baseResponse.floorPlanTable?.merged_child_table_details.forEach {
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

                                    var listTaxes: java.util.ArrayList<TaxData> = arrayListOf()
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
                                        var modifiers: java.util.ArrayList<Modifier> = arrayListOf()
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
                                                model.orderItemTaxes = mod.orderItemTaxes
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
                                    totalTaxWT = String.format("%.2f", totalTaxWT).toDouble()

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
                                LogUtil.logE("TODO", "serviceChargeWT:  ${serviceChargeWT}")

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
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                var isApplied = false
                serviceChargeList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                baseResponse.guestAttributes.size - 1
                            )
                        ) {
                            Log.d(
                                TAG,
                                "calculateDineInServiceCharge: DashBoard " + it.min_guest_count + "....." + it.max_guest_count + " in between " + baseResponse.guestAttributes.size.minus(
                                    1
                                )
                            )
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
            totalServiceChargeAmount = MethodUtils.getTwoDecimal(totalServiceChargeAmount)

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


                Log.d("TODO", "suTotalPaidGuest: " + subTotalDInin)

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

                var unpaidCount = (baseResponse.guestAttributes.size - 1) - paidGuestCount

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
            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN && prefProvider.getValueboolean(
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

            if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {

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

    fun showTipsAdded(tipAmount: Double, WholetotalPrice: Double) {
        if (tipAmount == 0.00) {
            binding.tipLayout.gone()
        } else {
            binding.tipLayout.visible()
            val percentageTip = String.format(
                "%.0f", MethodUtils.calculatePercentageFromAmount(
                    tipAmount,
                    WholetotalPrice
                )
            )

            binding.tipPercentLabel.text = "Tip ($percentageTip%)"
            binding.txtTipGiven.text = "" + MethodUtils.roundOffAmount(tipAmount)

        }

    }

    private fun showTipsAddedVer2(tipRate: Double, tipAmount: Double) {
        if (tipAmount == 0.00) {
            binding.tipLayout.gone()
        } else {
            binding.tipLayout.visible()
            binding.tipPercentLabel.text = "Tip (${String.format("%.0f", tipRate)}%)"
            binding.txtTipGiven.text = "" + MethodUtils.roundOffAmount(tipAmount)
        }
        //CALLBACK METHOD CALL
        onTipAdded(tipAmount)
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

/*
        getDineInOrderDetails(
            getOrderDetailsResponse,
            totalTax = totalTax,
            totalAmount = TotalAmt,
            totalDis = discount,
            cashOrSurCharge = CashOrSurcharge,
            subTotal = subtotal,
            TotalServiceCharge = serviceCharge
        )*/


    }

    fun setCustomerList(list: List<TbCustomer>) {
        allCustomerList.clear()
        allCustomerList.addAll(list)
    }


    fun showWouldYouLikeToAddTipScreen(
        tipListViewModel: TipListViewModel,
        wholeTotalPrice: Double
    ) {
        binding.apply {
            askForTipLayout.visible()
            setupActiveTipsList(tipListViewModel)
            observeActiveTipsList(wholeTotalPrice)

            mainCartLayout.gone()
            splashLayout.gone()
            thankYouLayout.gone()
            addTipKeypadLayout.gone()

            val isSignatureRequired = true

            if (isSignatureRequired) {
                signRootLayout.visible()
                tvContinue.visible()
                signLinearLayout.gravity = Gravity.TOP
            } else {
                signRootLayout.gone()
                tvContinue.gone()
                signLinearLayout.gravity = Gravity.CENTER_VERTICAL
            }

            binding.tvContinue.setOnSingleClickListener {
                mainCartLayout.visible()

//                val tippedAmount = 14.06 //Take this amount from selected item from list of active tips
//                //MethodUtils.percentageCalculation(
//                //                        wholeTotalPrice,
//                //                        model.rate
//                //                    )
//                val tipRate = MethodUtils.calculatePercentageFromAmount(
//                    tippedAmount,
//                    wholeTotalPrice
//                )
                Log.d(TAG, "showWouldYouLikeToAddTipScreen: TIP-RATE = $tipRate")
                Log.d(TAG, "showWouldYouLikeToAddTipScreen: TIPPED-AMOUNT = $tippedAmount")
                showTipsAddedVer2(tipRate, tippedAmount)

                addTipKeypadLayout.gone()
                askForTipLayout.gone()
                splashLayout.gone()
                thankYouLayout.gone()
            }

            signaturePad.setOnSignedListener(object : OnSignedListener {
                override fun onStartSigning() {
                    yourSignatureLabel.invisible()
                }

                override fun onSigned() {

                }

                override fun onClear() {
                    yourSignatureLabel.visible()
                }

            })


        }
    }

    override fun selectedItem(model: GetTipReponse.Data, pos: Int, wholeTotalPrice: Double) {
        when (model.name) {
            "No Tip" -> {
                showMainCart(false, model, wholeTotalPrice)
            }
            "Other" -> {
                showTipKeypad(wholeTotalPrice)
            }
            else -> {
                tipRate = model.rate
                tippedAmount = MethodUtils.percentageCalculation(
                    wholeTotalPrice,
                    model.rate
                )
                Log.d(TAG, "selectedItem: CALLED")
                lifecycleOwner.lifecycleScope.launch {
                    delay(3000)
                    binding.tvContinue.performClick()
                }
                //showMainCart(true, model, wholeTotalPrice)
            }
        }
    }
}
