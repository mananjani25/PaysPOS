package com.pays.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Option
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.databinding.ViewOptionSetNameBinding
import com.pays.pos.ui.fragments.inventory.OptionSetViewModel
import com.pays.pos.utils.callback.DeleteOptionCallback
import com.pays.pos.utils.callback.DeleteOptionSetCallback
import kotlin.collections.ArrayList

class SelectedOptionSetNameAdapter(val viewModel: OptionSetViewModel) :
    RecyclerView.Adapter<SelectedOptionSetNameAdapter.MyViewHolder>(), DeleteOptionCallback {

    private lateinit var selectOptionListAdapter: SelectedItemOptionsListAdapter
    var optionSetList = ArrayList<OptionSet>()
    var positionParent: Int = -1

    private lateinit var mSetCallback: DeleteOptionSetCallback
    fun setCallback(setCallback: DeleteOptionSetCallback) {
        mSetCallback = setCallback
    }

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
        itemBinding.optionSetListModel = optionSetList[position]
        itemBinding.position = position
        positionParent = position
        itemBinding.viewModel = viewModel

        //  itemBinding.tvOptionSetName.text = optionSetList[position].name + " Options"
        //  itemBinding.tvDisplayName.text = optionSetList[position].name

        if (optionSetList[position].name != "Select Option Set") {
            selectOptionListAdapter =
                SelectedItemOptionsListAdapter(optionSetList[position].options as ArrayList<Option>)

            selectOptionListAdapter.setCallback(this)
            itemBinding.rvOptionList.adapter = selectOptionListAdapter
        }

        itemBinding.ivDelete.setOnClickListener {
            mSetCallback.onItemClickListener(position, optionSetList[position])
            optionSetList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, optionSetList.size)
        }

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

    fun addAllOptions(optionSetList: ArrayList<OptionSet>) {

        this.optionSetList.apply {
            clear()
            addAll(optionSetList)
        }
        notifyDataSetChanged()
    }

    override fun onItemClickListener(position: Int) {

        for (i in optionSetList.indices) {
            if (optionSetList[i].options.isEmpty()) {
                mSetCallback.onItemClickListener(i, optionSetList[i])
                Log.d("positionParent", "::" + i)
                optionSetList.removeAt(i)
                notifyItemRemoved(i)
                notifyItemRangeChanged(i, optionSetList.size)
                break
            }
        }


        notifyDataSetChanged()

    }

}