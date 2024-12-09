package com.pays.pos.ui.adapter

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.databinding.ViewCustomerAssignOrderBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*
import kotlin.collections.ArrayList

class AssignCustomerToOrderAdapter :
    ListAdapter<TbCustomer, AssignCustomerToOrderAdapter.MyViewHolder>(CustomerDiffCallback()), Filterable {

    private var mList = ArrayList<TbCustomer>()
    private var filterList = ArrayList<TbCustomer>()

    private lateinit var mCallback: ItemCallback
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    public override fun getItem(position: Int): TbCustomer {
        return filterList[position]
    }

    fun add(categoryModel: List<TbCustomer>) {
        this.mList = categoryModel as ArrayList<TbCustomer>
        submitList(mList)
        this.filterList = categoryModel
        submitList(filterList)
//        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ViewCustomerAssignOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TbCustomer) {
            binding.model = item
            binding.executePendingBindings()

            val builder = StringBuilder()
            if (item.phones.isNotEmpty()) {
                item.phones.forEach {
                    builder.append(AlertUtils.usNumberFormat(it.phone_number) + " | ")
                }
            }

            if (item.addresses.isEmpty()) {
                binding.txtAddress1.visibility = View.GONE
            } else {
                binding.txtAddress1.visibility = View.VISIBLE
                for (i in 0..item.addresses.size) {
                    if (item.addresses[i].full_address.isNotEmpty()) {
                        binding.txtAddress1.text = item.addresses[i].full_address
                        break
                    }
                }
            }





            if (builder.isNotEmpty()) {
                binding.txtPhone.visibility = View.VISIBLE
                binding.txtPhone.text = builder.substring(0, builder.length - 3).toString()
            } else {
                binding.txtPhone.visibility = View.GONE
            }

            //customer name and loyalty point
            var fullname = item.first_name + " " + item.last_name
            if (fullname.toString().length > 39) {
                fullname.substring(0, 37) + "..."
            }
            setupNameAndLoyalty(
                fullname,
                item.enroll_to_loyalty == true,
                item.final_reward
            )
        }

        private fun setupNameAndLoyalty(
            name: String?,
            enrollToLoyalty: Boolean,
            loyaltyPoint: Int?
        ) {
            val ssName = SpannableStringBuilder(name)
            ssName.setSpan(
                TextAppearanceSpan(MainApplication.getInstance(), R.style.CustomerNameStyle),
                0,
                ssName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (enrollToLoyalty) {
                val ssPoint: SpannableStringBuilder = if (loyaltyPoint!! > 0) {
                    SpannableStringBuilder("Loyalty Points: $loyaltyPoint")
                } else {
                    SpannableStringBuilder("Loyalty Point: $loyaltyPoint")
                }

                ssPoint.setSpan(
                    TextAppearanceSpan(MainApplication.getInstance(), R.style.LoyaltyPointStyle),
                    0,
                    ssPoint.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                TextUtils.concat(
                    ssName.substring(0, 1).uppercase() + ssName.substring(1),
                    "  ",
                    ssPoint
                )
                    .also { binding.txtName.text = it }
            } else {
                TextUtils.concat(ssName.substring(0, 1).uppercase() + ssName.substring(1))
                    .also { binding.txtName.text = it }
            }

        }

        init {
            binding.root.setOnSingleClickListener {
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
        try {
            holder.bind(filterList[position])
        }catch (_:Exception){
            Log.e("AssignCustomerToOrderAdapter", "Error binding view at position $position")
        }
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase()
                filterList = if (charString.isEmpty()) {
                    mList
                } else {
                    val fList = ArrayList<TbCustomer>()

                    mList.filter {

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

                submitList(filterList)

//                notifyDataSetChanged()

            }
        }
    }

    class CustomerDiffCallback : DiffUtil.ItemCallback<TbCustomer>() {
        override fun areItemsTheSame(oldItem: TbCustomer, newItem: TbCustomer): Boolean {
            return oldItem.id == newItem.id // Assuming TbCustomer has a unique id
        }

        override fun areContentsTheSame(oldItem: TbCustomer, newItem: TbCustomer): Boolean {
            return oldItem == newItem
        }
    }

}