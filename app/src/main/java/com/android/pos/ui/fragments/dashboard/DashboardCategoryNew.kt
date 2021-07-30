package com.android.pos.ui.fragments.dashboard

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CategorySearchData
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.data.remote.Constants.HORIZONTAL
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.VERTICAL
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.databinding.PopupDashboardBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryItemAdapter1
import com.android.pos.ui.adapter.CategorySearchAdapter
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardCategoryNew : Fragment() {
    private lateinit var binding: FragmentDashboardCategoryNewBinding
    private val TAG = "DashboardCategoryNew"

    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList: MutableList<VenueDataResponse.Data.Category> = arrayListOf()
    private var categoryList1: MutableList<CategoryWithInventory> = arrayListOf()
    private var itemList: ArrayList<VenueDataResponse.Data.Category.Item> = arrayListOf()
    private var itemList1: ArrayList<TbItem?> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    private lateinit var searchAdapter: CategorySearchAdapter
    private lateinit var searchList: ArrayList<CategorySearchData>

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onAttach(context: Context) {
        super.onAttach(context)


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefProvider.setValueboolean(IS_CLOCKOUT, false)
        binding = FragmentDashboardCategoryNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.footer.imgClock.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.clockout_message)
            ) {
                positiveButton(getString(android.R.string.ok)) {
                    val bundle = Bundle()
                    bundle.putBoolean("isDashboard", true)
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_passcode,
                        bundle
                    )
                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVenueData()
        configureDrawer()
        onClick()
        //searchItem()

    }

    /*private fun searchItem() {
        binding.layoutMenu.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.layoutMenu.autoSearch.text.isNotEmpty()) {
                    searchbyKey(binding.layoutMenu.autoSearch.text.trim().toString())


                }


            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }*/


    private fun searchbyKey(searchKey: String) {
        Log.e(TAG, "searchKey:  $searchKey")
        Log.e(TAG, "categoryListSearch  ${Gson().toJson(categoryList1)}")


        var localVariable = ""

        for (i in 0 until categoryList1.size) {

            var dataSearch = categoryList1.get(i).inventoryLists?.filter {
                if (it?.name?.toLowerCase().toString()
                        .startsWith(searchKey.toLowerCase().toString())
                ) {
                    localVariable = it?.name!!
                    Log.e(TAG, "localVariableInside  ${localVariable}")

                    return@filter true

                } else {
                    return@filter false
                }


            }

            if (localVariable.isNotEmpty()) {
                Log.e(TAG, "localVariable  ${localVariable}")
                break
            }


            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val filters =categoryList1.stream().filter {
                    Log.e(TAG,"StreamItem  ${Gson().toJson(it)}")
                     for (i in 0 until it.inventoryLists!!.size){


                             return@filter (it.inventoryLists!!.get(i)?.name!!.startsWith(searchKey))

                     }
                     return@filter false


                }

                Log.e(TAG,"filtersItem  ${Gson().toJson(filters)}")
            }*/
        }


    }


    private fun searchCategory() {

        searchList = arrayListOf()


        for (i in 0 until categoryList1.size) {
            for (j in 0 until categoryList1.get(i).inventoryLists!!.size) {

                /* var imgPath = ""
                 if (categoryList1.get(i).inventoryLists?.get(j)?.imageUrl != null || categoryList1.get(i).inventoryLists?.get(j)?.imageUrl != ""){
                     imgPath = categoryList1.get(i).inventoryLists?.get(j)?.imageUrl.toString()
                 }
                 else {
                     imgPath =""
                 }*/
                searchList.add(
                    CategorySearchData(
                        categoryList1.get(i).inventoryLists!!.get(j)!!.itemId,
                        categoryList1.get(i).inventoryLists!!.get(j)!!.name,
                        categoryList1.get(i).inventoryLists?.get(j)?.imageUrl.toString(),
                        categoryList1.get(i).category.name,
                        categoryList1.get(i).category.id
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
        binding.layoutMenu.autoSearch.threshold = 3
        binding.layoutMenu.autoSearch.setAdapter(searchAdapter)
        binding.layoutMenu.autoSearch.setOnItemClickListener { parent, view, position, id ->
            val model: CategorySearchData = parent.getItemAtPosition(position) as CategorySearchData
            binding.layoutMenu.autoSearch.setText(model.title)
            resetTabbySearch(model)


        }
    }

    private fun onClick() {
        binding.layoutMenu.imgOptionMenu.setOnClickListener {
//            showPopup(binding.viewPopup)
            showPopup(binding.layoutMenu.imgOptionMenu)
            // showInfoDialog(binding.layoutMenu.imgOptionMenu, requireActivity())
        }

        binding.footer.linearMore.setOnClickListener {
            dialogPOSMenu()
            //findNavController().navigate(R.id.action_dashboardCategoryNew_to_menuPOS)

        }

    }

    private fun dialogPOSMenu() {

        val dialog: Dialog = Dialog(requireContext(), android.R.style.Theme_Light)

        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )


        dialog.setContentView(R.layout.menu_pos)
        dialog.setCanceledOnTouchOutside(false)

        val imgClose: ImageView = dialog.findViewById(R.id.imgClose)
        val footerView: View = dialog.findViewById(R.id.footer)

        val imgCalculator: ImageView = footerView.findViewById(R.id.imgCalculator)
        val txtCheckOut: TextView = footerView.findViewById(R.id.txtCheckOut)

        val imgMore: ImageView = footerView.findViewById(R.id.imgMore)
        val txtMore: TextView = footerView.findViewById(R.id.txtMore)
        val linearMore: LinearLayout = footerView.findViewById(R.id.linearMore)
        val linearHome: LinearLayout = dialog.findViewById(R.id.linearHome)
        val linearOrders: LinearLayout = dialog.findViewById(R.id.linearOrders)
        val linearTransaction: LinearLayout = dialog.findViewById(R.id.linearTransaction)
        val linearCash: LinearLayout = dialog.findViewById(R.id.linearCash)
        val linearReports: LinearLayout = dialog.findViewById(R.id.linearReports)
        val linearCust: LinearLayout = dialog.findViewById(R.id.linearCust)
        val linearTeam: LinearLayout = dialog.findViewById(R.id.linearTeam)
        val linearInventory: LinearLayout = dialog.findViewById(R.id.linearInventory)
        val linearSetting: LinearLayout = dialog.findViewById(R.id.linearSetting)
        val linearSupport: LinearLayout = dialog.findViewById(R.id.linearSupport)

        linearHome.setOnClickListener {
            closeDialog(dialog)
        }
        linearCust.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_customer)
            closeDialog(dialog)
        }
        linearReports.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_reports)
            dialog.dismiss()
        }
        linearTeam.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_teamList)
            dialog.dismiss()
        }
        linearInventory.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_inventory)
            dialog.dismiss()
        }
        linearSetting.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_settings)
            dialog.dismiss()
        }

        imgCalculator.setColorFilter(resources.getColor(R.color.txtColor))
        txtCheckOut.setTextColor(resources.getColor(R.color.txtColor))
        imgMore.setColorFilter(resources.getColor(R.color.txt_color_blue))
        txtMore.setTextColor(resources.getColor(R.color.txt_color_blue))

        imgClose.setOnClickListener {
            closeDialog(dialog)
        }




        dialog.show()


    }

    fun closeDialog(dialog: Dialog?) {

        dialog?.dismiss()


    }

    private fun horizontalTabList() {
        val params = binding.rvTabLayout.layoutParams
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
        params.width = 0
        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        var tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = HORIZONTAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout?.adapter?.notifyDataSetChanged()


        val set: ConstraintSet = ConstraintSet()
        set.clone(binding.constraintParent)

        //Category TabList Horizontal View Set
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.START,
            binding.root.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.TOP,
            binding.viewLine.id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM,
            binding.root.id,
            ConstraintSet.BOTTOM
        )
        set.setVerticalBias(binding.rvTabLayout.id, 0F)


        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(), 8)
        //CategoryList RecyclerView View Set
        val params1 = binding.rvTabLayout.layoutParams
        params1.height = 0
        params1.width = LinearLayout.LayoutParams.WRAP_CONTENT
        binding.rvTabLayout.layoutParams = params1

        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.TOP,
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.START,
            binding.root.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )

        set.applyTo(binding.constraintParent)

    }

    private fun verticalTabList() {
        var params: ViewGroup.LayoutParams = binding.rvTabLayout.layoutParams
        params.height = 0
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT


        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        var tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = VERTICAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout?.adapter?.notifyDataSetChanged()


        val set: ConstraintSet = ConstraintSet()
        set.clone(binding.constraintParent)

        //Category TabList REcyclerViewTab View Set
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.END,
            binding.linearMenu.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.TOP,
            binding.root.findViewById<View>(R.id.layoutMenu).id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvTabLayout.id,
            ConstraintSet.START,
            binding.rvPagerCategory.id,
            ConstraintSet.END
        )


        //CategoryList REcyclerView View Set
        val params1 = binding.rvTabLayout.layoutParams
        params1.height = 0
        params1.width = LinearLayout.LayoutParams.WRAP_CONTENT
        binding.rvTabLayout.layoutParams = params1

        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(), 6)


        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.START,
            binding.constraintParent.id,
            ConstraintSet.START
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.TOP,
            binding.root.findViewById<View>(R.id.layoutMenu).id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.BOTTOM,
            binding.root.findViewById<View>(R.id.footer).id,
            ConstraintSet.TOP
        )
        set.connect(
            binding.rvPagerCategory.id,
            ConstraintSet.END,
            binding.rvTabLayout.id,
            ConstraintSet.START
        )


        set.applyTo(binding.constraintParent)

    }

    private fun configureDrawer() {
        binding.layoutMenu.txtKeypad.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }


    }

    private fun setVenueData() {
        viewModel.venueDataLocal.observe(viewLifecycleOwner,
            {
                when (it.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        val tbCategory = it.data
                        if (tbCategory != null) {

                            categoryList1 = tbCategory as MutableList<CategoryWithInventory>

                            tabList.clear()
                            for (i in 0 until categoryList1.size) {

                                if (i == 0) {
                                    tabList.add(
                                        CategoryTabModel(
                                            categoryList1.get(i).category.id,
                                            categoryList1.get(i).category.name,
                                            true,
                                            0
                                        )
                                    )
                                } else {
                                    tabList.add(
                                        CategoryTabModel(
                                            categoryList1.get(i).category.id,
                                            categoryList1.get(i).category.name,
                                            false,
                                            0
                                        )
                                    )
                                }
                            }
                            if (categoryList1.isNotEmpty()) {
                                categoryList1.forEach {
                                    categoryTabsList.add(it.category.name)

                                    binding.rvTabLayout.adapter = CategoryTabAdapter1(
                                        requireContext(),
                                        tabList,
                                        object : CategoryTabAdapter1.TabListner {
                                            override fun onTabSelected(pos: Int) {
                                                Log.e("CatTab", "CatTab $pos")
                                                val listCategories =
                                                    (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list
                                                listCategories.clear()
                                                listCategories.add(
                                                    0,
                                                    TbItem()
                                                )

                                                categoryList1[pos].inventoryLists?.let { it1 ->
                                                    listCategories.addAll(
                                                        it1
                                                    )
                                                }
                                                (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list =
                                                    listCategories

                                                binding.rvPagerCategory.adapter?.notifyDataSetChanged()


                                            }
                                        })
                                    itemList1.clear()
                                    itemList1.add(
                                        0,
                                        TbItem()
                                    )

                                    categoryList1[0].inventoryLists?.let { it1 ->
                                        itemList1.addAll(
                                            it1
                                        )
                                    }

                                    searchCategory()
                                    binding.rvPagerCategory.adapter =
                                        CategoryItemAdapter1(requireContext(), itemList1, object :
                                            CategoryItemAdapter1.CategoryItemList {
                                            override fun onClick() {

                                            }

                                            override fun onClickedCreateItem() {
                                                findNavController().navigate(R.id.action_dashboardCategoryNew_to_createItem)
                                            }

                                        })
                                }
                            }

                        }
                    }
                    Status.ERROR ->
                        ProgressUtils.dismissProgressDialog()

                    Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

                }
            })


//        viewModel.venueData.observe(viewLifecycleOwner, {
//            it?.let { resource ->
//                when (resource.status) {
//                    Status.SUCCESS -> {
//
//                        ProgressUtils.dismissProgressDialog()
//
//                        resource.data?.let { category ->
//
//                            categoryList = category.data.categories.toMutableList()
//
//                            tabList.clear()
//                            for (i in 0 until categoryList.size) {
//
//                                if (i == 0) {
//                                    tabList.add(
//                                        CategoryTabModel(
//                                            0,
//                                            categoryList.get(i).name,
//                                            true,
//                                            0
//                                        )
//                                    )
//                                } else {
//                                    tabList.add(
//                                        CategoryTabModel(
//                                            0,
//                                            categoryList.get(i).name,
//                                            false,
//                                            0
//                                        )
//                                    )
//                                }
//                            }
//                            if (categoryList.isNotEmpty()) {
//                                categoryList.forEach {
//                                    categoryTabsList.add(it.name)
//
//                                    binding.rvTabLayout.adapter = CategoryTabAdapter(
//                                        requireContext(),
//                                        tabList,
//                                        object : CategoryTabAdapter.TabListner {
//                                            override fun onTabSelected(pos: Int) {
//                                                Log.e("CatTab", "CatTab $pos")
//                                                val listCategories =
//                                                    (binding.rvPagerCategory.adapter as CategoryItemAdapter).list
//                                                listCategories.clear()
//                                                listCategories.add(
//                                                    0,
//                                                    VenueDataResponse.Data.Category.Item(
//                                                        0,
//                                                        0,
//                                                        "",
//                                                        "",
//                                                        0.0,
//                                                        "",
//                                                        "",
//                                                        0,
//                                                        "",
//                                                        "",
//                                                        ""
//                                                    )
//                                                )
//
//                                                Log.e(
//                                                    TAG,
//                                                    "categoryListData  ${categoryList.get(pos).items}"
//                                                )
//
//                                                listCategories.addAll(categoryList.get(pos).items)
//                                                (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list =
//                                                    listCategories
//
//                                                binding.rvPagerCategory?.adapter?.notifyDataSetChanged()
//
//
//                                            }
//                                        })
//                                    itemList.clear()
//                                    itemList.add(
//                                        0,
//                                        VenueDataResponse.Data.Category.Item(
//                                            0,
//                                            0,
//                                            "",
//                                            "",
//                                            0.0,
//                                            "",
//                                            "",
//                                            0,
//                                            "",
//                                            "",
//                                            ""
//                                        )
//                                    )
//
//                                    itemList.addAll(categoryList.get(0).items)
//
//                                    Log.e("CatList", "${Gson().toJson(categoryList.get(0).items)}")
//                                    binding.rvPagerCategory.adapter =
//                                        CategoryItemAdapter(requireContext(), itemList, object :
//                                            CategoryItemAdapter.CategoryItemList {
//                                            override fun onClick() {
//
//                                            }
//
//                                            override fun onClickedCreateItem() {
//                                                findNavController().navigate(R.id.action_dashboardCategoryNew_to_createItem)
//                                            }
//
//                                        })
//                                }
//                            }
//                        }
//                    }
//                    Status.ERROR -> {
//                        ProgressUtils.dismissProgressDialog()
//
//                    }
//                    Status.LOADING -> {
//                        try {
//                            val activity: Activity = requireActivity() as Activity
//                            if (!activity.isFinishing) {
//                                ProgressUtils.showProgressDialog(requireActivity())
//                            }
//                        } catch (ex: Exception) {
//                            ex.printStackTrace()
//                        }
//                    }
//                }
//            }
//        })
    }


    fun showInfoDialog(
        anchorView: View,
        activity: Activity
    ): PopupWindow {

        val popWindow = PopupWindow(activity)
        val binding: PopupDashboardBinding =
            PopupDashboardBinding.inflate(
                LayoutInflater.from(activity),
                activity.window.decorView.findViewById(R.id.content),
                true
            )



        binding.txtHorizontal.setOnClickListener {
            popWindow.dismiss()
            horizontalTabList()
        }
        binding.txtVertical.setOnClickListener {
            popWindow.dismiss()
            verticalTabList()
        }
        popWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        val root = activity.window.decorView.rootView as ViewGroup
        applyDim(root, 0.5f)
        popWindow.contentView = binding.root
        popWindow.isOutsideTouchable = true
        popWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popWindow.isFocusable = true
        /* popWindow.showAsDropDown(
             anchorView,
             -(anchorView.x.toInt() - (anchorView.width / 2)),
             (anchorView.height) - 20
         )

 */        popWindow.showAsDropDown(anchorView, -(anchorView.width), (anchorView.height) - 20)

        popWindow.setOnDismissListener {
            clearDim(root)

        }
        return popWindow
    }

    fun applyDim(parent: ViewGroup, dimAmount: Float) {
        val dim: Drawable = ColorDrawable(Color.BLACK)
        dim.setBounds(0, 0, parent.width, parent.height)
        dim.alpha = (255 * dimAmount).toInt()
        val overlay = parent.overlay
        overlay.add(dim)
    }

    fun clearDim(parent: ViewGroup) {
        val overlay = parent.overlay
        overlay.clear()
    }

    private fun showPopup(view: View) {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.PopupMenuOverlapAnchor)
        val popup = PopupMenu(contextThemeWrapper, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.tab_category_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.menuVertical -> {
                    verticalTabList()
                    true
                }
                R.id.menuHorizontal -> {
                    horizontalTabList()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        var tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list
        Log.e(TAG, "searchTabList  ${Gson().toJson(tabList)}")
        Log.e(TAG, "searchmodel  ${Gson().toJson(model)}")
        for (i in 0 until tabList.size) {

            if (tabList.get(i).id == model.categoryID) {
                tabList.get(i).isSelected = true
                tabPos = i
            } else {
                tabList.get(i).isSelected = false
            }

        }

        // (binding.rvTabLayout.adapter as CategoryTabAdapter1).list.clear()
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()

        var listCategry = arrayListOf<TbItem?>()
        listCategry.add(
            0,
            TbItem()
        )
        categoryList1[tabPos].inventoryLists?.let { it1 ->
            listCategry.addAll(
                it1
            )
        }
        (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list.clear()
        (binding.rvPagerCategory.adapter as CategoryItemAdapter1).list = listCategry
        binding.rvPagerCategory.adapter?.notifyDataSetChanged()


    }
}