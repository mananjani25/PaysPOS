package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.remote.Constants.ADD_VALUE
import com.pays.pos.data.remote.Constants.BALANCE_INQUIRY
import com.pays.pos.data.remote.Constants.SELL_CARD
import com.pays.pos.databinding.ViewItemBinding
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import java.util.*

class ItemListAdapter(private val isChoose: Boolean, private val where: String) :
    RecyclerView.Adapter<ItemListAdapter.MyViewHolder>(), Filterable {
    var itemsList = ArrayList<TbItem>()
    var filterList = ArrayList<TbItem>()
    var selectedItemList = ArrayList<TbItem>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.model = item
            binding.executePendingBindings()
            if (absoluteAdapterPosition == 0) {
                binding.firstviewItems.visibility = View.VISIBLE
            } else {
                binding.firstviewItems.visibility = View.GONE
            }
            if (isChoose) {
                binding.ivCheck.visibility = View.VISIBLE
            } else {
                binding.ivCheck.visibility = View.GONE
            }
            if (where == "tax" || where == "modifier") {
                binding.layoutMenu.imgOrderMenu.gone()
            } else {
                binding.layoutMenu.imgOrderMenu.visible()
            }
        }

        init {
            binding.ivCheck.setOnClickListener {
                filterList[bindingAdapterPosition].isChecked =
                    !filterList[bindingAdapterPosition].isChecked

                if (filterList[bindingAdapterPosition].isChecked) {

                    selectedItemList.add(filterList[bindingAdapterPosition])
                } else {

                    selectedItemList.remove(filterList.get(bindingAdapterPosition))
                }
                notifyDataSetChanged()


            }

            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }

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

    fun addPaginationData(list: ArrayList<TbItem>) {
        filterList.addAll(filterList.size, list)
        /*for (i in 0 until list.size) {

            filterList.add(list[i])
        }*/
        notifyDataSetChanged()
    }

    /*private fun isAllItemsChecked(): Boolean {
            for (selectItem in itemsList) {
                if (!selectItem.isChecked) {
                    return false
                }
            }
            return true
        }*/

    fun selectAll(isChecked: Boolean) {
        selectedItemList.clear()
        if (isChecked) {
            for (selectItem in filterList) {
                selectItem.isChecked = true
                selectedItemList.add(selectItem)
            }
        } else {
            for (selectItem in filterList) {
                selectItem.isChecked = false
                selectedItemList.remove(selectItem)
            }
        }
        notifyDataSetChanged()
    }

    fun selectedItemList(): ArrayList<TbItem> {
        return selectedItemList
    }

    fun selectedItemFromEdit(itemIds: ArrayList<Int>) {
        selectedItemList.clear()
        filterList.forEach { TbItem ->
            itemIds.forEach {
                if (TbItem.itemId == it) {
                    TbItem.isChecked = true
                    selectedItemList.add(TbItem)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun add(categoryModel: List<TbItem>) {
//        this.itemsList = categoryModel as ArrayList<TbItem>
//        this.filterList = categoryModel
        val filteredList = categoryModel.filterNot {
            it.name == SELL_CARD || it.name == BALANCE_INQUIRY || it.name == ADD_VALUE
        }
        this.filterList = ArrayList(filteredList)
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
                        it.name.lowercase(Locale.getDefault())
                            .contains(charString.lowercase(Locale.getDefault()))
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

    fun getAll(): ArrayList<TbItem> {
        return filterList
    }


}
