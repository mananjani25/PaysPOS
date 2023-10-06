package com.android.pos.ui.fragments.settings.tax

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.databinding.FragmentTaxesBinding
import com.android.pos.ui.adapter.TaxListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaxesList : Fragment(), ItemCallback {

    private var position: Int = -1
    private lateinit var taxListUpdateDelete: ArrayList<TaxData>
    private lateinit var binding: FragmentTaxesBinding
    private val viewModel by activityViewModels<TaxListViewModel>()
    private lateinit var taxListadapter: TaxListAdapter
    private lateinit var taxObject: TaxData
    private val TAG = "TaxesList"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_taxes, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        observeShowProgress()
        observeData()
        viewModel.getTextList()
//        getTaxListObserver()
        setupSnackbar()
        deleteTax()
        notifyAdapter()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<AppCompatTextView>(R.id.txtNewTax).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_newTax)
        }
    }


    private fun setUpRecyclerView() {
        taxListadapter = TaxListAdapter(viewModel)
        taxListadapter.setCallback(this)
        binding.rvTaxList.adapter = taxListadapter
    }

    private fun observeData() {
        viewModel.taxesData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.data.isNotEmpty()) {

                    setTaxData(it.data)
                }
            }
        }
    }

    private fun getTaxListObserver() {
        viewModel.getTaxList.observe(viewLifecycleOwner) {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        Log.e(TAG, "taxListData ${Gson().toJson(it.data)}")
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTaxList.visibility = View.VISIBLE
                        resource.data?.let { taxList ->
//                            Collections.reverse(taxList)
                            setTaxData(taxList)
                        }
                        viewModel.setTaxData()
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
        }
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                taxListadapter.notifyDataSetChanged()
            }
        }
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

    private fun setTaxData(taxList: List<TaxData>) {
         var taxListRecyclerview =taxList.filter { it.isDeleted == false }
        taxListUpdateDelete = taxList as ArrayList<TaxData>
        taxListadapter.taxList.clear()
        taxListadapter.apply {
            addTaxes(taxListRecyclerview)
            notifyDataSetChanged()
        }
    }

    private fun deleteTax() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/


                AlertUtils.showCustomAlert(requireActivity(), it.message)
                 taxListUpdateDelete.remove(taxObject)
                var taxNewList =   taxListUpdateDelete.filter { it.isDeleted == false }
                 taxListadapter.addTaxes(taxNewList)
                 taxListadapter.notifyItemRemoved(position)
                 taxListadapter.notifyItemRangeChanged(position, taxNewList.size)

            }
        }

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    taxObject = taxListadapter.getItem(pos)
                    Log.e(TAG,"itemIdsSize:  ${taxObject.itemIds?.size}")
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("taxObject", taxObject)
                    findNavController().navigate(R.id.action_settings_to_newTax, bundle)
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        if (taxListadapter.getItem(pos).isActive) {
                            getString(R.string.delete_active_tax_message)
                        }else{
                            getString(R.string.delete_tax_message)
                        }
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
                }
            }
            true
        }
        popupMenu?.show()
    }
}