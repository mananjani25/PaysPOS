package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.ModifiersListModel
import com.android.pos.databinding.ViewModifiersBinding

class ModifiersListAdapter(val context: Context, val list: ArrayList<ModifiersListModel>) :
    RecyclerView.Adapter<ModifiersListAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding:ViewModifiersBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(item:ModifiersListModel){
            binding.model = item
            binding.executePendingBindings()
        }
        init {

        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifiersListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        var binding = ViewModifiersBinding.inflate(inflater,parent,false)
        return  MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifiersListAdapter.MyViewHolder, position: Int) {

        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }
}