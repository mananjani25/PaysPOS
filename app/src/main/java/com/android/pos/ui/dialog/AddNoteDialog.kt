package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.DailogAddNoteBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.NotesListAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.settings.notes.NoteListViewModel
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

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
        if (prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT) == Constants.DINE_IN) {
            headerItemPosition = requireArguments().getInt("headerPos")
            LogUtil.logE(TAG, "headerItemPosition:  ${headerItemPosition}")
        }


        with(binding) {
            if (isOrderNote) {
                if (dashBoardCategoryViewModel.cartModel?.note?.isNotEmpty() == true) {
                    edtNote.setText(dashBoardCategoryViewModel.cartModel?.note ?: "")
                    binding.txtRemovenote?.visible()
                } else {
                    binding.txtRemovenote?.gone()
                }
            } else {
                if (item?.note?.isNotEmpty() == true) {
                    edtNote.setText(item?.note)
                    binding.txtRemovenote?.visible()
                } else {
                    binding.txtRemovenote?.gone()
                }
            }
        }

        binding.txtSave.setOnClickListener {

            addNote()
        }

        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtRemovenote?.setOnClickListener {
            val result = Bundle().apply {
                putString("note", "")
                putParcelable("item", item)
                putBoolean("isOrderNote", isOrderNote)
                headerItemPosition?.let { putInt("headerPos", it) }
            }

            setFragmentResult("request_key_note", result)
            findNavController().navigateUp()
        }
    }


    private fun addNote() {
        val result = Bundle().apply {
            putString("note", binding.edtNote.text.toString().trim())
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
        addNote()
    }
}