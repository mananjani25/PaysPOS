package com.android.pos.ui.fragments.settings.tip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.DiscountListModel
import com.android.pos.databinding.FragmentTipsBinding
import com.android.pos.ui.adapter.DiscountListAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Tips : Fragment() {

    private lateinit var binding: FragmentTipsBinding
    private val viewModel by viewModels<TipListViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

       // setUpRecyclerView()
        getTipListObserver()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
     //   setAdapter()
        binding.txtAddNewTip.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addTip)
        }
    }

   /* private fun setUpRecyclerView() {
        adapter = TaxListAdapter()
        binding.rvTipList.adapter = adapter
    }*/

    private fun getTipListObserver() {
        viewModel.getTipList.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                      /*  resource.data?.let { taxList -> setTaxData(taxList) }*/
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvTipList.visibility = View.GONE
                    }
                }
            }
        })
    }

   /* private fun setTaxData(taxList: List<GetTaxResponse.Data>) {

        adapter.apply {
            addTaxes(taxList)
            notifyDataSetChanged()
        }
    }*/

    private fun setAdapter() {
        var list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Entertainment", "3.45%", false))
        list.add(DiscountListModel(0, "Tip One", "3.45%", false))
        list.add(DiscountListModel(0, "Entertainment Tip Two", "3.45%", false))
        list.add(DiscountListModel(0, "Tip Three", "3.45%", false))
        binding.rvTipList.adapter = DiscountListAdapter(requireContext(), list)
    }
}