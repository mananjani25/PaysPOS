package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.RowItemActiveTipsListBinding
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.setOnSingleClickListener
import com.android.pos.utils.extensions.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActiveTipsListAdapter() :
    RecyclerView.Adapter<ActiveTipsListAdapter.MyViewHolder>() {
    var selectedPosition = -1
    var wholeTotalPrice = 0.0
    private lateinit var listner: DiscountInterface

    inner class MyViewHolder(private val binding: RowItemActiveTipsListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: GetTipReponse.Data, position: Int) {
//            if (selectedPosition == position) {
//                binding.rootLayout.background =
//                    binding.root.context.getDrawable(R.drawable.button_selected)
//                binding.txtTipTitle.setTextColor(binding.root.context.resources.getColor(R.color.white))
//                binding.txtTipValue.setTextColor(binding.root.context.resources.getColor(R.color.white))
//
//
//            } else {
//                binding.root.background =
//                    binding.root.context.getDrawable(R.drawable.background_square_border_grey)
//                binding.txtTipTitle.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
//                binding.txtTipValue.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
//            }

            binding.apply {
                if (model.name.equals("No Tip") || model.name.equals("Other")) {
                    binding.txtNoTipLabel.visible()
                    binding.txtNoTipLabel.text = model.name

                    binding.txtTipValue.gone()
                    binding.txtTipTitle.gone()
                } else {
                    binding.txtTipValue.visible()
                    binding.txtTipTitle.visible()

                    binding.txtNoTipLabel.gone()

                    txtTipTitle.text = "${String.format("%.0f", model.rate)}%"
                    val tippedAmount = MethodUtils.percentageCalculation(
                        wholeTotalPrice,
                        model.rate
                    )

                    binding.txtTipValue.text = MethodUtils.roundOffAmount(tippedAmount)
                }


            }

            val coroutineScope = CoroutineScope(Dispatchers.Main)
            coroutineScope.launch {
                delay(5000)
                binding.rootLayout.performClick()
            }

        }

        init {
            binding.rootLayout.setOnSingleClickListener {
                selectedPosition = 1
                notifyDataSetChanged()
                listner.selectedItem(discountList[1], 1,wholeTotalPrice)
            }
        }
    }

    var discountList = ArrayList<GetTipReponse.Data>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActiveTipsListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowItemActiveTipsListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ActiveTipsListAdapter.MyViewHolder, position: Int) {
        holder.bind(discountList[position], position)
    }

    override fun getItemCount(): Int {
        return discountList.size
    }

    fun setList(list: List<GetTipReponse.Data>?, totalPrice: Double) {
        this.wholeTotalPrice = totalPrice
        this.discountList.apply {
            clear()
            if (list != null) {
                addAll(list)
            }
        }
        notifyDataSetChanged()
    }

    fun clearAll(){
        this.discountList.clear()
    }

    fun getItem(position: Int): GetTipReponse.Data {
        return discountList[position]
    }

    interface DiscountInterface {
        fun selectedItem(model: GetTipReponse.Data, pos: Int, wholeTotalPrice: Double)
    }

    fun setListner(Mlistner: DiscountInterface) {
        listner = Mlistner

    }

    /*fun getSelectedItem():GetTipReponse.Data{
       return  if (selectedPosition == -1){

        }
        else{
            discountList[selectedPosition]
        }

    }*/

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelectedItem() {
        selectedPosition = -1
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelected(id: Int?) {
        for (i in 0 until discountList.size) {
            if (id == discountList[i].id) {
                selectedPosition = i
                notifyDataSetChanged()
            }
        }

    }

}