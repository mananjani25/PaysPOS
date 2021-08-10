package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.ItemModifierSet
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewOrderModifierSetsBinding

class ItemModifierSetAdapter :
    RecyclerView.Adapter<ItemModifierSetAdapter.MyViewHolder>() {
    var filterList = ArrayList<ItemModifierSet>()
    var selectedModifierList = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifierSetsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var adapter: ItemModifierAdapter? = null

        fun bind(item: ItemModifierSet) {
            binding.model = item
            binding.executePendingBindings()

            if (item.max_allowed == 0 && item.min_required == 0) {
                binding.txtMinMax.visibility = View.GONE
            } else {
                binding.txtMinMax.visibility = View.VISIBLE
                binding.txtMinMax.text =
                    binding.root.context.getString(R.string.pick_up_min) + " " + item.min_required + " " + binding.root.context.getString(
                        R.string.max
                    ) + " " + item.max_allowed
            }

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.layoutManager = GridLayoutManager(binding.root.context, 3);
                adapter = ItemModifierAdapter(item.max_allowed, item.min_required)
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

    fun add(modifierSet: List<ItemModifierSet>) {
        this.filterList = modifierSet as ArrayList<ItemModifierSet>
        notifyDataSetChanged()

    }

    fun getItem(pos: Int): ItemModifierSet {
        return filterList[pos]
    }


    fun getAll(): ArrayList<ItemModifierSet> {
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