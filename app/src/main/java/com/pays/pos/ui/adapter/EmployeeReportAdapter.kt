package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValueWithString
import com.pays.pos.databinding.ViewEmployeeReportBinding

class EmployeeReportAdapter :
    RecyclerView.Adapter<EmployeeReportAdapter.MyViewHolder>() {

    private var preFixHeader = ""
    private var arrayList = ArrayList<ArrayList<KeyValueWithString>>()

    inner class MyViewHolder(private val binding: ViewEmployeeReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: ArrayList<KeyValueWithString>) {

            if (keyValueList.isNotEmpty()) {
                val empName = keyValueList[0].value
                val header = "$preFixHeader $empName"
                binding.txtHeader.text = header

                val employeeReportNestedAdapter = EmployeeReportNestedAdapter()
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
        val binding = ViewEmployeeReportBinding.inflate(inflater, parent, false)
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

    fun addPrefixHeader(preFixHeader: String) {
        this.preFixHeader = preFixHeader
    }
}