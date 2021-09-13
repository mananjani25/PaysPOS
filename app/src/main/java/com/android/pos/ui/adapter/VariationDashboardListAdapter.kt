package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.databinding.ViewTaxItemBinding
import com.android.pos.databinding.ViewVariationDashboardListBinding
import com.android.pos.ui.fragments.settings.tax.TaxListViewModel


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
            binding.variationModel = item
            binding.executePendingBindings()


            if (mpos == bindingAdapterPosition) {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier_choose)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.white))

            } else {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier)
                binding.edtName.setTextColor(binding.root.context.resources.getColor(R.color.viewTextColor))
                binding.edtPrice.setTextColor(binding.root.context.resources.getColor(R.color.viewTextColor))
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
}