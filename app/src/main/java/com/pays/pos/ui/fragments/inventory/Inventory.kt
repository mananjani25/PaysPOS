package com.pays.pos.ui.fragments.inventory

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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.InventoryItemModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CREATECATEGORY
import com.pays.pos.data.remote.Constants.CREATEITEM
import com.pays.pos.data.remote.Constants.CREATEMODIFIER
import com.pays.pos.data.remote.Constants.CREATEOPTION
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.FragmentInventoryBinding
import com.pays.pos.ui.adapter.InventoryAdapter
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Inventory : Fragment() {
    val TAG = this.javaClass.name
    private lateinit var binding: FragmentInventoryBinding
    private val viewModel by viewModels<InventoryViewModel>()
    private var itemsCount: Int? = 0
    private var categoriesCount: Int? = 0
    private var modifierSetsCount: Int? = 0
    private var optionSetsCount: Int? = 0
    private var hiddenCategoriesCount: Int? = 0
    private var hiddenItemsCount: Int? = 0
    private var hidden_items_website: Int? = 0
    private var mPos: Int = 0

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

            //val isCount = intent?.getBooleanExtra("isCount", false)

            getInventoryCountsObserver()

            val position = intent?.getIntExtra("position", 0)
            changePosition(position!!)
            setAdapter(position)

        }

    }

    private var syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            getInventoryCountsObserver()
        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()
        getInventoryCountsObserver()

        changePosition(0)
        setAdapter(0)
        requireContext().registerReceiver(broadcastReceiver, IntentFilter("inventory"))
        requireActivity().registerReceiver(
            syncReceiver,
            IntentFilter(Constants.SYNC_NOTIFICATION)
        )
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
        mPos = position
        when (position) {
            0 -> {
                val allItem: Fragment = AllItems(0, totalItems = itemsCount ?: 0)
                loadFragment(allItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = resources.getString(R.string.items_title)
            }
            1 -> {
                val category: Fragment = Categories(1)
                loadFragment(category)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.categories_title)

            }
            2 -> {
                val modifier: Fragment = Modifiers(2)
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
                val option: Fragment = Options(3)
                loadFragment(option)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = resources.getString(R.string.options_title)
            }

            4 -> {
                val hideItem: Fragment = HideItemListing(4)
                loadFragment(hideItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.hidden_items_title)
            }
            5 -> {
                val hideItem: Fragment = HideItemWebSiteListing(5)
                loadFragment(hideItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.website_hidden_items_title)
            }
            6 -> {
                val hideCategory: Fragment = HideCategoryListing(6)
                loadFragment(hideCategory)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text =
                    resources.getString(R.string.hidden_categories_title)
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
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount,
                        true
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount
                    )
                )
            }
            1 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount,
                        true
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                // list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount
                    )
                )
            }
            2 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount,
                        true
                    )
                )
                //  list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )

                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount
                    )
                )

            }
            3 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                // list.add(InventoryItemModel(0, "Discounts", true))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount,
                        true
                    )
                )

                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount
                    )
                )
            }

            4 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount, true
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount
                    )
                )
            }

            5 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )

                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website, true
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title), hiddenCategoriesCount
                    )
                )

            }
            6 -> {
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.items_title),
                        itemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.categories_title),
                        categoriesCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.modifiers_title),
                        modifierSetsCount
                    )
                )
                //list.add(InventoryItemModel(0, "Discounts"))
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.options_title),
                        optionSetsCount
                    )
                )

                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_items_title),
                        hiddenItemsCount
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.website_hidden_items_title),
                        hidden_items_website
                    )
                )
                list.add(
                    InventoryItemModel(
                        0,
                        resources.getString(R.string.hidden_categories_title),
                        hiddenCategoriesCount,
                        true
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
            InventoryAdapter(
                requireContext(),
                list,
                true,
                object : InventoryAdapter.InventoryListner {
                    override fun onItemSelect(position: Int) {
                        LogUtil.logE(TAG, "position  $position")
                        if (!list[position].isSelected) {
                            changePosition(position)
                        }
                    }

                })


    }

    //added by zeeshan for inventory items count
    private fun getInventoryCountsObserver() {
        try {
            if (view != null) {
                viewModel.inventoryCounts().observe(viewLifecycleOwner) {
                    it?.let { resource ->
                        when (resource.status) {
                            Status.SUCCESS -> {


                                /*private var itemsCount: Int? = 0
                            private var categoriesCount: Int? = 0
                            private var moodifierSetsCount: Int? = 0
                            private var optionSetsCount: Int? = 0
                            private var hiddenCategoriesCount: Int? = 0
                            private var hiddenItemsCount: Int? = 0*/


                                itemsCount = it.data?.data?.activeItems
                                categoriesCount = it.data?.data?.categories
                                modifierSetsCount = it.data?.data?.modifierSets
                                optionSetsCount = it.data?.data?.optionSets
                                hiddenCategoriesCount = it.data?.data?.hiddenCategories
                                hiddenItemsCount = it.data?.data?.hiddenItems
                                hidden_items_website = it.data?.data?.hidden_items_website

                                setAdapter(mPos)

                                Log.e("MENU ITEM","MENU SUCCESS ${mPos}")

                            }
                            Status.ERROR -> {

                                Log.e("MENU ITEM","MENU ERROR ${mPos}")

                                setAdapter(mPos)
                            }
                            Status.LOADING -> {
                                Log.e("MENU ITEM","MENU LOADING ${mPos}")

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

}