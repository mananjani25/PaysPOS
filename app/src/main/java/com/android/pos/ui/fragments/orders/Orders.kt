package com.android.pos.ui.fragments.orders

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()

        changePosition(0)
        setAdapter(0)
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

                }

            }


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
                val allItem: Fragment = ActiveOrderFragment()
                loadFragment(allItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Active Orders"
            }
            1 -> {
                val category: Fragment = UpcomingOrderFragment()
                loadFragment(category)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Upcoming Orders"

            }
            2 -> {
                val modifier: Fragment = CompletedOrderFragment()
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Completed"
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
            }
            1 -> {
                list.add(InventoryItemModel(0, "Active Orders"))
                list.add(InventoryItemModel(0, "Upcoming Orders", true))
                list.add(InventoryItemModel(0, "Completed"))


            }
            2 -> {
                list.add(InventoryItemModel(0, "Active Orders"))
                list.add(InventoryItemModel(0, "Upcoming Orders"))
                list.add(InventoryItemModel(0, "Completed", true))

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