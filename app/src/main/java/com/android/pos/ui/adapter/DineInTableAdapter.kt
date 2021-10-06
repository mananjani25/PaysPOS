package com.android.pos.ui.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.DineInModel
import com.android.pos.databinding.ViewDineInItemBinding
import com.android.pos.databinding.ViewDineInOrderTableBinding
import com.android.pos.utils.MethodUtils

class DineInTableAdapter : RecyclerView.Adapter<DineInTableAdapter.MyViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private lateinit var itemAdapter: CartAdapter
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DineInTableAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInOrderTableBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DineInTableAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInOrderTableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DineInModel) {

            itemAdapter = CartAdapter()
            binding.rvItems.adapter = itemAdapter
            itemAdapter.addCart(list[layoutPosition].items)

            if (list[layoutPosition].items.isNotEmpty()) {
                binding.txtTotal.visibility = View.VISIBLE
                val total = list.get(layoutPosition).items
                var sum = 0.0
                if (total.isNotEmpty()) {
                    total.forEach {
                        sum += it.price
                    }


                    binding.txtTotal.setText("Total : ${MethodUtils.roundOffAmount(sum)}")
                }


            } else {
                binding.txtTotal.visibility = View.GONE
            }

            if (list.get(layoutPosition).customer != null) {
                binding.txtTableName.setText(
                    list.get(layoutPosition).customer?.first_name + " " + list.get(
                        layoutPosition
                    ).customer?.last_name
                )
            } else {
                binding.txtTableName.setText(list.get(layoutPosition).title)

            }

            if (layoutPosition == 0) {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_simple_table))
                binding.imgProfile.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY)
            } else {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_group_person))

            }
        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        this.list = list
        notifyDataSetChanged()
    }
}