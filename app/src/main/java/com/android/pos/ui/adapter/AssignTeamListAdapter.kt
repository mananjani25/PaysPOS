package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TeamRole
import com.android.pos.databinding.ViewAssignedRoleItemBinding
import com.android.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel

class AssignTeamListAdapter(val viewModel: UserAccessPermissionViewModel) :
    RecyclerView.Adapter<AssignTeamListAdapter.MyViewHolder>() {

    private val permissionList = ArrayList<TeamRole>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AssignTeamListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewAssignedRoleItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssignTeamListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.tipItemBinding
        itemBinding.permissionListModel = permissionList[position]

        val employees = permissionList[position].employees.map { it.name }
        itemBinding.teamMemberList.text = TextUtils.join(",", employees)
        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = permissionList.size

    fun addPermissionList(permissionList: List<TeamRole>) {

        this.permissionList.apply {
            clear()
            addAll(permissionList)
            notifyDataSetChanged()
        }
    }

    fun getItem(position: Int): TeamRole {
        return permissionList[position]
    }

    inner class MyViewHolder(val tipItemBinding: ViewAssignedRoleItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root)
}