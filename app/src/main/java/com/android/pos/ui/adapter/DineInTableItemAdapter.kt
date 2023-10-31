package com.android.pos.ui.adapter

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewDineInTableItemsBinding
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback

class DineInTableItemAdapter : RecyclerView.Adapter<DineInTableItemAdapter.MyViewHolder>() {
    var cartList = ArrayList<TbCartItem>()
    private val TAG = "CartAdapter"

    private lateinit var mCallback: DineInItemListner

    fun setCallback(callback: DineInItemListner) {
        mCallback = callback
    }


    inner class MyViewHolder(private val binding: ViewDineInTableItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TbCartItem, pos: Int) {
            if (item.discountPrice != 0.0) {
                binding.tvDiscountRate.visibility = View.VISIBLE
                binding.tvRate.paintFlags = binding.tvRate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                LogUtil.logE(TAG, "PriceOriginalTotal  ${totalPrice(item)}")
                LogUtil.logE(TAG, "PriceDiscounted  ${item.discountPrice}")
                val dPrice = totalPrice(item) - item.discountPrice
                MethodUtils.setPriceTextView(binding.tvDiscountRate, dPrice)
            } else {
                binding.tvRate.paintFlags = 0
                binding.tvDiscountRate.text = ""
                binding.tvDiscountRate.visibility = View.GONE

            }


            MethodUtils.setPriceTextView(binding.tvRate, totalPrice(item))

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = DineInModifiersAdapter()
                binding.rvModifiers.adapter = adapter
                adapter.addAll(item.modifiers)
            } else {
                binding.rvModifiers.visibility = View.GONE
            }

            if (item.note.isEmpty()) {
                binding.txtNote.visibility = View.GONE
            } else {
                binding.txtNote.visibility = View.VISIBLE
            }

            if (item.isFired) {
                binding.chkIsFired.isChecked = true
                binding.chkIsFired.isEnabled = false
            } else {
                binding.chkIsFired.isChecked = false
                binding.chkIsFired.isEnabled = true

            }


            binding.model = item
            binding.executePendingBindings()
        }

        init {

            binding.chkIsFired.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    mCallback.onSendOrderToKitchen(cartList[layoutPosition])
                    binding.chkIsFired.isChecked = true
                    binding.chkIsFired.isEnabled = false
                }

            }

            binding.root.setOnClickListener {
//                mCallback.onItemClickListener(it, cartList[bindingAdapterPosition], layoutPosition)
            }
        }

    }

    fun addCart(mList: List<TbCartItem>?) {
        cartList = mList as ArrayList<TbCartItem>
        notifyDataSetChanged()
    }

    fun updateCart(mList: List<TbCartItem?>) {
        cartList.clear()
        cartList = mList as ArrayList<TbCartItem>
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DineInTableItemAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInTableItemsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DineInTableItemAdapter.MyViewHolder, position: Int) {
        holder.bind(cartList[position], position)

    }

    override fun getItemCount(): Int {
        return cartList.size
    }

    fun removeItem(pos: Int) {
        this.cartList.removeAt(pos)
        notifyItemRemoved(pos)
    }

    private fun totalPrice(model: TbCartItem): Double {

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

    interface DineInItemListner {
        fun onSendOrderToKitchen(item: TbCartItem)
    }

}