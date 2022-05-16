package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.OnlineOrderResponseModel
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewonlineorderlayoutBinding
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.OrderCallBack
import java.util.ArrayList


class OnlineOrderAdapter(val context: Context) : RecyclerView.Adapter<OnlineOrderAdapter.MyViewHolder>() {
    var orderList = ArrayList<OnlineOrderResponseModel.Data>()
    var filterList = ArrayList<OnlineOrderResponseModel.Data>()
    private var mCallback: OrderCallBack? = null
    fun setCallback(callback: OrderCallBack) {
        mCallback = callback
    }
    inner class MyViewHolder(private val binding: ViewonlineorderlayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {


        @SuppressLint("SetTextI18n")
        fun bind(item: OnlineOrderResponseModel.Data) {
            binding.viewModel = item
            binding.executePendingBindings()
            if (item.createdAt.isNotEmpty()) {
                binding.tvDate.text =
                    TimeFormatUtils.convertCurrentDate(
                        item.createdAt,
                        context
                    )
                binding.tvtime.text =
                    TimeFormatUtils.convertCurrentTime(
                        item.createdAt,
                        context
                    )

            }

            binding.txtCustomerName.text =
                (item.customer?.firstName ?: "") + " " + (item.customer?.lastName ?: "")

        }

        init {

        }
    }


    fun add(orders: List<OnlineOrderResponseModel.Data>) {
        this.orderList = orders as ArrayList<OnlineOrderResponseModel.Data>
        this.filterList = orders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewonlineorderlayoutBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

}