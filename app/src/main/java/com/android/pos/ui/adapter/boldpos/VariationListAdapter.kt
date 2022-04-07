package com.android.pos.ui.adapter.boldpos

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.databinding.ViewBoldVariationsBinding

import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.UpdateVariationCallback

class VariationListAdapter() :
    RecyclerView.Adapter<VariationListAdapter.MyViewHolder>() {
    private val TAG = "VariationListAdapter"


    var showVariationPriceClick: ((VariationsAttribute) -> Unit)? = null
    private var mpos: Int = 0
    var variationList = ArrayList<VariationsAttribute>()

    private lateinit var mCallback: UpdateVariationCallback
    fun setCallback(callback: UpdateVariationCallback) {
        mCallback = callback
    }

    private var mCallbackvariation: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallbackvariation = callback
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
        itemBinding.txtVariation.text = variationList[position].name
        itemBinding.txtPrice.text = variationList[position].price?.let {
            MethodUtils.roundOffAmount(
                it
            )
        }
        Log.e(TAG, "mpos:  ${mpos}")

        itemBinding.linearParent.isSelected = mpos == position


    }

    override fun getItemCount(): Int {
        return variationList.size
    }


    inner class MyViewHolder(val noteItemBinding: ViewBoldVariationsBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root) {

        init {

            noteItemBinding.linearParent.setOnClickListener {

                mpos = absoluteAdapterPosition

                mCallbackvariation?.onItemClickListener(it, mpos)
                showVariationPriceClick?.invoke(variationList[bindingAdapterPosition])
                Log.d("yash", "position: " + absoluteAdapterPosition)
                notifyDataSetChanged()


            }
        }
    }

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

    fun getItem(): VariationsAttribute {
        return variationList[mpos]
    }

    fun selectItem(id: Int) {
        if (variationList.isNotEmpty()) {
            for (i in 0 until variationList.size) {
                val variation = variationList[i]
                if (variation.id == id) {
                    Log.e(TAG,"positionChafnf ${i}")
                    mpos = i
                    notifyItemChanged(mpos)


                    break
                }
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

}