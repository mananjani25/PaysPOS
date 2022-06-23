package com.android.pos.ui.adapter.boldpos

import android.R.attr.data
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TaxData
import com.android.pos.databinding.LayoutTaxBifurcationBinding
import com.android.pos.databinding.LayoutTaxDashBifurcatioinBinding
import com.android.pos.utils.MethodUtils
import java.util.Date.from


class TaxBirfurcationAdapter(var isFrom: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var taxlist = ArrayList<TaxData>()

    class MyViewHolderTransaction(private val binding: LayoutTaxBifurcationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TaxData, pos: Int) {
            val items: List<String> = item.name!!.split(" ")
            var stringBuffer: StringBuffer = StringBuffer()
            items.forEach {
                stringBuffer.append(
                    it.substring(0, 1).toUpperCase() + it.substring(
                        1,
                        it.length
                    ) + " "
                )
            }

            if (stringBuffer.length > 15) {
                var value = stringBuffer.toString().substring(0, 15).toString() + "..."
                binding.txtName.text = value
            } else {
                binding.txtName.text = stringBuffer.toString()
            }

            if (item.taxType.equals("Percentage")) {
                binding.txtValue.text = String.format("%.2f", item.rate) + "%"
            } else {
                var totalAmountTax = item.totalTaxTypePrice
                if (item.subTotalAmount != 0.0) {
                    var final_percantage = (100 * totalAmountTax) / item.subTotalAmount!!
                    binding.txtValue.text = String.format("%.2f", final_percantage) + "%"
                } else {
                    binding.txtValue.text = String.format("%.2f", 0.0) + "%"
                }
            }

            MethodUtils.setPriceTextView(binding.txtAmount, item.totalTaxTypePrice ?: 0.0)
        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolderTransaction {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutTaxBifurcationBinding.inflate(layoutInflater, parent, false)
                return MyViewHolderTransaction(binding)
            }
        }
    }

    class MyViewHolderDashBoard(private val binding: LayoutTaxDashBifurcatioinBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: TaxData, pos: Int) {
            val items: List<String> = item.name!!.split(" ")
            var stringBuffer: StringBuffer = StringBuffer()
            items.forEach {
                stringBuffer.append(
                    it.substring(0, 1).toUpperCase() + it.substring(
                        1,
                        it.length
                    ) + " "
                )
            }
            if (stringBuffer.length > 15) {
                var value = stringBuffer.toString().substring(0, 15).toString() + "..."
                binding.txtName.text = value
            } else {
                binding.txtName.text = stringBuffer.toString()
            }

            if (item.taxType.equals("Percentage")) {
                binding.txtValue.text = String.format("%.2f", item.rate) + "%"
            } else {
                var totalAmountTax = item.totalTaxTypePrice
                if (item.subTotalAmount != 0.0) {
                    var final_percantage = (100 * totalAmountTax) / item.subTotalAmount!!
                    binding.txtValue.text = String.format("%.2f", final_percantage) + "%"
                } else {
                    binding.txtValue.text = String.format("%.2f", 0.0) + "%"
                }
            }

            MethodUtils.setPriceTextView(binding.txtAmount, item.totalTaxTypePrice ?: 0.0)
        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolderDashBoard {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    LayoutTaxDashBifurcatioinBinding.inflate(layoutInflater, parent, false)
                return MyViewHolderDashBoard(binding)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if (isFrom == "transaction") MyViewHolderTransaction.from(parent)
        else MyViewHolderDashBoard.from(parent)

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyViewHolderDashBoard -> holder.bind(taxlist[position], position)
            is MyViewHolderTransaction -> holder.bind(taxlist[position], position)
        }
    }

    fun setList(list: ArrayList<TaxData>) {
        taxlist = list
        notifyDataSetChanged()

    }

    fun clearList() {
        taxlist.clear()
        taxlist = arrayListOf()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return taxlist.size
    }
}