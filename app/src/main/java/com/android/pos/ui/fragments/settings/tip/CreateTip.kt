package com.android.pos.ui.fragments.settings.tip

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.Constants.CREATE_TIP
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.DialogAddNewTipBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
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
            binding.edtTip.setText(String.format("%.2f",tipData.rate))
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
            if (rate.isNotEmpty()) {
                rate_double = MethodUtils.roundOffAmountDouble(rate.toDouble())
            }
            viewModel.submit(rate_double)
        }
        binding.edtTip.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val temp_rate = s.toString()
                if (temp_rate.isNotEmpty()) {
                    if (temp_rate.toFloat() > 100) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            "Please Enter Percentage less than or Equal to 100"
                        ) { _, _ ->
                            binding.edtTip.setText("")
                        }
                    }
                }
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

    private fun backPressManage() {
        val controller = findNavController()
        controller.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATE_TIP)
        controller.popBackStack()
    }


    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTipResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTipResponse.message
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