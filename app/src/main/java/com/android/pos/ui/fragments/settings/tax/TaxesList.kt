package com.android.pos.ui.fragments.settings.tax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.databinding.FragmentTaxesBinding
import com.android.pos.ui.adapter.TaxListAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaxesList : Fragment() {

    private lateinit var binding: FragmentTaxesBinding
    private val viewModel by viewModels<TaxListViewModel>()
    private lateinit var adapter: TaxListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_taxes, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTaxListObserver()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<AppCompatTextView>(R.id.txtNewTax).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_newTax)
        }
    }


    private fun setUpRecyclerView() {
        adapter = TaxListAdapter()
        binding.rvTaxList.adapter = adapter
    }


    private fun getTaxListObserver() {
        viewModel.getTaxList.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTaxList.visibility = View.VISIBLE
                        resource.data?.let { taxList -> setTaxData(taxList.data) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTaxList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvTaxList.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setTaxData(taxList: List<GetTaxResponse.Data>) {

        adapter.apply {
            addTaxes(taxList)
            notifyDataSetChanged()
        }
    }
}