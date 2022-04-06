package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewOpenOrderItemsBinding


class OpenOrderItemsAdapter :
    RecyclerView.Adapter<OpenOrderItemsAdapter.MyViewHolder>() {
    var list = ArrayList<OpenOrderResponse.Data.Order.OrderItem>()

    inner class MyViewHolder(private val binding: ViewOpenOrderItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var adapter: OpenOrderItemModifierAdapter? = null
        fun bind(item: OpenOrderResponse.Data.Order.OrderItem) {
            binding.model = item
            binding.executePendingBindings()


            if (item.orderItemModifiers.isNotEmpty()) {

                binding.rvOpenOrder.addItemDecoration(
                    DividerItemDecoration(
                        binding.root.context,
                        LinearLayoutManager.VERTICAL
                    )
                )

                adapter = OpenOrderItemModifierAdapter()
                binding.rvOpenOrder.adapter = adapter
                adapter!!.addAll(item.orderItemModifiers)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOpenOrderItemsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    fun addAll(modifiers: List<OpenOrderResponse.Data.Order.OrderItem>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }

}