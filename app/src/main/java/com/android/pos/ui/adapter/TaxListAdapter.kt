package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.databinding.ViewTaxItemBinding


class TaxListAdapter : RecyclerView.Adapter<TaxListAdapter.MyViewHolder>() {

     var taxList = ArrayList<GetTaxResponse.Data>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewTaxItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaxListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.tipItemBinding
        itemBinding.taxModel = taxList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount()=taxList.size

    fun addTaxes(taxList: List<GetTaxResponse.Data>) {

        this.taxList.apply {
            clear()
            addAll(taxList)
        }
    }

    fun getItem(position:Int): GetTaxResponse.Data {
        return taxList[position]
    }

    inner class MyViewHolder(val tipItemBinding: ViewTaxItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root)
}