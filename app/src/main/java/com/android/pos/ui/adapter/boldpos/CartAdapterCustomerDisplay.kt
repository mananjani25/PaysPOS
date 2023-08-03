package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewItemCartCustomerDisplayBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.CartItemModifierAdapter
import com.android.pos.ui.adapter.CartItemModifierAdapterForCustomerDisplay
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.strike
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson

class CartAdapterCustomerDisplay : RecyclerView.Adapter<CartAdapterCustomerDisplay.MyViewHolder>() {
    private lateinit var prefProvider: PrefProvider
    var cartList = ArrayList<TbItem>()
    private val TAG = "CartAdapter"


    private lateinit var mCallback: MyCallback

    fun setCallback(callback: MyCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewItemCartCustomerDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TbItem, pos: Int) {
            prefProvider = PrefProvider(itemView.context)
            val showCashCreditPrice = prefProvider.getValueboolean(Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY, false)
            LogUtil.logE(TAG, "itemprice:  ${item.price}")
            binding.txtName.text = item.name
            binding.txtQuantity.text = "x" + item.itemQuantity
            binding.txtEachQntPrice.text = MethodUtils.roundOffAmount((item.price))
            MethodUtils.setPriceTextView(binding.txtEachQntPrice, totalEachPrice(item))

            if (MethodUtils.isEnableCashDiscount(itemView.context) && showCashCreditPrice && prefProvider.getValue(
                    Constants.ORDER_TYPE,
                    Constants.TAKEOUT
                ) != Constants.GIFT_CARD) {
                binding.txtTotalPrice.gone()
                binding.txtCashAmount.visible()
                binding.txtCardAmount.visible()

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
                        binding.txtCashAmount,
                        totalPrice(item) - cashOrSurchargeAmount
                    )
                    MethodUtils.setPriceTextView(binding.txtCardAmount, totalPrice(item))
                } else {
                    MethodUtils.setPriceTextView(binding.txtCashAmount, totalPrice(item))
                    MethodUtils.setPriceTextView(
                        binding.txtCardAmount,
                        totalPrice(item) + cashOrSurchargeAmount
                    )
                }


            } else {
                binding.txtTotalPrice.visible()
                MethodUtils.setPriceTextView(binding.txtTotalPrice, totalPrice(item))
                binding.txtCashAmount.gone()
                binding.txtCardAmount.gone()
            }

            if (item.discountPrice != 0.0) {

                var dPrice = 0.0
                var total_price_fordiscount = 0.0
                total_price_fordiscount += item.price * item.itemQuantity
                if (item.modifiers.isNotEmpty()) {
                    item.modifiers.forEach { it ->
                        total_price_fordiscount += it.price * it.itemQuantity
                    }
                }
                dPrice = total_price_fordiscount - (item.discountPrice * item.itemQuantity)

                binding.lnrDiscountRates?.visibility = View.VISIBLE
                if (MethodUtils.isEnableCashDiscount(itemView.context) && showCashCreditPrice) {
                    binding.txtCashAmount.strike = true
                    binding.tvCashDiscountRate?.visible()
                    binding.txtCardAmount.strike = true
                    binding.tvCardDiscountRate?.visible()

                    binding.tvDiscountRate.gone()

                    val cashOrSurchargeAmount = MethodUtils.calculateCashDiscount(
                        dPrice,
                        prefProvider,
                        itemView.context
                    )

                    if (prefProvider.getValue(
                            Constants.OPTION_TYPE,
                            "CashDiscount"
                        ) == "CashDiscount"
                    ) {
                        MethodUtils.setPriceTextView(binding.tvCashDiscountRate!!, dPrice - cashOrSurchargeAmount)
                        MethodUtils.setPriceTextView(binding.tvCardDiscountRate!!, dPrice)
                    }else{
                        MethodUtils.setPriceTextView(binding.tvCashDiscountRate!!, dPrice)
                        MethodUtils.setPriceTextView(binding.tvCardDiscountRate!!, dPrice + cashOrSurchargeAmount)
                    }

                }else{
                    binding.txtTotalPrice.strike = true
                    binding.tvDiscountRate.visible()

                    binding.tvCashDiscountRate?.gone()
                    binding.tvCardDiscountRate?.gone()

                    MethodUtils.setPriceTextView(binding.tvDiscountRate, dPrice)
                }





            } else {
                binding.lnrDiscountRates?.visibility = View.GONE

            }

            if (item.note.isEmpty()) {
                binding.txtNote.visibility = View.GONE
            } else {
                binding.txtNote.visibility = View.VISIBLE
                binding.txtNote.text = "Note: " + item.note
            }

            if (item.modifiers.isNotEmpty()) {
                binding.rvModifiers.visibility = View.VISIBLE
                val adapter = CartItemModifierAdapterForCustomerDisplay()
                binding.rvModifiers.adapter = adapter
                LogUtil.logE(TAG, "dineinMod  ${Gson().toJson(item.modifiers)}")
                adapter.addAll(item.modifiers)
            } else {
                binding.rvModifiers.visibility = View.GONE
            }


        }

        init {

            binding.root.setOnClickListener {

                mCallback.onItemClickListener(
                    it,
                    cartList[bindingAdapterPosition],
                    bindingAdapterPosition
                )
            }
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartAdapterCustomerDisplay.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemCartCustomerDisplayBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CartAdapterCustomerDisplay.MyViewHolder, position: Int) {
        holder.bind(cartList[position], position)

    }

    fun setList(list: ArrayList<TbItem>) {
        LogUtil.logE(TAG, "itemListSize ${list.size}")
        cartList = list
        notifyDataSetChanged()

//        cartList.clear()
//        cartList = arrayListOf()
//        cartList.addAll(list)
//        notifyDataSetChanged()
    }

    fun clearList() {
        cartList.clear()
        cartList = arrayListOf()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return cartList.size
    }

    fun addCart(mList: List<TbItem>?) {
        cartList = mList as ArrayList<TbItem>
        val it: MutableIterator<TbItem> = cartList.iterator()

        while (it.hasNext()) {
            val s: TbItem = it.next()
            if (s.isDestroy) {
                it.remove()
            }
        }

        notifyDataSetChanged()
    }

    private fun totalPrice(model: TbItem): Double {
        return model.price * model.itemQuantity
    }


    private fun totalEachPrice(model: TbItem): Double {
        return model.price * 1
    }


}