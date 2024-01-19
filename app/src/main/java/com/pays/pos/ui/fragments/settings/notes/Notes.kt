package com.pays.pos.ui.fragments.settings.notes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.databinding.FragmentNotesBinding

import com.pays.pos.ui.adapter.NotesListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class Notes : Fragment(), ItemCallback {
    private var isreOrder: Boolean = false
    private lateinit var binding: FragmentNotesBinding

    private lateinit var noteListadapter: NotesListAdapter
    private lateinit var noteObject: NoteResponse.Data
    private val viewModel by viewModels<NoteListViewModel>()
    private var position: Int = -1
    private lateinit var noteListUpdateDelete: ArrayList<NoteResponse.Data>
    private val TAG = "Notes"
    var dragFrom = -1
    var dragTo = -1
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        setUpRecyclerView()
        getTaxListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteTax()
        notifyAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.txtCreateNote.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_createNote)
        }
    }


    private fun setUpRecyclerView() {
        noteListadapter = NotesListAdapter(viewModel, false)
        binding.rvNoteLise.adapter = noteListadapter
        noteListadapter.setCallback(this)

        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val oldPos = viewHolder.bindingAdapterPosition
                val newPos = target.bindingAdapterPosition
                LogUtil.logE(
                    "reorder after", "" + ":::" + ":::" +
                            viewHolder.bindingAdapterPosition.toString() + " :::  " + target.bindingAdapterPosition.toString()
                )
                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                val a = noteListadapter.getItem(dragFrom).sort
                val b = noteListadapter.getItem(dragTo).sort
                LogUtil.logE("onItemMove", "$a:: $b")



                noteListadapter.onItemMove(
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

                    LogUtil.logE("clearView", "$dragFrom :: $dragTo")
                    reallyMoved(
                        noteListadapter.getItem(dragFrom).sort,
                        noteListadapter.getItem(dragTo).sort,
                        noteListadapter.getItem(viewHolder.bindingAdapterPosition).id
                    )
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvNoteLise)


    }

    private fun reallyMoved(oldPos: Int, newPos: Int, categoryIdOld: Int?) {
        if (categoryIdOld != null) {
            isreOrder = true
            LogUtil.logE("reallyMoved", "$oldPos :: $newPos")
            viewModel.reOrderItem(categoryIdOld, oldPos, newPos)
        }

    }

    private fun getTaxListObserver() {
        viewModel.getTaxList.observe(viewLifecycleOwner) {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvNoteLise.visibility = View.VISIBLE
                        resource.data?.let { taxList ->
                            setTaxData(taxList)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvNoteLise.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvNoteLise.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                noteListadapter.notifyDataSetChanged()
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

        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)


                viewModel.reOrder(noteListadapter.getAll())


            }
        }

    }

    private fun setTaxData(taxList: List<NoteResponse.Data>) {
        taxList.sortedWith(compareBy { it.sort })
        Collections.reverse(taxList)
        Log.d(TAG, "setTaxData: "+Gson().toJson(taxList))
        noteListUpdateDelete = taxList as ArrayList<NoteResponse.Data>
        noteListadapter.apply {
            addNotes(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteTax() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
            }
        }

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    noteObject = noteListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("taxObject", noteObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_createNote, bundle)
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        if (noteListadapter.getItem(pos).isActive) {
                            getString(R.string.delete_active_note_message)
                        } else {
                            getString(R.string.delete_note_message)
                        }
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            noteObject = noteListadapter.getItem(pos)
                            viewModel.delete(noteListadapter.getItem(pos).id)
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