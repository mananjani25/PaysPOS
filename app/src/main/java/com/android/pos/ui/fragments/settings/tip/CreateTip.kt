package com.android.pos.ui.fragments.settings.tip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.Constants.CREATE_TIP
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.SETTING_KEY
import com.android.pos.databinding.DialogAddNewTipBinding
import com.android.pos.utils.AlertUtils
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

        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            tipData = arguments?.getParcelable("tipObject")!!

            viewModel.setTipData(tipData)
            viewModel.isEditData(isEdit, tipData.id)
        }

        setupSnackbar()
        observeShowProgress()
        navigate()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            val controller = findNavController()
            controller.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATE_TIP)
            controller.popBackStack()
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
            event.getContentIfNotHandled()?.let { createTipResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTipResponse.message
                    ) { _, _ ->
                        findNavController().navigateUp()
                    }
                }
            }
        })

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

}