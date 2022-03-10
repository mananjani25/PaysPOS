package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.FragmentCartBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.boldpos.CartAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CartFragment : Fragment() {
    private lateinit var binding: FragmentCartBinding
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCartAdapter()
        addObserver()

    }

    private fun addObserver() {

        viewModel.mAllWords(
            TAKEOUT, 0
        ).observe(requireActivity(), {
            if (it.isNotEmpty()) {
                Log.e(TAG,"listSize  ${it.size}")

                var tbItems: ArrayList<TbItem> = arrayListOf()
                it[0].items?.toCollection(arrayListOf())?.let { it1 -> tbItems.addAll(it1) }
                cartAdapter.setList(tbItems)
            }

        })

    }

    private fun setCartAdapter() {
        cartAdapter = CartAdapter()
    }
}