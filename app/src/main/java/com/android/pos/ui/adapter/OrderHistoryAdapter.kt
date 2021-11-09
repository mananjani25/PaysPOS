package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.model.responseModel.orderhistory.Orders
import com.android.pos.databinding.ViewOrderHistoryBinding

class OrderHistoryAdapter :
    RecyclerView.Adapter<OrderHistoryAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<Orders>()

    inner class MyViewHolder(private val binding: ViewOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(order: Orders) {

            binding.txtDateTime.text = "${order.date}"
            binding.txtOrderDetails.text = "${order.itemDetails}"
            var payType = ""
            if (order.paymentDetails?.isNotEmpty() == true) {
                payType = order.paymentDetails[0].paymentType ?: ""
            }
            binding.txtPayType.text = payType

            //earned points
            /*(String.format(
                "%.2f",
                order.total
            )).also {
                binding.txtTotalEearned.text = it
            }*/
            binding.txtTotalEearned.text = "-"

            //Id and status
            val orderId = SpannableStringBuilder("${order.id}")
            orderId.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.OrderIdStyle),
                0,
                orderId.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val paymentStatus = SpannableStringBuilder("${order.paymentStatus}")
            paymentStatus.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.PaymentStatusStyle),
                0,
                paymentStatus.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            TextUtils.concat(orderId, "\n(", paymentStatus,")").also { binding.txtOrderId.text = it }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderHistoryBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<Orders>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

}