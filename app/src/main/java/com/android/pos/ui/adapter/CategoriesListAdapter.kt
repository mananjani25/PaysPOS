package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.ViewCategoryBinding
import com.android.pos.utils.statusUtils.Resource

class CategoriesListAdapter() :
    RecyclerView.Adapter<CategoriesListAdapter.MyViewHolder>() {
    var categoryList = ArrayList<TbCategory>()

    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(categoryList.get(position))

    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    fun add(categoryModel: List<TbCategory>) {
        this.categoryList = categoryModel as ArrayList<TbCategory>
        notifyDataSetChanged()
    }
}