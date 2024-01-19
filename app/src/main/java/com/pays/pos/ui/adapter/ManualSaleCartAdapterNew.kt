package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.databinding.ViewItemCartBinding
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.callback.ManualSaleOptionsCustomCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.swipereveallayout.ViewBinderHelper

class ManualSaleCartAdapterNew : RecyclerView.Adapter<ManualSaleCartAdapterNew.MyViewHolder>() {
    var list = ArrayList<TbCartItem>()
    private lateinit var listnerCall: ManualSaleInterface
    private lateinit var itemlistnerCall: ManualSaleOptionsCustomCallback
    val TAG = "ManualSaleCartAdapter"
    var  viewBinderHelper : ViewBinderHelper =
        ViewBinderHelper()

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(private val binding: ViewItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: TbCartItem, pos: Int) {
           // viewBinderHelper.bind(binding.swipeLayout,absoluteAdapterPosition.toString())
            binding.txtQuantity.text = model.itemQuantity.toString()
            binding.txtEachQntPrice.text = "$"+MethodUtils.roundOffAmountString(model.price)
            binding.txtTotalPrice.text = "$"+MethodUtils.roundOffAmountString((model.price * model.itemQuantity))



            if (model.note.isEmpty()) {
                binding.txtNote.visibility = View.GONE
            } else {
                binding.txtNote.visibility = View.VISIBLE
                binding.txtNote.text = "Note: " + model.note
            }

            binding.txtName.text = model.name
            if (model.discountPrice != 0.0) {
                binding.tvDiscountRate.visible()
                binding.txtTotalPrice.paintFlags =
                    binding.txtTotalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                val dPrice = (model.price * model.itemQuantity) - (model.discountPrice * model.itemQuantity )

                MethodUtils.setPriceTextView(binding.tvDiscountRate, dPrice)
            } else {
                binding.txtTotalPrice.setPaintFlags(binding.txtTotalPrice.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
                binding.tvDiscountRate.text = ""
                binding.tvDiscountRate.gone()
            }
            itemView.setOnClickListener {
                listnerCall.onItemClicked(list[pos], pos)
            }
            //binding.model = model
            binding.executePendingBindings()

        }


        init {
            viewBinderHelper.setOpenOnlyOne(true)

        }
    }

    fun setCallBack(listner: ManualSaleInterface) {
        this.listnerCall = listner
    }

    fun setItemCallBack(listner: ManualSaleOptionsCustomCallback) {
        this.itemlistnerCall = listner
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ManualSaleCartAdapterNew.MyViewHolder {
        val binding =
            ViewItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ManualSaleCartAdapterNew.MyViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getList(): List<TbCartItem> {
        return this.list
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: TbCartItem) {
        this.list.add(model)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(model: TbCartItem, position: Int) {
        list[position] = model
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(cartList: List<TbCartItem>?) {
        this.list = cartList as ArrayList<TbCartItem>
        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbCartItem {
        return list[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        this.list.clear()
        notifyDataSetChanged()
    }

    interface ManualSaleInterface {
        fun onItemClicked(model: TbCartItem, position: Int)
    }
}