package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.PermissionModuleListModel
import com.android.pos.data.model.SingleMemberTimeSheetListModel
import com.android.pos.databinding.ViewItemTimesheetBinding
import com.android.pos.databinding.ViewModuleSelectedItemBinding
import com.android.pos.databinding.ViewSingleMemberItemTimesheetBinding
import com.android.pos.ui.fragments.settings.teamrole.UserPermissionViewModel

class AllPermissionModuleAdapter(val viewModel: UserPermissionViewModel) :
    RecyclerView.Adapter<AllPermissionModuleAdapter.MyViewHolder>() {

    var allPermissionModule = ArrayList<PermissionModuleListModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModuleSelectedItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.permissionModule = allPermissionModule[position]
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = allPermissionModule.size

    fun addPermissionModule(allPermissionModule: List<PermissionModuleListModel>) {
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