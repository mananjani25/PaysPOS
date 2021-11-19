package com.android.pos.ui.fragments.settings.loyaltypoints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCreateLoyaltyBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CreateLoyaltyFragment : Fragment() {

    private lateinit var binding: FragmentCreateLoyaltyBinding
    private val viewModel by viewModels<LoyaltyPointViewModel>()

    var isEdit: Boolean = false
    private var loyaltyProgramsModel: LoyaltyProgramsModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateLoyaltyBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.createLoyaltyFragment = this
        binding.viewModel = viewModel
        initControls()
        initObservers()
        return binding.root
    }

    private fun initControls() {
        isEdit = arguments?.getBoolean("isEdit")!!

        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            loyaltyProgramsModel = arguments?.getParcelable("loyaltyObject")

            viewModel.setLoyaltyData(loyaltyProgramsModel)

            binding.swtCreateLoyalty.isChecked =
                loyaltyProgramsModel?.rewardType == getString(R.string.percentage_symbol)

        } else {
            binding.swtCreateLoyalty.isChecked = true
        }
        discountType(binding.swtCreateLoyalty.isChecked)

    }

    private fun initObservers() {
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
            binding.swtCreateLoyalty.text = getString(R.string.percentage_value)
            viewModel.loyaltyPointType = getString(R.string.percentage_symbol)
            binding.tvSymbolPer.visibility = View.VISIBLE
            binding.tvSymbolDollar.visibility = View.GONE
        } else {
            binding.swtCreateLoyalty.text = getString(R.string.amount_value)
            viewModel.loyaltyPointType = getString(R.string.dollar_symbol)
            binding.tvSymbolDollar.visibility = View.VISIBLE
            binding.tvSymbolPer.visibility = View.GONE
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

        viewModel.createLoyaltyPointData.observe(viewLifecycleOwner, { event ->
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