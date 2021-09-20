package com.android.pos.ui.fragments.dinein

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.android.pos.R
import com.android.pos.databinding.FragmentDineInBinding
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.viewModels
import com.android.pos.ui.adapter.DineInFloorNameListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.data.model.DineInFloorNameModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DineInFragment : Fragment() {

    private lateinit var binding: FragmentDineInBinding
    private lateinit var dineInFloorNameListAdapter: DineInFloorNameListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private var dineInFloorNameList = ArrayList<DineInFloorNameModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in,
            container,
            false
        )

        binding.lifecycleOwner = this
        //   binding.viewModel = viewModel

        setUpRecyclerView()

        /* val img = ImageView(requireActivity())
         img.setBackgroundColor(Color.RED)


         val params = FrameLayout.LayoutParams(20, 20)
         params.leftMargin = 1000
         params.topMargin = 500
         binding.flFloorPlan.addView(img, params)*/

        dineInFloorNameListAdapter.showFloorPlan = {

        }


        return binding.root
    }

    private fun setUpRecyclerView() {
        dineInFloorNameListAdapter = DineInFloorNameListAdapter(viewModel)
        binding.rvFloorName.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFloorName.adapter = dineInFloorNameListAdapter

        var floorTypeList = ArrayList<DineInFloorNameModel.FloorType>()

        var floorType = DineInFloorNameModel.FloorType()
        floorType.floorType = "Square"

        var floorType1 = DineInFloorNameModel.FloorType()
        floorType1.floorType = "Round"

        floorTypeList.add(floorType)
        floorTypeList.add(floorType1)

        dineInFloorNameList.add(DineInFloorNameModel("Main Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Party Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Family Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Bar and More", floorTypeList))

        dineInFloorNameListAdapter.addFloorName(dineInFloorNameList)

        dineInFloorNameList.get(0).floorTypeList?.forEach {
            if (it.floorType == "Square") {
                val inflatedViewSquare = layoutInflater.inflate(
                    R.layout.view_floor_square,
                    binding.flFloorPlan,
                    false
                )

                if (inflatedViewSquare != null) {
                    val llMainParentSquare: LinearLayout =
                        inflatedViewSquare.findViewById(R.id.llMainParentSquare)

                    if (llMainParentSquare.parent != null) {
                        (llMainParentSquare.parent as ViewGroup).removeView(llMainParentSquare)
                    }

                    val paramsSquare = FrameLayout.LayoutParams(150, 150)
                    paramsSquare.leftMargin = 100
                    paramsSquare.topMargin = 200
                    binding.flFloorPlan.addView(llMainParentSquare, paramsSquare)
                }
            } else if (it.floorType == "Round") {
                val inflatedViewRound = layoutInflater.inflate(
                    R.layout.view_floor_round,
                    binding.flFloorPlan,
                    false
                )

                if (inflatedViewRound != null) {
                    val llMainParentRound: LinearLayout =
                        inflatedViewRound.findViewById(R.id.llMainParentRound)

                    if (llMainParentRound.parent != null) {
                        (llMainParentRound.parent as ViewGroup).removeView(llMainParentRound)
                    }

                    val paramsRound = FrameLayout.LayoutParams(150, 150)
                    paramsRound.leftMargin = 500
                    paramsRound.topMargin = 300
                    binding.flFloorPlan.addView(llMainParentRound, paramsRound)
                }

            }
        }


        /*  val tvItemSquare: AppCompatTextView = inflatedViewSquare.findViewById(R.id.tvNoOFChairs)
          val tvItemRound: AppCompatTextView = inflatedViewRound.findViewById(R.id.tvNoOFChairs)*/


    }

}