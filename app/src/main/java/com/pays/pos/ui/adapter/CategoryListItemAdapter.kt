package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbItem
import com.pays.pos.databinding.ViewAssignItemToCategoryBinding
import com.pays.pos.data.remote.Constants.ADD_VALUE
import com.pays.pos.data.remote.Constants.BALANCE_INQUIRY
import com.pays.pos.data.remote.Constants.SELL_CARD

class CategoryListItemAdapter :
    RecyclerView.Adapter<CategoryListItemAdapter.MyViewHolder>() {

    var inventory = ArrayList<TbItem>()
    var categoryName: String = ""
    var selectedIds = ArrayList<Int>()

    inner class MyViewHolder(private val binding: ViewAssignItemToCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()
            if(bindingAdapterPosition==0){
                binding.firstviewCategory.visibility = View.VISIBLE
            }else{
                binding.firstviewCategory.visibility = View.GONE
            }
            if (item.isChecked) {
                binding.imgCheck.setImageResource(R.drawable.ic_outline_radio_button_checked)
            } else {
                binding.imgCheck.setImageResource(R.drawable.ic_uncheck_circle)
            }


            binding.imgCheck.setOnClickListener {
                if (categoryName != "Default Category" || inventory[layoutPosition].categoryName != "Default Category") {
                    inventory[layoutPosition].isChecked = !inventory[layoutPosition].isChecked
                    if (inventory[layoutPosition].isChecked) {
                        selectedIds.add(inventory[layoutPosition].itemId)
                    } else {
                        selectedIds.remove(inventory.get(layoutPosition).itemId)
                    }
                    notifyItemChanged(layoutPosition)
                }
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
        val filteredList = inventory.filterNot {
            it?.name == SELL_CARD || it?.name == BALANCE_INQUIRY || it?.name == ADD_VALUE
        }
//        this.inventory = inventory as ArrayList<TbItem>
        this.inventory = ArrayList(filteredList.filterNotNull())

    }

    fun categoryName(categoryName: String){
        this.categoryName = categoryName
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

    fun getTbItemsList() : ArrayList<TbItem>{
        return inventory
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