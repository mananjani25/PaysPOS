package com.android.pos.ui.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.BusinessSettingModel
import com.android.pos.databinding.ViewNoteItemBinding

class NotesListAdapter(val context: Context, val list: ArrayList<BusinessSettingModel>) :
    RecyclerView.Adapter<NotesListAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewNoteItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BusinessSettingModel) {
            binding.model = item
            binding.executePendingBindings()

        }

        init {

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotesListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewNoteItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: NotesListAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }
}