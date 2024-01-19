package com.pays.pos.ui.fragments.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.BusinessSettingModel
import com.pays.pos.data.remote.Constants.ADD_SERVICE_CHARGE
import com.pays.pos.data.remote.Constants.CREATEDISCOUNT
import com.pays.pos.data.remote.Constants.CREATELOYALTY
import com.pays.pos.data.remote.Constants.CREATE_NOTES
import com.pays.pos.data.remote.Constants.CREATE_TAX
import com.pays.pos.data.remote.Constants.CREATE_TIP
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.data.remote.Constants.ORDER_RECEIPTS
import com.pays.pos.data.remote.Constants.PRINTER
import com.pays.pos.data.remote.Constants.SCAN_GUN
import com.pays.pos.data.remote.Constants.SETUP_BUSINESS_DETAILS
import com.pays.pos.data.remote.Constants.TEAM_MEMBER
import com.pays.pos.databinding.FragmentSettingsBinding


import com.pays.pos.di.RolePermission
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.adapter.BusinessSettingAdapter
import com.pays.pos.ui.fragments.settings.Security.Security
import com.pays.pos.ui.fragments.settings.business.BusinessDetailsFragment
import com.pays.pos.ui.fragments.settings.discount.DiscountList
import com.pays.pos.ui.fragments.settings.hardware.Hardware
import com.pays.pos.ui.fragments.settings.loyaltypoints.LoyaltyPointFragment
import com.pays.pos.ui.fragments.settings.notes.Notes
import com.pays.pos.ui.fragments.settings.servicecharge.ServiceChargeList
import com.pays.pos.ui.fragments.settings.tax.TaxesList
import com.pays.pos.ui.fragments.settings.teamrole.TeamMemberSettings
import com.pays.pos.ui.fragments.settings.tip.TipsList
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.extensions.styleBold
import com.pays.pos.utils.extensions.styleNormal
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Settings : Fragment() {
    private lateinit var binding: FragmentSettingsBinding

    @Inject
    lateinit var rolePermission: RolePermission

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
        setAdapter(0)
        setCallBack()

    }

    fun init() {
        binding.commonToolbar.llEmailPrint?.visibility = View.GONE
        binding.txtBusiness.styleBold()
        binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.btnColor))
        binding.txtHardware.styleNormal()
        binding.txtSecurity.styleNormal()
        binding.txtMarketing.styleNormal()
        binding.txtEmployee.styleNormal()
        binding.txtReports.styleNormal()

        binding.commonToolbar.imgDrawer.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_menuFragment)

        }
        binding.commonToolbar.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)
        }
        binding.commonToolbar.txtSubTitle.setText("Taxes")

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(KEY)
            ?.observe(viewLifecycleOwner) { it ->

                when (it) {
                    CREATE_TAX -> {
                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(3)


                    }
                    CREATE_TIP -> {
                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(1)

                    }
                    ORDER_RECEIPTS -> {
                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(7)

                    }
                    CREATEDISCOUNT -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(4)

                    }
                    CREATE_NOTES -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(5)

                    }
                    ADD_SERVICE_CHARGE -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(2)

                    }
                    TEAM_MEMBER -> {
                        binding.txtBusiness.styleNormal()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleBold()
                        binding.txtReports.styleNormal()
                        binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.bg_color))
                        binding.txtHardware.setBackgroundColor(resources.getColor(R.color.bg_color))
                        binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.bg_color))
                        binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.bg_color))
                        binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.btnColor))
                        binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))
                        binding.rvBusiness.visibility = View.GONE
                        val frag: Fragment = TeamMemberSettings()
                        loadFragment(frag)
                        binding.commonToolbar.txtSubTitle.setText("Employees")


                    }
                    PRINTER, SCAN_GUN -> {

                        binding.txtBusiness.styleNormal()
                        binding.txtHardware.styleBold()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        binding.rvBusiness.visibility = View.GONE
                        val frag: Fragment = Hardware()
                        loadFragment(frag)
                        binding.commonToolbar.txtSubTitle.setText("Hardware")

                    }

                    CREATELOYALTY -> {
                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(6)
                    }
                    SETUP_BUSINESS_DETAILS -> {
                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(0)
                    }


                }


            }

    }

    private fun onClick() {
        binding.txtBusiness.setOnClickListener {
            binding.txtBusiness.styleBold()
            binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.btnColor))
            binding.txtHardware.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))

            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()

            if(binding.rvBusiness.visibility == View.GONE) {
                setAdapter(0)
            }

        }
        binding.txtHardware.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleBold()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
            binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtHardware.setBackgroundColor(resources.getColor(R.color.btnColor))
            binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.rvBusiness.visibility = View.GONE
            val frag: Fragment = Hardware()
            loadFragment(frag)
            binding.commonToolbar.txtSubTitle.setText("Hardware")

        }

        binding.txtSecurity.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleBold()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
            binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtHardware.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.btnColor))
            binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.rvBusiness.visibility = View.GONE
            val frag: Fragment = Security()
            loadFragment(frag)
            binding.commonToolbar.txtSubTitle.setText("Security")
        }

        binding.txtMarketing.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleBold()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()

            binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtHardware.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.btnColor))
            binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.rvBusiness.visibility = View.GONE
            val frag: Fragment = Marketing()
            loadFragment(frag)
            binding.commonToolbar.txtSubTitle.setText("Marketing")
        }

        binding.txtEmployee.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleBold()
            binding.txtReports.styleNormal()
            binding.txtBusiness.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtHardware.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtSecurity.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtMarketing.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.txtEmployee.setBackgroundColor(resources.getColor(R.color.btnColor))
            binding.txtReports.setBackgroundColor(resources.getColor(R.color.bg_color))
            binding.rvBusiness.visibility = View.GONE
            val frag: Fragment = TeamMemberSettings()
            loadFragment(frag)
            binding.commonToolbar.txtSubTitle.text = "Employees"
        }
        binding.txtReports.setOnClickListener {
            /*binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleBold()
            binding.rvBusiness.visibility = View.GONE
*/
        }


    }


    private fun setAdapter(selectedPos: Int) {
        val list: ArrayList<BusinessSettingModel> = arrayListOf()
        list.add(BusinessSettingModel(0, "Business Information", false))
        list.add(BusinessSettingModel(0, "Tips", false))
        list.add(BusinessSettingModel(0, "Service Charges", false))
        list.add(BusinessSettingModel(0, "Taxes", false))
        list.add(BusinessSettingModel(0, "Discounts", false))
        list.add(BusinessSettingModel(0, "Order Notes", false))
        list.add(BusinessSettingModel(0, "Loyalty Programs", false))
        list.add(BusinessSettingModel(0, "Receipt Settings", false))
        for (i in 0 until list.size) {
            list[i].isSelected = selectedPos == i

        }

        setupView(selectedPos)

        binding.rvBusiness.visibility = View.VISIBLE
        binding.rvBusiness.adapter = BusinessSettingAdapter(requireContext(), list, object :
            BusinessSettingAdapter.BusinessListInterface {
            override fun onClick(pos: Int) {
                if (!list[pos].isSelected) {
                    setupView(pos)
                }

            }

        })


    }

    private fun setupView(pos: Int) {
        when (pos) {
            0 -> {
                binding.commonToolbar.txtSubTitle.text = "Business Information"
                val service: Fragment = BusinessDetailsFragment()
                loadFragment(service)

            }
            1 -> {
                binding.commonToolbar.txtSubTitle.text = "Tips"
                val tips: Fragment = TipsList()
                loadFragment(tips)

            }
            2 -> {
                binding.commonToolbar.txtSubTitle.text = "Service Charges"
                val service: Fragment = ServiceChargeList()
                loadFragment(service)
            }
            3 -> {
                binding.commonToolbar.txtSubTitle.text = "Taxes"
                val taxFrag: Fragment = TaxesList()
                loadFragment(taxFrag)
            }
            4 -> {

                if (rolePermission.hasDiscountPermission(binding.root)) {
                    binding.commonToolbar.txtSubTitle.text = "Discounts"
                    val discount: Fragment = DiscountList()
                    loadFragment(discount)
                }

            }
            5 -> {
                binding.commonToolbar.txtSubTitle.text = "Order Notes"
                val notes: Fragment = Notes()
                loadFragment(notes)

            }
            6 -> {
                binding.commonToolbar.txtSubTitle.text = "Loyalty Programs"
                val service: Fragment = LoyaltyPointFragment()
                loadFragment(service)

            }
            7 -> {
                binding.commonToolbar.txtSubTitle.text = "Receipt Settings"
                val orderReceipts = OrderReceipt()
                loadFragment(orderReceipts)
            }


        }
    }

    private fun loadFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
            fm.beginTransaction().replace(binding.frameLayout.id, frag).commit()

    }

    fun setCallBack() {
        ((activity as MainActivity).fragmentCallBack) = { fragment ->
            LogUtil.logE("!_@_", "fragment callback")
            fragment?.let { loadFragment(it) }
        }
    }
}