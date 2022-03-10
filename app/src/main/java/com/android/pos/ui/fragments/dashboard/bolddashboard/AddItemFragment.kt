package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.databinding.FragmentAddItemBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddItemFragment(val listner: ItemListner) : Fragment() {
    private var item: TbItem? = null
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentAddItemBinding
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddItemFragment"
    private var qty = 1

    companion object {
        fun newInstance(item: TbItem, callback: ItemListner): AddItemFragment {
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
        getServiceCharges()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        onClick()

    }

    private fun onClick() {
        binding.imgMinus.setOnClickListener {
            if (qty == 1) {
                qty = 1
            } else {
                qty -= 1
            }

            binding.txtQuantity.setText("" + qty)

        }
        binding.imgPlus.setOnClickListener {
            qty += 1
            binding.txtQuantity.setText("" + qty)
        }

        binding.txtCancel.setOnClickListener {
            listner.onCancelItemSelected()
        }

        binding.txtDone.setOnClickListener {
            item?.itemQuantity = qty


        }

    }

    private fun getData() {
        item = requireArguments().getParcelable<TbItem>("item")
        setData()
    }

    private fun setData() {
        binding.txtItem.setText("" + item?.name)
    }


    private fun getServiceCharges() {

        serviceChargesObserve = Observer {

            if (it.status == Status.SUCCESS) {
                serviceChargesList = it.data
                Log.e(TAG, "serviceChargesList:  ${Gson().toJson(serviceChargesList)}")

            }

        }

        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }
}