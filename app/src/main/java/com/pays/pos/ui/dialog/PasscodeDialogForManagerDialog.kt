package com.pays.pos.ui.dialog

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.PasscodeDialogForManangerFragmentBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PasscodeDialogForManagerDialog : DialogFragment() {

    lateinit var binding: PasscodeDialogForManangerFragmentBinding

    companion object {
        fun newInstance() = PasscodeDialogForManagerDialog()
    }

    private val viewModel by viewModels<PasscodeDialogForManagerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PasscodeDialogForManangerFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 0, 100, 0, 100)
        dialog?.window?.setBackgroundDrawable(inset);

        val typeface: Typeface? =
            ResourcesCompat.getFont(requireActivity(), R.font.sf_pro_display_regular)
        binding.circlePin.setTypeface(typeface)
        onclickPasscode()
        observeShowProgress()
        setupSnackbar()
        binding.circlePin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                var value = s.toString()
                if (value.length == 4) {
                    LogUtil.logE("passCodeView", value)
                    viewModel.submit(value)

                }

                Log.d("yash", "afterTextChanged: $value")
            }

        })
        binding.imgBack.setOnClickListener {
            dialog?.dismiss()
        }


    }

    private fun onclickPasscode() {
        binding.first.setOnClickListener {
            binding.circlePin.append("1")
        }
        binding.second.setOnClickListener {
            binding.circlePin.append("2")
        }
        binding.third.setOnClickListener {
            binding.circlePin.append("3")
        }
        binding.fourth.setOnClickListener {
            binding.circlePin.append("4")
        }
        binding.five.setOnClickListener {
            binding.circlePin.append("5")
        }
        binding.six.setOnClickListener {
            binding.circlePin.append("6")
        }
        binding.seven.setOnClickListener {
            binding.circlePin.append("7")
        }
        binding.eight.setOnClickListener {
            binding.circlePin.append("8")
        }
        binding.nine.setOnClickListener {
            binding.circlePin.append("9")
        }
        binding.zero.setOnClickListener {
            binding.circlePin.append("0")
        }
        binding.clear.setOnClickListener {
            binding.circlePin.setText("")
        }
        binding.backspace.setOnClickListener {
            val text = binding.circlePin.text.toString()
            if (text.isNotEmpty()) {
                val temptext = text.substring(0, text.length - 1)
                binding.circlePin.setText(temptext)
            }
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    findNavController().popBackStack()
                    var isFrom = arguments?.getString("isFrom", "")
                    Log.d("yash", "observeShowProgress: " + isFrom)
                    Log.d("yash", "observeShowProgress: " + Gson().toJson(arguments))
                    if (isFrom == "orderDiscount") {
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                            arguments
                        )
                    } else if (isFrom == "itemDiscount") {
                        findNavController().navigate(
                            R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                            arguments
                        )
                    } else if (isFrom == "refundOnline") {
                        findNavController().navigate(
                            R.id.action_transaction_to_reasonForrefundonline,
                            arguments
                        )
                    } else if (isFrom == "refund") {
                        findNavController().navigate(
                            R.id.action_transactionDetailsFragment_to_issueRefundFragment,
                            arguments
                        )
                    }else if(isFrom=="rejectOnlineOrder"){
                        findNavController().navigate(
                            R.id.action_onlineOrder_to_reasonForrefundonline,
                            arguments
                        )
                    }else if(isFrom=="itemDiscountManual"){
                        findNavController().navigate(
                            R.id.action_manualSaleNew_to_addDiscountDialog,
                            arguments
                        )
                    }else if (isFrom=="orderDiscountManual"){
                        findNavController().navigate(
                            R.id.action_manualSaleNew__to_addDiscountDialog,
                            arguments
                        )
                    }
                }
            }
        }
    }

    private fun setupSnackbar() {
        viewModel.snackbarText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled().let {
                binding.circlePin.setText("")
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    it
                )
            }

        }
    }
}