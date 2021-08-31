package com.android.pos.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Option
import com.android.pos.databinding.ViewVariationItemBinding
import com.android.pos.ui.fragments.createitem.CreateItemViewModel

class VariationListAdapter(val viewModel: CreateItemViewModel) :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {

    var variationList = ArrayList<List<Option>>()

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
//        itemBinding.noteModel = noteList[position]
        // itemBinding.viewModel = viewModel
        // itemBinding.tvVariationsName.text = variationList[position].

        val variationName = variationList[position].map { it.name }
        itemBinding.tvVariationsName.text = TextUtils.join(",", variationName)

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return variationList.size

    }

    inner class MyViewHolder(val noteItemBinding: ViewVariationItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root)

    fun addVariations(variationList: ArrayList<List<Option>>) {
        this.variationList.apply {
            clear()
            addAll(variationList)
        }
        notifyDataSetChanged()
    }

}