package com.pays.pos.ui.fragments.dashboard.bolddashboard

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.indices
import androidx.lifecycle.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pax.poslink.log.LogFilter.Const
import com.pays.payments.callbacks.PaymentCallback
import com.pays.payments.design.*
import com.pays.payments.gateways.dejavoo.DejavooPaymentGateway
import com.pays.payments.gateways.valor.ValorPaymentGateway
import com.pays.pos.R
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.GuestPaymentCalculationModel
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.MagtekOnlineOrderRefundResponse
import com.pays.pos.data.model.responseModel.TimeDetailsResponse
import com.pays.pos.data.model.valor.ValorSuccessResponse
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CUSTOMER_SIGN_REQUIRED_ON_CD
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.MANUAL_SALE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.REDIRECT_FROM
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.databinding.ViewCustomDisplayBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.CreateCustomerEvent
import com.pays.pos.logger.MessageEvent
import com.pays.pos.logger.SyncCustomerEvent
import com.pays.pos.ui.adapter.ActiveTipsListAdapter
import com.pays.pos.ui.adapter.DineInAdapter
import com.pays.pos.ui.adapter.DineInTableAdapterCD
import com.pays.pos.ui.adapter.boldpos.CartAdapterCustomerDisplay
import com.pays.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.ui.fragments.transactions.TransactionViewModel
import com.pays.pos.utils.*
import com.pays.pos.utils.MethodUtils.Companion.generalizeAmount
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.ProgressUtils.dismissProgressDialog
import com.pays.pos.utils.callback.MyCallback
import com.pays.pos.utils.extensions.*
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import com.pays.pos.data.remote.Constants.IS_PAYMENT_SCREEN
import com.pays.pos.logger.CashBoxEvent
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
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
    val dineInViewModel: DineInOrderTableViewModel,
    val isTipBeforeScreen:Boolean = false
    /* val makeOneTimeReload:Boolean=false*/
) : Presentation(ContextThemeWrapper(context, R.style.CustomPresentationTheme), display), MyCallback, DineInAdapter.DineInCallback,
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
    public var refreshCount = 1

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
        Log.d("CUSTOM_DISPLAY::", "onCreate")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ViewCustomDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefProvider = PrefProvider(context)
        setupCartList()
        getCustomerList()
        observeServiceCharge()
        observeCashCardChange()
        observePasscodeScreen()
        setupTaxAdapter()
        initPOSLink()
        initViews()
        getDetails()
        getLoyaltyPointListObserver()
        initDiscountLiveData()


        if(prefProvider.getValueboolean(Constants.IS_PAYMENT_SCREEN,false)) {
            Log.e("TIP BEFORE WORKING","TIP BEFORE ENABLED")
            binding.askForTipBeforeLayout.visible()
        }
        else {
            Log.e("TIP BEFORE WORKING","TIP BEFORE NOT ENABLED")
            binding.askForTipBeforeLayout.gone()
        }



        //payment is in progress disable tip before clicks
        dashBoardCategoryViewModel.paymentInProgress.observe(lifecycleOwner) {
            if(it) {
                binding.askForTipBeforeLayout.gone()
                binding.mainCartLayout.visible()
            } else {
                if(dashBoardCategoryViewModel.tipBeforeEnabled)
                binding.askForTipBeforeLayout.visible()
            }
        }
        dashBoardCategoryViewModel.removeMainCart.observe(lifecycleOwner,object :Observer<Boolean>{
            override fun onChanged(t: Boolean?) {
                t?.let {
                    if (it){
                        binding.mainCartLayout.gone()
                    }else{
                        binding.mainCartLayout.visible()
                    }
                }
            }
        })

        dashBoardCategoryViewModel.tipRemovedObserver.observe(lifecycleOwner,object :Observer<Boolean>{
            override fun onChanged(value: Boolean) {
                if(value) {

                    Log.e("TIP BEFORE TRANSACTION","TIP BEFORE TRANSACTION")

                    binding.apply {
                        lnrLayoutTip.gone()
                    }


                    //shouldHighlightNoTipLayoutBefore(dashBoardCategoryViewModel.totalTipAmount == 0.0 )
                    //dashBoardCategoryViewModel.tipRemovedObserver.value = false
                }
            }
        })

    }


    fun checkForTipBeforeTransaction(_tipListViewModel: TipListViewModel){

        tipsListViewModel = _tipListViewModel

         Log.e("TIP BEFORE WORKING","CHECK FOR TIP BEFORE TRANSACTION")


        dashBoardCategoryViewModel.customerGivenTipBefore.observe(lifecycleOwner){
            if(it){
                if(dashBoardCategoryViewModel.totalTipAmount > 0.0) {
                    binding.apply {
                        askForTipBeforeLayout.gone()
                        mainCartLayout.visible()
                    }
                }
            }
        }


        binding.otherRootLayoutTipBefore.setOnClickListener {
            Log.e("TIP BEFORE WORKING","NO TIP CLICKED")

            activeTipsListAdapter?.clearSelectedItem()
            showTipKeypad(dashBoardCategoryViewModel.totalPrice)
        }

        binding.noTipRootLayoutTipBefore.setOnClickListener {


            Log.e("TIP BEFORE WORKING","NO TIP CLICKED")

            activeTipsListAdapter?.clearSelectedItem()
            dashBoardCategoryViewModel.apply {
                totalTipAmount = 0.0
                customerGivenTipBefore.value = true
            }

            binding.apply {
                mainCartLayout.visible()
                askForTipBeforeLayout.gone()
            }

           // shouldHighlightNoTipLayoutBefore(true)
          //  showThankYou(mWholeTotalPrice)
        }

        setupActiveTipsList(_tipListViewModel)



        dashBoardCategoryViewModel.splitChanged.observe(lifecycleOwner) {

            var wholeAmount = 0.0

            wholeAmount = try {
                prefProvider.getValue(
                    Constants.WHOLE_AMOUNT,
                    "0.0"
                ).toDouble()

        //                val finalAmount = MethodUtils.calculateCashDiscount(wholeAmount,prefProvider,context)
        //
        //                Log.e("FINAL AMOUNT","FINAL AMOUNT $finalAmount")
        //
        //                wholeAmount += finalAmount
            }catch (e:Exception) {
                dashBoardCategoryViewModel.totalPrice
            }

            Log.e("Total Tip Check ","SPLIT COUNT $it AND WHOLE AMOUNT = $wholeAmount")



            observeActiveTipsList(/*dashBoardCategoryViewModel.totalPrice*/ wholeAmount / it)
        }

    }



    public fun closeSecondaryDisplay() {
        System.exit(0)
    }

    private fun observePasscodeScreen() {
        dashBoardCategoryViewModel.passcodeScreenActive.observe(lifecycleOwner, object:Observer<Boolean>{
            override fun onChanged(t: Boolean?) {
                with(binding){
                t?.let {
                    if (it) {
                        /*Passcode screen is active, show the splash screen with logo*/
                        onLogOutOrClockOut(true)
                    } else {
                        /*Passcode screen is inactive, show the splash screen with sign up button*/
                        onLogOutOrClockOut(false)
                    }
                }
            }
            }
        })
    }

    /*-------------Customer Loyalty---------------*/
    private fun initViews() {

        if (prefProvider.getValueboolean(Constants.IS_PAYMENT_SCREEN,false)){
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                binding.btnSignUpOrCheckInMain.apply {
                    gone()
                }
            }

        }else{
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                binding.btnSignUpOrCheckInMain.apply {
                    visible()
                }
            }
        }
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (dashBoardCategoryViewModel.allSplit().isEmpty()){
                withContext(Dispatchers.Main){
                    binding.apply {
                        /*Commented for now because we don't need to show the signIn or change mobile button on Checkout Screen*/
//                        btnSignUpOrCheckInMain.visible()
                    }
                }
            }else{
                withContext(Dispatchers.Main) {
                    binding.apply {
                        btnSignUpOrCheckInMain.gone()
                    }
                }
            }
        }

        with(binding) {

            if (prefProvider.getValue(
                Constants.VENUE_LOGO,
                ""
            ).isNotEmpty()){
                imgBusiness?.let {
                    val decodedString: ByteArray = Base64.decode(prefProvider.getValue(
                        Constants.VENUE_LOGO,
                        ""
                    ), Base64.DEFAULT)
                    val decodedByte: Bitmap =
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    var requestOptions = RequestOptions()
                    requestOptions = requestOptions.transforms(CenterCrop(), RoundedCorners(100))
                    Glide.with(context).load(decodedByte)
                        .apply(requestOptions).into(it)
                    it.visible()
                }

            }else{
                imgBusiness?.gone()
            }
            if (prefProvider.getValue(Constants.CUSTOMER_NAME, "").isNotEmpty()) {
                btnSignUpOrCheckIn?.text = resources.getString(R.string.change_mobile_number)
                btnSignUpOrCheckInMain?.text = resources.getString(R.string.change_mobile_number)
//               Added below gone() statement just for initial phase
                btnSignUpOrCheckInMain.gone()
                btnSignUpOrCheckIn.gone()
                tvMessage?.text = "Customer added successfully"
            }else{
                btnSignUpOrCheckIn?.text = resources.getString(R.string.sign_up_or_check_in)
                btnSignUpOrCheckInMain?.text = resources.getString(R.string.sign_up_or_check_in)
                btnSignUpOrCheckInMain.visible()
                btnSignUpOrCheckIn.visible()
            }

            btnSignUpOrCheckIn?.setOnSingleClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber.text?.clear()
                    tvErrorMessage?.text = ""
                    splashLayout.gone()
                    keypadLayout?.visible()
                }
            })

            btnSignUpOrCheckInMain?.setOnSingleClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    binding.apply {
                        tvPhoneNumber.text?.clear()
                        splashLayout.gone()
                        keypadLayout?.visible()
                    }
                }
            })

            tvCancel?.setOnSingleClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    if (cartAdapter.cartList.isNotEmpty()){
                        mainCartLayout.visible()
                        splashLayout.gone()
                        keypadLayout?.gone()
                        tvErrorMessage?.text = ""

                    }else{
                        splashLayout.visible()
                        keypadLayout?.gone()
                    }
                }

            })

            /*---------------------KEYPAD----------------------*/
            btnOne?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("1")
                }
            })

            btnTwo?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("2")
                }
            })
            btnThree?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("3")
                }
            })
            btnFour?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("4")
                }
            })
            btnFive?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("5")
                }
            })
            btnSix?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("6")
                }
            })
            btnSeven?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("7")
                }
            })
            btnEight?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("8")
                }
            })
            btnNine?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("9")
                }
            })

            btnZero?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.append("0")
                }
            })

            btnClear?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.setText("")
                    tvErrorMessage?.text = ""
                }
            })

            btnBackSpace?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    tvPhoneNumber?.setText(MethodUtils.removeChars(tvPhoneNumber?.text.toString(), 1))
                    tvErrorMessage?.text = ""
                }
            })

            btnBackSpace?.setOnLongClickListener(object:View.OnLongClickListener{
                override fun onLongClick(p0: View?): Boolean {
                    tvPhoneNumber?.setText("")
                    return true
                }
            })
            /*---------------------KEYPAD----------------------*/

            tvDone?.setOnSingleClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
