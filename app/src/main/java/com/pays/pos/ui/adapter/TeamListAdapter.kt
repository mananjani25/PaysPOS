package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.TeamListModel
import com.pays.pos.databinding.ViewTeamItemBinding

class TeamListAdapter(val context: Context, val list: ArrayList<TeamListModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /*inner class MyViewHolder(private val binding: ViewTeamItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TeamListModel) {
            binding.model = item
            binding.executePendingBindings()
        }

    }*/

    class ViewHolderTitle(itemView: View) : RecyclerView.ViewHolder(itemView) {


    }

    class ViewHolderMain(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        /*val inflater = LayoutInflater.from(context)

        val binding =
            ViewTeamItemBinding.inflate(inflater, parent, false)*/

        return when (viewType) {
            0 -> ViewHolderTitle(
                LayoutInflater.from(context).inflate(R.layout.view_team_header, parent, false)
            )
            else -> ViewHolderMain(
                LayoutInflater.from(context).inflate(R.layout.view_team_item, parent, false)
            )
        }

    }

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            0 -> {
                val holder: ViewHolderTitle = holder as ViewHolderTitle
                holder.itemView.findViewById<AppCompatTextView>(R.id.txtHeader)
                    .setText(list.get(position).title)
            }
            else -> {
                (holder as ViewHolderMain).itemView.findViewById<AppCompatTextView>(R.id.tvInitialName)
                    .setText(list.get(position).title)
                (holder as ViewHolderMain).itemView.findViewById<AppCompatTextView>(R.id.txtName)
                    .setText(list.get(position).name)
                (holder as ViewHolderMain).itemView.findViewById<AppCompatTextView>(R.id.txtNumber)
                    .setText(list.get(position).email)
            }

        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list.get(position).isHeader) {
            0
        } else {
            1
        }
    }
}