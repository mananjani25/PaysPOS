package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.EodReportResponse
import com.android.pos.databinding.ViewSalesDetailsDataBinding

class SalesOrderDetailsAdapter :
    RecyclerView.Adapter<SalesOrderDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<EodReportResponse.Data.OrderSalesDetails.Data>()

    inner class MyViewHolder(val binding: ViewSalesDetailsDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSalesDetailsDataBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.binding
        itemBinding.model = arrayList[position]
        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<EodReportResponse.Data.OrderSalesDetails.Data>) {
        this.arrayList = arrayList as ArrayList<EodReportResponse.Data.OrderSalesDetails.Data>
        notifyDataSetChanged()
    }
}