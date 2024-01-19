package com.pays.pos.ui.fragments.settings.discount

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter

import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentCreateDiscountBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.DecimalDigitsCountFilter
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CreateDiscount : Fragment() {

    private lateinit var binding: FragmentCreateDiscountBinding
    private val viewModel by viewModels<CreateDiscountViewModel>()

    var isEdit: Boolean = false
    private lateinit var discountData: TbDiscount
    private val TAG = "CreateDiscount"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.createDiscountFragment = this


        isEdit = arguments?.getBoolean("isEdit")!!
        binding.tvSymbolPer.visibility = View.VISIBLE

        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.add_new_discount)

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_discount)
            discountData = arguments?.getParcelable("discountObject")!!

            viewModel.setDiscountData(discountData)

            val discountText = String.format("%.2f", discountData.percentage)
            val discount =
                if (discountText.toString().startsWith("0"))
                    discountText.substring(1, discountText.length)
                else
                    discountText

            binding.edtDiscount?.setText(discount)

            if (discountData.discountType == getString(R.string.disc_percentage)) {
                binding.swtDiscountType.isChecked = true
                binding.swtDiscountType.text = getString(R.string.percentage)
                binding.tvSymbolPer.visibility = View.VISIBLE
                binding.tvSymbolDollar.visibility = View.GONE
            } else {
                binding.swtDiscountType.isChecked = false
                binding.swtDiscountType.text = getString(R.string.dollar_amount)
                binding.tvSymbolDollar.visibility = View.VISIBLE
                binding.tvSymbolPer.visibility = View.GONE
            }

            viewModel.isEditData(isEdit, discountData.id)
        } else {

        }

        setupSnackbar()
        observeShowProgress()
        //addTextChangeListner()
        navigate()

        binding.edtDiscount.filters = arrayOf(DecimalDigitsCountFilter(2));

        binding.edtDiscount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    val temp_rate = s.toString()

                    if (temp_rate.isNotEmpty()) {

                        val inputValue = s.toString().toDoubleOrNull()

                        if (inputValue != null && inputValue in 0.0..0.99) {
                            if (s?.length!! == 1 && s.startsWith("0")) {
                                binding.edtDiscount.setText(".")
                                binding.edtDiscount.setSelection(binding.edtDiscount.length())
                            }
                        } else
                            if (binding.swtDiscountType.isChecked)
                                if (temp_rate.toFloat() > 100) {
                                    AlertUtils.showCustomAlertWithListenerWithOK(
                                        requireContext(),
                                        "Please enter percentage less than or equal to 100"
                                    ) { _, _ ->
                                        binding.edtDiscount.setText("")
                                    }
                                }
                    }
                    Log.e("Text", "No error")
                } catch (e: Exception) {
                    Log.e("Text Exception", e.printStackTrace().toString())
                }


                Log.e("Text Changed", s.toString())
            }

        })

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCLick()

    }

    private fun onCLick() {
        binding.header.imgBack.setOnClickListener {
            backPressManage()
        }

        binding.header.txtSave.setOnClickListener {

            var percentage = binding.edtDiscount.text.toString()
            var percentage_double = 0.0

            if (percentage.equals(".") || percentage.isEmpty()) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please enter valid Discount Rate"
                ) { _, _ ->
                    binding.edtDiscount.setText("")
                }
            } else {
                if (percentage.isNotEmpty()) {
                    try {
                        percentage_double = MethodUtils.roundOffAmountDouble(percentage.toDouble())
                        viewModel.submit(percentage_double)
                    } catch (e: Exception) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(), "Please Enter Valid Value") {_,_->
                            binding.edtDiscount.setText("")
                        }
                    }
                }
            }
        }
    }

    private fun backPressManage() {
        MethodUtils.hideKeyboard(requireActivity())
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.CREATEDISCOUNT
        )
        navControll.popBackStack()
    }

    fun discountType(isChecked: Boolean) {
        if (isChecked) {

            binding.swtDiscountType.text = "Percentage"
            viewModel.discountType(getString(R.string.disc_percentage))
            binding.tvSymbolPer.visibility = View.VISIBLE
            binding.tvSymbolDollar.visibility = View.GONE
            binding.edtDiscount?.setText("")
            val maxLength = 4
            val FilterArray: Array<InputFilter?> = arrayOfNulls<InputFilter>(1)
            FilterArray[0] = InputFilter.LengthFilter(maxLength)
            binding.edtDiscount?.filters = FilterArray

            if (binding.edtDiscount.text.toString()
                    .isNotEmpty() && binding.edtDiscount.text.toString().toDouble() > 100
            ) {
                binding.edtDiscount.setText("100")
                binding.edtDiscount.setSelection(binding.edtDiscount.length())
            }

        } else {
            binding.swtDiscountType.text = "Dollar"
            viewModel.discountType(getString(R.string.disc_amount))
            binding.tvSymbolDollar.visibility = View.VISIBLE
            binding.tvSymbolPer.visibility = View.GONE
            binding.edtDiscount?.setText("")
            val maxLength = 6
            val FilterArray: Array<InputFilter?> = arrayOfNulls<InputFilter>(1)
            FilterArray[0] = InputFilter.LengthFilter(maxLength)
            binding.edtDiscount?.filters = FilterArray
        }
    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createDiscountResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createDiscountResponse.message
                    ) { _, _ ->
                        backPressManage()
                    }
                }
            }
        })
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}