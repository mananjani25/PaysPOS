package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.remote.Constants.DEFAULT_CATEGORY
import com.android.pos.data.remote.Constants.GIFT_CARD_CATEGORY
import com.android.pos.databinding.ViewCategoryBinding
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.invisible
import com.android.pos.utils.extensions.setOnSingleClickListener
import com.android.pos.utils.extensions.visible
import java.util.*
import kotlin.collections.ArrayList

class CategoriesListAdapter(private val isChoose: Boolean) :
    RecyclerView.Adapter<CategoriesListAdapter.MyViewHolder>(), Filterable {
    var categoryList = ArrayList<TbCategory>()
    private var mpos: Int = -2
    private var filterList = ArrayList<TbCategory>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
    inner class MyViewHolder(private val binding: ViewCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbCategory) {
            binding.model = item
            binding.executePendingBindings()
            if (absoluteAdapterPosition == 0) {
                binding.firstviewCategory.visibility = View.VISIBLE
            } else {
                binding.firstviewCategory.visibility = View.GONE
            }
            if (isChoose) {
                binding.layoutMenu.imgOrderMenu.gone()
                if (mpos == absoluteAdapterPosition) {
                    binding.imageCheck.setImageResource(R.drawable.ic_outline_radio_button_checked)

                } else {
                    binding.imageCheck.setImageResource(R.drawable.ic_uncheck_circle)
                }
            } else {
                binding.imageCheck.setImageResource(R.drawable.ic_baseline_menu)
            }

            if (!isChoose) {
                if (item.name == GIFT_CARD_CATEGORY) {
                    binding.layoutMenu.imgOrderMenu.gone()
                    binding.imageCheck.invisible()
                }else if (item.name == DEFAULT_CATEGORY){
                    binding.imageCheck.invisible()
                } else {
                    binding.layoutMenu.imgOrderMenu.visible()
                    binding.imageCheck.visible()
                }
            }
        }

        init {


            binding.imageCheck.setOnClickListener {
                if (isChoose) {
                    mpos = absoluteAdapterPosition
                    notifyDataSetChanged()
                }
            }


            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
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

    fun getPositionOf(itemName:String):Int{
        for (i in 0 until filterList.size){
            if (filterList[i].name == itemName){
                return i
            }
        }
        return -1
    }

    fun add(categoryModel: List<TbCategory>) {

        categoryList.clear()
        filterList.clear()
        categoryList.addAll(categoryModel)
        filterList.addAll(categoryModel)
        notifyDataSetChanged()
    }

    fun getData(): TbCategory? {
        if (mpos < 0) {
            return null
        }
        return filterList[mpos]
    }

    fun getPos(): Int {
        return mpos
    }

    fun setPos(selectedId: Int) {

        /*for (i in filterList.indices) {
            if (filterList[i].id == selectedId) {
                mpos = i
                break
            }
        }*/

        mpos = filterList.indexOfFirst {
            it.id == selectedId
        }


    }

    fun getAll(): ArrayList<TbCategory> {

        return filterList
    }
}