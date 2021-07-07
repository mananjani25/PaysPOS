package com.android.pos.ui.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.databinding.ViewAssignItemToCategoryBinding

class CategoryListItemAdapter(val context: Context, val list: ArrayList<CategoryListItemModel>) :
    RecyclerView.Adapter<CategoryListItemAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: ViewAssignItemToCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryListItemModel) {
            binding.model = item
            binding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryListItemAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewAssignItemToCategoryBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoryListItemAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size
    }
}