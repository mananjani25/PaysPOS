package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.SingleMemberTimeSheetListModel
import com.android.pos.databinding.ViewItemTimesheetBinding
import com.android.pos.databinding.ViewSingleMemberItemTimesheetBinding

class SingleTeamMemberTimeSheetAdapter(var timeSheet: ArrayList<SingleMemberTimeSheetListModel>) :
    RecyclerView.Adapter<SingleTeamMemberTimeSheetAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSingleMemberItemTimesheetBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = timeSheet[position]
        //  itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = timeSheet.size

    inner class MyViewHolder(val discountItemBinding: ViewSingleMemberItemTimesheetBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }


}