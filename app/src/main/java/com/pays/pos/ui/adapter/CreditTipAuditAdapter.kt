package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewCreditTipAuditBinding
import com.pays.pos.databinding.ViewPaymentDetailsBinding
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class CreditTipAuditAdapter() :
    RecyclerView.Adapter<CreditTipAuditAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<ArrayList<KeyValue>>()

    inner class MyViewHolder(private val binding: ViewCreditTipAuditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: ArrayList<KeyValue>) {

            if (keyValueList.size == 5) {
                binding.txtPaymentId.text = keyValueList[0].value
                binding.txtLast4.text = keyValueList[1].value
                binding.txtSubtotal.text = keyValueList[2].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
                binding.txtTip.text = keyValueList[3].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
                binding.txtTotal.text = keyValueList[4].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
            } else if (keyValueList.size == 3) {
                binding.txtLast4.text = "Total"
                binding.txtSubtotal.text = keyValueList[0].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
                binding.txtTip.text = keyValueList[1].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
                binding.txtTotal.text = keyValueList[2].value?.toDouble()
                    ?.let { MethodUtils.roundOffAmount(it) }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCreditTipAuditBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: ArrayList<ArrayList<KeyValue>>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

}