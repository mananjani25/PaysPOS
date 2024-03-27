package com.pays.pos.ui.fragments.allorders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.model.InventoryItemModel
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants.ALL_ORDER_TAB
import com.pays.pos.data.remote.Constants.ALL_ORDER_TAB_POS
import com.pays.pos.data.remote.Constants.ONLINE_ORDER_TAB
import com.pays.pos.data.remote.Constants.ONLINE_ORDER_TAB_POS
import com.pays.pos.data.remote.Constants.OPEN_ORDER_TAB
import com.pays.pos.data.remote.Constants.OPEN_ORDER_TAB_POS
import com.pays.pos.data.remote.Constants.PHONE_ORDER_TAB
import com.pays.pos.data.remote.Constants.PHONE_ORDER_TAB_POS
import com.pays.pos.data.remote.Constants.THIRD_PARTY_ORDER_TAB
import com.pays.pos.data.remote.Constants.THIRD_PARTY_ORDER_TAB_POS
import com.pays.pos.databinding.FragmentAllOrdersCountsBinding
import com.pays.pos.databinding.FragmentOnlineOrderBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.InventoryAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.onlineorder.OnlineDetailViewModel
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.TAG
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AllOrdersCountsFragment(val tabPosition: Int) : Fragment() {

    private var ORDER_TAB_TYPE_ID: String = ""
    private lateinit var ordertypelist: java.util.ArrayList<TbOrderType>
    private var ORDER_TAB: String = ALL_ORDER_TAB
    private lateinit var binding: FragmentAllOrdersCountsBinding

    @Inject
    lateinit var rolePermission: RolePermission
    var startDate: String? = null
    var endDate: String? = null
    private var mPos: Int = 0
    private val viewModel by viewModels<AllOrdersViewModel>()
    private val ordersViewModel by activityViewModels<AllOrdersViewModel>()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentDate(Calendar.getInstance(), "", "", "0")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllOrdersCountsBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView: CURRENT POS = $tabPosition")
        requireContext().registerReceiver(broadcastReceiver, IntentFilter("allOrderCounts"));
        requireContext().registerReceiver(cancelledBroadcastReceiver, IntentFilter("cancelled"));
        binding.commonToolbar.root.gone()
        getAllOrderCounts(viewModel.startDate.value, viewModel.endDate.value)

        addChangeFragmentObserver()

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
        if (findNavController().currentDestination?.id == R.id.allOrdersFragment) {
            getOrderTypes()
            changePosition(0)
            setAdapter(mPos)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(broadcastReceiver)
        requireContext().unregisterReceiver(cancelledBroadcastReceiver)
    }

    private fun addChangeFragmentObserver(){

        /**
         * 0 = Pending orders , 1 = InProgress orders , 2 = Completed
         */

        ordersViewModel.changeTabPosition.observe(viewLifecycleOwner){
            if(it != -1){
                changePosition(it)
            }
        }

    }


    // To update all orders count if any new order created or updated
    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("08JUNE23", "onReceive: CALLED")
            val isCount = intent?.getBooleanExtra("isCount", false)
            startDate = intent?.getStringExtra("start_date")
            endDate = intent?.getStringExtra("end_date")

            getAllOrderCounts(startDate, endDate)

            if (isCount == true) {

                val count = intent.getIntExtra("count", 0)
                val orderType = intent.getStringExtra("param1")

                when (orderType) {
                    "0" -> {
                        //active
                        pendingOrdersCount = count
                        setAdapter(0)
                    }

                    "1" -> {
                        //complete
                        completedOrdersCount = count
                        setAdapter(1)
                    }

                    "2" -> {
                        //cancel
                        cancelledOrdersCount = count
                        setAdapter(2)
                    }
                }

                LogUtil.logE("broadcastReceiver", count.toString())
            } else {
                val position = intent?.getIntExtra("position", 0) ?: 0
                changePosition(position)
                setAdapter(position)
            }

        }
    }

    private var cancelledBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive: cancelledBroadcastReceiver: Called")
            val isCount = intent?.getBooleanExtra("isCount", false)
            startDate = intent?.getStringExtra("start_date")
            endDate = intent?.getStringExtra("end_date")

