package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.ViewRefundItemBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel

class RefundItemListAdapter(val viewModel: TransactionDetailsViewModel) :
    RecyclerView.Adapter<RefundItemListAdapter.MyViewHolder>() {

    var selectedItemList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()
    var noteList = ArrayList<GetOrderDetailsResponse.Data.OrderItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RefundItemListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewRefundItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addItems(noteList: List<GetOrderDetailsResponse.Data.OrderItem>) {
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: RefundItemListAdapter.MyViewHolder, position: Int) {

        holder.bind(noteList.get(position))

        val itemBinding = holder.itemBinding
        val context = itemBinding.root.context

        itemBinding.tvItemName.text = noteList[position].itemName

        val modifierNames = noteList[position].orderItemModifiers.map {
            it.name + " (" + context.getString(R.string.symbole) + " " + String.format(
                context.getString(
                    R.string.format
                ), it.price
            ) + ")"
        }

        if (modifierNames.isEmpty()) {
            itemBinding.tvModifierName.visibility = View.GONE
        } else {
            itemBinding.tvModifierName.visibility = View.VISIBLE
            itemBinding.tvModifierName.text = TextUtils.join(",", modifierNames)
        }

        itemBinding.ivCheck.setOnClickListener {
            noteList[position].isChecked = !noteList[position].isChecked

            if (noteList[position].isChecked) {
                selectedItemList.add(noteList[position])
            } else {
                selectedItemList.remove(noteList[position])
            }
            notifyDataSetChanged()
        }


        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    inner class MyViewHolder(val itemBinding: ViewRefundItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: GetOrderDetailsResponse.Data.OrderItem) {
            itemBinding.refundItemListModel = item

            itemBinding.tvItemName.text = noteList[bindingAdapterPosition].itemName

            val modifierNames = noteList[bindingAdapterPosition].orderItemModifiers.map {
                it.name + " (" + itemBinding.root.context.getString(R.string.symbole) + " " + String.format(
                    itemBinding.root.context.getString(
                        R.string.format
                    ), it.price
                ) + ")"
            }

            if (modifierNames.isEmpty()) {
                itemBinding.tvModifierName.visibility = View.GONE
            } else {
                itemBinding.tvModifierName.visibility = View.VISIBLE
                itemBinding.tvModifierName.text = TextUtils.join(",", modifierNames)
            }

        }

        init {
            itemBinding.ivCheck.setOnClickListener {
                noteList[bindingAdapterPosition].isChecked =
                    !noteList[bindingAdapterPosition].isChecked

                if (noteList[bindingAdapterPosition].isChecked) {
                    selectedItemList.add(noteList[bindingAdapterPosition])
                } else {
                    selectedItemList.remove(noteList[bindingAdapterPosition])
                }
                notifyDataSetChanged()
            }

            itemBinding.executePendingBindings()
        }
    }

    fun selectedItemList(): ArrayList<GetOrderDetailsResponse.Data.OrderItem> {
        return selectedItemList
    }

}