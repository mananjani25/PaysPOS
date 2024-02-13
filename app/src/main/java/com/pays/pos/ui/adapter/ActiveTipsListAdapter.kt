package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.databinding.RowItemActiveTipsListBinding
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
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
            if (selectedPosition == position) {
                binding.rootLayout.setBackgroundColor(Color.parseColor("#ff6000"))
                binding.txtTipTitle.setTextColor(Color.parseColor("#FFFFFF"))
                binding.txtTipValue.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                binding.rootLayout.setBackgroundColor(Color.parseColor("#363636"))
                binding.txtTipTitle.setTextColor(Color.parseColor("#ff6000"))
                binding.txtTipValue.setTextColor(Color.parseColor("#FFFFFF"))
            }

            binding.apply {

                binding.txtTipValue.visible()
                binding.txtTipTitle.visible()

                txtTipTitle.text = "${String.format("%.0f", model.rate)}%"
                val tippedAmount = MethodUtils.percentageCalculation(
                    wholeTotalPrice,
                    model.rate
                )

                binding.txtTipValue.text = MethodUtils.roundOffAmount(tippedAmount)

            }

            val coroutineScope = CoroutineScope(Dispatchers.Main)
            coroutineScope.launch {
                //delay(3000)
                //Log.d("MERA", "SCOPE: CALLED")
                //binding.rootLayout.performClick()
            }

        }

        init {
            //val selectedItem = layoutPosition
            val selectedItem = 2
            binding.rootLayout.setOnSingleClickListener {
                selectedPosition = layoutPosition
                notifyDataSetChanged()
                listner.selectedItem(discountList[layoutPosition], layoutPosition, wholeTotalPrice)
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

    fun clearAll() {
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