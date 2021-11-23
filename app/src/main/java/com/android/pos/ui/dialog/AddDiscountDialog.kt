package com.android.pos.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbDiscount
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants.AMOUNT
import com.android.pos.data.remote.Constants.PERCENTAGE
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.ui.adapter.DialogDiscountListAdapter
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class AddDiscountDialog : DialogFragment(), DialogDiscountListAdapter.DiscountInterface,
    TextWatcher {

    private var orderDiscountType: String = ""
    private var orderDiscountPrice: Double = 0.0
    private var totalOrderPrice: Double = 0.0
    private var isOrderDiscount: Boolean = false
    private lateinit var binding: DailogAddDiscountBinding
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var discountAdapter: DialogDiscountListAdapter
    private var discountModel: TbDiscount? = null
    var selectedListPos: Int = -1
    private var isFromDetails = false
    private lateinit var defaultModel: TbItem
    private var selectedCurrency: String = AMOUNT


    companion object {
        fun newInstance() = AddDiscountDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        discountAdapter = DialogDiscountListAdapter()
        binding.rvDiscountList.adapter = discountAdapter



        isOrderDiscount = requireArguments().getBoolean("isOrderDiscount", false)
        if (isOrderDiscount) {
            totalOrderPrice = requireArguments().getDouble("totalPrice", 0.0)
            orderDiscountPrice = requireArguments().getDouble("orderDiscountPrice", 0.0)
            orderDiscountType = requireArguments().getString("orderDiscountType").toString()

        }

        isFromDetails = requireArguments().getBoolean("isFromDetails", false)
        val model: TbItem? = requireArguments().getParcelable("model")
        defaultModel = model ?: TbItem()


        selectedCurrency = defaultModel.discountType
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
                            (defaultModel.discountPrice * 100) / (defaultModel.price * defaultModel.itemQuantity)
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
                    val applyDiscount =
                        (defaultModel.discountPrice * 100) / (defaultModel.price * defaultModel.itemQuantity)
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(applyDiscount))
                    percentageView()
                } else {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(defaultModel.discountPrice))
                    amountView()
                }
            }
        } else {
            if (orderDiscountPrice != 0.0) {

                if (orderDiscountType == getString(R.string.disc_percentage)) {
                    val applyDiscount = (orderDiscountPrice * 100) / (totalOrderPrice)
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(applyDiscount))
                    percentageView()
                } else {
                    binding.edtAmount.setText(MethodUtils.roundOffAmountString(orderDiscountPrice))
                    amountView()
                }
            }


        }

    }

    private fun onClick() {
        binding.txtCurrencyPercentage.setOnClickListener {

            percentageView()
            if (selectedListPos != -1) {
                discountAdapter.clearSelectedItem()
                selectedListPos = -1
            }
        }
        binding.txtCurrencyDollar.setOnClickListener {

            amountView()
            if (selectedListPos != -1) {
                discountAdapter.clearSelectedItem()
                selectedListPos = -1
            }
        }

        binding.txtRemoveDiscount.setOnClickListener {

            removeDiscount()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun amountView() {
        binding.txtCurrency.visibility = View.VISIBLE
        binding.txtPer.visibility = View.GONE
        selectedCurrency = AMOUNT
        binding.txtCurrencyDollar.background =
            requireContext().resources.getDrawable(R.drawable.background_discount_selected)
        binding.txtCurrencyPercentage.background =
            requireContext().resources.getDrawable(R.drawable.background_discount_unselected)


    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun percentageView() {
        selectedCurrency = PERCENTAGE
        binding.txtCurrency.visibility = View.GONE
        binding.txtPer.visibility = View.VISIBLE
        binding.txtCurrencyDollar.background =
            requireContext().resources.getDrawable(R.drawable.background_discount_unselected)
        binding.txtCurrencyPercentage.background =
            requireContext().resources.getDrawable(R.drawable.background_discount_selected)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
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
        viewModel.getDiscountList.observe(requireActivity(), {
            Log.e(TAG, "DiscountList ${Gson().toJson(it)}")
            if (it.data?.isNotEmpty() == true) {
                it.data.forEach {
                    it.isChecked = false
                }
                discountAdapter.setList(it.data)
                discountAdapter.setListner(this)
                discountAdapter.setSelected(defaultModel?.discountId)
            }


        })


    }

    private fun setupData() {

        binding.txtClear.setOnClickListener {

            binding.edtAmount.setText("0.00")

            discountAdapter.clearSelectedItem()
            selectedListPos = -1
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()

        }

        binding.txtSave.setOnClickListener {

            if (selectedListPos != -1) {

                val model = discountAdapter.getItem(selectedListPos)


                var a = binding.edtAmount.text.toString().toDouble()

                if (isOrderDiscount) {

                    a = totalOrderPrice * a / 100
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


                val result = Bundle().apply {
                    putParcelable("data", discount)
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

            } else if (binding.edtAmount.text?.isNotEmpty() == true && binding.edtAmount.text.toString() != "0.00"
            ) {


                discountModel =
                    if (selectedCurrency == PERCENTAGE) {


                        var a = binding.edtAmount.text.toString().toDouble()

                        if (isOrderDiscount) {

                            a = totalOrderPrice * a / 100
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
                            "",
                            -1,
                            0,
                            "",
                            binding.edtAmount.text.toString().toDouble(),
                            ""
                        )
                    }
                val result = Bundle().apply {
                    putParcelable("data", discountModel)
                }
                when {
                    isFromDetails -> {
                        Log.e(TAG, "PassingModel:  ${Gson().toJson(discountModel)}")
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
    }

    private fun removeDiscount() {
        val discount = TbDiscount("", "", -1, 0, "", 0.0, "")
        val result = Bundle().apply {
            putParcelable("data", discount)
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
        Log.e(TAG, "SelectedItem:  ${Gson().toJson(model)}")
        discountModel.apply { model }

        val discount: Double = model.percentage

        if (model.discountType == "Percentage") {
            percentageView()
        } else {
            amountView()
        }


        binding.edtAmount.setText(MethodUtils.roundOffAmountString(discount))
        selectedListPos = pos
    }

    fun calculateValue(number: String, delete: Boolean) {
        discountAdapter.clearSelectedItem()
        selectedListPos = -1
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))

        } else {
            binding.edtAmount.append(number)


        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    var current = ""
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        if (s.toString() != current) {
            binding.edtAmount.removeTextChangedListener(this)


            val cleanString: String = s!!.replace("""[$,.%]""".toRegex(), "")


            val parsed = cleanString.toDouble()

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted

            binding.edtAmount.setText(formatted.replace("""[$,%]""".toRegex(), ""))
            binding.edtAmount.setSelection(formatted.replace("""[$,%]""".toRegex(), "").length)


            if (selectedCurrency == AMOUNT) {

                var price =
                    ((defaultModel.price - defaultModel.discountPrice) * defaultModel.itemQuantity)

                if (isOrderDiscount) {
                    price = totalOrderPrice
                }

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
    }

    override fun afterTextChanged(s: Editable?) {

    }

}