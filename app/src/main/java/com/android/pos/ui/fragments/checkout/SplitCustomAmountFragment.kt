package com.android.pos.ui.fragments.checkout

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
import com.android.pos.databinding.FragmentSplitCustomAmountBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CartFragment
import com.android.pos.ui.fragments.dashboard.bolddashboard.CategoryFragment
import com.android.pos.utils.callback.ItemListner
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplitCustomAmountFragment() : Fragment(), ItemListner {
    private lateinit var binding: FragmentSplitCustomAmountBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSplitCustomAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()


    }




    override fun onItemSelected(item: TbItem) {

    }

    override fun onCancelItemSelected() {

    }

    private fun onClick() {
        binding.tvFullAmount.setOnClickListener {
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvFullAmount.setTextColor(resources.getColor(R.color.white))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }

        binding.tv2ways.setOnClickListener {
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv2ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tv3ways.setOnClickListener {
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv3ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tv4ways.setOnClickListener {
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv4ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tv5ways.setOnClickListener {
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv5ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tv6ways.setOnClickListener {
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv6ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCustom.setOnClickListener {
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustom.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
        }
    }


}