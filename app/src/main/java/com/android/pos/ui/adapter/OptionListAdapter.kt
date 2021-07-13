package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.OptionListModel
import com.android.pos.databinding.ViewOptionListBinding

class OptionListAdapter(val context: Context, val list: ArrayList<OptionListModel>) :
    RecyclerView.Adapter<OptionListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewOptionListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OptionListModel) {
            binding.model = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OptionListAdapter.MyViewHolder {
        val binding = ViewOptionListBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OptionListAdapter.MyViewHolder, position: Int) {

        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}