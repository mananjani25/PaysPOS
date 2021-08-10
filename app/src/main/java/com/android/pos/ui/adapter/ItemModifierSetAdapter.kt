package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.ModifierSet
import com.android.pos.databinding.ViewOrderModifierSetsBinding

class ItemModifierSetAdapter :
    RecyclerView.Adapter<ItemModifierSetAdapter.MyViewHolder>() {
    var filterList = ArrayList<ModifierSet>()
    var selectedModifierList = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifierSetsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var adapter: ItemModifierAdapter? = null

        fun bind(item: ModifierSet) {
            binding.model = item
            binding.executePendingBindings()

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.layoutManager = GridLayoutManager(binding.root.context, 3);
                adapter = ItemModifierAdapter()
                binding.rvModifiers.adapter = adapter
                adapter!!.addAll(item.modifiers)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemModifierSetAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderModifierSetsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ItemModifierSetAdapter.MyViewHolder, position: Int) {

        holder.bind(filterList[holder.bindingAdapterPosition])
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

    fun getSelectedModifiers(): ArrayList<Modifier> {
        selectedModifierList.clear()
        filterList.forEach { modifierSet ->
            modifierSet.modifiers.forEach {
                if (it.isChecked) {
                    it.modifierSetId = modifierSet.id
                    selectedModifierList.add(it)
                }
            }
        }
        return selectedModifierList
    }

    fun setData(modifiers: List<Modifier>) {


        filterList.forEach { modifierSet ->
            modifierSet.modifiers.forEach { modifierSet_Modifier ->
                modifiers.forEach {
                    if (modifierSet_Modifier.id == it.id) {
                        modifierSet_Modifier.isChecked = true
                    }
                }
            }
        }

        notifyDataSetChanged()

    }
}