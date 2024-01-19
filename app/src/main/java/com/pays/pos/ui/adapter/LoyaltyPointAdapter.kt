package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.LoyaltyProgramsModel
import com.pays.pos.databinding.ViewLoyaltyPointItemBinding
import com.pays.pos.ui.fragments.settings.loyaltypoints.LoyaltyPointViewModel
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener

class LoyaltyPointAdapter(val viewModel: LoyaltyPointViewModel) :
    RecyclerView.Adapter<LoyaltyPointAdapter.MyViewHolder>() {

    var loyaltyPointList = ArrayList<LoyaltyProgramsModel>()

    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LoyaltyPointAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewLoyaltyPointItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LoyaltyPointAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        val context = itemBinding.root.context
        itemBinding.serviceChargeModel = loyaltyPointList[position]
        itemBinding.viewModel = viewModel

        val model = loyaltyPointList[position]
        if (model.rewardType == "%") {
           // itemBinding.txtLoyaltyValue.text = model.amount.toString() + "%"
            itemBinding.txtLoyaltyValue.text =
                "" + String.format(context.getString(R.string.format) ,model.amount) + ""+context.getString(
                    R.string.percentage_symbol
                )

        } else {


            //itemBinding.txtLoyaltyValue.text = "$" + model.amount.toString()

            itemBinding.txtLoyaltyValue.text =
                context.getString(R.string.symbole) +  ""+String.format(context.getString(R.string.format) , model.amount)

        }

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = loyaltyPointList.size


    fun addServiceCharge(discountList: List<LoyaltyProgramsModel>) {

        this.loyaltyPointList.apply {
            clear()
            addAll(discountList)
        }
    }

    fun getItem(position: Int): LoyaltyProgramsModel {
        return loyaltyPointList[position]
    }

    fun update(id: Int) {

        loyaltyPointList.forEach {
            it.isEnable = it.id == id
        }
        notifyDataSetChanged()

    }

    inner class MyViewHolder(val discountItemBinding: ViewLoyaltyPointItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {
        init {
            discountItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it, bindingAdapterPosition)
            }

        }

    }

}