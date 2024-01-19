package com.pays.pos.ui.fragments.posmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.MenuPosBinding

class MenuPOS : Fragment() {
    private lateinit var binding: MenuPosBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.menu_pos,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureFooter()
        onClick()
    }

    private fun configureFooter() {
        binding.footer.imgCalculator.setColorFilter(resources.getColor(R.color.txtColor))
        binding.footer.txtCheckOut.setTextColor(resources.getColor(R.color.txtColor))
        binding.footer.imgMore.setColorFilter(resources.getColor(R.color.txt_color_blue))
        binding.footer.txtMore.setTextColor(resources.getColor(R.color.txt_color_blue))

    }

    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()

        }
    }


}