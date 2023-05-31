package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.databinding.ViewPrinterQueueListRvBinding

class PrinterQueueList():RecyclerView.Adapter<PrinterQueueList.MyViewHolder>()  {
    inner class MyViewHolder(private var binding:ViewPrinterQueueListRvBinding):RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPrinterQueueListRvBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 0

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    }
}