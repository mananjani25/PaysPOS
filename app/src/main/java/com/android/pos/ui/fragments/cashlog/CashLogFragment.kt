package com.android.pos.ui.fragments.cashlog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.databinding.FragmentCashLogBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CashLogAdapter
import com.android.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class CashLogFragment : Fragment() {

    private lateinit var binding: FragmentCashLogBinding
    private val viewModel by viewModels<CashLogViewModel>()
    private lateinit var adapter: CashLogAdapter
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCashLogBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startDatePickerObserver()
        endDatePickerObserver()
        setupAdapter()
        setupCalender()
        observeShowProgress()
        navigate()

        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)
        }
        binding.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
    }

    private fun setupCalender() {


        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            viewModel.apiCallTimeSheet()

        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
            viewModel.apiCallTimeSheet()

        }

        viewModel.setCurrentDate(myCalendar)

        viewModel.apiCallTimeSheet()


    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = CashLogAdapter()
        binding.rvOpenOrder.adapter = adapter
    }


    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), startDate, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                ).show()
            }

        })
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), endDate, myCalendar1
                        .get(Calendar.YEAR), myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                ).show()
            }
        })
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

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                adapter.add(it.data.cashes)
            }
        })

    }
}