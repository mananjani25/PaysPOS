package com.android.pos.ui.fragments.settings.loyaltypoints

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
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
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class LoyaltyPointFragment : Fragment() ,ItemCallback{

    private lateinit var binding: LoyaltyPointFragmentBinding

    private var position: Int = -1
    private lateinit var loyaltyProgramListUpdateDelete: ArrayList<LoyaltyProgramsModel>
    private val viewModel by viewModels<LoyaltyPointViewModel>()
    private lateinit var loyaltyPointAdapter: LoyaltyPointAdapter
    private lateinit var loyaltyProgramsModel: LoyaltyProgramsModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = LoyaltyPointFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getLoyaltyPointListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteLoyaltyPoint()
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
        loyaltyPointAdapter = LoyaltyPointAdapter(viewModel)
        binding.rvServiceCharge.adapter = loyaltyPointAdapter
        loyaltyPointAdapter.setCallback(this)
    }

    private fun getLoyaltyPointListObserver() {
        viewModel.loyaltyPoints.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvServiceCharge.visibility = View.VISIBLE
                        resource.data?.let { taxList ->
                        Collections.reverse(taxList)
                            setTaxData(taxList)
                        }
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

                loyaltyPointAdapter.update(it)
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
        loyaltyProgramListUpdateDelete = taxList as ArrayList<LoyaltyProgramsModel>
        loyaltyPointAdapter.apply {
            addServiceCharge(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteLoyaltyPoint() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
                loyaltyProgramListUpdateDelete.remove(loyaltyProgramsModel)
                loyaltyPointAdapter.addServiceCharge(loyaltyProgramListUpdateDelete)
                loyaltyPointAdapter.notifyItemRemoved(position)
                loyaltyPointAdapter.notifyItemRangeChanged(
                    position,
                    loyaltyProgramListUpdateDelete.size
                )

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    loyaltyProgramsModel = loyaltyPointAdapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    val loyaltyObj = loyaltyPointAdapter.getItem(pos)
                    bundle.putParcelable("loyaltyObject", loyaltyObj)
                    findNavController().navigate(
                        R.id.action_settings_to_createLoyaltyPointFragment,
                        bundle
                    )
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        if (loyaltyPointAdapter.getItem(pos).isEnable) {
                            getString(R.string.delete_active_loyalty_message)
                        } else {
                            getString(R.string.delete_loyalty_message)
                        }
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            loyaltyProgramsModel = loyaltyPointAdapter.getItem(pos)
                            viewModel.delete(loyaltyPointAdapter.getItem(pos).id)
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