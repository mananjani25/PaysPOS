package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.databinding.ViewCustomerListBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.getColorCompat
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*
import kotlin.collections.ArrayList


class CustomerListAdapter(
    val listner: CustomerInteface
) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>(), Filterable {
    private var filterList: ArrayList<TbCustomer> =
        arrayListOf()
    private val TAG = "CustomerListAdapter"

    private var list: ArrayList<TbCustomer> =
        arrayListOf()
    public var isSelectedPos: Int = 0

    init {

        filterList = list
    }

    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(model: TbCustomer, pos: Int) {

            val mModel = filterList[pos]


            if (mModel.last_name != null && mModel.last_name.isNotEmpty() && !mModel.last_name.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                binding.tvInitialName.text =
                    (mModel.first_name?.first() ?: "").toString() + "" + (mModel.last_name.first()
                        ?: "").toString()

            } else {
                binding.tvInitialName.text = "${
                    mModel.first_name?.subSequence(
                        0,
                        1
                    )
                }"
            }


            if (mModel.last_name != null && !mModel.last_name.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                binding.txtName.text =
                    mModel.first_name?.substring(0, 1)?.uppercase() + mModel.first_name?.substring(1) + " " + mModel.last_name

            } else {
                binding.txtName.text =
                    mModel.first_name?.substring(0, 1)?.uppercase() + mModel.first_name?.substring(1)
            }


            if (mModel.phones.isNotEmpty() && mModel.email?.isNotEmpty() == true) {
                binding.txtNumber.visibility = View.VISIBLE
                binding.txtNumber.text =
                    "" + AlertUtils.usNumberFormat(mModel.phones[0].phone_number) + " | " + mModel.email
            } else if (mModel.phones.isNotEmpty()) {
                binding.txtNumber.visibility = View.VISIBLE
                binding.txtNumber.text = AlertUtils.usNumberFormat(mModel.phones[0].phone_number)
            } else if (mModel.email?.isNotEmpty() == true) {
                binding.txtNumber.visibility = View.VISIBLE
                binding.txtNumber.text = mModel.email
            } else {
                binding.txtNumber.visibility = View.GONE
            }


            if (isSelectedPos == pos) {
                binding.layout.background =binding.root.context.getDrawable(R.drawable.button_action_hover)
                binding.txtName.setTextColor(binding.root.context.getColorCompat(R.color.txtColor))
                binding.txtNumber.setTextColor(binding.root.context.getColorCompat(R.color.txtColor))
            } else {
                binding.layout.background = binding.root.context.getDrawable(R.color.bg_color)
                binding.txtName.setTextColor(binding.root.context.getColorCompat(R.color.txtColor))
                binding.txtNumber.setTextColor(binding.root.context.getColorCompat(R.color.txtColor))
            }

            binding.model = model
            binding.executePendingBindings()


        }

        init {

            binding.root.setOnClickListener {
                isSelectedPos = layoutPosition
                notifyDataSetChanged()
                listner.onCustomerSelect(layoutPosition, filterList[layoutPosition])
            }
            binding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                mCallback?.onItemClickListener(it,layoutPosition)
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

    fun getList(): ArrayList<TbCustomer> {
        return filterList
    }


    interface CustomerInteface {
        fun onCustomerSelect(pos: Int, model: TbCustomer)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase()
                filterList = if (charString.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<TbCustomer>()

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

                        var fname = ""
                        if (it.first_name != null) {
                            fname = it.first_name
                        }

                        var lname = ""
                        if (it.last_name != null) {
                            lname = it.last_name
                        }

                        val name = "$fname $lname"

                        var email = ""
                        if (it.email != null) {
                            email = it.email
                        }

                        name.lowercase(Locale.getDefault()).contains(charSequence) or
                                email.lowercase(Locale.getDefault()).contains(charSequence) or
                                phone.contains(charSequence) or
                                company.lowercase(Locale.getDefault()).contains(charSequence)
                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<TbCustomer>
                }

                notifyDataSetChanged()

            }
        }
    }

    fun getItem(position: Int): TbCustomer {
        return filterList[position]
    }

    fun setList(listData: ArrayList<TbCustomer>) {
        this.list = listData
        filterList = list
        notifyDataSetChanged()
    }

}