package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewCartModifierCustomerDisplayBinding
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible

class CartItemModifierAdapterForCustomerDisplay :
    RecyclerView.Adapter<CartItemModifierAdapterForCustomerDisplay.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewCartModifierCustomerDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
            if (item.modifier_quantity > 1) {
                if (item.modifier_quantity>9){
                    binding.txtName.text = "${item.modifier_quantity}x ${item.name}"
                }else{
                    binding.txtName.text = "${item.modifier_quantity}x   ${item.name}"
                }
            }else{
                binding.txtName.text = "       ${item.name}"
            }

            if (MethodUtils.isEnableCashDiscount(itemView.context)) {
                binding.apply {
                    tvRate.gone()
                    tvRateCash.visible()
                    tvRateCard.visible()
                }
            }else{
                binding.apply {
                    tvRate.visible()
                    tvRateCash.gone()
                    tvRateCard.gone()
                }
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartItemModifierAdapterForCustomerDisplay.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCartModifierCustomerDisplayBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CartItemModifierAdapterForCustomerDisplay.MyViewHolder, position: Int) {

        holder.bind(list[holder.bindingAdapterPosition])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(modifierSet: Modifier) {
        list.add(modifierSet)
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): Modifier {
        return list[pos]
    }


    fun getAll(): ArrayList<Modifier> {
        return list
    }

    fun addAll(modifiers: List<Modifier>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }


}