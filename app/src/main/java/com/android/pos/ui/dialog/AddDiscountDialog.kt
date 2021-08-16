package com.android.pos.ui.dialog

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
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.ui.adapter.DialogDiscountListAdapter
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.android.pos.utils.AmountTextWatcher
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddDiscountDialog : DialogFragment(), DialogDiscountListAdapter.DiscountInterface {

    private lateinit var binding: DailogAddDiscountBinding
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var discountAdapter: DialogDiscountListAdapter
    private var discountModel: TbDiscount? = null
    private var selectedListPos: Int = -1
    private var isFromDetails = false
    private var defaultModel: TbItem? = null

    companion object {
        fun newInstance() = AddDiscountDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        discountAdapter = DialogDiscountListAdapter()
        binding.rvDiscountList.adapter = discountAdapter
        setDiscountList()
        setupData()
        setKeyPad()
        isFromDetails = requireArguments().getBoolean("isFromDetails", false)
        defaultModel = requireArguments().getParcelable("model")
        discountAdapter.setSelected(defaultModel?.discountId)

        if (defaultModel?.discountId == 0) {
            binding.edtAmount.append("" + defaultModel?.discountPrice)
        }
        if (defaultModel?.discountType == getString(R.string.disc_percentage)) {
            binding.edtAmount.text.toString().replace("$", "%")
        }

        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true))

        binding.swtDiscountType.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.edtAmount.text.toString().replace("$", "%")

            } else {

                binding.edtAmount.text.toString().replace("%", "$")
            }

        }

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
            if (it.data?.isNotEmpty() == true) {
                it.data.forEach {
                    it.isChecked = false
                }
                discountAdapter.setList(it.data)
                discountAdapter.setListner(this)
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
            dismiss()
        }

        binding.txtSave.setOnClickListener {
            if (binding.edtAmount.text?.isNotEmpty() == true && !(binding.edtAmount.text.toString()
                    .equals("$0.00"))
            ) {
                val replaceCurrency = binding.edtAmount.text.toString().replace("$", "")

                discountModel =
                    if (binding.swtDiscountType.isChecked) {
                        TbDiscount(
                            "",
                            getString(R.string.disc_percentage),
                            0,
                            0,
                            "",
                            replaceCurrency.toDouble(),
                            ""
                        )
                    } else {
                        TbDiscount("", "", 0, 0, "", replaceCurrency.toDouble(), "")
                    }
                val result = Bundle().apply {
                    putParcelable("data", discountModel)
                }
                if (isFromDetails) {
                    setFragmentResult("request_key_discount_details", result)
                } else {

                    setFragmentResult("request_key_discount", result)
                }
                findNavController().navigateUp()
            } else if (selectedListPos != -1) {
                val result = Bundle().apply {
                    putParcelable("data", discountAdapter.getItem(selectedListPos))
                }


                if (isFromDetails) {
                    setFragmentResult("request_key_discount_details", result)
                } else {

                    setFragmentResult("request_key_discount", result)
                }
                findNavController().navigateUp()

            } else {
                val discount = TbDiscount("", "", 0, 0, "", 0.0, "")
                val result  = Bundle().apply {
                    putParcelable("data",discount)
                }


                if (isFromDetails) {
                    setFragmentResult("request_key_discount_details", result)
                } else {

                    setFragmentResult("request_key_discount", result)
                }
                findNavController().navigateUp()

            }


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

    override fun selectedItem(model: TbDiscount, pos: Int) {
        Log.e(TAG, "SelectedItem:  ${Gson().toJson(model)}")
        discountModel.apply { model }
        binding.edtAmount.setText("0.00")
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
        if (binding.swtDiscountType.isChecked) {
            binding.edtAmount.text.toString().replace("$", "%")
        } else {
            binding.edtAmount.text.toString().replace("%", "$")
        }


    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

}