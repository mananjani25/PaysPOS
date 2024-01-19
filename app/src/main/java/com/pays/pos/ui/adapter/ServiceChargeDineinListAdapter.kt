package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.databinding.ViewServiceChargeItemBinding
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.ui.fragments.settings.servicecharge.ServiceChargeListViewModel
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible


class ServiceChargeDineinListAdapter(val viewModel: ServiceChargeListViewModel) :
    RecyclerView.Adapter<ServiceChargeDineinListAdapter.MyViewHolder>() {

    var serviceChargeList = ArrayList<TbServiceCharge>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ServiceChargeDineinListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewServiceChargeItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ServiceChargeDineinListAdapter.MyViewHolder,
        position: Int
    ) {
        val itemBinding = holder.discountItemBinding
        itemBinding.serviceChargeModel = serviceChargeList[position]
        itemBinding.viewModel = viewModel
        itemBinding.linearGuestcoount?.visible()
        itemBinding.minGuest?.text =
            "Min Guest (" + serviceChargeList[position].min_guest_count.toString() + ")"
        itemBinding.maxGuest?.text =
            "Max Guest (" + serviceChargeList[position].max_guest_count.toString() + ")"
        itemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
            mCallback?.onItemClickDineinListener(
                it,
                position,
                serviceChargeList[position].order_type
            )
        }
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


        }
    }

    interface ItemCallback {
        fun onItemClickDineinListener(view: View?, pos: Int, order_type: String?)
    }


}