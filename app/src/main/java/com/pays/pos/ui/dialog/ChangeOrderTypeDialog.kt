package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.OPEN_ORDER
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.databinding.DailogAddNoteBinding
import com.pays.pos.databinding.DialogChangeOrderTypeBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.NotesListAdapter
import com.pays.pos.ui.adapter.OrderTypeAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.settings.notes.NoteListViewModel
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class ChangeOrderTypeDialog : DialogFragment(), ItemCallback {

    private lateinit var binding: DialogChangeOrderTypeBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var orderTypeAdapter: OrderTypeAdapter? = null
    private val TAG = "ChangeOrderTypeDialog"

    @Inject
    lateinit var prefProvider: PrefProvider

    companion object {
        fun newInstance() = ChangeOrderTypeDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogChangeOrderTypeBinding.inflate(inflater, container, false)

        getOrderTypes()

        clickListeners()

        return binding.root
    }

    private fun clickListeners() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }
    }

    private fun getOrderTypes() {
        orderTypeAdapter = OrderTypeAdapter(isFromTypeChangeDialog = true, prefProvider)
        orderTypeAdapter?.setCallback(this)
        binding.rvOrderTypes.adapter = orderTypeAdapter

        viewModel.orderTypes().observe(requireActivity()) {

            if (prefProvider.getValue(ORDER_TYPE, "") == OPEN_ORDER || prefProvider.getValue(
                    ORDER_TYPE, "") == TAKEOUT
            ) {
                it.data?.let { it1 ->
                    orderTypeAdapter?.addAll(it1.filter { order ->
                        order.orderType == TAKEOUT || order.orderType == OPEN_ORDER
                    })
                }
            } else {
                it.data?.let { it1 ->
                    orderTypeAdapter?.addAll(it1.filter { order ->
                        order.orderType == prefProvider.getValue(
                            ORDER_TYPE, ""
                        )
                    })
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val model = orderTypeAdapter?.getItem(pos)
        LogUtil.logE(TAG, "itemClicked  ${Gson().toJson(model)}")
        prefProvider.setValue(Constants.ORDER_TYPE, model?.orderType!!)
        prefProvider.setValue(Constants.ORDER_TYPE_NAME, model.name)
        prefProvider.setValueInt(Constants.ORDER_TYPE_ID, model.id)
        val result = Bundle().apply {
            putParcelable("orderData", model)
        }
        requireActivity().supportFragmentManager.setFragmentResult(
            "request_order_type_change",
            result
        )
        findNavController().navigateUp()
    }
}