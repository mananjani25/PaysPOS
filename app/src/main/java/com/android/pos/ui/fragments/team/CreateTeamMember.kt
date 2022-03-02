package com.android.pos.ui.fragments.team

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.TbCountryList
import com.android.pos.data.entities.TeamRole
import com.android.pos.databinding.FragmentCreateTeamMemberBinding

import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.SpinnerCustomAdapter
import com.android.pos.utils.*
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                    viewLifecycleOwner,
                    {
                        binding.txtRoleName.text = it.data?.name
                    })

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

        binding.llPermission.setOnClickListener {

            MaterialAlertDialogBuilder(it.context, R.style.MaterialAlertDialogText)
                .setTitle("Choose Role")
                .setSingleChoiceItems(
                    items.toArray(arrayOfNulls<CharSequence>(items.size)),
                    selectedPos
                ) { dialogInterface, i ->

                    dialogInterface.dismiss()
                    selectedPos = i
                    viewModel.setRoleId(itemsIds[i])
                    binding.txtRoleName.text = items[i]
                }
                .show()
        }

        binding.header.txtSave.setOnClickListener {

            viewModel.createTaxDetails.value?.firstName =
                binding.edtFirstName.text.toString().trim()
            viewModel.createTaxDetails.value?.lastName = binding.edtLastName.text.toString().trim()
            viewModel.createTaxDetails.value?.email = binding.edtEmail.text.toString().trim()
            viewModel.createTaxDetails.value?.phoneNumber =
                binding.edtMobileNumber.text.toString().trim()
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
            setPhoneCountry(country)
            Log.d("yash", "observeShowProgress: " + it[0].name)
            Log.d("yash", "observeShowProgress: " + it[1].name)
        }
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


    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        findNavController().popBackStack()
                    }
                }
            }
        })

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    private fun getRoleListObserver() {
        viewModel.roleList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { roleList ->
                            teamRoleList = roleList as ArrayList<TeamRole>
                            Log.e("teamRoleList", teamRoleList.size.toString())


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
        })
    }

    private fun setPhoneCountry(countryList: ArrayList<TbCountryList>) {
        var adapter = SpinnerCustomAdapter(requireContext(), countrylist = countryList)
        binding.edtCountry.adapter = adapter

        binding.edtCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.isCountryChanged(countryList[position].id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        if(employeeModel!=null){
            countryList.forEachIndexed() { index, item ->
                if (employeeModel?.phone_country.equals(item.name)) {
                    viewModel.isCountryChanged(item.id)
                    binding.edtCountry.setSelection(index)
                }
            }
        }else{
            viewModel.isCountryChanged(0)
            binding.edtCountry.setSelection(0)
        }


    }

}