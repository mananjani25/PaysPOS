package com.pays.pos.ui.fragments.settings.stickyreceipt

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.Dash
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentStickyReceiptSettingsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StickyReceiptSettings : Fragment() {
    lateinit var binding: FragmentStickyReceiptSettingsBinding
    private val dashboardViewModel by viewModels<DashBoardCategoryViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_sticky_receipt_settings,
            container,
            false
        )

        initViews()
        initObserver()
        return binding.root
    }

    private fun initObserver() {
        dashboardViewModel.printOrderIdInStickyReceipt.observe(viewLifecycleOwner,object:Observer<Boolean>{
            override fun onChanged(t: Boolean?) {
                t?.let {
                    if (it){
                        activity?.let {
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                it, "Sticky Receipt Settings was successfully updated."
                            ) { _, _ ->
                                exitScreen()
                            }
                        }
                    }else{
                        exitScreen()
                    }
                }
            }
        })
    }

    private fun initViews() {
        with(binding) {
//            As or now we are just using the shared preferences because the data is less, when the whole receipt settings reqirement will come then the Room database will come.
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    swtOrderId.isChecked = dashboardViewModel.getLabelPrinterSettingsData().printOrderId
                } catch (e: Exception) {
                    Log.d("Exception", Gson().toJson(e))
                }
            }
//😃
//            swtOrderId.isChecked=prefProvider.getValueboolean(Constants.STICKY_ORDER_ID,false)

            txtSave.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    dashboardViewModel.updateOrderId(swtOrderId.isChecked)
//                    prefProvider.setValueboolean(Constants.STICKY_ORDER_ID,swtOrderId.isChecked)

                }
            })
        }

        binding.ivBack.setOnClickListener {
            exitScreen()
        }
    }

    private fun exitScreen() {
        findNavController().popBackStack()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            StickyReceiptSettings().apply {

            }
    }
}