package com.android.pos.ui.fragments.manualsales.ManualSaleBoldPOS

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.databinding.KeyPadManualSaleFragmentBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class KeyPadManualSaleFragment : Fragment() {

    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private lateinit var binding: KeyPadManualSaleFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = KeyPadManualSaleFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        binding.txtAmount.addTextChangedListener(AmountTextWatcher(binding.txtAmount, true))
        setKeypad()
    }

    private fun setKeypad() {
        binding.manualKeypad.first.setOnClickListener {
            calculateValue("1", false)
        }
        binding.manualKeypad.second.setOnClickListener {
            calculateValue("2", false)
        }
        binding.manualKeypad.third.setOnClickListener {
            calculateValue("3", false)
        }
        binding.manualKeypad.fourth.setOnClickListener {
            calculateValue("4", false)
        }
        binding.manualKeypad.five.setOnClickListener {
            calculateValue("6", false)
        }
        binding.manualKeypad.six.setOnClickListener {
            calculateValue("6", false)
        }
        binding.manualKeypad.seven.setOnClickListener {
            calculateValue("7", false)
        }
        binding.manualKeypad.eight.setOnClickListener {
            calculateValue("8", false)
        }
        binding.manualKeypad.nine.setOnClickListener {
            calculateValue("9", false)
        }
        binding.manualKeypad.zero.setOnClickListener {
            calculateValue("0", false)
        }
        binding.manualKeypad.clear.setOnClickListener {
            calculateValue("", true)
        }
        binding.manualKeypad.txtAdd.setOnClickListener {

        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        val inputMethodManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
        if (delete && binding.txtAmount.text?.length!! > 1) {

            binding.txtAmount.setText(removeLastCharacter(binding.txtAmount.text.toString()))
        } else if (binding.txtAmount.text?.trim()!!.equals("0.00")) {
            binding.txtAmount.setText("")
            binding.txtAmount.append(number)
        } else {
            binding.txtAmount.append(number)
        }

        val str = binding.txtAmount.text.toString().replace("""[$]""".toRegex(), "")
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

}