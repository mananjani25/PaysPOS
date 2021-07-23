package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.databinding.DialogCategoriesBinding
import com.android.pos.ui.adapter.CategoriesListAdapter
import com.android.pos.ui.fragments.inventory.CategoriesViewModel
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CategoriesDialog : DialogFragment() {
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

        setAdapter()
        categoriesObserver()
    }

    private fun categoriesObserver() {

        viewModel.categories.observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvCategoriesList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 -> adapter.add(it1) }
                    }
                    Status.ERROR -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        })
    }


    private fun setAdapter() {

        adapter = CategoriesListAdapter(true)
        binding.rvCategoriesList.adapter = adapter

    }
}