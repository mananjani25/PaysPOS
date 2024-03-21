package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.databinding.ViewOrderItemListBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TAG
import com.pays.pos.utils.extensions.gone
import com.google.gson.Gson


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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: OrderDetailsItemListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.taxItemBinding

        val context = itemBinding.root.context
        itemBinding.tvItemName.text = taxList[position].itemName
        itemBinding.tvQuantity.text =taxList[position].quantity.toString()


        var totalPrice = taxList[position].price * taxList[position].quantity

        if (taxList[position].note.isEmpty()) {
            itemBinding.txtNote.visibility = View.GONE
        } else {
            itemBinding.txtNote.visibility = View.VISIBLE
            itemBinding.txtNote.text = "Note: " + taxList[position].note
        }

        itemBinding.tvTotal.text = MethodUtils.roundOffAmount(totalPrice)

        itemBinding.tvRate.text = MethodUtils.roundOffAmount(taxList[position].price)
        if (taxList[position].orderItemModifiers.isNotEmpty()) {
            itemBinding.rvModifiers.visibility = View.VISIBLE
            val adapter = OrderDetailModifierListAdapter()
            itemBinding.rvModifiers.adapter = adapter
//            LogUtil.logE(TAG, "dineinMod  ${Gson().toJson(taxList[position].orderItemModifiers)}")
            adapter.addAll(taxList[position].orderItemModifiers)
        } else {
            itemBinding.rvModifiers.gone()
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