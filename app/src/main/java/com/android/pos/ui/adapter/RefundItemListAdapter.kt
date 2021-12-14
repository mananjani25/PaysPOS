package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.ViewRefundItemBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils

class RefundItemListAdapter(val viewModel: TransactionDetailsViewModel) :
    RecyclerView.Adapter<RefundItemListAdapter.MyViewHolder>() {

    var showItemSubTotal: (() -> Unit)? = null
    var selectedItemList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()
    var noteList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()
    var serviceCharge: Double = 0.0
    var cashdiscountType: String = ""
    var paymentType: String = ""
    var cash_discount_or_surcharge: Double = 0.0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RefundItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewRefundItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addItems(
        noteList: List<GetOrderDetailsResponse.Data.OrderItem>,
        serviceCharge: Double,
        cash_discount_or_surcharge: Double,
        cashDiscountType: String,
        paymentType: String
    ) {
        this.cash_discount_or_surcharge = cash_discount_or_surcharge
        this.cashdiscountType = cashDiscountType
        this.paymentType = paymentType
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
        this.serviceCharge =
            serviceCharge as Double
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: RefundItemListAdapter.MyViewHolder, position: Int) {

        holder.bind(noteList.get(position))
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    inner class MyViewHolder(val itemBinding: ViewRefundItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: GetOrderDetailsResponse.Data.OrderItem) {
            itemBinding.refundItemListModel = item

            itemBinding.tvItemName.text = item.itemName

            val modifierNames = item.orderItemModifiers.map {
                it.name + " (" + itemBinding.root.context.getString(R.string.symbole) + " " + String.format(
                    itemBinding.root.context.getString(
                        R.string.format
                    ), it.price
                ) + ")"
            }

            if (modifierNames.isEmpty()) {
                itemBinding.tvModifierName.visibility = View.GONE
            } else {
                itemBinding.tvModifierName.visibility = View.VISIBLE
                itemBinding.tvModifierName.text = TextUtils.join(",", modifierNames)
            }


            var totalTax = 0.0
            var totalServiceCharge = 0.0


            var totalItemPrice: Double = (item.totalPrice - item.discountAmount).toDouble()
            if (paymentType == "Cash") {
                if (cashdiscountType == "CashDiscount") {
                    totalItemPrice -= cash_discount_or_surcharge
                }
            } else if (paymentType == "Card") {
                if (cashdiscountType == "SurCharge") {
                    totalItemPrice += cash_discount_or_surcharge
                }
            }

            item.orderItemTaxes.forEach { tax ->
                tax.amount?.let {
                    totalTax += it
                }
            }

            item.orderItemModifiers.forEach { modifiers ->
                totalItemPrice += (modifiers.price * modifiers.quantity)
                item.orderItemTaxes.forEach { taxes ->
                    taxes.taxTotalAmount.let {
                        totalTax += it
                    }
                }

            }

            totalItemPrice += totalTax + serviceCharge
            MethodUtils.setPriceTextView(itemBinding.tvItemPrice, totalItemPrice.toDouble())

            itemBinding.ivCheck.setOnClickListener {
                item.isChecked = !item.isChecked

                if (item.isChecked) {
                    selectedItemList.add(item)
                } else {
                    selectedItemList.remove(item)
                }

                showItemSubTotal?.invoke()
                notifyDataSetChanged()
            }

            itemBinding.executePendingBindings()
        }
    }

    fun selectedItemList(): ArrayList<GetOrderDetailsResponse.Data.OrderItem> {
        return selectedItemList
    }

}