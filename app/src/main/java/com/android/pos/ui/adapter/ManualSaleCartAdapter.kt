package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ManualSaleOptionsCustomCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.swipereveallayout.ViewBinderHelper

class ManualSaleCartAdapter : RecyclerView.Adapter<ManualSaleCartAdapter.MyViewHolder>() {
    var list = ArrayList<TbItem>()
    private lateinit var listnerCall: ManualSaleInterface
    private lateinit var itemlistnerCall: ManualSaleOptionsCustomCallback
    val TAG = "ManualSaleCartAdapter"
    var  viewBinderHelper :ViewBinderHelper = ViewBinderHelper()

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(private val binding: ViewItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: TbItem, pos: Int) {
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
/*
            binding.rlRoot!!.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        listnerCall.onItemClicked(list[layoutPosition], layoutPosition)

                    }
                    MotionEvent.ACTION_UP -> {
                        listnerCall.onItemClicked(list[layoutPosition], layoutPosition)
                    }
                }

                return@OnTouchListener true
            })
*/

//            binding.swipeLayout.setOnDragListener { _, _ ->
//                if (binding.swipeLayout.isOpened)
//                    binding.swipeLayout.close(true)
//
//                false
//            }


          /*  binding.txtDiscount.setOnClickListener {
                itemlistnerCall.onItemClickListener(binding.txtDiscount,list[layoutPosition],layoutPosition)
            }
            binding.txtNote.setOnClickListener {
                itemlistnerCall.onItemClickListener(binding.txtNote,list[layoutPosition],layoutPosition)
            }
            binding.txtRename.setOnClickListener {
                itemlistnerCall.onItemClickListener(binding.txtRename,list[layoutPosition],layoutPosition)
            }
            binding.txtDelete.setOnClickListener {
                itemlistnerCall.onItemClickListener(binding.txtDelete,list[layoutPosition],layoutPosition)
            }*/

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
    ): ManualSaleCartAdapter.MyViewHolder {
        val binding =
            ViewItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ManualSaleCartAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getList(): List<TbItem> {
        return this.list
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: TbItem) {
        this.list.add(model)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(model: TbItem, position: Int) {
        list[position] = model
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(cartList: List<TbItem>?) {
        this.list = cartList as ArrayList<TbItem>
        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbItem {
        return list[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        this.list.clear()
        notifyDataSetChanged()
    }

    interface ManualSaleInterface {
        fun onItemClicked(model: TbItem, position: Int)
    }
}