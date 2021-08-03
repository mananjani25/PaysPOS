package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.databinding.DailogAddNoteBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddNoteDialog : DialogFragment() {

    private lateinit var binding: DailogAddNoteBinding

    companion object {
        fun newInstance() = AddNoteDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddNoteBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setupData()

        return binding.root
    }

    private fun setupData() {

        val note = requireArguments().getString("note")

        with(binding) {
            edtNote.setText(note)
        }

        binding.txtSave.setOnClickListener {

            val result = Bundle().apply {
                putString("note", binding.edtNote.text.toString().trim())
            }
            setFragmentResult("request_key_note", result)
            findNavController().navigateUp()
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