//                    Search on local,

                    //  1. if customer present then add the customer.
                    //  2. if customer not present then create the customer

                    Log.v("4732", "Done Clicked")
                    var mobileNumber = tvPhoneNumber?.text.toString().trim().replace(Regex("[^0-9]"), "")

                    tvPhoneNumber.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            // Check if the length of the number is 10 digits
                            val mobileNumber = s?.toString()?.trim()?.replace(Regex("[^0-9]"), "")
                            if (mobileNumber?.length == 10) {
                                // If the length is 10, remove the error message
                                tvErrorMessage?.text = ""
                            }
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            // No need to implement this for this use case
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            // No need to implement this for this use case
                        }
                    })

                    if (mobileNumber.length == 10) {
                        searchUserFromMobileNumber(mobileNumber)
                        tvPhoneNumber?.text?.clear()
                        tvErrorMessage?.text = ""
                    } else {
                        Toast.makeText(context,"Enter Valid Mobile Number", Toast.LENGTH_SHORT).show()
                        tvErrorMessage?.text = "Invalid Number"
                    }
//                    [{"id":2,"phone_number":"5555575575"}]
                }
            })


            dashBoardCategoryViewModel.changeCustDispSignInButtonTitle.observe(lifecycleOwner,object :Observer<String>{
                override fun onChanged(t: String?) {
                    t?.let {
                        if (it.isNotEmpty()){
                            btnSignUpOrCheckInMain.text=it
                        }
                    }
                }
            })

            dashBoardCategoryViewModel.removedCustomerFromManualSaleObs.observe(lifecycleOwner,object:Observer<Boolean>{
                override fun onChanged(t: Boolean?) {
                    t?.let {
                        if (it){
                            dismiss()
                            dismiss()
                            binding.apply {
                                txtCustomerName.text=""
                                tvLoyaltyBalance.text=""
                                tvMessage.text=resources.getString(R.string.loyalty_message)
                            }
//                            if (!makeOneTimeReload) {
                            dashBoardCategoryViewModel.removedCustomerFromManualSaleObs.value=false

                            dashBoardCategoryViewModel.reloadCustomerDisplay(it)
//                            }
                        }
                    }
                }
            })
        }
    }

    private fun searchUserFromMobileNumber(phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var found: List<TbPhones>? = null
            var customersListFromDb: List<TbCustomer?>? =
                null
            Log.v("4732", "Inside searchUserFromMobileNumber()")

            customersListFromDb =
                dashBoardCategoryViewModel.fetchCustomerFromPhoneNumber(phoneNumber)
            if (customersListFromDb?.isNotEmpty() ?: false) {
                customersListFromDb?.get(0)?.let {
                    found = it.phones.filter { it.phone_number.contains(phoneNumber) }
                }
                if (found?.isNotEmpty() ?: false) {
                    Log.v("4732", "addCustomer()_1")
                    addCustomer(customersListFromDb!!.get(0)!!)
                } else {
                    Log.v("4732", "createCustomer()_1")
                    createCustomer(phoneNumber)
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.keypadLayout?.gone()
                        binding.splashLayout?.gone()
                        binding.mainCartLayout?.visible()
                    }
                }
            } else {
                Log.v("4732", "addCustomer()_2")
                createCustomer(phoneNumber)
                CoroutineScope(Dispatchers.Main).launch {
                    binding.keypadLayout?.gone()
                    binding.splashLayout?.gone()
                    binding.mainCartLayout?.visible()
                }
            }
            Log.d("CustomersList:: ", Gson().toJson(customersListFromDb))

        }
    }


    private fun createCustomer(phoneNumber: String) {
      /*  binding.tvMessage?.post {
            binding.tvMessage?.text="Loading..."
        }*/

        CoroutineScope(Dispatchers.Main).launch {
            binding.apply {
                keypadLayout?.gone()
                splashLayout?.gone()
                mainCartLayout?.visible()
            }

            binding.root.invalidate() // or
//            this@CustomDisplay.window?.decorView?.invalidate()
        }

        Log.d("C_Loyalty: ", "createCustomer: Loading... Set")
        EventBus.getDefault().post(CreateCustomerEvent(true, phoneNumber))
    }

    public fun addCustomer(customer: TbCustomer) {
        prefProvider.setValue(
            Constants.CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )
        dashBoardCategoryViewModel.selectedCustomer=customer
        prefProvider.setValue(
            Constants.RECEIPT_CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )
        prefProvider.setValue(
            Constants.PREF_CUSTOMER,
            Gson().toJson(customer)
        )
        prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }
        /*This will click on the order type dynamically, only the variable name is clickOnTakeOut()*/
        dashBoardCategoryViewModel.clickOnTakeOut()
        Log.v("4732", "TakeOut Clicked on Main Screen")
