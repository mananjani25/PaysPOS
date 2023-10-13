package com.android.pos.ui.fragments.inventory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
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
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCategoriesBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.CategoriesListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Categories(val clickedPosition: Int) : Fragment(),ItemCallback {
    private var isreOrder: Boolean = false
    private lateinit var adapter: CategoriesListAdapter
    private lateinit var binding: FragmentCategoriesBinding
    var listSize:Int?=0
    /* private var position: Int = -1
     private lateinit var categoryListUpdateDelete: ArrayList<TbCategory>*/
    private val viewModel by viewModels<CategoriesViewModel>()
    private val TAG = "Categories"
    var dragFrom = -1
    var dragTo = -1
    lateinit var prefProvider:PrefProvider

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
        observeShowProgress()
        deleteObserve()
        prefProvider = PrefProvider(requireContext())
    }

    private fun categoriesObserver() {

        viewModel._getCategories().observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvCategoriesList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        LogUtil.logE(TAG, "getCategoryData  ${Gson().toJson(it.data)}")
                        it.data?.let { it1 ->
                            listSize=it.data.size
                            adapter.add(it1)
                            for (i in 0 until it1.size){
                                if (it1[i].name == Constants.GIFT_CARD_CATEGORY){
                                    prefProvider.setValueInt(Constants.GIFT_CARD_SORT,it1[i].sort)
                                    if (it1[i].sort == 1){
                                        prefProvider.setValueboolean(Constants.GIFT_CARD_AT_FIRST,true)
                                    }
                                }
                                if (it1[i].name == Constants.DEFAULT_CATEGORY){
                                    prefProvider.setValueInt(Constants.DEFAULT_CATEGORY_SORT,it1[i].sort)
                                }
                            }
                            binding.edtSearch.hint = "Search (" + it1.size + ") Categories"
                        }
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

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun deleteObserve() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                val intent = Intent()
                intent.action = "inventory"
                intent.putExtra("position", clickedPosition)
                requireContext().sendBroadcast(intent)
//                if (!isreOrder)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)

                if (isreOrder) {

                    isreOrder = false
                    LogUtil.logE(TAG, "getAllCategories  ${Gson().toJson(adapter.getAll())}")
                    viewModel.reOrder(adapter.getAll())
                    // categoriesObserver()
                }

//                val intent = Intent()
//                intent.action = "inventory"
//                intent.putExtra("position", clickedPosition)
//                requireContext().sendBroadcast(intent)
            }
        }

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
                    val oldPos = viewHolder.bindingAdapterPosition
                    val newPos = target.bindingAdapterPosition
                    LogUtil.logE(TAG,"posGOTPoldPos ${oldPos}")
                    LogUtil.logE(TAG,"posGOTPnewPos ${newPos}")

                    if (dragFrom == -1) {
                        dragFrom = oldPos
                    }
                    dragTo = target.bindingAdapterPosition

                    adapter.onItemMove(
                        viewHolder.bindingAdapterPosition,
                        target.bindingAdapterPosition
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
                              adapter.getItem(viewHolder.layoutPosition).id
                          )
                       /* reallyMoved(
                            dragFrom,
                            dragTo,
                            adapter.getItem(dragTo).id
                        )*/

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
            LogUtil.logE(TAG, "positionnewPos  ${newPos}")
            LogUtil.logE(TAG, "positionoldPos  ${oldPos}")
            viewModel.reOrderCategory(categoryIdOld, oldPos, newPos)
        }

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(
            if (adapter.getItem(pos).name == "Default Category") {
                R.menu.edit_menu
            } else {
                R.menu.edit_delete__hide_menu
            }, popupMenu.menu
        )
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("categoryObject", adapter.getItem(pos))
                    try {
                        val defaultCategoryObject: TbCategory? = adapter.categoryList.find { it.name == "Default Category"}
                        bundle.putParcelable("DefaultCategoryObject", defaultCategoryObject)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    findNavController().navigate(
                        R.id.action_inventory_to_createCategory,
                        bundle
                    )
                }
                R.id.menu_delete -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_category_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            viewModel.deleteCategory(adapter.getItem(pos).id, false, false)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }
                }
                R.id.menu_hide -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.hide_category_message)
                    ) {
                        positiveButton(getString(R.string.deactivate)) {
                            viewModel.deleteCategory(adapter.getItem(pos).id, true, false)
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