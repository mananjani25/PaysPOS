package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.manualsales.ManualSaleBoldPOS.KeyPadManualSaleFragment
import com.android.pos.utils.callback.ItemListner
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategoryBoldPOS : Fragment(), ItemListner {
    private lateinit var binding: FragmentDashboardCategoryBoldPosBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        loadCartFragment(CartFragment())
        loadCategoryFragment(CategoryFragment(this))


    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        val result = Bundle().apply {
            putInt("fragmentId", binding.frameLayout.id)
            putInt("checkoutHeaderId", binding.layoutHeaderCheckout.rlRoot.id)
            putInt("dashboardHeaderId", binding.layoutHeader.rlRoot.id)
        }
        frag.arguments = result
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun loadKeyPadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()
    }

    private fun onClick() {

        binding.layoutHeader.txtTransaction.setOnClickListener {
            viewModel.deleteCart()

        }
        binding.layoutHeader.txtDineIn.setOnClickListener {

        }
        binding.layoutHeader.imgDrawer.setOnClickListener {

        }

        binding.layoutHeader.txtTransaction.setOnClickListener {

        }
        binding.layoutHeader.ivLock.setOnClickListener {
            binding.layoutHeaderCheckout.rlRoot.visibility=View.VISIBLE
            binding.layoutHeader.rlRoot.visibility=View.GONE
            loadCategoryFragment(CheckoutDetailsFragmentNew())
        }
        binding.layoutHeaderCheckout.imgDrawer.setOnClickListener {
            binding.layoutHeaderCheckout.rlRoot.visibility=View.GONE
            binding.layoutHeader.rlRoot.visibility=View.VISIBLE
            loadCategoryFragment(CategoryFragment(this))
        }

        binding.layoutHeader.imgSync.setOnClickListener {
            viewModel.syncInventoryModule()
        }
        binding.layoutHeader.txtOpenOrder.setOnClickListener {
            loadCategoryFragment(CategoryFragment(this))
            binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.btnColor))
            binding.layoutHeader.txtOpenOrder.setTypeface(
                binding.layoutHeader.txtOpenOrder.typeface,
                Typeface.BOLD
            )
            binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.txtColor))
            binding.layoutHeader.txtKeypad.setTypeface(
                binding.layoutHeader.txtKeypad.typeface,
                Typeface.NORMAL
            )
        }
        binding.layoutHeader.txtKeypad.setOnClickListener {
            loadKeyPadFragment(KeyPadManualSaleFragment())
            binding.layoutHeader.txtKeypad.setTextColor(resources.getColor(R.color.btnColor))
            binding.layoutHeader.txtKeypad.setTypeface(
                binding.layoutHeader.txtKeypad.typeface,
                Typeface.BOLD
            )
            binding.layoutHeader.txtOpenOrder.setTextColor(resources.getColor(R.color.txtColor))
            binding.layoutHeader.txtOpenOrder.setTypeface(
                binding.layoutHeader.txtOpenOrder.typeface,
                Typeface.NORMAL
            )

        }


    }

    override fun onItemSelected(item: TbItem) {
        Log.e(TAG, "getitem:  ${Gson().toJson(item)}")
        val fragment = AddItemFragment.newInstance(item,this)
        loadCategoryFragment(fragment)

    }

    override fun onCancelItemSelected() {

    }

    private fun addItemsToCart(item: TbItem) {

    }

}