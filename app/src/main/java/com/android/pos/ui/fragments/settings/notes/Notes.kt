package com.android.pos.ui.fragments.settings.notes

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.FragmentNotesBinding
import com.android.pos.ui.adapter.NotesListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class Notes : Fragment() {
    private lateinit var binding: FragmentNotesBinding

    private lateinit var noteListadapter: NotesListAdapter
    private lateinit var noteObject: NoteResponse.Data
    private val viewModel by viewModels<NoteListViewModel>()
    private var position: Int = -1
    private lateinit var noteListUpdateDelete: ArrayList<NoteResponse.Data>

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

        object : SwipeHelper(activity, binding.rvNoteLise) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    noteObject = noteListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("taxObject", noteObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_createNote, bundle)

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_note_message)
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
                })

            }
        }
    }


    private fun getTaxListObserver() {
        viewModel.getTaxList.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvNoteLise.visibility = View.VISIBLE
                        resource.data?.let { taxList ->
                            Collections.reverse(taxList)
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
        })
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                noteListadapter.notifyDataSetChanged()
            }
        })
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

    private fun setTaxData(taxList: List<NoteResponse.Data>) {
        noteListUpdateDelete = taxList as ArrayList<NoteResponse.Data>
        noteListadapter.apply {
            addNotes(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteTax() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
                /*noteListUpdateDelete.remove(noteObject)
                noteListadapter.addNotes(noteListUpdateDelete)
                noteListadapter.notifyItemRemoved(position)
                noteListadapter.notifyItemRangeChanged(position, noteListUpdateDelete.size)*/
            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
}