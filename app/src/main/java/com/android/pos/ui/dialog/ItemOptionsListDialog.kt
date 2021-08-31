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
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.Option
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_OPTIONS
import com.android.pos.databinding.FragmentItemOptionsListBinding
import com.android.pos.ui.adapter.SelectedOptionSetNameAdapter
import com.android.pos.ui.fragments.inventory.OptionSetViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.DeleteOptionCallback
import com.android.pos.utils.extensions.setNavigationResult
import com.android.pos.utils.statusUtils.Status
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList


@AndroidEntryPoint
class ItemOptionsListDialog : DialogFragment(), AdapterView.OnItemSelectedListener,
    DeleteOptionCallback {

    private lateinit var finalvariationList: List<List<Option>>
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var optionName = ArrayList<String>()
    private lateinit var binding: FragmentItemOptionsListBinding
    private var variationList = ArrayList<List<Option>>()
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

            if (selectedOptionSetNameAdapter.optionSetList.size > 0) {

                // createOptionSets(selectedOptionSetNameAdapter.optionSetList)
                finalvariationList = computeCombinations(variationList)

                setNavigationResult(DIALOG_KEY, selectedOptionSetNameAdapter.optionSetList)
                setNavigationResult(DIALOG_KEY_OPTIONS, finalvariationList)
                findNavController().popBackStack()
            }

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
        selectedOptionSetNameAdapter = SelectedOptionSetNameAdapter(viewModel)
        selectedOptionSetNameAdapter.setCallback(this)
        binding.rvOptonSetList.adapter = selectedOptionSetNameAdapter

    }


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
                            optionName = itemOptionList.map { it.name } as ArrayList<String>

                            setUpOptionSpinnerAdapter(optionName)

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

    private fun createOptionSets(optionSetList: ArrayList<OptionSet>) {
        val elements: ArrayList<Array<String>> = arrayListOf()

        for (i in 0 until optionSetList.size) {
            val list: MutableList<String> = mutableListOf()

            for (j in optionSetList[i].options.indices) {
                list.add(optionSetList[i].options.get(j).name)

            }
            elements.add(list.toTypedArray())

        }

        val immutableElements: List<ImmutableList<String>> =
            makeListImmutable(elements)

        val cartesianProduct: List<List<String>> =
            Lists.cartesianProduct(immutableElements)

        println(cartesianProduct)

        cartesianProduct.forEach {
            Log.e("cartesianProduct", it.joinToString { it })
        }
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
            // finalvariationList.clear()
            selectedOptionSetNameAdapter.addOptions(itemOptionList[position])
            Log.d("options", "::" + itemOptionList[position].options)

            variationList.add(itemOptionList[position].options)
            Log.e("optionsvariation", "::$variationList")

            if (itemOptionList[position].name == binding.spOptions.selectedItem) {
                optionName.removeAt(position)
                itemOptionList.removeAt(position)
                binding.spOptions.setSelection(0, false)
                spinnerAdapter.notifyDataSetChanged()
            }
        }
        spinnerTouched = false

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    fun <T> computeCombinations(lists: List<List<T>>): List<List<T>> {
        var combinations: List<List<T>> = Arrays.asList(Arrays.asList())
        for (list in lists) {
            val extraColumnCombinations: MutableList<List<T>> = ArrayList()
            for (combination in combinations) {
                for (element in list) {
                    val newCombination: MutableList<T> = ArrayList(combination)
                    newCombination.add(element)
                    extraColumnCombinations.add(newCombination)
                }
            }
            combinations = extraColumnCombinations
        }
        return combinations
    }

    override fun onItemClickListener(position: Int?, optionSet: OptionSet) {
        itemOptionList.add(optionSet)
        variationList.removeAt(position!!)
        optionName = itemOptionList.map { it.name } as ArrayList<String>
        setUpOptionSpinnerAdapter(optionName)
    }

}