package com.pays.pos.ui.fragments.payment


import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.text.trimmedLength
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.epson.epos2.printer.Printer
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.aidl.ICallback
import com.pays.pos.aidl.IWoyouService
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.RedeemLoyaltyInfo
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.GuestDataModel
import com.pays.pos.data.model.SplitBundleModel
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.data.model.requestModel.giftCard.response.GiftCardAddValueResponse
import com.pays.pos.data.model.requestModel.giftCard.response.SellGiftCardResponseModel
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.BILLING_ADDRESS
import com.pays.pos.data.remote.Constants.BLUETOOTH
import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.BUSINESS_NAME
import com.pays.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.pays.pos.data.remote.Constants.BUSINESS_WEBSITE
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE
import com.pays.pos.data.remote.Constants.CUSTOMER
import com.pays.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.pays.pos.data.remote.Constants.GIFT_CARD
import com.pays.pos.data.remote.Constants.GUEST_POSITION
import com.pays.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED
import com.pays.pos.data.remote.Constants.KITCHEN
import com.pays.pos.data.remote.Constants.KITCHENANDCUSTOMER
import com.pays.pos.data.remote.Constants.LARGE
import com.pays.pos.data.remote.Constants.LOCK_SCREEN_TRANSACTION
import com.pays.pos.data.remote.Constants.LOYALTY_ADDED
import com.pays.pos.data.remote.Constants.OPEN_ORDER
import com.pays.pos.data.remote.Constants.OPEN_ORDER_UPDATE_FOR_PRINT
import com.pays.pos.data.remote.Constants.OPTION_TYPE
import com.pays.pos.data.remote.Constants.ORDER_COMPLETED
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.PAYMENT_ID
import com.pays.pos.data.remote.Constants.PAYMENT_ID_FOR_CUSTOMER_DISPLAY
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.pays.pos.data.remote.Constants.SAVE_SPLIT_BUNDLE
import com.pays.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.pays.pos.data.remote.Constants.SHIPPING_ADDRESS
import com.pays.pos.data.remote.Constants.SPLIT_DINEIN_CHECKOUT
import com.pays.pos.data.remote.Constants.SPLIT_DINEIN_MODEL
import com.pays.pos.data.remote.Constants.SPLIT_IS_GUESTPAY
import com.pays.pos.data.remote.Constants.SUB_TOTAL
import com.pays.pos.data.remote.Constants.SUB_TOTAL_DINEIN
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TOTAL_PRICE_DINEIN
import com.pays.pos.data.remote.Constants.VENUE_LOGO
import com.pays.pos.data.remote.Constants.WHOLE_AMOUNT
import com.pays.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.pays.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.pays.pos.databinding.FragmentOrderCompletBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.adapter.SplitListAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.ui.fragments.transactions.TransactionViewModel
import com.pays.pos.utils.*
import com.pays.pos.utils.MethodUtils.Companion.toDoubleWithPrecision
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.printer.PrinterClass.BLUETOOTH_TIMEOUT
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.*
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class OrderCompleteFragment : Fragment(), View.OnClickListener, StatusChangeEventListener,
    BatteryStatusChangeEventListener, ICallback {
    private var IS_GIFT_CARD_TYPE: Boolean = false
    private var isPrint: Boolean = false
    private var isPrintCustomer: Boolean = false
    private var isFirstKitPrint = false
    private val paymentViewModel by activityViewModels<PaymentViewModel>()
    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private var tipAmount: Double = 0.0
    private var dineInList: ArrayList<DineInModel> = arrayListOf()
    private var customerPrinterDineIn: List<PrinterResponse.Data.CustomerReceiptPrinters>? = null
    private var isFromCustomer: Boolean = false
    private var dis_charge_value: Double = 0.0
    private var isGuest: Boolean = false
    private var remainingAmount: Double = 0.0
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    private var splitPaidAmount: Double = 0.0
    private var woyouService: IWoyouService? = null
    private var splitValue: Int = -1
    private var subTotalWT = 0.0
    private var serviceCharge = 0.0
    private var totalTaxAmount = 0.0
    private var totalDiscount = 0.0
    private var changeAmtGlobal = 0.00
    private var payTypeGlb = ""
    private var redeemLoyaltyInfo: RedeemLoyaltyInfo? = null
    private var isSpilt: Boolean = false
    private var isCustomCash: Boolean = false
    private var isSplitByAmount: Boolean = false
    private var splitTotalAmount: Double = 0.0
    private var splitChange: Double = 0.0
    private var getDineInOrderDetails: GetOrderDetailsResponse.Data? = null
    private var isSplitByNo: Boolean = false
    private var isLastPayment: Boolean = false
    private var isDineIn: Boolean = false
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    private val viewModelDashBoard by activityViewModels<DashBoardCategoryViewModel>()
    private var orderID: Int = 0
    var paymentType: String = ""
    private var type: String = ""
    private var totalPrice: Double = 0.0
    private var paymentAmount: Double = 0.0
    private var paidAmountValue: Double = 0.0
    private var WholetotalPrice: Double = 0.0
    private val viewModel by viewModels<OrderCompleteViewModel>()
    private val paymentviewModel by viewModels<PaymentViewModel>()
    private var receiptModel: CreateOrderResponse.Data? = null
    private var giftCardReceiptModel: SellGiftCardResponseModel.Data? = null
    private var giftCardAddValueReceiptModel: GiftCardAddValueResponse.Data? = null
    private var receiptModelForOpenORder: CreateOrderResponse.Data? = null
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var splitList: ArrayList<SplitDetailListModel> = arrayListOf()
    private lateinit var printerDialog: PrinterDialog
    private var isGuestPaymentTotal = false
    private var isNotPrinted = true
    private var cartList: CartModel? = null
    private var paidAmount: Double = 0.0
    private var noCashAdjGlobal: Double = 0.0
    private var pd: Dialog? = null

    /*Star label printer - START*/
    lateinit var settings: StarConnectionSettings
    lateinit var printer: StarPrinter
    /*Star label printer - END*/

    private var oneItemPerReceipt: Boolean = true

    /*Added By Rahul */
    private var isOrderUpdated: Boolean = false

    private var orderTypeToCheckKiosk: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentOrderCompletBinding
    private val TAG = "OrderCompleteFragment"
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private lateinit var splitAdapter: SplitListAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("kitchenPrintTMM", "oncreated")
        isNotPrinted = true
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        isPrint = true
        isPrintCustomer = true

        isOrderUpdated = false

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel

            )
        }

        printerDialog = PrinterDialog()
        //progressDialog()

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            IS_GIFT_CARD_TYPE = true
        }

        if (requireArguments().getBoolean("isSpilt")) {
            observeSplitList()
        } else {
            prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT, "")
        }
        getKitchenReceiptSettings()

        observeTipsList()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) { it ->
                if (it.getString(Constants.KEY)?.lowercase() == "FROM_CUSTOMER".lowercase()) {
                    isFromCustomer = true
                    if (pd != null && pd?.isShowing == true) {
                        pd?.dismiss()
                    }
                }

            }
        getCustomerReceiptSettings()
        payTypeGlb = requireArguments().getString("paymentType") ?: ""

        getCustomerPrinterForDineIn()
        splitAdapter = SplitListAdapter()
        prefProvider.setValueboolean(Constants.IS_ORDER_UPDATE, value = false)
        binding.rvSplits.adapter = splitAdapter
        noCashAdjGlobal =
            MethodUtils.roundOffAmountDouble(requireArguments().getDouble("noCashAdj"))

        return binding.root

    }

    private val tipListViewModel by activityViewModels<TipListViewModel>()
    private val transactionViewModel by viewModels<TransactionViewModel>()

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    @Inject
    lateinit var apiModule1: ApiModule1

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()

            val finalPaidAmount: Double = if (isCustomCash) {
                MethodUtils.roundOffAmountDown(paidAmount)
            } else {
                MethodUtils.roundOffAmountDown(paidAmount + tipAmount)
            }

            if (prefProvider.getValueboolean(Constants.TIP_ADDED, false)) {
                prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                presentation.showThankYou(finalPaidAmount)
            } else {
                val paymentIdForCustomerDisplay = prefProvider.getValueInt(
                    PAYMENT_ID_FOR_CUSTOMER_DISPLAY, 0
                )
                presentation.showWouldYouLikeToAddTipScreen(
                    tipListViewModel,
                    transactionViewModel,
                    finalPaidAmount, paymentIdForCustomerDisplay,
                    paymentType == "Card",
                    paymentViewModel = paymentViewModel,
                    magRequestUtils = magtekRequestUtils,
                    apiModule1 = apiModule1,
                    true
                )
            }
        }
    }

    private fun observeSplitList() {
        viewModel.allSplitList.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                splitList = arrayListOf()
                splitList = it.toCollection(arrayListOf())
                splitAdapter.setList(it.toCollection(arrayListOf()))

            }
        }
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getKitchenReceiptSettings()  it -> ${
                            Gson().toJson(
                                it
                            )
                        }"
                    )
                )

            if (it != null) {
                EventBus.getDefault()
                    .post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getKitchenReceiptSettings()  it -> ${
                                Gson().toJson(
                                    it
                                )
                            }"
                        )
                    )

                kitchenSettingModel = it
                LogUtil.logE(TAG, "isFromCustomer:  ${isFromCustomer}")
                Log.e(
                    "getKitchenPrinters",
                    "size of kitchen print list ${Gson().toJson(receiptModel)}"
                )
                if (!isFromCustomer) {

                    EventBus.getDefault()
                        .post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getKitchenReceiptSettings()  isFromCustomer -> ${
                                    Gson().toJson(
                                        !isFromCustomer
                                    )
                                }"
                            )
                        )

                    getKitchenPrinters()
                }
            }
        }
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
                customerSettingModel = it


            }

        }

    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                tipsList = it

            }


        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sample Alert")
        builder.setMessage("This is a simple alert dialog.")

        val alertDialog: AlertDialog = builder.create()



        viewModelDashBoard.processingTipForCard.observe(viewLifecycleOwner) {
            if (it) {

                //viewModelDashBoard.tipButtonOnCustomerDisplayClicked.value = true
                binding.llHome.isClickable = false
                binding.llNoReceipt.isClickable = false
                ProgressUtils.showProgressDialog("Processing Tip", requireActivity())
                //alertDialog.show()
            } else {

                CoroutineScope(Dispatchers.IO).launch {
                    //delay(300)

                }

                // alertDialog.dismiss()
            }
        }

        viewModelDashBoard.customerGivenTip.observe(viewLifecycleOwner) {
            if (it) {

                binding.tipGivenLayout?.visible()

                var totalAmountToShow = 0.0
                var tipToShow = 0.0
                var finalAmountToShow = 0.0
                var changeAmount = 0.0

                viewModelDashBoard.apply {
                    finalAmount = MethodUtils.roundOffAmountString(totalTipAmount)
                        .toDouble() + MethodUtils.roundOffAmountString(
                        paidAmount
                    ).toDouble()

                    totalAmountToShow = totalAmount
                    tipToShow = totalTipAmount
                    finalAmountToShow = totalAmount + totalTipAmount

                    changeAmount =
                        MethodUtils.roundOffAmountString(paidAmount - finalAmountToShow).toDouble()

                    if (paymentTypeForTip.equals("cash", true))
                        if (changeAmount < 0.0 || changeAmount > 0.0) {

                            val _title = if (changeAmount < 0) "$" + Math.abs(changeAmount)
                                .toString() + " to collect more" else "$" + Math.abs(changeAmount)
                                .toString() + " Change"

                            binding.txtChangeAmount.apply {
                                visible()
                                text = _title
                            }
                        } else
                            binding.txtChangeAmount.gone()



                    binding.txtTotalAmount?.setText("$${MethodUtils.roundOffAmountString(totalAmountToShow)}")
                    binding.txtTipAmount?.setText  ("$${MethodUtils.roundOffAmountString(tipToShow)}")
                    binding.txtFinalAmount?.setText("$${MethodUtils.roundOffAmountString(finalAmountToShow)}")


                    customerGivenTip.value = false

                    runBlocking {
                        // delay(1000)
                        //  viewModelDashBoard.tipButtonOnCustomerDisplayClicked.value = false
                        ProgressUtils.dismissProgressDialog()
                        binding.llHome.isClickable = true
                        binding.llNoReceipt.isClickable = true
                    }
                }

            } else {
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Binding()
        setupSnackbar()
        observeShowProgress()
        observeTipClicked()

        lifecycleScope.launch {
            oneItemPerReceipt = dashboardViewModel.getLabelPrinterSettingsData().oneItemPerReciept
        }

        ProgressUtils.showProgressDialog(requireActivity())
        binding.llHome.isEnabled = false
        Handler().postDelayed({
            binding.llHome.isEnabled = true
        }, 500)

        arguments?.let {
            orderTypeToCheckKiosk = it.getString("orderType_to_check_kiosk", "")
        }

        prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)
        tipAmount = requireArguments().getDouble("TipAmount")
        isDineIn = requireArguments().getBoolean("isDineIn")
        if (isDineIn) {
            paidAmount = requireArguments().getDouble("PaidAmount")
            WholetotalPrice = requireArguments().getDouble("WholetotalPrice")
            remainingAmount = requireArguments().getDouble("remainingAmount")
            orderID = requireArguments().getInt("orderID")
            receiptModel = requireArguments().getParcelable("receiptData")
            splitValue = requireArguments().getInt("splitValue")
            isLastPayment = requireArguments().getBoolean("isLastPayment", false)
            LogUtil.logE(TAG, "isLastPaymentDine:  ${isLastPayment}")
            isGuest = requireArguments().getBoolean("isGuest")
            isGuestPaymentTotal = requireArguments().getBoolean("isGuestPaymentTotal")
            isSpilt = requireArguments().getBoolean("isSpilt")
            isSplitByNo = requireArguments().getBoolean("isSplitByNo")
            isCustomCash = requireArguments().getBoolean("isCustomCash")
            isSplitByAmount = requireArguments().getBoolean("isSplitByAmount")
            splitChange = requireArguments().getDouble("splitChange")
            paymentType = requireArguments().getString("paymentType", "")
            dis_charge_value = requireArguments().getDouble("dis_charge_value", 0.0)
            getDineInOrderDetails = requireArguments().getParcelable(PRINT_DATA_DINE_IN)
            dineInList =
                requireArguments().getParcelableArrayList<DineInModel>(DINE_IN_ADAPTER_LIST)
                    ?: arrayListOf()
            subTotalWT = requireArguments().getDouble(Constants.DINE_IN_SUBTOTAL)
            totalTaxAmount = requireArguments().getDouble(Constants.DINE_IN_TAX)
            totalDiscount = requireArguments().getDouble(Constants.DINE_IN_DISCOUNT)
            serviceCharge = requireArguments().getDouble(Constants.DINE_IN_SERVICECHARGE)
        } else {

            cartList = requireArguments().getParcelable("cartList")
            redeemLoyaltyInfo = requireArguments().getParcelable("redeemLoyalty")
            paidAmount = requireArguments().getDouble("PaidAmount")
            WholetotalPrice = requireArguments().getDouble("WholetotalPrice")
            remainingAmount = requireArguments().getDouble("remainingAmount")
            splitChange = requireArguments().getDouble("splitChange")
            orderID = requireArguments().getInt("orderID")
            receiptModel = requireArguments().getParcelable("receiptData")
            giftCardReceiptModel = requireArguments().getParcelable("giftCardReceiptData")
            receiptModelForOpenORder = requireArguments().getParcelable("receiptData")
            splitValue = requireArguments().getInt("splitValue")
            isSpilt = requireArguments().getBoolean("isSpilt")
            isSplitByNo = requireArguments().getBoolean("isSplitByNo")
            isCustomCash = requireArguments().getBoolean("isCustomCash")
            isSplitByAmount = requireArguments().getBoolean("isSplitByAmount")
            paymentType = requireArguments().getString("paymentType", "")
            dis_charge_value = requireArguments().getDouble("dis_charge_value", 0.0)

        }

        /*Added By Rahul */
        setUpdateEnabledInReceiptModel()
        setLabelData()

        binding.edtEmail.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.edtEmail.setHint("")
            }
        }
        if (!isDineIn) {
            if (isSpilt) {
                // saveDataInPrefrences()
                binding.viewSplitLine.visibility = View.VISIBLE
                binding.linearSplitLayout.visibility = View.VISIBLE
                binding.linerContent.visibility = View.VISIBLE
                binding.txtRemainingAmount.visibility = View.VISIBLE
                binding.txtRemainingAmountLabel.visibility = View.VISIBLE
                if (!remainingAmount.toString().contains("$")) {
                    binding.txtRemainingAmount.text = MethodUtils.roundOffAmount(remainingAmount)
                } else {
                    binding.txtRemainingAmount.text = String.format("%.2f", remainingAmount)
                }
                binding.llHome.visibility = View.GONE
                binding.llNoReceipt.text = "Next Payment"
                binding.llNoReceipt.setTextColor(requireActivity().resources.getColor(R.color.white))
                binding.llNoReceipt.background =
                    requireContext().getDrawable(R.drawable.button_selected)
                // binding.linearTopHeaderSplit.visibility = View.VISIBLE
                binding.txtHome.visibility = View.GONE


                LogUtil.logE("addSplitToDatabase", "XXX")
                val title = "Split "
                viewModel.addSplitToDatabase(
                    title,
                    (if (!isCustomCash) paidAmount + tipAmount else paidAmount) - splitChange,
                    remainingAmount
                )
                binding.txtTitle.text =
                    "$" + MethodUtils.roundOffAmountString(
                        if (!isCustomCash) paidAmount + tipAmount else paidAmount
                    )


                LogUtil.logE(TAG, "paymentpaidAmount  ${paidAmount}")
                LogUtil.logE(TAG, "paymentWholetotalPrice  ${WholetotalPrice}")
                LogUtil.logE(TAG, "paymentremainingAmount  ${remainingAmount}")
                LogUtil.logE(TAG, "paymentsplitValue ${splitValue}")
                LogUtil.logE(TAG, "paymentisSpilt  ${isSpilt}")
                LogUtil.logE(TAG, "paymentisCustomCash  ${isCustomCash}")
                LogUtil.logE(TAG, "paymentisSplitByAmount  ${isSplitByAmount}")
                if (isSpilt) {
                    var sp = requireArguments().getString(Constants.SPLIT_PAY_AMOUNT)
                    LogUtil.logE(TAG, "payment SplitTotalAmount  ${sp}")

                }

                if (isSplitByAmount) {
                    if (paidAmount > WholetotalPrice) {
                        changeAmtGlobal =
                            MethodUtils.roundOffAmountDouble(paidAmount - WholetotalPrice)
                                .toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if ((paidAmount - WholetotalPrice) > 0) paidAmount - WholetotalPrice else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount
                            ) + " payment successful"

                        LogUtil.logE("Change 1", binding.txtChangeAmount.text.toString())

                    } else {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(0.0).toDouble()
                        binding.txtChangeAmount.gone()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(0.0) + " Change"
                        LogUtil.logE("Change 2", binding.txtChangeAmount.text.toString())
                    }
                } else if (remainingAmount < paidAmount) {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange).toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount
                            ) + " payment successful"

                        LogUtil.logE("Change 2", binding.txtChangeAmount.text.toString())
                    } else {
                        var changeValue = 0.0
                        if (prefProvider.getValue(Constants.OPTION_TYPE, "") == "SurCharge") {
                            changeValue =
                                (paidAmount - prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "")
                                    .toDouble()) - remainingAmount
                        } else {
                            changeValue = paidAmount - remainingAmount
                        }
                        if (Math.round(changeValue) > 0.0) {
                            changeAmtGlobal =
                                MethodUtils.roundOffAmountDouble(changeValue).toDouble()
                            binding.txtChangeAmount.visible()
                            binding.txtChangeAmount.text =
                                MethodUtils.roundOffAmount(if (changeValue > 0) changeValue else 0.00) + " Change"

                            LogUtil.logE("Change 4", binding.txtChangeAmount.text.toString())
                        }
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"
                    }
                } else {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange).toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"

                        LogUtil.logE("Change 5", binding.txtChangeAmount.text.toString())
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount
                            ) + " payment successful"
                    } else {
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"
                    }
                }
            } else {
                binding.linerContent.visibility = View.VISIBLE
                binding.llHome.visibility = View.VISIBLE
                binding.txtHome.visibility = View.GONE
                //  binding.linearTopHeaderSplit.visibility = View.GONE
                binding.viewSplitLine.visibility = View.GONE
                binding.llNoReceipt.visibility = View.GONE
                binding.linearSplitLayout.visibility = View.GONE
                binding.txtRemainingAmount.visibility = View.GONE
                binding.txtRemainingAmountLabel.visibility = View.GONE
                binding.llNoReceipt.text = getString(R.string.no_receipt)
                binding.llNoReceipt.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
                binding.llNoReceipt.background =
                    requireContext().getDrawable(R.drawable.background_square_border_grey)
                viewModel.deleteSplitDb()
                if (isCustomCash) {
                    binding.txtTitle.text =
                        "$" + MethodUtils.roundOffAmountString(paidAmount)
                    binding.txtPaymentAmount.text =
                        "" + MainApplication.getInstance()!!
                            .getText(R.string.symbole) + MethodUtils.roundOffAmountString(paidAmount) + " payment successful"

                } else {
                    binding.txtTitle.text =
                        "$" + MethodUtils.roundOffAmountString(paidAmount + tipAmount)
                    binding.txtPaymentAmount.text =
                        "" + MainApplication.getInstance()!!
                            .getText(R.string.symbole) + MethodUtils.roundOffAmountString(paidAmount + tipAmount) + " payment successful"
                }


                if (isCustomCash) {
                    changeAmtGlobal =
                        MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount).toDouble()
                    binding.txtChangeAmount.visible()

                    val ca = remainingAmount - tipAmount
                    binding.txtChangeAmount.text =
                        MethodUtils.roundOffAmount(if (ca > 0) ca else 0.00) + " Change"

                    LogUtil.logE("Change 6", binding.txtChangeAmount.text.toString())
                } else {
                    if (remainingAmount < 0) {
                        changeAmtGlobal =
                            MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount).toDouble()
                        binding.txtChangeAmount.visible()
                        val ca = remainingAmount - tipAmount
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (ca > 0) ca else 0.00) + " Change"

                        LogUtil.logE("Change 7", binding.txtChangeAmount.text.toString())
                    }
                }


            }
        } else {
            if (isSpilt) {
                binding.viewSplitLine.visibility = View.VISIBLE
                binding.linearSplitLayout.visibility = View.VISIBLE
                binding.linerContent.visibility = View.VISIBLE
                binding.txtRemainingAmount.visibility = View.VISIBLE
                binding.txtRemainingAmountLabel.visibility = View.VISIBLE
                if (!remainingAmount.toString().contains("$")) {
                    binding.txtRemainingAmount.text = MethodUtils.roundOffAmount(remainingAmount)
                } else {
                    binding.txtRemainingAmount.text = String.format("%.2f", remainingAmount)
                }

                binding.llHome.visibility = View.GONE
                binding.llNoReceipt.text = "Next Payment"
                binding.llNoReceipt.setTextColor(requireActivity().resources.getColor(R.color.white))
                binding.llNoReceipt.background =
                    requireContext().getDrawable(R.drawable.button_selected)
                //  binding.linearTopHeaderSplit.visibility = View.VISIBLE
                binding.txtHome.visibility = View.GONE
                var title = "Split "
                LogUtil.logE("addSplitToDatabase", "XXX XXX")
                viewModel.addSplitToDatabase(
                    title,
                    (paidAmount + tipAmount) - splitChange,
                    remainingAmount
                )
                binding.txtTitle.text =
                    "$" + MethodUtils.roundOffAmountString(
                        paidAmount + tipAmount
                    )

                if (remainingAmount < paidAmount) {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange)
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"

                        LogUtil.logE("Change 8", binding.txtChangeAmount.text.toString())
                    } else {
                        var temp_Change =
                            MethodUtils.roundOffAmountDouble((paidAmount - noCashAdjGlobal) - remainingAmount)
                        if (!(temp_Change.equals(0.0) || temp_Change.equals(0) || temp_Change <= 0.0)) {
                            changeAmtGlobal =
                                MethodUtils.roundOffAmountDouble((paidAmount - noCashAdjGlobal) - remainingAmount)
                            binding.txtChangeAmount.visible()
                            val ca = (paidAmount - noCashAdjGlobal) - remainingAmount
                            binding.txtChangeAmount.text =
                                MethodUtils.roundOffAmount(if (ca > 0) ca else 0.00) + " Change"

                            LogUtil.logE("Change 9", binding.txtChangeAmount.text.toString())
                        }
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"
                    }

                } else {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange)
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"

                        LogUtil.logE("Change 10", binding.txtChangeAmount.text.toString())
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"
                    } else {
                        binding.txtPaymentAmount.text =
                            "" + MainApplication.getInstance()!!
                                .getText(R.string.symbole) + MethodUtils.roundOffAmountString(
                                paidAmount + tipAmount
                            ) + " payment successful"
                    }
                }
            } else {
                binding.viewSplitLine.visibility = View.GONE
                if (isGuest) {
                    if (isLastPayment) {
                        binding.llHome.visibility = View.VISIBLE
                        binding.txtHome.visibility = View.GONE
                        // binding.linearTopHeaderSplit.visibility = View.GONE
                        binding.llCheckOut.visibility = View.GONE
                    } else {
                        binding.llHome.visibility = View.GONE
                        binding.txtHome.visibility = View.GONE
                        //  binding.linearTopHeaderSplit.visibility = View.VISIBLE
                        binding.llCheckOut.visibility = View.VISIBLE
                    }
                } else {
                    binding.llHome.visibility = View.VISIBLE
                    binding.txtHome.visibility = View.GONE
                    //  binding.linearTopHeaderSplit.visibility = View.GONE
                    binding.llCheckOut.visibility = View.GONE
                }

                binding.linearSplitLayout.visibility = View.GONE
                binding.linerContent.visibility = View.VISIBLE
                binding.llNoReceipt.visibility = View.GONE
                binding.txtRemainingAmount.visibility = View.GONE
                binding.txtRemainingAmountLabel.visibility = View.GONE
                binding.llNoReceipt.text = getString(R.string.no_receipt)
                binding.llNoReceipt.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
                binding.llNoReceipt.background =
                    requireContext().getDrawable(R.drawable.background_square_border_grey)
                viewModel.deleteSplitDb()
                binding.txtTitle.text =
                    "$" + MethodUtils.roundOffAmountString(paidAmount + tipAmount)

                if (isCustomCash) {
                    changeAmtGlobal = MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount)
                    binding.txtChangeAmount.visible()
                    binding.txtChangeAmount.text =
                        MethodUtils.roundOffAmount(if ((remainingAmount - tipAmount) > 0) remainingAmount - tipAmount else 0.00) + " Change"
                } else {
                    if (remainingAmount < 0 || remainingAmount == 0.0) {
                        changeAmtGlobal =
                            MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount)
                        binding.txtChangeAmount.gone()
                    } else {
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if ((remainingAmount - tipAmount) > 0) remainingAmount - tipAmount else 0.00) + " Change"
                    }
                }

                binding.txtPaymentAmount.text =
                    "" + MainApplication.getInstance()!!
                        .getText(R.string.symbole) + MethodUtils.roundOffAmountString(paidAmount + tipAmount) + " payment successful"


            }
        }

        if (prefProvider.getValue(Constants.CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.txtAddCustomer.visibility = View.GONE
        } else {
            if (isSpilt) {
                binding.txtAddCustomer.visibility = View.GONE
            } else {
                binding.txtAddCustomer.visibility = View.GONE
            }
        }

        //  binding.txtNextbutton.setOnClickListener(this)
        binding.txtHome.setOnClickListener(this)
        binding.txtAddCustomer.setOnClickListener(this)
        binding.llMessage.setOnClickListener(this)
        binding.llEmail.setOnClickListener(this)
        binding.llNoReceipt.setOnClickListener(this)
        binding.llPrint.setOnClickListener(this)
        binding.txtSend.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llHome.setOnClickListener(this)
        binding.llCheckOut.setOnClickListener(this)


//        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
//            val result = bundle.getParcelable<TbCustomer>("data")
//            if (result != null) {
//                isFromCustomer = true
//                //  LogUtil.logE("request_key_customer", result.first_name)
//                val payment_id = prefProvider.getValueInt(PAYMENT_ID, 0)
//                val final_Reward = result.final_reward
//                result.id?.let { viewModel.assignCustomer(orderID, it, payment_id, final_Reward!!) }
//            }
//        }

        // clear pax response from database
        paymentViewModel.deletePaxPaymentData()
        if (!isSpilt) {

            prefProvider.setValue(Constants.ORDER_TYPE, "")
            prefProvider.setValue(Constants.ORDER_TYPE_NAME, "")
            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
            prefProvider.setValue(Constants.PREF_CUSTOMER, "")
            prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
            viewModelDashBoard.cartModel = null
            viewModel.deleteCart()
        }

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isSpilt) {
                        moveToDashboard()
                    }

                }

            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

    }

    private fun observeTipClicked() {
        dashboardViewModel.tipButtonOnCustomerDisplayClicked.observe(viewLifecycleOwner,
            object : Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    t?.let {
                        binding.llHome.isClickable = !it
                    }
                }
            })
    }

    /*Added By Rahul  */
    private fun setUpdateEnabledInReceiptModel() {
        if (prefProvider.getValue(Constants.OPEN_ORDER_ITEMS_OLD, "").isNotEmpty()) {
            var oldDataModel: List<OnlineOrderResponseModel.Data.OrderItem> =
                Gson().fromJson(
                    prefProvider.getValue(Constants.OPEN_ORDER_ITEMS_OLD, ""),
                    object :
                        TypeToken<List<OnlineOrderResponseModel.Data.OrderItem>?>() {}
                        .type
                )

            var (newData, isUpdated) = getPrintingData(
                oldDataModel,
                receiptModel?.order?.orderItems
            )

            newData?.forEach {
                if (!it.isPrinted) {
                    isUpdated = true
                }
            }

            receiptModel?.order?.apply {
                orderItems = newData!!
            }
            Log.d("isOrderUpdated", "870 -> ${isOrderUpdated}")
            isOrderUpdated = isUpdated
        }
    }

    public fun getPrintingData(
        oldItems: List<OnlineOrderResponseModel.Data.OrderItem>,
        newItems: List<CreateOrderResponse.Data.Order.OrderItem>?
    ): Pair<List<CreateOrderResponse.Data.Order.OrderItem>?, Boolean> {

        var isUpdated = false

        Log.d("isOrderUpdated", "882 -> ${isOrderUpdated}")
        if (oldItems.size == newItems?.size) {
            for (oldIndex in 0 until oldItems.size) {

                for (indexNew in 0 until newItems.size) {

                    if (oldItems.size != newItems.size) {
                        isUpdated = true
                    }

                    if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldIndex == indexNew) {
                        if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true
                        }
                        if (oldItems.get(oldIndex).price != newItems.get(indexNew).price) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true
                        }

                        if (!oldItems.get(oldIndex).note.equals(newItems.get(indexNew).note)) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }
                    }

                    if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size != newItems.get(indexNew).orderItemModifiers.size && oldIndex == indexNew
                    ) {
                        newItems.get(indexNew).isEdited = true
                        isUpdated = true

                    } else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size == newItems.get(indexNew).orderItemModifiers.size && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size != 0 && oldIndex == indexNew
                    ) {

                        if (oldItems.get(oldIndex).orderItemModifiers != newItems.get(
                                indexNew
                            ).orderItemModifiers
                        ) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }

