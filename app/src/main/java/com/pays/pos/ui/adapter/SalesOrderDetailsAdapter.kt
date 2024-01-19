package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.EodReportResponse
import com.pays.pos.databinding.ViewSalesDetailsDataBinding

class SalesOrderDetailsAdapter :
    RecyclerView.Adapter<SalesOrderDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<EodReportResponse.Data.OrderSalesDetails.Details>()

    inner class MyViewHolder(val binding: ViewSalesDetailsDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: EodReportResponse.Data.OrderSalesDetails.Details, position: Int) {
            binding.model = model
            binding.executePendingBindings()
        }
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
        holder.bind(arrayList.get(position), position)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<EodReportResponse.Data.OrderSalesDetails.Details>) {
        this.arrayList = arrayList as ArrayList<EodReportResponse.Data.OrderSalesDetails.Details>
        notifyDataSetChanged()
    }
}