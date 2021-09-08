package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.remote.Constants.AVAILABLE
import com.android.pos.data.remote.Constants.WIFI
import com.android.pos.databinding.ViewPrinterItemBinding

class PrinterListAdapter : RecyclerView.Adapter<PrinterListAdapter.MyViewHolder>() {

    private var list: ArrayList<PrinterListModel> = arrayListOf()
    private lateinit var listner: PrinterListInterface

    inner class MyViewHolder(private val binding: ViewPrinterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterListModel?) {
            if (list[bindingAdapterPosition].connectionType == WIFI) {
                binding.imgConnectionType.setImageDrawable(
                    binding.root.context.resources.getDrawable(
                        R.drawable.ic_lan
                    )
                )

            } else {
                binding.imgConnectionType.setImageDrawable(
                    binding.root.context.resources.getDrawable(
                        R.drawable.ic_baseline_bluetooth_24
                    )
                )

            }

            if (model != null) {
                binding.swtOrderId.isChecked = model.isActive
            }

            binding.imgPrinter.setOnClickListener {
                if (list[bindingAdapterPosition].isActive) {
                    listner.onPrinterSelected(list[bindingAdapterPosition])
                }
            }

            binding.imgDelete.setOnClickListener {
                listner.onDeletePrinter(list[bindingAdapterPosition])
            }

            binding.swtOrderId.setOnCheckedChangeListener { buttonView, isChecked ->
                if (list[bindingAdapterPosition].type != AVAILABLE) {
                    listner.onUpdatePrinterStatus(list[bindingAdapterPosition], isChecked)

                } else if (isChecked && !(list.get(bindingAdapterPosition).isActive)) {
                    listner.onPrinterActive(list.get(bindingAdapterPosition))
                    list.removeAt(bindingAdapterPosition)
                    notifyDataSetChanged()
                }


            }

            binding.imgEdit.setOnClickListener {
                if (list[bindingAdapterPosition].isActive) {
                    listner.onEditSelected(list[bindingAdapterPosition])
                }

            }

            if (list[bindingAdapterPosition].type == AVAILABLE) {
                binding.linearOption.visibility = View.GONE
            } else {
                binding.linearOption.visibility = View.VISIBLE
            }

            binding.model = model
            binding.executePendingBindings()
        }

        init {


        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PrinterListAdapter.MyViewHolder {
        val binding =
            ViewPrinterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrinterListAdapter.MyViewHolder, position: Int) {
        holder.bind(list?.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: ArrayList<PrinterListModel>) {
        this.list = list
        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: PrinterListModel) {
        list.add(model)
        notifyDataSetChanged()

    }

    fun setListner(list: PrinterListInterface) {
        this.listner = list
    }

    interface PrinterListInterface {
        fun onPrinterSelected(printerListModel: PrinterListModel)
        fun onPrinterActive(printerListModel: PrinterListModel)
        fun onEditSelected(printerListModel: PrinterListModel)
        fun onDeletePrinter(printerListModel: PrinterListModel)
        fun onUpdatePrinterStatus(printerListModel: PrinterListModel, isChecked: Boolean)

    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        val size = list.size
        this.list.clear()
        notifyItemRangeRemoved(0, size)
        notifyDataSetChanged()
    }
}