//                                if (oldItems.get(oldIndex).orderItemModifiers == newItems.get(indexNew).orderItemModifiers) {

                        for (oldModifiersIndex in 0 until oldItems.get(oldIndex).orderItemModifiers.size) {

                            for (newModifiersIndex in 0 until newItems.get(indexNew).orderItemModifiers.size) {

                                if (oldItems.get(oldIndex).orderItemModifiers.get(
                                        oldModifiersIndex
                                    ).id == newItems.get(indexNew).orderItemModifiers.get(
                                        newModifiersIndex
                                    ).id
                                ) {
                                    if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).modifier_quantity != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).modifierQuantity
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).price != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).price
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).price != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).price
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (!oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).name.equals(
                                            newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).name
                                        )
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    }

                                }
                            }

                        }

//                                }
                    }

                    /*  else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(oldIndex).orderItemModifiers.size == newItems.get(indexNew).orderItemModifiers.size) {
                          *//*if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity) {
                                    newItems.get(indexNew).isEdited = true
                                }
                                 Modifiers are not equal, i.e. edited
                                else*//* if (oldItems.get(oldIndex).orderItemModifiers != newItems.get(indexNew).orderItemModifiers) {
                                    newItems.get(indexNew).isEdited = true
                                }
                                *//* Modifiers are equal, i.e. the quantity of modifiers may change*//*
                            }*/ else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId) {
                        if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity && oldIndex == indexNew) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true


                        }
                        /* Modifiers are not equal, i.e. edited */
                        else if ((oldItems.get(oldIndex).orderItemModifiers != newItems.get(
                                indexNew
                            ).orderItemModifiers) && (oldIndex == indexNew)
                        ) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }
                        /* Modifiers are equal, i.e. the quantity of modifiers may change*/
                        else if (oldItems.get(oldIndex).orderItemModifiers == newItems.get(
                                indexNew
                            ).orderItemModifiers
                        ) {

                            for (oldModifiersIndex in 0 until oldItems.get(oldIndex).orderItemModifiers.size) {
                                for (newModifiersIndex in 0 until newItems.get(indexNew).orderItemModifiers.size) {
                                    if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).id == newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).id
                                    ) {
                                        if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).modifier_quantity != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).modifierQuantity
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).price != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).price
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).price != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).price
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (!oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).name.equals(
                                                newItems.get(indexNew).orderItemModifiers.get(
                                                    newModifiersIndex
                                                ).name
                                            )
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        }

                                    }
                                }
                            }

                        }

                    }

                }

            }
        } else {

            isUpdated = true

            for (oldIndex in 0 until oldItems.size) {

                for (indexNew in 0 until newItems!!.size) {

                    if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId) {
                        if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true
                        }
                        if (oldItems.get(oldIndex).price != newItems.get(indexNew).price) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true
                        }

                        if (!oldItems.get(oldIndex).note.equals(newItems.get(indexNew).note)) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }
                    }

                    if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size != newItems.get(indexNew).orderItemModifiers.size
                    ) {
                        newItems.get(indexNew).isEdited = true
                        isUpdated = true

                    } else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size == newItems.get(indexNew).orderItemModifiers.size && oldItems.get(
                            oldIndex
                        ).orderItemModifiers.size != 0
                    ) {

                        if (oldItems.get(oldIndex).orderItemModifiers != newItems.get(
                                indexNew
                            ).orderItemModifiers
                        ) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }

//                                if (oldItems.get(oldIndex).orderItemModifiers == newItems.get(indexNew).orderItemModifiers) {

                        for (oldModifiersIndex in 0 until oldItems.get(oldIndex).orderItemModifiers.size) {

                            for (newModifiersIndex in 0 until newItems.get(indexNew).orderItemModifiers.size) {

                                if (oldItems.get(oldIndex).orderItemModifiers.get(
                                        oldModifiersIndex
                                    ).id == newItems.get(indexNew).orderItemModifiers.get(
                                        newModifiersIndex
                                    ).id
                                ) {
                                    if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).modifier_quantity != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).modifierQuantity
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).price != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).price
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).price != newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).price
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    } else if (!oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).name.equals(
                                            newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).name
                                        )
                                    ) {
                                        newItems.get(indexNew).isEdited = true
                                        isUpdated = true


                                    }

                                }
                            }

                        }

