package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.databinding.ViewLoyaltyPointItemBinding
import com.android.pos.ui.fragments.settings.loyaltypoints.LoyaltyPointViewModel

class LoyaltyPointAdapter(val viewModel: LoyaltyPointViewModel) :
    RecyclerView.Adapter<LoyaltyPointAdapter.MyViewHolder>() {

    var loyaltyPointList = ArrayList<LoyaltyProgramsModel>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LoyaltyPointAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewLoyaltyPointItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LoyaltyPointAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.serviceChargeModel = loyaltyPointList[position]
        itemBinding.viewModel = viewModel

        val model = loyaltyPointList[position]
        if (model.rewardType == "%") {
            itemBinding.txtLoyaltyValue.text = model.amount.toString() + "%"
        } else {
            itemBinding.txtLoyaltyValue.text = "$" + model.amount.toString()
        }

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = loyaltyPointList.size


    fun addServiceCharge(discountList: List<LoyaltyProgramsModel>) {

        this.loyaltyPointList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): LoyaltyProgramsModel {
        return loyaltyPointList[position]
    }

    fun update(id: Int) {

        loyaltyPointList.forEach {
            it.isEnable = it.id == id
        }
        notifyDataSetChanged()

    }

    inner class MyViewHolder(val discountItemBinding: ViewLoyaltyPointItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

    }

}