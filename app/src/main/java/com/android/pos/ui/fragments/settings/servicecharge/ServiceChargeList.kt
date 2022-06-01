package com.android.pos.ui.fragments.settings.servicecharge

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.ServiceChargeListResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ServiceChargeFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ServiceChargeDineinListAdapter
import com.android.pos.ui.adapter.ServiceChargeListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.*
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class ServiceChargeList : Fragment(), ServiceChargeListAdapter.ItemCallback,
    ServiceChargeDineinListAdapter.ItemCallback {

    private lateinit var binding: ServiceChargeFragmentBinding

    private var position: Int = -1
    private lateinit var discountListUpdateDelete: ArrayList<TbServiceCharge>
    private val viewModel by viewModels<ServiceChargeListViewModel>()
    private lateinit var serviceChargeListadapter: ServiceChargeListAdapter
    private lateinit var serviceChargeDineiinListadapter: ServiceChargeDineinListAdapter
    private lateinit var serviceChargeObject: TbServiceCharge
    lateinit var dinein_servicechargelist: ArrayList<TbServiceCharge>
    lateinit var takeout_servicechargelist: ArrayList<TbServiceCharge>
    var service_charge_dineinEnable = false
    var service_charge_takeoutEnable = false

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = ServiceChargeFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        prefProvider = PrefProvider(requireContext())
        setUpRecyclerView()
        observeShowProgress()
        viewModel.getServiceChargeWholeList()
        observeData()
        deleteServiceCharge()
        binding.txtAddnew.setOnClickListener {
            findNavController().navigate(
                R.id.action_settings_to_addServiceCharge,
                bundleOf("isFrom" to "dinein")
            )
        }

        binding.imgCheckBox.setOnClickListener {
            service_charge_takeoutEnable = !service_charge_takeoutEnable
            if (service_charge_takeoutEnable) {
                binding.imgCheckBox.setImageResource(R.drawable.ic_check_box)
            } else {
                binding.imgCheckBox.setImageResource(R.drawable.ic_check_box_unchecked)
            }
            viewModel.updateServiceCharge(
                service_charge_takeoutEnable,
                true,
                prefProvider.getLocationId()
            )
        }
        binding.imgCheckBoxDinein.setOnClickListener {
            service_charge_dineinEnable = !service_charge_dineinEnable
            if (service_charge_dineinEnable) {
                binding.imgCheckBoxDinein.setImageResource(R.drawable.ic_check_box)
            } else {
                binding.imgCheckBoxDinein.setImageResource(R.drawable.ic_check_box_unchecked)
            }
            viewModel.updateServiceCharge(
                service_charge_dineinEnable,
                false,
                prefProvider.getLocationId()
            )
        }
        return binding.root
    }

    private fun observeData() {
        viewModel.servicedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                binding.linearFullview?.visible()
                service_charge_dineinEnable = it.enableDineInServiceCharge
                service_charge_takeoutEnable = it.serviceChargeEnable
                if (service_charge_takeoutEnable) {
                    binding.imgCheckBox.setImageResource(R.drawable.ic_check_box)
                } else {
                    binding.imgCheckBox.setImageResource(R.drawable.ic_check_box_unchecked)
                }
                if (service_charge_dineinEnable) {
                    binding.imgCheckBoxDinein.setImageResource(R.drawable.ic_check_box)
                } else {
                    binding.imgCheckBoxDinein.setImageResource(R.drawable.ic_check_box_unchecked)
                }
                takeout_servicechargelist = arrayListOf()
                dinein_servicechargelist = arrayListOf()

                if (it.serviceCharges.isNotEmpty()) {
                    it.serviceCharges.forEach { service ->
                        if (service.orderType == "TakeOutAndParkOrder") {
                            var data: TbServiceCharge = TbServiceCharge(
                                service.createdAt,
                                service.id,
                                service.isEnabled,
                                service.locationId,
                                service.minGuestCount,
                                service.maxGuestCount,
                                service.name,
                                service.orderType,
                                service.percentage,
                                service.updatedAt
                            )
                            takeout_servicechargelist.add(data)
                        } else if (service.orderType == "DineIn") {
                            var data: TbServiceCharge = TbServiceCharge(
                                service.createdAt,
                                service.id,
                                service.isEnabled,
                                service.locationId,
                                service.minGuestCount,
                                service.maxGuestCount,
                                service.name,
                                service.orderType,
                                service.percentage,
                                service.updatedAt
                            )
                            dinein_servicechargelist.add(data)
                        }
                    }
                    setTaxDatatakeout(takeout_servicechargelist)
                    setTaxDataDinein(dinein_servicechargelist)

                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpRecyclerView() {
        serviceChargeListadapter = ServiceChargeListAdapter(viewModel)
        binding.rvServiceChargeTakeoutopenorder.adapter = serviceChargeListadapter
        serviceChargeListadapter.setCallback(this)

        serviceChargeDineiinListadapter = ServiceChargeDineinListAdapter(viewModel)
        binding.rvServiceChargeDineiin.adapter = serviceChargeDineiinListadapter
        serviceChargeDineiinListadapter.setCallback(this)
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

    private fun setTaxDatatakeout(taxList: List<TbServiceCharge>) {
        serviceChargeListadapter.apply {
            addServiceCharge(taxList)
            notifyDataSetChanged()
        }
    }

    private fun setTaxDataDinein(taxList: List<TbServiceCharge>) {
        serviceChargeDineiinListadapter.apply {
            addServiceCharge(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteServiceCharge() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {


                AlertUtils.showCustomAlert(requireActivity(), it.message)
                dinein_servicechargelist.remove(serviceChargeObject)
                serviceChargeDineiinListadapter.addServiceCharge(dinein_servicechargelist)
                serviceChargeDineiinListadapter.notifyItemRemoved(position)
                serviceChargeDineiinListadapter.notifyItemRangeChanged(
                    position,
                    dinein_servicechargelist.size
                )

            }
        }

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int, order_type: String) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_delete)?.isVisible = false
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    serviceChargeObject = serviceChargeListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("serviceChargeObject", serviceChargeObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(
                        R.id.action_settings_to_addServiceCharge,
                        bundle
                    )
                }

            }
            true
        }
        popupMenu?.show()
    }

    override fun onItemClickDineinListener(view: View?, pos: Int, order_type: String) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    serviceChargeObject = serviceChargeDineiinListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putString("isFrom", "dinein")
                    bundle.putParcelable("serviceChargeObject", serviceChargeObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(
                        R.id.action_settings_to_addServiceCharge,
                        bundle
                    )
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_service_charge_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            serviceChargeObject = serviceChargeDineiinListadapter.getItem(pos)
                            viewModel.delete(serviceChargeDineiinListadapter.getItem(pos).id)
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