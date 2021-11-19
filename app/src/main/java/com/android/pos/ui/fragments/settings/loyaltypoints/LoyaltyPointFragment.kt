package com.android.pos.ui.fragments.settings.loyaltypoints

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
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.databinding.LoyaltyPointFragmentBinding
import com.android.pos.ui.adapter.LoyaltyPointAdapter
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
class LoyaltyPointFragment : Fragment() {

    private lateinit var binding: LoyaltyPointFragmentBinding

    private var position: Int = -1
    private lateinit var discountListUpdateDelete: ArrayList<LoyaltyProgramsModel>
    private val viewModel by viewModels<LoyaltyPointViewModel>()
    private lateinit var serviceChargeListadapter: LoyaltyPointAdapter
    private lateinit var serviceChargeObject: LoyaltyProgramsModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = LoyaltyPointFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTaxListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteServiceCharge()
        notifyAdapter()
        initListeners()

        return binding.root
    }

    private fun initListeners() {
        binding.txtAddLoyaltyProgram.setOnClickListener {

            findNavController().navigate(R.id.action_settings_to_createLoyaltyPointFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        view.findViewById<AppCompatTextView>(R.id.txtAddServiceCharge).setOnClickListener {
//            findNavController().navigate(R.id.action_settings_to_addServiceCharge)
//        }
    }

    private fun setUpRecyclerView() {
        serviceChargeListadapter = LoyaltyPointAdapter(viewModel)
        binding.rvServiceCharge.adapter = serviceChargeListadapter

        object : SwipeHelper(activity, binding.rvServiceCharge) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    serviceChargeObject = serviceChargeListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    val loyaltyObj = serviceChargeListadapter.getItem(pos)
                    bundle.putParcelable("loyaltyObject", loyaltyObj)
                    findNavController().navigate(
                        R.id.action_settings_to_createLoyaltyPointFragment,
                        bundle
                    )
                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_service_charge_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            serviceChargeObject = serviceChargeListadapter.getItem(pos)
                            viewModel.delete(serviceChargeListadapter.getItem(pos).id)
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
        viewModel.loyaltyPoints.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvServiceCharge.visibility = View.VISIBLE
                        resource.data?.let { taxList -> setTaxData(taxList) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvServiceCharge.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvServiceCharge.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                serviceChargeListadapter.notifyDataSetChanged()
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

    private fun setTaxData(taxList: List<LoyaltyProgramsModel>) {
        discountListUpdateDelete = taxList as ArrayList<LoyaltyProgramsModel>
        serviceChargeListadapter.apply {
            addServiceCharge(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteServiceCharge() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
                discountListUpdateDelete.remove(serviceChargeObject)
                serviceChargeListadapter.addServiceCharge(discountListUpdateDelete)
                serviceChargeListadapter.notifyItemRemoved(position)
                serviceChargeListadapter.notifyItemRangeChanged(
                    position,
                    discountListUpdateDelete.size
                )

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}