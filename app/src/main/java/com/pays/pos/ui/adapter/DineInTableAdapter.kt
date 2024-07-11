package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewDineInHeaderBinding
import com.pays.pos.databinding.ViewDineInTableItemsBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

class DineInTableAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private var serviceChargeList: ArrayList<TbServiceCharge> = arrayListOf()
    private lateinit var itemAdapter: DineInTableItemAdapter
    private lateinit var listner: DineInTableListner
    private val TAG = "DineInTableAdapter"
    private var isAnyPaymentDone : Boolean = false


    fun setListner(listner: DineInTableListner) {
        this.listner = listner
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return HeaderViewHolder(
                ViewDineInHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        } else {
            return MyViewHolder(
                ViewDineInTableItemsBinding.inflate(
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
    inner class HeaderViewHolder(private val binding: ViewDineInHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DineInModel, position: Int) {

            var guestAmt = 0.0
            var isPaid = true
            var isAllFired = true
            var noItem = true
            var guestSubTotal = 0.0
            var guestSubTotalWithOutCharges = 0.0
            var totalTaxAmt: Double = 0.0

            if (list[position].title?.trim()?.lowercase() == "Whole Table".trim().lowercase() || list[position].itemsCount == 0) {
                binding.imgPrint.visibility = View.INVISIBLE
            } else {
                binding.imgPrint.visibility = View.VISIBLE
            }

            if (list[position].title?.trim()?.lowercase() == "Whole Table".trim().lowercase() || isAnyPaymentDone) {
                binding.llRemoveGuest.visibility = View.INVISIBLE
            } else {
                if(list[position].itemsCount == 0 && !list[position].isPaid) {
                    binding.llRemoveGuest.visibility = View.VISIBLE
                } else {
                    binding.llRemoveGuest.visibility = View.INVISIBLE
                }
            }
            var guestDiscount = 0.0

            for (i in position + 1 until list.size) {

                if (list.get(i).isHeader == 1) {
                    noItem = false
                    list.get(i).item?.let {
                        if (!it.isPaid) {
                            guestDiscount += it.discountPrice
                            guestAmt += (it.itemQuantity * it.price) - it.discountPrice
                            guestSubTotal += (it.itemQuantity * it.price) - it.discountPrice
                            guestSubTotalWithOutCharges += (it.itemQuantity * it.price) - it.discountPrice
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach { it ->

                                    guestAmt += (it.itemQuantity * it.price)
                                    guestSubTotal += (it.itemQuantity * it.price)
                                    guestSubTotalWithOutCharges += (it.itemQuantity * it.price)
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


            if (list.get(position).customer != null) {
                binding.txtTableName.setText(
                    list.get(layoutPosition).customer?.first_name + " " + list.get(
                        layoutPosition
                    ).customer?.last_name
                )
            } else {
                binding.txtTableName.setText(list.get(layoutPosition).title)

            }
//            btnColorDark
//            colorGreen/

            Log.e(TAG, "guestAmtguestAmt  ${guestAmt}")
            binding.btnPay.visibility = View.VISIBLE
            if (isPaid && !noItem) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.btnPay.setBackgroundColor(binding.root.context.getColor(R.color.colorGreen))
                    binding.txtPay.text = "Paid"
                }

            } else if (list.get(position).isPaid) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.btnPay.setBackgroundColor(binding.root.context.getColor(R.color.colorGreen))
                    binding.txtPay.text = "Paid"
                }
            } else if (list[position].itemsCount == 0) {
                Log.e(TAG, "NoItemGuestAmt")
                binding.btnPay.visibility = View.INVISIBLE
            } else {
                binding.btnPay.visibility = View.VISIBLE

            }
            binding.chkIsFired.isEnabled = !noItem

            if (list[layoutPosition].title?.lowercase() == "Whole Table".lowercase() || list.get(0).totalGuestCount == 1) {
                Log.e(TAG, "TxtPayTitelTotal")
                binding.txtPay.visibility = View.GONE
                var layoutmanager: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutmanager.setMargins(0, 0, 0, 0)

            } else {
                binding.txtPay.visibility = View.VISIBLE
                // binding.txtTotal.visibility = View.VISIBLE
            }

            if (isAllFired && !noItem) {
                binding.chkIsFired.isChecked = true
                binding.chkIsFired.isPressed = true
                binding.chkIsFired.isEnabled = false
            } else if (noItem) {

                binding.chkIsFired.isChecked = false
                binding.chkIsFired.isPressed = false
                binding.chkIsFired.isEnabled = false
            }

            var totalServiceCharge = 0.0


            if (serviceChargeList.isNotEmpty() == true) {
                var isApplied = false
                serviceChargeList.forEach {
                    if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                        if (isInRange(
                                it.min_guest_count!!,
                                it.max_guest_count!!,
                                list[0].eligibleGuestsForDivision
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

                // Distributed order discount among guest based on discount percentage (To resolve minus guest amount issue)
                guestOrderDisShare = MethodUtils.roundOffAmountDouble(
                   MethodUtils.percentageCalculation(MethodUtils.roundOffAmountDouble(guestSubTotalWithOutCharges +
                           list.get(0).wholeTableSubTotal), list[0].orderDiscountPercentage))

//                guestOrderDisShare =
//                    MethodUtils.roundOffAmountDouble(list[0].orderDiscount / (list[0].totalGuestCount))
                /*  guestOrderDisShare =
                      (finalAmt * list.get(0).orderDiscount) / (list.get(0).orderTotalAmount + list.get(0).orderDiscount)*/

                LogUtil.logE("TODAY", "guestOrderDisShare  ${guestOrderDisShare}")

            }
            finalAmt = finalAmt - guestOrderDisShare

            LogUtil.logE("TODAY", "GuestguestSubTotal  ${guestSubTotal}")
            LogUtil.logE("TODAY", "GuesttotalServiceCharge  ${totalServiceCharge}")
            LogUtil.logE("TODAY", "GuesttotalTaxAmt  ${totalTaxAmt}")
            LogUtil.logE("TODAY", "GuestguestDividedAmt  ${list.get(0).guestDividedAmt}")
            //   LogUtil.logE(TAG,"GuestguestSubTotal  ${guestSubTotal}")

            // LogUtil.logE(TAG,"guestDivided  ${list.get(0).guestDividedAmt}")


            if (!list.get(position).isPaid) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.btnPay.setBackgroundColor(binding.root.context.getColor(R.color.btnColorDark))
                }
                binding.txtPay.text = "Pay : " + MethodUtils.roundOffAmount(finalAmt)
            }

            binding.btnPay.setOnClickListener {
                if (!list.get(position).isPaid) {
                    var listItem: ArrayList<TbCartItem> = arrayListOf()
                    var listItemWT: ArrayList<TbCartItem> = arrayListOf()
                    for (i in 0 until list.size) {
                        if (list[i].title?.lowercase() == "Whole Table".lowercase() && i != (list.size - 1)) {

                            for (j in i + 1 until list.size) {
                                if (list.get(j).isHeader == 1) {
                                    list.get(j).item?.let { it1 ->

                                        var itemToAdd = it1
                                        itemToAdd.guestIndexForDineIn = list[i].item?.guestIndexForDineIn
                                        listItemWT.add(itemToAdd) }
                                } else {
                                    break
                                }
                            }
                        }
                    }
                    for (i in bindingAdapterPosition + 1 until list.size) {
                        if (list.get(i).isHeader == 1) {
                            list[i].item?.apply {
                                orderType = "DineIn"
                                guestIndexForDineIn = list[i].item?.guestIndexForDineIn
                            }
                            list[i].item?.let { it1 ->
                                listItem.add(it1)

                            }
                        } else {
                            break;
                        }

                    }
                    Log.d(TAG, "bind: list of guest       " + Gson().toJson(listItem))
                    Log.d(TAG, "bind: list of whole table " + Gson().toJson(listItemWT))
                    var guestName = ""
                    if (listItem.isNotEmpty()) {
                        if (list[bindingAdapterPosition].customer != null) {
                            guestName =
                                list[bindingAdapterPosition].customer?.first_name.toString() + " " + list[bindingAdapterPosition].customer?.last_name.toString()
                        } else {
                            guestName = list[bindingAdapterPosition].title.toString()
                        }


                    }

                    LogUtil.logE(TAG, "listItemWTGuestPay:  ${Gson().toJson(listItemWT)}")
                    listner.onGuestPay(
                        list[position],
                        position,
                        MethodUtils.roundOffAmountDouble(guestSubTotal),
                        MethodUtils.roundOffAmountDouble(finalAmt),
                        MethodUtils.roundOffAmountDouble(totalTaxAmt),
                        MethodUtils.roundOffAmountDouble(totalServiceCharge + list[0].wholeTableSurTax),
                        guestOrderDisShare,
                        list[0].guestDividedAmt,
                        listItemWT,
                        listItem,
                        list[position].item?.guestIndexForDineIn?:0
                    )
                }
            }

            binding.imgPrint.setOnClickListener {
                var fisrtTime: Boolean = false
                var listItem: ArrayList<TbCartItem> = arrayListOf()
                var listItemWT: ArrayList<TbCartItem> = arrayListOf()
                for (i in 1 until list.size) {
                    if (list.get(i).isHeader == 1) {
                        list.get(i).item?.let { it1 -> listItemWT.add(it1) }
                    } else {
                        break
                    }
                }
                for (i in bindingAdapterPosition + 1 until list.size) {
                    if (list.get(i).isHeader == 1) {

                        list[i].item?.let { it1 -> listItem.add(it1) }
                    } else {
                        break;
                    }

                }

                var guestName = ""
                if (listItem.isNotEmpty()) {
                    if (list[bindingAdapterPosition].customer != null) {
                        guestName = list[bindingAdapterPosition].customer?.first_name.toString() + " " + list[bindingAdapterPosition].customer?.last_name.toString()
                    } else {
                        guestName = list[bindingAdapterPosition].title.toString()
                    }


                }
                if (listItem.isNotEmpty() || listItemWT.isNotEmpty()) {

                    LogUtil.logE("AAjeCje", "orderDiscount  ${list[0].orderDiscount}")
                    LogUtil.logE("AAjeCje", "guestDiscount  ${guestDiscount}")
                    listner.onGuestPrint(
                        listItem,
                        guestName,
                        listItemWT,
                        MethodUtils.roundOffAmountDouble(guestSubTotal + list.get(0).wholeTableSubTotal - guestOrderDisShare),
                        MethodUtils.roundOffAmountDouble(finalAmt),
                        MethodUtils.roundOffAmountDouble(totalTaxAmt + list.get(0).wholeTableTax),
                        MethodUtils.roundOffAmountDouble(totalServiceCharge + list.get(0).wholeTableSurTax),
                        guestOrderDisShare + guestDiscount + list[0].wholeTableDiscont
                    )
                }


            }

            binding.llRemoveGuest.setOnClickListener {
                listner.onRemoveGuest(layoutPosition)
            }
        }

        init {


            binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->

                if (buttonView.isPressed) {
                    if (isChecked) {
                        var listItem: ArrayList<TbCartItem> = arrayListOf()
                        var listItemWithGuest:HashMap<String,ArrayList<TbCartItem>> = hashMapOf()


                        val builder = java.lang.StringBuilder()


                        for (i in layoutPosition + 1 until list.size) {

                            if (list.get(i).isHeader == 1) {
                                list[i].item?.let { listItem.add(it) }

                                list.get(i).item?.orderItemId?.let {
                                    builder.append(it)
                                    if (i + 1 != list.size) {
                                        builder.append(",")
                                    }
                                }
                                list.get(i).item?.isFired = true

                            } else {
                                break
                            }


                        }
                        if (builder.isNotEmpty()) {
                            listItemWithGuest.set(list.get(layoutPosition).title.toString(),listItem)

                            listner.onWholeTableToKitchen(builder.toString(), listItem,listItemWithGuest)
                        }
                        binding.chkIsFired.isChecked = true
                        binding.chkIsFired.isEnabled = false

                    }
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


    inner class MyViewHolder(private val binding: ViewDineInTableItemsBinding) :
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
                val adapter = DineInModifiersAdapter()
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

            if (model.item?.isFired == true) {
                binding.chkIsFired.isChecked = true
                binding.chkIsFired.isPressed = true
                binding.chkIsFired.isEnabled = false
                binding.ivWastage.visibility = View.VISIBLE
            } else {
                binding.chkIsFired.isChecked = false
                binding.chkIsFired.isEnabled = true
                binding.ivWastage.visibility = View.GONE
            }

            list[bindingAdapterPosition].item?.dineInSort = bindingAdapterPosition
            list[bindingAdapterPosition].item?.sort = bindingAdapterPosition
            if (list[bindingAdapterPosition].empName == "null" || list[bindingAdapterPosition].empName == null) {
                binding.txtEmpName.text = ""
            } else {
                binding.txtEmpName.text = list[bindingAdapterPosition].empName
            }
            binding.model = model.item
            binding.executePendingBindings()

            binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->

                if (buttonView.isPressed) {

                    if (isChecked) {
                        if (list.get(layoutPosition).item != null) {
                            var listItemWithGuest:HashMap<String,ArrayList<TbCartItem>> = hashMapOf()
                            var itemsNew = list[bindingAdapterPosition].item
                            var ids: String? = null
                            itemsNew?.orderItemId?.let { ids = it.toString() }
                            itemsNew?.isFired = true

                            for (i in bindingAdapterPosition downTo  0){
                                if (list.get(i).isHeader == 0){
                                    var listITems:ArrayList<TbCartItem> = arrayListOf()
                                    list[bindingAdapterPosition].item?.let { listITems.add(it) }
                                    listItemWithGuest.set(list.get(i).title.toString(),listITems)

                                    break

                                }

                            }


                            ids?.let {
                                list[bindingAdapterPosition].item?.let { it1 ->
                                    listner.singleItemFired(
                                        it, layoutPosition,
                                        it1,
                                        listItemWithGuest
                                    )
                                }
                            }
                            binding.chkIsFired.isEnabled = false
                            list[bindingAdapterPosition].item = itemsNew

                            //itemAdapter.updateCart(list[bindingAdapterPosition].items)


                        }

                    }
                }
            }

            binding.ivWastage.setOnClickListener {
                listner.onAddToWastage(layoutPosition, list[bindingAdapterPosition].item!!)
            }

            /* itemAdapter = DineInTableItemAdapter()
             binding.rvItems.adapter = itemAdapter
             itemAdapter.addCart(list[layoutPosition].items)
             itemAdapter.setCallback(this)
             var isItemFired: Boolean = true

             if (list[layoutPosition].items.isNotEmpty()) {
                 binding.txtTotal.visibility = View.VISIBLE
                 val total = list.get(layoutPosition).items
                 var sum = 0.0
                 if (total.isNotEmpty()) {
                     total.forEach {
                         sum += it.price * it.itemQuantity
                         if (it.modifiers.isNotEmpty()) {
                             it.modifiers.forEach {
                                 sum += it.price * it.itemQuantity
                             }
                         }

                     }

                     sum += list.get(0).guestDividedAmt
                     binding.txtTotal.setText("Total : ${MethodUtils.roundOffAmount(sum)}")
                 }
                 itemAdapter.cartList.forEach {
                     if (it.isFired) {
                         isItemFired = true
                     } else {
                         isItemFired = false
                         return@forEach
                     }
                 }

                 if (isItemFired) {
                     binding.chkIsFired.isChecked = true
                     binding.chkIsFired.isEnabled = false
                 } else {
                     binding.chkIsFired.isChecked = false
                     binding.chkIsFired.isEnabled = true
                 }


             } else {
                 binding.txtTotal.visibility = View.INVISIBLE
             }

             if (list.get(layoutPosition).items.isEmpty()) {
                 binding.btnPay.visibility = View.GONE
             }




             if (list.get(layoutPosition).customer != null) {
                 binding.txtTableName.setText(
                     list.get(layoutPosition).customer?.first_name + " " + list.get(
                         layoutPosition
                     ).customer?.last_name
                 )
             } else {
                 binding.txtTableName.setText(list.get(layoutPosition).title)

             }

             if (layoutPosition == 0) {
                 binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_simple_table))
                 binding.imgProfile.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY)
             } else {
                 binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_group_person))

             }
             if (list[layoutPosition].isPaid) {
                 binding.btnPay.visibility = View.GONE
                 binding.btnPaid.visibility = View.VISIBLE

             } else {
                 binding.btnPay.visibility = View.VISIBLE
                 binding.btnPaid.visibility = View.INVISIBLE
             }

             binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->
                 if (isChecked) {
                     if (itemAdapter.cartList.isNotEmpty()) {
                         var itemsNew = list[bindingAdapterPosition].items
                         val ids: MutableList<Int> = ArrayList()
                         for (i in 0 until itemsNew.size) {
                             itemsNew.get(i).orderItemId?.let { ids.add(it) }
                             itemsNew.get(i).isFired = true

                         }

                         var idStr = Gson().toJson(ids.toTypedArray())
                         LogUtil.logE(TAG, "idStr:  $idStr")
                         listner.onWholeTableToKitchen(idStr)
                         binding.chkIsFired.isChecked = true
                         binding.chkIsFired.isEnabled = false
                         list[bindingAdapterPosition].items = itemsNew

                         //itemAdapter.updateCart(list[bindingAdapterPosition].items)


                     }

                 }
             }


             if (layoutPosition == 0) {
                 binding.btnPay.visibility = View.GONE
                 binding.btnPaid.visibility = View.INVISIBLE
                 binding.txtTotal.visibility = View.INVISIBLE
             } else {
                 binding.txtTotal.visibility = View.VISIBLE
             }

         }

         init {
             binding.btnPay.setOnClickListener {
                 listner.onGuestPay(list[layoutPosition], layoutPosition)
             }*/
        }

        override fun onSendOrderToKitchen(item: TbCartItem) {

            listner.onSendItemToKitchen(item)
        }

    }

    fun setList(list: ArrayList<DineInModel>, isAnyPaymentDone: Boolean = false) {
        this.isAnyPaymentDone = isAnyPaymentDone
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
            listItemWT: ArrayList<TbCartItem>,
            listItemGuestSelected: ArrayList<TbCartItem>,
            guestIndexForDineIn: Int
        )

        fun onSendItemToKitchen(item: TbCartItem)
        fun onWholeTableToKitchen(ids: String, listItems: ArrayList<TbCartItem>,listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>)
        fun singleItemFired(id: String, position: Int, item: TbCartItem,listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>)
        fun onGuestPrint(
            listItem: ArrayList<TbCartItem>,
            guestName: String,
            listWTitems: ArrayList<TbCartItem>,
            subTotalGuest: Double,
            total: Double,
            taxGuest: Double,
            serviceChargeGuest: Double,
            divideDiscount: Double
        )

        fun onAddToWastage(position: Int, item: TbCartItem)
        fun onRemoveGuest(position: Int)
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
                        val order1: Int = list[i].item?.dineInSort?:toPosition
                        val order2: Int = list[i + 1].item?.dineInSort?:toPosition
                        list[i].item?.sort = order2
                        list[i].item?.dineInSort = order2
                        list[i + 1].item?.sort = order1
                        list[i + 1].item?.dineInSort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(list, i, i - 1)

                        val order1: Int = list[i].item?.dineInSort?:toPosition
                        val order2: Int = list[i - 1].item?.dineInSort?:toPosition
                        list[i].item?.sort = order2
                        list[i].item?.dineInSort = order2
                        list[i - 1].item?.sort = order1
                        list[i - 1].item?.dineInSort = order1
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
            if(list.isNotEmpty()) {
                list.forEach {
                    if (it.isHeader == 1) {
                        it.item?.isFired = true
                    } else {
                        it.isFired = true
                    }
                }
            }
        } else {
            if(list.isNotEmpty() && clickedPos < list.size){
                list[clickedPos].item?.isFired = true
            }
        }
        notifyDataSetChanged()
    }

    fun setSurchargeList(serviceChargeListt: java.util.ArrayList<TbServiceCharge>) {
        this.serviceChargeList = serviceChargeListt
        notifyDataSetChanged()
    }

}