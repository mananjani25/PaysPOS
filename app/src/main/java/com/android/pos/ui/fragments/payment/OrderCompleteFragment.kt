package com.android.pos.ui.fragments.payment


import android.app.Dialog
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.text.trimmedLength
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.GuestDataModel
import com.android.pos.data.model.SplitBundleModel
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.BLUETOOTH
import com.android.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.android.pos.data.remote.Constants.BUSINESS_NAME
import com.android.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.android.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE
import com.android.pos.data.remote.Constants.CAT_ID_SELECTED
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.android.pos.data.remote.Constants.GUEST_POSITION
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED
import com.android.pos.data.remote.Constants.KITCHEN
import com.android.pos.data.remote.Constants.LARGE
import com.android.pos.data.remote.Constants.LOCK_SCREEN_TRANSACTION
import com.android.pos.data.remote.Constants.LOYALTY_ADDED
import com.android.pos.data.remote.Constants.OPEN_ORDER
import com.android.pos.data.remote.Constants.OPTION_TYPE
import com.android.pos.data.remote.Constants.ORDER_COMPLETED
import com.android.pos.data.remote.Constants.PAYMENT_ID
import com.android.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.android.pos.data.remote.Constants.SAVE_SPLIT_BUNDLE
import com.android.pos.data.remote.Constants.SERVICECHARGE_DINEIN_ORDER
import com.android.pos.data.remote.Constants.SPLIT_DINEIN_CHECKOUT
import com.android.pos.data.remote.Constants.SPLIT_DINEIN_MODEL
import com.android.pos.data.remote.Constants.SPLIT_IS_GUESTPAY
import com.android.pos.data.remote.Constants.SUB_TOTAL
import com.android.pos.data.remote.Constants.SUB_TOTAL_DINEIN
import com.android.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.android.pos.data.remote.Constants.SUNMI_PRINTER
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TOTAL_PRICE_DINEIN
import com.android.pos.data.remote.Constants.VENUE_LOGO
import com.android.pos.data.remote.Constants.WHOLE_AMOUNT
import com.android.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.android.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.android.pos.databinding.FragmentOrderCompletBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.SplitListAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.utils.*
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.printer.PrinterClass.BLUETOOTH_TIMEOUT
import com.android.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
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
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var splitList: ArrayList<SplitDetailListModel> = arrayListOf()
    private lateinit var printerDialog: PrinterDialog
    private var isGuestPaymentTotal = false
    private var cartList: CartModel? = null
    private var paidAmount: Double = 0.0
    private var noCashAdjGlobal: Double = 0.0
    private lateinit var pd: Dialog

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
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        printerDialog = PrinterDialog()
        progressDialog()

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
                    if (pd != null && pd.isShowing) {
                        pd.dismiss()
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

            if (it != null) {
                kitchenSettingModel = it
                Log.e(TAG, "isFromCustomer:  ${isFromCustomer}")

                Log.e(TAG, "getREceiptModel  ${Gson().toJson(receiptModel)}")
                if (!isFromCustomer) {
                    getKitchenPrinters()
                }
            }
        }
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
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
            Log.e(TAG, "isLastPaymentDine:  ${isLastPayment}")
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
            splitValue = requireArguments().getInt("splitValue")
            isSpilt = requireArguments().getBoolean("isSpilt")
            isSplitByNo = requireArguments().getBoolean("isSplitByNo")
            isCustomCash = requireArguments().getBoolean("isCustomCash")
            isSplitByAmount = requireArguments().getBoolean("isSplitByAmount")
            paymentType = requireArguments().getString("paymentType", "")
            dis_charge_value = requireArguments().getDouble("dis_charge_value", 0.0)

            // paidAmount = paidAmount - tipAmount
        }
        Log.e(TAG, "receiptModel:  ${Gson().toJson(receiptModel)}")
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


                Log.e("addSplitToDatabase", "XXX")
                val title = "Split "
                viewModel.addSplitToDatabase(
                    title,
                    (paidAmount + tipAmount) - splitChange,
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
                        changeAmtGlobal =
                            MethodUtils.roundOffAmountDouble(paidAmount - WholetotalPrice)
                                .toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if ((paidAmount - WholetotalPrice) > 0) paidAmount - WholetotalPrice else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount(paidAmount + tipAmount) + " payment successful"

                        Log.e("Change 1", binding.txtChangeAmount.text.toString())

                    } else {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(0.0).toDouble()
                        binding.txtChangeAmount.gone()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(0.0) + " Change"
                        Log.e("Change 2", binding.txtChangeAmount.text.toString())
                    }
                } else if (remainingAmount < paidAmount) {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange).toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"

                        Log.e("Change 2", binding.txtChangeAmount.text.toString())
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

                            Log.e("Change 4", binding.txtChangeAmount.text.toString())
                        }
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"
                    }
                } else {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange).toDouble()
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"

                        Log.e("Change 5", binding.txtChangeAmount.text.toString())
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"
                    } else {
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount(paidAmount + tipAmount) + " payment successful"
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
                        MethodUtils.roundOffAmount(paidAmount)
                    binding.txtPaymentAmount.text =
                        MethodUtils.roundOffAmount(paidAmount) + " payment successful"

                } else {
                    binding.txtTitle.text =
                        MethodUtils.roundOffAmount(paidAmount + tipAmount)
                    binding.txtPaymentAmount.text =
                        MethodUtils.roundOffAmount(paidAmount + tipAmount) + " payment successful"
                }


                if (isCustomCash) {
                    changeAmtGlobal =
                        MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount).toDouble()
                    binding.txtChangeAmount.visible()

                    val ca = remainingAmount - tipAmount
                    binding.txtChangeAmount.text =
                        MethodUtils.roundOffAmount(if (ca > 0) ca else 0.00) + " Change"

                    Log.e("Change 6", binding.txtChangeAmount.text.toString())
                } else {
                    if (remainingAmount < 0) {
                        changeAmtGlobal =
                            MethodUtils.roundOffAmountDouble(remainingAmount - tipAmount).toDouble()
                        binding.txtChangeAmount.visible()
                        val ca = remainingAmount - tipAmount
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (ca > 0) ca else 0.00) + " Change"

                        Log.e("Change 7", binding.txtChangeAmount.text.toString())
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
                Log.e("addSplitToDatabase", "XXX XXX")
                viewModel.addSplitToDatabase(
                    title,
                    (paidAmount + tipAmount) - splitChange,
                    remainingAmount
                )
                binding.txtTitle.text =
                    MethodUtils.roundOffAmount(paidAmount + tipAmount)

                if (remainingAmount < paidAmount) {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange)
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"

                        Log.e("Change 8", binding.txtChangeAmount.text.toString())
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

                            Log.e("Change 9", binding.txtChangeAmount.text.toString())
                        }
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"
                    }

                } else {
                    if (isCustomCash && splitChange != 0.0) {
                        changeAmtGlobal = MethodUtils.roundOffAmountDouble(splitChange)
                        binding.txtChangeAmount.visible()
                        binding.txtChangeAmount.text =
                            MethodUtils.roundOffAmount(if (splitChange > 0) splitChange else 0.00) + " Change"

                        Log.e("Change 10", binding.txtChangeAmount.text.toString())
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount((paidAmount + tipAmount)) + " payment successful"
                    } else {
                        binding.txtPaymentAmount.text =
                            MethodUtils.roundOffAmount(paidAmount + tipAmount) + " payment successful"
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
                    MethodUtils.roundOffAmount(paidAmount + tipAmount)

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
                    MethodUtils.roundOffAmount(paidAmount + tipAmount) + " payment successful"


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
//                //  Log.e("request_key_customer", result.first_name)
//                val payment_id = prefProvider.getValueInt(PAYMENT_ID, 0)
//                val final_Reward = result.final_reward
//                result.id?.let { viewModel.assignCustomer(orderID, it, payment_id, final_Reward!!) }
//            }
//        }

        if (!isSpilt) {

            prefProvider.setValue(Constants.ORDER_TYPE, TAKEOUT)
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
                binding.viewSplitLine.visibility = View.GONE
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
                    Log.e(TAG, "receiptModel:  ${Gson().toJson(receiptModel)}")
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
            Log.e(TAG, "ORDER_ID:  ${prefProvider.getValueInt("ORDER_ID", -1)}")
            //  saveDataInPrefrences()
            prefProvider.setValueInt(PAYMENT_ID, 0)
            if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                navController.previousBackStackEntry?.savedStateHandle?.set("data", bundle)
                navController.popBackStack()
            }
        }
    }

    private fun customerPrintWholeOrder() {

        var guestPos = requireArguments().getInt(GUEST_POSITION)
        Log.e(TAG, "getGuestPosition  ${guestPos}")

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

        if (customerReceiptPrinters.name.startsWith("CloudPrint", true)) {

            if (isGuest) {

                val checkOutDineInModel =
                    requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)
                Log.e(
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
                Log.e(
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

                            val checkOutDineInModel =
                                requireArguments().getParcelable<GuestDataModel>(Constants.DINE_IN_GUEST_PAYMENT_DATA)
                            Log.e(
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

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "PrinterIsNotNull:")
            }
        }

    }

    private fun generateGuestPrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>,
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

                if (noCashAdjGlobal != 0.0) {
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
                                "SurCharge",
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
                            "" + getDineInOrderDetails?.payments?.size?.minus(1)?.let {
                                getDineInOrderDetails?.payments?.get(
                                    it
                                )?.id
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
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        paymentType,
                        if (customerSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )



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
                    builder.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100)
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
        listWTitems: java.util.ArrayList<TbItem>,
        listGuestItem: java.util.ArrayList<TbItem>,
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
        listWTitems: java.util.ArrayList<TbItem>,
        listGuestItem: java.util.ArrayList<TbItem>,
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
        listWTitems: ArrayList<TbItem>,
        listGuestItem: ArrayList<TbItem>,
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
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )




            getDineInOrderDetails?.orderType?.trim()?.let { PrintSunmiUtils.printOrderType(it) }


            if (customerSettingModel.fonts == LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    PrintSunmiUtils.orderId("OrderID:" + getDineInOrderDetails?.id)
                }

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


            val str4 = padLine(
                "Service Charge",
                "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalServiceCharge!!),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.serviceCharge(str4)
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (tipAmount != 0.0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.tips(str8)
            }

            if (noCashAdjGlobal != 0.0) {

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
                        "SurCharge",
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

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                val str10 = padLine(
                    "Transaction ID",
                    "" + getDineInOrderDetails?.payments?.size?.minus(1)?.let {
                        getDineInOrderDetails?.payments?.get(
                            it
                        )?.id
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.transactionId(str10)
            }


            val str11 = padLine(
                "Transaction Type",
                paymentType, if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.transactionType(str11)




            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getDineInOrderDetails?.note!!)
            }


            if (customerSettingModel.showQrCode) {


                getDineInOrderDetails?.digitalReceiptUrl?.let { Log.e("digitalReceiptUrl1", it) }


                getDineInOrderDetails?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCode(it) }
            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printSunmiDineinInner(
        paymentType: String,
        listWTitems: ArrayList<TbItem>,
        listGuestItem: ArrayList<TbItem>,
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
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )



            SunmiPrintHelper.getInstance().lineWrap(1)

            getDineInOrderDetails?.orderType?.trim()?.let { PrintSunmiUtils.headerText(it) }

            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.fonts == LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    PrintSunmiUtils.normalText("OrderID:" + getDineInOrderDetails?.id)
                }

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


            val str4 = padLine(
                "Service Charge",
                "$" + MethodUtils.roundOffAmountString(checkOutDineInModel?.totalServiceCharge!!),
                if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.normalText(str4)
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (tipAmount != 0.0) {

                val str8 = padLine(
                    "Tips",
                    "$" + MethodUtils.roundOffAmountString(tipAmount),
                    if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str8)
            }

            if (noCashAdjGlobal != 0.0) {

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
                        "SurCharge",
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

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {

                val str10 = padLine(
                    "Transaction ID",
                    "" + getDineInOrderDetails?.payments?.size?.minus(1)?.let {
                        getDineInOrderDetails?.payments?.get(
                            it
                        )?.id
                    }, if (customerSettingModel.fonts == LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.normalText(str10)
            }


            val str11 = padLine(
                "Transaction Type",
                paymentType, if (customerSettingModel.fonts == LARGE) 23 else 48
            ).toString()
            PrintSunmiUtils.normalText(str11)




            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(getDineInOrderDetails?.note!!)
            }


            if (customerSettingModel.showQrCode) {


                SunmiPrintHelper.getInstance().lineWrap(1)
                getDineInOrderDetails?.digitalReceiptUrl?.let { PrintSunmiUtils.qrCodeInner(it) }
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

                Log.e(TAG, "receiptModelDineinData  ${Gson().toJson(receiptModel)}")
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
                    ).lowercase() == "CashDiscount".lowercase()
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
                } else if (payTypeGlb.lowercase() == "Card".lowercase()) {

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
                            "SurCharge",
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
                            "" + getDineInOrderDetails?.payments?.size?.minus(1)
                                ?.let { getDineInOrderDetails?.payments?.get(it)?.id },
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


                    builder.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100)

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
                    Log.e(TAG, "PrinterError: " + e.localizedMessage)
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
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )


            getDineInOrderDetails?.orderType?.trim()?.let { PrintSunmiUtils.printOrderType(it) }


            if (customerSettingModel.fonts == LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    PrintSunmiUtils.orderId("OrderID:" + getDineInOrderDetails?.id)
                }

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

                            dineInList[i]?.customer?.first_name + " " +
                                    if (dineInList[i]?.customer?.last_name != null) {
                                        dineInList[i].customer?.last_name
                                    } else {
                                        ""
                                    }?.let { PrintSunmiUtils.printTextCenter(it) }

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
                ).lowercase() == "CashDiscount".lowercase()
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


            } else if (payTypeGlb.lowercase() == "Card".lowercase()) {


                val str8 = padLine(
                    "SurCharge",
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
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
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

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {


                val str10 = padLine(
                    "Transaction ID",
                    "" + getDineInOrderDetails?.payments?.size?.minus(1)
                        ?.let { getDineInOrderDetails?.payments?.get(it)?.id },
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


            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(getDineInOrderDetails?.note!!)
            }


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
            Log.e("printDineReciept","Staring....")
            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)
            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                PrintSunmiUtils.headerText("OrderID:" + getDineInOrderDetails?.id)
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
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )

            SunmiPrintHelper.getInstance().lineWrap(1)

            getDineInOrderDetails?.orderType?.trim()?.let { PrintSunmiUtils.headerText(it) }
            SunmiPrintHelper.getInstance().lineWrap(1)

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

                            dineInList[i]?.customer?.first_name + " " +
                                    if (dineInList[i]?.customer?.last_name != null) {
                                        dineInList[i].customer?.last_name
                                    } else {
                                        ""
                                    }?.let { PrintSunmiUtils.normalTextCenter(it) }

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
                ).lowercase() == "CashDiscount".lowercase()
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


            } else if (payTypeGlb.lowercase() == "Card".lowercase()) {


                val str8 = padLine(
                    "SurCharge",
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
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                    }


                }

            }
            Log.e("printDineReciept","Staring 1....")

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

            if (getDineInOrderDetails?.payments?.isNotEmpty() == true) {


                val str10 = padLine(
                    "Transaction ID",
                    "" + getDineInOrderDetails?.payments?.size?.minus(1)
                        ?.let { getDineInOrderDetails?.payments?.get(it)?.id },
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

                PrintSunmiUtils.normalText(str11)

            }


            if (getDineInOrderDetails?.note != null && getDineInOrderDetails?.note != "" && customerSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInner(getDineInOrderDetails?.note!!)
            }


            if (customerSettingModel.showQrCode) {

                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.qrCodeInner(receiptModel?.order?.digital_receipt_url.toString())

                // getDineInOrderDetails?.digitalReceiptUrl.toString().let { PrintSunmiUtils.qrCode(it) }
            }
            Log.e("printDineReciept","Staring 2....")
            PrintSunmiUtils.cutPaperInner()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("printDineReciept","Staring error....")
        }
    }

    private fun moveToCheckOut() {
        if (isDineIn) {
            if (!isLastPayment) {
                val bundle = Bundle()
                Log.e(TAG, "guestorderID ${orderID}")
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
        if (!isSpilt) {
            removeCustomer()


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
                    removePrefrenceDinein()
                    // redirect to passcode
                    if (prefProvider.getValueboolean(LOCK_SCREEN_TRANSACTION, false)) {
                        prefProvider.setValueboolean(ORDER_COMPLETED, true)
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_passcode)
                    } else {
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    }

                }
            } else {
                if (findNavController().currentDestination?.id == R.id.orderCompleteFragment) {
                    removeCustomer()
                    removePrefrenceDinein()
                    // redirect to passcode
                    if (prefProvider.getValueboolean(LOCK_SCREEN_TRANSACTION, false)) {
                        prefProvider.setValueboolean(ORDER_COMPLETED, true)
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_passcode)
                    } else {
                        findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                    }
                }
            }
        }
    }

    fun saveDataInPrefrences() {
        Log.e(TAG, "cartListORderCom:  ${Gson().toJson(cartList)}")
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

        Log.e("SAVE_SPLIT_BUNDLE", Gson().toJson(model))
        prefProvider.setValue(SAVE_SPLIT_BUNDLE, Gson().toJson(model).toString())

    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {


                        kitchenPrinterList = it.data
                        val remain = requireArguments().getDouble("remainingAmount")
                        if (requireArguments().getBoolean("isDineIn")) {
                            customerPrintWholeOrder()

                        } else {
                            getCustomerPrinters(true)
                        }


                        if (!prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                            if (!requireArguments().getBoolean("isSpilt") && receiptModel?.order?.orderType?.lowercase() != "OpenOrder".lowercase()) {
                                if (!requireArguments().getBoolean("isDineIn") && !requireArguments().getBoolean(
                                        "isFromActiveOrder"
                                    )
                                ) {
                                    if (kitchenPrinterList.isNotEmpty()) {
                                        for (i in 0 until kitchenPrinterList.size) {
                                            kitchenPrinterList[i].orderTypes.forEach {

                                                if (it.orderTypeId == receiptModel?.order?.orderTypeId

                                                ) {

                                                    it.printerSettings.forEach {
                                                        if (it.printType.lowercase()
                                                                .equals(KITCHEN.lowercase()) && it.autoPrinting
                                                        ) {
                                                            initKitchenPrinter(
                                                                kitchenPrinterList.get(i),
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


                        pd.dismiss()


                    }

                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()

                }
            }

        }

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
                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        }
    }

    private fun getCustomerPrinters(autoPrintCheck: Boolean) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        if (autoPrintCheck) {
                            customerList.forEach { cus ->
                                cus.orderTypes.forEach {

                                    if (it.orderTypeId == receiptModel?.order?.orderTypeId) {

                                        it.printerSettings.forEach {
                                            if (it.printType.lowercase()
                                                    .equals(CUSTOMER.lowercase()) && it.autoPrinting
                                            ) {


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

        }

    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {

        pd.show()

        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            customerReceiptPrinters.ipAddress?.let { sunmiPrinterInit(it) }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService1()
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
                    pd.dismiss()
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
                    pd.dismiss()
                    e.printStackTrace()
                }
            } else {
                pd.dismiss()
                Log.e(TAG, "PrinterIsNotNull:")
            }
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

            Log.e(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")
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
            if (receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
                || receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
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
                            "SurCharge",
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

            var newPaidAmount = paidAmount
            if (MethodUtils.roundOffAmountDouble(paidAmount + tipAmount) == MethodUtils.roundOffAmountDouble(
                    (receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                    )?.amount ?: 0.0).plus(
                        (receiptModel?.order?.payments?.get(
                            receiptModel?.order?.payments?.size?.minus(
                                1
                            ) ?: 0
                        )?.tips ?: 0.0)
                    ) ?: 0.0
                )
            ) {
                newPaidAmount = paidAmount + tipAmount
            }

            if (isSpilt) {
                newPaidAmount = paidAmount + tipAmount
            }
            Log.e("ToCheck", "PaidAmount ${newPaidAmount}")

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
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName,
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
                        receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType,
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

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

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

                            builder.addText(
                                padLine(
                                    receiptModel?.order?.customer?.phones?.size?.minus(
                                        1
                                    )?.let {
                                        receiptModel?.order?.customer?.phones?.get(
                                            it
                                        )?.phoneNumber
                                    }, "", if (customerSettingModel.fonts == LARGE) {
                                        24
                                    } else {
                                        48
                                    }
                                )
                            )

                        }


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

                            builder.addText(
                                padLine(
                                    receiptModel?.order?.customer?.addresses?.size?.minus(1)?.let {
                                        receiptModel?.order?.customer?.addresses?.get(
                                            it
                                        )?.fullAddress
                                    }, "", if (customerSettingModel.fonts == LARGE) {
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


            Log.e(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "") {

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


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel?.order?.digital_receipt_url.toString())
                Log.e(TAG, "BitmapHeight ${bitmap.height}")
                Log.e(TAG, "BitmapWidth ${bitmap.width}")
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
                builder.addPulse(
                    com.epson.epos2.printer.Printer.DRAWER_HIGH,
                    com.epson.epos2.printer.Printer.PULSE_100
                )
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    BLUETOOTH_TIMEOUT, status, battery
                )


                PrinterClass.closePrinter()
                pd.dismiss()
                // findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                pd.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            pd.dismiss()
            e.printStackTrace()
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {

        if (data.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)

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

                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(200)
                                generateKitchenReceiptSunmi(data, type)
                            }


                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(200)
                    generateKitchenReceiptSunmi(data, type)
                }
            }

        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(200)
                setService2()
            }

        } else {

            PrinterClass.closePrinter()
            if (PrinterClass.getPrinter() == null) {
                //  printerDialog.show(requireContext())

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
                    //  printerDialog.dismiss()
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

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderText(builder, receiptModel?.order?.orderType.toString())
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()
                Log.e(TAG, "LowerCAse ${tmps.trimmedLength()}")

                if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                        .toString().lowercase() || receiptModel?.order?.orderType.toString()
                        .lowercase() == "Open Order".trim()
                        .toString().lowercase()
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


                builder.addFeedLine(2)
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
                builder.addTextSize(fontSizeH, fontSizeW)
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
                        fontSizeW
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


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
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

                                    builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
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

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderText(builder, receiptModel?.order?.orderType.toString())
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()
                Log.e(TAG, "LowerCAse ${tmps.trimmedLength()}")

                if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                        .toString().lowercase() || receiptModel?.order?.orderType.toString()
                        .lowercase() == "Open Order".trim()
                        .toString().lowercase()
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


                builder.addFeedLine(2)
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
                        "OrderID:" + receiptModel?.order?.id,
                        "",
                        if (kitchenSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

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
                        "ReceiptID:" + receiptModel?.order?.offlineId,
                        "",
                        if (kitchenSettingModel.fonts == LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
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
                        fontSizeW
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


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
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

                                    builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                                }
                            }
                        }

                    }
                }

            }

            builder.addFeedLine(2)

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

                timeOut = 1000
            }

            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    timeOut, status, battery
                )

                Handler(Looper.getMainLooper()).postDelayed(Runnable {

                }, 1000)
                //printerDialog.dismiss()
                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
//                printerDialog.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        try {


            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)

            if (kitchenSettingModel.showOrderType) {


                PrintSunmiUtils.printOrderType(receiptModel?.order?.orderType.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                    .toString().lowercase() || receiptModel?.order?.orderType.toString()
                    .lowercase() == "Open Order".trim()
                    .toString().lowercase()
            ) {

                PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.orderId(
                padLine(
                    "OrderID:" + receiptModel?.order?.id,
                    "",
                    if (kitchenSettingModel.fonts == LARGE) 23 else 48
                ).toString()
            )
            SunmiPrinterApi.getInstance().lineWrap(1)

            PrintSunmiUtils.receiptID(
                padLine(
                    "ReceiptID:" + receiptModel?.order?.offlineId,
                    "",
                    if (kitchenSettingModel.fonts == LARGE) 23 else 48
                ).toString()
            )
            SunmiPrinterApi.getInstance().lineWrap(1)

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

            receiptModel?.order?.orderItems?.let {
                addOrdersForKitchen(
                    it
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNote(receiptModel?.order?.note.toString())

            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (receiptModel?.order?.customer != null) {


                    PrintSunmiUtils.customerDetails()

                    if (kitchenSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                PrintSunmiUtils.customerPhone(
                                    it
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


                                receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
                                    PrintSunmiUtils.customerAddress(
                                        it
                                    )
                                }
                            }
                        }
                    }

                }
            }

            PrintSunmiUtils.cutPaper()

            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())

        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }


    private fun generateKitchenReceiptSunmiInner(

    ) {
        try {
            PrintSunmiUtils.fontSizeInner(LARGE)
            SunmiPrintHelper.getInstance().initPrinter()

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(receiptModel?.order?.orderType.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                    .toString().lowercase() || receiptModel?.order?.orderType.toString()
                    .lowercase() == "Open Order".trim()
                    .toString().lowercase()
            ) {

//                PrintSunmiUtils.headerText(receiptModel?.order?.deliveryType.toString())
//                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            PrintSunmiUtils.normalText(
                padLine(
                    "OrderID:" + receiptModel?.order?.id,
                    "",
                    if (kitchenSettingModel.fonts == LARGE) 23 else 23
                ).toString()
            )
            PrintSunmiUtils.normalText(
                padLine(
                    "ReceiptID:" + receiptModel?.order?.offlineId,
                    "",
                    if (kitchenSettingModel.fonts == LARGE) 23 else 23
                ).toString()
            )

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.normalText(
                    padLine(
                        "Employee:" + receiptModel?.order?.employee?.name, "",
                        if (kitchenSettingModel.fonts == LARGE) 23 else 23
                    ).toString()
                )


            }
            PrintSunmiUtils.normalText(
                padLine(
                    getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        receiptModel?.order?.createdAt.toString()
                    ),
                    "",
                    if (kitchenSettingModel.fonts == LARGE) 23 else 23
                ).toString()
            )



            PrintSunmiUtils.addHorizontalInner()
            SunmiPrintHelper.getInstance().lineWrap(1)

            receiptModel?.order?.orderItems?.let {
                addOrdersForKitchenInner(
                    it
                )
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInner(receiptModel?.order?.note.toString())

            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (receiptModel?.order?.customer != null) {


                    PrintSunmiUtils.customerDetailsInner()

                    if (kitchenSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber?.let {
                                PrintSunmiUtils.normalText(
                                    it
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


                                receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
                                    PrintSunmiUtils.normalText(
                                        it
                                    )
                                }
                            }
                        }
                    }

                }
            }

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

        Log.e(TAG, "getDimen:  ${dimen}")
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
    }

    fun removePrefrenceDinein() {
        prefProvider.setValue("PaidAmount", "")
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
        prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(WHOLE_AMOUNT, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValue(SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValueInt("ORDER_ID", -1)
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
        Log.e(TAG, "onStatusChangePrinter:  $p0")

    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {
        Log.e(TAG, "onBatteryEventPrinter:  $p0")

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
        pd.setContentView(R.layout.view_loading)
        // pd.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL)
//        pd.setMessage("Please Wait..")
        pd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pd.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        pd.setCanceledOnTouchOutside(false)
        pd.setCancelable(false)
        pd.show()


    }

    override fun onPause() {
        super.onPause()
        if (pd != null && pd.isShowing) {
            pd.dismiss()
        }

    }

    private fun sunmiPrinterInit(ipAddress: String) {

        SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiBlueToothPrinter, ipAddress)

        connect()

    }

    fun connect() {
        if (!SunmiPrinterApi.getInstance().isConnected) {
            pd.dismiss()
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
                        sunmiPrint()
                    }

                    override fun onDisconnect() {
                        println("onDisconnect")
                    }

                })
        } else {
            sunmiPrint()
        }
    }

    private fun sunmiPrint() {


        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            SunmiPrinterApi.getInstance().printerInit()

            Log.e(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")
            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {

                printBusinessLogo()

            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(BUSINESS_NAME, ""),
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )


//            SunmiPrinterApi.getInstance().setAlignMode(1)
//            SunmiPrinterApi.getInstance().enableBold(false)
//            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
//            receiptModel?.order?.venue_website?.let { SunmiPrinterApi.getInstance().printText(it) }
//            SunmiPrinterApi.getInstance().lineWrap(1)


            receiptModel?.order?.orderType?.trim()?.let { PrintSunmiUtils.printOrderType(it) }


            if (receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
                || receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
            ) {

                receiptModel?.order?.deliveryType?.let { PrintSunmiUtils.deliveryType(it) }
            }



            if (customerSettingModel.fonts == LARGE) {

                if (customerSettingModel.showOrderIdTop) {

                    PrintSunmiUtils.orderId("OrderID:" + receiptModel?.order?.id)

                }

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
                    if (customerSettingModel.showOrderIdTop) {
                        "OrderID:" + receiptModel?.order?.id
                    } else {
                        ""
                    },
                    "ReceiptID:" + receiptModel?.order?.offlineId?.trim(),
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

            if (receiptModel?.order?.totalServiceCharges != null) {


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

            if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                    receiptModel?.order?.payments?.size!! - 1
                )?.paymentType?.lowercase() == "Card".lowercase() && receiptModel?.order?.payments?.get(
                    receiptModel?.order?.payments?.size!! - 1
                )?.cash_discount_type?.lowercase() == "SurCharge".lowercase()
            ) {

                if (receiptModel?.order?.totalCashDiscountFee != null) {


                    val str8 = padLine(
                        "SurCharge",
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


            var newPaidAmount = paidAmount
            if (MethodUtils.roundOffAmountDouble(paidAmount + tipAmount) == MethodUtils.roundOffAmountDouble(
                    (receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                    )?.amount ?: 0.0).plus(
                        (receiptModel?.order?.payments?.get(
                            receiptModel?.order?.payments?.size?.minus(
                                1
                            ) ?: 0
                        )?.tips ?: 0.0)
                    ) ?: 0.0
                )
            ) {
                newPaidAmount = paidAmount + tipAmount
            }

            if (isSpilt) {
                newPaidAmount = paidAmount + tipAmount
            }
            Log.e("ToCheck", "PaidAmount ${newPaidAmount}")


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
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
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


                val str13 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()


                val str14 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString()

                PrintSunmiUtils.cardDetails(str12, str13, str14)
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {


                    PrintSunmiUtils.customerDetails()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }
                    if (customerSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            PrintSunmiUtils.customerPhone(
                                padLine(
                                    receiptModel?.order?.customer?.phones?.size?.minus(
                                        1
                                    )?.let {
                                        receiptModel?.order?.customer?.phones?.get(
                                            it
                                        )?.phoneNumber
                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )


                        }


                    }



                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            PrintSunmiUtils.customerAddress(
                                padLine(
                                    receiptModel?.order?.customer?.addresses?.size?.minus(1)?.let {
                                        receiptModel?.order?.customer?.addresses?.get(
                                            it
                                        )?.fullAddress
                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )

                        }
                    }

                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            Log.e(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "") {

                PrintSunmiUtils.orderNote(receiptModel?.order?.note!!)

            }


            if (customerSettingModel.showQrCode) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                receiptModel?.order?.digital_receipt_url?.let { PrintSunmiUtils.qrCode(it) }

            }

            PrintSunmiUtils.cutPaper()
            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())


            pd.dismiss()

        } catch (e: Exception) {
            pd.dismiss()
            e.printStackTrace()
        }

    }

    private fun sunmiPrintInner() {


        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()


            if (customerSettingModel.showOrderIdTop) {

                PrintSunmiUtils.headerText("OrderID:" + receiptModel?.order?.id)
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            Log.e(TAG, "getVanueLogo:  ${prefProvider.getValue(VENUE_LOGO, "")}")
            if (customerSettingModel.showVenueLogo && prefProvider.getValue(VENUE_LOGO, "")
                    .isNotEmpty()
            ) {

                PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))

            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(BUSINESS_NAME, ""),
                prefProvider.getValue(BUSINESS_ADDRESS, ""),
                prefProvider.getValue(BUSINESS_PHONE_NO, "")
            )


//            SunmiPrinterApi.getInstance().setAlignMode(1)
//            SunmiPrinterApi.getInstance().enableBold(false)
//            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
//            receiptModel?.order?.venue_website?.let { SunmiPrinterApi.getInstance().printText(it) }
//            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrintHelper.getInstance().lineWrap(1)
            receiptModel?.order?.orderType?.trim()?.let { PrintSunmiUtils.headerText(it) }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
                || receiptModel?.order?.orderType?.lowercase() == OPEN_ORDER.lowercase()
            ) {

                receiptModel?.order?.deliveryType?.let { PrintSunmiUtils.headerText(it) }
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

            if (receiptModel?.order?.totalServiceCharges != null) {


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

            if (receiptModel?.order?.payments?.isNotEmpty() == true && receiptModel?.order?.payments?.get(
                    receiptModel?.order?.payments?.size!! - 1
                )?.paymentType?.lowercase() == "Card".lowercase() && receiptModel?.order?.payments?.get(
                    receiptModel?.order?.payments?.size!! - 1
                )?.cash_discount_type?.lowercase() == "SurCharge".lowercase()
            ) {

                if (receiptModel?.order?.totalCashDiscountFee != null) {


                    val str8 = padLine(
                        "SurCharge",
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


            var newPaidAmount = paidAmount
            if (MethodUtils.roundOffAmountDouble(paidAmount + tipAmount) == MethodUtils.roundOffAmountDouble(
                    (receiptModel?.order?.payments?.get(
                        receiptModel?.order?.payments?.size?.minus(1) ?: 0
                    )?.amount ?: 0.0).plus(
                        (receiptModel?.order?.payments?.get(
                            receiptModel?.order?.payments?.size?.minus(
                                1
                            ) ?: 0
                        )?.tips ?: 0.0)
                    ) ?: 0.0
                )
            ) {
                newPaidAmount = paidAmount + tipAmount
            }

            if (isSpilt) {
                newPaidAmount = paidAmount + tipAmount
            }
            Log.e("ToCheck", "PaidAmount ${newPaidAmount}")


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
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
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
            PrintSunmiUtils.normalText(str11)


            if (receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.paymentType?.lowercase() == "Card".lowercase()) {


                val str12 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardName.toString()


                val str13 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardType.toString()


                val str14 =
                    receiptModel?.order?.payments?.get(receiptModel?.order?.payments?.size!! - 1)?.cardNumber.toString()

                PrintSunmiUtils.cardDetailsInner(str12, str13, str14)
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                if (receiptModel?.order?.customer != null) {


                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)

                    }
                    if (customerSettingModel.showCustomerPhone) {

                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {

                            PrintSunmiUtils.normalText(
                                padLine(
                                    receiptModel?.order?.customer?.phones?.size?.minus(
                                        1
                                    )?.let {
                                        receiptModel?.order?.customer?.phones?.get(
                                            it
                                        )?.phoneNumber
                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )


                        }


                    }



                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {

                            PrintSunmiUtils.normalText(
                                padLine(
                                    receiptModel?.order?.customer?.addresses?.size?.minus(1)?.let {
                                        receiptModel?.order?.customer?.addresses?.get(
                                            it
                                        )?.fullAddress
                                    }, "", if (customerSettingModel.fonts == LARGE) 23 else 48
                                ).toString()
                            )

                        }
                    }

                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            Log.e(TAG, "showOrderNote:  ${receiptModel?.order?.note}")
            if (receiptModel?.order?.note != null && receiptModel?.order?.note != "") {

                PrintSunmiUtils.orderNoteInner(receiptModel?.order?.note!!)

            }


            if (customerSettingModel.showQrCode) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                receiptModel?.order?.digital_receipt_url?.let { PrintSunmiUtils.qrCodeInner(it) }

            }

            PrintSunmiUtils.cutPaperInner()
            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())


            pd.dismiss()

        } catch (e: Exception) {
            pd.dismiss()
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

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                printDineInTable1Inner()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService()
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService1() {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                sunmiPrintInner()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService1()
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService2() {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner()


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService2()
            }, 2000)
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService(
        paymentType: String,
        listWTitems: java.util.ArrayList<TbItem>,
        listGuestItem: java.util.ArrayList<TbItem>,
        guestName: String,
        finaldisLocal: Double,
        checkOutDineInModel: GuestDataModel?,
        orderDiscount: Double,
        guestTaxes: Double,
        guestSubTotal: Double,
        guestServiceCharge: Double
    ) {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            Log.e("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                Log.e("SunmiPrintHelpe1r", "isBlueToothPrinter")

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
            Log.e("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            Log.e("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            Log.e("SunmiPrintHelper", "ELSE")
        }
    }

}