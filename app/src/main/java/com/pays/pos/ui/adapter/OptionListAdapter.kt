package com.pays.pos.ui.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.databinding.ViewOptionListBinding
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*

class OptionListAdapter :
    RecyclerView.Adapter<OptionListAdapter.MyViewHolder>(), Filterable {

    var list = ArrayList<OptionSet>()
    var filterList = ArrayList<OptionSet>()
    private val TAG = "OptionListAdapter"
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
    inner class MyViewHolder(private val binding: ViewOptionListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OptionSet) {
            binding.model = item
            if(absoluteAdapterPosition==0){
                binding.firstviewOption.visibility = View.VISIBLE
            }else{
                binding.firstviewOption.visibility = View.GONE
            }
            binding.executePendingBindings()



        }
        init {
            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }


        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OptionListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOptionListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OptionListAdapter.MyViewHolder, position: Int) {

        holder.bind(filterList[position])
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(modifierSet: List<OptionSet>) {
        this.list = modifierSet as ArrayList<OptionSet>
        this.filterList = modifierSet
        notifyDataSetChanged()

    }

    fun getItem(pos: Int): OptionSet {
        return filterList[pos]
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        Log.e(TAG,"fromPosition:   ${fromPosition}  toPosition:  ${toPosition}")
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

  /*  fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
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
    }*/
    fun getAll(): ArrayList<OptionSet> {
        return filterList
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filterList = list
                } else {
                    val fList = ArrayList<OptionSet>()
                    for (row in list) {


                        if (!TextUtils.isEmpty(row.name) && row.name.lowercase(Locale.getDefault())
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
                    filterList = results.values as ArrayList<OptionSet>
                }

                notifyDataSetChanged()

            }
        }
    }

    /*override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase()
                filterList = if (charString.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<OptionSet>()
                    list.filter {
                        it.name.lowercase(Locale.getDefault()).contains(charSequence)
                    }.forEach { fList.add(it) }
                    fList
                }
                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<OptionSet>
                }
                notifyDataSetChanged()

            }
        }
    }*/
}