package com.android.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.TeamListModel
import com.android.pos.databinding.ViewTeamItemBinding

class TeamListAdapter(val context: Context, val list: ArrayList<TeamListModel>) :
    RecyclerView.Adapter<TeamListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewTeamItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TeamListModel) {
            binding.model = item
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TeamListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewTeamItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}