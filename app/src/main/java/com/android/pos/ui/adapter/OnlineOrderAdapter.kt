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
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.ViewonlineorderlayoutBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.TAG
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson
import java.util.*


class OnlineOrderAdapter(val context: Context) :
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
            binding.llShowLayout?.visibility = View.GONE
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
                adapter = OnlineOrderItemsAdapter()
                binding.rvOpenOrder.adapter = adapter
                Log.e("TAG", "OpenOrderorderItems:  ${Gson().toJson(item.orderItems)}")

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
                        R.drawable.ic_down_solid_arrow,
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
            binding.root.setOnClickListener {

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
                        if (it.offlineId.lowercase(Locale.getDefault())
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
                            }
                        }
                    }

                        fList
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