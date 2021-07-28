package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.databinding.FragmentAddEditCustomerBinding
import com.android.pos.utils.AlertUtils
import com.google.gson.Gson

class AddEditCustomer : Fragment() {
    private lateinit var binding: FragmentAddEditCustomerBinding
    private var isEdit = false
    private val TAG = "AddEditCustomer"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddEditCustomerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isEdit = requireArguments().getBoolean("isEdit", false)
        Log.e(TAG, "isEdit  $isEdit")

        if (isEdit) {
            binding.txtCustomerType.setText("Edit Customer")
            val editModel: com.android.pos.data.model.CustomerListResponse.Data? =
                requireArguments().getParcelable<com.android.pos.data.model.CustomerListResponse.Data>(
                    "dataModel"
                )
            Log.e(TAG, "editModel  ${Gson().toJson(editModel)}")
            binding.edtFName.setText("${editModel?.first_name}")
            binding.edtLName.setText("${editModel?.last_name}")
            binding.edtPhoneNo.setText(
                "${
                    AlertUtils.usNumberFormat(editModel?.phones?.get(0)!!.phone_number)
                }"
            )
            if (editModel.email != null) {
                binding.edtEmailAdd.setText("${editModel.email}")
            }

            if (editModel.addresses.get(0).country != null) {
                binding.edtAddress.setText("${editModel.addresses.get(0).country}")
            }
            binding.edtStreet.setText("${editModel.addresses.get(0).street}")
            binding.edtCity.setText("${editModel.addresses.get(0).city}")
            binding.edtState.setText("${editModel.addresses.get(0).state}")
            binding.edtZip.setText("${editModel.addresses.get(0).postcode}")
            binding.edtCompany.setText("company")
            if (editModel.birth_date != null) {
                binding.edtBirthDay.setText("${editModel.birth_date}")
            }


            binding.edtEmailAdd.isEnabled = true
            binding.edtAddress.isEnabled = true
            binding.edtStreet.isEnabled = true
            binding.edtCity.isEnabled = true
            binding.edtState.isEnabled = true
            binding.edtZip.isEnabled = true
            binding.edtCompany.isEnabled = true
            binding.edtBirthDay.isEnabled = false

        } else {
            binding.txtCustomerType.setText("New Customer")
            binding.edtEmailAdd.isEnabled = true
            binding.edtAddress.isEnabled = true
            binding.edtStreet.isEnabled = true
            binding.edtCity.isEnabled = true
            binding.edtState.isEnabled = true
            binding.edtZip.isEnabled = true
            binding.edtCompany.isEnabled = true
            binding.edtBirthDay.isEnabled = false

        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }
}