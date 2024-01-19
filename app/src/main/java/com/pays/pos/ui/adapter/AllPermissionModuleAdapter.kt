package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.ModulePermission
import com.pays.pos.data.model.PermissionModuleListModel
import com.pays.pos.data.model.responseModel.GetTeamRoleModule
import com.pays.pos.databinding.ViewModuleSelectedItemBinding
import com.pays.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel

class AllPermissionModuleAdapter(val viewModelAccess: UserAccessPermissionViewModel) :
    RecyclerView.Adapter<AllPermissionModuleAdapter.MyViewHolder>() {

    var allPermissionModule = ArrayList<ModulePermission>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModuleSelectedItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.permissionModule = allPermissionModule[position]
        itemBinding.viewModel = viewModelAccess

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = allPermissionModule.size

    fun addPermissionModule(allPermissionModule: List<ModulePermission>) {
        this.allPermissionModule.apply {
            clear()
            addAll(allPermissionModule)
            notifyDataSetChanged()
        }
    }

    inner class MyViewHolder(val discountItemBinding: ViewModuleSelectedItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {
    }


}