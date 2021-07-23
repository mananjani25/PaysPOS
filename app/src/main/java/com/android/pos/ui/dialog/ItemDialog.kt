package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.DialogItemsBinding
import com.android.pos.ui.adapter.ItemListAdapter
import com.android.pos.ui.fragments.inventory.ItemsViewModel
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ItemDialog : DialogFragment(), View.OnClickListener {
    private lateinit var adapter: ItemListAdapter
    private lateinit var binding: DialogItemsBinding
    private val viewModel by viewModels<ItemsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_items, container, false)
        binding.lifecycleOwner = this

//        selectedId = arguments?.getInt("selectedId", -2)!!

        setAdapter()
        categoriesObserver()


        return binding.root
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

        viewModel.items.observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvItemList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 -> adapter.add(it1 as List<TbItem>) }
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


        })
    }


    private fun setAdapter() {

        adapter = ItemListAdapter(true)
        //adapter.setPos(selectedId)
        binding.rvItemList.adapter = adapter
        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)
        binding.txtTaxAll.setOnClickListener(this)
        binding.txtExemptAll.setOnClickListener(this)

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

                adapter.selectedItemList()
                /* val chooseModel = adapter.getData()
                 selectedId = adapter.getPos()
                 if (chooseModel != null) {
                     val result = Bundle().apply {
                         putParcelable("data", chooseModel)
                         putInt("selectedId", selectedId)
                     }
                     setFragmentResult("request_key", result)
                 }
                 findNavController().navigateUp()*/
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