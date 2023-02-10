package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.Window
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.responseModel.TimeDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewCustomDisplayBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.invisible
import com.android.pos.utils.extensions.isVisible
import com.android.pos.utils.extensions.visible

class CustomDisplay(
    display: Display,
    context: Context,
    val lifecycleOwner: LifecycleOwner,
    val dashBoardCategoryViewModel: DashBoardCategoryViewModel,
    val passcodeViewModel: PasscodeViewModel
) : Presentation(context, display), MyCallback, DineInAdapter.DineInCallback {

    private lateinit var dineInCartAdapter: DineInAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var binding: ViewCustomDisplayBinding
    lateinit var prefProvider: PrefProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ViewCustomDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefProvider = PrefProvider(context)
        setupList()
        setupTaxAdapter()
    }

    override fun onDisplayChanged() {
        super.onDisplayChanged()

        dashBoardCategoryViewModel.mAllWords(
            prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(lifecycleOwner) {
            it?.let {
                updateCustomerDisplay(it)
            }
        }
    }

    private fun setupList() {

        cartAdapter = CartAdapter()
        cartAdapter.setCallback(this)

        dineInCartAdapter = DineInAdapter()
        dineInCartAdapter.setListner(this)

        binding.rvCartList.layoutManager = LinearLayoutManager(context)

        if (prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT) == Constants.DINE_IN) {
            binding.rvCartList.adapter = dineInCartAdapter
        } else {
            binding.rvCartList.adapter = cartAdapter
        }

    }

    private fun updateCustomerDisplay(cartList: List<CartModel>) {
        if (this::binding.isInitialized) {

            if (cartList.isEmpty()) {

                passcodeViewModel.timeDetails.observe(lifecycleOwner) { res ->
                    res.data?.let { tdr ->
                        binding.currentTime.text = tdr.data.time
                        binding.currentDate.text = tdr.removeWhiteSpaces()
                    }
                }

                binding.splashLayout.visibility = View.VISIBLE
                binding.mainCartLayout.visibility = View.GONE

            } else {

                binding.mainCartLayout.visibility = View.VISIBLE
                binding.splashLayout.visibility = View.GONE

                if (prefProvider.getValue(
                        Constants.ORDER_TYPE, Constants.TAKEOUT
                    ) == Constants.DINE_IN
                ) {
                    if (cartList[0].dineInList?.isNotEmpty() == true) {
                        val dineInList = cartList[0].dineInList

                        dineInCartAdapter.setList(
                            dineInList?.toCollection(arrayListOf()) ?: arrayListOf()
                        )
                    }
                } else {
                    cartList[0].items?.toCollection(arrayListOf())?.let { it1 ->
                        cartAdapter.setList(it1)
                    }
                    displayCustomer()
                }
                setupTotals(cartList)

            }
        }
    }

    fun showSurcharge(isInCheckout: Boolean) {
        if (isInCheckout) {
            if (MethodUtils.isEnableCashDiscount(context)) {
                binding.linearCashDiscount.visible()
                if (prefProvider.getValue(
                        Constants.OPTION_TYPE,
                        "CashDiscount"
                    ) == "CashDiscount"
                ) {
                    binding.labelCashSurcharge.text = "Cash Discount"
                } else {
                    binding.labelCashSurcharge.text = "SurCharge"
                }
            } else {
                binding.linearCashDiscount.gone()
            }
        }
    }

    private fun setupTotals(cartList: List<CartModel>) {
        dashBoardCategoryViewModel.apply {

            if (order_note.isNotEmpty()) {
                binding.relativeOrderNotes.visibility = View.VISIBLE
                binding.txtOrderNote.text = order_note
            } else {
                binding.relativeOrderNotes.visibility = View.GONE
            }

            binding.txtSubTotal.text = MethodUtils.roundOffAmount(subTotalPrice)
            binding.txtTax.text = MethodUtils.roundOffAmount(totalTax)
            binding.txtServiceCharge.text = MethodUtils.roundOffAmount(totalServiceCharge)
            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(totalDiscount)
            binding.txtNoncashAdj.text = MethodUtils.roundOffAmount(cashdiscountAmount)
            //showSurcharge(true)

            dashBoardCategoryViewModel.apply {
                val data: TbCustomer? = prefProvider.getCustomerData()
                if (data != null) {
                    if (loyaltyPointCondition(data) && redeemLoyaltyInfo.needToApplyLoyalty) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._80sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.VISIBLE
                        binding.lblLoyaltyPoints.visibility = View.VISIBLE

                        binding.txtLabelLoyaltyAmounts.visibility = View.VISIBLE
                        binding.checkloylaty.visibility = View.GONE
                        binding.txtLoyaltyAmount.text = "- $${
                            String.format(
                                "%.2f", redeemLoyaltyInfo.usedLoyaltyAmount
                            )
                        }"
                        binding.txtLoyaltyPoints.text = "${redeemLoyaltyInfo.usedLoyaltyPoints}"
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._60sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.GONE
                        binding.lblLoyaltyPoints.visibility = View.GONE
                    }
                }
            }

            if (taxBirfurcationAdapter.taxlist.size == 0) {
                binding.imgDropdown.gone()
            } else {
                binding.imgDropdown.visible()
            }

            if (cartList[0].taxlistDynamic?.isNotEmpty() == true) {
                taxBirfurcationAdapter.setList(cartList[0].taxlistDynamic as ArrayList<TaxData>)
            }

            itemCalculation(cartList, binding.txtTotal, context)
        }
    }

    private fun displayCustomer() {

        val name = prefProvider.getValue(Constants.CUSTOMER_NAME, "")
        if (name.isNotEmpty()) {
            binding.txtCustomerName.visible()
            binding.txtLoyaltyPointsLabel.visible()
            binding.txtLoyaltyPointsLabel.text =
                "Loyalty Points: ${dashBoardCategoryViewModel.redeemLoyaltyInfo.usedLoyaltyPoints}"
            binding.txtCustomerName.text = name

        } else {
            binding.txtLoyaltyPointsLabel.invisible()
            binding.txtCustomerName.invisible()

        }

    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        taxBirfurcationAdapter.setList(arrayListOf())
    }

    fun onTaxClicked(shouldShow: Boolean) {
        if (this::binding.isInitialized && this::taxBirfurcationAdapter.isInitialized) {
            if (shouldShow) {

                if (taxBirfurcationAdapter.taxlist.size == 1) {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._60sdp).toInt()
                    } else {
                        if (binding.relativeLoylatyPoints.isVisible()) {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._80sdp).toInt()
                        } else {
                            binding.liinearInfoLayout.layoutParams.height =
                                resources.getDimension(R.dimen._70sdp).toInt()
                        }
                    }
                } else if (taxBirfurcationAdapter.taxlist.size == 2) {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._110sdp).toInt()
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._100sdp).toInt()
                    }
                } else {
                    if (dashBoardCategoryViewModel.order_note.isNotEmpty()) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._110sdp).toInt()
                    } else {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._100sdp).toInt()
                    }

                }

                binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                binding.relativeDynamicTax.visible()

            } else {

                if (binding.relativeLoylatyPoints.isVisible()) {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._80sdp).toInt()
                } else {
                    binding.liinearInfoLayout.layoutParams.height =
                        resources.getDimension(R.dimen._60sdp).toInt()
                }

                binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                binding.relativeDynamicTax.gone()

            }
        }
    }

    private fun TimeDetailsResponse?.removeWhiteSpaces(): CharSequence? {
        return this?.data?.date?.replace("\n", "")?.replace("  ", "")?.replace(",", ", ")
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int) {}

    override fun onHeaderSelected(position: Int) {}

    override fun onItemSelected(headerPosition: Int, position: Int, item: TbItem) {}

    override fun onCustomerClicked(position: Int, isRemoved: Boolean) {}

    override fun onItemDelete(position: Int, itemPosition: Int, data: TbItem) {}

    fun showThankYou(paidAmount: String) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()

            thankYouLayout.visible()
            txtPaidAmount.text = "Paid $paidAmount"
        }
    }

    fun onLogOutOrClockOut() {
        binding.apply {
            mainCartLayout.gone()
            thankYouLayout.gone()
            splashLayout.visible()
        }
    }

    fun showTableDetails(dineInListItems: java.util.ArrayList<DineInModel>) {
        val cartlist: java.util.ArrayList<CartModel> = arrayListOf()
        cartlist.add(CartModel().apply { dineInList = dineInListItems })
        updateCustomerDisplay(cartlist)
    }

    fun showTipsAdded(tipAmount: Double, WholetotalPrice: Double) {
        if (tipAmount == 0.00) {
            binding.tipLayout.gone()
        } else {
            binding.tipLayout.visible()
            val percentageTip = String.format(
                "%.0f", MethodUtils.calculatePercentageFromAmount(
                    tipAmount,
                    WholetotalPrice
                )
            )

            binding.tipPercentLabel.text = "Tip ($percentageTip%)"
            binding.txtTipGiven.text = "" + MethodUtils.roundOffAmount(tipAmount)

        }

    }
}
