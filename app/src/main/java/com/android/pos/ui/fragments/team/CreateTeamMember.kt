package com.android.pos.ui.fragments.team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentCreateTeamMemberBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateTeamMember : Fragment() {


    private var employeeModel: Employee? = null
    private val viewModel by viewModels<CreateTeamViewModel>()

    private lateinit var binding: FragmentCreateTeamMemberBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_create_team_member,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.imgClose.setOnClickListener {
            findNavController().navigateUp()
        }

        employeeModel = arguments?.getParcelable("data")

        if (employeeModel != null) {
            binding.btnSave.text = getString(R.string.update)
            binding.txtTitle.text = "Edit Team Member"

            viewModel.setTaxData(employeeModel!!)
            viewModel.isEditData(true, employeeModel!!.id)
        }


        setupSnackbar()
        observeShowProgress()
        navigate()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        findNavController().popBackStack()
                    }
                }
            }
        })

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}