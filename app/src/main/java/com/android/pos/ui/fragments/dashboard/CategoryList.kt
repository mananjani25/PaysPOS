package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.databinding.FragmentCategoryItemListBinding
import com.android.pos.ui.adapter.CategoryItemAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable

@AndroidEntryPoint
class CategoryList : Fragment() {

    private lateinit var binding: FragmentCategoryItemListBinding


    companion object {
        private val ITEM_LIST = "item_list"
        fun newInstance(list: List<VenueDataResponse.Data.Category.Item>): CategoryList {
            val args: Bundle = Bundle()
            args.putSerializable(ITEM_LIST, list as Serializable)
            val fragment = CategoryList()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_category_item_list,
            container,
            false
        )
        binding.lifecycleOwner = this

        val list: ArrayList<VenueDataResponse.Data.Category.Item> =
            requireArguments().get(ITEM_LIST) as ArrayList<VenueDataResponse.Data.Category.Item>
        Log.e("CategoryList", "${list.size}")

        binding.recyclerViewItemsList.adapter = CategoryItemAdapter(
            requireContext(),
            list,
            object : CategoryItemAdapter.CategoryItemList {
                override fun onClick() {
                    findNavController().navigate(R.id.action_dashboardCategory_to_createItem)
                }

                override fun onClickedCreateItem() {

                }

            })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}