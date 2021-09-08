package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.EditPrinterModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewEditPrinterListBinding

class EditPrinterListAdapter : RecyclerView.Adapter<EditPrinterListAdapter.MyViewHolder>() {
    private var list: ArrayList<PrinterResponse.Data.OrderTypes> = arrayListOf()

    inner class MyViewHolder(private val binding: ViewEditPrinterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: PrinterResponse.Data.OrderTypes) {
            binding.model = model
            binding.txtPrintersLabel.setText(model.orderTypeName)
            if (model.printerSettings.size == 2) {
                if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {
                    binding.chBoxCustomerManual.isChecked =
                        model.printerSettings.get(0).manualPrinting
                    binding.chBoxCustomerManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting


                } else {
                    binding.chBoxKitchenManual.isChecked =
                        model.printerSettings.get(0).manualPrinting
                    binding.chBoxKitchenManual2.isChecked =
                        model.printerSettings.get(0).autoPrinting


                }

                if (model.printerSettings.get(1).printType == Constants.KITCHEN) {
                    binding.chBoxKitchenManual.isChecked =
                        model.printerSettings.get(1).manualPrinting
                    binding.chBoxKitchenManual2.isChecked =
                        model.printerSettings.get(1).autoPrinting

                } else {
                    binding.chBoxCustomerManual.isChecked =
                        model.printerSettings.get(1).manualPrinting
                    binding.chBoxCustomerManual2.isChecked =
                        model.printerSettings.get(1).autoPrinting
                }

            }

            binding.chBoxCustomerManual.setOnCheckedChangeListener { buttonView, isChecked ->

                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).manualPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).manualPrinting = isChecked

                    }
                }

            }
            binding.chBoxCustomerManual2.setOnCheckedChangeListener { buttonView, isChecked ->
                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.CUSTOMER) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).autoPrinting = isChecked

                    }
                }

            }

            binding.chBoxKitchenManual.setOnCheckedChangeListener { buttonView, isChecked ->


                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.KITCHEN) {

                        model.printerSettings.get(0).manualPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).manualPrinting = isChecked

                    }
                }

            }

            binding.chBoxKitchenManual2.setOnCheckedChangeListener { buttonView, isChecked ->

                if (model.printerSettings.size == 2) {
                    if (model.printerSettings.get(0).printType == Constants.KITCHEN) {

                        model.printerSettings.get(0).autoPrinting = isChecked
                    } else {
                        model.printerSettings.get(1).autoPrinting = isChecked

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
        this.list = dataList
        notifyDataSetChanged()
    }

    fun getList(): ArrayList<PrinterResponse.Data.OrderTypes> {
        return list
    }


}