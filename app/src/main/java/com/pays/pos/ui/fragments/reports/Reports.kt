package com.pays.pos.ui.fragments.reports

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.FragmentReportsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.fragments.employeeTipSummary.EmployeeTipSummary
import com.pays.pos.ui.fragments.report.ReportEODFragment
import com.pays.pos.ui.fragments.report.ReportSummaryFragment
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class Reports : Fragment() {

    private lateinit var binding: FragmentReportsBinding

    @Inject
    lateinit var rolePermission: RolePermission

    @set:Inject
    internal var prefProvider: PrefProvider? = null

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

        selectedPosition(1)
        val frag: Fragment = ReportSummaryFragment()
        loadFragment(frag)

        onClick()

        setFragmentResultListener("request_key_eod") { _: String, bundle: Bundle ->


            bundle.getString("email")?.let {

                EventBus.getDefault().post(it)

            }
        }

    }


    private fun onClick() {
        /*   binding.txtCurrentDrawer.setOnClickListener {
               selectedPosition(0)
               val frag: Fragment = CurrentDrawer()
               loadFragment(frag)
           }*/
        binding.txtSales.setOnClickListener {
            if (rolePermission.hasReportSummaryPermission(binding.root)) {
                selectedPosition(1)
                val frag: Fragment = ReportSummaryFragment()
                loadFragment(frag)
            }
        }

        binding.txtEodReport.setOnClickListener {
            selectedPosition(2)
            val frag: Fragment = ReportEODFragment(showHeader = false)
            loadFragment(frag)

        }
        binding.txtEmployeeTipSummary.setOnClickListener {
            selectedPosition(4)
            val frag: Fragment = EmployeeTipSummary()
            loadFragment(frag)
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
        binding.commonToolbar.imgDrawer.setOnSingleClickListener {
            // (requireActivity() as MainActivity).enableDrawer()
            findNavController().navigate(R.id.action_reports_to_menuFragment)
        }

        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_reports_to_dashboardCategory)
        }

        binding.commonToolbar.txtEmail?.setOnClickListener {
            Log.e("ReportEODFragment","onclick txtEmail from parent fragment")
            EventBus.getDefault().post("1")
        }
        binding.commonToolbar.imgPrintEODReport?.setOnClickListener {
            EventBus.getDefault().post("2")
        }
    }

    fun selectedPosition(position: Int) {
        when (position) {
            0 -> {
                /*  binding.commonToolbar.txtSubTitle.setText("Current Drawer")
                  binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.white))
                  binding.txtCurrentDrawer.background=requireContext().resources.getDrawable(R.drawable.button_action_hover)

                  binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                  binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                  binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                  binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))*/
            }
            1 -> {

                binding.commonToolbar.txtSubTitle.setText("Sales Report")
                /*  binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                  binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))
  */
                binding.txtEodReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtEodReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtEmployeeTipSummary.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtEmployeeTipSummary.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtSales.background =
                    requireContext().resources.getDrawable(R.drawable.button_action_hover)

                binding.commonToolbar.txtEmail?.gone()
                binding.commonToolbar.imgPrintEODReport?.gone()
            }
            2 -> {

                binding.commonToolbar.txtEmail?.visible()
                binding.commonToolbar.imgPrintEODReport?.visible()
                binding.commonToolbar.txtSubTitle.text = getString(R.string.end_of_day_Report) + " ("+prefProvider?.employeeName()+")"
                /* binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                 binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))*/

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtEmployeeTipSummary.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtEmployeeTipSummary.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtEodReport.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtEodReport.background =
                    requireContext().resources.getDrawable(R.drawable.button_action_hover)

            }

            3 -> {

                binding.commonToolbar.txtSubTitle.setText("Shift Report")
                /* binding.txtCurrentDrawer.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                 binding.txtCurrentDrawer.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))*/

                binding.txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtEodReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtEodReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtEmployeeTipSummary.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                binding.txtEmployeeTipSummary.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                binding.txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.txtShiftReport.background =
                    requireContext().resources.getDrawable(R.drawable.button_action_hover)


            }
            4-> {

                binding.apply {

                    commonToolbar.txtSubTitle.text = requireContext().getString(R.string.employee_tip_summary)

                    txtSales.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                    txtSales.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                    txtEodReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                    txtEodReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                    txtShiftReport.setTextColor(requireContext().resources.getColor(R.color.txtColor))
                    txtShiftReport.setBackgroundColor(requireContext().resources.getColor(R.color.bg_color))

                    txtEmployeeTipSummary.setTextColor(requireContext().resources.getColor(R.color.white))
                    txtEmployeeTipSummary.background =
                        requireContext().resources.getDrawable(R.drawable.button_action_hover)

                    binding.commonToolbar.txtEmail?.visible()
                    binding.commonToolbar.imgPrintEODReport?.visible()

                }

            }

        }
    }

    fun loadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()
    }
}