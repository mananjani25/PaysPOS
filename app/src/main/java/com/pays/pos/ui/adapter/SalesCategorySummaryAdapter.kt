package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.databinding.ViewEmployeeGuestDetailsBinding
import com.pays.pos.databinding.ViewPaymentDetailsBinding
import com.pays.pos.databinding.ViewSalesReportBinding
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class SalesCategorySummaryAdapter :
    RecyclerView.Adapter<SalesCategorySummaryAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValue>()

    inner class MyViewHolder(private val binding: ViewEmployeeGuestDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(obj: KeyValue) {

            binding.txtPaymentId.text = obj.key
            binding.txtLast4.text = obj.showData()
            binding.executePendingBindings()


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewEmployeeGuestDetailsBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {

        return arrayList.size
    }

    fun add(arrayListNew: ArrayList<ArrayList<KeyValue>>?) {
        this.arrayList.clear()


        if (arrayListNew?.isNotEmpty() == true) {

            for (i in 0 until arrayListNew.size){

                for (j in 0 until arrayListNew.get(i).size){
                    this.arrayList.add(arrayListNew.get(i).get(j))
                }
            }


        }
        notifyDataSetChanged()
    }
}