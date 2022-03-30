package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.data.remote.Constants.SPLIT_ENABLE
import com.android.pos.databinding.FragmentPaymentBoldPosBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.TAG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentBoldPosFragment : Fragment() {
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int = -1

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentPaymentBoldPosBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentBoldPosBinding.inflate(inflater, container, false)
        binding.layoutHeaderCheckout.rlRoot.visibility = View.VISIBLE
        binding.lifecycleOwner = this

        orderId = arguments?.getInt("orderId")

        Log.e("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        loadCartFragment(CartFragment(null))
        Handler(Looper.getMainLooper()).postDelayed({ /* Create an Intent that will start the Menu-Activity. */
            loadCategoryFragment(CheckoutDetailsFragmentNew())
        }, 100)

        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            Log.d(TAG, "onViewCreated: "+prefProvider.getValueboolean(SPLIT_ENABLE,false))
            if (prefProvider.getValueboolean(Constants.SPLIT_ENABLE, false)) {
                AlertUtils.showCustomAlert(requireContext(), "Please complete all payment.")
            } else {
                findNavController().popBackStack()
            }
        }

        listeners()
        setFragmentResultListener(
            "request_key_tips"
        ) { requestKey: String, bundle: Bundle ->
            Log.d(TAG, "onViewCreated: " + bundle)
        }
    }

    private fun listeners() {
        binding.layoutHeaderCheckout.tvAddTip.setOnClickListener {
            findNavController().navigate(R.id.action_paymentBoldPosFragment_to_addTipDialog)
        }
        binding.layoutHeaderCheckout.tvAddDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_paymentBoldPosFragment_to_addDiscountDialogFragment)
        }
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putBoolean("isFromPayment", true)
            putString(REDIRECT_FROM, arguments?.getString(REDIRECT_FROM))
            // putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
        }
        frag.arguments = result
        //frag.arguments = arguments

        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager

        Log.e("orderId :: ", orderId.toString())
        val bundle = Bundle().apply {
            orderId?.let { putInt("orderId", it) }
            putInt("paymentId", paymentId)
            putString("orderOfflineId", orderOfflineId)
            putString("paymentOfflineId", paymentOfflineId)
            putString(REDIRECT_FROM, arguments?.getString(REDIRECT_FROM))

        }
        fragment.arguments = bundle
       // fragment.arguments = arguments

        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
        // binding.frameLayout?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }


}