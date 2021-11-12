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
import com.android.pos.data.model.responseModel.orderhistory.PaymentDetail
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewOrderHistoryBinding
import com.android.pos.utils.MethodUtils.Companion.getFormattedDateTime

class OrderHistoryAdapter :
    RecyclerView.Adapter<OrderHistoryAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<Orders>()

    inner class MyViewHolder(private val binding: ViewOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(order: Orders) {

            //Date Time
            setupDateTime(order.createdAt)

            //details
            binding.txtOrderDetails.text = "${order.itemDetails}"

            //Id and status
            setupIdAndStatus(order.id, order.paymentStatus)

            //amount and pay type
            if (order.paymentDetails?.isNotEmpty() == true) {
                setupAmountPayType(order.total, order.paymentDetails)
            }
            setupAmountPayType(order.total, order.paymentDetails)

            //loyalty points
            binding.txtLoyaltyPoints.text = "-"

        }

        private fun setupAmountPayType(total: Double?, paymentDetails: List<PaymentDetail>?) {
            val totalFormatted = "$" + String.format(
                "%.2f",
                total ?: 0.0
            )
            val ssTotal = SpannableStringBuilder(totalFormatted)
            ssTotal.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.DateStyle),
                0,
                ssTotal.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val payType = if (paymentDetails?.isNotEmpty() == true) {
                paymentDetails[0].paymentType ?: ""
            } else {
                ""
            }
            val ssPayType = SpannableStringBuilder(payType)
            ssPayType.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.TimeStyle),
                0,
                ssPayType.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            TextUtils.concat(ssTotal, "\n", ssPayType)
                .also { binding.txtPayType.text = it }
        }

        private fun setupIdAndStatus(id: Int?, paymentStatus: String?) {
            val orderId = SpannableStringBuilder("$id")
            orderId.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.OrderIdStyle),
                0,
                orderId.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val paymentStatus = SpannableStringBuilder("$paymentStatus")
            paymentStatus.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.PaymentStatusStyle),
                0,
                paymentStatus.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            TextUtils.concat(orderId, "\n", paymentStatus)
                .also { binding.txtOrderId.text = it }
        }

        private fun setupDateTime(dateTime: String?) {
            if (dateTime.isNullOrEmpty()) {
                binding.txtDateTime.text = "-"
            } else {
                val date = getFormattedDateTime(
                    inputFormat = Constants.DateFormat_yyyy_MM_dd_T_HH_mm_ss_SSSZ,
                    date = dateTime,
                    outputFormat = Constants.DateFormat_MMM_dd_yyyy,
                    timeZoneApplied = true
                )
                val time = getFormattedDateTime(
                    inputFormat = Constants.DateFormat_yyyy_MM_dd_T_HH_mm_ss_SSSZ,
                    date = dateTime,
                    outputFormat = Constants.DateFormat_hh_mm_a,
                    timeZoneApplied = true
                )
                val ssDate = SpannableStringBuilder(date)
                ssDate.setSpan(
                    TextAppearanceSpan(MainApplication.getInstance(), R.style.DateStyle),
                    0,
                    ssDate.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                val ssTime = SpannableStringBuilder(time)
                ssTime.setSpan(
                    TextAppearanceSpan(MainApplication.getInstance(), R.style.TimeStyle),
                    0,
                    ssTime.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                TextUtils.concat(ssDate, "\n", ssTime)
                    .also { binding.txtDateTime.text = it }
            }
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