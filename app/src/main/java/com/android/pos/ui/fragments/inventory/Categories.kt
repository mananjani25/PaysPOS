package com.android.pos.ui.fragments.inventory

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.databinding.FragmentCategoriesBinding
import com.android.pos.ui.adapter.CategoriesListAdapter
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Categories : Fragment() {
    private lateinit var adapter: CategoriesListAdapter
    private lateinit var binding: FragmentCategoriesBinding

    /* private var position: Int = -1
     private lateinit var categoryListUpdateDelete: ArrayList<TbCategory>*/
    private val viewModel by viewModels<CategoriesViewModel>()
    var dragFrom = -1
    var dragTo = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_categories, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()

        categoriesObserver()
    }

    private fun categoriesObserver() {

        viewModel.categories.observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvCategoriesList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 -> adapter.add(it1) }
                    }
                    Status.ERROR -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvCategoriesList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        })
    }

    private fun onClick() {
        binding.txtCreateCategory.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createCategory)
        }
    }

    private fun setAdapter() {

        adapter = CategoriesListAdapter(false)
        binding.rvCategoriesList.adapter = adapter

        object : SwipeHelper(activity, binding.rvCategoriesList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                 underlayButtons.add(UnderlayButton(
                     "Hide",
                     0,
                     Color.parseColor("#2997cc")
                 ) { pos ->
                    // hideCategoryCall(pos)

                 })
                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("categoryObject", adapter.getItem(pos))
                    findNavController().navigate(R.id.action_inventory_to_createCategory,bundle)


                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_category_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            viewModel.deleteCategory(adapter.getItem(pos)?.id!!)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }

                    /*activity?.let {
                        AlertUtils.showConfirmAlert(
                            it, getString(R.string.delete_category_message)
                        ) { _, _ ->

                            deleteCategoryCall(pos)


                        }
                    }*/

                })
            }
        }

        val touchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP + DOWN, 0) {


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val oldPos = viewHolder.layoutPosition
                    val newPos = target.layoutPosition
                    Log.e(
                        "reorder after",
                        viewHolder.layoutPosition.toString() + " :::  " + target.layoutPosition.toString()
                    )

                    if (dragFrom == -1) {
                        dragFrom = oldPos
                    }
                    dragTo = newPos

                    adapter.onItemMove(
                        viewHolder.layoutPosition,
                        target.layoutPosition
                    )

                    return true
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {

                    if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                        reallyMoved(
                            dragFrom,
                            dragTo,
                            adapter.getItem(viewHolder.layoutPosition)?.id
                        )
                    }

                    dragFrom = -1
                    dragTo = -1
                }

            })

        touchHelper.attachToRecyclerView(binding.rvCategoriesList)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // TODO Auto-generated method stub

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

                /* if (s.isEmpty()) {
                     imgSearchCategory.setImageResource(R.drawable.ic_search)
                 } else imgSearchCategory.setImageResource(R.drawable.ic_close_gray)*/

            }
        })


    }
    private fun reallyMoved(oldPos: Int, newPos: Int, categoryIdOld: Int?) {
        if (categoryIdOld != null) {
            //  reorderCall(categoryIdOld, oldPos, newPos)
        }

    }
}