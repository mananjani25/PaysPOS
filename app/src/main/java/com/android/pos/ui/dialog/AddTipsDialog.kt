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
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.Constants.AMOUNT
import com.android.pos.data.remote.Constants.PERCENTAGE
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.ui.adapter.DialogDiscountListAdapter
import com.android.pos.ui.adapter.DialogTipsListAdapter
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class AddTipsDialog : DialogFragment(), DialogTipsListAdapter.DiscountInterface,
    TextWatcher {

    private lateinit var binding: DailogAddDiscountBinding
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var tipsListAdapter: DialogTipsListAdapter
    private var discountModel: TbDiscount? = null
    var selectedListPos: Int = -1
    private var isFromDetails = false
    private lateinit var defaultModel: NoteResponse.Data
    private var selectedCurrency: String = AMOUNT


    companion object {
        fun newInstance() = AddTipsDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtTitle.text = getString(R.string.add_tips)

        tipsListAdapter = DialogTipsListAdapter()
        binding.rvDiscountList.adapter = tipsListAdapter


        binding.edtAmount.addTextChangedListener(this)

        setDiscountList()
        setupData()
        setKeyPad()
        onClick()


    }

    private fun onClick() {
        binding.txtCurrencyPercentage.setOnClickListener {

            percentageView()
            if (selectedListPos != -1) {
                tipsListAdapter.clearSelectedItem()
                selectedListPos = -1
            }
        }
        binding.txtCurrencyDollar.setOnClickListener {

            amountView()
            if (selectedListPos != -1) {
                tipsListAdapter.clearSelectedItem()
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
                tipsListAdapter.setList(it.data)
                tipsListAdapter.setListner(this)
                //tipsListAdapter.setSelected(defaultModel?.discountId)
            }


        })


    }

    private fun setupData() {

        binding.txtClear.setOnClickListener {

            binding.edtAmount.setText("0.00")

            tipsListAdapter.clearSelectedItem()
            selectedListPos = -1
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()

        }

        binding.txtSave.setOnClickListener {

            if (selectedListPos != -1) {
                val result = Bundle().apply {
                    putParcelable("data", tipsListAdapter.getItem(selectedListPos))
                }


                if (isFromDetails) {
                    setFragmentResult("request_key_discount_details", result)
                } else {

                    setFragmentResult("request_key_discount", result)
                }
                findNavController().navigateUp()

            } else if (binding.edtAmount.text?.isNotEmpty() == true && binding.edtAmount.text.toString() != "0.00"
            ) {


                discountModel =
                    if (selectedCurrency == PERCENTAGE) {
                        TbDiscount(
                            "",
                            getString(R.string.disc_percentage),
                            0,
                            0,
                            "",
                            binding.edtAmount.text.toString().toDouble(),
                            ""
                        )
                    } else {
                        TbDiscount(
                            "",
                            "",
                            0,
                            0,
                            "",
                            binding.edtAmount.text.toString().toDouble(),
                            ""
                        )
                    }
                val result = Bundle().apply {
                    putParcelable("data", discountModel)
                }
                if (isFromDetails) {
                    Log.e(TAG, "PassingModel:  ${Gson().toJson(discountModel)}")
                    setFragmentResult("request_key_discount_details", result)
                } else {

                    setFragmentResult("request_key_discount", result)
                }
                findNavController().navigateUp()
            } else {
                removeDiscount()

            }


        }
    }

    private fun removeDiscount() {
        val discount = TbDiscount("", "", 0, 0, "", 0.0, "")
        val result = Bundle().apply {
            putParcelable("data", discount)
        }


        if (isFromDetails) {

            setFragmentResult("request_key_discount_details", result)
        } else {

            setFragmentResult("request_key_discount", result)
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
        tipsListAdapter.clearSelectedItem()
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