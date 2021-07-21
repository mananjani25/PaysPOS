package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.CategoryListModel
import com.android.pos.databinding.ViewCategoryBinding

class CategoriesListAdapter(val context: Context, var list: ArrayList<TbCategory>) :
    RecyclerView.Adapter<CategoriesListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()

        }

        init {

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoriesListAdapter.MyViewHolder {
        val binding = ViewCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoriesListAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(categoryModel: List<TbCategory?>) {
        list = categoryModel as ArrayList<TbCategory>
        notifyDataSetChanged()
    }
}