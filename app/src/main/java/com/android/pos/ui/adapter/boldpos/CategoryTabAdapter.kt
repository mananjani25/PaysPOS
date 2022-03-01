package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.ViewCategoryTablayoutBinding

class CategoryTabAdapter(var list: ArrayList<CategoryTabModel>) :
    RecyclerView.Adapter<CategoryTabAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewCategoryTablayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: CategoryTabModel) {
            binding.viewLine.background =
                if (model.isSelected) binding.root.context.resources.getDrawable(R.color.btnColorDark) else binding.root.context.resources.getDrawable(
                    R.color.switchTrackColor
                )
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ViewCategoryTablayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list.get(position))

    }

    override fun getItemCount(): Int {
        return list.size
    }
}