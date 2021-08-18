package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.CategorySearchData
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.CUSTOMER_NAME
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.HORIZONTAL
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.ORDER_TYPE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.remote.Constants.VERTICAL
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.*
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DashboardCategoryNew : Fragment(), CategoryItemAdapter1.CategoryItemList, MyCallback,
    ItemCallback, View.OnClickListener {

    private var assignCustomer: TbCustomer? = null
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var singleItem: TbItem? = null
    private var cartList: List<CartModel> = emptyList()
    private lateinit var binding: FragmentDashboardCategoryNewBinding

    private val TAG = "DashboardCategoryNew"

    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var categoryList1: MutableList<CategoryWithInventory> = arrayListOf()
    private var itemList1: ArrayList<TbItem?> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    private lateinit var searchAdapter: CategorySearchAdapter
    private lateinit var searchList: ArrayList<CategorySearchData>
    private lateinit var cartAdapter: CartAdapter
    private lateinit var orderTypeAdapter: OrderTypeAdapter

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

        hideOrderType()

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
        getOrderTypes()
        getServiceCharges()
        observeShowProgress()
        setupSnackbar()

        binding.layoutCart.llShowMenu.setOnClickListener(this)
        binding.layoutCart.txtCrtNewCustomer.setOnClickListener(this)
        binding.layoutCart.txtClearItems.setOnClickListener(this)
        binding.layoutCart.llInfo.setOnClickListener(this)
        binding.layoutCart.btnPay.setOnClickListener(this)
        binding.layoutCart.llCartMenu.setOnClickListener(this)
        binding.layoutCart.imgOrderMenu.setOnClickListener(this)

        binding.root.setOnClickListener {
            if (binding.layoutCart.llCustomerDialog.visibility == View.VISIBLE) {
                binding.layoutCart.llCustomerDialog.visibility = View.GONE
            }
        }

        binding.root.setOnClickListener {
            if (binding.layoutCart.llOrderMenu.visibility == View.VISIBLE) {
                binding.layoutCart.llOrderMenu.visibility = View.GONE
            }
        }

        if (prefProvider.getValue(CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.layoutCart.txtCustomerName.text =
                prefProvider.getValue(CUSTOMER_NAME, "").toString()
            binding.layoutCart.txtCrtNewCustomer.text = "Remove Customer"
        }


    }

    private fun getOrderTypes() {
        orderTypeAdapter = OrderTypeAdapter()
        orderTypeAdapter.setCallback(this)
        binding.rvOrderType.adapter = orderTypeAdapter

        viewModel.orderTypes().observe(requireActivity(), {
            it.data?.let { it1 -> orderTypeAdapter.addAll(it1) }
        })
    }

    private fun getServiceCharges() {

        viewModel.serviceCharges.observe(requireActivity(), {
            serviceChargesList = it.data

            getCartList()

        })
    }

    private fun hideMenu() {
        if (binding.layoutCart.llCustomerDialog.visibility == View.VISIBLE) {
            binding.layoutCart.llCustomerDialog.visibility = View.GONE
        } else {
            binding.layoutCart.llCustomerDialog.visibility = View.VISIBLE
        }
    }

    private fun hideOrderMenu() {
        if (binding.layoutCart.llOrderMenu.visibility == View.VISIBLE) {
            binding.layoutCart.llOrderMenu.visibility = View.GONE
        } else {
            binding.layoutCart.llOrderMenu.visibility = View.VISIBLE
        }
    }

    private fun getCartList() {

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)
        binding.layoutCart.rvCart.adapter = cartAdapter

        object : SwipeHelper(activity, binding.layoutCart.rvCart) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {
                underlayButtons.add(UnderlayButton(
                    "Add Note",
                    0,
                    Color.parseColor("#FA9905")
                ) { pos ->


                    setFragmentResultListener("request_key_note") { requestKey: String, bundle: Bundle ->
                        val note = bundle.getString("note")

                        singleItem!!.note = note.toString()
                        singleItem?.let { viewModel.cartLogic(cartList, it, UPDATE) }
                    }

                    singleItem = cartAdapter.getItem(pos)
                    val bundle = Bundle().apply {
                        putString("note", singleItem!!.note)
                    }

                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_addNoteDialog,
                        bundle
                    )
                })


                underlayButtons.add(UnderlayButton(
                    "Add Discount",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_addDiscountDialog/*,
                        bundle*/
                    )
                })


                underlayButtons.add(
                    UnderlayButton(
                        "Delete",
                        0,
                        Color.parseColor("#FF3C30")
                    ) { pos ->

                        alert(
                            getString(R.string.app_name),
                            getString(R.string.delete_item_message)
                        ) {
                            positiveButton(getString(R.string.tv_delete)) {
                                // Do positive stuff here
                                val item = cartAdapter.getItem(pos)
                                cartAdapter.removeIitem(pos)
                                viewModel.cartLogic(cartList, item, DELETE)
                            }
                            negativeButton(R.string.tv_cancel) {
                                // Do negative stuff here
                            }
                        }
                    })
            }
        }

        viewModel.mAllWords.observe(
            requireActivity(), {
                cartList = it
                if (cartList.isNotEmpty()) {

                    binding.layoutCart.rvCart.visibility = View.VISIBLE
                    binding.layoutCart.llPayment.visibility = View.VISIBLE
                    cartAdapter.addCart(cartList[0].items)

                    viewModel.itemCalculation(
                        cartList,
                        binding.layoutCart.txtTotalAmount
                    )
                } else {
                    viewModel.itemCalculation(
                        cartList,
                        binding.layoutCart.txtTotalAmount
                    )
                    binding.layoutCart.rvCart.visibility = View.GONE
                    binding.layoutCart.llPayment.visibility = View.GONE
                }
            }
        )

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {

                Log.e("request_key_customer", result.first_name)
                prefProvider.setValue(CUSTOMER_NAME, result.first_name + " " + result.last_name)
                binding.layoutCart.txtCustomerName.text = result.first_name + " " + result.last_name
                binding.layoutCart.txtCrtNewCustomer.text = "Remove Customer"
                assignCustomer = result
            }
        }
    }

    private fun hideOrderType() {

        if (prefProvider.getValueInt(ORDER_TYPE_ID, -1) != -1) {

            binding.layoutCart.llCart.visibility = View.VISIBLE
            binding.lltakeout.visibility = View.GONE

        } else {
            binding.lltakeout.visibility = View.VISIBLE
            binding.layoutCart.llCart.visibility = View.GONE
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
            showPopup(binding.layoutMenu.imgOptionMenu)
        }

        binding.footer.linearMore.setOnClickListener {
            dialogPOSMenu()

        }

    }

    private fun dialogPOSMenu() {

        val dialog = Dialog(requireContext(), android.R.style.Theme_Light)

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

        linearOrders.setOnClickListener {

            findNavController().navigate(R.id.action_dashboardCategoryNew_to_orders)
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

    private fun closeDialog(dialog: Dialog?) {
        dialog?.dismiss()
    }

    private fun horizontalTabList() {
        val params = binding.rvTabLayout.layoutParams
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
        params.width = 0
        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = HORIZONTAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()


        val set = ConstraintSet()
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
        val params: ViewGroup.LayoutParams = binding.rvTabLayout.layoutParams
        params.height = 0
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT


        binding.rvTabLayout.layoutParams = params
        binding.rvTabLayout.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list

        for (i in 0 until tabList.size) {
            tabList.get(i).type = VERTICAL

        }
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()


        val set = ConstraintSet()
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
            findNavController().navigate(R.id.action_dashboardCategoryNew_to_manualSales)
            // (requireActivity() as MainActivity).enableDrawer()
        }


    }

    private fun setVenueData() {
        viewModel.venueDataLocal().observe(
            viewLifecycleOwner,
            {
                when (it.status) {
                    Status.SUCCESS -> {

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
                                        CategoryItemAdapter1(requireContext(), itemList1, this)
                                }
                            }

                        }

                        ProgressUtils.dismissProgressDialog()
                    }
                    Status.ERROR ->
                        ProgressUtils.dismissProgressDialog()

                    Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

                }
            })

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


    @SuppressLint("SetTextI18n")
    private fun showPopupWindow(view: View) {

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window, null)

        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)

        txtSubTotal.text = "$" + String.format(
            "%.2f",
            viewModel.subTotalPrice
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            viewModel.totalServiceCharge
        )
        txtDiscount.text = "$" + String.format(
            "%.2f",
            viewModel.totalDiscount
        )
        txtTotalAmount.text = binding.layoutCart.txtTotalAmount.text.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            viewModel.totalTax
        )

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.setBackgroundDrawable(BitmapDrawable())
        popupWindow.isOutsideTouchable = true


        popupWindow.setOnDismissListener(PopupWindow.OnDismissListener {
            //TODO do sth here on dismiss
        })
        popupWindow.showAtLocation(view, Gravity.TOP, 600, 650);
    }

    private fun resetTabbySearch(model: CategorySearchData) {
        var tabPos = -1
        val tabList = (binding.rvTabLayout.adapter as CategoryTabAdapter1).list
        Log.e(TAG, "searchTabList  ${Gson().toJson(tabList)}")
        Log.e(TAG, "searchmodel  ${Gson().toJson(model)}")
        for (i in 0 until tabList.size) {

            if (tabList[i].id == model.categoryID) {
                tabList[i].isSelected = true
                tabPos = i
            } else {
                tabList.get(i).isSelected = false
            }

        }

        // (binding.rvTabLayout.adapter as CategoryTabAdapter1).list.clear()
        (binding.rvTabLayout.adapter as CategoryTabAdapter1).list = tabList
        binding.rvTabLayout.adapter?.notifyDataSetChanged()

        val listCategry = arrayListOf<TbItem?>()
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

    override fun onClick(item: TbItem) {

        if (item.modifier_set_ids.isEmpty()) {
            item.itemQuantity = item.itemQuantity + 1
            if (cartList.isEmpty()) {
                viewModel.setServiceCharges(serviceChargesList)
            }

            viewModel.cartLogic(cartList, item, ADD)
        } else {

            ItemPopup(item, true)
        }

    }

    override fun onClickedCreateItem() {
        findNavController().navigate(R.id.action_dashboardCategoryNew_to_createItem)
    }

    @SuppressLint("SetTextI18n")
    override fun onItemClickListener(view: View?, data: TbItem) {

        ItemPopup(data, false)
    }

    private fun ItemPopup(data: TbItem, isItemClick: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = lp

        dialog.setContentView(R.layout.dialog_update_quantity)

        val imgClose: AppCompatImageView = dialog.findViewById(R.id.imgBack)
        val txtSave: AppCompatTextView = dialog.findViewById(R.id.txtSave)
        val txtTitle: AppCompatTextView = dialog.findViewById(R.id.txtTitle)
        val txtQty: AppCompatEditText = dialog.findViewById(R.id.txtQty)
        val llPlus: LinearLayoutCompat = dialog.findViewById(R.id.llPlus)
        val llMinus: LinearLayoutCompat = dialog.findViewById(R.id.llMinus)
        val btnRemove: AppCompatTextView = dialog.findViewById(R.id.btnRemove)
        val btnAddDiscount: AppCompatTextView = dialog.findViewById(R.id.btnAddDiscount)
        val edtNote: AppCompatEditText = dialog.findViewById(R.id.edtNote)
        val rvModifierSets: RecyclerView = dialog.findViewById(R.id.rvModifierSets)
        val txtCustomItemName: AppCompatTextView = dialog.findViewById(R.id.txtCustomItemName)
        val edtItemName: AppCompatEditText = dialog.findViewById(R.id.edtItemName)

        edtItemName.visibility = View.GONE
        txtCustomItemName.visibility = View.GONE
        var discountPrice = data.discountPrice / data.itemQuantity


        var adapter: ItemModifierSetAdapter? = null

        var qty = data.itemQuantity
        if (isItemClick) {
            qty = 1
            txtQty.setText(qty.toString())
            btnRemove.visibility = View.GONE
            btnAddDiscount.visibility = View.GONE
        }

        if (data.modifier_set_ids.isNotEmpty()) {
            adapter = ItemModifierSetAdapter()
            rvModifierSets.adapter = adapter

            val intArray = IntArray(data.modifier_set_ids.size) { i ->
                data.modifier_set_ids[i]
            }
            viewModel.modifierSet(intArray).observe(requireActivity(), {
                if (it.data != null && it.data.isNotEmpty()) {
                    rvModifierSets.visibility = View.VISIBLE
                    it.data.let { it1 -> adapter.add(it1) }

                    if (!isItemClick) {
                        adapter.setData(data.modifiers)
                    }

                } else rvModifierSets.visibility = View.GONE

            })
        }

        edtNote.setText(data.note)



        txtQty.setText(qty.toString())

        if (data.discountPrice != 0.0) {
            txtTitle.text = data.name + "  $" + String.format(
                "%.2f",
                (totalPrice(data) - data.discountPrice)
            )
        } else {
            txtTitle.text = data.name + "  $" + String.format(
                "%.2f",
                totalPrice(data)
            )
        }
        /*txtTitle.text = data.name + "  $" + String.format(
            "%.2f",
            data.price
        )*/

        imgClose.setOnClickListener {
            dialog.dismiss()
        }
        txtSave.setOnClickListener {


            if (minMaxValidationCheck(adapter)) {
                dialog.dismiss()

                data.note = edtNote.text.toString().trim()
                data.itemQuantity = txtQty.text.toString().toInt()
                if (!isItemClick) {
                    data.discountPrice = (discountPrice * txtQty.text.toString().toInt())
                }


                val modifiers = adapter?.getSelectedModifiers()
                if (modifiers != null) {
                    modifiers.forEach {
                        it.itemQuantity = data.itemQuantity
                    }
                    data.modifiers = modifiers
                }

                if (cartList.isEmpty()) {
                    viewModel.setServiceCharges(serviceChargesList)
                }

                if (isItemClick) {
                    viewModel.cartLogic(cartList, data, ADD)
                } else
                    viewModel.cartLogic(cartList, data, UPDATE)
            } else {
                AlertUtils.showCustomAlert(
                    binding.root.context,
                    binding.root.context.getString(R.string.you_can_add)
                )

            }
        }

        llPlus.setOnClickListener {
            qty += 1
            txtQty.setText(qty.toString())


        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            txtQty.setText(qty.toString())


        }
        btnRemove.setOnClickListener {


            viewModel.cartLogic(cartList, data, DELETE)
            dialog.dismiss()
        }
        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    Log.e(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                        data.discountPrice = calculateDiscountPercentage(
                            totalPrice(data),
                            result.percentage
                        )
                        discountPrice = data.discountPrice
                        data.discountId = result.id
                        data.discountType = result.discountType
                        data.isManualSales = false
                        Log.e(TAG, "insideDiscountmodel:  ${Gson().toJson(data)}")
                        //   viewModel.cartLogic(cartList, data, Constants.UPDATE)
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
                            (totalPrice(data) - data.discountPrice)
                        )

                    } else if (data.price > result.percentage) {

                        data.discountPrice = result.percentage
                        data.discountId = 0
                        data.discountType = result.discountType
                        data.isManualSales = false
                        discountPrice = data.discountPrice

                        //viewModel.cartLogic(cartList, data, Constants.UPDATE)
                        txtTitle.text = data.name + "  $" + String.format(
                            "%.2f",
                            (totalPrice(data) - data.discountPrice)
                        )
                    }
                    else{
                      /*  data.discountPrice = 0.0
                        data.discountType = ""
                        data.isManualSales = false
                        data.discountId = 0
                        discountPrice = data.discountPrice*/

                    }

                } else {
                    data.discountPrice = 0.0
                    data.discountType = ""
                    data.isManualSales = false
                    data.discountId = 0
                    discountPrice = data.discountPrice
                    // viewModel.cartLogic(cartList, data, Constants.UPDATE)
                }

            }


            val bundle = Bundle().apply {
                putBoolean("isFromDetails", true)
                putParcelable("model", data)
            }

            findNavController().navigate(
                R.id.action_dashboardCategoryNew_to_addDiscountDialog,
                bundle
            )
        }


        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun minMaxValidationCheck(adapter: ItemModifierSetAdapter?): Boolean {
        adapter?.getAll()?.forEach {
            return (it.min_required == 0) || minLogic(
                it.min_required,
                it.modifiers
            )
        }
        return true
    }

    private fun minLogic(
        maxCount: Int,
        modifiers: List<Modifier>
    ): Boolean {

        if (maxCount == 0) {
            return true
        }
        var totalMinMax = 0

        modifiers.forEach {
            if (it.isChecked) {
                totalMinMax += 1
            }
        }

        return maxCount <= totalMinMax
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val orderType = orderTypeAdapter.getItem(pos)
        prefProvider.setValueInt(ORDER_TYPE_ID, orderType.id)
        prefProvider.setValue(ORDER_TYPE_NAME, orderType.name)

        hideOrderType()
    }

    override fun onClick(v: View?) {

        when (v?.id) {

            R.id.llShowMenu -> {
                hideMenu()
            }
            R.id.imgOrderMenu -> {
                hideOrderMenu()
            }
            R.id.txtCrtNewCustomer -> {
                if (prefProvider.getValue(CUSTOMER_NAME, "").toString().isNotEmpty()) {
                    binding.layoutCart.txtCrtNewCustomer.text = "Add Customer"
                    binding.layoutCart.txtCustomerName.text = "Add Customer"
                    prefProvider.setValue(CUSTOMER_NAME, "")
                } else {
                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_assignCustomerOrderFragment
                    )
                }

            }
            R.id.txtClearItems -> {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_items_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        // Do positive stuff here
                        viewModel.deleteCart()

                        hideOrderMenu()
                    }
                    negativeButton(R.string.tv_cancel) {
                        // Do negative stuff here
                    }
                }

            }

            R.id.llInfo -> {
                showPopupWindow(v)
            }

            R.id.btnPay -> {

                if (cartList.isNotEmpty()) {
                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", viewModel.totalPrice)
                    bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                    bundle.putDouble("totalTax", viewModel.totalTax)
                    bundle.putDouble("totalDiscount", viewModel.totalDiscount)
                    bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                    cartList[0].customer = assignCustomer
                    bundle.putParcelable("cartList", cartList[0])

                    findNavController().navigate(
                        R.id.action_dashboardCategoryNew_to_paymentFragment,
                        bundle
                    )
                }
            }
            R.id.llCartMenu -> {

            }
        }
    }

    fun calculateDiscountPercentage(originalPrice: Double, percentage: Double): Double {
        val disPrice = Math.round((originalPrice * percentage) / 100).toDouble()
        Log.e(TAG, "disPrice  ${disPrice}")
        return if (disPrice < originalPrice) {
            disPrice
        } else {
            0.0
        }


    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }


}