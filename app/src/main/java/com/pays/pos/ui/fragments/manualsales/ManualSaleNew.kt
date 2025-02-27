package com.pays.pos.ui.fragments.manualsales

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ADD
import com.pays.pos.data.remote.Constants.AMOUNT
import com.pays.pos.data.remote.Constants.CUSTOMER_NAME
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.pays.pos.data.remote.Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.data.remote.Constants.LOYALTY_ADDED
import com.pays.pos.data.remote.Constants.MANUALSALE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.databinding.FragmentManualSaleNewBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.ManualSaleCartAdapter
import com.pays.pos.ui.adapter.ManualSaleCartAdapterNew
import com.pays.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.getSaltString
import com.pays.pos.utils.callback.ManualSaleOptionsCustomCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.subTotalToDouble
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class ManualSaleNew : Fragment(), ManualSaleCartAdapter.ManualSaleInterface,
    ManualSaleOptionsCustomCallback, ManualSaleCartAdapterNew.ManualSaleInterface {

    private var manualCartModel: CartModel? = null
    private var mPostion: Int = 0
    private var manualItemId: Int = 0
    private var manualCategoryId: Int = 0
    private lateinit var nameObserver: Observer<List<TbCartItem>>
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"
    private var cartModelsList: List<CartModel>? = null
    private var cartItemsList: List<TbCartItem>? = null
    private lateinit var manualSaleCartAdapterNew: ManualSaleCartAdapterNew
    private var cartItemModel = TbCartItem()
    var orderDiscount = 0.0
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    var amountToBepaid = 0.0
    var totalquantity = 0
    var taxClickable = false
    private var initialItemQuantity = 0


    @Inject
    lateinit var rolePermission: RolePermission
    private var serviceChargesList: ArrayList<TbServiceCharge>? = null
    private var discountList: List<TbDiscount>? = null
    private var taxList: List<TaxData>? = null
    private var assignCustomer: TbCustomer? = null
    private var isPayClicked: Boolean = false
    var tabItemMOdel = TbItem()
    var tabCartItemModel = TbCartItem()
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    var selectedHeaderPosition: Int = 0
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dineInViewModel by activityViewModels<DineInOrderTableViewModel>()
    private lateinit var presentation: CustomDisplay

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
        getDiscountList()

        /*Added by Rahul to solve the custom item not printing issue - START*/
        prefProvider.setValueboolean(Constants.DO_PRINT_CUSTOM, false)
        /*Added by Rahul to solve the custom item not printing issue - END*/


        if (arguments != null) {
            selectedHeaderPosition = requireArguments().getInt("selectedHeaderPosition", 0)
        }
        binding.layoutHeader.edtSearch.visibility = View.GONE

        viewModel.reloadCustomerDisplay.observe(viewLifecycleOwner,object:Observer<Boolean>{
            override fun onChanged(t: Boolean) {
                if (t){
                    getCustomerDisplay(requireContext())?.let {
                        presentation = CustomDisplay(
                            it,
                            requireContext(),
                            viewLifecycleOwner,
                            dashBoardCategoryViewModel = viewModel,
                            passcodeViewModel,
                            dineInViewModel,
                        )

                        if (presentation!=null) {
                            presentation.show()
                        }
                    }

                }
            }
        })

        return binding.root
    }

    private fun getDiscountList() {
        viewModel.discountList.observe(requireActivity()) {

            if (it.data != null)
                discountList = it.data
        }
    }


    private fun getTaxList() {
        viewModel.taxList.observe(requireActivity()) {
            if (it.data != null)
                taxList = it.data
        }
    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        var taxlist = arrayListOf<TaxData>()
        taxBirfurcationAdapter.setList(taxlist)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgSearch.visibility = View.GONE
        binding.layoutMenu.autoSearch.visibility = View.GONE
        binding.layoutMenu.imgOptionMenu.visibility = View.GONE
        viewModel.redeemLoyaltyInfo.needToApplyLoyalty =
            prefProvider.getValueboolean(LOYALTY_ADDED, false)

        binding.footer.txtEmployeeName.text =
            prefProvider.getValue(Constants.EMPLOYEE_NAME, "").toString()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    prefProvider.setValue(Constants.REDIRECT_FROM, "")
                    viewModel.cartModel = null
                    viewModel.manualCartOrderNote=""
                    findNavController().popBackStack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
            binding.btnPay.gone()
        } else {
            binding.btnPay.gone()
        }

        getManualCategoryId()
        onConfig()
        onClickKeypad()
        getCartList()
        getTaxList()
        onClick()
        listener()
        setUpToolbar()//created By Zeeshan
        callbackForDialog()
        setupTaxAdapter()
        binding.linearTaxDetail.setOnClickListener {
            if (taxBirfurcationAdapter.taxlist.size > 0) {
                if (!taxClickable) {
                    Log.d(TAG, "onViewCreated: " + taxBirfurcationAdapter.taxlist.size)
                    taxClickable = true
                    if (taxBirfurcationAdapter.taxlist.size == 1) {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._75sdp).toInt()
                        } else {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._60sdp).toInt()
                        }
                    } else if (taxBirfurcationAdapter.taxlist.size == 2) {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._85sdp).toInt()
                        } else {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._75sdp).toInt()
                        }
                    } else {
                        if (viewModel.order_note.isNotEmpty()) {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._110sdp).toInt()
                        } else {
                            binding.linearBottomInfo.layoutParams.height =
                                resources.getDimension(R.dimen._100sdp).toInt()
                        }

                    }
                    binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                    binding.relativeDynamicTax.visible()
                } else {
                    if (viewModel.order_note.isNotEmpty()) {
                        binding.linearBottomInfo.layoutParams.height =
                            resources.getDimension(R.dimen._60sdp).toInt()
                    } else {
                        binding.linearBottomInfo.layoutParams.height =
                            resources.getDimension(R.dimen._50sdp).toInt()
                    }
                    taxClickable = false
                    binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.relativeDynamicTax.gone()
                }
            }

        }




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

    private fun setCurrentSubTotal(itemQuantity: Int) {
        val subTotalText = binding.txtSubTotal.text.toString()
        val subTotal = subTotalText.subTotalToDouble()
        viewModel.currentTotalPrice = subTotal
        viewModel.clickedItemQuantity = itemQuantity
    }


    fun setTaxBifurcationData(taxlistData: ArrayList<TaxData>) {
        if (taxlistData?.isNotEmpty()) {
            Log.d(TAG, "addObserver: " + taxlistData.size)
            setupTaxAdapter()
            taxClickable = false
            if (viewModel.order_note.isNotEmpty()) {
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._60sdp).toInt()
            } else {
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.visible()
            taxBirfurcationAdapter.setList(taxlistData)
            binding.relativeDynamicTax.gone()
        } else {
            if (viewModel.order_note.isNotEmpty()) {
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._60sdp).toInt()
            } else {
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
            }
            binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.imgDropdown.gone()
            binding.relativeDynamicTax.gone()
            taxClickable = false
        }
    }

    fun reSetTaxBifurcationData() {
        taxBirfurcationAdapter.clearList()
        if (viewModel.order_note.isNotEmpty()) {
            binding.linearBottomInfo.layoutParams.height =
                resources.getDimension(R.dimen._60sdp).toInt()
        } else {
            binding.linearBottomInfo.layoutParams.height =
                resources.getDimension(R.dimen._50sdp).toInt()
        }

        taxClickable = false
        binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.imgDropdown.gone()
        binding.relativeDynamicTax.gone()
    }

    //created By Zeeshaan
    private fun setUpToolbar() {
        binding.layoutHeader.txtKeypad.setTextColor(requireContext().resources.getColor(R.color.btnColor))
        binding.layoutHeader.txtTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_manualSalesNew_to_transactionFragment)
            }
            clearManualCartItems()
        }
        binding.layoutHeader.imgSync.setOnClickListener {
            //  viewModel.syncInventoryModule(requireActivity())
        }
        binding.layoutHeader.imgDrawer.setOnSingleClickListener {
            findNavController().navigate(R.id.action_manualSalesNew_to_menuFragment)

        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("isSwap", true)
            bundle.putBoolean("isDashboard", false)
            viewModel.deleteCart()
            findNavController().navigate(
                R.id.action_manualSalesNew_to_passcode,
                bundle
            )
        }
        binding.layoutHeader.txtUserName.text =
            prefProvider.getValue(Constants.EMPLOYEE_NAME, "")
        binding.layoutHeader.ivLock.setOnClickListener {
            prefProvider.setValue(Constants.REDIRECT_FROM, "")
            findNavController().navigate(R.id.action_manualSalesNew_to_reportEODFragment)
        }
        binding.layoutHeader.txtDineIn.setOnClickListener {
            try {
                prefProvider.setValue(Constants.REDIRECT_FROM, "")
                findNavController().navigate(R.id.action_manualSalesNew_to_dineInFragmentPays)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.layoutHeader.txtHome.setOnClickListener {
            try {
                var activityWeakReference=WeakReference(requireActivity())
                activityWeakReference.get()?.let {
                    MethodUtils.hideSoftKeyboard(it)
                }
                prefProvider.setValue(Constants.REDIRECT_FROM, "")
                viewModel.cartModel = null
                viewModel.manualCartOrderNote=""
                findNavController().navigateUp()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    private fun getManualCategoryId() {

        viewModel.returnedVal.observe(viewLifecycleOwner) {

            if (it != null) {
                manualCategoryId = it.id
                if (it.item_ids.isNotEmpty())
                    manualItemId = it.item_ids[0]
                LogUtil.logE("manualItemId", manualItemId.toString())
            }

        }
    }

    private fun getLoyaltyPrograms() {
        LogUtil.logE("Loyalty", "getLoyaltyPrograms called..")
        viewModel.activeLoyaltyProgram = prefProvider.getActiveLoyaltyData()
        viewModel.activeLoyaltyProgramLiveData.observe(requireActivity()) {
            if (it.data != null) {
                LogUtil.logE("Loyalty", "getLoyaltyPrograms fetched..")
                prefProvider.saveActiveLoyaltyData(it.data)
                viewModel.activeLoyaltyProgram = it.data
            }
        }
    }

    private fun getCartList() {

        if (isAdded) {

            viewModel.observeLatestCartModel().observe(viewLifecycleOwner) {

                if (it.isNotEmpty()) {
                    it.forEach { cm ->
                        if (cm.isMaual) {
                            manualCartModel = cm

                            prefProvider.setValue(
                                Constants.discountType,
                                manualCartModel?.discountType.toString()
                            )
                            prefProvider.setValue(
                                Constants.discountSelectedValue,
                                manualCartModel?.discountSelectdValue.toString()
                            )
                            prefProvider.setValue(
                                Constants.discountPrice,
                                manualCartModel?.discountPrice.toString()
                            )
                            prefProvider.setValue(
                                Constants.taxListDynamic,
                                Gson().toJson(manualCartModel?.taxlistDynamic)
                            )
                            return@forEach
                        }
                    }

                    Log.d(
                        "31OCT23",
                        "addObserver: CartModel Updated = ${Gson().toJson(manualCartModel)}"
                    )
                    Log.d(
                        "31OCT23",
                        "addObserver: CartItems Updated = ${Gson().toJson(cartItemsList)}"
                    )

                    updateCartFooter()

                }
            }

            viewModel.getManualSaleCartItems(
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            ).observe(viewLifecycleOwner) {
                cartItemsList = it
                LogUtil.logE(TAG, "cartListBeforeTax  ${Gson().toJson(cartItemsList)}")

                if (cartItemsList?.isNotEmpty()!!) {

                    cartItemsList?.forEach {
                        it.taxes = taxList
                    }
                    manualSaleCartAdapterNew.setList(cartItemsList)

                }

                updateCartFooter()
            }
        }


    }

    private fun updateCartFooter() {
        if (cartItemsList?.isNotEmpty() == true) {
            viewModel.itemCalculationNew(
                manualCartModel,
                cartItemsList,
                binding.txtTotalAmount, requireContext(),
                isFromManualSales = true
            )
            manualCartModel?.let { cm ->
                setTaxBifurcationData(cm.taxlistDynamic as ArrayList<TaxData>)
            }
            setTextValue()

            prefProvider.setValue(
                Constants.discountType,
                manualCartModel?.discountType.toString()
            )
            prefProvider.setValue(
                Constants.discountSelectedValue,
                manualCartModel?.discountSelectdValue.toString()
            )
            prefProvider.setValue(
                Constants.discountPrice,
                manualCartModel?.discountPrice.toString()
            )
            prefProvider.setValue(
                Constants.taxListDynamic,
                Gson().toJson(manualCartModel?.taxlistDynamic)
            )

            if (viewModel.order_note.isNotEmpty()) {
                binding.relativeOrderNotes.visible()
                binding.txtOrderNote.text = viewModel.order_note

                prefProvider.setValue(
                    Constants.orderNote,
                    viewModel.order_note.toString()
                )

            } else {
                binding.relativeOrderNotes.gone()
            }
        } else {
            manualSaleCartAdapterNew.clearList()
            reSetTaxBifurcationData()

            viewModel.itemCalculationNew(
                null,
                emptyList(),
                binding.txtTotalAmount, requireContext(),
                isFromManualSales = true
            )
            binding.relativeOrderNotes.gone()
            setTextValue()
        }
    }

    private fun setTextValue() {

        binding.txtSubTotal.text = "$" + String.format(
            "%.2f",
            viewModel.subTotalPrice
        )
        binding.txtServiceCharge.text = "$" + String.format(
            "%.2f",
            viewModel.totalServiceCharge
        )
        if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
            binding.tvDiscountText.text = "Discount (${viewModel.cartModel?.discountSelectdValue}%)"
        } else {
            binding.tvDiscountText.text = "Discount"
        }
        binding.txtDiscount.text = "- $" + String.format(
            "%.2f",
            viewModel.totalDiscount
        )
        //txtTotalAmount.text = binding.txtTotalAmount.text.toString()
        binding.txtTax.text = "$" + String.format(
            "%.2f",
            viewModel.totalTax
        )
        binding.txtTotal.text = "$" + String.format(
            "%.2f",
            viewModel.totalPrice
        )
        if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
            binding.tvDiscountText.text = "Discount (${viewModel.cartModel?.discountSelectdValue}%)"
        } else {
            binding.tvDiscountText.text = "Discount"
        }
        binding.tvDiscount.text = "-$" + String.format(
            "%.2f",
            viewModel.totalDiscount
        )
        binding.txtTotalAmount.text = "$" + String.format(
            "%.2f",
            viewModel.totalPrice
        )
    }

    private fun getServiceCharge() {
        viewModel.serviceCharges.observe(requireActivity()) {
            if (it.data != null)
                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                    serviceChargesList = arrayListOf()
                    it.data.forEach { service ->
                        if (service.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                            serviceChargesList?.add(service)
                        }
                    }
                    serviceChargesList = it.data as ArrayList<TbServiceCharge>?
                } else {
                    if (prefProvider.getValueboolean(
                            Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                            false
                        )
                    ) {
                        LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                        serviceChargesList = arrayListOf()
                        it.data?.forEach { service ->
                            if (service.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                                serviceChargesList?.add(service)
                            }
                        }

                    }
                }

        }
    }

    private fun listener() {

        viewModel.clickTakeOut.observe(
            viewLifecycleOwner,
            object : androidx.lifecycle.Observer<Event<Boolean>> {
                override fun onChanged(t: Event<Boolean>?) {
                    setUpCustomer(Gson().fromJson<TbCustomer>(prefProvider.getValue(
                        Constants.PREF_CUSTOMER,
                        ""
                    ), TbCustomer::class.java))
                }
            })

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
            prefProvider.setValue(
                Constants.RECEIPT_CUSTOMER_NAME,
                customer.first_name + " " + customer.last_name
            )
            customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

            binding.txtAddCustomer.text = customer.first_name + " " + customer.last_name
            binding.txtCrtNewCustomer.text = "Remove Customer"

            //set loyalty
            if (viewModel.loyaltyPointCondition(customer)) {
                "${getString(R.string.loyalty_points)}: ${customer.final_reward}".also {
                    binding.txtLoyaltyPoints.text = it
                }
                binding.txtLoyaltyPoints.visible()
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
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
        binding.txtAddCustomer.text = getString(R.string.add_customer2)
        //  binding.txtCrtNewCustomer.text = "Add Customer"
        binding.txtLoyaltyPoints.gone()
        refreshItemCalculation()
    }

    private fun <TbCartItem> merge(
        first: List<TbCartItem>,
        second: List<TbCartItem>
    ): List<TbCartItem> {
        return first + second
    }

    private fun onClick() {
        var mainCartList: ArrayList<TbCartItem>
        var mainCartModel: CartModel
        var totalDiscount: Double = viewModel.totalDiscount

        nameObserver = Observer<List<TbCartItem>> {


            val bundle = Bundle()
            if (isPayClicked && cartItemsList?.isNotEmpty() == true) {
                LogUtil.logE(
                    "!_@_",
                    "Total Price: ${viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0}"
                )
                bundle.putDouble(
                    "totalPrice",
                    viewModel.redeemLoyaltyInfo.getAmountToBePaid() ?: 0.0
                )
                bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                bundle.putDouble("totalTax", viewModel.totalTax)
                bundle.putDouble("totalDiscount", totalDiscount)
                bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                viewModel.cartModel?.customer = assignCustomer
                bundle.putParcelable("cartList", viewModel.cartModel)
                LogUtil.logE(TAG, "cartListManualSale  ${Gson().toJson(cartItemsList)}")
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

                mainCartList = it as ArrayList<TbCartItem>
                mainCartModel = viewModel.cartModel ?: CartModel()

                if (cartItemsList != null && cartItemsList!!.isNotEmpty()) {


                    mainCartModel.taxlistDynamic = getTaxBifurcationList(
                        manualCartModel?.taxlistDynamic,
                        mainCartModel.taxlistDynamic
                    )

                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(mainCartModel.taxlistDynamic)
                    )

                    if (prefProvider.getValueboolean(IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)) {
                        cartItemsList?.forEach { itemdatap ->
                            itemdatap.isEdited = true
                        }
                    }
                    Log.d(TAG, "data list: " + Gson().toJson(mainCartModel.taxlistDynamic))
                    val manualItems = cartItemsList

                    Log.d(
                        "WINZO",
                        "#1 - Manual Items List Size = ${manualItems?.size} = ${
                            getItemIds(manualItems!!)
                        } and "
                    )
                    Log.d(
                        "WINZO",
                        "#2 - Main Items List Size = ${mainCartList.size} = ${
                            getItemIds(mainCartList)
                        } and "
                    )


                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                        /*val mainItems =
                            mainCartList[0].dineInList?.get(selectedHeaderPosition)?.items
                        val mergeItems = merge(mainItems!!, manualItems!!)
                        mainCartList[0].dineInList?.get(selectedHeaderPosition)?.items =
                            mergeItems as ArrayList<TbItem>*/


                        val mainItems = mainCartList
                        val mergeItems = merge(mainItems, manualItems)
                        mainCartList = mergeItems.toCollection(arrayListOf())
                    } else {
                        val mainItems = mainCartList
                        val mergeItems = merge(mainItems, manualItems)
                        Log.d(
                            "WINZO",
                            "#3 - Merged Items List Size = ${mergeItems.size} = ${
                                getItemIds(mergeItems)
                            } and "
                        )
                        mainCartList = mergeItems.toCollection(arrayListOf())
                        Log.d(
                            "WINZO",
                            "#4 - Main Items List Again Size = ${mainCartList.size} = ${
                                getItemIds(mainCartList)
                            } and "
                        )

                    }


                    //  mainCartModel.discountPrice = 100.0

                    var mainDiscount = viewModel.mainCartDiscount
                    var customDiscount = manualCartModel?.discountPrice!!

                    mainCartModel.discountPrice =
                        if (mainDiscount > customDiscount) mainDiscount else customDiscount

                    val updatedDiscount = mainCartModel.discountPrice

                    viewModel.customCartUpdateDiscount = updatedDiscount

