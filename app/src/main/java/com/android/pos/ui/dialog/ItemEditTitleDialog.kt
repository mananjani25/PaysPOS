package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.pos.R
import com.android.pos.data.model.OptionListModel
import com.android.pos.databinding.DialogEditItemTitleBinding
import com.android.pos.ui.adapter.ChooseColorsAdapter
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ItemEditTitleDialog : DialogFragment(), View.OnClickListener {
    private lateinit var adapter: ChooseColorsAdapter
    private lateinit var binding: DialogEditItemTitleBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_edit_item_title, container, false)
        binding.lifecycleOwner = this

        setAdapter()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    private fun setAdapter() {
        val list: ArrayList<OptionListModel> = arrayListOf()
        list.add(OptionListModel(R.color.colorF2, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFA, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.color72, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFF, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.color23, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorAA, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorF7, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFD, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorD8, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorF9, "Drink Size", "3 Options"))
        binding.rvColors.layoutManager = GridLayoutManager(requireContext(), 5)
        adapter = ChooseColorsAdapter(list)
        binding.rvColors.adapter = adapter
        binding.imgBack.setOnClickListener(this)
        binding.txtSave.setOnClickListener(this)


    }

    override fun onClick(v: View?) {


        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtSave -> {

            }

        }
    }


}