//                                }
                    }

                    /*  else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId && oldItems.get(oldIndex).orderItemModifiers.size == newItems.get(indexNew).orderItemModifiers.size) {
                          *//*if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity) {
                                    newItems.get(indexNew).isEdited = true
                                }
                                 Modifiers are not equal, i.e. edited
                                else*//* if (oldItems.get(oldIndex).orderItemModifiers != newItems.get(indexNew).orderItemModifiers) {
                                    newItems.get(indexNew).isEdited = true
                                }
                                *//* Modifiers are equal, i.e. the quantity of modifiers may change*//*
                            }*/ else if (oldItems.get(oldIndex).itemId == newItems.get(indexNew).itemId) {
                        if (oldItems.get(oldIndex).quantity != newItems.get(indexNew).quantity) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true


                        }
                        /* Modifiers are not equal, i.e. edited */
                        else if ((oldItems.get(oldIndex).orderItemModifiers != newItems.get(
                                indexNew
                            ).orderItemModifiers) && (oldIndex == indexNew)
                        ) {
                            newItems.get(indexNew).isEdited = true
                            isUpdated = true

                        }
                        /* Modifiers are equal, i.e. the quantity of modifiers may change*/
                        else if (oldItems.get(oldIndex).orderItemModifiers == newItems.get(
                                indexNew
                            ).orderItemModifiers
                        ) {

                            for (oldModifiersIndex in 0 until oldItems.get(oldIndex).orderItemModifiers.size) {
                                for (newModifiersIndex in 0 until newItems.get(indexNew).orderItemModifiers.size) {
                                    if (oldItems.get(oldIndex).orderItemModifiers.get(
                                            oldModifiersIndex
                                        ).id == newItems.get(indexNew).orderItemModifiers.get(
                                            newModifiersIndex
                                        ).id
                                    ) {
                                        if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).modifier_quantity != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).modifierQuantity
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).price != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).price
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).price != newItems.get(indexNew).orderItemModifiers.get(
                                                newModifiersIndex
                                            ).price
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        } else if (!oldItems.get(oldIndex).orderItemModifiers.get(
                                                oldModifiersIndex
                                            ).name.equals(
                                                newItems.get(indexNew).orderItemModifiers.get(
                                                    newModifiersIndex
                                                ).name
                                            )
                                        ) {
                                            newItems.get(indexNew).isEdited = true
                                            isUpdated = true

                                        }

                                    }
                                }
                            }

                        }

                    }

                }

            }
        }


        return Pair(newItems, isUpdated)
    }


    private fun setLabelData() {
        if (paymentType == "Card") {
            binding.txtTitleCash.text = "Card"
        } else {
            binding.txtTitleCash.text = "Cash"
        }

        binding.txtRemainingAmountLabel.text = "Remaining Amount"
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtHome -> {
                moveToDashboard()
            }

            R.id.txt_nextbutton -> {
                moveToNextPayment()
            }

            R.id.llHome -> {
                dashboardViewModel.clearCartModelBackup()
                moveToDashboard()
            }

            R.id.txtAddCustomer -> {
                setFragmentResultListener("ordercomplete_customer") { requestKey: String, bundle: Bundle ->


                }
                val bundle = Bundle()
                bundle.putBoolean("fromPayment", true)
                findNavController().navigate(R.id.action_orderCompleteFragment_to_assignCustomerOrderFragment)
            }

            R.id.llCheckOut -> {
                moveToCheckOut()
            }

            R.id.llMessage -> {

                binding.llNoReceipt.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llNoReceipt.setTextColor(resources.getColor(R.color.txtColor))

                binding.llEmail.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llEmail.setTextColor(resources.getColor(R.color.txtColor))

                binding.llMessage.background = resources.getDrawable(R.drawable.button_selected)
                binding.llMessage.setTextColor(resources.getColor(R.color.white))

                /*binding.llPrint.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llPrint.setTextColor(resources.getColor(R.color.txtColor))
*/
                type = "Message"
                binding.linerContent.visibility = View.VISIBLE
                if (isSpilt) {
                    binding.linearSplitLayout.visibility = View.VISIBLE
                } else {
                    binding.linearSplitLayout.visibility = View.GONE

                }
                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.GONE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.VISIBLE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
//                binding.viewSplitLine.visibility = View.GONE
                binding.tvMessage.setText(getString(R.string.please_enter_customer_contact_number))
                MethodUtils.hideKeyboard(requireActivity())
            }

            R.id.llEmail -> {
                binding.llNoReceipt.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llNoReceipt.setTextColor(resources.getColor(R.color.txtColor))

                binding.llEmail.background = resources.getDrawable(R.drawable.button_selected)
                binding.llEmail.setTextColor(resources.getColor(R.color.white))

                binding.llMessage.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llMessage.setTextColor(resources.getColor(R.color.txtColor))

                /*  binding.llPrint.background =
                      resources.getDrawable(R.drawable.background_square_border_grey)
                  binding.llPrint.setTextColor(resources.getColor(R.color.txtColor))
  */
                type = "Email"

                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.VISIBLE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                binding.tvMessage.setText(getString(R.string.please_enter_customer_email_address))
                MethodUtils.hideKeyboard(requireActivity())
            }

            R.id.llNoReceipt -> {
                binding.llNoReceipt.background = resources.getDrawable(R.drawable.button_selected)
                binding.llNoReceipt.setTextColor(resources.getColor(R.color.white))

                binding.llEmail.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llEmail.setTextColor(resources.getColor(R.color.txtColor))

                binding.llMessage.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llMessage.setTextColor(resources.getColor(R.color.txtColor))
                /*
                                binding.llPrint.background =
                                    resources.getDrawable(R.drawable.background_square_border_grey)
                                binding.llPrint.setTextColor(resources.getColor(R.color.txtColor))*/
                moveToDashboard()
            }

            R.id.llPrint -> {
                //removeCustomer()
                /*  binding.llPrint.background = resources.getDrawable(R.drawable.button_selected)
                  binding.llPrint.setTextColor(resources.getColor(R.color.white))*/

                isNotPrinted = true
                isPrintCustomer = true

                binding.llEmail.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llEmail.setTextColor(resources.getColor(R.color.txtColor))

                binding.llMessage.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llMessage.setTextColor(resources.getColor(R.color.txtColor))

                binding.llNoReceipt.background =
                    resources.getDrawable(R.drawable.background_square_border_grey)
                binding.llNoReceipt.setTextColor(resources.getColor(R.color.txtColor))

                if (isDineIn) {
                    LogUtil.logE(TAG, "receiptModel:  ${Gson().toJson(receiptModel)}")
                    customerPrintWholeOrder(false)

                } else {
                    getCustomerPrinters(false)
                }

                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }

            R.id.txtSend -> {
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt, R.id.txtSend"))

                MethodUtils.hideKeyboard(requireActivity())
                viewModel.submit(
                    type,
                    binding.edtEmail.text.toString().trim(),
                    binding.edtPhoneNo.text.toString().trim(),
                    orderID,
                    IS_GIFT_CARD_TYPE
                )


            }

            R.id.imgBack -> {
                backpress()
            }
        }
    }

    private fun moveToNextPayment() {
        if (isDineIn) {
            val navController = findNavController()
            var bundle = Bundle()
            bundle.putBoolean("isNextPayment", true)
            bundle.putInt("splitvalue", splitValue)
            bundle.putInt("orderId", orderID)
            bundle.putDouble("splitPaidAmount", paymentAmount)
            bundle.putDouble("remainingAmount", remainingAmount)
            bundle.putBoolean("isSplitByNo", isSplitByNo)
            bundle.putBoolean("isSplitByAmount", isSplitByAmount)
            bundle.putBoolean("isCustomCash", isCustomCash)
            bundle.putParcelable(PRINT_DATA_DINE_IN, getDineInOrderDetails)
            bundle.putParcelableArrayList(DINE_IN_ADAPTER_LIST, dineInList)
            bundle.putDouble("subTotalPrice", subTotalWT)
            bundle.putDouble("totalServiceCharge", serviceCharge)
            bundle.putDouble("totalDiscount", totalDiscount)
            bundle.putDouble("divideCashDiscount", totalDiscount)
            bundle.putDouble("totalTax", totalTaxAmount)
            prefProvider.setValueInt(PAYMENT_ID, 0)
            if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            }

        } else {
            val navController = findNavController()
            var bundle = Bundle()
            bundle.putBoolean("isNextPayment", true)
            bundle.putDouble("splitPaidAmount", paymentAmount)
            bundle.putDouble("remainingAmount", remainingAmount)
            if (isSplitByAmount) {
                bundle.putInt("splitvalue", splitValue)
                bundle.putBoolean("isSplitByNo", isSplitByNo)
                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
            } else {
                bundle.putInt("splitvalue", splitValue)
                bundle.putBoolean("isSplitByNo", isSplitByNo)
                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
            }
            bundle.putBoolean("isCustomCash", isCustomCash)
            LogUtil.logE(TAG, "ORDER_ID:  ${prefProvider.getValueInt("ORDER_ID", -1)}")
            //  saveDataInPrefrences()
            prefProvider.setValueInt(PAYMENT_ID, 0)
            if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            }
        }
    }

    private fun customerPrintWholeOrder(shouldCheckForAutoPrinting: Boolean) {
        isPrint = false
        isPrintCustomer = false

        var guestPos = requireArguments().getInt(GUEST_POSITION)
        LogUtil.logE(TAG, "getGuestPosition  ${guestPos}")

        var listItem: java.util.ArrayList<TbCartItem> = arrayListOf()
        var listItemWT: java.util.ArrayList<TbCartItem> = arrayListOf()
        for (i in 1 until dineInList.size) {
            if (dineInList.get(i).isHeader == 1) {
                dineInList.get(i).item?.let { it1 -> listItemWT.add(it1) }
            } else {
                break
            }
        }
        for (i in guestPos + 1 until dineInList.size) {
            if (dineInList.get(i).isHeader == 1) {

                dineInList[i].item?.let { it1 -> listItem.add(it1) }
            } else {
                break;
            }

        }
        customerPrinterDineIn?.forEach { cpd ->
            if (cpd.status) {

                if (shouldCheckForAutoPrinting && !isGuest) {
                    cpd.orderTypes.forEach {
                        if (it.orderTypeId == receiptModel?.order?.orderTypeId) {
                            it.printerSettings.forEach {
                                if (it.printType.lowercase() == CUSTOMER.lowercase() && it.autoPrinting
                                ) {
                                    initDineInPrinter(
                                        cpd,
                                        Constants.CUSTOMER,
                                        paymentType,
                                        true,
                                        listGuestItem = listItem,
                                        dineInList.get(guestPos).title.toString(),
                                        listItemWT
                                    )
                                }
                            }
                        }
                    }
                } else {
                    initDineInPrinter(
                        cpd,
                        Constants.CUSTOMER,
                        paymentType,
                        true,
                        listGuestItem = listItem,
                        dineInList.get(guestPos).title.toString(),
                        listItemWT
                    )
                }

            }

        }
    }

    private fun initDineInPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        guestPrint: Boolean,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {

        if (customerReceiptPrinters.name.startsWith("CloudPrint", true)) {

            if (isGuest) {

                val checkOutDineInModel =
                    requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)
                LogUtil.logE(
                    TAG,
                    "checkOutDineInModel:  ${Gson().toJson(checkOutDineInModel)}"
                )


                if (checkOutDineInModel != null) {
                    generateGuestPrint(
                        customerReceiptPrinters,
                        type,
                        paymentType,
                        listGuestItem,
                        guestName,
                        listWTitems,
                        checkOutDineInModel
                    )
                }

            } else {


                generateDineInPrint(customerReceiptPrinters, type, "")
            }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            if (isGuest) {

                val checkOutDineInModel =
                    requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)
                LogUtil.logE(
                    TAG,
                    "checkOutDineInModel:  ${Gson().toJson(checkOutDineInModel)}"
                )


                if (checkOutDineInModel != null) {
                    generateGuestPrint(
                        customerReceiptPrinters,
                        type,
                        paymentType,
                        listGuestItem,
                        guestName,
                        listWTitems,
                        checkOutDineInModel
                    )
                }

            } else {


                generateDineInPrint(customerReceiptPrinters, type, "")
            }

        } else {

            PrinterClass.closePrinter()
            if (PrinterClass.getPrinter() == null) {
                var printer: Print? = Print(requireContext())
                if (printer != null) {
//                printer.setStatusChangeEventCallback(this)
//                printer.setBatteryStatusChangeEventCallback(this)
                }

                val enabled = Print.FALSE

                try {
                    var interval: Int = 1000
                    if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                        interval = PrinterClass.BLUETOOTH_TIMEOUT
                    }
                    printer?.openPrinter(

                        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                            Print.DEVTYPE_BLUETOOTH
                        } else {
                            Print.DEVTYPE_TCP
                        },
                        customerReceiptPrinters.ipAddress,
                        enabled,
                        interval
                    )
                    //printer?.setStatusChangeEventCallback(this)

                } catch (e: Exception) {
                    LogUtil.logE(TAG, "PrinterException: " + e.message)
                    printer = null
                    return
                }
                try {

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)
                        if (isGuest) {

                            val checkOutDineInModel =
                                requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)


                            if (checkOutDineInModel != null) {
                                generateGuestPrint(
                                    customerReceiptPrinters,
                                    type,
                                    paymentType,
                                    listGuestItem,
                                    guestName,
                                    listWTitems,
                                    checkOutDineInModel
                                )
                            }

                        } else {


                            generateDineInPrint(customerReceiptPrinters, type, "")
                        }

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                LogUtil.logE(TAG, "PrinterIsNotNull:")
            }
        }

    }

    private fun generateGuestPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        listWTitems: ArrayList<TbCartItem>,
        checkOutDineInModel: GuestDataModel?,

        ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0
        var orderDiscount = 0.0

        val guestCount = dineInList.size - 1

        listGuestItem.forEach {
            guestSubTotal += (it.price * it.itemQuantity) - it.discountPrice


            it.taxes?.forEach { tax ->
                if (tax.isActive) {

                    guestTaxes += if (tax.taxType == "Percentage") {

                        var modifierPrice = 0.0
                        val price =
                            (it.price * it.itemQuantity) - it.discountPrice

                        it.modifiers.forEach {
                            modifierPrice += (it.price * it.itemQuantity)
                        }

                        val totalPrice = price + modifierPrice

                        val itemTaxPrice =
                            (tax.rate * totalPrice) / 100

                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    } else {

                        String.format("%.2f", tax.rate * it.itemQuantity)
                            .toDouble()
                    }
                }

            }
            guestDiscount += it.discountPrice

        }

        orderDiscount = (getDineInOrderDetails?.totalDiscount?.minus(guestDiscount))?.div(
            ((getDineInOrderDetails?.guestAttributes?.size!! - 1))
        )!!
        var finaldisLocal = 0.0
        finaldisLocal = orderDiscount + guestDiscount


        if (prefProvider.getValueboolean(SERVICECHARGE_DINEIN_ORDER, false)) {
            var isApplied = false
            dineInList.get(0).serviceChargeList?.forEach {
                if (it.order_type == SERVICECHARGE_DINEIN_ORDER) {
                    if (isInRange(it.min_guest_count!!, it.max_guest_count!!, guestCount)) {
                        guestServiceCharge += (guestSubTotal * it.percentage) / 100
                        isApplied = true
                        return@forEach
                    }
                }
            }
            if (!isApplied) {
                dineInList.get(0).serviceChargeList?.forEach { service ->
                    if (service.id == checkMaxGuestCountId(dineInList.get(0).serviceChargeList!!)) {
                        guestServiceCharge += (guestSubTotal * service.percentage) / 100
                        return@forEach
                    }
                }
            }

        }



        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {


            customerReceiptPrinters.ipAddress?.let {
                sunmiPrinterInit(
                    it, paymentType,
                    listWTitems,
                    listGuestItem,
                    guestName,
                    finaldisLocal,
                    checkOutDineInModel,
                    orderDiscount,
                    guestTaxes,
                    guestSubTotal,
                    guestServiceCharge
                )
            }


        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {
            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            setService(
                paymentType,
                listWTitems,
                listGuestItem,
                guestName,
                finaldisLocal,
                checkOutDineInModel,
                orderDiscount,
                guestTaxes,
                guestSubTotal,
                guestServiceCharge
            )

        } else {


            var builder: Builder? = null
            try {
                builder =
                    Builder(
                        if (customerReceiptPrinters.name.substring(0, 6).toString()
                                .lowercase() == "TM-m30".lowercase()
                        ) {
                            "TM-m30"
                        } else {
                            customerReceiptPrinters.name
                        }, PrinterClass.language, requireActivity()
                    )


                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(2, 2)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                        builder.addText("OrderID:" + getDineInOrderDetails?.custom_order_id)
                    } else {
                        builder.addText("OrderID:" + getDineInOrderDetails?.id)
                    }
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(2)
                }

                if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                        .isNotEmpty()
                ) {
                    builder.addFeedLine(1)
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                    val decodedString: ByteArray = android.util.Base64.decode(
                        prefProvider.getValue(VENUE_LOGO, ""),
                        android.util.Base64.DEFAULT
                    )
                    val bitmap: Bitmap =
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                    builder.addImage(
                        newBitmap, 0, 0,
                        newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                        Builder.HALFTONE_DITHER, 1.0
                    )
                }


                if (paymentType.isNotEmpty()) {
                    builder.addTextFont(Builder.FONT_E)

                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(2, 2)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addText("Paid" + "\n")

                }


                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_NAME, "").toString()
                )
                if (customerSettingModel.showVenueAddress) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)

                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    addBuilderText(
                        builder,
                        prefProvider.getValue(Constants.BUSINESS_ADDRESS, "")
                            .toString()
                    )
                }
                if (customerSettingModel.showVenuePhone) {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    addBuilderText(
                        builder,
                        MethodUtils.formatPhoneNumber(
                            prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                        )
                    )
                }
                if (customerSettingModel.showWebsiteAddress) {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    addBuilderText(
                        builder,
                        prefProvider.getValue(BUSINESS_WEBSITE, "").toString()
                    )
                }

                if (customerSettingModel.showOrderType) {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)

                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(2, 2)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    builder.addText(getDineInOrderDetails?.orderTypeName + "\n")
                }

                if (customerSettingModel.fonts == LARGE) {


                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getDineInOrderDetails?.offlineId
                        }
                    )

                    if (customerSettingModel.showTeam) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText("Employee:" + getDineInOrderDetails?.employee?.name)

                    }

                    if (customerSettingModel.showOrderTime) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        )

                    }

                    if (customerSettingModel.showPrintTime) {


                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                            val current = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                            val formatted = current.format(formatter)

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )
                            builder.addText(
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    formatted
                                )
                            )
                        }


                    }
                } else {


                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                                "ENTJKOIJH8745"
                            } else {
                                getDineInOrderDetails?.offlineId
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    if (customerSettingModel.showTeam) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showTeam) {
                                    "Employee:" + getDineInOrderDetails?.employee?.name
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                    if (customerSettingModel.showOrderTime) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showOrderTime) {
                                    "Order Time:" + getReceiptFormatDateFromUTCServer(
                                        requireContext(),
                                        getDineInOrderDetails?.createdAt.toString()
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }

                    if (customerSettingModel.showPrintTime) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                            val current = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                            val formatted = current.format(formatter)
                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //  builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )


                            builder.addText(
                                padLine(
                                    if (customerSettingModel.showPrintTime) {
                                        "Print Time:" + getCurrentTimeFromTimeZone(
                                            requireContext(),
                                            formatted
                                        )
                                    } else {
                                        ""
                                    },
                                    "",
                                    if (customerSettingModel.fonts == LARGE) {
                                        24
                                    } else {
                                        48
                                    }
                                )
                            )

                        }
                    }
                }

                builder.addFeedLine(1)

                addHorizontalLine(builder)


                for (i in 0 until listWTitems.size) {
                    // builder.addFeedLine(1)
                    /* builder.addTextLineSpace(30)
                     builder.addFeedUnit(30)*/
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    addWholeTbItemToGuest(
                        builder,
                        listWTitems.get(i),
                        customerSettingModel.fonts,
                        customerSettingModel.showModifiers,
                        dineInList.get(0).totalGuestCount,
                        dineInList.get(0).serviceChargeList ?: arrayListOf(),
                        prefProvider
                    )
                }
                builder.addFeedLine(1)


                if (listGuestItem.isNotEmpty()) {
                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(guestName)


                    listGuestItem.forEach {
                        addOrderItemForDineIn(
                            builder,
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )

                    }
                    builder.addFeedLine(2)
                }

                if (finaldisLocal != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            "Total Discount",

                            if (finaldisLocal == 0.0) {
//                                "-$" + MethodUtils.roundOffAmountString(0.0)
                                "$" + MethodUtils.roundOffAmountString(0.0)
                            } else {

                                "-$" + MethodUtils.roundOffAmountString(
                                    checkOutDineInModel?.totalDiscount ?: 0.0
                                )

                            },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )


                builder.addText(
                    padLine(
                        "Sub Total",
                        "$" + MethodUtils.roundOffAmountString(
                            (checkOutDineInModel?.subTotal!!) - orderDiscount
                        ),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                if (guestTaxes != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            "Tax",
                            "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalTax),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if ((getDineInOrderDetails?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                        Constants.SERVICECHARGE_DINEIN_ORDER,
                        false
                    )
                ) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            "Service Charge",
                            "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalServiceCharge!!),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (tipAmount != 0.0) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Tips",
                            "$" + MethodUtils.roundOffAmountString(tipAmount),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (noCashAdjGlobal != 0.0 && customerSettingModel.showCashDisSurCharg) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    if (prefProvider.getValue(OPTION_TYPE, "")
                            .lowercase() == "CashDiscount".lowercase() && payTypeGlb == "Cash"
                    ) {

                        builder.addText(
                            padLine(
                                "Cash Discount",
                                if (noCashAdjGlobal == 0.0) {
                                    "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                                } else {
                                    "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                                },
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    } else if (prefProvider.getValue(OPTION_TYPE, "")
                            .lowercase() == "SurCharge".lowercase() && payTypeGlb == "Card"
                    ) {
                        builder.addText(
                            padLine(
                                Constants.SURCHARGE_TEXT,
                                if (noCashAdjGlobal == 0.0) {
                                    "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                                } else {
                                    "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                                },
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }
                }



                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)



                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                var totalAmt = checkOutDineInModel.totalAmount + tipAmount

                if (payTypeGlb.lowercase() == "Cash".lowercase() && prefProvider.getValue(
                        OPTION_TYPE,
                        ""
                    ).lowercase() == "CashDiscount".lowercase()
                ) {

                    totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
                } else if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                        OPTION_TYPE, ""
                    ).lowercase() == "SurCharge".lowercase()
                ) {
                    totalAmt = MethodUtils.roundOffAmountDouble(totalAmt + noCashAdjGlobal)
                }




                builder.addText(
                    padLine(
                        "Total Price",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Paid Amount",
                        "$" + MethodUtils.roundOffAmountString(
                            paidAmount + tipAmount
                        ),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //ADDCHANGE
                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )



                if (customerSettingModel.showRefundAmount) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                    }
                }



                if (customerSettingModel.showTipSuggestion) {
                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Additional Tips",
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )


                    builder.addFeedLine(1)

                    addHorizontalLine(builder)

                    if (tipsList.isNotEmpty()) {
                        addTipsList(
                            builder,
                            tipsList,

                            MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                            customerSettingModel.fonts
                        )

                    }
                }

                builder.addFeedLine(1)
                if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            "Transaction ID",
                            "" + receiptModel?.order?.payments?.size?.minus(1)
                                ?.let { receiptModel?.order?.payments?.get(it)?.id },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Transaction Type",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    /*builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )*/

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    var strCardType =
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType

                    if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                        var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                        var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                        strCardType =
                            paymentViewModel.extData.substring(
                                applabStartIndex + "<APPLAB>".length,
                                applabEndIndex
                            )
                    }

                    builder.addText(
                        padLine(
                            "",
                            strCardType,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }



                if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                    builder.addFeedLine(2)
                    builder.addTextFont(Builder.FONT_B)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(1, 1)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(getDineInOrderDetails?.note)
                }


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //customer signature line.
                builder.addText(
                    padLine(
                        "Customer Signature",
                        addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                if (customerSettingModel.showQrCode) {
                    builder.addFeedLine(1)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    val bitmap =
                        generateQRCode(getDineInOrderDetails?.digitalReceiptUrl.toString())

                    val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                    builder.addImage(
                        newBitmap, 0, 0,
                        newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                        Builder.HALFTONE_DITHER, 1.0
                    )
                }

                builder.addFeedLine(2)

                builder.addCut(Builder.CUT_FEED)

                val status = IntArray(1)
                val battery = IntArray(1)


                try {
                    if (paymentType.equals("Cash", true)) {
                        builder.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100)
                    }
                    PrinterClass.getPrinter()?.sendData(
                        builder,
                        PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                    )

                    PrinterClass.closePrinter()
                    // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
                } catch (e: Exception) {
                    PrinterClass.closePrinter()
                    e.printStackTrace()
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }


    private fun sunmiPrinterInit(
        ipAddress: String,
        paymentType: String,
        listWTitems: java.util.ArrayList<TbCartItem>,
        listGuestItem: java.util.ArrayList<TbCartItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {

        SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiBlueToothPrinter, ipAddress)


        connect(
            paymentType,
            listWTitems,
            listGuestItem,
            guestName,
            finaldisLocal,
            checkOutDineInModel,
            orderDiscount,
            guestTaxes,
            guestSubTotal,
            guestServiceCharge
        )


    }

    private fun connect(
        paymentType: String,
        listWTitems: java.util.ArrayList<TbCartItem>,
        listGuestItem: java.util.ArrayList<TbCartItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {

        if (!SunmiPrinterApi.getInstance().isConnected) {
            SunmiPrinterApi.getInstance()
                .connectPrinter(requireContext(), object : ConnectCallback {

                    override fun onFound() {
                        println("onFound")
                    }

                    override fun onUnfound() {
                        println("onUnfound")
                    }

                    override fun onConnect() {
                        println("onConnect")
                        printSunmiDinein(
                            paymentType,
                            listWTitems,
                            listGuestItem,
                            guestName,
                            finaldisLocal,
                            checkOutDineInModel,
                            orderDiscount,
                            guestTaxes,
                            guestSubTotal,
                            guestServiceCharge
                        )

                    }

                    override fun onDisconnect() {
                        println("onDisconnect")
                    }

                })
        } else {
            printSunmiDinein(
                paymentType,
                listWTitems,
                listGuestItem,
                guestName,
                finaldisLocal,
                checkOutDineInModel,
                orderDiscount,
                guestTaxes,
                guestSubTotal,
                guestServiceCharge
            )

        }
    }

    private fun printSunmiDinein(
        paymentType: String,
        listWTitems: ArrayList<TbCartItem>,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {
        try {
            PrintSunmiUtils.fontSize(customerSettingModel.fonts)
            SunmiPrinterApi.getInstance().printerInit()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getDineInOrderDetails?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getDineInOrderDetails?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                printBusinessLogo()

            }


            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.paymentType("Paid")
            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getDineInOrderDetails?.orderTypeName?.trim()
                    ?.let { PrintSunmiUtils.printOrderType(it) }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.receiptID(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + getDineInOrderDetails?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderTime(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getDineInOrderDetails?.createdAt.toString()
                        )
                    )
                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrintSunmiUtils.orderTime(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                val str = padLine(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    },
                    "",
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.orderId(str)

                if (customerSettingModel.showTeam) {

                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + getDineInOrderDetails?.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.employee(empName)

                }



                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.orderTime(orderTime)


                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)


                    }
                }

            }

            PrintSunmiUtils.addHorizontal()



            for (i in 0 until listWTitems.size) {

                addWholeTbItemToGuest(
                    listWTitems.get(i),
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers,
                    dineInList.get(0).totalGuestCount,
                    dineInList.get(0).serviceChargeList ?: arrayListOf()
                )
            }


            if (listGuestItem.isNotEmpty()) {

                PrintSunmiUtils.printTextCenter(guestName)


                listGuestItem.forEach {
                    addOrderItemForDineIn(
                        it,
                        customerSettingModel.fonts,
                        customerSettingModel.showModifiers
                    )

                }
            }
            SunmiPrinterApi.getInstance().lineWrap(2)

            if (finaldisLocal != null) {
                val str1 = padLine(
                    "Total Discount",

                    if (finaldisLocal == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(0.0)
                        "$" + MethodUtils.roundOffAmountString(0.0)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(
                            checkOutDineInModel?.totalDiscount ?: 0.0
                        )
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.totalDiscount(str1)
            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(
                    (checkOutDineInModel?.subTotal!!) - orderDiscount
                ), if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.subTotal(str2)



            if (guestTaxes != null) {

                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalTax),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.tax(str3)
            }

            if ((getDineInOrderDetails?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalServiceCharge!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.serviceCharge(str4)
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (tipAmount != 0.0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.tips(str8)
            }

            if (noCashAdjGlobal != 0.0 && customerSettingModel.showCashDisSurCharg) {

                if (prefProvider.getValue(OPTION_TYPE, "")
                        .lowercase() == "CashDiscount".lowercase() && payTypeGlb == "Cash"
                ) {

                    val str8 = padLine(
                        "Cash Discount",
                        if (noCashAdjGlobal == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        }, if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.cashDiscount(str8)


                } else if (prefProvider.getValue(OPTION_TYPE, "")
                        .lowercase() == "SurCharge".lowercase() && payTypeGlb == "Card"
                ) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        if (noCashAdjGlobal == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        },
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.cashDiscount(str8)

                }
            }


            var totalAmt = checkOutDineInModel.totalAmount + tipAmount

            if (payTypeGlb.lowercase() == "Cash".lowercase() && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "CashDiscount".lowercase()
            ) {

                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
            } else if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                    OPTION_TYPE, ""
                ).lowercase() == "SurCharge".lowercase()
            ) {
                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt + noCashAdjGlobal)
            }


            val str5 = padLine(
                "Total Price",
                "$" + MethodUtils.roundOffAmountString(totalAmt),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.totalPrice(str5)


            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    paidAmount + tipAmount
                ),
                if (customerSettingModel.fonts == LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.totalPrice(str6)

            //ADDCHANGE

            val str7 = padLine(
                "Change Amount",
                "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.changeAmount(str7)
            SunmiPrinterApi.getInstance().lineWrap(2)

            if (customerSettingModel.showRefundAmount) {

            }



            if (customerSettingModel.showTipSuggestion) {
                PrintSunmiUtils.additionalTips()

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        tipsList,
                        MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                        customerSettingModel.fonts
                    )

                }
            }
            if (receiptModel?.order?.payments?.isNotEmpty() == true) {

                val str10 = padLine(
                    "Transaction ID",
                    "" + receiptModel?.order?.payments?.size?.minus(1)
                        ?.let { receiptModel?.order?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.transactionId(str10)

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.transactionType(str11)
            }

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                /*PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )*/

                var strCardType =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType

                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    strCardType =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }

                PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        strCardType,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )
                PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )

            }

            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getDineInOrderDetails?.note!!)
            }
            SunmiPrinterApi.getInstance().lineWrap(2)
            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)

            if (customerSettingModel.showQrCode) {


                getDineInOrderDetails?.digitalReceiptUrl?.let {
                    LogUtil.logE(
                        "digitalReceiptUrl1",
                        it
                    )
                }


                getDineInOrderDetails?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCode(it) }
            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printSunmiDineinInner(
        paymentType: String,
        listWTitems: ArrayList<TbCartItem>,
        listGuestItem: ArrayList<TbCartItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {
        try {
            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)
            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + getDineInOrderDetails?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + getDineInOrderDetails?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))

            }


            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.headerText("Paid")
            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) {
                    prefProvider.getValue(BUSINESS_ADDRESS, "")
                } else "", prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                )
                /* if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                     BUSINESS_PHONE_NO,
                     ""
                 ) else ""*/
            )


            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showOrderType) {
                getDineInOrderDetails?.orderTypeName?.trim()?.let { PrintSunmiUtils.headerText(it) }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + getDineInOrderDetails?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getDineInOrderDetails?.createdAt.toString()
                        )
                    )
                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                val str = padLine(

                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    },
                    "",
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.normalText(str)

                if (customerSettingModel.showTeam) {

                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + getDineInOrderDetails?.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(empName)

                }



                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(orderTime)


                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)


                    }
                }

            }

            PrintSunmiUtils.addHorizontalInner()



            for (i in 0 until listWTitems.size) {

                addWholeTbItemToGuestInner(
                    listWTitems.get(i),
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers,
                    dineInList.get(0).totalGuestCount,
                    dineInList.get(0).serviceChargeList ?: arrayListOf()
                )
            }


            if (listGuestItem.isNotEmpty()) {

                PrintSunmiUtils.normalTextCenter(guestName)


                listGuestItem.forEach {
                    addOrderItemForDineInInner(
                        it,
                        customerSettingModel.fonts,
                        customerSettingModel.showModifiers
                    )

                }
            }
            SunmiPrintHelper.getInstance().lineWrap(2)

            if (finaldisLocal != null) {
                val str1 = padLine(
                    "Total Discount",

                    if (finaldisLocal == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(0.0)
                        "$" + MethodUtils.roundOffAmountString(0.0)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(
                            checkOutDineInModel?.totalDiscount ?: 0.0
                        )
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str1)
            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(
                    (checkOutDineInModel?.subTotal!!) - orderDiscount
                ), if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.normalText(str2)



            if (guestTaxes != null) {

                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalTax),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str3)
            }

            if (getDineInOrderDetails?.serviceChargeEnabled == true && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalServiceCharge!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.normalText(str4)
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (tipAmount != 0.0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str8)
            }

            if (noCashAdjGlobal != 0.0 && customerSettingModel.showCashDisSurCharg) {

                if (prefProvider.getValue(OPTION_TYPE, "")
                        .lowercase() == "CashDiscount".lowercase() && payTypeGlb == "Cash"
                ) {

                    val str8 = padLine(
                        "Cash Discount",
                        if (noCashAdjGlobal == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        }, if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(str8)


                } else if (prefProvider.getValue(OPTION_TYPE, "")
                        .lowercase() == "SurCharge".lowercase() && payTypeGlb == "Card"
                ) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        if (noCashAdjGlobal == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                        },
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(str8)

                }
            }


            var totalAmt = checkOutDineInModel.totalAmount + tipAmount

            if (payTypeGlb.lowercase() == "Cash".lowercase() && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "CashDiscount".lowercase()
            ) {

                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
            } else if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                    OPTION_TYPE, ""
                ).lowercase() == "SurCharge".lowercase()
            ) {
                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt + noCashAdjGlobal)
            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            val str5 = padLine(
                "Total Price",
                "$" + MethodUtils.roundOffAmountString(totalAmt),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.boldText(str5)


            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    paidAmount + tipAmount
                ),
                if (customerSettingModel.fonts == LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.boldText(str6)

            //ADDCHANGE

            val str7 = padLine(
                "Change Amount",
                "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.boldText(str7)
            SunmiPrintHelper.getInstance().lineWrap(2)

            if (customerSettingModel.showRefundAmount) {

            }



            if (customerSettingModel.showTipSuggestion) {
                PrintSunmiUtils.additionalTipsInner()

                if (tipsList.isNotEmpty()) {
                    addTipsListInner(
                        tipsList,
                        MethodUtils.roundOffAmountDouble(guestSubTotal + guestServiceCharge + guestTaxes),
                        customerSettingModel.fonts
                    )

                }
            }
            if (receiptModel?.order?.payments?.isNotEmpty() == true) {

                val str10 = padLine(
                    "Transaction ID",
                    "" + receiptModel?.order?.payments?.size?.minus(1)
                        ?.let { receiptModel?.order?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str10)

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str11)
            }

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                /*PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )*/

                var strCardType =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType

                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    strCardType =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }

                PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        strCardType,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )
                PrintSunmiUtils.normalText(
                    padLine(
                        "",
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber,
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )

            }




            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(getDineInOrderDetails?.note!!)
            }


            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.showQrCode) {


                SunmiPrintHelper.getInstance().lineWrap(1)
                getDineInOrderDetails?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCodeInner(it) }
            }

            if (paymentType.equals("Cash", true)) {
                val aa = ByteArray(5)

                aa[0] = 0x10
                aa[1] = 0x14
                aa[2] = 0x00
                aa[3] = 0x00
                aa[4] = 0x00


                try {
                    SunmiPrinterApi.getInstance().sendRawData(aa)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                try {
                    SunmiPrintHelper.getInstance().openCashBox()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            PrintSunmiUtils.cutPaperInner()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
        return (minn <= value && value <= maxx)
    }

    fun checkMaxGuestCountId(serviceChargeList: ArrayList<TbServiceCharge>): Int {
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

    private fun generateDineInPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        s: String
    ) {

        if (customerReceiptPrinters.name.startsWith("CloudPrint", true)) {


            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, customerReceiptPrinters.ipAddress)


            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound 1")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            printDineInTable1()
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                printDineInTable1()
            }


        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {
            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())

            viewLifecycleOwner.lifecycleScope.launch {
                delay(200)
                setService()
            }


        } else {
            var builder: Builder? = null
            try {
                builder =
                    Builder(
                        if (customerReceiptPrinters.name.substring(0, 6).toString()
                                .lowercase() == "TM-m30".lowercase()
                        ) {
                            "TM-m30"
                        } else {
                            customerReceiptPrinters.name
                        }, PrinterClass.language, requireActivity()
                    )

                LogUtil.logE(TAG, "receiptModelDineinData  ${Gson().toJson(receiptModel)}")

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(2, 2)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                        builder.addText("OrderID:" + getDineInOrderDetails?.custom_order_id)
                    } else {
                        builder.addText("OrderID:" + getDineInOrderDetails?.id)
                    }
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(2)
                }

                if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                        .isNotEmpty()
                ) {
                    builder.addFeedLine(1)
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                    val decodedString: ByteArray = android.util.Base64.decode(
                        prefProvider.getValue(VENUE_LOGO, ""),
                        android.util.Base64.DEFAULT
                    )
                    val bitmap: Bitmap =
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                    builder.addImage(
                        newBitmap, 0, 0,
                        newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                        Builder.HALFTONE_DITHER, 1.0
                    )
                }

                if (paymentType.isNotEmpty()) {
                    builder.addTextFont(Builder.FONT_E)

                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(2, 2)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addText(paymentType + "\n")

                }
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText("Paid")
                builder.addFeedLine(1)


                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)


                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_NAME, "").toString()
                )
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_ADDRESS, "7450 DW 51 FH,AT,Suite 503")
                        .toString()
                )
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.formatPhoneNumber(
                        prefProvider.getValue(
                            Constants.BUSINESS_PHONE_NO,
                            ""
                        ).toString()
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                builder.addText(getDineInOrderDetails?.orderTypeName + "\n")

                if (customerSettingModel.fonts == LARGE) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getDineInOrderDetails?.offlineId
                        }
                    )

                    if (customerSettingModel.showTeam) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText("Employee:" + getDineInOrderDetails?.employee?.name)

                    }

                    if (customerSettingModel.showOrderTime) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        )

                    }

                    if (customerSettingModel.showPrintTime) {


                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                            val current = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                            val formatted = current.format(formatter)

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )
                            builder.addText(
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    formatted
                                )
                            )
                        }


                    }
                } else {


                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(

                            "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                                "ENTJKOIJH8745"
                            } else {
                                getDineInOrderDetails?.offlineId
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    if (customerSettingModel.showTeam) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showTeam) {
                                    "Employee:" + getDineInOrderDetails?.employee?.name
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                    if (customerSettingModel.showOrderTime) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showOrderTime) {
                                    "Order Time:" + getReceiptFormatDateFromUTCServer(
                                        requireContext(),
                                        getDineInOrderDetails?.createdAt.toString()
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }

                    if (customerSettingModel.showPrintTime) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                            val current = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                            val formatted = current.format(formatter)
                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //  builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )


                            builder.addText(
                                padLine(
                                    if (customerSettingModel.showPrintTime) {
                                        "Print Time:" + getCurrentTimeFromTimeZone(
                                            requireContext(),
                                            formatted
                                        )
                                    } else {
                                        ""
                                    },
                                    "",
                                    if (customerSettingModel.fonts == LARGE) {
                                        24
                                    } else {
                                        48
                                    }
                                )
                            )

                        }
                    }
                }

                builder.addFeedLine(1)

                addHorizontalLine(builder)



                for (i in 0 until dineInList?.size) {
                    if (dineInList[i].isHeader == 0) {

                        if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {

                            builder.addFeedLine(1)
                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            // builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )
                            if (dineInList[i]?.customer == null) {
                                builder.addText(dineInList[i]?.title)
                            } else {
                                builder.addText(
                                    dineInList[i]?.customer?.first_name + " " +
                                            if (dineInList[i]?.customer?.last_name != null) {
                                                dineInList[i].customer?.last_name
                                            } else {
                                                ""
                                            }
                                )
                            }
                        }


                    } else {
                        dineInList.get(i).item?.let {
                            addOrderItemForDineIn(
                                builder,
                                it,
                                customerSettingModel.fonts,
                                customerSettingModel.showModifiers
                            )
                        }

                    }


                }



                builder.addFeedLine(2)

                if (getDineInOrderDetails?.totalDiscount != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Total Discount",

                            if (getDineInOrderDetails?.totalDiscount == 0.0) {
//                                "-$" + MethodUtils.roundOffAmountString(0.0)
                                "$" + MethodUtils.roundOffAmountString(0.0)
                            } else {
                                getDineInOrderDetails?.totalDiscount?.let {
                                    "-$" + MethodUtils.roundOffAmountString(it)
                                }
                            },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Sub Total",
                        "$" + MethodUtils.roundOffAmountString(
                            getDineInOrderDetails?.subTotal ?: 0.0
                        ),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                if (totalTaxAmount != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Tax",
                            "$" + MethodUtils.roundOffAmountString(
                                getDineInOrderDetails?.totalTaxAmount ?: 0.0
                            ),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if ((getDineInOrderDetails?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                        Constants.SERVICECHARGE_DINEIN_ORDER,
                        false
                    )
                ) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Service Charge",
                            "$" + MethodUtils.roundOffAmountString(
                                getDineInOrderDetails?.totalServiceCharges ?: 0.0
                            ),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }


                if (tipAmount != 0.0) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Tips",
                            "$" + MethodUtils.roundOffAmountString(tipAmount),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }




                if (noCashAdjGlobal != 0.0 && payTypeGlb == "Cash" && prefProvider.getValue(
                        OPTION_TYPE,
                        ""
                    )
                        .lowercase() == "CashDiscount".lowercase() && customerSettingModel.showCashDisSurCharg
                ) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Cash Discount",
                            if (noCashAdjGlobal == 0.0) {
                                "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                            } else {
                                "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                            },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                } else if (payTypeGlb.lowercase() == "Card".lowercase() && customerSettingModel.showCashDisSurCharg) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            "" + MethodUtils.roundOffAmount(
                                receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size!! - 1
                                )?.cash_discount_or_surcharge!!
                            ),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )


                }



                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                var totalAmt =
                    MethodUtils.roundOffAmountDouble(
                        (getDineInOrderDetails?.totalAmount)
                    )

                if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                        OPTION_TYPE,
                        ""
                    ).lowercase() == "SurCharge".lowercase()
                ) {
                    totalAmt += receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cash_discount_or_surcharge!!
                }

                if (payTypeGlb == "Cash" && prefProvider.getValue(
                        OPTION_TYPE,
                        ""
                    ).lowercase() == "CashDiscount".lowercase()
                ) {

                    totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
                }


                builder.addText(
                    padLine(
                        "Total Price",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Paid Amount",
                        "$" + MethodUtils.roundOffAmountString(
                            paidAmount + tipAmount
                        ),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )



                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //ADDCHANGE

                if (remainingAmount == 0.0) {
                    changeAmtGlobal += tipAmount
                }
                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (isSpilt) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Remaining Amount",
                            "$" + MethodUtils.roundOffAmountString(remainingAmount),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }



                if (customerSettingModel.showRefundAmount) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                        /*builder.addText(
                            padLine(
                                "Change Amount",
                                "$" + MethodUtils.roundOffAmountString(
                                    (getDineInOrderDetails?.payments?.get(
                                        0
                                    )?.amount!! - (getDineInOrderDetails?.totalAmount!!)
                                            )
                                ),
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    24
                                } else {
                                    48
                                }

                            )
                        )*/
                    }
                }

                if (getDineInOrderDetails?.totalTips == 0.0) {
                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    var tip = ""

                    if (getDineInOrderDetails?.totalTips != 0.0) {
                        tip = getDineInOrderDetails?.totalTips.toString()
                    }
                    builder.addText(
                        padLine(
                            "Tips",
                            if (customerSettingModel.showTipLineForCash) {
                                "_____________"
                            } else {
                                ""
                            },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                    builder.addText(
                        padLine(
                            "Total",
                            if (customerSettingModel.showTipLineForCash) {
                                "_____________"
                            } else {
                                ""
                            },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }


                if (customerSettingModel.showTipSuggestion) {
                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Additional Tips",
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )


                    builder.addFeedLine(1)



                    addHorizontalLine(builder)

                    if (tipsList.isNotEmpty()) {
                        addTipsList(
                            builder,
                            tipsList,
                            receiptModel?.order?.totalAmount?.toDouble() ?: 0.0,
                            customerSettingModel.fonts
                        )

                    }
                }

                builder.addFeedLine(1)
                if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            "Transaction ID",
                            "" + receiptModel?.order?.payments?.size?.minus(1)
                                ?.let { receiptModel?.order?.payments?.get(it)?.id },
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Transaction Type",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    /*builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )*/

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    var strCardType =
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType

                    if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                        var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                        var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                        strCardType =
                            paymentViewModel.extData.substring(
                                applabStartIndex + "<APPLAB>".length,
                                applabEndIndex
                            )
                    }

                    builder.addText(
                        padLine(
                            "",
                            strCardType,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                    builder.addFeedLine(2)
                    builder.addTextFont(Builder.FONT_B)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(1, 1)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(getDineInOrderDetails?.note)
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //customer signature line.
                builder.addText(
                    padLine(
                        "Customer Signature",
                        addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                if (customerSettingModel.showQrCode) {
                    builder.addFeedLine(1)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    val bitmap =
                        generateQRCode(getDineInOrderDetails?.digitalReceiptUrl.toString())

                    val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                    builder.addImage(
                        newBitmap, 0, 0,
                        newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                        Builder.HALFTONE_DITHER, 1.0
                    )
                }

                builder.addFeedLine(2)

                builder.addCut(Builder.CUT_FEED)

                val status = IntArray(1)
                val battery = IntArray(1)


                try {

                    if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType.equals(
                            "Cash",
                            true
                        )
                    ) {
                        builder.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100)
                    }

                    PrinterClass.getPrinter()?.sendData(
                        builder,
                        if (customerReceiptPrinters.name.substring(0, 6).toString()
                                .lowercase() == "TM-m30".lowercase() || customerReceiptPrinters.name.substring(
                                0,
                                6
                            ).toString().lowercase() == "TM-m10".lowercase()
                        ) {
                            PrinterClass.BLUETOOTH_TIMEOUT
                        } else {
                            PrinterClass.SEND_TIMEOUT

                        }, status, battery
                    )

                    PrinterClass.closePrinter()

                    //findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
                } catch (e: Exception) {
                    PrinterClass.closePrinter()
                    e.printStackTrace()
                    LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    private fun printDineInTable1() {
        try {
            PrintSunmiUtils.fontSize(customerSettingModel.fonts)
            SunmiPrinterApi.getInstance().printerInit()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getDineInOrderDetails?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + getDineInOrderDetails?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                printBusinessLogo()

            }

            if (paymentType.isNotEmpty()) {
                PrintSunmiUtils.paymentType(paymentType)
            }
            PrintSunmiUtils.paymentType("Paid")

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getDineInOrderDetails?.orderTypeName?.trim()
                    ?.let { PrintSunmiUtils.printOrderType(it) }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.receiptID(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    }
                )


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + getDineInOrderDetails?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderTime(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getDineInOrderDetails?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.orderTime(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                val str = padLine(

                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    },
                    "",
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString().trim()

                PrintSunmiUtils.orderId(str)

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + getDineInOrderDetails?.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.employee(empName)

                }
                if (customerSettingModel.showOrderTime) {

                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()


                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontal()

            SunmiPrinterApi.getInstance().lineWrap(1)

            for (i in 0 until dineInList?.size) {
                if (dineInList[i].isHeader == 0) {

                    if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {


                        if (dineInList[i]?.customer == null) {

                            dineInList[i]?.title?.let { PrintSunmiUtils.printTextCenter(it) }

                        } else {

//                            dineInList[i].customer?.first_name + " " +
//                                    if (dineInList[i].customer?.last_name != null) {
//                                        dineInList[i].customer?.last_name
//                                    } else {
//                                        ""
//                                    }?.let { PrintSunmiUtils.printTextCenter(it) }

                            dineInList[i].customer?.let {
                                PrintSunmiUtils.printTextCenter(it.first_name.toString() + " " + it.last_name.toString())
                            }
                        }
                    }


                } else {
                    dineInList.get(i).item?.let {
                        addOrderItemForDineIn(
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )
                    }

                    SunmiPrinterApi.getInstance().lineWrap(1)
                }


            }

            SunmiPrinterApi.getInstance().lineWrap(2)

            if (getDineInOrderDetails?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (getDineInOrderDetails?.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(0.0)
                        "$" + MethodUtils.roundOffAmountString(0.0)
                    } else {
                        getDineInOrderDetails?.totalDiscount?.let {
                            "-$" + MethodUtils.roundOffAmountString(it)
                        }
                    },
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.totalDiscount(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(
                    getDineInOrderDetails?.subTotal ?: 0.0
                ),
                if (customerSettingModel.fonts == LARGE) {
                    23
                } else {
                    48
                }
            ).toString()


            PrintSunmiUtils.subTotal(str2)


            if (totalTaxAmount != null) {

                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(
                        getDineInOrderDetails?.totalTaxAmount ?: 0.0
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }

                ).toString()
                PrintSunmiUtils.tax(str3)
            }

            if ((getDineInOrderDetails?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(
                        getDineInOrderDetails?.totalServiceCharges ?: 0.0
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.serviceCharge(str4)
            }


            if (tipAmount != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.tips(str8)
            }




            if (noCashAdjGlobal != 0.0 && payTypeGlb == "Cash" && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                )
                    .lowercase() == "CashDiscount".lowercase() && customerSettingModel.showCashDisSurCharg
            ) {


                val str8 = padLine(
                    "Cash Discount",
                    if (noCashAdjGlobal == 0.0) {
                        "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                    },
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.cashDiscount(str8)


            } else if (payTypeGlb.lowercase() == "Card".lowercase() && customerSettingModel.showCashDisSurCharg) {


                val str8 = padLine(
                    Constants.SURCHARGE_TEXT,
                    "" + MethodUtils.roundOffAmount(
                        receiptModel?.order?.payments?.get(
                            receiptModel?.order?.payments?.size!! - 1
                        )?.cash_discount_or_surcharge!!
                    ),
                    if (customerSettingModel.fonts == LARGE) 23 else 48

                ).toString()

                PrintSunmiUtils.surCharge(str8)
                SunmiPrinterApi.getInstance().lineWrap(1)

            }


            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    (getDineInOrderDetails?.totalAmount)
                )

            if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "SurCharge".lowercase()
            ) {
                totalAmt += receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cash_discount_or_surcharge!!
            }

            if (payTypeGlb == "Cash" && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "CashDiscount".lowercase()
            ) {

                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
            }


            val str5 = padLine(
                "Total Price",
                "$" + MethodUtils.roundOffAmountString(totalAmt),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.totalPrice(str5)


            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    paidAmount + tipAmount
                ),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.totalPrice(str6)


            //ADDCHANGE

            val str7 = padLine(
                "Change Amount",
                "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.changeAmount(str7)

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (isSpilt) {

                val str7 = padLine(
                    "Remaining Amount",
                    "$" + MethodUtils.roundOffAmountString(remainingAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.changeAmount(str7)

                SunmiPrinterApi.getInstance().lineWrap(2)
            }



            if (customerSettingModel.showRefundAmount) {

                if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                }
            }

            if (getDineInOrderDetails?.totalTips == 0.0) {


                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == LARGE) {
                        PrintSunmiUtils.tips("Tips      _____________")
                        PrintSunmiUtils.tips("Total     _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
                        PrintSunmiUtils.tips("Total                             _____________")
                    }


                }

            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTips()

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        tipsList,
                        receiptModel?.order?.totalAmount?.toDouble() ?: 0.0,
                        customerSettingModel.fonts
                    )

                }
            }

            if (receiptModel?.order?.payments?.isNotEmpty() == true) {


                val str10 = padLine(
                    "Transaction ID",
                    "" + receiptModel?.order?.payments?.size?.minus(1)
                        ?.let { receiptModel?.order?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.transactionId(str10)

            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (receiptModel?.order?.payments?.isNotEmpty() == true) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.transactionType(str11)

            }

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {


                val str12 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName.toString()


                var str13 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()


                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    str13 =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }


                val str14 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString()

                PrintSunmiUtils.cardDetails(str12, str13, str14)
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getDineInOrderDetails?.note!!)
            }
//            SunmiPrinterApi.getInstance().lineWrap(2)
//            PrintSunmiUtils.tips("__________________________")
//            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().lineWrap(2)
            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)

            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCode(receiptModel?.order?.digital_receipt_url.toString())

                // getDineInOrderDetails?.digitalReceiptUrl.toString().let { PrintSunmiUtils.qrCode(it) }
            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun printDineInTable1Inner() {
        try {
            LogUtil.logE("printDineReciept", "Staring....")
            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)
            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + getDineInOrderDetails?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + getDineInOrderDetails?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))

            }

//            if (paymentType.isNotEmpty()) {
//                PrintSunmiUtils.headerText(paymentType)
//            }
//            PrintSunmiUtils.headerText("Paid")

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) {
                    prefProvider.getValue(BUSINESS_ADDRESS, "")
                } else "", prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                )
                /* if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                     BUSINESS_PHONE_NO,
                     ""
                 ) else ""*/
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                getDineInOrderDetails?.orderTypeName?.trim()?.let { PrintSunmiUtils.headerText(it) }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    }
                )


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + getDineInOrderDetails?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            getDineInOrderDetails?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                PrintSunmiUtils.normalText(
                    "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getDineInOrderDetails?.offlineId
                    }
                )

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + getDineInOrderDetails?.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(empName)

                }
                if (customerSettingModel.showOrderTime) {

                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                getDineInOrderDetails?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()


                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontalInner()

            SunmiPrintHelper.getInstance().lineWrap(1)

            for (i in 0 until dineInList?.size) {
                if (dineInList[i].isHeader == 0) {

                    if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {


                        if (dineInList[i]?.customer == null) {

                            dineInList[i]?.title?.let { PrintSunmiUtils.normalTextCenter(it) }

                        } else {

                            var customerName = dineInList[i]?.customer?.first_name + " "

                            if (dineInList[i]?.customer?.last_name != null) {
                                customerName += dineInList[i].customer?.last_name
                            }
                            PrintSunmiUtils.normalTextCenter(customerName)


                        }
                    }


                } else {
                    dineInList.get(i).item?.let {
                        addOrderItemForDineInInner(
                            it,
                            customerSettingModel.fonts,
                            customerSettingModel.showModifiers
                        )
                    }

                    SunmiPrintHelper.getInstance().lineWrap(1)
                }


            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (getDineInOrderDetails?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (getDineInOrderDetails?.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(0.0)
                        "$" + MethodUtils.roundOffAmountString(0.0)
                    } else {
                        getDineInOrderDetails?.totalDiscount?.let {
                            "-$" + MethodUtils.roundOffAmountString(it)
                        }
                    },
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(
                    getDineInOrderDetails?.subTotal ?: 0.0
                ),
                if (customerSettingModel.fonts == LARGE) {
                    23
                } else {
                    48
                }
            ).toString()


            PrintSunmiUtils.normalText(str2)


            if (totalTaxAmount != null) {

                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(
                        getDineInOrderDetails?.totalTaxAmount ?: 0.0
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }

                ).toString()
                PrintSunmiUtils.normalText(str3)
            }

            if ((getDineInOrderDetails?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_DINEIN_ORDER,
                    false
                )
            ) {
                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(
                        getDineInOrderDetails?.totalServiceCharges ?: 0.0
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str4)
            }


            if (tipAmount != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str8)
            }




            if (noCashAdjGlobal != 0.0 && payTypeGlb == "Cash" && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                )
                    .lowercase() == "CashDiscount".lowercase() && customerSettingModel.showCashDisSurCharg
            ) {


                val str8 = padLine(
                    "Cash Discount",
                    if (noCashAdjGlobal == 0.0) {
                        "$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(noCashAdjGlobal)
                    },
                    if (customerSettingModel.fonts == LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str8)


            } else if (payTypeGlb.lowercase() == "Card".lowercase() && customerSettingModel.showCashDisSurCharg) {


                val str8 = padLine(
                    Constants.SURCHARGE_TEXT,
                    "" + MethodUtils.roundOffAmount(
                        receiptModel?.order?.payments?.get(
                            receiptModel?.order?.payments?.size!! - 1
                        )?.cash_discount_or_surcharge!!
                    ),
                    if (customerSettingModel.fonts == LARGE) 23 else 48

                ).toString()

                PrintSunmiUtils.normalText(str8)
                SunmiPrintHelper.getInstance().lineWrap(1)

            }


            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    (getDineInOrderDetails?.totalAmount)
                )

            if (payTypeGlb.lowercase() == "Card".lowercase() && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "SurCharge".lowercase()
            ) {
                totalAmt += receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cash_discount_or_surcharge!!
            }

            if (payTypeGlb == "Cash" && prefProvider.getValue(
                    OPTION_TYPE,
                    ""
                ).lowercase() == "CashDiscount".lowercase()
            ) {

                totalAmt = MethodUtils.roundOffAmountDouble(totalAmt - noCashAdjGlobal)
            }


            SunmiPrintHelper.getInstance().lineWrap(1)
            val str5 = padLine(
                "Total Price",
                "$" + MethodUtils.roundOffAmountString(totalAmt),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str5)


            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    paidAmount + tipAmount
                ),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.boldText(str6)


            //ADDCHANGE

            val str7 = padLine(
                "Change Amount",
                "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str7)

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (isSpilt) {

                val str7 = padLine(
                    "Remaining Amount",
                    "$" + MethodUtils.roundOffAmountString(remainingAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.boldText(str7)

                SunmiPrintHelper.getInstance().lineWrap(2)
            }



            if (customerSettingModel.showRefundAmount) {

                if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                }
            }

            if (getDineInOrderDetails?.totalTips == 0.0) {


                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == LARGE) {
                        PrintSunmiUtils.boldText("Tips      _____________")
                        PrintSunmiUtils.boldText("Total     _____________")
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                        PrintSunmiUtils.boldText("Total                             _____________")
                    }


                }

            }
            LogUtil.logE("printDineReciept", "Staring 1....")

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTipsInner()

                if (tipsList.isNotEmpty()) {
                    addTipsListInner(
                        tipsList,
                        receiptModel?.order?.totalAmount?.toDouble() ?: 0.0,
                        customerSettingModel.fonts
                    )

                }
            }
            if (receiptModel?.order?.payments?.isNotEmpty() == true) {


                val str10 = padLine(
                    "Transaction ID",
                    "" + receiptModel?.order?.payments?.size?.minus(1)
                        ?.let { receiptModel?.order?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.normalText(str10)

            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (receiptModel?.order?.payments?.isNotEmpty() == true) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.normalTextTest(str11)

            }

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {
                /*val str12 = padLine(
                    "",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName.toString(),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                if (!str12.isNullOrBlank()) {
                    PrintSunmiUtils.normalTextTest(str12)
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }*/

                var strCardType =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()

                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    strCardType =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }

                val str13 = padLine(
                    "",
                    strCardType,
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                if (!str13.isNullOrBlank()) {
                    PrintSunmiUtils.normalTextTest(str13)
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
                val str14 = padLine(
                    "",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString(),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                if (!str14.isNullOrBlank()) {
                    PrintSunmiUtils.normalTextTest(str14)
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInner(getDineInOrderDetails?.note!!)
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
            PrintSunmiUtils.boldText("__________________________")
            SunmiPrintHelper.getInstance().lineWrap(1)

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.showQrCode) {

                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.qrCodeInner(receiptModel?.order?.digital_receipt_url.toString())

                // getDineInOrderDetails?.digitalReceiptUrl.toString().let { PrintSunmiUtils.qrCode(it) }
            }

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType.equals(
                    "Cash",
                    true
                )
            ) {
                val aa = ByteArray(5)

                aa[0] = 0x10
                aa[1] = 0x14
                aa[2] = 0x00
                aa[3] = 0x00
                aa[4] = 0x00


                try {
                    SunmiPrinterApi.getInstance().sendRawData(aa)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                try {
                    SunmiPrintHelper.getInstance().openCashBox()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            LogUtil.logE("printDineReciept", "Staring 2....")
            PrintSunmiUtils.cutPaperInner()

        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.logE("printDineReciept", "Staring error....")
        }
    }

    private fun moveToCheckOut() {
        if (isDineIn) {
            if (!isLastPayment) {
                val bundle = Bundle()
                LogUtil.logE(TAG, "guestorderID ${orderID}")
                bundle.putInt("orderId", orderID)
                if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                    findNavController().navigate(
                        R.id.action_orderCompleteFragment_to_dineInOrderTable,
                        bundle
                    )
                }
                removePrefrenceDinein()
            }
        }
    }

    override fun onStop() {
        super.onStop()
//         Runtime.getRuntime().gc()

        /*This if condition is added by Rahul to print the "Updated" text in the kitchen receipt when split payment is done*/
        if (!isSpilt) {
            prefProvider.setValue(Constants.OPEN_ORDER_ITEMS, "")
            /*Added By Rahul */
            prefProvider.setValue(Constants.OPEN_ORDER_ITEMS_OLD, "")

        }
        if (!isSpilt) {
            removeCustomer()
        }

        var current: String = prefProvider.getValue("GC_CALLING", "0")

        prefProvider.setValue("GC_CALLING", (current.toInt() + 1).toString())
        Log.d("Thread TrackingGC_CALLING", current)

        if (((prefProvider.getValue("GC_CALLING", "0").toInt()) % 4) == 0) {
            Log.d("Thread TrackingGC_CALLING_CALLED", current)
            System.gc()
            System.runFinalization()
        }

        if (((prefProvider.getValue("GC_CALLING", "0").toInt()) % 8) == 0) {
            Log.d("Thread TrackingGC_CALLING_RESTART", current)
            prefProvider.setValue("GC_CALLING", "0")
            restartTheApplication()
        }

    }


    private inline fun restartTheApplication() {
        val packageManager: PackageManager = context!!.packageManager
        val intent: Intent = packageManager.getLaunchIntentForPackage(context!!.packageName)!!
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        // Required for API 34 and later
        // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        // Required for API 34 and later
        // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        mainIntent.setPackage(context!!.packageName)
        context!!.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    private fun moveToDashboard() {
        prefProvider.setValueboolean(Constants.TIP_ADDED, false)
        prefProvider.deleteValue(Constants.DO_PRINT)

        if (isSpilt) {
            if (isDineIn) {
                val navController = findNavController()
                var bundle = Bundle()
                bundle.putBoolean("isNextPayment", true)
                bundle.putInt("splitvalue", splitValue)
                bundle.putInt("orderId", orderID)
                bundle.putDouble("splitPaidAmount", paymentAmount)
                bundle.putDouble("remainingAmount", remainingAmount)
                bundle.putBoolean("isSplitByNo", isSplitByNo)
                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                bundle.putBoolean("isCustomCash", isCustomCash)
                bundle.putParcelable(PRINT_DATA_DINE_IN, getDineInOrderDetails)
                bundle.putParcelableArrayList(DINE_IN_ADAPTER_LIST, dineInList)
                bundle.putDouble("subTotalPrice", subTotalWT)
                bundle.putDouble("totalServiceCharge", serviceCharge)
                bundle.putDouble("totalDiscount", totalDiscount)
                bundle.putDouble("divideCashDiscount", totalDiscount)
                bundle.putDouble("totalTax", totalTaxAmount)
                prefProvider.setValueInt(PAYMENT_ID, 0)
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            } else {
                val navController = findNavController()
                var bundle = Bundle()
                bundle.putBoolean("isNextPayment", true)
                bundle.putDouble("splitPaidAmount", paymentAmount)
                bundle.putDouble("remainingAmount", remainingAmount)
                if (isSplitByAmount) {
                    bundle.putInt("splitvalue", splitValue)
                    bundle.putBoolean("isSplitByNo", isSplitByNo)
                    bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                } else {
                    bundle.putInt("splitvalue", splitValue)
                    bundle.putBoolean("isSplitByNo", isSplitByNo)
                    bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                }
                bundle.putBoolean("isCustomCash", isCustomCash)
                //saveDataInPrefrences()
                prefProvider.setValueInt(PAYMENT_ID, 0)
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            }
        } else {
            dashboardViewModel.wholetotalPrice = 0.0
            if (isGuest) {
                if (!isLastPayment) {
                    val bundle = Bundle()
                    LogUtil.logE(TAG, "guestorderID ${orderID}")
                    bundle.putInt("orderId", orderID)

                    findNavController().navigate(
                        R.id.action_orderCompleteFragment_to_dineInOrderTable,
                        bundle
                    )
                } else {
                    removeCustomer()
                    removePrefrenceDinein()
                    // redirect to passcode
                    if (prefProvider.getValueboolean(LOCK_SCREEN_TRANSACTION, false)) {

                        clearObserver()
                        prefProvider.setValueboolean(ORDER_COMPLETED, true)
                        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")
                        if (viewModelDashBoard.boldPosNeedToRefresh)
                            findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                        else
                            findNavController().navigate(R.id.action_orderCompleteFragment_to_passcode)
                    } else {

                        clearObserver()
                        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    }

                }
            } else {
                if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                    removeCustomer()
                    removePrefrenceDinein()
                    // redirect to passcode
                    if (prefProvider.getValueboolean(LOCK_SCREEN_TRANSACTION, false)) {


                        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")

                        clearObserver()
                        prefProvider.setValueboolean(ORDER_COMPLETED, true)
                        if (viewModelDashBoard.boldPosNeedToRefresh)
                            findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                        else
                            findNavController().navigate(R.id.action_orderCompleteFragment_to_passcode)
                    } else {


                        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM, "")
                        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")

                        clearObserver()
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    }
                }
            }
        }
    }

    fun saveDataInPrefrences() {
        LogUtil.logE(TAG, "cartListORderCom:  ${Gson().toJson(cartList)}")
        var model = SplitBundleModel(
            true,
            splitValue,
            prefProvider.getValueInt("ORDER_ID", -1),
            0.0,
            prefProvider.getValue(WHOLE_AMOUNT, "0.0").toDouble(),
            isSplitByNo,
            isSplitByAmount,
            isCustomCash,
            getDineInOrderDetails,
            dineInList,
            prefProvider.getValue(Constants.SUB_TOTAL, "0.0").toDouble(),
            prefProvider.getValue(Constants.SERVICE_CHARGE, "0.0").toDouble(),
            prefProvider.getValue(Constants.TOTAL_DISCOUNT, "0.0").toDouble(),
            prefProvider.getValue(Constants.TAX_CHARGE, "0.0").toDouble(),
            prefProvider.getValue(Constants.TIP, "0.0").toDouble(),
            prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "0.0").toDouble(),
            cartList,
            redeemLoyaltyInfo
        )

        LogUtil.logE("SAVE_SPLIT_BUNDLE", Gson().toJson(model))
        prefProvider.setValue(SAVE_SPLIT_BUNDLE, Gson().toJson(model).toString())

    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner,
            object : Observer<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
                override fun onChanged(it: Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>?) {
                    when (it?.status) {
                        Status.SUCCESS -> {
                            ProgressUtils.dismissProgressDialog()
                            if (it.data != null && isPrint == true) {
                                isPrint = false

                                kitchenPrinterList = it.data


                                val remain = requireArguments().getDouble("remainingAmount")
                                if (requireArguments().getBoolean("isDineIn")) {
                                    customerPrintWholeOrder(true)
                                } else {
                                    getCustomerPrinters(true)
                                }




                                if (prefProvider.getValueboolean(
                                        IS_PRINTER_QUEUE_ENABLE,
                                        false
                                    ) == false
                                ) {
                                    if (!requireArguments().getBoolean("isSpilt")) {
                                        if (!requireArguments().getBoolean("isDineIn") && !requireArguments().getBoolean(
                                                "isFromActiveOrder"
                                            )
                                        ) {
                                            if (receiptModel?.order?.orderType == OPEN_ORDER
                                            ) {

                                                var noItem: Boolean = false

                                                if (prefProvider.getValueboolean(
                                                        OPEN_ORDER_UPDATE_FOR_PRINT,
                                                        false
                                                    ) == true
                                                ) {
                                                    var list = checkOrderItemsForOpenORderUpdate()
                                                    if (list.isEmpty()) {
                                                        noItem = true
                                                    }
                                                }

                                                if (kitchenPrinterList.isNotEmpty() && noItem == false) {
                                                    for (i in 0 until kitchenPrinterList.size) {
                                                        if (kitchenPrinterList[i].status) {
                                                            kitchenPrinterList[i].orderTypes.forEach {

                                                                if (it.orderTypeId == receiptModel?.order?.orderTypeId

                                                                ) {

                                                                    it.printerSettings.forEach {
                                                                        if ((it.printType.lowercase()
                                                                                .equals(KITCHEN.lowercase()) || it.printType.lowercase()
                                                                                .equals(
                                                                                    KITCHENANDCUSTOMER.lowercase()
                                                                                )) && it.autoPrinting
                                                                        ) {


                                                                            if (checkItemsforPrinter(
                                                                                    receiptModel?.order?.orderItems
                                                                                        ?: arrayListOf(),
                                                                                    kitchenPrinterList[i].printerCategories.toCollection(
                                                                                        arrayListOf()
                                                                                    )
                                                                                )
                                                                            ) {
                                                                                if (!prefProvider.getValueboolean(
                                                                                        Constants.NO_NEED_TO_PRINT,
                                                                                        false
                                                                                    )
                                                                                ) {
                                                                                    if (prefProvider.getValueboolean(
                                                                                            Constants.DO_PRINT,
                                                                                            false
                                                                                        ) || /*This is added to solve the custom item printing issue when "open order" is selected.*/ prefProvider.getValueboolean(
                                                                                            Constants.DO_PRINT_CUSTOM,
                                                                                            false
                                                                                        )
                                                                                    ) {
                                                                                        initKitchenPrinter(
                                                                                            kitchenPrinterList.get(
                                                                                                i
                                                                                            ),
                                                                                            KITCHEN
                                                                                        )
                                                                                    } else {
                                                                                        var oldCartModelString =
                                                                                            prefProvider.getValue(
                                                                                                "BEFORE_ORDER_NOTE",
                                                                                                ""
                                                                                            )
                                                                                        if (oldCartModelString.isNotEmpty()) {
                                                                                            var oldCartModel =
                                                                                                Gson().fromJson<CartModel>(
                                                                                                    oldCartModelString,
                                                                                                    CartModel::class.java
                                                                                                )
                                                                                            if (!oldCartModel.note.equals(
                                                                                                    cartList?.note
                                                                                                )
                                                                                            ) {
                                                                                                initKitchenPrinter(
                                                                                                    kitchenPrinterList.get(
                                                                                                        i
                                                                                                    ),
                                                                                                    KITCHEN
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                }
                                                                            }

                                                                        }
                                                                    }

                                                                }
                                                            }
                                                        }

                                                        /*IF A PROBLEM IS OCCURING WHEN "UPDATED" IS NOT GETTING PRINTED ON KITCHEN RECEIPT, THEN IT MAY BE BECAUSE THE TIME GIVEN BELOW */
                                                        if (kitchenPrinterList.size - 1 == i) {
                                                            Handler(Looper.getMainLooper()).postDelayed(
                                                                Runnable {
                                                                    isOrderUpdated = false
                                                                }, 4000
                                                            )
                                                        }

                                                    }
                                                } else {
                                                    for (i in 0 until kitchenPrinterList.size) {
                                                        if (prefProvider.getValueboolean(
                                                                Constants.DO_PRINT,
                                                                false
                                                            )
                                                        ) {
                                                            initKitchenPrinter(
                                                                kitchenPrinterList.get(
                                                                    i
                                                                ),
                                                                KITCHEN
                                                            )
                                                        }

                                                        /*IF A PROBLEM IS OCCURING WHEN "UPDATED" IS NOT GETTING PRINTED ON KITCHEN RECEIPT, THEN IT MAY BE BECAUSE THE TIME GIVEN BELOW */
                                                        if (kitchenPrinterList.size - 1 == i) {
                                                            Handler(Looper.getMainLooper()).postDelayed(
                                                                Runnable {
                                                                    isOrderUpdated = false
                                                                }, 4000
                                                            )
                                                        }
                                                    }

                                                }
                                            } else if (prefProvider.getValue(
                                                    ORDER_TYPE,
                                                    ""
                                                ) != OPEN_ORDER
                                            ) {
                                                if (isFirstKitPrint == false) {
                                                    isFirstKitPrint = true
                                                    if (kitchenPrinterList.isNotEmpty()) {
                                                        for (i in 0 until kitchenPrinterList.size) {
                                                            if (kitchenPrinterList[i].status) {
                                                                kitchenPrinterList[i].orderTypes.forEach {

                                                                    if (it.orderTypeId == receiptModel?.order?.orderTypeId

                                                                    ) {

                                                                        it.printerSettings.forEach {
                                                                            if (it.printType.lowercase()
                                                                                    .equals(KITCHEN.lowercase()) && it.autoPrinting
                                                                            ) {

                                                                                if (checkItemsforPrinter(
                                                                                        receiptModel?.order?.orderItems
                                                                                            ?: arrayListOf(),
                                                                                        kitchenPrinterList[i].printerCategories.toCollection(
                                                                                            arrayListOf()
                                                                                        )
                                                                                    )
                                                                                ) {
                                                                                    if (!prefProvider.getValueboolean(
                                                                                            Constants.NO_NEED_TO_PRINT,
                                                                                            false
                                                                                        )
                                                                                    ) {
                                                                                        if (prefProvider.getValueboolean(
                                                                                                Constants.DO_PRINT,
                                                                                                false
                                                                                            )
                                                                                        ) {
                                                                                            initKitchenPrinter(
                                                                                                kitchenPrinterList.get(
                                                                                                    i
                                                                                                ),
                                                                                                KITCHEN
                                                                                            )
                                                                                        } else if (prefProvider.getValueboolean(
                                                                                                Constants.DO_PRINT_CUSTOM,
                                                                                                true
                                                                                            )
                                                                                        ) {
                                                                                            initKitchenPrinter(
                                                                                                kitchenPrinterList.get(
                                                                                                    i
                                                                                                ),
                                                                                                KITCHEN
                                                                                            )
                                                                                        } else {
                                                                                            try {
                                                                                                if (receiptModel!!.order.orderType.equals(
                                                                                                        "KioskOpenorder",
                                                                                                        ignoreCase = true
                                                                                                    )
                                                                                                ) {
                                                                                                    initKitchenPrinter(
                                                                                                        kitchenPrinterList.get(
                                                                                                            i
                                                                                                        ),
                                                                                                        KITCHEN
                                                                                                    )
                                                                                                }
                                                                                            } catch (e: java.lang.Exception) {

                                                                                            }
                                                                                        }
                                                                                    }

                                                                                }

                                                                            }
                                                                        }

                                                                    }
                                                                }
                                                            }

                                                            /*IF A PROBLEM IS OCCURING WHEN "UPDATED" IS NOT GETTING PRINTED ON KITCHEN RECEIPT, THEN IT MAY BE BECAUSE THE TIME GIVEN BELOW */
                                                            if (kitchenPrinterList.size - 1 == i) {
                                                                Handler(Looper.getMainLooper()).postDelayed(
                                                                    Runnable {
                                                                        isOrderUpdated = false
                                                                    }, 4000
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }


                                        }
                                    } else {
                                        Log.d("Kiosk", "Kiosk")
                                        /* if (prefProvider.getValue(Constants.ORDER_TYPE,"").equals(Constants.KIOSK_OPEN_ORDER)){
 //                                            Print the receipt here
                                             initKitchenPrinter(kitchenPrinterList.get(
                                                 i
                                             ), KITCHEN)
                                         }*/
                                    }
                                }

                                if (remainingAmount == 0.0)
                                    prefProvider.setValueboolean(Constants.DO_PRINT_CUSTOM, false)
                                //    prefProvider.setValueboolean(Constants.DO_PRINT, false)
                                if (orderTypeToCheckKiosk.equals(
                                        "KioskOpenorder",
                                        ignoreCase = true
                                    ) && !isSpilt
                                ) {
                                    for (i in 0 until kitchenPrinterList.size) {
                                        if (kitchenPrinterList[i].status) {
                                            kitchenPrinterList[i].orderTypes.forEach {

                                                if (it.orderTypeId == receiptModel?.order?.orderTypeId

                                                ) {

                                                    it.printerSettings.forEach {
                                                        if ((it.printType.lowercase()
                                                                .equals(KITCHEN.lowercase()) || it.printType.lowercase()
                                                                .equals(
                                                                    KITCHENANDCUSTOMER.lowercase()
                                                                )) && it.autoPrinting
                                                        ) {


                                                            if (checkItemsforPrinter(
                                                                    receiptModel?.order?.orderItems
                                                                        ?: arrayListOf(),
                                                                    kitchenPrinterList[i].printerCategories.toCollection(
                                                                        arrayListOf()
                                                                    )
                                                                )
                                                            ) {
                                                                initKitchenPrinter(
                                                                    kitchenPrinterList.get(
                                                                        i
                                                                    ),
                                                                    KITCHEN
                                                                )
                                                            }

                                                        }
                                                    }

                                                }
                                            }
                                        }

                                        /*IF A PROBLEM IS OCCURING WHEN "UPDATED" IS NOT GETTING PRINTED ON KITCHEN RECEIPT, THEN IT MAY BE BECAUSE THE TIME GIVEN BELOW */
                                        if (kitchenPrinterList.size - 1 == i) {
                                            Handler(Looper.getMainLooper()).postDelayed(
                                                Runnable {
                                                    isOrderUpdated = false
                                                }, 4000
                                            )
                                        }

                                    }
                                }

                                pd?.dismiss()


                            }


                        }

                        Status.LOADING -> {
//                    ProgressUtils.showProgressDialog(requireActivity())
                        }

                        Status.ERROR -> {
                            ProgressUtils.dismissProgressDialog()

                        }
                    }


                }

            })
        /*viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->

        }*/

    }

    private fun checkOrderItemsForOpenORderUpdate(): ArrayList<CreateOrderResponse.Data.Order.OrderItem> {
        var printOrderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
            arrayListOf()
        var orderItemsToPrint: ArrayList<CreateOrderResponse.Data.Order.OrderItem> = arrayListOf()
        receiptModel?.order?.orderItems?.let { orderItemsToPrint.addAll(it) }

        val serializedObject: String =
            prefProvider.getValue(
                Constants.OPEN_ORDER_ITEMS,
                ""
            )

        Log.e(
            TAG, "getserializedObject:  ${
                prefProvider.getValue(
                    Constants.OPEN_ORDER_ITEMS,
                    ""
                )
            }"
        )
        if (serializedObject.isNotEmpty()) {
            val gson = Gson()
            val type = object :
                TypeToken<List<CreateOrderResponse.Data.Order.OrderItem?>?>() {}.type
            var arrayItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                gson.fromJson<Any>(
                    serializedObject,
                    type
                ) as ArrayList<CreateOrderResponse.Data.Order.OrderItem>

            LogUtil.logE(
                TAG,
                "arrayItems:  ${
                    Gson().toJson(
                        arrayItems
                    )
                }"
            )

            Log.e(
                TAG,
                "updafwwewe  ${Gson().toJson(Gson().toJson(receiptModel?.order?.orderItems))}"
            )
            var itemIds: ArrayList<Int> =
                arrayListOf()
            arrayItems.forEach {
                itemIds.add(it.itemId)
            }


            orderItemsToPrint.forEachIndexed { index, orderItem ->

                if (itemIds.contains(
                        orderItem.itemId
                    )
                ) {

                    arrayItems.forEachIndexed { index, orderItemJ ->

                        if (orderItemJ.itemId == orderItem.itemId) {
                            if (orderItemJ.quantity != orderItem.quantity) {
                                if (orderItem.quantity > orderItemJ.quantity) {
                                    orderItem.quantity =
                                        orderItem.quantity - orderItemJ.quantity

                                    Log.e(TAG, "ItemColdQty ${orderItemJ.quantity}")
                                    Log.e(TAG, "ItemCnewQty ${orderItem.quantity}")
                                    Log.e(TAG, "FinalOrderItemQty  ${orderItem.quantity}")
                                    if (!printOrderItems.contains(
                                            orderItem
                                        )
                                    ) {
                                        printOrderItems.add(
                                            orderItem
                                        )
                                        return@forEachIndexed

                                    }
                                }
                            }


                        }
                    }


                } else {
                    printOrderItems.add(
                        orderItem
                    )
                }


            }
        }
        /*  prefProvider.setValue(
              Constants.OPEN_ORDER_ITEMS,
              ""
          )*/

        Log.e(TAG, "ReturnListForPrint  ${Gson().toJson(printOrderItems)}")

        return printOrderItems


    }

    private fun getCustomerPrinterForDineIn() {
        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerPrinterDineIn = it.data
                    }


                }

                Status.ERROR -> {

                    ProgressUtils.dismissProgressDialog()

                }

                Status.LOADING -> {
//                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        }
    }

    private fun getCustomerPrinters(autoPrintCheck: Boolean) {

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrin...)  autoCheckPrint -> ${
                        Gson().toJson(
                            autoPrintCheck
                        )
                    }"
                )
            )

        viewModel.getCustomerPrinterList().observe(this@OrderCompleteFragment) {

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it -> ${
                            Gson().toJson(
                                it
                            )
                        }"
                    )
                )

            when (it.status) {
                Status.SUCCESS -> {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it.status -> Success"))
                    if (it.data != null && isPrintCustomer) {

                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it.data -> ${
                                        Gson().toJson(
                                            it.data
                                        )
                                    }"
                                )
                            )
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  isPrintCustomer -> ${
                                        Gson().toJson(
                                            isPrintCustomer
                                        )
                                    }"
                                )
                            )

                        isPrintCustomer = false
                        isPrint = false
                        val customerList = it.data

                        if (IS_GIFT_CARD_TYPE) {
                            EventBus.getDefault()
                                .post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  IS_GIFT_CARD_TYPE -> ${
                                            Gson().toJson(
                                                IS_GIFT_CARD_TYPE
                                            )
                                        }"
                                    )
                                )

                            customerList.forEach {
                                if (it.status) {
                                    initPrinter(it, CUSTOMER, autoPrintCheck)
                                }
                            }
                        } else {
                            EventBus.getDefault()
                                .post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  IS_GIFT_CARD_TYPE -> ${
                                            Gson().toJson(
                                                IS_GIFT_CARD_TYPE
                                            )
                                        }"
                                    )
                                )

                            if (autoPrintCheck) {
                                EventBus.getDefault()
                                    .post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  autoPrintCheck -> ${
                                                Gson().toJson(
                                                    autoPrintCheck
                                                )
                                            }"
                                        )
                                    )

                                EventBus.getDefault()
                                    .post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  customerList -> ${
                                                Gson().toJson(
                                                    customerList
                                                )
                                            }"
                                        )
                                    )

                                customerList.forEach { cus ->
                                    if (cus.status) {
                                        EventBus.getDefault()
                                            .post(
                                                MessageEvent(
                                                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  customerList.forEach { cus -> ${
                                                        Gson().toJson(
                                                            cus.status
                                                        )
                                                    }"
                                                )
                                            )

                                        cus.orderTypes.forEach {
                                            EventBus.getDefault()
                                                .post(
                                                    MessageEvent(
                                                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  customerList.forEach_cus.orderTypes { cus -> ${
                                                            Gson().toJson(
                                                                cus.orderTypes
                                                            )
                                                        }"
                                                    )
                                                )


                                            if (it.orderTypeId == receiptModel?.order?.orderTypeId) {

                                                EventBus.getDefault()
                                                    .post(
                                                        MessageEvent(
                                                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  if (it.orderTypeId == receiptModel?.order?.orderTypeId) _ it.orderTypeId -> ${
                                                                Gson().toJson(
                                                                    it.orderTypeId
                                                                )
                                                            }"
                                                        )
                                                    )

                                                EventBus.getDefault()
                                                    .post(
                                                        MessageEvent(
                                                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  if (it.orderTypeId == receiptModel?.order?.orderTypeId) _ receiptModel?.order?.orderTypeId -> ${
                                                                Gson().toJson(
                                                                    receiptModel?.order?.orderTypeId
                                                                )
                                                            }"
                                                        )
                                                    )

                                                EventBus.getDefault()
                                                    .post(
                                                        MessageEvent(
                                                            "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  if (it.orderTypeId == receiptModel?.order?.orderTypeId) _ it.printerSettings -> ${
                                                                Gson().toJson(
                                                                    it.printerSettings
                                                                )
                                                            }"
                                                        )
                                                    )

                                                it.printerSettings.forEach {

                                                    EventBus.getDefault()
                                                        .post(
                                                            MessageEvent(
                                                                "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it.printerSettings.forEach _ it.printType.lowercase() -> ${
                                                                    Gson().toJson(
                                                                        it.printType.lowercase()
                                                                    )
                                                                }"
                                                            )
                                                        )

                                                    EventBus.getDefault()
                                                        .post(
                                                            MessageEvent(
                                                                "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it.printerSettings.forEach _ it.autoPrinting -> ${
                                                                    Gson().toJson(
                                                                        it.autoPrinting
                                                                    )
                                                                }"
                                                            )
                                                        )

                                                    if (it.printType.lowercase()
                                                            .equals(CUSTOMER.lowercase()) && it.autoPrinting
                                                    ) {

                                                        initPrinter(cus, CUSTOMER, autoPrintCheck)


                                                    }
                                                }
                                            }
                                        }
                                    }

                                }

                            } else {
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  autoPrintCheck_ else"))

                                customerList.forEach {
                                    if (it.status) {
                                        initPrinter(it, CUSTOMER, autoPrintCheck)
                                    }

                                }
                            }
                        }
                        isPrintCustomer = false

                    } else {
                        EventBus.getDefault()
                            .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  else _1"))
                        Log.d("getCustomerPrinterList", " data isPrint = $isPrint")
                    }

                    ProgressUtils.dismissProgressDialog()

                }

                Status.ERROR -> {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.getCustomerPrinters(autoPrint...)  it.status -> Error"))

                    ProgressUtils.dismissProgressDialog()
                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }
        }
    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        isAutoPrint: Boolean
    ) {
        Log.e(TAG, "checkAutoPrint  ${isAutoPrint}")
        pd?.show()

        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _initPrinter(customerRecei..."))

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _initPrinter(customerRecei... -> ${
                        Gson().toJson(
                            customerReceiptPrinters.name
                        )
                    }"
                )
            )


        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _initPrinter(customerRecei... _ customerReceiptPrinters.name.startsWith(SUNMI_PRINTER"))

            customerReceiptPrinters.ipAddress?.let { sunmiPrinterInit(it, isAutoPrint) }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _initPrinter(customerRecei... _ else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER"))

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService1(isAutoPrint)
            }


        } else {


            PrinterClass.closePrinter()
            if (PrinterClass.getPrinter() == null) {

                var printer: Print? = Print(requireContext())
                if (printer != null) {
                    printer.setStatusChangeEventCallback(this)
                    printer.setBatteryStatusChangeEventCallback(this)
                }

                val enabled = Print.FALSE

                try {
                    var interval: Int = 1000
                    if (customerReceiptPrinters.printer_type == BLUETOOTH) {
                        interval = BLUETOOTH_TIMEOUT
                    }
                    printer?.openPrinter(

                        if (customerReceiptPrinters.printer_type == BLUETOOTH) {
                            Print.DEVTYPE_BLUETOOTH
                        } else {
                            Print.DEVTYPE_TCP
                        },
                        customerReceiptPrinters.ipAddress,
                        enabled,
                        10000
                    )
                    //printer?.setStatusChangeEventCallback(this)

                } catch (e: Exception) {
                    e.printStackTrace()
                    pd?.dismiss()
                    LogUtil.logE(TAG, "PrinterException: " + e.message)
                    printer = null
                    return
                }
                try {

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)

                        if (IS_GIFT_CARD_TYPE) {
                            generatePrintForGiftCard(customerReceiptPrinters, type)
                        } else {
                            if (customerReceiptPrinters.status) {
                                generatePrint(customerReceiptPrinters, type, isAutoPrint)
                            }
                        }

                    }

                } catch (e: Exception) {
                    pd?.dismiss()
                    e.printStackTrace()
                }
            } else {
                pd?.dismiss()
                LogUtil.logE(TAG, "PrinterIsNotNull:")
            }
        }

    }


    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        isAutoPrint: Boolean
    ) {
        var builder: Builder? = null
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")

            if (customerSettingModel.showOrderIdTop) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    builder.addText("OrderID:" + receiptModel?.order?.id)

                }

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }


            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)

            builder.addTextLang(Builder.LANG_EN)


            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, prefProvider.getValue(BUSINESS_NAME, "").toString())
            if (customerSettingModel.showVenueAddress) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(BUSINESS_ADDRESS, "").toString()
                )
            }
            if (customerSettingModel.showVenuePhone) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.formatPhoneNumber(
                        prefProvider.getValue(BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }

            if (customerSettingModel.showWebsiteAddress) {
                receiptModel?.order?.venue_website?.let {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    addBuilderText(builder, it)
                }
            }
            if (customerSettingModel.showOrderType) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(receiptModel?.order?.orderTypeName + "\n")
            }

            if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                receiptModel?.order?.orderType.equals("Online Order", true) ||
                receiptModel?.order?.orderType.equals("OnlineOrder", true)
            ) {


                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(receiptModel?.order?.deliveryType + "\n")


            }



            if (customerSettingModel.fonts == LARGE) {

                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText("ReceiptID:" + receiptModel?.order?.offlineId)

                if (customerSettingModel.showTeam && receiptModel?.order?.employee?.name != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + receiptModel?.order?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "ReceiptID:" + receiptModel?.order?.offlineId,
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam && receiptModel?.order?.employee?.name != null) {
                                "Employee:" + receiptModel?.order?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    receiptModel?.order?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (customerSettingModel.showPrintTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            receiptModel?.order?.orderItems?.let {
                addOrderItems(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            builder.addFeedLine(2)

            if (receiptModel?.order?.totalDiscount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Total Discount",

                        if (receiptModel?.order?.totalDiscount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                            "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        },
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            var totalDiscount: Double = 0.0
            receiptModel?.order?.totalDiscount?.let {
                totalDiscount = it
            }
            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.subTotal!!),
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (receiptModel?.order?.totalTaxAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalTaxAmount!!),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (receiptModel?.order?.totalServiceCharges != null && (receiptModel?.order?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalServiceCharges!!),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (tipAmount > 0) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tips",
                        "$" + MethodUtils.roundOffAmountString(tipAmount.toDouble()),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (customerSettingModel.showCashDisSurCharg) {
                if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.paymentType?.lowercase() == "Card".lowercase() && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "SurCharge".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                Constants.SURCHARGE_TEXT,
                                "$" + MethodUtils.roundOffAmountString(
                                    receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0
                                ),
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }


                } else if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                "Cash Discount",
                                if (receiptModel?.order?.totalCashDiscountFee == 0.0) {
                                    "$" + MethodUtils.roundOffAmountString(
                                        receiptModel?.order?.payments?.get(
                                            receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                        )?.cash_discount_or_surcharge ?: 0.0
                                    )
                                } else {
                                    "-$" + MethodUtils.roundOffAmountString(
                                        receiptModel?.order?.payments?.get(
                                            receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                        )?.cash_discount_or_surcharge ?: 0.0
                                    )
                                },
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }
                }
            }




            if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                    if (receiptModel?.order?.loyaltyAmount != 0.0) {
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                "Used Loyalty Amount",
                                "-$" + receiptModel?.order?.loyaltyAmount?.let {
                                    MethodUtils.roundOffAmountString(
                                        it
                                    )
                                },
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }


                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Used Loyalty Points",
                            receiptModel?.order?.payments!![0].loyaltyUSedPoints.toString(),
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            var totalfamount = 0.0

            if (receiptModel?.order?.totalAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                /* if (receiptModel?.order?.totalDiscount != 0.0) {
                     totalAmt =
                         (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalDiscount!!))

                 }*/


                if (paymentType == "Cash") {

                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    //PLZCHECK
                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()) {
                        builder.addText(
                            padLine(
                                "Total Price",
                                "$" + MethodUtils.roundOffAmountString(
                                    finalAmt - (receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0)
                                ),
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt - (receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.cash_discount_or_surcharge ?: 0.0)
                        )
                    } else {
                        builder.addText(
                            padLine(
                                "Total Price",
                                "$" + MethodUtils.roundOffAmountString(finalAmt),
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)

                    }

                } else {
                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    totalfamount = finalAmt

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "SurCharge".lowercase()) {


                        builder.addText(
                            padLine(
                                "Total Price",
                                "$" + MethodUtils.roundOffAmountString(
                                    finalAmt.plus(
                                        (receiptModel?.order?.payments?.get(
                                            receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                        )?.cash_discount_or_surcharge ?: 0.0)
                                    )
                                ),
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt.plus(
                                (receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0)
                            )
                        )
                    } else {

                        builder.addText(
                            padLine(
                                "Total Price",
                                "$" + MethodUtils.roundOffAmountString(finalAmt),
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)
                    }
                }


            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            val newPaidAmount = if (isCustomCash) {
                paidAmount
            } else {
                paidAmount + tipAmount
            }

            builder.addText(
                padLine(
                    "Paid Amount",
                    "$" + MethodUtils.roundOffAmountString(
                        newPaidAmount
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )



            if (customerSettingModel.showRefundAmount) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //ADDCHANGE
                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (isSpilt) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Remaining Amount",
                        "$" + MethodUtils.roundOffAmountString(remainingAmount),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }



            if (receiptModel?.order?.totalTips == 0.0) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                var tip = ""

                if (receiptModel?.order?.totalTips != 0.0) {
                    tip = receiptModel?.order?.totalTips.toString()
                }
                builder.addText(
                    padLine(
                        "Tips",
                        if (customerSettingModel.showTipLineForCash) {
                            "_____________"
                        } else {
                            ""
                        },
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
                builder.addText(
                    padLine(
                        "Total",
                        if (customerSettingModel.showTipLineForCash) {
                            "_____________"
                        } else {
                            ""
                        },
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (customerSettingModel.showTipSuggestion) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Additional Tips",
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        builder,
                        tipsList,
                        if (receiptModel?.order?.totalDiscount != 0.0 && receiptModel?.order?.totalAmount ?: 0.0 > receiptModel?.order?.totalDiscount ?: 0.0) {
                            (totalfamount)
                        } else {
                            receiptModel?.order?.totalAmount!!
                        },
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(
                builder, customerSettingModel.fonts
            )
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Transaction ID",
                    "" + receiptModel?.order?.payments?.size?.minus(1)
                        ?.let { receiptModel?.order?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Transaction Type",
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                if (!(receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    /*builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )*/
                }


                var strCardType =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType
                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    strCardType =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }


                if (!(strCardType).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    var strCardType =
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType
                    if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                        var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                        var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                        strCardType =
                            paymentViewModel.extData.substring(
                                applabStartIndex + "<APPLAB>".length,
                                applabEndIndex
                            )
                    }

                    builder.addText(
                        padLine(
                            "",
                            strCardType,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
                if (!(receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber,
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

            }

            if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null && !prefProvider.getValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        ""
                    ).toString()
                        .equals("") && !prefProvider.getValue(Constants.RECEIPT_CUSTOMER_NAME, "")
                        .toString().equals("kotlin.Unit", true)
                ) {

                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName,
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }
                    if (customerSettingModel.showCustomerPhone) {


                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            var phoneNoLast = receiptModel?.order?.customer?.phones?.get(
                                receiptModel?.order?.customer?.phones?.size?.minus(
                                    1
                                ) ?: 0
                            )?.phoneNumber ?: ""

                            if (phoneNoLast.isNotEmpty()) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                //builder.addTextAlign(Builder.ALIGN_LEFT)
                                builder.addTextLang(Builder.LANG_EN)
                                addCustomerTextSize(builder, customerSettingModel.fonts)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.COLOR_1
                                )

                                val phone = receiptModel?.order?.customer?.phones?.size?.minus(
                                    1
                                )?.let {
                                    receiptModel?.order?.customer?.phones?.get(
                                        it
                                    )?.phoneNumber
                                }
                                builder.addText(
                                    padLine(
                                        MethodUtils.formatPhoneNumber(phone.toString()),
                                        "",
                                        if (customerSettingModel.fonts == LARGE) {
                                            24
                                        } else {
                                            48
                                        }
                                    )
                                )

                            }

                        }

                    }



                    if (customerSettingModel.showCustomerAddress && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("") && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("kotlin.Unit", true)
                    ) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )


                            receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {

                                        builder.addText(
                                            padLine(
                                                it.fullAddress,
                                                "",
                                                if (customerSettingModel.fonts == LARGE) {
                                                    24
                                                } else {
                                                    48
                                                }
                                            )
                                        )

                                    }
                                }


                        }
                    }

                }
            }


            LogUtil.logE(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "" && customerSettingModel.showOrderNote) {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("Order Note")
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(receiptModel?.order?.note)
            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            //customer signature line.
            builder.addText(
                padLine(
                    "Customer Signature",
                    addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel?.order?.digital_receipt_url.toString())
                LogUtil.logE(TAG, "BitmapHeight ${bitmap.height}")
                LogUtil.logE(TAG, "BitmapWidth ${bitmap.width}")
                val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(2)
            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)



            try {
                if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType.equals(
                        "Cash",
                        true
                    ) && isAutoPrint
                ) {
                    builder.addPulse(
                        com.epson.epos2.printer.Printer.DRAWER_HIGH,
                        com.epson.epos2.printer.Printer.PULSE_100
                    )
                }
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    BLUETOOTH_TIMEOUT, status, battery
                )


                PrinterClass.closePrinter()
                pd?.dismiss()
                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                pd?.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            pd?.dismiss()
            e.printStackTrace()
        }
    }

    /**
     * This method is used to print the receipt for sell gift card or add value in gift card.
     * This method prints from TM-m30 Printers
     * */
    private fun generatePrintForGiftCard(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {
        var builder: Builder? = null
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")

            if (customerSettingModel.showOrderIdTop) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText("OrderID:" + giftCardReceiptModel?.gift_card?.id)

                builder.addFeedLine(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }


            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)

            builder.addTextLang(Builder.LANG_EN)


            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, prefProvider.getValue(BUSINESS_NAME, "").toString())
            if (customerSettingModel.showVenueAddress) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(BUSINESS_ADDRESS, "").toString()
                )
            }
            if (customerSettingModel.showVenuePhone) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.formatPhoneNumber(
                        prefProvider.getValue(BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }

            if (customerSettingModel.showWebsiteAddress) {
                receiptModel?.order?.venue_website?.let {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    addBuilderText(builder, it)
                }
            }
            if (customerSettingModel.showOrderType) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(giftCardReceiptModel?.gift_card?.order_type_name + "\n")
            }

            if (customerSettingModel.fonts == LARGE) {

                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    //"ReceiptID:" + giftCardReceiptModel?.gift_card?.id
                    "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                    )?.offline_id
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + giftCardReceiptModel?.gift_card?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            giftCardReceiptModel?.gift_card?.payments?.get(
                                giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                            )?.created_at!!
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                Log.d("giftCardReceiptModel", "model = ${Gson().toJson(giftCardReceiptModel)}")
                builder.addText(
                    padLine(
                        /*"ReceiptID:" + giftCardReceiptModel?.gift_card?.id,
                        ""*/
                        "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                            giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                        )?.offline_id,
                        "",
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam) {
                                "Employee:" + giftCardReceiptModel?.gift_card?.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime && giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                            1
                        ) ?: 0
                    )?.created_at?.isNotEmpty() == true
                ) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    giftCardReceiptModel?.gift_card?.payments?.get(
                                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                                            1
                                        ) ?: 0
                                    )?.created_at!!
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (customerSettingModel.showPrintTime && giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                    )?.created_at?.isNotEmpty() == true
                ) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )


                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            val giftCardList: MutableList<CreateOrderResponse.Data.Order.OrderItem> =
                mutableListOf()

            var giftCardAmount = 0.00
            if (giftCardReceiptModel?.gift_card?.payments != null && giftCardReceiptModel?.gift_card?.payments?.isNotEmpty()!!) {
                giftCardAmount =
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.amount?.toPrecision(
                        2
                    )?.toDoubleWithPrecision(2)!!
            }

            Log.d(
                TAG,
                "generatePrintForGiftCard:  giftCardAmount = ${giftCardAmount.toPrecision(2)}"
            )

            giftCardList.add(
                0, CreateOrderResponse.Data.Order.OrderItem(
                    itemName = "${giftCardReceiptModel?.gift_card?.gift_card_type} Gift Card",
                    price = giftCardAmount, quantity = 1
                )
            )

            addOrderItems(
                builder,
                giftCardList,
                customerSettingModel.fonts,
                customerSettingModel.showModifiers
            )

            builder.addFeedLine(2)

            builder.addText(
                padLine(
                    "Total Price",
                    "$${giftCardAmount.toPrecision(2)}",
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            builder.addText(
                padLine(
                    "Paid Amount",
                    "$" + MethodUtils.roundOffAmountString(
                        paidAmount
                    ),
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            if (customerSettingModel.showRefundAmount) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //ADDCHANGE
                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            builder.addFeedLine(1)

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Transaction ID",
                    "" + giftCardReceiptModel?.gift_card?.payments?.size?.minus(1)
                        ?.let { giftCardReceiptModel?.gift_card?.payments?.get(it)?.id },
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Transaction Type",
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type,
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )

            if (giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type?.lowercase() == "Card".lowercase()) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "",
                        giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_name,
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "",
                        giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_type,
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "",
                        giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_number,
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

                if (giftCardReceiptModel?.gift_card?.customer != null) {

                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                giftCardReceiptModel?.gift_card?.customer?.firstName + " " + giftCardReceiptModel?.gift_card?.customer?.lastName,
                                "",
                                if (customerSettingModel.fonts == LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )
                    }
                    if (customerSettingModel.showCustomerPhone) {


                        if (giftCardReceiptModel?.gift_card?.customer?.phones?.isNotEmpty() == true) {

                            var phoneNoLast =
                                giftCardReceiptModel?.gift_card?.customer?.phones?.get(
                                    giftCardReceiptModel?.gift_card?.customer?.phones?.size?.minus(
                                        1
                                    ) ?: 0
                                )?.phoneNumber ?: ""

                            if (phoneNoLast.isNotEmpty()) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                //builder.addTextAlign(Builder.ALIGN_LEFT)
                                builder.addTextLang(Builder.LANG_EN)
                                addCustomerTextSize(builder, customerSettingModel.fonts)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.COLOR_1
                                )

                                val phone =
                                    giftCardReceiptModel?.gift_card?.customer?.phones?.size?.minus(
                                        1
                                    )?.let {
                                        giftCardReceiptModel?.gift_card?.customer?.phones?.get(
                                            it
                                        )?.phoneNumber
                                    }
                                builder.addText(
                                    padLine(
                                        MethodUtils.formatPhoneNumber(phone.toString()),
                                        "",
                                        if (customerSettingModel.fonts == LARGE) {
                                            24
                                        } else {
                                            48
                                        }
                                    )
                                )

                            }

                        }

                    }



                    if (customerSettingModel.showCustomerAddress && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("") && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("kotlin.Unit", true)
                    ) {
                        if (giftCardReceiptModel?.gift_card?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )


                            giftCardReceiptModel?.gift_card?.customer?.addresses?.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {

                                        builder.addText(
                                            padLine(
                                                it.fullAddress,
                                                "",
                                                if (customerSettingModel.fonts == LARGE) {
                                                    24
                                                } else {
                                                    48
                                                }
                                            )
                                        )

                                    }
                                }


                        }
                    }

                }
            }

            builder.addFeedLine(2)

            //customer signature line.
            builder.addText(
                padLine(
                    "Customer Signature",
                    addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            builder.addFeedLine(2)
            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)

            try {
                if (giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type.equals(
                        "Cash",
                        true
                    )
                ) {
                    builder.addPulse(
                        com.epson.epos2.printer.Printer.DRAWER_HIGH,
                        com.epson.epos2.printer.Printer.PULSE_100
                    )
                }
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    BLUETOOTH_TIMEOUT, status, battery
                )


                PrinterClass.closePrinter()
                pd?.dismiss()
                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                pd?.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            pd?.dismiss()
            e.printStackTrace()
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        prefProvider.deleteValue(Constants.DO_PRINT)

        if (data.name.startsWith(SUNMI_PRINTER, true)) {


            try {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)
                Log.d("initKitchenPrinter", "SunmiBlueToothPrinter is ${data.ipAddress}")

            } catch (e: Exception) {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiNetPrinter, data.ipAddress)
                Log.d("initKitchenPrinter", "SunmiNetPrinter")

            }

            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            generateKitchenReceiptSunmi(data, type)

                            /*       viewLifecycleOwner.lifecycleScope.launch {
                                       delay(200)

                                   }*/


                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generateKitchenReceiptSunmi(data, type)
                /* viewLifecycleOwner.lifecycleScope.launch {
                     delay(200)

                 }*/
            }


        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {
            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            runBlocking {
                delay(200)
                setService2(data, type)
            }

        } else if (data.name.contains("TSP", ignoreCase = true)) {
            settings = StarConnectionSettings(InterfaceType.Lan, data.macAddress)
            printer = StarPrinter(settings, requireContext())

            var isPrint = true
            /*receiptModel?.order?.let {
                it.orderItems.forEach {
                    if (it.isItemEdited){
                        isPrint=true
                        return@forEach
                    }
                }
            }*/

            if (isPrint) {
                GlobalScope.launch {
                    try {
                        val builder = StarXpandCommandBuilder()

                        var printerBuilder = PrinterBuilder()

                        with(printerBuilder) {
                            styleInternationalCharacter(InternationalCharacterType.Usa)
                            styleCharacterSpace(0.0)

                            styleAlignment(Alignment.Center)
                            if (!oneItemPerReceipt) {
                                receiptModel?.order?.orderItems?.forEach { item ->
                                    data.printerCategories.forEach { category ->
                                        if (category.id == item.categoryId && category.printerEnable) {
                                            for (singularity in 1..item.quantity) {
                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(3, 3)
                                                        )
                                                        .actionPrintText(
                                                            "OrderId:${receiptModel?.order?.custom_order_id}"
                                                        )
                                                )

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            "${receiptModel?.order?.orderTypeName}"
                                                        )
                                                )

                                                actionFeedLine(1)

                                                if (receiptModel?.order?.orderType?.contains(
                                                        "Phone",
                                                        true
                                                    ) == true
                                                ) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleBold(true)
                                                            .styleMagnification(
                                                                MagnificationParameter(2, 2)
                                                            )
                                                            .actionPrintText(
                                                                "${receiptModel?.order?.deliveryType}"
                                                            )
                                                    )

                                                    actionFeedLine(1)
                                                }

                                                add(
                                                    PrinterBuilder()
                                                        .styleAlignment(Alignment.Left)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            content = addOrderSingleItemForStarKitchen(
                                                                1,
                                                                item,
                                                                data.printerCategories.toCollection(
                                                                    arrayListOf()
                                                                )
                                                            )
                                                        )
                                                )

                                                actionFeedLine(1)

                                                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleAlignment(Alignment.Center)
                                                            .actionPrintText(
                                                                content = if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                                    receiptModel?.order?.note.toString()
                                                                } else ""
                                                            )
                                                    )
                                                var printedName = StringBuilder("")
                                                receiptModel?.order?.customer?.firstName?.let { firstName ->
                                                    receiptModel?.order?.customer?.lastName?.let { lastName ->
                                                        if (kitchenSettingModel.showCustomerName) {
                                                            if (!firstName.contains(
                                                                    "customer",
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                printedName.append(firstName)
                                                                printedName.append(" ")
                                                            }

                                                            if (!lastName.isBlank()) {
                                                                printedName.append(lastName)
                                                            }

                                                            if (printedName.isNotEmpty()) {
                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .styleBold(true)
                                                                        .actionPrintText(
                                                                            content = "Customer Details\n"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Center)
                                                                        .actionPrintText(
                                                                            content =
                                                                            "--------------------------------------------"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .actionPrintText(
                                                                            content = printedName.toString()
                                                                        )
                                                                )

                                                            }
                                                        }

                                                    }
                                                }

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .actionPrintText(
                                                            getReceiptFormatDateFromUTCServer(
                                                                requireContext(),
                                                                receiptModel?.order?.createdAt.toString()
                                                            )
                                                        )
                                                )

                                                actionFeedLine(1)

                                                actionCut(CutType.Partial)
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (isOrderUpdated == true) {
                                    add(
                                        PrinterBuilder()
                                            .styleBold(true)
                                            .actionPrintText(
                                                "***** UPDATED *****"
                                            )
                                    )
                                }

                                styleAlignment(Alignment.Center)

                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .styleMagnification(
                                            MagnificationParameter(3, 3)
                                        )
                                        .actionPrintText(
                                            "OrderId:${receiptModel?.order?.custom_order_id}"
                                        )
                                )

                                styleAlignment(Alignment.Center)

                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .actionPrintText(
                                            if (kitchenSettingModel.showOrderType)
                                                receiptModel!!.order.orderTypeName
                                            else ""
                                        )
                                )

                                actionFeedLine(1)

                                if (receiptModel!!.order.orderTypeName == Constants.PHONE_ORDER_) {
                                    add(
                                        PrinterBuilder()
                                            .styleBold(true)
                                            .actionPrintText(
                                                receiptModel!!.order.deliveryType
                                            )
                                    )
                                    actionFeedLine(1)
                                }

                                add(
                                    PrinterBuilder()
                                        .actionPrintText(
                                            "Employee:${receiptModel?.order?.employee?.name}"
                                        )
                                )
                                actionFeedLine(1)

                                add(
                                    PrinterBuilder()
                                        .actionPrintText(
                                            getReceiptFormatDateFromUTCServer(
                                                requireContext(),
                                                receiptModel?.order?.createdAt.toString()
                                            )
                                        )
                                )

                                actionFeedLine(1)

                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .actionPrintText(
                                            "------------------------"
                                        )
                                )

                                actionFeedLine(1)

                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .styleMagnification(
                                            MagnificationParameter(1, 1)
                                        )
                                        .actionPrintText(
                                            content = addOrdersForStarKitchen(
                                                receiptModel?.order?.orderItems!!,
                                                data.printerCategories.toCollection(arrayListOf())
                                            )
                                        )
                                )

                                actionFeedLine(1)
                                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Center)
                                            .styleBold(true)
                                            .actionPrintText(
                                                content = if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    "--------------------------------------------\nOrder Note"
                                                } else ""
                                            )
                                    )
                                }

                                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Center)
                                            .actionPrintText(
                                                content = if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    receiptModel?.order?.note.toString()
                                                } else ""
                                            )
                                    )
                                }
                                actionFeedLine(1)
                                if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .styleBold(true)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                                    "\nCustomer Details\n"
                                                } else ""
                                            )
                                    )
                                }

                                if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Center)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                                    "--------------------------------------------"
                                                } else ""
                                            )
                                    )
                                }
                                if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (receiptModel?.order?.customer?.firstName != null || receiptModel?.order?.customer?.lastName != null)) {
                                                    receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName
                                                } else ""
                                            )
                                    )
                                }
                                if (kitchenSettingModel.showCustomerPhone && receiptModel?.order?.customer?.phones?.get(
                                        0
                                    ) != null
                                ) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerPhone && receiptModel?.order?.customer?.phones?.get(
                                                        0
                                                    ) != null
                                                ) {

                                                    var phoneNumber =
                                                        receiptModel?.order?.customer?.phones?.get(
                                                            0
                                                        )?.phoneNumber.toString()
                                                    if (phoneNumber.length != 10) {
                                                        // Handle invalid input (must be 10 digits)
                                                        "Invalid phone number"
                                                    }

                                                    val areaCode = phoneNumber.substring(0, 3)
                                                    val firstPart = phoneNumber.substring(3, 6)
                                                    val secondPart = phoneNumber.substring(6)

                                                    "($areaCode)$firstPart-$secondPart"


                                                    /* MethodUtils.formatPhoneNumber(
                                                     receiptModel?.order?.customer?.phones?.get(
                                                         0
                                                     )?.phoneNumber.toString()
                                                 )*/
                                                } else ""
                                            )
                                    )
                                }
                                printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)

                            }

                            /* runBlocking {
                            isOrderUpdated=false
                            receiptModel?.order?.orderItems?.let {item->
                                item.forEach {
                                    if (it.isItemEdited){
                                        isOrderUpdated=true
                                        return@runBlocking
                                    }
                                }
                            }
                        }*/
                            /*Added By Rahul */


                            /*if (kitchenSettingModel.showCustomerPhone && receiptModel?.order?.customer?.phones?.get(
                                    0
                                ) != null
                            ) {

                            }*/
                        }

