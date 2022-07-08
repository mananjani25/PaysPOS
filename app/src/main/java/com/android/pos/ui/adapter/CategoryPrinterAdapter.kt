package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.databinding.ViewPrinterCategoryBinding

class CategoryPrinterAdapter() : RecyclerView.Adapter<CategoryPrinterAdapter.MyViewHolder>() {
    inner class MyViewHolder(val itemView:ViewPrinterCategoryBinding):RecyclerView.ViewHolder(itemView.root)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryPrinterAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPrinterCategoryBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoryPrinterAdapter.MyViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return 0


    }
}