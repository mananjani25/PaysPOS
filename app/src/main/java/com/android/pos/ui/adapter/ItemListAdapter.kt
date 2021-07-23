package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemBinding
import java.util.*
import kotlin.collections.ArrayList

class ItemListAdapter() :
    RecyclerView.Adapter<ItemListAdapter.MyViewHolder>(), Filterable {
    var itemsList = ArrayList<TbItem>()
    var filterList = ArrayList<TbItem>()

    inner class MyViewHolder(private val binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemListAdapter.MyViewHolder, position: Int) {
        holder.bind(filterList[position])

    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(categoryModel: List<TbItem>) {
        this.itemsList = categoryModel as ArrayList<TbItem>
        this.filterList = categoryModel
        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbItem {
        return filterList[position]
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(filterList, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(filterList, i, i - 1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

    fun remove(deleteObj: TbItem?, deletePos: Int) {
        filterList.remove(deleteObj)
        notifyItemRemoved(deletePos)

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    itemsList
                } else {
                    val fList = ArrayList<TbItem>()

                    itemsList.filter {
                        it.name.lowercase(Locale.getDefault()).contains(charSequence)
                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<TbItem>
                }

                notifyDataSetChanged()

            }
        }
    }
}
