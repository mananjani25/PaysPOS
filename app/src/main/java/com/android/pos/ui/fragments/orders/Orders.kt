package com.android.pos.ui.fragments.orders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.data.remote.Constants.ACTIVE_ORDER
import com.android.pos.data.remote.Constants.CANCELED_ORDER
import com.android.pos.data.remote.Constants.COMPLETED_ORDER
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.UPCOMING_ORDER
import com.android.pos.databinding.FragmentInventoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.InventoryAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Orders : Fragment() {
    val TAG = this.javaClass.name
    private lateinit var binding: FragmentInventoryBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_inventory, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }


    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var position = intent?.getIntExtra("position", 0)
            changePosition(position!!)
            setAdapter(position)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()
        changePosition(0)
        setAdapter(0)
        requireContext().registerReceiver(broadcastReceiver, IntentFilter("cancelled"));
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(KEY)
            ?.observe(viewLifecycleOwner) { it ->
                Log.e(TAG, "InventoryLifeCycler  $it")
                when (it) {
                    ACTIVE_ORDER -> {
                        changePosition(0)
                        setAdapter(0)
                    }

                    UPCOMING_ORDER -> {
                        changePosition(1)
                        setAdapter(1)
                    }
                    COMPLETED_ORDER -> {
                        changePosition(2)
                        setAdapter(2)
                    }
                    CANCELED_ORDER -> {
                        changePosition(3)
                        setAdapter(3)
                    }
                }

            }


    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(broadcastReceiver)
    }
    private fun configureToolbar() {
        binding.commonToolbar.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_orders_to_dashboardCategoryNew)
        }

        binding.commonToolbar.txtTitle.text = ""
        binding.commonToolbar.imgOptionMenu.visibility = View.GONE
        binding.commonToolbar.txtSubTitle.text = "Active Orders"
        binding.commonToolbar.imgOptionMenuContainer.visibility = View.GONE
    }

    private fun changePosition(position: Int) {
        when (position) {
            0 -> {
                val activeOrders = ActiveOrderFragment.newInstance("0")
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Active Orders"
            }
            1 -> {
                val upcomingOrders = ActiveOrderFragment.newInstance("Upcoming")
                loadFragment(upcomingOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Upcoming Orders"

            }
            2 -> {
                val modifier: Fragment = ActiveOrderFragment.newInstance("1")
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Completed"
            }

            3 -> {
                val cancelled = ActiveOrderFragment.newInstance("2")
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
        val list: ArrayList<InventoryItemModel> = arrayListOf()
        when (pos) {
            0 -> {
                list.add(InventoryItemModel(0, "Active Orders", true))
                list.add(InventoryItemModel(0, "Upcoming Orders"))
                list.add(InventoryItemModel(0, "Completed"))
                list.add(InventoryItemModel(0, "Cancelled Orders"))
            }
            1 -> {
                list.add(InventoryItemModel(0, "Active Orders"))
                list.add(InventoryItemModel(0, "Upcoming Orders", true))
                list.add(InventoryItemModel(0, "Completed"))
                list.add(InventoryItemModel(0, "Cancelled Orders"))

            }
            2 -> {
                list.add(InventoryItemModel(0, "Active Orders"))
                list.add(InventoryItemModel(0, "Upcoming Orders"))
                list.add(InventoryItemModel(0, "Completed", true))
                list.add(InventoryItemModel(0, "Cancelled Orders"))

            }
            3 -> {
                list.add(InventoryItemModel(0, "Active Orders"))
                list.add(InventoryItemModel(0, "Upcoming Orders"))
                list.add(InventoryItemModel(0, "Completed"))
                list.add(InventoryItemModel(0, "Cancelled Orders", true))

            }
        }
        binding.recyclerViewItemsList.adapter =
            InventoryAdapter(requireContext(), list, object : InventoryAdapter.InventoryListner {
                override fun onItemSelect(position: Int) {
                    Log.e(TAG, "position  $position")
                    changePosition(position)
                }

            })


    }
}