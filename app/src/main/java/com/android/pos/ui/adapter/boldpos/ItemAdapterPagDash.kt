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
import com.android.pos.utils.extensions.visible

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


        @SuppressLint("ResourceType")
        fun bind(model: TbItem?, position: Int) {
            var itename_price: StringBuffer = StringBuffer()

            if (model?.name?.length!! >= 30) {
                if (getItemPriceIsValid(model.price).isNotEmpty()){
                    itename_price.append(
                        model.name.substring(
                            0,
                            30
                        ) + "...\n" + getItemPriceIsValid(model.price)
                    )
                    binding.txtCategoryName.text = itename_price
                }else{
                    itename_price.append(
                        model.name.substring(
                            0,
                                40
                        ) + "..."
                    )
                    binding.txtCategoryName.text = itename_price
                }
            } else {
                if (getItemPriceIsValid(model.price).isNotEmpty()){
                    binding.txtCategoryName.text =
                        "" + model.name + "\n\n" + getItemPriceIsValid(model.price)
                }else {
                    binding.txtCategoryName.text = "" + model.name
                }

            }

            if (model.modifier_set_ids.isNotEmpty()) {
                binding.viewLineFormodifier.visible()
            } else {
                binding.viewLineFormodifier.gone()
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
                        LogUtil.logE(
                            "ITemAdapter",
                            "onClickposition  ${position}  itemname ${it.name}"
                        )
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

    fun getItemPriceIsValid(amount: Double): String {
        if (amount > 0.0) {
            return amount.let { MethodUtils.roundOffAmount(it) }
        }else{
            return ""
        }


    }

    fun setPos(selectedId: Int) {
        mpos = selectedId
    }

    fun clearData() {
        snapshot().toCollection(arrayListOf()).clear()
        notifyDataSetChanged()
    }


}