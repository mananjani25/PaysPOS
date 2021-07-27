package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.ViewItemBinding
import com.android.pos.databinding.ViewTeamItemBinding

class CustomerListAdapter(val context: Context, val list: ArrayList<CustomerModel>) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewTeamItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(model:CustomerModel){

            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomerListAdapter.MyViewHolder {
        val binding = ViewTeamItemBinding.inflate(LayoutInflater.from(context),parent,false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CustomerListAdapter.MyViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return list.size
    }
}