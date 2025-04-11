package com.pays.pos.ui.fragments.settings.loyaltypoints

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.pays.pos.R
import com.pays.pos.data.entities.LoyaltyProgramsModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentCreateLoyaltyBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
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
        initListeners()
        setupKeyboard()
        return binding.root
    }


    private fun initListeners() {
        binding.swtFixedValue.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.cbPercentageValue.isChecked = false
                discountType(false)
            }

        }
        binding.cbPercentageValue.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.swtFixedValue.isChecked = false
                discountType(true)
            }

        }
    }

    private fun initControls() {
        isEdit = arguments?.getBoolean("isEdit")!!


        if (isEdit) {

            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_loyalty_point)
            loyaltyProgramsModel = arguments?.getParcelable("loyaltyObject")

            viewModel.setLoyaltyData(loyaltyProgramsModel)

            if (loyaltyProgramsModel?.rewardType == getString(R.string.percentage_symbol)) {
                binding.cbPercentageValue.isChecked =
                    loyaltyProgramsModel?.rewardType == getString(R.string.percentage_symbol)
                discountType(true)
            } else {
                binding.swtFixedValue.isChecked =
                    loyaltyProgramsModel?.rewardType == getString(R.string.dollar_symbol)
                discountType(false)

            }
            binding.editLoyaltyTarget.setText(loyaltyProgramsModel!!.rewardPoint.toString())
            binding.editLoyaltyAmount.setText(String.format(getString(R.string.format), loyaltyProgramsModel!!.amount))

        } else {
            binding.header.txtSave.text = getString(R.string.save)
            binding.header.txtTitle.text = getString(R.string.create_loyalty_point)

            binding.cbPercentageValue.isChecked = false
            binding.swtFixedValue.isChecked = true

            discountType(false)

        }

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
        binding.editLoyaltyAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {

                    Log.e("Tracking Service Charge","Changed "+s.toString())

                    var amount = s.toString()
                    if(viewModel.loyaltyPointType == getString(R.string.percentage_symbol)){
                        if (amount.toInt() > 100) {
                            binding.editLoyaltyAmount!!.setText("100")
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun onCLick() {
        binding.header.imgBack.setOnClickListener {
            backPressManage()
        }
        binding.header.txtSave.setOnClickListener {
            if(binding.editLoyaltyAmount.text.toString().isNotEmpty()) {
                viewModel.setLoyaltyAmount(binding.editLoyaltyAmount.text.toString().toDouble())
            }else{
                viewModel.setLoyaltyAmount(0.00)
            }
            if(binding.editLoyaltyTarget.text.toString().isNotEmpty()) {
                viewModel.setLoyaltyTarget(binding.editLoyaltyTarget.text.toString().toInt())
            }else{
                viewModel.setLoyaltyTarget(0)
            }
            viewModel.submit()
        }
    }

    private fun backPressManage() {
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.CREATELOYALTY
        )
        navControll.popBackStack()
    }

    fun discountType(isChecked: Boolean) {
        if (isChecked) {
            binding.cbPercentageValue?.text = getString(R.string.percentage_value)
            viewModel.loyaltyPointType = getString(R.string.percentage_symbol)
            binding.tvSymbolPer.visibility = View.VISIBLE
            binding.editLoyaltyAmount?.setText("")
            val maxLength = 4
            val FilterArray: Array<InputFilter?> = arrayOfNulls<InputFilter>(1)
            FilterArray[0] = InputFilter.LengthFilter(maxLength)
            binding.editLoyaltyAmount?.filters = FilterArray
            binding.tvSymbolDollar.visibility = View.GONE
        } else {
            binding.swtFixedValue.text = getString(R.string.fixed_value)
            viewModel.loyaltyPointType = getString(R.string.dollar_symbol)
            binding.tvSymbolDollar.visibility = View.VISIBLE
            binding.tvSymbolPer.visibility = View.GONE
            binding.editLoyaltyAmount?.setText("")
            val maxLength = 7
            val FilterArray: Array<InputFilter?> = arrayOfNulls<InputFilter>(1)
            FilterArray[0] = InputFilter.LengthFilter(maxLength)
            binding.editLoyaltyAmount?.filters = FilterArray
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

    private fun setupKeyboard() {
        binding.editLoyaltyTarget.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {

                binding.loyaltyBox?.postDelayed({
                    binding.loyaltyBox!!.smoothScrollTo(0, binding.editLoyaltyTarget.top+1)
                }, 200)
            }
        }

    }



    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}