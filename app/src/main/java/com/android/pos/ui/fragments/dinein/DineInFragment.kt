package com.android.pos.ui.fragments.dinein

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.android.pos.R
import com.android.pos.databinding.FragmentDineInBinding
import android.widget.FrameLayout
import android.widget.ImageView


class DineInFragment : Fragment() {

    private lateinit var binding: FragmentDineInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in,
            container,
            false
        )

        binding.lifecycleOwner = this
        //   binding.viewModel = viewModel

        val img = ImageView(requireActivity())
        img.setBackgroundColor(Color.RED)


        val params = FrameLayout.LayoutParams(20, 20)
        params.leftMargin = 1000
        params.topMargin = 500
        binding.flFloorPlan.addView(img, params)

        return binding.root
    }

}