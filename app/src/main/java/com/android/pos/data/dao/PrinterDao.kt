package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.CartModel
import com.android.pos.data.model.PrinterListModel

interface PrinterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(printermodel:PrinterListModel):Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<PrinterListModel>)

    @Query("select * from PrintModel")
    fun allItem(orderType: String): LiveData<List<CartModel>>



}