//        if (dashBoardCategoryViewModel.currentCartItems.isNotEmpty()){
//            dashBoardCategoryViewModel.callUpdateCartFooter(true)
//        }
        dashBoardCategoryViewModel.changeCustomerDispSignButtonTitle(resources.getString(R.string.change_mobile_number))
        EventBus.getDefault()
            .post(SyncCustomerEvent(true, customer.first_name + " " + customer.last_name))
//        displayCustomer()

        CoroutineScope(Dispatchers.Main).launch {
            displayCustomer()
            binding.apply {
                tvMessage?.text="Customer added successfully"
                btnSignUpOrCheckInMain.text=resources.getString(R.string.change_mobile_number)

                /*Gone is temporary, we will remove this in future*/
                btnSignUpOrCheckInMain.gone()
                btnSignUpOrCheckIn.gone()

                btnSignUpOrCheckIn.text=resources.getString(R.string.change_mobile_number)
                txtCustomerName.visible()
                txtCustomerName.apply { text = customer.first_name + " " + customer.last_name }
                keypadLayout?.gone()
                splashLayout?.gone()
                mainCartLayout?.visible()
            }
//            Log.v("4732", "Customer Name is shown")
//
////            if (savedInstanceStateBackup!=null){
////                onCreate(savedInstanceStateBackup)
            dashBoardCategoryViewModel.refreshCartFragment()
            }
////            onCreate(null)
//

//        }

