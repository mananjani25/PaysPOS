package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.ViewEditPrinterListBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.google.gson.Gson

class EditPrinterListAdapter : RecyclerView.Adapter<EditPrinterListAdapter.MyViewHolder>() {
    private var list: ArrayList<PrinterResponse.Data.OrderTypes> = arrayListOf()
    private val TAG = "EditPrinterListAdapter"

    inner class MyViewHolder(private val binding: ViewEditPrinterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterResponse.Data.OrderTypes) {
            binding.model = model
            binding.txtPrintersLabel.setText(model.orderTypeName)
            LogUtil.logE(TAG, "printerSettings:  ${Gson().toJson(model.printerSettings)}")


            if (model.printerSettings.size == 2 ) {
                binding.txtKitReceipt.visible()
                binding.txtCustomerReceipt.visible()
                binding.chBoxCustomerManual2.visible()
                binding.chBoxKitchenManual2.visible()
                if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {
                  /*  binding.chBoxCustomerManual.isChecked =
                        model.printerSettings.get(0).manualPrinting*/
                    binding.chBoxCustomerManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting


                } else {
                  /*  binding.chBoxKitchenManual.isChecked =
                        model.printerSettings.get(0).manualPrinting*/
                    binding.chBoxKitchenManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting


                }

                if (model.printerSettings.get(1).printType == Constants.KITCHEN) {
                   /* binding.chBoxKitchenManual.isChecked =
                        model.printerSettings.get(1).manualPrinting*/
                    binding.chBoxKitchenManual2.isChecked =
                        model.printerSettings.get(1).autoPrinting

                } else {
                  /*  binding.chBoxCustomerManual.isChecked =
                        model.printerSettings.get(1).manualPrinting*/
                    binding.chBoxCustomerManual2.isChecked =
                        model.printerSettings.get(1).autoPrinting
                }



            } else if (model.printerSettings.size == 1) {


                if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {
                    binding.chBoxKitchenManual2.gone()
                    binding.chBoxCustomerManual2.visible()
                    binding.viewLine.gone()
                    binding.viewLine2.gone()
                    binding.txtCustomerReceipt.visible()

                    binding.txtKitReceipt.gone()


                    binding.chBoxCustomerManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting


                } else if (model.printerSettings.get(0).printType == Constants.KITCHEN) {
                    binding.chBoxKitchenManual2.visible()
                    binding.chBoxCustomerManual2.gone()
                    binding.viewLine.gone()
                    binding.viewLine2.gone()
                    binding.txtKitReceipt.visible()
                    binding.txtCustomerReceipt.gone()

                   /* binding.chBoxKitchenManual.isChecked =
                        model.printerSettings.get(0).manualPrinting*/
                    binding.chBoxKitchenManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting

                }

            }

            /*binding.chBoxCustomerManual.setOnCheckedChangeListener { buttonView, isChecked ->

                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).manualPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).manualPrinting = isChecked

                    }
                }

            }*/
            binding.chBoxCustomerManual2.setOnCheckedChangeListener { buttonView, isChecked ->
                if (model.printerSettings.size == 2) {

                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).autoPrinting = isChecked

                    }
                } else if (model.printerSettings.size == 1) {
                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(0).autoPrinting = isChecked

                    }

                }

            }

            /*binding.chBoxKitchenManual.setOnCheckedChangeListener { buttonView, isChecked ->


                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.KITCHEN) {

                        model.printerSettings.get(0).manualPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).manualPrinting = isChecked

                    }
                }

            }*/

            binding.chBoxKitchenManual2.setOnCheckedChangeListener { buttonView, isChecked ->

                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.KITCHEN) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).autoPrinting = isChecked

                    }
                } else if (model.printerSettings.size == 1) {
                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(0).autoPrinting = isChecked

                    }

                }


            }



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
    fun setList(dataList: ArrayList<PrinterResponse.Data.OrderTypes>) {
        /*---------------Removing the Kiosk Order Types-----------------*/
        var toRemove=dataList.filter { it.orderTypeName.contains("Kiosk",ignoreCase = true) }
        dataList.removeAll(toRemove)
        /*---------------Removing the Kiosk Order Types-----------------*/

        list.clear()
        list = arrayListOf()
        this.list = dataList
        notifyDataSetChanged()
    }

    fun getList(): ArrayList<PrinterResponse.Data.OrderTypes> {
        return list
    }


}