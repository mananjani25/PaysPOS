package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.databinding.ViewBoldVariationsBinding
import com.android.pos.ui.fragments.createitem.CreateItemViewModel
import com.android.pos.utils.callback.UpdateVariationCallback

class VariationListAdapter(val viewModel: CreateItemViewModel) :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {

    var showVariationPriceClick: ((VariationsAttribute) -> Unit)? = null
    private var mpos: Int = 0
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
        val binding = ViewBoldVariationsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: VariationListAdapter.MyViewHolder, position: Int) {

        val itemBinding = holder.noteItemBinding
        itemBinding.txtVariation.setText("" + variationList[position])

    }

    override fun getItemCount(): Int {
        return variationList.size
    }


    inner class MyViewHolder(val noteItemBinding: ViewBoldVariationsBinding) :
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

    fun addVariations(variationList: List<VariationsAttribute>) {

        this.variationList.apply {
            clear()
            addAll(variationList)
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