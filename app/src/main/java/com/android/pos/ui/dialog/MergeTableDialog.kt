package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.android.pos.R
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.databinding.DialogMergeTableSelectionBinding
import com.android.pos.ui.adapter.MergeTableSelectionAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MergeTableDialog : DialogFragment() {
    private lateinit var binding: DialogMergeTableSelectionBinding
    private val list: ArrayList<MergeTableListModel> = arrayListOf()
    private lateinit var adapter: MergeTableSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_merge_table_selection,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MergeTableSelectionAdapter()

        setAdapter()
        onClick()

    }

    private fun onClick() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }
    }

    private fun setAdapter() {
        var tableList: ArrayList<String> = arrayListOf()
        tableList.add("Select Primary Table")
        tableList.add("Table 1")
        tableList.add("Table 2")
        tableList.add("Table 3")

        var floorList: ArrayList<String> = arrayListOf()
        floorList.add("Select Floor Plan")
        floorList.add("Bar")
        floorList.add("Party Dining")
        floorList.add("Test")



        list.add(MergeTableListModel(tableList, floorList))

        binding.rvTableList.adapter = adapter
        adapter.setList(list)

    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.60).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

}