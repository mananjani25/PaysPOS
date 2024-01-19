package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.databinding.ViewCancelOrderReasonsBinding

class CancelOrderReasonAdapter (val listner: CustomerInteface):
    RecyclerView.Adapter<CancelOrderReasonAdapter.MyViewHolder>() {

    private var isSelectedPos: Int = 0


    private var filterList = ArrayList<VenueDetailsResponse.Data.CancelOrderReason>()

    inner class MyViewHolder(private val binding: ViewCancelOrderReasonsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(employee: VenueDetailsResponse.Data.CancelOrderReason) {
            binding.cancelOrderReasons = employee
            binding.executePendingBindings()
        }

        init {

            binding.root.setOnClickListener {
                isSelectedPos = layoutPosition
                notifyDataSetChanged()
                listner.onReasonSelect(layoutPosition, filterList[layoutPosition])
            }

        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewCancelOrderReasonsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(filterList[position])
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(filterList: List<VenueDetailsResponse.Data.CancelOrderReason>?) {
        this.filterList.clear()
        if (filterList?.isNotEmpty() == true) {
            this.filterList.addAll(filterList)
        }
        notifyDataSetChanged()
    }

    interface CustomerInteface {
        fun onReasonSelect(pos: Int, model: VenueDetailsResponse.Data.CancelOrderReason)
    }

}