package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewOpenOrderItemBinding
import com.android.pos.utils.TimeFormatUtils

class OpenOrderAdapter :
    RecyclerView.Adapter<OpenOrderAdapter.MyViewHolder>() {

    var orderList = ArrayList<OpenOrderResponse.Data.Order>()

    inner class MyViewHolder(private val binding: ViewOpenOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: OpenOrderResponse.Data.Order) {
            binding.viewModel = item
            binding.executePendingBindings()

            val model = orderList[bindingAdapterPosition]

            binding.tvDate.text =
                TimeFormatUtils.convertCurrentDate(orderList[bindingAdapterPosition].createdAt) + "\n" + TimeFormatUtils.convertCurrentTime(
                    orderList[bindingAdapterPosition].createdAt
                )
            binding.txtCustomerName.text =
                (model.customer?.firstName ?: "") + " " + (model.customer?.lastName ?: "")

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OpenOrderAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOpenOrderItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OpenOrderAdapter.MyViewHolder, position: Int) {
        holder.bind(orderList.get(position))
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    fun add(orders: List<OpenOrderResponse.Data.Order>) {
        orderList = orders as ArrayList<OpenOrderResponse.Data.Order>
        notifyDataSetChanged()
    }

}