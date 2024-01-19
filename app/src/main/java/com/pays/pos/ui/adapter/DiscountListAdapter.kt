package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.databinding.ViewDiscountItemBinding
import com.pays.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener

class DiscountListAdapter(val viewModel: DiscountListViewModel) :
    RecyclerView.Adapter<DiscountListAdapter.MyViewHolder>() {

    var discountList = ArrayList<TbDiscount>()

    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
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

        init {
            discountItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, position)
            }
        }

        /*init {

            discountItemBinding.imgCheckBox.setOnClickListener {
                discountList[layoutPosition].isChecked = !discountList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }


}