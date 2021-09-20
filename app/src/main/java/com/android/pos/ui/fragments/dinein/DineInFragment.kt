package com.android.pos.ui.fragments.dinein

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class DineInFragment : Fragment() {

    private lateinit var binding: FragmentDineInBinding
    private lateinit var dineInFloorNameListAdapter: DineInFloorNameListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private var dineInFloorNameList = ArrayList<DineInFloorNameModel>()
    private var dineInFloorTypeList = ArrayList<DineInFloorNameModel.FloorType>()

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

        binding.tvTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dineInFragment_to_transactionFragment)
        }

        binding.tvOrders.setOnClickListener {
            findNavController().navigate(R.id.action_dineInFragment_to_orders)
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
        floorType.noOFChairs = 4
        floorType.tableName = "001"

        var floorType1 = DineInFloorNameModel.FloorType()
        floorType1.floorType = "Round"
        floorType1.noOFChairs = 5
        floorType1.tableName = "002"

        floorTypeList.add(floorType)
        floorTypeList.add(floorType1)

        dineInFloorNameList.add(DineInFloorNameModel("Main Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Party Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Family Dining", floorTypeList))
        dineInFloorNameList.add(DineInFloorNameModel("Bar and More", floorTypeList))

        dineInFloorNameListAdapter.addFloorName(dineInFloorNameList)


        dineInFloorTypeList =
            dineInFloorNameList[0].floorTypeList as ArrayList<DineInFloorNameModel.FloorType>

        for (i in dineInFloorTypeList.indices) {
            if (dineInFloorTypeList[i].floorType == "Square") {
                val inflatedViewSquare = layoutInflater.inflate(
                    R.layout.view_floor_square,
                    binding.flFloorPlan,
                    false
                )

                if (inflatedViewSquare != null) {
                    val llMainParentSquare: LinearLayout =
                        inflatedViewSquare.findViewById(R.id.llMainParentSquare)

                    val tvNoOFChairs: AppCompatTextView =
                        inflatedViewSquare.findViewById(R.id.tvNoOFChairs)

                    tvNoOFChairs.text = "" + dineInFloorTypeList[i].noOFChairs

                    val tvTableName: AppCompatTextView =
                        inflatedViewSquare.findViewById(R.id.tvTableName)

                    tvTableName.text = "" + dineInFloorTypeList[i].tableName

                    if (llMainParentSquare.parent != null) {
                        (llMainParentSquare.parent as ViewGroup).removeView(llMainParentSquare)
                    }

                    /*pass object in settag*/
                    inflatedViewSquare.tag = i.toString()

                    val paramsSquare = FrameLayout.LayoutParams(110, 110)
                    paramsSquare.leftMargin = 100
                    paramsSquare.topMargin = 200
                    binding.flFloorPlan.addView(llMainParentSquare, paramsSquare)

                    inflatedViewSquare.setOnClickListener(clickInInflatedLayout()) //setting click to each item_content
                }
            } else if (dineInFloorTypeList[i].floorType == "Round") {
                val inflatedViewRound = layoutInflater.inflate(
                    R.layout.view_floor_round,
                    binding.flFloorPlan,
                    false
                )

                if (inflatedViewRound != null) {
                    val llMainParentRound: LinearLayout =
                        inflatedViewRound.findViewById(R.id.llMainParentRound)

                    val tvNoOFChairs: AppCompatTextView =
                        inflatedViewRound.findViewById(R.id.tvNoOFChairs)

                    tvNoOFChairs.text = "" + dineInFloorTypeList[i].noOFChairs


                    val tvTableName: AppCompatTextView =
                        inflatedViewRound.findViewById(R.id.tvTableName)

                    tvTableName.text = "" + dineInFloorTypeList[i].tableName

                    if (llMainParentRound.parent != null) {
                        (llMainParentRound.parent as ViewGroup).removeView(llMainParentRound)
                    }

                    /*pass object in settag*/
                    llMainParentRound.tag = i.toString()

                    val paramsRound = FrameLayout.LayoutParams(120, 120)
                    paramsRound.leftMargin = 500
                    paramsRound.topMargin = 300
                    binding.flFloorPlan.addView(llMainParentRound, paramsRound)

                    inflatedViewRound.setOnClickListener(clickInInflatedLayout()) //setting click to each item_content
                }

            }
        }

        /*  dineInFloorNameList[0].floorTypeList?.forEach {
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

                      inflatedViewSquare.setTag(Integer.toString(i));

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
          }*/


    }

    private fun clickInInflatedLayout(): View.OnClickListener {
        return View.OnClickListener { v ->
            val position = v.tag.toString().toInt()
            Log.d("Clickeditematposition", "::$position")
        }
    }

}