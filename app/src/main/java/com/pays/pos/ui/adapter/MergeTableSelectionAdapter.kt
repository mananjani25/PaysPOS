package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.MergeFloorModel
import com.pays.pos.data.model.MergeTableListModel
import com.pays.pos.data.model.MergeTableModel
import com.pays.pos.databinding.ViewMergeTableListBinding
import com.pays.pos.utils.LogUtil
import com.google.gson.Gson

class MergeTableSelectionAdapter : RecyclerView.Adapter<MergeTableSelectionAdapter.MyViewHolder>() {
    private var list: ArrayList<MergeTableListModel> = arrayListOf()
    private lateinit var tableAdapter: ArrayAdapter<MergeTableModel>
    private lateinit var floorAdapter: ArrayAdapter<MergeFloorModel>
    private val TAG = "MergeTableSelectionAda"

    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: ArrayList<MergeTableListModel>) {
        this.list = list
        notifyDataSetChanged()

    }


    inner class MyViewHolder(private val binding: ViewMergeTableListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: MergeTableListModel) {

            tableAdapter = ArrayAdapter(
                binding.root.context,
                R.layout.spinner_text_selected,
                arrayListOf()
            )
            floorAdapter = ArrayAdapter(
                binding.root.context,
                R.layout.spinner_text_selected,
                model.listFloorPlan
            )

            binding.spnTable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    binding.spnTable.setSelection(position)
                    list.get(bindingAdapterPosition).selectedTableId =
                        (parent?.adapter?.getItem(position) as MergeTableModel).id
                    list[bindingAdapterPosition].orderDetails =
                        (parent?.adapter?.getItem(position) as MergeTableModel).orderDetails
                    LogUtil.logE(
                        TAG,
                        "selectedTableParetnID ${(parent?.adapter?.getItem(position) as MergeTableModel).id}"
                    )
                    LogUtil.logE(
                        TAG,
                        "selectedORderID  ${(binding.spnTable.adapter.getItem(position) as MergeTableModel).orderId}"
                    )
                    LogUtil.logE(TAG, "selectedTablePosition  ${position}")
                    Log.e(TAG,"getTableName  ${(binding.spnTable.adapter.getItem(position) as MergeTableModel).name}")
                    list.get(bindingAdapterPosition).tableSelectedPosition = position
                    list.get(bindingAdapterPosition).tableChairCount =
                        (binding.spnTable.adapter.getItem(position) as MergeTableModel).chairCount

                    list.get(bindingAdapterPosition).orderId =
                        (binding.spnTable.adapter.getItem(position) as MergeTableModel).orderId
                    list.get(bindingAdapterPosition).secondaryChairCount = (binding.spnTable.adapter.getItem(position) as MergeTableModel).chairCount

                    //list[layoutPosition].table =  (binding.spnTable.adapter.getItem(position) as MergeTableModel)

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }


            }


            binding.spnFloorName.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        var tempTableList: ArrayList<MergeTableModel> = arrayListOf()
                        list.get(0).listFloorPlan.get(position).id
                        tempTableList = list.get(bindingAdapterPosition).listTable.filter { it ->
                            it.floorId == list.get(bindingAdapterPosition).listFloorPlan.get(position).id
                        }.toCollection(arrayListOf())
                        list[bindingAdapterPosition].selectedFloorPlanId =
                            list.get(0).listFloorPlan.get(position).id


                        var sortedlist = tempTableList.toList().sortedBy { it.id }
                        tempTableList = ArrayList(sortedlist)
                        Log.e(TAG,"tempTableList  ${Gson().toJson(tempTableList)}")
                        tableAdapter = ArrayAdapter(
                            binding.root.context,
                            R.layout.spinner_text_selected,
                            tempTableList
                        )
                        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spnTable.adapter = tableAdapter

                        /* tableAdapter.clear()
                         tableAdapter.addAll(tempTableList)
                         tableAdapter.notifyDataSetChanged()
                         binding.spnTable.isSelected = true
                         binding.spnTable.setSelection(0)
 */


                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                }

            tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


            binding.spnFloorName.adapter = floorAdapter
            binding.spnTable.adapter = tableAdapter





            if (layoutPosition != 0) {
                binding.imgDelete.visibility = View.VISIBLE
            } else {
                binding.imgDelete.visibility = View.GONE
            }

            binding.executePendingBindings()


        }

        init {

            binding.imgDelete.setOnClickListener {
                if (layoutPosition != 0) {
                    list.removeAt(layoutPosition)
                    notifyItemRemoved(layoutPosition)
                    notifyItemChanged(layoutPosition, list.size)


                }
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MergeTableSelectionAdapter.MyViewHolder {
        val binding =
            ViewMergeTableListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MergeTableSelectionAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size

    }

    fun addItem(model: MergeTableListModel) {
        list.add(model)
        notifyItemInserted(list.size-1)

    }

    fun getList(): ArrayList<MergeTableListModel> {
        return list
    }

    fun getSelectedIds(): String {

        var ids = ""
        for (i in 0 until list.size) {
            ids = list.get(i).selectedTableId.toString()

        }
        LogUtil.logE(TAG, "selectedIDs  ${ids}")


        return ids
    }
}