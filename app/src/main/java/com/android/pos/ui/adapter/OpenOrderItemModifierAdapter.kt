package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewOpenOrderItemModifiersBinding
import com.android.pos.databinding.ViewOpenOrderItemsBinding

class OpenOrderItemModifierAdapter :
    RecyclerView.Adapter<OpenOrderItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>()

    inner class MyViewHolder(private val binding: ViewOpenOrderItemModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier) {
            binding.model = item
            binding.executePendingBindings()

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOpenOrderItemModifiersBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    fun addAll(modifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }

}