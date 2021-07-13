package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.DiscountListModel
import com.android.pos.databinding.ViewDiscountItemBinding

class DiscountListAdapter(val context: Context, val list: ArrayList<DiscountListModel>) :
    RecyclerView.Adapter<DiscountListAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: ViewDiscountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DiscountListModel) {
            binding.model = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiscountListAdapter.MyViewHolder {
        val binding = ViewDiscountItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DiscountListAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

}