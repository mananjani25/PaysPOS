package com.pays.pos.ui.fragments.team

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.TbCountryList
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.databinding.FragmentCreateTeamMemberBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.SpinnerCustomAdapter
import com.pays.pos.utils.*
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateTeamMember : Fragment() {


    private var selectedPos: Int = -1
    private var teamRoleList: ArrayList<TeamRole> = arrayListOf()
    private var employeeModel: Employee? = null
    private val viewModel by viewModels<CreateTeamViewModel>()
    var items: ArrayList<String> = arrayListOf()
    var itemsIds: ArrayList<Int> = arrayListOf()
    private lateinit var binding: FragmentCreateTeamMemberBinding
    private var country = arrayListOf<TbCountryList>()
    private var country_name = arrayListOf<String>()


    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_create_team_member,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        employeeModel = arguments?.getParcelable("data")

        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.tv_create_team_member)

        if (employeeModel != null) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.tv_update_team_member)

            employeeModel!!.teamRoleId?.let { viewModel.setRoleId(it) }
            viewModel.setTaxData(employeeModel!!)
            viewModel.isEditData(true, employeeModel!!.id)
            viewModel.locationId = employeeModel!!.locationId

            employeeModel?.teamRoleId?.let { viewModel.roleNameById(it) }
                ?.observe(
                    viewLifecycleOwner
                ) {
                    binding.txtRoleName.text = it.data?.name
                }

            MethodUtils.setPriceEditText(binding.edtHours, employeeModel!!.hourlyWages)

            binding.edtFirstName.setText(employeeModel!!.firstName)
            binding.edtLastName.setText(employeeModel!!.lastName)
            binding.edtEmail.setText(employeeModel!!.email)
            binding.edtMobileNumber.setText(employeeModel!!.phoneNumber)
            binding.edtPasscode.setText(employeeModel!!.passcode)
        }

        binding.edtHours.addTextChangedListener(AmountWatcher(binding.edtHours))

        setupSnackbar()
        observeShowProgress()
        navigate()
        getRoleListObserver()

        binding.llCountry.setOnClickListener {
            val textView = TextView(context)
            textView.text = "Choose Country"
            textView.setPadding(20, 20, 20, 20)
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.textSize = 20f
            textView.setBackgroundColor(resources.getColor(R.color.btnColor))
            textView.setTextColor(Color.WHITE)
            val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTeamTheme)
            builder.setCustomTitle(textView)
            builder.setItems(
                country_name.toArray(arrayOfNulls<String>(country_name.size))
            ) { dialog, which ->
                dialog.dismiss()
                selectedPos = which
                country.forEachIndexed { index, item ->
                    if (item.name == country_name[selectedPos]) {
                        viewModel.isCountryChanged(item.id)
                        Log.d("yash", "onCreateView: " + item.id)
                        Log.d("yashh", "onCreateView: " + item.name)
                    }
                }
                binding.txtCountrName.text = country_name[selectedPos]
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()

        }
        binding.llPermission.setOnClickListener {
            val textView = TextView(context)
            textView.text = "Choose Role"
            textView.setPadding(20, 20, 20, 20)
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.textSize = 20f
            textView.setBackgroundColor(resources.getColor(R.color.btnColor))
            textView.setTextColor(Color.WHITE)
            val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTeamTheme)
            builder.setCustomTitle(textView)
            builder.setItems(
                items.toArray(arrayOfNulls<CharSequence>(items.size))
            ) { dialog, which ->
                dialog.dismiss()
                selectedPos = which
                viewModel.setRoleId(itemsIds[which])
                binding.txtRoleName.text = items[which]
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        binding.header.txtSave.setOnClickListener {

            viewModel.createTaxDetails.value?.firstName =
                binding.edtFirstName.text.toString().trim()
            viewModel.createTaxDetails.value?.lastName = binding.edtLastName.text.toString().trim()
            viewModel.createTaxDetails.value?.email = binding.edtEmail.text.toString().trim()
            viewModel.createTaxDetails.value?.phoneNumber = binding.edtMobileNumber.text.toString().trim()
            viewModel.createTaxDetails.value?.passcode = binding.edtPasscode.text.toString().trim()

            var hours = 0.0
            if (binding.edtHours.text.toString().trim().isNotEmpty()) {
                val s = binding.edtHours.text.toString().trim()
                hours = MethodUtils.clearString(s)
            }

            viewModel.createTaxDetails.value?.hourly_wages = hours

            viewModel.submit()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.coutrylist.observe(viewLifecycleOwner) {
            country = it as ArrayList<TbCountryList>
            country.forEach { it1 ->
                country_name.add(it1.name)
            }
            if (employeeModel != null) {
                country_name.forEachIndexed { index, item ->
                    if (item == employeeModel!!.phone_country && index < country.size) {
                        viewModel.isCountryChanged(country[index].id)
                        binding.txtCountrName.text = country_name[index].toString()
                    }

                }
            } else {
                viewModel.isCountryChanged(country[0].id)
                binding.txtCountrName.text = country_name[0].toString()
            }


//            setPhoneCountry(country)

            Log.d("yash", "observeShowProgress: " + it[0].name)
            Log.d("yash", "observeShowProgress: " + it[1].name)
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

    }


    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        findNavController().popBackStack()
                    }
                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    private fun getRoleListObserver() {
        viewModel.roleList.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { roleList ->
                            teamRoleList = roleList as ArrayList<TeamRole>
                            LogUtil.logE("teamRoleList", teamRoleList.size.toString())


                            teamRoleList.forEachIndexed { index, teamRole ->

                                items.add(teamRole.name)
                                itemsIds.add(teamRole.id)
                            }


                        }

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


}