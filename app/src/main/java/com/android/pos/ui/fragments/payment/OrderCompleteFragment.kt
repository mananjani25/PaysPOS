package com.android.pos.ui.fragments.payment


import android.content.Context.WINDOW_SERVICE
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.BLUETOOTH
import com.android.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.android.pos.data.remote.Constants.BUSINESS_NAME
import com.android.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.android.pos.data.remote.Constants.GUEST_POSITION
import com.android.pos.data.remote.Constants.KITCHEN
import com.android.pos.data.remote.Constants.LARGE
import com.android.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.android.pos.data.remote.Constants.SUB_TOTAL
import com.android.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.android.pos.databinding.FragmentOrderCompletBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.SplitListAdapter
import com.android.pos.utils.*
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.printer.PrinterClass.BLUETOOTH_TIMEOUT
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class OrderCompleteFragment : Fragment(), View.OnClickListener, StatusChangeEventListener,
    BatteryStatusChangeEventListener {
    private var tipAmount: Double = 0.0
    private var dineInList: ArrayList<DineInModel> = arrayListOf()
    private var customerPrinterDineIn: List<PrinterResponse.Data.CustomerReceiptPrinters>? = null
    private var isFromCustomer: Boolean = false
    private var dis_charge_value: Double = 0.0
    private var isGuest: Boolean = false
    private var remainingAmount: Double = 0.0
    private var splitPaidAmount: Double = 0.0
    private var splitValue: Int = -1
    private var subTotalWT = 0.0
    private var serviceCharge = 0.0
    private var totalTaxAmount = 0.0
    private var totalDiscount = 0.0

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
    private var orderID: Int = 0
    var paymentType: String = ""
    private var type: String = ""
    private var totalPrice: Double = 0.0
    private var paymentAmount: Double = 0.0
    private var paidAmountValue: Double = 0.0
    private var WholetotalPrice: Double = 0.0
    private val viewModel by viewModels<OrderCompleteViewModel>()
    private var receiptModel: CreateOrderResponse.Data? = null
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var splitList: ArrayList<SplitDetailListModel> = arrayListOf()

    private var isGuestPaymentTotal = false

    private var paidAmount: Double = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentOrderCompletBinding
    private val TAG = "OrderCompleteFragment"
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private lateinit var splitAdapter: SplitListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        observeTipsList()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) { it ->
                if (it.getString(Constants.KEY)?.lowercase() == "FROM_CUSTOMER".lowercase()) {
                    isFromCustomer = true
                }

            }
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()
        getCustomerPrinterForDineIn()
        splitAdapter = SplitListAdapter()
        prefProvider.setValueboolean(Constants.IS_ORDER_UPDATE, value = false)
        binding.rvSplits.adapter = splitAdapter
        if (requireArguments().getBoolean("isSpilt")) {
            observeSplitList()
        }
        return binding.root
    }

    private fun observeSplitList() {
        viewModel.allSplitList.observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                splitList = arrayListOf()
                splitList = it.toCollection(arrayListOf())
                splitAdapter.setList(it.toCollection(arrayListOf()))

            }
        })
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it
                if (!isFromCustomer) {
                    getKitchenPrinters()
                }
            }
        })
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner, {
            if (it != null) {
                customerSettingModel = it


            }

        })

    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                tipsList = it

            }


        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupSnackbar()
        observeShowProgress()
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
            isGuest = requireArguments().getBoolean("isGuest")
            isGuestPaymentTotal = requireArguments().getBoolean("isGuestPaymentTotal")
            isSpilt = requireArguments().getBoolean("isSpilt")
            isSplitByNo = requireArguments().getBoolean("isSplitByNo")
            isCustomCash = requireArguments().getBoolean("isCustomCash")
            isSplitByAmount = requireArguments().getBoolean("isSplitByAmount")
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


            //  paidAmount = paidAmount - tipAmount

        } else {
            paidAmount = requireArguments().getDouble("PaidAmount")
            WholetotalPrice = requireArguments().getDouble("WholetotalPrice")
            remainingAmount = requireArguments().getDouble("remainingAmount")
            splitChange = requireArguments().getDouble("splitChange")
            orderID = requireArguments().getInt("orderID")
            receiptModel = requireArguments().getParcelable("receiptData")
            splitValue = requireArguments().getInt("splitValue")
            isSpilt = requireArguments().getBoolean("isSpilt")
            isSplitByNo = requireArguments().getBoolean("isSplitByNo")
            isCustomCash = requireArguments().getBoolean("isCustomCash")
            isSplitByAmount = requireArguments().getBoolean("isSplitByAmount")
            paymentType = requireArguments().getString("paymentType", "")
            dis_charge_value = requireArguments().getDouble("dis_charge_value", 0.0)

            // paidAmount = paidAmount - tipAmount
        }
        setLabelData()

        if (!isDineIn) {
            if (isSpilt) {
                binding.constraintSplit.visibility = View.VISIBLE
                binding.viewSplitLine.visibility = View.VISIBLE
                binding.txtRemainingAmount.visibility = View.VISIBLE
                binding.txtRemainingAmountLabel.visibility = View.VISIBLE
                if (!remainingAmount.toString().contains("$")) {
                    binding.txtRemainingAmount.text = MethodUtils.roundOffAmount(remainingAmount)
                } else {
                    binding.txtRemainingAmount.text = String.format("%.2f", remainingAmount)
                }
                binding.llHome.visibility = View.GONE
                binding.llNoReceipt.text = "Next Payment"
                binding.txtHome.text = "Next Payment"
                binding.llNoReceipt.background = requireContext().getDrawable(R.color.black)

                var title = "Split "
                viewModel.addSplitToDatabase(
                    title,
                    paidAmount - splitChange,
                    remainingAmount
                )
                binding.txtTitle.text =
                    MethodUtils.roundOffAmount(paidAmount + tipAmount)


                Log.e(TAG, "paymentpaidAmount  ${paidAmount}")
                Log.e(TAG, "paymentWholetotalPrice  ${WholetotalPrice}")
                Log.e(TAG, "paymentremainingAmount  ${remainingAmount}")
                Log.e(TAG, "paymentsplitValue ${splitValue}")
                Log.e(TAG, "paymentisSpilt  ${isSpilt}")
                Log.e(TAG, "paymentisCustomCash  ${isCustomCash}")
                Log.e(TAG, "paymentisSplitByAmount  ${isSplitByAmount}")
                if (isSpilt) {
                    var sp = requireArguments().getString(Constants.SPLIT_PAY_AMOUNT)
                    Log.e(TAG, "payment SplitTotalAmount  ${sp}")

                }

                if (isSplitByAmount) {
                    if (paidAmount > WholetotalPrice) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(paidAmount - WholetotalPrice) + " Change"
                        binding.txtPaymentAmount.text =
                            "Out of " + MethodUtils.roundOffAmount(paidAmount + tipAmount)

                    } else {

                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(0.0) + " Change"
                    }
                } else if (remainingAmount < paidAmount) {
                    if (isCustomCash && splitChange != 0.0) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(splitChange) + " Change"
                        binding.txtPaymentAmount.text =
                            "Out of " + MethodUtils.roundOffAmount((paidAmount + tipAmount))
                    } else {

                        val changeValue = (paidAmount - dis_charge_value) - remainingAmount
                        if (changeValue > 0.0) {
                            binding.txtChangeAmount.text =
                                MethodUtils.roundOffAmount(changeValue) + " Change"
                        }
                        binding.txtPaymentAmount.text =
                            "Out of " + MethodUtils.roundOffAmount((paidAmount + tipAmount))
                    }
                } else {
                    if (isCustomCash && splitChange != 0.0) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(splitChange) + " Change"
                        binding.txtPaymentAmount.text =
                            "Out of " + MethodUtils.roundOffAmount((paidAmount + tipAmount))
                    } else {
                        binding.txtPaymentAmount.text =
                            "Out of " + MethodUtils.roundOffAmount(paidAmount + tipAmount)
                    }
                }
            } else {
                binding.llHome.visibility = View.VISIBLE
                binding.txtHome.visibility = View.VISIBLE
                binding.llNoReceipt.visibility = View.VISIBLE
                binding.viewSplitLine.visibility = View.GONE
                binding.constraintSplit.visibility = View.GONE
                binding.txtRemainingAmount.visibility = View.GONE
                binding.txtRemainingAmountLabel.visibility = View.GONE
                binding.llNoReceipt.text = getString(R.string.no_receipt)
                binding.txtHome.text = getString(R.string.tv_home)
                viewModel.deleteSplitDb()
                binding.txtTitle.text =
                    MethodUtils.roundOffAmount(paidAmount + tipAmount)

                if (isCustomCash) {
                    binding.txtChangeAmount.text =
                        MethodUtils.roundOffAmount(remainingAmount) + " Change"
                } else {
                    if (remainingAmount < 0) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(remainingAmount) + " Change"
                    }
                }

                binding.txtPaymentAmount.text =
                    "Out of " + MethodUtils.roundOffAmount(paidAmount + tipAmount)


            }
        } else {
            if (isSpilt) {
                binding.constraintSplit.visibility = View.VISIBLE
                binding.viewSplitLine.visibility = View.VISIBLE
                binding.txtRemainingAmount.visibility = View.VISIBLE
                binding.txtRemainingAmountLabel.visibility = View.VISIBLE
                if (!remainingAmount.toString().contains("$")) {
                    binding.txtRemainingAmount.text = MethodUtils.roundOffAmount(remainingAmount)
                } else {
                    binding.txtRemainingAmount.text = String.format("%.2f", remainingAmount)
                }

                binding.llHome.visibility = View.GONE
                binding.llNoReceipt.text = "Next Payment"
                binding.txtHome.text = "Next Payment"
                binding.llNoReceipt.background = requireContext().getDrawable(R.color.black)
                var title = "Split "
                viewModel.addSplitToDatabase(
                    title,
                    paidAmount,
                    remainingAmount
                )
                binding.txtTitle.text =
                    MethodUtils.roundOffAmount(paidAmount + tipAmount)
                if (remainingAmount < paidAmount) {
                    var temp_Change =
                        MethodUtils.roundOffAmountDouble((paidAmount - dis_charge_value) - remainingAmount)
                    if (!(temp_Change.equals(0.0) || temp_Change.equals(0) || temp_Change <= 0.0)) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount((paidAmount - dis_charge_value) - remainingAmount) + " Change"
                    }

                }

                binding.txtPaymentAmount.text =
                    "Out of " + MethodUtils.roundOffAmount(paidAmount + tipAmount)
            } else {
                if (isGuest) {
                    if (isLastPayment) {
                        binding.llHome.visibility = View.VISIBLE
                        binding.txtHome.visibility = View.VISIBLE
                        binding.llCheckOut.visibility = View.GONE
                    } else {
                        binding.llHome.visibility = View.GONE
                        binding.txtHome.visibility = View.GONE
                        binding.llCheckOut.visibility = View.VISIBLE
                    }
                } else {
                    binding.llHome.visibility = View.VISIBLE
                    binding.txtHome.visibility = View.VISIBLE
                    binding.llCheckOut.visibility = View.GONE
                }
                binding.llNoReceipt.visibility = View.VISIBLE
                binding.viewSplitLine.visibility = View.GONE
                binding.constraintSplit.visibility = View.GONE
                binding.txtRemainingAmount.visibility = View.GONE
                binding.txtRemainingAmountLabel.visibility = View.GONE
                binding.llNoReceipt.text = getString(R.string.no_receipt)
                binding.txtHome.text = getString(R.string.tv_home)
                viewModel.deleteSplitDb()
                binding.txtTitle.text =
                    MethodUtils.roundOffAmount(paidAmount)

                if (isCustomCash) {
                    binding.txtChangeAmount.text =
                        MethodUtils.roundOffAmount(remainingAmount) + " Change"
                } else {
                    if (remainingAmount < 0) {
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(remainingAmount) + " Change"
                    }
                }

                binding.txtPaymentAmount.text =
                    "Out of " + MethodUtils.roundOffAmount(paidAmount + tipAmount)


            }
        }

        if (prefProvider.getValue(Constants.CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.txtAddCustomer.visibility = View.GONE
        } else {
            binding.txtAddCustomer.visibility = View.VISIBLE
        }


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

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                //  Log.e("request_key_customer", result.first_name)

                result.id?.let { viewModel.assignCustomer(orderID, it) }
            }
        }

        if (!isSpilt) {

            prefProvider.setValue(Constants.ORDER_TYPE, "")
            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
            prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
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
            R.id.llHome -> {
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
                type = "Message"
                binding.linerContent.visibility = View.VISIBLE
                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.GONE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.VISIBLE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.constraintSplit.visibility = View.GONE
                binding.viewSplitLine.visibility = View.GONE
                MethodUtils.hideKeyboard(requireActivity())
            }
            R.id.llEmail -> {

                type = "Email"

                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.VISIBLE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                MethodUtils.hideKeyboard(requireActivity())
            }
            R.id.llNoReceipt -> {

                moveToDashboard()
            }
            R.id.llPrint -> {
                removeCustomer()
                if (isDineIn) {
                    customerPrintWholeOrder()

                } else {
                    getCustomerPrinters(false)
                }

                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
            R.id.txtSend -> {

                MethodUtils.hideKeyboard(requireActivity())
                viewModel.submit(
                    type,
                    binding.edtEmail.text.toString().trim(),
                    binding.edtPhoneNo.text.toString().trim(),
                    orderID
                )


            }
            R.id.imgBack -> {
                backpress()
            }
        }
    }

    private fun customerPrintWholeOrder() {

        var guestPos = requireArguments().getInt(GUEST_POSITION)
        var listItem: java.util.ArrayList<TbItem> = arrayListOf()
        var listItemWT: java.util.ArrayList<TbItem> = arrayListOf()
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
        customerPrinterDineIn?.forEach {


            initDineInPrinter(
                it,
                Constants.CUSTOMER,
                paymentType,
                true,
                listGuestItem = listItem,
                dineInList.get(guestPos).title.toString(),
                listItemWT
            )

        }
    }

    private fun initDineInPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        guestPrint: Boolean,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
        subTotalGuest: Double = 0.0,
        total: Double = 0.0,
        taxGuest: Double = 0.0,
        serviceChargeGuest: Double = 0.0,
        divideDiscount: Double = 0.0
    ) {

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
                    1000
                )
                //printer?.setStatusChangeEventCallback(this)

            } catch (e: Exception) {
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }
            try {

                if (printer != null) {
                    PrinterClass.setPrinter(printer)
                    if (isGuest) {


                        generateGuestPrint(
                            customerReceiptPrinters,
                            type,
                            paymentType,
                            listGuestItem,
                            guestName,
                            listWTitems,
                        )

                    } else {


                        generateDineInPrint(customerReceiptPrinters, type, "")
                    }

                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generateGuestPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,

        ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0

        val guestCount = dineInList.size - 1
        Log.e(TAG, "guestCount:  ${guestCount}")



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

        dineInList.get(0).serviceChargeList?.forEach {
            if (it.isEnabled) {
                guestServiceCharge += (guestSubTotal * it.percentage) / 100
            }
        }

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
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
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

            builder.addText(getDineInOrderDetails?.orderType + "\n")

            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + getDineInOrderDetails?.id)

                }
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
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
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
                        builder.addText("Print Time:" + formatted)
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getDineInOrderDetails?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getDineInOrderDetails?.offlineId
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                            if (customerSettingModel.fonts == Constants.LARGE) {
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
                            if (customerSettingModel.showTeam) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    getDineInOrderDetails?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
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
                                if (customerSettingModel.showTeam) {
                                    "Print Time:" + formatted
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
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

                addWholeTbItemToGuest(
                    builder,
                    listWTitems.get(i),
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers,
                    dineInList.get(0).totalGuestCount,
                    dineInList.get(0).serviceChargeList ?: arrayListOf()
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

            if (getDineInOrderDetails?.totalDiscount != null) {
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

                        if (guestDiscount == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(0.0)
                        } else {

                            "-$" + MethodUtils.roundOffAmountString(guestDiscount)

                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                        subTotalWT
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
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
                        "$" + MethodUtils.roundOffAmountString(totalTaxAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(serviceCharge),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            /*if (getOrderDetailsResponse?.cash_discount_or_surcharge != null) {
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
                        if (getOrderDetailsResponse?.cash_discount_or_surcharge == 0.0) {
                            "$" + getOrderDetailsResponse?.cash_discount_or_surcharge?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }
                        } else {
                            "-$" + getOrderDetailsResponse?.cash_discount_or_surcharge?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }*/

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
            var totalAmt =
                MethodUtils.roundOffAmountDouble(
                    subTotalWT + totalTaxAmount + serviceCharge
                )



            builder.addText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
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

                    /*  builder.addText(
                          padLine(
                              "Change Amount",
                              "$" + MethodUtils.roundOffAmountString(
                                  (getDineInOrderDetails?.payments?.get(
                                      0
                                  )?.amount!! - getDineInOrderDetails?.totalAmount!!)
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

            /* if (getOrderDetailsResponse?.totalTips == 0.0) {
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

                 if (getOrderDetailsResponse?.totalTips != 0.0) {
                     tip = getOrderDetailsResponse?.totalTips.toString()
                 }
                 builder.addText(
                     padLine(
                         "Tips",
                         if (customerSettingModel.showTipLineForCash) {
                             "_____________"
                         } else {
                             ""
                         },
                         if (customerSettingModel.fonts == Constants.LARGE) {
                             24
                         } else {
                             48
                         }
                     )
                 )
             }*/


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
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {
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
                        getDineInOrderDetails?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {
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
                        getDineInOrderDetails?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            /*if (customerSettingModel.showCustomerAddress != false or customerSettingModel.showCustomerPhone != false or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {

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
                            if (customerSettingModel.fonts == Constants.LARGE) {
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

                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
                    }

                    if (customerSettingModel.showCustomerAddress) {
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

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }*/


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
                builder.addTextAlign(Builder.ALIGN_LEFT)
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


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getDineInOrderDetails?.digitalReceiptUrl.toString())

                val newBitmap = Bitmap.createScaledBitmap(bitmap, 175, 175, true)
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

    private fun generateDineInPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        s: String
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
            builder.addText("Unpaid")
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
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
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

            builder.addText(getDineInOrderDetails?.orderType + "\n")

            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + getDineInOrderDetails?.id)

                }
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
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
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
                        builder.addText("Print Time:" + formatted)
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + getDineInOrderDetails?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getDineInOrderDetails?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getDineInOrderDetails?.offlineId
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                            if (customerSettingModel.fonts == Constants.LARGE) {
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
                            if (customerSettingModel.showTeam) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    getDineInOrderDetails?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
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
                                if (customerSettingModel.showTeam) {
                                    "Print Time:" + formatted
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
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
                            "$" + MethodUtils.roundOffAmountString(0.0)
                        } else {
                            getDineInOrderDetails?.totalDiscount?.let {
                                "-$" + MethodUtils.roundOffAmountString(it)
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                    "$" + MethodUtils.roundOffAmountString(subTotalWT),
                    if (customerSettingModel.fonts == Constants.LARGE) {
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
                        "$" + MethodUtils.roundOffAmountString(totalTaxAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (serviceCharge != null) {
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
                        "$" + MethodUtils.roundOffAmountString(serviceCharge),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getDineInOrderDetails?.totalTips != 0.0) {

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
                        "$" + getDineInOrderDetails?.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }




            if (getDineInOrderDetails?.cash_discount_or_surcharge != null) {
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
                        if (getDineInOrderDetails?.cash_discount_or_surcharge == 0.0) {
                            "$" + getDineInOrderDetails?.cash_discount_or_surcharge?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }
                        } else {
                            "-$" + getDineInOrderDetails?.cash_discount_or_surcharge?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
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
            var totalAmt =
                MethodUtils.roundOffAmountDouble(subTotalWT + serviceCharge + totalTaxAmount)



            builder.addText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
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
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                        if (customerSettingModel.fonts == Constants.LARGE) {
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
                        if (totalDiscount != 0.0) {
                            (subTotalWT + serviceCharge + totalTaxAmount - (getDineInOrderDetails?.totalDiscount?.toDouble()
                                ?: 0.0))
                        } else {
                            (subTotalWT + serviceCharge + totalTaxAmount - (getDineInOrderDetails?.totalDiscount
                                ?: 0.0))
                        },
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)
            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {
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
                        getDineInOrderDetails?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {
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
                        getDineInOrderDetails?.payments?.get(0)?.paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            /*if (customerSettingModel.showCustomerAddress != false or customerSettingModel.showCustomerPhone != false or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {

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
                            if (customerSettingModel.fonts == Constants.LARGE) {
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

                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
                    }

                    if (customerSettingModel.showCustomerAddress) {
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

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }*/


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
                builder.addTextAlign(Builder.ALIGN_LEFT)
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


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getDineInOrderDetails?.digitalReceiptUrl.toString())

                val newBitmap = Bitmap.createScaledBitmap(bitmap, 175, 175, true)
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


                requireActivity().runOnUiThread {
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
                }
                PrinterClass.closePrinter()

                //findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun moveToCheckOut() {
        if (isDineIn) {
            if (isGuestPaymentTotal) {
                val bundle = Bundle()
                bundle.putInt("orderId", orderID)

                findNavController().navigate(
                    R.id.action_orderCompleteFragment_to_dineInOrderTable,
                    bundle
                )
            }
        }
    }

    private fun moveToDashboard() {

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
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            }
        } else {
            if (isGuest) {
                if (!isLastPayment) {
                    val bundle = Bundle()
                    Log.e(TAG, "guestorderID ${orderID}")
                    bundle.putInt("orderId", orderID)

                    findNavController().navigate(
                        R.id.action_orderCompleteFragment_to_dineInOrderTable,
                        bundle
                    )
                } else {
                    removeCustomer()
                    findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                }
            } else {
                removeCustomer()
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
        }
    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        it.data

                        kitchenPrinterList = it.data
                        for (i in 0 until kitchenPrinterList.size) {
                            kitchenPrinterList[i].orderTypes.forEach {


                                if (it.orderTypeName.trim()
                                        .lowercase().equals(
                                            receiptModel?.order?.orderType?.toString()?.trim()
                                                ?.lowercase()
                                        )
                                ) {

                                    it.printerSettings.forEach {
                                        if (it.printType.lowercase()
                                                .equals(KITCHEN.lowercase()) && it.autoPrinting
                                        ) {
                                            initKitchenPrinter(kitchenPrinterList.get(i), KITCHEN)

                                        }
                                    }

                                }
                            }


                        }

                        getCustomerPrinters(true)
                    }

                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()

                }
            }

        })

    }

    private fun getCustomerPrinterForDineIn() {
        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
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
                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        })
    }

    private fun getCustomerPrinters(autoPrintCheck: Boolean) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        if (autoPrintCheck) {
                            customerList.forEach { cus ->
                                cus.orderTypes.forEach {

                                    if (it.orderTypeName.trim()
                                            .lowercase().equals(
                                                receiptModel?.order?.orderType?.toString()?.trim()
                                                    ?.lowercase()
                                            )
                                    ) {

                                        it.printerSettings.forEach {
                                            if (it.printType.lowercase()
                                                    .equals(CUSTOMER.lowercase()) && it.autoPrinting
                                            ) {
                                                requireActivity().runOnUiThread {
                                                    ProgressUtils.showProgressDialog(requireActivity())
                                                }
                                                initPrinter(cus, CUSTOMER)


                                            }
                                        }
                                    }
                                }

                            }


                        } else {
                            customerList.forEach {
                                initPrinter(it, CUSTOMER)


                            }
                        }


                    }


                }
                Status.ERROR -> {

                    ProgressUtils.dismissProgressDialog()

                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        })

    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {

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
                    1000
                )
                printer?.setStatusChangeEventCallback(this)

            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }
            try {

                if (printer != null) {
                    PrinterClass.setPrinter(printer)

                    generatePrint(customerReceiptPrinters, type)

                }

            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                e.printStackTrace()
            }
        } else {
            ProgressUtils.dismissProgressDialog()
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generatePrint(
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
                prefProvider.getValue(BUSINESS_ADDRESS, "7450 DW 51 FH,AT,Suite 503").toString()
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
            addBuilderText(builder, prefProvider.getValue(BUSINESS_PHONE_NO, "").toString())


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
            builder.addText(receiptModel?.order?.orderType + "\n")

            if (customerSettingModel.fonts == LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + receiptModel?.order?.id)

                }
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


                    builder.addText("Order Time:" + getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString()))

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
                        builder.addText("Print Time:" + formatted)
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + receiptModel?.order?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + receiptModel?.order?.offlineId,
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
                            if (customerSettingModel.showTeam) {
                                "Order Time:" + getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString())
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
                                if (customerSettingModel.showTeam) {
                                    "Print Time:" + formatted
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

            if (receiptModel?.order?.totalServiceCharges != null) {
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

            if (receiptModel?.order?.totalTips != 0.0) {

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
                        "$" + receiptModel?.order?.totalTips?.let {
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
                            "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalCashDiscountFee!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalCashDiscountFee!!)
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
                var totalAmt = MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalAmount!!)
                /* if (receiptModel?.order?.totalDiscount != 0.0) {
                     totalAmt =
                         (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalDiscount!!))

                 }*/


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

                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(0.0),
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
                        if (receiptModel?.order?.totalDiscount != 0.0) {
                            (receiptModel?.order?.totalAmount!!.toDouble() - receiptModel?.order?.totalDiscount!!.toDouble())
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
                    receiptModel?.order?.payments?.get(0)?.transactionId,
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
                    receiptModel?.order?.payments?.get(0)?.paymentType,
                    if (customerSettingModel.fonts == LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )
            if (customerSettingModel.showCustomerAddress != false or customerSettingModel.showCustomerPhone != false or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {

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

                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
                    }

                    if (customerSettingModel.showCustomerAddress) {
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

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }


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
                builder.addTextAlign(Builder.ALIGN_LEFT)
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


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel?.order?.digital_receipt_url.toString())
                Log.e(TAG, "BitmapHeight ${bitmap.height}")
                Log.e(TAG, "BitmapWidth ${bitmap.width}")
                val newBitmap = Bitmap.createScaledBitmap(bitmap, 175, 175, true)
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
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    BLUETOOTH_TIMEOUT, status, battery
                )
                ProgressUtils.dismissProgressDialog()

                PrinterClass.closePrinter()
                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            ProgressUtils.dismissProgressDialog()
            e.printStackTrace()
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        if (PrinterClass.getPrinter() == null) {
            requireActivity().runOnUiThread {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                printer.setStatusChangeEventCallback(this)
                printer.setBatteryStatusChangeEventCallback(this)
            }

            val enabled = Print.TRUE

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
                ProgressUtils.dismissProgressDialog()
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type)

            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        var builder: Builder? = null
        try {
            Log.e(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            builder = Builder(pname, PrinterClass.language, requireActivity())

            if (kitchenSettingModel.showOrderType) {


                builder.addFeedLine(0)
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

                addBuilderText(builder, receiptModel?.order?.orderType.toString())
            }


            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "OrderID:" + receiptModel?.order?.id,
                    "",
                    33
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
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
                    33
                )
            )
            if (kitchenSettingModel.showTeamMember) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
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
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString()),
                    "",
                    33
                )
            )

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            addHorizontalKitchenLine(builder)

            receiptModel?.order?.orderItems?.let { addOrdersForKitchen(builder, it) }

            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)
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
                builder.addText("Order Note")

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )


                builder.addText(receiptModel?.order?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (receiptModel?.order?.customer != null) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    //builder.addTextLineSpace(20)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(1, 1)
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
                    builder.addTextSize(1, 1)
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
                        builder.addTextSize(1, 1)
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
                            builder.addTextSize(1, 1)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber)
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
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
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

                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.SEND_TIMEOUT, status, battery
                )
                ProgressUtils.dismissProgressDialog()
                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            ProgressUtils.dismissProgressDialog()
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

        Log.e(TAG, "getDimen:  ${dimen}")
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    private fun backpress() {
        MethodUtils.hideKeyboard(requireActivity())
        binding.edtPhoneNo.text?.clear()
        binding.edtEmail.text?.clear()
        binding.llSendReceipt.visibility = View.GONE
        binding.imgBack.visibility = View.GONE
        binding.txtHome.visibility = View.VISIBLE
        binding.txtAddCustomer.visibility = View.VISIBLE
        binding.llOptions.visibility = View.VISIBLE
    }


    fun removeCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue("WholeTotal", "")
        prefProvider.setValue(SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValueInt("ORDER_ID", -1)
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        backpress()
                    }
                }
            }
        })

        viewModel.data1.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                binding.txtAddCustomer.visibility = View.GONE
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->

                    }
                }
            }
        })


    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {
        Log.e(TAG, "onStatusChangePrinter:  $p0")

    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {
        Log.e(TAG, "onBatteryEventPrinter:  $p0")

    }


}