package com.android.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewAssignItemToCategoryBinding

class CategoryListItemAdapter :
    RecyclerView.Adapter<CategoryListItemAdapter.MyViewHolder>() {

    var inventory = ArrayList<TbItem>()
    var selectedIds = ArrayList<Int>()

    inner class MyViewHolder(private val binding: ViewAssignItemToCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()

            if (item.isChecked) {
                binding.imgCheck.setImageResource(R.drawable.ic_outline_radio_button_checked)
            } else {
                binding.imgCheck.setImageResource(R.drawable.ic_uncheck_circle)
            }


            binding.imgCheck.setOnClickListener {
                inventory[layoutPosition].isChecked = !inventory[layoutPosition].isChecked
                if (inventory[layoutPosition].isChecked) {
                    selectedIds.add(inventory[layoutPosition].itemId)
                } else {
                    selectedIds.remove(inventory.get(layoutPosition).itemId)
                }
                notifyItemChanged(layoutPosition)

                Log.e("selectedIds", selectedIds.toString())
            }
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

    fun getIds(): ArrayList<Int> {
        return selectedIds
    }

    override fun getItemCount(): Int {
        return inventory.size
    }

    fun add(inventory: List<TbItem?>) {
        this.inventory = inventory as ArrayList<TbItem>
    }

    fun selectedItemFromEdit(itemIds: List<Int>) {

        inventory.forEach { TbItem ->
            itemIds.forEach {
                if (TbItem.itemId == it) {
                    TbItem.isChecked = true
                    selectedIds.add(TbItem.itemId)
                }
            }
        }
        notifyDataSetChanged()

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
    /* fun add(categoryModel: List<TbCategory>) {
         this.categoryList = categoryModel as ArrayList<TbCategory>
         this.filterList = categoryModel
         notifyDataSetChanged()
     }*/
}