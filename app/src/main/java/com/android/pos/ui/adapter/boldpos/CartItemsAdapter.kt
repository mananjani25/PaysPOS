package com.android.pos.ui.adapter.boldpos

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCartItem
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.ui.adapter.CartItemModifierAdapter
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.strike

class CartItemsAdapter : ListAdapter<TbCartItem, CartItemsAdapter.MyViewHolder>(callback) {
    private val TAG = "CartAdapter"

    companion object {
        val callback = object : DiffUtil.ItemCallback<TbCartItem>() {
            override fun areItemsTheSame(
                oldItem: TbCartItem,
                newItem: TbCartItem
            ) =
                oldItem.itemId == newItem.itemId

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: TbCartItem,
                newItem: TbCartItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    private lateinit var mCallback: MyCallback

    fun setCallback(callback: MyCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(val binding: ViewItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemsAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemCartBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartItemsAdapter.MyViewHolder, position: Int) {
        with(holder){
            val item = getItem(position)
            binding.txtName.text = item.name
            binding.txtQuantity.text = item.itemQuantity.toString()
            binding.txtEachQntPrice.text = MethodUtils.roundOffAmount((item.price))
            MethodUtils.setPriceTextView(binding.txtEachQntPrice, totalEachPrice(item))

            MethodUtils.setPriceTextView(binding.txtTotalPrice, totalPrice(item))

            if (item.discountPrice != 0.0) {
                binding.tvDiscountRate.visibility = View.VISIBLE
                binding.txtTotalPrice.strike = true
                var dPrice = 0.0
                var total_price_fordiscount = 0.0
                total_price_fordiscount += item.price * item.itemQuantity
                if (item.modifiers.isNotEmpty()) {
                    item.modifiers.forEach { it ->
                        total_price_fordiscount += it.price * it.itemQuantity
                    }
                }
                dPrice = total_price_fordiscount - (item.discountPrice * item.itemQuantity)

                MethodUtils.setPriceTextView(binding.tvDiscountRate, dPrice)
            } else {
                binding.txtTotalPrice.strike = false
                binding.tvDiscountRate.text = ""
                binding.tvDiscountRate.visibility = View.GONE
            }

            if (item.note.isEmpty()) {
                binding.txtNote.visibility = View.GONE
            } else {
                binding.txtNote.visibility = View.VISIBLE
                binding.txtNote.text = "Note: " + item.note
            }
            var updatedModifiers = item.modifiers.filter {
                !it._destroy
            } ?: arrayListOf()
            if (updatedModifiers.isNotEmpty()) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = CartItemModifierAdapter()
                binding.rvModifiers.adapter = adapter
                adapter.addAll(updatedModifiers)
            } else {
                binding.rvModifiers.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                mCallback.onCartItemClickListener(
                    it,
                    item,
                    bindingAdapterPosition
                )
            }
        }

    }

    private fun totalPrice(model: TbCartItem): Double {
        return model.price * model.itemQuantity
    }

    private fun totalEachPrice(model: TbCartItem): Double {
        return model.price * 1
    }

}