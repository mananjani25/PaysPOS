package com.android.pos.ui.fragments.dashboard

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.data.remote.Constants.HORIZONTAL
import com.android.pos.data.remote.Constants.VERTICAL
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.databinding.PopupDashboardBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryItemAdapter
import com.android.pos.ui.adapter.CategoryTabAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategoryNew : Fragment() {
    private lateinit var binding: FragmentDashboardCategoryNewBinding
    private val TAG = "DashboardCategoryNew"

    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList: MutableList<VenueDataResponse.Data.Category> = arrayListOf()
    private var itemList: ArrayList<VenueDataResponse.Data.Category.Item> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardCategoryNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVenueData()
        configureDrawer()
        onClick()
    }

    private fun onClick() {
        binding.layoutMenu.imgOptionMenu.setOnClickListener {
            showPopup(binding.viewPopup)
            // showInfoDialog(binding.layoutMenu.imgOptionMenu, requireActivity())
        }

    }

    private fun horizontalTabList() {
        val params = binding.rvTabLayout.layoutParams
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
        params.width = 0
        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        var tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = HORIZONTAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter).list = tabList
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


        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(),5)
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

        var tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = VERTICAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter).list = tabList
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

        binding.rvPagerCategory.layoutManager = GridLayoutManager(requireContext(),3)


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

        //        viewModel.venueDataLocal.observe(viewLifecycleOwner,
//            {
//                when (it.status) {
//                    Status.SUCCESS -> {
//                        val tbCategory = it.data
//                        if (tbCategory != null) {
//                            Log.e("venueDataLocal", "SUCCESS" + tbCategory.size)
//
//                        }
//                    }
//                    Status.ERROR ->
//                        Log.e("venueDataLocal", "ERROR")
//
//                    Status.LOADING -> Log.e("venueDataLocal", "LOADING")
//
//                }
//            })
        viewModel.venueData.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        ProgressUtils.dismissProgressDialog()

                        resource.data?.let { category ->

                            categoryList = category.data.categories.toMutableList()

                            tabList.clear()
                            for (i in 0 until categoryList.size) {

                                if (i == 0) {
                                    tabList.add(
                                        CategoryTabModel(
                                            0,
                                            categoryList.get(i).name,
                                            true,
                                            0
                                        )
                                    )
                                } else {
                                    tabList.add(
                                        CategoryTabModel(
                                            0,
                                            categoryList.get(i).name,
                                            false,
                                            0
                                        )
                                    )
                                }
                            }
                            if (categoryList.isNotEmpty()) {
                                categoryList.forEach {
                                    categoryTabsList.add(it.name)

                                    binding.rvTabLayout.adapter = CategoryTabAdapter(
                                        requireContext(),
                                        tabList,
                                        object : CategoryTabAdapter.TabListner {
                                            override fun onTabSelected(pos: Int) {
                                                Log.e("CatTab", "CatTab $pos")
                                                var listCategories =
                                                    (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list
                                                listCategories.clear()
                                                listCategories.add(
                                                    0,
                                                    VenueDataResponse.Data.Category.Item(
                                                        0,
                                                        0,
                                                        "",
                                                        "",
                                                        0.0,
                                                        "",
                                                        "",
                                                        0,
                                                        "",
                                                        "",
                                                        ""
                                                    )
                                                )

                                                Log.e(
                                                    TAG,
                                                    "categoryListData  ${categoryList.get(pos).items}"
                                                )

                                                listCategories.addAll(categoryList.get(pos).items)
                                                (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list =
                                                    listCategories

                                                binding.rvPagerCategory?.adapter?.notifyDataSetChanged()


                                            }
                                        })
                                    itemList.clear()
                                    itemList.add(
                                        0,
                                        VenueDataResponse.Data.Category.Item(
                                            0,
                                            0,
                                            "",
                                            "",
                                            0.0,
                                            "",
                                            "",
                                            0,
                                            "",
                                            "",
                                            ""
                                        )
                                    )

                                    itemList.addAll(categoryList.get(0).items)

                                    Log.e("CatList", "${Gson().toJson(categoryList.get(0).items)}")
                                    binding.rvPagerCategory.adapter =
                                        CategoryItemAdapter(requireContext(), itemList, object :
                                            CategoryItemAdapter.CategoryItemList {
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
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()

                    }
                    Status.LOADING -> {
                        try {
                            val activity: Activity = requireActivity() as Activity
                            if (!activity.isFinishing) {
                                ProgressUtils.showProgressDialog(requireActivity())
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        })
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

}