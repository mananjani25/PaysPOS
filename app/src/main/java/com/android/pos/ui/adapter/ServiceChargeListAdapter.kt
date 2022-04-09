package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.databinding.ViewServiceChargeItemBinding
import com.android.pos.ui.fragments.settings.servicecharge.ServiceChargeListViewModel
import com.android.pos.utils.callback.ItemCallback

class ServiceChargeListAdapter(val viewModel: ServiceChargeListViewModel) :
    RecyclerView.Adapter<ServiceChargeListAdapter.MyViewHolder>() {

    var serviceChargeList = ArrayList<TbServiceCharge>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
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
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = serviceChargeList.size


    fun addServiceCharge(discountList: List<TbServiceCharge>) {

        this.serviceChargeList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): TbServiceCharge {
        return serviceChargeList[position]
    }

    inner class MyViewHolder(val discountItemBinding: ViewServiceChargeItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked =
                    !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
            discountItemBinding.layoutMenu.imgOrderMenu.setOnClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }

        }
    }



}