package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew

class CartFragment() : Fragment() {
    private lateinit var binding: FragmentCartBinding
    var fragmentId:Int?=null
    var checkoutHeaderId:Int=0
    var dashboardHeaderId:Int=0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        Log.e("bundleData",arguments.toString())

        fragmentId=arguments?.getInt("fragmentId")
        checkoutHeaderId=arguments?.getInt("checkoutHeaderId")!!
        dashboardHeaderId= arguments?.getInt("dashboardHeaderId")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()


    }

    private fun initListeners() {
        binding.tvPayNow.setOnClickListener {
            val checkoutHeader: RelativeLayout = activity?.findViewById(checkoutHeaderId) as RelativeLayout
            checkoutHeader.visibility=View.VISIBLE

            val dashboardHeader: RelativeLayout = activity?.findViewById(dashboardHeaderId) as RelativeLayout
            dashboardHeader.visibility=View.GONE
            loadCategoryFragment(CheckoutDetailsFragmentNew())
        }
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val bundle=Bundle().apply {
            fragmentId?.let { putInt("fragmentId", it) }
        }
        fragment.arguments=bundle
        fragmentId?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }

}