package com.android.pos.ui.fragments.settings.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.Constants.CREATE_NOTES
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.CreateNoteBinding
import com.android.pos.ui.fragments.settings.tax.CreateTaxViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
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

        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            noteData = arguments?.getParcelable("taxObject")!!

            viewModel.setNoteData(noteData)
            viewModel.isEditData(isEdit, noteData.id)
        }

        setupSnackbar()
        observeShowProgress()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATE_NOTES)
            navController.popBackStack()
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
            event.getContentIfNotHandled()?.let {
                if (it) {
                    findNavController().navigateUp()
                }
            }
        })


    }


    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}
