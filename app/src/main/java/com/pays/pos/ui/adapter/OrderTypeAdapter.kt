package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DEFAULT_ORDER
import com.pays.pos.databinding.ViewOrderTypeBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.callback.ItemCallback

class OrderTypeAdapter(val isFromTypeChangeDialog: Boolean = false, val prefProvider: PrefProvider? = null) :
    RecyclerView.Adapter<OrderTypeAdapter.MyViewHolder>() {
    var list = ArrayList<TbOrderType>()

    private lateinit var mCallback: ItemCallback
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }


    inner class MyViewHolder(private val binding: ViewOrderTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TbOrderType) {
            binding.model = item
            binding.executePendingBindings()
            if(isFromTypeChangeDialog) {
                if(item.name == prefProvider?.getValue(Constants.ORDER_TYPE_NAME, DEFAULT_ORDER)){
                    binding.root.setBackgroundResource(R.drawable.border_orange)
                }else{
                    binding.root.setBackgroundResource(R.drawable.background_square_border_grey)
                }
            } else {
                if (item.name == DEFAULT_ORDER) {
                    binding.root.setBackgroundResource(R.drawable.border_orange)
                } else {
                    binding.root.setBackgroundResource(R.drawable.background_square_border_grey)
                }
            }
        }

        init {

            binding.root.setOnClickListener {
                mCallback.onItemClickListener(it, bindingAdapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderTypeAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderTypeBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OrderTypeAdapter.MyViewHolder, position: Int) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(orderTypes: TbOrderType) {
        list.add(orderTypes)
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): TbOrderType {
        return list[pos]
    }


    fun getAll(): ArrayList<TbOrderType> {
        return list
    }

    fun addAll(orderTypes: List<TbOrderType>) {
        list = orderTypes as ArrayList<TbOrderType>
        notifyDataSetChanged()
    }


}