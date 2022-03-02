package com.android.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewOrderModifiersBinding
import com.android.pos.utils.AlertUtils

class ItemModifierAdapter(private val maxAllowed: Int, private val minRequired: Int) :
    RecyclerView.Adapter<ItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
            if (list[position].isChecked) {
                binding.llMain.setBackgroundResource(R.drawable.bg_squre_modifier_choose)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.white))
            } else {
                binding.llMain.setBackgroundResource(R.drawable.bg_squre_modifier)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
            }
        }

        init {

            binding.llMain.setOnClickListener {

                list[bindingAdapterPosition].isChecked = !list[bindingAdapterPosition].isChecked

                if ((minRequired == 0) || maxLogic(
                        maxAllowed,
                        list
                    )
                ) {
                    Log.e("minRequired", "ture")
                } else {
                    Log.e("minRequired", "false")

                    AlertUtils.showCustomAlert(
                        binding.root.context,
                        binding.root.context.getString(R.string.you_can_not_add_more_then) + maxAllowed + binding.root.context.getString(
                            R.string.items
                        )
                    )


                    list[bindingAdapterPosition].isChecked = !list[bindingAdapterPosition].isChecked
                }


                notifyDataSetChanged()
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemModifierAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOrderModifiersBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ItemModifierAdapter.MyViewHolder, position: Int) {

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

    private fun maxLogic(
        maxCount: Int,
        modifiers: List<Modifier>
    ): Boolean {

        if (maxCount == 0) {
            return true
        }
        var totalMinMax = 0

        modifiers.forEach {
            if (it.isChecked) {
                totalMinMax += 1
            }
        }

        return maxCount >= totalMinMax
    }
}