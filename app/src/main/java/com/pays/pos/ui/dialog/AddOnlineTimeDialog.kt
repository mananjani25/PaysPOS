package com.pays.pos.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.AddOnlineTimeDiialogBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.allorders.AllOrdersViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class AddOnlineTimeDialog : DialogFragment() {


    @Inject
    lateinit var prefProvider: PrefProvider
    private var isFromDetails = false
    private var isFromTransaction = false
    lateinit var binding: AddOnlineTimeDiialogBinding
    private val ordersViewModel by activityViewModels<AllOrdersViewModel>()
    var doneOnce = false
    var finalstring = ""
    var timeFilter: InputFilter? = null
    var order_id: Int? = null
    var isSelected = false
    var listTextView: ArrayList<AppCompatTextView> = arrayListOf()

    companion object {
        fun newInstance() = AddOnlineTimeDialog()
    }

    var string_final: StringBuffer = StringBuffer()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            if (arguments?.getInt("order_id") != null) order_id =
                requireArguments().getInt("order_id")
        }
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 300, 150, 300)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.txtTitle?.text = getString(R.string.add_time_online)
        setupData()
        setKeyPad()
        listTextView.add(binding.txt15)
        listTextView.add(binding.txt30)
        listTextView.add(binding.txt45)
        listTextView.add(binding.txt60)
        listTextView.add(binding.customMinutes)
        setUpClickForMinutes()

    }


    private fun setUpClickForMinutes() {
        binding.txt15.setOnClickListener {
            val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
            val inset = InsetDrawable(back, 150, 300, 150, 300)
            dialog?.window?.setBackgroundDrawable(inset);
            finalstring = "15"
            isSelected = true
            binding.linearCustom.gone()
            listTextView = arrayListOf()
            listTextView.add(binding.txt30)
            listTextView.add(binding.txt45)
            listTextView.add(binding.txt60)
            listTextView.add(binding.customMinutes)
            setBackGroundAndTextColor(listTextView, binding.txt15)

        }
        binding.txt30.setOnClickListener {
            val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
            val inset = InsetDrawable(back, 150, 300, 150, 300)
            dialog?.window?.setBackgroundDrawable(inset);
            finalstring = "30"
            isSelected = true
            binding.linearCustom.gone()
            listTextView = arrayListOf()
            listTextView.add(binding.txt15)
            listTextView.add(binding.txt45)
            listTextView.add(binding.txt60)
            listTextView.add(binding.customMinutes)
            setBackGroundAndTextColor(listTextView, binding.txt30)

        }
        binding.txt45.setOnClickListener {
            val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
            val inset = InsetDrawable(back, 150, 300, 150, 300)
            dialog?.window?.setBackgroundDrawable(inset);
            finalstring = "45"
            isSelected = true
            binding.linearCustom.gone()
            listTextView = arrayListOf()
            listTextView.add(binding.txt30)
            listTextView.add(binding.txt15)
            listTextView.add(binding.txt60)
            listTextView.add(binding.customMinutes)
            setBackGroundAndTextColor(listTextView, binding.txt45)
        }
        binding.txt60.setOnClickListener {
            val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
            val inset = InsetDrawable(back, 150, 300, 150, 300)
            dialog?.window?.setBackgroundDrawable(inset);
            binding.linearCustom.gone()
            finalstring = "60"
            isSelected = true
            listTextView = arrayListOf()
            listTextView.add(binding.txt30)
            listTextView.add(binding.txt45)
            listTextView.add(binding.txt15)
            listTextView.add(binding.customMinutes)
            setBackGroundAndTextColor(listTextView, binding.txt60)

        }
        binding.customMinutes.setOnClickListener {

            val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
            val inset = InsetDrawable(back, 150, 110, 150, 110)
            dialog?.window?.setBackgroundDrawable(inset);
            finalstring = ""
            isSelected = false
            binding.linearCustom.visible()
            listTextView = arrayListOf()
            listTextView.add(binding.txt30)
            listTextView.add(binding.txt45)
            listTextView.add(binding.txt60)
            listTextView.add(binding.txt15)
            setBackGroundAndTextColor(listTextView, binding.customMinutes)

        }
    }


    @SuppressLint("ResourceType")
    fun setBackGroundAndTextColor(
        unselectedList: ArrayList<AppCompatTextView>,
        selectedTextView: AppCompatTextView
    ) {
        for (i in unselectedList.indices) {
            unselectedList[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.txtColor))
            unselectedList[i].background =
                ContextCompat.getDrawable(requireContext(), R.drawable.border_with_field)
        }
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.txtColor))
        selectedTextView.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.btnColor
            )
        )

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AddOnlineTimeDiialogBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        return binding.root
    }

    private fun setKeyPad() {
        binding.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.tvTwo.setOnClickListener {
            calculateValue("2", false)

        }

        binding.tvThree.setOnClickListener {
            calculateValue("3", false)

        }

        binding.tvFour.setOnClickListener {

            calculateValue("4", false)
        }

        binding.tvFive.setOnClickListener {
            calculateValue("5", false)
        }

        binding.tvSix.setOnClickListener {
            calculateValue("6", false)
        }

        binding.tvSeven.setOnClickListener {
            calculateValue("7", false)
        }

        binding.tvEight.setOnClickListener {
            calculateValue("8", false)
        }

        binding.tvNine.setOnClickListener {
            calculateValue("9", false)
        }

        binding.tvZero.setOnClickListener {
            calculateValue("0", false)
        }

        binding.tvClear.setOnClickListener {
            calculateValue("", true)
        }


    }


    private fun setupData() {

        binding.txtClear.setOnClickListener {
            binding.edtAmount.text?.clear()
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {
            if (!isSelected) {
                finalstring = binding.edtAmount.text.toString()
            }
            if (finalstring.isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), "Please select time or enter custom time.")
            } else {
                val result = Bundle().apply {
                    putInt("time", finalstring.toInt())
                    order_id?.let { it1 -> putInt("order_id", it1) }
                }
                requireActivity().supportFragmentManager.setFragmentResult(
                    "request_key_time",
                    result
                )

                ordersViewModel.removedPosition.apply {
                    value = this.value?.let {
                            it1 -> Pair(it1.first,true) }
                }

                findNavController().navigateUp()
            }
        }
    }


    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)


    }


    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount?.text?.length!! > 0 && delete) {
            binding.edtAmount?.setText(removeLastCharacter(binding?.edtAmount!!.text.toString()))
        } else {
            binding.edtAmount.append(number)
            if (binding.edtAmount.text!!.length == 3) {
                var value = binding.edtAmount.text.toString().toInt()
                if (value > 120) {
                    binding.edtAmount.text!!.clear()
                    binding.edtAmount.append("120")
                }
            }
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }


}