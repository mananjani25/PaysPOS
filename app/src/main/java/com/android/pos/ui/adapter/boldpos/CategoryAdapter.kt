package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.ViewBoldCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.utils.MethodUtils

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
            binding.txtCategoryName.text = model.title
            var itename_price: StringBuffer = StringBuffer()
            if (model.title.length > 30) {
                itename_price.append(model.title.substring(0, 30) + "...")
                binding.txtCategoryName.text = itename_price
            } else {
                binding.txtCategoryName.text = model.title
            }
            binding.root.setOnClickListener {
                listner.onTabSelected(bindingAdapterPosition)
                list.forEachIndexed { index, categoryTabModel ->

                    categoryTabModel.isSelected = index == bindingAdapterPosition
                }
                notifyDataSetChanged()
            }


        }

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list[position])


    }

    override fun getItemCount(): Int {
        return list.size

    }
}