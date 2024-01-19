package com.pays.pos.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Employee
import com.pays.pos.databinding.ViewAssignTeamMemberRoleBinding
import java.util.*
import kotlin.collections.ArrayList

class AssignRoleTeamMemberListAdapter() :
    RecyclerView.Adapter<AssignRoleTeamMemberListAdapter.MyViewHolder>(), Filterable {
    var itemsList = ArrayList<Employee>()
    var filterList = ArrayList<Employee>()
    var selectedItemList = ArrayList<Employee>()

    inner class MyViewHolder(private val binding: ViewAssignTeamMemberRoleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Employee) {
            binding.model = item
            binding.executePendingBindings()


        }

        init {
            binding.ivCheck.setOnClickListener {
                filterList[bindingAdapterPosition].isChecked = !filterList[bindingAdapterPosition].isChecked

                if (filterList[bindingAdapterPosition].isChecked) {
                    selectedItemList.add(filterList[bindingAdapterPosition])
                } else {
                    selectedItemList.remove(filterList[bindingAdapterPosition])
                }
                notifyDataSetChanged()
            }
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AssignRoleTeamMemberListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewAssignTeamMemberRoleBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AssignRoleTeamMemberListAdapter.MyViewHolder,
        position: Int
    ) {
        holder.bind(filterList[position])
        Log.d("selectedItemList", "::$selectedItemList")

    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    /*private fun isAllItemsChecked(): Boolean {
            for (selectItem in itemsList) {
                if (!selectItem.isChecked) {
                    return false
                }
            }
            return true
        }*/

    /*fun selectAll(isChecked: Boolean) {
        selectedItemList.clear()
        if (isChecked) {
            for (selectItem in filterList) {
                selectItem.isChecked = true
                selectedItemList.add(selectItem)
            }
        } else {
            for (selectItem in filterList) {
                selectItem.isChecked = false
                selectedItemList.remove(selectItem)
            }
        }
        notifyDataSetChanged()
    }*/

     fun selectedItemList(): ArrayList<Employee> {
         return selectedItemList
     }

    fun selectedItemFromEdit(itemIds: List<Employee>) {
        selectedItemList.clear()
        filterList.forEach { employee ->
            itemIds.forEach {
                if (employee.id == it.id) {
                    employee.isChecked = true
                    selectedItemList.add(employee)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun add(employeeList: List<Employee>) {
        this.itemsList = employeeList as ArrayList<Employee>
        this.filterList = employeeList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Employee {
        return filterList[position]
    }


    /*fun remove(deleteObj: Employee?, deletePos: Int) {
        filterList.remove(deleteObj)
        notifyItemRemoved(deletePos)

    }*/

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    itemsList
                } else {
                    val fList = ArrayList<Employee>()

                    itemsList.filter {
                        it.name?.lowercase(Locale.getDefault())?.contains(charSequence) == true
                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    filterList = results.values as ArrayList<Employee>
                }

                notifyDataSetChanged()

            }
        }
    }

    fun getAll(): ArrayList<Employee> {
        return filterList
    }
}
