package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.DiscountListModel
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.databinding.ViewDiscountItemBinding

class DiscountListAdapter() :
    RecyclerView.Adapter<DiscountListAdapter.MyViewHolder>() {

    var discountList = ArrayList<GetDiscountResponse.Data>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiscountListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDiscountItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DiscountListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.discountModel = discountList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = discountList.size


    fun addDiscount(discountList: List<GetDiscountResponse.Data>) {

        this.discountList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): GetDiscountResponse.Data {
        return discountList[position]
    }

    inner class MyViewHolder(val discountItemBinding: ViewDiscountItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root)

}