package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.databinding.ViewDineInItemBinding
import com.android.pos.utils.callback.MyCallback
import com.google.gson.Gson

class DineInAdapter : RecyclerView.Adapter<DineInAdapter.MyViewHolder>(), MyCallback {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private lateinit var listner: DineInCallback
    private lateinit var itemAdapter: CartAdapter
    private val TAG = "DineInAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DineInAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DineInAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInItemBinding) :
        RecyclerView.ViewHolder(binding.root), MyCallback {
        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(model: DineInModel) {
            binding.model = model
            binding.executePendingBindings()
            itemAdapter = CartAdapter()
            binding.rvCart.adapter = itemAdapter

            itemAdapter.addCart(list.get(layoutPosition).items)
            binding.txtTableName.setText(list.get(layoutPosition).title)
            if (layoutPosition == list.get(0).selectedPosition) {
                binding.rvCart.visibility = View.VISIBLE
                binding.constraintHeader.setBackground(binding.root.context.getDrawable(R.drawable.background_dine_in_selected))
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.white))

                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                }

            } else {
                binding.rvCart.visibility = View.VISIBLE
                binding.constraintHeader.setBackground(
                    binding.root.context.resources.getDrawable(R.drawable.background_dine_in_unselected)
                )
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.txtColor))
                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.txtColor))
                }
            }


            if (layoutPosition == 0) {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_simple_table))

            } else {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_group_person))
            }

            itemAdapter.setCallback(this)
        }

        init {

            binding.constraintHeader.setOnClickListener {

                //  listner.onHeaderSelected(layoutPosition)
                list.get(0).selectedPosition = layoutPosition
                notifyDataSetChanged()
            }
        }

        override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {
            Log.e(TAG, "DineInMyView ${Gson().toJson(data)}")
            list.get(0).itemPosition = position
            position?.let { listner.onItemSelected(layoutPosition, it, data) }

        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: TbItem, selectedPos: Int) {
        val items: ArrayList<TbItem> = list.get(selectedPos).items
        items.add(item)
        list.add(selectedPos, DineInModel(0, false, selectedPos, items = items))
        notifyDataSetChanged()
    }

    fun setListner(listner: DineInCallback) {
        this.listner = listner
    }

    interface DineInCallback {
        fun onHeaderSelected(position: Int)
        fun onItemSelected(headerPosition: Int, position: Int, item: TbItem)
    }

    fun getHeaderPosition(): Int {
        return list.get(0).selectedPosition
    }

    fun getList(): ArrayList<DineInModel> {
        return list
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {
        Log.e(TAG, "DineInItem:  ${Gson().toJson(data)}")
    }


}