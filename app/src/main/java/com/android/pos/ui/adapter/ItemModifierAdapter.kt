package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewOrderModifiersBinding

class ItemModifierAdapter(private val maxAllowed: Int, private val minRequired: Int) :
    RecyclerView.Adapter<ItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
        }

        init {

            binding.root.setOnClickListener {

                list[bindingAdapterPosition].isChecked = !list[bindingAdapterPosition].isChecked

                if ((minRequired == 0) || maxLogic(
                        maxAllowed,
                        list
                    )
                ) {

                } else {

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