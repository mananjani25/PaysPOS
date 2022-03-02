package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.ViewBoldCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1

class CategoryAdapter(
    val context: Context,
    var list: ArrayList<CategoryTabModel>,
    val listner: CategoryTabAdapter1.TabListner
) : RecyclerView.Adapter<CategoryAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ViewBoldCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    inner class MyViewHolder(private val binding: ViewBoldCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: CategoryTabModel) {
            binding.txtCategoryName.isSelected = model.isSelected
            binding.txtCategoryName.setText(model.title)

            binding.txtCategoryName.setOnClickListener {
                list.forEachIndexed { index, categoryTabModel ->
                    if (index == bindingAdapterPosition){
                        categoryTabModel.isSelected = true
                    }
                    else{
                        categoryTabModel.isSelected = false
                    }

                }
                notifyDataSetChanged()
            }


        }

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list.get(position))


    }

    override fun getItemCount(): Int {
        return list.size

    }
}