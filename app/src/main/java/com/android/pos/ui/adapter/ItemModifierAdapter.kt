package com.android.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewOrderModifiersBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.callback.ModifierLongClickCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible

class ItemModifierAdapter(
    private val maxAllowed: Int,
    private val minRequired: Int,
    private val mLongClickcallback: ModifierLongClickCallback?
) :
    RecyclerView.Adapter<ItemModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewOrderModifiersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()
            if (list[position].itemQuantity == 0) {
                binding.txtModifierQnt.gone()
            } else {
                binding.txtModifierQnt.visible()
            }
            if (list[position].isChecked && !list[position]._destroy) {
                if (list[position].itemQuantity == 0) {
                    binding.txtModifierQnt.gone()
                } else {
                    binding.txtModifierQnt.visible()
                }
                binding.llMain.setBackgroundResource(R.drawable.bg_squre_modifier_choose)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.white))
            } else {
                binding.txtModifierQnt.gone()
                binding.llMain.setBackgroundResource(R.drawable.bg_squre_modifier)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
            }
        }

        init {

            binding.llMain.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    mLongClickcallback?.onLongClickListener(
                        list[position].id,
                        bindingAdapterPosition,
                        list[position].modifier_quantity
                    )
                    return true
                }

            })
            binding.txtModifierQnt.setOnClickListener {
                mLongClickcallback?.onLongClickListener(
                    list[position].id,
                    bindingAdapterPosition,
                    list[position].modifier_quantity
                )
            }
            binding.llMain.setOnClickListener {

                if (list.isNotEmpty() && bindingAdapterPosition >= 0) {

                    list[bindingAdapterPosition].isChecked = !list[bindingAdapterPosition].isChecked
                    list[bindingAdapterPosition].itemQuantity = 1

                    if (maxLogic(
                            maxAllowed,
                            list
                        )
                    ) {
                        LogUtil.logE("minRequired", "ture")
                    } else {
                        LogUtil.logE("minRequired", "false")

                        AlertUtils.showCustomAlert(
                            binding.root.context,
                            binding.root.context.getString(R.string.you_can_not_add_more_then) + maxAllowed + binding.root.context.getString(
                                R.string.items
                            )
                        )


                        list[bindingAdapterPosition].isChecked =
                            !list[bindingAdapterPosition].isChecked
                    }


                    notifyDataSetChanged()
                }
            }
        }

    }

    private fun showModifierLayout() {

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