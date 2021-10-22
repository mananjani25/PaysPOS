package com.android.pos.ui.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.databinding.ViewDineInOrderTableBinding
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson

class DineInTableAdapter : RecyclerView.Adapter<DineInTableAdapter.MyViewHolder>() {
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
    ): DineInTableAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInOrderTableBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DineInTableAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInOrderTableBinding) :
        RecyclerView.ViewHolder(binding.root), DineInTableItemAdapter.DineInItemListner {
        fun bind(model: DineInModel) {

            itemAdapter = DineInTableItemAdapter()
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
            }
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
        return position
    }


}