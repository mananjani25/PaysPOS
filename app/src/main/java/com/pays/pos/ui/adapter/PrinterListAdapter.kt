package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.AVAILABLE
import com.pays.pos.data.remote.Constants.WIFI
import com.pays.pos.databinding.ViewPrinterItemBinding
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.fragments.settings.hardware.printer.Printer.Companion.viewModelObject
import com.pays.pos.utils.TAG
import org.greenrobot.eventbus.EventBus


class PrinterListAdapter : RecyclerView.Adapter<PrinterListAdapter.MyViewHolder>() {

    public var dataList: ArrayList<PrinterListModel> = arrayListOf()
    private lateinit var listner: PrinterListInterface

    inner class MyViewHolder(private val binding: ViewPrinterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterListModel?) {

            if (model?.printerName != "") {
                binding.txtPrinterName.text = model?.printerName
            } else {
                model.printerName = "InnerPrinter"
            }


            if (dataList[layoutPosition].connectionType == WIFI) {
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
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} PrinterListAdapter.kt_binding.imgPrinter.setOnClickListener Clicked"
                    )
                )
                if (dataList.isNotEmpty()) {
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} PrinterListAdapter.kt_binding.imgPrinter.setOnClickListener dataList.isNotEmpty()"
                        )
                    )
                    if (dataList[layoutPosition].isActive) {
                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} PrinterListAdapter.kt_binding.imgPrinter.setOnClickListener isActive"
                            )
                        )
                        listner.onPrinterSelected(dataList[layoutPosition])
                    }
                }
            }

            binding.imgDelete.setOnClickListener {
                if (dataList.isNotEmpty()) {
                    listner.onDeletePrinter(dataList[layoutPosition])
                }
            }

            binding.swtOrderId.setOnCheckedChangeListener(object :
                CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                   /* buttonView?.let { buttonView ->

                    }*/

                    if (buttonView!!.isPressed) {
                        if (dataList[layoutPosition].type != AVAILABLE) {
                            if (isChecked) {
                                buttonView!!.isChecked = false
                                listner.onUpdatePrinterStatus(dataList[layoutPosition], isChecked)
                            } else {
                                buttonView!!.isChecked = true
                                listner.onUpdatePrinterStatus(dataList[layoutPosition], isChecked)
                            }
                        } else if (isChecked && !(dataList.get(layoutPosition).isActive)) {
                            buttonView!!.isChecked = false
                            listner.onPrinterActive(dataList.get(layoutPosition), layoutPosition)
                            /*dataList.removeAt(layoutPosition)
                        notifyDataSetChanged()*/
                        }

                    }
                }
            })


            binding.imgEdit.setOnClickListener {
                try {
                    if (dataList[layoutPosition].type != AVAILABLE) {
                        listner.onEditSelected(dataList[layoutPosition])
                    }
                } catch (e: Exception) {
                    Log.d("layoutPosition", "Error : ${e.message}")
                }

            }

            if (dataList[layoutPosition].type == AVAILABLE) {
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
        Log.e("REC_CRASH", position.toString())
        holder.bind(dataList?.get(position))
    }

    override fun getItemCount(): Int {

        return dataList.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setList(dataList: ArrayList<PrinterListModel>) {

        var temp = ""
        var newList = arrayListOf<PrinterListModel>()

        dataList.forEach {

            if (temp == it.printerName) {
                //viewModelObject.deletePrinter(it)
//                Log.d("deDupedNodes","Duplicate operaion id = ${it.id} , name = ${it.printerName}")
            } else {
                Log.d("deDupedNodes", "Unique opera")
//                Log.d("deDupedNodes","Unique operaion id = ${it.id} , name = ${it.printerName}")
                newList.add(it)
            }
            temp = it.printerName!!

        }

        this.dataList.clear()
        this.dataList = newList
        notifyDataSetChanged()

    }


    fun getList(): List<PrinterListModel> {
        return dataList
    }


    @SuppressLint("NotifyDataSetChanged")
    fun addItem(model: PrinterListModel) {
        dataList.add(model)
        notifyItemRangeInserted(0, dataList.size)
        notifyDataSetChanged()

    }

    fun setListner(dataList: PrinterListInterface) {
        this.listner = dataList
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
        val size = dataList.size
        this.dataList.clear()
        dataList = arrayListOf()
        notifyItemRangeRemoved(0, size)
        notifyDataSetChanged()
    }

    fun removeItemAt(pos: Int) {
        if (pos<dataList.size){
        dataList.removeAt(pos)
        notifyItemRangeRemoved(pos, dataList.size)}
    }

    fun removeItem(item: PrinterListModel) {
        dataList.remove(item)
        notifyDataSetChanged()

    }

    fun addAll(tempAvailableList: java.util.ArrayList<PrinterListModel>) {
        dataList.addAll(tempAvailableList)
        notifyDataSetChanged()
    }
}