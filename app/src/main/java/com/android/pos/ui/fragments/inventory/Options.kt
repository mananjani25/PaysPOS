package com.android.pos.ui.fragments.inventory

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.databinding.FragmentOptionsBinding
import com.android.pos.ui.adapter.OptionListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Options : Fragment(), TextWatcher {
    private lateinit var binding: FragmentOptionsBinding
    private var isreOrder: Boolean = false
    var dragFrom = -1
    var dragTo = -1
    private lateinit var adapter: OptionListAdapter
    private val viewModel by viewModels<OptionSetViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOptionsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        optionSetObserver()
        swipeViewSetup()
        observeShowProgress()
        deleteObserve()

        binding.txtcreateoption.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createOption)
        }
    }

    private fun setAdapter() {
        binding.rvOptonList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        adapter = OptionListAdapter()
        binding.rvOptonList.adapter = adapter
        binding.edtSearch.addTextChangedListener(this)
    }

    private fun optionSetObserver() {

        viewModel.optionSets().observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvOptonList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 ->
                            adapter.add(it1)
                            binding.edtSearch.hint = "Search (" + it1.size + ") Options"
                        }
                    }
                    Status.ERROR -> {
                        binding.rvOptonList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvOptonList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        }
    }

    private fun swipeViewSetup() {

        object : SwipeHelper(activity, binding.rvOptonList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("optionObject", adapter.getItem(pos))
                    findNavController().navigate(
                        R.id.action_inventory_to_createOption,
                        bundle
                    )


                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_option_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            adapter.getItem(pos).id?.let { viewModel.deleteOptionSet(it) }
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }


                })
            }
        }

        val touchHelper =
            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {


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
                        adapter.getItem(dragFrom).sort?.let {
                            reallyMoved(
                                it,
                                adapter.getItem(dragTo).sort!!,
                                adapter.getItem(viewHolder.layoutPosition).id
                            )
                        }
                    }

                    dragFrom = -1
                    dragTo = -1
                }

            })

        touchHelper.attachToRecyclerView(binding.rvOptonList)
    }

    private fun reallyMoved(oldPos: Int, newPos: Int, modifierSetId: Int?) {
        if (modifierSetId != null) {

            isreOrder = true
            viewModel.reOrderOption(modifierSetId, oldPos, newPos)
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

    }

    private fun deleteObserve() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (!isreOrder)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)

                if (isreOrder) {
                    isreOrder = false
                    viewModel.reOrder(adapter.getAll())
                }
            }
        })

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        adapter.filter.filter(s.toString().trim())
    }
}