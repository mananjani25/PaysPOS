package com.pays.pos.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.BusinessSettingModel
import com.pays.pos.databinding.ViewBusinessSettingBinding

class BusinessSettingAdapter(
    val context: Context,
    val list: ArrayList<BusinessSettingModel>,
    var listner: BusinessListInterface
) :
    RecyclerView.Adapter<BusinessSettingAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewBusinessSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BusinessSettingModel) {
            binding.model = item
            binding.executePendingBindings()

        }

        init {

            binding.root.setOnClickListener {
                listner.onClick(layoutPosition)

                for (i in 0 until list.size) {
                    if (i == layoutPosition) {
                        list.get(i).isSelected = true
                    } else {
                        list.get(i).isSelected = false

                    }
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BusinessSettingAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewBusinessSettingBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusinessSettingAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface BusinessListInterface {
        fun onClick(pos: Int)
    }
}