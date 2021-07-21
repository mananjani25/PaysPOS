package com.android.pos.data.db

import androidx.lifecycle.LiveData
import com.android.pos.data.entities.TbCategory
import com.android.pos.utils.statusUtils.Resource


interface IDataManager {

    suspend fun abs()
    fun categoryList(): LiveData<Resource<List<TbCategory>>>
}