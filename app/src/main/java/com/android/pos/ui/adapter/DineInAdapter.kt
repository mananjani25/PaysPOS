package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.DineInModel
import com.android.pos.databinding.ViewDineInItemBinding

class DineInAdapter : RecyclerView.Adapter<DineInAdapter.MyViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DineInAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DineInAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DineInModel) {
            binding.model = model
            binding.executePendingBindings()
        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        this.list = list
    }
}