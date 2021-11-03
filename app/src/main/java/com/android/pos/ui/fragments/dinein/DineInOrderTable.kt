package com.android.pos.ui.fragments.dinein

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
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
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_UPDATE
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.FragmentDineInOrderTableBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInTableAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.ArrayTable
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DineInOrderTable : Fragment(), DineInTableAdapter.DineInTableListner {
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
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var paymentAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    var totalAmount = 0.0
    var totalAmtnew: Double = 0.0
    private var popupWindow: PopupWindow? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    var dragFrom = -1
    var dragTo = -1
    var notPayAnyAmount: Boolean = false
    var serviceChargeList: ArrayList<TbServiceCharge> = arrayListOf()


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
        observeShowProgress()
        setupSnackbar()
        observeServiceCharge()

        navigateDineInOrder()
        return binding.root
    }

    private fun observeServiceCharge() {
        viewModel.getServiceChargeList.observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                serviceChargeList = it.toCollection(arrayListOf())

            }

        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()

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
                binding.txtTitle.setText("" + floorPlanModel?.tableName)
            }


        } else {

            orderId = arguments?.getInt("orderId")
            orderId?.let { viewModel.apiCallOrderDetails(it) }
            binding.txtTitle.setText("Order Details")


            /*cartList = arguments?.getParcelable("cartList")
            dineInData = arguments?.getParcelable("dineInList")

            //totalPrice = requireArguments().getDouble("totalPrice")

            Log.e(TAG, "getDineIncartList:   ${Gson().toJson(cartList)}")
            orderId = dineInData?.order!!.id

            if (cartList?.dineInList != null) {

                var list = cartList?.dineInList!!.toCollection(arrayListOf())

                val guestAttributes = dineInData!!.order.guestAttributes

                var wholeTableAmt = 0.0
                guestAttributes.get(0).guestItemAttributes.forEach {
                    wholeTableAmt += it.amount * it.quantity
                }

                Log.e(TAG, "wholeTableAmt:  ${wholeTableAmt}")

                var dividedAmt: Double = wholeTableAmt / (guestAttributes.size - 1)

                Log.e(TAG, "dividedAmt:   ${dividedAmt}")
                list.get(0).guestDividedAmt = MethodUtils.roundOffAmountDouble(dividedAmt)
                for (i in 0 until guestAttributes.size) {

                    for (j in 0 until list.size) {
                        if (cartList?.dineInList!![j].title == guestAttributes.get(i).name) {
                            list[j].id = guestAttributes.get(i).id
                        }

                        for (k in 0 until list.get(j).items.size) {
                            guestAttributes.get(i).guestItemAttributes.forEach {
                                if (list.get(j).items.get(k).timeStamp == it.timestamp) {
                                    list.get(j).items.get(k).isPaid = it.isPaid
                                    list.get(j).items.get(k).orderItemId = it.orderItemId
                                }
                            }
                        }


                    }

                }

                dineInTableAdapter.setList(list)

                binding.txtTotalAmountNew.setText("${MethodUtils.roundOffAmount(totalPrice)}")

            }*/

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
                    builder.substring(0, builder.length - 1).toString()
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

            var total = subTotal + totalTax


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


            val bundle = Bundle()
            bundle.putDouble("totalPrice", total)
            bundle.putDouble("subTotalPrice", subTotal)
            bundle.putDouble("totalTax", totalTax)
            bundle.putParcelable("model", model)
            bundle.putParcelable("floorPlan", floorPlanModel)
            bundle.putBoolean("isTotalPayment", true)
            bundle.putDouble("totalServiceCharge", 0.0)
            bundle.putDouble("totalDiscount", 0.0)
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
            orderId?.let { it1 -> bundle.putInt("orderId", it1) }

            prefProvider.setValueboolean(DINE_IN_UPDATE, true)
            prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
            prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
            prefProvider.setValueInt(Constants.DINE_IN_TABLE_ID, 2)
            prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)
            /*   prefProvider.setValue(Constants.ORDER_TYPE, DINE_IN)
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


                viewModel.fireItemToKitchen(orderId!!, true, idStr)
            }

        }

        binding.btnPay.setOnClickListener {


            val bundle = Bundle()
            bundle.putDouble("totalPrice", totalPrice)
            bundle.putDouble("subTotalPrice", subTotalPrice)
            bundle.putDouble("totalTax", totalTax)
            bundle.putDouble("totalDiscount", totalDiscount)
            bundle.putDouble("totalServiceCharge", totalServiceCharge)
            bundle.putString("future_delivery_date", future_delivery_date)
            bundle.putString("future_delivery_time", future_delivery_time)
            bundle.putBoolean("update", false)

            bundle.putParcelable("cartList", cartList)

            findNavController().navigate(R.id.action_dineInOrderTable_to_paymentFragment, bundle)

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

        txtSubTotal.text = "$" + String.format(
            "%.2f",
            subTotalPrice
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            totalServiceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            totalDiscount
        )
        txtTotalAmount.text = binding.txtTotalAmountNew.text.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            totalTax
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

    override fun onGuestPay(dineInModel: DineInModel, position: Int) {

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

        val bundle = Bundle()
        dineInModel.id?.let { bundle.putInt("id", it) }
        bundle.putParcelable("cartList", cartList)
        bundle.putDouble("totalPrice", total)
        bundle.putDouble("subTotalPrice", subTotal)
        bundle.putDouble("totalTax", totalTax)
        bundle.putParcelable("model", model)
        bundle.putParcelable("floorPlan", floorPlanModel)
        orderId?.let { bundle.putInt("orderId", it) }

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

        if ((wholeTableAmt - paidAmount) == subTotal) {
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


        viewModel.fireItemToKitchen(orderId!!, true, idStr)

    }

    override fun onWholeTableToKitchen(ids: String) {

        viewModel.fireItemToKitchen(orderId!!, true, ids)
    }

    override fun singleItemFired(id: String, position: Int) {

        clickedPos = position
        viewModel.fireItemToKitchen(orderId!!, true, id)

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
        viewModel._Basedata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (baseResponse != null) {
                    getOrderDetailsResponse = baseResponse
                    var list: ArrayList<DineInModel> = arrayListOf()

                    var wholeTableAmt = 0.0

                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val model = DineInModel()

                        var listTbItem: ArrayList<TbItem> = arrayListOf()

                        var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                        for (j in 0 until guestItem.size) {

                            baseResponse.orderItems.forEach {
                                if (it.timestamp == guestItem[j].timestamp) {
                                    val item = TbItem()
                                    item.isPaid = it.isPaid
                                    item.discountPrice = it.discountAmount
                                    item.discountId = it.discountId
                                    item.discountType = it.discountType

                                    item.name = it.itemName
                                    item.categoryId = it.categoryId
                                    item.itemId = it.itemId





                                    if (it.orderItemModifiers.isNotEmpty()) {
                                        var modifiers: ArrayList<Modifier> = arrayListOf()
                                        it.orderItemModifiers.forEach {
                                            val model = Modifier()
                                            model.id = it.id
                                            model.itemQuantity = it.quantity
                                            model.name = it.name
                                            model.orderModifierId = it.orderItemId
                                            model.price = it.price

                                            modifiers.add(model)

                                        }
                                        item.modifiers = modifiers

                                    }
                                    item.price = it.price
                                    item.itemQuantity = it.quantity
                                    item.orderItemId = it.id
                                    item.note = it.note
                                    item.isFired = it.isFired




                                    listTbItem.add(item)

                                }
                            }
                        }

                        model.items = listTbItem
                        if (listTbItem.isNotEmpty()) {
                            model.isPaid = listTbItem.get(0).isPaid
                        }

                        model.title = baseResponse.guestAttributes.get(i).name
                        model.id = baseResponse.guestAttributes[i].id

                        list.add(model)


                    }

                    var paidAmt = 0.0

                    //New Drag and drop Code
                    var totalPay = 0.0
                    val dineInList: ArrayList<DineInModel> = arrayListOf()

                    for (i in 0 until baseResponse.guestAttributes.size) {
                        val model = DineInModel()
                        var totalGuestPrice = 0.0
                        var wholeTableAmt = 0.0
                        var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes
                        //model.isPaid = listTbItem.get(0).isPaid

                        model.title = baseResponse.guestAttributes.get(i).name
                        model.isHeader = 0


                        var fullAmt = 0.0
                        var subTotalWT = 0.0

                        for (j in 0 until guestItem.size) {
                            if (baseResponse.orderItems.isNotEmpty()) {
                                baseResponse.orderItems.forEach { it ->
                                    if (it.timestamp == guestItem[j].timestamp) {
                                        totalGuestPrice += it.price * it.quantity
                                        model.isPaid = it.isPaid
                                        fullAmt += it.quantity * it.price
                                        subTotalWT += it.quantity * it.price



                                        if (it.orderItemModifiers.isNotEmpty()) {
                                            it.orderItemModifiers.forEach {
                                                totalGuestPrice += it.price * it.quantity
                                                fullAmt += it.quantity * it.price
                                                subTotalWT += it.quantity * it.price

                                            }
                                        }
                                        if (it.orderItemTaxes.isNotEmpty()) {
                                            it.orderItemTaxes.forEach { tax ->

                                                fullAmt += tax.rate


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

                        var serviceCharge = 0.0
                        serviceChargeList.forEach {
                            if (it.isEnabled) {
                                serviceCharge += (subTotalWT * it.percentage) / 100
                            }
                        }

                        fullAmt += serviceCharge
                        totalPay += fullAmt

                        var dividedAmt = fullAmt / (baseResponse.guestAttributes.size - 1)



                        model.guestDividedAmt = dividedAmt
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
                                                itemIds = arrayListOf()
                                            )
                                        )
                                    }



                                    item.taxes = listTaxes



                                    if (it.orderItemModifiers.isNotEmpty()) {
                                        var modifiers: ArrayList<Modifier> = arrayListOf()
                                        it.orderItemModifiers.forEach {
                                            val model = Modifier()
                                            model.id = it.id
                                            model.itemQuantity = it.quantity
                                            model.name = it.name
                                            model.orderModifierId = it.orderItemId
                                            model.price = it.price

                                            modifiers.add(model)


                                        }
                                        item.modifiers = modifiers

                                    }
                                    item.price = it.price
                                    item.itemQuantity = it.quantity
                                    item.orderItemId = it.id
                                    item.note = it.note
                                    item.isFired = guestItem.get(j).is_fired




                                    itemDineIn.isHeader = 1
                                    itemDineIn.item = item

                                    dineInList.add(itemDineIn)


                                }

                            }


                        }
                    }

                    binding.txtTotalAmountNew.setText(MethodUtils.roundOffAmount(totalPay))

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

                        dineInList.forEach {
                            if (it.isHeader == 1) {
                                it.item?.let { it1 -> viewModel.taxCalculation(it1) }
                            }
                        }


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

                        list.get(0).guestDividedAmt = dividedAmt

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
                }

            } else {

                break
            }
        }
        guestShare += wholeTableAmt / (guestCount - 1)

        for (i in 0 until oldList.size) {
            var model = DineInModel()
            if (oldList.get(i).isHeader == 1) {
                model.item = oldList.get(i).item
                model.totalTableAmt = totalTablePrice


            } else {
                model.title = oldList.get(i).title

                model.guestDividedAmt = guestShare
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

}
