package com.android.pos.ui.fragments.manualsales

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.CUSTOMER_NAME
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.LOYALTY_ADDED
import com.android.pos.data.remote.Constants.MANUALSALE
import com.android.pos.data.remote.Constants.MANUAL_SALE_CATEGORY_ID
import com.android.pos.data.remote.Constants.MANUAL_SALE_ITEM_ID
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentManualSaleNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.ManualSaleCartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ManualSaleOptionsCustomCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManualSaleNew : Fragment(), ManualSaleCartAdapter.ManualSaleInterface,
    ManualSaleOptionsCustomCallback {

    private var mPostion: Int = 0
    private var manualItemId: Int = 0
    private var manualCategoryId: Int = 0
    private lateinit var nameObserver: Observer<List<CartModel>>
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"
    private var cartList: List<CartModel>? = null
    private lateinit var cartAdapter: ManualSaleCartAdapter
    private var cartItemModel = TbItem()
    private val viewModel by viewModels<ManualSaleViewModel>()
    var amountToBepaid = 0.0
    var totalquantity = 0

    @Inject
    lateinit var rolePermission: RolePermission
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var discountList: List<TbDiscount>? = null
    private var taxList: List<TaxData>? = null
    private var assignCustomer: TbCustomer? = null
    private var isPayClicked: Boolean = false
    var tabItemMOdel = TbItem()

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSaleNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        getLoyaltyPrograms()
        getServiceCharge()
        Log.e(TAG, "CategoryId: ${prefProvider.getValueInt(MANUAL_SALE_CATEGORY_ID, 1)}")
        Log.e(TAG, "CategoryItemId: ${prefProvider.getValueInt(MANUAL_SALE_ITEM_ID, 1)}")
        Log.e(TAG, "cartDetails: $arguments")

        getDiscountList()


        return binding.root
    }

    private fun getDiscountList() {
        dashboardViewModel.discountList.observe(requireActivity(), {

            if (it.data != null)
                discountList = it.data
        })
    }


    private fun getTaxList() {
        dashboardViewModel.taxList.observe(requireActivity(), {
            if (it.data != null)
                taxList = it.data
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgSearch.visibility = View.GONE
        binding.layoutMenu.autoSearch.visibility = View.GONE
        binding.layoutMenu.imgSync.visibility = View.GONE
        binding.layoutMenu.imgOptionMenu.visibility = View.GONE
        viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
            prefProvider.getValueboolean(LOYALTY_ADDED, false)
        //    prefProvider.setValue(CUSTOMER_NAME, "")

        binding.footer.txtEmployeeName.text =
            prefProvider.getValue(Constants.EMPLOYEE_NAME, "").toString()

        getManualCategoryId()
        onConfig()
        onClickKeypad()
        getCartList()
        getTaxList()
        onClick()
        listner()
        callbackForDialog()
        binding.footer.linearEmpnameRole.setOnClickListener {
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(R.id.action_manualSaleNew_to_passcode, bundle)
        }

        binding.footer.linearClockout.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_reportEODFragment)
        }

        binding.layoutMenu.txtProducts.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun getManualCategoryId() {

        viewModel.returnedVal.observe(viewLifecycleOwner) {

            if (it != null) {
                manualCategoryId = it.id
                manualItemId = it.item_ids[0]
            }

        }
    }

    private fun getLoyaltyPrograms() {
        Log.e("Loyalty", "getLoyaltyPrograms called..")
        viewModel.activeLoyaltyProgram = prefProvider.getActiveLoyaltyData()
        viewModel.activeLoyaltyProgramLiveData.observe(requireActivity()) {
            if (it.data != null) {
                Log.e("Loyalty", "getLoyaltyPrograms fetched..")
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            }
        }
    }

    private fun getCartList() {

        if (isAdded)
            viewModel.cartList(prefProvider.getValueInt(EMPLOYEE_ID, 0))
                .observe(requireActivity()) {
                    cartList = it
                    Log.e(TAG, "cartListBeforeTax  ${Gson().toJson(cartList)}")
                    if (cartList?.isNotEmpty()!!) {
                        cartList?.get(0)?.items?.forEach {
                            it.taxes = taxList
                        }
                        cartAdapter.setList(cartList?.get(0)?.items)

                        viewModel.itemCalculation(
                            cartList?.get(0)?.items,
                            binding.txtTotalAmount
                        )
                    } else {

                        cartAdapter.clearList()

                        viewModel.itemCalculation(
                            null,
                            binding.txtTotalAmount
                        )
                    }


                }
    }

    private fun getServiceCharge() {
        viewModel.serviceCharge.observe(requireActivity(), {
            if (it.data != null)
                serviceChargesList = it.data
        })
    }

    private fun listner() {
        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                setUpCustomer(result)
            }
        }
    }


    private fun setUpCustomer(customer: TbCustomer?) {
        //set customer
        if (customer != null) {
            viewModel.selectedCustomer = customer
            assignCustomer = customer
            prefProvider.saveCustomerData(customer)
            prefProvider.setValue(CUSTOMER_NAME, customer.first_name + " " + customer.last_name)
            customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

            binding.txtCustomerName.text = customer.first_name + " " + customer.last_name
            binding.txtCrtNewCustomer.text = "Remove Customer"

            //set loyalty
            if (viewModel.loyaltyPointCondition(customer)) {
                binding.txtLoyaltyPoints.visible()
                "${getString(R.string.loyalty_points)}: ${customer.final_reward}".also {
                    binding.txtLoyaltyPoints.text = it
                }
            } else {
                binding.txtLoyaltyPoints.gone()
            }
            refreshItemCalculation()
        } else {
            clearCustomer()
            refreshItemCalculation()
        }
    }

    private fun clearCustomer() {
        viewModel.selectedCustomer = null
        assignCustomer = null
        prefProvider.saveCustomerData(null)
        prefProvider.setValue(CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
        binding.txtCustomerName.text = "Add Customer"
        binding.txtCrtNewCustomer.text = "Add Customer"
        binding.txtLoyaltyPoints.gone()
        refreshItemCalculation()
    }

    private fun <TbItem> merge(first: List<TbItem>, second: List<TbItem>): List<TbItem> {
        return first + second
    }

    private fun onClick() {
        var mainCartList: ArrayList<CartModel>

        nameObserver = Observer<List<CartModel>> {


            val bundle = Bundle()
            if (isPayClicked && cartList?.isNotEmpty() == true) {
                Log.e(
                    "!_@_",
                    "Total Price: ${viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0}"
                )
                bundle.putDouble(
                    "totalPrice",
                    viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
                )
                bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                bundle.putDouble("totalTax", viewModel.totalTax)
                bundle.putDouble("totalDiscount", viewModel.totalDiscount)
                bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                cartList?.get(0)?.customer = assignCustomer
                bundle.putParcelable("cartList", cartList?.get(0))
                Log.e(TAG, "cartListManualSale  ${Gson().toJson(cartList?.get(0))}")
                bundle.putString(
                    "redeemLoyalty",
                    Gson().toJson(viewModel.redeemLoyaltyInfo)
                )
                var finaltotal = viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
                prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, finaltotal.toString())
                prefProvider.setValue(
                    Constants.SUB_TOTAL_ACTUAL,
                    viewModel.subTotalPrice.toString()
                )
                prefProvider.setValue(
                    Constants.TOTAL_DISCOUNT_ACTUAL,
                    viewModel.totalDiscount.toString()
                )
                prefProvider.setValue(
                    Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
                    viewModel.totalServiceCharge.toString()
                )
                prefProvider.setValue(
                    Constants.TAX_CHARGE_ACTUAL,
                    viewModel.totalTax.toString()
                )
                prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")

            }
            if (it != null && it.isNotEmpty()) {


                mainCartList = it as ArrayList<CartModel>

                if (cartList != null && cartList!!.isNotEmpty()) {


                    val manualItems = cartList!![0].items
                    val mainItems = mainCartList[0].items

                    val mergeItems = merge(mainItems!!, manualItems!!)

                    mainCartList[0].items = mergeItems

                    dashboardViewModel.addCart(mainCartList[0])

                    viewModel.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID,0))

                    dashboardViewModel.mAllWords(
                        prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                        prefProvider.getValueInt(
                            Constants.EMPLOYEE_ID, 0
                        )
                    ).removeObserver(nameObserver)

                    if (isPayClicked && viewModel.totalPrice != 0.0) {
                        findNavController().navigate(
                            R.id.action_manualSaleNew_to_paymentFragment,
                            bundle
                        )

                    } else {

                        val navControll = findNavController()
                        val bundle = Bundle()
                        bundle.putString("manualSale", MANUALSALE)
                        arguments?.getString("headerPosition")?.toInt()?.let { it1 ->
                            bundle.putInt("headerPosition", it1)
                        }
                        bundle.putString("tabItem", Gson().toJson(tabItemMOdel))
                        bundle.putParcelableArrayList(
                            "guestsList",
                            arguments?.getParcelableArrayList("guestsList")
                        )
                        navControll.previousBackStackEntry?.savedStateHandle?.set(KEY, bundle)
                        navControll.popBackStack()

/*                        val navControll = findNavController()
                        navControll.previousBackStackEntry?.savedStateHandle?.set(
                            KEY,
                            MANUALSALE
                        )
                        navControll.popBackStack()*/
                    }

                }
            } else {

                if (cartList != null && cartList!!.isNotEmpty()) {
                    cartList?.forEach { it ->
                        it.isMaual = false
                        it.orderType = prefProvider.getValue(Constants.ORDER_TYPE, "").toString()

                    }
                    viewModel.saveManualSaleData(cartList!!)
                }

                if (isPayClicked && viewModel.totalPrice != 0.0) {
                    findNavController().navigate(
                        R.id.action_manualSaleNew_to_paymentFragment,
                        bundle
                    )

                } else {
                    val navControll = findNavController()
                    navControll.previousBackStackEntry?.savedStateHandle?.set(
                        Constants.KEY,
                        Constants.MANUALSALE
                    )
                    navControll.popBackStack()
                }
            }

        }

        binding.txtSave.setOnClickListener {
            if (cartList?.isNotEmpty() == true) {
                dashboardViewModel.mAllWords(
                    prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT).toString(),
                    prefProvider.getValueInt(
                        Constants.EMPLOYEE_ID, 0
                    )
                ).observe(
                    viewLifecycleOwner, nameObserver
                )
            }
        }

        binding.imgOrderMenu.setOnClickListener {
            hideClearCart()
        }

        binding.txtClearCart.setOnClickListener {
            if (cartList?.isNotEmpty() == true) {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_items_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        viewModel.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
                        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                        binding.txtTotalAmount.text = "$0.00"

                    }
                    negativeButton(R.string.tv_cancel) {

                    }
                }
                hideClearCart()
            }
        }
        binding.relAddCustomer.setOnClickListener {
            dialogMenu()
        }

        binding.layoutMenu.txtProducts.setOnClickListener {
            if (cartList?.isNotEmpty() == true) {
                dashboardViewModel.mAllWords(
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    prefProvider.getValueInt(
                        Constants.EMPLOYEE_ID, 0
                    )
                ).observe(
                    viewLifecycleOwner, nameObserver
                )
            }
        }

        binding.btnPay.setOnClickListener {

            if (binding.txtTotalAmount.text.toString() != "$0.00" && cartList?.isNotEmpty() == true) {
                /*  isPayClicked = true

                  dashboardViewModel.mAllWords(
                      prefProvider.getValue(Constants.ORDER_TYPE, "").toString()
                  ).observe(
                      viewLifecycleOwner, nameObserver
                  )
  */
                val bundle = Bundle()
                Log.e(
                    "!_@_",
                    "Total Price: ${viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0}"
                )
                bundle.putDouble(
                    "totalPrice",
                    viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
                )
                bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                bundle.putDouble("totalTax", viewModel.totalTax)
                bundle.putDouble("totalDiscount", viewModel.totalDiscount)
                bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                cartList?.get(0)?.customer = assignCustomer
                bundle.putParcelable("cartList", cartList?.get(0))
                bundle.putString(
                    "redeemLoyalty",
                    Gson().toJson(viewModel.redeemLoyaltyInfo)
                )

                findNavController().navigate(R.id.action_manualSaleNew_to_paymentFragment, bundle)
            }
        }

        binding.footer.linearMore.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_menuFragment)
            //dialogPOSMenu()
        }

        binding.footer.linearTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_manualSaleNew_to_transactionFragment)
            }
        }
        binding.footer.linearOpenOrders.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_orders)
        }
        binding.llInfo.setOnClickListener {
            showPopupWindow(it)
        }

        binding.txtCrtNewCustomer.setOnClickListener {
            if (prefProvider.getValue(CUSTOMER_NAME, "").toString().isNotEmpty()) {
                clearCustomer()
            } else {
                findNavController().navigate(R.id.action_manualSaleNew_to_assignCustomerOrderFragment)


            }

        }

        binding.txtClearItems.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.delete_items_message)
            ) {
                positiveButton(getString(R.string.tv_delete)) {
                    // Do positive stuff here
                    viewModel.deleteCart(prefProvider.getValueInt(EMPLOYEE_ID, 0))
                    binding.txtTotalAmount.setText("$0.00")
                    //resetCart()
                    dialogMenu()

                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }

        }

        binding.root.setOnClickListener {
            if (binding.llCustomerDialog.visibility == View.VISIBLE) {
                binding.llCustomerDialog.visibility = View.GONE
            }
            if (binding.llOrderMenu.visibility == View.VISIBLE) {
                binding.llOrderMenu.visibility = View.GONE
            }

        }
    }

    private fun onClickKeypad() {
        binding.llKeypad.manualKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.manualKeypad.tvTwo.setOnClickListener {

            calculateValue("2", false)
        }
        binding.llKeypad.manualKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)
        }
        binding.llKeypad.manualKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }
        binding.llKeypad.manualKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }
        binding.llKeypad.manualKeypad.tvSix.setOnClickListener {

            calculateValue("6", false)
        }
        binding.llKeypad.manualKeypad.tvSeven.setOnClickListener {

            calculateValue("7", false)
        }
        binding.llKeypad.manualKeypad.tvEight.setOnClickListener {

            calculateValue("8", false)
        }
        binding.llKeypad.manualKeypad.tvNine.setOnClickListener {

            calculateValue("9", false)
        }
        binding.llKeypad.manualKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)

        }
        binding.llKeypad.manualKeypad.imgAdd.setOnClickListener {
            // binding.txtAmount.setText( "0.00")

            if (!(binding.llKeypad.txtAmount.text!!.trim().toString()
                    .equals("0.00")) && (!(binding.llKeypad.txtAmount.text!!.trim().toString()
                    .equals("$0.00"))) && (!binding.llKeypad.txtAmount.text!!.trim().toString()
                    .equals("0"))
            ) {
                addItemToCart(binding.llKeypad.txtAmount.text.toString(), true)
                binding.llKeypad.txtAmount.setText("0.00")
            }

            binding.txtSubTotal.text = "$" + String.format(
                "%.2f",
                viewModel.subTotalPrice
            )
            binding.txtServiceCharge.text = "$" + String.format(
                "%.2f",
                viewModel.totalServiceCharge
            )
            binding.txtDiscount.text = "- $" + String.format(
                "%.2f",
                viewModel.totalDiscount
            )
            //txtTotalAmount.text = binding.txtTotalAmount.text.toString()
            binding.txtTotalTax.text = "$" + String.format(
                "%.2f",
                viewModel.totalTax
            )
            binding.txtTotal.text = "$" + String.format(
                "%.2f",
                viewModel.totalPrice
            )
            binding.tvDiscount.text = "$" + String.format(
                "%.2f",
                viewModel.totalDiscount
            )
        }
        binding.llKeypad.manualKeypad.tvBack.setOnClickListener {
            if (binding.llKeypad.txtAmount.text.toString().isNotEmpty()) {
                calculateValue("", true)
            }

        }
    }

    private fun addItemToCart(price: String, isAdd: Boolean) {
        val replaceCurrency = price.replace("$", "")
        Log.e(TAG, "replaceCurrency  ${replaceCurrency}")
        var count = 0
        if (isAdd) {
            tabItemMOdel = TbItem()
            var count = 0


            if (cartList?.size != 0) {


                cartList?.get(0)?.items?.let {

                    if (it.isNotEmpty()) {
                        count =
                            it.get(cartList?.get(0)?.items!!.size - 1).customItemCount
                    } else {
                        count = -1
                    }
                }

            }
            count++
            tabItemMOdel.customItemCount = count
            tabItemMOdel.name = "Custom Item ${count}"
            if (binding.llKeypad.edtItemName.text?.isNotEmpty() == true) {
                tabItemMOdel.name = binding.llKeypad.edtItemName.text.toString()
                count--
                tabItemMOdel.customItemCount = count
            }

            tabItemMOdel.price = replaceCurrency.toDouble()
            tabItemMOdel.itemQuantity = 1
            tabItemMOdel.discountPrice = 0.0
            tabItemMOdel.isManualSales = true

            if (cartAdapter.getList().isEmpty()) {
                tabItemMOdel.customItemID = 1
            } else {

                var id = cartAdapter.getItem(cartAdapter.getList().size - 1).customItemID
                id++

                tabItemMOdel.customItemID = id

            }

            tabItemMOdel.itemId = manualItemId
            tabItemMOdel.categoryId = manualCategoryId
            dashboardViewModel.ordertypelist.forEach {
                if (it.orderType == Constants.TAKEOUT)
                    tabItemMOdel.orderItemId = it.id
            }

            viewModel.cartLogic(cartList, tabItemMOdel, ADD)
            binding.llKeypad.edtItemName.text?.clear()

        }

    }

    private fun callbackForDialog() {
        setFragmentResultListener("request_key_item_rename") { resultKey: String, bundle: Bundle ->
            val data = bundle.getString("item_name")
            cartItemModel.name = data.toString()
            cartItemModel?.let {
                viewModel.cartLogic(cartList, it, Constants.UPDATE)
            }


        }
        setFragmentResultListener("request_key_note") { requestKey: String, bundle: Bundle ->
            val note = bundle.getString("note")

            cartItemModel.note = note.toString()

            cartItemModel?.let {
                viewModel.cartLogic(
                    cartList,
                    it,
                    Constants.UPDATE
                )
            }
        }
        setFragmentResultListener("request_key_discount") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            if (result != null) {
                val pos = mPostion
                Log.e(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                if (result.discountType == requireContext().getString(R.string.disc_percentage)) {
                    val cartModel = cartAdapter.getItem(pos)
                    cartModel.discountPrice = calculateDiscountPercentage(
                        cartAdapter.getItem(pos).price,
                        result.percentage
                    )
                    cartModel.discountId = result.id
                    cartModel.discountType = result.discountType
                    cartModel.isDiscountDefault = true

                    Log.e(TAG, "cartModelPArseMsd   ${Gson().toJson(cartModel)}")
                    viewModel.cartLogic(cartList, cartModel, Constants.UPDATE)

                } else if (result.discountType == "Amount") {
                    val cartModel = cartAdapter.getItem(pos)
                    cartModel.discountPrice = result.percentage
                    cartModel.isDiscountDefault = false
                    cartModel.discountType = result.discountType
                    viewModel.cartLogic(cartList, cartModel, Constants.UPDATE)



                    Log.e(TAG, "DiscountInDollar")
                } else {
                    val cartModel = cartAdapter.getItem(pos)
                    cartModel.discountPrice = 0.0
                    cartModel.discountType = ""
                    cartModel.isManualSales = true
                    viewModel.cartLogic(cartList, cartModel, Constants.UPDATE)


                }


            }
        }
    }


    private fun onConfig() {
        binding.llKeypad.txtAmount.addTextChangedListener(
            AmountTextWatcher(
                binding.llKeypad.txtAmount,
                true
            )
        )
        binding.llKeypad.txtAmount.setText("0.00")

        //set customer data
        setUpCustomer(prefProvider.getCustomerData())

        cartAdapter = ManualSaleCartAdapter()
        cartAdapter.setCallBack(this)
        cartAdapter.setItemCallBack(this)
        binding.rvSaleCart.adapter = cartAdapter



      /*  binding.layoutMenu.txtProducts.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutMenu.txtKeypad.setTextColor(resources.getColor(R.color.txt_color_blue))*/

    }

    private fun calculateValue(number: String, delete: Boolean) {

        if (delete && binding.llKeypad.txtAmount.text?.length!! > 1) {

            binding.llKeypad.txtAmount.setText(removeLastCharacter(binding.llKeypad.txtAmount.text.toString()))
        } else if (binding.llKeypad.txtAmount.text?.trim()!!.equals("0.00")) {
            binding.llKeypad.txtAmount.setText("")
            binding.llKeypad.txtAmount.append(number)
        } else {
            binding.llKeypad.txtAmount.append(number)
        }
        addItemToCart(binding.llKeypad.txtAmount.text.toString(), false)
        val str = binding.llKeypad.txtAmount.text.toString().replace("""[$]""".toRegex(), "")
        //binding.txtAmount.setText("${binding.txtAmount.text.toString().replace("""[$]""".toRegex(), "")}")
        var newText = StringBuilder(str).insert(0, "%").toString()


        Log.e(
            TAG,
            "afterTextSet  ${
                binding.llKeypad.txtAmount.text.toString().replace("""[$]""".toRegex(), "%")
            }"
        )
    }


    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun getFirstValue(str: String): String {
        return str.toString().substring(0, str.indexOf('.'))


    }

    private fun dialogMenu() {
        if (binding.llCustomerDialog.visibility == View.VISIBLE) {
            binding.llCustomerDialog.visibility = View.GONE
        } else {
            binding.llCustomerDialog.visibility = View.VISIBLE

        }

    }

    @SuppressLint("SetTextI18n")
    override fun onItemClicked(model: TbItem, position: Int) {
        Log.e(TAG, "Itemmodel: ${Gson().toJson(model)}")
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
        val txtTitle: AppCompatTextView = dialog.findViewById(R.id.txtTitle)
        val txtQty: AppCompatEditText = dialog.findViewById(R.id.txtQty)
        val llPlus: LinearLayoutCompat = dialog.findViewById(R.id.llPlus)
        val llMinus: LinearLayoutCompat = dialog.findViewById(R.id.llMinus)
        val btnRemove: AppCompatTextView = dialog.findViewById(R.id.btnRemove)
        val btnAddDiscount: AppCompatTextView = dialog.findViewById(R.id.btnAddDiscount)
        val edtNote: AppCompatEditText = dialog.findViewById(R.id.edtNote)
        val edtItemName: AppCompatEditText = dialog.findViewById(R.id.edtItemName)
        val txtSave: AppCompatTextView = dialog.findViewById(R.id.txtSave)

        edtNote.setText(model.note)
        totalquantity = 0
        var qty = model.itemQuantity
        totalquantity = qty

        txtQty.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                val enteredString = s.toString()
                if (enteredString.startsWith("0")) {

                    if (enteredString.length > 0) {
                        txtQty.setText(enteredString.substring(1))
                    } else {
                        txtQty.setText("")
                    }
                } else if (s.toString().trim().isNotEmpty() && s.toString().toInt() > 10000) {
                    txtQty.setText("10000")
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        edtItemName.setText(model.name)
        txtQty.setText(qty.toString())
        if (model.discountPrice != 0.0) {
            txtTitle.text = model.name + "  $" + String.format(
                "%.2f",
                ((model.price * model.itemQuantity) - model.discountPrice)
            )
        } else {
            txtTitle.text = model.name + "  $" + String.format(
                "%.2f",
                (model.price * model.itemQuantity)
            )
        }

        imgClose.setOnClickListener {
            dialog.dismiss()
        }

        txtSave.setOnClickListener {
            dialog.dismiss()
            val itemCost = model.price

            if (model.discountPrice != 0.0) {
                val dis = model.discountPrice / model.itemQuantity

                model.discountPrice =
                    String.format("%.2f", (dis * txtQty.text.toString().toInt())).toDouble()
            }


            model.note = edtNote.text.toString().trim()
            model.itemQuantity = txtQty.text.toString().toInt()
            model.name = edtItemName.text.toString()

            model.price = String.format("%.2f", (itemCost)).toDouble()

            viewModel.setPosition(position)

            viewModel.cartLogic(cartList, model, Constants.UPDATE)
        }

        llPlus.setOnClickListener {
            qty += 1
            totalquantity = qty
            txtQty.setText(qty.toString())
        }
        llMinus.setOnClickListener {

            if (qty > 1) {
                qty -= 1
            }
            totalquantity = qty
            txtQty.setText(qty.toString())
        }

        btnRemove.setOnClickListener {
            Log.e(TAG, "modelRemove:  ${Gson().toJson(model)}")
            viewModel.cartLogic(cartList, model, Constants.DELETE)
            dialog.dismiss()
        }

        btnAddDiscount.setOnClickListener {
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    Log.e(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                        model.discountPrice = calculateDiscountPercentage(
                            model.price * totalquantity,
                            result.percentage
                        )
                        model.discountId = result.id
                        model.discountType = result.discountType
                        model.isManualSales = true
                        viewModel.cartLogic(cartList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            ((model.price * totalquantity) - model.discountPrice)
                        )

                    } else if (result.discountType == "Amount") {

                        model.discountPrice = result.percentage
                        model.discountId = result.id
                        model.discountType = result.discountType
                        model.isManualSales = true

                        viewModel.cartLogic(cartList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            ((model.price * totalquantity) - model.discountPrice)
                        )
                    } else {
                        model.discountPrice = 0.0
                        model.discountType = ""
                        model.isManualSales = true
                        viewModel.cartLogic(cartList, model, Constants.UPDATE)
                    }
                } else {
                    model.discountPrice = 0.0
                    model.discountType = ""
                    model.isManualSales = true
                    viewModel.cartLogic(cartList, model, Constants.UPDATE)


                }
            }
            val bundle = Bundle().apply {
                putBoolean("isFromDetails", true)
                putParcelable("model", model)
            }
            findNavController().navigate(R.id.action_manualSaleNew_to_addDiscountDialog, bundle)
        }


        dialog.setCanceledOnTouchOutside(false)
        dialog.dismiss()
        dialog.show()


    }

    private fun showPopupWindow(view: View) {

        val popupView: View = layoutInflater.inflate(R.layout.info_popup_window_new, null)

        val chkLoyalty: CheckBox = popupView.findViewById(R.id.chkLoyaltyAmount)
        val txtSubTotal: AppCompatTextView = popupView.findViewById(R.id.txtSubTotal)
        val txtServiceCharge: AppCompatTextView = popupView.findViewById(R.id.txtServiceCharge)
        val txtDiscount: AppCompatTextView = popupView.findViewById(R.id.txtDiscount)
        val txtTotalAmount: AppCompatTextView = popupView.findViewById(R.id.txtTotalAmount)
        val txtTotalTax: AppCompatTextView = popupView.findViewById(R.id.txtTotalTax)
        val txtLoyaltyAmount: AppCompatTextView = popupView.findViewById(R.id.txtLoyaltyAmount)
//        val groupLoyalty: Group = popupView.findViewById(R.id.groupLoyalty)
        val txtLoyaltyPoints: AppCompatTextView = popupView.findViewById(R.id.txtLoyaltyPoints)
        val lblLoyaltyPoints: AppCompatTextView = popupView.findViewById(R.id.lblLoyaltyPoints)
        val lblLoyaltyAmount: AppCompatTextView = popupView.findViewById(R.id.lblLoyaltyAmount)
        val txtTotalcashAdj: AppCompatTextView = popupView.findViewById(R.id.txtnoncashadj)
        val linear_NonCashDiscount: LinearLayoutCompat =
            popupView.findViewById(R.id.linear_NonCashDiscount)

        //listeners
        chkLoyalty.setOnCheckedChangeListener { _, p1 ->
            viewModel.redeemLoyaltyInfo.needToApplyLoyalty = p1
            prefProvider.setValueboolean(Constants.LOYALTY_ADDED, p1)
            //refresh pop up data and final calculation
            //setPopUpData(popupView)
            refreshItemCalculation()
            //display total price to be paid
            MethodUtils.setPriceTextView(
                txtTotalAmount,
                viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
            )
        }

        if (prefProvider.getValueboolean(Constants.LOYALTY_ADDED, false)) {
            if (!chkLoyalty.isChecked) {
                chkLoyalty.isChecked = true
            }
        } else {
            chkLoyalty.isChecked = false
        }

        //display the loyalty point


        val customer = viewModel.selectedCustomer
        if (viewModel.loyaltyPointCondition(customer)) {

            lblLoyaltyPoints.visible()
            txtLoyaltyPoints.visible()
            lblLoyaltyAmount.visible()
            txtLoyaltyAmount.visible()

            Log.e(TAG, "InsideLoyalty")
            Log.e(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
            amountToBepaid = viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
            txtLoyaltyAmount.text =
                "- $${String.format("%.2f", viewModel.redeemLoyaltyInfo.usedLoyaltyAmount)}"
            txtLoyaltyPoints.text = "${viewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"


            // groupLoyalty.gone()
            chkLoyalty.visible()
            chkLoyalty.isChecked = viewModel.redeemLoyaltyInfo.needToApplyLoyalty
        } else {
            amountToBepaid = viewModel.totalPrice
            lblLoyaltyPoints.gone()
            txtLoyaltyPoints.gone()
            lblLoyaltyAmount.gone()
            txtLoyaltyAmount.gone()

            //   groupLoyalty.gone()
            chkLoyalty.gone()
        }

        txtSubTotal.text = "$" + String.format(
            "%.2f",
            viewModel.subTotalPrice
        )
        txtServiceCharge.text = "$" + String.format(
            "%.2f",
            viewModel.totalServiceCharge
        )
        txtDiscount.text = "- $" + String.format(
            "%.2f",
            viewModel.totalDiscount
        )
        //txtTotalAmount.text = binding.txtTotalAmount.text.toString()
        txtTotalTax.text = "$" + String.format(
            "%.2f",
            viewModel.totalTax
        )

        if (prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)) {
            linear_NonCashDiscount.visibility = View.VISIBLE
            txtTotalcashAdj.text = "$" + String.format(
                "%.2f",
                MethodUtils.calculateCashDiscount(
                    amountToBepaid!!,
                    prefProvider,
                    requireContext()
                )
            )
        } else {
            linear_NonCashDiscount.visibility = View.GONE
        }

        //display total price to be paid
        MethodUtils.setPriceTextView(txtTotalAmount, amountToBepaid ?: 0.0)

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

    private fun refreshItemCalculation() {
        viewModel.itemCalculation(
            if (cartList?.isNotEmpty() == true) {
                cartList?.get(0)?.items
            } else {
                null
            },
            binding.txtTotalAmount
        )
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
        val txtEmployeename: TextView = footerView.findViewById(R.id.txtEmployeeName)
        val txtMore: TextView = footerView.findViewById(R.id.txtMore)
        val linearMore: LinearLayout = footerView.findViewById(R.id.linearMore)
        val linearHome: LinearLayout = dialog.findViewById(R.id.linearHome)
        val linearOrders: LinearLayout = dialog.findViewById(R.id.linearOrders)
        val linearEmprole: LinearLayoutCompat = footerView.findViewById(R.id.linear_empname_role)
        val linearclockout: LinearLayoutCompat = footerView.findViewById(R.id.linear_clockout)
        val linearTransaction: LinearLayout = dialog.findViewById(R.id.linearTransaction)
        val linearCash: LinearLayout = dialog.findViewById(R.id.linearCash)
        val linearReports: LinearLayout = dialog.findViewById(R.id.linearReports)
        val linearCust: LinearLayout = dialog.findViewById(R.id.linearCust)
        val linearTeam: LinearLayout = dialog.findViewById(R.id.linearTeam)
        val linearHardware: LinearLayout = dialog.findViewById(R.id.linearHardware)
        val linearInventory: LinearLayout = dialog.findViewById(R.id.linearInventory)
        val txtBusinessName: TextView = dialog.findViewById(R.id.txtBusinessName)
        val linearSetting: LinearLayout = dialog.findViewById(R.id.linearSetting)
        val linearSupport: LinearLayout = dialog.findViewById(R.id.linearSupport)

        txtEmployeename.text =
            prefProvider.getValue(Constants.EMPLOYEE_NAME, "").toString()
        txtBusinessName.text = getString(R.string.business_name) + ": " + prefProvider.getValue(
            Constants.BUSINESS_NAME,
            ""
        )


        linearEmprole.setOnClickListener {
            dialog.dismiss()
            var bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            findNavController().navigate(R.id.action_manualSaleNew_to_passcode, bundle)
        }
        linearclockout.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_manualSaleNew_to_reportEODFragment)
        }
        linearHome.setOnClickListener {
            findNavController().popBackStack()
            closeDialog(dialog)
        }

        linearHardware.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_hardware)
            closeDialog(dialog)
        }

        linearCash.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_Cashlog)
            closeDialog(dialog)
        }

        linearTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_transactionFragment)
            closeDialog(dialog)
        }
        linearOrders.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_orders)
            closeDialog(dialog)
        }
        linearCust.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_customer)
            closeDialog(dialog)
        }
        linearReports.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_reports)
            dialog.dismiss()
        }
        linearTeam.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_teamList)
            dialog.dismiss()
        }
        linearInventory.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_inventory)
            dialog.dismiss()
        }
        linearSetting.setOnClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_settings)
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

    fun calculateDiscountPercentage(originalPrice: Double, percentage: Double): Double {
        val disPrice = MethodUtils.roundOffAmountDouble((originalPrice * percentage) / 100)
        Log.e(TAG, "disPrice  $disPrice")
        return if (disPrice < originalPrice) {
            disPrice
        } else {
            0.0
        }


    }

    fun hideClearCart() {
        if (binding.llOrderMenu.visibility == View.VISIBLE) {
            binding.llOrderMenu.visibility = View.GONE
        } else {
            binding.llOrderMenu.visibility = View.VISIBLE
        }
    }

    override fun onItemClickListener(view: View?, data: TbItem, pos: Int) {

        mPostion = pos
        when (view?.id) {

            R.id.txt_discount -> {
                val bundle = Bundle().apply {
                    putBoolean("isFromDetails", false)
                    putParcelable("model", data)
                }

                cartAdapter.viewBinderHelper.closeLayout(pos.toString())
                findNavController().navigate(
                    R.id.action_manualSaleNew_to_addDiscountDialog,
                    bundle
                )
            }
/*            R.id.txt_delete -> {
                cartAdapter.viewBinderHelper.closeLayout(pos.toString())
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_item_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        // Do positive stuff here
                        val item = cartAdapter.getItem(pos)

                        Log.e(TAG, "item ${Gson().toJson(item)}")
                        viewModel.cartLogic(cartList, item, Constants.DELETE)

                    }
                    negativeButton(R.string.tv_cancel) {
                        // Do negative stuff here
                    }
                }
            }
            R.id.txt_note -> {
                cartItemModel = cartAdapter.getItem(pos)
                val bundle = Bundle().apply {
                    putString("note", cartItemModel.note)
                }
                cartAdapter.viewBinderHelper.closeLayout(pos.toString())
                findNavController().navigate(
                    R.id.action_manualSaleNew_to_addNoteDialog,
                    bundle
                )
            }
            R.id.txt_rename -> {
                Log.e(TAG, "pospospos  ${pos}")

                cartItemModel = cartAdapter.getItem(pos)
                val bundle: Bundle = bundleOf("item_name" to cartItemModel.name)
                cartAdapter.viewBinderHelper.closeLayout(pos.toString())
                findNavController().navigate(
                    R.id.action_manualSaleNew_to_itemRenameDialog,
                    bundle
                )

            }*/
        }
    }
}

