package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.databinding.ViewVariationItemBinding
import com.android.pos.ui.fragments.createitem.CreateItemViewModel
import com.android.pos.utils.callback.UpdateVariationCallback

class VariationListAdapter(val viewModel: CreateItemViewModel) :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {

    var variationList = ArrayList<ArrayList<VariationsAttribute>>()

    private lateinit var mCallback: UpdateVariationCallback
    fun setCallback(callback: UpdateVariationCallback) {
        mCallback = callback
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VariationListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewVariationItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }


    override fun onBindViewHolder(holder: VariationListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.noteItemBinding

        val variationName = variationList[position].map { it.name }
        itemBinding.tvVariationsName.text = TextUtils.join(",", variationName)
        itemBinding.tvPrice.text = variationList[position][0].price.toString()
        itemBinding.tvSku.text = variationList[position][0].sku
        itemBinding.tvStock.text = variationList[position][0].stockQty

        itemBinding.root.setOnClickListener {
            mCallback.onItemClickListener(variationList[position])
        }

        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return variationList.size

    }

    inner class MyViewHolder(val noteItemBinding: ViewVariationItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root)

    fun addAllVariations(variationList: ArrayList<ArrayList<VariationsAttribute>>) {
        this.variationList.apply {
            clear()
            addAll(variationList)
        }
        notifyDataSetChanged()
    }

    fun addVariation(variation: ArrayList<VariationsAttribute>) {
        this.variationList.apply {
            // clear()
            add(variation)
        }
        notifyDataSetChanged()
    }

}