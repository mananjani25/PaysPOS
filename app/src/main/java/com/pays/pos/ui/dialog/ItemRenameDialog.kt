package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.pays.pos.databinding.DailogAddNoteBinding
import com.pays.pos.databinding.DialogRenameItemBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemRenameDialog : DialogFragment() {

    private lateinit var binding: DialogRenameItemBinding

    companion object {
        fun newInstance() = ItemRenameDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogRenameItemBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpData()
        return binding.root
    }

    private fun setUpData() {
        val data = requireArguments().getString("item_name")
        binding.apply {
            edtItemName.setText(data)

        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {
            val result = Bundle().apply {
                putString("item_name", binding.edtItemName.text.toString().trim().replace("\\s+".toRegex(), " "))
            }
            setFragmentResult("request_key_item_rename", result)
            findNavController().navigateUp()
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