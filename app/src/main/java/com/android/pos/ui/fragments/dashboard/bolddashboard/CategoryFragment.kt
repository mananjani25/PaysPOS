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
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCategoryBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.CategoryItemAdapter1
import com.android.pos.ui.adapter.CategorySearchAdapter
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.ui.adapter.boldpos.CategoryParentAdapter
import com.android.pos.ui.adapter.boldpos.CategoryTabAdapter
import com.android.pos.ui.adapter.boldpos.ItemAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

    @Inject
    lateinit var prefProvider: PrefProvider

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

                            categoryParentAdapter.addList(list)

                            categoryList1[0].inventoryLists?.filter {
                                it!!.isHide
                            }?.let { it1 ->
                                itemList1.addAll(it1)
                            }



                            searchCategory()


                            if (list.isNotEmpty()) {
                                if (prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0) == 0) {
                                    Log.e(TAG, "GOTZERO")
                                    itemAdapter.addList(itemList1)
                                    binding.rvCategoryParent.scrollToPosition(0)

                                } else {
                                    changePositionOfCate()

                                }


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

    private fun changePositionOfCate() {
        var tabPos = -1
        val tabList = categoryParentAdapter.getList()
        Log.e(TAG, "tabListGET  ${Gson().toJson(tabList)}")

        var posParent = -1

        Log.d(
            TAG,
            "changePositionOfCate: " + prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0)
        )
        for (i in 0 until tabList.size) {
            posParent++

            tabList[i].list.forEachIndexed { index, it ->

                if (it.id == prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0)) {
                    Log.e(TAG, "indexCategory  ${index}")
                    it.isSelected = true
                    tabPos = index
                    //prefProvider.setValueInt(Constants.CAT_ID_SELECTED, 0)
                    return@forEachIndexed


                } else {
                    it.isSelected = false
                }

            }

            if (tabPos != -1) {
                break

            }

        }
        Log.e(TAG, "selectedTabList  ${Gson().toJson(tabList)}")
        var itemList: ArrayList<TbItem?> = arrayListOf()
        Log.e(TAG, "tabPosGET  ${tabPos}")
        Log.e(TAG, "posParentGET  ${posParent}")

        var tempV = getTabPosByParent(posParent)
        tabPos += tempV



        binding.rvCategoryParent.smoothScrollToPosition(posParent)

        if (tabPos != -1) {
            if (tabPos < categoryList1.size) {
                categoryList1[tabPos].inventoryLists?.filter {
                    it!!.isHide
                }?.let { it1 ->
                    itemList.addAll(it1)
                }
            }
            itemAdapter.addList(itemList)

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
        edtSearch?.threshold = 2
        edtSearch?.setAdapter(searchAdapter)
        edtSearch?.setOnItemClickListener { parent, _, position, _ ->
            val model: CategorySearchData = parent.getItemAtPosition(position) as CategorySearchData
            edtSearch.setText(model.title)
            edtSearch.setSelection(model.title.length)
            MethodUtils.hideKeyboard(requireActivity())
            resetTabbySearch(model)


        }
    }

    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = categoryParentAdapter.getList()
        Log.e(TAG, "tabList  ${Gson().toJson(tabList)}")

        var posParent = -1

        for (i in 0 until tabList.size) {
            posParent++

            tabList[i].list.forEachIndexed { index, it ->

                if (it.id == model.categoryID) {
                    Log.e(TAG, "indexCategory  ${index}")
                    it.isSelected = true
                    prefProvider.setValueInt(Constants.CAT_ID_SELECTED, model.categoryID)
                    tabPos = index
                    categoryParentAdapter.notifyItemChanged(posParent)
                    return@forEachIndexed


                } else {
                    it.isSelected = false
                }

            }

            if (tabPos != -1) {
                break

            }

        }
        Log.d(TAG, "resetTabbySearch: " + Gson().toJson(tabList))
//        categoryParentAdapter.addList(tabList.toCollection(arrayListOf()))
//        categoryParentAdapter.notifyDataSetChanged()

        var itemList: ArrayList<TbItem?> = arrayListOf()
        Log.e(TAG, "tabPos  ${tabPos}")
        Log.e(TAG, "posParent  ${posParent}")
        var tabTempPos = getTabPosByParent(posParent)
        Log.e(TAG, "tabTempPos  ${tabTempPos}")
        tabPos += tabTempPos
        /* if (posParent == 1) {
             tabPos += 8
         } else if (posParent == 2) {
             tabPos += 16
         } else if (posParent == 3) {
             tabPos += 24
         } else if (posParent == 4) {
             tabPos += 32
         } else if (posParent == 5) {
             tabPos += 40
         }*/

        binding.rvCategoryParent.smoothScrollToPosition(posParent)
        if (tabPos != -1) {
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

        }
    }

    private fun getTabPosByParent(posParent: Int): Int {
        return posParent * 8


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
        itemAdapter = ItemAdapter(requireContext(), arrayListOf(), listner)
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
                        Log.e(TAG, "categoryId ${categoryTabModel.id}")
                        Log.e(
                            TAG,
                            "selectedLastID ${
                                prefProvider.getValueInt(
                                    Constants.CAT_ID_SELECTED,
                                    0
                                )
                            }"
                        )
                        if (categoryTabModel.id == prefProvider.getValueInt(
                                Constants.CAT_ID_SELECTED,
                                0
                            )
                        ) {
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
        listner.onItemSelected(item)


    }

    override fun onClickedCreateItem() {

    }

    override fun onCategorySelected(parentPosition: Int, childPosition: Int) {
        Log.e(TAG, "parentPosition  ${parentPosition} childPosition ${childPosition}")

        val categoryId =
            categoryParentAdapter.getList()[parentPosition].list[childPosition].id
        Log.e(TAG, "categoryId  ${categoryId}")

        prefProvider.setValueInt(Constants.CAT_ID_SELECTED, categoryId)

        viewModel.getItemByCategoryId(categoryId).observe(viewLifecycleOwner) {

            if (it.status == Status.SUCCESS) {
                if (it.data != null && it.data.isNotEmpty()) {
                    Log.e(TAG, "CategorySelectedata")
                    itemAdapter.setPos(-2)
                    it.data.filter {
                        it.isHide
                    }.let { it1 ->
                        edtSearch?.text?.clear()
                        if (prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0) == 0) {
                            Log.e(TAG, "GOTZERO")
                            itemAdapter.addList(it.data.toCollection(arrayListOf()))
                            binding.rvCategoryParent.scrollToPosition(0)

                        } else {
                            changePositionOfCate()
                        }


                        //itemAdapter.addList(it1.toCollection(arrayListOf()))
                        Log.d(TAG, "onCategorySelected: size" + it1.size)
                    }
                } else {
                    itemAdapter.clearList()
                }
            }

        }

    }


    override fun onPositionChanged(position: Int) {
        Log.e(TAG, "onPOSChanged ${position}")
    }

    override fun onTabSelected(pos: Int) {

    }


}