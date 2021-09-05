package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Option
import com.android.pos.databinding.ViewSelectedItemOptionListBinding
import com.android.pos.utils.callback.DeleteOptionCallback
import kotlin.collections.ArrayList

class SelectedItemOptionsListAdapter(val options: ArrayList<Option>) :
    RecyclerView.Adapter<SelectedItemOptionsListAdapter.MyViewHolder>() {

    private lateinit var mSetCallback: DeleteOptionCallback
    fun setCallback(setCallback: DeleteOptionCallback) {
        mSetCallback = setCallback
    }

    inner class MyViewHolder(val optionSetBinding: ViewSelectedItemOptionListBinding) :
        RecyclerView.ViewHolder(optionSetBinding.root) {

        fun bind(item: Option) {
            optionSetBinding.model = item
            optionSetBinding.executePendingBindings()

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewSelectedItemOptionListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(options[position])
        val itemBinding = holder.optionSetBinding
        itemBinding.imgDeleteOptions.setOnClickListener {

            options.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, options.size)
            if (options.size == 0) {
                mSetCallback.onItemClickListener(position)
            }
        }


    }

    override fun getItemCount(): Int {
        return options.size
    }

}