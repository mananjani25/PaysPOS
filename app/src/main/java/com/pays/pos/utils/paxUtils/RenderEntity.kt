package com.pays.pos.utils.paxUtils

import android.view.View
import android.view.ViewGroup

interface RenderEntity : ViewCreator

interface ViewCreator {
    fun createView(parent: ViewGroup?): CommonItemView<*>?
}

interface CommonItemView<T : RenderEntity?> {
    fun render(renderEntity: T)
    val view: View?
}

