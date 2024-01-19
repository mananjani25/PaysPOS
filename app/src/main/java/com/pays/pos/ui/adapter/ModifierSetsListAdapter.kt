package com.pays.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.databinding.ViewModifierSetsBinding
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*

class ModifierSetsListAdapter(val isCreateItem: Boolean) :
    RecyclerView.Adapter<ModifierSetsListAdapter.MyViewHolder>(), Filterable {
    var list = ArrayList<ModifierSet>()
    var filterList = ArrayList<ModifierSet>()
    var selectedItemList = ArrayList<ModifierSet>()
    private var mCallback: ItemCallback? = null
    private var deleteCallback: ModifierCallback? = null
    private var onModDelete: ModifierDeleteCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    fun setDeleteCallback(callback: ModifierCallback) {
        deleteCallback = callback

    }

    fun onDelteCallbackMod(callback: ModifierDeleteCallback) {
        onModDelete = callback
    }

    inner class MyViewHolder(private val binding: ViewModifierSetsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierSet) {
            binding.model = item
            binding.executePendingBindings()

            if (absoluteAdapterPosition == 0) {
                binding.firstviewModifier.visibility = View.VISIBLE
            } else {
                binding.firstviewModifier.visibility = View.GONE
            }
            if (isCreateItem) {
                //  binding.imgCheck.visibility = View.VISIBLE
                binding.imgReorder.visibility = View.GONE
            } else {
                //binding.imgCheck.visibility = View.GONE
                binding.imgReorder.visibility = View.VISIBLE
            }

            val builder = StringBuilder()
            if (item.modifiers.isNotEmpty()) {

                item.modifiers.sortedBy {
                    it.sort
                }

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
            binding.root.setOnClickListener {
                filterList[layoutPosition].isChecked = !filterList[layoutPosition].isChecked

                if (filterList[layoutPosition].isChecked) {
                    selectedItemList.add(filterList[layoutPosition])
                } else {
                    selectedItemList.remove(filterList.get(layoutPosition))
                }
                notifyDataSetChanged()
            }
            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }

            binding.imgDelete.setOnClickListener {
                onModDelete?.onDelete(bindingAdapterPosition)

            }


        }

    }

    fun selectedItemList(): ArrayList<ModifierSet> {
        return selectedItemList
    }

    fun selectedItemFromEdit(itemIds: ArrayList<Int>) {
        selectedItemList.clear()
        filterList.forEach { modifierSet ->
            itemIds.forEach {
                if (modifierSet.id == it) {
                    modifierSet.isChecked = true
                    selectedItemList.add(modifierSet)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifierSetsListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModifierSetsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)


    }

    override fun onBindViewHolder(holder: ModifierSetsListAdapter.MyViewHolder, position: Int) {

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

    fun addItem(mod: ModifierSet) {
        this.filterList.add(mod)
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


                        val order1: Int = filterList[i].sort
                        val order2: Int = filterList[i + 1].sort
                        filterList[i].sort = order2
                        filterList[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(filterList, i, i - 1)

                        val order1: Int = filterList[i].sort
                        val order2: Int = filterList[i - 1].sort
                        filterList[i].sort = (order2)
                        filterList[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

 /*   fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(filterList, i, i + 1)


                        val order1: Int = filterList[i].sort
                        val order2: Int = filterList[i + 1].sort
                        filterList[i].sort = order2
                        filterList[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(filterList, i, i - 1)

                        val order1: Int = filterList[i].sort
                        val order2: Int = filterList[i - 1].sort
                        filterList[i].sort = (order2)
                        filterList[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }
*/


    fun removeItem(pos:Int){
        filterList.removeAt(pos)
        notifyDataSetChanged()

    }
    fun getAll(): ArrayList<ModifierSet> {
        return filterList
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filterList = list
                } else {
                    val fList = ArrayList<ModifierSet>()
                    for (row in list) {


                        if (!TextUtils.isEmpty(row.name) && row.name?.lowercase(Locale.getDefault())!!
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(row)
                        }

                    }
                    filterList = fList
                }

                val filterResults = FilterResults()
                filterResults.values = filterList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<ModifierSet>
                }

                notifyDataSetChanged()

            }
        }
    }

    /*override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase(Locale.getDefault())
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
    }*/

    interface ModifierCallback {
        fun onDeleteCallback(modifierSet: ModifierSet)
    }

    interface ModifierDeleteCallback {
        fun onDelete(pos: Int)
    }
}