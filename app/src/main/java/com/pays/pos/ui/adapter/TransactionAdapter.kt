package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTransactionListResponse
import com.pays.pos.data.remote.Constants.DEFAULT_ORDER
import com.pays.pos.data.remote.Constants.GIFT_CARD
import com.pays.pos.data.remote.Constants.GIFT_CARD_AMOUNT_TAB
import com.pays.pos.data.remote.Constants.INVOICE
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.databinding.ViewPaginationBinding
import com.pays.pos.databinding.ViewTransactionItemBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.transactions.TransactionViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TimeFormatUtils.convertCurrentDate
import com.pays.pos.utils.TimeFormatUtils.convertCurrentTime
import com.pays.pos.utils.callback.ItemCallback
import java.util.Locale

class TransactionAdapter(val viewModel: TransactionViewModel, val prefProvider: PrefProvider) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    var employeeTimeSheet = ArrayList<GetTransactionListResponse.Data.Payment>()
    private var filterList = ArrayList<GetTransactionListResponse.Data.Payment>()
    private val TYPE_FOOTER = 1
    private val TYPE_ITEM = 2
    lateinit var context: Context


    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    private var showLoader = false

    fun showLoading(status: Boolean) {
        showLoader = status
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            val binding = ViewTransactionItemBinding.inflate(inflater, parent, false)
            MyViewHolder(binding)
        } else {
            val binding = ViewPaginationBinding.inflate(inflater, parent, false)
            FooterViewHolder(binding)
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            val itemBinding = holder.discountItemBinding
            itemBinding.itemSheetModel = filterList[position]
            itemBinding.viewModel = viewModel
             context = itemBinding.root.context

            val model = filterList[position]

            itemBinding.tvDate.text = convertCurrentDate(filterList[position].createdAt, context)
            itemBinding.tvTime.text = convertCurrentTime(
                filterList[position].createdAt, context
            )

/*
            itemBinding.tvDate.text =
                convertCurrentDate(filterList[position].createdAt, context) + "\n" + convertCurrentTime(
                    filterList[position].createdAt, context
                )
*/
            if (model.paymentType == "Card") {
                if(model.cardNumber.isNotEmpty()){
                    if(model.cardNumber.length == 4){
                        itemBinding.tvPaymentType.text = model.paymentType + "(${model.cardNumber})"
                    } else if (model.cardNumber.length > 4){
                        itemBinding.tvPaymentType.text = model.paymentType + "(${model.cardNumber.substring(model.cardNumber.length - 4)})"
                    } else {
                        itemBinding.tvPaymentType.text = model.paymentType
                    }
                }else{
                    itemBinding.tvPaymentType.text = model.paymentType
                }
                itemBinding.tvPaymentType.setTextColor(itemBinding.root.resources.getColor(R.color.btnColor))
            } else {
                itemBinding.tvPaymentType.text = model.paymentType
                itemBinding.tvPaymentType.setTextColor(itemBinding.root.resources.getColor(R.color.txtColor))
            }
            try {
                if (model.orderDetails != null && model.orderDetails.orderTypeName != null) {
                    itemBinding.tvOrderType?.text = model.orderDetails.orderTypeName.toString()
                } else {
                    if(model.payableType == GIFT_CARD || model.payableType == GIFT_CARD_AMOUNT_TAB || model.payableType == INVOICE ) {
                        itemBinding.tvOrderType?.text = DEFAULT_ORDER
                    } else {
                        itemBinding.tvOrderType?.text = "-"
                    }
                }
            } catch (e: Exception) {
                if(model.payableType == GIFT_CARD || model.payableType == GIFT_CARD_AMOUNT_TAB || model.payableType == INVOICE) {
                    itemBinding.tvOrderType?.text = DEFAULT_ORDER
                } else {
                    itemBinding.tvOrderType?.text = "-"
                }
            }

            itemBinding.txtCustomerName.text = (model.customer?.firstName
                ?: "") + " " + (model.customer?.lastName ?: "")

            itemBinding.txtTeamName?.text = model.employeeName

            if (model.refundedAmount != 0.0) {
                itemBinding.txtTip.isEnabled = false
                itemBinding.tvRefundedAmount.visibility = View.VISIBLE
                itemBinding.tvRefundedAmount.text =
                    "(Refunded " + context.getString(R.string.symbole) + String.format(
                        context.getString(R.string.format),
                        model.refundedAmount
                    ) + ")"
            }

            if(model.refundedAmount == 0.0) {
                itemBinding.tvRefundedAmount.visibility = View.GONE
            }

            try {
                if(model.orderDetails.paymentStatus == "Cancelled"){
                    itemBinding.txtTip.isEnabled = false
                    itemBinding.tvRefundedAmount.visibility = View.GONE
                    itemBinding.tvOrderCancelled.visibility = View.VISIBLE
                } else {
                    itemBinding.txtTip.isEnabled = true
                    itemBinding.tvOrderCancelled.visibility = View.GONE
                }
            }catch (e:Exception) {

                itemBinding.txtTip.isEnabled = true
                itemBinding.tvOrderCancelled.visibility = View.GONE
              //  itemBinding.tvRefundedAmount.visibility = View.GONE
            }

//            if (model.transactionId.isNotEmpty()) {
//                itemBinding.txtTransactionId.visibility = View.VISIBLE
//            } else {
//                itemBinding.txtTransactionId.visibility = View.GONE
//            }


            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                if (model.custom_order_id!= null) {
                    if (model.custom_order_id != 0) {
                        itemBinding.txtTransactionId.text = model.custom_order_id.toString()
                    } else {
                        if(model.payableType == GIFT_CARD || model.payableType == GIFT_CARD_AMOUNT_TAB || model.payableType == INVOICE) {
                            itemBinding.txtTransactionId.text = model.payableId.toString()
                        } else {
                            itemBinding.txtTransactionId.text = "-"
                        }
                    }
                } else {
                    if(model.payableType == GIFT_CARD || model.payableType == GIFT_CARD_AMOUNT_TAB || model.payableType == INVOICE)  {
                        itemBinding.txtTransactionId.text = model.payableId.toString()
                    } else {
                        itemBinding.txtTransactionId.text = "-"
                    }
                }
            } else {
                if (model.orderId != null) {
                    if (model.orderId != 0) {
                        itemBinding.txtTransactionId.text = model.orderId.toString()
                    } else {
                        itemBinding.txtTransactionId.text = "-"
                    }
                } else {
                    itemBinding.txtTransactionId.text = "-"
                }
            }


            if ( model.payableType == GIFT_CARD_AMOUNT_TAB  || model.payableType == GIFT_CARD){
                itemBinding.txtTransactionId.text = model.giftCardId.toString()
            }

            if (model.payableType == GIFT_CARD_AMOUNT_TAB  || model.payableType == GIFT_CARD) {
                if(model.offlineId != null && model.offlineId.isNotEmpty()) {
                    itemBinding.txtReceiptId.text = model.offlineId
                }
            }else{
                if(model.orderDetails.receiptId != null && model.orderDetails.receiptId.isNotEmpty()){
                    itemBinding.txtReceiptId.text = model.orderDetails.receiptId
                }
            }

            itemBinding.executePendingBindings()

            itemBinding.txtTip.setOnClickListener {

//                (filterList[position].paymentType == "Card" && filterList[position].tips > 0) ||
                if (filterList[position].paymentType == "External" || (filterList[position].tips > 0.0 && ( MethodUtils.roundOffAmountDouble(filterList[position].refundedAmount + filterList[position].tips)) ==  MethodUtils.roundOffAmountDouble(filterList[position].totalAmount))) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        context,
                        "Tip cannot be adjusted for this transaction."
                    ) { _, _ ->
                    }
                } else
                    mCallback?.onItemClickListener(it, position)
            }
        }
    }

    override fun getItemCount() = filterList.size

    fun addAll(employeeTimeSheets: List<GetTransactionListResponse.Data.Payment>) {

        LogUtil.logE("teamTimesheetList", employeeTimeSheets.size.toString())
        employeeTimeSheet.addAll(employeeTimeSheets)
        filterList = employeeTimeSheet
        notifyDataSetChanged()
    }


    inner class MyViewHolder(val discountItemBinding: ViewTransactionItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

    }

    inner class FooterViewHolder(paginationBinding: ViewPaginationBinding) :
        RecyclerView.ViewHolder(paginationBinding.root) {

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    employeeTimeSheet
                } else {
                    val fList = ArrayList<GetTransactionListResponse.Data.Payment>()

                    for (it in employeeTimeSheet) {
                        if ((if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false))
                                it.custom_order_id.toString().lowercase(Locale.getDefault()) else
                                it.orderId.toString().lowercase(Locale.getDefault()))
                                .contains(charString.lowercase(Locale.getDefault())) ||
                            (it.customer != null && it.customer.firstName != null && it.customer.firstName.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))) ||
                            (it.customer != null && it.customer.lastName != null && it.customer.lastName.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))) ||
                            (it.customer != null && it.customer.firstName != null && it.customer.lastName != null && (it.customer.firstName.lowercase(
                                Locale.getDefault()) + " " + it.customer.lastName.lowercase(Locale.getDefault()))
                                .contains(charString.lowercase(Locale.getDefault()))) ||
                            it.employeeName != null && it.employeeName != null && it.employeeName.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault())) ||
                            (it.orderDetails != null && it.orderDetails.receiptId != null && it.orderDetails.receiptId.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))) ||
                            String.format(context.getString(R.string.format), it.totalAmount)
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(it)
                        }
                    }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList =
                        results.values as ArrayList<GetTransactionListResponse.Data.Payment>
                }

                notifyDataSetChanged()

            }
        }
    }

    fun getItem(pos: Int): GetTransactionListResponse.Data.Payment? {
        return if (pos in 0 until filterList.size) {
            filterList[pos]
        } else {
            Log.e("TransactionAdapter", "Invalid index: $pos, list size: ${filterList.size}")
            null // Prevents crash
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /*override fun getItemViewType(position: Int): Int {
        return position
    }*/

    override fun getItemViewType(position: Int): Int {
        return if (showLoader) {
            if (position == filterList.size - 1) TYPE_FOOTER else TYPE_ITEM
        } else {
            TYPE_ITEM
        }
    }

    fun clear() {
        employeeTimeSheet.clear()
        filterList.clear()
    }


    fun updateTip(selectedPos: Int, amountTip: Double) {
        val singleTransaction = getItem(selectedPos)
        singleTransaction?.let { singleTransactionValue ->
            val amountTotal = singleTransactionValue.amount + amountTip
            singleTransactionValue.tips = amountTip
            singleTransactionValue.totalAmount = amountTotal
            notifyItemChanged(selectedPos)
        } ?: Log.e("TransactionAdapter", "updateTip() failed: No transaction found at index $selectedPos")
    }


}