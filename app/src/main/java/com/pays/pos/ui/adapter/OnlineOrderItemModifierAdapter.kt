package com.pays.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.databinding.ViewOnlineOrderItemModifiersBinding
import com.pays.pos.databinding.ViewOpenOrderItemModifiersBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson

class OnlineOrderItemModifierAdapter :
    RecyclerView.Adapter<OnlineOrderItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<OnlineOrderResponseModel.Data.OrderItem.OrderItemModifier>()
    private val TAG = "OpenOrderItemModifier"

    inner class MyViewHolder(private val binding: ViewOnlineOrderItemModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OnlineOrderResponseModel.Data.OrderItem.OrderItemModifier) {
            LogUtil.logE(TAG,"ModifierItem:  ${Gson().toJson(item)}")

            binding.txtPrice.text = MethodUtils.roundOffAmount(item.price)
            binding.txtCustomerName.text = MethodUtils.roundOffAmount(item.price * item.quantity)

            binding.model = item
            binding.executePendingBindings()
            if (item.modifier_quantity > 1) {
                if (item.modifier_quantity>9){
                    binding.txtItemName.text = "${item.modifier_quantity}x ${item.name}"
                }else{
                    binding.txtItemName.text = "${item.modifier_quantity}x   ${item.name}"
                }
            }else{
                binding.txtItemName.text = "       ${item.name}"
            }

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOnlineOrderItemModifiersBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    fun addAll(modifiers: List<OnlineOrderResponseModel.Data.OrderItem.OrderItemModifier>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }

}