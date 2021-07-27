package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.ViewCustomerListBinding
import com.android.pos.databinding.ViewItemBinding
import com.android.pos.databinding.ViewTeamItemBinding

class CustomerListAdapter(val context: Context, val list: ArrayList<CustomerModel>,val listner:CustomerInteface) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: CustomerModel) {
            binding.model = model
            binding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomerListAdapter.MyViewHolder {
        val binding = ViewCustomerListBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CustomerListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface CustomerInteface{
        fun onCustomerSelect(pos:Int)
    }
}