//                    mainDiscount = viewModel.mainCartDiscount
//
//                    if (manualCartModel?.discountPrice != 0.0)
//                        mainCartModel.discountPrice = manualCartModel?.discountPrice ?: 0.0


                    viewModel.discountNeedToUpdate = true
                    viewModel.cartFooterNeedToBeUpdated = true

                    if (manualCartModel?.note?.isNotEmpty() == true)
                        mainCartModel.note = manualCartModel?.note ?: ""

                    prefProvider.setValue(
                        Constants.orderNote,
                        mainCartModel.note
                    )
                    prefProvider.setValue(
                        Constants.discountPrice,
                        mainCartModel.discountPrice.toString()
                    )

                    manualCartModel?.let { it1 ->
                        viewModel.deleteCartModel(it1)
                        EventBus.getDefault()
                            .post(MessageEvent("${Constants.LINE_BREAK_TAB} ManualSaleNew.kt_MANUAL_CART_MODEL_CLEARED: it1 -> viewModel.deleteCartModel(it1)"))

                    }


                    viewModel.deleteManualSaleItemsFromCartItems()

                    viewModel.updateCartModel(mainCartModel)
                    Log.d("BYPASS", "onClick: before MainCartList = ${getItemIds(mainCartList)}")
                    mainCartList.forEach { mcItem ->
                        mcItem.isManualSaleItem = false
                        mcItem.orderType = prefProvider.getValue(ORDER_TYPE, TAKEOUT)

                    }
                    viewModel.saveManualSaleDataNew(mainCartList)
                    Log.d("BYPASS", "onClick: after MainCartList = ${getItemIds(mainCartList)}")

                    viewModel.getAllCartItems(
                        prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString(),
                        prefProvider.getValueInt(
                            Constants.EMPLOYEE_ID, 0
                        )
                    ).asLiveData().removeObserver(nameObserver)

                    if (isPayClicked && viewModel.totalPrice != 0.0) {
                        findNavController().navigate(
                            R.id.action_manualSaleNew_to_paymentFragment,
                            bundle
                        )

                    } else {

                        prefProvider.setValue(Constants.REDIRECT_FROM, "")
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

                    }

                }
            } else {

                if (cartItemsList != null && cartItemsList!!.isNotEmpty()) {
                    cartItemsList?.forEach { it ->
                        it.isManualSaleItem = false
                        it.orderType =
                            prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString()

                    }
                    viewModel.saveManualSaleDataNew(cartItemsList!!)
                }

                if (isPayClicked && viewModel.totalPrice != 0.0) {
                    findNavController().navigate(
                        R.id.action_manualSaleNew_to_paymentFragment,
                        bundle
                    )

                } else {
                    prefProvider.setValue(Constants.REDIRECT_FROM, "")
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
            /*Added by Rahul to solve the custom item not printing issue - START*/
            prefProvider.setValueboolean(Constants.DO_PRINT_CUSTOM, true)
            /*Added by Rahul to solve the custom item not printing issue - END*/

            if (cartItemsList?.isNotEmpty() == true) {

                viewModel.getAllCartItems(
                    prefProvider.getValue(ORDER_TYPE, TAKEOUT).toString(),
                    prefProvider.getValueInt(
                        Constants.EMPLOYEE_ID, 0
                    )
                ).asLiveData().observe(
                    viewLifecycleOwner, nameObserver
                )
            }
        }

        binding.btnPay.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                viewModel.subTotalPrice=binding.txtSubTotal.text.toString().replace('$',' ').trim().toDouble()
                viewModel.totalTax = binding.txtTax.text.toString().replace('$',' ').trim().toDouble()
                /*viewModel.totalDiscount = 0.0
                totalDiscount = viewModel.totalDiscount
                prefProvider.setValue(
                    Constants.TOTAL_DISCOUNT_ACTUAL,
                    viewModel.totalDiscount.toString()
                )
                prefProvider.setValue(
                    Constants.TOTAL_DISCOUNT,
                    viewModel.totalDiscount.toString()
                )
                viewModel.mainCartDiscount = viewModel.totalDiscount*/
                viewModel.totalServiceCharge = binding.txtServiceCharge.text.toString().replace('$',' ').trim().toDouble()
//                viewModel.totalPrice = (viewModel.subTotalPrice + viewModel.totalTax + viewModel.totalServiceCharge) - viewModel.totalDiscount

                /*Added by Rahul to solve the custom item not printing issue - START*/
                prefProvider.setValueboolean(Constants.DO_PRINT_CUSTOM, true)
                /*Added by Rahul to solve the custom item not printing issue - END*/

                if (cartItemsList?.isNotEmpty() == true) {
                    prefProvider.setValue(Constants.REDIRECT_FROM, Constants.MANUAL_SALE)
                    prefProvider.setValue(
                        ORDER_TYPE,
                        prefProvider.getValue(ORDER_TYPE, TAKEOUT)
                    )
                    prefProvider.setValue("PaidAmount", "")
                    prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                    prefProvider.setValueInt("cardCount", 0)
                    prefProvider.setValue(Constants.SUB_TOTAL, "")
                    prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                    prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                    prefProvider.setValue(Constants.TIP, "")
                    prefProvider.setValue(Constants.TAX_CHARGE, "")
                    prefProvider.setValue(Constants.SERVICE_CHARGE, /*viewModel.totalServiceCharge.toString()*/"")
                    prefProvider.setValue("CART_MODEL1", Gson().toJson(manualCartModel))
                    prefProvider.setValue("CART_MODEL2", Gson().toJson(cartModelsList))
                    prefProvider.setValueboolean(IS_FROM_ALL_ORDER, false)
                    val bundle = Bundle()
                        bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)
                    findNavController().navigate(
                        R.id.action_manualSaleCart_to_paymentBoldPosFragment,
                        bundle
                    )
                    viewModel.boldPosNeedToRefresh = true
                } else {

                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                    ) { _, _ ->
                    }
                }


            }
        })

        /*binding.btnPay.setOnClickListener {
            if (cartItemsList?.isNotEmpty() == true) {
                prefProvider.setValue(Constants.REDIRECT_FROM, Constants.MANUAL_SALE)
                prefProvider.setValue(
                    ORDER_TYPE,
                    prefProvider.getValue(ORDER_TYPE, TAKEOUT)
                )
                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")
                prefProvider.setValueboolean(IS_FROM_ALL_ORDER,false)
                findNavController().navigate(
                    R.id.action_manualSaleCart_to_paymentBoldPosFragment
                )
            } else {

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                ) { _, _ ->
                }
            }

        }
*/

        binding.imgOrderMenu.setOnSingleClickListener {

            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.manual_sale_menu, popupMenu.menu)
            /*if (cartItemsList?.isEmpty() == true) {
                popupMenu.menu.findItem(R.id.menu_order_discount).isVisible = false
                popupMenu.menu.findItem(R.id.menu_order_note).isVisible = false
                popupMenu.menu.findItem(R.id.menu_clear_cart).isVisible = false
            }*/

            if (prefProvider.getValue(Constants.CUSTOMER_NAME, "").isEmpty())
                popupMenu.menu.findItem(R.id.menu_remove_customer).isVisible = false

            /*Added by Rahul for solving Discount issue */
            popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(menuItem: MenuItem?): Boolean {
                    when (menuItem?.itemId) {
                        R.id.menu_clear_cart -> {
//                            if (cartItemsList?.isNotEmpty() == true) {
                            alert(
                                getString(R.string.app_name),
                                getString(R.string.delete_items_message)
                            ) {
                                positiveButton(getString(R.string.tv_delete)) {
                                    viewModel.deleteManualSaleItemsFromCartItems()
                                    manualCartModel?.let { it1 ->
                                        viewModel.deleteCartModel(it1)
                                        EventBus.getDefault()
                                            .post(MessageEvent("${Constants.LINE_BREAK_TAB} ManualSaleNew.kt_MANUAL_CART_MODEL_CLEARED: it1 -> viewModel.deleteCartModel(it1)"))
                                    }
                                    viewModel.manualCartOrderNote=""
                                    prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                                    binding.txtTotalAmount.text = "$0.00"
                                    binding.txtTotal.text = "$0.00"
                                    binding.tvDiscountText.text = "Discount"
                                    binding.tvDiscount.text = "-$0.00"
                                    binding.txtSubTotal.text = "$0.00"
                                    binding.txtTax.text = "$0.00"
                                    binding.txtServiceCharge.text = "$0.00"
                                    clearCustomer()
                                    reSetTaxBifurcationData()
                                    redirectToCategoryType()
                                    viewModel.boldPosNeedToRefresh = true
                                }
                                negativeButton(R.string.tv_cancel) {

                                }
                            }
                            //  hideClearCart()
//                            }

                        }

                        R.id.menu_order_note -> {
                            findNavController().navigate(
                                R.id.action_manualSaleNew_to_addNoteDialog,
                                bundleOf("isOrderNote" to true, "isFromManual" to true)
                            )
                        }

                        R.id.menu_remove_customer -> {

                            if (cartModelsList?.isNotEmpty() == true && cartModelsList!![0].customer != null) {
                                cartModelsList!![0].customer = null
                                viewModel.addCart(cartModelsList!![0])
                            }
                            clearCustomer()
                            viewModel.changeCustomerDispSignButtonTitle(getString(R.string.sign_up_or_check_in))
                            viewModel.removedCustomerFromManualSale(true)

                        }

                        /*R.id.menu_order_discount -> {
                            viewModel.currentTotalPrice = binding.txtSubTotal.text.toString().subTotalToDouble()
                            runBlocking {
                                addOrderDiscount()
                            }
                        }*/
                    }
                    return true
                }

            })

            popupMenu.show()
            // hideClearCart()
        }

        binding.txtClearCart.setOnClickListener {
            if (cartItemsList?.isNotEmpty() == true) {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_items_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        prefProvider.setValue(Constants.REDIRECT_FROM, "")
                        viewModel.deleteCart()
                        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                        binding.txtTotalAmount.text = "$0.00"
                        refreshItemCalculation()

                    }
                    negativeButton(R.string.tv_cancel) {

                    }
                }
                binding.imgDropdown.gone()
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
                binding.relativeDynamicTax.gone()
                hideClearCart()
            }
        }
        binding.txtAddCustomer.setOnSingleClickListener {
            findNavController().navigate(R.id.action_manualSaleNew_to_assignCustomerOrderFragment)
        }

        binding.layoutMenu.txtProducts.setOnClickListener {
            if (cartItemsList?.isNotEmpty() == true) {
                viewModel.getAllCartItems(
                    prefProvider.getValue(ORDER_TYPE, "").toString(),
                    prefProvider.getValueInt(
                        Constants.EMPLOYEE_ID, 0
                    )
                ).asLiveData().observe(
                    viewLifecycleOwner, nameObserver
                )
            }
        }



        binding.footer.linearMore.setOnClickListener {
            prefProvider.setValue(Constants.REDIRECT_FROM, "")
            findNavController().navigate(R.id.action_manualSaleNew_to_menuFragment)
            //dialogPOSMenu()
        }

        binding.footer.linearTransaction.setOnClickListener {

            if (rolePermission.hasTransactionPermission(binding.root)) {
                prefProvider.setValue(Constants.REDIRECT_FROM, "")
                findNavController().navigate(R.id.action_manualSaleNew_to_transactionFragment)
            }
        }
        binding.footer.linearOpenOrders.setOnClickListener {
            prefProvider.setValue(Constants.REDIRECT_FROM, "")
            findNavController().navigate(R.id.action_manualSaleNew_to_orders)
        }
        /*binding.llInfo.setOnClickListener {
            showPopupWindow(it)
        }*/

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
                    viewModel.deleteCart()
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

    public suspend fun addOrderDiscount() {


        val bundle = Bundle()
        bundle.putBoolean("isOrderDiscount", true)
        bundle.putDouble("totalPrice", viewModel.subTotalPrice)

        if (viewModel.cartModel?.discountType?.isEmpty() == true) {

            CoroutineScope(Dispatchers.IO).async {
                viewModel.cartModel = viewModel.getManualSaleFromCart(
                    prefProvider.getValueInt(
                        Constants.EMPLOYEE_ID,
                        0
                    )
                )
            }.await()


        }

        if (cartItemsList?.isNotEmpty() == true) {

            var totalItemswithQuantity = 0

            if (prefProvider.getValue(
                    ORDER_TYPE,
                    TAKEOUT
                ) == Constants.DINE_IN
            ) {
                /*cartModelsList?.get(0)?.dineInList?.forEach {
                    it.items.forEach { it1 ->
                        totalItemswithQuantity += it1.itemQuantity
                    }
                }*/
            } else {
                cartItemsList?.forEach {
                    totalItemswithQuantity += it.itemQuantity

                }
            }

            var job: Deferred<Unit>? = null
            if (viewModel.cartModel == null) {
                runBlocking {
                    reasignCartModelFromDB()
                }
            }


            var perItemDiscount = 0.0
            if (viewModel.cartModel?.discountPrice != 0.0) {
                if (totalItemswithQuantity == 0) {
                    totalItemswithQuantity = 1
                }
                perItemDiscount =
                    MethodUtils.roundOffAmountDouble(
                        (viewModel.cartModel?.discountPrice
                            ?: 0.0) / totalItemswithQuantity
                    )
            }

            viewModel.cartModel?.let {
                bundle.putDouble(
                    "orderDiscount",
                    it.discountPrice
                )
            }
            viewModel.cartModel?.discountPrice?.let { it1 ->
                bundle.putDouble(
                    "orderDiscountPrice",
                    it1
                )
            }
            Log.d("DISCOUNT::", viewModel.cartModel?.discountType.toString())

            Log.d(
                "SELECTEDVALUE::",
                viewModel.cartModel?.discountSelectdValue.toString()
            )
            if (viewModel.cartModel?.discountType == null) {
                viewModel.cartModel?.discountType = AMOUNT
            }
            Log.d("DISCOUNT::", viewModel.cartModel?.discountType.toString())

            bundle.putString(
                "orderDiscountType",
                viewModel.cartModel?.discountType
            )
            viewModel.cartModel?.discountSelectdValue?.let { it1 ->
                bundle.putDouble(
                    "selectedvalue",
                    it1
                )
            }
            bundle.putDouble("itemOrderDiscount", perItemDiscount)
            bundle.putInt("totalquantity", totalItemswithQuantity)
            /*Added by Rahul for solving Discount issue */
        } else {
            Log.d("orderDiscountPrice", binding.tvDiscount.text.toString())
            Log.d("selectedvalue", binding.tvDiscount.text.toString())
            try {
                if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                    binding.tvDiscountText.text = "Discount (${viewModel.cartModel?.discountSelectdValue}%)"
                } else {
                    binding.tvDiscountText.text = "Discount"
                }
                bundle.putDouble(
                    "orderDiscountPrice",
                    binding.tvDiscount.text.toString().toDouble() ?: 0.0
                )
            } catch (e: NumberFormatException) {
                bundle.putDouble(
                    "orderDiscountPrice",
                    0.0
                )
            }

            try {
                if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
                    binding.tvDiscountText.text = "Discount (${viewModel.cartModel?.discountSelectdValue}%)"
                } else {
                    binding.tvDiscountText.text = "Discount"
                }
                bundle.putDouble(
                    "selectedvalue",
                    binding.tvDiscount.text.toString().toDouble() ?: 0.0
                )
            } catch (e: NumberFormatException) {
                bundle.putDouble(
                    "selectedvalue",
                    0.0
                )
            }


        }
        bundle.putString("isFrom", "orderDiscountManual")
