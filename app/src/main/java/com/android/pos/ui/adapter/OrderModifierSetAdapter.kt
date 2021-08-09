package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.ModifierSet
import com.android.pos.databinding.ViewOrderModifierSetsBinding

class OrderModifierSetAdapter :
    RecyclerView.Adapter<OrderModifierSetAdapter.MyViewHolder>() {
    var filterList = ArrayList<ModifierSet>()

    inner class MyViewHolder(private val binding: ViewOrderModifierSetsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierSet) {
            binding.model = item
            binding.executePendingBindings()

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.layoutManager = GridLayoutManager(binding.root.context, 3);
                val adapter = OrderModifierAdapter()
                binding.rvModifiers.adapter = adapter
                adapter.addAll(item.modifiers)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderModifierSetAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderModifierSetsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OrderModifierSetAdapter.MyViewHolder, position: Int) {

        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(modifierSet: List<ModifierSet>) {
        this.filterList = modifierSet as ArrayList<ModifierSet>
        notifyDataSetChanged()

    }

    fun getItem(pos: Int): ModifierSet {
        return filterList[pos]
    }


    fun getAll(): ArrayList<ModifierSet> {
        return filterList
    }

}