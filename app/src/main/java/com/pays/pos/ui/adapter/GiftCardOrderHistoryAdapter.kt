package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.giftCardOrderHistory.GiftCardRecord
import com.pays.pos.data.model.responseModel.orderhistory.PaymentDetail
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewGiftCardOrderHistoryBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.MethodUtils.Companion.getFormattedDateTime

class GiftCardOrderHistoryAdapter :
    RecyclerView.Adapter<GiftCardOrderHistoryAdapter.MyViewHolder>() {
    lateinit var myOnclickedListner: MyOnclickedListner
    private var arrayList = ArrayList<GiftCardRecord>()

    var prefProvider: PrefProvider? = null
    fun setPrefrenceData(temp_prefrence: PrefProvider) {
        prefProvider = temp_prefrence
    }

    fun setListner(listner: MyOnclickedListner) {
        this.myOnclickedListner = listner

    }

    inner class MyViewHolder(private val binding: ViewGiftCardOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(order: GiftCardRecord) {

            //Date Time
            setupDateTime(order.created_at)

            //details
            binding.txtOrderDetails.text = order.item_details

            //Id and status

            /*if (prefProvider?.getValueboolean(
                    Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                    false
                ) == true
            ) {
                setupIdAndStatus(order.custom_order_id, order.paymentStatus)
            } else {*/
                setupIdAndStatus(order.id, order.event)
            //}


            //amount and pay type
            /*if (order.paymentDetails?.isNotEmpty() == true) {
                setupAmountPayType(order.total, order.paymentDetails)
            }*/
            //setupAmountPayType(order.amount.toDouble(), "Cash")
            binding.txtPayType.text = "$${order.amount}"
        }

        private fun setupAmountPayType(total: Double?, paymentDetails: List<PaymentDetail>?) {

            val sring = SpannableStringBuilder()

            var size = 1
            paymentDetails?.forEach {


                val totalFormatted = "$" + String.format(
                    "%.2f",
                    it.amount ?: 0.0
                )
                val ssTotal = SpannableStringBuilder(totalFormatted)
                ssTotal.setSpan(
                    TextAppearanceSpan(MainApplication.getInstance(), R.style.DateStyle),
                    0,
                    ssTotal.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val payType = it.paymentType/*if (paymentDetails.isNotEmpty()) {
                    paymentDetails[0].paymentType ?: ""
                } else {
                    ""
                }*/
                val ssPayType = SpannableStringBuilder(payType)
                ssPayType.setSpan(
                    TextAppearanceSpan(MainApplication.getInstance(), R.style.TimeStyle),
                    0,
                    ssPayType.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                val a = TextUtils.concat(ssTotal, "\n", ssPayType)

                sring.append(a)
                if (paymentDetails.size > 1 && paymentDetails.size > size)
                    sring.append("\n\n")

                size += 1
            }
            binding.txtPayType.text = sring.toString()
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
        val binding = ViewGiftCardOrderHistoryBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<GiftCardRecord>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

    interface MyOnclickedListner {
        fun onclickedReorder(orders: GiftCardRecord)
    }
}