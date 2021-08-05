package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.databinding.ViewCustomerListBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.extensions.getColorCompat
import java.util.*
import kotlin.collections.ArrayList


class CustomerListAdapter(
    val listner: CustomerInteface
) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>(), Filterable {
    private var filterList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> =
        arrayListOf()
    private val TAG = "CustomerListAdapter"

    private var list: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> =
        arrayListOf()
    private var isSelectedPos: Int = -1

    init {

        filterList = list
    }


    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: com.android.pos.data.model.CustomerListResponse.Data, pos: Int) {


            try {
                if (filterList.get(pos).first_name != null && filterList.get(
                        pos
                    ).last_name != null
                ) {
                    binding.tvInitialName.setText(
                        filterList.get(pos).first_name.first() + "" + filterList.get(
                            pos
                        ).last_name.first()
                    )

                } else {
                    binding.tvInitialName.setText(
                        "${
                            filterList.get(pos).first_name?.subSequence(
                                0,
                                2
                            )
                        }"
                    )
                }

                if (filterList.get(pos).email != null && filterList.get(
                        pos
                    ).phones.size > 0
                ) {

                    binding.txtNumber.setText(
                        "" + AlertUtils.usNumberFormat(
                            filterList.get(pos).phones.get(
                                0
                            ).phone_number
                        ) + " | " + filterList.get(pos).email
                    )
                } else if (filterList.get(pos).phones.size > 0) {
                    binding.txtNumber.setText(
                        "" + AlertUtils.usNumberFormat(
                            filterList.get(pos).phones.get(
                                0
                            ).phone_number
                        )
                    )

                }


                if (isSelectedPos == pos) {
                    binding.layout.background =
                        binding.root.context.getDrawable(R.color.txt_color_blue)
                    binding.txtName.setTextColor(binding.root.context.getColorCompat(R.color.white))
                    binding.txtNumber.setTextColor(binding.root.context.getColorCompat(R.color.white))
                } else {
                    binding.layout.background = binding.root.context.getDrawable(R.color.white)
                    binding.txtName.setTextColor(binding.root.context.getColorCompat(R.color.txtColor))
                    binding.txtNumber.setTextColor(binding.root.context.getColorCompat(R.color.colorB9))
                }

                binding.model = model
                binding.executePendingBindings()

                /* binding.root.setOnClickListener {

                 Log.e(TAG, "filterSize  ${filterList.size}")

                 //notifyDataSetChanged()
                 listner.onCustomerSelect(layoutPosition, filterList.get(layoutPosition))

             }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {

            binding.root.setOnClickListener {
                isSelectedPos = layoutPosition
                notifyDataSetChanged()
                listner.onCustomerSelect(layoutPosition, filterList[layoutPosition])
            }

        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomerListAdapter.MyViewHolder {
        val binding =
            ViewCustomerListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CustomerListAdapter.MyViewHolder, position: Int) {
        holder.bind(filterList[position], position)
    }

    override fun getItemCount(): Int {
        if (list.size > 0) {

            return filterList.size

        } else {
            return 0
        }
    }

    fun getList(): ArrayList<com.android.pos.data.model.CustomerListResponse.Data> {
        return filterList
    }


    interface CustomerInteface {
        fun onCustomerSelect(pos: Int, model: com.android.pos.data.model.CustomerListResponse.Data)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<CustomerListResponse.Data>()




                    list.filter {

                        val ss = StringBuilder()
                        it.phones.forEach { phone ->
                            ss.append(phone.phone_number)
                        }
                        val phone = ss.toString()

                        var company = ""
                        if (it.company != null) {
                            company = it.company
                        }

                        it.first_name.lowercase(Locale.getDefault()).contains(charSequence) or
                                it.last_name.lowercase(Locale.getDefault())
                                    .contains(charSequence) or
                                it.email.lowercase(Locale.getDefault()).contains(charSequence) or
                                phone.contains(charSequence) or
                                company.lowercase(Locale.getDefault()).contains(charSequence)
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

    fun getItem(position: Int): com.android.pos.data.model.CustomerListResponse.Data {
        return filterList[position]
    }

    fun setList(listData: ArrayList<com.android.pos.data.model.CustomerListResponse.Data>) {
        this.list = listData
        filterList = list
        notifyDataSetChanged()
    }

}