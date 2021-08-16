package com.android.pos.ui.fragments.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.databinding.PaymentFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class PaymentFragment : Fragment(), View.OnClickListener {

    private var cartList: CartModel? = null
    private var splitValue: Int = -1
    private var totalPrice: Double = 0.0
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private lateinit var binding: PaymentFragmentBinding

    @Inject
    lateinit var prefProvider: PrefProvider


    companion object {
        fun newInstance() = PaymentFragment()
    }

    private val viewModel by viewModels<PaymentViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = viewModel

        cartList = requireArguments().getParcelable("cartList")

        setupData()
        callbackSetup()
        observeShowProgress()
        observeData()


        return binding.root
    }


    @SuppressLint("SetTextI18n")
    private fun callbackSetup() {

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            val splitAfterAmount = totalPrice / splitValue

            MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
            val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

            binding.txtSplitAmount.text = getString(R.string.edit_split_amount)

            binding.txtSplitValue.text =
                "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

        }


    }

    private fun setupData() {

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")

        MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        if (totalDiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totalDiscount
            )
        }


        binding.txtSplitAmount.setOnClickListener(this)
        binding.txtCustom.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llCash.setOnClickListener(this)


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                findNavController().popBackStack()
            }

            R.id.llCash -> {

                val myRequest = cartList?.let {
                    viewModel.createOrderRequest(
                        it,
                        subTotalPrice,
                        totalPrice,
                        totalServiceCharge,
                        totalTax
                    )
                }
                if (myRequest != null) {
                    viewModel.submit(myRequest)
                }


            }
            R.id.txtCustom -> {
                findNavController().navigate(R.id.action_paymentFragment_to_customAmountFragment)
            }
            R.id.txtSplitAmount -> {

                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice)
                bundle.putInt("splitValue", splitValue)
                findNavController().navigate(
                    R.id.action_paymentFragment_to_splitAmountFragment,
                    bundle
                )
            }
        }

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


    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                viewModel.deleteCart()

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(), it.message
                ) { _, _ ->
                    findNavController().popBackStack()
                }

            }
        })

    }
}