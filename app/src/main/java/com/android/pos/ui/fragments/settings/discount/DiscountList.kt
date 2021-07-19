package com.android.pos.ui.fragments.settings.discount

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.DiscountListModel
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.databinding.DiscountFragmentBinding
import com.android.pos.databinding.FragmentDiscountBinding
import com.android.pos.ui.adapter.DiscountListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscountList : Fragment() {

    private lateinit var binding: DiscountFragmentBinding

    private var position: Int = -1
    private lateinit var discountListUpdateDelete: ArrayList<GetDiscountResponse.Data>
    private val viewModel by viewModels<DiscountListViewModel>()
    private var discountListadapter = DiscountListAdapter()
    private lateinit var discountObject: GetDiscountResponse.Data


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DiscountFragmentBinding.inflate(inflater, container, false)
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

        //setAdapter()
          onClick()

    }

    private fun setUpRecyclerView() {
        binding.rvDiscountList.adapter = discountListadapter

        object : SwipeHelper(activity, binding.rvDiscountList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    discountObject = discountListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("discountObject", discountObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_createDiscount, bundle)

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos
                    activity?.let {
                        AlertUtils.showConfirmAlert(
                            it, getString(R.string.delete_tax_message)
                        ) { _, _ ->
                            discountObject = discountListadapter.getItem(pos)
                            viewModel.delete(discountListadapter.getItem(pos).id)
                        }
                    }


                })
            }
        }
    }

    private fun getTaxListObserver() {
        viewModel.getDiscountList.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvDiscountList.visibility = View.VISIBLE
                        resource.data?.let { taxList -> setTaxData(taxList.data) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvDiscountList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvDiscountList.visibility = View.GONE
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

    private fun setTaxData(taxList: List<GetDiscountResponse.Data>) {
        discountListUpdateDelete = taxList as ArrayList<GetDiscountResponse.Data>
        discountListadapter.apply {
            addDiscount(taxList)
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

                AlertUtils.showAlert(requireActivity(), it.message)
                discountListUpdateDelete.remove(discountObject)
                discountListadapter.addDiscount(discountListUpdateDelete)
                discountListadapter.notifyItemRemoved(position)
                discountListadapter.notifyItemRangeChanged(position, discountListUpdateDelete.size)

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    private fun onClick() {
        binding.txtCreateDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_createDiscount)
        }
    }

    /*private fun setAdapter() {
        val list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Staff Meal", "20%", false))
        list.add(DiscountListModel(0, "Military", "15%", false))
        list.add(DiscountListModel(0, "Senior Citizen", "25%", false))
        binding.rvDiscountList.adapter = DiscountListAdapter(requireContext(), list)

    }*/

}