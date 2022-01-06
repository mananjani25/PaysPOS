package com.android.pos.ui.fragments.dinein

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.Group
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.MERGEDANDOCCUPIED
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.FragmentDineInOrderTableBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInTableAdapter
import com.android.pos.utils.*
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DineInOrderTable : Fragment(), DineInTableAdapter.DineInTableListner {
    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = listOf()
    private var toFinalAmt: Double = 0.0
    private var totalTaxAmt: Double = 0.0
    private var isFireAll: Boolean = false
    private var clickedPos: Int = 0
    private lateinit var binding: FragmentDineInOrderTableBinding
    private var cartList: CartModel? = null
    private var dineInData: CreateOrderResponse.Data? = null
    private var orderId: Int? = null
    private var getOrderDetailsResponse: GetOrderDetailsResponse.Data? = null
    private val TAG = "DineInOrderTable"
    private lateinit var dineInTableAdapter: DineInTableAdapter
    private var totalPrice: Double = 0.0
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    var cashDiscount: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    var totalGuestCount = 0
    var divideCashDiscount = 0.0
    private var paymentAmount: Double = 0.0
    private var subTotalWT = 0.0
    private var subTotalDInin = 0.0
    private var WTOnlyTax = 0.0
    private var WTServiceTax = 0.0
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var serviceCharge = 0.0
    private var finalTaxAmt = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    var totalAmount = 0.0
    var totalAmtnew: Double = 0.0
    private var allCustomerList: ArrayList<TbCustomer> = arrayListOf()
    private var popupWindow: PopupWindow? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    var dragFrom = -1
    var dragTo = -1
    var notPayAnyAmount: Boolean = false
    var paidGuestAmount = 0
    var serviceChargeList: ArrayList<TbServiceCharge> = arrayListOf()
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    lateinit var cashDiscountModel: CashDiscountModel
    var optionType = ""

    @Inject
    lateinit var prefProvider: PrefProvider
    private val viewModel by viewModels<DineInOrderTableViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in_order_table,
            container,
            false
        )
        binding.lifecycleOwner = this

        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        observeShowProgress()
        setupSnackbar()
        getCustomerList()
        observeServiceCharge()
        singleItemFireObserver()
        getCustomerReceiptSettings()
        getKitchenReceiptSettings()
        observeFireAll()
        observeQueueCreated()

        observeTipsList()

        navigateDineInOrder()
        observeUnMergeTable()
        return binding.root
    }


    private fun getCustomerList() {
        viewModel.customer().observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                allCustomerList = it.toCollection(arrayListOf())
            }
        })
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it
                getKitchenPrinters()
            }
        })
    }

    private fun observeServiceCharge() {
        viewModel.getServiceChargeList.observe(viewLifecycleOwner, {
            if (it.data?.isNotEmpty() == true) {
                serviceChargeList = it.data.toCollection(arrayListOf())
                dineInTableAdapter.setSurchargeList(serviceChargeList)

            }

        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //getData()

        dineInTableAdapter = DineInTableAdapter()
        binding.rvItemList.adapter = dineInTableAdapter
        dineInTableAdapter.setListner(this)



        if (arguments?.getBoolean("isFromFloor") == true || arguments?.getBoolean("isGuestPaid") == true) {
            floorPlanModel = arguments?.getParcelable("floorPlan")


            if (floorPlanModel == null) {
                orderId = arguments?.getInt("orderId")

                orderId?.let { viewModel.apiCallOrderDetails(it) }

            } else {
                orderId = floorPlanModel?.currentOrderDetails?.orderId
                floorPlanModel?.currentOrderDetails?.orderId?.let { viewModel.apiCallOrderDetails(it) }
                // binding.txtTitle.setText("" + floorPlanModel?.tableName)
            }


        } else {

            orderId = arguments?.getInt("orderId")
            orderId?.let { viewModel.apiCallOrderDetails(it) }
            //  binding.txtTitle.setText("Order Details")

        }
        onClick()


    }

    private fun getData() {
        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()

    }

    private fun onClick() {

        binding.imgMergeTable.setOnClickListener {
            getOrderDetailsResponse?.floorPlanTable?.id?.let { it1 -> viewModel.unMergeTable(it1) }
        }

        binding.imgPrintAll.setOnClickListener {
            getCustomerPrinters("")

        }

        binding.llInfo1.setOnClickListener {
            showPopupWindow(it)

        }

        binding.txtFloorPlan.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dineInFragment)

        }

        binding.txtFireAll.setOnClickListener {
            val list = dineInTableAdapter.getList()
            val ids: MutableList<Int> = ArrayList()
            list.forEach {
                if (it.isHeader == 1) {
                    it.item?.orderItemId?.let { it1 -> ids.add(it1) }
                }
            }

            // var idStr = Gson().toJson(ids.toTypedArray())
            val builder = java.lang.StringBuilder()
            for (i in 0 until ids.size) {
                builder.append(ids.get(i))
                builder.append(",")
            }

            orderId?.let { it1 ->
                isFireAll = true
                viewModel.fireItemToKitchen(
                    it1,
                    true,
                    builder.substring(0, builder.length - 1).toString(),
                    true
                )
            }
        }

        binding.btnPayNew.setOnClickListener {

            val adapterList = dineInTableAdapter.getList()

            var subTotal = 0.0
            var amtToPay = 0.0
            val totalItem: ArrayList<TbItem> = arrayListOf()
            for (i in 0 until adapterList.size) {
                if (adapterList.get(i).isHeader == 1) {
                    adapterList.get(i).item?.let {
                        totalItem.add(it)
                    }
                }
            }
            if (totalItem.isNotEmpty()) {
                totalItem.forEach {
                    if (!it.isPaid) {
                        subTotal += (it.price * it.itemQuantity) - it.discountPrice
                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {
                                subTotal += it.itemQuantity * it.price
                            }
                        }

                        it.taxes?.forEach { tax ->
                            totalTax += tax.rate
                        }
                    }
                }
            }
            // subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt


            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                var divideGuest = totalGuestCount - paidGuestAmount
                divideCashDiscount =
                    MethodUtils.calculateCashDiscount(
                        toFinalAmt,
                        prefProvider,
                        requireContext()
                    ) / divideGuest
            } else {
                divideCashDiscount = 0.0
            }

            var model = GuestPaymentRequest(
                GuestPaymentAttributes().apply {
                    amount = MethodUtils.roundOffAmountDouble(toFinalAmt)
                    cardName = ""
                    cardNumber = ""
                    cardType = ""
                    cashDiscount = 0.0
                    cash_discount_or_surcharge = divideCashDiscount
                    cashDiscountFee = 0.0
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    taxAmount = MethodUtils.roundOffAmountDouble(finalTaxAmt)
                    subTotalPrice = MethodUtils.roundOffAmountDouble(subTotalWT)
                    offlineId = randomOfflineId()
                    payableType = "GuestTab"
                    paymentType = "Cash"
                    transactionId = randomOfflineId()
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    serviceChargeAmount = MethodUtils.roundOffAmountDouble(serviceCharge)


                },
                DineInPaymentUpdateModel()
            )
            val bundle = Bundle()
            bundle.putDouble("totalPrice", MethodUtils.roundOffAmountDouble(toFinalAmt))
            bundle.putDouble("subTotalPrice", MethodUtils.roundOffAmountDouble(subTotalDInin))
            bundle.putDouble("totalTax", MethodUtils.roundOffAmountDouble(finalTaxAmt))
            bundle.putParcelable("model", model)
            bundle.putDouble(
                "divideCashDiscount",
                MethodUtils.roundOffAmountDouble(divideCashDiscount)
            )
            bundle.putParcelable("floorPlan", floorPlanModel)
            bundle.putDouble("totalServiceCharge", MethodUtils.roundOffAmountDouble(serviceCharge))
            bundle.putDouble("totalDiscount", MethodUtils.roundOffAmountDouble(totalDiscount))
            bundle.putBoolean("isTotalPayment", true)
            bundle.putBoolean("isLastPayment", true)

            orderId?.let { it1 -> bundle.putInt("orderId", it1) }

            findNavController().navigate(
                R.id.action_dineInOrderTable_to_payByGuestDialog,
                bundle
            )


        }


        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }
        binding.txtHomeBottom.setOnClickListener {
            findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
        }

        binding.txtEditOrder.setOnClickListener {
            val list = dineInTableAdapter.getList()
            val newList: ArrayList<DineInModel> = arrayListOf()


            for (i in 0 until list.size) {
                val model = DineInModel()
                if (list[i].isHeader == 0) {
                    val listTbItem: ArrayList<TbItem> = arrayListOf()
                    model.id = list[i].id
                    model.isPaid = list[i].isPaid
                    model.title = list[i].title
                    model.customer = list[i].customer
                    model.isFired = list[i].isFired
                    model.guestDividerAmt = list[i].guestDividerAmt
                    model.guestDividedAmt = list[i].guestDividedAmt


                    for (j in i + 1 until list.size) {
                        if (list[j].isHeader == 1) {
                            list[j].item?.let { it1 -> listTbItem.add(it1) }

                        } else {
                            break
                        }

                    }
                    model.items = listTbItem
                    newList.add(model)


                }


            }

            val bundle = Bundle()
            bundle.putBoolean("is_dine_in_edit", true)
            bundle.putParcelableArrayList(
                "dine_in_list",
                newList
            )

            bundle.putDouble("totalDiscount", totalDiscount)
            bundle.putParcelable("tableDetails", getOrderDetailsResponse?.floorPlanTable)

            orderId?.let { it1 -> bundle.putInt("orderId", it1) }

            prefProvider.setValueboolean(DINE_IN_UPDATE, true)
            prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
            prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
            prefProvider.setValueInt(Constants.DINE_IN_TABLE_ID, 2)
            prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)

            /*   prefProvider.setValu
            e(Constants.ORDER_TYPE, DINE_IN)
               prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
               prefProvider.setValueInt(ORDER_TYPE_ID, 2)*/

            findNavController().navigate(
                R.id.action_dineInOrderTable_to_dashboardCategoryNew,
                bundle
            )


        }

        binding.btnSendOrder.setOnClickListener {
            var guestAttribute = dineInData?.order?.guestAttributes

            var guestList = dineInTableAdapter.getList()
            for (i in 0 until guestList.size) {
                guestList.get(i).isFired = true
                guestList.get(i).items.forEach {
                    it.isFired = true
                }
            }

            dineInTableAdapter.setList(guestList.toCollection(arrayListOf()))

            if (guestAttribute != null) {

                val ids: MutableList<Int> = ArrayList()


                /*  for (i in 0 until guestAttribute.size) {
                      if (guestAttribute[i].guestItemAttributes != null) {
                          guestAttribute[i].guestItemAttributes.forEach {
                              it.orderItemId?.let { it1 -> ids.add(it1) }
                          }
                      }
                  }*/



                for (i in 0 until guestAttribute.size) {
                    guestAttribute[i].guestItemAttributes.forEach { it ->

                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }
                var idStr = ids.toString()


                // viewModel.fireItemToKitchen(dineInData?.order?.id!!, true, idStr)

            } else {

                var guestAttribute = dineInTableAdapter.getList()
                val ids: MutableList<Int> = ArrayList()

                for (i in 0 until guestAttribute.size) {
                    guestAttribute[i].items.forEach { it ->
                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }
                var idStr = Gson().toJson(ids.toTypedArray())


                viewModel.fireItemToKitchen(orderId!!, true, idStr, false)
            }

        }

        /*  binding.imgClose.setOnClickListener {
              findNavController().popBackStack()
          }*/

        binding.llInfo.setOnClickListener {
            showPopupWindow(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPopupWindow(view: View) {

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window, null)

        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)
        val txtTotalcashAdj: AppCompatTextView = popupView.findViewById(R.id.txtnoncashadj)
        val chkLoyaltyAmount: CheckBox = popupView.findViewById(R.id.chkLoyaltyAmount)
        val groupLoyalty: Group = popupView.findViewById(R.id.groupLoyalty)
        chkLoyaltyAmount.visibility = View.GONE
        groupLoyalty.visibility = View.GONE

        val linear_NonCashDiscount: LinearLayoutCompat =
            popupView.findViewById(R.id.linear_NonCashDiscount)

        if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {
            linear_NonCashDiscount.visibility = View.VISIBLE
            txtTotalcashAdj.text = "$" + String.format(
                "%.2f",
                MethodUtils.calculateCashDiscount(
                    toFinalAmt,
                    prefProvider,
                    requireContext()
                )
            )
        } else {
            linear_NonCashDiscount.visibility = View.GONE
        }




        txtSubTotal.text = "$" + String.format(
            "%.2f",
            subTotalDInin
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            serviceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            totalDiscount
        )


        txtTotalAmount.text = binding.txtTotalAmountNew.text.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            finalTaxAmt
        )

//        if (popupWindow == null) {
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow!!.setBackgroundDrawable(BitmapDrawable())
        popupWindow!!.isOutsideTouchable = true


        popupWindow!!.setOnDismissListener(PopupWindow.OnDismissListener {

        })
        popupWindow!!.showAtLocation(view, Gravity.TOP, 600, 650);
//        } else {
//            popupWindow!!.dismiss()
//            popupWindow = null
//        }

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

        viewModel.msgText.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)


                dineInTableAdapter.updateStatus(clickedPos, isFireAll)


                // orderId?.let { it1 -> viewModel.apiCallOrderDetails(it1) }
            }
        })


    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    /* override fun onGuestPay(dineInModel: DineInModel, position: Int) {
         Log.e(TAG, "dineInModelPay:  ${Gson().toJson(dineInModel)}")


         val total = dineInModel.items
         var subTotal = 0.0
         var totalTax = 0.0
         if (total.isNotEmpty()) {
             total.forEach {
                 subTotal += it.price * it.itemQuantity
                 it.taxes?.forEach { tax ->
                     totalTax += tax.rate
                 }
             }
             subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt

             var total = subTotal + totalTax
             Log.e(TAG, "total:  ${total}")

             var model = GuestPaymentRequest(
                 PaymentAttributes().apply {
                     amount = total
                     cardName = ""
                     cardNumber = ""
                     cardType = ""
                     cashDiscount = 0.0
                     cashDiscountFee = 0.0
                     employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                     taxAmount = totalTax
                     subTotalPrice = subTotal
                     offlineId = randomOfflineId()
                     payableType = "GuestTab"
                     paymentType = "Cash"
                     transactionId = randomOfflineId()
                     terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)

                 }
             )

             // dineInModel.id?.let { viewModel.payByGuest(it, model) }

             Log.e(TAG, "DineTablecartList:  ${Gson().toJson(cartList)}")
             Log.e(TAG, "DineTabletotalPrice:   ${totalPrice}")
             Log.e(TAG, "DineTabletotalTax: ${totalTax}")

             val bundle = Bundle()
             bundle.putInt("id", dineInModel.id!!)
             bundle.putParcelable("cartList", cartList)
             bundle.putDouble("totalPrice", total)
             bundle.putDouble("subTotalPrice", subTotal)
             bundle.putDouble("totalTax", totalTax)
             bundle.putParcelable("model", model)
             bundle.putParcelable("floorPlan", floorPlanModel)

             val list = dineInTableAdapter.getList()

             var wholeTableAmt = 0.0
             for (i in 0 until list.size) {
                 list.get(i).items.forEach {
                     wholeTableAmt += it.price * it.itemQuantity
                     if (it.modifiers.isNotEmpty()) {
                         it.modifiers.forEach {
                             wholeTableAmt += it.price * it.itemQuantity
                         }
                     }

                 }

             }


             var totalPaid = 0.0

             var wtAmt: Double = 0.0
             list.get(0).items.forEach {
                 wtAmt += it.price * it.itemQuantity
                 if (it.modifiers.isNotEmpty()) {
                     it.modifiers.forEach {
                         wtAmt += it.price * it.itemQuantity
                     }
                 }
             }


             Log.e(TAG, " ComplexResponse  ${Gson().toJson(list)}")

             var dividedAmt: Double =
                 wtAmt / (list.size - 1)
             Log.e(TAG, "NEwdividedAmt ${dividedAmt}")
             for (i in 0 until list.size) {
                 list.get(i).items.forEach {
                     if (it.isPaid) {
                         totalPaid += it.price * it.itemQuantity
                         if (it.modifiers.isNotEmpty()) {
                             it.modifiers.forEach {

                                 totalPaid += it.price * it.itemQuantity
                             }
                         }

                         Log.e(TAG, "totalPaidNew:  ${totalPaid}")
                     }

                     Log.e(TAG, "isPaidAmt ${list.get(i).isPaid}")


                 }
                 if (list.get(i).isPaid) {
                     totalPaid += dividedAmt
                 }
             }

             totalPaid = totalPaid
             wholeTableAmt = wholeTableAmt
             total = total

             Log.e(TAG, "totalPaid  ${totalPaid}")
             Log.e(TAG, "wholeTableAmtGetD  ${wholeTableAmt}")

             Log.e(TAG, "itemPayment:  ${total}")

             if ((wholeTableAmt - totalPaid) == total) {
                 bundle.putBoolean("isLastPayment", true)
             } else {
                 bundle.putBoolean("isLastPayment", false)
             }

             findNavController().navigate(
                 R.id.action_dineInOrderTable_to_payByGuestDialog,
                 bundle
             )
             *//* val list = dineInTableAdapter.getList()
             list[position].isPaid = true
             dineInTableAdapter.setList(list.toCollection(arrayListOf()))
 *//*

        }

    }*/

    override fun onGuestPay(
        dineInModel: DineInModel,
        position: Int,
        subTotalGuest: Double,
        totalGuest: Double,
        taxGuest: Double,
        serviceChargeGuest: Double,
        cashSurcharge: Double
    ) {

        //New Drag and Drop


        val adapterList = dineInTableAdapter.getList()


        var subTotal = 0.0
        var amtToPay = 0.0
        val totalItem: ArrayList<TbItem> = arrayListOf()
        for (i in position + 1 until adapterList.size) {
            if (adapterList.get(i).isHeader == 1) {
                adapterList.get(i).item?.let {
                    totalItem.add(it)
                }
            } else {
                break
            }
        }
        if (totalItem.isNotEmpty()) {
            totalItem.forEach {
                subTotal += it.price * it.itemQuantity
                it.taxes?.forEach { tax ->
                    totalTax += tax.rate
                }
            }
        }
        subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt

        var total = subTotal + totalTax


        val paymentAttr = GuestPaymentAttributes().apply {
            amount = totalGuest
            cardName = ""
            cardNumber = ""
            cardType = ""
            cashDiscount = 0.0
            cashDiscountFee = 0.0
            cash_discount_or_surcharge = cashSurcharge / totalGuestCount
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            taxAmount = taxGuest
            subTotalPrice = subTotalGuest
            offlineId = randomOfflineId()
            payableType = "GuestTab"
            paymentType = "Cash"
            transactionId = randomOfflineId()
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            order_id = orderId
            paymentAttributes = listOf(GuestPaymentAttributes().apply {
                amount = totalGuest
                cardName = ""
                cardNumber = ""
                cardType = ""
                cashDiscount = 0.0
                cashDiscountFee = 0.0
                cash_discount_or_surcharge = cashSurcharge / totalGuestCount
                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                taxAmount = taxGuest
                subTotalPrice = subTotalGuest
                offlineId = randomOfflineId()
                payableType = "GuestTab"
                paymentType = "Cash"
                transactionId = randomOfflineId()
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                order_id = orderId

            })

        }
        var dineInOrderModel = DineInPaymentUpdateModel()
        dineInOrderModel.id = orderId
        // dineInOrderModel.paymentAttributes = paymentAttr

        var modelReq = DineInOrderPayment(dineInOrderModel)

        //var splitModel = SpitByOrderRequestModel(orderId,completed_all_payments = false,paymentAttr,null)

        var model = GuestPaymentRequest(paymentAttr, dineInOrderModel)

        val bundle = Bundle()
        dineInModel.id?.let { bundle.putInt("id", it) }
        bundle.putParcelable("cartList", cartList)
        bundle.putDouble("totalPrice", totalGuest)
        bundle.putDouble("cashSurcharge", cashSurcharge)
        bundle.putInt("totalGuestCount", totalGuestCount)
        bundle.putInt("paidGuestCount", paidGuestAmount)
        bundle.putDouble("subTotalPrice", subTotalGuest)
        bundle.putDouble("totalTax", taxGuest)
        bundle.putParcelable("model", model)
        bundle.putParcelable("floorPlan", floorPlanModel)
        bundle.putDouble("totalServiceCharge", serviceChargeGuest)
        bundle.putParcelable("orderPayment", modelReq)
        orderId?.let { bundle.putInt("orderId", it) }
        bundle.putBoolean("isGuestPay", true)

        var wholeTableAmt = 0.0
        var paidAmount = 0.0
        for (i in 0 until adapterList.size) {
            adapterList.get(i).item.let {
                wholeTableAmt += it?.price?.times(it.itemQuantity) ?: 0.0
                if (it?.isPaid == true) {
                    paidAmount += it.price.times(it.itemQuantity)
                }


            }
            if (adapterList.get(i).isPaid) {
                paidAmount += adapterList.get(0).guestDividedAmt
            }

        }


        if (totalGuestCount.minus(1) == paidGuestAmount) {
            bundle.putBoolean("isLastPayment", true)
        } else {
            bundle.putBoolean("isLastPayment", false)
        }
        findNavController().navigate(
            R.id.action_dineInOrderTable_to_payByGuestDialog,
            bundle
        )
        // var subTotal = 0.0
        var totalTax = 0.0


        /*val total = dineInModel.items
        var subTotal = 0.0
        var totalTax = 0.0
        if (total.isNotEmpty()) {
            total.forEach {
                subTotal += it.price * it.itemQuantity
                it.taxes?.forEach { tax ->
                    totalTax += tax.rate
                }
            }
            subTotal += dineInTableAdapter.getList().get(0).guestDividedAmt

            var total = subTotal + totalTax
            Log.e(TAG, "total:  ${total}")

            var model = GuestPaymentRequest(
                PaymentAttributes().apply {
                    amount = total
                    cardName = ""
                    cardNumber = ""
                    cardType = ""
                    cashDiscount = 0.0
                    cashDiscountFee = 0.0
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    taxAmount = totalTax
                    subTotalPrice = subTotal
                    offlineId = randomOfflineId()
                    payableType = "GuestTab"
                    paymentType = "Cash"
                    transactionId = randomOfflineId()
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)

                }
            )

            // dineInModel.id?.let { viewModel.payByGuest(it, model) }

            Log.e(TAG, "DineTablecartList:  ${Gson().toJson(cartList)}")
            Log.e(TAG, "DineTabletotalPrice:   ${totalPrice}")
            Log.e(TAG, "DineTabletotalTax: ${totalTax}")

            val bundle = Bundle()
            bundle.putInt("id", dineInModel.id!!)
            bundle.putParcelable("cartList", cartList)
            bundle.putDouble("totalPrice", total)
            bundle.putDouble("subTotalPrice", subTotal)
            bundle.putDouble("totalTax", totalTax)
            bundle.putParcelable("model", model)
            bundle.putParcelable("floorPlan", floorPlanModel)

            val list = dineInTableAdapter.getList()

            var wholeTableAmt = 0.0
            for (i in 0 until list.size) {
                list.get(i).items.forEach {
                    wholeTableAmt += it.price * it.itemQuantity
                    if (it.modifiers.isNotEmpty()) {
                        it.modifiers.forEach {
                            wholeTableAmt += it.price * it.itemQuantity
                        }
                    }

                }

            }


            var totalPaid = 0.0

            var wtAmt: Double = 0.0
            list.get(0).items.forEach {
                wtAmt += it.price * it.itemQuantity
                if (it.modifiers.isNotEmpty()) {
                    it.modifiers.forEach {
                        wtAmt += it.price * it.itemQuantity
                    }
                }
            }


            Log.e(TAG, " ComplexResponse  ${Gson().toJson(list)}")

            var dividedAmt: Double =
                wtAmt / (list.size - 1)
            Log.e(TAG, "NEwdividedAmt ${dividedAmt}")
            for (i in 0 until list.size) {
                list.get(i).items.forEach {
                    if (it.isPaid) {
                        totalPaid += it.price * it.itemQuantity
                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {

                                totalPaid += it.price * it.itemQuantity
                            }
                        }

                        Log.e(TAG, "totalPaidNew:  ${totalPaid}")
                    }

                    Log.e(TAG, "isPaidAmt ${list.get(i).isPaid}")


                }
                if (list.get(i).isPaid) {
                    totalPaid += dividedAmt
                }
            }

            totalPaid = totalPaid
            wholeTableAmt = wholeTableAmt
            total = total

            Log.e(TAG, "totalPaid  ${totalPaid}")
            Log.e(TAG, "wholeTableAmtGetD  ${wholeTableAmt}")

            Log.e(TAG, "itemPayment:  ${total}")

            if ((wholeTableAmt - totalPaid) == total) {
                bundle.putBoolean("isLastPayment", true)
            } else {
                bundle.putBoolean("isLastPayment", false)
            }

            findNavController().navigate(
                R.id.action_dineInOrderTable_to_payByGuestDialog,
                bundle
            )*/
        /* val list = dineInTableAdapter.getList()
         list[position].isPaid = true
         dineInTableAdapter.setList(list.toCollection(arrayListOf()))
*/

    }


    override fun onSendItemToKitchen(item: TbItem) {

        val ids: MutableList<Int> = ArrayList()
        item.orderItemId?.let { ids.add(it) }


        /*  for (i in 0 until guestAttribute.size) {
              if (guestAttribute[i].guestItemAttributes != null) {
                  guestAttribute[i].guestItemAttributes.forEach {
                      it.orderItemId?.let { it1 -> ids.add(it1) }
                  }
              }
          }*/


        var idStr = Gson().toJson(ids.toString())


        viewModel.fireItemToKitchen(orderId!!, true, idStr, false, item)

    }

    override fun onWholeTableToKitchen(ids: String) {

        viewModel.fireItemToKitchen(orderId!!, true, ids, true)
    }

    override fun singleItemFired(id: String, position: Int, item: TbItem) {

        clickedPos = position
        viewModel.fireItemToKitchen(orderId!!, true, id, false, item)


        for (i in 0 until kitchenPrinterList.size) {

            initKitchenPrinter(kitchenPrinterList.get(i), Constants.KITCHEN, item)
        }

    }

    override fun onGuestPrint(
        listItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>
    ) {
        if (listItem[0].isPaid == true) {
            guestPrint("Paid", listItem, guestName, listWTitems)
        } else {
            guestPrint("Unpaid", listItem, guestName, listWTitems)

        }

    }

    private fun guestPrint(
        paymentStatus: String,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        wtItems: ArrayList<TbItem>
    ) {

        Log.e(TAG, "customerListSize  ${customerList.size}")
        if (customerList.isNotEmpty()) {
            customerList.forEach {
                initPrinter(
                    it,
                    Constants.CUSTOMER,
                    paymentStatus,
                    true,
                    listGuestItem,
                    guestName,
                    wtItems
                )

            }
        }

    }

    private fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss


        return timeStampFinal
    }

    protected open fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }

    private fun navigateDineInOrder() {
        viewModel.Basedata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
                    subTotalWT = 0.0
                    serviceCharge = 0.0
                    totalDiscount = 0.0

                    if (baseResponse.floorPlanTable.status == MERGEDANDOCCUPIED) {
                        binding.imgMergeTable.visibility = View.VISIBLE
                        binding.imgMergeTable.setImageDrawable(
                            requireContext().resources.getDrawable(
                                R.drawable.ic_unmerge
                            )
                        )
                    } else {
                        binding.imgMergeTable.visibility = View.GONE
                    }

                    if (baseResponse.floorPlanTable.status == MERGEDANDOCCUPIED) {

                        var listTableMerge: ArrayList<String> = arrayListOf()
                        listTableMerge.add(baseResponse.floorPlanTable.tableNumber.toString())
                        baseResponse.floorPlanTable?.merged_child_table_details.forEach {
                            listTableMerge.add(it.table_number.toString())

                        }
                        var txtMergedTbNo = android.text.TextUtils.join(",", listTableMerge)
                        binding.txtTitle.text = "Table " + txtMergedTbNo

                    } else {
                        binding.txtTitle.text = baseResponse.floorPlanTable.tableName
                    }


                    getOrderDetailsResponse = baseResponse
                    /*binding.txtTitle.text = baseResponse.*/
                    var list: ArrayList<DineInModel> = arrayListOf()

                    var wholeTableAmt = 0.0

                    var paidAmt = 0.0
                    var wholeTablePosition: Int = 0
                    //New Drag and drop Code
                    var totalPay = 0.0
                    var itemsDiscount = 0.0
                    var isPaidCount = 0.0
                    var isUnpaidCount = 0.0
                    var WholeTableAmount = 0.0
                    val dineInList: ArrayList<DineInModel> = arrayListOf()

                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val model = DineInModel()
                        var totalGuestPrice = 0.0
                        var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                        //model.isPaid = listTbItem.get(0).isPaid

                        model.title = baseResponse.guestAttributes.get(i).name
                        model.isHeader = 0

                        if (baseResponse?.guestAttributes?.get(i)?.customerId != null && baseResponse.guestAttributes.get(
                                i
                            ).customerId != 0
                        ) {
                            allCustomerList.forEach {
                                if (it.id == baseResponse.guestAttributes.get(i).customerId) {
                                    model.customer = it
                                }
                            }

                        }


                        var fullAmt = 0.0


                        for (j in 0 until guestItem.size) {
                            if (baseResponse.orderItems.isNotEmpty()) {
                                baseResponse.orderItems.forEach { it ->
                                    if (it.timestamp == guestItem[j].timestamp) {

                                        model.isPaid = it.isPaid
                                        if (!it.isPaid) {
                                            totalGuestPrice += (it.price * it.quantity)
                                            fullAmt += it.quantity * it.price
                                            subTotalWT += (it.quantity * it.price)
                                            itemsDiscount += it.discountAmount

                                        }


                                        if (it.orderItemModifiers.isNotEmpty()) {
                                            it.orderItemModifiers.forEach { it1 ->
                                                if (!it.isPaid) {
                                                    totalGuestPrice += (it1.price * it1.quantity)
                                                    fullAmt += it1.quantity * it1.price
                                                    subTotalWT += it1.quantity * it1.price

                                                }
                                            }
                                        }
                                        if (it.orderItemTaxes.isNotEmpty()) {
                                            it.orderItemTaxes.forEach { tax ->
                                                if (!it.isPaid) {
                                                    totalTaxAmt += tax.rate
                                                    fullAmt += tax.rate

                                                }

                                            }
                                        }


                                    }

                                    if (it.isPaid) {
                                        notPayAnyAmount = true
                                    } else {


                                    }
                                }

                            }
                        }

                        serviceCharge = 0.0
                        var tmpSubTotal = subTotalWT - itemsDiscount
                        serviceChargeList.forEach {
                            if (it.isEnabled) {
                                serviceCharge += (tmpSubTotal * it.percentage) / 100
                            }
                        }

                        fullAmt += serviceCharge
                        totalPay += fullAmt


                        var dividedAmt = subTotalWT / (baseResponse.guestAttributes.size - 1)
                        model.guestDividedAmt = dividedAmt
                        Log.d("one", "navigateDineInOrder: " + model.guestDividedAmt)
                        totalGuestPrice +=
                            fullAmt / (baseResponse.guestAttributes.size - 1)

                        model.totalGuestPrice = totalGuestPrice
                        if (serviceChargeList.isNotEmpty()) {
                            model.serviceChargeList = serviceChargeList
                        }

                        model.id = baseResponse.guestAttributes[i].id

                        dineInList.add(model)



                        for (j in 0 until guestItem.size) {
                            baseResponse.orderItems.forEach {
                                val itemDineIn: DineInModel = DineInModel()
                                if (it.timestamp == guestItem[j].timestamp) {

                                    val item = TbItem()
                                    item.isPaid = it.isPaid
                                    item.discountPrice = it.discountAmount
                                    item.discountId = it.discountId
                                    item.discountType = it.discountType

                                    item.name = it.itemName
                                    item.itemId = it.itemId
                                    item.categoryId = it.categoryId
                                    item.guestItemId = guestItem[j].id

                                    var listTaxes: ArrayList<TaxData> = arrayListOf()
                                    it.orderItemTaxes.forEach {
                                        listTaxes.add(
                                            TaxData(
                                                createdAt = it.createdAt,
                                                id = it.id,
                                                locationId = prefProvider.getValueInt(
                                                    LOCATION_ID,
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
                                        var modifiers: ArrayList<Modifier> = arrayListOf()
                                        it.orderItemModifiers.forEach { mod ->
                                            val model = Modifier()
                                            model.id = mod.id
                                            model.itemQuantity = mod.quantity
                                            model.name = mod.name
                                            model.orderModifierId = mod.orderItemId
                                            model.price = mod.price


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




                                    itemDineIn.isHeader = 1
                                    itemDineIn.item = item
                                    itemDineIn.empName =
                                        baseResponse.floorPlanTable.lockByName.toString()

                                    dineInList.add(itemDineIn)


                                }

                            }


                        }
                    }

                    //paid and unpaid guest count
                    totalGuestCount = baseResponse.guestAttributes.size - 1
                    var totalGuestPaidCount = 0.0

                    //Whole Table Calculation
                    var WTSubTotal: Double = 0.0
                    var WTTaxes: Double = 0.0
                    var WTServiceCharge: Double = 0.0




                    for (i in 1 until dineInList.size) {

                        if (dineInList.get(i).isHeader == 1) {
                            dineInList.get(i).item?.let {
                                if (!it.isPaid) {
                                    WTSubTotal += (it.itemQuantity * it.price) - it.discountPrice

                                    if (it.modifiers.isNotEmpty()) {
                                        it.modifiers.forEach {
                                            WTSubTotal += it.itemQuantity * it.price
                                        }

                                    }
                                    if (it.taxes?.isNotEmpty() == true) {
                                        it.taxes?.forEach { tax ->
                                            if (tax.isActive) {
                                                WTTaxes += if (tax.taxType == "Percentage") {

                                                    var modifierPrice = 0.0
                                                    val price =
                                                        (it.price * it.itemQuantity) - it.discountPrice

                                                    it.modifiers.forEach {
                                                        modifierPrice += (it.price * it.itemQuantity)
                                                    }

                                                    val totalPrice = price + modifierPrice

                                                    val itemTaxPrice =
                                                        (tax.rate * totalPrice) / 100
                                                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                                                    String.format("%.2f", itemTaxPrice)
                                                        .toDouble()
                                                } else {

                                                    String.format(
                                                        "%.2f",
                                                        tax.rate * it.itemQuantity
                                                    )
                                                        .toDouble()
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                        } else {
                            break

                        }
                    }
                    if (serviceChargeList?.isNotEmpty() == true) {
                        serviceChargeList?.forEach {
                            if (it.isEnabled) {
                                WTServiceCharge += (WTSubTotal * it.percentage) / 100
                            }
                        }

                    }

                    Log.e(TAG, "WTSubTotal:  ${WTSubTotal}")
                    Log.e(TAG, "WTTaxes:  ${WTTaxes}")
                    WTOnlyTax = WTTaxes
                    WTServiceTax = WTServiceCharge
                    Log.e(TAG, "WTServiceCharge:  ${WTServiceCharge}")

                    Log.e(TAG, "totalDiscount:  ${baseResponse.totalDiscount}")
                    Log.e(TAG, "itemsDiscount:  ${itemsDiscount}")


                    var orderDiscount = 0.0
                    if (baseResponse.totalDiscount >= itemsDiscount) {
                        orderDiscount = baseResponse.totalDiscount - itemsDiscount
                    }
                    dineInList.get(0).guestDividedAmt =
                        MethodUtils.roundOffAmountDouble((WTSubTotal + WTTaxes + WTServiceCharge - orderDiscount) / (baseResponse.guestAttributes.size - 1))
                    dineInList.get(0).totalGuestCount = baseResponse.guestAttributes.size - 1
                    dineInList.get(0).wholeTableSubTotal =
                        WTSubTotal / dineInList.get(0).totalGuestCount
                    dineInList.get(0).wholeTableTax = WTTaxes / dineInList.get(0).totalGuestCount
                    dineInList.get(0).wholeTableSurTax =
                        WTServiceCharge / dineInList.get(0).totalGuestCount

                    Log.d(
                        "guestDivide",
                        "navigateDineInOrder: " + (WTSubTotal + WTTaxes + WTServiceCharge - orderDiscount) / (baseResponse.guestAttributes.size - 1)
                    )
                    WholeTableAmount = MethodUtils.roundOffAmountDouble(
                        (WTSubTotal + WTTaxes + WTServiceCharge - orderDiscount)
                    )

                    var service: Double = 0.0
                    var serviceSubTotal = subTotalWT - itemsDiscount

                    serviceChargeList.forEach {
                        service += (serviceSubTotal * it.percentage) / 100
                    }

                    // subTotalWT -= baseResponse.totalDiscount


                    dineInList.forEach {
                        if (it.isHeader == 1 && !it.isPaid) {
                            it.item?.let { it1 ->
                                viewModel.taxCalculation(it1)
                                totalDiscount = it1.discountPrice
                            }

                        }
                    }
                    totalDiscount = 0.0
                    totalDiscount += baseResponse.totalDiscount

                    val totalAmoountTxt =
                        MethodUtils.roundOffAmount(subTotalWT + serviceCharge + viewModel.totalTaxAmount - baseResponse.totalDiscount)

                    var fisrtTime: Boolean = false
                    for (i in 1 until dineInList.size) {
                        if (dineInList.get(i).isHeader == 1 && fisrtTime) {

                            if (dineInList.get(i).item?.isPaid == false) {
                                dineInList.get(i).item?.let {
                                    totalAmtnew += (it.price * it.itemQuantity) - it.discountPrice
                                    if (it.modifiers.isNotEmpty()) {
                                        it.modifiers.forEach {
                                            totalAmtnew += it.price * it.itemQuantity
                                        }

                                    }

                                    if (it.taxes?.isNotEmpty() == true) {
                                        it.taxes?.forEach {
                                            totalAmtnew += it.rate
                                        }

                                    }

                                    totalAmtnew += dineInList.get(0).guestDividedAmt

                                }
                            }

                        } else if (dineInList.get(i).isHeader == 0) {
                            fisrtTime = true
                        }

                    }
                    if (dineInList.isNotEmpty()) {
                        dineInTableAdapter.setList(dineInList)


                        //  binding.txtTotalAmountNew.setText("${MethodUtils.roundOffAmount(totalAmtnew)}")

                        if (!notPayAnyAmount) {
                            touchHelper.attachToRecyclerView(binding.rvItemList)
                        }
                    }




                    if (list.isNotEmpty()) {
                        baseResponse.guestAttributes.get(0).guestItemAttributes.forEach {
                            wholeTableAmt += it.amount * it.quantity
                        }

                        var dividedAmt: Double =
                            wholeTableAmt / (baseResponse.guestAttributes.size - 1)

//                        list.get(0).guestDividedAmt = dividedAmt

                        //  dineInTableAdapter.setList(list)
                        cartList = getCartModel(list)


                        totalAmount = 0.0
                        for (i in 0 until list.size) {
                            list.get(i).items.forEach { it ->
                                if (list.get(i).isPaid) {
                                    paidAmt += (it.itemQuantity * it.price) - it.discountPrice

                                    if (it.modifiers.isNotEmpty()) {
                                        it.modifiers.forEach {
                                            paidAmt += it.price * it.itemQuantity
                                        }
                                    }

                                }
                                totalAmount += it.itemQuantity * it.price

                                if (it.modifiers.isNotEmpty()) {
                                    it.modifiers.forEach {
                                        totalAmount += it.itemQuantity * it.price
                                    }
                                }


                            }
                            if (list.get(i).items.isNotEmpty()) {

                                if (list.get(i).items.get(0).isPaid) {
                                    paidAmt += dividedAmt
                                }
                            }

                        }

                    }

                    totalAmount -= paidAmt

                    if (totalAmount < 0) {
                        totalAmount = 0.0
                    }


                    //     binding.txtTotalAmountNew.setText("${MethodUtils.roundOffAmount(totalAmount)}")
                    totalPrice = MethodUtils.roundOffAmountDouble(totalAmount)


                    var guestAmt = 0.0
                    var isPaid = true
                    var isAllFired = true
                    var noItem = true
                    var guestSubTotal = 0.0
                    var totalTaxAmt: Double = 0.0
                    var serviceChargeGu = 0.0
                    var paidGuestCount = 0
                    var isFirstHeader = false

                    if (dineInList.isNotEmpty()) {

                        for (i in 0 until dineInList.size) {
                            if (dineInList.get(i).isHeader == 1 && isFirstHeader == false) {
                                dineInList.get(i).item?.let {
                                    if (!it.isPaid) {
                                        guestAmt += (it.itemQuantity * it.price) - it.discountPrice
                                        guestSubTotal += (it.itemQuantity * it.price) - it.discountPrice
                                        if (it.modifiers.isNotEmpty()) {
                                            it.modifiers.forEach { it ->
                                                guestAmt += it.itemQuantity * it.price
                                                guestSubTotal += it.itemQuantity * it.price
                                                Log.d("yash", "navigateDineInOrder: " + guestAmt)
                                                Log.d(
                                                    "yash",
                                                    "navigateDineInOrder: " + guestSubTotal
                                                )

                                            }
                                        }

                                        if (it.taxes?.isNotEmpty() == true) {

                                            it.taxes?.forEach { tax ->
                                                if (tax.isActive) {
                                                    totalTaxAmt += if (tax.taxType == "Percentage") {

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

                                                        String.format(
                                                            "%.2f",
                                                            tax.rate * it.itemQuantity
                                                        )
                                                            .toDouble()
                                                    }
                                                }
                                                guestAmt += tax.rate
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
                            } else if (dineInList.get(i).isHeader == 0) {
                                if (i == 0) {
                                    isFirstHeader = true
                                } else {
                                    isFirstHeader = false
                                }
                                if (i != 0 && dineInList.size > i + 1) {
                                    if (dineInList.get(i + 1).item != null && dineInList.get(i + 1).item?.isPaid == true) {
                                        paidGuestCount++
                                    }
                                }

                            }


                        }

                        if (dineInList[0].serviceChargeList?.isNotEmpty() == true) {
                            dineInList[0].serviceChargeList?.forEach {
                                if (it.isEnabled) {
                                    serviceChargeGu += (guestSubTotal * it.percentage) / 100
                                }
                            }


                        }
                        guestSubTotal = guestSubTotal


                        var divShare =
                            WholeTableAmount / ((baseResponse.guestAttributes.size - 1))

                        var totalG = baseResponse.guestAttributes.size - 1
                        var unpaidCount = totalG - paidGuestCount


                        var myShare = unpaidCount * divShare


                        if (paidGuestCount > 0) {
                            paidGuestAmount = paidGuestCount
                            subTotalDInin =
                                (WTSubTotal / totalG) * unpaidCount + guestSubTotal
                        } else {
                            subTotalDInin = WTSubTotal + guestSubTotal
                        }


                        Log.d(TAG, "navigateDineInOrder: " + subTotalDInin)

                        if (paidGuestCount > 0) {
                            serviceCharge =
                                (WTServiceCharge / totalG) * unpaidCount + serviceChargeGu
                            myShare -= WTServiceCharge / totalG * unpaidCount
                        } else {
                            serviceCharge = WTServiceCharge + serviceChargeGu
                            myShare -= WTServiceCharge
                        }
                        Log.d(TAG, "navigateDineInOrder: " + serviceCharge)

                        if (paidGuestCount > 0) {
                            finalTaxAmt = (WTTaxes / totalG) * unpaidCount + totalTaxAmt
                            myShare -= WTTaxes / totalG * unpaidCount
                        } else {
                            finalTaxAmt = WTTaxes + totalTaxAmt
                            myShare -= WTTaxes
                        }
                        Log.d(TAG, "navigateDineInOrder: " + finalTaxAmt)
                        Log.d(TAG, "navigateDineInOrder: " + myShare)
                        Log.d(TAG, "navigateDineInOrder: " + guestSubTotal)


                        var finalAmt =
                            guestSubTotal + serviceCharge + finalTaxAmt + myShare

                        Log.d(TAG, "navigateDineInOrder: " + finalAmt)
                        viewModel.totalTaxAmount = totalAmount
                        subTotalWT = guestSubTotal + myShare
                        toFinalAmt = finalAmt

                        binding.txtTotalAmountNew.text = MethodUtils.roundOffAmount(
                            finalAmt
                        )


                    }

                }
            }
        })
    }

    fun getCartModel(list: ArrayList<DineInModel>): CartModel {
        var model = CartModel()
        var listItem: ArrayList<TbItem> = arrayListOf()
        for (i in 0 until list.size) {
            listItem.addAll(list.get(i).items)


        }
        model.orderType = "DineIn"
        model.dineInList = list
        model.employeeID = prefProvider.getValueInt(EMPLOYEE_ID, 0)
        model.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
        model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)


        return model
    }


    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }

    private fun updateAdapterData() {


        var oldList = dineInTableAdapter.getList()
        var newList: ArrayList<DineInModel> = arrayListOf()
        var wholeTableAmt = 0.0
        var WTTax = 0.0
        var WTServiceCharge = 0.0
        var totalPaid = 0.0
        var guestShare = 0.0
        var totalTablePrice = 0.0
        var guestCount = 0

        for (i in 0 until oldList.size) {
            if (oldList.get(i).isHeader == 1) {
                if (oldList.get(i).item?.isPaid == true) {
                    oldList.get(i).item?.let {
                        totalPaid += (it.price * it.itemQuantity) - it.discountPrice
                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {
                                totalPaid += it.price * it.itemQuantity
                            }
                        }
                    }

                } else {

                    oldList.get(i).item?.let {
                        totalTablePrice += (it.price * it.itemQuantity) - it.discountPrice
                        if (it.modifiers.isNotEmpty()) {
                            it.modifiers.forEach {
                                totalTablePrice += it.price * it.itemQuantity
                            }
                        }
                    }

                }
            } else {
                guestCount++
            }


        }



        for (i in 1 until oldList.size) {
            if (oldList.get(i).isHeader == 1) {

                oldList.get(i).item?.let { it ->
                    wholeTableAmt += (it.price * it.itemQuantity) - it.discountPrice

                    if (it.modifiers.isNotEmpty()) {
                        it.modifiers.forEach {
                            wholeTableAmt += it.price * it.itemQuantity

                        }
                    }
                    if (it.taxes?.isNotEmpty() == true) {
                        it.taxes?.forEach { tax ->
                            if (tax.isActive) {
                                WTTax += if (tax.taxType == "Percentage") {

                                    var modifierPrice = 0.0
                                    val price =
                                        (it.price * it.itemQuantity) - it.discountPrice

                                    it.modifiers.forEach {
                                        modifierPrice += (it.price * it.itemQuantity)
                                    }

                                    val totalPrice = price + modifierPrice

                                    val itemTaxPrice =
                                        (tax.rate * totalPrice) / 100
                                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                                    String.format("%.2f", itemTaxPrice)
                                        .toDouble()
                                } else {

                                    String.format(
                                        "%.2f",
                                        tax.rate * it.itemQuantity
                                    )
                                        .toDouble()
                                }
                            }
                        }
                    }
                }


            } else {

                break
            }

        }
        if (serviceChargeList?.isNotEmpty() == true) {
            serviceChargeList?.forEach {
                if (it.isEnabled) {
                    WTServiceCharge += (wholeTableAmt * it.percentage) / 100
                }
            }

        }
        guestShare += (wholeTableAmt + WTServiceCharge + WTTax) / (guestCount - 1)

        for (i in 0 until oldList.size) {
            var model = DineInModel()
            if (oldList.get(i).isHeader == 1) {
                model.item = oldList.get(i).item
                model.totalTableAmt = totalTablePrice


            } else {
                model.title = oldList.get(i).title
                model.customer = oldList.get(i).customer

                model.guestDividedAmt = guestShare
                Log.d("two", "navigateDineInOrder: " + model.guestDividedAmt)
            }
            model.isHeader = oldList.get(i).isHeader
            newList.add(model)

        }
        var guestAmt = 0.0

        for (i in 0 until newList.size) {
            if (newList.get(i).isHeader == 1) {
                newList.get(i).item?.let { it ->
                    guestAmt += (it.price * it.itemQuantity) - it.discountPrice
                    if (it.modifiers.isNotEmpty()) {
                        it.modifiers.forEach {
                            guestAmt += it.itemQuantity * it.price
                        }
                    }

                }
            } else {
                newList.get(i).totalGuestPrice = guestAmt + guestShare
                guestAmt = 0.0
            }
        }

        dineInTableAdapter.setList(newList)

        updateOrderCall()

    }

    private fun updateOrderCall() {
        val list = dineInTableAdapter.getList()

        val orderModel = OrderAttributeRequestModel()

        orderModel.apply {
            id = orderId
            date = TimeFormatUtils.getCurrentDate()
            deliveryType = "DineIn"
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 1)
            offlineId = getOrderDetailsResponse?.offlineId.toString()
            openOrderType = "DineIn"
            orderTypeId = 2
            paymentStatus = 0
            getOrderDetailsResponse?.subTotal?.let {
                subTotal = it
            }
            getOrderDetailsResponse?.totalAmount?.let {
                totalAmount = it
            }
            getOrderDetailsResponse?.totalServiceCharges?.let {
                totalServiceCharges = it
            }

            val listOrderAttribute: ArrayList<OrderItemsAttribute> = arrayListOf()

            /* getOrderDetailsResponse?.orderItems?.let {
                 for (i in 0 until it.size) {
                     val model = OrderItemsAttribute()
                     model.category_id = it.get(i).categoryId
                     model.discountAmount = it.get(i).discountAmount
                     model.discountType = it.get(i).discountType
                     model.editTimestamp = it.get(i).timestamp
                     model.employeeId = it.get(i).employeeId
                     model.id = it.get(i).id
                     model.isEdited = true
                     model.itemId = it.get(i).itemId
                     model.orderId = it.get(i).orderId
                     model.price = it.get(i).price
                     model.quantity = it.get(i).quantity
                     model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                     model.timestamp = System.currentTimeMillis().toString()
                     model.totalPrice = it.get(i).totalPrice
                     if (it.get(i).orderItemModifiers.isNotEmpty()) {
                         var listModifiers: ArrayList<OrderItemModifierAttribute> =
                             arrayListOf()
                         it.get(i).orderItemModifiers.forEach {
                             val model = OrderItemModifierAttribute()
                             model.id = it.id
                             model.order_item_id = it.orderItemId
                             model.name = it.name
                             model.orderId = it.orderId
                             model.price = it.price
                             model.totalPrice = it.price
                             model.quantity = it.quantity
                             it.modifierId?.let {
                                 model.modifier_set_id = it.toInt()
                             }
                             var listTaxAttributes: ArrayList<OrderModifierTaxesAttribute> =
                                 arrayListOf()

                             if (it.orderItemTaxes.isNotEmpty()) {

                                 it.orderItemTaxes.forEach {
                                     val model = OrderModifierTaxesAttribute()
                                     model.id = it.id
                                     model.amount = it.amount
                                     model.isDefault = it.isDefault
                                     model.name = it.name

                                     listTaxAttributes.add(model)
                                 }


                             }
                             model.order_item_taxes_attributes = listTaxAttributes

                             listModifiers.add(model)

                         }
                         model.orderItemModifiersAttributes = listModifiers
                     }


                     listOrderAttribute.add(
                         model
                     )


                 }
             }*/

            val guestAttributes: ArrayList<GuestsAttributes> = arrayListOf()
            for (i in 0 until list.size) {

                val model = GuestsAttributes()
                if (list.get(i).isHeader == 1) {

                } else {

                }


            }


        }


    }


    val touchHelper =
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val oldPos = viewHolder.layoutPosition
                val newPos = target.layoutPosition

                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                dineInTableAdapter.onItemMove(
                    viewHolder.layoutPosition,
                    target.layoutPosition
                )

                return true
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //updateAdapterData()


            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    /* reallyMoved(
                         adapter.getItem(dragFrom).sort,
                         adapter.getItem(dragTo).sort,
                         adapter.getItem(viewHolder.layoutPosition).id
                     )*/


                }


                dragFrom = -1
                dragTo = -1
                updateAdapterData()

            }

        })

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner, {
            if (it != null) {
                customerSettingModel = it


            }

        })

    }

    private fun getCustomerPrinters(paymentType: String) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerList = it.data

                        customerList.forEach {
                            initPrinter(
                                it,
                                Constants.CUSTOMER,
                                paymentType,
                                false,
                                arrayListOf(),
                                "",
                                arrayListOf()
                            )


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
        type: String,
        paymentType: String,
        guestPrint: Boolean,
        listGuestItem: ArrayList<TbItem>,
        guestName: String,
        listWTitems: ArrayList<TbItem>
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
                    if (guestPrint) {
                        generateGuestPrint(
                            customerReceiptPrinters,
                            type,
                            paymentType,
                            listGuestItem,
                            guestName,
                            listWTitems
                        )

                    } else {


                        generatePrint(customerReceiptPrinters, type, "")
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
        listWTitems: ArrayList<TbItem>
    ) {
        var guestSubTotal = 0.0
        var guestTaxes = 0.0
        var guestServiceCharge = 0.0
        var guestDiscount = 0.0

        val guestCount = dineInTableAdapter.getList().size - 1
        Log.e(TAG, "guestCount:  ${guestCount}")



        listGuestItem.forEach {
            guestSubTotal += (it.price * it.itemQuantity) - it.discountPrice


            it.taxes?.forEach { tax ->
                if (tax.isActive) {
                    Log.e(TAG, "getTaxP  ${Gson().toJson(tax)}")
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

        serviceChargeList.forEach {
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

            builder.addText(getOrderDetailsResponse?.orderType + "\n")

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

                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)

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
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
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


                    builder.addText("Employee:" + getOrderDetailsResponse?.employee?.name)

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
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:a")
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
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
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
                                "Employee:" + getOrderDetailsResponse?.employee?.name
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
                                    getOrderDetailsResponse?.createdAt.toString()
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
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:a")
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
                    builder, listWTitems.get(i), customerSettingModel.fonts,
                    customerSettingModel.showModifiers, totalGuestCount, serviceChargeList
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

            if (getOrderDetailsResponse?.totalDiscount != null) {
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
                        guestSubTotal + dineInTableAdapter.getList().get(0).guestDividedAmt
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
                        "$" + MethodUtils.roundOffAmountString(guestTaxes),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (guestServiceCharge != null) {
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
                        "$" + MethodUtils.roundOffAmountString(guestServiceCharge),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


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
                    guestSubTotal + guestTaxes + guestServiceCharge + dineInTableAdapter.getList()
                        .get(0).guestDividedAmt
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
                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    builder.addText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
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
            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
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
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
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
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
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


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {

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

                builder.addText(getOrderDetailsResponse?.note)
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getOrderDetailsResponse?.digitalReceiptUrl.toString())

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
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        paymentType: String
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

            builder.addText(getOrderDetailsResponse?.orderType + "\n")

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

                    builder.addText("OrderID:" + getOrderDetailsResponse?.id)

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
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
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


                    builder.addText("Employee:" + getOrderDetailsResponse?.employee?.name)

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
                            getOrderDetailsResponse?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:a")
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
                            "OrderID:" + getOrderDetailsResponse?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                            "ENTJKOIJH8745"
                        } else {
                            getOrderDetailsResponse?.offlineId
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
                                "Employee:" + getOrderDetailsResponse?.employee?.name
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
                                    getOrderDetailsResponse?.createdAt.toString()
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
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:a")
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


            val dineInList = dineInTableAdapter.getList()
            for (i in 0 until dineInList.size) {
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
                    if (dineInList[i].customer == null) {
                        builder.addText(dineInList[i].title)
                    } else {
                        builder.addText(
                            dineInList[i].customer?.first_name + " " +
                                    if (dineInList[i].customer?.last_name != null) {
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

            if (getOrderDetailsResponse?.totalDiscount != null) {
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

                        if (getOrderDetailsResponse?.totalDiscount == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(0.0)
                        } else {
                            getOrderDetailsResponse?.totalDiscount?.let {
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


            if (viewModel.totalTaxAmount != null) {
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
                        "$" + MethodUtils.roundOffAmountString(finalTaxAmt),
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

            if (getOrderDetailsResponse?.totalTips != 0.0) {

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
                        "$" + getOrderDetailsResponse?.totalTips?.let {
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




            if (getOrderDetailsResponse?.cash_discount_or_surcharge != null) {
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
                MethodUtils.roundOffAmountDouble(subTotalWT + serviceCharge + finalTaxAmt)



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
                if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {

                    builder.addText(
                        padLine(
                            "Change Amount",
                            "$" + MethodUtils.roundOffAmountString(
                                (getOrderDetailsResponse?.payments?.get(
                                    0
                                )?.amount!! - getOrderDetailsResponse?.totalAmount!!)
                            ),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }

            if (getOrderDetailsResponse?.totalTips == 0.0) {
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
                        if (viewModel.totalDiscountAmount != 0.0) {
                            (subTotalWT + serviceCharge + viewModel.totalTaxAmount - getOrderDetailsResponse?.totalDiscount!!.toDouble())
                        } else {
                            (subTotalWT + serviceCharge + viewModel.totalTaxAmount - getOrderDetailsResponse?.totalDiscount!!.toDouble())
                        },
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)
            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
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
                        getOrderDetailsResponse?.payments?.get(0)?.transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (getOrderDetailsResponse?.payments?.isNotEmpty() == true) {
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
                        getOrderDetailsResponse?.payments?.get(0)?.paymentType,
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


            if (getOrderDetailsResponse?.note != null && getOrderDetailsResponse?.note != "" && customerSettingModel.showOrderNote) {

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

                builder.addText(getOrderDetailsResponse?.note)
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(getOrderDetailsResponse?.digitalReceiptUrl.toString())

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

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                tipsList = it
            }


        })
    }

    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?

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


        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = it.data
                        /* for (i in 0 until kitchenPrinterList.size) {

                             initKitchenPrinter(kitchenPrinterList.get(i), Constants.KITCHEN)
                         }*/
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

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: TbItem

    ) {
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
//                printer.setStatusChangeEventCallback(this)
//                printer.setBatteryStatusChangeEventCallback(this)
            }

            val enabled = Print.TRUE

            try {

                printer?.openPrinter(
                    if (data.printer_type == Constants.BLUETOOTH) {
                        Print.DEVTYPE_BLUETOOTH
                    } else {
                        Print.DEVTYPE_TCP
                    },
                    data.ipAddress,
                    enabled,
                    1000
                )
                // printer?.setStatusChangeEventCallback(this)

            } catch (e: Exception) {
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)

                generateKitchenReceipt(data, type, item)

            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: TbItem
    ) {
        var builder: Builder? = null
        try {

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

                addBuilderText(builder, getOrderDetailsResponse?.orderType.toString())
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
                    "OrderID:" + getOrderDetailsResponse?.id,
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
                    "ReceiptID:" + if (getOrderDetailsResponse?.offlineId?.isEmpty() == true) {
                        "ENTJKOIJH8745"
                    } else {
                        getOrderDetailsResponse?.offlineId
                    },
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
                        "Employee:" + getOrderDetailsResponse?.employee?.name, "",
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
                    Constants.getReceiptFormatDateFromUTCServer(getOrderDetailsResponse?.createdAt.toString()),
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


            addOrdersForKitchenDineIn(builder, item)

            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                builder.addText(getOrderDetailsResponse?.note.toString())
            }


            /*if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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
                    *//* builder.addTextLineSpace(30)
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
                 builder.addText(receiptModel?.order?.customer?.email)*//*

                    if (kitchenSettingModel.showCustomerAddress) {
                        if (getOrderDetailsResponse?.customer?.addresses?.isNotEmpty() == true) {

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

                            builder.addText(getOrderDetailsResponse?.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }*/

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

    private fun observeUnMergeTable() {
        viewModel.unMergeStatusUpdate.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), status.toString()
                ) { _, _ ->
                    findNavController().navigate(R.id.action_dineInOrderTable_to_dashboardCategoryNew)
                }


            }
        })
    }

    private fun observeQueueCreated() {
        viewModel.queueCreateSuccess.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->

            }

        })
    }

    private fun observeFireAll() {
        viewModel.fireAllStatus.observe(viewLifecycleOwner, { event ->
            Log.e(TAG, "FireAllStatusObserved")

            var orderItemsAttributes: ArrayList<OrderItemsAttribute> = arrayListOf()
            getOrderDetailsResponse?.orderItems?.forEach {
                val orderItem = OrderItemsAttribute()
                orderItem.category_id = it.categoryId
                orderItem.discountAmount = it.discountAmount
                orderItem.discountId = it.discountId
                orderItem.discountType = it.discountType
                orderItem.employeeId = it.employeeId
                orderItem.id = it.id
                orderItem.isPaid = it.isPaid
                orderItem.itemId = it.itemId
                orderItem.itemName = it.itemName
                orderItem.note = it.note
                orderItem.orderId = it.orderId
                orderItem.note = it.note
                var taxList: ArrayList<OrderItemTaxesAttribute> = arrayListOf()
                it.orderItemTaxes.forEach { tax ->
                    var taxModel = OrderItemTaxesAttribute()
                    taxModel.taxType = tax.taxType
                    taxModel.orderId = tax.orderId
                    taxModel.name = tax.name
                    taxModel.taxTotalAmount = tax.amount ?: 0.0
                    taxModel.id = tax.id
                    taxModel.isDefault = tax.isDefault
                    taxModel.orderItemId = tax.orderItemId
                    taxModel.rate = tax.rate
                    taxModel.taxId = tax.taxId

                    taxList.add(taxModel)

                }
                orderItem.orderItemTaxesAttributes = taxList

                var listModifiers: ArrayList<OrderItemModifierAttribute> = arrayListOf()
                it.orderItemModifiers.forEach {
                    var modifierModel = OrderItemModifierAttribute()
                    modifierModel.id = it.id
                    modifierModel.order_item_id = it.orderItemId
                    modifierModel.orderId = it.orderId
                    modifierModel.name = it.name
                    modifierModel.quantity = it.quantity
                    modifierModel.modifier_set_id = it.modifierId?.toInt() ?: 0
                    modifierModel.price = it.price

                    listModifiers.add(modifierModel)


                }
                orderItem.orderItemModifiersAttributes = listModifiers

                orderItemsAttributes.add(orderItem)


            }

            var orderRequest = OrderAttributeRequestModel()

            orderRequest.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 0)
            orderRequest.totalAmount = getOrderDetailsResponse?.totalAmount ?: 0.0

            orderRequest.offlineId = getOrderDetailsResponse?.offlineId ?: randomOfflineId()
            orderRequest.id = getOrderDetailsResponse?.id ?: 0

            val createQueueRequest = CreateQueuePrinterRequestModel(
                location_id = prefProvider.getValueInt(LOCATION_ID, 0),
                order_type = "Dine In",
                printer_id = listOf(),
                order_item_attributes = orderItemsAttributes,
                order_data = orderRequest,
                terminal_id = prefProvider.getValueInt(TERMINAL_ID, 0)


            )
            viewModel.createQueuePrinter(createQueueRequest)


        })
    }

    private fun singleItemFireObserver() {

        viewModel.fireSingleStatus.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {
                var itemList: ArrayList<OrderItemsAttribute> = arrayListOf()
                val itemModel = OrderItemsAttribute()
                itemModel.category_id = it.categoryId
                itemModel.discountAmount = it.discountPrice
                itemModel.discountId = it.discountId
                itemModel.discountType = it.discountType
                itemModel.itemName = it.name
                itemModel.quantity = it.itemQuantity
                var listModifiers: ArrayList<OrderItemModifierAttribute> = arrayListOf()
                it.modifiers.forEach {
                    var modifierModel = OrderItemModifierAttribute()
                    modifierModel.price = it.price
                    modifierModel.id = it.id
                    modifierModel.name = it.name
                    modifierModel.totalPrice = it.price
                    modifierModel.quantity = it.itemQuantity

                    listModifiers.add(modifierModel)
                }

                itemModel.orderItemModifiersAttributes = listModifiers


                var orderRequest = OrderAttributeRequestModel()

                orderRequest.orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 0)
                orderRequest.totalAmount = getOrderDetailsResponse?.totalAmount ?: 0.0

                orderRequest.offlineId = getOrderDetailsResponse?.offlineId ?: randomOfflineId()
                orderRequest.id = getOrderDetailsResponse?.id ?: 0

                val createQueueRequest = CreateQueuePrinterRequestModel(
                    location_id = prefProvider.getValueInt(LOCATION_ID, 0),
                    order_type = "Dine In",
                    printer_id = listOf(),
                    order_item_attributes = arrayListOf(),
                    order_data = orderRequest,
                    terminal_id = prefProvider.getValueInt(TERMINAL_ID, 0)


                )
                viewModel.createQueuePrinter(createQueueRequest)
            }


        })

    }

}


