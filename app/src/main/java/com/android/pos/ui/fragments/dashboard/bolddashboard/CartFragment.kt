package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CartFragment() : Fragment() {
    private lateinit var binding: FragmentCartBinding
    var fragmentId: Int? = null
    var checkoutHeaderId: Int = 0
    var dashboardHeaderId: Int = 0
    private lateinit var cartAdapter: CartAdapter

    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "CartFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        Log.e("bundleData", arguments.toString())

        fragmentId = arguments?.getInt("fragmentId")
        checkoutHeaderId = arguments?.getInt("checkoutHeaderId")!!
        dashboardHeaderId = arguments?.getInt("dashboardHeaderId")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        setCartAdapter()
        addObserver()

    }

    private fun addObserver() {

        viewModel.mAllWords(
            TAKEOUT, prefProvider.getValueInt(com.android.pos.data.remote.Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity(), {
            if (it.isNotEmpty()) {
                Log.e(TAG, "listSize  ${Gson().toJson(it)}")
                it[it.size - 1].items?.toCollection(arrayListOf())
                    ?.let { it1 -> cartAdapter.setList(it1) }
                viewModel.itemCalculation(
                    it,
                    binding.txtTotal,
                    requireContext()
                )
                viewModel.setCartModel(it)

                binding.txtSubTotal.text = MethodUtils.roundOffAmount(viewModel.subTotalPrice)
                binding.txtTax.text = MethodUtils.roundOffAmount(viewModel.totalTax)
                binding.txtServiceCharge.text =
                    MethodUtils.roundOffAmount(viewModel.totalServiceCharge)
                binding.tvPayNow.text = "Pay " + MethodUtils.roundOffAmount(viewModel.totalPrice)

            }
        })
    }

    fun initListeners() {
        binding.tvPayNow.setOnClickListener {
            val checkoutHeader: RelativeLayout =
                activity?.findViewById(checkoutHeaderId) as RelativeLayout
            checkoutHeader.visibility = View.VISIBLE

            val dashboardHeader: RelativeLayout =
                activity?.findViewById(dashboardHeaderId) as RelativeLayout
            dashboardHeader.visibility = View.GONE
            loadCategoryFragment(CheckoutDetailsFragmentNew())
        }
    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val bundle = Bundle().apply {
            fragmentId?.let { putInt("fragmentId", it) }
        }
        fragment.arguments = bundle
        fragmentId?.let { fm.beginTransaction().replace(it, fragment).commit() }
    }

    /*   fun loadCategoryFragment(fragment: Fragment) {
          val fm: FragmentManager = requireActivity().supportFragmentManager
          val bundle=Bundle().apply {
              fragmentId?.let { putInt("fragmentId", it) }
          }
          fragment.arguments=bundle
          fragmentId?.let { fm.beginTransaction().replace(it, fragment).commit() }
                  var tbItems: ArrayList<TbItem> = arrayListOf()
                  it[0].items?.toCollection(arrayListOf())?.let { it1 -> tbItems.addAll(it1) }
                  cartAdapter.setList(tbItems)
              }

          }*/


    private fun setCartAdapter() {
        cartAdapter = CartAdapter()
        binding.rvCartList.adapter = cartAdapter
    }

}