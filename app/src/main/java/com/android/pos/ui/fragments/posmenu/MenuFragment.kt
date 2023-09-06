package com.android.pos.ui.fragments.posmenu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.BuildConfig
import com.android.pos.R
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.android.pos.databinding.FragmentMenuBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.getCustomerDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : DialogFragment() {
    private lateinit var binding: FragmentMenuBinding

    @Inject
    lateinit var rolePermission: RolePermission
    private val viewModel by viewModels<DashBoardCategoryViewModel>()

    private val dashBoardCategoryViewModel by activityViewModels<DashBoardCategoryViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpHeader()
        versionDisplay()

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel
            )
        }

        return binding.root
    }

    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()

    @Inject
    lateinit var apiService: ApiService

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    private fun setUpHeader() {
        binding.header.txtTitle.text = "Settings"
        binding.header.txtSave.text = getString(R.string.tv_home)
        binding.header.txtLogout?.visible()
        if (prefProvider.getValueboolean(IS_MASTER_TERMINAL, false)) {
            binding.linearPrinterQueue.visible()
        } else {
            binding.linearPrinterQueue.gone()
        }
    }

    private fun versionDisplay() {

        binding.txtVersion?.text =
            "Version : " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"
    }

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        observeShowProgress()
    }

    private fun observeShowProgress() {
        viewModel.logout.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {


                    dashBoardCategoryViewModel.cartModel = null
                    viewModel.destroyedList = arrayListOf()
                    viewModel.clearTable()
                    viewModel.deleteCart()
                    prefProvider.setClear()
                    prefProvider.setValue(Constants.AUTH_TOKEN, "")
                    prefProvider.setValue(Constants.BASE_URL_NEW, BASE_URL)


                    findNavController().navigate(R.id.action_global_login)


                }
            }
        }

        if (view != null) {
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

        viewModel.clockOut.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                AlertUtils.showCustomAlert(requireContext(), it)
                val bundle = Bundle()
                bundle.putBoolean("isDashboard", false)
                bundle.putBoolean("isSwap", false)
                if (findNavController().currentDestination?.id == R.id.menuFragment) {
                    findNavController().navigate(
                        R.id.action_menuFragment_to_passcode,
                        bundle
                    )
                }

            }
        }
    }


    private fun onClick() {
        binding.linearPrinterQueue.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_printerQueueList)
        }

        binding.header.txtSave.setOnClickListener {
            findNavController().navigateUp()
            manageCustomerDisplay()
        }
        binding.linearSettings.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settings)
        }
        binding.linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_hardware)
        }
        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
            manageCustomerDisplay()

        }
        binding.linearInventory.setOnClickListener {
            if (rolePermission.hasInventoryPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_inventory)
            }
        }

        binding.linearOrders.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_allOrders)
        }
        binding.linearTeam.setOnClickListener {
            if (rolePermission.hasEmployeePermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_teamList)
            }
        }
        binding.linearTransactions.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_transactionFragment)
            }
        }
        binding.linearCashLog.setOnClickListener {
            if (rolePermission.hasCashLogPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_cashLogFragment)
            }
        }
        binding.linearCustomers.setOnClickListener {
            if (rolePermission.hasCustomerPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_customer)
            }
        }
        binding.linearReports.setOnClickListener {
            if (rolePermission.hasReportSummaryPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_reports)
            }
        }
        binding.header.txtLogout?.setOnClickListener {
            alert("", "Are you sure you want to Logout?") {
                this.positiveButton("Logout") {

                    if (prefProvider.getValue(Constants.ORDER_TYPE, "").isNotEmpty()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.decreaseOnGoingOrderCounter(true)
                        }
                    } else {
                        viewModel.logoutAPI()
                    }

                }
                this.negativeButton("Cancel") {
                }

            }

            // closeDialog(dialog)
        }

        binding.llClockOut.setOnClickListener {
//            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
//                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)

            alert(
                getString(R.string.app_name),
                prefProvider.employeeName() + ", Are you sure, you want to clockout?"
            ) {
                positiveButton(getString(android.R.string.ok)) {
                    viewModel.clockOut()


                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
//            findNavController().navigate(R.id.action_menuFragment_to_reportEODFragment)
//            }
        }

    }

    private fun manageCustomerDisplay() {
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
            dashBoardCategoryViewModel.mAllWords(
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(requireActivity()) {
                it?.let {
                    presentation.updateCustomerDisplay(it)
                }
            }
        }
    }

    private fun closeDialog(dialog: Dialog?) {
        dialog?.dismiss()
    }


}