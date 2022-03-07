package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentAddItemBinding
import com.android.pos.utils.callback.ItemListner

class AddItemFragment(val listner: ItemListner) : Fragment() {
    private var item: TbItem? = null
    private lateinit var binding: FragmentAddItemBinding

    companion object {
        fun newInstance(item: TbItem,callback: ItemListner): AddItemFragment {
            val bundle: Bundle = Bundle()
            bundle.putParcelable("item", item)
            val frag = AddItemFragment(callback)
            frag.arguments = bundle
            return frag

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddItemBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        onClick()

    }

    private fun onClick() {
        binding.imgMinus.setOnClickListener {

        }
        binding.imgPlus.setOnClickListener {

        }
        binding.txtCancel.setOnClickListener {

        }

    }

    private fun getData() {
        item = requireArguments().getParcelable<TbItem>("item")
        setData()
    }

    private fun setData() {
        binding.txtItem.setText("" + item?.name)
    }


}