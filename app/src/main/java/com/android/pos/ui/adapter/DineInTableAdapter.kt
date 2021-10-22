package com.android.pos.ui.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.databinding.*
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

class DineInTableAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private lateinit var itemAdapter: DineInTableItemAdapter
    private lateinit var listner: DineInTableListner
    private val TAG = "DineInTableAdapter"


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
            (holder as HeaderViewHolder).bind(list.get(position))
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    inner class HeaderViewHolder(private val binding: ViewDineInHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: DineInModel) {

            if (list.get(layoutPosition).customer != null) {
                binding.txtTableName.setText(
                    list.get(layoutPosition).customer?.first_name + " " + list.get(
                        layoutPosition
                    ).customer?.last_name
                )
            } else {
                binding.txtTableName.setText(list.get(layoutPosition).title)

            }

            if (list[layoutPosition].isPaid) {
                binding.btnPay.visibility = View.GONE
                binding.btnPaid.visibility = View.VISIBLE

            } else {
                binding.btnPay.visibility = View.VISIBLE
                binding.btnPaid.visibility = View.INVISIBLE
            }
            if (layoutPosition == 0) {
                binding.btnPay.visibility = View.GONE
                binding.btnPaid.visibility = View.INVISIBLE
                binding.txtTotal.visibility = View.INVISIBLE
            } else {
                binding.txtTotal.visibility = View.VISIBLE
            }

            binding.txtTotal.setText("" + MethodUtils.roundOffAmountDouble(list[layoutPosition].totalGuestPrice))

        }

        init {
            binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->
                Log.e(TAG, "NextITemIndax  ${layoutPosition + 1}")

                if (isChecked) {
                    val ids: MutableList<Int> = ArrayList()
                    for (i in layoutPosition + 1 until list.size) {

                        if (list.get(i).isHeader == 1) {
                            list.get(i).item?.orderItemId?.let { ids.add(it) }

                        } else {
                            break
                        }


                    }
                    var idStr = Gson().toJson(ids.toTypedArray())
                    Log.e(TAG, "idStr:  ${Gson().toJson(idStr)}")
                    listner.onWholeTableToKitchen(idStr)
                    binding.chkIsFired.isChecked = true
                    binding.chkIsFired.isEnabled = false
                    // list[bindingAdapterPosition].item =


                    if (list.get(layoutPosition).item != null) {
                        var itemsNew = list[bindingAdapterPosition].item
                        val ids: MutableList<Int> = ArrayList()
                        itemsNew?.orderItemId?.let { ids.add(it) }
                        itemsNew?.isFired = true


                        var idStr = Gson().toJson(ids.toTypedArray())
                        Log.e(TAG, "idStr:  $idStr")
                        listner.onWholeTableToKitchen(idStr)
                        binding.chkIsFired.isChecked = true
                        binding.chkIsFired.isEnabled = false
                        list[bindingAdapterPosition].item = itemsNew

                        //itemAdapter.updateCart(list[bindingAdapterPosition].items)


                    }
                }


            }

            binding.btnPay.setOnClickListener {
                Log.e(TAG, "OnPayClicked")
                listner.onGuestPay(list[layoutPosition], layoutPosition)
            }

        }
    }


    inner class MyViewHolder(private val binding: ViewDineInTableItemsBinding) :
        RecyclerView.ViewHolder(binding.root), DineInTableItemAdapter.DineInItemListner {
        fun bind(model: DineInModel) {

            if (model.item?.discountPrice != 0.0) {
                binding.tvDiscountRate.visibility = View.VISIBLE
                binding.tvRate.paintFlags =
                    binding.tvRate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                Log.e(TAG, "PriceOriginalTotal  ${model.item?.let { totalPrice(it) }}")
                Log.e(TAG, "PriceDiscounted  ${model.item?.discountPrice}")
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
                binding.chkIsFired.isEnabled = false
            } else {
                binding.chkIsFired.isChecked = false
                binding.chkIsFired.isEnabled = true

            }


            binding.model = model.item
            binding.executePendingBindings()

            binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (list.get(layoutPosition).item != null) {
                        var itemsNew = list[bindingAdapterPosition].item
                        val ids: MutableList<Int> = ArrayList()
                        itemsNew?.orderItemId?.let { ids.add(it) }
                        itemsNew?.isFired = true


                        var idStr = Gson().toJson(ids.toTypedArray())
                        Log.e(TAG, "idStr:  $idStr")
                        listner.onWholeTableToKitchen(idStr)
                        binding.chkIsFired.isChecked = true
                        binding.chkIsFired.isEnabled = false
                        list[bindingAdapterPosition].item = itemsNew

                        //itemAdapter.updateCart(list[bindingAdapterPosition].items)


                    }

                }
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
                         Log.e(TAG, "idStr:  $idStr")
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

        override fun onSendOrderToKitchen(item: TbItem) {
            Log.e(TAG, "onItwdetewt  ${Gson().toJson(item)}")
            listner.onSendItemToKitchen(item)
        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    interface DineInTableListner {
        fun onGuestPay(dineInModel: DineInModel, position: Int)
        fun onSendItemToKitchen(item: TbItem)
        fun onWholeTableToKitchen(ids: String)
    }

    fun getList(): List<DineInModel> {
        return list
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].isHeader
    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
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

}