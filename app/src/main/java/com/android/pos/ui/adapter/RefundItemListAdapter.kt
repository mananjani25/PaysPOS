package com.android.pos.ui.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.ViewRefundItemBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils

class RefundItemListAdapter(val viewModel: TransactionDetailsViewModel) :
    RecyclerView.Adapter<RefundItemListAdapter.MyViewHolder>() {

    var showItemSubTotal: (() -> Unit)? = null
    var selectedItemList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()
    var noteList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RefundItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewRefundItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addItems(noteList: List<GetOrderDetailsResponse.Data.OrderItem>) {
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
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

            var totalItemPrice = item.totalPrice


            item.orderItemTaxes.forEach { tax ->
                totalTax += tax.taxTotalAmount
            }

            item.orderItemModifiers.forEach { modifiers ->
                totalItemPrice += (modifiers.price * modifiers.quantity)
                modifiers.orderItemTaxes.forEach { taxes ->
                    totalItemPrice += taxes.taxTotalAmount
                }

            }
            totalItemPrice += totalTax
            MethodUtils.setPriceTextView(itemBinding.tvItemPrice, totalItemPrice)

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