package com.pays.pos.ui.fragments.cashlog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.CashLogResponse
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentCashLogBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.CashLogAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.PaginationScrollListener
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class CashLogFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var cashLogResponse: CashLogResponse.Data
    private lateinit var binding: FragmentCashLogBinding
    private val viewModel by viewModels<CashLogViewModel>()
    private lateinit var adapter: CashLogAdapter
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener

    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private lateinit var terminalListGlobal: ArrayList<VenueDetailsResponse.Data.Terminal>

    private var TOTAL_PAGES = 0
    var PAGE_START = 1
    private var isLoading = false
    private var currentPage = PAGE_START
    private var isLastPage = false


    @Inject
    lateinit var prefProvider: PrefProvider
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCashLogBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        viewModel.setCurrentDate(myCalendar)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setNumerFormat()
        setupData()
        startDatePickerObserver()
        endDatePickerObserver()
        setupAdapter()
        setupCalender()
        observeShowProgress()
        loadTerminals()


        binding.txtHome.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_cashLogFragment_to_dashboardCategoryNew)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.imgDrawer.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_cashlogfragment_to_menuFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupData() {

        binding.spTerminals.onItemSelectedListener = this
    }

    private fun setupCalender() {
        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                currentPage = 1
                adapter.clear()
                viewModel.apiCallTimeSheet(
                    getTerminalId(binding.spTerminals.selectedItemPosition).toString(),
                    currentPage
                )
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }
        }

        endTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")
            if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30) {
                currentPage = 1
                adapter.clear()
                viewModel.apiCallTimeSheet(
                    getTerminalId(binding.spTerminals.selectedItemPosition).toString(),
                    currentPage
                )
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }

        }


        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                startTime,
                myCalendar2.get(Calendar.HOUR),
                myCalendar2.get(Calendar.MINUTE),
                false
            ).show()
        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                endTime,
                myCalendar3.get(Calendar.HOUR),
                myCalendar3.get(Calendar.MINUTE),
                false
            ).show()


        }

        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvOpenOrder.layoutManager = layoutManager

        binding.rvOpenOrder.addOnScrollListener(object :
            PaginationScrollListener(layoutManager) {


            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun getTotalPageCount(): Int {
                return 0
            }


            override fun loadMoreItems() {
                currentPage += 1
                if (currentPage <= TOTAL_PAGES) {
                    isLoading = true
                    viewModel.apiCallTimeSheet(
                        getTerminalId(binding.spTerminals.selectedItemPosition).toString(),
                        currentPage
                    )

                } else {
                    adapter.showLoading(false)
                }

            }

        })


        // viewModel.apiCallTimeSheet(getTerminalId(binding.spTerminals.selectedItemPosition).toString())


    }

    fun timeCalculateForStartEndTime(hour: Int, minute: Int, isStart: String): String {
        var timestring = ""
        var hoursfinal: Int = 0
        if ((hour == 12 && minute > 0) || (hour > 12 && minute > 0) || (hour > 12 && minute == 0)) {
            if (hour == 12) {
                hoursfinal = hour
            } else {
                hoursfinal = hour - 12
            }
            if (hoursfinal < 10) {
                if (minute < 10) {
                    timestring = "0$hoursfinal:0$minute PM"
                } else {
                    timestring = "0$hoursfinal:$minute PM"
                }
            } else {
                if (minute < 10) {
                    timestring = "$hoursfinal:0$minute PM"
                } else {
                    timestring = "$hoursfinal:$minute PM"
                }
            }
        } else {
            if (hour == 0) {
                if (minute < 10) {
                    timestring = "${hour.plus(12)}:0$minute AM"
                } else {
                    timestring = "${hour.plus(12)}:$minute AM"
                }
            } else {
                if (hour < 10) {
                    if (minute < 10) {
                        timestring = "0$hour:0$minute AM"
                    } else {
                        timestring = "0$hour:$minute AM"
                    }
                } else {
                    if (minute < 10) {
                        timestring = "$hour:0$minute AM"
                    } else {
                        timestring = "$hour:$minute AM"
                    }
                }
            }

        }

        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        var startDatestring = ""
        if (isStart == "isstart") {
            startDatestring = sdf.format(myCalendar.time)
        } else {
            startDatestring = sdf.format(myCalendar1.time)
        }
        return "$startDatestring $timestring"
    }

    private fun differnceTrue(date1: String, date2: String?): Long {
        var dateType1: Date
        var dateType2: Date
        var daydifference = "0".toLong()
//        11/30/2021 09:40 AM
        try {
            var dates = SimpleDateFormat("MM/dd/yyyy")
            dateType1 = dates.parse(date1.substringBefore(" "))
            dateType2 = dates.parse(date2?.substringBefore(" "))
            var differencedate = abs(dateType1.time - dateType2.time)
            daydifference = differencedate / (24 * 60 * 60 * 1000)
            Log.d("yash", "differnceTrue: " + daydifference)
            return daydifference
        } catch (e: Exception) {
        }
        return daydifference
    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = CashLogAdapter(context, prefProvider)
        binding.rvOpenOrder.adapter = adapter
    }


    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.data.cashes.isNotEmpty()) {
                    binding.txtNodata.visibility = View.GONE
                    binding.rvOpenOrder.visibility = View.VISIBLE

                    TOTAL_PAGES = it.data.pagination.maxPageSize.toInt()
                    adapter.showLoading(false)

                    cashLogResponse = it.data
                    binding.cashLogModel = cashLogResponse



                    adapter.add(cashLogResponse.cashes)

                    isLoading = false
                    if (currentPage != TOTAL_PAGES) {

                        adapter.showLoading(true)
                    }

                } else {


                    if (adapter.itemCount == 0) {
                        binding.rvOpenOrder.visibility = View.GONE
                        binding.txtNodata.visibility = View.VISIBLE
                        cashLogResponse = it.data
                        binding.cashLogModel = cashLogResponse
                        binding.txtNodata.text = it.message

                    } else {
                        binding.txtNodata.visibility = View.GONE
                        binding.rvOpenOrder.visibility = View.VISIBLE

                        TOTAL_PAGES = it.data.pagination.maxPageSize.toInt()
                        adapter.showLoading(false)

                        cashLogResponse = it.data
                        binding.cashLogModel = cashLogResponse

                        adapter.add(cashLogResponse.cashes)

                        isLoading = false
                        if (currentPage != TOTAL_PAGES) {

                            adapter.showLoading(true)
                        }

                    }
                }

            }
        }

    }

    private fun loadTerminals() {

        viewModel.getTerminalListDatabse.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { terminalList ->
                            terminalListGlobal =
                                terminalList as ArrayList<VenueDetailsResponse.Data.Terminal>

                            val isPresent = terminalListGlobal.any { it.name == "All Terminals" }

                            if (!isPresent) {
                                //    terminalListGlobal.removeAt(0)
                                terminalListGlobal.add(
                                    0,
                                    VenueDetailsResponse.Data.Terminal(
                                        "",
                                        -1,
                                        -1,
                                        false,
                                        "All Terminals",
                                        "",
                                        ""
                                    )
                                )
                            }
                            val roleName = terminalListGlobal.map { it.name }
                            setUpTerminalSpinnerAdapter(roleName as ArrayList<String>)

                            navigate()
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    private fun getTerminalId(position: Int): Int {
        if (this::terminalListGlobal.isInitialized) {
            return terminalListGlobal.get(position).id
        } else {
            return -1
        }
    }

    private fun setUpTerminalSpinnerAdapter(terminalList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spTerminals.adapter = spinnerAdapter



        terminalListGlobal.forEachIndexed { index, item ->
            if (item.id == prefProvider.getValueInt(Constants.TERMINAL_ID, 0)) {
                LogUtil.logE(
                    "TerminalId",
                    prefProvider.getValueInt(Constants.TERMINAL_ID, 0).toString()
                )
                LogUtil.logE("TerminalId name", item.name)
                binding.spTerminals.setSelection(index)
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        adapter.clearList()
        viewModel.apiCallTimeSheet(
            getTerminalId(binding.spTerminals.selectedItemPosition).toString(),
            currentPage
        )

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}