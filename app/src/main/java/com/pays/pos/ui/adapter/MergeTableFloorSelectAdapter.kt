package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.MergeFloorModel
import com.pays.pos.data.model.MergeTableListModel
import com.pays.pos.data.model.MergeTableModel
import com.pays.pos.databinding.ViewMergeTableListBinding


class MergeTableFloorSelectAdapter :
    RecyclerView.Adapter<MergeTableFloorSelectAdapter.ViewHolder>() {

    private var floorPlanID: ArrayList<Int> = arrayListOf()
    private var floorplanHashMap: HashMap<MergeFloorModel, ArrayList<MergeTableModel>> = hashMapOf()
    private var floorplanSecondaryId: Int = 0
    private var tableId: Int = 0
    private var selectedFloorTable: ArrayList<MergeTableListModel> =
        arrayListOf<MergeTableListModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun setList(
        list: ArrayList<Int>,
        floorplanHashMap: HashMap<MergeFloorModel, ArrayList<MergeTableModel>>
    ) {
        this.floorPlanID = list
        this.floorplanHashMap = floorplanHashMap
        notifyDataSetChanged()
    }

    fun addFloorPlanTableId(floorplanId: Int, tableId: Int) {
        selectedFloorTable.add(
            MergeTableListModel(
                arrayListOf(),
                arrayListOf(),
                tableId,
                null,
                floorplanId,
                null,
                null,
                null
            )
        )
    }

    fun getFloorPlanTableIDs(): ArrayList<MergeTableListModel> {
        return selectedFloorTable
    }

    inner class ViewHolder(val binding: ViewMergeTableListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            if (absoluteAdapterPosition != 0) {
                binding.imgDelete.visibility = View.VISIBLE
            } else {
                binding.imgDelete.visibility = View.GONE
            }


        }

        init {
            var floorplandefault: ArrayList<String> = arrayListOf()
            var tabledefault: ArrayList<String> = arrayListOf()

            floorplanHashMap.forEach {
                floorplandefault.add(it.key.name)
            }
            binding.txtFloorPlanLabel.setOnClickListener {
                if (absoluteAdapterPosition >= 0 && absoluteAdapterPosition < selectedFloorTable.size) {
                    selectedFloorTable.removeAt(absoluteAdapterPosition)
                }
               // binding.txtselectprimarytable.text = "Select Table to Merge"
                val textView = TextView(binding.root.context)
                textView.text = "Select Floor Plan"
                textView.setPadding(20, 20, 20, 20)
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                textView.textSize = 20f
                textView.setBackgroundColor(binding.root.context.resources.getColor(R.color.btnColor))
                textView.setTextColor(Color.WHITE)
                val builder = AlertDialog.Builder(binding.root.context, R.style.CustomDialogTheme)
                builder.setCustomTitle(textView)
                builder.setItems(
                    floorplandefault.toArray(arrayOfNulls<String>(floorplandefault.size))
                ) { dialog, which ->

                    binding.txtFloorPlanLabel.text = floorplandefault[which]
                    tabledefault = arrayListOf()
                    floorplanHashMap.forEach {
                        if (it.key.name == floorplandefault[which]) {
                            floorplanSecondaryId = it.key.id
                            floorplanHashMap[it.key]?.forEach {
                                tabledefault.add(it.name)
                            }
                        }
                    }

                    dialog.dismiss()
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()

            }
           /* binding.txtselectprimarytable.setOnClickListener {
                if (floorplanSecondaryId != 0) {
                    val textView = TextView(binding.root.context)
                    textView.text = "Select Table to Merge"
                    textView.setPadding(20, 20, 20, 20)
                    textView.setTypeface(Typeface.DEFAULT_BOLD);
                    textView.textSize = 20f
                    textView.setBackgroundColor(binding.root.context.resources.getColor(R.color.btnColor))
                    textView.setTextColor(Color.WHITE)
                    val builder =
                        AlertDialog.Builder(binding.root.context, R.style.CustomDialogTheme)
                    builder.setCustomTitle(textView)
                    builder.setItems(
                        tabledefault.toArray(arrayOfNulls<String>(tabledefault.size))
                    ) { dialog, which ->
                        dialog.dismiss()
                        floorplanHashMap.forEach {
                            if (it.key.id == floorplanSecondaryId) {
                                tableId = floorplanHashMap[it.key]?.get(which)?.id ?: 0
                                addFloorPlanTableId(floorplanSecondaryId, tableId)
                            }
                        }
                      //  binding.txtselectprimarytable.text = tabledefault[which]
                    }
                    val alertDialog: AlertDialog = builder.create()
                    alertDialog.show()
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        binding.root.context,
                        "Please Select Floor."
                    ) { _, _ ->

                    }
                }

            }*/

            binding.imgDelete.setOnClickListener {
                if (absoluteAdapterPosition != 0) {
                    floorPlanID.removeAt(absoluteAdapterPosition)
                    notifyItemRemoved(absoluteAdapterPosition)
                    notifyItemChanged(absoluteAdapterPosition, floorPlanID.size)
                }
            }


        }
    }


    fun checkTableIsAdded() {
        var floorplanSelected: ArrayList<Int> = arrayListOf()
        var tableSelected: ArrayList<Int> = arrayListOf()
        for (i in selectedFloorTable.indices) {
            selectedFloorTable[i].selectedFloorPlanId?.let { floorplanSelected.add(it) }
            selectedFloorTable[i].selectedTableId?.let { tableSelected.add(it) }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ViewMergeTableListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return floorPlanID.size
    }
}
