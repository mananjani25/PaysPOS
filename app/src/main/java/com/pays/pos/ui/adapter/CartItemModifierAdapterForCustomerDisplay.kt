package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewCartModifierCustomerDisplayBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class CartItemModifierAdapterForCustomerDisplay :
    RecyclerView.Adapter<CartItemModifierAdapterForCustomerDisplay.MyViewHolder>() {
    private lateinit var prefProvider: PrefProvider
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewCartModifierCustomerDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            prefProvider = PrefProvider(itemView.context)
            val showCashCreditPrice = prefProvider.getValueboolean(Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY, false)
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

            if (MethodUtils.isEnableCashDiscount(itemView.context) && showCashCreditPrice) {
                binding.apply {
                    tvRate.gone()
                    tvRateCash.visible()
                    tvRateCard.visible()

                    val cashOrSurchargeAmount = MethodUtils.calculateCashDiscount(
                        totalPrice(item),
                        prefProvider,
                        itemView.context
                    )
                    if (prefProvider.getValue(
                            Constants.OPTION_TYPE,
                            "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        MethodUtils.setPriceTextView(
                            binding.tvRateCash,
                            totalPrice(item) - cashOrSurchargeAmount
                        )
                        MethodUtils.setPriceTextView(binding.tvRateCard, totalPrice(item))
                    } else {
                        MethodUtils.setPriceTextView(binding.tvRateCash, totalPrice(item))
                        MethodUtils.setPriceTextView(
                            binding.tvRateCard,
                            totalPrice(item) + cashOrSurchargeAmount
                        )
                    }
                }
            }else{
                binding.apply {
                    tvRate.visible()
                    MethodUtils.setPriceTextView(binding.tvRate, totalPrice(item))
                    tvRateCash.gone()
                    tvRateCard.gone()
                }
            }
        }

    }

    private fun totalPrice(model: Modifier): Double {
        return model.price * model.itemQuantity
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