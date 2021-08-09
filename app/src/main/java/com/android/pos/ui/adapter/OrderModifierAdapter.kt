package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewOrderModifiersBinding

class OrderModifierAdapter :
    RecyclerView.Adapter<OrderModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()
    var selectedItemList = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
        }

        init {

            binding.root.setOnClickListener {
                list[layoutPosition].isChecked = !list[layoutPosition].isChecked

                if (list[layoutPosition].isChecked) {
                    selectedItemList.add(list[layoutPosition])
                } else {
                    selectedItemList.remove(list.get(layoutPosition))
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderModifierAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderModifiersBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OrderModifierAdapter.MyViewHolder, position: Int) {

        holder.bind(list.get(position))
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