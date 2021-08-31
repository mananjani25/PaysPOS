package com.android.pos.ui.fragments.settings.hardware

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.EditPrinterModel
import com.android.pos.databinding.FragmentEditPrinterBinding
import com.android.pos.ui.adapter.EditPrinterListAdapter

class EditPrinter : Fragment() {
    lateinit var binding: FragmentEditPrinterBinding
    private lateinit var adapter:EditPrinterListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditPrinterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EditPrinterListAdapter()
        onClick()
        setAdapter()
    }

    private fun setAdapter() {
        val list:ArrayList<EditPrinterModel> = arrayListOf()
        list.add(
            EditPrinterModel(
            printerName = "Printer Name 1"
        ))
        list.add(
            EditPrinterModel(
            printerName = "Printer Name 2"
        ))
        list.add(
            EditPrinterModel(
            printerName = "Printer Name 3"
        ))

        binding.rvPrinterList.adapter = adapter
        adapter.setList(list)
    }

    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}