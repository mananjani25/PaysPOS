package com.pays.pos.utils.callback

import com.pays.pos.data.entities.TbItem

interface ItemListner {

    fun onItemSelected(item: TbItem, position: Int)
    fun onCancelItemSelected(isCancel:Boolean = false)
    fun onCategorySelected(item: TbItem)

}