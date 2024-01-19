package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TaxData
import com.pays.pos.databinding.ViewTaxItemBinding
import com.pays.pos.ui.fragments.settings.tax.TaxListViewModel
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener


class TaxListAdapter(val viewModel: TaxListViewModel) :
    RecyclerView.Adapter<TaxListAdapter.MyViewHolder>() {

    var taxList = ArrayList<TaxData>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewTaxItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaxListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.taxItemBinding
        itemBinding.taxModel = taxList[position]
        val context = itemBinding.root.context
        if (taxList[position].taxType == context.getString(R.string.disc_percentage)) {
            itemBinding.tvRate.text =
                "" + String.format(context.getString(R.string.format) ,taxList[position].rate) + " "+context.getString(
                    R.string.percentage_symbol
                )
        } else {
            itemBinding.tvRate.text =
                context.getString(R.string.symbole) +  " "+String.format(context.getString(R.string.format) , taxList[position].rate)
        }
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = taxList.size

    fun addTaxes(taxList: List<TaxData>) {

        this.taxList.apply {
            clear()
            addAll(taxList)
        }
    }

    fun getItem(position: Int): TaxData {
        return taxList[position]
    }

    inner class MyViewHolder(val taxItemBinding: ViewTaxItemBinding) :
        RecyclerView.ViewHolder(taxItemBinding.root) {
            init {
                taxItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }
            }

        /*init {

            taxItemBinding.imgCheckBox.setOnClickListener {
                taxList.get(layoutPosition).isActive = !taxList.get(layoutPosition).isActive

                notifyDataSetChanged()

            }

        }*/
    }
}