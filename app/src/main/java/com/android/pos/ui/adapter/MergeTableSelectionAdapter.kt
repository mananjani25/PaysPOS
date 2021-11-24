package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.MergeFloorModel
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.data.model.MergeTableModel
import com.android.pos.databinding.ViewMergeTableListBinding
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

            tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


            binding.spnFloorName.adapter = floorAdapter
            binding.spnTable.adapter = tableAdapter

            binding.spnTable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    list.get(layoutPosition).selectedTableId =
                        (binding.spnTable.adapter.getItem(position) as MergeTableModel).id

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
                        tempTableList = list.get(0).listTable.filter { it ->
                            it.floorId == list.get(0).listFloorPlan.get(position).id
                        }.toCollection(arrayListOf())

                        tableAdapter.clear()
                        tableAdapter.addAll(tempTableList)
                        tableAdapter.notifyDataSetChanged()

                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                }

            if (layoutPosition != 0) {
                binding.imgDelete.visibility = View.VISIBLE
            } else {
                binding.imgDelete.visibility = View.GONE
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

    fun getSelectedIds(): String {

        var ids = ""
        for (i in 0 until list.size) {
            ids = list.get(i).selectedTableId.toString()

        }
        Log.e(TAG, "selectedIDs  ${ids}")


        return ids
    }
}