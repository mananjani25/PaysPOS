package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.responseModel.EmployeeListResponse
import com.pays.pos.databinding.ViewSelectedTeamMemberItemBinding
import com.pays.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel

class AllSelectedTeamMemberAdapter(val viewModelAccess: UserAccessPermissionViewModel) :
    RecyclerView.Adapter<AllSelectedTeamMemberAdapter.MyViewHolder>() {

    var employeeList = ArrayList<Employee>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSelectedTeamMemberItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.selectedEmployeeModel = employeeList[position]
        itemBinding.viewModel = viewModelAccess

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = employeeList.size

    fun addEmployee(employeeList: List<Employee>) {
        this.employeeList.apply {
            clear()
            addAll(employeeList)
        }
    }

    inner class MyViewHolder(val discountItemBinding: ViewSelectedTeamMemberItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

    }


}