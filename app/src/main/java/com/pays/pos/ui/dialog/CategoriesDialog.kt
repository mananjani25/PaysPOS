package com.pays.pos.ui.dialog

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
import com.pays.pos.R
import com.pays.pos.databinding.DialogCategoriesBinding
import com.pays.pos.ui.adapter.CategoriesListAdapter
import com.pays.pos.ui.fragments.inventory.CategoriesViewModel
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CategoriesDialog : DialogFragment(), View.OnClickListener {
    private var selectedId: Int = 0
    private lateinit var adapter: CategoriesListAdapter
    private lateinit var binding: DialogCategoriesBinding
    private val viewModel by viewModels<CategoriesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_categories, container, false)
        binding.lifecycleOwner = this

        selectedId = arguments?.getInt("selectedId", -2)!!

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

        viewModel.categoriesAll.observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.progressCircular.visibility = View.GONE
                        if (it.data?.isNotEmpty() == true) {

                            binding.rvCategoriesList.visibility = View.VISIBLE
                            binding.txtNodata?.visibility = View.GONE

                            it.data.let { it1 -> adapter.add(it1) }
                            if (selectedId != -2)
                                adapter.setPos(selectedId)

                        } else {
                            binding.rvCategoriesList.visibility = View.GONE
                            binding.txtNodata?.visibility = View.VISIBLE
                        }

                    }
                    Status.ERROR -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                        binding.txtNodata?.visibility = View.VISIBLE
                    }
                    Status.LOADING -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.txtNodata?.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        }
    }


    private fun setAdapter() {

        adapter = CategoriesListAdapter(true)
        binding.rvCategoriesList.adapter = adapter
        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)

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

                val chooseModel = adapter.getData()
                if (chooseModel != null) {
                    selectedId = adapter.getPos()
                    val result = Bundle().apply {
                        putParcelable("data", chooseModel)
                        putInt("selectedId", selectedId)
                    }
                    setFragmentResult("request_key", result)
                    findNavController().navigateUp()
                }
            }

        }
    }
}