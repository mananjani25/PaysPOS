package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.data.model.responseModel.report.KeyValueWithString
import com.android.pos.databinding.ViewSalesOrdersReportBinding
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible

class SalesOrderDetailsAdapter :
    RecyclerView.Adapter<SalesOrderDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<ArrayList<KeyValueWithString>>()

    inner class MyViewHolder(private val binding: ViewSalesOrdersReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: ArrayList<KeyValueWithString>) {
            if (keyValueList.size > 0) {

                val employeeReportNestedAdapter = SalesOrderNestedDetailsAdapter()
                employeeReportNestedAdapter.add(keyValueList)
                binding.rvNested.adapter = employeeReportNestedAdapter

            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSalesOrdersReportBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: ArrayList<ArrayList<KeyValueWithString>>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }
}