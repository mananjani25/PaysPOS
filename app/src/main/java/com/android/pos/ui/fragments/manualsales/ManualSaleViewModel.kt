package com.android.pos.ui.fragments.manualsales

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.UPDATE
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualSaleViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private val TAG = "ManualSaleViewModel"


    private fun addCart(cartModel: CartModel) {
        viewModelScope.launch {
            posRepository.addItemCart(cartModel)
        }
    }

    fun cartLogic(cartList: List<CartModel>?, item: TbItem, type: String) {
        if (cartList != null && cartList.isEmpty()) {
            val model = addCartModel(item)
            Log.e(TAG, "AddCartModel:  ${Gson().toJson(model)}")
            addCart(model)
        } else {
            val list = cartList?.get(0)?.items?.toMutableList()
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
                            } else {
                                model.itemQuantity = model.itemQuantity++
                            }
                            list.set(index, model)
                        }
                    } else {
                        item.itemQuantity = 1
                        list.add(item)
                    }
                } else if (type == DELETE) {
                    list.remove(item)
                }
                val cartModel = CartModel().apply {
                    cartId = cartList[0].cartId
                    items = list
                }
                addCart(cartModel)
                /*if (list.isEmpty()) {
                    deleteCart()
                }

*/
            }

        }


    }

    private fun addCartModel(item: TbItem): CartModel {
        val inventoryModelList = ArrayList<TbItem>()
        val cartModel = CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            item.itemQuantity = 1
            inventoryModelList.add(item)
            items = inventoryModelList
        }
        return cartModel
    }
}