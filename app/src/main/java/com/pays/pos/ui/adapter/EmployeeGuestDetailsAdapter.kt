package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewEmployeeGuestDetailsBinding
import com.pays.pos.databinding.ViewSalesReportBinding
import com.pays.pos.utils.MethodUtils

class EmployeeGuestDetailsAdapter :
    RecyclerView.Adapter<EmployeeGuestDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewEmployeeGuestDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValue: KeyValue) {
            binding.txtPaymentId.text = keyValue.key
            if (keyValue.key.equals("Average Spent Per Guest",ignoreCase = true)){
                if (keyValue.value?.isNotEmpty() == true) {
                    binding.txtLast4.text = keyValue.value.toDouble()
                        .let { MethodUtils.roundOffAmount(it) }
                }else{
                    binding.txtLast4.text = MethodUtils.roundOffAmount(0.00)
                }
            }else{
                binding.txtLast4.text = keyValue.value
            }

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewEmployeeGuestDetailsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
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