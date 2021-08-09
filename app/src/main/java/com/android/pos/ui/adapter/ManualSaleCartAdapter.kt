package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.ManualSaleCartModel

import com.android.pos.databinding.ViewManualSaleItemBinding

class ManualSaleCartAdapter : RecyclerView.Adapter<ManualSaleCartAdapter.MyViewHolder>() {
    var list = ArrayList<ManualSaleCartModel>()


    inner class MyViewHolder(private val binding: ViewManualSaleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ManualSaleCartModel) {

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
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getList(): List<ManualSaleCartModel> {
        return this.list
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: ManualSaleCartModel) {
        this.list.apply {
            add(model)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(model: ManualSaleCartModel, position: Int) {
        list[position] = model
        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(cartList: List<ManualSaleCartModel>) {
        this.list.apply {
            clear()
            addAll(cartList)
        }
        notifyDataSetChanged()
    }
}