//        onCreate(null)


    }

    private fun getDetails() {
        dashBoardCategoryViewModel.getBusinessData.observe(lifecycleOwner) { it ->
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.tvBusinessName?.text = resource.data?.business_name
                    }
                    Status.ERROR -> {
                    }
                    Status.LOADING -> {
                    }
                }
            }
        }
    }

    private fun getLoyaltyPointListObserver() {
        dashBoardCategoryViewModel.loyaltyPoints.observe(lifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        try {
                            ProgressUtils.dismissProgressDialog()
                            run breaking@{
                                resource.data?.forEach {
                                    if (it.isEnable){
                                        binding.tvRewards?.text = getPreparedRewardStatement(it)
                                        return@breaking
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
                    }
                    Status.LOADING -> {
                    }
                }
            }
        })
    }

    private fun getPreparedRewardStatement(it: LoyaltyProgramsModel): CharSequence {
        var point = ""
        if (it.rewardPoint > 1) {
            point = "points"
        } else {
            point = "point"
        }

        return "Redeem $${generalizeAmount(it.amount.toString())} on every ${generalizeAmount(it.rewardPoint.toString())} ${point}."
    }


    /*-------------Customer Loyalty---------------*/


    private fun observeCashCardChange() {
        dashBoardCategoryViewModel.customerCashAmount.observe(lifecycleOwner,
            object : Observer<String> {
                override fun onChanged(t: String?) {
                    Log.d("CustomerDisp::", "Obsever Called")
                    binding.txtTotalCash?.text = t
                }
            })

        dashBoardCategoryViewModel.customerCardAmount.observe(lifecycleOwner,
            object : Observer<String> {
                override fun onChanged(t: String?) {
                    Log.d("CustomerDisp::", "Obsever Called")
                    binding.txtTotalCard?.text = t
                }
            })
    }

    private fun initDiscountLiveData() {
        dashBoardCategoryViewModel.latestDiscount.observe(lifecycleOwner,
            object : Observer<Double> {
                override fun onChanged(t: Double?) {
                    lifecycleOwner.lifecycleScope.launch {
                        binding.txtDiscountCard?.text = "-$${String.format("%.2f", t)}"
                        binding.txtDiscountCash?.text = "-$${String.format("%.2f", t)}"
                    }
                }

            })
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
            dashBoardCategoryViewModel.getManualSaleCartItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(lifecycleOwner) {
                it?.let {
                    updateCustomerDisplay(it)
                }
            }
        } else {

            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                binding.linearBottomNew?.gone()
                dashBoardCategoryViewModel.getAllDineInCartItems(DINE_IN).asLiveData()
                    .observe(lifecycleOwner) {
                        Log.d("WINZO", "onDisplayChanged: ${it.size}")
                        it?.let {
                            if (it.isNotEmpty())
                                updateCustomerDisplay(it)
                        }
                    }
            } else {
                dashBoardCategoryViewModel.getAllCartItems(
                    prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                ).asLiveData().observe(lifecycleOwner) {
                    Log.d("WINZO", "onDisplayChanged: ${it.size}")
                    it?.let {
                        if (it.isNotEmpty())
                            updateCustomerDisplay(it)
                    }
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

    fun updateCustomerDisplay(cartList: List<TbCartItem>) {
        Log.d(TAG, "updateCustomerDisplay: OUTSIDE")
        showCashCreditPrice = prefProvider.getValueboolean(
            Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY,
            false
        )
        if (this::binding.isInitialized) {
            Log.d(TAG, "updateCustomerDisplay: INSIDE")
            Log.d(TAG, "updateCustomerDisplay: cartList = ${Gson().toJson(cartList)}")
            if (cartList.isNotEmpty()) {

                if(!isTipBeforeScreen)
                    binding.mainCartLayout.visibility = View.VISIBLE
                binding.splashLayout.visibility = View.GONE

                val isDineIn = prefProvider.getValue(
                    Constants.ORDER_TYPE, Constants.TAKEOUT
                ) == Constants.DINE_IN

                if (isDineIn) {
                    if (dashBoardCategoryViewModel.cartModel?.dineInList?.isNotEmpty() == true) {
                        val dineInList = dashBoardCategoryViewModel.cartModel?.dineInList

                        dineInCartAdapter.setList(
                            dineInList?.toCollection(arrayListOf()) ?: arrayListOf(),
                            dashBoardCategoryViewModel.currentCartItems
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
                    cartList.toCollection(arrayListOf()).let { it1 ->
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
//                    onDisplayChanged()
                    /* if (refreshCount<=5) {
                         onDisplayChanged()
                     }else{
                         refreshCount=1
                     }*/
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
                    binding.txtServiceChargeCard?.text =
                        MethodUtils.roundOffAmount(totalServiceCharge)
                    Log.v("CustomerScreen Amount_2:", totalPrice.toString())

                    binding.txtTotalCash?.text = getCashDiscountedPrice(totalPrice)
                    binding.txtTotalCard?.text = MethodUtils.roundOffAmount(totalPrice)

                } else {
                    binding.txtSubTotalCash?.text = MethodUtils.roundOffAmount(subTotalPrice)
                    binding.txtSubTotalCard?.text = getSurchargedPrice(subTotalPrice)

                    binding.txtTaxCash?.text = MethodUtils.roundOffAmount(totalTax)
                    binding.txtTaxCard?.text = getSurchargedPrice(totalTax)

                    binding.txtServiceChargeCash?.text =
                        MethodUtils.roundOffAmount(totalServiceCharge)
                    binding.txtServiceChargeCard?.text = getSurchargedPrice(totalServiceCharge)
//This is being called again, and hence the old value is getting reset
//                    Log.v("CustomerScreen Amount_1:", totalPrice.toString())
//                    Log.v("CustomerScreen Amount_BACKUP_1:", mWholeTotalPrice.toString())
//                    Log.v("CustomerScreen Amount_BACKUP_2:", dashBoardCategoryViewModel.wholetotalPrice.toString())
                    if (totalPrice < dashBoardCategoryViewModel.wholetotalPrice) {

                        if (dashBoardCategoryViewModel.customerCashAmount.value?.isNotEmpty()
                                ?: false
                        ) {
                            binding.txtTotalCash?.text =
                                dashBoardCategoryViewModel.customerCashAmount.value
                        } else {
                            binding.txtTotalCash?.text =
                                MethodUtils.roundOffAmount(dashBoardCategoryViewModel.wholetotalPrice)
                        }

                        if (dashBoardCategoryViewModel.customerCardAmount.value?.isNotEmpty()
                                ?: false
                        ) {
                            binding.txtTotalCard?.text =
                                dashBoardCategoryViewModel.customerCardAmount.value
                        } else {
                            binding.txtTotalCard?.text =
                                getSurchargedPrice(dashBoardCategoryViewModel.wholetotalPrice)
                        }

                        Log.v("CustomerScreen:", "1")
                    } else {
                        /* binding.txtTotalCash?.text = MethodUtils.roundOffAmount(totalPrice)
                         binding.txtTotalCard?.text = getSurchargedPrice(totalPrice)*/

                        if (dashBoardCategoryViewModel.customerCashAmount.value?.isNotEmpty()
                                ?: false
                        ) {
                            binding.txtTotalCash?.text =
                                dashBoardCategoryViewModel.customerCashAmount.value
                        } else {
                            binding.txtTotalCash?.text = MethodUtils.roundOffAmount(totalPrice)
                        }


                        if (dashBoardCategoryViewModel.customerCardAmount.value?.isNotEmpty()
                                ?: false
                        ) {
                            binding.txtTotalCard?.text =
                                dashBoardCategoryViewModel.customerCardAmount.value
                        } else {
                            binding.txtTotalCard?.text = getSurchargedPrice(totalPrice)
                        }



                        Log.v("CustomerScreen:", "0")

                    }

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


                if (MethodUtils.isEnableCashDiscount(context) && prefProvider.getValue(
                        ORDER_TYPE,
                        TAKEOUT
                    ) != Constants.GIFT_CARD
                ) {

                    if (prefProvider.getValue(
                            Constants.OPTION_TYPE,
                            "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        if (isInsideCheckout) {
                            binding.txtOrderTotal?.text = getCashDiscountedPrice(totalPrice)
                        } else {
                            binding.txtOrderTotal?.text = MethodUtils.roundOffAmount(totalPrice)
                        }

                        binding.txtCashDiscountSurchargeCard?.text =
                            "-" + MethodUtils.roundOffAmount(cashdiscountAmount)
                    } else {
                        if (isInsideCheckout) {
                            binding.txtOrderTotal?.text = getSurchargedPrice(totalPrice)
                        } else {
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
        Log.v("4732", "inside displayCustomer()")

        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        if (name.isNotEmpty()) {
            Log.v("4732", "inside displayCustomer()_1")
//            CoroutineScope(Dispatchers.Main).launch {
            binding.apply {
                txtCustomerName.post { txtCustomerName.visible() }
                txtCustomerName.post { txtCustomerName.text=name }
                txtLoyaltyPointsLabel.post {  txtLoyaltyPointsLabel.visible() }
            }

//                show()
//                binding.root.invalidate() // or
//                this@CustomDisplay.window?.decorView?.invalidate()
//                show()
                Log.v("4732", "inside displayCustomer()_2 -> ${name}")
//            }

            if (dashBoardCategoryViewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    Log.v("4732", "inside displayCustomer()_3")
                    binding.tvLoyaltyBalance.visible()
                    binding.tvLoyaltyPoints.visible()
                    binding.tvLoyaltyBalance.text =
                        "${context.resources.getString(R.string.applied_loyalty_balance)}: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyAmount}"
                    binding.tvLoyaltyPoints.text =
                        "${context.resources.getString(R.string.applied_loyalty_points)}: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
                }
                /*dashBoardCategoryViewModel.reloadCustomerDisplay()*/
//                if (!makeOneTimeReload) {
                    dashBoardCategoryViewModel.reloadCustomerDisplay(false)
//                }
            } else {
                /*binding.tvLoyaltyBalance.invisible()
                binding.tvLoyaltyPoints.invisible()
                */
//                Handler(Looper.getMainLooper()).post(Runnable {

//              CoroutineScope(Dispatchers.Main).launch {
//                    binding.apply {
                binding.tvMessage.post { binding.tvMessage.text="Customer added successfully" }
                binding.btnSignUpOrCheckInMain.post { binding.btnSignUpOrCheckInMain.text=resources.getString(R.string.change_mobile_number)
                    /*Gone is temprary*/
                binding.btnSignUpOrCheckInMain.gone() }
                binding.btnSignUpOrCheckIn.post { binding.btnSignUpOrCheckIn.text=resources.getString(R.string.change_mobile_number)
                    /*Gone is temprary*/
                    binding.btnSignUpOrCheckIn.gone()}
                binding.txtCustomerName.post { binding.txtCustomerName.visibility=View.VISIBLE }
                binding.txtCustomerName.post { binding.txtCustomerName.text=name }
//                        txtCustomerName.invalidate()
//                        txtCustomerName.postInvalidate()
                binding.keypadLayout.post { binding.keypadLayout.gone() }
                binding.splashLayout.post { binding.splashLayout.gone() }
                binding.mainCartLayout.post { binding.mainCartLayout.visible() }

                /*dashBoardCategoryViewModel.reloadCustomerDisplay()*/
//                if (!makeOneTimeReload) {
                    dashBoardCategoryViewModel.reloadCustomerDisplay(false)
//                }

//                        binding.root.invalidate() // or
//                        show()
//                        this@CustomDisplay.window?.decorView?.invalidate()
//                    }
//                }

//                })
//                show()

//                binding.root.invalidate() // or
//                this@CustomDisplay.window?.decorView?.invalidate()
                /*CoroutineScope(Dispatchers.Main).launch {
                    Log.v("4732", "inside displayCustomer()_removed_1 -> -> ${name}")

                    *//*-----------Customer Loyalty------------*//*
                    binding.tvLoyaltyBalance.gone()
                    binding.tvLoyaltyPoints.gone()
                    *//*binding.txtCustomerName.post {
                        this@CustomDisplay.name=name
                    }*//*
                    *//*-----------Customer Loyalty------------*//*
                }*/
            }
//            binding.root.invalidate() // or
//            this@CustomDisplay.window?.decorView?.invalidate()

            CoroutineScope(Dispatchers.Main).launch {
                binding.txtLoyaltyPointsLabel.text =
                    "Loyalty Balance: ${
                        if (dashBoardCategoryViewModel.redeemLoyaltyInfo.needToApplyLoyalty) {
                            dashBoardCategoryViewModel.redeemLoyaltyInfo.remainingLoyaltyPoints
                        } else {
                            dashBoardCategoryViewModel.redeemLoyaltyInfo.availablePoints
                        }
                    }"
                binding.txtCustomerName.text = name
                binding.txtCustomerName.post { binding.txtCustomerName.text=name }
            }


        } else {
         /*   binding.txtLoyaltyPointsLabel.invisible()
            binding.txtCustomerName.invisible()
            binding.tvLoyaltyBalance.invisible()
            binding.tvLoyaltyPoints.invisible()*/
            Log.v("4732", "inside displayCustomer() removed loyalty")
            /*----------Customer Loyalty--------------*/
            binding.txtLoyaltyPointsLabel.gone()
            binding.txtCustomerName.gone()
            binding.tvLoyaltyBalance.gone()
            binding.tvLoyaltyPoints.gone()
            /*----------Customer Loyalty--------------*/
//            binding.root.invalidate() // or
//            this@CustomDisplay.window?.decorView?.invalidate()
//            show()
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

    override fun onItemClickListener(view: View?, data: TbCartItem, position: Int) {}
    override fun onCartItemClickListener(view: View?, data: TbCartItem, position: Int) {
        TODO("Not yet implemented")
    }

    override fun onHeaderSelected(position: Int) {}

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbCartItem) {}

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {}

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbCartItem) {}
    override fun onRemoveGuest(position: Int) {}

    fun showThankYou(paidAmount: Double) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()
            askForTipLayout.gone()
            addTipKeypadLayout.gone()
            progressLayout.gone()

            thankYouLayout.visible()
            try {
                if (dashBoardCategoryViewModel.selectedCustomer != null) {
                    txtEarnedLoyalty?.visible()
                    txtEarnedLoyalty?.post {
                        /*if (dashBoardCategoryViewModel.selectedCustomer?.final_reward.toString()
                                .toInt()!=0){
                            txtEarnedLoyalty?.setText(
                                "You have earned ${
                                    ((dashBoardCategoryViewModel.selectedCustomer?.final_reward.toString()
                                        .toInt()).minus(
                                            dashBoardCategoryViewModel.earnedLoyaltyPoints.value?.peekContent()
                                                .toString().toInt()
                                        )).toString()
                                } reward points"
                            )
                        }else{
                            txtEarnedLoyalty?.setText(
                                "You have earned ${
                                    (dashBoardCategoryViewModel.earnedLoyaltyPoints.value?.peekContent()
                                                .toString().toInt()).toString()
                                } reward points"
                            )
                        }*/

//                        if (dashBoardCategoryViewModel.selectedCustomer?.final_reward.toString().toInt()!=0){
                        try {
                            txtEarnedLoyalty?.setText(
                                if (dashBoardCategoryViewModel.earnedLoyaltyPoints.value?.peekContent()
                                        .toString().toInt() == 1
                                ) {
                                    "Your balance loyalty point is ${
                                        (dashBoardCategoryViewModel.earnedLoyaltyPoints.value?.peekContent()
                                            .toString().toInt()).toString()
                                    }."
                                } else {
                                    "Your balance loyalty points are ${
                                        (dashBoardCategoryViewModel.earnedLoyaltyPoints.value?.peekContent()
                                            .toString().toInt()).toString()
                                    }."
                                }

                            )
//                        }
                        }catch (e:Exception){

                        }
                    }
                }
            }catch (e:Exception){

            }

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
            val response = apiService.getTimeDetails(prefProvider.getValueInt(TERMINAL_ID, -1))

            binding.currentTime.text = response.data.time
            binding.currentDate.text = response.removeWhiteSpaces()
        }.runCatching {
            Log.d(
                "CustomDisplay",
                "onLogOutOrClockOutWithApiService: Some Exception"
            )
        }
    }

    fun onLogOutOrClockOut(value: Boolean) {
        binding.apply {
            if (value) {
                mainCartLayout.gone()
                thankYouLayout.gone()
//            splashLayout.visible()
                imgPaysSplash?.visible()
                splashLoyalty?.gone()
            }else{
                mainCartLayout.gone()
                thankYouLayout.gone()
//            splashLayout.visible()
                imgPaysSplash?.gone()
                splashLoyalty?.visible()
            }
        }
    }

    fun showThankyouLayout() {
        binding.apply {
            mainCartLayout.gone()
            thankYouLayout.gone()
//            splashLayout.visible()
            imgPaysSplash?.gone()
            splashLoyalty?.gone()
            thankYouLayout.visible()

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
        } catch (e: Exception) {
            Log.d("getCustomerList", "exception : ${e.toString()}")
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
            if(!isTipBeforeScreen)
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
                                    val item = TbCartItem()
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


        binding.apply {
            rvActiveTipsListTipBefore.apply {
                layoutManager = GridLayoutManager(context, 4)
                adapter = activeTipsListAdapter
            }
        }
        tipsListViewModel = tipListViewModel
    }

    private fun observeActiveTipsList(wholeTotalPrice: Double) {
        tipsListViewModel.getTipActiveList.observe(lifecycleOwner) {

            try {
                Log.d("C_Disp_2::", wholeTotalPrice.toString())

                if (it.data?.isNotEmpty() == true) {

                    activeTipsListAdapter?.clearAll()

                    it.data.forEach { data ->
                        data.isChecked = false
                    }
                    binding.rvActiveTipsList.layoutManager =
                        GridLayoutManager(context, it.data.size)

                    binding.rvActiveTipsListTipBefore.layoutManager =
                        GridLayoutManager(context, it.data.size)

//                activeTipsListAdapter?.setList(it.data, wholeTotalPrice)
                    activeTipsListAdapter?.setList(it.data, wholeTotalPrice)
                    activeTipsListAdapter?.setListner(this)
                    lifecycleOwner.lifecycleScope.launch {
                        //delay(5000)
                        //binding.rvActiveTipsList.smoothScrollToPosition(tipsList.size - 1)
                    }

                    Log.d("Payment_TYPE:: ", dashBoardCategoryViewModel.paymentTypeForTip)
                    if (dashBoardCategoryViewModel.paymentTypeForTip.equals(
                            "cash",
                            ignoreCase = true
                        )
                    ) {
                        activeTipsListAdapter?.setList(
                            it.data,
                            wholeTotalPrice
                                .toDouble()
                        )
                    } else if (dashBoardCategoryViewModel.paymentTypeForTip.equals(
                            "card",
                            ignoreCase = true
                        )
                    ) {
                        activeTipsListAdapter?.setList(
                            it.data,
                            wholeTotalPrice
                                .toDouble()
                        )
                    } else {
                        activeTipsListAdapter?.setList(it.data, wholeTotalPrice)
                    }
                    activeTipsListAdapter?.setListner(this)
                    lifecycleOwner.lifecycleScope.launch {
                        //delay(5000)
                        //binding.rvActiveTipsList.smoothScrollToPosition(tipsList.size - 1)
                        Log.d("Payment_TYPE:: ", dashBoardCategoryViewModel.paymentTypeForTip)
                        if (dashBoardCategoryViewModel.paymentTypeForTip.equals(
                                "cash",
                                ignoreCase = true
                            )
                        ) {
                            activeTipsListAdapter?.setList(
                                it.data,
                                wholeTotalPrice
                                    .toDouble()
                            )
                        } else if (dashBoardCategoryViewModel.paymentTypeForTip.equals(
                                "card",
                                ignoreCase = true
                            )
                        ) {
                            activeTipsListAdapter?.setList(
                                it.data,
                                wholeTotalPrice
                                    .toDouble()
                            )
                        } else {
                            activeTipsListAdapter?.setList(it.data, wholeTotalPrice)
                        }
                        //activeTipsListAdapter?.setListner(this)
                        lifecycleOwner.lifecycleScope.launch {
                            //delay(5000)
                            //binding.rvActiveTipsList.smoothScrollToPosition(tipsList.size - 1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "CRASH CUSTOMER DISPLAY - GET TIP LIST ACTIVE  ",
                    "observeActiveTipsList " + e.message
                )
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
            askForTipBeforeLayout.gone()
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

            txtContinue.setOnSingleClickListener {

                //dashBoardCategoryViewModel.tipButtonOnCustomerDisplayClicked.value=true

                txtContinue.isEnabled = false


                tippedAmount =
                    edtAmount.text.toString().replace("$", "").trim().toDouble()

                if(prefProvider.getValueboolean(IS_PAYMENT_SCREEN,false)) {
                    dashBoardCategoryViewModel.apply {
                        totalTipAmount = tippedAmount
                        customerGivenTipBefore.value = true
                    }

                    addTipKeypadLayout.gone()
                    askForTipBeforeLayout.gone()

                } else {

                    if (mIsCardPayment) {
                        if (/*!mIsSignatureRequired*/ true) {
//                       /* showWouldYouLikeToAddTipScreen(
//                            tipsListViewModel,
//                            mTransactionViewModel,
//                            mWholeTotalPrice,
//                            mOrderID,
//                            mIsCardPayment,
//                            mPaymentViewModel,
//                            magtekRequestUtils,
//                            apiModule1,
//                            true
//                        )
//                    } else {*/
//                        magtekCall(wholeTotalPrice)

                        /*if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                            magtekCall(wholeTotalPrice)
                        } else {
                            adjustPaxTips()
                        }*/

                        when(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"")){
                            Constants.PAX->{
                                if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    adjustPaxTips()
                                }else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    AlertUtils.showCustomAlert(
                                        context,
                                        "Please connect to PAX device"
                                    )
                                }
                            }

                            Constants.VALOR, Constants.VELOR->{
                                adjustValorTips()
                            }

                            Constants.DEJAVOO->{
                                adjustDejavooTips()
                            }

                            else->{
                                if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                                    magtekCall(wholeTotalPrice)
                                }else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    AlertUtils.showCustomAlert(
                                        context,
                                        "Please connect a payment device"
                                    )
                                }
                            }
                        }


                       /*
                        if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                            magtekCall(wholeTotalPrice)
                        } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            )
                        ) {
                            adjustPaxTips()
                        } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            )
                        ) {
                            AlertUtils.showCustomAlert(
                                context,
                                "Please connect to PAX device"
                            )
                        }*/
                    }
                } else {
                    callUpdateTip()
                }
            }

                txtContinue.isEnabled = true
            }

        }
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                }
            })
    }

    private fun adjustPaxTips() {
        GlobalScope.launch {
            dashBoardCategoryViewModel.processingTipForCard.postValue(true)

            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    context!!,
                    Constants.FILE_PATH + SettingINI.FILENAME
                )
            )
            val tip_amt = (tippedAmount * 100).toInt()
            Log.d("Amt: ", "tip $tip_amt RefNo ${mPaymentViewModel.paxReferenceNo}")

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
                            callUpdateTip()
                        }
                    }
                } else {
                    dashBoardCategoryViewModel.setTipErrorObservable(resultTxt)
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        ProgressUtils.dismissProgressDialog()
                        /*AlertUtils.showCustomAlertWithListenerWithOK(context, resultTxt, object :
                            DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                try {
                                    p0?.dismiss()
                                } catch (e: Exception) {
                                }
                            }
                        })*/
                        Log.d("resultCode not 000000:", "param $resultCode $resultTxt")
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
//                    ProgressUtils.dismissProgressDialog()
                    if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT") {
                        Log.d("Error: ", "Please check your internet connection")
//                        Toast.makeText(requireContext(), "Please check your internet connection", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("Error: ", "getMerchantDetails Failed ${result.Code} ${result.Msg}")
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
        if(!isTipBeforeScreen)
            binding.mainCartLayout.visibility = View.VISIBLE
        binding.splashLayout.visibility = View.GONE
        dineInPaymentDetails = model

    }

    fun setCustomerList(list: List<TbCustomer>) {
        allCustomerList.clear()
        allCustomerList.addAll(list)
    }

    var cashLogServerOrderIdMain: Int = 0
    var cashLogPaymentIdMain: Int = 0

    fun showWouldYouLikeToAddTipScreen(
        tipListViewModel: TipListViewModel,
        transactionViewModel: TransactionViewModel? = null,
        wholeTotalPrice: Double,
        orderId: Int, //This is payment id but the variable name is order id
        isCardPayment: Boolean,
        paymentViewModel: PaymentViewModel,
        magRequestUtils: MagtekRequestUtils,
        apiModule1: ApiModule1,
        fromKeypad: Boolean = false,
        totalPrice: Double,
        cashLogServerOrderId: Int,
        cashLogPaymentId: Int
    ) {
        cashLogServerOrderIdMain=cashLogServerOrderId
        cashLogPaymentIdMain=cashLogPaymentId

        mTipListViewModel = tipListViewModel
        mOrderID = orderId
        mIsCardPayment = isCardPayment
        mIsSignatureRequired =
            prefProvider.getValueboolean(CUSTOMER_SIGN_REQUIRED_ON_CD, false)

        if(!isTipBeforeScreen) {
            mTransactionViewModel = transactionViewModel!!
            mPaymentViewModel = paymentViewModel!!
            magtekRequestUtils = magRequestUtils!!
            magensaResponse = mPaymentViewModel.magensaResponse ?: ""
        }



        this.apiModule1 = apiModule1
        mWholeTotalPrice = wholeTotalPrice

        binding.apply {
            askForTipLayout.visible()
            setupActiveTipsList(mTipListViewModel)
//            observeActiveTipsList(wholeTotalPrice)
            Log.d("C_Disp_3::", mPaymentViewModel.tipOnAmount.toString())
            if (dashBoardCategoryViewModel.getSplitCount() == 1) {
                observeActiveTipsList(/*mPaymentViewModel.tipOnAmount*/totalPrice / dashBoardCategoryViewModel.getSplitCount())
            } else {
            //    observeActiveTipsList(mPaymentViewModel.tipOnAmount / dashBoardCategoryViewModel.getSplitCount())
                observeActiveTipsList(/*mPaymentViewModel.tipOnAmount*/totalPrice / dashBoardCategoryViewModel.getSplitCount())
            }

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

            clearSignLayout.setOnClickListener {
                binding.signaturePad.clear()
            }

            otherRootLayout.setOnClickListener {
                activeTipsListAdapter?.clearSelectedItem()
                showTipKeypad(wholeTotalPrice)
            }

            noTipRootLayout.setOnClickListener {

                /**
                 * Customer Clicked no Tip , so it will reflect tip as $0.0
                 */
                dashBoardCategoryViewModel.apply {
                    totalTipAmount = 0.0
                    customerGivenTip.value = true
                    employeeGivenTip = false
                }

                showThankYou(mWholeTotalPrice)
            }

            tvContinue.setOnSingleClickListener {

                signatureInBase64 = bitmapToBase64(signaturePad.signatureBitmap)

//                magtekCall(wholeTotalPrice)

                if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                    magtekCall(wholeTotalPrice)
                } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
                    adjustPaxTips()
                } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
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


    private fun shouldHighlightNoTipLayoutBefore(isHighlight: Boolean) {
        if (isHighlight) {
            activeTipsListAdapter?.clearSelectedItem()
            binding.noTipRootLayoutTipBefore.setBackgroundColor(Color.parseColor("#ff6000"))
            binding.txtNoTipLabelTipBefore.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.noTipRootLayoutTipBefore.setBackgroundColor(Color.parseColor("#363636"))
            binding.txtNoTipLabelTipBefore.setTextColor(Color.parseColor("#ff6000"))
        }

    }

    private fun shouldHighlightNoTipLayout(isHighlight: Boolean) {
        if (isHighlight) {
            binding.noTipRootLayout.setBackgroundColor(Color.parseColor("#ff6000"))
            binding.txtNoTipLabel.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.noTipRootLayout.setBackgroundColor(Color.parseColor("#363636"))
            binding.txtNoTipLabel.setTextColor(Color.parseColor("#ff6000"))
        }

    }

    private fun shouldHighlightOtherTipLayout(isHighlight: Boolean) {
        if (isHighlight) {
            binding.otherRootLayout.setBackgroundColor(Color.parseColor("#ff6000"))
            binding.txtOtherLabel.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.otherRootLayout.setBackgroundColor(Color.parseColor("#363636"))
            binding.txtOtherLabel.setTextColor(Color.parseColor("#ff6000"))
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
                        dashBoardCategoryViewModel.apply {
                            totalTipAmount = tippedAmount
                            customerGivenTip.value = true
                            employeeGivenTip = false
                        }
                        if (!mIsCardPayment){
                            /*TODO: Make cash_event call here with the same orderID*/
                            if (tippedAmount > 0) {
                                makeCashEventCallToUpdateTip(mOrderID, tippedAmount)
                            }
                        }
                        dashBoardCategoryViewModel.processingTipForCard.value = false
                        // dashBoardCategoryViewModel.tipButtonOnCustomerDisplayClicked.value=false

                        prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                        showThankYou(mWholeTotalPrice + tippedAmount)
                    } else {
                        showErrorLayout(it.message)
                        dashBoardCategoryViewModel.processingTipForCard.value = false
                    }
                }
            }

        }
    }

    private fun callUpdateTipValor(transactionViewModel: TransactionViewModel) {
        mTransactionViewModel = transactionViewModel
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
                        dashBoardCategoryViewModel.apply {
                            totalTipAmount = tippedAmount
                            customerGivenTip.value = true
                            employeeGivenTip = false
                        }

                        dashBoardCategoryViewModel.processingTipForCard.value = false
                        // dashBoardCategoryViewModel.tipButtonOnCustomerDisplayClicked.value=false

                        prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                        showThankYou(mWholeTotalPrice + tippedAmount)
                    } else {
                        showErrorLayout(it.message)
                        dashBoardCategoryViewModel.processingTipForCard.value = false
                    }
                }
            }

        }
    }

    private fun makeCashEventCallToUpdateTip(mOrderID: Int, tippedAmount: Double) {
        val cashLogRequest = CashLogRequest(
            tippedAmount,
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
            "in",
            cashLogServerOrderIdMain,
            cashLogPaymentIdMain,
            "Tip added to the order",
            prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
            null,
            null
        )
        dashBoardCategoryViewModel.makeCashInOutCallFromCustomerDisplay(cashLogRequest)

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
        // dashBoardCategoryViewModel.tipButtonOnCustomerDisplayClicked.value=true

        tipRate = model.rate
        tippedAmount = MethodUtils.percentageCalculation(wholeTotalPrice, model.rate)
        Log.d("selectedItem: ", "tip params $tipRate $tippedAmount")

        Log.e("TOTAL TIP Check", "TIP RATE")


        if (prefProvider.getValueboolean(Constants.IS_PAYMENT_SCREEN, false)) {
            /**
             * Used to show Given TIPS on OrderCompleted Fragment
             */
            dashBoardCategoryViewModel.apply {
                totalTipAmount = tippedAmount
                customerGivenTipBefore.value = true
                Log.d("selectedItem: ", "updating tip params $tipRate $tippedAmount")
            }

            shouldHighlightNoTipLayoutBefore(false)

            binding.askForTipBeforeLayout.gone()

        } else {
            if (mIsCardPayment /*&& !mIsSignatureRequired*/) {
                /**
                 * Used to show Given TIPS on OrderCompleted Fragment
                 */
                /*  dashBoardCategoryViewModel.apply {
              totalTipAmount = tippedAmount
              customerGivenTip.value = true
          }
  */
                try {
                    if (mIsCardPayment /*&& !mIsSignatureRequired*/) {
//            callUpdateTip()
                        when (prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE, "")) {
                            Constants.PAX -> {
                                if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    adjustPaxTips()
                                } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    AlertUtils.showCustomAlert(
                                        context,
                                        "Please connect to PAX device"
                                    )
                                }
                            }

                            Constants.VALOR, Constants.VELOR -> {
                                adjustValorTips()
                            }

                            Constants.DEJAVOO -> {
                                adjustDejavooTips()
                            }

                            else -> {
                                if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                                    magtekCall(wholeTotalPrice)
                                } else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                                        Constants.IS_PAX_CONNECTED,
                                        false
                                    )
                                ) {
                                    AlertUtils.showCustomAlert(
                                        context,
                                        "Please connect a payment device"
                                    )
                                }
                            }
                        }

                        /* if (prefProvider.getValue(Constants.VALOR_APP_ID, "").isNotEmpty()) {
                    adjustValorTips()
                }else if (prefProvider.getValue(
                        Constants.VALOR_APP_ID, ""
                    ).isEmpty()){
                    adjustDejavooTips()
                }
                else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
                    adjustPaxTips()
                }
                else if (mPaymentViewModel.paxReferenceNo.isNullOrEmpty()) {
                    magtekCall(wholeTotalPrice)
                }
                else if (!mPaymentViewModel.paxReferenceNo.isNullOrEmpty() && !prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
                    AlertUtils.showCustomAlert(
                        context,
                        "Please connect to PAX device"
                    )
                }*/
                    } else if (!mIsCardPayment) {
                        openCashDrawer()
                        callUpdateTip()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (!binding.signaturePad.isEmpty) {
                    enableConfirmButton()
                }
            }else {
                openCashDrawer()
                callUpdateTip()
            }
        }
    }

    private fun openCashDrawer(){
        if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
            EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
        } else {
            SunmiPrintHelper.getInstance().openCashBox()
        }
    }

    private fun adjustDejavooTips() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = PaymentGatewayFactory(
                ValorPaymentGateway(),
                DejavooPaymentGateway()
            ).create(gatewayType)

            mPaymentViewModel.dejavooRefTxnId?.let {dejavooRefTxnId->
                var dejavoo=Dejavoo(
                    registerId =  prefProvider.getValue(
                        Constants.DEJAVOO_REGISTER_ID,""
                    ),
                    authKey = prefProvider.getValue(
                        Constants.DEJAVOO_AUTH_KEY,""
                    ),
                    tpn = prefProvider.getValue(
                        Constants.DEJAVOO_TPN,""
                    ),
                    paymentType = "Credit",
                    transType="TipAdjust",
                    amount= dashBoardCategoryViewModel.totalAmount.toString(),
                    tip = tippedAmount.toString(),
                    refId= dejavooRefTxnId,
                    printReceipt= false,
                    performedBy=  prefProvider.employeeName(),
                    isProd=  Constants.paymentLive,
                    txnType = TransactionType.TIP_ADJUSTMENT
                )
                paymentGateway.processPayment(
                    context.applicationContext,
                    dejavoo,
                    onSuccess = { tResponse->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        mPaymentViewModel.dejavooRefTxnId=null
                        callUpdateTip()
//                    transactionJsonResponse.nameValuePairs?.let {
//                        if (it.msg != null) {
//                            if (it.msg!!.contains(
//                                    "APPROVED"
//                                )
//                            ) {
//                                mPaymentViewModel.valorRefTxnId = null
//                                mPaymentViewModel.valorTransactionNumber = null
//                                callUpdateTip()
////                                dashBoardCategoryViewModel.takenTipUsingValor.postValue(Event(transactionViewModel))
//                            } else {
//                                dismissProgressDialog()
//                                /* runOnUiThread(Runnable {
//                                     AlertUtils.showCustomAlert(
//                                         requireContext(),
//                                         it.msg
//                                     )
//                                 })*/
//                            }
//                        }
//                    }

                    },
                    onFailure = {
                        ProgressUtils.dismissProgressDialog()

                        /*runOnUiThread(Runnable {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                errorMessage
                            )
                        })*/

                    }
                )
                /* Process Tip Adjust */
            }
        }
    }

    lateinit var paymentCoroutineScope: CoroutineScope
    val paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CustomDisplay adjustValorTips()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }

    private fun adjustValorTips() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
