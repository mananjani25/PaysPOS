package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.databinding.ViewOpenOrderItemsBinding
import com.pays.pos.utils.MethodUtils


class OpenOrderItemsAdapter :
    RecyclerView.Adapter<OpenOrderItemsAdapter.MyViewHolder>() {
    var list = ArrayList<OpenOrderResponse.Data.Order.OrderItem>()

    inner class MyViewHolder(private val binding: ViewOpenOrderItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var adapter: OpenOrderItemModifierAdapter? = null
        fun bind(item: OpenOrderResponse.Data.Order.OrderItem) {
            var totalPri = item.totalPrice
            var price = item.price
//            if (item.orderItemModifiers.isNotEmpty()) {
//
//                item.orderItemModifiers.forEach {
//                    totalPri += (it.price * it.quantity)
//                    price += it.price
//                }
//
//
//            }
            binding.txtPrice.text = MethodUtils.roundOffAmount(price)
            binding.CustomFontRegularStyle.text = MethodUtils.roundOffAmount(totalPri)

            binding.model = item
            binding.executePendingBindings()


            if (item.orderItemModifiers.isNotEmpty()) {

/*
                binding.rvOpenOrder.addItemDecoration(
                    DividerItemDecoration(
                        binding.root.context,
                        LinearLayoutManager.VERTICAL
                    )
                )
*/

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