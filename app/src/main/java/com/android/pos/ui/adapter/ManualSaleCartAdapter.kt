package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.ManualSaleCartModel
import com.android.pos.databinding.ViewManualSaleItemBinding
import com.android.pos.utils.CustomSwipeLayout.SwipeLayout
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ManualSaleOptionsCustomCallback
import com.android.pos.utils.swipereveallayout.ViewBinderHelper

class ManualSaleCartAdapter : RecyclerView.Adapter<ManualSaleCartAdapter.MyViewHolder>() {
    var list = ArrayList<TbItem>()
    private lateinit var listnerCall: ManualSaleInterface
    private lateinit var itemlistnerCall: ManualSaleOptionsCustomCallback
    val TAG = "ManualSaleCartAdapter"
    var  viewBinderHelper :ViewBinderHelper = ViewBinderHelper()

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(private val binding: ViewManualSaleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val txtItem: TextView = binding.root.findViewById(R.id.txtItem)
        fun bind(model: TbItem, pos: Int) {
           // viewBinderHelper.bind(binding.swipeLayout,absoluteAdapterPosition.toString())
            binding.txtQuantity.text = "x ${model.itemQuantity}"
            binding.txtItemPrice.text = "$"+model.price
            binding.txtTotalPrice.text = "$"+MethodUtils.roundOffAmountString((model.price * model.itemQuantity))



/*
            if (model.note.isNotEmpty()) {
                binding.txtNoteMannualcart.visibility = View.VISIBLE
                binding.txtNoteMannualcart.text = "Note: " + model.note
            } else {
                binding.txtNoteMannualcart.visibility = View.INVISIBLE

            }
*/
            txtItem.text = list[pos].name
/*
            if (list[pos].discountPrice != 0.0) {
                binding.txtItemPrice.paintFlags =
                    binding.txtItemPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                val dPrice = (list[pos].price * list[pos].itemQuantity) - list[pos].discountPrice

                MethodUtils.setPriceTextView(binding.txtDiscountPrice, dPrice)
            } else {
                binding.txtItemPrice.paintFlags = 0
                binding.txtDiscountPrice.text = ""
            }
*/

            binding.model = model
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
            binding.llRoot.setOnClickListener {
                listnerCall.onItemClicked(list[layoutPosition], layoutPosition)
            }

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
            ViewManualSaleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        Log.e("TbListSize", "TbListSize ${list.size}")
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