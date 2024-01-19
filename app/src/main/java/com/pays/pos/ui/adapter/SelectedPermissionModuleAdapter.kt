package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.ModulePermission
import com.pays.pos.data.model.PermissionModuleListModel
import com.pays.pos.data.model.responseModel.GetTeamRoleModule
import com.pays.pos.databinding.ViewModuleSelectedItem1Binding

import com.pays.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel

class SelectedPermissionModuleAdapter(val viewModelAccess: UserAccessPermissionViewModel) :
    RecyclerView.Adapter<SelectedPermissionModuleAdapter.MyViewHolder>() {

    var selectedModule = ArrayList<ModulePermission>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModuleSelectedItem1Binding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.permissionModule = selectedModule[position]
        itemBinding.viewModel = viewModelAccess

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = selectedModule.size

    fun addPermissionModule(selectedModule: List<ModulePermission>) {
        this.selectedModule.apply {
            clear()
            addAll(selectedModule)
            notifyDataSetChanged()
        }
    }

    inner class MyViewHolder(val discountItemBinding: ViewModuleSelectedItem1Binding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {


    }


}