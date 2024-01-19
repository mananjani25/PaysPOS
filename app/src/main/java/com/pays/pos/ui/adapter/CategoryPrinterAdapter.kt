package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.databinding.ViewPrinterCategoryBinding

class CategoryPrinterAdapter(
    var list: ArrayList<PrinterResponse.Data.PrinterCategories>,
    var listner: CategoryPrinter
) :
    RecyclerView.Adapter<CategoryPrinterAdapter.MyViewHolder>() {
    inner class MyViewHolder(private val binding: ViewPrinterCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(model: PrinterResponse.Data.PrinterCategories) {
            binding.txtCategoryName.setText(model.name)
            binding.chCategory.isChecked = model.printerEnable


        }

        init {

            binding.chCategory.setOnCheckedChangeListener { compoundButton, b ->
                var selectAllCat = true
                if (compoundButton.isPressed) {

                    list[bindingAdapterPosition].printerEnable = b


                }
                list.forEach {
                    if (!it.printerEnable) {
                        selectAllCat = false
                        return@forEach
                    }

                }
                listner.categoryAllSelected(selectAllCat)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryPrinterAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewPrinterCategoryBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoryPrinterAdapter.MyViewHolder, position: Int) {
        holder.onBind(list[position])


    }

    override fun getItemCount(): Int {
        return list.size


    }

    fun addList(listData: ArrayList<PrinterResponse.Data.PrinterCategories>) {
        this.list.clear()
        this.list = arrayListOf()
        this.list.addAll(listData)
        notifyDataSetChanged()


    }

    fun getList(): List<PrinterResponse.Data.PrinterCategories> {
        return this.list
    }

    fun selectAll(boolean: Boolean) {
        this.list.forEach {
            it.printerEnable = boolean
        }
        notifyDataSetChanged()
    }

    interface CategoryPrinter {
        fun categoryAllSelected(flag: Boolean)
    }
}