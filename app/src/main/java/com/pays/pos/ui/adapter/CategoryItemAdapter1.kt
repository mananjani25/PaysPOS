package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbItem
import com.pays.pos.databinding.ViewCreateItemBinding
import com.pays.pos.databinding.ViewDashboardItemBinding

class CategoryItemAdapter1(
    val context: Context,
    var list: ArrayList<TbItem?>,
    val listner: CategoryItemList
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MyViewHolder(private val binding: ViewDashboardItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem) {
            binding.viewModel = item
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                list[bindingAdapterPosition]?.let { it1 -> listner.onClick(item = it1) }
            }
        }

    }

    inner class CustomItemHolder(private val binding: ViewCreateItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {

            list.filter { it!!.isHide }

            binding.root.setOnClickListener {
                listner.onClickedCreateItem()
            }


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == 0) {
            val binding = ViewCreateItemBinding.inflate(LayoutInflater.from(context), parent, false)
            CustomItemHolder(binding)

        } else {
            val binding =
                ViewDashboardItemBinding.inflate(LayoutInflater.from(context), parent, false)
            MyViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (position == 0) {
            (holder as CustomItemHolder)
        } else {
            list[position]?.let { (holder as MyViewHolder).bind(it) }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface CategoryItemList {
        fun onClick(item: TbItem)
        fun onClickedCreateItem()
    }

    override fun getItemViewType(position: Int): Int {

        return if (position == 0) {
            0
        } else {
            1
        }

    }

    fun addAll(itemList: java.util.ArrayList<TbItem?>) {
        list = itemList
        notifyDataSetChanged()
    }
}