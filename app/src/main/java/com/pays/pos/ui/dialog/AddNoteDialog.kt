package com.pays.pos.ui.dialog

import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DailogAddNoteBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.NotesListAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.AddItemFragment
import com.pays.pos.ui.fragments.settings.notes.NoteListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class AddNoteDialog : DialogFragment(), ItemCallback {

    private var isOrderNote: Boolean = false
    private var item: TbCartItem? = null
    private var headerItemPosition: Int? = null
    private lateinit var binding: DailogAddNoteBinding
    private lateinit var noteListadapter: NotesListAdapter
    private val viewModel by viewModels<NoteListViewModel>()
    private val dashBoardCategoryViewModel by viewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddNoteDialog"

    @Inject
    lateinit var prefProvider: PrefProvider

    companion object {
        fun newInstance() = AddNoteDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddNoteBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setupData()
        setupAdapter()
        noteList()

        return binding.root
    }

    private fun setupAdapter() {

        noteListadapter = NotesListAdapter(viewModel, true)
        noteListadapter.setCallback(this)
        binding.rvNotes.adapter = noteListadapter
    }

    private fun setupData() {

        item = requireArguments().getParcelable("item")
        isOrderNote = requireArguments().getBoolean("isOrderNote")

        if ((requireArguments().getString("from")
                .toString()).equals(AddItemFragment.javaClass.name)
        ) {

            with(binding) {
                tvTitle?.text = getString(R.string.add_item_note)
                edtNote?.setHint(getString(R.string.add_item_note))

            }
        }

        if (prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT) == Constants.DINE_IN) {
            headerItemPosition = requireArguments().getInt("headerPos")
            LogUtil.logE(TAG, "headerItemPosition:  ${headerItemPosition}")
        }


        with(binding) {
            txtRemovenote?.gone()
            if (isOrderNote) {
                if (dashBoardCategoryViewModel.cartModel?.note?.isNotEmpty() == true) {
                    edtNote.setText(dashBoardCategoryViewModel.cartModel?.note ?: "")
                    txtRemovenote?.visible()
                } else {
                    txtRemovenote?.gone()
                }
            } else {
                if (item?.note?.isNotEmpty() == true) {
                    edtNote.setText(item?.note)
                    txtRemovenote?.visible()
                } else {
                    txtRemovenote?.gone()
                }
            }
        }

        binding.txtSave.setOnClickListener {

            if (binding.edtNote.text!!.toString().trim()
                    .isNotEmpty() && binding.edtNote.text!!.toString().trim().isNotBlank()
            ) {
                addNote()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    getString(R.string.enter_order_note),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                        }

                    })
            }
        }

        binding.imgBack.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                item?.note=""
                var it=item
                dismiss()
            }
        })
        /*binding.imgBack.setOnClickListener {
            dismiss()
        }*/
        binding.txtRemovenote?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val result = Bundle().apply {
                    putString("note", "")
                    item?.let {
                        if (!it.note.equals("")) {
                            it.isItemEdited = true
                            prefProvider.setValueboolean(
                                Constants.DO_PRINT,
                                true
                            )
                        }
                    }
                    putParcelable("item", item)
                    putBoolean("isOrderNote", isOrderNote)
                    headerItemPosition?.let { putInt("headerPos", it) }
                }

                setFragmentResult("request_key_note", result)
                findNavController().navigateUp()
            }

        })
    }


    private fun addNote() {
        val result = Bundle().apply {
            putString("note", binding.edtNote.text.toString().trim())
            item?.let {
                if (!it.note.equals(
                        binding.edtNote.text.toString().trim()
                    ) && prefProvider.getValue(Constants.OPEN_ORDER_ITEMS, "").isNotEmpty()
                ) {
                    it.isItemEdited = true

                    prefProvider.setValueboolean(
                        Constants.DO_PRINT,
                        true
                    )
                }
            }

            putParcelable("item", item)
            putBoolean("isOrderNote", isOrderNote)
            headerItemPosition?.let { putInt("headerPos", it) }
        }

        setFragmentResult("request_key_note", result)
        findNavController().navigateUp()
    }


    private fun noteList() {
        viewModel.taxListActive.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { taxList -> setTaxData(taxList) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    private fun setTaxData(taxList: List<NoteResponse.Data>) {
        taxList.sortedWith(compareBy { it.sort })
        Collections.reverse(taxList)
        noteListadapter.apply {
            addNotes(taxList)
            notifyDataSetChanged()
        }
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

    override fun onItemClickListener(view: View?, pos: Int) {

        val note = noteListadapter.getItem(pos)
        binding.edtNote.setText(note.name)
//        addNote()
    }
}