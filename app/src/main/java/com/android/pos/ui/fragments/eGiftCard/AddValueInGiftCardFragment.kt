package com.android.pos.ui.fragments.eGiftCard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentAddValueInGiftCardBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddValueInGiftCardFragment : Fragment() {

    private lateinit var binding: FragmentAddValueInGiftCardBinding

    @Inject
    lateinit var prefProvider: PrefProvider
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddValueInGiftCardBinding.inflate(layoutInflater)
        setDefaultAmountsInKeypad()
        setKeyPad()
        onClick()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true,isFromGiftCard = true))


    }

    private fun setDefaultAmountsInKeypad() {
        binding.apply {
            llKeypad.apply {
                txt10.text = "$25"
                txt20.text = "$30"
                txt30.text = "$35"
            }
        }
    }

    private fun setKeyPad() {
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

        binding.llKeypad.tvClear.setOnClickListener {
            calculateValue("", true)
        }

        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

        binding.llKeypad.txtClear.setOnClickListener {
            binding.edtAmount.setText("0.00")
        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))
        } else {
            binding.edtAmount.append(number)
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun onClick() {

        binding.imgBack.setOnClickListener {
            clearCartOnBackPress()
            findNavController().popBackStack()
        }

        binding.txtNext.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            val amount = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
            val giftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ","")

            if(giftCardNumber.length != 8){
                AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number.")
                return@setOnClickListener
            } else if (amount <= 0.0) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter amount")
                return@setOnClickListener
            } else {
                prefProvider.setValue(Constants.GIFT_CARD_TYPE,"Digital")
                prefProvider.setValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, amount.toString())
                prefProvider.setValue(Constants.GIFT_CARD_NUMBER, giftCardNumber)
                prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, true)

                moveToCheckout()
            }
        }

        binding.llKeypad.txt10.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(25.0))
        }

        binding.llKeypad.txt20.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(30.0))
        }

        binding.llKeypad.txt30.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(35.0))
        }
    }

    //delete cart and clear local data on back press
    private fun clearCartOnBackPress() {
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValue(Constants.ORDER_TYPE, "")
        prefProvider.setValue(Constants.ORDER_TYPE_NAME, "")
        prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)

        dashboardViewModel.deleteCart()
    }

    // Move to checkout screen with added gift card details to make payment
    private fun moveToCheckout() {

        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")

        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)

        prefProvider.saveCustomerData(null)

        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")

        dashboardViewModel.deleteCart()

        prefProvider.setValue(Constants.ORDER_TYPE, Constants.GIFT_CARD)
        prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.GIFT_CARD_NAME)

        val cm = CartModel()
        val tbItem = TbCartItem()
        tbItem.name = "Digital Gift Card"
        tbItem.quantity = 1
        tbItem.itemQuantity = 1
        val totalPrice = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
        tbItem.price = totalPrice
        tbItem.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
        tbItem.orderTypeId = 0
        tbItem.orderType = Constants.GIFT_CARD
        tbItem.orderTypeName = Constants.GIFT_CARD

        cm.apply {
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            isOpenOrder = false
            orderTypeId = 0
            orderType = Constants.GIFT_CARD
            orderTypeName = Constants.GIFT_CARD
        }

        dashboardViewModel.addCart(cm)
        dashboardViewModel.addItemToCartItems(tbItem)

        val bundle = Bundle()
        bundle.putBoolean("update", true)
        bundle.putDouble("totalPrice", totalPrice)
        bundle.putDouble("finalprice", totalPrice)
        bundle.putDouble(
            "cashDiscountSurcharge",
            MethodUtils.calculateCashDiscount(
                totalPrice,
                prefProvider,
                requireContext()
            )
        )
        bundle.putDouble("subTotalPrice", totalPrice)
        bundle.putDouble("totalTax", 0.0)
        bundle.putDouble("totalDiscount", 0.0)
        bundle.putDouble("totalServiceCharge", 0.0)
        bundle.putParcelable("cartList", cm)
        bundle.putBoolean("isFromPayment", true)

        findNavController().navigate(
            R.id.action_addValueInGiftCard_to_paymentBoldPosFragment,
            bundle
        )
    }

}