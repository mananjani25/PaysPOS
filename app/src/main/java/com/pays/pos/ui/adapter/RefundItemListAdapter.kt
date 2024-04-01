package com.pays.pos.ui.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewRefundItemBinding
import com.pays.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import java.text.NumberFormat

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
    var tipValue: Double = 0.0
    var rate_or_amount = ""
    var orderType = ""
    var final_Amount = 0.0


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
        temp_final_Amount: Double,
        noteList: List<GetOrderDetailsResponse.Data.OrderItem>,
        serviceCharge: List<TbServiceCharge>?,
        cash_discount_or_surcharge: Double,
        cashDiscountType: String,
        paymentType: String,
        totalDiscount: Double,
        loyaltyAmount: Double?,
        tipAmount: Double?,
        orderType: String
    ) {
        this.final_Amount = temp_final_Amount
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
        this.tipValue = tipAmount!!
        this.serviceChargeList =
            serviceCharge as List<TbServiceCharge>
        this.orderType = orderType
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

            var totalItemPrice: Double = 0.0
            totalItemPrice = totalPrice(item)

            var actualSubTotalWithoutOrderDis = 0.0
            noteList.forEach { orderItem ->
                actualSubTotalWithoutOrderDis += (orderItem.price * orderItem.quantity) - (orderItem.discountAmount)
                orderItem.orderItemModifiers.forEach { modifierNames ->
                    actualSubTotalWithoutOrderDis += modifierNames.price * modifierNames.quantity
                }
            }
            Log.d("yash", "bind:subTotal Without OrderDiscount " + actualSubTotalWithoutOrderDis)


            orderDiscount = (totalItemPrice * totalDiscount) / actualSubTotalWithoutOrderDis
            val nfone: NumberFormat = NumberFormat.getNumberInstance()
            nfone.maximumFractionDigits = 3
            val rounded1: String = nfone.format(orderDiscount)
            orderDiscount = rounded1.toDouble()
            Log.d(
                "yash",
                "bind: [" + absoluteAdapterPosition + "] orderDiscount : " + orderDiscount
            )
//            item.orderItemTaxes.forEach { tax ->
//                tax.taxTotalAmount.let {
//                    totalTax += it
//                }
//
//
//            }
            item.orderItemTaxes.forEach { tax ->
                totalTax += if (tax.taxType == "Percentage") {
                    if (totalItemPrice < 0.0) {

                        String.format("%.2f", 0.00)
                            .toDouble()
                    } else {
                        val itemTaxPrice =
                            (tax.rate * totalItemPrice) / 100
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                        String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    }

                } else {
                    Log.d("yash", "taxCalculation: " + tax.taxType)
                    if (totalItemPrice <= 0.0) {
                        String.format("%.2f", 0.00)
                            .toDouble()
                    } else {
                        String.format("%.2f", tax.rate * item.quantity)
                            .toDouble()
                    }
                }
            }
            Log.d("yash", "bind: [$absoluteAdapterPosition] totaltax : $totalTax")

            var totalServiceCharge = 0.0



            if (totalItemPrice >= orderDiscount) {
                totalItemPrice -= orderDiscount
            }

            serviceChargeList.forEach {
                totalServiceCharge += (totalItemPrice * it.percentage) / 100
            }
            String.format("%.2f", totalServiceCharge)
                .toDouble()
            totalItemPrice += (totalTax + totalServiceCharge)

            var cashDiscountDivide = 0.0
            if (cash_discount_or_surcharge > 0.0) {
                cashDiscountDivide = (cash_discount_or_surcharge * totalItemPrice) / final_Amount
            }

            val nf2: NumberFormat = NumberFormat.getNumberInstance()
            nf2.maximumFractionDigits = 2
            val rounded2: String = nf2.format(cashDiscountDivide)
//            cashDiscountDivide = rounded2.toDouble()
            if (rounded2.contains(',')){
                val result = rounded2.filter { it != ',' }
                cashDiscountDivide=result.toDouble()
            }else{
                cashDiscountDivide = rounded2.toDouble()
            }
            Log.d(
                "yash",
                "bind: [$absoluteAdapterPosition] cashDiscountDivide : $cashDiscountDivide"
            )

            if (paymentType == "Cash") {
                if (totalItemPrice >= cashDiscountDivide) {
                    totalItemPrice -= cashDiscountDivide
                }
            } else if (paymentType == "Card") {
                totalItemPrice += cashDiscountDivide
            }


            var loyaltyAmountPerItem = 0.0
            loyaltyAmountPerItem = (loyaltyAmount * totalItemPrice) / final_Amount
            Log.d(
                "yash",
                "bind: [" + absoluteAdapterPosition + "] loyaltyAmountPerItem : " + loyaltyAmountPerItem
            )

            /* var tip_divided = 0.0
             if (totalItemPrice == 0.0) {
                 if (final_Amount == 0.0) {
                     tip_divided = tipValue / noteList.size
                 } else {
                     tip_divided = (totalItemPrice * tipValue) / final_Amount
                 }
             } else {
                 if (cashdiscountType == "SurCharge") {
                     tip_divided =
                         (totalItemPrice * tipValue) / (final_Amount + cash_discount_or_surcharge)
                 } else {
                     tip_divided = (totalItemPrice * tipValue) / (final_Amount)
                 }
             }

             val nf1: NumberFormat = NumberFormat.getNumberInstance()
             nf1.maximumFractionDigits = 2
             val rounded: String = nf1.format(tip_divided)
             tip_divided = rounded.replace(",","").toDouble()
             if (paymentType == "Card") {
                     if (totalItemPrice >= tip_divided) {
                         // Commented by Mansi to remove tip from refund
 //                        totalItemPrice += tip_divided
                     } else if (totalItemPrice == 0.0) {
                         totalItemPrice += tipValue / noteList.size
                     } else {
                         totalItemPrice += tipValue
                     }
             }




            Log.d(
                "yash",
                "bind: [$absoluteAdapterPosition] tip_divided : $tip_divided"
            )*/

            if (totalItemPrice >= loyaltyAmountPerItem) {
                totalItemPrice -= loyaltyAmountPerItem
            }




            Log.d("yash", "bind: [$absoluteAdapterPosition] finalTotal : $totalItemPrice")
            MethodUtils.setPriceTextView(itemBinding.tvItemPrice, totalItemPrice.toPrecision(2).toDouble())
            itemBinding.ivCheck.setOnClickListener {
                item.isChecked = !item.isChecked
                selectedItemList[bindingAdapterPosition].isChecked = item.isChecked
//                showItemSubTotal?.invoke()
                notifyDataSetChanged()
            }


            if(selectedItemList[bindingAdapterPosition].refundedAmount != 0.0) {
                itemBinding.root.visibility = View.GONE
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
                totalPrice += items.price * items.quantity
            }

            ((model.price) * model.quantity) - model.discountAmount + totalPrice
        } else {

            ((model.price) * model.quantity) - model.discountAmount

        }
    }


}