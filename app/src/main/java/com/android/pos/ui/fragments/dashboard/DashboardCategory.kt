package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.model.responseModel.category.Category
import com.android.pos.data.model.responseModel.item.Item
import com.android.pos.databinding.FragmentDashboardCategoryBinding
import com.android.pos.ui.activities.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategory : Fragment() {

    private lateinit var binding: FragmentDashboardCategoryBinding
    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList: MutableList<Category> = arrayListOf()
    private var itemList: ArrayList<Item> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var isFlag = false
    override fun onPause() {
        super.onPause()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dashboard_category,
            container,
            false
        )

        /* binding.layoutMenu.imgOptionMenu.setOnClickListener {

             if (isFlag) {
                 isFlag = false
                 binding.tabLayout.rotation = 0F
             } else {
                 isFlag = true
                 binding.tabLayout.rotation = 270F

             }
         }*/

        binding.lifecycleOwner = this


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutMenu.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
    }


}