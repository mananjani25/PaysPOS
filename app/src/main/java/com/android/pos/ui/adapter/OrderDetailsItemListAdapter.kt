package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.ViewOrderItemListBinding
import com.android.pos.databinding.ViewTaxItemBinding
import com.android.pos.ui.fragments.settings.tax.TaxListViewModel


class OrderDetailsItemListAdapter :
    RecyclerView.Adapter<OrderDetailsItemListAdapter.MyViewHolder>() {

    var taxList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderDetailsItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderItemListBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderDetailsItemListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.taxItemBinding

        val context = itemBinding.root.context
        itemBinding.tvItemName.text = taxList[position].itemName
        itemBinding.tvQuantity.text = "x" + taxList[position].quantity
        itemBinding.tvRate.text = context.getString(R.string.symbole) + " " + String.format(
            context.getString(R.string.format),
            taxList[position].price
        )

        val modifierNames = taxList[position].orderItemModifiers.map {
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

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = taxList.size

    fun addOrderDetailsItems(taxList: List<GetOrderDetailsResponse.Data.OrderItem>) {

        this.taxList.apply {
            clear()
            addAll(taxList)
        }
        notifyDataSetChanged()
    }


    inner class MyViewHolder(val taxItemBinding: ViewOrderItemListBinding) :
        RecyclerView.ViewHolder(taxItemBinding.root) {
    }
}