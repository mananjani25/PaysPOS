package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewSalesReportBinding

class SalesReportAdapter :
    RecyclerView.Adapter<SalesReportAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewSalesReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValue: KeyValue) {
            binding.keyValue = keyValue
            binding.executePendingBindings()
            if (keyValue.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
                binding.txtValue.setTextColor(binding.root.resources.getColor(R.color.colorRed))
                var value = binding.txtValue.text
                if (!value.contains("$0.00") && !value.contains("-")) {
                    value = "-$value"
                }
                binding.txtValue.text = value
            }
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