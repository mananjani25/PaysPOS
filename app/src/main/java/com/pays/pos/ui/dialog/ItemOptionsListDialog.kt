package com.pays.pos.ui.dialog

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
import com.pays.pos.R
import com.pays.pos.data.entities.Option
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.data.remote.Constants.DIALOG_KEY
import com.pays.pos.data.remote.Constants.DIALOG_KEY_OPTIONS
import com.pays.pos.databinding.FragmentItemOptionsListBinding

import com.pays.pos.ui.adapter.SelectedOptionSetNameAdapter
import com.pays.pos.ui.fragments.inventory.OptionSetViewModel
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.DeleteOptionSetCallback
import com.pays.pos.utils.extensions.setNavigationResult
import com.pays.pos.utils.statusUtils.Status
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList


@AndroidEntryPoint
class ItemOptionsListDialog : DialogFragment(), AdapterView.OnItemSelectedListener,
    DeleteOptionSetCallback {

    private var optionSetList: ArrayList<OptionSet>? = null

    // private var optionItemIds: ArrayList<Int>? = null
    private lateinit var finalvariationList: List<List<Option>>

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var optionName = ArrayList<String>()

    private lateinit var binding: FragmentItemOptionsListBinding
    private var variationList = ArrayList<List<Option>>()
    private lateinit var selectedOptionSetNameAdapter: SelectedOptionSetNameAdapter
    private var itemOptionList = ArrayList<OptionSet>()
    private var itemOptionListCopy = ArrayList<OptionSet>()
    private val viewModel by viewModels<OptionSetViewModel>()
    private var spinnerTouched = false
    var isEdit: Boolean = false
    var isLiveData: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemOptionsListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.spOptions.onItemSelectedListener = this
        setAdapter()

        binding.spOptions.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.header.txtSave.setOnClickListener {

            if (selectedOptionSetNameAdapter.optionSetList.size > 0) {

                // createOptionSets(selectedOptionSetNameAdapter.optionSetList)
                finalvariationList = computeCombinations(variationList)
                setNavigationResult(DIALOG_KEY, selectedOptionSetNameAdapter.optionSetList)
                setNavigationResult(DIALOG_KEY_OPTIONS, finalvariationList)
                findNavController().popBackStack()
            }

        }

        isEdit = arguments?.getBoolean("isEdit")!!

        optionSetList = if (isEdit) {
            arguments?.getParcelableArrayList("optionSets")
        } else {
            arguments?.getParcelableArrayList("optionSets")
        }

        binding.header.imgBack.setOnClickListener {
            dismiss()
        }

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.tv_update_variation)
            binding.header.txtTitle.text = getString(R.string.tv_options_update)
        } else {
            binding.header.txtSave.text = getString(R.string.tv_create_variation)
            binding.header.txtTitle.text = getString(R.string.tv_options)
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

                            if (!isLiveData) {

                                if (optionSetList != null) {
                                    var data=optionSetList
                                    optionSetList=ArrayList()

                                    for (i in data?.indices!!){
                                         optionSetList!!.addAll(listOf(it1[i]))
                                    }

                                    selectedOptionSetNameAdapter.addAllOptions(optionSetList!!)

                                    optionSetList?.forEach {
                                        variationList.add(it.options)
                                    }

                                    val myCollection = it1 as ArrayList<OptionSet>
                                    val iterator = myCollection.iterator()
                                    while (iterator.hasNext()) {
                                        val item = iterator.next()
                                        optionSetList?.forEach { selectedEmployee ->
                                            if (item.name == selectedEmployee.name) {
                                                iterator.remove()
                                            }
                                        }
                                    }

                                    itemOptionList.addAll(it1)

                                    val optionSet = OptionSet()
                                    optionSet.name = getString(R.string.tv_select_option_set)
                                    itemOptionList.add(0, optionSet)
                                    optionName = itemOptionList.map { it.name } as ArrayList<String>

                                    setUpOptionSpinnerAdapter(optionName)
                                } else {
                                    itemOptionList =
                                        it1 as ArrayList<OptionSet>
                                    val optionSet = OptionSet()
                                    optionSet.name = getString(R.string.tv_select_option_set)
                                    itemOptionList.add(0, optionSet)
                                    optionName = itemOptionList.map { it.name } as ArrayList<String>

                                    setUpOptionSpinnerAdapter(optionName)
                                }
                                // itemOptionListCopy.addAll(itemOptionList)

                                itemOptionList.forEach { optionSetCopy ->
                                    val optionSet = OptionSet()
                                    optionSet.displayName = optionSetCopy.displayName
                                    optionSet.id = optionSetCopy.id
                                    optionSet.locationId = optionSetCopy.locationId
                                    optionSet.name = optionSetCopy.name
                                    optionSet.optionType = optionSetCopy.optionType
                                    optionSet.sort = optionSetCopy.sort

                                    val optionsList = ArrayList<Option>()
                                    optionSetCopy.options.forEach {
                                        val options = Option()
                                        options.id = it.id
                                        options.optionSetId = it.optionSetId
                                        options.name = it.name
                                        options.sort = it.sort
                                        options._destroy = it._destroy
                                        optionsList.add(options)
                                    }
                                    optionSet.options = optionsList

                                    itemOptionListCopy.add(optionSet)
                                }

                                Log.d("itemOptionListCopy", "::" + itemOptionListCopy)
                            }
                            isLiveData = true
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
            LogUtil.logE("cartesianProduct", it.joinToString { it })
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
        /*if (binding.spOptions.selectedItem == getString(R.string.tv_select_option_set)) {
            return
        }*/

        if (spinnerTouched) {

            if (binding.spOptions.selectedItem == getString(R.string.tv_select_option_set)) {
                return
            } else {
                selectedOptionSetNameAdapter.addOptions(itemOptionList[position])

                loadAllOptionsFromOriginalList(position)
                Log.d("options", "::" + itemOptionList[position].options)

                variationList.add(itemOptionList[position].options)
                LogUtil.logE("optionsvariation", "::$variationList")

                if (itemOptionList[position].name == binding.spOptions.selectedItem) {
                    optionName.removeAt(position)
                    itemOptionList.removeAt(position)
                    binding.spOptions.setSelection(0, false)
                    spinnerAdapter.notifyDataSetChanged()
                }
            }


        }
        spinnerTouched = false

    }

    private fun loadAllOptionsFromOriginalList(position: Int) {
        if (position >= 0 && itemOptionList.size > 0 && itemOptionList.size > position) {
            val id = itemOptionList[position].id
            for (optionSet in itemOptionListCopy) {
                if (optionSet.id == id) {
                    itemOptionList[position].options = emptyList()
                    val newOptionList = arrayListOf<Option>()
                    optionSet.options.forEach {
                        newOptionList.add(it.clone() as Option)
                    }
                    itemOptionList[position].options = newOptionList
                }
            }
        }
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

    override fun onItemClickListener(position: Int?, optionSet: OptionSet?) {

        if (optionSet != null) {

            if (optionSet.options.isEmpty()) {
                variationList.removeAt(position!!)
                for (i in itemOptionListCopy.indices) {
                    if (optionSet.name == itemOptionListCopy[i].name) {
                        itemOptionList.add(itemOptionListCopy[i])
                        //  itemOptionList.remove(optionSet)
                        break
                    }
                }
            } else {
                itemOptionList.add(optionSet)
                variationList.removeAt(position!!)
            }

        } /*else {

            //   itemOptionList.clear()
            variationList.removeAt(position!!)
            itemOptionList.add(position + 1, itemOptionListCopy[position + 1])


        }*/
        optionName = itemOptionList.map { it.name } as ArrayList<String>
        setUpOptionSpinnerAdapter(optionName)

    }

}