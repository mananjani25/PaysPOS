package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.databinding.ViewCategoryVerticalBinding


class DashboardItemAdapter(
    val context: Context,
    val list: ArrayList<String>,
    val listner: DashboardListner
) :
    RecyclerView.Adapter<DashboardItemAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: ViewCategoryVerticalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String) {
            binding.viewModel = item
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener {
                listner.onItemClick(layoutPosition)
            }
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewCategoryVerticalBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
         holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface DashboardListner {
        fun onItemClick(layoutPosition: Int)
    }
}