package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.databinding.ViewPrinterQueueListBinding

class PrinterQueueListAdapter : RecyclerView.Adapter<PrinterQueueListAdapter.MyViewHolder>() {
    private var list: ArrayList<PrinterQueueModel> = arrayListOf()


    fun setList(listQueue: ArrayList<PrinterQueueModel>) {
        list.clear()
        list.addAll(listQueue)
        notifyDataSetChanged()
        //notifyItemRangeInserted(0,list.size)

        //notifyItemRangeChanged(0, list.size)
        //notifyDataSetChanged()

    }

    inner class MyViewHolder(private val binding: ViewPrinterQueueListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterQueueModel) {
            binding.txtOfflineId.text = model.offlineId
            binding.txtOrderId.text = ""
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


    fun updatePrintStatus(position: Int, status: String) {
        if (list.isNotEmpty()) {
            list.get(position).status = status
            notifyItemChanged(position)
        }
    }

    fun getList(): List<PrinterQueueModel> {
        return list
    }

    fun clearList() {
        var tmpList = arrayListOf<PrinterQueueModel>()
        tmpList.addAll(list)
        list.clear()
        list = arrayListOf()
        notifyItemRangeRemoved(0, tmpList.size)
    }

    fun removeItemAt(position: Int) {
        if (list.isNotEmpty()) {
            this.list.removeAt(0)
            notifyItemRemoved(0)
        }

    }

}
