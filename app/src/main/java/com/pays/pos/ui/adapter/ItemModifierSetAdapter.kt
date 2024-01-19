package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.databinding.ViewOrderModifierSetsBinding
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.callback.ModifierLongClickCallback

class ItemModifierSetAdapter(
    val viewModel: DashBoardCategoryViewModel,
    private val _itemId: Int,
    private val viewLifecycleOwner: LifecycleOwner
) :
    RecyclerView.Adapter<ItemModifierSetAdapter.MyViewHolder>() {
    var filterList = ArrayList<ModifierSet>()
    var selectedModifierList = ArrayList<Modifier>()
    private var mLongClickcallback: ModifierLongClickCallback? = null
    fun setLongCallback(modifiercallback: ModifierLongClickCallback) {
        mLongClickcallback = modifiercallback
    }
    inner class MyViewHolder(private val binding: ViewOrderModifierSetsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var adapter: ItemModifierAdapter? = null

        @SuppressLint("SetTextI18n")
        fun bind(item: ModifierSet) {
            binding.model = item

            viewModel.getMinMax(_itemId, item.id)?.observe(viewLifecycleOwner) { minMax ->

                if (minMax != null) {
                    if (minMax.maxAllowed == 0 && minMax.minRequired == 0) {
                        binding.txtMinMax.visibility = View.GONE
                    } else {
                        item.min_required = minMax.minRequired
                        item.max_allowed = minMax.maxAllowed
                        binding.txtMinMax.visibility = View.VISIBLE
                        binding.txtMinMax.text =
                            binding.root.context.getString(R.string.pick_up_min) + " " + minMax.minRequired + " " + binding.root.context.getString(
                                R.string.max
                            ) + " " + minMax.maxAllowed
                    }


                } else {
                    binding.txtMinMax.visibility = View.GONE
                }

                if (item.modifiers.isNotEmpty()) {
                    binding.rvModifiers.layoutManager = GridLayoutManager(binding.root.context, 3);
                    adapter = ItemModifierAdapter(item.max_allowed, item.min_required,mLongClickcallback)
                    binding.rvModifiers.adapter = adapter
                    adapter!!.addAll(item.modifiers)
                }
            }





            binding.executePendingBindings()
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


    fun setData(modifierSet: List<ModifierSet>) {
        this.filterList = arrayListOf()
        this.filterList = modifierSet as ArrayList<ModifierSet>
        notifyDataSetChanged()

    }




}