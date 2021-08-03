package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.TimeSheetListModel
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.ViewItemTimesheetBinding
import com.android.pos.databinding.ViewSelectedTeamMemberItemBinding

class SelectedTeamMemberAdapter :
    RecyclerView.Adapter<SelectedTeamMemberAdapter.MyViewHolder>() {

    var employeeList = ArrayList<EmployeeListResponse.Data.Employee>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSelectedTeamMemberItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.selectedEmployeeModel = employeeList[position]
        //  itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = employeeList.size

    fun addEmployee(employeeList: List<EmployeeListResponse.Data.Employee>) {
        this.employeeList.apply {
            clear()
            addAll(employeeList)
        }
    }

    inner class MyViewHolder(val discountItemBinding: ViewSelectedTeamMemberItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }


}