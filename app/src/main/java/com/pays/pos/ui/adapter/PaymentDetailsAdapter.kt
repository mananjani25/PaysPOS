package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewPaymentDetailsBinding
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class PaymentDetailsAdapter(val hideRefund: Boolean) :
    RecyclerView.Adapter<PaymentDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<ArrayList<KeyValue>>()

    inner class MyViewHolder(private val binding: ViewPaymentDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: ArrayList<KeyValue>) {

            if (keyValueList.size == 1) {
                val obj = keyValueList[0]
                binding.txtTitle.text = obj.key
                binding.txtPrice.text = obj.showData()
                if (obj.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
                    binding.txtPrice.setTextColor(binding.root.resources.getColor(R.color.colorRed))
                }
            } else if (keyValueList.size == 2) {
                val obj = keyValueList[0]
                binding.txtTitle.text = obj.key
                binding.txtPrice.text = obj.showData()
                if (obj.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
                    binding.txtPrice.setTextColor(binding.root.resources.getColor(R.color.colorRed))
                }


                if (hideRefund) {
                    binding.txtRefund.gone()
                } else {
                    binding.txtRefund.visible()
                    val obj2 = keyValueList[1]
                    binding.txtRefund.text = obj2.showData()
                }
            }

            if (keyValueList[0].key.toString().toLowerCase().contains("Total Refund".toLowerCase())) {
                var value = binding.txtPrice.text
                if (!value.contains("$0.00") && !value.contains("-")) {
                    value = "-$value"
                }
                binding.txtPrice.text = value
            }
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

    fun add(arrayList: ArrayList<ArrayList<KeyValue>>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

}