package com.android.pos.ui.fragments.manualsales

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.databinding.FragmentManualSaleNewBinding

class ManualSaleNew : Fragment() {
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSaleNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutMenu.imgSearch.visibility = View.GONE
        binding.layoutMenu.autoSearch.visibility = View.GONE
        onConfig()
        onClickKeypad()
    }

    private fun onClickKeypad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.tvTwo.setOnClickListener {

            calculateValue("2", false)
        }
        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)
        }
        binding.llKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }
        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }
        binding.llKeypad.tvSix.setOnClickListener {

            calculateValue("6", false)
        }
        binding.llKeypad.tvSeven.setOnClickListener {

            calculateValue("7", false)
        }
        binding.llKeypad.tvEight.setOnClickListener {

            calculateValue("8", false)
        }
        binding.llKeypad.tvNine.setOnClickListener {

            calculateValue("9", false)
        }
        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)

        }
        binding.llKeypad.imgAdd.setOnClickListener {


        }
        binding.llKeypad.tvBack.setOnClickListener {
            if (binding.txtAmount.text.toString().isNotEmpty()){
            calculateValue("", true)
            }

        }
    }

    private fun onConfig() {
        binding.layoutMenu.txtProducts.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutMenu.txtKeypad.setTextColor(resources.getColor(R.color.txt_color_blue))

    }

    private fun calculateValue(number: String, delete: Boolean) {

        if (delete) {
            binding.txtAmount.text = removeLastCharacter(binding.txtAmount.text.toString())
        } else if (binding.txtAmount.text.trim().equals("0.00")) {
            binding.txtAmount.text = ""
            binding.txtAmount.append(number)
        } else {
            binding.txtAmount.append(number)

        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun getFirstValue(str: String): String {
        return str.toString().substring(0, str.indexOf('.'))


    }


}