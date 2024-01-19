package com.pays.pos.ui.adapter.boldpos

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TaxData
import com.pays.pos.databinding.LayoutTaxBifurcationBinding
import com.pays.pos.databinding.LayoutTaxDashBifurcatioinBinding
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson


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

            if (stringBuffer.length > 10) {
                var value = stringBuffer.toString().substring(0, 10).toString() + "..."
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

            Log.e("totalTaxTypePriceManan","totalTaxTypePrice:  ${item.totalTaxTypePrice}")
            MethodUtils.setPriceTextViewDown(binding.txtAmount, item.totalTaxTypePrice ?: 0.0)
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

            Log.e("checkTaxDataAdapter","checkTaxData ${Gson().toJson(item)}")
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
            if (stringBuffer.length > 10) {
                var value = stringBuffer.toString().substring(0, 10).toString() + "..."
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
            Log.e("TaxBiferguationMan","totalTaxTypePrice:  ${item.totalTaxTypePrice}")
            MethodUtils.setPriceTextViewDown(binding.txtAmount, item.totalTaxTypePrice ?: 0.0)
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