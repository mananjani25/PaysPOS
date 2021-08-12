package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbDiscount
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.databinding.ViewDiscountItemBinding
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel

class DiscountListAdapter(val viewModel: DiscountListViewModel) :
    RecyclerView.Adapter<DiscountListAdapter.MyViewHolder>() {

    var discountList = ArrayList<TbDiscount>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiscountListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDiscountItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DiscountListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.discountModel = discountList[position]
        val context = itemBinding.root.context
        if (discountList[position].discountType == context.getString(R.string.disc_percentage)) {
            itemBinding.tvRate.text =
                "" + String.format(
                    context.getString(R.string.format),
                    discountList[position].percentage
                ) + " " + context.getString(R.string.percentage_symbol)
        } else {
            itemBinding.tvRate.text = context.getString(R.string.symbole) + " " + String.format(
                context.getString(R.string.format),
                discountList[position].percentage
            )
        }

        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = discountList.size


    fun addDiscount(discountList: List<TbDiscount>) {

        this.discountList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): TbDiscount {
        return discountList[position]
    }

    inner class MyViewHolder(val discountItemBinding: ViewDiscountItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {

            discountItemBinding.imgCheckBox.setOnClickListener {
                discountList[layoutPosition].isChecked = !discountList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }


}