package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.entities.OptionSet
import com.android.pos.databinding.FragmentItemOptionsListBinding
import com.android.pos.ui.adapter.SelectedOptionSetNameAdapter
import com.android.pos.ui.fragments.inventory.OptionSetViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList


@AndroidEntryPoint
class ItemOptionsListDialog : DialogFragment(), AdapterView.OnItemSelectedListener {

    private lateinit var elements: ArrayList<Array<String>>
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var roleName = ArrayList<String>()
    private lateinit var binding: FragmentItemOptionsListBinding

    private lateinit var selectedOptionSetNameAdapter: SelectedOptionSetNameAdapter
    private lateinit var itemOptionList: ArrayList<OptionSet>
    private val viewModel by viewModels<OptionSetViewModel>()
    private var spinnerTouched = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemOptionsListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.spOptions.onItemSelectedListener = this
        setAdapter()

        binding.spOptions.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                spinnerTouched = true
                return false
            }
        })

        binding.txtDone.setOnClickListener {

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        optionSetObserver()
        observeShowProgress()

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


    private fun setAdapter() {
        selectedOptionSetNameAdapter = SelectedOptionSetNameAdapter()
        binding.rvOptonSetList.adapter = selectedOptionSetNameAdapter

    }

    var products: List<List<Int>> = ArrayList()

    private fun optionSetObserver() {

        viewModel.optionSets().observe(viewLifecycleOwner, { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        it.data?.let { it1 ->
                            itemOptionList =
                                it1 as ArrayList<OptionSet>

                            val optionSet = OptionSet()
                            optionSet.name = getString(R.string.tv_select_option_set)
                            itemOptionList.add(0, optionSet)
                            roleName = itemOptionList.map { it.name } as ArrayList<String>

                            setUpOptionSpinnerAdapter(roleName)

                        }
                    }
                    Status.ERROR -> {
                    }
                    Status.LOADING -> {
                    }
                }
            }


        })

    }

    private fun createOptionSets() {
        elements = arrayListOf()

        for (i in 0 until itemOptionList.size) {
            val list: MutableList<String> = mutableListOf()

            for (j in itemOptionList[i].options.indices) {
                list.add(itemOptionList[i].options.get(j).name)

            }
            elements.add(list.toTypedArray())

        }

        val immutableElements: List<ImmutableList<String>> =
            makeListImmutable(elements)

        val cartesianProduct: List<List<String>> =
            Lists.cartesianProduct(immutableElements)

        println(cartesianProduct)
    }


    /**
     * @param values the list of all profiles provided by the client in matrix.json
     * @return the list of ImmutableList to compute the Cartesian product of values
     */
    private fun makeListImmutable(values: List<Array<String>>): List<ImmutableList<String>> {
        val converted: MutableList<ImmutableList<String>> = LinkedList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            values.forEach(Consumer { array: Array<String>? ->
                converted.add(
                    ImmutableList.copyOf(array)
                )
            })
        }
        return converted
    }


    fun cartesianProduct(a: Set<*>, b: Set<*>, vararg sets: Set<*>): Set<List<*>> =
        (setOf(a, b).plus(sets))
            .fold(listOf(listOf<Any?>())) { acc, set ->
                acc.flatMap { list -> set.map { element -> list + element } }
            }
            .toSet()

    private fun setUpOptionSpinnerAdapter(orderTypeList: ArrayList<String>) {

        spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            orderTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spOptions.adapter = spinnerAdapter


    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (binding.spOptions.selectedItem == getString(R.string.tv_select_option_set)) {
            return
        }

        if (spinnerTouched) {
            selectedOptionSetNameAdapter.addOptions(itemOptionList[position])
            Log.d("options", "::" + itemOptionList[position].options)

            /*Log.e("addOptions", setOf( itemOptionList[position].options.map {
                it.name
            }).toString())*/



            if (itemOptionList[position].name == binding.spOptions.selectedItem) {
                roleName.removeAt(position)
                itemOptionList.removeAt(position)
                binding.spOptions.setSelection(0, false)
                spinnerAdapter.notifyDataSetChanged()
            }
        }
        spinnerTouched = false

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

}