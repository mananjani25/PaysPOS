package com.android.pos.ui.adapter.boldpos

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.utils.MethodUtils
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
            Log.e(TAG, "itemprice:  ${item.price}")
            binding.txtName.text = item.name
            binding.txtQuantity.text = "X" + item.itemQuantity
            binding.txtEachQntPrice.text = MethodUtils.roundOffAmount((item.price))
            binding.txtTotalPrice.text =
                MethodUtils.roundOffAmount((item.price * item.itemQuantity))


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

    fun clearList() {
        cartList.clear()
        cartList = arrayListOf()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return cartList.size
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

}