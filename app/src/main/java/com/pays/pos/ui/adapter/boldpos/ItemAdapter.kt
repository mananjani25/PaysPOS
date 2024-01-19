package com.pays.pos.ui.adapter.boldpos

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbItem
import com.pays.pos.databinding.ViewCategoryItemBoldBinding
import com.pays.pos.ui.adapter.CategoryItemAdapter1
import com.pays.pos.ui.adapter.ItemListAdapter
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.callback.ItemListner
import com.pays.pos.utils.extensions.gone

class ItemAdapter(
    val context: Context,
    var list: ArrayList<TbItem?>,
    val listener: ItemListner,
    var lastChecked: TextView? = null
) : RecyclerView.Adapter<ItemAdapter.MyViewHolder>() {
    private var mpos: Int = -2

    inner class MyViewHolder(private val binding: ViewCategoryItemBoldBinding) :
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
                    list[position]?.let {
                        LogUtil.logE("ITemAdapter", "onClickposition  ${position}")
                        listener.onItemSelected(it, position)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.MyViewHolder {
        val binding =
            ViewCategoryItemBoldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position), position)

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

    fun setPos(selectedId: Int) {
        mpos = selectedId
    }

}