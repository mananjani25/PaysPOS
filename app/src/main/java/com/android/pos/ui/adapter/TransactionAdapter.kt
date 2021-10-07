package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTransactionListResponse
import com.android.pos.databinding.ViewPaginationBinding
import com.android.pos.databinding.ViewTransactionItemBinding
import com.android.pos.ui.fragments.transactions.TransactionViewModel
import com.android.pos.utils.TimeFormatUtils.convertCurrentDate
import com.android.pos.utils.TimeFormatUtils.convertCurrentTime
import com.android.pos.utils.callback.ItemCallback

class TransactionAdapter(val viewModel: TransactionViewModel) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    var employeeTimeSheet = ArrayList<GetTransactionListResponse.Data.Payment>()
    private var filterList = ArrayList<GetTransactionListResponse.Data.Payment>()
    private val TYPE_FOOTER = 1
    private val TYPE_ITEM = 2


    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    private var showLoader = false

    fun showLoading(status: Boolean) {
        showLoader = status
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            val binding = ViewTransactionItemBinding.inflate(inflater, parent, false)
            MyViewHolder(binding)
        } else {
            val binding = ViewPaginationBinding.inflate(inflater, parent, false)
            FooterViewHolder(binding)
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            val itemBinding = holder.discountItemBinding
            itemBinding.itemSheetModel = filterList[position]
            itemBinding.viewModel = viewModel
            val context = itemBinding.root.context

            val model = filterList[position]

            itemBinding.tvDate.text =
                convertCurrentDate(filterList[position].createdAt) + "\n" + convertCurrentTime(
                    filterList[position].createdAt
                )
            itemBinding.txtCustomerName.text =
                (model.customer.firstName ?: "") + " " + (model.customer.lastName ?: "")

            if (filterList[position].refundedAmount != 0.0) {
                itemBinding.tvRefundedAmount.text =
                    "(Refunded \n" + context.getString(R.string.symbole) + " " + String.format(
                        context.getString(R.string.format),
                        filterList[position].refundedAmount
                    ) + ")"
            } else {
                //  itemBinding.tvRefundedAmount.visibility = View.GONE
            }

            itemBinding.executePendingBindings()

            itemBinding.txtTip.setOnClickListener {

                mCallback?.onItemClickListener(it, position)
            }
        }
    }

    override fun getItemCount() = filterList.size

    fun addAll(employeeTimeSheets: List<GetTransactionListResponse.Data.Payment>) {

        Log.e("teamTimesheetList", employeeTimeSheets.size.toString())
        employeeTimeSheet.addAll(employeeTimeSheets)
        filterList.addAll(employeeTimeSheets)
        notifyDataSetChanged()
    }


    inner class MyViewHolder(val discountItemBinding: ViewTransactionItemBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

    }

    inner class FooterViewHolder(paginationBinding: ViewPaginationBinding) :
        RecyclerView.ViewHolder(paginationBinding.root) {

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

    fun getItem(pos: Int): GetTransactionListResponse.Data.Payment {

        return filterList[pos]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /*override fun getItemViewType(position: Int): Int {
        return position
    }*/

    override fun getItemViewType(position: Int): Int {
        return if (showLoader) {
            if (position == filterList.size - 1) TYPE_FOOTER else TYPE_ITEM
        } else {
            TYPE_ITEM
        }
    }

    fun clear() {
        employeeTimeSheet.clear()
        filterList.clear()
    }


}