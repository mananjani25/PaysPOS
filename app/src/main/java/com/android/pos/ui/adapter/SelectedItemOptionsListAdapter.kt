package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Option
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.ViewCategoryBinding
import com.android.pos.databinding.ViewSelectedItemOptionListBinding
import java.util.*
import kotlin.collections.ArrayList

class SelectedItemOptionsListAdapter :
    RecyclerView.Adapter<SelectedItemOptionsListAdapter.MyViewHolder>() {
    var optionList = ArrayList<Option>()

    inner class MyViewHolder(private val binding: ViewSelectedItemOptionListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Option) {
            binding.model = item
            binding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewSelectedItemOptionListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(optionList[position])

    }

    override fun getItemCount(): Int {
        return optionList.size
    }

    fun addOptions(optionList: List<Option>) {
        this.optionList.apply {
          //  clear()
            addAll(optionList)
        }
        notifyDataSetChanged()
    }
}