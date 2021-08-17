package com.android.pos.ui.fragments.manualsales

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.SALE_CUSTOMER_NAME
import com.android.pos.databinding.FragmentManualSaleNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ManualSaleCartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.settings.tax.TaxListViewModel
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.lang.StringBuilder
import javax.inject.Inject

@AndroidEntryPoint
class ManualSaleNew : Fragment(), ManualSaleCartAdapter.ManualSaleInterface {
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"
    private var cartList: List<CartModel>? = null
    private lateinit var cartAdapter: ManualSaleCartAdapter
    private var cartItemModel = TbItem()
    private val viewModel by viewModels<ManualSaleViewModel>()

    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var discountList: List<TbDiscount>? = null
    private var taxList: List<TaxData>? = null

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSaleNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.footer.imgInfo.visibility = View.GONE
        binding.footer.imgDelete.visibility = View.VISIBLE
        getServiceCharge()
        getTaxList()
        getDiscountList()
        return binding.root
    }

    private fun getDiscountList() {
        dashboardViewModel.discountList.observe(requireActivity(), {
            discountList = it.data
        })
    }


    private fun getTaxList() {
        dashboardViewModel.taxList.observe(requireActivity(), {
            Log.e(TAG, "getTaxList:  ${Gson().toJson(it.data)}")
            taxList = it.data
            getCartList()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgSearch.visibility = View.GONE
        binding.layoutMenu.autoSearch.visibility = View.GONE
        onConfig()
        onClickKeypad()
        getCartList()
        onClick()
        listner()
    }

    private fun getCartList() {

        viewModel.cartList.observe(requireActivity(), {
            cartList = it
            Log.e(TAG, "cartListBeforeTax  ${Gson().toJson(cartList)}")
            if (cartList?.isNotEmpty()!!) {
                cartList?.get(0)?.items?.forEach {
                    it.taxes = taxList
                }
                cartAdapter.setList(cartList?.get(0)?.items)

                viewModel.itemCalculation(
                    cartList?.get(0)?.items,
                    binding.txtChargeAmount,
                    dashboardViewModel.serviceCharges.value?.data
                )
            } else {

                cartAdapter.clearList()

            }


        })
    }

    private fun getServiceCharge() {
        viewModel.serviceCharge.observe(requireActivity(), {
            Log.e(TAG, "serviceCharge: ${Gson().toJson(it)}")
            serviceChargesList = it.data
        })
    }

    private fun listner() {
        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                prefProvider.setValue(
                    SALE_CUSTOMER_NAME,
                    result.first_name + " " + result.last_name
                )
                binding.txtCustomerName.text = result.first_name + " " + result.last_name
                binding.txtCrtNewCustomer.text = "Remove Customer"
            }
        }
    }

    private fun onClick() {
        binding.relAddCustomer.setOnClickListener {
            dialogMenu()
        }

        binding.btnPay.setOnClickListener {

            if (binding.txtChargeAmount.text.toString() != "$0.00") {
                val bundle = Bundle()
                bundle.putDouble("totalPrice", viewModel.totalPrice)
                bundle.putDouble("subTotalPrice", viewModel.subTotalPrice)
                bundle.putDouble("totalTax", viewModel.totalTax)
                bundle.putDouble("totalDiscount", viewModel.totalDiscount)
                bundle.putDouble("totalServiceCharge", viewModel.totalServiceCharge)

                findNavController().navigate(R.id.action_manualSaleNew_to_paymentFragment, bundle)
            }
        }

        binding.footer.linearMore.setOnClickListener {
            dialogPOSMenu()
        }

        binding.llInfo.setOnClickListener {
            showPopupWindow(it)
        }
        binding.footer.imgDelete.setOnClickListener {

            if (cartList?.isNotEmpty() == true) {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.delete_items_message)
                ) {
                    positiveButton(getString(R.string.tv_delete)) {
                        viewModel.deleteCart()

                        binding.txtChargeAmount.setText("$0.00")

                    }
                    negativeButton(R.string.tv_cancel) {

                    }
                }
            }

        }
        binding.txtCrtNewCustomer.setOnClickListener {
            if (prefProvider.getValue(SALE_CUSTOMER_NAME, "").toString().isNotEmpty()) {
                binding.txtCrtNewCustomer.text = "Add Customer"
                binding.txtCustomerName.text = "Add Customer"
                prefProvider.setValue(SALE_CUSTOMER_NAME, "")
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
                    binding.txtChargeAmount.setText("$0.00")
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

        }
    }

    private fun onClickKeypad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.tvTwo.setOnClickListener {

            calculateValue("2", false)
        }
        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)
        }
        binding.llKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }
        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }
        binding.llKeypad.tvSix.setOnClickListener {

            calculateValue("6", false)
        }
        binding.llKeypad.tvSeven.setOnClickListener {

            calculateValue("7", false)
        }
        binding.llKeypad.tvEight.setOnClickListener {

            calculateValue("8", false)
        }
        binding.llKeypad.tvNine.setOnClickListener {

            calculateValue("9", false)
        }
        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)

        }
        binding.llKeypad.imgAdd.setOnClickListener {
            // binding.txtAmount.setText( "0.00")

            if (!(binding.txtAmount.text!!.trim().toString()
                    .equals("0.00")) && (!(binding.txtAmount.text!!.trim().toString()
                    .equals("$0.00"))) && (!binding.txtAmount.text!!.trim().toString().equals("0"))
            ) {
                addItemToCart(binding.txtAmount.text.toString(), true)
                binding.txtAmount.setText("0.00")
            }


        }
        binding.llKeypad.tvBack.setOnClickListener {
            if (binding.txtAmount.text.toString().isNotEmpty()) {
                calculateValue("", true)
            }

        }
    }

    private fun addItemToCart(price: String, isAdd: Boolean) {
        val replaceCurrency = price.replace("$", "")
        Log.e(TAG, "replaceCurrency  ${replaceCurrency}")
        var count = 0
        if (isAdd) {
            var model = TbItem()
            var count = 0

            if (cartList?.isNotEmpty() == true) {
                count =
                    cartList?.get(0)?.items?.get(cartList?.get(0)?.items!!.size - 1)?.customItemCount!!
            }
            count++

            Log.e(TAG, "getcount:  ${count}")
            model.customItemCount = count
            model.name = "Custom Item ${count}"
            if (binding.edtItemName.text?.isNotEmpty() == true) {
                model.name = binding.edtItemName.text.toString()
                count--
                model.customItemCount = count
            }

            model.price = replaceCurrency.toDouble()
            model.itemQuantity = 1
            model.discountPrice = 0.0
            model.isManualSales = true

            if (cartAdapter.getList().isEmpty()) {
                model.customItemID = 1
            } else {

                var id = cartAdapter.getItem(cartAdapter.getList().size - 1).customItemID
                id++
                Log.e(TAG, "CustomItemid:  ${id}")
                model.customItemID = id

            }


            Log.e(TAG, "Parsemodel  ${Gson().toJson(model)}")
            viewModel.cartLogic(cartList, model, ADD)
            binding.edtItemName.text?.clear()

        }

    }

    private fun onConfig() {
        binding.txtAmount.addTextChangedListener(AmountTextWatcher(binding.txtAmount, true))
        binding.txtAmount.setText("0.00")
        if (prefProvider.getValue(SALE_CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.txtCustomerName.text = prefProvider.getValue(SALE_CUSTOMER_NAME, "")
            binding.txtCrtNewCustomer.text = "Remove Customer"
        }
        cartAdapter = ManualSaleCartAdapter()
        cartAdapter.setCallBack(this)
        binding.rvSaleCart.adapter = cartAdapter

        object : SwipeHelper(activity, binding.rvSaleCart) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {
                underlayButtons.add(
                    UnderlayButton(
                        "Rename",
                        0,
                        Color.parseColor("#08CAE3")
                    ) { pos ->

                        Log.e(TAG, "pospospos  ${pos}")
                        setFragmentResultListener("request_key_item_rename") { resultKey: String, bundle: Bundle ->
                            val data = bundle.getString("item_name")
                            cartItemModel.name = data.toString()
                            cartItemModel?.let {
                                viewModel.cartLogic(cartList, it, Constants.UPDATE)
                            }


                        }
                        cartItemModel = cartAdapter.getItem(pos)
                        val bundle: Bundle = bundleOf("item_name" to cartItemModel.name)

                        findNavController().navigate(
                            R.id.action_manualSaleNew_to_itemRenameDialog,
                            bundle
                        )

                    }

                )

                underlayButtons.add(UnderlayButton(
                    "Add Note",
                    0,
                    Color.parseColor("#FA9905")
                ) { pos ->


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
                    cartItemModel = cartAdapter.getItem(pos)
                    val bundle = Bundle().apply {
                        putString("note", cartItemModel.note)
                    }

                    findNavController().navigate(
                        R.id.action_manualSaleNew_to_addNoteDialog,
                        bundle
                    )


                })

                underlayButtons.add(UnderlayButton(
                    "Add Discount",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    setFragmentResultListener("request_key_discount") { requestKey: String, bundle: Bundle ->
                        val result = bundle.getParcelable<TbDiscount>("data")
                        if (result != null) {
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

                            } else if (cartAdapter.getItem(pos).price > result.percentage) {
                                val cartModel = cartAdapter.getItem(pos)
                                cartModel.discountPrice = result.percentage
                                cartModel.isDiscountDefault = false
                                cartModel.discountType = result.discountType
                                viewModel.cartLogic(cartList, cartModel, Constants.UPDATE)



                                Log.e(TAG, "DiscountInDollar")
                            }


                        }
                    }
                    val bundle = bundleOf("isFromDetails" to false)
                    findNavController().navigate(
                        R.id.action_manualSaleNew_to_addDiscountDialog,
                        bundle
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

                                Log.e(TAG, "item ${Gson().toJson(item)}")
                                viewModel.cartLogic(cartList, item, Constants.DELETE)
                            }
                            negativeButton(R.string.tv_cancel) {
                                // Do negative stuff here
                            }
                        }

                    })
            }

        }

        binding.layoutMenu.txtProducts.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutMenu.txtKeypad.setTextColor(resources.getColor(R.color.txt_color_blue))

    }

    private fun calculateValue(number: String, delete: Boolean) {

        if (delete && binding.txtAmount.text?.length!! > 1) {

            binding.txtAmount.setText(removeLastCharacter(binding.txtAmount.text.toString()))
        } else if (binding.txtAmount.text?.trim()!!.equals("0.00")) {
            binding.txtAmount.setText("")
            binding.txtAmount.append(number)
        } else {
            binding.txtAmount.append(number)
        }
        addItemToCart(binding.txtAmount.text.toString(), false)
        val str = binding.txtAmount.text.toString().replace("""[$]""".toRegex(), "")
        //binding.txtAmount.setText("${binding.txtAmount.text.toString().replace("""[$]""".toRegex(), "")}")
        var newText = StringBuilder(str).insert(0, "%").toString()


        Log.e(
            TAG,
            "afterTextSet  ${binding.txtAmount.text.toString().replace("""[$]""".toRegex(), "%")}"
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
        val txtSave: AppCompatTextView = dialog.findViewById(R.id.txtSave)
        val txtTitle: AppCompatTextView = dialog.findViewById(R.id.txtTitle)
        val txtQty: AppCompatEditText = dialog.findViewById(R.id.txtQty)
        val llPlus: LinearLayoutCompat = dialog.findViewById(R.id.llPlus)
        val llMinus: LinearLayoutCompat = dialog.findViewById(R.id.llMinus)
        val btnRemove: AppCompatTextView = dialog.findViewById(R.id.btnRemove)
        val btnAddDiscount: AppCompatTextView = dialog.findViewById(R.id.btnAddDiscount)
        val edtNote: AppCompatEditText = dialog.findViewById(R.id.edtNote)
        val edtItemName: AppCompatEditText = dialog.findViewById(R.id.edtItemName)

        edtNote.setText(model.note)
        var qty = model.itemQuantity

        edtItemName.setText(model.name)
        txtQty.setText(qty.toString())
        if (model.discountPrice != 0.0) {
            txtTitle.text = model.name + "  $" + String.format(
                "%.2f",
                (model.price - model.discountPrice)
            )
        } else {
            txtTitle.text = model.name + "  $" + String.format(
                "%.2f",
                model.price
            )
        }

        imgClose.setOnClickListener {
            dialog.dismiss()
        }

        txtSave.setOnClickListener {
            dialog.dismiss()
            var itemCost = (model.price / model.itemQuantity).toDouble()

            Log.e(TAG, "discountOriginal ${model.discountPrice}")
            if (model.discountPrice != 0.0) {
                val dis = model.discountPrice / model.itemQuantity
                Log.e(TAG, "discountdis:  ${dis}")

                model.discountPrice =
                    String.format("%.2f", (dis * txtQty.text.toString().toInt())).toDouble()
                Log.e(
                    TAG,
                    "discountCountPrice  ${
                        String.format("%.2f", (dis * txtQty.text.toString().toInt())).toDouble()
                    }"
                )
            }


            model.note = edtNote.text.toString().trim()
            model.itemQuantity = txtQty.text.toString().toInt()
            model.name = edtItemName.text.toString()

            model.price = String.format("%.2f", (itemCost * model.itemQuantity)).toDouble()

            viewModel.cartLogic(cartList, model, Constants.UPDATE)
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
                            model.price,
                            result.percentage
                        )
                        model.discountId = result.id
                        model.discountType = result.discountType
                        model.isManualSales = true
                        Log.e(TAG, "insideDiscountmodel:  ${Gson().toJson(model)}")
                        viewModel.cartLogic(cartList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            (model.price - model.discountPrice)
                        )

                    } else if (model.price > result.percentage) {

                        model.discountPrice = result.percentage
                        model.discountType = result.discountType
                        model.isManualSales = true

                        viewModel.cartLogic(cartList, model, Constants.UPDATE)
                        txtTitle.text = model.name + "  $" + String.format(
                            "%.2f",
                            (model.price - model.discountPrice)
                        )
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
        dialog.show()


    }

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
        txtTotalAmount.text = binding.txtChargeAmount.text.toString()
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
            findNavController().popBackStack()
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
        val disPrice = Math.round((originalPrice * percentage) / 100).toDouble()
        Log.e(TAG, "disPrice  ${disPrice}")
        return if (disPrice < originalPrice) {
            disPrice
        } else {
            0.0
        }


    }
}