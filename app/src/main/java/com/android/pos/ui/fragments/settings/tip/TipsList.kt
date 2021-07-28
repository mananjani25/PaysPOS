package com.android.pos.ui.fragments.settings.tip

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
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.FragmentTipsBinding
import com.android.pos.ui.adapter.TipsListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TipsList : Fragment() {

    private var position: Int = -1
    private lateinit var binding: FragmentTipsBinding
    private val viewModel by viewModels<TipListViewModel>()
    private lateinit var tipListadapter: TipsListAdapter
    private lateinit var tipObject: GetTipReponse.Data
    private lateinit var tipListUpdateDelete: ArrayList<GetTipReponse.Data>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTipListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteTip()
        notifyAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //   setAdapter()
        binding.txtAddNewTip.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addTip)
        }
    }

    private fun setUpRecyclerView() {
        tipListadapter = TipsListAdapter(viewModel)
        binding.rvTipList.adapter = tipListadapter

        object : SwipeHelper(activity, binding.rvTipList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    tipObject = tipListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("tipObject", tipObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_addTip, bundle)

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_tip_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            tipObject = tipListadapter.getItem(pos)
                            viewModel.delete(tipListadapter.getItem(pos).id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }
                })
            }
        }
    }

    private fun getTipListObserver() {
        viewModel.getTipList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                        resource.data?.let { tipList -> setTipData(tipList) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvTipList.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                tipListadapter.notifyDataSetChanged()
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

    private fun setTipData(tipList: List<GetTipReponse.Data>) {
        tipListUpdateDelete = tipList as ArrayList<GetTipReponse.Data>
        tipListadapter.apply {
            addTips(tipList)
            notifyDataSetChanged()
        }
    }

    private fun deleteTip() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/

                AlertUtils.showCustomAlert(requireActivity(), it.message)
                /*tipListUpdateDelete.remove(tipObject)
                tipListadapter.addTips(tipListUpdateDelete)
                tipListadapter.notifyItemRemoved(position)
                tipListadapter.notifyItemRangeChanged(position, tipListUpdateDelete.size)*/

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
}