//                        printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)

                        var document = DocumentBuilder()
                            .addPrinter(printerBuilder)
                        builder.addDocument(
                            document
                        )

                        val commands = builder.getCommands()

                        printer.openAsync().await()

//                val jobSettings = StarSpoolJobSettings(true, 30, "Print from Android")

                        printer.printAsync(commands).await()



                        Log.d("Printing", "Success")
                    } catch (e: Exception) {
                        Log.d("Printing", "Error: ${e}")
                    } finally {
                        printer.closeAsync().await()
                    }
                }
            }

        } else {

            if (isNotPrinted) {
                isNotPrinted = false
                if (!data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    Log.e(TAG, "YesInsideU220")

                    var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                            .contains("TM-m".lowercase())
                    ) {
                        Log.e(TAG, "YesContains")
                        Printer(
                            Printer.TM_M30,
                            Printer.MODEL_ANK, requireContext()
                        )
                    } else {
                        Printer(
                            Printer.TM_U220,
                            Printer.MODEL_ANK, requireContext()
                        )


                    }

                    mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                        if (printerStatusInfo.getPaperTakenSensor() == Printer.REMOVAL_DETECT_PAPER) {
                            Log.e(
                                "PrinterEvent",
                                "REMOVAL_DETECT_PAPER"
                            )
                        }

                        if (printerStatusInfo.getPaperTakenSensor() == Printer.REMOVAL_DETECT_UNKNOWN) {

                            Log.e(
                                "PrinterEvent",
                                "REMOVAL_DETECT_PAPER"
                            )
                        }

                        Log.e(
                            "PrinterEvent",
                            "Printed"
                        )


                        if (printerStatusInfo.connection == 1) {
                            try {
                                printer.disconnect()

                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    try {
                        Log.e(TAG, "printerDataType:  ${data.printer_type}")

                        var printerAdd =
                            if (data.printer_type == BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                        if (mPrinter.status.connection == 0) {

                            mPrinter.connect(
                                printerAdd,
                                Printer.PARAM_DEFAULT
                            )
                            // mPrinter.disconnect()
                        }

                        //  mPrinter.startMonitor()

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(200)
                            generateReceiptForU220(mPrinter, data, type)
                        }

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }


                } else {


                    PrinterClass.closePrinter()
                    if (PrinterClass.getPrinter() == null) {
                        //  printerDialog.show(requireContext())

                        var printer: Print? = Print(requireContext())
                        /*if (printer != null) {
                       printer.setStatusChangeEventCallback(this)
                       printer.setBatteryStatusChangeEventCallback(this)
                   }*/


                        val enabled = Print.FALSE

                        try {

                            printer?.openPrinter(
                                if (data.printer_type == BLUETOOTH) {
                                    Print.DEVTYPE_BLUETOOTH
                                } else {
                                    Print.DEVTYPE_TCP
                                },
                                data.ipAddress,
                                enabled,
                                1000
                            )
                            printer?.setStatusChangeEventCallback(this)

                        } catch (e: Exception) {
                            //  printerDialog.dismiss()
                            LogUtil.logE(TAG, "PrinterException: " + e.message)
                            printer = null
                            return
                        }

                        if (printer != null) {
                            PrinterClass.setPrinter(printer)


                            generateKitchenReceipt(data, type)

                        }

                    } else {
                        LogUtil.logE(TAG, "PrinterIsNotNull:")
                    }
                }
            }
            Log.e("kitchenPrintTMM", "printer name = ${data.name}")


        }
    }


    private fun generateReceiptForU220(
        mPrinter: Printer,
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {


        var fontSizeH = 1
        var fontSizeW = 1
        when (kitchenSettingModel.fonts) {
            Constants.SMALL -> {
                fontSizeH = 1
                fontSizeW = 1
            }

            Constants.MEDIUM -> {
                fontSizeH = 1
                fontSizeW = 2
            }

            LARGE -> {
                fontSizeH = 2
                fontSizeW = 2
            }


        }


        mPrinter.addFeedUnit(30)
        mPrinter.addFeedLine(2)

        mPrinter.addTextFont(Builder.FONT_E)
        mPrinter.addTextAlign(Builder.ALIGN_CENTER)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(2, 2)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.TRUE,
            Builder.COLOR_1
        )

        mPrinter.addText("OrderID:" + receiptModel?.order?.custom_order_id)
        mPrinter.addFeedLine(1)
        mPrinter.addFeedUnit(30)
        mPrinter.addFeedLine(1)

        if (kitchenSettingModel.showOrderType) {


            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderTextForU220(mPrinter, receiptModel?.order?.orderTypeName.toString())
        }
        var tmps = "Open Order".toString().trim()
            .toString().lowercase()
        LogUtil.logE(TAG, "LowerCAse ${tmps.trimmedLength()}")

        if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
            receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
            receiptModel?.order?.orderType.equals("Online Order", true) ||
            receiptModel?.order?.orderType.equals("OnlineOrder", true)
        ) {
            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderTextForU220(mPrinter, receiptModel?.order?.deliveryType.toString())
        }

        if (kitchenSettingModel.showTeamMember) {

            mPrinter.addFeedLine(1)
            mPrinter.addFeedUnit(30)
            mPrinter.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            mPrinter.addText(
                padLine(
                    "Employee:" + receiptModel?.order?.employee?.name, "",
                    33
                )
            )

        }
        mPrinter.addFeedLine(1)
        mPrinter.addFeedUnit(30)
        mPrinter.addTextFont(Builder.FONT_E)
        //  builder.addTextAlign(Builder.ALIGN_LEFT)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(fontSizeH, fontSizeW)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        mPrinter.addText(
            padLine(
                getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    receiptModel?.order?.createdAt.toString()
                ),
                "",
                33
            )
        )

        mPrinter.addFeedLine(1)

        mPrinter.addTextFont(Builder.FONT_B)
        //builder.addTextLineSpace(20)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(fontSizeH, fontSizeW)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        if (data.modalName.equals("TM-L100", ignoreCase = true)) {

            var str: String = ""
            for (i in 0 until 30) {
                str += "-"
            }

            mPrinter.addText(str)
        } else {
            addHorizontalKitchenLineForU220(mPrinter)
        }

        receiptModel?.order?.orderItems?.let {
            addOrdersForKitchenU220(
                mPrinter,
                it,
                fontSizeH,
                fontSizeW,
                data.printerCategories.toCollection(arrayListOf())
            )
        }
        mPrinter.addFeedLine(1)

        if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
            mPrinter.addFeedUnit(30)
            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addText("Order Note")

            mPrinter.addFeedLine(1)
            mPrinter.addFeedUnit(30)

            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )


            mPrinter.addText(receiptModel?.order?.note.toString())
        }

        mPrinter.addFeedLine(1)

        if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
            mPrinter.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            if (data.modalName.equals("TM-L100", ignoreCase = true)) {

                var str: String = ""
                for (i in 0 until 30) {
                    str += "-"
                }

                mPrinter.addText(str)
            } else {
                addHorizontalKitchenLineForU220(mPrinter)
            }
            if (receiptModel?.order?.customer != null) {

                mPrinter.addFeedLine(1)
                mPrinter.addFeedUnit(30)
                mPrinter.addFeedLine(1)
                mPrinter.addTextFont(Builder.FONT_E)
                //builder.addTextLineSpace(20)
                mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(fontSizeH, fontSizeW)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                mPrinter.addText("Customer Details" + "\n")

                mPrinter.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(fontSizeH, fontSizeW)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                if (data.modalName.equals("TM-L100", ignoreCase = true)) {

                    var str: String = ""
                    for (i in 0 until 30) {
                        str += "-"
                    }

                    mPrinter.addText(str)
                } else {
                    addHorizontalKitchenLineForU220(mPrinter)
                }
                if (kitchenSettingModel.showCustomerName) {

                    mPrinter.addFeedLine(1)
                    mPrinter.addFeedUnit(30)
                    mPrinter.addTextFont(Builder.FONT_E)
                    mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    mPrinter.addTextLang(Builder.LANG_EN)
                    mPrinter.addTextSize(fontSizeH, fontSizeW)
                    mPrinter.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    mPrinter.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                }


                if (kitchenSettingModel.showCustomerPhone) {

                    if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {
                        mPrinter.addFeedLine(1)
                        mPrinter.addFeedUnit(30)
                        mPrinter.addTextFont(Builder.FONT_E)
                        mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                        //builder.addTextLineSpace(20)
                        mPrinter.addTextLang(Builder.LANG_EN)
                        mPrinter.addTextSize(fontSizeH, fontSizeW)
                        mPrinter.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        mPrinter.addText(
                            MethodUtils.formatPhoneNumber(
                                receiptModel?.order?.customer?.phones?.get(
                                    0
                                )?.phoneNumber.toString()
                            )
                        )
                    }

                }
                /* builder.addTextLineSpace(30)
             builder.addFeedUnit(30)
             builder.addTextFont(Builder.FONT_E)
             builder.addTextAlign(Builder.ALIGN_LEFT)
             //builder.addTextLineSpace(20)
             builder.addTextLang(Builder.LANG_EN)
             builder.addTextSize(1, 1)
             builder.addTextStyle(
                 Builder.FALSE,
                 Builder.FALSE,
                 Builder.TRUE,
                 Builder.COLOR_1
             )
             builder.addText(receiptModel?.order?.customer?.email)*/

                if (kitchenSettingModel.showCustomerAddress) {
                    if (receiptModel?.order?.orderType?.trim().toString()
                            .lowercase() == "Open Order".trim()
                            .toString().lowercase()
                        && receiptModel?.order?.deliveryType?.trim().toString()
                            .lowercase() == "Pickup".trim().lowercase()
                    ) {

                    } else {

                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            mPrinter.addFeedLine(1)
                            mPrinter.addFeedUnit(30)
                            mPrinter.addTextFont(Builder.FONT_E)
                            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            mPrinter.addTextLang(Builder.LANG_EN)
                            mPrinter.addTextSize(fontSizeH, fontSizeW)
                            mPrinter.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        mPrinter.addText(
                                            it.fullAddress
                                        )
                                    }
                                }

                            //  builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }
                }

            }
        }

        mPrinter.addFeedLine(4)
        mPrinter.addCut(Builder.CUT_FEED)

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            mPrinter.clearCommandBuffer()
//            mPrinter.endTransaction()

            /*  try {
                  mPrinter.disconnect()
              } catch (e: java.lang.Exception) {
                  e.printStackTrace()
              }*/

        } catch (e: java.lang.Exception) {
            /* try {
                 mPrinter.disconnect()
             } catch (e: Exception) {
                 e.printStackTrace()
             }*/
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        var builder: Builder? = null
        try {
            LogUtil.logE(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {

                var fontSizeH = 1
                var fontSizeW = 1
                when (kitchenSettingModel.fonts) {
                    Constants.SMALL -> {
                        fontSizeH = 1
                        fontSizeW = 1
                    }

                    Constants.MEDIUM -> {
                        fontSizeH = 1
                        fontSizeW = 2
                    }

                    LARGE -> {
                        fontSizeH = 2
                        fontSizeW = 2
                    }


                }

                builder = Builder(pname, PrinterClass.language, requireActivity())
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(4)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    builder.addText("OrderID:" + receiptModel?.order?.id)
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.orderTypeName.toString())
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()
                LogUtil.logE(TAG, "LowerCAse ${tmps.trimmedLength()}")

                if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                    receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                    receiptModel?.order?.orderType.equals("Online Order", true) ||
                    receiptModel?.order?.orderType.equals("OnlineOrder", true)
                ) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.deliveryType.toString())
                }

