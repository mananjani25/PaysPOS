package com.pays.pos.ui.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.databinding.DialogCancelOrderReasonBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.CancelOrderReasonAdapter
import com.pays.pos.ui.fragments.allorders.AllOrdersViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ReasonForCancelOrderDialog : DialogFragment() {

    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogCancelOrderReasonBinding
    private lateinit var refundData: RefundRequestModel
    private val viewModel by viewModels<AllOrdersViewModel>()
    var cancelOrderReasonsList = ArrayList<VenueDetailsResponse.Data.CancelOrderReason>()
    private lateinit var cancelOrderReasonAdapter: CancelOrderReasonAdapter
    private var itemPos: Int = 0
    var reason_id = 0

    @Inject
    lateinit var prefProvider: PrefProvider
    var orderId: Int? = null
    var startDate: String = ""
    var endDate: String = ""

    companion object {
        fun newInstance() = ReasonForCancelOrderDialog()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_cancel_order_reason, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        orderId = arguments?.getInt("orderId")
        startDate = arguments?.getString("startDate").toString()
        endDate = arguments?.getString("endDate").toString()

        binding.txtDone.setOnClickListener {

            val reason = binding.etReason.text.toString().trim()

            if (reason.isEmpty()) {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.cancel_order_reason_message)
                ) {
                    positiveButton(getString(R.string.tv_ok)) {

                    }
                    negativeButton(R.string.cancel) {
                        // Do negative stuff here
                    }
                }
            } else {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.cancel_order_message)
                ) {
                    positiveButton(getString(R.string.yes)) {
                        viewModel.cancelOrder(orderId!!, reason, reason_id)
                    }
                    negativeButton(R.string.no) {
                        // Do negative stuff here
                    }
                }
            }

        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        getCancelOrderReasons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


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

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

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

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        dismiss()
                        val intent = Intent()
                        intent.action = "cancelled"
                        intent.putExtra("isCount", false)
                        intent.putExtra("start_date", startDate)
                        intent.putExtra("end_date", endDate)
                        intent.putExtra("position", 2)
                        requireContext().sendBroadcast(intent)
                    }
                }
            }
        }


    }

    private fun navigate() {
    }

    private fun getCancelOrderReasons() {
        viewModel.getcancelOrderReasonsDatabse.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { reasonsList ->
                            cancelOrderReasonsList.clear()
                            cancelOrderReasonsList.addAll(it.data as ArrayList<VenueDetailsResponse.Data.CancelOrderReason>)

                            cancelOrderReasonAdapter = CancelOrderReasonAdapter(object :
                                CancelOrderReasonAdapter.CustomerInteface {
                                override fun onReasonSelect(
                                    pos: Int,
                                    model: VenueDetailsResponse.Data.CancelOrderReason
                                ) {
                                    reason_id = model.id
                                    itemPos = model.id
                                    binding.etReason.setText(model.reason)

                                    /*binding.layoutTool.txtSubTitle.setText(model.first_name + " " + model.last_name)
                                    loadFragment(model)*/
                                }

                            })
                            binding.rvReasons.adapter = cancelOrderReasonAdapter

                            cancelOrderReasonAdapter.add(cancelOrderReasonsList)


                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }


    }


}