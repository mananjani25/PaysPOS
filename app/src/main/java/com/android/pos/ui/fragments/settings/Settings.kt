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
import com.android.pos.data.remote.Constants.ADD_SERVICE_CHARGE
import com.android.pos.data.remote.Constants.CREATEDISCOUNT
import com.android.pos.data.remote.Constants.CREATELOYALTY
import com.android.pos.data.remote.Constants.CREATE_NOTES
import com.android.pos.data.remote.Constants.CREATE_TAX
import com.android.pos.data.remote.Constants.CREATE_TIP
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.ORDER_RECEIPTS
import com.android.pos.data.remote.Constants.PRINTER
import com.android.pos.data.remote.Constants.TEAM_MEMBER
import com.android.pos.databinding.FragmentSettingsBinding
import com.android.pos.di.RolePermission
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.BusinessSettingAdapter
import com.android.pos.ui.fragments.settings.discount.DiscountList
import com.android.pos.ui.fragments.settings.hardware.Hardware
import com.android.pos.ui.fragments.settings.loyaltypoints.LoyaltyPointFragment
import com.android.pos.ui.fragments.settings.notes.Notes
import com.android.pos.ui.fragments.settings.servicecharge.ServiceChargeList
import com.android.pos.ui.fragments.settings.tax.TaxesList
import com.android.pos.ui.fragments.settings.teamrole.TeamMemberSettings
import com.android.pos.ui.fragments.settings.tip.TipsList
import com.android.pos.utils.extensions.styleBold
import com.android.pos.utils.extensions.styleNormal
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
                        setAdapter(0)


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
                        setAdapter(2)

                    }
                    CREATEDISCOUNT -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(3)

                    }
                    CREATE_NOTES -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(4)

                    }
                    ADD_SERVICE_CHARGE -> {

                        binding.txtBusiness.styleBold()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleNormal()
                        binding.txtReports.styleNormal()
                        setAdapter(5)

                    }
                    TEAM_MEMBER -> {
                        binding.txtBusiness.styleNormal()
                        binding.txtHardware.styleNormal()
                        binding.txtSecurity.styleNormal()
                        binding.txtMarketing.styleNormal()
                        binding.txtEmployee.styleBold()
                        binding.txtReports.styleNormal()
                        binding.rvBusiness.visibility = View.GONE
                        val frag: Fragment = TeamMemberSettings()
                        loadFragment(frag)
                        binding.commonToolbar.txtSubTitle.setText("Team Member")


                    }
                    PRINTER -> {

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


                }


            }

    }

    private fun onClick() {
        binding.txtBusiness.setOnClickListener {
            binding.txtBusiness.styleBold()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleNormal()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()

            setAdapter(0)

        }
        binding.txtHardware.setOnClickListener {
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

        binding.txtSecurity.setOnClickListener {
            binding.txtBusiness.styleNormal()
            binding.txtHardware.styleNormal()
            binding.txtSecurity.styleBold()
            binding.txtMarketing.styleNormal()
            binding.txtEmployee.styleNormal()
            binding.txtReports.styleNormal()
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
            binding.rvBusiness.visibility = View.GONE
            val frag: Fragment = TeamMemberSettings()
            loadFragment(frag)
            binding.commonToolbar.txtSubTitle.setText("Team Member")
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
        var list: ArrayList<BusinessSettingModel> = arrayListOf()
        list.add(BusinessSettingModel(0, "Taxes", false))
        list.add(BusinessSettingModel(0, "Tips", false))
        list.add(BusinessSettingModel(0, "Order Receipts", false))
        list.add(BusinessSettingModel(0, "Discount", false))
        list.add(BusinessSettingModel(0, "Notes", false))
        list.add(BusinessSettingModel(0, "Service Charge", false))
        list.add(BusinessSettingModel(0, "Loyalty Points", false))
        for (i in 0 until list.size) {
            if (selectedPos == i) {
                list[i].isSelected = true
            } else {
                list[i].isSelected = false
            }

        }

        when (selectedPos) {
            0 -> {
                binding.commonToolbar.txtSubTitle.text = "Taxes"
                val taxFrag: Fragment = TaxesList()
                loadFragment(taxFrag)
            }
            1 -> {
                binding.commonToolbar.txtSubTitle.text = "Tips"
                val tips: Fragment = TipsList()
                loadFragment(tips)

            }
            2 -> {
                binding.commonToolbar.txtSubTitle.text = "Order Receipts"
                val orderReceipts = OrderReceipt()
                loadFragment(orderReceipts)
            }
            3 -> {
                if (rolePermission.hasDiscountPermission(binding.root)) {
                    binding.commonToolbar.txtSubTitle.text = "Discount"
                    val discount: Fragment = DiscountList()
                    loadFragment(discount)
                }
            }
            4 -> {

                binding.commonToolbar.txtSubTitle.text = "Notes"
                val notes: Fragment = Notes()
                loadFragment(notes)

            }
            5 -> {
                binding.commonToolbar.txtSubTitle.text = "Service Charge"
                val service: Fragment = ServiceChargeList()
                loadFragment(service)

            }
            6 -> {
                binding.commonToolbar.txtSubTitle.text = "Loyalty Points"
                val service: Fragment = LoyaltyPointFragment()
                loadFragment(service)

            }
        }



        binding.rvBusiness.visibility = View.VISIBLE
        binding.rvBusiness.adapter = BusinessSettingAdapter(requireContext(), list, object :
            BusinessSettingAdapter.BusinessListInterface {
            override fun onClick(pos: Int) {
                when (pos) {
                    0 -> {
                        binding.commonToolbar.txtSubTitle.setText("Taxes")
                        val taxFrag: Fragment = TaxesList()
                        loadFragment(taxFrag)


                    }
                    1 -> {
                        binding.commonToolbar.txtSubTitle.setText("Tips")
                        val tips: Fragment = TipsList()
                        loadFragment(tips)

                    }
                    2 -> {
                        binding.commonToolbar.txtSubTitle.setText("Order Receipts")
                        val orderReceipts = OrderReceipt()
                        loadFragment(orderReceipts)
                    }
                    3 -> {

                        if (rolePermission.hasDiscountPermission(binding.root)) {
                            binding.commonToolbar.txtSubTitle.setText("Discount")
                            val discount: Fragment = DiscountList()
                            loadFragment(discount)
                        }

                    }
                    4 -> {
                        binding.commonToolbar.txtSubTitle.setText("Notes")
                        val notes: Fragment = Notes()
                        loadFragment(notes)

                    }
                    5 -> {
                        binding.commonToolbar.txtSubTitle.setText("Service Charge")
                        val service: Fragment = ServiceChargeList()
                        loadFragment(service)
                    }
                    6 -> {
                        binding.commonToolbar.txtSubTitle.text = "Loyalty Points"
                        val service: Fragment = LoyaltyPointFragment()
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