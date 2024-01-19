package com.pays.pos.ui.fragments.createoption

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.Option
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DialogCreateOptionBinding
import com.pays.pos.ui.adapter.OptionAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateOptionSet : Fragment(), TextWatcher {
    private var optionSet: OptionSet? = null
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

        isEdit = arguments?.getBoolean("isEdit")!!

        setupAdapter()
        setupUI()
        setupSnackbar()
        observeShowProgress()
        observeData()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCLick()

    }

    private fun setupUI() {

        binding.edtOption.addTextChangedListener(this)
        binding.header.txtTitle.text = getString(R.string.create_option)
        binding.header.txtSave.text = getString(R.string.save)

        if (isEdit) {
            binding.header.txtTitle.text = getString(R.string.update_option_set)
            binding.header.txtSave.text = getString(R.string.update)


            optionSet = arguments?.getParcelable("optionObject")!!

            viewModel.setData(isEdit, optionSet!!.name, optionSet!!.id, optionSet!!.displayName ?: "")


            optionSet!!.options.sortedBy {
                it.sort
            }

            adapter.addAll(optionSet!!.options)
        }

        binding.header.txtSave.setOnClickListener {
            viewModel.setModifiers(adapter.getAll())
            viewModel.setDeleteModifiers(adapter.getDelete())
            viewModel.submit()
        }
    }

    private fun setupAdapter() {

        adapter = OptionAdapter(isEdit)
        binding.rvModifiers.adapter = adapter


        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val oldPos = viewHolder.bindingAdapterPosition
                val newPos = target.bindingAdapterPosition
                LogUtil.logE(
                    "reorder after", "" + ":::" + ":::" +
                            viewHolder.bindingAdapterPosition.toString() + " :::  " + target.bindingAdapterPosition.toString()
                )
                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                val a = adapter.getItem(dragFrom).sort
                val b = adapter.getItem(dragTo).sort
                LogUtil.logE("onItemMove", "$a:: $b")



                adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

                return true
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvModifiers)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        if (s.hashCode() == binding.edtOption.text.hashCode()) {
            // do other things
            binding.edtOption.removeTextChangedListener(this)

            if (s != null && s.length == 1) {
                val model = Option().apply {
                    name = binding.edtOption.text.toString().trim()
                }
                adapter.add(model)
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
        binding.header.imgBack.setOnClickListener {
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
            event.getContentIfNotHandled()?.let { createOptionResponse ->

                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createOptionResponse.message
                    ) { _, _ ->
                        val navControll = findNavController()
                        navControll.previousBackStackEntry?.savedStateHandle?.set(
                            Constants.KEY,
                            Constants.CREATEOPTION
                        )
                        navControll.popBackStack()
                    }
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