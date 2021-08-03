package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.databinding.ViewCustomerAssignOrderBinding

class AssignCustomerToOrderAdapter :
    RecyclerView.Adapter<AssignCustomerToOrderAdapter.MyViewHolder>() {

    private var mList = ArrayList<CustomerListResponse.Data>()
    private var filterList = ArrayList<CustomerListResponse.Data>()

    fun add(categoryModel: List<CustomerListResponse.Data>) {
        this.mList = categoryModel as ArrayList<CustomerListResponse.Data>
        this.filterList = categoryModel
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ViewCustomerAssignOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CustomerListResponse.Data) {
            binding.model = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AssignCustomerToOrderAdapter.MyViewHolder {
        val binding =
            ViewCustomerAssignOrderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(
        holder: AssignCustomerToOrderAdapter.MyViewHolder,
        position: Int
    ) {

        holder.bind(filterList[position])
    }

    override fun getItemCount(): Int {
        return filterList.size
    }
}