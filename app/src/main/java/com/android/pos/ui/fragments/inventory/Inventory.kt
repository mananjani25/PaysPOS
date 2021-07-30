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
import com.android.pos.data.remote.Constants.CREATEDISCOUNT
import com.android.pos.data.remote.Constants.CREATEITEM
import com.android.pos.data.remote.Constants.CREATEMODIFIER
import com.android.pos.data.remote.Constants.CREATEOPTION

import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentInventoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.InventoryAdapter
import com.google.gson.Gson
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

        Log.e(TAG, "InventoryLoad " + Gson().toJson(savedInstanceState))

        changePosition(0)
        setAdapter(0)
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(KEY)
            ?.observe(viewLifecycleOwner) { it ->
                Log.e(TAG, "InventoryLifeCycler  $it")
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
                    CREATEDISCOUNT -> {
                        changePosition(3)
                        setAdapter(3)
                    }
                    CREATEOPTION -> {
                        changePosition(4)
                        setAdapter(4)
                    }
                }

            }


    }

    private fun configureToolbar() {
        binding.commonToolbar.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_dashboardCategory)
        }

        binding.commonToolbar.txtTitle.text = "Inventory"
        binding.commonToolbar.imgOptionMenu.visibility = View.GONE
        binding.commonToolbar.txtSubTitle.text = "All Items"
        binding.commonToolbar.imgOptionMenuContainer.visibility = View.GONE
    }

    private fun changePosition(position: Int) {
        when (position) {
            0 -> {
                val allItem: Fragment = AllItems()
                loadFragment(allItem)
                binding.commonToolbar.txtSetItem.visibility = View.GONE
                binding.commonToolbar.txtSubTitle.text = "All Items"
            }
            1 -> {
                val category: Fragment = Categories()
                loadFragment(category)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Categories"

            }
            2 -> {
                val modifier: Fragment = Modifiers()
                loadFragment(modifier)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Modifiers"
            }
            3 -> {
                val discount: Fragment = Discounts()
                loadFragment(discount)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Discounts"

            }
            4 -> {
                val option: Fragment = Options()
                loadFragment(option)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Options"
            }
            5 -> {
                val hideCategory: Fragment = HideCategoryListing()
                loadFragment(hideCategory)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Hidden Categories"
            }

            6 -> {
                val hideItem: Fragment = HideItemListing()
                loadFragment(hideItem)
                binding.commonToolbar.txtSetItem.visibility = View.VISIBLE
                binding.commonToolbar.txtSubTitle.text = "Hidden Items"
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
                list.add(InventoryItemModel(0, "All Items", true))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items"))
            }
            1 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories", true))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items"))

            }
            2 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers", true))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items"))

            }
            3 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts", true))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items"))

            }

            4 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options", true))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items"))

            }

            5 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories", true))
                list.add(InventoryItemModel(0, "Hidden Items"))

            }

            6 -> {
                list.add(InventoryItemModel(0, "All Items"))
                list.add(InventoryItemModel(0, "Categories"))
                list.add(InventoryItemModel(0, "Modifiers"))
                list.add(InventoryItemModel(0, "Discounts"))
                list.add(InventoryItemModel(0, "Options"))
                list.add(InventoryItemModel(0, "Hidden Categories"))
                list.add(InventoryItemModel(0, "Hidden Items", true))

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