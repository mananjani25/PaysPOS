package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.TimeDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewCustomDisplayBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInAdapter
import com.android.pos.ui.adapter.DineInAdapterCustomerDisplay
import com.android.pos.ui.adapter.boldpos.CartAdapterCustomerDisplay
import com.android.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.invisible
import com.android.pos.utils.extensions.visible

class CustomDisplay(
    display: Display,
    context: Context,
    val lifecycleOwner: LifecycleOwner,
    val dashBoardCategoryViewModel: DashBoardCategoryViewModel,
    val passcodeViewModel: PasscodeViewModel,
    val onPayNowClick: () -> Unit
) : Presentation(context, display), MyCallback, DineInAdapterCustomerDisplay.DineInCallback {

    private lateinit var dineInCartAdapter: DineInAdapterCustomerDisplay
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private lateinit var cartAdapter: CartAdapterCustomerDisplay
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
        binding.tvPayNow.setOnClickListener {
//            onDisplayChanged()
//            onPayNowClick
            Log.d("TAGGER", "PAY NOW CALLED")
        //Toast.makeText(context, "HELLO", Toast.LENGTH_SHORT).show()
        }
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

        cartAdapter = CartAdapterCustomerDisplay()
        cartAdapter.setCallback(this)

        dineInCartAdapter = DineInAdapterCustomerDisplay()
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
                    if (MethodUtils.isEnableCashDiscount(context)) {
                        binding.txtTotalLabel.gone()
                        binding.txtCashLabel.visible()
                        binding.txtCardLabel.visible()
                    } else {
                        binding.txtTotalLabel.visible()
                        binding.txtCashLabel.gone()
                        binding.txtCardLabel.gone()

                    }
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
            binding.txtSubTotal.text = MethodUtils.roundOffAmount(subTotalPrice)

            binding.txtTax.text = MethodUtils.roundOffAmount(totalTax)
            binding.txtServiceCharge.text = MethodUtils.roundOffAmount(totalServiceCharge)
            binding.txtDiscount.text = "-" + MethodUtils.roundOffAmount(totalDiscount)
            binding.txtNoncashAdj.text = MethodUtils.roundOffAmount(cashdiscountAmount)

            showSurcharge(false)

            if (taxBirfurcationAdapter.taxlist.size == 0) {
                binding.imgDropdown.gone()
            } else {
                binding.imgDropdown.visible()
            }

            if (cartList[0].taxlistDynamic?.isNotEmpty() == true) {
                taxBirfurcationAdapter.setList(cartList[0].taxlistDynamic as ArrayList<TaxData>)
            }

            itemCalculation(cartList, binding.txtTotal, context)
            binding.tvPayNow.text = "Pay " + binding.txtTotal.text.toString()
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
            dashBoardCategoryViewModel.apply {
                val data: TbCustomer? = prefProvider.getCustomerData()
                if (data != null) {
                    if (loyaltyPointCondition(data) && redeemLoyaltyInfo.needToApplyLoyalty) {
                        binding.liinearInfoLayout.layoutParams.height =
                            resources.getDimension(R.dimen._70sdp).toInt()
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
                            resources.getDimension(R.dimen._50sdp).toInt()
                        binding.relativeLoylatyPoints.visibility = View.GONE
                        binding.lblLoyaltyPoints.visibility = View.GONE
                    }
                }
            }
        } else {
            binding.txtLoyaltyPointsLabel.invisible()
            binding.txtCustomerName.invisible()
            binding.liinearInfoLayout.layoutParams.height =
                resources.getDimension(R.dimen._50sdp).toInt()
            binding.relativeLoylatyPoints.visibility = View.GONE
            binding.lblLoyaltyPoints.visibility = View.GONE
        }

    }

    private fun setupTaxAdapter() {
        taxBirfurcationAdapter = TaxBirfurcationAdapter("dashboard")
        binding.rvTax.adapter = taxBirfurcationAdapter
        taxBirfurcationAdapter.setList(arrayListOf())
    }

    fun onTaxClicked(shouldShow: Boolean) {
        if (this::binding.isInitialized && this::taxBirfurcationAdapter.isInitialized) {
            if (!shouldShow) {
                binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                binding.relativeDynamicTax.gone()
            } else {
                binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                binding.relativeDynamicTax.visible()
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

    fun showThankYou(paidAmount: Double) {
        binding.apply {
            mainCartLayout.gone()
            splashLayout.gone()

            thankYouLayout.visible()
            txtPaidAmount.text = "Paid ${MethodUtils.roundOffAmount(paidAmount)}"
        }
    }
}
