package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.remote.Constants.PRINT_PAID
import com.android.pos.data.remote.Constants.PRINT_UNPAID
import com.android.pos.databinding.ViewOpenOrderItemBinding
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible

class OpenOrderAdapter(val context: Context) :
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


            if (item.futureDeliveryDate?.isNotEmpty() == true && item.futureDeliveryDate != null) {
                binding.tvDate.text =
                    TimeFormatUtils.convertDateFormatForOpenOrder(
                        item.futureDeliveryDate!!,
                        context
                    )

                binding.tvDate.text=item.futureDeliveryTime
            } else {

            }

            binding.txtCustomerName.text =
                (item.customer?.firstName ?: "") + " " + (item.customer?.lastName ?: "")

            if (item.orderItems.isNotEmpty()) {

                binding.rvOpenOrder.visible()
                binding.rvOpenOrder.addItemDecoration(
                    DividerItemDecoration(
                        binding.root.context,
                        LinearLayoutManager.VERTICAL
                    )
                )

                adapter = OpenOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter
                adapter!!.addAll(item.orderItems)
            } else {
                binding.rvOpenOrder.gone()
            }


            if (item.paymentStatus == "Cancelled" || item.paymentStatus == "Paid") {

                binding.txtCancelOrder.visibility = View.GONE
                binding.txtEditOrder.visibility = View.GONE
                binding.txtPrintReceipt.visibility = View.VISIBLE
                binding.txtPayNow.visibility = View.GONE
                binding.txtPrintReceipt.text = "Print Receipt"

            } else {
                binding.txtCancelOrder.visibility = View.VISIBLE
                binding.txtEditOrder.visibility = View.VISIBLE
                binding.txtPrintReceipt.visibility = View.VISIBLE
                binding.txtPrintReceipt.text = "Print Unpaid Receipt"
                binding.txtPayNow.visibility = View.VISIBLE
            }

            if (!item.isCheck) {

                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.bg_color))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.llShowLayout.visibility = View.GONE
                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_down_solid_arrow))
                binding.imgIndicator.setColorFilter(ContextCompat.getColor(context,R.color.drawable_ic_color))
            } else {

                binding.llMainLayout.background=itemView.context.getDrawable(R.drawable.button_selected)
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.llShowLayout.visibility = View.VISIBLE
                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_solid_up_arrow))
                binding.imgIndicator.setColorFilter(ContextCompat.getColor(context,R.color.white))

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
                binding.txtCancelOrder.background=itemView.context.getDrawable(R.drawable.button_selected)
                binding.txtEditOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPrintReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtCustomerReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "")
            }

            binding.txtPrintReceipt.setOnClickListener {

                binding.txtCancelOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPrintReceipt.background=itemView.context.getDrawable(R.drawable.button_selected)
                binding.txtCustomerReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)

                if (orderList[bindingAdapterPosition].paymentStatus == "Paid") {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_PAID)
                } else {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_UNPAID)
                }
            }

            binding.txtEditOrder.setOnClickListener {
                binding.txtCancelOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background=itemView.context.getDrawable(R.drawable.button_selected)
                binding.txtPrintReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtCustomerReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "UPDATE")
            }

            binding.txtPayNow.setOnClickListener {
                binding.txtCancelOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPrintReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtCustomerReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background=itemView.context.getDrawable(R.drawable.button_selected)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "PAY")
            }
            binding.txtCustomerReceipt.setOnClickListener {
                binding.txtCancelOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPrintReceipt.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtCustomerReceipt.background=itemView.context.getDrawable(R.drawable.button_selected)
                binding.txtPayNow.background=itemView.context.getDrawable(R.drawable.background_square_border_grey)
                if (orderList[bindingAdapterPosition].paymentStatus == "Paid") {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_PAID)
                } else {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_UNPAID)
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

    fun getItem(pos: Int): OpenOrderResponse.Data.Order {

        return orderList[pos]
    }

    fun update(position: Int) {
        orderList.removeAt(position)
        notifyDataSetChanged()
    }

}