package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.databinding.ViewPaymentDetailsBinding
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible

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
                binding.txtPrice.text = obj.showFormattedValue()
            } else if (keyValueList.size == 2) {
                val obj = keyValueList[0]
                binding.txtTitle.text = obj.key
                binding.txtPrice.text = obj.showFormattedValue()

                if (hideRefund) {
                    binding.txtRefund.gone()
                } else {
                    binding.txtRefund.visible()
                    val obj2 = keyValueList[1]
                    binding.txtRefund.text = obj2.showFormattedValue()
                }
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