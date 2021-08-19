package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTransactionListResponse
import com.android.pos.databinding.ViewTransactionItemBinding
import com.android.pos.ui.fragments.transactions.TransactionViewModel
import com.android.pos.utils.TimeFormatUtils.convertCurrentDate
import com.android.pos.utils.TimeFormatUtils.convertCurrentTime

class TransactionAdapter(val viewModel: TransactionViewModel) :
    RecyclerView.Adapter<TransactionAdapter.MyViewHolder>(), Filterable {

    var employeeTimeSheet = ArrayList<GetTransactionListResponse.Data.Payment>()
    private var filterList = ArrayList<GetTransactionListResponse.Data.Payment>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewTransactionItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = filterList[position]
        itemBinding.viewModel = viewModel

        val model = filterList[position]

        itemBinding.tvDate.text =
            convertCurrentDate(filterList[position].createdAt) + "\n" + convertCurrentTime(
                filterList[position].createdAt
            )
        itemBinding.txtCustomerName.text =
            (model.customer.firstName ?: "") + " " + (model.customer.lastName ?: "")

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = filterList.size

    fun teamTimesheetList(employeeTimeSheet: List<GetTransactionListResponse.Data.Payment>) {

        this.employeeTimeSheet.apply {
            clear()
            addAll(employeeTimeSheet)
            notifyDataSetChanged()
        }
        this.filterList = employeeTimeSheet as ArrayList<GetTransactionListResponse.Data.Payment>

    }


    inner class MyViewHolder(val discountItemBinding: ViewTransactionItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    employeeTimeSheet
                } else {
                    val fList = ArrayList<GetTransactionListResponse.Data.Payment>()

                    /* employeeTimeSheet.filter {
                         it.teamName.lowercase(Locale.getDefault()).contains(charSequence)

                     }.forEach { fList.add(it) }*/

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList =
                        results.values as ArrayList<GetTransactionListResponse.Data.Payment>
                }

                notifyDataSetChanged()

            }
        }
    }


}