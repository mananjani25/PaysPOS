package com.android.pos.ui.fragments.payment

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.android.pos.data.remote.Constants.BUSINESS_NAME
import com.android.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.android.pos.data.remote.Constants.BUSINESS_WEBSITE
import com.android.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.android.pos.databinding.FragmentOrderCompletBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.*
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import javax.inject.Inject
import android.content.Context.WINDOW_SERVICE
import android.graphics.Point
import android.util.Printer
import android.view.*


import androidx.core.content.ContextCompat.getSystemService
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.Constants.BLUETOOTH
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.KITCHEN
import com.android.pos.data.remote.Constants.WIFI
import com.android.pos.utils.printer.PrinterClass.BLUETOOTH_TIMEOUT
import com.google.zxing.qrcode.encoder.QRCode


@AndroidEntryPoint
class OrderCompleteFragment : Fragment(), View.OnClickListener, StatusChangeEventListener,
    BatteryStatusChangeEventListener {
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    private var orderID: Int = 0
    private var type: String = ""
    private var totalPrice: Double = 0.0
    private var paymentAmount: Double = 0.0
    private val viewModel by viewModels<OrderCompleteViewModel>()
    private var receiptModel: CreateOrderResponse.Data? = null

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentOrderCompletBinding
    private val TAG = "OrderCompleteFragment"
    private var tipsList: List<GetTipReponse.Data> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        observeTipsList()
        return binding.root
    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                tipsList = it
                Log.e(TAG, "tipsList:  ${Gson().toJson(tipsList)}")
            }


        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getKitchenPrinters()

        setupSnackbar()
        observeShowProgress()



        totalPrice = requireArguments().getDouble("totalPrice")
        paymentAmount = requireArguments().getDouble("paymentAmount")
        orderID = requireArguments().getInt("orderID")
        receiptModel = requireArguments().getParcelable("receiptData")

        Log.e(TAG, "receiptModel:   ${Gson().toJson(receiptModel)}")
        binding.txtTitle.text =
            MethodUtils.roundOffAmount(paymentAmount) + " cash"

        if (totalPrice != paymentAmount) {
            binding.txtChangeAmount.text =
                MethodUtils.roundOffAmount(paymentAmount - totalPrice) + " Change"
        }
        binding.txtPaymentAmount.text = "Out of " + MethodUtils.roundOffAmount(paymentAmount)

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

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                //  Log.e("request_key_customer", result.first_name)

                result.id?.let { viewModel.assignCustomer(orderID, it) }
            }
        }
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtHome -> {
                removeCustomer()
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)

            }
            R.id.txtAddCustomer -> {
                findNavController().navigate(R.id.action_orderCompleteFragment_to_assignCustomerOrderFragment)
            }
            R.id.llMessage -> {
                type = "Message"
                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.GONE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.VISIBLE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
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
                removeCustomer()
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
            R.id.llPrint -> {
                removeCustomer()
                getCustomerPrinters()

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

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = it.data
                        Log.e(TAG, "kitchenPrinterList:   ${kitchenPrinterList.size}")
                        for (i in 0 until kitchenPrinterList.size) {
                            initKitchenPrinter(kitchenPrinterList.get(i), KITCHEN)
                        }
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

    private fun getCustomerPrinters() {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        customerList.forEach {
                            Log.e(TAG, "PrinterName:  ${it.name}")
                            Log.e(TAG, "PrinterModelName:  ${it.modalName}")
                            Log.e(TAG, "PrinterType:  ${it.printer_type}")
                            if (it.name == "TM-m30_030295") {
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
        Log.e(TAG, "getPrinter:  ${PrinterClass.getPrinter()}")
        PrinterClass.closePrinter()
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                printer.setStatusChangeEventCallback(this)
                printer.setBatteryStatusChangeEventCallback(this)
            }

            val enabled = Print.FALSE

            try {
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
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generatePrint(customerReceiptPrinters, type)

            }

        } else {
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
                Builder("TM-m30", PrinterClass.language, requireActivity())
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
            builder.addTextSize(1, 1)
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
            builder.addTextSize(1, 1)
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
                builder.addTextSize(1, 1)
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


            builder.addFeedLine(1)
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
                    "ReceiptID:" + receiptModel?.order?.offlineId,
                    48
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

            Log.e(
                TAG,
                "ConvertDateTime:  ${getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString())}"
            )
            builder.addText(
                padLine(
                    "Employee:" + receiptModel?.order?.employee?.name,
                    getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString()),
                    48
                )
            )


            builder.addFeedLine(1)

            addHorizontalLine(builder)

            receiptModel?.order?.orderItems?.let { addOrderItems(builder, it) }

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.subTotal!!),
                    48
                )
            )


            if (receiptModel?.order?.totalTaxAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalTaxAmount!!),
                        48
                    )
                )
            }

            if (receiptModel?.order?.totalServiceCharges != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalServiceCharges!!),
                        48
                    )
                )
            }

            if (receiptModel?.order?.totalTips != 0.0) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
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
                        "Tips",
                        "$" + receiptModel?.order?.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        48
                    )
                )
            }


            if (receiptModel?.order?.totalDiscount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                        "Total Discount",

                        if (receiptModel?.order?.totalDiscount == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalDiscount!!)
                        },
                        48
                    )
                )

            }

            if (receiptModel?.order?.totalCashDiscountFee != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                        "Cash Discount",
                        if (receiptModel?.order?.totalCashDiscountFee == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalCashDiscountFee!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel?.order?.totalCashDiscountFee!!)
                        },
                        48
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
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                var totalAmt = MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalAmount!!)
                if (receiptModel?.order?.totalDiscount != 0.0) {
                    totalAmt =
                        (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalDiscount!!))

                }

                builder.addText(
                    padLine(
                        "Total Price",
                        "$" + totalAmt,
                        48
                    )
                )

            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString((receiptModel?.order?.payments?.get(0)?.amount!! - receiptModel?.order?.totalAmount!!)),
                    48
                )
            )

            if (receiptModel?.order?.totalTips == 0.0) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
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
                        "_____________",
                        48
                    )
                )
            }


            builder.addFeedLine(1)
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText(padLine("Additional Tips", "", 48))


            /*builder.addTextLineSpace(0)
            builder.addFeedUnit(0)*/


            /*builder.addText(
                padLine(
                    "","",
                    48
                )
            )*/


            builder.addFeedLine(1)

            addHorizontalLine(builder)


            // builder.addText("--------------------------------------------------------")


            if (tipsList.isNotEmpty()) {
                addTipsList(builder, tipsList, receiptModel?.order?.totalAmount!!)

            }
            /*builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
*/
            /*  builder.addTextFont(Builder.FONT_E)
              // builder.addTextAlign(Builder.ALIGN_LEFT)
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
                      "Total Tips",
                      "$" + receiptModel?.order?.totalTips?.let { MethodUtils.roundOffAmountString(it) },
                      48
                  )
              )
  */

            builder.addFeedLine(1)
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
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
                    48
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
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
                    48
                )
            )

            if (receiptModel?.order?.customer != null) {

                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(padLine("Customer Details", "", 48))

                builder.addFeedLine(1)

                addHorizontalLine(builder)
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText("7450 DW 51 FH AT,Suite 503")

            }


            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "") {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
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

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    BLUETOOTH_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
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

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        Log.e(TAG, "getPrinter:  ${PrinterClass.getPrinter()}")
        Log.e(TAG, "datadataKitchenPRinter  ${Gson().toJson(data)}")

        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                printer.setStatusChangeEventCallback(this)
                printer.setBatteryStatusChangeEventCallback(this)
            }

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
            val pname = if (customerReceiptPrinters.name == "TM-m30_030295") {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            builder =
                Builder(pname, PrinterClass.language, requireActivity())

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

            Log.e(
                TAG,
                "ConvertDateTime:  ${getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString())}"
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

            if (receiptModel?.order?.note?.isNotEmpty() == true) {
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
                builder.addText("(635)984-5211")

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

                builder.addText("7450 DW 51 FH AT,Suite 503")

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

                PrinterClass.closePrinter()

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