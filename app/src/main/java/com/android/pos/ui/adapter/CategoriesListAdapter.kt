package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.ViewCategoryBinding
import com.android.pos.utils.statusUtils.Resource
import java.util.*
import kotlin.collections.ArrayList

class CategoriesListAdapter :
    RecyclerView.Adapter<CategoriesListAdapter.MyViewHolder>(), Filterable {
    var categoryList = ArrayList<TbCategory>()

    private var filterList = ArrayList<TbCategory>()

    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(filterList[position])

    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filterList = categoryList
                } else {
                    val fList = ArrayList<TbCategory>()
                    for (row in categoryList) {


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
                    filterList = results.values as ArrayList<TbCategory>
                }

                notifyDataSetChanged()

            }
        }
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

    fun getItem(position: Int): TbCategory? {
        return filterList[position]
    }

    fun add(categoryModel: List<TbCategory>) {
        this.categoryList = categoryModel as ArrayList<TbCategory>
        this.filterList = categoryModel
        notifyDataSetChanged()
    }
}