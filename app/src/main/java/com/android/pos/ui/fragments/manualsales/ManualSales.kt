package com.android.pos.ui.fragments.manualsales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentManualSalesBinding
import com.android.pos.ui.adapter.ManualSaleCartAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AlertUtils.showAlert
import com.android.pos.utils.extensions.alert

class ManualSales : Fragment() {
    private lateinit var binding: FragmentManualSalesBinding
    lateinit var adapter: ManualSaleCartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSalesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        onClickCalculation()
        setAdapter()
    }

    private fun setAdapter() {
        adapter = ManualSaleCartAdapter()
        binding.rvCart.adapter = adapter

    }

    private fun onClickCalculation() {
        binding.includeView.tvOne.setOnClickListener {
            calculateValue("1", false)


        }
        binding.includeView.tvTwo.setOnClickListener {
            calculateValue("2", false)

        }

        binding.includeView.tvThree.setOnClickListener {

            calculateValue("3", false)
        }
        binding.includeView.tvFour.setOnClickListener {

            calculateValue("4", false)
        }
        binding.includeView.tvFive.setOnClickListener {

            calculateValue("5", false)
        }

        binding.includeView.tvSix.setOnClickListener {

            calculateValue("6", false)
        }
        binding.includeView.tvSeven.setOnClickListener {

            calculateValue("7", false)
        }
        binding.includeView.tvEight.setOnClickListener {

            calculateValue("8", false)
        }
        binding.includeView.tvNine.setOnClickListener {

            calculateValue("9", false)
        }
        binding.includeView.tvZero.setOnClickListener {

            calculateValue("0", false)
        }
        binding.includeView.tvBack.setOnClickListener {
            calculateValue("", true)
        }

        binding.includeView.tvDot.setOnClickListener {
            calculateValue(".", false)
        }

    }

    private fun onClick() {
        binding.imgCacncel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtAddToCart.setOnClickListener {
            if (binding.edtInventoryName.text.trim().isEmpty()) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    getString(R.string.modifier_empty_validation)
                )
                { _, _ ->


                }
            }
        }
        binding.txtHome.setOnClickListener {
            findNavController().popBackStack()
        }
    }


    private fun calculateValue(number: String, delete: Boolean) {
        if (delete) {
            binding.txtAmount.text = removeLastCharacter(binding.txtAmount.text.toString())

        } else {
            binding.txtAmount.append(number)
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

}