package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewOpenOrderItemBinding
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.OrderCallBack

class OpenOrderAdapter :
    RecyclerView.Adapter<OpenOrderAdapter.MyViewHolder>() {

    var orderList = ArrayList<OpenOrderResponse.Data.Order>()

    private var mCallback: OrderCallBack? = null
    fun setCallback(callback: OrderCallBack) {
        mCallback = callback
    }


    inner class MyViewHolder(private val binding: ViewOpenOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var adapter: OpenOrderItemsAdapter? = null

        @SuppressLint("SetTextI18n")
        fun bind(item: OpenOrderResponse.Data.Order) {
            binding.viewModel = item
            binding.executePendingBindings()

            binding.llShowLayout.visibility = View.GONE

            binding.tvDate.text =
                TimeFormatUtils.convertCurrentDate(item.createdAt) + "\n" + TimeFormatUtils.convertCurrentTime(
                    item.createdAt
                )
            binding.txtCustomerName.text =
                (item.customer?.firstName ?: "") + " " + (item.customer?.lastName ?: "")

            if (item.orderItems.isNotEmpty()) {

                binding.rvOpenOrder.addItemDecoration(
                    DividerItemDecoration(
                        binding.root.context,
                        LinearLayoutManager.VERTICAL
                    )
                )

                adapter = OpenOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter
                adapter!!.addAll(item.orderItems)
            }


            if (item.paymentStatus == "Cancelled") {

                binding.txtCancelOrder.visibility = View.GONE
                binding.txtEditOrder.visibility = View.GONE
                binding.txtPrintReceipt.visibility = View.VISIBLE
                binding.txtPayNow.visibility = View.GONE

            } else {
                binding.txtCancelOrder.visibility = View.VISIBLE
                binding.txtEditOrder.visibility = View.VISIBLE
                binding.txtPrintReceipt.visibility = View.VISIBLE
                binding.txtPayNow.visibility = View.VISIBLE
            }

            if (!item.isCheck) {

                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.white))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.llShowLayout.visibility = View.GONE
                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_arrow_down))

            } else {

                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.black))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.llShowLayout.visibility = View.VISIBLE
                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_arrow_up))
            }
        }

        init {
            binding.root.setOnClickListener {

                val item = orderList[bindingAdapterPosition]

                if (item.isCheck) {
                    item.isCheck = false
                } else {
                    orderList.forEach {
                        it.isCheck = false
                    }
                    item.isCheck = true

                }
                notifyDataSetChanged()

            }

            binding.txtCancelOrder.setOnClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "")
            }

            binding.txtEditOrder.setOnClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "UPDATE")
            }
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

    fun getItem(pos: Int): OpenOrderResponse.Data.Order {

        return orderList[pos]
    }

    fun update(position: Int) {
        orderList.removeAt(position)
        notifyDataSetChanged()
    }

}