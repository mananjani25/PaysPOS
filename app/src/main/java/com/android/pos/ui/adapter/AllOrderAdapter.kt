package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.OnlineOrderResponseModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ALL_ORDER_TAB
import com.android.pos.data.remote.Constants.ONLINE_ORDER_TAB
import com.android.pos.data.remote.Constants.OPEN_ORDER_TAB
import com.android.pos.data.remote.Constants.PHONE_ORDER_TAB
import com.android.pos.data.remote.Constants.THIRD_PARTY_ORDER_TAB
import com.android.pos.databinding.ViewAllOrderLayoutBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.TAG
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import java.text.SimpleDateFormat
import java.util.*


class AllOrderAdapter(val context: Context, val prefProvider: PrefProvider) :
    RecyclerView.Adapter<AllOrderAdapter.MyViewHolder>(),
    Filterable {
    var orderList = ArrayList<OnlineOrderResponseModel.Data>()
    var filterList = ArrayList<OnlineOrderResponseModel.Data>()
    var orderedTab: String = ""
    private var mCallback: OrderCallBack? = null
    fun setCallback(callback: OrderCallBack) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewAllOrderLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var adapter: OnlineOrderItemsAdapter? = null

        @SuppressLint("SetTextI18n")
        fun bind(item: OnlineOrderResponseModel.Data) {
            binding.viewModel = item
            binding.executePendingBindings()
            binding.llShowLayout.visibility = View.GONE

            if (orderedTab == ALL_ORDER_TAB) {
                binding.txtOrderType.visible()
                binding.txtOrderType.text = item.orderTypeName
            } else {
                binding.txtOrderType.gone()
            }

            if (prefProvider.getValueboolean(Constants.ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                binding.tvOrderID.text = item.custom_order_id.toString()
            } else {
                binding.tvOrderID.text = item.id.toString()
            }

            if (item.futureDeliveryDate != null && item.futureDeliveryDate.isNotEmpty()) {
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

            if(!item.terminalName.isNullOrEmpty()) {
                binding.txtStationName.text = item.terminalName
            }

            if (item.futureDeliveryTime != null && item.futureDeliveryTime.isNotEmpty()) {
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

            if (orderedTab == ALL_ORDER_TAB) {
                if (item.orderType == ONLINE_ORDER_TAB || item.orderType == THIRD_PARTY_ORDER_TAB) {
                    binding.viewOrderStatus.gone()
                    when (item.order_status) {
                        "Pending" -> {
                            binding.orderStatusLinear.visible()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "InProgress" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.visible()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "Completed" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.visible()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "UpComing" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.visible()
                        }

                        else -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.visible()
                            binding.orderUpcomingButton.gone()
                        }
                    }
                } else {
                    binding.viewOrderStatus.visible()
                    binding.orderStatusLinear.gone()
                    binding.orderInprogressButton.gone()
                    binding.orderCompletedButton.gone()
                    binding.orderCancelledButton.gone()
                    binding.orderUpcomingButton.gone()
                }
            } else {
                binding.viewOrderStatus.gone()
                if (orderedTab == ONLINE_ORDER_TAB || orderedTab == THIRD_PARTY_ORDER_TAB
                    || item.orderType == ONLINE_ORDER_TAB || item.orderType == THIRD_PARTY_ORDER_TAB
                ) {

                    when (item.order_status) {
                        "Pending" -> {
                            binding.orderStatusLinear.visible()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "InProgress" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.visible()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "Completed" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.visible()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.gone()
                        }

                        "UpComing" -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.gone()
                            binding.orderUpcomingButton.visible()
                        }

                        else -> {
                            binding.orderStatusLinear.gone()
                            binding.orderInprogressButton.gone()
                            binding.orderCompletedButton.gone()
                            binding.orderCancelledButton.visible()
                            binding.orderUpcomingButton.gone()
                        }
                    }
                }
            }

            if(item.customer != null){
                binding.txtCustomerName.text = (item.customer.firstName) + " " + (item.customer.lastName)
            }else{
                binding.txtCustomerName.text = "-"
            }

            if(item.employee != null){
                binding.txtEmployeeName.text = (item.employee.firstName) + " " + (item.employee.lastName)
            }else{
                binding.txtEmployeeName.text = "-"
            }

            if (item.orderItems.isNotEmpty()) {
                binding.rvOpenOrder.visible()
                adapter = OnlineOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter

                adapter!!.addAll(item.orderItems)
            } else {
                binding.rvOpenOrder.gone()
            }

            if (orderedTab == OPEN_ORDER_TAB) {
                binding.txtDeliveryOrPickup.gone()
            } else {
                if (item.deliveryType != null && item.deliveryType.isNotEmpty()) {
                    binding.txtDeliveryOrPickup.text = item.deliveryType
                }
                binding.txtDeliveryOrPickup.visible()
                if (item.orderType == OPEN_ORDER_TAB) {
                    binding.txtDeliveryOrPickup.text = "-"
                }else{
                    binding.txtDeliveryOrPickup.text = item.deliveryType
                }
            }

            if (!item.isCheck) {
                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.bg_color))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.txtColor))
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
                binding.llShowLayout.visibility = View.VISIBLE

                if (orderedTab == OPEN_ORDER_TAB || orderedTab == PHONE_ORDER_TAB
                    || item.orderType == OPEN_ORDER_TAB || item.orderType == PHONE_ORDER_TAB
                ) {
                    if (item.paymentStatus == "Paid" || item.paymentStatus == "Cancelled") {
                        binding.lnrPhoneAndOnlineButtons.gone()
                    } else {
                        binding.lnrPhoneAndOnlineButtons.visible()
                    }
                } else {
                    binding.lnrPhoneAndOnlineButtons.gone()
                }

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
                mCallback?.onItemClickListener(it, absoluteAdapterPosition, "rejected")
            }
            binding.completedImg.setOnClickListener {
                mCallback?.onItemClickListener(it, absoluteAdapterPosition, "Completed")
            }

            binding.root.setOnClickListener {
                try {
                    val item = filterList[bindingAdapterPosition]

                    if (item.isCheck) {
                        item.isCheck = false
                    } else {
                        filterList.forEach {
                            it.isCheck = false
                        }
                        item.isCheck = true

                    }
                    notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            binding.txtCancelOrder.setOnClickListener {
                if (MethodUtils.isDoubleClick()) return@setOnClickListener
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "CANCEL")
            }

            binding.txtEditOrder.setOnClickListener {
                binding.txtEditOrder.background =
                    AppCompatResources.getDrawable(itemView.context,R.drawable.button_selected)
                binding.txtPayNow.background =
                    AppCompatResources.getDrawable(itemView.context,R.drawable.background_square_border_grey)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "UPDATE")
            }

            binding.txtPayNow.setOnClickListener {
                binding.txtEditOrder.background =
                    AppCompatResources.getDrawable(itemView.context,R.drawable.background_square_border_grey)
                binding.txtPayNow.background =
                    AppCompatResources.getDrawable(itemView.context,R.drawable.button_selected)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "PAY")
            }

            binding.txtCustomerReceipt.setOnClickListener {
                if (filterList[bindingAdapterPosition].paymentStatus == "Paid") {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, Constants.PRINT_PAID)
                } else {
                    mCallback?.onItemClickListener(
                        it, bindingAdapterPosition,
                        Constants.PRINT_UNPAID
                    )
                }

            }

            binding.txtRePrintKitchenReceipt.setOnClickListener {
                mCallback?.onItemClickListener(
                    it,
                    bindingAdapterPosition,
                    "REPRINT_KITCHEN_RECEIPT"
                )
            }
        }
    }


    fun add(orders: List<OnlineOrderResponseModel.Data>, orderTab: String) {
        this.orderList = orders as ArrayList<OnlineOrderResponseModel.Data>
        this.filterList = orders
        this.orderedTab = orderTab
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewAllOrderLayoutBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun getItem(pos: Int): OnlineOrderResponseModel.Data {

        return filterList[pos]
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
                        } else*/ if ((if (prefProvider.getValueboolean(
                                    Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                                    false
                                )
                            )
                                it.custom_order_id.toString().lowercase(Locale.getDefault()) else
                                it.id.toString().lowercase(Locale.getDefault()))
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
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