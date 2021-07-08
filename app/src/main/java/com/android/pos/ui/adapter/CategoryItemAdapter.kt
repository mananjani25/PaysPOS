package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.databinding.ViewDashboardItemBinding

class CategoryItemAdapter(val context: Context,val list:ArrayList<DashboardItemModel>,val listner:CategoryItemList):RecyclerView.Adapter<CategoryItemAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding:ViewDashboardItemBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(item:DashboardItemModel){
            binding.viewModel = item
            binding.executePendingBindings()
        }
        init {

            binding.root.setOnClickListener {
                listner.onClick()
            }
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryItemAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewDashboardItemBinding.inflate(inflater,parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryItemAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }
    interface CategoryItemList{
        fun onClick()
    }
}