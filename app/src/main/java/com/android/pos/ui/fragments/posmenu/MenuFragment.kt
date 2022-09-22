package com.android.pos.ui.fragments.posmenu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentMenuBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.alert
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : DialogFragment() {
    private lateinit var binding: FragmentMenuBinding

    @Inject
    lateinit var rolePermission: RolePermission
    private val viewModel by viewModels<DashBoardCategoryViewModel>()

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


        return binding.root
    }

    private fun setUpHeader() {
        binding.header.txtTitle.text = getString(R.string.menu)
        binding.header.txtSave.text = getString(R.string.tv_home)
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

                    viewModel.clearTableAll()
                    viewModel.clearTable()
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

    }


    private fun onClick() {
        binding.header.txtSave.setOnClickListener {
            findNavController().navigateUp()
            /*findNavController().navigateUp()*/
        }
        binding.linearSettings.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settings)
        }
        binding.linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_hardware)
        }
        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.linearInventory.setOnClickListener {
            if (rolePermission.hasInventoryPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_inventory)
            }
        }

        binding.linearOrders.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_orders)
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
        binding.linearLogout.setOnClickListener {
            alert("", "Are you sure you want to Logout?") {
                this.positiveButton("Logout") {
                    viewModel.logoutAPI()
                }
                this.negativeButton("Cancel") {
                }

            }

            // closeDialog(dialog)
        }


    }

    private fun closeDialog(dialog: Dialog?) {
        dialog?.dismiss()
    }


}