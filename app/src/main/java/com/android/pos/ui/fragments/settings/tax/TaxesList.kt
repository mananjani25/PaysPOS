package com.android.pos.ui.fragments.settings.tax

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.databinding.FragmentTaxesBinding
import com.android.pos.ui.adapter.TaxListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaxesList : Fragment() {

    private var position: Int = -1
    private lateinit var taxListUpdateDelete: ArrayList<GetTaxResponse.TaxData>
    private lateinit var binding: FragmentTaxesBinding
    private val viewModel by viewModels<TaxListViewModel>()
    private var taxListadapter = TaxListAdapter()
    private lateinit var taxObject: GetTaxResponse.TaxData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_taxes, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTaxListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteTax()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<AppCompatTextView>(R.id.txtNewTax).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_newTax)
        }
    }


    private fun setUpRecyclerView() {
        binding.rvTaxList.adapter = taxListadapter

        object : SwipeHelper(activity, binding.rvTaxList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    taxObject = taxListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("taxObject", taxObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_newTax, bundle)

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_tax_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            taxObject = taxListadapter.getItem(pos)
                            viewModel.delete(taxListadapter.getItem(pos).id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }

                })
            }
        }
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

    private fun setTaxData(taxList: List<GetTaxResponse.TaxData>) {
        taxListUpdateDelete = taxList as ArrayList<GetTaxResponse.TaxData>
        taxListadapter.apply {
            addTaxes(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteTax() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/


                AlertUtils.showCustomAlert(requireActivity(), it.message)
                taxListUpdateDelete.remove(taxObject)
                taxListadapter.addTaxes(taxListUpdateDelete)
                taxListadapter.notifyItemRemoved(position)
                taxListadapter.notifyItemRangeChanged(position, taxListUpdateDelete.size)

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
}