//
//                builder.addTextLineSpace(30)
//                builder.addFeedUnit(30)
//                builder.addTextFont(Builder.FONT_E)
//                //  builder.addTextAlign(Builder.ALIGN_LEFT)
//                builder.addTextLang(Builder.LANG_EN)
//                builder.addTextSize(fontSizeH, fontSizeW)
//                builder.addTextStyle(
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.COLOR_1
//                )
//
//
//                builder.addText(
//                    padLine(
//                        "ReceiptID:" + receiptModel?.order?.offlineId,
//                        "",
//                        33
//                    )
//                )
                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            33
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLine(builder)


                receiptModel?.order?.orderItems?.let {
                    addOrdersForKitchen(
                        builder!!,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(receiptModel?.order?.note.toString())
                }

                addHorizontalKitchenLine(builder)
                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (receiptModel?.order?.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.formatPhoneNumber(
                                        receiptModel?.order?.customer?.phones?.get(
                                            0
                                        )?.phoneNumber.toString()
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
                     builder.addFeedUnit(30)
                     builder.addTextFont(Builder.FONT_E)
                     builder.addTextAlign(Builder.ALIGN_LEFT)
                     //builder.addTextLineSpace(20)
                     builder.addTextLang(Builder.LANG_EN)
                     builder.addTextSize(1, 1)
                     builder.addTextStyle(
                         Builder.FALSE,
                         Builder.FALSE,
                         Builder.TRUE,
                         Builder.COLOR_1
                     )
                     builder.addText(receiptModel?.order?.customer?.email)*/

                        if (kitchenSettingModel.showCustomerAddress) {
                            if (receiptModel?.order?.orderType?.trim().toString()
                                    .lowercase() == "Open Order".trim()
                                    .toString().lowercase()
                                && receiptModel?.order?.deliveryType?.trim().toString()
                                    .lowercase() == "Pickup".trim().lowercase()
                            ) {

                            } else {

                                if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                                    builder.addTextLineSpace(30)
                                    builder.addFeedUnit(30)
                                    builder.addTextFont(Builder.FONT_E)
                                    builder.addTextAlign(Builder.ALIGN_LEFT)
                                    //builder.addTextLineSpace(20)
                                    builder.addTextLang(Builder.LANG_EN)
                                    builder.addTextSize(fontSizeH, fontSizeW)
                                    builder.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.TRUE,
                                        Builder.COLOR_1
                                    )
                                    receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                        ?.forEach {

                                            if (it.typeOfAddress.equals(
                                                    BILLING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                builder!!.addText(
                                                    it.fullAddress
                                                )
                                            }
                                        }

                                    //  builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                                }
                            }
                        }

                    }
                }
            } else {

                var fontSizeH = 1
                var fontSizeW = 1
                when (kitchenSettingModel.fonts) {
                    Constants.SMALL -> {
                        fontSizeH = 1
                        fontSizeW = 1
                    }

                    Constants.MEDIUM -> {
                        fontSizeH = 1
                        fontSizeW = 2
                    }

                    LARGE -> {
                        fontSizeH = 2
                        fontSizeW = 2
                    }


                }

                builder = Builder(pname, PrinterClass.language, requireActivity())

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText(
                        "OrderID:" + receiptModel?.order?.custom_order_id
                    )
                } else {
                    builder.addText(
                        "OrderID:" + receiptModel?.order?.id
                    )
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.orderTypeName.toString())
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()
                LogUtil.logE(TAG, "LowerCAse ${tmps.trimmedLength()}")



                if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                    receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                    receiptModel?.order?.orderType.equals("Online Order", true) ||
                    receiptModel?.order?.orderType.equals("OnlineOrder", true)
                ) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, receiptModel?.order?.deliveryType.toString())
                }


