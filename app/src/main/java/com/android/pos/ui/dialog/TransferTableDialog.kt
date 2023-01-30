package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.android.pos.R
import com.android.pos.data.model.responseModel.AvailableStatu
import com.android.pos.data.model.responseModel.AvailableTransferTableList
import com.android.pos.data.model.responseModel.OccupiedTable
import com.android.pos.databinding.DialogTransferTableSelectionBinding
import com.google.gson.Gson

class TransferTableDialog : DialogFragment() {
    private lateinit var binding: DialogTransferTableSelectionBinding
    private var floorDetails: AvailableTransferTableList? = null
    private val TAG = "TransferTableDialog"
    private lateinit var floorOccupiedAdapter: ArrayAdapter<OccupiedTable>
    private lateinit var tableOccupiedAdapter: ArrayAdapter<OccupiedTable.TableList>
    private lateinit var floorAvailableAdapter: ArrayAdapter<AvailableStatu>
    private lateinit var tableAvailableAdapter: ArrayAdapter<AvailableStatu.TableList>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_transfer_table_selection,
            container,
            false
        )
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        setData()
    }

    private fun onClick() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }

        binding.txtSave
    }

    private fun setData() {
        floorDetails = requireArguments().getParcelable<AvailableTransferTableList>("floorList")
        Log.e(TAG, "floorDetails:  ${Gson().toJson(floorDetails)}")

        //This is for First Part for Ocuupied Floor and Table (as mentioned in header from table part in UI)
        var occupiedFloorList: ArrayList<OccupiedTable> = arrayListOf()
        var occupiedTableList: ArrayList<OccupiedTable.TableList> = arrayListOf()


        occupiedFloorList.add(
            OccupiedTable(
                name = "Please Select Floor Plan",
                id = 0,
                floor_plan_tables = arrayListOf()
            )
        )
        occupiedFloorList.addAll(floorDetails?.data?.occupied_tables ?: arrayListOf())
        floorOccupiedAdapter =
            ArrayAdapter(binding.root.context, R.layout.spinner_text_selected, occupiedFloorList)
        floorOccupiedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnFloorName.adapter = floorOccupiedAdapter
        binding.spnFloorName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (occupiedFloorList.get(position).floor_plan_tables.isNotEmpty()) {

                    tableOccupiedAdapter.clear()
                    tableOccupiedAdapter.addAll(occupiedFloorList.get(position).floor_plan_tables)
                    tableOccupiedAdapter.notifyDataSetChanged()
                } else {
                    tableOccupiedAdapter.clear()
                    tableOccupiedAdapter.add(
                        OccupiedTable.TableList(
                            id = 0,
                            tableName = "Please Select Table",
                            tableNumber = "",
                            status = "",
                            floorPlanId = 0
                        )
                    )
                    tableOccupiedAdapter.notifyDataSetChanged()

                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }



        tableOccupiedAdapter =
            ArrayAdapter(binding.root.context, R.layout.spinner_text_selected, arrayListOf())
        tableOccupiedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnTableName.adapter = tableOccupiedAdapter
        binding.spnTableName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {


            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }


        //This is for Second Part for Available Floor and Table (as mentioned in the second lable from table part in UI)

        var availableFloorList: ArrayList<AvailableStatu> = arrayListOf()
        var availableTableList: ArrayList<AvailableStatu.TableList> = arrayListOf()



        availableTableList.add(
            AvailableStatu.TableList(
                id = 0,
                tableName = "Please Select Table",
                tableNumber = "",
                status = "",
                floorPlanId = 0
            )
        )

        availableFloorList.add(
            AvailableStatu(
                name = "Please Select Floor Plan",
                id = 0,
                floor_plan_tables = arrayListOf()
            )
        )

        availableFloorList.addAll(floorDetails?.data?.available_status ?: arrayListOf())

        floorAvailableAdapter =
            ArrayAdapter(binding.root.context, R.layout.spinner_text_selected, availableFloorList)
        floorAvailableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnFloorName1.adapter = floorAvailableAdapter
        binding.spnFloorName1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (availableFloorList.get(position).floor_plan_tables.isNotEmpty()) {
                    tableAvailableAdapter.clear()
                    tableAvailableAdapter.addAll(availableFloorList.get(position).floor_plan_tables)
                    tableAvailableAdapter.notifyDataSetChanged()

                } else {


                    tableAvailableAdapter.clear()
                    tableAvailableAdapter.add(
                        AvailableStatu.TableList(
                            id = 0,
                            tableName = "Please Select Table",
                            tableNumber = "",
                            status = "",
                            floorPlanId = 0
                        )
                    )
                    tableAvailableAdapter.notifyDataSetChanged()


                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        tableAvailableAdapter =
            ArrayAdapter(binding.root.context, R.layout.spinner_text_selected, arrayListOf())
        tableAvailableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnTableName1.adapter = tableAvailableAdapter
        binding.spnTableName1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

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

}