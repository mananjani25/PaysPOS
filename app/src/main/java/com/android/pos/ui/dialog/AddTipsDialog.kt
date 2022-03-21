package com.android.pos.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.DailogAddTipsBinding
import com.android.pos.ui.adapter.DialogTipsListAdapter
import com.android.pos.ui.fragments.settings.tip.TipListViewModel
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class AddTipsDialog : DialogFragment(), DialogTipsListAdapter.DiscountInterface,
    TextWatcher {

    private var tipID: Int? = null
    private var totalTip: Double = 0.0
    private var totalPrice: Double = 0.0
    private lateinit var binding: DailogAddTipsBinding
    private val viewModel by activityViewModels<TipListViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var tipsListAdapter: DialogTipsListAdapter
    private var tipModel: GetTipReponse.Data? = null
    var selectedListPos: Int = -1
    private var isFromDetails = false


    companion object {
        fun newInstance() = AddTipsDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments!=null){
            if (arguments?.getDouble("totalPrice")!=null) totalPrice = requireArguments().getDouble("totalPrice")
            if (arguments?.getDouble("totalTip")!=null) totalTip = requireArguments().getDouble("totalTip")
        }

        binding.txtTitle.text = getString(R.string.add_tips)

        tipsListAdapter = DialogTipsListAdapter()
        binding.rvDiscountList.adapter = tipsListAdapter

        binding.edtAmount.setText(MethodUtils.roundOffAmountString(totalTip))

        binding.edtAmount.addTextChangedListener(this)

        setDiscountList()
        setupData()
        setKeyPad()
        onClick()
    }

    private fun onClick() {
        binding.txtRemoveDiscount.setOnClickListener {

            // removeDiscount()
        }

        binding.llKeypad.txt10.setOnClickListener {
            val rate = binding.llKeypad.txt10.text.toString().trim()
                .substring(0, binding.llKeypad.txt10.text.toString().length - 1).toDouble()

            val price = MethodUtils.percentageCalculation(totalPrice, rate)
            binding.edtAmount.removeTextChangedListener(this)
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
        binding.llKeypad.txt20.setOnClickListener {
            val rate = binding.llKeypad.txt20.text.toString().trim()
                .substring(0, binding.llKeypad.txt20.text.toString().length - 1).toDouble()
            val price = MethodUtils.percentageCalculation(totalPrice, rate)
            binding.edtAmount.removeTextChangedListener(this)
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
        binding.llKeypad.txt30.setOnClickListener {


            val rate = binding.llKeypad.txt30.text.toString().trim()
                .substring(0, binding.llKeypad.txt30.text.toString().length - 1).toDouble()

            val price = MethodUtils.percentageCalculation(totalPrice, rate)

            binding.edtAmount.removeTextChangedListener(this)
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
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
            calculateValue("", true)
        }
        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

    }

    private fun setDiscountList() {
        viewModel.getTipList.observe(requireActivity()) {
            Log.e(TAG, "DiscountList ${Gson().toJson(it)}")
            if (it.data?.isNotEmpty() == true) {
                it.data.forEach {
                    it.isChecked = false
                }
                tipsListAdapter.setList(it.data)
                tipsListAdapter.setListner(this)
                //tipsListAdapter.setSelected(defaultModel?.discountId)
            }


        }


    }

    private fun setupData() {

        binding.llKeypad.txtClear.setOnClickListener {

            binding.edtAmount.setText("0.00")

            tipsListAdapter.clearSelectedItem()
            selectedListPos = -1
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {

            var amount = 0.0

            val stAmount = binding.edtAmount.text.toString().trim()

            if (stAmount.isNotEmpty() && stAmount != "0.00") {
                amount = binding.edtAmount.text.toString().trim().toDouble()
            }

            val result = Bundle().apply {
                putDouble("tipAmount", amount)
                tipID?.let { putInt("tipId", tipID ?: 0) }

            }
            setFragmentResult("request_key_tips", result)
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
        Log.e(TAG, "SelectedItem:  ${Gson().toJson(model)}")
        tipModel.apply { model }
        tipID = model.id

        val tipCalculation = (totalPrice * model.rate) / 100

        binding.edtAmount.setText(MethodUtils.roundOffAmountString(tipCalculation))
        selectedListPos = pos
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



            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }


}