package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.databinding.ViewCustomerAssignOrderBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.callback.ItemCallback
import java.util.*
import kotlin.collections.ArrayList

class AssignCustomerToOrderAdapter :
    RecyclerView.Adapter<AssignCustomerToOrderAdapter.MyViewHolder>(), Filterable {

    private var mList = ArrayList<CustomerListResponse.Data>()
    private var filterList = ArrayList<CustomerListResponse.Data>()

    private lateinit var mCallback: ItemCallback
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    fun getItem(position: Int): CustomerListResponse.Data {
        return filterList[position]
    }

    fun add(categoryModel: List<CustomerListResponse.Data>) {
        this.mList = categoryModel as ArrayList<CustomerListResponse.Data>
        this.filterList = categoryModel
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ViewCustomerAssignOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CustomerListResponse.Data) {
            binding.model = item
            binding.executePendingBindings()

            val builder = StringBuilder()
            if (item.phones.isNotEmpty()) {
                item.phones.forEach {
                    builder.append(AlertUtils.usNumberFormat(it.phone_number) + " | ")
                }
            }

            if (item.addresses.isNotEmpty()) {
                binding.txtAddress1.visibility = View.VISIBLE
                item.addresses.forEach {
                    binding.txtAddress1.text = it.full_address
                }
            } else {
                binding.txtAddress1.visibility = View.GONE
            }

            if (builder.isNotEmpty()) {
                binding.txtPhone.visibility = View.VISIBLE
                binding.txtPhone.text = builder.substring(0, builder.length - 3).toString()
            } else {
                binding.txtPhone.visibility = View.GONE
            }
        }

        init {
            binding.root.setOnClickListener {
                mCallback.onItemClickListener(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AssignCustomerToOrderAdapter.MyViewHolder {
        val binding =
            ViewCustomerAssignOrderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(
        holder: AssignCustomerToOrderAdapter.MyViewHolder,
        position: Int
    ) {

        holder.bind(filterList[position])
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    mList
                } else {
                    val fList = ArrayList<CustomerListResponse.Data>()




                    mList.filter {

                        val ss = StringBuilder()
                        it.phones.forEach { phone ->
                            ss.append(phone.phone_number)
                        }
                        val phone = ss.toString()

                        it.first_name.lowercase(Locale.getDefault()).contains(charSequence) or
                                it.last_name.lowercase(Locale.getDefault())
                                    .contains(charSequence) or
                                it.email.lowercase(Locale.getDefault()).contains(charSequence) or
                                phone.contains(charSequence)
                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<CustomerListResponse.Data>
                }

                notifyDataSetChanged()

            }
        }
    }
}