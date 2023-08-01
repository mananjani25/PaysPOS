package com.android.pos.ui.fragments.employeeTipSummary

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.model.responseModel.employeeTipSummary.EmployeeTipSummaryResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMAIL
import com.android.pos.data.remote.Constants.END_DATE
import com.android.pos.data.remote.Constants.START_DATE
import com.android.pos.databinding.FragmentEmployeeTipSammaryBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.EmployeeTipSummaryAdapter
import com.android.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.android.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.EventObserver
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.PrintSunmiUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.addBuilderText
import com.android.pos.utils.addCustomerTextSize
import com.android.pos.utils.addHorizontalLine
import com.android.pos.utils.addItemsInEmployeeTipsSummary
import com.android.pos.utils.addItemsInEmployeeTipsSummaryM30
import com.android.pos.utils.addSixHeaderForEmployeeTipSummary
import com.android.pos.utils.addSixHeaderForEmployeeTipSummarySunmi
import com.android.pos.utils.employeeTipSummaryHeader
import com.android.pos.utils.extensions.differenceTrue
import com.android.pos.utils.extensions.timeCalculateForStartEndTime
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class EmployeeTipSummary : Fragment() {


    private var ETSdataList: ArrayList<EmployeeTipSummaryResponse.Data>? = null

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    private val TAG = "EmployeeTipSummary"

    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = listOf()

    private val viewModel by viewModels<EmployeeTipSummaryViewModel>()
    private lateinit var binding: FragmentEmployeeTipSammaryBinding
    private val employeeTipSummaryAdapter by lazy { EmployeeTipSummaryAdapter() }

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener

    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmployeeTipSammaryBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupCalender()
        initAdapter()
        setInitDate()
        loadData()
        initObserver()

    }

    private fun loadData() {

        viewModel.getEmployeeTipSummary()
    }

    private fun initObserver() {

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

        viewModel.data.observe(viewLifecycleOwner, EventObserver { data ->
            data.let {

                employeeTipSummaryAdapter.add(it.data)

            }

        })

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

    private fun setInitDate() {

        viewModel.setCurrentDate(myCalendar)

    }

    private fun initAdapter() {

        binding.apply {

            rvEmployeeTipSummary.adapter = employeeTipSummaryAdapter

        }
    }

    private fun setupCalender() {
        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = requireContext().timeCalculateForStartEndTime(hour, minute, "isstart",myCalendar,myCalendar1)
            if (requireContext().differenceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                // Call API here
                viewModel.getEmployeeTipSummary()
                Log.e("setupCalender","1 start date = ${viewModel.startDate.value}, End date = ${viewModel.endDate.value}")

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
            viewModel.endDate.value = requireContext().timeCalculateForStartEndTime(hour, minute, "isend",myCalendar,myCalendar1)
            if (requireContext().differenceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30) {
                // Call API here
                viewModel.getEmployeeTipSummary()
                Log.e("setupCalender","2 start date = ${viewModel.startDate.value}, End date = ${viewModel.endDate.value}")

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
                myCalendar2.get(2),
                myCalendar2.get(2),
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
                myCalendar1.get(2),
                myCalendar1.get(2),
                false
            ).show()

        }
    }

    private fun sendEmail() {

        if (viewModel.terminalId!=-1) {
            viewModel.getEmployeeEmail(viewModel.terminalId)
                .observe(viewLifecycleOwner) {

                    if (it.status == Status.SUCCESS) {
                        val bundle = Bundle()
                        bundle.putBoolean("EOD", true)
                        bundle.putBoolean("ETS", true)
                        bundle.putInt("type", 2)
                        bundle.putString(EMAIL, it.data?.email)
                        bundle.putString(START_DATE, viewModel.startDate.value)
                        bundle.putString(END_DATE, viewModel.endDate.value)
                        findNavController().navigate(
                            R.id.action_reports_to_sendReceiptFragment,
                            bundle
                        )
                    }
                }
        } else {
            val bundle = Bundle()
            bundle.putBoolean("EOD", true)
            bundle.putInt("type", 2)
            bundle.putString(EMAIL, "")
            bundle.putString(START_DATE, viewModel.startDate.value)
            bundle.putString(END_DATE, viewModel.endDate.value)
            findNavController().navigate(
                R.id.action_reports_to_sendReceiptFragment,
                bundle
            )

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String?) {
        // Do something
        if (event.equals("1")) {
            sendEmail()
        } else if (event.equals("2")) {
           // generateEODReport() // Print
        }else {
            if (event!=null){
                viewModel.getEmployeeTipSummary()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
    }

    override fun onStart() {
        super.onStart()
        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }
}