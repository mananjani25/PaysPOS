package com.android.pos.ui.adapter.boldpos

import android.R.attr.data
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TaxData
import com.android.pos.databinding.LayoutTaxBifurcationBinding
import com.android.pos.utils.MethodUtils


class TaxBirfurcationAdapter : RecyclerView.Adapter<TaxBirfurcationAdapter.MyViewHolder>() {
    var taxlist = ArrayList<TaxData>()

    inner class MyViewHolder(private val binding: LayoutTaxBifurcationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaxData, pos: Int) {
            val items: List<String> = item.name!!.split(" ")
            var stringBuffer: StringBuffer = StringBuffer()
            items.forEach {
                stringBuffer.append(it.substring(0, 1).toUpperCase() + it.substring(1, it.length))
            }
            binding.txtName.text = stringBuffer.toString()

            if (item.taxType.equals("Percentage")) {
                binding.txtValue.text = String.format("%.2f", item.rate) + "%"
            } else {
                binding.txtValue.text = "$" + String.format("%.2f", item.rate)
            }

            MethodUtils.setPriceTextView(binding.txtAmount, item.totalTaxTypePrice ?: 0.0)
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaxBirfurcationAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutTaxBifurcationBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaxBirfurcationAdapter.MyViewHolder, position: Int) {
        holder.bind(taxlist[position], position)
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