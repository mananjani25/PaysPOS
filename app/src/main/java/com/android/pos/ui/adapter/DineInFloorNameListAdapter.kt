package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.DineInFloorNameModel
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.ViewDineInFloorNameBinding
import com.android.pos.ui.fragments.dinein.DineInViewModel
import com.android.pos.utils.callback.ItemCallback

class DineInFloorNameListAdapter(val viewModel: DineInViewModel) :
    RecyclerView.Adapter<DineInFloorNameListAdapter.MyViewHolder>() {

    var showFloorPlan: ((DineInFloorNameModel) -> Unit)? = null
    var floorNameList = ArrayList<DineInFloorNameModel>()
    private var mpos: Int = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DineInFloorNameListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInFloorNameBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addFloorName(noteList: ArrayList<DineInFloorNameModel>) {
        this.floorNameList.apply {
            clear()
            addAll(noteList)
        }
    }

    fun getItem(position: Int): DineInFloorNameModel {
        return floorNameList[position]
    }

    override fun onBindViewHolder(holder: DineInFloorNameListAdapter.MyViewHolder, position: Int) {
        holder.bind(floorNameList[position])

    }

    override fun getItemCount(): Int {
        return floorNameList.size

    }

    inner class MyViewHolder(val binding: ViewDineInFloorNameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DineInFloorNameModel) {
            binding.dineInModel = item
            binding.executePendingBindings()


            if (mpos == bindingAdapterPosition) {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier_choose)
                binding.tvFloorName.setTextColor(binding.root.context.resources.getColor(R.color.white))

            } else {
                binding.llItemName.setBackgroundResource(R.drawable.bg_squre_modifier)
                binding.tvFloorName.setTextColor(binding.root.context.resources.getColor(R.color.viewTextColor))
            }
        }

        init {

            binding.llItemName.setOnClickListener {
                mpos = layoutPosition
                showFloorPlan?.invoke(floorNameList[bindingAdapterPosition])
                notifyDataSetChanged()

            }

        }
    }

}