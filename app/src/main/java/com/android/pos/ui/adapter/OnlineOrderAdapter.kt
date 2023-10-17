package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.OnlineOrderResponseModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewonlineorderlayoutBinding
import com.android.pos.utils.LogUtil
import com.android.pos.di.PrefProvider
import com.android.pos.utils.TAG
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*


class OnlineOrderAdapter(val context: Context, val prefProvider: PrefProvider) :
    RecyclerView.Adapter<OnlineOrderAdapter.MyViewHolder>(),
    Filterable {
    var orderList = ArrayList<OnlineOrderResponseModel.Data>()
    var filterList = ArrayList<OnlineOrderResponseModel.Data>()
    private var mCallback: OrderCallBack? = null
    fun setCallback(callback: OrderCallBack) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewonlineorderlayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var adapter: OnlineOrderItemsAdapter? = null

        @SuppressLint("SetTextI18n")
        fun bind(item: OnlineOrderResponseModel.Data) {
            binding.viewModel = item
            binding.executePendingBindings()
            binding.llShowLayout.visibility = View.GONE
            if (prefProvider.getValueboolean(Constants.ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                binding.tvOrderID.text = item.custom_order_id.toString()
            } else {
                binding.tvOrderID.text = item.id.toString()
            }

            binding.txtOrderType?.text = item.deliveryType
            if (item.futureDeliveryDate != null) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd")
                val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
                val date = inputFormat.parse(item.futureDeliveryDate)
                val formattedDate = outputFormat.format(date)
                binding.tvDate.text = formattedDate
            } else {
                binding.tvDate.text =
                    TimeFormatUtils.convertCurrentDate(
                        item.createdAt,
                        context
                    )
            }
            if (item.futureDeliveryTime != null) {
                val inputFormat = SimpleDateFormat("hh:mm a")
                val outputFormat = SimpleDateFormat("hh:mm a")
                TimeFormatUtils.prefProvider = PrefProvider(context = context!!)
                val date = inputFormat.parse(item.futureDeliveryTime.toString())
                val formattedTime = outputFormat.format(date)
                binding.tvtime.text = formattedTime
            } else {
                binding.tvtime.text =
                    TimeFormatUtils.convertCurrentTime(
                        item.createdAt,
                        context
                    )
            }


            if (item.order_status == "Pending") {
                binding.orderStatusLinear?.visible()
                binding.orderInprogressButton?.gone()
                binding.orderCompletedButton?.gone()
                binding.orderCancelledButton?.gone()
                binding.orderUpcomingButton?.gone()
            } else if (item.order_status == "InProgress") {
                binding.orderStatusLinear?.gone()
                binding.orderInprogressButton?.visible()
                binding.orderCompletedButton?.gone()
                binding.orderCancelledButton?.gone()
                binding.orderUpcomingButton?.gone()
            } else if (item.order_status == "Completed") {
                binding.orderStatusLinear?.gone()
                binding.orderInprogressButton?.gone()
                binding.orderCompletedButton?.visible()
                binding.orderCancelledButton?.gone()
                binding.orderUpcomingButton?.gone()
            } else if (item.order_status == "UpComing") {
                binding.orderStatusLinear?.gone()
                binding.orderInprogressButton?.gone()
                binding.orderCompletedButton?.gone()
                binding.orderCancelledButton?.gone()
                binding.orderUpcomingButton?.visible()
            } else {
                binding.orderStatusLinear?.gone()
                binding.orderInprogressButton?.gone()
                binding.orderCompletedButton?.gone()
                binding.orderCancelledButton?.visible()
                binding.orderUpcomingButton?.gone()
            }



            binding.txtCustomerName.text =
                (item.customer?.firstName ?: "") + " " + (item.customer?.lastName ?: "")
            if (item.orderItems.isNotEmpty()) {
                binding.rvOpenOrder.visible()
                adapter = OnlineOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter

                adapter!!.addAll(item.orderItems)
            } else {
                binding.rvOpenOrder.gone()
            }
            if (!item.isCheck) {
                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.bg_color))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvPaymentstatus.setTextColor(binding.root.resources.getColor(R.color.btnColor))
                binding.llShowLayout.visibility = View.GONE
                binding.imgIndicator.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.background_sales_button,
                        binding.root.resources.newTheme()
                    )
                )
                binding.imgIndicator.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.drawable_ic_color
                    )
                )
            } else {

                binding.llMainLayout.background =
                    itemView.context.getDrawable(R.drawable.button_selected)
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvPaymentstatus.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.llShowLayout.visibility = View.VISIBLE
                binding.imgIndicator.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_solid_up_arrow,
                        binding.root.resources.newTheme()
                    )
                )
                binding.imgIndicator.setColorFilter(ContextCompat.getColor(context, R.color.white))
            }
        }

        init {
            binding.acceptImg.setOnClickListener {
                mCallback?.onItemClickListener(it, absoluteAdapterPosition, "accepted")
            }
            binding.declineImg.setOnClickListener {
                mCallback?.onItemClickListener(it, absoluteAdapterPosition, "cancelled")
            }
            binding.completedImg?.setOnClickListener {
                mCallback?.onItemClickListener(it, absoluteAdapterPosition, "Completed")
            }
            binding.root.setOnClickListener {

                val item = filterList[position]

                if (item.isCheck) {
                    item.isCheck = false
                } else {
                    filterList.forEach {
                        it.isCheck = false
                    }
                    item.isCheck = true

                }
                notifyDataSetChanged()

            }
        }
    }


    fun add(orders: List<OnlineOrderResponseModel.Data>) {
        this.orderList = orders as ArrayList<OnlineOrderResponseModel.Data>
        this.filterList = orders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewonlineorderlayoutBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    orderList
                } else {
                    val fList = ArrayList<OnlineOrderResponseModel.Data>()

                    for (it in orderList) {
                        Log.d(TAG, "performFiltering: " + it.offlineId)
                        /*if (it.offlineId.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(it)
                        } else*/ if ((if (prefProvider.getValueboolean(Constants.ORDER_NUMBER_STARTING_FROM_ONE,false))
                                it.custom_order_id.toString().lowercase(Locale.getDefault()) else
                                it.id.toString().lowercase(Locale.getDefault()))
                                .contains(charString.lowercase(Locale.getDefault()))) {
                            fList.add(it)
                        } else if (it.customer != null && it.customer.firstName.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(it)
                        } else if (it.customer != null && it.customer.lastName.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(it)
                        } else if (it.customer != null && ((it.customer.firstName.lowercase(Locale.getDefault()) + " "
                                    + it.customer.lastName.lowercase(Locale.getDefault())))
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            fList.add(it)
                        } else if (it.employee != null) {
                            if (
                                it.employee.firstName.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault()))
                            ) {
                                fList.add(it)
                            } else if (it.employee.lastName.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault()))
                            ) {
                                fList.add(it)
                            } else if (it.employee.name.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault()))
                            ) {
                                fList.add(it)
                            }
                        }
                    }

                    fList
                }
                if (filterList.size == 0){
                    mCallback?.noDataAvailableFilter()
                }else {
                    mCallback?.hideNoDataAvailable()
                }
                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<OnlineOrderResponseModel.Data>
                }

                notifyDataSetChanged()

            }
        }
    }

}