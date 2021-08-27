package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.OptionSet
import com.android.pos.databinding.ViewOptionSetNameBinding
import kotlin.collections.ArrayList

class SelectedOptionSetNameAdapter :
    RecyclerView.Adapter<SelectedOptionSetNameAdapter.MyViewHolder>() {

    private lateinit var selectOptionListAdapter: SelectedItemOptionsListAdapter
    var optionSetList = ArrayList<OptionSet>()

    inner class MyViewHolder(val optiosetNameBinding: ViewOptionSetNameBinding) :
        RecyclerView.ViewHolder(optiosetNameBinding.root) {

        /* fun bind(item: OptionSet) {
             optiosetNameBinding.executePendingBindings()

         }*/
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ViewOptionSetNameBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.optiosetNameBinding

        itemBinding.tvOptionSetName.text = optionSetList[position].name
        selectOptionListAdapter = SelectedItemOptionsListAdapter(optionSetList[position].options)
        itemBinding.rvOptionList.adapter = selectOptionListAdapter

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return optionSetList.size
    }

    fun addOptions(optionSetList: OptionSet) {
        this.optionSetList.apply {
            add(optionSetList)
        }
        notifyItemInserted(this.optionSetList.size)
    }

}