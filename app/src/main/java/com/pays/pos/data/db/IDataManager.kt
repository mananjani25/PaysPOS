package com.pays.pos.data.db

import androidx.lifecycle.LiveData
import com.pays.pos.data.entities.TbCategory
import com.pays.pos.utils.statusUtils.Resource


interface IDataManager {

    suspend fun abs()

}