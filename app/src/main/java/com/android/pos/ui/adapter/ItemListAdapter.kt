package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemBinding

class ItemListAdapter() :
    RecyclerView.Adapter<ItemListAdapter.MyViewHolder>() {
    var itemsList = ArrayList<TbItem>()

    inner class MyViewHolder(private val binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemListAdapter.MyViewHolder, position: Int) {
        holder.bind(itemsList[position])

    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun add(categoryModel: List<TbItem>) {
        this.itemsList = categoryModel as ArrayList<TbItem>
        notifyDataSetChanged()
    }
}
