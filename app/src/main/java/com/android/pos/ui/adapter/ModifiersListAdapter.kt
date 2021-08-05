package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.ModifierSet
import com.android.pos.databinding.ViewModifiersBinding
import java.util.*
import kotlin.collections.ArrayList

class ModifiersListAdapter :
    RecyclerView.Adapter<ModifiersListAdapter.MyViewHolder>(), Filterable {
    var list = ArrayList<ModifierSet>()
    var filterList = ArrayList<ModifierSet>()

    inner class MyViewHolder(private val binding: ViewModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierSet) {
            binding.model = item
            binding.executePendingBindings()


            val builder = StringBuilder()
            if (item.modifiers.isNotEmpty()) {
                item.modifiers.forEach {
                    if (it.name.isNotEmpty())
                        builder.append(it.name.trim() + ",")
                }
            }
            if (builder.isNotEmpty()) {
                binding.modifiers.visibility = View.VISIBLE
                binding.modifiers.text = builder.substring(0, builder.length - 1).toString()
            } else {
                binding.modifiers.visibility = View.GONE
            }
        }

        init {

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifiersListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModifiersBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifiersListAdapter.MyViewHolder, position: Int) {

        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(modifierSet: List<ModifierSet>) {
        this.list = modifierSet as ArrayList<ModifierSet>
        this.filterList = modifierSet
        notifyDataSetChanged()

    }

    fun getItem(pos: Int): ModifierSet {
        return filterList[pos]
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(filterList, i, i + 1)


//                        val order1: Int = filterList[i].sort
//                        val order2: Int = filterList[i + 1].sort
//                        filterList[i].sort = order2
//                        filterList[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(filterList, i, i - 1)

//                        val order1: Int = filterList[i].sort
//                        val order2: Int = filterList[i - 1].sort
//                        filterList[i].sort = (order2)
//                        filterList[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

    fun getAll(): ArrayList<ModifierSet> {
        return filterList
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<ModifierSet>()
                    list.filter {
                        it.name.lowercase(Locale.getDefault()).contains(charSequence)
                    }.forEach { fList.add(it) }
                    fList
                }
                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<ModifierSet>
                }
                notifyDataSetChanged()

            }
        }
    }
}