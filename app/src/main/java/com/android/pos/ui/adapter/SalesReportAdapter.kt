package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.databinding.ViewSalesReportBinding

class SalesReportAdapter :
    RecyclerView.Adapter<SalesReportAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewSalesReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValue: KeyValue) {
            binding.keyValue = keyValue
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSalesReportBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList.get(position))
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<KeyValue>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

}