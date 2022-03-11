package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewModifiersBoldBinding

class ModifiersAdapter : RecyclerView.Adapter<ModifiersAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private var binding: ViewModifiersBoldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Modifier) {


        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifiersAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModifiersBoldBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifiersAdapter.MyViewHolder, position: Int) {
        holder.bind(list[holder.bindingAdapterPosition])

    }

    override fun getItemCount(): Int {
        return list.size

    }

    fun add(modifierSet: Modifier) {
        list.add(modifierSet)
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): Modifier {
        return list[pos]
    }


    fun getAll(): ArrayList<Modifier> {
        return list
    }

    fun addAll(modifiers: List<Modifier>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }

}