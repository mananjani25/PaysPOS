package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.orderhistory.Orders
import com.pays.pos.data.model.responseModel.orderhistory.PaymentDetail
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewOrderHistoryBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.MethodUtils.Companion.getFormattedDateTime
import com.pays.pos.utils.Pref

class OrderHistoryAdapter(val callBack: (View, Orders) -> Unit) :
    RecyclerView.Adapter<OrderHistoryAdapter.MyViewHolder>() {
    lateinit var myOnclickedListner: MyOnclickedListner
    private var arrayList = ArrayList<Orders>()

    var finalreward = ""
    var enrolltrueloyalty = false
    var prefProvider: PrefProvider? = null
    fun setPrefrenceData(temp_prefrence: PrefProvider) {
        prefProvider = temp_prefrence
    }

    fun setListner(listner: MyOnclickedListner) {
        this.myOnclickedListner = listner

    }

    inner class MyViewHolder(private val binding: ViewOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(order: Orders) {

            //Date Time
            setupDateTime(order.createdAt)

            //details
            binding.txtOrderDetails.text = "${order.itemDetails}"

            //Id and status

            if (prefProvider?.getValueboolean(
                    Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                    false
                ) == true
            ) {
                setupIdAndStatus(order.custom_order_id, order.paymentStatus)
            } else {
                setupIdAndStatus(order.id, order.paymentStatus)
            }


            //amount and pay type
            if (order.paymentDetails?.isNotEmpty() == true) {
                setupAmountPayType(order.total, order.paymentDetails)
            }
            setupAmountPayType(order.total, order.paymentDetails)

            //loyalty points
            if (enrolltrueloyalty) {
                binding.txtLoyaltyPoints.visibility = View.VISIBLE
                binding.txtLoyaltyPoints.text = "${order.order_loyalty_points ?: 0}"
                binding.txtUsedLoyaltyPoints.visibility = View.VISIBLE
                binding.txtUsedLoyaltyPoints.text = "${order.used_reward_points ?: 0}"
            } else {
                binding.txtLoyaltyPoints.visibility = View.GONE
            }

            if (arrayList[absoluteAdapterPosition].orderType == "OnlineWebOrder"
                || arrayList[absoluteAdapterPosition].orderType == "OnlineOrder" ||
                arrayList[absoluteAdapterPosition].orderType == "Online Order"
            ) {
                binding.txtReorder.setTextColor(binding.root.resources.getColor(R.color.gray_color))
                binding.txtReorder.setOnClickListener(null)
            } else {
                //Reorder
                binding.txtReorder.setTextColor(binding.root.resources.getColor(R.color.txt_color_blue))
                binding.txtReorder.setOnClickListener {
                    myOnclickedListner.onclickedReorder(arrayList[absoluteAdapterPosition])
                }
            }
        }

        private fun setupAmountPayType(total: Double?, paymentDetails: List<PaymentDetail>?) {
            try {
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
            } catch (e: Exception) {

            }
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

    interface MyOnclickedListner {
        fun onclickedReorder(orders: Orders)
    }
}