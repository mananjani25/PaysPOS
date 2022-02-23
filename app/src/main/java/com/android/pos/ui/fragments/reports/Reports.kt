package com.android.pos.ui.fragments.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentReportsBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.report.ReportSummaryFragment
import com.android.pos.di.RolePermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Reports : Fragment() {

    private lateinit var binding: FragmentReportsBinding

    @Inject
    lateinit var rolePermission: RolePermission

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()

        selectedPosition(0)
        val frag: Fragment = CurrentDrawer()
        loadFragment(frag)

        onClick()

    }


    private fun onClick() {
        binding.txtCurrentDrawer.setOnClickListener {
            selectedPosition(0)
            val frag: Fragment = CurrentDrawer()
            loadFragment(frag)
        }
        binding.txtSales.setOnClickListener {
            if (rolePermission.hasReportSummaryPermission(binding.root)) {
                selectedPosition(1)
                val frag: Fragment = ReportSummaryFragment()
                loadFragment(frag)
            }
        }
        binding.txtShiftReport.setOnClickListener {
            if (rolePermission.hasReportSummaryPermission(binding.root)) {
                selectedPosition(2)
                val frag: Fragment = ReportSummaryFragment()
                loadFragment(frag)
            }

        }
    }

    private fun configureToolbar() {
        binding.commonToolbar.txtTitle.setText("Reports")
        binding.commonToolbar.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }

        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_reports_to_dashboardCategory)
        }
    }

    fun selectedPosition(position: Int) {
        when (position) {
            0 -> {
                binding.commonToolbar.txtSubTitle.setText("Current Drawer")
                binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.btnColor))

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))
            }
            1 -> {

                binding.commonToolbar.txtSubTitle.setText("Sales Report")
                binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.btnColor))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

            }
            2 -> {

                binding.commonToolbar.txtSubTitle.setText("Shift Report")
                binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.btnColor))


            }

        }
    }

    fun loadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()
    }
}