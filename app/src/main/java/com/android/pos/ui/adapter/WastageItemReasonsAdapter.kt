package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.databinding.ViewWastageItemReasonBinding

class WastageItemReasonsAdapter (val listener: CustomerInterface):
    RecyclerView.Adapter<WastageItemReasonsAdapter.MyViewHolder>() {

    private var selectedPos: Int = 0
    private var wastageReasonsList = ArrayList<VenueDetailsResponse.Data.WastageReason>()

    inner class MyViewHolder(private val binding: ViewWastageItemReasonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(reasons: VenueDetailsResponse.Data.WastageReason) {
            binding.wastageItemsReasons = reasons

            if(layoutPosition == selectedPos){
                binding.llMain.background = binding.root.context.getDrawable(R.drawable.background_save_selected)
            } else {
                binding.llMain.background = binding.root.context.getDrawable(R.drawable.dine_in_order_footer_unselected)
            }
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener {
                selectedPos = layoutPosition
                notifyDataSetChanged()
                listener.onReasonSelect(layoutPosition, wastageReasonsList[layoutPosition])
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewWastageItemReasonBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(wastageReasonsList[position])
    }

    override fun getItemCount(): Int {
        return wastageReasonsList.size
    }

    fun add(wastageReasonsList: List<VenueDetailsResponse.Data.WastageReason>?) {
        this.wastageReasonsList.clear()
        if (wastageReasonsList?.isNotEmpty() == true) {
            this.wastageReasonsList.addAll(wastageReasonsList)
        }
        notifyDataSetChanged()
    }

    interface CustomerInterface {
        fun onReasonSelect(pos: Int, model: VenueDetailsResponse.Data.WastageReason)
    }
}