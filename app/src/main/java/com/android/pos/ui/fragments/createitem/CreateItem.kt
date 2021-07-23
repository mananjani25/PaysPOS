package com.android.pos.ui.fragments.createitem

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.CreateItemBinding
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateItem : Fragment() {

    private var selectedId: Int = -2
    private var isEdit: Boolean = false
    private lateinit var itemObject: TbItem
    private lateinit var binding: CreateItemBinding
    private val viewModel by viewModels<CreateItemViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.create_item, container, false)
        binding.lifecycleOwner = this
        binding.createItemVewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!
        setupData()
        setupSnackbar()
        observeShowProgress()
        return binding.root
    }

    private fun setupData() {

        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            itemObject = arguments?.getParcelable("itemObject")!!
            viewModel.setData(itemObject)
        }

        binding.relativeCategory.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("selectedId", selectedId)
            }
            findNavController().navigate(R.id.action_createItem_to_categoriesDialog, bundle)
        }

        setFragmentResultListener("request_key") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCategory>("data")
            selectedId = bundle.getInt("selectedId")
            if (result != null) {
                Log.e("result", result.name)
                binding.txtCategoryName.text = result.name
            }
            // do something with the result
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CREATEITEM
            )
            navControll.popBackStack()
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

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }
}