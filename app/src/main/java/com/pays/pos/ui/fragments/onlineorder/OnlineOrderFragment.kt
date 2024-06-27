package com.pays.pos.ui.fragments.onlineorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.InventoryItemModel
import com.pays.pos.data.remote.ApiService
import com.pays.pos.databinding.FragmentOnlineOrderBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.InventoryAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.TAG
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnlineOrderFragment : Fragment() {

    private lateinit var binding: FragmentOnlineOrderBinding

    @Inject
    lateinit var rolePermission: RolePermission
    var startDate: String? = null
    var endDate: String? = null
    private var mPos: Int = 0
    private val viewModel by viewModels<OnlineDetailViewModel>()
    private var ongoingOrderCount: Int? = 0
    private var pendingOrdersCount: Int? = 0
    private var cancelledOrdersCount: Int? = 0
    private var completedOrdersCount: Int? = 0
    private var upcomingOrderCount: Int? = 0

    @set:Inject
    internal var prefProvider: PrefProvider? = null
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()

    private lateinit var presentation: CustomDisplay
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOnlineOrderBinding.inflate(inflater, container, false)
        requireContext().registerReceiver(broadcastReceiver, IntentFilter("onlineOrder"));
        configureToolbar()
        getOrderCountsObserver(startDate, endDate)

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

    @Inject
    lateinit var apiService: ApiService

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val isCount = intent?.getBooleanExtra("isCount", false)
            startDate = intent?.getStringExtra("start_date")
            endDate = intent?.getStringExtra("end_date")

            getOrderCountsObserver(startDate, endDate)

            if (isCount == true) {

                val count = intent.getIntExtra("count", 0)
                val orderType = intent.getStringExtra("param1")

                when (orderType) {

                    "0" -> {
                        pendingOrdersCount = count
                        setAdapter(0)
                    }
                    "1" -> {
                        ongoingOrderCount = count
                        setAdapter(1)
                    }
                    "2" -> {
                        completedOrdersCount = count
                        setAdapter(2)
                    }
                    "3" -> {
                        cancelledOrdersCount = count
                        setAdapter(3)
                    }
                    "4" -> {
                        upcomingOrderCount = count
                        setAdapter(4)
                    }
                }

                LogUtil.logE("broadcastReceiver", count.toString())
            } else {
                val position = intent?.getIntExtra("position", 0)
                changePosition(position!!)
                setAdapter(position)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changePosition(0)
        setAdapter(mPos)
    }

    private fun configureToolbar() {
        binding.commonToolbar?.imgDrawer.setOnSingleClickListener {
            findNavController().navigate(R.id.action_onlineOrder_to_menuposbold)
        }
        binding.commonToolbar?.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_onlineOrder_to_dashboarCategorynew)
        }

        binding.commonToolbar.txtTitle?.text = "Online Orders"
        binding.commonToolbar?.imgOptionMenu?.visibility = View.GONE
        binding.commonToolbar?.txtSubTitle?.text = "Pending Orders"
        binding.commonToolbar?.imgOptionMenuContainer?.visibility = View.GONE
    }

    private fun getOrderCountsObserver(startDate: String?, endDate: String?) {
        try {
            viewModel.onLineorderCounts(startDate, endDate).observe(viewLifecycleOwner) {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {

                            pendingOrdersCount = it.data?.data?.online_pending_orders
                            ongoingOrderCount = it.data?.data?.online_in_progress_orders
                            completedOrdersCount = it.data?.data?.online_complete_orders
                            cancelledOrdersCount = it.data?.data?.online_rejected_orders
                            upcomingOrderCount = it.data?.data?.upcoming_orders

                            setAdapter(mPos)

                        }
                        Status.ERROR -> {
                            setAdapter(mPos)
                        }
                        Status.LOADING -> {
                            setAdapter(mPos)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun changePosition(position: Int) {
        mPos = position
        when (position) {

            0 -> {
                val activeOrders = OnlineDetailFragment("0", startDate, endDate)
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Pending Orders"
            }
            1 -> {
                val activeOrders = OnlineDetailFragment("1", startDate, endDate)
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "InProgress Orders"
            }
            2 -> {
                val modifier: Fragment = OnlineDetailFragment("2", startDate, endDate)
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Completed Orders"
            }

            3 -> {
                val cancelled = OnlineDetailFragment("3", startDate, endDate)
                loadFragment(cancelled)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Rejected Orders"
            }
            4 -> {
                val activeOrders = OnlineDetailFragment("4", startDate, endDate)
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "UpComing Orders"
            }

        }


    }

    private fun loadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()

    }

    private fun setAdapter(pos: Int) {

        mPos = pos

        val list: ArrayList<InventoryItemModel> = arrayListOf()
        when (pos) {
            0 -> {
                list.add(InventoryItemModel(0, "Pending Orders ", pendingOrdersCount, true))
                list.add(InventoryItemModel(0, "InProgress Orders ", ongoingOrderCount))
                list.add(InventoryItemModel(0, "Completed Orders", completedOrdersCount))
                list.add(InventoryItemModel(0, "Rejected Orders ", cancelledOrdersCount))
                list.add(InventoryItemModel(0, "UpComing Orders ", upcomingOrderCount))
            }
            1 -> {
                list.add(InventoryItemModel(0, "Pending Orders ", pendingOrdersCount))
                list.add(InventoryItemModel(0, "InProgress Orders ", ongoingOrderCount, true))
                list.add(InventoryItemModel(0, "Completed Orders", completedOrdersCount))
                list.add(InventoryItemModel(0, "Rejected Orders ", cancelledOrdersCount))
                list.add(InventoryItemModel(0, "UpComing Orders ", upcomingOrderCount))

            }
            2 -> {
                list.add(InventoryItemModel(0, "Pending Orders ", pendingOrdersCount))
                list.add(InventoryItemModel(0, "InProgress Orders ", ongoingOrderCount))
                list.add(InventoryItemModel(0, "Completed Orders", completedOrdersCount, true))
                list.add(InventoryItemModel(0, "Rejected Orders ", cancelledOrdersCount))
                list.add(InventoryItemModel(0, "UpComing Orders ", upcomingOrderCount))

            }
            3 -> {
                list.add(InventoryItemModel(0, "Pending Orders ", pendingOrdersCount))
                list.add(InventoryItemModel(0, "InProgress Orders ", ongoingOrderCount))
                list.add(InventoryItemModel(0, "Completed Orders", completedOrdersCount))
                list.add(InventoryItemModel(0, "Rejected Orders ", cancelledOrdersCount, true))
                list.add(InventoryItemModel(0, "UpComing Orders ", upcomingOrderCount))

            }
            4 -> {
                list.add(InventoryItemModel(0, "Pending Orders ", pendingOrdersCount))
                list.add(InventoryItemModel(0, "InProgress Orders ", ongoingOrderCount))
                list.add(InventoryItemModel(0, "Completed Orders", completedOrdersCount))
                list.add(InventoryItemModel(0, "Rejected Orders ", cancelledOrdersCount))
                list.add(InventoryItemModel(0, "UpComing Orders ", upcomingOrderCount, true))
            }
        }
        binding.recyclerViewItemsList.adapter =
            InventoryAdapter(
                requireContext(),
                list,
                false,
                object : InventoryAdapter.InventoryListner {
                    override fun onItemSelect(position: Int) {
                        if (position == 3) {
                            if (rolePermission.hasCancelOrderPermission(binding.root)) {
                                changePosition(position)
                            }
                        } else {
                            changePosition(position)
                        }
                        LogUtil.logE(TAG, "position  $position")


                    }

                })


    }

}