//            getAllOrderCounts(startDate, endDate)

            var position = intent?.getIntExtra("position", 0) ?: 0
            if(tabPosition == ALL_ORDER_TAB_POS){
                position = 3 //For cancelled in all orders otherwise 2
            }
            changePosition(position)
            setAdapter(position)

        }
    }

    private fun getOrderTypes() {

        dashboardViewModel.getOrderTypes.observe(requireActivity()) { res ->
            if (res.status == Status.SUCCESS) {
                if (res.data != null) {
                    ordertypelist = res.data.toCollection(arrayListOf())
                    Log.d("JUNELOGS", "getOrderTypes: $ordertypelist")
                    when (tabPosition) {
                        0 -> {
                            ORDER_TAB = ALL_ORDER_TAB
                        }

                        1 -> {
                            ORDER_TAB = OPEN_ORDER_TAB
                        }

                        2 -> {
                            ORDER_TAB = PHONE_ORDER_TAB
                        }

                        3 -> {
                            ORDER_TAB = ONLINE_ORDER_TAB
                        }

                        4 -> {
                            ORDER_TAB = THIRD_PARTY_ORDER_TAB
                        }

                    }

                    if (ORDER_TAB != ALL_ORDER_TAB) {
                        val filterList = ordertypelist.filter { it.orderType == ORDER_TAB }
                        if (filterList.isNotEmpty()) {
                            ORDER_TAB_TYPE_ID = filterList[0].id.toString()
                        }
                    }

                }
            }

        }
    }

    // To get all order type's total order count
    private fun getAllOrderCounts(startDate: String?, endDate: String?) {
        Log.d("08JUNE23", "getAllOrderCounts: CALLED")
        try {
            viewModel.allOrderCounts(startDate, endDate).observe(viewLifecycleOwner) {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            Log.d("08JUNE23", "getAllOrderCounts: ${it.data?.data}")


                            when (ORDER_TAB) {
                                ALL_ORDER_TAB -> {
                                    pendingOrdersCount = it.data?.data?.all_orders?.pending ?: 0
                                    ongoingOrderCount = it.data?.data?.all_orders?.in_progress ?: 0
                                    completedOrdersCount = it.data?.data?.all_orders?.completed ?: 0
                                    cancelledOrdersCount = it.data?.data?.all_orders?.rejected ?: 0
                                    upcomingOrderCount = it.data?.data?.all_orders?.upcoming ?: 0
                                }

                                OPEN_ORDER_TAB -> {
                                    pendingOrdersCount = it.data?.data?.open_orders?.active ?: 0
                                    ongoingOrderCount = 0
                                    completedOrdersCount =
                                        it.data?.data?.open_orders?.completed ?: 0
                                    cancelledOrdersCount =
                                        it.data?.data?.open_orders?.cancelled ?: 0
                                    upcomingOrderCount = 0
                                }

                                PHONE_ORDER_TAB -> {
                                    pendingOrdersCount = it.data?.data?.phone_orders?.active ?: 0
                                    ongoingOrderCount = 0
                                    completedOrdersCount =
                                        it.data?.data?.phone_orders?.completed ?: 0
                                    cancelledOrdersCount =
                                        it.data?.data?.phone_orders?.cancelled ?: 0
                                    upcomingOrderCount = 0
                                }

//                                ONLINE_ORDER_TAB -> {
//                                    pendingOrdersCount = it.data?.data?.web_orders?.pending ?: 0
//                                    ongoingOrderCount = it.data?.data?.web_orders?.in_progress ?: 0
//                                    completedOrdersCount = it.data?.data?.web_orders?.completed ?: 0
//                                    cancelledOrdersCount = it.data?.data?.web_orders?.rejected ?: 0
//                                    upcomingOrderCount = it.data?.data?.web_orders?.upcoming ?: 0
//                                }
//
//                                THIRD_PARTY_ORDER_TAB -> {
//                                    pendingOrdersCount =
//                                        it.data?.data?.third_party_online_orders?.pending ?: 0
//                                    ongoingOrderCount =
//                                        it.data?.data?.third_party_online_orders?.in_progress ?: 0
//                                    completedOrdersCount =
//                                        it.data?.data?.third_party_online_orders?.completed ?: 0
//                                    cancelledOrdersCount =
//                                        it.data?.data?.third_party_online_orders?.rejected ?: 0
//                                    upcomingOrderCount =
//                                        it.data?.data?.third_party_online_orders?.upcoming ?: 0
//                                }

                                // Added to reflect order count of online order and web order combine

                                ONLINE_ORDER_TAB,
                                THIRD_PARTY_ORDER_TAB -> {
                                    var orderOl = 0
                                    var orderWeb = 0

                                    // combine pending order
                                    orderOl = it.data?.data?.web_orders?.pending ?: 0
                                    orderWeb = it.data?.data?.third_party_online_orders?.pending ?: 0

                                    pendingOrdersCount = orderOl+orderWeb

                                    //combine in progress order
                                    orderOl = it.data?.data?.web_orders?.in_progress ?: 0
                                    orderWeb = it.data?.data?.third_party_online_orders?.in_progress ?: 0

                                    ongoingOrderCount = orderOl+orderWeb

                                    //combine in completed order
                                    orderOl = it.data?.data?.web_orders?.completed ?: 0
                                    orderWeb = it.data?.data?.third_party_online_orders?.completed ?: 0

                                    completedOrdersCount = orderOl+orderWeb

                                    //combine in rejected order
                                    orderOl = it.data?.data?.web_orders?.completed ?: 0
                                    orderWeb = it.data?.data?.third_party_online_orders?.rejected ?: 0

                                    completedOrdersCount = orderOl+orderWeb

                                    //combine in upcoming order
                                    orderOl = it.data?.data?.web_orders?.completed ?: 0
                                    orderWeb = it.data?.data?.third_party_online_orders?.upcoming ?: 0

                                    completedOrdersCount = orderOl+orderWeb
                                }
                            }

                            val allOrdersPendingCount = it.data?.data?.all_orders?.pending ?: 0
                            val openOrdersPendingCount = it.data?.data?.open_orders?.active ?: 0
                            val phoneOrdersPendingCount = it.data?.data?.phone_orders?.active ?: 0
                            var webOrdersPendingCount = it.data?.data?.web_orders?.pending ?: 0
                            var thirdPartyOrdersPendingCount =
                                it.data?.data?.third_party_online_orders?.pending ?: 0

                            //Added to reflect order count of online order and web order combine
                            webOrdersPendingCount += thirdPartyOrdersPendingCount
                            thirdPartyOrdersPendingCount = webOrdersPendingCount


                            EventBus.getDefault().post(
                                PendingCounts(
                                    allOrdersPendingCount,
                                    openOrdersPendingCount,
                                    phoneOrdersPendingCount,
                                    webOrdersPendingCount,
                                    thirdPartyOrdersPendingCount
                                )
                            )

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

        if (tabPosition == OPEN_ORDER_TAB_POS || tabPosition == PHONE_ORDER_TAB_POS) {

            when (position) {
                0 -> {
                    val activeOrders = AllOrdersListingFragment(
                        "0", "Pending", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(activeOrders)
                }

                1 -> {
                    val completedOrders = AllOrdersListingFragment(
                        "1", "Completed", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(completedOrders)
                }

                2 -> {
                    val cancelledOrders = AllOrdersListingFragment(
                        "2", "Rejected", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(cancelledOrders)
                }

            }

        } else {

            when (position) {
                0 -> {
                    val activeOrders = AllOrdersListingFragment(
                        "0", "Pending", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(activeOrders)
                }

                1 -> {
                    val inProgressOrders = AllOrdersListingFragment(
                        "1", "InProgress", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(inProgressOrders)
                }

                2 -> {
                    val completedOrders = AllOrdersListingFragment(
                        "2", "Completed", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(completedOrders)
                }

                3 -> {
                    val cancelledOrders = AllOrdersListingFragment(
                        "3", "Rejected", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(cancelledOrders)
                }

                4 -> {
                    val upcomingOrders = AllOrdersListingFragment(
                        "4", "UpComing", startDate, endDate, ORDER_TAB, ORDER_TAB_TYPE_ID
                    )
                    loadFragment(upcomingOrders)
                }
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

        list.add(InventoryItemModel(0, "Pending Orders", pendingOrdersCount, pos == 0))
        if (tabPosition == OPEN_ORDER_TAB_POS || tabPosition == PHONE_ORDER_TAB_POS) {
            list.add(InventoryItemModel(0, "Completed", completedOrdersCount, pos == 1))
            list.add(InventoryItemModel(0, "Cancelled ", cancelledOrdersCount, pos == 2))
        } else {
            list.add(InventoryItemModel(0, "InProgress Orders", ongoingOrderCount, pos == 1))
            list.add(InventoryItemModel(0, "Completed", completedOrdersCount, pos == 2))
            list.add(InventoryItemModel(0, "Cancelled ", cancelledOrdersCount, pos == 3))
            if (tabPosition == ONLINE_ORDER_TAB_POS || tabPosition == ALL_ORDER_TAB_POS || tabPosition == THIRD_PARTY_ORDER_TAB_POS) {
                list.add(InventoryItemModel(0, "Upcoming", upcomingOrderCount, pos == 4))
            }
        }

        binding.recyclerViewItemsList.adapter = InventoryAdapter(requireContext(),
            list,
            false,
            object : InventoryAdapter.InventoryListner {
                override fun onItemSelect(position: Int) {

                    var cancelPosition = 0

                    cancelPosition =
                        if (tabPosition == OPEN_ORDER_TAB_POS || tabPosition == PHONE_ORDER_TAB_POS) {
                            2
                        } else {
                            3
                        }

                    if (position == cancelPosition && rolePermission.hasCancelOrderPermission(
                            binding.root
                        )
                    ) {
                        changePosition(position)
                    } else {
                        changePosition(position)
                    }

                    LogUtil.logE(TAG, "position  $position")
                }
            })
    }

}