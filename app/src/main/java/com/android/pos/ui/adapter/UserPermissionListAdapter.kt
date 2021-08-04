package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.ViewTipItemBinding
import com.android.pos.databinding.ViewUserPermissionItemBinding
import com.android.pos.ui.fragments.settings.tip.TipListViewModel

class UserPermissionListAdapter() : RecyclerView.Adapter<UserPermissionListAdapter.MyViewHolder>() {

    private val tipList = ArrayList<GetTipReponse.Data>()

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
      //  itemBinding.tipModel = tipList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = tipList.size

    fun addTips(tipList: List<GetTipReponse.Data>) {

        this.tipList.apply {
            clear()
            addAll(tipList)
        }
    }

    fun getItem(position:Int): GetTipReponse.Data {
        return tipList[position]
    }

    inner class MyViewHolder(val tipItemBinding: ViewUserPermissionItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root){


    }}