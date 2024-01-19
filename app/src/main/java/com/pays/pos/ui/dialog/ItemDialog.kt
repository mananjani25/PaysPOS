package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.remote.Constants.DIALOG_KEY
import com.pays.pos.databinding.DialogItemsBinding

import com.pays.pos.ui.adapter.ItemListAdapter
import com.pays.pos.ui.fragments.inventory.ItemsViewModel
import com.pays.pos.utils.extensions.setNavigationResult
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ItemDialog : DialogFragment(), View.OnClickListener {
    private var where: String = ""
    private lateinit var adapter: ItemListAdapter
    private lateinit var binding: DialogItemsBinding
    private val viewModel by viewModels<ItemsViewModel>()
    var isEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_items, container, false)
        binding.lifecycleOwner = this
        setupUI()
        setAdapter()
        categoriesObserver()
        return binding.root
    }

    private fun setupUI() {
        where = arguments?.getString("where", "")!!

        if (where == "tax") {
            binding.txtTaxAll.text = getString(R.string.tv_tax_all)
        } else if (where == "modifier") {
            binding.txtTaxAll.text = getString(R.string.tv_modifier_all)
        }
        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)
        binding.txtTaxAll.setOnClickListener(this)
        binding.txtExemptAll.setOnClickListener(this)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun categoriesObserver() {

        viewModel.getItemsList(where).observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvItemList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 -> adapter.add(it1 as List<TbItem>) }


                        val itemIds = arguments?.getIntegerArrayList("itemIds")
                        if (itemIds != null) {
                            adapter.selectedItemFromEdit(itemIds)
                        }
                    }
                    Status.ERROR -> {
                        binding.rvItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        }
    }


    private fun setAdapter() {

        adapter = ItemListAdapter(true,where)
        binding.rvItemList.adapter = adapter
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtDone -> {
                Log.e("selectedItemList", "selectedItemList  ${adapter.selectedItemList().size}")
                setNavigationResult(DIALOG_KEY, adapter.selectedItemList())
                findNavController().popBackStack()
            }
            R.id.txtTaxAll -> {
                adapter.selectAll(true)
            }

            R.id.txtExemptAll -> {
                adapter.selectAll(false)
            }

        }
    }
}