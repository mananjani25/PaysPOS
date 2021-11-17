package com.android.pos.ui.fragments.transactions

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.IS_REFUND
import com.android.pos.databinding.FragmentTransactionDetailsBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.OrderDetailsItemListAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TimeFormatUtils.convertCurrentDate
import com.android.pos.utils.TimeFormatUtils.convertCurrentTime
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionDetailsBinding
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private lateinit var orderDetailsItemAdapter: OrderDetailsItemListAdapter
    private lateinit var paymentDetailsResponse: GetPaymentOrderDetailsResponse
    private var orderId: Int = -1
    private var paymentId: Int = -1
    private var isFromTrans: Boolean = false

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_transaction_details,
            container,
            false
        )

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        orderId = arguments?.getInt("orderId")!!
        paymentId = arguments?.getInt("paymentId")!!
        isFromTrans = arguments?.getBoolean("isFromTrans")!!
        if (isFromTrans) {
            viewModel.apiCallPaymentDetails(paymentId)
        } else {
            viewModel.apiCallOrderDetails(orderId)

        }
        setupSnackbar()
        observeShowProgress()
        setUpRecyclerView()
        navigate()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack(R.id.transactionFragment, false)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    private fun setUpRecyclerView() {
        orderDetailsItemAdapter = OrderDetailsItemListAdapter()
        binding.rvOrderItems.adapter = orderDetailsItemAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            findNavController().popBackStack(R.id.transactionFragment, false)
        }

        binding.txtHome.setOnClickListener {
            findNavController().popBackStack(R.id.dashboardCategoryNew, false)
        }

        binding.tvIssueRefund.setOnClickListener {
            val bundle = Bundle().apply {
                paymentDetailsResponse.data.order.order_items.forEach {
                    it.isChecked = false
                }
                putInt("paymentId",paymentId)
                putParcelable("orderDetailsResponse", paymentDetailsResponse)
            }
            findNavController().navigate(
                R.id.action_transactionDetailsFragment_to_issueRefundFragment,
                bundle
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun navigate() {
        ProgressUtils.showProgressDialog(requireActivity())
        viewModel.dataPayment.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                paymentDetailsResponse = it
                binding.tvDate.text =
                    convertCurrentDate(it.data.order.created_at) + " " + convertCurrentTime(
                        it.data.order.created_at
                    )

                binding.tvTransactionDate.text =
                    convertCurrentTime(it.data.order.created_at) + "\n" + convertCurrentDate(
                        it.data.order.created_at
                    )

                if (it.data.order.customer != null) {
                    binding.tvCustomerName.text =
                        it.data.order.customer.firstName + " " + it.data.order.customer.lastName
                } else {
                    binding.tvCustomerName.text = ""
                }
                binding.orderDetails = it
                orderDetailsItemAdapter.addOrderDetailsItems(it.data.order.order_items)


                if (!paymentDetailsResponse.data.order.total_discount.equals(0.0)) {
                    binding.llDiscount.visibility = View.VISIBLE
                }

                if (!paymentDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                    binding.llRefundAmount.visibility = View.VISIBLE
                }

                /*if (orderDetailsResponse.data.totalAmount == orderDetailsResponse.data.refundDetails.refundedAmount) {
                    binding.tvIssueRefund.visibility = View.GONE
                }*/

                if (!paymentDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0)) {
                    binding.tvIssueRefund.visibility = View.GONE
                }

                if (paymentDetailsResponse.data.order.order_type.equals(
                        "Open Order",
                        ignoreCase = true
                    ) &&
                    paymentDetailsResponse.data.order.payment_status.equals(
                        "unpaid",
                        ignoreCase = true
                    )
                ) {
                    binding.tvIssueRefund.visibility = View.GONE
                }

                ProgressUtils.dismissProgressDialog()
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

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}