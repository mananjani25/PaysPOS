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
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.requestModel.PaymentAttributes
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.FragmentDineInOrderTableBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInTableAdapter
import com.android.pos.ui.fragments.inventory.Modifiers
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DineInOrderTable : Fragment(), DineInTableAdapter.DineInTableListner {
    private lateinit var binding: FragmentDineInOrderTableBinding
    private var cartList: CartModel? = null
    private var dineInData: CreateOrderResponse.Data? = null
    private var orderId: Int? = null
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
    private var popupWindow: PopupWindow? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    var dragFrom = -1
    var dragTo = -1

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

        navigateDineInOrder()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()

        dineInTableAdapter = DineInTableAdapter()
        binding.rvItemList.adapter = dineInTableAdapter
        dineInTableAdapter.setListner(this)

        if (arguments?.getBoolean("isFromFloor") == true || arguments?.getBoolean("isGuestPaid") == true) {
            floorPlanModel = arguments?.getParcelable("floorPlan")
            Log.e(TAG, "GetOrderId   ${floorPlanModel?.currentOrderDetails?.orderId}")
            orderId = floorPlanModel?.currentOrderDetails?.orderId
            floorPlanModel?.currentOrderDetails?.orderId?.let { viewModel.apiCallOrderDetails(it) }
            binding.txtTitle.setText("" + floorPlanModel?.tableName)


        } else {
            cartList = arguments?.getParcelable("cartList")
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

                binding.txtTotalAmount.setText("${MethodUtils.roundOffAmount(totalPrice)}")

            }

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
        binding.btnSendOrder.setOnClickListener {
            var guestAttribute = dineInData?.order?.guestAttributes
            Log.e(TAG, "guestAttribute:  ${Gson().toJson(guestAttribute)}")
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
                        Log.e(TAG, "OrderItemId ${it.orderItemId}")
                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }


                Log.e(TAG, "orderITemIDs;  ${Gson().toJson(ids)}")
                var idStr = Gson().toJson(ids.toTypedArray())
                Log.e(TAG, "idStr:   $idStr")

                viewModel.fireItemToKitchen(dineInData?.order?.id!!, true, idStr)

            } else {
                Log.e(TAG, "Else")
                var guestAttribute = dineInTableAdapter.getList()
                val ids: MutableList<Int> = ArrayList()

                for (i in 0 until guestAttribute.size) {
                    guestAttribute[i].items.forEach { it ->
                        Log.e(TAG, "OrderItemId ${it.orderItemId}")
                        ids.add(it.orderItemId!!)
                        //ids.toMutableList().add(it.orderItemId!!)
                    }

                }


                Log.e(TAG, "orderITemIDs;  ${Gson().toJson(ids)}")
                var idStr = Gson().toJson(ids.toTypedArray())
                Log.e(TAG, "idStr:   $idStr")

                viewModel.fireItemToKitchen(orderId!!, true, idStr)
            }

        }

        binding.btnPay.setOnClickListener {


            val bundle = Bundle()
            bundle.putDouble("totalPrice", totalPrice)
            bundle.putDouble("subTotalPrice", totalPrice)
            bundle.putDouble("totalTax", totalTax)
            bundle.putDouble("totalDiscount", totalDiscount)
            bundle.putDouble("totalServiceCharge", totalServiceCharge)
            bundle.putString("future_delivery_date", future_delivery_date)
            bundle.putString("future_delivery_time", future_delivery_time)
            bundle.putBoolean("update", false)

            bundle.putParcelable("cartList", cartList)

            findNavController().navigate(R.id.action_dineInOrderTable_to_paymentFragment, bundle)

        }

        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }

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
        txtTotalAmount.text = binding.txtTotalAmount.text.toString()
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
            }
        })


    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    override fun onGuestPay(dineInModel: DineInModel, position: Int) {
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

            totalPaid = MethodUtils.roundOffAmountDouble(totalPaid)
            wholeTableAmt = MethodUtils.roundOffAmountDouble(wholeTableAmt)
            total = MethodUtils.roundOffAmountDouble(total)

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
            /* val list = dineInTableAdapter.getList()
             list[position].isPaid = true
             dineInTableAdapter.setList(list.toCollection(arrayListOf()))
 */

        }

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


        var idStr = Gson().toJson(ids.toTypedArray())
        Log.e(TAG, "orderITemIDs;  ${idStr}")

        viewModel.fireItemToKitchen(orderId!!, true, idStr)

    }

    override fun onWholeTableToKitchen(ids: String) {
        Log.e(TAG, "idsids:  ${ids}")
        viewModel.fireItemToKitchen(orderId!!, true, ids)
    }

    private fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        Log.e("timeStampFinal", timeStampFinal)

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
                                    Log.e(TAG, "isFired  ${it.isFired}")



                                    listTbItem.add(item)

                                }
                            }
                        }

                        model.items = listTbItem
                        model.isPaid = listTbItem.get(0).isPaid

                        model.title = baseResponse.guestAttributes.get(i).name
                        model.id = baseResponse.guestAttributes[i].id

                        list.add(model)


                        /*for (j in 0 until baseResponse.guestAttributes.get(i).guestItemAttributes.size) {
                            var guestItem = baseResponse.guestAttributes.get(i).guestItemAttributes

                            val item = TbItem()
                            item.price = guestItem.get(j).amount.toDouble()
                            item.orderItemId = guestItem.get(j).orderItemId
                            item.isFired = guestItem.get(j).is_fired
                            item.isPaid = guestItem.get(j).isPaid
                            item


                        }
    */
                    }

                    var paidAmt = 0.0
                    if (list.isNotEmpty()) {


                        baseResponse.guestAttributes.get(0).guestItemAttributes.forEach {
                            wholeTableAmt += it.amount * it.quantity
                        }

                        Log.e(TAG, "wholeTableAmt:  ${wholeTableAmt}")

                        Log.e(TAG, "Repon: ${baseResponse.guestAttributes.size - 1}")
                        var dividedAmt: Double =
                            wholeTableAmt / (baseResponse.guestAttributes.size - 1)

                        Log.e(TAG, "dividedAmt:   ${dividedAmt}")
                        list.get(0).guestDividedAmt = dividedAmt


                        Log.e(TAG, "listData:  ${Gson().toJson(list)}")
                        dineInTableAdapter.setList(list)
                        cartList = getCartModel(list)


                        totalAmount = 0.0
                        for (i in 0 until list.size) {


                            list.get(i).items.forEach { it ->
                                if (list.get(i).isPaid) {
                                    paidAmt += it.itemQuantity * it.price

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

                            if (list.get(i).items.get(0).isPaid) {
                                paidAmt += dividedAmt
                            }

                        }

                    }

                    Log.e(TAG, "totalAmount  ${totalAmount}")
                    Log.e(TAG, "totalAmountpaidAmt  ${paidAmt}")

                    totalAmount -= paidAmt

                    if (totalAmount < 0) {
                        totalAmount = 0.0
                    }


                    binding.txtTotalAmount.setText("${MethodUtils.roundOffAmount(totalAmount)}")
                    totalPrice = MethodUtils.roundOffAmountDouble(totalAmount)



                    Log.e(TAG, "GetbaseResponse:  ${Gson().toJson(baseResponse)}")


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



        Log.e(TAG, "listItem:  ${Gson().toJson(listItem)}")

        Log.e(TAG, "CartModel:  ${Gson().toJson(model)}")
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


}