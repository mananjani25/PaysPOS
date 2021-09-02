package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.databinding.ViewVariationItemBinding
import com.android.pos.ui.fragments.createitem.CreateItemViewModel

class VariationListAdapter(val viewModel: CreateItemViewModel) :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {

    var variationList = ArrayList<ArrayList<VariationsAttribute>>()

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
        itemBinding.variationModel = variationList[position]

        val variationName = variationList[position].map { it.name }
        itemBinding.tvVariationsName.text = TextUtils.join(",", variationName)



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
            clear()
            add(variation)
        }
        notifyDataSetChanged()
    }

}