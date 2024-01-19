package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.remote.Constants.AVAILABLE
import com.android.pos.data.remote.Constants.WIFI
import com.android.pos.databinding.ViewPrinterItemBinding
import com.android.pos.ui.fragments.settings.hardware.printer.Printer.Companion.viewModelObject
import com.android.pos.utils.TAG


class PrinterListAdapter : RecyclerView.Adapter<PrinterListAdapter.MyViewHolder>() {

    private var list: ArrayList<PrinterListModel> = arrayListOf()
    private lateinit var listner: PrinterListInterface

    inner class MyViewHolder(private val binding: ViewPrinterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterListModel?) {

            if (model?.printerName !=""){
                binding.txtPrinterName.text =  model?.printerName
            }else {
                model.printerName = "InnerPrinter"
            }


            if (list[layoutPosition].connectionType == WIFI) {
                binding.imgConnectionType.setImageDrawable(
                    binding.root.context.getDrawable(
                        R.drawable.ic_lan
                    )
                )

            } else {

                binding.imgConnectionType.setImageDrawable(
                    binding.root.context.getDrawable(
                        R.drawable.ic_baseline_bluetooth_24
                    )
                )

            }

            if (model != null) {
                binding.swtOrderId.isChecked = model.isActive
            }

            binding.imgPrinter.setOnClickListener {
                if (list.isNotEmpty()) {
                    if (list[layoutPosition].isActive) {
                        listner.onPrinterSelected(list[layoutPosition])
                    }
                }
            }

            binding.imgDelete.setOnClickListener {
                if (list.isNotEmpty()) {
                    listner.onDeletePrinter(list[layoutPosition])
                }
            }

            binding.swtOrderId.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    if (list[layoutPosition].type != AVAILABLE) {
                        if(isChecked){
                            buttonView.isChecked = false
                            listner.onUpdatePrinterStatus(list[layoutPosition], isChecked)
                        }else{
                            buttonView.isChecked = true
                            listner.onUpdatePrinterStatus(list[layoutPosition], isChecked)
                        }
                    } else if (isChecked && !(list.get(layoutPosition).isActive)) {
                        buttonView.isChecked = false
                        listner.onPrinterActive(list.get(layoutPosition), layoutPosition)
                        /*list.removeAt(layoutPosition)
                    notifyDataSetChanged()*/
                    }

                }

            }

            binding.imgEdit.setOnClickListener {
               try {
                   if (list[layoutPosition].type != AVAILABLE) {
                       listner.onEditSelected(list[layoutPosition])
                   }
               }catch (e:Exception){
                   Log.d("layoutPosition","Error : ${e.message}")
               }

            }

            if (list[layoutPosition].type == AVAILABLE) {
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
        Log.e("REC_CRASH",position.toString())
        holder.bind(list?.get(position))
    }

    override fun getItemCount(): Int {

        return list.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: ArrayList<PrinterListModel>) {

       var temp = ""
        var newList = arrayListOf<PrinterListModel>()

        list.forEach {

            if (temp == it.printerName){
                //viewModelObject.deletePrinter(it)
//                Log.d("deDupedNodes","Duplicate operaion id = ${it.id} , name = ${it.printerName}")
            }else {
                Log.d("deDupedNodes","Unique opera")
//                Log.d("deDupedNodes","Unique operaion id = ${it.id} , name = ${it.printerName}")
                newList.add(it)
            }
            temp = it.printerName!!

        }

        this.list.clear()
        this.list = newList
        notifyDataSetChanged()

    }


    fun getList():List<PrinterListModel>{
        return list
    }




    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: PrinterListModel) {
        list.add(model)
         notifyItemRangeInserted(0,list.size)
        notifyDataSetChanged()

    }

    fun setListner(list: PrinterListInterface) {
        this.listner = list
    }

    interface PrinterListInterface {
        fun onPrinterSelected(printerListModel: PrinterListModel)
        fun onPrinterActive(printerListModel: PrinterListModel, layoutPosition: Int)
        fun onEditSelected(printerListModel: PrinterListModel)
        fun onDeletePrinter(printerListModel: PrinterListModel)
        fun onUpdatePrinterStatus(printerListModel: PrinterListModel, isChecked: Boolean)

    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        val size = list.size
        this.list.clear()
        list = arrayListOf()
        notifyItemRangeRemoved(0, size)
        notifyDataSetChanged()
    }

    fun removeItemAt(pos: Int) {
        list.removeAt(pos)
        notifyItemRangeRemoved(pos, list.size)
    }

    fun removeItem(item:PrinterListModel){
        list.remove(item)
        notifyDataSetChanged()

    }

    fun addAll(tempAvailableList: java.util.ArrayList<PrinterListModel>) {
        list.addAll(tempAvailableList)
        notifyDataSetChanged()
    }
}