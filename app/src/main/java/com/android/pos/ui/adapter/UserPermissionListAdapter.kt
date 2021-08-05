package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.responseModel.GetUserPermissionListResponse
import com.android.pos.databinding.ViewUserPermissionItemBinding

class UserPermissionListAdapter : RecyclerView.Adapter<UserPermissionListAdapter.MyViewHolder>() {

    private val permissionList = ArrayList<TeamRole>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserPermissionListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewUserPermissionItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPermissionListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.tipItemBinding
        itemBinding.permissionListModel = permissionList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = permissionList.size

    fun addPermissionList(permissionList: List<TeamRole>) {

        this.permissionList.apply {
            clear()
            addAll(permissionList)
        }
    }

    fun getItem(position: Int): TeamRole {
        return permissionList[position]
    }

    inner class MyViewHolder(val tipItemBinding: ViewUserPermissionItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root)
}