package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.report.KeyValueWithString
import com.android.pos.databinding.ViewEmployeeReportBinding

class EmployeeReportAdapter :
    RecyclerView.Adapter<EmployeeReportAdapter.MyViewHolder>() {

    private var preFixHeader = ""
    private var arrayList = ArrayList<ArrayList<KeyValueWithString>>()

    inner class MyViewHolder(private val binding: ViewEmployeeReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: ArrayList<KeyValueWithString>) {

            if (keyValueList.isNotEmpty()) {
                val empName = keyValueList[0].value
                val header = "$preFixHeader $empName"
                binding.txtHeader.text = header

                val employeeReportNestedAdapter = EmployeeReportNestedAdapter()
                employeeReportNestedAdapter.add(keyValueList)
                binding.rvNested.adapter = employeeReportNestedAdapter
            }
            /*for (i in 0 until keyValueList.size) {
                val obj = keyValueList[i]
                setValue(obj)
            }*/
        }

        /*private fun setValue(keyValue: KeyValueWithString) {
            if (keyValue.key?.isNotEmpty() == true) {
                if (keyValue.key == EMP_NAME) {
                    binding.txtName.text = keyValue.value.toString()
                } else if (keyValue.key == AMT_BY_CASH || keyValue.key.contains(AMT_BY_CASH)) {
                    binding.txtAmtByCash.text = keyValue.showFormattedValue()
                } else if (keyValue.key == SERVICE_CHARGE_BY_CASH || keyValue.key.contains(
                        SERVICE_CHARGE_BY_CASH
                    )
                ) {
                    binding.txtServiceChargeByCash.text = keyValue.showFormattedValue()
                } else if (keyValue.key == TIP_BY_CASH || keyValue.key.contains(TIP_BY_CASH)) {
                    binding.txtTipByCash.text = keyValue.showFormattedValue()
                } else if (keyValue.key == REFUND || keyValue.key.contains(REFUND)) {
                    binding.txtRefund.text = keyValue.showFormattedValue()
                } else if (keyValue.key == TIP || keyValue.key.contains(TIP)) {
                    binding.txtTips.text = keyValue.showFormattedValue()
                } else if (keyValue.key == DISCOUNT || keyValue.key.contains(DISCOUNT)) {
                    binding.txtDiscounts.text = keyValue.showFormattedValue()
                } else if (keyValue.key == AMT_COLLECTED || keyValue.key.contains(AMT_COLLECTED)) {
                    binding.txtAmtCollected.text = keyValue.showFormattedValue()
                }
            }
        }*/
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewEmployeeReportBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: ArrayList<ArrayList<KeyValueWithString>>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

    fun addPrefixHeader(preFixHeader: String) {
        this.preFixHeader = preFixHeader
    }
}