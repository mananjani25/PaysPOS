package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.ManualSaleCartModel

import com.android.pos.databinding.ViewManualSaleItemBinding

class ManualSaleCartAdapter : RecyclerView.Adapter<ManualSaleCartAdapter.MyViewHolder>() {
    var list = ArrayList<TbItem>()


    inner class MyViewHolder(private val binding: ViewManualSaleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: TbItem, pos: Int) {

            binding.model = model
            binding.executePendingBindings()
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ManualSaleCartAdapter.MyViewHolder {
        val binding =
            ViewManualSaleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ManualSaleCartAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getList(): List<TbItem> {
        return this.list
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: TbItem) {
        this.list.add(model)
        Log.e("TbListSize","TbListSize ${list.size}")
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(model: TbItem, position: Int) {
        list[position] = model
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(cartList: List<TbItem>?) {
        this.list = cartList as ArrayList<TbItem>
        notifyDataSetChanged()
    }
    fun getItem(position: Int):TbItem{
        return list[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList(){
        this.list.clear()
        notifyDataSetChanged()
    }
}