package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.ViewCategoryBinding

class CategoriesListAdapter(private val isChoose: Boolean) :
    RecyclerView.Adapter<CategoriesListAdapter.MyViewHolder>() {
    private var mpos: Int = -2
    var categoryList = ArrayList<TbCategory>()

    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()

            if (isChoose && mpos == layoutPosition) {
                binding.imageCheck.setImageResource(R.drawable.ic_remove_circle)
            } else binding.imageCheck.setImageResource(R.drawable.ic_outline_circle)
        }

        init {

            if (isChoose) {
                binding.imageCheck.setImageResource(R.drawable.ic_outline_circle)
                binding.imageCheck.setOnClickListener {
                    mpos = layoutPosition
                    notifyDataSetChanged()
                }
            } else {
                binding.imageCheck.setImageResource(R.drawable.ic_arrow_forward)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(categoryList.get(position))

    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    fun add(categoryModel: List<TbCategory>) {
        this.categoryList = categoryModel as ArrayList<TbCategory>
        notifyDataSetChanged()
    }
}