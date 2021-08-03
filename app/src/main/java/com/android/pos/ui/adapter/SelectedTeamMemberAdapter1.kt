package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.TimeSheetListModel
import com.android.pos.databinding.ViewItemTimesheetBinding

class SelectedTeamMemberAdapter1(var timeSheet: ArrayList<TimeSheetListModel>) :
    RecyclerView.Adapter<SelectedTeamMemberAdapter1.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemTimesheetBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = timeSheet[position]
        //  itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = timeSheet.size

    inner class MyViewHolder(val discountItemBinding: ViewItemTimesheetBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }


}