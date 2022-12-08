package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.ViewCartModifierBinding
import com.android.pos.databinding.ViewOrderModifierListBinding
import com.android.pos.utils.MethodUtils

class OrderDetailModifierListAdapter :
    RecyclerView.Adapter<OrderDetailModifierListAdapter.MyViewHolder>() {
    var list = ArrayList<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>()
    private val TAG = "OrderDetailModifierListAdapter"

    inner class MyViewHolder(private val binding: ViewOrderModifierListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier) {
            binding.tvItemName.text = "x${item.modifier_quantity} ${item.name}"
            binding.tvQuantity.setText("x " + item.quantity.toString())
            binding.tvRate.text = MethodUtils.roundOffAmount(item.price)
            var totalPrice = item.price * item.quantity
            binding.tvTotal.text = MethodUtils.roundOffAmount(totalPrice)
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderDetailModifierListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderModifierListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(
        holder: OrderDetailModifierListAdapter.MyViewHolder,
        position: Int
    ) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addAll(modifiers: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }


}