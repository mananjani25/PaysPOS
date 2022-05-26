package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.databinding.ViewEmployeeGuestDetailsBinding
import com.android.pos.databinding.ViewSalesReportBinding

class EmployeeGuestDetailsAdapter :
    RecyclerView.Adapter<EmployeeGuestDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewEmployeeGuestDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValue: KeyValue) {
            binding.txtPaymentId.text = keyValue.key
            binding.txtLast4.text = keyValue.value
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