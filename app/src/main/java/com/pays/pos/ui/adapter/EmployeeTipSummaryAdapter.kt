package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.employeeTipSummary.EmployeeTipSummaryResponse
import com.pays.pos.databinding.EmployeeTipSummaryRowBinding
import com.pays.pos.databinding.ItemWiseSalesRowBinding
import com.pays.pos.ui.fragments.employeeTipSummary.EmployeeTipSummary

class EmployeeTipSummaryAdapter() :
    RecyclerView.Adapter<EmployeeTipSummaryAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<EmployeeTipSummaryResponse.Data>()
    private lateinit var binding : EmployeeTipSummaryRowBinding

    inner class MyViewHolder(private val binding: EmployeeTipSummaryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(modelData: EmployeeTipSummaryResponse.Data) {
                binding.model = modelData
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = EmployeeTipSummaryRowBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }


    fun add(data: ArrayList<EmployeeTipSummaryResponse.Data>) {
        arrayList.clear()
        if (data.isNotEmpty()){
            this.arrayList.addAll(data)
        }
        notifyDataSetChanged()

    }


}