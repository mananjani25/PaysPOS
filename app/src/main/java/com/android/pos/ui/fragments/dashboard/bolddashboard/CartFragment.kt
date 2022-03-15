package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.extensions.alert
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
    private var isFromPayment = false

    companion object {
        fun newInstacne(isFromPayment: Boolean): CartFragment {
            val bundle = bundleOf("isFromPayment" to isFromPayment)
            val frag = CartFragment()
            frag.arguments = bundle
            return frag

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        Log.e("bundleData", arguments.toString())

        if (arguments?.getInt("fragmentId") != null)
            fragmentId = arguments?.getInt("fragmentId")
        if (arguments?.getInt("checkoutHeaderId") != null)
            checkoutHeaderId = arguments?.getInt("checkoutHeaderId")!!

        if (arguments?.getInt("dashboardHeaderId") != null)
            dashboardHeaderId = arguments?.getInt("dashboardHeaderId")!!

        isFromPayment = arguments?.getBoolean("isFromPayment") ?: false
        setUpData()
        return binding.root
    }

    private fun setUpData() {
        if (isFromPayment) {
            binding.linearOrderPlace.visibility = View.GONE
            binding.imgOrderMenu.visibility = View.GONE
        }
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
        ).observe(requireActivity()) {
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

            } else {
                cartAdapter.clearList()
            }
        }
    }

    fun initListeners() {
        binding.tvPayNow.setOnClickListener {
            if (cartAdapter.cartList.isNotEmpty()) {

                findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_paymentBoldPosFragment)
                /* val checkoutHeader: RelativeLayout =
                     activity?.findViewById(checkoutHeaderId) as RelativeLayout
                 checkoutHeader.visibility = View.VISIBLE

                 val dashboardHeader: RelativeLayout =
                     activity?.findViewById(dashboardHeaderId) as RelativeLayout
                 dashboardHeader.visibility = View.GONE
                 loadCategoryFragment(CheckoutDetailsFragmentNew())*/
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    resources.getString(R.string.please_add_Atleast_one_item_in_cart)
                ) { _, _ ->
                }
            }

        }

        binding.imgOrderMenu.setOnClickListener {

            hideOrderMenu()

        }

        binding.llClearCart.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.delete_items_message)
            ) {
                positiveButton(getString(R.string.tv_delete)) {
                    // Do positive stuff here
                    prefProvider.setValueInt(Constants.DINE_INGUEST_SELECTED, 0)



                    viewModel.deleteCart()

                    if (prefProvider.getValue(Constants.ORDER_TYPE, "").toString() != "") {
                        prefProvider.setValue(Constants.ORDER_TYPE, "")
                    }

                    hideOrderMenu()
                    prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)


                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }
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

    private fun hideOrderMenu() {
        if (binding.llClearCart.visibility == View.VISIBLE) {
            binding.llClearCart.visibility = View.GONE
        } else {
            binding.llClearCart.visibility = View.VISIBLE
        }
    }

}