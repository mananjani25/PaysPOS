package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddDiscountDialog : DialogFragment() {

    private lateinit var binding: DailogAddDiscountBinding
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private val TAG = "AddDiscountDialog"

    companion object {
        fun newInstance() = AddDiscountDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setupData()


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e(TAG,"getDiscountList  ${Gson().toJson(viewModel.getDiscountList.value)}")
    }

    private fun setupData() {

//        val note = requireArguments().getString("note")
//
//        with(binding) {
//            edtAmount.setText(note)
//        }

        binding.llKeypad.tvOne.setOnClickListener {

//            val result = Bundle().apply {
//                putString("note", binding.edtNote.text.toString().trim())
//            }
            // setFragmentResult("request_key_note", result)
//            findNavController().navigateUp()
        }

        binding.imgBack.setOnClickListener {
            dismiss()
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
}