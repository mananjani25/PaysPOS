package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewCategoryItemBoldBinding
import com.android.pos.ui.adapter.CategoryItemAdapter1
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.extensions.visible

class ItemAdapter(
    val context: Context,
    var list: ArrayList<TbItem?>,
    val listener: CategoryItemAdapter1.CategoryItemList
) : RecyclerView.Adapter<ItemAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewCategoryItemBoldBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: TbItem?) {
            binding.txtCategoryName.text = "" + model?.name
            binding.txtPrice.visible()
            binding.txtPrice.text = model?.price?.let { MethodUtils.roundOffAmount(it) }

        }

        init {

            binding.linearItem.setOnClickListener {
                list[bindingAdapterPosition]?.let { listener.onClick(it) }
                binding.txtCategoryName.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.txtPrice.setTextColor(binding.root.resources.getColor(R.color.white))
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.MyViewHolder {
        val binding =
            ViewCategoryItemBoldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addList(itemList1: ArrayList<TbItem?>) {
        list.clear()
        list = arrayListOf()
        list.addAll(itemList1)
        notifyDataSetChanged()

    }

    fun clearList() {
        this.list.clear()
        this.list = arrayListOf()
        notifyDataSetChanged()
    }
}