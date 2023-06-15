package com.android.pos.ui.fragments.eGiftCard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentBalanceInquiryBinding
import com.android.pos.utils.extensions.gone

class BalanceInquiryFragment : Fragment() {

    private lateinit var binding: FragmentBalanceInquiryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBalanceInquiryBinding.inflate(layoutInflater)
        hideDefaultKeypads()
        onClick()
        return binding.root
    }

    private fun hideDefaultKeypads() {
        binding.apply {
            llKeypad.apply {
                txt10.gone()
                txt20.gone()
                txt30.gone()
                tvClear.gone()
            }
        }
    }

    private fun onClick() {

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtNext.setOnClickListener {

        }
    }

    }