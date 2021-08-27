package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
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
import android.view.MotionEvent
import com.android.pos.data.entities.Option
import com.android.pos.data.model.VariationListModel
import com.android.pos.ui.adapter.SelectedOptionSetNameAdapter
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

import java.util.*
import java.util.Arrays.asList
import kotlin.collections.ArrayList
import kotlin.reflect.KFunction
import java.util.Collections.emptyList
import java.util.function.Function

import java.util.stream.Collectors.toList
import java.util.stream.Stream
import java.util.Arrays.asList
import java.util.Collections.emptyList
import java.util.Optional.of
import java.util.stream.Collectors.toList


@AndroidEntryPoint
class ItemOptionsListDialog : DialogFragment(), AdapterView.OnItemSelectedListener {

    private lateinit var elements: ArrayList<Array<String>>
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var roleName = ArrayList<String>()
    private lateinit var binding: FragmentItemOptionsListBinding
    private var finalVariationList = ArrayList<List<Option>>()
    private var variationList = ArrayList<String>()
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

                            var set = mutableSetOf<String>()

                            itemOptionList.forEach { option ->

                                option.options.forEach {
                                    set.add(it.name)
                                }
                            }

                            Log.e("itemOptionList", set.toString())

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

            /* val a = VariationListModel()
             a.data = itemOptionList[position].options*/
            finalVariationList.add(itemOptionList[position].options)
            Log.e("optionsvariation", "::$finalVariationList")


            val product = computeCombinations2(finalVariationList)
            /*val employees = permissionList[position].employees?.map { it.name }
            itemBinding.teamMemberList.text = TextUtils.join(",", employees!!)*/

            /* val builder = StringBuilder()
             product?.forEach {
                 it.map {
                     builder.append(it.name.trim() + ",")
                 }

             }
             val finalVariation = builder.substring(0, builder.length - 1).toString()

             Log.e("optionsvariationoutside", "::$finalVariation")*/
            val builder = StringBuilder()
            product?.forEach {
                it.forEach {
                    variationList.add(builder.append(it.name.trim() + ",").toString())
                }
            }
            Log.e("optionsvariationoutside", "::$variationList")
            /* product?.forEach {

                 it.map {
                     it.name
                 }
             }*/
            /* val of = setOf(finalVariationList)
             val cartesianProductNew = cartesianProduct(of)
             println(cartesianProductNew)
             Log.d("options", "::" + cartesianProductNew)*/

            /*Log.e("addOptions", setOf(itemOptionList[0].options.map {
                it.name
            }).toString())*/
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

    private fun cartesianProductNew(finalVariationList: ArrayList<List<Option>>): List<Pair<List<Option>, List<Option>>> {
        val pairs = finalVariationList.withIndex().flatMap { (i1, e1) ->
            finalVariationList.withIndex().filter { (i2, _) ->
                i1 != i2
            }.map { (_, e2) ->
                Pair(e1, e2)
            }
        }

        Log.d("finalVariationList", "::$finalVariationList")
        return pairs

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    fun <T> computeCombinations2(lists: List<List<T>>): List<List<T>>? {
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

    fun cartesianProduct(a: Set<*>, vararg sets: Set<*>): Set<List<*>> =
        (setOf(a).plus(sets))
            .fold(listOf(listOf<Any?>())) { acc, set ->
                acc.flatMap { list -> set.map { element -> list + element } }
            }
            .toSet()


    fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
        return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
    }

    /*fun <T> cartesianProductLatest(i: Int, vararg a: List<T>): List<List<T>> {
        if (i == a.size) {
            val result: MutableList<List<T>> = ArrayList()
            result.add(ArrayList())
            return result
        }
        val next = cartesianProductLatest(i + 1, *a)
        val result: MutableList<List<T>> = ArrayList()
        for (j in a[i].indices) {
            for (k in next.indices) {
                val concat: MutableList<T> = ArrayList()
                concat.add(a[i][j])
                concat.addAll(next[k])
                result.add(concat)
            }
        }
        return result
    }*/

}