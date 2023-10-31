package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.ui.adapter.CartItemModifierAdapter
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.strike
import okhttp3.internal.notify

class CartAdapter : RecyclerView.Adapter<CartAdapter.MyViewHolder>() {
    var cartList = ArrayList<TbCartItem>()
    private val TAG = "CartAdapter"


    private lateinit var mCallback: MyCallback

    fun setCallback(callback: MyCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCartItem, pos: Int) {
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


        }

        init {

            binding.root.setOnClickListener {
                mCallback.onItemClickListener(
                    it,
                    cartList[bindingAdapterPosition],
                    bindingAdapterPosition
                )
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemCartBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CartAdapter.MyViewHolder, position: Int) {
        holder.bind(cartList[position], position)

    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }

    fun setList(list: ArrayList<TbCartItem>) {
        LogUtil.logE(TAG, "itemListSize ${list.size}")
        cartList = list
        notifyDataSetChanged()

//        cartList.clear()
//        cartList = arrayListOf()
//        cartList.addAll(list)
//        notifyDataSetChanged()
    }

    fun clearList() {
        cartList.clear()
        cartList = arrayListOf()
        notifyDataSetChanged()
    }

    fun addItemInList(item: TbCartItem){
        cartList.add(item)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return cartList.size
    }

    fun addCart(mList: ArrayList<TbCartItem>?) {
        if (mList != null) {
            cartList = mList
        }
        val it: MutableIterator<TbCartItem> = cartList.iterator()

        while (it.hasNext()) {
            val s: TbCartItem = it.next()
            if (s.isDestroy) {
                it.remove()
            }
        }

        notifyDataSetChanged()
    }

    private fun totalPrice(model: TbCartItem): Double {
        return model.price * model.itemQuantity
    }


    private fun totalEachPrice(model: TbCartItem): Double {
        return model.price * 1
    }


}