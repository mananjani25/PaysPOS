package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.model.responseModel.GetUserPermissionListResponse
import com.pays.pos.databinding.ViewUserPermissionItemBinding
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener

class UserPermissionListAdapter : RecyclerView.Adapter<UserPermissionListAdapter.MyViewHolder>() {

    private val permissionList = ArrayList<TeamRole>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
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

        itemBinding.imgArrow.visibility = View.GONE

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
        RecyclerView.ViewHolder(tipItemBinding.root){
            init {
                tipItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }
            }
        }
}