//                builder.addTextLineSpace(30)
//                builder.addFeedUnit(30)
//                builder.addTextFont(Builder.FONT_E)
//                //  builder.addTextAlign(Builder.ALIGN_LEFT)
//                builder.addTextLang(Builder.LANG_EN)
//                builder.addTextSize(fontSizeH, fontSizeW)
//                builder.addTextStyle(
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.FALSE,
//                    Builder.COLOR_1
//                )
//
//
//                builder.addText(
//                    padLine(
//                        "ReceiptID:" + receiptModel?.order?.offlineId,
//                        "",
//                        if (kitchenSettingModel.fonts == LARGE) {
//                            24
//                        } else {
//                            48
//                        }
//                    )
//                )
                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            if (kitchenSettingModel.fonts == LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        if (kitchenSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addFeedLine(1)



                addHorizontalLine(builder)

                receiptModel?.order?.orderItems?.let {
                    addOrdersForKitchen(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(receiptModel?.order?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (receiptModel?.order?.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.formatPhoneNumber(
                                        receiptModel?.order?.customer?.phones?.get(
                                            0
                                        )?.phoneNumber.toString()
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
                 builder.addFeedUnit(30)
                 builder.addTextFont(Builder.FONT_E)
                 builder.addTextAlign(Builder.ALIGN_LEFT)
                 //builder.addTextLineSpace(20)
                 builder.addTextLang(Builder.LANG_EN)
                 builder.addTextSize(1, 1)
                 builder.addTextStyle(
                     Builder.FALSE,
                     Builder.FALSE,
                     Builder.TRUE,
                     Builder.COLOR_1
                 )
                 builder.addText(receiptModel?.order?.customer?.email)*/

                        if (kitchenSettingModel.showCustomerAddress) {
                            if (receiptModel?.order?.orderType?.trim().toString()
                                    .lowercase() == "Open Order".trim()
                                    .toString().lowercase()
                                && receiptModel?.order?.deliveryType?.trim().toString()
                                    .lowercase() == "Pickup".trim().lowercase()
                            ) {

                            } else {

                                if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                                    builder.addTextLineSpace(30)
                                    builder.addFeedUnit(30)
                                    builder.addTextFont(Builder.FONT_E)
                                    builder.addTextAlign(Builder.ALIGN_LEFT)
                                    //builder.addTextLineSpace(20)
                                    builder.addTextLang(Builder.LANG_EN)
                                    builder.addTextSize(fontSizeH, fontSizeW)
                                    builder.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.TRUE,
                                        Builder.COLOR_1
                                    )

                                    receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                        ?.forEach {

                                            if (it.typeOfAddress.equals(
                                                    BILLING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                builder.addText(
                                                    it.fullAddress
                                                )
                                            }
                                        }
                                    // builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                                }
                            }
                        }

                    }
                }

            }

            builder.addFeedLine(5)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)

            var timeOut = PrinterClass.SEND_TIMEOUT
            if (customerReceiptPrinters.printer_type == BLUETOOTH) {
                timeOut = BLUETOOTH_TIMEOUT
            }

            if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
            ) {

                timeOut = 10000
            }

            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    timeOut, status, battery
                )

                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
//                printerDialog.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmi(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        try {

            runBlocking {

                delay(500)

                PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
                SunmiPrinterApi.getInstance().printerInit()

                /*Added By Rahul */
                if (isOrderUpdated == true) {
                    PrintSunmiUtils.orderIdLarge("***** UPDATED *****")
                }

                SunmiPrinterApi.getInstance().lineWrap(4)
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

                if (kitchenSettingModel.showOrderType) {


                    PrintSunmiUtils.printOrderType(receiptModel?.order?.orderTypeName.toString())
                    SunmiPrinterApi.getInstance().lineWrap(1)

                }



                if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                    receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                    receiptModel?.order?.orderType.equals("Online Order", true) ||
                    receiptModel?.order?.orderType.equals("OnlineOrder", true)
                ) {
                    PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())
                    SunmiPrinterApi.getInstance().lineWrap(1)
                }


                /*  if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                      .toString().lowercase() || receiptModel?.order?.orderType.toString()
                      .lowercase() == "Open Order".trim()
                      .toString().lowercase()
              ) {

                  PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())
                  SunmiPrinterApi.getInstance().lineWrap(1)
              }*/


                if (kitchenSettingModel.showTeamMember) {

                    PrintSunmiUtils.employee(
                        padLine(
                            "Employee:" + receiptModel?.order?.employee?.name, "",
                            if (kitchenSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                    )


                }
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderTime(
                    padLine(
                        getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        ),
                        "",
                        if (kitchenSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                )



                PrintSunmiUtils.addHorizontal()
                SunmiPrinterApi.getInstance().lineWrap(1)

                Log.e(
                    TAG,
                    "getValueUpdate:  ${
                        prefProvider.getValueboolean(
                            OPEN_ORDER_UPDATE_FOR_PRINT,
                            false
                        )
                    }"
                )
                if (prefProvider.getValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false) == true) {
                    receiptModel?.order?.orderItems?.let {
                        var printOrderItems = checkOrderItemsForOpenORderUpdate()

                        Log.e(TAG, "printeOrderItems  ${Gson().toJson(printOrderItems)}")


                        addOrdersForKitchen(
                            if (printOrderItems.isNotEmpty()) printOrderItems else it,
                            kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                        )
                    }


                } else {

                    receiptModel?.order?.orderItems?.let {

                        addOrdersForKitchen(
                            it,
                            kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                        )
                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
                if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                    PrintSunmiUtils.orderNote(receiptModel?.order?.note.toString())

                }

                SunmiPrinterApi.getInstance().lineWrap(1)
                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (receiptModel?.order?.customer != null) {

                        PrintSunmiUtils.customerDetails()

                        if (kitchenSettingModel.showCustomerName) {

                            PrintSunmiUtils.customerName(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                                receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                    PrintSunmiUtils.customerPhone(
                                        MethodUtils.formatPhoneNumber(it)
                                    )
                                }
                            }

                        }

                        if (kitchenSettingModel.showCustomerAddress) {
                            if (receiptModel?.order?.orderType?.trim().toString()
                                    .lowercase() == "Open Order".trim()
                                    .toString().lowercase()
                                && receiptModel?.order?.deliveryType?.trim().toString()
                                    .lowercase() == "Pickup".trim().lowercase()
                            ) {

                            } else {

                                if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                                    receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                        ?.forEach {

                                            if (it.typeOfAddress.equals(
                                                    BILLING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                PrintSunmiUtils.customerAddress(
                                                    it.fullAddress
                                                )
                                            }
                                        }

//                                receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                    PrintSunmiUtils.customerAddress(
//                                        it
//                                    )
//                                }
                                }
                            }
                        }

                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(2)
                PrintSunmiUtils.cutPaper()

                //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmiInner(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        try {
            // PrintSunmiUtils.fontSizeInner(LARGE)
            SunmiPrintHelper.getInstance().initPrinter()
            /*Added By Rahul */
            try {
                if (isOrderUpdated == true || cartList!!.isEdited == true) {
                    PrintSunmiUtils.headerText("***** UPDATED *****")
                }
            } catch (e: java.lang.NullPointerException) {

            }

            try {
                SunmiPrintHelper.getInstance().lineWrap(4)
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.id)
                }
            } catch (e: Exception) {
            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(receiptModel?.order?.orderTypeName.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (receiptModel?.order?.orderType.equals("Online Order", true) ||
                receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                receiptModel?.order?.orderType.equals(PHONE_ORDER, true)
            ) {
                PrintSunmiUtils.headerText(receiptModel?.order?.deliveryType.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (kitchenSettingModel.showTeamMember) {
                PrintSunmiUtils.normalTextLarge("Employee:" + receiptModel?.order?.employee?.name)
            }
            PrintSunmiUtils.normalTextLarge(
                getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    receiptModel?.order?.createdAt.toString()
                )
            )
            SunmiPrintHelper.getInstance().lineWrap(1)


            PrintSunmiUtils.addHorizontalInner()
            SunmiPrintHelper.getInstance().lineWrap(1)

            receiptModel?.order?.orderItems?.let {

                addOrdersForKitchenInner(
                    it,
                    kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )
            }


            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(receiptModel?.order?.note.toString())

            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                if (receiptModel?.order?.customer != null) {


                    PrintSunmiUtils.customerDetailsInner()

                    if (kitchenSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalTextLarge(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                PrintSunmiUtils.normalTextLarge(
                                    MethodUtils.formatPhoneNumber(it)
                                )
                            }
                        }

                    }

                    if (kitchenSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.orderType?.trim().toString()
                                .lowercase() == "Open Order".trim()
                                .toString().lowercase()
                            && receiptModel?.order?.deliveryType?.trim().toString()
                                .lowercase() == "Pickup".trim().lowercase()
                        ) {

                        } else {

                            if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {


//                                receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                    PrintSunmiUtils.normalTextLarge(
//                                        it
//                                    )
//                                }

                                receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                    ?.forEach {

                                        if (it.typeOfAddress.equals(
                                                BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            PrintSunmiUtils.normalTextLarge(
                                                it.fullAddress
                                            )
                                        }
                                    }
                            }
                        }
                    }

                }
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            PrintSunmiUtils.cutPaperInner()

            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())

        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }


    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager = requireContext().getSystemService(WINDOW_SERVICE) as WindowManager?

        // initializing a variable for default display.

        // initializing a variable for default display.
        val display: Display = manager!!.defaultDisplay

        // creating a variable for point which
        // is to be displayed in QR Code.

        // creating a variable for point which
        // is to be displayed in QR Code.
        val point = Point()
        display.getSize(point)

        // getting width and
        // height of a point

        // getting width and
        // height of a point
        val width: Int = point.x
        val height: Int = point.y

        // generating dimension from width and height.

        // generating dimension from width and height.
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4

        LogUtil.logE(TAG, "getDimen:  ${dimen}")
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    private fun backpress() {
        MethodUtils.hideKeyboard(requireActivity())
        binding.edtPhoneNo.text?.clear()
        binding.edtEmail.text?.clear()
        binding.llSendReceipt.visibility = View.GONE
        binding.imgBack.visibility = View.GONE
        binding.txtHome.visibility = View.GONE
        binding.txtAddCustomer.visibility = View.GONE
        binding.llOptions.visibility = View.VISIBLE
        binding.llEmail.background =
            resources.getDrawable(R.drawable.background_square_border_grey)
        binding.llEmail.setTextColor(resources.getColor(R.color.txtColor))
        binding.llMessage.background =
            resources.getDrawable(R.drawable.background_square_border_grey)
        binding.llMessage.setTextColor(resources.getColor(R.color.txtColor))
    }

    fun removePrefrenceDinein() {
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.REDIRECT_FROM, "")
        prefProvider.setValue(TOTAL_PRICE_DINEIN, "")
        prefProvider.setValue(SUB_TOTAL_DINEIN, "")
        prefProvider.setValueInt(PAYMENT_ID, 0)
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TIPS_AMOUNT_DINEIN, "")
        prefProvider.setValue(Constants.TAX_CHARGE_DINEIN, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE_DINEIN, "")
        prefProvider.setValueInt("orderId", -1)
        prefProvider.setValue(SPLIT_DINEIN_MODEL, "")
        prefProvider.setValue(SPLIT_IS_GUESTPAY, "")
        prefProvider.setValue(SPLIT_DINEIN_CHECKOUT, "")


    }


    fun removeCustomer() {
        prefProvider.setValue(
            Constants.OPEN_ORDER_ITEMS,
            ""
        )

        /*Added By Rahul */
        prefProvider.setValue(
            Constants.OPEN_ORDER_ITEMS_OLD,
            ""
        )
        prefProvider.setValueboolean(OPEN_ORDER_UPDATE_FOR_PRINT, false)
        prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(WHOLE_AMOUNT, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValue(Constants.REDIRECT_FROM, "")
        prefProvider.setValue(SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValueInt("ORDER_ID", -1)

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt -> removeCustomer()_ ORDER_ID -> ${
                    Gson().toJson(prefProvider.getValueInt("ORDER_ID", -2))
                } _2"
            )
        )

        prefProvider.setValueInt(PAYMENT_ID, 0)
        prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.SUB_TOTAL_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_ACTUAL, "0.0")
        prefProvider.setValue(
            Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
            "0.0"
        )
        prefProvider.setValue(Constants.TAX_CHARGE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")
        prefProvider.setValue(SPLIT_DINEIN_MODEL, "")
        prefProvider.setValue(SPLIT_IS_GUESTPAY, "")
        prefProvider.setValue(SPLIT_DINEIN_CHECKOUT, "")
        prefProvider.setValueboolean(IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        prefProvider.setValueboolean(LOYALTY_ADDED, false)
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        backpress()
                    }
                }
            }
        }

        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                binding.txtAddCustomer.visibility = View.GONE
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->

                    }
                }
            }
        }


    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {
        LogUtil.logE(TAG, "onStatusChangePrinter:  $p0")

    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {
        LogUtil.logE(TAG, "onBatteryEventPrinter:  $p0")

    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input: InputStream = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            // Log exception
            null
        }
    }


    fun progressDialog() {
        pd = Dialog(requireActivity())
        pd?.setContentView(R.layout.view_loading)
        // pd.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL)
//        pd.setMessage("Please Wait..")
        pd?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pd?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        pd?.setCanceledOnTouchOutside(false)
        pd?.setCancelable(false)
        pd?.show()


    }

    override fun onPause() {
        super.onPause()
        if (pd != null && pd?.isShowing == true) {
            pd?.dismiss()
        }

        /*This if condition is added by Rahul to print the "Updated" text in the kitchen receipt when split payment is done*/
        if (!isSpilt) {
            prefProvider.setValue(Constants.OPEN_ORDER_ITEMS, "")

            /*Added By Rahul */
            prefProvider.setValue(Constants.OPEN_ORDER_ITEMS_OLD, "")

            prefProvider.setValue("BEFORE_ORDER_NOTE", "")
        }
        if (this::presentation.isInitialized) {
//            presentation.hide()
        }
    }

    private fun sunmiPrinterInit(ipAddress: String, isAutoPrint: Boolean) {


        try {
            SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiBlueToothPrinter, ipAddress)
        } catch (e: Exception) {
            SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiNetPrinter, ipAddress)
        }

        connect()

    }

    fun connect() {
        if (!SunmiPrinterApi.getInstance().isConnected) {
            pd?.dismiss()
            SunmiPrinterApi.getInstance()
                .connectPrinter(requireContext(), object : ConnectCallback {

                    override fun onFound() {
                        println("onFound")
                    }

                    override fun onUnfound() {
                        println("onUnfound")
                    }

                    override fun onConnect() {
                        println("onConnect")
                        if (IS_GIFT_CARD_TYPE) {
                            sunmiCloudPrintForGiftCard()
                        } else {
                            sunmiPrint()
                        }
                    }

                    override fun onDisconnect() {
                        println("onDisconnect")
                    }

                })
        } else {
            if (IS_GIFT_CARD_TYPE) {
                sunmiCloudPrintForGiftCard()
            } else {
                sunmiPrint()
            }
        }
    }

    /**
     * This method is used to print the receipt for sell gift card or add value in gift card.
     * This method prints from Sunmi Cloud Printer
     * */
    private fun sunmiCloudPrintForGiftCard() = try {

        PrintSunmiUtils.fontSize(customerSettingModel.fonts)

        SunmiPrinterApi.getInstance().printerInit()

        if (customerSettingModel.showOrderIdTop) {
            PrintSunmiUtils.orderIdLarge("OrderID:" + giftCardReceiptModel?.gift_card?.id)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        if (customerSettingModel.showVenueLogo
            && prefProvider.getValue(VENUE_LOGO, "").isNotEmpty()
        ) {
            printBusinessLogo()
        }

        PrintSunmiUtils.printBusinessDetails(
            prefProvider.getValue(BUSINESS_NAME, ""),
            if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                BUSINESS_ADDRESS,
                ""
            ) else "",
            if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                BUSINESS_PHONE_NO,
                ""
            ) else ""
        )
        if (customerSettingModel.showWebsiteAddress) {
            PrintSunmiUtils.venueWebsite(prefProvider.getValue(BUSINESS_WEBSITE, ""))
        } else {
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        if (customerSettingModel.showOrderType) {
            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.printOrderType(
                giftCardReceiptModel?.gift_card?.order_type_name ?: "TakeOut"
            )
        }

        if (customerSettingModel.fonts == LARGE) {

            PrintSunmiUtils.receiptID(
                //"ReceiptID:" + giftCardReceiptModel?.gift_card?.id
                "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                    giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                )?.offline_id
            )

            if (customerSettingModel.showTeam) {
                PrintSunmiUtils.employee("Employee: ${giftCardReceiptModel?.gift_card?.employee?.name}")
            }

            if (customerSettingModel.showOrderTime) {

                PrintSunmiUtils.orderTime(
                    "Order Time:" + getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        giftCardReceiptModel?.gift_card?.payments?.get(
                            giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                        )?.created_at!!
                    )
                )

            }

            if (customerSettingModel.showPrintTime) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    PrintSunmiUtils.orderTime(
                        "Print Time:" + getCurrentTimeFromTimeZone(
                            requireContext(),
                            MethodUtils.formatted()
                        )
                    )

                }

            }
        } else {


            val str = padLine(
                //"ReceiptID:" + giftCardReceiptModel?.gift_card?.id
                "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                    giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                        1
                    ) ?: 0
                )?.offline_id,
                "",
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString().trim()

            PrintSunmiUtils.orderId(str)

            if (customerSettingModel.showTeam) {

                val empName = padLine(
                    if (customerSettingModel.showTeam) {
                        "Employee: ${giftCardReceiptModel?.gift_card?.employee?.name}"
                    } else {
                        ""
                    },
                    "", if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.employee(empName)

            }
            if (customerSettingModel.showOrderTime
                && giftCardReceiptModel?.gift_card?.payments?.get(
                    giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                        1
                    ) ?: 0
                )?.created_at?.isNotEmpty() == true
            ) {

                val orderTime = padLine(
                    if (customerSettingModel.showOrderTime) {
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            giftCardReceiptModel?.gift_card?.payments?.get(
                                giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                                    1
                                ) ?: 0
                            )?.created_at!!
                        )
                    } else {
                        ""
                    },
                    "", if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.orderTime(orderTime)

            }

            if (customerSettingModel.showPrintTime && giftCardReceiptModel?.gift_card?.payments?.get(
                    giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                )?.created_at?.isNotEmpty() == true
            ) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    val printTime = padLine(
                        if (customerSettingModel.showPrintTime) {
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.orderTime(printTime)

                }
            }
        }


        PrintSunmiUtils.addHorizontal()

        val giftCardList: MutableList<CreateOrderResponse.Data.Order.OrderItem> = mutableListOf()

        var giftCardAmount = 0.00
        if (giftCardReceiptModel?.gift_card?.payments != null && giftCardReceiptModel?.gift_card?.payments?.isNotEmpty()!!) {
            giftCardAmount =
                giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.amount?.toPrecision(
                    2
                )?.toDoubleWithPrecision(2)!!
        }

        Log.d(TAG, "sunmiCloudPrintForGiftCard: giftCardAmount = ${giftCardAmount.toPrecision(2)}")

        giftCardList.add(
            0, CreateOrderResponse.Data.Order.OrderItem(
                itemName = "${giftCardReceiptModel?.gift_card?.gift_card_type} Gift Card",
                price = giftCardAmount, quantity = 1
            )
        )

        addOrderItems(
            giftCardList,
            false,
            customerSettingModel.fonts
        )

        SunmiPrinterApi.getInstance().lineWrap(1)

        val str5 = padLine(
            "Total Price",
            "$${giftCardAmount.toPrecision(2)}",
            if (customerSettingModel.fonts == LARGE) 23 else 48
        ).toString()
        PrintSunmiUtils.totalPrice(str5)

        val str6 = padLine(
            "Paid Amount",
            "$" + MethodUtils.roundOffAmountString(
                paidAmount
            ), if (customerSettingModel.fonts == LARGE) 23 else 48
        ).toString()
        PrintSunmiUtils.totalPrice(str6)

        if (customerSettingModel.showRefundAmount) {

            val str7 = padLine(
                "Change Amount",
                "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.changeAmount(str7)
            SunmiPrinterApi.getInstance().lineWrap(2)

        }

        val str10 = padLine(
            "Transaction ID",
            "" + giftCardReceiptModel?.gift_card?.payments?.size?.minus(1)
                ?.let { giftCardReceiptModel?.gift_card?.payments?.get(it)?.id },
            if (customerSettingModel.fonts == LARGE) 23 else 48
        ).toString()
        PrintSunmiUtils.transactionId(str10)

        val str11 = padLine(
            "Transaction Type",
            giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type,
            if (customerSettingModel.fonts == LARGE) 23 else 48
        ).toString()
        PrintSunmiUtils.transactionType(str11)


        if (giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type?.lowercase() == "Card".lowercase()) {


            val str12 =
                giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_name.toString()


            val str13 =
                giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_type.toString()


            val str14 =
                giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_number.toString()

            PrintSunmiUtils.cardDetails(str12, str13, str14)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

            if (giftCardReceiptModel?.gift_card?.customer != null && !prefProvider.getValue(
                    Constants.RECEIPT_CUSTOMER_NAME,
                    ""
                ).toString().equals("") && !prefProvider.getValue(
                    Constants.RECEIPT_CUSTOMER_NAME,
                    ""
                ).toString().equals("kotlin.Unit", true)
            ) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.customerDetails()

                if (customerSettingModel.showCustomerName) {

                    PrintSunmiUtils.customerName(giftCardReceiptModel?.gift_card?.customer?.firstName + " " + giftCardReceiptModel?.gift_card?.customer?.lastName)

                }
                if (customerSettingModel.showCustomerPhone) {

                    if (giftCardReceiptModel?.gift_card?.customer?.phones?.isNotEmpty() == true) {

                        val number = giftCardReceiptModel?.gift_card?.customer?.phones?.size?.minus(
                            1
                        )?.let {
                            giftCardReceiptModel?.gift_card?.customer?.phones?.get(
                                it
                            )?.phoneNumber
                        }
                        PrintSunmiUtils.customerPhone(
                            padLine(
                                number?.let { MethodUtils.formatPhoneNumber(it) },
                                "",
                                if (customerSettingModel.fonts == LARGE) 23 else 48
                            ).toString()
                        )


                    }


                }



                if (customerSettingModel.showCustomerAddress && !prefProvider.getValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        ""
                    ).toString()
                        .equals("") && !prefProvider.getValue(Constants.RECEIPT_CUSTOMER_NAME, "")
                        .toString().equals("kotlin.Unit", true)
                ) {
                    if (giftCardReceiptModel?.gift_card?.customer?.addresses?.isNotEmpty() == true) {

                        giftCardReceiptModel?.gift_card?.customer?.addresses!!.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                            .forEach {

                                if (it.typeOfAddress.equals(
                                        SHIPPING_ADDRESS,
                                        ignoreCase = true
                                    )
                                ) {
                                    PrintSunmiUtils.customerAddress(
                                        it.fullAddress
                                    )
                                }
                            }


//                            PrintSunmiUtils.customerAddress(
//                                padLine(
//                                    giftCardReceiptModel?.gift_card?.customer?.addresses?.size?.minus(1)?.let {
//                                        giftCardReceiptModel?.gift_card?.customer?.addresses?.get(
//                                            it
//                                        )?.fullAddress
//                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
//                                ).toString()
//                            )

                    }
                }

            }
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        SunmiPrinterApi.getInstance().lineWrap(2)
        val str8 = padLine(
            "Customer Signature",
            "     _________________________",
            48
        ).toString()

        PrintSunmiUtils.customerSignature(str8)

        PrintSunmiUtils.cutPaper()

        pd?.dismiss()

    } catch (e: Exception) {
        pd?.dismiss()
        e.printStackTrace()
    }

    private fun sunmiPrint() {


        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            SunmiPrinterApi.getInstance().printerInit()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.order?.id)

                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")
            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {

                printBusinessLogo()

            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )
            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