//                        ProgressUtils.dismissProgressDialog()

            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = PaymentGatewayFactory(
                ValorPaymentGateway(),
                DejavooPaymentGateway()
            ).create(gatewayType)

            mPaymentViewModel.valorRefTxnId?.let { valorRefTxId ->
                context?.let {

                    var valor = Valor(
                        apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                        appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                        epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                        endpoint = Constants.VALOR_TIP_ADJUST,
                        txnType = TransactionType.TIP_ADJUSTMENT,
                        channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                        transMode = "1",
                        transCode = "1",
                        reqTxnId = valorRefTxId,
                        tipAmount = tippedAmount.toString(),
                        tipEntry = "1",
                        txn_type = "refund",
                        surchargeIndicator = "1",
                        sale_refund = "1",
                        isProd = Constants.paymentLive,
                        transactionId = ""
                    )

                    paymentGateway.processPayment(
                        context = it.applicationContext,
                        valor,
                        onSuccess = {tResponse->
                            var transactionJsonResponse = Gson().fromJson<ValorSuccessResponse>(
                                tResponse,
                                ValorSuccessResponse::class.java
                            )
                            transactionJsonResponse.nameValuePairs?.let {
                                if (it.msg != null) {
                                    if (it.msg!!.contains(
                                            "APPROVED"
                                        )
                                    ) {
                                        mPaymentViewModel.valorRefTxnId = null
                                        mPaymentViewModel.valorTransactionNumber = null
                                        callUpdateTip()
//                                dashBoardCategoryViewModel.takenTipUsingValor.postValue(Event(transactionViewModel))
                                    } else {
                                        dismissProgressDialog()
                                        /* runOnUiThread(Runnable {
                                             AlertUtils.showCustomAlert(
                                                 requireContext(),
                                                 it.msg
                                             )
                                         })*/
                                    }
                                }
                            }

                        },
                        onFailure = {
                            ProgressUtils.dismissProgressDialog()

                            /*runOnUiThread(Runnable {
                                AlertUtils.showCustomAlert(
                                    requireContext(),
                                    errorMessage
                                )
                            })*/
                        }
                    )
                }
            }
            /* Process Tip Adjust */

        }
    }

    private fun enableConfirmButton() {
        binding.tvContinue.isEnabled = true
        binding.tvContinue.setBackgroundColor(Color.parseColor("#ff6000"))
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

        /* if (MethodUtils.isEnableCashDiscount(context) && !showCashCreditPrice) {
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
             dashBoardCategoryViewModel.totalPrice=cashTotal.substring(1).toDouble()
             binding.txtTotalCash?.setText(cashTotal)
             binding.txtTotalCard?.setText(cardTotal)
             this.onDisplayChanged()
             this.onContentChanged()
         }*/
    }

    override fun onStop() {
        if (this@CustomDisplay::paymentCoroutineScope.isInitialized) {
            paymentCoroutineScope.cancel()
        }
        super.onStop()
    }
}
