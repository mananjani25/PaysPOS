package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.android.pos.databinding.FragmentPaymentBoldPosBinding

import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentBoldPosFragment : Fragment() {
    private lateinit var binding: FragmentPaymentBoldPosBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentBoldPosBinding.inflate(inflater, container, false)
        binding.layoutHeaderCheckout.rlRoot.visibility = View.VISIBLE
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCartFragment(CartFragment())
        loadCategoryFragment(CheckoutDetailsFragmentNew())
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            findNavController().popBackStack()
        }

    }
    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putBoolean("isFromPayment",true)
           // putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
        }
        frag.arguments = result
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
       /* val bundle = Bundle().apply {
            fragmentId?.let { putInt("fragmentId", it) }
        }*/
       // fragment.arguments = bundle
        fm.beginTransaction().replace(binding.frameLayout.id,fragment).commit()
       // binding.frameLayout?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }

}