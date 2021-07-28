package com.android.pos.ui.fragments.settings.discount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCreateDiscountBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CreateDiscount : Fragment() {

    private lateinit var binding: FragmentCreateDiscountBinding
    private val viewModel by viewModels<CreateDiscountViewModel>()

    var isEdit: Boolean = false
    private lateinit var discountData: GetDiscountResponse.Data

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

        if (isEdit) {
            discountData = arguments?.getParcelable("discountObject")!!

            viewModel.setDiscountData(discountData)

            if (discountData.discountType == getString(R.string.disc_percentage)) {
                binding.swtDiscountType.isChecked = true
                binding.swtDiscountType.text = getString(R.string.disc_percentage)
            } else {
                binding.swtDiscountType.isChecked = false
                binding.swtDiscountType.text = getString(R.string.disc_amount)
            }

            viewModel.isEditData(isEdit, discountData.id)
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCLick()

    }

    private fun onCLick() {
        binding.imgBack.setOnClickListener {
            backPressManage()
        }
    }

    private fun backPressManage() {
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.CREATEDISCOUNT
        )
        navControll.popBackStack()
    }

    fun discountType(isChecked: Boolean) {
        if (isChecked) {
            binding.swtDiscountType.text = getString(R.string.disc_percentage)
            viewModel.discountType(getString(R.string.disc_percentage))
        } else {
            binding.swtDiscountType.text = getString(R.string.disc_amount)
            viewModel.discountType(getString(R.string.disc_amount))
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