//            SunmiPrinterApi.getInstance().setAlignMode(1)
//            SunmiPrinterApi.getInstance().enableBold(false)
//            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
//            receiptModel?.order?.venue_website?.let { SunmiPrinterApi.getInstance().printText(it) }
//            SunmiPrinterApi.getInstance().lineWrap(1)


            if (customerSettingModel.showOrderType) {
                SunmiPrinterApi.getInstance().lineWrap(1)

                receiptModel?.order?.orderTypeName?.trim()
                    ?.let { PrintSunmiUtils.printOrderType(it) }
            }


            if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                receiptModel?.order?.orderType.equals("Online Order", true) ||
                receiptModel?.order?.orderType.equals("OnlineOrder", true)
            ) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                receiptModel?.order?.deliveryType?.let { PrintSunmiUtils.deliveryType(it) }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }



            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.receiptID("ReceiptID:" + receiptModel?.order?.offlineId)


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + receiptModel?.order?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderTime(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.orderTime(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )

                    }


                }
            } else {


                val str = padLine(
                    "ReceiptID:" + receiptModel?.order?.offlineId?.trim(),
                    "",
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.orderId(str)

                if (customerSettingModel.showTeam) {

                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + receiptModel?.order?.employee?.name
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.employee(empName)

                }
                if (customerSettingModel.showOrderTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.order?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)

                }

                if (customerSettingModel.showPrintTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }


            PrintSunmiUtils.addHorizontal()


            Log.e(TAG, "orderItemsGetD:  ${Gson().toJson(receiptModel?.order?.orderItems)}")
            receiptModel?.order?.orderItems?.let {
                addOrderItems(
                    it,
                    customerSettingModel.showModifiers,
                    customerSettingModel.fonts
                )
            }
            SunmiPrinterApi.getInstance().lineWrap(1)


            if (receiptModel?.order?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",
                    if (receiptModel?.order?.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.totalDiscount(str1)

            }


            var totalDiscount: Double = 0.0
            receiptModel?.order?.totalDiscount?.let {
                totalDiscount = it
            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.subTotal!!),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.subTotal(str2)

            if (receiptModel?.order?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalTaxAmount!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.tax(str3)

            }

            if (receiptModel?.order?.totalServiceCharges != null && (receiptModel?.order?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {


                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalServiceCharges!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.serviceCharge(str4)
            }


            if (tipAmount > 0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount.toDouble()),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.tips(str8)

            }

            if (customerSettingModel.showCashDisSurCharg) {

                if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.paymentType?.lowercase() == "Card".lowercase() && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "SurCharge".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {


                        val str8 = padLine(
                            Constants.SURCHARGE_TEXT,
                            "$" + MethodUtils.roundOffAmountString(
                                receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.cashDiscount(str8)

                    }


                } else if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {

                        val str8 = padLine(
                            "Cash Discount",
                            if (receiptModel?.order?.totalCashDiscountFee == 0.0) {
                                "$" + MethodUtils.roundOffAmountString(
                                    receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0
                                )
                            } else {
                                "-$" + MethodUtils.roundOffAmountString(
                                    receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0
                                )
                            }, if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.cashDiscount(str8)


                    }
                }
            }




            if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                    if (receiptModel?.order?.loyaltyAmount != 0.0) {


                        val str8 = padLine(
                            "Used Loyalty Amount",
                            "-$" + receiptModel?.order?.loyaltyAmount?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }, if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.loyaltyAmount(str8)

                    }


                    val str8 = padLine(
                        "Used Loyalty Points",
                        receiptModel?.order?.payments!![0].loyaltyUSedPoints.toString(),
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.loyaltyPoint(str8)

                }
            }


            var totalfamount = 0.0

            if (receiptModel?.order?.totalAmount != null) {
                SunmiPrinterApi.getInstance().lineWrap(1)

                if (paymentType == "Cash") {

                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    //PLZCHECK
                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()) {

                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(
                                finalAmt - (receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0)
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.totalPrice(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt - (receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.cash_discount_or_surcharge ?: 0.0)
                        )
                    } else {


                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(finalAmt),
                            if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.totalPrice(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)

                    }

                } else {
                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    totalfamount = finalAmt

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "SurCharge".lowercase()) {


                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(
                                finalAmt.plus(
                                    (receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0)
                                )
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.totalPrice(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt.plus(
                                (receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0)
                            )
                        )
                    } else {

                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(finalAmt),
                            if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.totalPrice(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)
                    }
                }


            }

            val newPaidAmount = if (isCustomCash) {
                paidAmount
            } else {
                paidAmount + tipAmount
            }

            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    newPaidAmount
                ), if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.totalPrice(str6)



            if (customerSettingModel.showRefundAmount) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.changeAmount(str7)
                SunmiPrinterApi.getInstance().lineWrap(2)

            }


            if (isSpilt) {

                val str7 = padLine(
                    "Remaining Amount",
                    "$" + MethodUtils.roundOffAmountString(remainingAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.refundAmount(str7)
                SunmiPrinterApi.getInstance().lineWrap(2)

            }



            if (receiptModel?.order?.totalTips == 0.0) {


                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == LARGE) {
                        PrintSunmiUtils.tips("Tips      _____________")
                        PrintSunmiUtils.tips("Total     _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
                        PrintSunmiUtils.tips("Total                             _____________")
                    }


                }


            }


            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTips()

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        tipsList,
                        if (receiptModel?.order?.totalDiscount != 0.0 && receiptModel?.order?.totalAmount ?: 0.0 > receiptModel?.order?.totalDiscount ?: 0.0) {
                            (totalfamount)
                        } else {
                            receiptModel?.order?.totalAmount!!
                        },
                        customerSettingModel.fonts
                    )
                    SunmiPrinterApi.getInstance().lineWrap(2)

                }
            }


            val str10 = padLine(
                "Transaction ID",
                "" + receiptModel?.order?.payments?.size?.minus(1)
                    ?.let { receiptModel?.order?.payments?.get(it)?.id },
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.transactionId(str10)

            val str11 = padLine(
                "Transaction Type",
                receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.transactionType(str11)


            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {


                val str12 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName.toString()


                var str13 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()


                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                    var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                    str13 =
                        paymentViewModel.extData.substring(
                            applabStartIndex + "<APPLAB>".length,
                            applabEndIndex
                        )
                }


                val str14 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString()

                PrintSunmiUtils.cardDetails(str12, str13, str14)
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null && !prefProvider.getValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        ""
                    ).toString()
                        .equals("") && !prefProvider.getValue(Constants.RECEIPT_CUSTOMER_NAME, "")
                        .toString().equals("kotlin.Unit", true)
                ) {


                    PrintSunmiUtils.customerDetails()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }
                    if (customerSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            val number = receiptModel?.order?.customer?.phones?.size?.minus(
                                1
                            )?.let {
                                receiptModel?.order?.customer?.phones?.get(
                                    it
                                )?.phoneNumber
                            }
                            PrintSunmiUtils.customerPhone(
                                padLine(
                                    number?.let { MethodUtils.formatPhoneNumber(it) },
                                    "",
                                    if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )


                        }


                    }



                    if (customerSettingModel.showCustomerAddress && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("") && !prefProvider.getValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            ""
                        ).toString().equals("kotlin.Unit", true)
                    ) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            receiptModel?.order?.customer?.addresses!!.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }


