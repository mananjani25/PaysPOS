package com.pays.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.databinding.ViewPrinterQueueListRvBinding

class PrinterQueueList() : RecyclerView.Adapter<PrinterQueueList.MyViewHolder>() {

    private var list: ArrayList<com.pays.pos.data.model.PrinterQueueList> = arrayListOf()

    inner class MyViewHolder(private var binding: ViewPrinterQueueListRvBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: com.pays.pos.data.model.PrinterQueueList) {

            binding.model = data

            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPrinterQueueListRvBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    fun addData(listD: java.util.ArrayList<com.pays.pos.data.model.PrinterQueueList>) {
        list.clear()
        list = arrayListOf()
        list.addAll(listD)
        notifyDataSetChanged()


    }

    fun clearData() {
        list.clear()
        list = arrayListOf()
        notifyDataSetChanged()
    }
}