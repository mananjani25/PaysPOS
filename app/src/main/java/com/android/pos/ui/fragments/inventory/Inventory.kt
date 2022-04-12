
package com.android.pos.ui.fragments.inventory

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
import com.android.pos.data.remote.Constants.CREATECATEGORY
import com.android.pos.data.remote.Constants.CREATEITEM
import com.android.pos.data.remote.Constants.CREATEMODIFIER
import com.android.pos.data.remote.Constants.CREATEOPTION
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentInventoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.InventoryAdapter
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Inventory : Fragment() {
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
                when (it) {
                    CREATEITEM -> {
                        changePosition(0)
                        setAdapter(0)
                    }

                    CREATECATEGORY -> {
                        changePosition(1)
                        setAdapter(1)
                    }
                    CREATEMODIFIER -> {
                        changePosition(2)
                        setAdapter(2)
                    }
                    /*  CREATEDISCOUNT -> {
                          changePosition(3)
                          setAdapter(3)
                      }*/
                    CREATEOPTION -> {
                        changePosition(3)
                        setAdapter(3)
                    }
                }

            }


    }

    private fun configureToolbar() {
        binding.commonToolbar.imgDrawer.setOnClickListener {
           // (requireActivity() as MainActivity).enableDrawer()
            findNavController().navigate(R.id.action_inventory_to_menuFragment)
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_dashboardCategory)
        }

        binding.commonToolbar.txtTitle.text = resources.getString(R.string.inventory_title)
        binding.commonToolbar.imgOptionMenu.visibility = View.GONE
        binding.commonToolbar.txtSubTitle.text = resources.getString(R.string.items_title)
        binding.commonToolbar.imgOptionMenuContainer.visibility = View.GONE
    }

    private fun changePosition(position: Int) {
        when (position) {
            0 -> {
                val allItem: Fragment = AllItems()
                loadFragment(allItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = resources.getString(R.string.items_title)
            }
            1 -> {
                val category: Fragment = Categories()
                loadFragment(category)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.categories_title)

            }
            2 -> {
                val modifier: Fragment = Modifiers()
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.modifiers_title)
            }
            /* 3 -> {
                 val discount: Fragment = DiscountList()
                 loadFragment(discount)
                 binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                 binding.commonToolbar.txtSubTitle.text = "Discounts"

             }*/
            3 -> {
                val option: Fragment = Options()
                loadFragment(option)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = resources.getString(R.string.options_title)
            }
            4 -> {
                val hideCategory: Fragment = HideCategoryListing()
                loadFragment(hideCategory)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.hidden_categories_title)
            }

            5 -> {
                val hideItem: Fragment = HideItemListing()
                loadFragment(hideItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.hidden_items_title)
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
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title), 0,true))
                list.add(InventoryItemModel(0, resources.getString(R.string.categories_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title),0))
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title)
                        ,0)
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title),0))
            }
            1 -> {
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        0,true
                    )
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title),0))
                // list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title)
                        ,0)
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title),0))

            }
            2 -> {
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.categories_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title), 0,true))
                //  list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title)
                        ,0)
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title),0))

            }
            3 -> {
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.categories_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title),0))
                // list.add(InventoryItemModel(0, "Discounts", true))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title), 0,true))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title)
                        ,0)
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title),0))

            }

            4 -> {
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.categories_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title),0))
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        0,true
                    )
                )
                list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title),0))

            }

            5 -> {
                list.add(InventoryItemModel(0, resources.getString(R.string.items_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.categories_title),0))
                list.add(InventoryItemModel(0, resources.getString(R.string.modifiers_title),0))
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, resources.getString(R.string.options_title),0))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title)
                        ,0)
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        0,true
                    )
                )

            }

            /* 6 -> {
                 list.add(InventoryItemModel(0, "All Items"))
                 list.add(InventoryItemModel(0, "Categories"))
                 list.add(InventoryItemModel(0, "Modifiers"))
                 //list.add(InventoryItemModel(0, "Discounts"))
                 list.add(InventoryItemModel(0, "Options"))
                 list.add(InventoryItemModel(0, "Hidden Categories"))
                 list.add(InventoryItemModel(0, resources.getString(R.string.hidden_items_title), true))

             }*/

        }
        binding.recyclerViewItemsList.adapter =
            InventoryAdapter(requireContext(), list, true, object : InventoryAdapter.InventoryListner {
                override fun onItemSelect(position: Int) {
                    Log.e(TAG, "position  $position")
                    changePosition(position)
                }

            })


    }

    //added by zeeshan for inventory items count
    private fun getOrderCountsObserver(startDate: String?, endDate: String?) {
        try {
/*
            viewModel.orderCounts(startDate,endDate).observe(viewLifecycleOwner) {
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
*/

        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}