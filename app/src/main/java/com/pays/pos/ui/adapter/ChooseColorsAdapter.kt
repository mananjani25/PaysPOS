package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.OptionListModel
import com.pays.pos.databinding.ViewItemTitleEditBinding
import com.pays.pos.databinding.ViewOptionListBinding

class ChooseColorsAdapter(val list: ArrayList<OptionListModel>) :
    RecyclerView.Adapter<ChooseColorsAdapter.MyViewHolder>() {

    private var mpos: Int = -1

    inner class MyViewHolder(private val binding: ViewItemTitleEditBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OptionListModel) {
            binding.model = item
            binding.executePendingBindings()
            binding.layout.setBackgroundResource(item.id)

            if (mpos == layoutPosition) {
                binding.imageCheck.visibility = View.VISIBLE
            } else {
                binding.imageCheck.visibility = View.GONE
            }

            binding.root.setOnClickListener {

                mpos = layoutPosition
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChooseColorsAdapter.MyViewHolder {
        val binding =
            ViewItemTitleEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ChooseColorsAdapter.MyViewHolder, position: Int) {

        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}