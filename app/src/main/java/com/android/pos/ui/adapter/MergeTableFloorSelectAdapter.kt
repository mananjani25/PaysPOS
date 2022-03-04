package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.MergeFloorModel
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.data.model.MergeTableModel
import com.android.pos.databinding.ViewMergeTableListBinding


class MergeTableFloorSelectAdapter :
    RecyclerView.Adapter<MergeTableFloorSelectAdapter.ViewHolder>() {

    private var floorPlanID: ArrayList<Int> = arrayListOf()
    private var floorplanHashMap: HashMap<MergeFloorModel, ArrayList<MergeTableModel>> = hashMapOf()


    @SuppressLint("NotifyDataSetChanged")
    fun setList(
        list: ArrayList<Int>,
        floorplanHashMap: HashMap<MergeFloorModel, ArrayList<MergeTableModel>>
    ) {
        this.floorPlanID = list
        this.floorplanHashMap = floorplanHashMap
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ViewMergeTableListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            if (absoluteAdapterPosition != 0) {
                binding.imgDelete.visibility = View.VISIBLE
            } else {
                binding.imgDelete.visibility = View.GONE
            }
            var floorplandefault: ArrayList<String> = arrayListOf()
            var tabledefault: ArrayList<String> = arrayListOf()

            floorplanHashMap.forEach {
                floorplandefault.add(it.key.name)
            }

            binding.txtFloorPlanLabel.setOnClickListener {
                binding.txtselectprimarytable.text = "Select Table to Merge"
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
                            Log.d("yash", "setData: key " + it.key.name)
                            floorplanHashMap[it.key]?.forEach {
                                tabledefault.add(it.name)
                            }
                        }
                    }

                    dialog.dismiss()
                    Log.d("yash", "setData: " + floorplandefault[which])
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()

            }
            binding.txtselectprimarytable.setOnClickListener {
                val textView = TextView(binding.root.context)
                textView.text = "Select Table to Merge"
                textView.setPadding(20, 20, 20, 20)
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                textView.textSize = 20f
                textView.setBackgroundColor(binding.root.context.resources.getColor(R.color.btnColor))
                textView.setTextColor(Color.WHITE)
                val builder = AlertDialog.Builder(binding.root.context, R.style.CustomDialogTheme)
                builder.setCustomTitle(textView)
                builder.setItems(
                    tabledefault.toArray(arrayOfNulls<String>(tabledefault.size))
                ) { dialog, which ->
                    dialog.dismiss()
                    binding.txtselectprimarytable.text = tabledefault[which]
                    Log.d("yash", "setData: " + tabledefault[which])
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()

            }

        }

        init {
            binding.imgDelete.setOnClickListener {
                if (absoluteAdapterPosition != 0) {
                    floorPlanID.removeAt(absoluteAdapterPosition)
                    notifyItemRemoved(absoluteAdapterPosition)
                    notifyItemChanged(absoluteAdapterPosition, floorPlanID.size)
                }
            }


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
