package com.pays.pos.ui.fragments.settings.tip

import android.os.Bundle
import android.text.Editable
import android.text.Selection.setSelection
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
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.remote.Constants.CREATE_TIP
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.DialogAddNewTipBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.DecimalDigitsCountFilter
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateTip : Fragment() {
    private lateinit var binding: DialogAddNewTipBinding
    private val viewModel by viewModels<CreateTipsViewModel>()

    var isEdit: Boolean = false
    private lateinit var tipData: GetTipReponse.Data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAddNewTipBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!

        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.create_tip)

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_tip)


            tipData = arguments?.getParcelable("tipObject")!!

            viewModel.setTipData(tipData)
            binding.edtTip.setText(String.format("%.2f", tipData.rate))
            viewModel.isEditData(isEdit, tipData.id)
        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.header.txtSave.setOnClickListener {
            var rate = binding.edtTip.text.toString()
            var rate_double = 0.0

            if (rate.equals(".")) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please enter valid Tip Rate"
                ) { _, _ ->
                    binding.edtTip.setText("")
                }
            } else {
                if (rate.isNotEmpty()) {
                    rate_double = MethodUtils.roundOffAmountDouble(rate.toDouble())
                }
                viewModel.submit(rate_double)
            }
        }

        binding.edtTip.filters = arrayOf(DecimalDigitsCountFilter(2));
        binding.edtTip.addTextChangedListener(object : TextWatcher {
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

                            if (s?.length!! > 1 && s.startsWith("0"))
                                binding.edtTip.setText("0.")
                        } else
                            if (temp_rate.toFloat() > 100) {
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    "Please enter percentage less than or equal to 100"
                                ) { _, _ ->
                                    binding.edtTip.setText("")
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.imgBack.setOnClickListener {
            backPressManage()
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

    }

    private fun backPressManage() {
        val controller = findNavController()
        controller.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATE_TIP)
        controller.popBackStack()
    }


    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTipResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTipResponse.message
                    ) { _, _ ->
                        backPressManage()
                    }
                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

}