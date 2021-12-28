package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.databinding.ViewPrinterQueueListBinding

class PrinterQueueListAdapter : RecyclerView.Adapter<PrinterQueueListAdapter.MyViewHolder>() {
    private var list: ArrayList<PrinterQueueModel> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setList(listQueue: ArrayList<PrinterQueueModel>) {
        this.list = listQueue
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ViewPrinterQueueListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterQueueModel) {
            binding.txtOfflineId.text = model.offlineId
            binding.txtOrderId.text = ""+model.orderId
            binding.txtOrderType.text = model.orderType
            binding.txtTerminalName.text = model.terminalName
            binding.txtTotalAmt.text = "$" + model.totalAmt
            binding.txtStatus.text = model.status

        }

    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PrinterQueueListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPrinterQueueListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrinterQueueListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size

    }


    fun getList(): List<PrinterQueueModel> {
        return list
    }

    fun clearList() {
        list.clear()
        list = arrayListOf()
        notifyDataSetChanged()
    }
    fun removeItemAt(position:Int){
        this.list.removeAt(position)
        notifyItemRemoved(position)

    }

}
