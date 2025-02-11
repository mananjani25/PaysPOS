package com.pays.pos.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.remote.Constants.AMOUNT
import com.pays.pos.data.remote.Constants.PERCENTAGE
import com.pays.pos.databinding.DailogAddDiscountBinding
import com.pays.pos.ui.adapter.DialogDiscountListAdapter
import com.pays.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.extensions.alert
import com.google.gson.Gson
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class AddDiscountDialog : DialogFragment(), DialogDiscountListAdapter.DiscountInterface,
    TextWatcher {

    private var isSet: Boolean = false
    private var itemOrderDiscount: Double = 0.0
    private var itemPrice: Double = 0.0
    private var orderDiscountType: String = ""
    private var orderDiscountPrice: Double = 0.0
    private var totalOrderPrice: Double = 0.0
    private var itemQuantity: Int = 0
    private var isOrderDiscount: Boolean = false
    private lateinit var binding: DailogAddDiscountBinding
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private val dashBoardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var discountAdapter: DialogDiscountListAdapter
    private var discountModel: TbDiscount? = null
    var selectedListPos: Int = -1
    private var isFromDetails = false
    private lateinit var defaultModel: TbCartItem
    private var selectedCurrency: String = AMOUNT
    var modifierPrice: Double = 0.0
    var orderDiscount: Double = 0.0
    var selectedvalue: Double = 0.0

    companion object {
        fun newInstance() = AddDiscountDialog()

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        discountAdapter = DialogDiscountListAdapter()
        binding.rvDiscountList.adapter = discountAdapter

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context, R.color.bg_color))
        val inset = InsetDrawable(back, 150, 80, 150, 80)
        dialog?.window?.setBackgroundDrawable(inset);

        isOrderDiscount = requireArguments().getBoolean("isOrderDiscount", false)
        if (isOrderDiscount) {
            totalOrderPrice = requireArguments().getDouble("totalPrice", 0.0)
            orderDiscountPrice = requireArguments().getDouble("orderDiscountPrice", 0.0)
            orderDiscountType = requireArguments().getString("orderDiscountType").toString()
           /*Added by Rahul for solving Discount issue */
            if (orderDiscountType.equals("null")) {
                orderDiscountType = AMOUNT
            }
        }
        selectedvalue = requireArguments().getDouble("selectedvalue")
        orderDiscount = requireArguments().getDouble("orderDiscount")
        isFromDetails = requireArguments().getBoolean("isFromDetails", false)
        val model: TbCartItem? = requireArguments().getParcelable("model")
        itemOrderDiscount = requireArguments().getDouble("itemOrderDiscount")
        itemQuantity = requireArguments().getInt("totalquantity")
        defaultModel = model ?: TbCartItem()

        LogUtil.logE(TAG, "dataModel ${Gson().toJson(model)}")

        if (model?.discountId != -1 && model?.discountType == PERCENTAGE) {
            LogUtil.logE(TAG, "DiscountPercentage")
            orderDiscountType = PERCENTAGE
            var disPercentage = 0.0
            disPercentage =
                MethodUtils.roundOffAmountDouble(100 * model.discountPrice / model.price)
            LogUtil.logE(TAG, "disPercentage  ${disPercentage}")


        }
        if (defaultModel.modifiers.isNotEmpty()) {
            for (i in defaultModel.modifiers.indices) {
                modifierPrice += ((defaultModel.modifiers[i].price) * (defaultModel.modifiers[i].itemQuantity))
            }
        }


        itemPrice = (defaultModel.price) + modifierPrice
        if (isOrderDiscount) {
            itemPrice -= orderDiscount
        } else {
            if (itemPrice >= itemOrderDiscount) {
                itemPrice -= itemOrderDiscount
            }
        }

        selectedCurrency = orderDiscountType
        if (selectedCurrency.isEmpty()) {
            selectedCurrency = AMOUNT
            amountView()
        }

        //Added by Rahul to resolve BIS - 3548
        if (selectedCurrency.equals(AMOUNT)){
            amountView()
        }else{
            percentageView()
        }

        if (model?.discountType == PERCENTAGE) {
            selectedCurrency = PERCENTAGE
        } else if (model?.discountType == AMOUNT) {
            selectedCurrency = AMOUNT
        }

        binding.edtAmount.addTextChangedListener(this)

        setDiscountList()
        setupData()
        setKeyPad()
        onClick()



        if (!isOrderDiscount) {
            if (defaultModel.discountId == 0) {
                binding.txtRemoveDiscount.visibility = View.GONE
                binding.edtAmount.append(MethodUtils.roundOffAmountString(defaultModel.discountPrice))

                if (defaultModel.discountPrice != 0.0) {
                    if (defaultModel.discountType == getString(R.string.disc_percentage)) {
                        val applyDiscount =
                            (defaultModel.discountPrice * 100) / (itemPrice/*(defaultModel.price + modifierPrice) * defaultModel.itemQuantity*/)
                        binding.edtAmount.setText(MethodUtils.roundOffAmountString(applyDiscount))
                        percentageView()
                    } else {
                        binding.edtAmount.setText(MethodUtils.roundOffAmountString(defaultModel.discountPrice))
                        amountView()
                    }
                    binding.txtRemoveDiscount.visibility = View.VISIBLE
                }
            } else {

                binding.txtRemoveDiscount.visibility = View.VISIBLE

                discountAdapter.setSelected(defaultModel.discountId)

                if (discountAdapter.selectedPosition != -1)
                    selectedListPos = discountAdapter.selectedPosition
                if (defaultModel.discountType == getString(R.string.disc_percentage)) {
                    if (discountAdapter.selectedPosition != -1) {
                        val applydis = discountAdapter.discountList[selectedListPos].percentage
                        binding.edtAmount.setText(
                            MethodUtils.roundOffAmountString(
                                applydis
                            )
                        )

                        LogUtil.logE(TAG, "edtAmountSetThird")
                    } else {
                        val applyDiscount =
                            (defaultModel.discountPrice * 100) / (itemPrice/*(defaultModel.price + modifierPrice) * defaultModel.itemQuantity*/)
                        binding.edtAmount.setText(
                            MethodUtils.roundOffAmountString(
                                (applyDiscount).toPrecision(2).toDouble()
                            )
                        )

                        LogUtil.logE(TAG, "edtAmountSetThird")
                    }

                    percentageView()
                } else {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(defaultModel.discountPrice))
                    amountView()
                }
            }
        } else {
            if (orderDiscountPrice != 0.0) {

                if (orderDiscountType == getString(R.string.disc_percentage)) {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(selectedvalue))
                    percentageView()
                } else {
                    isSet = true
                    binding.edtAmount.removeTextChangedListener(this)
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(orderDiscountPrice))
                    binding.edtAmount.addTextChangedListener(this)
                    amountView()
                }
            }


        }

        percentageView()
    }

    private fun onClick() {
        binding.txtCurrencyPercentage.setOnClickListener {

            percentageView()
            binding.edtAmount.setText("0.00")
            if (selectedListPos != -1) {
                discountAdapter.clearSelectedItem()
                selectedListPos = -1
            }


        }
        binding.txtCurrencyDollar.setOnClickListener {
            binding.edtAmount.setText("0.00")
            amountView()
            if (selectedListPos != -1) {
                discountAdapter.clearSelectedItem()
                selectedListPos = -1
            }


        }

        binding.txtRemoveDiscount.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (binding.edtAmount.text.toString().trim()
                        .isNotEmpty() && binding.edtAmount.text.toString().trim().isNotBlank()
                ) {
                    removeDiscount()
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        getString(R.string.discount_is_not_applied), null
                    )
                }
            }

        })

        binding.llKeypad.txt10.setOnClickListener {

            val discount = TbDiscount(
                "",
                selectedCurrency,
                -1,
                -1,
                "",
                10.0,
                ""
            )

            calculationDiscount(discount, -1)
        }
        binding.llKeypad.txt20.setOnClickListener {

            val discount = TbDiscount(
                "",
                selectedCurrency,
                -1,
                -1,
                "",
                20.0,
                ""
            )

            calculationDiscount(discount, -1)
        }
        binding.llKeypad.txt30.setOnClickListener {
            val discount = TbDiscount(
                "",
                selectedCurrency,
                -1,
                -1,
                "",
                30.0,
                ""
            )

            calculationDiscount(discount, -1)
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun amountView() {
        binding.llKeypad.txt10.text = "$10"
        binding.llKeypad.txt20.text = "$20"
        binding.llKeypad.txt30.text = "$30"
        binding.txtCurrency.visibility = View.VISIBLE
        binding.txtPer.visibility = View.GONE
        selectedCurrency = AMOUNT
        binding.txtCurrencyDollar.background =
            requireContext().resources.getDrawable(R.drawable.button_selected)
        binding.txtCurrencyPercentage.background =
            requireContext().resources.getDrawable(R.drawable.background_square_border_grey)
        binding.txtCurrencyDollar.setTextColor(requireActivity().resources.getColor(R.color.white))
        binding.txtCurrencyPercentage.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
        binding.edtAmount.setText(binding.edtAmount.text.toString().trim())


    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun percentageView() {
        binding.llKeypad.txt10.text = "10%"
        binding.llKeypad.txt20.text = "20%"
        binding.llKeypad.txt30.text = "30%"
        selectedCurrency = PERCENTAGE
        binding.txtCurrency.visibility = View.GONE
        binding.txtPer.visibility = View.VISIBLE
        binding.txtCurrencyDollar.background =
            requireContext().resources.getDrawable(R.drawable.background_square_border_grey)
        binding.txtCurrencyPercentage.background =
            requireContext().resources.getDrawable(R.drawable.button_selected)
        binding.txtCurrencyDollar.setTextColor(requireActivity().resources.getColor(R.color.txtColor))
        binding.txtCurrencyPercentage.setTextColor(requireActivity().resources.getColor(R.color.white))
        binding.edtAmount.setText(binding.edtAmount.text.toString().trim())


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.rvDiscountList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        return binding.root
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

    }

    private fun setDiscountList() {
        viewModel.discountList.observe(requireActivity()) {
            LogUtil.logE(TAG, "DiscountList ${Gson().toJson(it)}")
            if (it.data?.isNotEmpty() == true) {
                it.data.forEach {
                    it.isChecked = false
                }
                discountAdapter.setList(it.data)
                discountAdapter.setListner(this)
                discountAdapter.setSelected(defaultModel.discountId)
            }


        }


    }

    private fun setupData() {

        binding.llKeypad.txtClear.setOnClickListener {
            binding.edtAmount.removeTextChangedListener(this)
            binding.edtAmount.setText("0.00")
            binding.edtAmount.addTextChangedListener(this)

            discountAdapter.clearSelectedItem()
            selectedListPos = -1
        }
        binding.imgBack.setOnClickListener {
            if (discountModel == null) {
                discountModel = TbDiscount("", "", -1, 0, "", 0.0, "")
                dashBoardViewModel.cartModel?.discountSelectdValue = 0.0
            }
            findNavController().navigateUp()

        }

        binding.txtSave.setOnClickListener {
            //added to resolve discount issue
            if(!dashBoardViewModel.discountNeedToUpdate)
                dashBoardViewModel.cartFooterNeedToBeUpdated = false

            if (binding.edtAmount.text.toString().trim()
                    .isNotEmpty() && binding.edtAmount.text.toString().trim().isNotBlank()
            ) {
                addDiscount()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    getString(R.string.enter_a_discount),
                    null
                )
            }
        }
    }

    private fun addDiscount() {
        defaultModel.itemQuantity = itemQuantity
        if (selectedListPos != -1) {

            val model = discountAdapter.getItem(selectedListPos)


            var a = binding.edtAmount.text.toString().toDouble()

            if (isOrderDiscount && selectedCurrency == PERCENTAGE) {

                a = (totalOrderPrice + orderDiscountPrice) * a / 100
            }

            val discount = TbDiscount(
                "",
                model.discountType,
                model.id,
                0,
                model.name,
                a,
                ""
            )

            PrefProvider(requireContext()).setValue(Constants.discountType,
                discountModel?.discountType ?: defaultModel.discountType
            )

            val result = Bundle().apply {
                putParcelable("data", discount)
                putParcelable("item", defaultModel)
                putDouble("value", binding.edtAmount.text.toString().toDouble())
            }


            when {
                isFromDetails -> {
                    setFragmentResult("request_key_discount_details", result)
                }
                isOrderDiscount -> {
                    setFragmentResult("request_key_discount_order", result)
                }
                else -> {

                    setFragmentResult("request_key_discount", result)
                }
            }
            findNavController().navigateUp()

        } else if (binding.edtAmount.text?.isNotEmpty() == true && binding.edtAmount.text.toString() != "0.00") {


            discountModel =
                if (selectedCurrency == PERCENTAGE) {


                    var a = binding.edtAmount.text.toString().toDouble()

                    if (isOrderDiscount) {

                        a = (totalOrderPrice + orderDiscountPrice) * a / 100
                    }

                        TbDiscount(
                            "",
                            getString(R.string.disc_percentage),
                            -1,
                            0,
                            "",
                            a,
                            ""
                        )
                    } else {
                        TbDiscount(
                            "",
                            "Amount",
                            -1,
                            0,
                            "",
                            binding.edtAmount.text.toString().toDouble(),
                            ""
                        )
                    }


//                if(defaultModel.discountPrice > dashBoardViewModel.currentTotalPrice)
//                    defaultModel.discountPrice = dashBoardViewModel.currentTotalPrice

                val result = Bundle().apply {
                    putParcelable("data", discountModel)
                    putParcelable("item", defaultModel)
                    putDouble("value", binding.edtAmount.text.toString().toDouble())
                }

            PrefProvider(requireContext()).setValue(Constants.discountType,
                discountModel?.discountType ?: defaultModel.discountType
            )

            when {
                    isFromDetails -> {
                        LogUtil.logE(TAG, "PassingModel:  ${Gson().toJson(discountModel)}")
                        setFragmentResult("request_key_discount_details", result)
                    }

                    isOrderDiscount -> {
                        setFragmentResult("request_key_discount_order", result)
                    }

                    else -> {

                    setFragmentResult("request_key_discount", result)
                }
            }
            findNavController().navigateUp()
        } else {
            removeDiscount()

        }

    }

    private fun removeDiscount() {
        val discount = TbDiscount("", "", -1, 0, "", 0.0, "")
        val result = Bundle().apply {
            putParcelable("data", discount)
            putParcelable("item", defaultModel)
        }

        PrefProvider(requireContext()).setValue(Constants.discountType, "")


        when {
            isFromDetails -> {

                setFragmentResult("request_key_discount_details", result)
            }
            isOrderDiscount -> {
                setFragmentResult("request_key_discount_order", result)
            }
            else -> {

                setFragmentResult("request_key_discount", result)
            }
        }
        findNavController().navigateUp()
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    override fun selectedItem(model: TbDiscount, pos: Int) {

        calculationDiscount(model, pos)

    }

    private fun calculationDiscount(model: TbDiscount, pos: Int) {
        if (isOrderDiscount) {

            if (model.discountType == requireContext().getString(R.string.disc_percentage)) {

                val percentage = (totalOrderPrice * model.percentage) / 100

                if (percentage <= totalOrderPrice) {
                    setData(model, pos)
                } else {
                    displayError("Discount amount should be less then item amount.")

                }

            } else {
                if (model.percentage <= totalOrderPrice) {

                    setData(model, pos)

                } else {
                    displayError("Discount amount should be less then total amount.")

                }
            }


        } else {

            if (model.discountType == requireContext().getString(R.string.disc_percentage)) {

                val percentage = (itemPrice * model.percentage) / 100

                if (percentage <= itemPrice) {
                    setData(model, pos)
                } else {
                    displayError("Discount amount should less then item amount.")

                }
            } else {
                if (model.percentage <= itemPrice) {

                    setData(model, pos)

                } else {
                    displayError("Discount amount should less then item amount.")
                }
            }


        }
    }

    private fun displayError(message: String) {

        AlertUtils.showCustomAlert(
            requireContext(),
            message
        )
        discountAdapter.clearSelectedItem()
    }

    private fun setData(model: TbDiscount, pos: Int) {
        discountModel.apply { model }

        val discount: Double = model.percentage

        if (model.discountType == "Percentage") {
            percentageView()
        } else {
            amountView()
        }

        binding.edtAmount.removeTextChangedListener(this)
        binding.edtAmount.setText(MethodUtils.roundOffAmountString(discount))
        binding.edtAmount.addTextChangedListener(this)
        selectedListPos = pos
    }

    private fun calculateValue(number: String, delete: Boolean) {
        discountAdapter.clearSelectedItem()
        selectedListPos = -1
        if (binding.edtAmount.text?.length!! >= 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))

        } else {
            if (number.isNotEmpty()) {
                binding.edtAmount.append(number)
            } else {
                binding.edtAmount.addTextChangedListener(this)
            }
        }
    }

    private fun removeLastCharacter(str: String): String {
        if (str.length == 1) {
            binding.edtAmount.addTextChangedListener(this)
        }
        return str.substring(0, str.length - 1)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    var current = ""
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        LogUtil.logE(TAG, "isSet  ${isSet}")

        if (!isSet) {
            if (s.toString().isNotEmpty()) {
                binding.edtAmount.removeTextChangedListener(this)


                val cleanString: String = s!!.replace("""[$,.%]""".toRegex(), "")


                val parsed = cleanString.toDouble()

                val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


                current = formatted

                LogUtil.logE(TAG, "formatted  ${formatted}")
                binding.edtAmount.setText(formatted.replace("""[$,%]""".toRegex(), ""))

                var selection = formatted.replace("""[$,%]""".toRegex(), "").length

                try {
                    binding.edtAmount.setSelection(selection)
                } catch (e: Exception) {
                    Log.d("onTextChanged", "onTextChanged exception = $selection")
                    if (selection <= 0) {
                        selection = 0
                        binding.edtAmount.setSelection(selection)
                    } else {
                        binding.edtAmount.setSelection(selection - 1)
                    }
                }


                /*Added by Rahul for solving Discount issue */
                if (selectedCurrency.equals("null") || selectedCurrency == AMOUNT) {
                    selectedCurrency= AMOUNT
                    var price = 0.0


                    if (isOrderDiscount) {
                        if (totalOrderPrice == 0.0) {

                            price = totalOrderPrice + orderDiscountPrice
                        } else
                            price = totalOrderPrice
                    } else {
                        price = itemPrice /*- defaultModel.discountPrice*/
                    }

                    LogUtil.logE(TAG, "pricediscount  ${price}")
                    LogUtil.logE(TAG, "discountPriceDefault  ${defaultModel.discountPrice}")
                    LogUtil.logE(TAG, "TextAmount ${binding.edtAmount.text.toString().toDouble()}")
                    if (defaultModel.discountPrice == 0.0) {

                        if (binding.edtAmount.text.toString().toDouble() > price) {
                            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
                        }

                    } else {

                        if (binding.edtAmount.text.toString()
                                .toDouble() > price
                        ) {
                            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
                        }
                    }
                }

                if (selectedCurrency == PERCENTAGE) {
                    if (binding.edtAmount.text.toString().toDouble() > 100) {
                        binding.edtAmount.setText("100.00")
                    }
                }

                binding.edtAmount.addTextChangedListener(this)
            }
        } else {
            isSet = false
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }

}