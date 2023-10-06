package com.android.pos.ui.fragments.settings.tax

import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.responseModel.CreateTaxResponse
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateTaxViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private var isEnableTax: Boolean = false
    val createTaxDetails = MutableLiveData(CreateTaxRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTaxResponse?>>()
    val data: LiveData<Event<CreateTaxResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var taxId: Int = -1
    private var itemPricingViewModel: String = ""

    private var isEdit: Boolean = false

    private var enableTaxViewModel: Boolean = true
    private var customAmountViewModel: Boolean = false
    private var taxTypeViewModel: String = "Percentage"
    private var itemIdsViewModel = ArrayList<Int>()

    private lateinit var taxData: CreateTaxRequestModel

    private lateinit var resource: Resource<CreateTaxResponse>

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.taxId = taxId
        this.isEdit = isEdit
    }

    fun setTaxData(taxData: TaxData) {
        createTaxDetails.value?.name = taxData.name!!
        createTaxDetails.value?.rate = taxData.rate
        enableTaxViewModel = taxData.isActive
        customAmountViewModel = taxData.isCustomAmount
        taxTypeViewModel = taxData.taxType.toString()

        isEnableTax = taxData.isActive
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun updateTaxDataInItem(tax: TaxData, itemIds: ArrayList<Int>, oldItemIds: List<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempTaxData: ArrayList<TaxData> = arrayListOf()

                // Remove Tax from Items
                oldItemIds.forEach {
                    if (!itemIds.contains(it)) {
                        val item: TbItem? = posRepository.getSingleItem(it)
                        if (item != null) {
                            tempTaxData = item.taxes as ArrayList<TaxData> ?: arrayListOf()
                            tempTaxData.removeIf { t -> t.id == tax.id }
                            posRepository.updateTaxDataForItem(tempTaxData, it)
                        }
                    }
                }

                // Add Tax to Items
                itemIds.forEach {
                    if (!oldItemIds.contains(it)) {
                        val item: TbItem? = posRepository.getSingleItem(it)
                        if (item != null) {
                            tempTaxData = item.taxes as ArrayList<TaxData> ?: arrayListOf()
                            if (!tempTaxData.contains(element = tax)) {
                                tempTaxData.add(tax)
                            }
                            posRepository.updateTaxDataForItem(tempTaxData, it)
                        }
                    }
                }

                // Update Tax data in items if tax name or value is changed. (BIS-2667)
                tax.itemIds.forEach {
                    val item: TbItem? = posRepository.getSingleItem(it)
                    if (item != null && !item.taxes.isNullOrEmpty()) {
                        tempTaxData = item.taxes as ArrayList<TaxData> ?: arrayListOf()
                        tempTaxData.removeIf { taxData-> taxData.id == tax.id }
                        tempTaxData.add(tax)
                        posRepository.updateTaxDataForItem(tempTaxData, it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setItemIds(itemIds: ArrayList<Int>) {
        this.itemIdsViewModel = itemIds
    }

    fun setItemPricing(itemPricing: String?) {
        this.itemPricingViewModel = itemPricing.toString()
    }

    fun enableTax(enableTax: Boolean) {
        this.enableTaxViewModel = enableTax
    }

    fun customAmount(customAmount: Boolean) {
        this.customAmountViewModel = customAmount
    }

    fun discountType(taxType: String) {
        this.taxTypeViewModel = taxType
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun submit(rates: Double) {
        val value = createTaxDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.tax_name_validate)
        } else if (rates == 0.0 || rates == 0.00
        ) {
            _snackbarText.value = Event(R.string.tax_rate_validate)
        } /*else if (TextUtils.isEmpty(itemPricingViewModel.trim())) {
            _snackbarText.value = Event(R.string.item_pricing_validate)
        } */ else {
            _showProgress.value = Event(true)


            taxData = CreateTaxRequestModel().apply {
                if (isEdit) id = taxId
                name = value!!.name.trim().replace("\\s+".toRegex(), " ")
                rate = rates
                isDefault = enableTaxViewModel
                isCustomAmount = customAmountViewModel
                itemIds = itemIdsViewModel
                itemPricing = /*itemPricingViewModel*/ "Add Tax To Item Price"
                taxType = taxTypeViewModel
                isActive = enableTaxViewModel
                locationId = prefProvider.getValueInt(LOCATION_ID, -1)

            }

            /*else {
                taxData = CreateTaxRequestModel().apply {
                    name = value!!.name
                    rate = value.rate
                    isActive = enableTaxViewModel
                    itemIds = itemIdsViewModel
                    itemPricing = itemPricingViewModel
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            }*/


            viewModelScope.launch {
                val taxDataFromDb: TaxData? = taxServiceChargeRepository.getItemsListOfTax(taxId)

                val oldItemIds: List<Int> = taxDataFromDb?.itemIds ?: arrayListOf()
                if (isEdit) {
                    resource = taxServiceChargeRepository.updateTax(taxId, taxData)
                } else {
                    resource = taxServiceChargeRepository.createTax(taxData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createTaxResponse ->
                                    val tax = createTaxResponse.data
                                    /* TaxData(
                                         name = createTaxResponse.data.name,
                                         id = createTaxResponse.data.id,
                                         locationId = createTaxResponse.data.locationId,
                                         rate = createTaxResponse.data.rate,
                                         taxType = createTaxResponse.data.taxType,
                                         isActive = createTaxResponse.data.isActive,
                                         isDefault = createTaxResponse.data.isDefault,
                                         isCustomAmount = createTaxResponse.data.isCustomAmount,
                                         itemPricing = createTaxResponse.data.itemPricing,
                                         itemIds = createTaxResponse.data.itemIds,
                                         createdAt = createTaxResponse.data.createdAt,
                                         updatedAt = createTaxResponse.data.updatedAt
                                     )*/

//                                    updateTaxDataInItem(tax, taxData.itemIds as ArrayList<Int>, oldItemIds)
                                    taxServiceChargeRepository.createTaxDatabase(tax)

                                    itemIdsViewModel = ArrayList()
                                    _data.value = Event(createTaxResponse)

                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }
                        }
                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }

                    Status.LOADING -> {
                        _showProgress.value = Event(true)
                    }
                }
            }

        }

    }


}