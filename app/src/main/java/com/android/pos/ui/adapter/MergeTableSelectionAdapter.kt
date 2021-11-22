package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.databinding.ViewMergeTableListBinding

class MergeTableSelectionAdapter : RecyclerView.Adapter<MergeTableSelectionAdapter.MyViewHolder>() {
    private var list: ArrayList<MergeTableListModel> = arrayListOf()


    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: ArrayList<MergeTableListModel>) {
        this.list = list
        notifyDataSetChanged()

    }


    inner class MyViewHolder(private val binding: ViewMergeTableListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: MergeTableListModel) {

            if (layoutPosition != 0) {
                binding.imgDelete.visibility = View.VISIBLE
            } else {
                binding.imgDelete.visibility = View.GONE
            }


        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MergeTableSelectionAdapter.MyViewHolder {
        val binding =
            ViewMergeTableListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MergeTableSelectionAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size

    }
}