package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.AllItemModel
import com.android.pos.databinding.ViewItemBinding

class ItemListAdapter(val context:Context,val list:ArrayList<AllItemModel>):RecyclerView.Adapter<ItemListAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding:ViewItemBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(item:AllItemModel){

        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewItemBinding.inflate(inflater,parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemListAdapter.MyViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return list.size
    }
}