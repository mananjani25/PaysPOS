package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewAssignItemToCategoryBinding

class CategoryListItemAdapter :
    RecyclerView.Adapter<CategoryListItemAdapter.MyViewHolder>() {

    var inventory = ArrayList<TbItem>()

    inner class MyViewHolder(private val binding: ViewAssignItemToCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryListItemAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewAssignItemToCategoryBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoryListItemAdapter.MyViewHolder, position: Int) {
        holder.bind(inventory[position])

    }

    override fun getItemCount(): Int {
        return inventory.size
    }

    fun add(inventory: List<TbItem?>) {
        this.inventory = inventory as ArrayList<TbItem>
    }

    /* fun add(categoryModel: List<TbCategory>) {
         this.categoryList = categoryModel as ArrayList<TbCategory>
         this.filterList = categoryModel
         notifyDataSetChanged()
     }*/
}