package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    val TAG = "DashBoardCateViewModel"
    var totalPrice: Double = 0.0
    var totalCount = 0
    var subTotalPrice = 0.0
    var totalTax = 0.0
    var totalServiceCharge = 0.0

    val venueData = posRepository.syncVenueData()

    val venueDataLocal = posRepository.venueDataLocal()

    val serviceCharges = posRepository.serviceChargeList()

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    fun modifierSet(intArray: IntArray) = posRepository.modifierSetList(intArray)

    var mAllWords = posRepository.getCartList()


    init {
        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            resource.data?.let {
                                taxServiceChargeRepository.addAllTaxDatabase(it.data.taxes)
                                //posRepository.addAllNotesDatabase(it.data.notes)

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

    private fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart()
        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {


        if (cartList != null && cartList.isEmpty()) {
            // empty cart hoy to new cart create kare
            val cartModel = addCartModel(item)
            addCart(cartModel)
        } else {

            // already cart ma hoy to add/update/delete kare flag wise
            val list = cartList?. get(0)?.items?.toMutableList()
            if (list != null && list.isNotEmpty()) {

                if (type == ADD || type == UPDATE) {
                    var index = -1

                    list.forEachIndexed { pos, tbItem ->
                        if (tbItem.itemId == item.itemId) {
                            index = pos
                            return@forEachIndexed
                        }
                    }
                    if (index != -1) {
                        val model = cartList[0].items?.get(index)
                        if (model != null) {
                            if (type == "UPDATE") {
                                model.itemQuantity = item.itemQuantity
                            } else
                                model.itemQuantity = item.itemQuantity
                            list[index] = model
                        }
                    } else {
                        item.itemQuantity = 1
                        list.add(item)
                    }
                } else if (type == DELETE) {
                    // single item remove from cart
                    list.remove(item)
                }
                val cartModel = CartModel().apply {
                    cartId = cartList[0].cartId
                    items = list
                }
                addCart(cartModel)

                if (list.isEmpty()) {
                    // delete carts
                    deleteCart()
                }
            }


        }
    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val cartModel = CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            item.itemQuantity = item.itemQuantity
            inventoryModelList.add(item)
            items = inventoryModelList
        }
        return cartModel
    }

    @SuppressLint("SetTextI18n")
    fun itemCalculation(
        itemList: List<TbItem>?,
        txtTotalAmount: AppCompatTextView,
        serviceChargesList: List<GetServiceChargeResponse.Data>?
    ) {


        totalPrice = 0.0
        totalCount = 0
        subTotalPrice = 0.0
        totalTax = 0.0
        totalServiceCharge = 0.0

        itemList?.forEach { item ->
            totalCount += item.itemQuantity
            subTotalPrice += item.price * item.itemQuantity

            item.taxes?.forEach { tax ->
                if (tax.isActive) {
                    if (tax.taxType == "Percentage") {
                        val itemTaxPrice = (tax.rate * (item.price * item.itemQuantity)) / 100
                        Log.e("itemTaxPrice", "" + itemTaxPrice)
                        totalTax += String.format("%.2f", itemTaxPrice)
                            .toDouble()
                    }
                }
            }

            item.modifiers.forEach {

                Log.e("modifiers", it.name)
            }
        }

        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {

            serviceChargesList.forEach {
                if (it.isEnabled) {
                    totalServiceCharge = (subTotalPrice * it.percentage) / 100
                    Log.e("totalServiceCharge", totalServiceCharge.toString())
                }
            }

        }


        Log.e(TAG, "totalTax  ${totalTax}")

        totalPrice = subTotalPrice + totalTax + totalServiceCharge

        txtTotalAmount.text = "$" + String.format(
            "%.2f",
            totalPrice
        )

    }


}