//                            PrintSunmiUtils.customerAddress(
//                                padLine(
//                                    receiptModel?.order?.customer?.addresses?.size?.minus(1)?.let {
//                                        receiptModel?.order?.customer?.addresses?.get(
//                                            it
//                                        )?.fullAddress
//                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
//                                ).toString()
//                            )

                        }
                    }

                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            LogUtil.logE(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(receiptModel?.order?.note!!)

            }

            /*  PrintSunmiUtils.tips("__________________________")
              SunmiPrinterApi.getInstance().lineWrap(1)*/

            SunmiPrinterApi.getInstance().lineWrap(2)
            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)

            if (customerSettingModel.showQrCode) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                receiptModel?.order?.digital_receipt_url?.let { PrintSunmiUtils.qrCode(it) }

            }

            PrintSunmiUtils.cutPaper()
            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())


            pd?.dismiss()

        } catch (e: Exception) {
            pd?.dismiss()
            e.printStackTrace()
        }

    }

    private fun sunmiPrintInner(isAutoPrint: Boolean) {

        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _sunmiPrintInner(isAutoPrint: Boolean) -> Here_2"))

        try {

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _sunmiPrintInner(isAutoPrint: Boolean) _ try entered-> Here_3"))

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()


            try {
                if (isOrderUpdated == true) {
//                    PrintSunmiUtils.orderIdLarge("***** 7 UPDATED *****")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {

                PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))

            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) {
                    prefProvider.getValue(BUSINESS_ADDRESS, "")
                } else "", prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                )
                /* This below code is commented because we need to show the phone number everytime, Business Phone number is not optional
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                ) else ""*/
            )


//            SunmiPrinterApi.getInstance().setAlignMode(1)
//            SunmiPrinterApi.getInstance().enableBold(false)
//            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
//            receiptModel?.order?.venue_website?.let { SunmiPrinterApi.getInstance().printText(it) }
//            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            /* if (customerSettingModel.showOrderType) {
                 receiptModel?.order?.orderTypeName?.trim()?.let { PrintSunmiUtils.headerText(it) }
                 SunmiPrintHelper.getInstance().lineWrap(1)
             }*/
            if (customerSettingModel.showOrderType) {
                receiptModel?.order?.orderTypeName?.trim()?.let {
                    PrintSunmiUtils.headerText(it)
                    if (!it.contains("Phone", true)) {
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    }

                }

            }

            if (receiptModel?.order?.orderType.equals(PHONE_ORDER, true) ||
                receiptModel?.order?.orderType.equals("OnlineWebOrder", true) ||
                receiptModel?.order?.orderType.equals("Online Order", true) ||
                receiptModel?.order?.orderType.equals("OnlineOrder", true)
            ) {

                receiptModel?.order?.deliveryType?.let { PrintSunmiUtils.headerText(it) }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            if (customerSettingModel.fonts == LARGE) {


                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.order?.offlineId)


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + receiptModel?.order?.employee?.name)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.order?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )

                    }


                }
            } else {


                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.order?.offlineId?.trim())

                if (customerSettingModel.showTeam) {

                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + receiptModel?.order?.employee?.name
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(empName)

                }
                if (customerSettingModel.showOrderTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.order?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)

                }

                if (customerSettingModel.showPrintTime && receiptModel?.order?.createdAt?.isNotEmpty() == true) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontalInner()

            receiptModel?.order?.orderItems?.let {
                addOrderItemsInner(
                    it,
                    customerSettingModel.showModifiers,
                    customerSettingModel.fonts
                )
            }
            SunmiPrintHelper.getInstance().lineWrap(1)


            if (receiptModel?.order?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",
                    if (receiptModel?.order?.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.normalText(str1)

            }


            var totalDiscount: Double = 0.0
            receiptModel?.order?.totalDiscount?.let {
                totalDiscount = it
            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.subTotal!!),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.normalText(str2)

            if (receiptModel?.order?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalTaxAmount!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str3)

            }

            if (receiptModel?.order?.totalServiceCharges != null && (receiptModel?.order?.serviceChargeEnabled == true) && prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {


                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalServiceCharges!!),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str4)
            }


            if (tipAmount > 0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount.toDouble()),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str8)

            }

            if (customerSettingModel.showCashDisSurCharg) {
                if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.paymentType?.lowercase() == "Card".lowercase() && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "SurCharge".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {


                        val str8 = padLine(
                            Constants.SURCHARGE_TEXT,
                            "$" + MethodUtils.roundOffAmountString(
                                receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.normalText(str8)

                    }


                } else if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size!! - 1
                    )?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()
                ) {

                    if (receiptModel?.order?.totalCashDiscountFee != null) {

                        val str8 = padLine(
                            "Cash Discount",
                            if (receiptModel?.order?.totalCashDiscountFee == 0.0) {
                                "$" + MethodUtils.roundOffAmountString(
                                    receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0
                                )
                            } else {
                                "-$" + MethodUtils.roundOffAmountString(
                                    receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0
                                )
                            }, if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.normalText(str8)


                    }
                }
            }




            if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                    if (receiptModel?.order?.loyaltyAmount != 0.0) {


                        val str8 = padLine(
                            "Used Loyalty Amount",
                            "-$" + receiptModel?.order?.loyaltyAmount?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }, if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.normalText(str8)

                    }


                    val str8 = padLine(
                        "Used Loyalty Points",
                        receiptModel?.order?.payments!![0].loyaltyUSedPoints.toString(),
                        if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(str8)


                }
            }


            var totalfamount = 0.0

            if (receiptModel?.order?.totalAmount != null) {
                SunmiPrintHelper.getInstance().lineWrap(1)

                if (paymentType == "Cash") {

                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    //PLZCHECK
                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "CashDiscount".lowercase()) {

                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(
                                finalAmt - (receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0)
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.boldText(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt - (receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.cash_discount_or_surcharge ?: 0.0)
                        )
                    } else {


                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(finalAmt),
                            if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.boldText(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)

                    }

                } else {
                    var finalAmt: Double = (receiptModel?.order?.subTotal
                        ?: 0.0).plus(receiptModel?.order?.totalTaxAmount ?: 0.0)
                        .plus(receiptModel?.order?.totalServiceCharges ?: 0.0).plus(
                            receiptModel?.order?.payments?.get(
                                receiptModel?.order?.payments?.size?.minus(1) ?: 0
                            )?.tips ?: 0.0
                        )

                    totalfamount = finalAmt

                    if (receiptModel?.order?.payments?.isNotEmpty() == true) {
                        if (receiptModel?.order?.payments?.get(0)?.isLoyaltyApplied == true && receiptModel?.order?.payments!![0].loyaltyUSedPoints != 0) {
                            finalAmt -= receiptModel?.order?.loyaltyAmount ?: 0.0
                        }
                    }

                    if (receiptModel?.order?.cash_discount_type?.lowercase() == "SurCharge".lowercase()) {


                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(
                                finalAmt.plus(
                                    (receiptModel?.order?.payments?.get(
                                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                    )?.cash_discount_or_surcharge ?: 0.0)
                                )
                            ), if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.boldText(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(
                            finalAmt.plus(
                                (receiptModel?.order?.payments?.get(
                                    receiptModel?.order?.payments?.size?.minus(1) ?: 0
                                )?.cash_discount_or_surcharge ?: 0.0)
                            )
                        )
                    } else {

                        val str5 = padLine(
                            "Total Price",
                            "$" + MethodUtils.roundOffAmountString(finalAmt),
                            if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()
                        PrintSunmiUtils.boldText(str5)


                        totalfamount = MethodUtils.roundOffAmountDouble(finalAmt)
                    }
                }


            }

            val newPaidAmount = if (isCustomCash) {
                paidAmount
            } else {
                paidAmount + tipAmount
            }

            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    newPaidAmount
                ), if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str6)



            if (customerSettingModel.showRefundAmount) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.boldText(str7)
                SunmiPrintHelper.getInstance().lineWrap(1)

            }


            if (isSpilt) {

                val str7 = padLine(
                    "Remaining Amount",
                    "$" + MethodUtils.roundOffAmountString(remainingAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.boldText(str7)
                SunmiPrintHelper.getInstance().lineWrap(1)

            }



            if (receiptModel?.order?.totalTips == 0.0) {

                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == LARGE) {
                        PrintSunmiUtils.boldText("Tips      _____________")
                        PrintSunmiUtils.boldText("Total     _____________")
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                        PrintSunmiUtils.boldText("Total                             _____________")
                    }

                    SunmiPrintHelper.getInstance().lineWrap(1)
                }


            }


            if (customerSettingModel.showTipSuggestion) {

                PrintSunmiUtils.additionalTipsInner()

                if (tipsList.isNotEmpty()) {
                    addTipsListInner(
                        tipsList,
                        if (receiptModel?.order?.totalDiscount != 0.0 && receiptModel?.order?.totalAmount ?: 0.0 > receiptModel?.order?.totalDiscount ?: 0.0) {
                            (totalfamount)
                        } else {
                            receiptModel?.order?.totalAmount!!
                        },
                        customerSettingModel.fonts
                    )
                    SunmiPrintHelper.getInstance().lineWrap(1)

                }
            }


            val str10 = padLine(
                "Transaction ID",
                "" + receiptModel?.order?.payments?.size?.minus(1)
                    ?.let { receiptModel?.order?.payments?.get(it)?.id },
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.normalText(str10)

            val str11 = padLine(
                "Transaction Type",
                receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType,
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.normalTextTest(str11)
            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {

                val strCardName =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName.toString()


                var strCardType =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()

                if (paymentViewModel.extData != null && !paymentViewModel.extData.isNullOrEmpty()) {

                    if (paymentViewModel.extData.contains("<APPLAB>")) {
                        var applabStartIndex = paymentViewModel.extData.indexOf("<APPLAB>")
                        var applabEndIndex = paymentViewModel.extData.indexOf("</APPLAB>")
                        strCardType =
                            paymentViewModel.extData.substring(
                                applabStartIndex + "<APPLAB>".length,
                                applabEndIndex
                            )
                    } else {
                        strCardType = "N/A"
                    }
                }


                val strCardNumber =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString()

                PrintSunmiUtils.cardDetailsInner(
                    strCardName,
                    strCardType,
                    strCardNumber,
                    customerSettingModel.fonts
                )
//                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null && !prefProvider.getValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        ""
                    ).toString()
                        .equals("") && !prefProvider.getValue(Constants.RECEIPT_CUSTOMER_NAME, "")
                        .toString().equals("kotlin.Unit", true)
                ) {

                    SunmiPrintHelper.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }
                    if (customerSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            val phone = receiptModel?.order?.customer?.phones?.size?.minus(
                                1
                            )?.let {
                                receiptModel?.order?.customer?.phones?.get(
                                    it
                                )?.phoneNumber
                            }
                            PrintSunmiUtils.normalText(
                                padLine(
                                    phone?.let { MethodUtils.formatPhoneNumber(it) },
                                    "",
                                    if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )


                        }


                    }



                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            receiptModel?.order?.customer?.addresses?.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {

                                        PrintSunmiUtils.normalText(
                                            padLine(
                                                it.fullAddress,
                                                "",
                                                if (customerSettingModel.fonts == LARGE) 23 else 48
                                            ).toString()
                                        )
                                    }
                                }


                        }
                    }

                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            LogUtil.logE(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInner(receiptModel?.order?.note!!)

            }

            //  PrintSunmiUtils.boldText("__________________________")
            SunmiPrintHelper.getInstance().lineWrap(1)

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (customerSettingModel.showQrCode) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                receiptModel?.order?.digital_receipt_url?.let { PrintSunmiUtils.qrCodeInner(it) }

            }

            PrintSunmiUtils.cutPaperInner()

            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType.equals(
                    "Cash",
                    true
                )
            ) {


                if (woyouService != null) {
                    try {
                        woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val aa = ByteArray(5)

                    aa[0] = 0x10
                    aa[1] = 0x14
                    aa[2] = 0x00
                    aa[3] = 0x00
                    aa[4] = 0x00


                    try {
                        SunmiPrinterApi.getInstance().sendRawData(aa)
                    } catch (e: java.lang.Exception) {
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _sunmiPrintInner(isAutoPrint: Boolean) _ catch (e: Exception)_2 -> ${
                                        Gson().toJson(
                                            e.printStackTrace()
                                        )
                                    }"
                                )
                            )
                        e.printStackTrace()

                    }
                    try {
                        if (isAutoPrint) {
                            SunmiPrintHelper.getInstance().openCashBox()
                        }
                    } catch (e: java.lang.Exception) {
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _sunmiPrintInner(isAutoPrint: Boolean) _ catch (e: Exception)_3 -> ${
                                        Gson().toJson(
                                            e.printStackTrace()
                                        )
                                    }"
                                )
                            )
                        e.printStackTrace()

                    }


                }
            }


            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())


            pd?.dismiss()

        } catch (e: Exception) {
            pd?.dismiss()

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _sunmiPrintInner(isAutoPrint: Boolean) _ catch (e: Exception) -> ${
                            Gson().toJson(
                                e.printStackTrace()
                            )
                        }"
                    )
                )

            e.printStackTrace()
        }

    }

    /**
     * This method is used to print the receipt for sell gift card or add value in gift card.
     * This method prints from Sunmi Inner Printer
     * */
    private fun sunmiInnerPrintForGiftCard() {

        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                PrintSunmiUtils.headerText("OrderID:" + giftCardReceiptModel?.gift_card?.id)
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {
                PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))
            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) {
                    prefProvider.getValue(BUSINESS_ADDRESS, "")
                } else "", prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                )
                /*if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    BUSINESS_PHONE_NO,
                    ""
                ) else ""*/
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(prefProvider.getValue(BUSINESS_WEBSITE, ""))
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                giftCardReceiptModel?.gift_card?.order_type_name?.trim()
                    ?.let { PrintSunmiUtils.headerText(it) }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == LARGE) {

                PrintSunmiUtils.normalText(
                    //"ReceiptID:" + giftCardReceiptModel?.gift_card?.id
                    "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                    )?.offline_id
                )

                if (customerSettingModel.showTeam) {
                    PrintSunmiUtils.normalText("Employee: ${giftCardReceiptModel?.gift_card?.employee?.name}")
                }

                if (customerSettingModel.showOrderTime) {
                    PrintSunmiUtils.normalText(
                        "Order Time:" + getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            giftCardReceiptModel?.gift_card?.payments?.get(
                                giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                            )?.created_at!!
                        )
                    )
                }

                if (customerSettingModel.showPrintTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }
                }
            } else {

                PrintSunmiUtils.normalText(
                    //"ReceiptID:" + giftCardReceiptModel?.gift_card?.id
                    "ReceiptID:" + giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                    )?.offline_id
                )

                if (customerSettingModel.showTeam) {

                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + giftCardReceiptModel?.gift_card?.employee?.name
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.normalText(empName)

                }
                if (customerSettingModel.showOrderTime && giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                            1
                        ) ?: 0
                    )?.created_at?.isNotEmpty() == true
                ) {

                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                giftCardReceiptModel?.gift_card?.payments?.get(
                                    giftCardReceiptModel?.gift_card?.payments?.size?.minus(
                                        1
                                    ) ?: 0
                                )?.created_at!!
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)

                }

                if (customerSettingModel.showPrintTime && giftCardReceiptModel?.gift_card?.payments?.get(
                        giftCardReceiptModel?.gift_card?.payments?.size?.minus(1) ?: 0
                    )?.created_at?.isNotEmpty() == true
                ) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontalInner()

            val giftCardList: MutableList<CreateOrderResponse.Data.Order.OrderItem> =
                mutableListOf()

            var giftCardAmount = 0.00
            if (giftCardReceiptModel?.gift_card?.payments != null && giftCardReceiptModel?.gift_card?.payments?.isNotEmpty()!!) {
                giftCardAmount =
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.amount?.toPrecision(
                        2
                    )?.toDoubleWithPrecision(2)!!
            }

            Log.d(
                TAG,
                "sunmiInnerPrintForGiftCard:  giftCardAmount = ${giftCardAmount.toPrecision(2)}"
            )

            giftCardList.add(
                0, CreateOrderResponse.Data.Order.OrderItem(
                    itemName = "${giftCardReceiptModel?.gift_card?.gift_card_type} Gift Card",
                    price = giftCardAmount, quantity = 1
                )
            )

            addOrderItemsInner(
                giftCardList,
                false,
                customerSettingModel.fonts
            )
            SunmiPrintHelper.getInstance().lineWrap(1)

            val str5 = padLine(
                "Total Price",
                "$${giftCardAmount.toPrecision(2)}",
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str5)

            val str6 = padLine(
                "Paid Amount",
                "$" + MethodUtils.roundOffAmountString(
                    paidAmount
                ), if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str6)

            if (customerSettingModel.showRefundAmount) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(changeAmtGlobal),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.boldText(str7)
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            val str10 = padLine(
                "Transaction ID",
                "" + giftCardReceiptModel?.gift_card?.payments?.size?.minus(1)
                    ?.let { giftCardReceiptModel?.gift_card?.payments?.get(it)?.id },
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str10)

            val str11 = padLine(
                "Transaction Type",
                giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type,
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.boldText(str11)

            if (giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type?.lowercase() == "Card".lowercase()) {

                val str12 =
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_name.toString()


                val str13 =
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_type.toString()


                val str14 =
                    giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.card_number.toString()

                PrintSunmiUtils.cardDetailsInner(str12, str13, str14, customerSettingModel.fonts)
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showCustomerAddress || customerSettingModel.showCustomerPhone || customerSettingModel.showCustomerName) {

                if (giftCardReceiptModel?.gift_card?.customer != null) {

                    SunmiPrintHelper.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(giftCardReceiptModel?.gift_card?.customer?.firstName + " " + giftCardReceiptModel?.gift_card?.customer?.lastName)

                    }
                    if (customerSettingModel.showCustomerPhone) {

                        if (giftCardReceiptModel?.gift_card?.customer?.phones?.isNotEmpty() == true) {

                            val phone =
                                giftCardReceiptModel?.gift_card?.customer?.phones?.size?.minus(
                                    1
                                )?.let {
                                    giftCardReceiptModel?.gift_card?.customer?.phones?.get(
                                        it
                                    )?.phoneNumber
                                }
                            PrintSunmiUtils.normalText(
                                padLine(
                                    phone?.let { MethodUtils.formatPhoneNumber(it) },
                                    "",
                                    if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )

                        }

                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (giftCardReceiptModel?.gift_card?.customer?.addresses?.isNotEmpty() == true) {

                            giftCardReceiptModel?.gift_card?.customer?.addresses?.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                ?.forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalText(
                                            padLine(
                                                it.fullAddress,
                                                "",
                                                if (customerSettingModel.fonts == LARGE) 23 else 48
                                            ).toString()
                                        )
                                    }
                                }

                        }
                    }

                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            SunmiPrintHelper.getInstance().lineWrap(1)

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)

            PrintSunmiUtils.cutPaperInner()

            if (giftCardReceiptModel?.gift_card?.payments?.get(giftCardReceiptModel?.gift_card?.payments?.size!! - 1)?.payment_type.equals(
                    "Cash", true
                )
            ) {
                if (woyouService != null) {
                    woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                } else {
                    val aa = ByteArray(5)

                    aa[0] = 0x10
                    aa[1] = 0x14
                    aa[2] = 0x00
                    aa[3] = 0x00
                    aa[4] = 0x00

                    try {
                        SunmiPrinterApi.getInstance().sendRawData(aa)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    try {
                        SunmiPrintHelper.getInstance().openCashBox()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }

                }
            }

            pd?.dismiss()

        } catch (e: Exception) {
            pd?.dismiss()
            e.printStackTrace()
        }

    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider.getValue(VENUE_LOGO, ""),
            Base64.NO_CLOSE
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, false)

        PrintSunmiUtils.printLogo(newBitmap)

    }

    private fun setService() {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                printDineInTable1Inner()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService()
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService1(isAutoPrint: Boolean) {
        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> Here"))

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> SunmiPrintHelper.getIns..().sunmiPrinter -> ${
                        Gson().toJson(
                            SunmiPrintHelper.getInstance().sunmiPrinter
                        )
                    }"
                )
            )

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> SunmiPrintHelper.FoundSunmiPrinter -> ${
                        Gson().toJson(
                            SunmiPrintHelper.FoundSunmiPrinter
                        )
                    }"
                )
            )

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> Here_2"))

            if (!BluetoothUtil.isBlueToothPrinter) {

                if (IS_GIFT_CARD_TYPE) {
                    sunmiInnerPrintForGiftCard()
                } else {
                    sunmiPrintInner(isAutoPrint)
                }

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> Here_3"))

            Handler(Looper.getMainLooper()).postDelayed({
                setService1(isAutoPrint)
            }, 2000)
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> Here_4"))

            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} OrderCompleteFragment.kt _setService1(isAutoPrint) -> SunmiPrintHelper.LostSunmi... -> ${
                            Gson().toJson(
                                SunmiPrintHelper.LostSunmiPrinter
                            )
                        }"
                    )
                )

        }
    }

    private fun setService2(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(kitchenReceiptPrinters, type)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService2(kitchenReceiptPrinters, type)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService(
        paymentType: String,
        listWTitems: java.util.ArrayList<TbCartItem>,
        listGuestItem: java.util.ArrayList<TbCartItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                printSunmiDineinInner(
                    paymentType,
                    listWTitems,
                    listGuestItem,
                    guestName,
                    finaldisLocal,
                    checkOutDineInModel,
                    orderDiscount,
                    guestTaxes,
                    guestSubTotal,
                    guestServiceCharge
                )


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService()
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            LogUtil.logE(TAG, "onServiceConnected  1")
            woyouService = IWoyouService.Stub.asInterface(service)

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            LogUtil.logE(TAG, "onServiceDisConnected  2")
            woyouService = null


        }

    }

    private fun Binding() {
        val intent = Intent()
        intent.setPackage("com.pays.pos")
        intent.action = "com.pays.pos.aidl.IWoyouService"
        MainApplication.getInstance()?.applicationContext?.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun asBinder(): IBinder {
        return woyouService?.asBinder()!!

    }

    override fun onRunResult(isSuccess: Boolean, code: Int, msg: String?) {

    }

    private fun clearObserver() {

        //System.gc()
        // requireActivity().cacheDir.delete()
        //  restartActivity()


        deleteCache(requireContext())


        dashboardViewModel.currentCartItems = arrayListOf()
        dashboardViewModel.duplicateCurrentCartItem = arrayListOf()

        dashboardViewModel.orderCompletedCount.value =
            dashboardViewModel.orderCompletedCount.value?.plus(1)
        dashboardViewModel.orderCompleted.value = true

        viewLifecycleOwnerLiveData.removeObservers(viewLifecycleOwner)



        Runtime.getRuntime().apply {
            gc()
            System.gc()
            freeMemory()
        }



        onDestroy()


    }

    fun deleteCache(context: Context) {
        try {
            val dir: File = context.cacheDir
            deleteDir(dir)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory()) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile()) {
            dir.delete()
        } else {
            false
        }
    }

    private fun restartActivity() {

        requireActivity().apply {
            val intent = intent
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }


}