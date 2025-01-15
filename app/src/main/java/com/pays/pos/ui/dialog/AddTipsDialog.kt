package com.pays.pos.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DailogAddTipsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.DialogTipsListAdapter
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AddTipsDialog : DialogFragment(), DialogTipsListAdapter.DiscountInterface {

    private var rate: Double = 0.00
    private var tipID: Int? = null
    private var totalTip: Double = 0.00
    private var totalPrice: Double = 0.0
    private lateinit var binding: DailogAddTipsBinding
    private val viewModel by activityViewModels<TipListViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var tipsListAdapter: DialogTipsListAdapter
    private var tipModel: GetTipReponse.Data? = null
    var selectedListPos: Int = -1
    var splitCount = 1

    @Inject
    lateinit var prefProvider: PrefProvider
    private var isFromDetails = false
    private var isFromTransaction = false

    companion object {
        fun newInstance() = AddTipsDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            if (arguments?.getDouble("totalPrice") != null) totalPrice =
                requireArguments().getDouble("totalPrice")
            if (arguments?.getDouble("totalTip") != null) totalTip =
                requireArguments().getDouble("totalTip")

            if (arguments?.getInt("splitCount") != null) {
                splitCount =
                    requireArguments().getInt("splitCount")
            }

            if (arguments?.getBoolean("isFromTransaction") != null) {
                isFromTransaction = requireArguments().getBoolean("isFromTransaction", false)
            }
        }

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 80, 150, 80)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.txtTitle.text = getString(R.string.add_tips)

        tipsListAdapter = DialogTipsListAdapter()
        binding.rvDiscountList.adapter = tipsListAdapter


        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true))
        binding.edtAmount.setText("" + MethodUtils.roundOffAmountString(totalTip))

        setDiscountList()
        setupData()
        setKeyPad()
        onClick()
    }

    private fun onClick() {


        binding.llKeypad.txt10.setOnClickListener {
            rate = binding.llKeypad.txt10.text.toString().trim()
                .substring(0, binding.llKeypad.txt10.text.toString().length - 1).toDouble()

            resetDialogTipsList()

            var price = 0.0
            price = if (isFromTransaction) {
                MethodUtils.percentageCalculation(
                    totalPrice, rate
                )
            } else {
                MethodUtils.percentageCalculation(
                    prefProvider.getValue(
                        Constants.WHOLE_AMOUNT,
                        "0.0"
                    ).toDouble()  / splitCount, rate
                )
            }

            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
        binding.llKeypad.txt20.setOnClickListener {
            rate = binding.llKeypad.txt20.text.toString().trim()
                .substring(0, binding.llKeypad.txt20.text.toString().length - 1).toDouble()
            var price = 0.0
            price = if (isFromTransaction) {
                MethodUtils.percentageCalculation(
                    totalPrice, rate
                )
            } else {
                MethodUtils.percentageCalculation(
                    prefProvider.getValue(
                        Constants.WHOLE_AMOUNT,
                        "0.0"
                    ).toDouble() / splitCount, rate
                )
            }
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))

        }
        binding.llKeypad.txt30.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                rate = binding.llKeypad.txt30.text.toString().trim()
                    .substring(0, binding.llKeypad.txt30.text.toString().length - 1).toDouble()

                var price = 0.0
                price = if (isFromTransaction) {
                    MethodUtils.percentageCalculation(
                        totalPrice, rate
                    )
                } else {
                    MethodUtils.percentageCalculation(
                        prefProvider.getValue(
                            Constants.WHOLE_AMOUNT,
                            "0.0"
                        ).toDouble() / splitCount, rate
                    )
                }
                binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))

            }
        })

//        binding.llKeypad.txt30.setOnClickListener {
//
//
//
//        }
    }

    private fun resetDialogTipsList() {
        tipsListAdapter.discountList.forEach {
            it.isCheckedInAdapter=false
        }
        tipsListAdapter.notifyDataSetChanged()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

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
            dashboardViewModel.customerGivenTip.value=false
            calculateValue("", true)
        }
        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

    }

    private fun setDiscountList() {
        viewModel.getTipActiveList.observe(requireActivity()) {
            LogUtil.logE(TAG, "DiscountList ${Gson().toJson(it)}")
            if (it.data?.isNotEmpty() == true) {

                it.data.forEach {
                    it.isChecked = false
                }
                tipsListAdapter.setList(it.data)
                tipsListAdapter.setListner(this)
            }


        }


    }

    private fun setupData() {

        binding.llKeypad.txtClear.setOnClickListener {
            dashboardViewModel.customerGivenTip.value=false
            binding.edtAmount.setText("0.00")

            tipsListAdapter.clearSelectedItem()
            selectedListPos = -1
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {

            var amount = 0.0

            amount = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
            if (amount >0.0){
                val result = Bundle().apply {
                    putDouble("tipAmount", amount)
                    putDouble("tipPercent",rate)
                    tipID?.let { putInt("tipId", tipID ?: 0) }

                }
                if (isFromTransaction) {
                    setFragmentResult("request_key_tips", result)
                } else {
                    requireActivity().supportFragmentManager.setFragmentResult(
                        "request_key_tips",
                        result
                    )
                }

                findNavController().navigateUp()
            }else{
                AlertUtils.showCustomAlert(requireContext(), "Please enter tip amount")
                return@setOnClickListener
            }

        }

        binding.txtRemove?.setOnClickListener {


            val result = Bundle().apply {
                putDouble("tipAmount", 0.0)
                putDouble("tipPercent",0.0)
                tipID?.let { putInt("tipId", tipID ?: 0) }

            }
            if (isFromTransaction) {
                setFragmentResult("request_key_tips", result)
            } else {
                requireActivity().supportFragmentManager.setFragmentResult(
                    "request_key_tips",
                    result
                )
            }

            findNavController().navigateUp()
        }
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

    override fun selectedItem(model: GetTipReponse.Data, pos: Int) {
        LogUtil.logE(TAG, "SelectedItem:  ${Gson().toJson(model)}")
        if (model.isCheckedInAdapter){
            rate = model.rate
            tipModel.apply { model }
            tipID = model.id
            var tipCalculation = 0.0
            if (isFromTransaction) {
                tipCalculation = (totalPrice * model.rate) / 100
            } else {
                totalPrice =
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / splitCount
                tipCalculation = (totalPrice * model.rate) / 100
            }
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(tipCalculation))
            selectedListPos = pos
        }else{
            binding.edtAmount.setText("0.00")
        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        tipsListAdapter.clearSelectedItem()
        tipID = null
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


}