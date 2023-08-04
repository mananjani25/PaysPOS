package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants.ADD_VALUE
import com.android.pos.data.remote.Constants.BALANCE_INQUIRY
import com.android.pos.data.remote.Constants.SELL_CARD
import com.android.pos.databinding.ViewItemBinding
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.setOnSingleClickListener
import com.android.pos.utils.extensions.visible
import java.util.*

class ItemListPageAdapter :
    PagingDataAdapter<TbItem, ItemListPageAdapter.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemListPageAdapter.ViewHolder {
        return ViewHolder(
            ViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    inner class ViewHolder(val binding: ViewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tbItem: TbItem?) {
            binding.model = tbItem

            if (absoluteAdapterPosition == 0) {
                binding.firstviewItems.visibility = View.VISIBLE
            } else {
                binding.firstviewItems.visibility = View.GONE
            }

            binding.ivCheck.visibility = View.GONE
            binding.layoutMenu.imgOrderMenu.visible()

            if (tbItem != null) {
                if(tbItem.name == SELL_CARD || tbItem.name == ADD_VALUE || tbItem.name == BALANCE_INQUIRY) {
                    binding.layoutMenu.imgOrderMenu.gone()
                    binding.tvItemPrice.gone()
                } else {
                    binding.layoutMenu.imgOrderMenu.visible()
                    binding.tvItemPrice.visible()
                }
            }
            binding.executePendingBindings()
        }

        init {

            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }

        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TbItem>() {
            override fun areItemsTheSame(oldItem: TbItem, newItem: TbItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TbItem, newItem: TbItem): Boolean {
                return oldItem.itemId == newItem.itemId
            }

        }
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(snapshot().items, i, i + 1)

                        val order1: Int? = snapshot().items.get(i)?.sort
                        val order2: Int? = snapshot().items.get(i+1)?.sort
                        if (order2 != null) {
                            snapshot().items.get(i)?.sort = order2
                        }
                        if (order1 != null) {
                            snapshot().items.get(i+1)?.sort = order1
                        }
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(snapshot().items, i, i - 1)

                        val order1: Int? = snapshot().items.get(i)?.sort
                        val order2: Int? = snapshot().items.get(i-1)?.sort
                        if (order2 != null) {
                            snapshot().items.get(i)?.sort = order2
                        }
                        if (order1 != null) {
                            snapshot().items.get(i - 1)?.sort = order1
                        }
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }


}