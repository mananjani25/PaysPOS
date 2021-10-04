package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.CashLogResponse
import com.android.pos.databinding.ViewCashLogBinding
import com.android.pos.utils.TimeFormatUtils

class CashLogAdapter :
    RecyclerView.Adapter<CashLogAdapter.MyViewHolder>() {

    var orderList = ArrayList<CashLogResponse.Data.Cashe>()

    inner class MyViewHolder(private val binding: ViewCashLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: CashLogResponse.Data.Cashe) {
            binding.viewModel = item
            binding.executePendingBindings()

            binding.txtDateTime.text =
                TimeFormatUtils.convertCurrentDate(item.createdAt) + " " + TimeFormatUtils.convertCurrentTime(
                    item.createdAt
                )

            if (item.event.equals("IN", ignoreCase = true)) {
                binding.txtEvent.text = "Cash IN"
            } else {
                binding.txtEvent.text = "Cash OUT"
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCashLogBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(orderList.get(position))
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    fun add(orders: List<CashLogResponse.Data.Cashe>) {
        orderList = orders as ArrayList<CashLogResponse.Data.Cashe>
        notifyDataSetChanged()
    }

}