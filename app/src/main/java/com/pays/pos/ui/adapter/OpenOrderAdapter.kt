package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.PRINT_PAID
import com.pays.pos.data.remote.Constants.PRINT_UNPAID
import com.pays.pos.databinding.ViewOpenOrderItemBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TimeFormatUtils
import com.pays.pos.utils.callback.OrderCallBack
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.google.gson.Gson
import java.util.*

class OpenOrderAdapter(val context: Context, val prefProvider: PrefProvider) :

    RecyclerView.Adapter<OpenOrderAdapter.MyViewHolder>(), Filterable {

    var orderList = ArrayList<OpenOrderResponse.Data.Order>()
    var filterList = ArrayList<OpenOrderResponse.Data.Order>()
    private val TAG = "OpenOrderAdapter"

    private var mCallback: OrderCallBack? = null
    fun setCallback(callback: OrderCallBack) {
        mCallback = callback
    }


    inner class MyViewHolder(private val binding: ViewOpenOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var adapter: OpenOrderItemsAdapter? = null

        @SuppressLint("SetTextI18n")
        fun bind(item: OpenOrderResponse.Data.Order) {
            binding.viewModel = item
            binding.executePendingBindings()

            binding.llShowLayout.visibility = View.GONE



            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE,false)){
                binding.tvOrderID.text = item.custom_order_id.toString()
            }else{
                binding.tvOrderID.text = item.id.toString()
            }
            if (item.createdAt.isNotEmpty()) {
                binding.tvDate.text =
                    TimeFormatUtils.convertCurrentDate(
                        item.createdAt,
                        context
                    )
                binding.tvtime.text =
                    TimeFormatUtils.convertCurrentTime(
                        item.createdAt,
                        context
                    )

            }

            binding.txtCustomerName.text =
                (item.customer?.firstName ?: "") + " " + (item.customer?.lastName ?: "")

            if (item.orderItems.isNotEmpty()) {

                binding.rvOpenOrder.visible()
/*
                binding.rvOpenOrder.addItemDecoration(
                    DividerItemDecoration(
                        binding.root.context,
                        LinearLayoutManager.VERTICAL
                    )
                )
*/

                adapter = OpenOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter
                LogUtil.logE(TAG,"OpenOrderorderItems:  ${Gson().toJson(item.orderItems)}")

                adapter!!.addAll(item.orderItems)
            } else {
                binding.rvOpenOrder.gone()
            }


            if (item.paymentStatus == "Cancelled" || item.paymentStatus == "Paid") {

                binding.txtCancelOrder.visibility = View.GONE
                binding.txtEditOrder.visibility = View.GONE

                binding.txtPayNow.visibility = View.GONE


            } else {
                binding.txtCancelOrder.visibility = View.VISIBLE
                binding.txtEditOrder.visibility = View.VISIBLE
                binding.txtPayNow.visibility = View.VISIBLE
            }

            if (!item.isCheck) {

                binding.llMainLayout.setBackgroundColor(binding.root.resources.getColor(R.color.bg_color))
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.txtColor))
                binding.llShowLayout.visibility = View.GONE
                binding.imgIndicator.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_down_solid_arrow,
                        binding.root.resources.newTheme()
                    )
                )

//                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_down_solid_arrow))
                binding.imgIndicator.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.drawable_ic_color
                    )
                )
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.btnColor))

            } else {

                binding.llMainLayout.background =
                    itemView.context.getDrawable(R.drawable.button_selected)
                binding.tvDate.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderType.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalAmount.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTotalTips.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.txtCustomerName.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvOrderID.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvTeamMember.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.llShowLayout.visibility = View.VISIBLE
//                binding.imgIndicator.setImageDrawable(binding.root.resources.getDrawable(R.drawable.ic_solid_up_arrow))
                binding.imgIndicator.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_solid_up_arrow,
                        binding.root.resources.newTheme()
                    )
                )
                binding.imgIndicator.setColorFilter(ContextCompat.getColor(context, R.color.white))
                binding.tvDeliveryStatus.setTextColor(binding.root.resources.getColor(R.color.white))

            }
        }

        init {
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
//                binding.txtCancelOrder.background =
//                    itemView.context.getDrawable(R.drawable.button_selected)
//                binding.txtEditOrder.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)

//                binding.txtCustomerReceipt.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
//                binding.txtPayNow.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "")
            }


            binding.txtEditOrder.setOnClickListener {
//                binding.txtCancelOrder.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background =
                    itemView.context.getDrawable(R.drawable.button_selected)
//                binding.txtCustomerReceipt.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background =
                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "UPDATE")
            }

            binding.txtPayNow.setOnClickListener {
//                binding.txtCancelOrder.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtEditOrder.background =
                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
//                binding.txtCustomerReceipt.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtPayNow.background =
                    itemView.context.getDrawable(R.drawable.button_selected)
                mCallback?.onItemClickListener(it, bindingAdapterPosition, "PAY")
            }
            binding.txtCustomerReceipt.setOnClickListener {
//                binding.txtCancelOrder.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
//                binding.txtEditOrder.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
//                binding.txtCustomerReceipt.background =
//                    itemView.context.getDrawable(R.drawable.button_selected)
//                binding.txtPayNow.background =
//                    itemView.context.getDrawable(R.drawable.background_square_border_grey)
                if (filterList[bindingAdapterPosition].paymentStatus == "Paid") {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_PAID)
                } else {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition, PRINT_UNPAID)
                }

            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OpenOrderAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOpenOrderItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OpenOrderAdapter.MyViewHolder, position: Int) {
        holder.bind(filterList.get(position))
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    fun add(orders: List<OpenOrderResponse.Data.Order>) {
        this.orderList = orders as ArrayList<OpenOrderResponse.Data.Order>
        this.filterList = orders
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): OpenOrderResponse.Data.Order {

        return filterList[pos]
    }

    fun update(position: Int) {
        filterList.removeAt(position)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    orderList
                } else {
                    val fList = ArrayList<OpenOrderResponse.Data.Order>()

                    for (it in orderList) {
                        if ((if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE,false))
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
                            if (it.employee.firstName.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault())) ||
                                it.employee.lastName.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault())) ||
                                it.employee.name.lowercase(Locale.getDefault())
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
                    filterList = results.values as ArrayList<OpenOrderResponse.Data.Order>
                }

                notifyDataSetChanged()

            }
        }
    }

}