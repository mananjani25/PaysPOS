package com.pays.pos.ui.fragments.posmenu

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pays.pos.BuildConfig
import com.pays.pos.R
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.pays.pos.databinding.FragmentMenuBinding
import com.pays.pos.di.ApiModule.BASE_URL
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.disconnectSocket
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.workmanager.UploadWorker2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        binding.txtTerminal.text = "Terminal Login ID : "+prefProvider.getUniqueId()
    }

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        observeShowProgress()
    }

    @SuppressLint("RestrictedApi")
    private fun observeShowProgress() {
        viewModel.logout.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {

                    if (prefProvider.getValueboolean(IS_MASTER_TERMINAL, false) && prefProvider.getValueboolean(
                            Constants.IS_PRINTER_QUEUE_ENABLE,false)
                    ){
                        requireActivity().disconnectSocket()

                    }
/*
                    val data = Data.Builder()
                        //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                        // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                        .put("location_id", prefProvider?.getValueInt(Constants.LOCATION_ID, 0))
                        .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, ""))
                        .put(
                            Constants.IS_PRINTER_QUEUE_ENABLE, prefProvider?.getValueboolean(
                                Constants.IS_PRINTER_QUEUE_ENABLE, false
                            )
                        )
                        .put("is_cancel_work",true)
                        .build()

                    val uploadWorkRequest =
                        OneTimeWorkRequest.Builder(
                            UploadWorker2::class.java
                        ).addTag(Constants.PRINTER_QUEUE_BACKGROUND)
                            .setInputData(data)
                            .build()


                    val workManager = WorkManager.getInstance(requireContext())

                    try {

                        workManager.enqueueUniqueWork(
                            Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.REPLACE,
                            uploadWorkRequest
                        )

                    } catch (e: java.lang.Exception) {

                        e.printStackTrace()
                    }*/
                    dashBoardCategoryViewModel.cartModel = null
                    viewModel.destroyedList = arrayListOf()
                    // To refrain from disconnecting PAX after logout
                    val paxConnectionStatus = prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)

                    viewModel.clearTable()
                    viewModel.deleteCart()
                    prefProvider.setClear()
                    prefProvider.setValue(Constants.AUTH_TOKEN, "")
                    prefProvider.setValue(Constants.BASE_URL_NEW, BASE_URL)
                    prefProvider.setValueboolean(Constants.CHECK_QUEUE_CANCEL,true)
                    prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, paxConnectionStatus)

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

    fun clearManualCartItems(){
        CoroutineScope(Dispatchers.IO).launch {

            val data = viewModel.getManualSaleCartItemsList(
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            )

            if(data?.isNotEmpty() == true){
                data.forEach {
                    dashboardViewModel.deleteCartItem(it.cartItemId)
                }
                dashboardViewModel.deleteManualCartModel()

            }
        }
    }


    private fun onClick() {

        binding.txtTerminal.setOnClickListener {
            copy()
        }

        binding.linearPrinterQueue.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_printerQueueList)
            clearManualCartItems()
        }

        binding.header.txtSave.setOnClickListener {
            findNavController().navigateUp()
            manageCustomerDisplay()
        }
        binding.linearSettings.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settings)
            clearManualCartItems()
        }
        binding.linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_hardware)
            clearManualCartItems()
        }
        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
            manageCustomerDisplay()

        }
        binding.linearInventory.setOnClickListener {
            if (rolePermission.hasInventoryPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_inventory)
            }
            clearManualCartItems()
        }

        binding.linearOrders.setOnClickListener {
            dashBoardCategoryViewModel.deleteCart()
            findNavController().navigate(R.id.action_menuFragment_to_allOrders)
        }
        binding.linearTeam.setOnClickListener {
            if (rolePermission.hasEmployeePermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_teamList)
            }
            clearManualCartItems()
        }
        binding.linearTransactions.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_transactionFragment)
            }
            clearManualCartItems()
        }
        binding.linearCashLog.setOnClickListener {
            if (rolePermission.hasCashLogPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_cashLogFragment)
            }
            clearManualCartItems()
        }
        binding.linearCustomers.setOnClickListener {
            if (rolePermission.hasCustomerPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_customer)
            }
            clearManualCartItems()
        }
        binding.linearReports.setOnClickListener {
            if (rolePermission.hasReportSummaryPermission(binding.root)) {
                findNavController().navigate(R.id.action_menuFragment_to_reports)
            }
            clearManualCartItems()
        }
        binding.header.txtLogout?.setOnClickListener {
            alert("", "Are you sure you want to Logout?") {
                this.positiveButton("Logout") {

                    dashboardViewModel.apply {
                        clearCartModelBackup()
                        deleteCart()
                        currentCartItems = arrayListOf()
                        duplicateCurrentCartItem = arrayListOf()
                    }

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
            clearManualCartItems()
        }

    }

    private fun manageCustomerDisplay() {
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
            dashBoardCategoryViewModel.getAllCartItems(
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).asLiveData().observe(requireActivity()) {
                it?.let {
                    presentation.updateCustomerDisplay(it)
                }
            }
        }
    }

    private fun closeDialog(dialog: Dialog?) {
        dialog?.dismiss()
    }

    private fun copy() {

        println(binding.txtTerminal.text.toString().trim())

        val cm: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.text = prefProvider.getUniqueId()
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }


}