package com.pays.pos.ui.fragments.eGiftCard

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentPurchaseGiftCardBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseGiftCardFragment : Fragment() {

    private lateinit var giftCardType: String
    private lateinit var binding: FragmentPurchaseGiftCardBinding

    @Inject
    lateinit var prefProvider: PrefProvider
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "PurchaseGiftCardFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPurchaseGiftCardBinding.inflate(layoutInflater)

        setDigitalAsSelected()
        setDefaultAmountsInKeypad()
        setKeyPad()
        onClick()

        return binding.root
    }

    private fun setDigitalAsSelected() {
        binding.rdGroupSelectGiftCard.check(binding.rdBtnDigitalGiftCard.id)
        giftCardType = "Digital"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true))
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

            dashboardViewModel.clearGiftCardCart()

            findNavController().popBackStack()
        }

        binding.txtNext.setOnClickListener {

            val amount = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()

            if (amount > 0.0) {

                Log.d("TAG", "onClick: giftCardType = $giftCardType")
                prefProvider.setValue(Constants.GIFT_CARD_TYPE,giftCardType)

                Log.d("TAG", "onClick: giftCardPurchaseAmount = $amount")
                prefProvider.setValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, amount.toString())


                    findNavController().navigate(R.id.action_purchaseGiftCard_to_addCustomerToGiftCard)


            } else {

                AlertUtils.showCustomAlert(requireContext(), "Please enter amount")
                return@setOnClickListener

            }

        }

        binding.rdGroupSelectGiftCard.setOnCheckedChangeListener { _, i ->
            Log.e(TAG,"checkGroupSele")
            when (i) {

                binding.rdBtnDigitalGiftCard.id -> {
                    giftCardType = "Digital"
                }

                binding.rdBtnPlasticGiftCard.id -> {
                    giftCardType = "Physical"
                }
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

}