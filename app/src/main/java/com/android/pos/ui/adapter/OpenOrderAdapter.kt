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

class OpenOrderAdapter :
    RecyclerView.Adapter<OpenOrderAdapter.MyViewHolder>() {

    var orderList = ArrayList<OpenOrderResponse.Data.Order>()
    var isShown = false

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

        }

        init {
            binding.root.setOnClickListener {

                if (isShown) {
                    isShown = false
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
                } else {
                    isShown = true
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

                    val model = orderList[bindingAdapterPosition]

                    binding.txtDeliveryDate.text =
                        TimeFormatUtils.convertCurrentDate(model.createdAt) + "\n" + TimeFormatUtils.convertCurrentTime(
                            model.createdAt
                        )


                    binding.txtStatus.text = model.paymentStatus
                    binding.txtCustomerNameDetails.text =
                        (model.customer?.firstName ?: "") + " " + (model.customer?.lastName ?: "")

                    binding.txtEmail.text = model.customer?.email ?: ""
                    binding.txtPhoneNo.text = ""
                    binding.txtAddress.text = ""
                }
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

}