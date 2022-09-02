package com.android.pos.ui.adapter.boldpos

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewCategoryItemBoldBinding
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.extensions.gone

class ItemAdapterPagDash(
    val listener: ItemListner,
    var lastChecked: TextView? = null
) : PagingDataAdapter<TbItem, ItemAdapterPagDash.ViewHolder>(
    ItemListPageAdapter.DIFF_CALLBACK
) {

    private var mpos: Int = -2


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemAdapterPagDash.ViewHolder {
        return ViewHolder(
            ViewCategoryItemBoldBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    inner class ViewHolder(val binding: ViewCategoryItemBoldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val checkedTextView = binding.txtCategoryName
        var itename_price: StringBuffer = StringBuffer()

        @SuppressLint("ResourceType")
        fun bind(model: TbItem?, position: Int) {
            if (model?.name?.length!! > 30) {
                itename_price.append(
                    model?.name.substring(
                        0,
                        30
                    ) + "...\n" + model?.price?.let { MethodUtils.roundOffAmount(it) })
                binding.txtCategoryName.text = itename_price
            } else {
                binding.txtCategoryName.text =
                    "" + model?.name + "\n\n" + model?.price?.let { MethodUtils.roundOffAmount(it) }
            }

            binding.txtPrice.gone()
            if (mpos == absoluteAdapterPosition) {

                if (lastChecked != null) {
                    lastChecked?.isSelected = false
                }
                lastChecked = checkedTextView
                binding.txtCategoryName.isSelected = false
            } else {
                binding.txtCategoryName.isSelected = false

            }

            binding.root.setOnClickListener {
                try {
                    getItem(position)?.let {
                        LogUtil.logE("ITemAdapter", "onClickposition  ${position}")
                        listener.onItemSelected(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (lastChecked != null) {
                    lastChecked?.isSelected = false
                }
                lastChecked = checkedTextView


            }
        }


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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)

    }

    fun setPos(selectedId: Int) {
        mpos = selectedId
    }


}