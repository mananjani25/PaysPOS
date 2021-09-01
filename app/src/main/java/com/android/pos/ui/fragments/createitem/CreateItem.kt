package com.android.pos.ui.fragments.createitem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.Option
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_OPTIONS
import com.android.pos.databinding.CreateItemBinding
import com.android.pos.ui.adapter.ModifierSetsListAdapter
import com.android.pos.ui.adapter.VariationListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateItem : Fragment(), View.OnClickListener {

    private var selectedId: Int = -2
    private var isEdit: Boolean = false
    private lateinit var itemObject: TbItem
    private lateinit var binding: CreateItemBinding
    private val viewModel by viewModels<CreateItemViewModel>()
    private lateinit var variationListAdapter: VariationListAdapter
    private var optionSetList: ArrayList<OptionSet>? = null

    private lateinit var adapter: ModifierSetsListAdapter
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
        setAdapter()
        setupSnackbar()
        observeShowProgress()
        getModifiers()
        navigate()

        binding.tvAddOptions.setOnClickListener {

            val bundle = Bundle()
            if (isEdit) {
                bundle.putBoolean("isEdit", true)
                //  bundle.putIntegerArrayList("optionIds", itemIds)
            } else {
                bundle.putParcelableArrayList("optionSets", optionSetList)
            }
            findNavController().navigate(R.id.action_createItem_to_itemOptionsListDialog, bundle)
        }


        val resultDialogOptionIds = getNavigationResultLiveData<ArrayList<OptionSet>>(DIALOG_KEY)
        resultDialogOptionIds?.observe(viewLifecycleOwner) {
            // it.clear()
            optionSetList = it

            /*if (it.size > 0) {
                binding.itemsCount.text = "" + it.size + " Items"
            } else {
                binding.itemsCount.text = "No Items"
            }

            it.forEach {
                itemIds.add(it.itemId)
            }*/
            // viewModel.setItemIds(itemIds)
        }


        val resultDialogVariations =
            getNavigationResultLiveData<List<List<Option>>>(DIALOG_KEY_OPTIONS)
        resultDialogVariations?.observe(viewLifecycleOwner) { variationList ->
            //  var variationsName = ArrayList<String>()
            /*variationList?.forEach {
                it.map {
                    variationsName.add(it.name)
                }
            }*/
            binding.llVariationTitle.visibility = View.VISIBLE
            variationListAdapter.addVariations(variationList as ArrayList<List<Option>>)
        }

        return binding.root
    }

    private fun getModifiers() {

        viewModel.modifierSet.observe(requireActivity(), {

            it.data?.let { it1 -> adapter.add(it1) }
        })
    }

    private fun setAdapter() {
        adapter = ModifierSetsListAdapter(true)
        binding.rvModifiersList.adapter = adapter

        variationListAdapter = VariationListAdapter(viewModel)
        binding.rvVariationList.adapter = variationListAdapter
    }

    private fun setupData() {


        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            itemObject = arguments?.getParcelable("itemObject")!!
            viewModel.setData(itemObject)
        }

        binding.chooseCategory.setOnClickListener(this)
        binding.imgEdit.setOnClickListener(this)

        setFragmentResultListener("request_key") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCategory>("data")
            selectedId = bundle.getInt("selectedId")
            if (result != null) {
                if (result.name == "None") {
                    binding.txtCategoryName.text = ""
                } else
                    binding.txtCategoryName.text = result.name
            }
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chooseCategory -> {
                val bundle = Bundle().apply {
                    putInt("selectedId", selectedId)
                }
                findNavController().navigate(R.id.action_createItem_to_categoriesDialog, bundle)
            }

            R.id.imgEdit -> {
                findNavController().navigate(R.id.action_createItem_to_itemEditTitleDialog)
            }
        }
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        findNavController().navigateUp()
                    }
                }

            }
        })
    }
}