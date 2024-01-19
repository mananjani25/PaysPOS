package com.pays.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.databinding.ViewBoldVariationsBinding
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel

class ModifiersAdapter(
    val viewModel: DashBoardCategoryViewModel,
    private val _itemId: Int,
    private val viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<ModifiersAdapter.MyViewHolder>() {
    var filterList = ArrayList<ModifierSet>()
    var selectedModifierList = ArrayList<Modifier>()


    inner class MyViewHolder(private var binding: ViewBoldVariationsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModifierSet) {
            binding.txtVariation.text = item.name


        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifiersAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewBoldVariationsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifiersAdapter.MyViewHolder, position: Int) {
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


    private fun maxLogic(
        maxCount: Int,
        modifiers: List<Modifier>
    ): Boolean {

        if (maxCount == 0) {
            return true
        }
        var totalMinMax = 0

        modifiers.forEach {
            if (it.isChecked) {
                totalMinMax += 1
            }
        }

        return maxCount >= totalMinMax
    }

}