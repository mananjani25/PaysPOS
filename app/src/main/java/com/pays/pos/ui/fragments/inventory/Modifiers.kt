package com.pays.pos.ui.fragments.inventory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.databinding.FragmentModifiersBinding
import com.pays.pos.ui.adapter.ModifierSetsListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Modifiers(val clickedPosition: Int) : Fragment(), TextWatcher, ItemCallback,
    ModifierSetsListAdapter.ModifierDeleteCallback {
    private var isreOrder: Boolean = false
    var dragFrom = -1
    var dragTo = -1
    private lateinit var binding: FragmentModifiersBinding
    private lateinit var adapter: ModifierSetsListAdapter
    private val viewModel by viewModels<ModifierSetViewModel>()
    var listSize: Int? = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentModifiersBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        onClick()

        modifierSetsObserver()
        swipeViewSetup()
        observeShowProgress()
        deleteObserve()
    }


    private fun onClick() {
        binding.txtcreatemodifieer.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.inventory) {
                findNavController().navigate(R.id.action_inventory_to_createIModifierSet)
            }
        }
    }

    private fun setAdapter() {

        binding.rvModifiersList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        adapter = ModifierSetsListAdapter(false)
        binding.rvModifiersList.adapter = adapter
        adapter.setCallback(this)
        adapter.onDelteCallbackMod(this)
        binding.edtSearch.addTextChangedListener(this)
    }

    private fun modifierSetsObserver() {

        viewModel.modifierSets().observe(viewLifecycleOwner) {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        binding.progressCircular.visibility = View.GONE
                        if (it.data?.isNotEmpty() == true) {
                            binding.rvModifiersList.visibility = View.VISIBLE
                            binding.txtNodata?.gone()
                            it.data?.let { it1 ->
                                adapter.add(it1)
                                binding.edtSearch.hint = "Search (" + it1.size + ") Modifiers"
                            }
                        } else {
                            binding.rvModifiersList.visibility = View.GONE
                            binding.txtNodata?.visible()
                            binding.txtNodata?.text = "No Data Available"
                        }

                    }
                    Status.ERROR -> {
                        binding.rvModifiersList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvModifiersList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        }
    }

    private fun swipeViewSetup() {

        val touchHelper =
            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val oldPos = viewHolder.bindingAdapterPosition
                    val newPos = target.bindingAdapterPosition
                    LogUtil.logE(
                        "reorder after",
                        viewHolder.layoutPosition.toString() + " :::  " + target.layoutPosition.toString()
                    )

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
                        adapter.getItem(dragFrom).sort.let {
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

        touchHelper.attachToRecyclerView(binding.rvModifiersList)
    }

    private fun reallyMoved(oldPos: Int, newPos: Int, modifierSetId: Int?) {
        if (modifierSetId != null) {

            isreOrder = true
            viewModel.reOrderModifier(modifierSetId, oldPos, newPos)
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
               // if (!isreOrder)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)

                if (isreOrder) {
                    isreOrder = false
                    viewModel.reOrder(adapter.getAll())
                }

                val intent = Intent()
                intent.action = "inventory"
                intent.putExtra("position", clickedPosition)
                requireContext().sendBroadcast(intent)
            }
        })

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString() == " ") {
            binding.edtSearch.setText("")
        }
    }

    override fun afterTextChanged(s: Editable?) {
        adapter.filter.filter(s.toString().lowercase().trim())
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("modifierObject", adapter.getItem(pos))
                    findNavController().navigate(
                        R.id.action_inventory_to_createIModifierSet,
                        bundle
                    )
                }
                R.id.menu_delete -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_modifier_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            adapter.getItem(pos).id?.let { viewModel.deleteModifierSet(it) }
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

    override fun onDelete(pos: Int) {
        alert(
            getString(R.string.app_name),
            getString(R.string.delete_modifier_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                adapter.getItem(pos).id?.let { viewModel.deleteModifierSet(it) }
            }
            negativeButton(R.string.tv_cancel) {
                // Do negative stuff here
            }
        }

    }


}