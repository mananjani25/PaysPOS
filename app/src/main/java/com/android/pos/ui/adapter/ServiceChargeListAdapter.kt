package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.DiscountListModel
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.databinding.ViewDiscountItemBinding
import com.android.pos.databinding.ViewServiceChargeItemBinding

class ServiceChargeListAdapter() :
    RecyclerView.Adapter<ServiceChargeListAdapter.MyViewHolder>() {

    var serviceChargeList = ArrayList<GetServiceChargeResponse.Data>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ServiceChargeListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewServiceChargeItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ServiceChargeListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.serviceChargeModel = serviceChargeList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = serviceChargeList.size


    fun addServiceCharge(discountList: List<GetServiceChargeResponse.Data>) {

        this.serviceChargeList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): GetServiceChargeResponse.Data {
        return serviceChargeList[position]
    }

    inner class MyViewHolder(val discountItemBinding: ViewServiceChargeItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root){

            init {
                discountItemBinding.imgCheckBox.setOnClickListener {
                    serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                    notifyDataSetChanged()
                }
            }
        }

}