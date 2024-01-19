package com.pays.pos.ui.fragments.settings.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTaxResponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.remote.Constants.CREATE_NOTES
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.CreateNoteBinding
import com.pays.pos.ui.fragments.settings.tax.CreateTaxViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateNote : Fragment() {
    private lateinit var binding: CreateNoteBinding
    private val viewModel by viewModels<CreateNoteViewModel>()

    var isEdit: Boolean = false
    private lateinit var noteData: NoteResponse.Data


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CreateNoteBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!

        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.add_new_note)

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_note)
            noteData = arguments?.getParcelable("taxObject")!!

            viewModel.setNoteData(noteData)
            viewModel.isEditData(isEdit, noteData.id)
        }

        setupSnackbar()
        observeShowProgress()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.header.txtSave.setOnClickListener {
            viewModel.submit()
        }
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

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        backPressManage()
                    }
                }
            }
        })


    }

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATE_NOTES)
        navController.popBackStack()
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}
