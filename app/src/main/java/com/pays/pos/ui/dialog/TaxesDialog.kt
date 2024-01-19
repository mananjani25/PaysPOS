package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.DialogTaxesBinding
import com.pays.pos.ui.adapter.TaxesAdapter
import com.pays.pos.ui.fragments.inventory.CategoriesViewModel
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TaxesDialog : DialogFragment(), View.OnClickListener {
    private var selectedIds: ArrayList<String>? = ArrayList()
    private lateinit var adapter: TaxesAdapter
    private lateinit var binding: DialogTaxesBinding
    private val viewModel by viewModels<CategoriesViewModel>()
    private var isFromEdit: Boolean? = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_taxes, container, false)
        binding.lifecycleOwner = this

        initControls()
        initObservers()

        return binding.root
    }

   /* override fun getTheme(): Int {
        return R.style.DialogWidthTheme
    }*/

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog?.window
        val size = Point()
        val display: Display? = window?.windowManager?.defaultDisplay
        display?.getSize(size)
        val width: Int = size.x
        window?.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window?.setGravity(Gravity.CENTER)
    }

    private fun initControls() {

        binding.txtTitle.text = getString(R.string.taxes)
        selectedIds = arguments?.getStringArrayList("selectedId")
        isFromEdit = arguments?.getBoolean("isEdit", false)

        adapter = TaxesAdapter(true)
        binding.rvTaxes.adapter = adapter
        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtDone -> {

                val selectedIds = ArrayList<String>()
                var nameToDisplay = ""
                if (adapter.taxList.isNotEmpty()) {
                    for (taxData in adapter.taxList) {
                        if (taxData.isChecked == true) {
                            selectedIds.add("${taxData.id}")
                            nameToDisplay += "${taxData.name}, "
                        }
                    }
                    if (nameToDisplay.isNotEmpty()) {
                        nameToDisplay = nameToDisplay.dropLast(2)
                    }
                    val result = Bundle().apply {
                        putStringArrayList("selectedId", selectedIds)
                        putString("nameToDisplay", nameToDisplay)
                    }
                    setFragmentResult("tax_request_key", result)
                }
                dismiss()
                //findNavController().navigateUp()
            }
        }
    }

    private fun initObservers() {

        viewModel.enableTaxes.observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {


                        binding.progressCircular.visibility = View.GONE

                        if (it.data?.isNotEmpty() == true) {
                            binding.rvTaxes.visibility = View.VISIBLE
                            binding.txtNodata.visibility = View.GONE

                            it.data.let { it1 ->
                                if (!isFromEdit!!) {
                                    it1.forEach { taxData ->
                                        taxData.isChecked = true
                                    }
                                } else {
                                    if (selectedIds?.isNotEmpty() == true) {
                                        it1.forEach { taxData ->
                                            taxData.isChecked =
                                                (selectedIds?.contains(taxData.id.toString()) == true)
                                        }
                                    }
                                }
                                adapter.addList(it1)
                            }

                        } else {
                            binding.rvTaxes.visibility = View.GONE
                            binding.txtNodata.visibility = View.VISIBLE
                        }
                    }
                    Status.ERROR -> {
                        binding.rvTaxes.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvTaxes.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        }
    }
}