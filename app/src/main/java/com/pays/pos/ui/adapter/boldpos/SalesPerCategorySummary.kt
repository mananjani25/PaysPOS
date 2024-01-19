package com.pays.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewEmployeeGuestDetailsBinding
import com.pays.pos.utils.MethodUtils

class SalesPerCategorySummary : RecyclerView.Adapter<SalesPerCategorySummary.MyViewHolder>() {
    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewEmployeeGuestDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(obj: KeyValue) {
            binding.txtPaymentId.text = obj.key
            if (obj.value?.trim()?.isNotEmpty() == true) {
                binding.txtLast4.text = MethodUtils.roundOffAmount(obj.value.toString().toDouble() ?: 0.0)
            }
            binding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SalesPerCategorySummary.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewEmployeeGuestDetailsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: SalesPerCategorySummary.MyViewHolder, position: Int) {
        holder.bind(arrayList[position])

    }

    override fun getItemCount(): Int {
        return arrayList.size

    }

    fun add(arrayListNew: ArrayList<KeyValue>) {
        this.arrayList.clear()
        this.arrayList.addAll(arrayListNew)


        notifyDataSetChanged()
    }
}