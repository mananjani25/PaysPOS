package com.pays.pos.ui.adapter

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbItem
import com.pays.pos.databinding.ViewCartItemBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.callback.MyCallback

class CartAdapter : RecyclerView.Adapter<CartAdapter.MyViewHolder>() {

    var cartList = ArrayList<TbItem>()
    private val TAG = "CartAdapter"

    private lateinit var mCallback: MyCallback

    fun setCallback(callback: MyCallback) {
        mCallback = callback
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCartItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addCart(mList: List<TbItem>?) {
        cartList = mList as ArrayList<TbItem>
        val it: MutableIterator<TbItem> = cartList.iterator()

        while (it.hasNext()) {
            val s: TbItem = it.next()
            if (s.isDestroy) {
                it.remove()
            }
        }

        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbItem {
        return cartList[position]
    }

    override fun onBindViewHolder(holder: CartAdapter.MyViewHolder, position: Int) {
        holder.bind(cartList[position], position)
    }

    override fun getItemCount(): Int {
        return cartList.size

    }

    inner class MyViewHolder(val binding: ViewCartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem, pos: Int) {

            if (item.discountPrice != 0.0) {
                binding.tvDiscountRate.visibility = View.VISIBLE
                binding.tvRate.paintFlags = binding.tvRate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                var dPrice = 0.0

                dPrice = totalPrice(item) - (item.discountPrice * item.itemQuantity)

                MethodUtils.setPriceTextView(binding.tvDiscountRate, dPrice)
            } else {
                binding.tvRate.paintFlags = 0
                binding.tvDiscountRate.text = ""
                binding.tvDiscountRate.visibility = View.GONE

            }

            MethodUtils.setPriceTextView(binding.tvRate, totalPrice(item))

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = CartItemModifierAdapter()
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

            LogUtil.logE("itemQuantity", "" + item.itemQuantity)
            binding.model = item
            binding.executePendingBindings()
        }

        init {

            binding.root.setOnClickListener {
                if (bindingAdapterPosition >= 0 && cartList.size > 0 && bindingAdapterPosition < cartList.size) {
                    /*mCallback.onItemClickListener(
                        it,
                        cartList[bindingAdapterPosition],
                        layoutPosition
                    )*/
                }
            }
        }
    }

    fun removeItem(pos: Int) {
        this.cartList.removeAt(pos)
        notifyItemRemoved(pos)
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


}