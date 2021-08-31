package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.EditPrinterModel
import com.android.pos.databinding.ViewEditPrinterListBinding

class EditPrinterListAdapter : RecyclerView.Adapter<EditPrinterListAdapter.MyViewHolder>() {
    private var list: ArrayList<EditPrinterModel> = arrayListOf()

    inner class MyViewHolder(private val binding: ViewEditPrinterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: EditPrinterModel) {
            binding.model = model
            binding.executePendingBindings()
        }


        init {

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EditPrinterListAdapter.MyViewHolder {
        val binding =
            ViewEditPrinterListBinding.inflate(LayoutInflater.from(parent.context), null, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EditPrinterListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(dataList: ArrayList<EditPrinterModel>) {
        this.list = dataList
        notifyDataSetChanged()
    }


}