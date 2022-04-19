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
import com.android.pos.databinding.FragmentMenuBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.extensions.alert
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : DialogFragment() {
    private lateinit var binding: FragmentMenuBinding
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



        return binding.root
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
        viewModel.logout.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    prefProvider.setClear()
                    viewModel.clearAllTables()
                    findNavController().navigate(R.id.action_global_login)


                }
            }
        })

    }

    private fun onClick() {
        binding.txtHome.setOnClickListener {
            findNavController().navigateUp()
            /*findNavController().navigateUp()*/
        }
        binding.linearSettings.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settings)
        }
        binding.linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_hardware)
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.linearInventory.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_inventory)
        }

        binding.linearOrders.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_orders)
        }
        binding.linearTeam.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_teamList)
        }
        binding.linearTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_transactionFragment)
        }
        binding.linearCashLog.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_cashLogFragment)
        }
        binding.linearCustomers.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_customer)
        }
        binding.linearReports.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_reports)
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