package com.pays.pos.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.databinding.DialogAddToWastageBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.WastageItemReasonsAdapter
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class AddItemToWastageDialog : DialogFragment() {

    private lateinit var binding: DialogAddToWastageBinding
    private val viewModel by viewModels<DineInOrderTableViewModel>()
    private var wastageItemsReasonsList = ArrayList<VenueDetailsResponse.Data.WastageReason>()
    private lateinit var wastageItemReasonsAdapter: WastageItemReasonsAdapter
    private var wastageNote: String = ""
    private var selectedWastageReason: VenueDetailsResponse.Data.WastageReason? = null

    @Inject
    lateinit var prefProvider: PrefProvider
    var itemQuantity: Int = 1
    var quantity: Int = 0

    companion object {
        fun newInstance() = AddItemToWastageDialog()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_add_to_wastage, container, false)
        binding.lifecycleOwner = this
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        getWastageItemReasons()
        observeShowProgress()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemQuantity = arguments?.getInt("itemQuantity") ?: 1
        quantity = itemQuantity
        binding.txtQty.setText(quantity.toString())

        binding.txtQty.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                try {
                    val enteredString = s.toString()
                    if (enteredString.startsWith("0")) {
                        if (enteredString.isNotEmpty()) {
                            binding.txtQty.setText(enteredString.substring(1))
                        } else {
                            binding.txtQty.setText("")
                            quantity = 0
                        }
                    } else if (s.toString().trim().isNotEmpty() && s.toString()
                            .toInt() > itemQuantity
                    ) {
                        binding.txtQty.setText(itemQuantity.toString())
                    }
                    binding.txtQty.setSelection(binding.txtQty.length())
                    quantity = binding.txtQty.text.toString().toInt()
                } catch (e: Exception) {
                    quantity = 0
                    e.printStackTrace()
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.llPlus.setOnClickListener {
            quantity += 1
            binding.txtQty.setText(quantity.toString())
        }

        binding.llMinus.setOnClickListener {
            if (quantity > 1) {
                quantity -= 1
            }
            binding.txtQty.setText(quantity.toString())
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtDone.setOnClickListener {
            if (quantity == 0) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), getString(R.string.wastage_item_quantity_message)
                ) { _, _ -> }
            } else {
                val bundle = Bundle().apply {
                    putInt("itemQuantity", quantity)
                    if (selectedWastageReason != null) {
                        putParcelable("wastageReason", selectedWastageReason)
                    }
                    putString("wastageNote", wastageNote)
                }
                requireActivity().supportFragmentManager.setFragmentResult(
                    "request_for_add_to_wastage",
                    bundle
                )
                findNavController().navigateUp()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun observeShowProgress() {
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
    }

    private fun getWastageItemReasons() {
        viewModel.getAllWastageReasonsList.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { reasonsList ->
                            wastageItemsReasonsList.clear()
                            wastageItemsReasonsList.addAll(reasonsList as ArrayList<VenueDetailsResponse.Data.WastageReason>)
                            if (wastageItemsReasonsList.size > 0) {
                                selectedWastageReason = wastageItemsReasonsList[0]
                            }
                            wastageItemReasonsAdapter = WastageItemReasonsAdapter(object :
                                WastageItemReasonsAdapter.CustomerInterface {
                                override fun onReasonSelect(
                                    pos: Int,
                                    model: VenueDetailsResponse.Data.WastageReason
                                ) {
                                    selectedWastageReason = model
                                }

                            })
                            binding.rvWastageReasons.adapter = wastageItemReasonsAdapter

                            wastageItemReasonsAdapter.add(wastageReasonsList = wastageItemsReasonsList)
                        }
                    }

                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }

                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }
}