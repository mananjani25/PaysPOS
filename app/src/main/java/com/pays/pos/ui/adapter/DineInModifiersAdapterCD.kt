package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Modifier
import com.pays.pos.databinding.ViewDineInTableModifiersBinding
import com.pays.pos.databinding.ViewDineInTableModifiersCdBinding

class DineInModifiersAdapterCD : RecyclerView.Adapter<DineInModifiersAdapterCD.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewDineInTableModifiersCdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
            if (item.modifier_quantity > 1) {
                if (item.modifier_quantity>9){
                    binding.modiferName.text = "${item.modifier_quantity}x ${item.name}"
                }else{
                    binding.modiferName.text = "${item.modifier_quantity}x   ${item.name}"
                }
            }else{
                binding.modiferName.text = "       ${item.name}"
            }
        }

    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DineInModifiersAdapterCD.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInTableModifiersCdBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DineInModifiersAdapterCD.MyViewHolder, position: Int) {
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