/*Added by Rahul for solving Discount issue */
        for (key in bundle.keySet()) {
            Log.d("BUNDLE_PRINT_MANUAL", "Key: $key, value: ${bundle.get(key)}")
        }


        if (prefProvider.isAdmin() || prefProvider.isManager()) {
            if (findNavController().currentDestination?.id != R.id.addDiscountDialog) {
                findNavController().navigate(
                    R.id.action_manualSaleNew__to_addDiscountDialog,
                    bundle
                )
            }
        } else {
            findNavController().navigate(
                R.id.action_manualSaleNew_to_pascodeManagerDailog, bundle
            )
        }


    }

    private fun getItemIds(items: List<TbCartItem>): String {
        var ids = ""
        items.forEach {
            ids += it.cartItemId
        }
        return ids
    }

    private fun redirectToCategoryType() {
        binding.layoutHeader.txtHome.performClick()

    }

    private fun getTaxBifurcationList(list1: List<TaxData>?, list2: List<TaxData>?): List<TaxData> {
        var size1 = list1!!.size
        var size2 = list2!!.size
        var final_list: List<TaxData> = emptyList()
        var listremaining: List<TaxData> = emptyList()
        if (size1 > size2) {
            final_list = list1
            list1.forEachIndexed { index, taxData ->
                var found = -1
                list2.forEachIndexed { indexmanual, manualtax ->
                    if (taxData.id == manualtax.id) {
                        found = indexmanual
                    }
                }
                if (found != -1) {
                    final_list[index].subTotalAmount =
                        final_list[index].subTotalAmount.plus(
                            list2[found].subTotalAmount
                        )
                    final_list[index].totalTaxTypePrice =
                        final_list[index].totalTaxTypePrice.plus(
                            list2[found].totalTaxTypePrice
                        )
                } else {
                    listremaining = listOf(taxData)
                }
            }
            listremaining.forEach { remainingdata ->
                if (remainingdata !in list1) {
                    final_list =
                        concatenate(final_list, listOf(remainingdata))
                }

            }
        } else if (size2 > size1) {
            final_list = list2
            final_list.forEachIndexed { index, taxData ->
                var found = -1
                list1.forEachIndexed { indexmanual, manualtax ->
                    if (taxData.id == manualtax.id) {
                        found = indexmanual
                    }
                }
                if (found != -1) {
                    final_list[index].subTotalAmount =
                        final_list[index].subTotalAmount.plus(
                            list1[found].subTotalAmount
                        )
                    final_list[index].totalTaxTypePrice =
                        final_list[index].totalTaxTypePrice.plus(
                            list1[found].totalTaxTypePrice
                        )
                } else {
                    listremaining = listOf(taxData)
                }
            }
            listremaining.forEach { remainingdata ->
                if (remainingdata !in list1) {
                    final_list =
                        concatenate(final_list, listOf(remainingdata))
                }

            }
        } else {
            final_list = list1
            final_list.forEachIndexed { index, taxData ->
                var found = -1
                list2.forEachIndexed { indexmanual, manualtax ->
                    if (taxData.id == manualtax.id) {
                        found = indexmanual
                    }
                }
                if (found != -1) {
                    final_list[index].subTotalAmount =
                        final_list[index].subTotalAmount.plus(
                            list2[found].subTotalAmount
                        )
                    final_list[index].totalTaxTypePrice =
                        final_list[index].totalTaxTypePrice.plus(
                            list2[found].totalTaxTypePrice
                        )
                } else {
                    listremaining = listOf(taxData)
                }
            }
            listremaining.forEach { remainingdata ->
                if (remainingdata !in list1) {
                    final_list =
                        concatenate(final_list, listOf(remainingdata))
                }

            }
        }
        Log.d(TAG, "getTaxBifurcationList: final list" + Gson().toJson(final_list))
        return final_list
    }

    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
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

            if (binding.llKeypad.txtAmount.text!!.trim()
                    .toString() != "0.00" && (binding.llKeypad.txtAmount.text!!.trim()
                    .toString() != "$0.00") && (binding.llKeypad.txtAmount.text!!.trim()
                    .toString() != "0")
            ) {
                binding.linearBottomInfo.layoutParams.height =
                    resources.getDimension(R.dimen._50sdp).toInt()
                taxClickable = false
                binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                binding.relativeDynamicTax.gone()
                addItemToCart(binding.llKeypad.txtAmount.text.toString(), true)
                binding.llKeypad.txtAmount.setText("0.00")
            }


        }
        binding.llKeypad.manualKeypad.tvBack.setOnClickListener {
            if (binding.llKeypad.txtAmount.text.toString().isNotEmpty()) {
                calculateValue("", true)
            }

        }
    }

    fun clearManualCartItems() {
        CoroutineScope(Dispatchers.IO).launch {

            val data = viewModel.getManualSaleCartItemsList(
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            )

            if (data?.isNotEmpty() == true) {
                data.forEach {
                    viewModel.deleteCartItem(it.cartItemId)
                }
                viewModel.deleteManualCartModel()
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} manualSaleNew.kt_MANUAL_CART_MODEL_CLEARED: viewModel.deleteManualCartModel()"))

            }
        }
    }

    private fun addItemToCart(price: String, isAdd: Boolean) {
        val replaceCurrency = price.replace("$", "")
        LogUtil.logE(TAG, "replaceCurrency  ${replaceCurrency}")
        var count = 0
        if (isAdd) {
            tabCartItemModel = TbCartItem()
            var count = 0


            if (cartItemsList?.size != 0) {


                cartItemsList?.let {

                    count = if (it.isNotEmpty()) {
                        it[cartItemsList?.size?.minus(1)!!].customItemCount
                    } else {
                        -1
                    }
                }

            }
            count++
            tabCartItemModel.customItemCount = count
            tabCartItemModel.name = "Custom Item ${count}"
            if (binding.llKeypad.edtItemName.text?.trim()?.isNotEmpty() == true) {
                tabCartItemModel.name = binding.llKeypad.edtItemName.text!!.toString().trim()
                    .replace("\\s+".toRegex(), " ")
                count--
                tabCartItemModel.customItemCount = count
            }

            tabCartItemModel.price = replaceCurrency.toDouble()
            tabCartItemModel.itemQuantity = 1
            tabCartItemModel.discountPrice = 0.0
            tabCartItemModel.isManualSaleItem = true

            if (manualSaleCartAdapterNew.getList().isEmpty()) {
                tabCartItemModel.customItemID = 1
                binding.imgDropdown.visible()
            } else {

                var id =
                    manualSaleCartAdapterNew.getItem(manualSaleCartAdapterNew.getList().size - 1).customItemID
                id++

                tabCartItemModel.customItemID = id
                binding.imgDropdown.visible()
            }

            tabCartItemModel.itemId = manualItemId
            tabCartItemModel.categoryId = manualCategoryId

            tabCartItemModel.taxes = taxList
            tabCartItemModel.timeStamp = System.currentTimeMillis().toString()


            LogUtil.logE("ordertypelist", Gson().toJson(viewModel.ordertypelist))

            viewModel.ordertypelist.forEach {
                if (it.name.equals(
                        prefProvider.getValue(Constants.ORDER_TYPE_NAME, "TakeOut"),
                        ignoreCase = true
                    )
                ) {
                    prefProvider.setValue(ORDER_TYPE, it.orderType)
                    prefProvider.setValueInt(Constants.ORDER_TYPE_ID, it.id)
                    tabCartItemModel.orderType = it.orderType
                    tabCartItemModel.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                    tabCartItemModel.isManualSales = true
                }
            }

            LogUtil.logE(
                "orderTypeId",
                prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1).toString()
            )

            if(prefProvider.getValue(ORDER_TYPE,"") == DINE_IN) {
                tabCartItemModel.orderType = "DineIn"
                tabCartItemModel.guestIndexForDineIn = viewModel.dineInHeaderPosition
            }

            viewModel.manualSaleCartLogicNew(cartItemsList, tabCartItemModel, ADD)
            binding.llKeypad.edtItemName.text?.clear()

        }

    }

    // To generate unique time stamp for new item
    fun randomOfflineId(): String {
        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        return timeStampFinal
    }

    private fun callbackForDialog() {
        setFragmentResultListener("request_key_item_rename") { resultKey: String, bundle: Bundle ->
            val data = bundle.getString("item_name")
            cartItemModel.name = data.toString()
            viewModel.cartModel?.taxlistDynamic = arrayListOf()
            prefProvider.setValue(
                Constants.taxListDynamic,
                Gson().toJson(viewModel.cartModel?.taxlistDynamic)
            )
            cartItemsList?.forEach { items ->
                items.taxes?.forEach { taxData ->
                    taxData.subTotalAmount = 0.0
                    taxData.totalTaxTypePrice = 0.0
                }
            }
            cartItemModel.let {
                viewModel.manualSaleCartLogicNew(cartItemsList, it, Constants.UPDATE)
            }


        }
        setFragmentResultListener("request_key_discount_order") { _: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            val value = bundle.getDouble("value")
            /*Added by Rahul for solving Discount issue */
            if (viewModel.cartModel == null) {
                runBlocking {
                    reasignCartModelFromDB()
                }
            }


            if (value == 0.0) {
                Log.d("FRAGMENT_RESULT", value.toString())
                binding.tvDiscountText.text = "Discount"
                binding.txtDiscount.text = ""


                viewModel.cartModel?.apply {
                    discountSelectdValue = 0.0
                    discountPrice = 0.0
                    discountType = result?.discountType ?: ""
                }
                prefProvider.setValue(Constants.discountType, result?.discountType ?: "")

            } else {
                prefProvider.setValue(Constants.discountType, result?.discountType ?: "")

                viewModel.cartModel?.apply {
                    discountType = result?.discountType ?: ""
                    discountSelectdValue = value
                    discountPrice = value

                }

                /*if (result!!.discountType.equals(AMOUNT)){
                    viewModel.cartModel?.discountPrice=value
                }else if (result!!.discountType.equals(PERCENTAGE)){

                }*/

            }
            if (result != null /*&& viewModel.totalPrice != 0.0*/) {

                orderDiscount = result.percentage

                val discountApplyPrice = viewModel.totalPrice
                val price = discountApplyPrice - orderDiscount

                if (orderDiscount > viewModel.currentTotalPrice)
                    orderDiscount = viewModel.currentTotalPrice

                manualCartModel?.let {
                    manualCartModel?.discountPrice = orderDiscount
                    manualCartModel?.discountSelectdValue = value
                    manualCartModel?.discountType = result.discountType
                    if (result.id != -1) {
                        manualCartModel?.discountId = result.id
                    }
                    prefProvider.setValue(
                        Constants.discountType,
                        manualCartModel?.discountType.toString()
                    )
                    prefProvider.setValue(
                        Constants.discountSelectedValue,
                        value.toString()
                    )
                    viewModel.updateCartModel(it)
                }

                prefProvider.setValue(Constants.discountType, result?.discountType ?: "")
                prefProvider.setValue(Constants.discountSelectedValue, value.toString())


            }

        }
        setFragmentResultListener("request_key_note") { requestKey: String, bundle: Bundle ->
            val note = bundle.getString("note")
            val isOrderNote = bundle.getBoolean("isOrderNote")
            if (isOrderNote) {
                viewModel.manualCartOrderNote=note.toString()
                manualCartModel?.let {
                    manualCartModel?.note = note.toString()
                    prefProvider.setValue(
                        Constants.orderNote,
                        manualCartModel?.note.toString()
                    )
                    viewModel.updateCartModel(it)
                }

            } else {
                cartItemModel.note = note.toString()
                viewModel.cartModel?.taxlistDynamic = arrayListOf()
                prefProvider.setValue(
                    Constants.taxListDynamic,
                    Gson().toJson(viewModel.cartModel?.taxlistDynamic)
                )
                cartItemsList?.forEach { items ->
                    items.taxes?.forEach { taxData ->
                        taxData.subTotalAmount = 0.0
                        taxData.totalTaxTypePrice = 0.0
                    }
                }
                cartItemModel.let {
                    viewModel.manualSaleCartLogicNew(
                        cartItemsList,
                        it,
                        Constants.UPDATE
                    )
                }
            }
        }
        setFragmentResultListener("request_key_discount") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbDiscount>("data")
            if (result != null) {
                val pos = mPostion
                LogUtil.logE(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                prefProvider.setValue(Constants.discountType, result.discountType)

                if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                    val cartItem = manualSaleCartAdapterNew.getItem(pos)
                    var disPrice = calculateDiscountPercentage(
                        manualSaleCartAdapterNew.getItem(pos).price,
                        result.percentage
                    )
                    LogUtil.logE(TAG, "discountPrice:  $disPrice")
                    manualCartModel?.discountPrice = disPrice
                    manualCartModel?.discountId = result.id
                    manualCartModel?.discountType = result.discountType

                    LogUtil.logE(TAG, "cartModelPArseMsd   ${Gson().toJson(manualCartModel)}")
                    manualCartModel?.taxlistDynamic = arrayListOf()
                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(manualCartModel?.taxlistDynamic)
                    )
                    prefProvider.setValue(Constants.isManual, "1")
                    prefProvider.setValue(
                        Constants.discountPrice,
                        manualCartModel?.discountPrice.toString()
                    )

                    cartItemsList?.forEach { items ->
                        items.taxes?.forEach { taxData ->
                            taxData.subTotalAmount = 0.0
                            taxData.totalTaxTypePrice = 0.0
                        }
                    }
                    viewModel.manualSaleCartLogicNew(cartItemsList, cartItem, Constants.UPDATE)

                } else if (result.discountType == "Amount") {
                    val cartItem = manualSaleCartAdapterNew.getItem(pos)
                    manualCartModel?.discountPrice =
                        MethodUtils.roundOffAmountDouble(result.percentage * cartItem.itemQuantity)
                    manualCartModel?.discountType = result.discountType
                    manualCartModel?.taxlistDynamic = arrayListOf()
                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(manualCartModel?.taxlistDynamic)
                    )
                    prefProvider.setValue(Constants.isManual, "1")
                    prefProvider.setValue(
                        Constants.discountPrice,
                        manualCartModel?.discountPrice.toString()
                    )

                    cartItemsList?.forEach { items ->
                        items.taxes?.forEach { taxData ->
                            taxData.subTotalAmount = 0.0
                            taxData.totalTaxTypePrice = 0.0
                        }
                    }
                    viewModel.manualSaleCartLogicNew(cartItemsList, cartItem, Constants.UPDATE)

                } else {
                    val cartItem = manualSaleCartAdapterNew.getItem(pos)
                    manualCartModel?.discountPrice = 0.0
                    manualCartModel?.discountType = ""
                    manualCartModel?.taxlistDynamic = arrayListOf()
                    prefProvider.setValue(
                        Constants.taxListDynamic,
                        Gson().toJson(manualCartModel?.taxlistDynamic)
                    )
                    prefProvider.setValue(Constants.isManual, "1")
                    prefProvider.setValue(
                        Constants.discountPrice,
                        manualCartModel?.discountPrice.toString()
                    )

                    cartItemsList?.forEach { items ->
                        items.isManualSaleItem = true
                        items.taxes?.forEach { taxData ->
                            taxData.subTotalAmount = 0.0
                            taxData.totalTaxTypePrice = 0.0
                        }
                    }
                    viewModel.manualSaleCartLogicNew(cartItemsList, cartItem, Constants.UPDATE)

                }

            }
        }
    }

    /*Added by Rahul for solving Discount issue */
    private suspend fun reasignCartModelFromDB() {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.cartModel = viewModel.getManualSaleFromCart(
                prefProvider.getValueInt(
                    Constants.EMPLOYEE_ID,
                    0
                )
            )
        }.await()

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

        manualSaleCartAdapterNew = ManualSaleCartAdapterNew()
        manualSaleCartAdapterNew.setCallBack(this)
        manualSaleCartAdapterNew.setItemCallBack(this)
        binding.rvSaleCart.adapter = manualSaleCartAdapterNew


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


        LogUtil.logE(
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

            LogUtil.logE(TAG, "InsideLoyalty")
            LogUtil.logE(TAG, Gson().toJson(viewModel.redeemLoyaltyInfo))
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
        if (viewModel.cartModel?.discountSelectdValue != 0.0 && viewModel.cartModel?.discountSelectdValue != null) {
            binding.tvDiscountText.text = "Discount (${viewModel.cartModel?.discountSelectdValue}%)"
        } else {
            binding.tvDiscountText.text = "Discount"
        }
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
        Log.d("DISCOUNT_ISSUE", "itemCalculation called 3")
        viewModel.itemCalculationNew(
            viewModel.cartModel,
            cartItemsList,
            binding.txtTotal,
            requireContext(),
            isFromManualSales = true
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
            if (rolePermission.hasCashLogPermission(binding.root)) {
                findNavController().navigate(R.id.action_manualSaleNew_to_Cashlog)
            }
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
            if (rolePermission.hasCustomerPermission(binding.root)) {
                findNavController().navigate(R.id.action_manualSaleNew_to_customer)
                closeDialog(dialog)
            }
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
        LogUtil.logE(TAG, "originalPrice  $originalPrice")
        LogUtil.logE(TAG, "disPrice  $disPrice")
        return if (disPrice <= originalPrice) {
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
                    cartModelsList?.get(0)?.let { putDouble("orderDiscount", it.discountPrice) }
                    putParcelable("model", data)
                }

                manualSaleCartAdapterNew.viewBinderHelper.closeLayout(pos.toString())
                findNavController().navigate(
                    R.id.action_manualSaleNew_to_addDiscountDialog,
                    bundle
                )
            }

        }
    }

    override fun onItemClicked(model: TbCartItem, position: Int) {
        LogUtil.logE(TAG, "ItemPosition: $position")
        viewModel.setPosition(position)
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = lp
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

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
        initialItemQuantity = model.itemQuantity
        totalquantity = 0
        var qty = model.itemQuantity
        totalquantity = qty

        txtQty.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                if (s.toString().isNotEmpty()) {
                    val enteredString = s.toString()
                    totalquantity = s.toString().toInt()
                    if (enteredString.startsWith("0")) {

                        if (enteredString.length > 0) {
                            txtQty.setText(enteredString.substring(1))
                        } else {
                            txtQty.setText("")
                        }
                    } else if (s.toString().trim().isNotEmpty() && s.toString().toInt() > 1000) {
                        txtQty.setText("1000")
                        totalquantity = txtQty.text.toString().toInt()
                    }
                    txtQty.setSelection(txtQty.text!!.length)
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
                ((model.price * model.itemQuantity) - (model.discountPrice * model.itemQuantity))
            )
        } else {
            txtTitle.text = model.name
        }

        imgClose.setOnClickListener {
            dialog.dismiss()
        }

        txtSave.setOnClickListener {

            if (edtItemName.text.toString().trim().isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter custom item name")
                return@setOnClickListener
            }
            dialog.dismiss()
            val itemCost = model.price

            if (model.discountPrice != 0.0) {
                val dis = (model.discountPrice * model.itemQuantity) / model.itemQuantity

                model.discountPrice =
                    String.format("%.2f", (dis)).toDouble()
            }


            model.note = edtNote.text.toString().trim()
            LogUtil.logE("TAG", "notes${edtNote.text.toString().trim()}")
            if (txtQty.text.toString().isNotEmpty()) {
                model.itemQuantity = txtQty.text.toString().toInt()
            } else {
                model.itemQuantity = 1
            }

            cartItemsList?.get(position)?.name =
                edtItemName.text.toString().trim().replace("\\s+".toRegex(), " ")
            Log.d(TAG, "onItemClicked: name  " + edtItemName.text.toString())

            model.price = String.format("%.2f", (itemCost)).toDouble()

            viewModel.setPosition(position)

            LogUtil.logE(TAG, "ItemPosition: $position")
            viewModel.cartModel?.taxlistDynamic = arrayListOf()
            cartItemsList?.forEach { items ->
                items.taxes?.forEach { taxData ->
                    taxData.subTotalAmount = 0.0
                    taxData.totalTaxTypePrice = 0.0
                }
            }
            viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.UPDATE)
            viewModel.boldPosNeedToRefresh = true
        }

        llPlus.setOnClickListener {
            totalquantity += 1
            txtQty.setText(totalquantity.toString())
        }
        llMinus.setOnClickListener {

            if (totalquantity > 1) {
                totalquantity -= 1
            }
            txtQty.setText(totalquantity.toString())
        }

        btnRemove.setOnClickListener {
            LogUtil.logE(TAG, "modelRemove:  ${Gson().toJson(model)}")
            viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.DELETE)
            dialog.dismiss()
        }

        btnAddDiscount.setOnClickListener {
            viewModel.setPosition(position)
            setCurrentSubTotal(txtQty.text.toString().toInt())
            setFragmentResultListener("request_key_discount_details") { requestKey: String, bundle: Bundle ->
                val result = bundle.getParcelable<TbDiscount>("data")
                if (result != null) {
                    LogUtil.logE(TAG, "GetDiscountResult:  ${Gson().toJson(result)}")
                    if (result.discountType == requireContext().getString(R.string.disc_percentage)) {

                        model.discountPrice = calculateDiscountPercentage(
                            model.price,
                            result.percentage
                        )


                        if (model.discountPrice > viewModel.currentTotalPrice) {
                            model.discountPrice = viewModel.currentTotalPrice
                            Log.e(
                                "Discount Tracking Pays",
                                "Discount greater  = ${model.discountPrice} and Current price = ${viewModel.currentTotalPrice}"
                            )

                        }

                        if (viewModel.clickedItemQuantity > 1) {
                            model.discountPrice =
                                model.discountPrice / viewModel.clickedItemQuantity

                        }

                        model.discountId = result.id
                        model.discountType = result.discountType
                        model.isManualSales = true
                        viewModel.cartModel?.taxlistDynamic = arrayListOf()
                        cartItemsList?.forEach { items ->
                            items.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }
                        viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            ((model.price * totalquantity) - model.discountPrice * totalquantity)
                        )

                    } else if (result.discountType == "Amount") {

                        model.discountPrice = result.percentage
                        model.discountId = result.id
                        model.discountType = result.discountType
                        model.isManualSales = true

                        viewModel.cartModel?.taxlistDynamic = arrayListOf()
                        cartItemsList?.forEach { items ->
                            items.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }
                        viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            ((model.price * totalquantity) - model.discountPrice * totalquantity)
                        )
                    } else {
                        model.discountPrice = 0.0
                        model.discountType = ""
                        model.isManualSales = true
                        viewModel.cartModel?.taxlistDynamic = arrayListOf()
                        cartItemsList?.forEach { items ->
                            items.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }
                        viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.UPDATE)
                    }
                } else {
                    model.discountPrice = 0.0
                    model.discountType = ""
                    model.isManualSales = true
                    viewModel.cartModel?.taxlistDynamic = arrayListOf()
                    cartItemsList?.forEach { items ->
                        items.taxes?.forEach { taxData ->
                            taxData.subTotalAmount = 0.0
                            taxData.totalTaxTypePrice = 0.0
                        }
                    }
                    viewModel.manualSaleCartLogicNew(cartItemsList, model, Constants.UPDATE)


                }
            }
            val bundle = Bundle().apply {
                putInt("totalquantity", initialItemQuantity)
                putBoolean("isFromDetails", true)

                if (cartItemsList?.isNotEmpty() == true) {

                    var totalItemswithQuantity = initialItemQuantity

                    var perItemDiscount = 0.0
                    if (viewModel.cartModel?.discountPrice != 0.0) {
                        if (totalItemswithQuantity == 0) {
                            totalItemswithQuantity = 1
                        }
                        perItemDiscount =
                            MethodUtils.roundOffAmountDouble(
                                (viewModel.cartModel?.discountPrice
                                    ?: 0.0) / totalItemswithQuantity
                            )
                    }

                    viewModel.cartModel
                        ?.let { putDouble("orderDiscount", it.discountPrice) }

                    viewModel.cartModel?.discountPrice?.let { it1 ->
                        putDouble(
                            "orderDiscountPrice",
                            it1
                        )
                    }
                    putString("orderDiscountType", viewModel.cartModel?.discountType)
                    viewModel.cartModel?.discountSelectdValue?.let { it1 ->
                        putDouble(
                            "selectedvalue",
                            it1
                        )
                    }
                    putDouble("itemOrderDiscount", perItemDiscount)
                }
                putParcelable("model", model)
            }
            bundle.putString("isFrom", "itemDiscountManual")
            if (prefProvider.isAdmin() || prefProvider.isManager()) {

                if (findNavController().currentDestination?.id != R.id.addDiscountDialog) {
                    findNavController().navigate(
                        R.id.action_manualSaleNew_to_addDiscountDialog,
                        bundle
                    )
                }
            } else {
                findNavController().navigate(
                    R.id.action_manualSaleNew_to_pascodeManagerDailog, bundle
                )
            }

        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.dismiss()
        dialog.show()
    }

    override fun onDestroyView() {
        viewModel.manualCartOrderNote=""
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
    }
}