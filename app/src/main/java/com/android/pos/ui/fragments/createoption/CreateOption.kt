package com.android.pos.ui.fragments.createoption

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.DialogCreateOptionBinding
import com.android.pos.ui.adapter.OptionAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class CreateOption : Fragment(), TextWatcher {

    private lateinit var binding: DialogCreateOptionBinding
    private lateinit var adapter: OptionAdapter
    private val viewModel by viewModels<CreateOptionViewModel>()
    var dragFrom = -1
    var dragTo = -1
    private var isEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogCreateOptionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.createOptionViewModel = viewModel

//        isEdit = arguments?.getBoolean("isEdit")!!

        // setupAdapter()
        // setupUI()
        setupSnackbar()
        observeShowProgress()
        observeData()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCLick()

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        if (s.hashCode() == binding.edtOption.text.hashCode()) {
            // do other things
            binding.edtOption.removeTextChangedListener(this)

            /*if (s != null && s.length == 1) {
                val model = Modifier().apply {
                    name = binding.edtModifier.text.toString().trim()
                    price = 0.00
                }
                adapter.add(model)
            }*/
            binding.edtOption.text?.clear()
            binding.edtOption.clearFocus()
            binding.edtOption.addTextChangedListener(this)
        }

        if (s.hashCode() == binding.edtOption.text.hashCode()) {
            binding.edtOption.removeTextChangedListener(this)

            if (s != null && s.length == 1) {

                val parsed = s.toString().toDouble()
                val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))
                /*val model = Modifier().apply {
                    name = ""
                    price = formatted.replace("""[$,]""".toRegex(), "").toDouble()
                }
                adapter.add(model)*/
            }
            binding.edtOption.text?.clear()
            binding.edtOption.clearFocus()
            binding.edtOption.addTextChangedListener(this)
        }

        viewModel.setModifiers(adapter.getAll())

    }

    override fun afterTextChanged(s: Editable?) {
    }

    private fun onCLick() {
        binding.imgBack.setOnClickListener {
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CREATEOPTION
            )
            navControll.popBackStack()
        }
    }

    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    val navControll = findNavController()
                    navControll.previousBackStackEntry?.savedStateHandle?.set(
                        Constants.KEY,
                        Constants.CREATEOPTION
                    )
                    navControll.popBackStack()
                }
            }
        })
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

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }
}