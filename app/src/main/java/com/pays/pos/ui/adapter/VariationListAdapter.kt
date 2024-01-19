package com.pays.pos.ui.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.databinding.ViewVariationItemBinding
import com.pays.pos.ui.fragments.createitem.CreateItemViewModel
import com.pays.pos.utils.callback.UpdateVariationCallback

class VariationListAdapter(val viewModel: CreateItemViewModel) :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {

    var variationList = ArrayList<VariationsAttribute>()

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
        val context = itemBinding.root.context

        val variationName = variationList[position].name
        itemBinding.tvVariationsName.text = variationName

        if (variationList[position].price != null) {
            itemBinding.tvMarkupPrice?.text =
                context.getString(R.string.symbole) + " " + String.format(
                    context.getString(R.string.format),
                    variationList[position].price
                )
        } else {
            itemBinding.tvMarkupPrice?.text = "Variable"
        }

        if (variationList[position].price != null) {
            itemBinding.tvPrice.text =
                context.getString(R.string.symbole) + " " + String.format(
                    context.getString(R.string.format),
                    variationList[position].price
                )
        } else {
            itemBinding.tvPrice.text = "Variable"
        }

        itemBinding.tvSku.text = variationList[position].sku
        itemBinding.tvStock.text = variationList[position].stockQty

        itemBinding.root.setOnClickListener {
            mCallback.onItemClickListener(position, variationList[position])
        }

        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return variationList.size

    }

    inner class MyViewHolder(val noteItemBinding: ViewVariationItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root)

    fun addAllVariations(variationList: ArrayList<VariationsAttribute>) {
        this.variationList.apply {
            clear()
            addAll(variationList)
        }
        notifyDataSetChanged()
    }

    fun updateVariation(position: Int, variation: VariationsAttribute) {

        variationList.set(position, variation)

        notifyDataSetChanged()

    }

    fun deleteVariation(position: Int, variation: VariationsAttribute): Int {

        variationList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, variationList.size)

        if (variationList.size == 0) {
            return 0
        }

        return variationList.size

    }


    fun selectedVariation() = variationList

    /*fun selectedVariation() {
        var int = 0
        variationList.forEach {
            if(it.isChange){
                int ++
            }
        }
        if (int == 0){
            // no changes
        }else {

        }
    }*/

}