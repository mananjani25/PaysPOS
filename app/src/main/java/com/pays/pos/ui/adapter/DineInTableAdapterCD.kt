package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewDineInHeaderCdBinding
import com.pays.pos.databinding.ViewDineInTableItemsCdBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import java.util.*

class DineInTableAdapterCD() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private var serviceChargeList: ArrayList<TbServiceCharge> = arrayListOf()
    private lateinit var listner: DineInTableListner
    private val TAG = "DineInTableAdapterCD"


    fun setListner(listner: DineInTableListner) {
        this.listner = listner
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return HeaderViewHolder(
                ViewDineInHeaderCdBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        } else {
            return MyViewHolder(
                ViewDineInTableItemsCdBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        }


    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (getItemViewType(position) == 1) {
            (holder as MyViewHolder).bind(list.get(position))
        } else {
            (holder as HeaderViewHolder).bind(list.get(position), position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    @SuppressLint("NotifyDataSetChanged")
    inner class HeaderViewHolder(private val binding: ViewDineInHeaderCdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DineInModel, position: Int) {

            var guestAmt = 0.0
            var isPaid = true
            var isAllFired = true
            var noItem = true
            var guestSubTotal = 0.0
            var totalTaxAmt: Double = 0.0

            var guestDiscount = 0.0

            for (i in position + 1 until list.size) {

                if (list.get(i).isHeader == 1) {
                    noItem = false
                    list.get(i).item?.let {
                        if (!it.isPaid) {
                            guestDiscount += it.discountPrice
                            guestAmt += (it.itemQuantity * it.price) - it.discountPrice
                            guestSubTotal += (it.itemQuantity * it.price) - it.discountPrice
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach { it ->

                                    guestAmt += (it.itemQuantity * it.price)
                                    guestSubTotal += (it.itemQuantity * it.price)

                                }
                            }

                            if (it.taxes?.isNotEmpty() == true) {

                                it.taxes?.forEach { tax ->

                                    if (tax.isActive) {
                                        totalTaxAmt += if (tax.taxType == "Percentage") {

                                            var modifierPrice = 0.0
                                            val price =
                                                (it.price * it.itemQuantity)

                                            it.modifiers.forEach {
                                                modifierPrice += (it.price * it.itemQuantity)
                                            }

                                            val totalPrice =
                                                price + modifierPrice - it.discountPrice

                                            val itemTaxPrice =
                                                (tax.rate * totalPrice) / 100

                                            itemTaxPrice
                                        } else {

                                            tax.rate * it.itemQuantity
                                        }
                                    }
                                    guestAmt += tax.rate
                                }


                            }


                        }

                        if (!it.isPaid) {
                            isPaid = it.isPaid
                        }

                        if (!it.isFired) {
                            isAllFired = false
                        }
                    }


                } else {
                    break
                }
                if (list.get(i).isHeader == 0) {
                    isPaid = list.get(i).isPaid
                }
            }


            guestAmt += list.get(0).guestDividedAmt

            Log.e("CheckCustomer","customerData:   ${list[bindingAdapterPosition].customer}")

            if (list.get(bindingAdapterPosition).customer != null) {
                binding.txtTableName.setText(
                    list.get(bindingAdapterPosition).customer?.first_name + " " + list.get(
                        bindingAdapterPosition
                    ).customer?.last_name
                )
            } else {
                binding.txtTableName.setText(list.get(bindingAdapterPosition).title)

            }
//            btnColorDark
//            colorGreen/

            Log.e(TAG, "guestAmtguestAmt  ${guestAmt}")

            var totalServiceCharge = 0.0

            if (serviceChargeList.isNotEmpty() == true) {
                var isApplied = false
                serviceChargeList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                list.get(0).totalGuestCount
                            )
                        ) {
                            isApplied = true
                            totalServiceCharge += (guestSubTotal * it.percentage) / 100
                            return@forEach

                        }
                    }

                }
                if (!isApplied) {
                    serviceChargeList.forEach { service ->
                        if (service.id == checkMaxGuestCountId()) {
                            totalServiceCharge += (guestSubTotal * service.percentage) / 100
                            return@forEach
                        }
                    }
                }


            }


            var finalAmt =
                guestSubTotal + totalServiceCharge + totalTaxAmt + list.get(0).guestDividedAmt

            finalAmt = String.format("%.2f", finalAmt).toDouble()
            LogUtil.logE("TODAY", "guestSubTotal  ${guestSubTotal}")
            LogUtil.logE("TODAY", "totalTaxAmt  ${totalTaxAmt}")
            LogUtil.logE("TODAY", "guestDividedAmt  ${(list.get(0).guestDividedAmt)}")
            LogUtil.logE("TODAY", "totalServiceCharge  ${(totalServiceCharge)}")
            LogUtil.logE("TODAY", "orderTotalAmount  ${list.get(0).orderTotalAmount}")
            LogUtil.logE("TODAY", "orderfinalAmt:  ${finalAmt}")

            var guestOrderDisShare = 0.0
            if (list.get(0).orderDiscount > 0) {
                guestOrderDisShare =
                    MethodUtils.roundOffAmountDouble(list[0].orderDiscount / (list[0].totalGuestCount))
                /*  guestOrderDisShare =
                      (finalAmt * list.get(0).orderDiscount) / (list.get(0).orderTotalAmount + list.get(0).orderDiscount)*/

                LogUtil.logE("TODAY", "guestOrderDisShare  ${guestOrderDisShare}")

            }
            finalAmt = finalAmt - guestOrderDisShare

            Log.e("CheckListData","TotalGuestCount:  ${list[bindingAdapterPosition].totalGuestCount}")
            Log.e("CheckListData","Title:  ${list[bindingAdapterPosition].title}")
            if (list[bindingAdapterPosition].title?.lowercase() != "Whole Table".lowercase() && list[0].totalGuestCount > 1) {
                if (isPaid && !noItem) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.txtGuestTotal?.setTextColor(binding.root.context.getColor(R.color.colorGreen))
                        binding.txtGuestTotal?.text = "Paid"
                    }

                } else if (list.get(position).isPaid) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.txtGuestTotal?.setTextColor(binding.root.context.getColor(R.color.colorGreen))
                        binding.txtGuestTotal?.text = "Paid"
                    }
                } else {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.txtGuestTotal?.setTextColor(binding.root.context.getColor(R.color.white))
                    }
                    binding.txtGuestTotal?.text = "Pay " + MethodUtils.roundOffAmount(finalAmt)
                }


            }

        }

        fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
            return (minn <= value && value <= maxx)
        }

        fun checkMaxGuestCountId(): Int {
            var maxValue = 0
            var serviceChargeId = 0
            serviceChargeList.forEach { serviceCharge ->
                if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                    if (serviceCharge.max_guest_count!! >= maxValue) {
                        maxValue = serviceCharge.max_guest_count
                        serviceChargeId = serviceCharge.id
                    }
                }
            }
            return serviceChargeId
        }
    }


    inner class MyViewHolder(private val binding: ViewDineInTableItemsCdBinding) :
        RecyclerView.ViewHolder(binding.root), DineInTableItemAdapter.DineInItemListner {
        fun bind(model: DineInModel) {

            if (model.item?.discountPrice != 0.0) {
                binding.tvDiscountRate.visibility = View.VISIBLE
                binding.tvRate.paintFlags =
                    binding.tvRate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                val dPrice = model.item?.let { totalPrice(it) - it.discountPrice }
                dPrice?.let { MethodUtils.setPriceTextView(binding.tvDiscountRate, it) }
            } else {
                binding.tvRate.paintFlags = 0
                binding.tvDiscountRate.text = ""
                binding.tvDiscountRate.visibility = View.GONE

            }


            model.item?.let { totalPrice(it) }?.let {
                MethodUtils.setPriceTextView(
                    binding.tvRate,
                    it
                )
            }

            if (model.item?.modifiers?.isNotEmpty() == true) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = DineInModifiersAdapterCD()
                binding.rvModifiers.adapter = adapter
                model.item?.modifiers?.let { adapter.addAll(it) }
            } else {
                binding.rvModifiers.visibility = View.GONE
            }

            if (model.item?.note?.isEmpty() == true) {
                binding.txtNote.visibility = View.GONE
            } else {
                binding.txtNote.visibility = View.VISIBLE
            }

            MethodUtils.setPriceTextView(
                binding.txtEmpName,
                model.item?.price!!
            )

//            if (list[bindingAdapterPosition].empName == "null" || list[bindingAdapterPosition].empName == null) {
//                binding.txtEmpName.text = ""
//            } else {
//                binding.txtEmpName.text = list[bindingAdapterPosition].empName
//            }
            binding.model = model.item
            binding.executePendingBindings()


        }

        override fun onSendOrderToKitchen(item: TbCartItem) {

            listner.onSendItemToKitchen(item)
        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    interface DineInTableListner {
        fun onGuestPay(
            dineInModel: DineInModel,
            position: Int,
            subTotal: Double,
            total: Double,
            tax: Double,
            serviceCharge: Double,
            discount: Double,
            guestDividedAmt: Double,
            listItemWT: ArrayList<TbItem>,
            listItemGuestSelected: ArrayList<TbItem>
        )

        fun onSendItemToKitchen(item: TbCartItem)
        fun onWholeTableToKitchen(ids: String, listItems: ArrayList<TbItem>)
        fun singleItemFired(id: String, position: Int, item: TbItem)
        fun onGuestPrint(
            listItem: ArrayList<TbItem>,
            guestName: String,
            listWTitems: ArrayList<TbItem>,
            subTotalGuest: Double,
            total: Double,
            taxGuest: Double,
            serviceChargeGuest: Double,
            divideDiscount: Double
        )
    }

    fun getList(): List<DineInModel> {
        return list
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].isHeader
    }

    private fun totalPrice(model: TbCartItem): Double {

        return model.price * model.itemQuantity
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(list, i, i + 1)


                        val order1: Int = list[i].sort
                        val order2: Int = list[i + 1].sort
                        list[i].sort = order2
                        list[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(list, i, i - 1)

                        val order1: Int = list[i].sort
                        val order2: Int = list[i - 1].sort
                        list[i].sort = (order2)
                        list[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

    fun updateStatus(clickedPos: Int, isFireAll: Boolean) {
        if (isFireAll) {
            list.forEach {
                if (it.isHeader == 1) {
                    it.item?.isFired = true

                } else {
                    it.isFired = true
                }
            }
        } else {

            list[clickedPos].item?.isFired = true
        }
        notifyDataSetChanged()
    }

    fun setSurchargeList(serviceChargeListt: java.util.ArrayList<TbServiceCharge>) {
        this.serviceChargeList = serviceChargeListt
        notifyDataSetChanged()
    }

}