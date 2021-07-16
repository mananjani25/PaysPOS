package com.android.pos.ui.fragments.settings.tax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.databinding.DialogCreateNewTaxBinding
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateTax : Fragment() {

    private lateinit var binding: DialogCreateNewTaxBinding

    private val viewModel by viewModels<CreateTaxViewModel>()

    var isEdit: Boolean = false
    private lateinit var taxData: GetTaxResponse.TaxData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_create_new_tax, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel


        isEdit = arguments?.getBoolean("isEdit")!!

        if (isEdit) {
            taxData = arguments?.getParcelable("taxObject")!!

            viewModel.createTaxDetails.value?.name = taxData.name
            viewModel.createTaxDetails.value?.rate = taxData.rate.toDouble()

            viewModel.isEditData(isEdit,taxData.id)
        }

        setupSnackbar()
        observeShowProgress()
        // navigate()


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatImageView>(R.id.imgBack).setOnClickListener {
            findNavController().navigateUp()
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

    /*private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    findNavController().navigate(R.id.action_login_to_scheduledShifts)
                }
            }
        })

    }*/

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}