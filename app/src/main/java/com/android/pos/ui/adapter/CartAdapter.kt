package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewCartItemBinding
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
        }

        init {

            binding.root.setOnClickListener {
                mCallback.onItemClickListener(it, cartList[bindingAdapterPosition])
            }
        }
    }

}