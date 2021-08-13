package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewCartItemBinding
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback

class CartAdapter : RecyclerView.Adapter<CartAdapter.MyViewHolder>() {

    var cartList = ArrayList<TbItem>()

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
        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbItem {
        return cartList[position]
    }

    override fun onBindViewHolder(holder: CartAdapter.MyViewHolder, position: Int) {
        holder.bind(cartList[position])
    }

    override fun getItemCount(): Int {
        return cartList.size

    }

    inner class MyViewHolder(val binding: ViewCartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()

            MethodUtils.setPriceTextView(binding.tvRate, totalPrice(item))

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = CartItemModifierAdapter()
                binding.rvModifiers.adapter = adapter
                adapter.addAll(item.modifiers)
            } else {
                binding.rvModifiers.visibility = View.GONE
            }
        }

        init {

            binding.root.setOnClickListener {
                mCallback.onItemClickListener(it, cartList[bindingAdapterPosition])
            }
        }
    }

    fun removeIitem(pos: Int) {
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