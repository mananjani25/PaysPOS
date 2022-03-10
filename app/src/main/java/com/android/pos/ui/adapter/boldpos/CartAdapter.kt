package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.utils.callback.MyCallback

class CartAdapter : RecyclerView.Adapter<CartAdapter.MyViewHolder>() {
    var cartList = ArrayList<TbItem>()
    private val TAG = "CartAdapter"


    private lateinit var mCallback: MyCallback

    fun setCallback(callback: MyCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem, pos: Int) {


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

    fun setList(list: ArrayList<TbItem>) {
        cartList.clear()
        cartList = arrayListOf()
        cartList.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return cartList.size
    }
}