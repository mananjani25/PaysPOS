package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryParentModel
import com.android.pos.databinding.ViewParentCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1

class CategoryParentAdapter(
    val context: Context,
    var list: ArrayList<CategoryParentModel>,
    val listner: CategoryTabAdapter1.TabListner
) : RecyclerView.Adapter<CategoryParentAdapter.MyViewHolder>(), CategoryTabAdapter1.TabListner {

    inner class MyViewHolder(private val binding: ViewParentCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: CategoryParentModel) {

            binding.rvCategory.adapter =
                CategoryAdapter(binding.root.context, model.list, this@CategoryParentAdapter)
            binding.rvCategory.isNestedScrollingEnabled = false

        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryParentAdapter.MyViewHolder {
        return MyViewHolder(
            ViewParentCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryParentAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onTabSelected(pos: Int) {

    }
}