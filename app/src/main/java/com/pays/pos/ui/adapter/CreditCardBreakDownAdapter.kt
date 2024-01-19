package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.EodReportResponse
import com.pays.pos.databinding.ViewPaymentDetailsBinding

class CreditCardBreakDownAdapter(val hideRefund: Boolean) :
    RecyclerView.Adapter<CreditCardBreakDownAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<EodReportResponse.Data.CreditCardBreakdown>()

    inner class MyViewHolder(private val binding: ViewPaymentDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: EodReportResponse.Data.CreditCardBreakdown) {

            binding.txtPrice.text = keyValueList.showData()
            binding.txtRefund.text = keyValueList.showDataTip()
            binding.txtTitle.text = keyValueList.key

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPaymentDetailsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<EodReportResponse.Data.CreditCardBreakdown>) {
        this.arrayList = arrayList as ArrayList<EodReportResponse.Data.CreditCardBreakdown>
        notifyDataSetChanged()
    }

}