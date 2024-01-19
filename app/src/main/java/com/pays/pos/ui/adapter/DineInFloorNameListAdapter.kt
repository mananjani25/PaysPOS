package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.databinding.ViewDineInFloorNameBinding
import com.pays.pos.ui.fragments.dinein.DineInViewModel

class DineInFloorNameListAdapter(val viewModel: DineInViewModel) :
    RecyclerView.Adapter<DineInFloorNameListAdapter.MyViewHolder>() {

    var showFloorPlan: ((GetFloorPlanResponse.Data) -> Unit)? = null
    var floorNameList = ArrayList<GetFloorPlanResponse.Data>()
    private var mpos: Int = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DineInFloorNameListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInFloorNameBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addFloorName(noteList: List<GetFloorPlanResponse.Data>) {
        this.floorNameList.apply {
            clear()
            addAll(noteList)
        }
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: DineInFloorNameListAdapter.MyViewHolder, position: Int) {
        holder.bind(floorNameList[position])

    }

    override fun getItemCount(): Int {
        return floorNameList.size

    }

    inner class MyViewHolder(val binding: ViewDineInFloorNameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GetFloorPlanResponse.Data) {
            binding.dineInModel = item
            binding.executePendingBindings()


            if (mpos == bindingAdapterPosition) {
                binding.llItemName.setBackgroundColor(binding.root.context.resources.getColor(R.color.btnColor))
                binding.tvFloorName.setTextColor(binding.root.context.resources.getColor(R.color.white))

            } else {
                binding.llItemName.setBackgroundColor(binding.root.context.resources.getColor(R.color.bg_color))
                binding.tvFloorName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
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
    fun getSelectedPos():Int{
        return mpos
    }

}