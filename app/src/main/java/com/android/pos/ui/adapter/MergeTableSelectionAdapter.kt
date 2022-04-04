package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.MergeFloorModel
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.data.model.MergeTableModel
import com.android.pos.databinding.ViewMergeTableListBinding

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
                    Log.e(
                        TAG,
                        "selectedTableParetnID ${(parent?.adapter?.getItem(position) as MergeTableModel).id}"
                    )
                    Log.e(
                        TAG,
                        "selectedORderID  ${(binding.spnTable.adapter.getItem(position) as MergeTableModel).orderId}"
                    )
                    Log.e(TAG, "selectedTablePosition  ${position}")
                    list.get(bindingAdapterPosition).tableSelectedPosition = position
                    list.get(bindingAdapterPosition).tableChairCount =
                        (binding.spnTable.adapter.getItem(position) as MergeTableModel).chairCount

                    list.get(bindingAdapterPosition).orderId =
                        (binding.spnTable.adapter.getItem(position) as MergeTableModel).orderId


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
                        tempTableList = list.get(layoutPosition).listTable.filter { it ->
                            it.floorId == list.get(layoutPosition).listFloorPlan.get(position).id
                        }.toCollection(arrayListOf())
                        list[layoutPosition].selectedFloorPlanId =
                            list.get(0).listFloorPlan.get(position).id



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
        notifyDataSetChanged()

    }

    fun getList(): ArrayList<MergeTableListModel> {
        return list
    }

    fun getSelectedIds(): String {

        var ids = ""
        for (i in 0 until list.size) {
            ids = list.get(i).selectedTableId.toString()

        }
        Log.e(TAG, "selectedIDs  ${ids}")


        return ids
    }
}