package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.CashLogResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewCashLogBinding
import com.pays.pos.databinding.ViewPaginationBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.TimeFormatUtils

class CashLogAdapter(val context: Context?, val prefProvider: PrefProvider) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var orderList = ArrayList<CashLogResponse.Data.Cashe>()

    private val TYPE_FOOTER = 1
    private val TYPE_ITEM = 2

    private var showLoader = false

    fun clearList() {
        orderList.clear()
        orderList = arrayListOf()
        notifyDataSetChanged()
    }

    fun showLoading(status: Boolean) {
        showLoader = status
    }

    inner class MyViewHolder(private val binding: ViewCashLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: CashLogResponse.Data.Cashe) {
            binding.viewModel = item
            binding.executePendingBindings()
            if (item.reason.isNotEmpty()) {
                binding.txtReason.text = item.reason
            } else {
                binding.txtReason.text = "Refund Initiated"
            }


            binding.txtDateTime.text =
                TimeFormatUtils.convertCurrentDate(
                    item.createdAt,
                    context
                )
            binding.txtTime.text = TimeFormatUtils.convertCurrentTime(
                item.createdAt, context
            )


            if (prefProvider.getValueboolean(Constants.ORDER_NUMBER_STARTING_FROM_ONE, false) && !item.reason.contains("Gift card")) {
                binding.txtOrderId.text = item.custom_order_id.toString()
            } else {
                binding.txtOrderId.text = item.orderId.toString()
            }


            if (item.event.equals("IN", ignoreCase = true)) {
                binding.txtEvent.text = "Cash IN"
            } else {
                binding.txtEvent.text = "Cash OUT"
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            val binding = ViewCashLogBinding.inflate(inflater, parent, false)
            MyViewHolder(binding)
        } else {
            val binding = ViewPaginationBinding.inflate(inflater, parent, false)
            FooterViewHolder(binding)
        }

    }

    inner class FooterViewHolder(paginationBinding: ViewPaginationBinding) :
        RecyclerView.ViewHolder(paginationBinding.root) {

    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            holder.bind(orderList.get(position))
        }
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (showLoader) {
            if (position == orderList.size - 1) TYPE_FOOTER else TYPE_ITEM
        } else {
            TYPE_ITEM
        }
    }

    fun add(orders: List<CashLogResponse.Data.Cashe>) {
        orderList.addAll(orders)
        notifyDataSetChanged()
    }

    fun clear(){
        orderList.clear()
    }


}