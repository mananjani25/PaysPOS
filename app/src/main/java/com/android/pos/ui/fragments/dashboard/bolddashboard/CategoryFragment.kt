package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
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
import com.android.pos.ui.adapter.boldpos.ItemAdapterPagDash
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.extensions.runOnUiThread
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CategoryFragment(val listner: ItemListner, val edtSearch: AutoCompleteTextView?) : Fragment(),
    CategoryTabAdapter1.TabListner,
    CategoryItemAdapter1.CategoryItemList, CategoryParentAdapter.CategoryParentListner {
    private var categoryList1: ArrayList<CategoryWithInventory> = arrayListOf()
    private lateinit var binding: FragmentCategoryBinding
    private lateinit var categoryParentAdapter: CategoryParentAdapter
    private lateinit var itemAdapter: ItemAdapterPagDash
    private lateinit var searchList: ArrayList<CategorySearchData>
    private lateinit var searchAdapter: CategorySearchAdapter
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var itemList1: ArrayList<TbItem?> = arrayListOf()
    private var allItems: ArrayList<TbItem?> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    var list: ArrayList<CategoryParentModel> = arrayListOf()
    private lateinit var categoryTabAdapter: CategoryTabAdapter
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
                    CoroutineScope(Dispatchers.IO).launch {

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
                                        if (it?.isDeleted == false)
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

                                val tabListLine: ArrayList<CategoryTabModel> = arrayListOf()
                                for (i in 0 until list.size) {
                                    tabListLine.add(CategoryTabModel(0, "", i == 0, 0))
                                }
                                runOnUiThread(Runnable {
                                    categoryTabAdapter.addList(tabListLine)
                                    categoryParentAdapter.addList(list)
                                })

                                categoryList1[0].inventoryLists?.filter {
                                    it!!.isHide && !it.isDeleted
                                }?.let { it1 ->
                                    itemList1.addAll(it1)
                                }



                                searchCategory()


                                if (list.isNotEmpty()) {
                                    if (prefProvider.getValueInt(
                                            Constants.CAT_ID_SELECTED,
                                            0
                                        ) == 0
                                    ) {
                                        Log.e("ItemAdapter", "AddedItem 1 ")
                                        Log.e("ItemAdapter", "itemListSize  ${itemList1.size}")

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            viewModel.itemsByCat(categoryList1[0].category.id)
                                                .collectLatest {
                                                    Log.e("CollectItems", "Collect")
                                                    lifecycleScope.launch(Dispatchers.Main) {
                                                        itemAdapter.submitData(it)
                                                    }
                                                }
                                        }
                                        runOnUiThread(Runnable {
                                            binding.rvItemList.adapter = null
                                          /*  itemAdapter = ItemAdapterPagDash(
                                                listner,
                                                null,
                                                prefProvider
                                            )
                                            binding.rvItemList.setHasFixedSize(true)
                                            binding.rvItemList.layoutManager =
                                                GridLayoutManager(
                                                    requireContext(),
                                                    4
                                                )*/
                                            binding.rvItemList.adapter = itemAdapter
                                        })
                                        runOnUiThread(Runnable {
                                            binding.rvCategoryParent.scrollToPosition(0)
                                        })

                                    } else {
                                        changePositionOfCate()

                                    }


                                }

                            } else {
                                Log.e("CategoryEmpty", "CategoryEmpty clearList")
                                runOnUiThread(Runnable {
                                    categoryParentAdapter.clearList()
                                })
                            }

                        } else {
                            Log.e(TAG, "CCategoryEmpty")
                            runOnUiThread {
                                categoryParentAdapter.clearList()
                            }
                        }
                        ProgressUtils.dismissProgressDialog()


                    }
                }

                Status.ERROR ->
                    ProgressUtils.dismissProgressDialog()

                Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

            }
        }


    }

    // to navigate to the desired category
    private fun changePositionOfCate() {
        runOnUiThread(Runnable {
            var tabPos = -1
            val tabList = categoryParentAdapter.getList()

            var posParent = -1

            for (i in 0 until tabList.size) {
                posParent++

                tabList[i].list.forEachIndexed { index, it ->

                    if (it.id == prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0)) {
                        it.isSelected = true
                        tabPos = index
                        return@forEachIndexed


                    } else {
                        it.isSelected = false
                    }

                }

                if (tabPos != -1) {
                    break

                }

            }
            var itemList: ArrayList<TbItem?> = arrayListOf()

            var tempV = getTabPosByParent(posParent)
            tabPos += tempV


            if (posParent != 0) {
                binding.rvCategoryParent.smoothScrollToPosition(posParent)
            }

            if (tabPos != -1) {
                if (tabPos < categoryList1.size) {
                    /*  categoryList1[tabPos].inventoryLists?.filter {
                          it!!.isHide && !it.isDeleted
                      }?.let { it1 ->
                          itemList.addAll(it1)
                      }*/

                    runOnUiThread(Runnable {
                        itemAdapter.clearData()
                    })


                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.itemsByCat(categoryList1[tabPos].category.id).collectLatest {

                                lifecycleScope.launch(Dispatchers.Main) {
                                    itemAdapter.submitData(it)
                                }


                            }
                        }


                }
                Log.e("ItemAdapter", "ItermListAdded 2 ")


                /* itemAdapter.snapshot().toCollection(arrayListOf()).addAll(itemList)
                 itemAdapter.notifyDataSetChanged()
     */
            }
        })

    }

    private fun searchCategory() {
        runOnUiThread(Runnable {
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
                val model: CategorySearchData =
                    parent.getItemAtPosition(position) as CategorySearchData
                edtSearch.setText(model.title)
                edtSearch.setSelection(model.title.length)
                MethodUtils.hideKeyboard(requireActivity())
                resetTabbySearch(model)


            }
        })
    }

    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = categoryParentAdapter.getList()

        var posParent = -1

        for (i in 0 until tabList.size) {
            posParent++

            tabList[i].list.forEachIndexed { index, it ->

                if (it.id == model.categoryID) {
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

        val itemList: ArrayList<TbItem?> = arrayListOf()
        val tabTempPos = getTabPosByParent(posParent)
        tabPos += tabTempPos

        binding.rvCategoryParent.smoothScrollToPosition(posParent)
        if (tabPos != -1) {
            Log.e(TAG, "gettabPos:  ${tabPos}")
            categoryList1[tabPos].inventoryLists?.filter {
                it!!.isHide
            }?.let { it1 ->
                itemList.addAll(it1)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.itemsByCat(categoryList1[tabPos].category.id).collectLatest {

                    lifecycleScope.launch(Dispatchers.Main){
                        itemAdapter.snapshot().toCollection(arrayListOf()).clear()


                        itemAdapter.submitData(it)
                        for (i in itemList.indices) {
                            if (itemList[i]?.itemId == model.itemID) {
                                itemAdapter.setPos(i)
                                break
                            }
                        }
                    }

                    //itemAdapter.notifyDataSetChanged()


                }
            }


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
        itemAdapter = ItemAdapterPagDash(listner, null, prefProvider)
        binding.rvItemList.setHasFixedSize(true)
        binding.rvItemList.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvItemList.adapter = itemAdapter
        binding.rvItemList.isFocusable = false
        //   binding.rvItemList.layoutManager = GridLayoutManager(requireContext(),4)

        //  binding.rvItemList.setHasFixedSize(true)
        binding.rvCategoryParent.adapter = categoryParentAdapter
        binding.rvCategoryParent.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvCategoryParent)
        binding.rvCategoryParent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                runOnUiThread(Runnable {
                val bindingAdapterPos =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                categoryParentAdapter.list.forEachIndexed { index1, it ->
                    it.list.forEachIndexed { index, categoryTabModel ->

                        categoryTabModel.isSelected =
                            categoryTabModel.id == prefProvider.getValueInt(
                                Constants.CAT_ID_SELECTED,
                                0
                            )
                    }
                }
                val tabList: ArrayList<CategoryTabModel> = arrayListOf()
                for (i in 0 until categoryParentAdapter.list.size) {
                    tabList.add(CategoryTabModel(0, "", i == bindingAdapterPos, 0))
                }
                categoryTabAdapter.addList(tabList)
                categoryParentAdapter.notifyDataSetChanged()

                })

                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        val tabList: ArrayList<CategoryTabModel> = arrayListOf()
        categoryTabAdapter = CategoryTabAdapter(tabList)
        binding.rvTabLayout.adapter = categoryTabAdapter
    }


    override fun onClick(item: TbItem) {
        listner.onItemSelected(item, 0)


    }

    override fun onClickedCreateItem() {

    }

    override fun onCategorySelected(parentPosition: Int, childPosition: Int) {
        lifecycleScope.launch {
            itemAdapter.submitData(PagingData.empty())
        }
        Log.e(
            "onCategorySelected",
            "parentPosition : $parentPosition childPosition : $childPosition"
        )


        if (childPosition != -1 && parentPosition != -1) {
            val categoryId =
                categoryParentAdapter.getList()[parentPosition].list[childPosition].id

            prefProvider.setValueInt(Constants.CAT_ID_SELECTED, categoryId)
            Log.e(TAG, "cateSelectedcategoryId  ${categoryId}")

            getItemsByCategory(categoryId)
        }


    }

    private fun getItemsByCategory(catId: Int) {

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.itemsByCat(catId).collectLatest {


                //itemAdapter.setPos(-2)

                lifecycleScope.launch(Dispatchers.Main) {

                    edtSearch?.text?.clear()
                    if (prefProvider.getValueInt(Constants.CAT_ID_SELECTED, 0) == 0) {

                            itemAdapter.submitData(it)
                            binding.rvCategoryParent.scrollToPosition(0)


                    } else {
                        changePositionOfCate()
                    }

                    itemAdapter.submitData(it)
                }
            }


        }
    }


    override fun onPositionChanged(position: Int) {
    }

    override fun onTabSelected(pos: Int) {

    }


}