package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentPayFullAmountBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.ui.fragments.dashboard.bolddashboard.CategoryFragment
import com.android.pos.utils.callback.ItemListner
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayFullAmountFragment(val bundle: Bundle?) : Fragment(), ItemListner {
    private lateinit var binding: FragmentPayFullAmountBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    var frameLayoutId=0
    var llRoot=0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentPayFullAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        frameLayoutId= bundle?.getInt("frameLayoutId")!!
        llRoot= bundle?.getInt("llRoot")!!

    }

    private fun onClick() {
        binding.llManualCardEntry.setOnClickListener {
            val viewPager: FrameLayout = activity?.findViewById(frameLayoutId) as FrameLayout
            viewPager.visibility=View.VISIBLE

            val llRoot: LinearLayout = activity?.findViewById(llRoot) as LinearLayout
            llRoot.visibility=View.GONE

            loadManualCardEntryFragment(ManualCardEntryFragment())
        }
    }

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(frameLayoutId, fragment).commit()
    }


    override fun onItemSelected(item: TbItem) {

    }

    override fun onCancelItemSelected() {

    }

}