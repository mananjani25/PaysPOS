package com.android.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.BusinessSettingModel
import com.android.pos.databinding.FragmentSettingsBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.BusinessSettingAdapter
import com.android.pos.utils.extensions.styleBold
import com.android.pos.utils.extensions.styleNormal

class Settings : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        onClick()
        setAdapter()

    }

    fun init() {
        binding.txtBusiness.styleBold()
        binding.txtHardware.styleNormal()
        binding.txtSecurity.styleNormal()
        binding.txtMarketing.styleNormal()
        binding.txtEmployee.styleNormal()
        binding.txtReports.styleNormal()

        binding.commonToolbar.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)

        }
        binding.commonToolbar.txtSubTitle.setText("Taxes")

    }

    private fun onClick() {
        binding.txtBusiness.setOnClickListener {
            binding.txtBusiness.styleBold()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()

            setAdapter()

        }
        binding.txtHardware.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleBold()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
            binding.rvBusiness.visibility = View.GONE
        }

        binding.txtSecurity.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleBold()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
            binding.rvBusiness.visibility = View.GONE
        }

        binding.txtMarketing.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleBold()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
            binding.rvBusiness.visibility = View.GONE
        }

        binding.txtEmployee.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleBold()
            binding.txtReports.styleNormal()
            binding.rvBusiness.visibility = View.GONE
        }
        binding.txtReports.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleBold()
            binding.rvBusiness.visibility = View.GONE

        }


    }


    private fun setAdapter() {
        var list: ArrayList<BusinessSettingModel> = arrayListOf()
        list.add(BusinessSettingModel(0, "Taxes", true))
        list.add(BusinessSettingModel(0, "Tips", false))
        list.add(BusinessSettingModel(0, "Order Receipts", false))
        list.add(BusinessSettingModel(0, "Discount", false))
        list.add(BusinessSettingModel(0, "Notes", false))
        list.add(BusinessSettingModel(0, "Service Charge", false))
        val taxFrag: Fragment = Taxes()
        loadFragment(taxFrag)

        binding.rvBusiness.visibility = View.VISIBLE
        binding.rvBusiness.adapter = BusinessSettingAdapter(requireContext(), list, object :
            BusinessSettingAdapter.BusinessListInterface {
            override fun onClick(pos: Int) {
                when (pos) {
                    0 -> {
                        binding.commonToolbar.txtSubTitle.setText("Taxes")
                        val taxFrag: Fragment = Taxes()
                        loadFragment(taxFrag)


                    }
                    1 -> {
                        binding.commonToolbar.txtSubTitle.setText("Tips")
                        val tips: Fragment = Tips()
                        loadFragment(tips)

                    }
                    2 -> {
                        binding.commonToolbar.txtSubTitle.setText("Order Receipts")
                        val orderReceipts = OrderReceipt()
                        loadFragment(orderReceipts)
                    }
                    3 -> {
                        binding.commonToolbar.txtSubTitle.setText("Discount")
                        val discount: Fragment = Discount()
                        loadFragment(discount)

                    }
                    4 -> {
                        binding.commonToolbar.txtSubTitle.setText("Notes")
                        val notes: Fragment = Notes()
                        loadFragment(notes)

                    }
                    5 -> {
                        binding.commonToolbar.txtSubTitle.setText("Service Charge")
                        val service: Fragment = ServiceCharge()
                        loadFragment(service)
                    }

                }

            }

        })


    }

    fun loadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()

    }
}