package com.android.pos.ui.fragments.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val venueData = posRepository.syncVenueData()


    val venueDataLocal = posRepository.venueDataLocal()

    var mAllWords = posRepository.getCartList()

    fun addCart(cartModel: CartModel) {

        viewModelScope.launch {
            appDatabase.cartDao().add(cartModel)
        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem) {

        val inventoryModelList = ArrayList<TbItem>()

        if (cartList != null && cartList!!.isEmpty()) {

            val cartModel = CartModel().apply {
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)

                item.itemQuantity = 1
                inventoryModelList.add(item)
                items = inventoryModelList
            }
            addCart(cartModel)
        } else {

            val list = cartList?.get(0)?.items?.toMutableList()

            var index = -1

            list?.forEachIndexed { pos, tbItem ->
                if (tbItem.itemId == item.itemId) {
                    index = pos
                    return@forEachIndexed
                }
            }
            if (index != -1) {
                val model = cartList?.get(0)?.items?.get(index)
                if (model != null) {
                    model.itemQuantity = model.itemQuantity + 1
                    list?.set(index, model)
                }
            } else {
                item.itemQuantity = 1
                list?.add(item)
            }
            val cartModel = CartModel().apply {
                cartId = cartList?.get(0)?.cartId!!
                items = list
            }
            addCart(cartModel)

        }
    }

}