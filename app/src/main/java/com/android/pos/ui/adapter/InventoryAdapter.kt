package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.databinding.ViewInventoryItemsBinding

class InventoryAdapter(val context: Context, val list: ArrayList<InventoryItemModel>) :
    RecyclerView.Adapter<InventoryAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewInventoryItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItemModel) {
            binding.model = item
            binding.executePendingBindings()
        }

        init {

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InventoryAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewInventoryItemsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }
}