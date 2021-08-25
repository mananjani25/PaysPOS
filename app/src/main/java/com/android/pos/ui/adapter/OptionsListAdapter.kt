package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.ViewCategoryBinding
import java.util.*
import kotlin.collections.ArrayList

class OptionsListAdapter(private val isChoose: Boolean) :
    RecyclerView.Adapter<OptionsListAdapter.MyViewHolder>(), Filterable {
    var categoryList = ArrayList<TbCategory>()
    private var mpos: Int = -2
    private var filterList = ArrayList<TbCategory>()

    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()

            if (isChoose) {
                if (mpos == layoutPosition) {
                    binding.imageCheck.setImageResource(R.drawable.ic_outline_radio_button_checked)
                } else {
                    binding.imageCheck.setImageResource(R.drawable.ic_uncheck_circle)
                }
            } else {
                binding.imageCheck.setImageResource(R.drawable.ic_arrow_forward)
            }
        }

        init {


            binding.imageCheck.setOnClickListener {
                if (isChoose) {
                    mpos = layoutPosition
                    notifyDataSetChanged()
                }
            }

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

    fun getItem(position: Int): TbCategory {
        return filterList[position]
    }

    fun add(categoryModel: List<TbCategory>) {

        categoryList.clear()
        filterList.clear()
        if (isChoose) {
            categoryList.add(0, TbCategory().apply {
                name = "None"
            })
            filterList.add(0, TbCategory().apply {
                name = "None"
            })
        }

        categoryList.addAll(categoryModel)
        filterList.addAll(categoryModel)
        notifyDataSetChanged()
    }

    fun getData(): TbCategory? {
        if (mpos == -2) {
            return null
        }
        return filterList[mpos]
    }

    fun getPos(): Int {
        return mpos
    }

    fun setPos(selectedId: Int) {
        mpos = selectedId

    }

    fun getAll(): ArrayList<TbCategory> {

        return filterList
    }
}