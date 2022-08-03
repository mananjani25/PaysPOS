package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.ClockinOutReportModel
import com.android.pos.databinding.ViewClockInClockOutReportBinding

class ClockInClockOutAdapter : RecyclerView.Adapter<ClockInClockOutAdapter.MyViewHolder>() {
    private var arrayList = ArrayList<ClockinOutReportModel>()

    inner class MyViewHolder(private val binding: ViewClockInClockOutReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ClockinOutReportModel) {

        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClockInClockOutAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewClockInClockOutReportBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)


    }

    override fun onBindViewHolder(holder: ClockInClockOutAdapter.MyViewHolder, position: Int) {
        holder.bind(arrayList[position])


    }

    override fun getItemCount(): Int {
        return arrayList.size

    }
    fun setList(list:ArrayList<ClockinOutReportModel>){
        this.arrayList.clear()
        this.arrayList.addAll(list)
        notifyDataSetChanged()
    }
}