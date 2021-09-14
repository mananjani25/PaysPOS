package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

@AndroidEntryPoint
class OrderCompleteFragment : Fragment(), View.OnClickListener, StatusChangeEventListener,
    BatteryStatusChangeEventListener {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                Log.e("request_key_customer", result.first_name)

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
                            Log.e(TAG, "PrinterType: TM-m30 ${it.printer_type}")
                            if (it.name == "TM-m30") {
                                initPrinter(it)

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

    private fun initPrinter(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        Log.e(TAG, "getPrinter:  ${PrinterClass.getPrinter()}")
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                printer.setStatusChangeEventCallback(this)
                printer.setBatteryStatusChangeEventCallback(this)
            }

            val enabled = Print.FALSE

            try {
                printer?.openPrinter(
                    Print.DEVTYPE_TCP,
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
                generatePrint(customerReceiptPrinters)
            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generatePrint(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        var builder: Builder? = null
        try {
            builder =
                Builder(customerReceiptPrinters.name, PrinterClass.language, requireActivity())
            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_A)

            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, prefProvider.getValue(BUSINESS_NAME, "").toString())
            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_C)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
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
            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_C)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addBuilderText(builder, prefProvider.getValue(BUSINESS_PHONE_NO, "").toString())
            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_A)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addBuilderText(builder, prefProvider.getValue(BUSINESS_WEBSITE, "").toString())
            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_C)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
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
                    46
                )
            )

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_C)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            Log.e(
                TAG,
                "ConvertDateTime:  ${getReceiptFormatDateFromUTCServer(receiptModel?.order?.employee?.createdAt.toString())}"
            )
            builder.addText(
                padLine(
                    "Employee:" + receiptModel?.order?.employee?.id,
                    getReceiptFormatDateFromUTCServer(receiptModel?.order?.employee?.createdAt.toString()),
                    46
                )
            )
            builder.addFeedLine(3)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.SEND_TIMEOUT, status, battery
                )
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