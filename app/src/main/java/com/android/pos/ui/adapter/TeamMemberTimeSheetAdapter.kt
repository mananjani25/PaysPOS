package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.android.pos.databinding.ViewItemTimesheetBinding
import com.android.pos.ui.fragments.settings.teamrole.TeamMemberSheetViewModel

class TeamMemberTimeSheetAdapter(val viewModel: TeamMemberSheetViewModel) :
    RecyclerView.Adapter<TeamMemberTimeSheetAdapter.MyViewHolder>() {

    var employeeTimeSheet = ArrayList<GetEmployeesTimeSheetResponse.Data>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemTimesheetBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = employeeTimeSheet[position]
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = employeeTimeSheet.size

    fun teamTimesheetList(employeeTimeSheet: List<GetEmployeesTimeSheetResponse.Data>) {
        this.employeeTimeSheet.apply {
            clear()
            addAll(employeeTimeSheet)
            notifyDataSetChanged()
        }
    }


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