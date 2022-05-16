package com.android.pos.ui.fragments.onlineorder

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.databinding.FragmentOnlineOrderBinding

import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.InventoryAdapter
import com.android.pos.utils.TAG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class OnlineOrderFragment : Fragment() {

    private lateinit var binding: FragmentOnlineOrderBinding

    @Inject
    lateinit var rolePermission: RolePermission
    var startDate:String?=null
    var endDate:String?=null
    private var mPos: Int = 0
    private var pendingOrdersCount: Int? = 0
    private var cancelledOrdersCount: Int? = 0
    private var completedOrdersCount: Int? = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOnlineOrderBinding.inflate(inflater,  container, false)
        configureToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changePosition(0)
        setAdapter(mPos)
    }
    private fun configureToolbar() {
        binding.commonToolbar?.imgDrawer?.setOnClickListener {
            findNavController().navigate(R.id.action_onlineOrder_to_menuposbold)
        }
        binding.commonToolbar?.txtHome?.setOnClickListener {
            findNavController().navigate(R.id.action_onlineOrder_to_dashboarCategorynew)
        }

        binding.commonToolbar?.txtTitle?.text = "Online Orders"
        binding.commonToolbar?.imgOptionMenu?.visibility = View.GONE
        binding.commonToolbar?.txtSubTitle?.text = "Ongoing Orders"
        binding.commonToolbar?.imgOptionMenuContainer?.visibility = View.GONE
    }
    private fun changePosition(position: Int) {
        mPos = position
        when (position) {
            0 -> {
                val activeOrders = OnlineDetailFragment("0",startDate,endDate)
                loadFragment(activeOrders)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Ongoing Orders"
            }
            1 -> {
                val modifier: Fragment = OnlineDetailFragment("1",startDate,endDate)
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "Completed Orders"
            }

            2 -> {
                val cancelled = OnlineDetailFragment("2",startDate,endDate)
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
                list.add(InventoryItemModel(0, "Ongoing Orders ",0, true))
                list.add(InventoryItemModel(0, "Completed Orders",completedOrdersCount))
                list.add(InventoryItemModel(0, "Cancelled Orders ",cancelledOrdersCount))
            }
            1 -> {
                list.add(InventoryItemModel(0, "Ongoing Orders ",0))
                list.add(InventoryItemModel(0, "Completed Orders",completedOrdersCount ,true))
                list.add(InventoryItemModel(0, "Cancelled Orders ",cancelledOrdersCount))

            }
            2 -> {
                list.add(InventoryItemModel(0, "Ongoing Orders ",0))
                list.add(InventoryItemModel(0, "Completed Orders",completedOrdersCount))
                list.add(InventoryItemModel(0, "Cancelled Orders ",cancelledOrdersCount, true))

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
                        Log.e(TAG, "position  $position")


                    }

                })


    }

}