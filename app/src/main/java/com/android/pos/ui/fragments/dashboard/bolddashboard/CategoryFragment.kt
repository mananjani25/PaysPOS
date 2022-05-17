package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CategoryParentModel
import com.android.pos.data.model.CategorySearchData
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.FragmentCategoryBinding
import com.android.pos.ui.adapter.CategoryItemAdapter1
import com.android.pos.ui.adapter.CategorySearchAdapter
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.ui.adapter.boldpos.CategoryParentAdapter
import com.android.pos.ui.adapter.boldpos.CategoryTabAdapter
import com.android.pos.ui.adapter.boldpos.ItemAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryFragment(val listner: ItemListner, val edtSearch: AutoCompleteTextView?) : Fragment(),
    CategoryTabAdapter1.TabListner,
    CategoryItemAdapter1.CategoryItemList, CategoryParentAdapter.CategoryParentListner {
    private var categoryList1: ArrayList<CategoryWithInventory> = arrayListOf()
    private lateinit var binding: FragmentCategoryBinding
    private lateinit var categoryParentAdapter: CategoryParentAdapter
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var searchList: ArrayList<CategorySearchData>
    private lateinit var searchAdapter: CategorySearchAdapter
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var itemList1: ArrayList<TbItem?> = arrayListOf()
    private var allItems: ArrayList<TbItem?> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    var list: ArrayList<CategoryParentModel> = arrayListOf()
    lateinit var itemListner: ItemListner
    private val TAG = "CategoryFragment"

    companion object {
        fun newInstance(callback: ItemListner): CategoryFragment {
            val fragment = CategoryFragment(callback, null)
            return fragment

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        observeShowProgress()
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        setVenueData()

    }

    private fun setVenueData() {
        viewModel.venueDataLocal().observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Status.SUCCESS -> {

                    val tbCategory = it.data
                    if (tbCategory != null) {
                        tabList.clear()
                        tabList = arrayListOf()
                        itemList1.clear()
                        itemList1 = arrayListOf()
                        list.clear()
                        list = arrayListOf()



                        categoryList1 = tbCategory as ArrayList<CategoryWithInventory>
                        if (categoryList1.isNotEmpty()) {

                            var tmpTabList: ArrayList<CategoryTabModel> = arrayListOf()

                            allItems.clear()
                            allItems = arrayListOf()
                            for (i in 0 until categoryList1.size) {


                                tabList.add(
                                    CategoryTabModel(
                                        categoryList1[i].category.id,
                                        categoryList1[i].category.name ?: "",
                                        if (i == 0) true else false,
                                        0
                                    )
                                )

                                tmpTabList.add(
                                    CategoryTabModel(
                                        categoryList1[i].category.id,
                                        categoryList1[i].category.name ?: "",
                                        if (i == 0) true else false,
                                        0
                                    )

                                )
                                categoryList1[i].inventoryLists?.forEach {
                                    allItems.add(it)
                                }
                                if ((tmpTabList.size == 8) or (tabList.size > 8 && tabList.size == categoryList1.size)) {
                                    var model = CategoryParentModel()
                                    model.list.addAll(tmpTabList)
                                    list.add(model)
                                    tmpTabList.clear()
                                    tmpTabList = arrayListOf()

                                } else if (tmpTabList.size < 8 && tmpTabList.size == categoryList1.size) {
                                    list.add(CategoryParentModel(tmpTabList))
                                }


                            }

                            categoryList1[0].inventoryLists?.filter {
                                it!!.isHide
                            }?.let { it1 ->
                                itemList1.addAll(it1)
                            }

                            categoryParentAdapter.addList(list)

                            searchCategory()


                            itemAdapter.addList(itemList1)
                            if (list.isNotEmpty()) {
                                binding.rvCategoryParent.scrollToPosition(0)

                            }

                        }

                    }
                    ProgressUtils.dismissProgressDialog()


                }
                Status.ERROR ->
                    ProgressUtils.dismissProgressDialog()

                Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

            }
        }

    }

    private fun searchCategory() {

        searchList = arrayListOf()
        categoryList1.forEach { categories ->
            val itemList = categories.inventoryLists
            itemList?.filter { it?.isHide == true }?.forEach { tbItem ->
                searchList.add(
                    CategorySearchData(
                        tbItem?.itemId ?: 0,
                        tbItem?.name ?: "",
                        tbItem?.imageUrl.toString(),
                        categories.category.name ?: "",
                        categories.category.id
                    )
                )
            }
        }
        searchAdapter =
            CategorySearchAdapter(
                requireActivity(),
                R.layout.search_category_item,
                searchList
            )
        edtSearch?.threshold = 3
        edtSearch?.setAdapter(searchAdapter)
        edtSearch?.setOnItemClickListener { parent, _, position, _ ->
            val model: CategorySearchData = parent.getItemAtPosition(position) as CategorySearchData
            edtSearch?.setText(model.title)
            resetTabbySearch(model)


        }
    }


    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = categoryParentAdapter.getList()
        for (i in 0 until tabList.get(0).list.size) {
            if (tabList.get(0).list[i].id == model.categoryID) {
                tabList.get(0).list[i].isSelected = true
                tabPos = i
            } else {
                tabList.get(0).list.get(i).isSelected = false
            }

        }

        categoryParentAdapter.list = tabList.toCollection(arrayListOf())
        categoryParentAdapter.notifyDataSetChanged()

        var itemList: ArrayList<TbItem?> = arrayListOf()

        categoryList1[tabPos].inventoryLists?.filter {
            it!!.isHide
        }?.let { it1 ->
            itemList.addAll(it1)
        }
        itemAdapter.list.clear()
        itemAdapter.list = itemList
        for (i in itemList.indices) {
            if (itemList[i]?.itemId == model.itemID) {
                itemAdapter.setPos(i)
                break
            }
        }

        itemAdapter.notifyDataSetChanged()


        // (binding.rvTabLayout.adapter as CategoryTabAdapter1).list.clear()
        /* (binding.rvCategoryParent.adapter as CategoryParentAdapter).list = tabList
         binding.rvCategoryParent.adapter?.notifyDataSetChanged()

         val listCategry = arrayListOf<TbItem?>()
         //listCategry.add(0, TbItem())
         categoryList1[tabPos].inventoryLists?.let { it1 -> listCategry.addAll(it1)
         }
         (binding.rvItemList.adapter as ItemAdapter).list.clear()
         (binding.rvItemList.adapter as ItemAdapter).list = listCategry
         binding.rvItemList.adapter?.notifyDataSetChanged()
 */

    }


    private fun observeShowProgress() {
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


    private fun setAdapter() {
        val list: ArrayList<CategoryParentModel> = arrayListOf()
        val listCategories: ArrayList<CategoryTabModel> = arrayListOf()
        for (i in 0 until 8) {
            listCategories.add(CategoryTabModel(0, "Drinks", i == 0, 0))
        }
        list.add(CategoryParentModel(listCategories))
        list.add(CategoryParentModel(listCategories))
        categoryParentAdapter = CategoryParentAdapter(requireContext(), arrayListOf(), this)
        itemAdapter = ItemAdapter(requireContext(), arrayListOf(), this)
        binding.rvItemList.adapter = itemAdapter
        binding.rvCategoryParent.adapter = categoryParentAdapter
        binding.rvCategoryParent.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvCategoryParent)
        binding.rvCategoryParent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                val bindingAdapterPos =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                Log.e(TAG, "bindingAdapterPosbindingAdapterPos  ${bindingAdapterPos}")

                categoryParentAdapter.list.forEachIndexed { index1, it ->
                    it.list.forEachIndexed { index, categoryTabModel ->
                        if (index1 == categoryParentAdapter.selectedParentPos && index == categoryParentAdapter.selectedCategoryPos) {
                            categoryTabModel.isSelected = true
                        } else {
                            categoryTabModel.isSelected = false
                        }
                    }
                }
                categoryParentAdapter.notifyDataSetChanged()

                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })


        val tabList: ArrayList<CategoryTabModel> = arrayListOf()
        for (i in 0 until list.size) {
            tabList.add(CategoryTabModel(0, "", i == 0, 0))
        }

        binding.rvTabLayout.adapter = CategoryTabAdapter(tabList)

    }


    override fun onClick(item: TbItem) {
        Log.e(TAG, "selectedItem:  ${Gson().toJson(item)}")

        listner.onItemSelected(item)


    }

    override fun onClickedCreateItem() {

    }

    override fun onCategorySelected(parentPosition: Int, childPosition: Int) {
        Log.e(TAG, "category parentPosition ${parentPosition}")
        Log.e(TAG, "category childPosition ${childPosition}")
        val categoryList = categoryParentAdapter.getList()
        Log.e(TAG, "categoryList:  ${Gson().toJson(categoryList)}")
        var listItems: ArrayList<TbItem?> = arrayListOf()
        listItems = itemAdapter.list.toCollection(arrayListOf())
        listItems.clear()
        listItems = arrayListOf()

        val categoryId =
            categoryParentAdapter.getList().get(parentPosition).list.get(childPosition).id
        Log.e(TAG, "selectedcategoryId:  ${categoryId}")
        Log.e(TAG, "itemList1itemList1:  ${Gson().toJson(itemList1)}")
        Log.e(TAG, "selectedInventory  ${Gson().toJson(categoryList1)}")
        categoryList1[childPosition].inventoryLists?.filter {
            it!!.isHide
        }?.let { it1 ->
            listItems.addAll(it1)
        }
        Log.e(TAG, " newlistItems: ${Gson().toJson(listItems)}")
        if (listItems.isNotEmpty()) {
            itemAdapter.setPos(-2)
            itemAdapter.addList(listItems)
        } else {
            Log.e(TAG, "ItemAdapterEmpty")
            itemAdapter.clearList()
        }

    }

    override fun onPositionChanged(position: Int) {
        Log.e(TAG, "onPOSChanged ${position}")
    }

    override fun onTabSelected(pos: Int) {

    }


}