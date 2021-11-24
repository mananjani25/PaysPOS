package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.MergeFloorModel
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.data.model.MergeTableModel
import com.android.pos.data.model.responseModel.GetFloorPlanDetailResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.DialogMergeTableSelectionBinding
import com.android.pos.ui.adapter.MergeTableSelectionAdapter
import com.android.pos.ui.fragments.dinein.DineInViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MergeTableDialog : DialogFragment() {
    private lateinit var binding: DialogMergeTableSelectionBinding
    private val list: ArrayList<MergeTableListModel> = arrayListOf()
    private lateinit var adapter: MergeTableSelectionAdapter
    private var listFloorPlan: ArrayList<GetFloorPlanDetailResponse.Data>? = null
    private lateinit var tableAdapter: ArrayAdapter<MergeTableModel>
    private lateinit var floorAdapter: ArrayAdapter<MergeFloorModel>
    private val viewModel by viewModels<DineInViewModel>()
    private val TAG = "MergeTableDialog"
    private var tableSelectedPos: Int = 0

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
        observeMergeTable()
        observeShowProgress()
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    private fun observeMergeTable() {
        viewModel.mergeStatusChange.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->
                Log.e(TAG, "AnyStatus:  ${status}")
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), status.toString()
                ) { _, _ ->
                    val bundle = Bundle()
                    bundle.putBoolean("merge_done", true)
                    setFragmentResult("request_key_table_selection", bundle)
                    val navController = findNavController()
                    navController.popBackStack()
                }


            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MergeTableSelectionAdapter()

        setData()
        onClick()


    }

    private fun setData() {
        listFloorPlan = requireArguments().getParcelableArrayList("floorList")


        var listTable: ArrayList<MergeTableModel> = arrayListOf()
        var listFloor: ArrayList<MergeFloorModel> = arrayListOf()

        listFloorPlan?.forEach {
            listFloor.add(MergeFloorModel(it.id, it.name))

            it.floor_plan_tables.forEach { table ->
                listTable.add(MergeTableModel(table.id, table.table_name, it.id, it.name))
            }
        }
        list.add(MergeTableListModel(listTable, listFloor))

        binding.rvTableList.adapter = adapter
        adapter.setList(list)

        tableAdapter = ArrayAdapter(
            binding.root.context,
            R.layout.spinner_text_selected,
            arrayListOf()
        )
        floorAdapter = ArrayAdapter(
            binding.root.context,
            R.layout.spinner_text_selected,
            list.get(0).listFloorPlan
        )
        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnFloorName.adapter = floorAdapter
        binding.spnTableName.adapter = tableAdapter

        binding.spnTableName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tableSelectedPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.spnFloorName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

    }

    private fun onClick() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtSave.setOnClickListener {
            val parentTableId = tableAdapter.getItem(tableSelectedPos)?.id
            val childIds = adapter.getSelectedIds()
            parentTableId?.let { it1 -> viewModel.mergeTable(it1, childIds) }


        }
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

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })


    }
}