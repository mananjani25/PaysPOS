package com.pays.pos.ui.adapter.boldpos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewEmployeeGuestDetailsBinding
import com.pays.pos.utils.MethodUtils

class SalesPerCategorySummary : RecyclerView.Adapter<SalesPerCategorySummary.MyViewHolder>() {
    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewEmployeeGuestDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(obj: KeyValue, isDivider: Boolean) {
            if (isDivider) {
                // Set the divider logic (such as height and background color)
                binding.root.apply {
                    setBackgroundColor(Color.LTGRAY)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
                }
            } else {
                // Regular item binding
                binding.txtPaymentId.text = obj.key
                if (obj.value?.trim()?.isNotEmpty() == true) {
                    binding.txtLast4.text = MethodUtils.roundOffAmount(obj.value.toString().toDouble() ?: 0.0)
                }
            }
            binding.executePendingBindings()

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        // Inflate a normal layout or divider based on viewType
        val binding = if (viewType == 1) {
            ViewEmployeeGuestDetailsBinding.inflate(inflater, parent, false)
        } else {
            ViewEmployeeGuestDetailsBinding.inflate(inflater, parent, false)
        }

        return MyViewHolder(binding)
    }


    override fun onBindViewHolder(holder: SalesPerCategorySummary.MyViewHolder, position: Int) {
//        holder.bind(arrayList[position])
        val item = arrayList[position]
        val isDivider = item.key == "FULL_LINE_DIVIDER"
        holder.bind(item, isDivider)

    }

    override fun getItemViewType(position: Int): Int {
        return if (arrayList[position].key == "FULL_LINE_DIVIDER") {
            1 // Divider
        } else {
            0 // Normal item
        }
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