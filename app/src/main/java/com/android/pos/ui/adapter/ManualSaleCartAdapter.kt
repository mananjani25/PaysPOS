package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.ManualSaleCartModel

import com.android.pos.databinding.ViewManualSaleItemBinding

class ManualSaleCartAdapter : RecyclerView.Adapter<ManualSaleCartAdapter.MyViewHolder>() {
    var list = ArrayList<TbItem>()
    private lateinit var listnerCall: ManualSaleInterface


    inner class MyViewHolder(private val binding: ViewManualSaleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val txtItem: TextView = binding.root.findViewById(R.id.txtItem)
        fun bind(model: TbItem, pos: Int) {
            if (pos == list.size - 1) {
                binding.txtQuantity.setText("x 1")
            } else {
                binding.txtQuantity.setText("x ${model.itemQuantity}")
            }

            txtItem.setText(list[pos].name)
            binding.model = model
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener {
                if (layoutPosition != list.size - 1) {
                    listnerCall.onItemClicked(list.get(layoutPosition), layoutPosition)
                }
            }

        }


    }

    fun setCallBack(listner: ManualSaleInterface) {
        this.listnerCall = listner
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
        Log.e("TbListSize", "TbListSize ${list.size}")
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

    fun getItem(position: Int): TbItem {
        return list[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        this.list.clear()
        notifyDataSetChanged()
    }

    interface ManualSaleInterface {
        fun onItemClicked(model: TbItem, position: Int)
    }
}