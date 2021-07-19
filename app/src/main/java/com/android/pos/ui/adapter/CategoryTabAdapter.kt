package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.ViewDashboardTabItemBinding

class CategoryTabAdapter(
    val context: Context,
    val list: ArrayList<CategoryTabModel>,
    val listner: TabListner
) :
    RecyclerView.Adapter<CategoryTabAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: ViewDashboardTabItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryTabModel) {
            binding.model = item
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener {
                listner.onTabSelected(layoutPosition)
                for (i in 0 until list.size) {
                    if (i == layoutPosition) {
                        list.get(i).isSelected = true
                    } else {
                        list.get(i).isSelected = false
                    }
                }

                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryTabAdapter.MyViewHolder {
        val binding =
            ViewDashboardTabItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryTabAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface TabListner {
        fun onTabSelected(pos: Int)
    }
}