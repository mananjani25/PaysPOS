package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.EodReportResponse
import com.android.pos.databinding.ItemWiseSalesRowBinding

class ItemWiseSalesAdapter() :
    RecyclerView.Adapter<ItemWiseSalesAdapter.MyViewHolder>() {

    lateinit var binding : ItemWiseSalesRowBinding
    var itemWiseSales = ArrayList<EodReportResponse.Data.ItemWiseSalesData>()

    inner class MyViewHolder(private val binding: ItemWiseSalesRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: EodReportResponse.Data.ItemWiseSalesData) {
                binding.model  = model
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = ItemWiseSalesRowBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(itemWiseSales[position])
    }

    override fun getItemCount(): Int {
        return itemWiseSales.size
    }

    fun add(itemWiseSales: ArrayList<EodReportResponse.Data.ItemWiseSalesData>) {
            this.itemWiseSales.clear()
            this.itemWiseSales.addAll(itemWiseSales)
            notifyDataSetChanged()
    }


}