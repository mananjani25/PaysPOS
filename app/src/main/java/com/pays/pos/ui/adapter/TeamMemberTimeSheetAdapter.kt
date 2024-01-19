package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.pays.pos.databinding.ViewItemTimesheetBinding
import com.pays.pos.ui.fragments.settings.teamrole.TeamMemberSheetViewModel
import java.util.*
import kotlin.collections.ArrayList

class TeamMemberTimeSheetAdapter(val viewModel: TeamMemberSheetViewModel) :
    RecyclerView.Adapter<TeamMemberTimeSheetAdapter.MyViewHolder>(), Filterable {

    var employeeTimeSheet = ArrayList<GetEmployeesTimeSheetResponse.Data>()
    private var filterList = ArrayList<GetEmployeesTimeSheetResponse.Data>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewItemTimesheetBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = filterList[position]
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = filterList.size

    fun teamTimesheetList(employeeTimeSheet: List<GetEmployeesTimeSheetResponse.Data>) {

        this.employeeTimeSheet.apply {
            clear()
            addAll(employeeTimeSheet)
            notifyDataSetChanged()
        }
        this.filterList = employeeTimeSheet as ArrayList<GetEmployeesTimeSheetResponse.Data>

    }


    inner class MyViewHolder(val discountItemBinding: ViewItemTimesheetBinding) :
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
                val charString = charSequence.toString().lowercase()
                filterList = if (charString.isEmpty()) {
                    employeeTimeSheet
                } else {
                    val fList = ArrayList<GetEmployeesTimeSheetResponse.Data>()

                    employeeTimeSheet.filter {
                        it.teamName.lowercase(Locale.getDefault()).contains(charSequence)

                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<GetEmployeesTimeSheetResponse.Data>
                }

                notifyDataSetChanged()

            }
        }
    }


}