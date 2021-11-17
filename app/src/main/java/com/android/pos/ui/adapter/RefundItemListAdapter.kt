package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.ViewRefundItemBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils

class RefundItemListAdapter(val viewModel: TransactionDetailsViewModel) :
    RecyclerView.Adapter<RefundItemListAdapter.MyViewHolder>() {

    var showItemSubTotal: (() -> Unit)? = null
    var selectedItemList = ArrayList<GetPaymentOrderDetailsResponse.Data.Order.Order_items>()
    var noteList = ArrayList<GetPaymentOrderDetailsResponse.Data.Order.Order_items>()
    var serviceCharge:Double=0.0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RefundItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewRefundItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addItems(
        noteList: List<GetPaymentOrderDetailsResponse.Data.Order.Order_items>,
        serviceCharge:Double
    ) {
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
        fun bind(item: GetPaymentOrderDetailsResponse.Data.Order.Order_items) {
            itemBinding.refundItemListModel = item

            itemBinding.tvItemName.text = item.item_name

            val modifierNames = item.order_item_modifiers.map {
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

            var totalItemPrice:Double = (item.total_price - item.discount_amount).toDouble()


            item.order_item_taxes.forEach { tax ->


                totalTax += tax.tax_total_amount
            }

            item.order_item_modifiers.forEach { modifiers ->
                totalItemPrice += (modifiers.price * modifiers.quantity)
                item.order_item_taxes.forEach { taxes ->
                    totalTax += taxes.tax_total_amount
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

    fun selectedItemList(): ArrayList<GetPaymentOrderDetailsResponse.Data.Order.Order_items> {
        return selectedItemList
    }

}