package com.pays.pos.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.opengl.Visibility
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.InventoryItemModel
import com.pays.pos.databinding.ViewInventoryItemsBinding
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class InventoryAdapter(
    val context: Context,
    val list: ArrayList<InventoryItemModel>,
    val isHide: Boolean,
    val listener: InventoryListner
) :
    RecyclerView.Adapter<InventoryAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewInventoryItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItemModel) {
            binding.model = item
            if (absoluteAdapterPosition == 0) {


                binding.firstview.visibility = View.VISIBLE
            } else {
                binding.firstview.visibility = View.GONE
            }


            if (item.isSelected) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.linearBackgroundOrder.setBackgroundColor(
                        binding.root.context.getColor(
                            R.color.txt_color_blue
                        )
                    )
                    binding.txtTitle.setTextColor(binding.root.context.getColor(R.color.white))
                    binding.txtCount.setTextColor(binding.root.context.getColor(R.color.white))
                }
                binding.txtTitle.setTypeface(binding.txtTitle.typeface, Typeface.BOLD)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.linearBackgroundOrder.setBackgroundColor(
                        binding.root.context.getColor(
                            R.color.bg_color
                        )
                    )
                    binding.txtTitle.setTextColor(binding.root.context.getColor(R.color.txtColor))
                    binding.txtCount.setTextColor(binding.root.context.getColor(R.color.txtColor))
                }
                binding.txtTitle.setTypeface(binding.txtTitle.typeface, Typeface.NORMAL)
            }
            binding.executePendingBindings()

        }

        init {
            binding.root.setOnClickListener {
                listener.onItemSelect(layoutPosition)

                Log.e("MENU ITEM ","MENU ITEM CLICKED ${list[layoutPosition].title}")

                for (i in 0 until list.size) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        list[i].isSelected = i == layoutPosition
                    }
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