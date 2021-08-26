package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.entities.OptionSet
import com.android.pos.databinding.FragmentItemOptionsListBinding
import com.android.pos.ui.adapter.OptionListAdapter
import com.android.pos.ui.adapter.SelectedItemOptionsListAdapter
import com.android.pos.ui.fragments.inventory.OptionSetViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import android.view.MotionEvent


@AndroidEntryPoint
class ItemOptionsListDialog : DialogFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var roleName = ArrayList<String>()
    private lateinit var binding: FragmentItemOptionsListBinding

    private lateinit var adapter: SelectedItemOptionsListAdapter
    private lateinit var itemOptionList: ArrayList<OptionSet>
    private val viewModel by viewModels<OptionSetViewModel>()
    private var spinnerTouched = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemOptionsListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.spOptions.onItemSelectedListener = this
        setAdapter()

        binding.spOptions.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                spinnerTouched = true
                return false
            }
        })

        binding.txtDone.setOnClickListener {

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        optionSetObserver()
        observeShowProgress()

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


    private fun setAdapter() {
        adapter = SelectedItemOptionsListAdapter()
        binding.rvOptonList.adapter = adapter
    }

    private fun optionSetObserver() {

        viewModel.optionSets().observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        it.data?.let { it1 ->
                            itemOptionList =
                                it1 as ArrayList<OptionSet>

                            val optionSet = OptionSet()
                            optionSet.name = getString(R.string.tv_select_option_set)
                            itemOptionList.add(0, optionSet)
                            roleName = itemOptionList.map { it.name } as ArrayList<String>

                            setUpOptionSpinnerAdapter(roleName)

                        }
                    }
                    Status.ERROR -> {
                    }
                    Status.LOADING -> {
                    }
                }
            }


        })
    }

    private fun setUpOptionSpinnerAdapter(orderTypeList: ArrayList<String>) {

        spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            orderTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spOptions.adapter = spinnerAdapter


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

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (binding.spOptions.selectedItem == getString(R.string.tv_select_option_set)) {
            return
        }

        if (spinnerTouched) {
            adapter.addOptions(itemOptionList[position].options)
            if (itemOptionList[position].name == binding.spOptions.selectedItem) {
                roleName.removeAt(position)
                itemOptionList.removeAt(position)
                binding.spOptions.setSelection(0, false)
                spinnerAdapter.notifyDataSetChanged()
            }
        }
        spinnerTouched = false

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

}