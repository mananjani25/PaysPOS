package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.item.Item
import com.pays.pos.databinding.ViewCreateItemBinding
import com.pays.pos.databinding.ViewDashboardItemBinding

class CategoryItemAdapter(
    val context: Context,
    var list: ArrayList<Item>,
    val listner: CategoryItemList
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class MyViewHolder(private val binding: ViewDashboardItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

//        fun bind(item: VenueDataResponse.Data.Category.Item) {
//            binding.viewModel = item
//            binding.executePendingBindings()
//        }

        init {
            binding.root.setOnClickListener {
                listner.onClick()
            }
        }
    }

    inner class CustomItemHolder(private val binding: ViewCreateItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listner.onClickedCreateItem()
            }


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        if (viewType == 0) {
            val binding = ViewCreateItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return CustomItemHolder(binding)

        } else {
            val binding =
                ViewDashboardItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return MyViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (position == 0) {
            (holder as CustomItemHolder)
        } else {
            //  (holder as MyViewHolder).bind(list[position])
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface CategoryItemList {
        fun onClick()
        fun onClickedCreateItem()
    }

    override fun getItemViewType(position: Int): Int {

        return if (position == 0) {
            0
        } else {
            1
        }

    }
}