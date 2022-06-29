package com.android.pos.ui.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewRefundItemBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.MethodUtils

class RefundItemListAdapter(val viewModel: TransactionDetailsViewModel) :
    RecyclerView.Adapter<RefundItemListAdapter.MyViewHolder>() {

    var showItemSubTotal: (() -> Unit)? = null
    private var selectedItemList: ArrayList<GetOrderDetailsResponse.Data.OrderItem> = arrayListOf()
    var noteList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()
    var serviceCharge: Double = 0.0
    var cashdiscountType: String = ""
    var paymentType: String = ""
    var cash_discount_or_surcharge: Double = 0.0
    var totalDiscount: Double = 0.0
    var loyaltyAmount: Double = 0.0
    var serviceChargeList: List<TbServiceCharge> = arrayListOf()

    var rate_or_amount = ""

    fun setSelectedItemList(
        list: ArrayList<GetOrderDetailsResponse.Data.OrderItem>,
        value: String,
        rate_or_amount: String
    ) {
        selectedItemList.clear()
        selectedItemList.addAll(list)
        this.rate_or_amount = rate_or_amount

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RefundItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewRefundItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addItems(
        noteList: List<GetOrderDetailsResponse.Data.OrderItem>,
        serviceCharge: List<TbServiceCharge>?,
        cash_discount_or_surcharge: Double,
        cashDiscountType: String,
        paymentType: String,
        totalDiscount: Double,
        loyaltyAmount: Double?
    ) {
        this.cash_discount_or_surcharge = cash_discount_or_surcharge
        this.totalDiscount = totalDiscount
        if (loyaltyAmount != null) {
            this.loyaltyAmount = loyaltyAmount
        }
        this.cashdiscountType = cashDiscountType
        this.paymentType = paymentType
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
        this.serviceChargeList =
            serviceCharge as List<TbServiceCharge>
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: RefundItemListAdapter.MyViewHolder, position: Int) {

        holder.bind(noteList.get(position))
    }

    override fun getItemCount(): Int {
        return noteList.size

    }


    inner class MyViewHolder(val itemBinding: ViewRefundItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: GetOrderDetailsResponse.Data.OrderItem) {
            itemBinding.refundItemListModel = item

            itemBinding.tvItemName.text = item.itemName

            val modifierNames = item.orderItemModifiers.map {
                it.name + " (" + itemBinding.root.context.getString(R.string.symbole) + " " + String.format(
                    itemBinding.root.context.getString(
                        R.string.format
                    ), it.price
                ) + ")"
            }

            if (modifierNames.isEmpty()) {
                itemBinding.tvModifierName.visibility = View.GONE
            } else {
                itemBinding.tvModifierName.visibility = View.VISIBLE
                itemBinding.tvModifierName.text = TextUtils.join(",", modifierNames)
            }

            var totalTax = 0.0
            var orderDiscount = 0.0
            var loyaltyAmountPerItem = 0.0


            orderDiscount = (totalDiscount) / itemCount
            Log.d(
                "yash",
                "bind: [" + absoluteAdapterPosition + "] orderDiscount : " + orderDiscount
            )


            loyaltyAmountPerItem = loyaltyAmount / itemCount
            Log.d(
                "yash",
                "bind: [" + absoluteAdapterPosition + "] loyaltyAmountPerItem : " + loyaltyAmountPerItem
            )

            var totalItemPrice: Double = 0.0
            totalItemPrice = totalPrice(item)



            item.orderItemTaxes.forEach { tax ->
                tax.taxTotalAmount.let {
                    totalTax += it
                }
            }
            Log.d("yash", "bind: [$absoluteAdapterPosition] totaltax : $totalTax")

            var totalServiceCharge = 0.0
            serviceChargeList.forEach {
                if (it.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                    totalServiceCharge += (totalItemPrice * it.percentage) / 100
                }
            }
            Log.d(
                "yash",
                "bind: [$absoluteAdapterPosition] totalServiceCharge : $totalServiceCharge"
            )

            totalItemPrice += (totalTax + totalServiceCharge) - orderDiscount - loyaltyAmountPerItem


            Log.d(
                "yash",
                "bind: [$absoluteAdapterPosition] totalItemPrice : $totalItemPrice"
            )

            var cashDiscountDivide = 0.0
            if (paymentType == "Cash") {
                if (cashdiscountType == "CashDiscount" && rate_or_amount.isNotEmpty()) {
                    cashDiscountDivide = (totalItemPrice * (rate_or_amount.toDouble())) / 100
                }
            } else if (paymentType == "Card") {
                if (cashdiscountType == "SurCharge" && rate_or_amount.isNotEmpty()) {
                    cashDiscountDivide = totalItemPrice * (rate_or_amount.toDouble()) / 100
                }
            }
            Log.d(
                "yash",
                "bind: [$absoluteAdapterPosition] cashDiscountDivide : $cashDiscountDivide"
            )

            if (paymentType == "Cash") {
                totalItemPrice -= cashDiscountDivide
            } else if (paymentType == "Card") {
                totalItemPrice += cashDiscountDivide
            }
            Log.d("yash", "bind: [$absoluteAdapterPosition] finalTotal : $totalItemPrice")
            MethodUtils.setPriceTextView(itemBinding.tvItemPrice, totalItemPrice.toDouble())
            var count = 0.0
            itemBinding.ivCheck.setOnClickListener {
                item.isChecked = !item.isChecked

                if (item.isChecked) {
                    count += totalItemPrice
                    Log.d("yash", "bind: sellecttotal :  $count")
//                    selectedItemList.add(item)
                } else {
                    count -= totalItemPrice
                    Log.d("yash", "bind: sellecttotal :  $count")
//                    selectedItemList.remove(item)
                }

                showItemSubTotal?.invoke()
                notifyDataSetChanged()
            }



            itemBinding.executePendingBindings()
        }
    }


    fun selectedItemList(): ArrayList<GetOrderDetailsResponse.Data.OrderItem> {
        return selectedItemList
    }

    private fun totalPrice(model: GetOrderDetailsResponse.Data.OrderItem): Double {

        return if (model.orderItemModifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.orderItemModifiers
            mList.forEach { items ->
                totalPrice += items.price * model.quantity
            }

            ((model.price) * model.quantity) - model.discountAmount + totalPrice
        } else {

            ((model.price) * model.quantity) - model.discountAmount

        }
    }


}