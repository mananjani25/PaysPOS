package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.GetEmployeeTimeSheetDetailsResponse
import com.pays.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.pays.pos.databinding.ViewSingleMemberItemTimesheetBinding

class SingleTeamMemberTimeSheetAdapter :
    RecyclerView.Adapter<SingleTeamMemberTimeSheetAdapter.MyViewHolder>() {

    var employeeTimeSheet = ArrayList<GetEmployeeTimeSheetDetailsResponse.Data>()
    var employeeID : Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSingleMemberItemTimesheetBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemBinding = holder.discountItemBinding
        itemBinding.itemSheetModel = employeeTimeSheet[position]
        //  itemBinding.viewModel = viewModel
        itemBinding.tvEmployeeId?.text = "# $employeeID"
        itemBinding.tvclockIntime.text =
            employeeTimeSheet[position].date + "\n" + employeeTimeSheet[position].clockInTime
        itemBinding.tvclockOuttime.text =
            employeeTimeSheet[position].clockOutDate + "\n" + employeeTimeSheet[position].clockOutTime

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = employeeTimeSheet.size

    inner class MyViewHolder(val discountItemBinding: ViewSingleMemberItemTimesheetBinding) :
        RecyclerView.ViewHolder(discountItemBinding.root) {

        /*init {
            discountItemBinding.imgCheckBox.setOnClickListener {
                serviceChargeList[layoutPosition].isChecked = !serviceChargeList[layoutPosition].isChecked
                notifyDataSetChanged()
            }
        }*/
    }

    fun teamTimesheetDetailsList(employeeTimeSheet: List<GetEmployeeTimeSheetDetailsResponse.Data>, employeeID : Int) {
        this.employeeID = employeeID
        this.employeeTimeSheet.apply {
            clear()
            addAll(employeeTimeSheet)
            notifyDataSetChanged()
        }
    }


}