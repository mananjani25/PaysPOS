package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.databinding.ViewVariationDashboardListBinding


class VariationDashboardListAdapter :
    RecyclerView.Adapter<VariationDashboardListAdapter.MyViewHolder>() {

    var showVariationPriceClick: ((VariationsAttribute) -> Unit)? = null
    private var mpos: Int = 0
    var variationList = ArrayList<VariationsAttribute>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VariationDashboardListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewVariationDashboardListBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: VariationDashboardListAdapter.MyViewHolder,
        position: Int
    ) {
        holder.bind(variationList[position])
    }

    override fun getItemCount() = variationList.size

    fun addVariations(variationList: List<VariationsAttribute>) {

        this.variationList.apply {
            clear()
            addAll(variationList)
        }
        notifyDataSetChanged()
    }


    fun getItem(): VariationsAttribute {
        return variationList[mpos]
    }

    inner class MyViewHolder(val binding: ViewVariationDashboardListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VariationsAttribute) {

            val context = binding.root.context
            binding.variationModel = item
            binding.executePendingBindings()

            if (item.priceType == "Fixed") {
                binding.edtPrice.text =
                    context.getString(R.string.symbole) + " " + String.format(
                        context.getString(R.string.format),
                        variationList[bindingAdapterPosition].price
                    )
            } else if (item.priceType == "Variable") {
                binding.edtPrice.text = "Variable"
            }

            if (mpos == bindingAdapterPosition) {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier_choose)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.white))

            } else {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
            }
        }

        init {

            binding.llItemName.setOnClickListener {
                mpos = layoutPosition
                showVariationPriceClick?.invoke(variationList[bindingAdapterPosition])
                notifyDataSetChanged()

            }

        }

    }

    fun updateVariation(variation: VariationsAttribute) {
        var position = -1
        variationList.forEachIndexed { index, variationsAttribute ->
            if (variationsAttribute.id == variation.id) {
                position = index
                return@forEachIndexed
            }
        }

        if (position != -1) {
            variationList.set(position, variation)
        }

        notifyDataSetChanged()
    }

    fun selectItem(id: Int) {
        if (variationList.isNotEmpty()) {
            for (i in 0 until variationList.size) {
                val variation = variationList[i]
                if (variation.id == id) {
                    mpos = i
                    notifyItemChanged(mpos)
                    break
                }
            }
        }
    }
}