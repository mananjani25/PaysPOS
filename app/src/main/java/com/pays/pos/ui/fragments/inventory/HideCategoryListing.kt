package com.pays.pos.ui.fragments.inventory

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.databinding.FragmentCategoriesBinding
import com.pays.pos.ui.adapter.CategoriesListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.SwipeHelper
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HideCategoryListing(val clickedPosition: Int) : Fragment() ,ItemCallback{
    private var isreOrder: Boolean = false
    private lateinit var adapter: CategoriesListAdapter
    private lateinit var binding: FragmentCategoriesBinding
    var listSize:Int?=0
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
        binding.txtCreatecatagory.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()

        categoriesObserver()
        observeShowProgress()
        deleteObserve()
    }

    private fun categoriesObserver() {

        viewModel.unhideCategories.observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        listSize=it.data?.size
                        if (it.data?.isNotEmpty() == true) {
                            binding.rvCategoriesList.visibility = View.VISIBLE
                            binding.txtNodata.visibility = View.GONE
                            it.data?.let { it1 ->
                                adapter.add(it1)
                                binding.edtSearch.hint = "Search (" + it1.size + ") Categories"
                            }

                        } else {

                            binding.edtSearch.hint = "Search (" + 0 + ") Categories"
                            binding.txtNodata.visibility = View.VISIBLE
                            if (it.message != null && it.message.isNotEmpty())
                                binding.txtNodata.text = it.message
                            else
                                binding.txtNodata.text = "No data available"

                            binding.rvCategoriesList.visibility = View.GONE

                            // binding.rvOpenOrder.visibility = View.GONE
                        }

                        binding.progressCircular.visibility = View.GONE
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


        }
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

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        val intent = Intent()
                        intent.action = "inventory"
                        intent.putExtra("position", clickedPosition)
                        requireContext().sendBroadcast(intent)
                    }
                }
            }
        }


    }

    private fun deleteObserve() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (!isreOrder)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)

                if (isreOrder) {

                    isreOrder = false
                    viewModel.reOrder(adapter.getAll())
                    // categoriesObserver()

                    val intent = Intent()
                    intent.action = "inventory"
                    intent.putExtra("position", clickedPosition)
                    requireContext().sendBroadcast(intent)
                }
            }
        })

    }

    private fun onClick() {
        binding.txtCreatecatagory.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createCategory)
        }
    }

    private fun setAdapter() {

        binding.rvCategoriesList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = CategoriesListAdapter(false)
        binding.rvCategoriesList.adapter = adapter
        adapter.setCallback(this)

        val touchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP + DOWN, 0) {


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val oldPos = viewHolder.layoutPosition
                    val newPos = target.layoutPosition
                    LogUtil.logE(
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
                            adapter.getItem(dragFrom).sort,
                            adapter.getItem(dragTo).sort,
                            adapter.getItem(viewHolder.layoutPosition)?.id
                        )
                    }

                    dragFrom = -1
                    dragTo = -1
                }

            })

        touchHelper.attachToRecyclerView(binding.rvCategoriesList)

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.edtSearch.setText("")
                }
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
            isreOrder = true
            viewModel.reOrderCategory(categoryIdOld, newPos, oldPos)
        }
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_edit)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_delete)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_hide)?.setTitle("Unhide")
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_hide -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.unhide_category_message)
                    ) {
                        positiveButton(getString(R.string.activate)) {
                            viewModel.deleteCategory(adapter.getItem(pos).id, true, true)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }

                }
            }
            true
        }
        popupMenu?.show()
    }
}