package com.android.pos.ui.fragments.orders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.data.remote.Constants.ACTIVE_ORDER
import com.android.pos.data.remote.Constants.CANCELED_ORDER
import com.android.pos.data.remote.Constants.COMPLETED_ORDER
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentInventoryBinding
import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.InventoryAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.getCustomerDisplay
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Orders : Fragment() {
    private var mPos: Int = 0
    private var activeOrdersCount: Int? = 0
    private var cancelledOrdersCount: Int? = 0
    private var completedOrdersCount: Int? = 0
    private var upcomingOrdersCount: Int? = 0
    val TAG = this.javaClass.name
    private lateinit var binding: FragmentInventoryBinding
    var startDate: String? = null
    var endDate: String? = null

    @Inject
    lateinit var rolePermission: RolePermission

    private val viewModel by viewModels<ActiveOrderViewModel>()
    private lateinit var presentation: CustomDisplay
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_inventory, container, false)
        binding.lifecycleOwner = this

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel
            )
        }

        configureToolbar()
        changePosition(0)
        // setAdapter(0)
        getOrderCountsObserver("", "")
        requireContext().registerReceiver(broadcastReceiver, IntentFilter("cancelled"));
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(KEY)
            ?.observe(viewLifecycleOwner) { it ->
                LogUtil.logE(TAG, "InventoryLifeCycler  $it")
                when (it) {
                    ACTIVE_ORDER -> {
                        changePosition(0)
                        setAdapter(0)
                    }
                    COMPLETED_ORDER -> {
                        changePosition(1)
                        setAdapter(1)
                    }
                    CANCELED_ORDER -> {
                        changePosition(2)
                        setAdapter(2)
                    }
                }

            }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOut()
        }
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
                        //active
                        activeOrdersCount = count
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
                val position = intent?.getIntExtra("position", 0)
                changePosition(position!!)
                setAdapter(position)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    private fun getOrderCountsObserver(startDate: String?, endDate: String?) {
        try {
            if (view != null) {
                viewModel.orderCounts(startDate, endDate).observe(viewLifecycleOwner) {
                    it?.let { resource ->
                        when (resource.status) {
                            Status.SUCCESS -> {

                                activeOrdersCount = it.data?.data?.activeOrders
                                cancelledOrdersCount = it.data?.data?.cancelledOrders
                                completedOrdersCount = it.data?.data?.completedOrders
                                upcomingOrdersCount = it.data?.data?.upcomingOrders

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
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    private fun configureToolbar() {
        binding.commonToolbar.imgDrawer.setOnClickListener {
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
            findNavController().navigate(R.id.action_orders_to_menuposbold)
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
            findNavController().navigate(R.id.action_orders_to_dashboardCategoryNew)
        }

        binding.commonToolbar.txtTitle.text = "Open Orders"
        binding.commonToolbar.imgOptionMenu.visibility = View.GONE
        binding.commonToolbar.txtSubTitle.text = "Active Orders"
        binding.commonToolbar.imgOptionMenuContainer.visibility = View.GONE
    }

    private fun changePosition(position: Int) {
        mPos = position
        when (position) {
            0 -> {
                val activeOrders = ActiveOrderFragment("0", startDate, endDate)
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Active Orders"
            }
            1 -> {
                val modifier: Fragment = ActiveOrderFragment("1", startDate, endDate)
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Completed"
            }

            2 -> {
                val cancelled = ActiveOrderFragment("2", startDate, endDate)
                loadFragment(cancelled)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Cancelled Orders"
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
                list.add(InventoryItemModel(0, "Active Orders ", activeOrdersCount, true))
//                list.add(InventoryItemModel(0, "Upcoming Orders ",upcomingOrdersCount))
                list.add(InventoryItemModel(0, "Completed ", completedOrdersCount))
                list.add(InventoryItemModel(0, "Cancelled Orders ", cancelledOrdersCount))
            }
            1 -> {
                list.add(InventoryItemModel(0, "Active Orders ", activeOrdersCount))
//                list.add(InventoryItemModel(0, "Upcoming Orders ",upcomingOrdersCount))
                list.add(InventoryItemModel(0, "Completed ", completedOrdersCount, true))
                list.add(InventoryItemModel(0, "Cancelled Orders ", cancelledOrdersCount))

            }
            2 -> {
                list.add(InventoryItemModel(0, "Active Orders ", activeOrdersCount))
//                list.add(InventoryItemModel(0, "Upcoming Orders ",upcomingOrdersCount))
                list.add(InventoryItemModel(0, "Completed ", completedOrdersCount))
                list.add(InventoryItemModel(0, "Cancelled Orders ", cancelledOrdersCount, true))

            }
        }
        binding.recyclerViewItemsList.adapter =
            InventoryAdapter(
                requireContext(),
                list,
                false,
                object : InventoryAdapter.InventoryListner {
                    override fun onItemSelect(position: Int) {
                        if (position == 2) {
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