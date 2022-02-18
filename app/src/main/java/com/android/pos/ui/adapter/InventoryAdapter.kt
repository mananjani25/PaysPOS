package com.android.pos.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.databinding.ViewInventoryItemsBinding

class InventoryAdapter(
    val context: Context,
    val list: ArrayList<InventoryItemModel>,
    val listener: InventoryListner
) :
    RecyclerView.Adapter<InventoryAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewInventoryItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItemModel) {
            binding.model = item
            if(absoluteAdapterPosition==0){
                binding.firstview.visibility = View.VISIBLE
            }else{
                binding.firstview.visibility = View.GONE
            }
            if(item.isSelected){
                binding.txtTitle.setTypeface(binding.txtTitle.typeface,Typeface.BOLD)
            }else{
                binding.txtTitle.setTypeface(binding.txtTitle.typeface,Typeface.NORMAL)
            }
            binding.executePendingBindings()

        }

        init {
            binding.root.setOnClickListener {

                listener.onItemSelect(layoutPosition)
                for (i in 0 until list.size) {
                    list[i].isSelected = i == layoutPosition
                }

                notifyDataSetChanged()
            }

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

    interface InventoryListner {
        fun onItemSelect(position: Int)
    }
}