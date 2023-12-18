package com.android.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewCartModifierBinding
import com.google.gson.Gson

class CartItemModifierAdapter :
    RecyclerView.Adapter<CartItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()
    private val TAG = "CartItemModifierAdapter"

    inner class MyViewHolder(private val binding: ViewCartModifierBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
            if (item.modifier_quantity > 1) {
                if (item.modifier_quantity>9){
                    binding.txtName.text = "${item.modifier_quantity}x ${item.name}"
                }else{
                    binding.txtName.text = "${item.modifier_quantity}x   ${item.name}"
                }
            }else{
                binding.txtName.text = "${item.modifier_quantity}x   ${item.name}"
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartItemModifierAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCartModifierBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CartItemModifierAdapter.MyViewHolder, position: Int) {

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