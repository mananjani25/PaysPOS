package com.android.pos.ui.fragments.createmodifier

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.CreateModifierSetBinding
import com.android.pos.ui.adapter.ModifierAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class CreateModifierSet : Fragment() {
    private var modifierSet: ModifierSet? = null
    private var isEdit: Boolean = false
    private lateinit var adapter: ModifierAdapter
    private lateinit var binding: CreateModifierSetBinding
    private val viewModel by viewModels<CreateModifierViewModel>()
    var dragFrom = -1
    var dragTo = -1

    private var itemIds = ArrayList<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.create_modifier_set, container, false)
        binding.lifecycleOwner = this
        binding.createModifierViewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!
        setupAdapter()
        setupUI()
        setupSnackbar()
        observeShowProgress()
        observeData()
        return binding.root
    }


    private fun setupUI() {

        binding.header.txtTitle.text = getString(R.string.new_modifier_set)
        binding.header.txtSave.text = getString(R.string.save)


        val resultDialogKey = getNavigationResultLiveData<ArrayList<TbItem>>(Constants.DIALOG_KEY)
        resultDialogKey?.observe(viewLifecycleOwner) {
            itemIds.clear()
            if (it.size > 0) {
                binding.txtItemsTotal.text = "" + it.size + " Items"
            } else {
                binding.txtItemsTotal.text = "No Items"
            }

            it.forEach {
                itemIds.add(it.itemId)
            }
            viewModel.setItemIds(itemIds)
        }

        if (isEdit) {
            binding.header.txtTitle.text = getString(R.string.update_modifier_set)
            binding.header.txtSave.text = getString(R.string.update)

            modifierSet = arguments?.getParcelable("modifierObject")!!
            itemIds = modifierSet!!.itemIds as ArrayList<Int>
            viewModel.setItemIds(itemIds)
            viewModel.setData(isEdit, modifierSet!!.name, modifierSet!!.id)
            if (itemIds.size > 0) {
                binding.txtItemsTotal.text = "" + itemIds.size + " Items"
            } else {
                binding.txtItemsTotal.text = "No Items"
            }

            modifierSet!!.modifiers.sortedBy {
                it.sort
            }

            adapter.addAll(modifierSet!!.modifiers)
        }

        binding.header.txtSave.setOnClickListener {
            viewModel.setModifiers(adapter.getAll())
            viewModel.setDeleteModifiers(adapter.getDelete())
            viewModel.submit()
        }
    }

    private fun setupAdapter() {

        adapter = ModifierAdapter(isEdit)
        binding.rvModifiers.adapter = adapter

        binding.llAddItems.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("where", "modifier")
//            if (isEdit) {
            bundle.putIntegerArrayList("itemIds", itemIds)
//            }
            findNavController().navigate(R.id.action_createIModifierSet_to_itemDialog, bundle)
        }

        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val oldPos = viewHolder.bindingAdapterPosition
                val newPos = target.bindingAdapterPosition
                Log.e(
                    "reorder after", "" + ":::" + ":::" +
                            viewHolder.bindingAdapterPosition.toString() + " :::  " + target.bindingAdapterPosition.toString()
                )
                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                val a = adapter.getItem(dragFrom).sort
                val b = adapter.getItem(dragTo).sort
                Log.e("onItemMove", "$a:: $b")



                adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

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
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvModifiers)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.imgBack.setOnClickListener {
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CREATEMODIFIER
            )
            navControll.popBackStack()
        }

        binding.edtModifier.setOnKeyListener(object : KeyListener, View.OnKeyListener {
            override fun getInputType(): Int {
              return 0
            }

            override fun onKeyDown(
                view: View?,
                text: Editable?,
                keyCode: Int,
                event: KeyEvent?
            ): Boolean {

                return false
            }

            override fun onKeyUp(
                view: View?,
                text: Editable?,
                keyCode: Int,
                event: KeyEvent?
            ): Boolean {
                return false
            }

            override fun onKeyOther(view: View?, text: Editable?, event: KeyEvent?): Boolean {
                return false
            }

            override fun clearMetaKeyState(view: View?, content: Editable?, states: Int) {

            }

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if ((event?.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    addModifierset(true)
                    return true
                }
                return false
            }

        })
        binding.edtPrice.setOnKeyListener(object : KeyListener, View.OnKeyListener {
            override fun getInputType(): Int {
               return 0
            }

            override fun onKeyDown(
                view: View?,
                text: Editable?,
                keyCode: Int,
                event: KeyEvent?
            ): Boolean {

                return false
            }

            override fun onKeyUp(
                view: View?,
                text: Editable?,
                keyCode: Int,
                event: KeyEvent?
            ): Boolean {
                return false
            }

            override fun onKeyOther(view: View?, text: Editable?, event: KeyEvent?): Boolean {
                return false
            }

            override fun clearMetaKeyState(view: View?, content: Editable?, states: Int) {

            }

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if ((event?.action == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    addModifierset(false)
                    return true
                }
                return false
            }

        })
    }

    private fun addModifierset(isModifier: Boolean) {
        if (isModifier) {
            if (binding.edtModifier.text.toString() != null && binding.edtModifier.text.toString()
                    .isNotEmpty()
            ) {
                val model = Modifier().apply {
                    name = binding.edtModifier.text.toString().trim()
                    price = 0.00
                }
                adapter.add(model)
            }
            binding.edtModifier.text?.clear()
            binding.edtModifier.clearFocus()

        } else {

            if (binding.edtPrice.text.toString() != null && binding.edtPrice.text.toString()
                    .isNotEmpty()
            ) {

                val parsed = binding.edtPrice.text.toString().trim().toDouble()
                val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))
                val model = Modifier().apply {
                    name = ""
                    price = formatted.replace("""[$,]""".toRegex(), "").toDouble()
                }
                adapter.add(model)
            }
            binding.edtPrice.text?.clear()
            binding.edtPrice.clearFocus()
        }

        viewModel.setModifiers(adapter.getAll())
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

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }


    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { message ->

                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, message
                    ) { _, _ ->
                        lifecycleScope.launchWhenResumed {
                            val navControll = findNavController()
                            navControll.previousBackStackEntry?.savedStateHandle?.set(
                                Constants.KEY,
                                Constants.CREATEMODIFIER
                            )
                            navControll.popBackStack()
                        }
                    }
                }


            }
        })
    }
}